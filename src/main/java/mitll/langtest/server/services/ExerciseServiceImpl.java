/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.services;

import mitll.langtest.client.services.ExerciseService;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.database.exercise.Search;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.exercise.TripleExercises;
import mitll.langtest.server.scoring.AlignmentHelper;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.sorter.SimpleSorter;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.AlignmentAndScore;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ExerciseServiceImpl<T extends CommonShell & ScoredExercise>
    extends MyRemoteServiceServlet
    implements ExerciseService<T> {
  private static final Logger logger = LogManager.getLogger(ExerciseServiceImpl.class);

  private static final int SLOW_EXERCISE_EMAIL = 2000;
  private static final int SLOW_MILLIS = 50;
  private static final int WARN_DUR = 100;

  private static final int MIN_DEBUG_DURATION = 30;
  private static final int MIN_WARN_DURATION = 1000;
  private static final String LISTS = "Lists";

  private static final boolean DEBUG_SEARCH = false;
  private static final boolean DEBUG = false;
  private static final boolean DEBUG_ID_LOOKUP = false;
  private static final boolean DEBUG_FULL = false;

  /**
   * @param request
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getTypeToValues
   */
  public FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException {
    int userFromSessionID = getUserIDFromSessionOrDB();
    return db.getFilterResponseHelper().getTypeToValues(request, getProjectIDFromUser(userFromSessionID), userFromSessionID);
  }

  /**
   * Complicated.
   * <p>
   * Get exercise ids, either from the predefined set or a user list.
   * Take the result and if there's a unit-chapter filter, use that to return only the exercises in the selected
   * units/chapters.
   * Further optionally filter by string prefix on each item's english or f.l.
   * <p>
   * Supports lookup by id
   * <p>
   * Marks items with state/second state given user id. User is used to mark the audio in items with whether they have
   * been played by that user.
   * <p>
   * Uses role to determine if we're in recorder mode and marks items recorded by the user as RECORDED.
   * <p>
   * Sorts the result by unit, then chapter, then alphabetically in chapter. If role is recorder, put the recorded
   * items at the front.
   *
   * @param request
   * @return
   */
  @Override
  public ExerciseListWrapper<T> getExerciseIds(ExerciseListRequest request) throws DominoSessionException {
    long then = System.currentTimeMillis();
    int projectID = request.getProjID() == -1 ? getProjectIDFromUser() : request.getProjID();

    if (projectID == -1) { // not sure how this can happen now that we throw DominoSessionException
      logger.warn("getExerciseIds project id is -1?  It should probably have a real value.");
      List<T> ts = new ArrayList<>();
      return new ExerciseListWrapper<>(request.getReqID(), ts);
    }

    logger.info("getExerciseIds : (" + getLanguage() + ") " + "getting exercise ids for request " + request);

    if (request.getDialogID() != -1) {
      return getDialogResponse(request, projectID);
    }

    try {
      boolean isUserListReq = request.getUserListID() != -1;
      UserList<CommonExercise> userListByID =
          isUserListReq ? db.getUserListByIDExercises(request.getUserListID(), getProjectIDFromUser()) : null;

      // logger.info("found user list " + isUserListReq + " " + userListByID);
      if (request.getTypeToSelection().isEmpty()) {   // no unit-chapter filtering
        // get initial exercise set, either from a user list or predefined
        ExerciseListWrapper<T> exerciseWhenNoUnitChapter = getExerciseWhenNoUnitChapter(request, projectID, userListByID);

        long diff = System.currentTimeMillis() - then;
        if (diff > 20 || true)
          logger.info("getExerciseIds : 1 req  " + request + " took " + diff +
              " millis to get " + exerciseWhenNoUnitChapter.getSize());

        return exerciseWhenNoUnitChapter;
      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        if (userListByID != null) {
          Collection<CommonExercise> exercisesForState =
              getExercisesFromUserListFiltered(request.getTypeToSelection(), userListByID);
          ExerciseListWrapper<T> exerciseListWrapperForPrefix = getExerciseListWrapperForPrefix(request,
              db.filterExercises(request, new ArrayList<>(exercisesForState), projectID), projectID);

          logger.info("getExerciseIds : 2 req  " + request + " took " + (System.currentTimeMillis() - then) +
              " millis to get " + exerciseListWrapperForPrefix.getSize());

          return exerciseListWrapperForPrefix;
        } else {
          ExerciseListWrapper<T> exercisesForSelectionState = getExercisesForSelectionState(request, projectID);

          logger.info("getExerciseIds : 3 req  " + request + " took " + (System.currentTimeMillis() - then) +
              " millis to get " + exercisesForSelectionState.getSize());

          return exercisesForSelectionState;
        }
      }
    } catch (Exception e) {
      logger.warn("got " + e, e);
      logAndNotifyServerException(e);
      return new ExerciseListWrapper<>();
    }
  }

  /**
   * Maybe dialog id on request is bogus?
   *
   * @param request
   * @param projectID
   * @return
   * @throws DominoSessionException
   * @see #getExerciseIds
   */
  private ExerciseListWrapper<T> getDialogResponse(ExerciseListRequest request, int projectID) throws DominoSessionException {
    List<CommonExercise> commonExercises = Collections.emptyList();

    {
      IDialog dialog = getDialog(request.getDialogID());
      if (dialog != null) {
        commonExercises = getCommonExercises(dialog.getCoreVocabulary());
        commonExercises.addAll(getCommonExercises(dialog.getExercises()));

        // logger.info("request " + request.getPrefix());
        {
          String prefix = request.getPrefix();
          if (!prefix.isEmpty()) {
            commonExercises = new ArrayList<>(getSearchMatches(commonExercises, prefix, projectID));
          }
        }
      }
    }

    if (request.isOnlyFL()) {
//      logger.info("before " + commonExercises.size());
      commonExercises = db.getFilterResponseHelper().getCommonExercisesWithoutEnglish(commonExercises);
      //    logger.info("after  " + commonExercises.size());
    }

    //logger.info("getDialogResponse returning exercises for " + request.getDialogID() + " " + collect.size());
    return makeExerciseListWrapper(request, commonExercises, projectID);
  }

  @NotNull
  private List<CommonExercise> getCommonExercises(List<ClientExercise> exercises) {
    return exercises
        .stream()
        .map(ClientExercise::asCommon)
        .collect(Collectors.toList());
  }

  /**
   * @param request
   * @param projectID
   * @param userListByID
   * @return
   */
  private ExerciseListWrapper<T> getExerciseWhenNoUnitChapter(ExerciseListRequest request,
                                                              int projectID,
                                                              UserList<CommonExercise> userListByID) {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises;
    boolean predefExercises = userListByID == null;
    exercises = predefExercises ? getExercises(projectID) : getCommonExercises(userListByID);
    // now if there's a prefix, filter by prefix match

    TripleExercises<CommonExercise> exercisesForSearch = new TripleExercises<>().setByExercise(exercises);

    {
      String prefix = request.getPrefix();
      if (!prefix.isEmpty()) {
        logger.info("getExerciseWhenNoUnitChapter found prefix '" + prefix + "' for user list " + userListByID);
        // now do a trie over matches
        exercisesForSearch = getSearchResult(request, projectID, exercises, predefExercises, prefix);

        if (exercisesForSearch.isEmpty()) {
          String prefix1 = prefix.replaceAll("\\s++", "");
          if (!prefix1.equals(prefix)) {
            // logger.info("OK remove the spaces " + prefix1);
            exercisesForSearch = getSearchResult(request, projectID, exercises, predefExercises, prefix1);
          }
        }
      }
    }

    if (DEBUG_ID_LOOKUP) logger.info("getExerciseWhenNoUnitChapter triple resp       " + exercisesForSearch);
    exercisesForSearch.setByExercise(db.filterExercises(request, exercisesForSearch.getByExercise(), projectID));
    if (DEBUG_ID_LOOKUP) logger.info("getExerciseWhenNoUnitChapter after triple resp " + exercisesForSearch);

    // TODO : I don't think we need this?
/*        if (!isUserListReq) {
          markRecordedState(userID, request.getActivityType(), exercises, request.isOnlyExamples());
        }*/

    // now sort : everything gets sorted the same way
    List<CommonExercise> commonExercises;

    if (request.isOnlyExamples() && !request.getPrefix().isEmpty()) {
      boolean sortByFL = getProject(projectID).isEnglish();
      String searchTerm = request.getPrefix();
      commonExercises = new ArrayList<>();

      // make sure we only return the context exercises...
      exercisesForSearch.getByContext().forEach(ex -> ex.getDirectlyRelated().forEach(dir -> commonExercises.add(dir.asCommon())));
      getSortedExercises(sortByFL, searchTerm, commonExercises);

      logger.info("getExerciseWhenNoUnitChapter OK only return context " + commonExercises.size());
    } else {
      commonExercises = getSortedExercises(request, exercisesForSearch, predefExercises, projectID);
    }

    long now = System.currentTimeMillis();

    long diff = now - then;
    if (diff > 20 || DEBUG_ID_LOOKUP) {
      logger.info("getExerciseWhenNoUnitChapter took " + diff + " to get " + commonExercises.size());
    }

    return makeExerciseListWrapper(request, commonExercises, projectID);
  }

  private TripleExercises<CommonExercise> getSearchResult(ExerciseListRequest request, int projectID, List<
      CommonExercise> exercises, boolean predefExercises, String prefix) {
    TripleExercises<CommonExercise> exercisesForSearch;
    exercisesForSearch = getExercisesForSearch(prefix, exercises, predefExercises, projectID, request.getUserID(),
        !request.isPlainVocab());
    if (request.getLimit() > 0) {
      exercisesForSearch.setByExercise(getFirstFew(prefix, request, exercisesForSearch.getByExercise(), projectID));
    }
    return exercisesForSearch;
  }

  /**
   * Complicated -- put the vocab matches first, then context matches second
   *
   * @param request
   * @param tripleExercises
   * @param predefExercises
   * @return
   * @see #getExerciseWhenNoUnitChapter
   */
  private List<CommonExercise> getSortedExercises(ExerciseListRequest request,
                                                  TripleExercises<CommonExercise> tripleExercises,
                                                  boolean predefExercises,
                                                  int projID) {
    Project projectForUser = getProject(projID);

    List<CommonExercise> commonExercises = tripleExercises.getByExercise();
    if (commonExercises.isEmpty()) {
      commonExercises = tripleExercises.getByID();
    }
    if (predefExercises) {
      commonExercises = new ArrayList<>(tripleExercises.getByID());
      List<CommonExercise> basicExercises = new ArrayList<>(tripleExercises.getByExercise());
      boolean sortByFL = projectForUser.isEnglish();
      String searchTerm = request.getPrefix();

      {
        // only do this if we control the sort in the facet exercise list drop down
        sortExercises(basicExercises, sortByFL, searchTerm);
      }

      Set<Integer> unique = new HashSet<>();
      if (DEBUG)
        logger.info("getSortedExercises adding " + tripleExercises.getByID().size() + " by id tripleExercises");

      // 1) first add any exact by id matches - should only be one
      if (tripleExercises.getByID().size() > 1)
        logger.error("expecting only 0 or 1 matches for by id " + tripleExercises.getByID().size());

      tripleExercises.getByID().forEach(e -> unique.add(e.getID()));
      if (DEBUG) logger.info("getSortedExercises adding " + basicExercises.size() + " basicExercises");

      basicExercises
          .stream()
          .filter(e -> !unique.contains(e.getID()))
          .forEach(commonExercises::add);

      commonExercises.forEach(e -> unique.add(e.getID()));

      // last come context matches
      {
        List<CommonExercise> contextExercises = getSortedContext(tripleExercises, sortByFL, searchTerm);
        if (DEBUG)
          logger.info("getSortedExercises adding " + contextExercises.size() + " contextExercises, " + unique.size() + " unique");

        contextExercises
            .stream()
            .filter(e -> !unique.contains(e.getID()))
            .forEach(commonExercises::add);
      }
    }
    // }
    return commonExercises;
  }

  @NotNull
  private List<CommonExercise> getSortedContext(TripleExercises<CommonExercise> tripleExercises,
                                                boolean sortByFL, String searchTerm) {
    List<CommonExercise> byContext = tripleExercises.getByContext();
    return getSortedExercises(sortByFL, searchTerm, byContext);
  }

  @NotNull
  private List<CommonExercise> getSortedExercises(boolean sortByFL, String
      searchTerm, List<CommonExercise> byContext) {
    boolean hasSearch = !searchTerm.isEmpty();

    List<CommonExercise> contextExercises = new ArrayList<>(byContext);
    {
      if (!contextExercises.isEmpty() && hasSearch) {
        // if the search term is in the fl, sort by fl
        sortByFL = contextExercises.iterator().next().getForeignLanguage().contains(searchTerm);
        if (DEBUG) logger.info("getSortedExercises found search term " + searchTerm + " = " + sortByFL);
      }
      sortExercises(contextExercises, sortByFL, searchTerm);
    }
    return contextExercises;
  }


  /**
   * Return shortest matches first (on fl term).
   *
   * @param request
   * @param exercises
   * @param projid
   * @return
   */
  @NotNull
  private List<CommonExercise> getFirstFew(String prefix, ExerciseListRequest request, List<CommonExercise> exercises,
                                           int projid) {
    //logger.info("getFirstFew only taking " + request.getLimit() + " from " + exercises.size() + " that match " + prefix);
    Collator collator = getAudioFileHelper(projid).getCollator();

    exercises.sort((o1, o2) -> {
      String foreignLanguage = o1.getForeignLanguage();
      boolean hasSearch1 = foreignLanguage.toLowerCase().contains(prefix.toLowerCase());
      String foreignLanguage1 = o2.getForeignLanguage();
      boolean hasSearch2 = foreignLanguage1.toLowerCase().contains(prefix.toLowerCase());

      if (hasSearch1 && !hasSearch2) return -1;
      else if (hasSearch2 && !hasSearch1) return +1;
      else if (hasSearch1) {
        int i = Integer.compare(foreignLanguage.length(), foreignLanguage1.length());
        return i == 0 ? collator.compare(foreignLanguage, foreignLanguage1) : i;
      } else {
        String cforeignLanguage = o1.getContext();
        String cforeignLanguage1 = o2.getContext();
        int i = Integer.compare(cforeignLanguage.length(), cforeignLanguage1.length());
        return i == 0 ? collator.compare(cforeignLanguage, cforeignLanguage1) : i;
      }
    });

    exercises = exercises.subList(0, Math.min(exercises.size(), request.getLimit()));

    return exercises;
  }

  /**
   * TODO : this doesn't make sense - we need to do this on all projects, once.
   * Here it's just doing it on the first project that's asked for.
   * <p>
   * TODO : remove duplicate
   * Called from the client:
   *
   * @return
   * @see #getExerciseWhenNoUnitChapter
   * @see #getExerciseWhenNoUnitChapter(ExerciseListRequest, int, UserList)
   */
  private List<CommonExercise> getExercises(int projectID) {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises = db.getExercises(projectID, false);
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("getExercises took " + (now - then) + " millis to get the raw exercise list for " + projectID);// getLanguage(projectID));
    }

    return exercises;
  }

  /**
   * Copies the exercises....?
   *
   * @param userListByID
   * @return
   * @see #getExerciseIds
   * @see #getExercisesFromUserListFiltered(java.util.Map, mitll.langtest.shared.custom.UserList)
   */
  private <U extends CommonExercise> List<U> getCommonExercises(UserList<U> userListByID) {
    return new ArrayList<>(userListByID.getExercises());
  }

  /**
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises
   * @see #getExerciseIds
   */
  private ExerciseListWrapper<T> getExercisesForSelectionState(ExerciseListRequest request, int projid) {
    return getExerciseListWrapperForPrefix(request,
        db.getFilterResponseHelper().getExercisesForSelectionState(request, projid), projid);
  }


  /**
   * Always sort the result
   *
   * @param exercisesForState
   * @param projID
   * @return
   * @see #getExerciseIds
   * @see #getExercisesForSelectionState
   */
  private ExerciseListWrapper<T> getExerciseListWrapperForPrefix(ExerciseListRequest request,
                                                                 Collection<CommonExercise> exercisesForState,
                                                                 int projID) {
    logger.info("getExerciseListWrapperForPrefix initially found " + exercisesForState.size());

    String prefix = request.getPrefix().trim(); // leading or trailing spaces shouldn't do anything
    int userID = request.getUserID();
    //boolean incorrectFirst = request.isIncorrectFirstOrder();

    boolean hasPrefix = !prefix.isEmpty();
    if (hasPrefix) {
      logger.info("getExerciseListWrapperForPrefix" +
          "\n\tuserID   " + userID +
          "\n\tprefix   '" + prefix + "'" +
          (request.getActivityType() != ActivityType.UNSET ? "" : "\n\tactivity " + request.getActivityType()));
    }

//    int i = markRecordedState(userID, request.getActivityType(), exercisesForState, request.isOnlyExamples());
    //   logger.info("getExerciseListWrapperForPrefix marked " + i + " as recorded");

    if (hasPrefix) {
      logger.info("getExerciseListWrapperForPrefix check for prefix match over " + exercisesForState.size());
      exercisesForState = getSearchMatches(exercisesForState, prefix, projID);
      if (exercisesForState.isEmpty()) {
        String noSpaces = prefix.replaceAll("\\s++", "");
        if (!noSpaces.equals(prefix)) {
          logger.info("\ttrying " + noSpaces);
          exercisesForState = getSearchMatches(exercisesForState, noSpaces, projID);
        }
      }
    }

    if (exercisesForState.isEmpty() && !prefix.isEmpty()) { // allow lookup by id
      CommonExercise exercise = getExercise(userID, projID, prefix);
      if (exercise != null) exercisesForState = Collections.singletonList(exercise);
    }
    // why copy???
    List<CommonExercise> copy = new ArrayList<>(exercisesForState);
    sortExercises(copy, false, request.getPrefix());
    logger.info("getExerciseListWrapperForPrefix returning " + copy.size());

    return makeExerciseListWrapper(request, copy, projID);
  }

  /**
   * TODO : replace with search matcher
   * So first we search over vocab, then over context sentences
   *
   * @param exercisesForState
   * @param prefix
   * @param projID
   * @return
   * @see #getExerciseListWrapperForPrefix(ExerciseListRequest, Collection, int)
   */
  @NotNull
  private Collection<CommonExercise> getSearchMatches(Collection<CommonExercise> exercisesForState,
                                                      final String prefix,
                                                      final int projID) {
    Collection<CommonExercise> originalSet = exercisesForState;
    // logger.info("original set" +originalSet.size());
    long then = System.currentTimeMillis();
    String language = getLanguage(projID);
    SmallVocabDecoder smallVocabDecoder = getSmallVocabDecoder(projID);

    {
      ExerciseTrie<CommonExercise> trie =
          new ExerciseTrie<>(exercisesForState, language, smallVocabDecoder, true, false);
      long now = System.currentTimeMillis();
      if (now - then > 20 || DEBUG_SEARCH)
        logger.info("getSearchMatches took " + (now - then) + " millis to build trie for " + exercisesForState.size() + " exercises");
      exercisesForState = trie.getExercises(prefix);

      if (exercisesForState.isEmpty()) {
        String prefix1 = StringUtils.stripAccents(prefix);
        exercisesForState = trie.getExercises(prefix1);
        logger.info("getSearchMatches trying '" + prefix1 + "' instead of " + prefix + " found " + exercisesForState.size());
      } else if (DEBUG_SEARCH) {
        exercisesForState.forEach(exercise -> logger.info("trie for " + prefix + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage()));
      }
    }

    Set<Integer> unique = new HashSet<>();
    exercisesForState.forEach(e -> unique.add(e.getID()));

    {
      then = System.currentTimeMillis();

      if (DEBUG_SEARCH) {
        originalSet.forEach(exercise -> {
          logger.info("ex " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage());
          exercise.getDirectlyRelated().forEach(ex -> {
            logger.info("\tdirect ex " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage());
          });
        });
      }

      ExerciseTrie<CommonExercise> contextTrie = new ExerciseTrie<>(originalSet, language, smallVocabDecoder, false, false);
      long now = System.currentTimeMillis();
      if (now - then > 20 || DEBUG_SEARCH) {
        logger.info("took " + (now - then) + " millis to build trie for " + originalSet.size() + " context exercises");
      }

      List<CommonExercise> contextExercises = contextTrie.getExercises(prefix);
      if (contextExercises.isEmpty()) {
        contextExercises = contextTrie.getExercises(StringUtils.stripAccents(prefix));
        logger.info("getSearchMatches context " +
            "trying '" + StringUtils.stripAccents(prefix) + "' instead of " + prefix + " found " + contextExercises.size());
      } else if (DEBUG_SEARCH) {
        contextExercises.forEach(exercise -> logger.info("contextExercises trie for '" + prefix + "' = " +
            "eng '" + exercise.getEnglish() + "' '" + exercise.getForeignLanguage() + "'"));
      }
      exercisesForState.addAll(contextExercises.stream().filter(e -> !unique.contains(e.getID())).collect(Collectors.toList()));
    }
    if (exercisesForState.isEmpty()) {
      logger.info("getSearchMatches neither " + prefix + " nor " + StringUtils.stripAccents(prefix) + " found any matches against " + exercisesForState.size());
    }
    return exercisesForState;
  }

  /**
   * TODO : what to do with request role here?
   * <p>
   * NOTE NOTE NOTE : not doing ensureMP3 - since we likely don't have access to file system for here.
   * ALSO - ideally this is done at the moment the wav is made.
   * <p>
   * Send the first exercise along so we don't have to ask for it after we get the initial list
   * <p>
   * scores should be consistent with getScoreHistoryPerExercise
   *
   * @param exercises
   * @param projID
   * @return
   * @see #getScoreHistoryPerExercise
   * @see #getExerciseIds
   * @see #getExerciseListWrapperForPrefix
   */
  private ExerciseListWrapper<T> makeExerciseListWrapper(ExerciseListRequest request,
                                                         Collection<CommonExercise> exercises,
                                                         int projID) {
    long then = System.currentTimeMillis();
    int userID = request.getUserID();
    List<T> exerciseShells = getExerciseShells(exercises, request.isQC());

    ExerciseListWrapper<T> exerciseListWrapper = new ExerciseListWrapper<T>(request.getReqID(), exerciseShells);

    if (!request.isQC()) {
      // should be consistent with getScoreHistoryPerExercise
      // @see #getScoreHistoryPerExercise
      Map<Integer, Float> scores = db.getResultDAO().getScores(userID, exerciseShells);

/*
      List<Integer> sorted = new ArrayList<>(scores.keySet());
      Collections.sort(sorted);
      sorted.forEach(exid -> logger.info("exids #" + exid+" -> " + scores.get(exid)));
*/
      exerciseListWrapper.setIdToScore(scores);
    }

    logger.debug("makeExerciseListWrapper returning " + exerciseListWrapper + " in " + (System.currentTimeMillis() - then) + " millis");
    return exerciseListWrapper;
  }

  /**
   * 0) Add annotations to fields on exercise
   * 1) Attach audio recordings to exercise.
   * 2) Adds information about whether the audio has been played or not...
   * 3) Attach history info (when has the user recorded audio for the item under the learn tab and gotten a score)
   *
   * @param userID
   * @param firstExercise
   * @param isQC
   * @param projID
   * @seex LoadTesting#getExercise(String, long, boolean)
   * @see #makeExerciseListWrapper
   * @see #getAnnotatedExercise(int, int, int)
   */
  private void addAnnotationsAndAudio(int userID, CommonExercise firstExercise, boolean isQC, int projID) {
    long then = System.currentTimeMillis();

    if (DEBUG) {
      logger.info("addAnnotationsAndAudio adding anno to " + firstExercise.getID() +
          "\n\twith " + firstExercise.getDirectlyRelated().size() + " context exercises");
    }

    addAnnotations(firstExercise); // todo do this in a better way

    long now = System.currentTimeMillis();
    int oldID = firstExercise.getID();
    String language = getLanguage(projID);

    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + language + ") took " + (now - then) + " millis to add annotations to exercise " + oldID);
    }
    then = now;

    db.getAudioDAO().attachAudioToExercises(Collections.singleton(firstExercise), db.getLanguageEnum(projID), projID);

    if (DEBUG) {
      // logger.info("attached " + i + " audio cuts to " + firstExercise.getID());
      for (AudioAttribute audioAttribute : firstExercise.getAudioAttributes())
        logger.debug("\t addAnnotationsAndAudio ex " + oldID + " audio " + audioAttribute);
    }

    now = System.currentTimeMillis();
    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + language + ") took " +
          (now - then) + " millis to attach audio to exercise " + oldID);
    }
    then = now;

    if (isQC) {
      addPlayedMarkings(userID, firstExercise);
      now = System.currentTimeMillis();
      if (now - then > SLOW_MILLIS) {
        logger.debug("addAnnotationsAndAudio : (" + language + ") took " + (now - then) + " millis to add played markings to exercise " + oldID);
      }
    }

    then = now;

    db.getResultDAO().attachScoreHistory(userID, firstExercise, getLanguageEnum(firstExercise));

    now = System.currentTimeMillis();
    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + language + ") took " + (now - then) + " millis to attach score history to exercise " + oldID);
    }

    if (DEBUG) {
      for (AudioAttribute audioAttribute : firstExercise.getAudioAttributes())
        logger.debug("\t addAnnotationsAndAudio ret ex " + oldID + " audio " + audioAttribute);
    }
  }

  private String getLanguage(int projID) {
    return db.getLanguage(projID);
  }

  /**
   * Add annotations to parent exercise and any child exercise.
   *
   * @param byID
   * @see #addAnnotationsAndAudio(int, CommonExercise, boolean, int)
   */
  private void addAnnotations(ClientExercise byID) {
    IUserListManager userListManager = db.getUserListManager();
    userListManager.addAnnotations(byID);
    byID.getDirectlyRelated().forEach(userListManager::addAnnotations);
  }

  /**
   * Only add the played markings if doing QC.
   * <p>
   * TODO : This is an expensive query - we need a smarter way of remembering when audio has been played.
   *
   * @param userID
   * @param firstExercise
   * @see #addAnnotationsAndAudio(int, CommonExercise, boolean, int)
   */
  private void addPlayedMarkings(int userID, CommonExercise firstExercise) {
    db.getEventDAO().addPlayedMarkings(userID, firstExercise);
  }

  /**
   * TODO : slow?
   *
   * @param commonExercises
   * @param sortByFL
   * @param searchTerm
   * @paramx role
   * @see #getExerciseListWrapperForPrefix(ExerciseListRequest, Collection, int)
   * @see #getSortedExercises
   */
  private <U extends CommonShell> void sortExercises(List<U> commonExercises,
                                                     boolean sortByFL,
                                                     String searchTerm) {
    new SimpleSorter<U>().getSorted(commonExercises, sortByFL, searchTerm);
  }


  /**
   * TODO : revisit the parameterized types here.
   *
   * Search should not return context - should get its parent...
   *
   * If you ask for a context exercise id, you get back the parent exercise.
   * If you look up by domino id, you get the matching exercise.
   *
   * @param prefix
   * @param exercises
   * @param predefExercises
   * @param matchOnContext
   * @return
   * @see #getExerciseIds
   */
  private TripleExercises<CommonExercise> getExercisesForSearch(String prefix,
                                                                Collection<CommonExercise> exercises,
                                                                boolean predefExercises,
                                                                int projectID,
                                                                int userID,
                                                                boolean matchOnContext) {
    Search<CommonExercise> search = new Search<CommonExercise>(db);
    TripleExercises<CommonExercise> exercisesForSearch =
        search.getExercisesForSearch(prefix, exercises, predefExercises, projectID, matchOnContext);
    exercisesForSearch.setByID(Collections.emptyList());

    {
      int exid = search.getID(prefix.trim());
      if (exid != -1 && exid != 1) {
        getSpecialExerciseByID(projectID, userID, exercisesForSearch, exid);
      }
    }

    return exercisesForSearch;
  }

  private void getSpecialExerciseByID(int projectID, int userID, TripleExercises<
      CommonExercise> exercisesForSearch, int exid) {
    if (DEBUG_ID_LOOKUP) logger.info("getExercisesForSearch looking for exercise in " + projectID + " = " + exid);
    CommonExercise exercise = getAnnotatedExercise(userID, projectID, exid);

    if (exercise != null && exercise.isContext()) {
      if (DEBUG_ID_LOOKUP)
        logger.info("\tgetExercisesForSearch found context sentence in " + projectID + " = " + exid);
      int parentExerciseID = getParentExerciseID(projectID, exid, exercise);

      if (parentExerciseID > 0) {
        exercise = getAnnotatedExercise(userID, projectID, parentExerciseID);
        if (exercise != null) {
          exid = parentExerciseID;
          if (DEBUG_ID_LOOKUP)
            logger.info("\tgetExercisesForSearch found parent for sentence in " + projectID + " = " + exid);
        }
      }
    }

    if (exercise == null) {
      if (DEBUG_ID_LOOKUP)
        logger.info("getExercisesForSearch looking for exercise in " + projectID + " with domino id " + exid);
      exid = db.getExerciseDAO(projectID).getExIDForDominoID(projectID, exid);
      if (exid > 0) {
        if (DEBUG_ID_LOOKUP)
          logger.info("getExercisesForSearch got match for domino - exercise in " + projectID + " = " + exid);
        exercise = getAnnotatedExercise(userID, projectID, exid);
      }
    }

    if (exercise != null && exercise.getProjectID() == projectID) {
      exercisesForSearch.setByID(Collections.singletonList(exercise));
    } else {
      logger.info("getExercisesForSearch no exercise with " + projectID + " " + exid);
    }
  }

  private int getParentExerciseID(int projectID, int exid, CommonExercise exercise) {
    int parentExerciseID1 = exercise.getParentExerciseID();
    return parentExerciseID1 > 0 ? parentExerciseID1 : db.getExerciseDAO(projectID).getParentFor(exid);
  }

  /**
   * On the fly we make a new section helper to do filtering of user list.
   * <p>
   * TODO : include lists as just another facet for the purpose of filtering
   * If the filter and then the list results in no exercises, unselect the list.
   *
   * @param typeToSelection
   * @param userListByID
   * @param
   * @return
   * @see #getExerciseIds
   */
  private <U extends CommonExercise> Collection<U> getExercisesFromUserListFiltered(
      Map<String, Collection<String>> typeToSelection,
      UserList<U> userListByID) {
    Collection<U> listExercises = getCommonExercises(userListByID);
    typeToSelection.remove(LISTS);
    if (typeToSelection.isEmpty()) {
      logger.info("getExercisesFromUserListFiltered returning  " + userListByID.getExercises().size() +
          " exercises for " + userListByID.getID());
      return listExercises;
    } else {
      SectionHelper<U> helper = new SectionHelper<>();

      logger.info("getExercisesFromUserListFiltered found " + listExercises.size() + " for list " + userListByID);
      long then = System.currentTimeMillis();
      listExercises.forEach(helper::addExercise);
      long now = System.currentTimeMillis();

      if (now - then > 100) {
        logger.debug("getExercisesFromUserListFiltered used " + listExercises.size() + " exercises to build a hierarchy in " + (now - then) + " millis");
      }
      //helper.report();
      Collection<U> exercisesForState = helper.getExercisesForSelectionState(typeToSelection);
      logger.debug("\tgetExercisesFromUserListFiltered after found " + exercisesForState.size() + " matches to " + typeToSelection);
      return /*typeToSelection.isEmpty() ? listExercises :*/ exercisesForState;
    }
  }

  private int warn = 0;

  /**
   * Save transmission bandwidth - don't send a list of fully populated items - just send enough to populate a list
   *
   * @param exercises
   * @param skipDups
   * @return
   * @see #makeExerciseListWrapper
   */
  private List<T> getExerciseShells(Collection<CommonExercise> exercises, boolean skipDups) {
    List<T> ids = new ArrayList<>(exercises.size());

    Set<Integer> checkDups = new HashSet<>();
    exercises.forEach(ex -> {
      if (skipDups && checkDups.contains(ex.getID())) {
        //   logger.info("skip dup " + ex.getID());
      } else {
        checkDups.add(ex.getID());
        ids.add((T) ex.getShell());
      }
    });
    return ids;
  }

  /**
   * NOTE NOTE NOTE : not doing ensureMP3 - since we likely don't have access to file system for here.
   * Joins with annotation data when doing QC.
   *
   * @param exid
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   * @see mitll.langtest.client.list.ExerciseList#goGetNextAndCacheIt
   * @see mitll.langtest.client.analysis.PlayAudio#playLast
   */
  public T getExercise(int exid) throws DominoSessionException {
    return (T) getAnnotatedExercise(getUserIDFromSessionOrDB(), getProjectIDFromUser(), exid);
  }

  /**
   * So on netprof1-dev, we need to tell it the exercise has changed.
   *
   * @param projID
   * @param exid
   * @throws DominoSessionException
   * @see NewUserExercise#refreshEx
   */
  @Override
  public void refreshExercise(int projID, int exid) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    getDatabase().getProject(projID).getExerciseDAO().refresh(exid);
  }

  public int getExerciseIDOrParent(int exid) throws DominoSessionException {
    int projectIDFromUser = getProjectIDFromUser();
    CommonExercise exerciseByID = getProject(projectIDFromUser).getExerciseByID(exid);
    if (exerciseByID != null && exerciseByID.isContext()) {
      int parentExerciseID = exerciseByID.getParentExerciseID();
      if (parentExerciseID == -1) {
        parentExerciseID = getParentExerciseID(projectIDFromUser, exid, exerciseByID);
      }
      return parentExerciseID;
    } else {
      return exid;
    }
  }

  /**
   * @param userID
   * @param projectID
   * @param exid
   * @return
   * @see #getExercise(int, boolean)
   */
  @Nullable
  private CommonExercise getAnnotatedExercise(int userID, int projectID, int exid) {
    long then2 = System.currentTimeMillis();
    CommonExercise byID = db.getCustomOrPredefExercise(projectID, exid);

    if (byID == null) {
      logger.warn("getAnnotatedExercise : can't find exercise #" + exid + " in project #" + projectID);
      return null;
    }

    byID = new Exercise(byID);

    long now = System.currentTimeMillis();

    String language = getLanguage(projectID);
    {
      if (now - then2 > WARN_DUR) {
        logger.debug("getAnnotatedExercise : (" + language + ") took " + (now - then2) + " millis to find exercise " +
            exid + " for " + userID);
      }
    }

/*    if (byID == null) {
      Collection<CommonExercise> exercises = getExercises();
      logger.error("getAnnotatedExercise : huh? couldn't find exercise with id '" + exid + "' when examining " +
          exercises.size() + " items");
    } else {*/
    logger.debug("getAnnotatedExercise : (" + language +
            ") project " + projectID +
            " find exercise " + exid + " for " + userID + " : " + byID
        //+"\n\tcontext" + byID.getDirectlyRelated()
    );
    then2 = System.currentTimeMillis();
    addAnnotationsAndAudio(userID, byID, false, projectID);
    now = System.currentTimeMillis();
    if (now - then2 > WARN_DUR) {
      logger.debug("getAnnotatedExercise : (" + language + ") took " + (now - then2) + " millis to add annotations to " +
          "exercise " + exid + " for " + userID);
    }
//      then2 = System.currentTimeMillis();

    //logger.debug("getExercise : returning " + byID);
    // NOTE : not ensuring MP3s or OGG versions of WAV file.
    // ensureMP3s(byID, pathHelper.getInstallPath());

    if (DEBUG) {
      for (AudioAttribute audioAttribute : byID.getAudioAttributes())
        logger.debug("\t addAnnotationsAndAudio after ensure mp3 ex " + byID.getID() + " audio " + audioAttribute);
      logger.debug("getExercise : returning " + byID);
    }

//      now = System.currentTimeMillis();
//      if (now - then2 > WARN_DUR) {
//        if (WARN_MISSING_FILE) {
//          logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis " +
//              "to ensure there are mp3s for exercise " + exid + " for " + userID);
//        }
//      }
    //  }
    checkPerformance(projectID, exid, then2);

    /*    if (byID != null) {
     *//*
      logger.debug("returning (" + language + ") exercise " + byID.getOldID() + " : " + byID);
      for (AudioAttribute audioAttribute : byID.getAudioAttributes()) {
        logger.info("\thas " + audioAttribute);
      }
*//*
    } else {
      logger.warn(language + " : couldn't find exercise with id '" + exid + "'");
    }*/
    // return byID;
    // TODO : why doesn't this work?
    return byID;
  }

  /**
   * @param request
   * @param ids
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getExercises
   */
  @Override
  public ExerciseListWrapper<ClientExercise> getFullExercises(ExerciseListRequest request, Collection<Integer> ids) throws DominoSessionException {
    List<ClientExercise> exercises = new ArrayList<>();

    int userID = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser(userID);
    if (projectID == -1) {
      logger.info("getFullExercises : no project for user " + userID);
      return new ExerciseListWrapper<>();
    }
    Language language = getLanguageEnum(projectID);

    long then = System.currentTimeMillis();
    Set<ClientExercise> toAddAudioTo = getCommonExercisesWithAnnotationsAdded(ids, exercises, projectID);
    long now = System.currentTimeMillis();

    if (now - then > 20 || DEBUG_FULL)
      logger.info("getFullExercises took " + (now - then) + " to get " + exercises.size() + " exercises" +
          "\n\tfor req = " + ids);

/*
    if (ids.size() > toAddAudioTo.size()) {
//      logger.info("getFullExercises decreased from " + ids.size() + " to " + toAddAudioTo.size());
    } else {
//      logger.info("getFullExercises getting " + ids.size() + " exercises");
    }
*/

//    if (!toAddAudioTo.isEmpty()) {
    then = System.currentTimeMillis();
    // TODO : sigh - super expensive...?
    db.getAudioDAO().attachAudioToExercises(toAddAudioTo, language, projectID);
    now = System.currentTimeMillis();

    if (now - then > 10 || DEBUG_FULL)
      logger.info("getFullExercises took " + (now - then) + " to attach audio to " + toAddAudioTo.size() + " exercises");

//    then = System.currentTimeMillis();
//    new AlignmentHelper(serverProps, db.getRefResultDAO()).addAlignmentOutput(getProject(projectID), toAddAudioTo);
//    now = System.currentTimeMillis();

    if (now - then > 20 || DEBUG_FULL)
      logger.info("getFullExercises took " + (now - then) + " to attach alignment output to " + toAddAudioTo.size() + " exercises");

//    } else {
//      // logger.info("getFullExercises all " + ids.size() + " exercises have audio");
//    }

    then = System.currentTimeMillis();

    now = System.currentTimeMillis();
    if (now - then > 50 || DEBUG_FULL)
      logger.info("getFullExercises took " + (now - then) + " to add scores to " + exercises.size() + " exercises");

    Map<Integer, CorrectAndScore> scoreHistoryPerExercise = getScoreHistoryPerExercise(ids, exercises, userID, language);
    logger.info("getFullExercises found " + exercises.size() + " exercises and " + scoreHistoryPerExercise.size() + " scores");

/*
    List<Integer> sorted = new ArrayList<>(scoreHistoryPerExercise.keySet());
    Collections.sort(sorted);
    sorted.forEach(exid -> logger.info("correct & score : exids #" + exid+" -> " + scoreHistoryPerExercise.get(exid)));
*/

    maybeAddPlayedMarkings(request, exercises, userID);

    then = System.currentTimeMillis();

    Set<Integer> audioIDs = new HashSet<>();
    for (ClientExercise exercise : exercises) {
      for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
        audioIDs.add(audioAttribute.getUniqueID());
      }
      exercise.getDirectlyRelated().forEach(exercise1 -> exercise1.getAudioAttributes().forEach(audioAttribute -> audioIDs.add(audioAttribute.getUniqueID())));
    }
    Map<Integer, AlignmentAndScore> cachedAlignments = db.getRefResultDAO().getCachedAlignments(projectID, audioIDs);

    now = System.currentTimeMillis();
    if (now - then > 30 || DEBUG_FULL)
      logger.info("getFullExercises took " + (now - then) + " to get cached scores for " + exercises.size() + " exercises");

    // for (CommonExercise exercise : exercises) logger.info("\treturning " + exercise.getID());
    return new ExerciseListWrapper<>(request.getReqID(), exercises, scoreHistoryPerExercise, cachedAlignments);
  }

  private void maybeAddPlayedMarkings(ExerciseListRequest request, List<ClientExercise> exercises, int userID) {
    if (request.isOnlyUninspected() && request.isQC()) {
      exercises.forEach(firstExercise -> {
        addPlayedMarkings(userID, firstExercise.asCommon());
      });
    }
  }

  /**
   * Find the closest student answer in time for this exercise - and return the path to the audio file
   * so we can play the audio from analysis.
   *
   * @param userID
   * @param exid
   * @param nearTime
   * @return Pair - ref audio first, student audio second
   * @throws DominoSessionException
   * @see mitll.langtest.client.analysis.PlayAudio#playLast
   */
  @Override
  public Pair getLatestScoreAudioPath(int userID, int exid, long nearTime) throws DominoSessionException {
    getUserIDFromSessionOrDB();  // just so we check permissions

    int projectIDFromUser = getProjectIDFromUser(userID);

    List<CorrectAndScore> correctAndScoresForEx = getCorrectAndScoresForEx(userID, projectIDFromUser, exid);

    logger.info("getLatestScoreAudioPath user " + userID + " project " + projectIDFromUser +
        " at " + new Date(nearTime) + " found " + correctAndScoresForEx.size());

    CorrectAndScore closest = getCorrectAndScoreClosestToTime(correctAndScoresForEx, nearTime);

    Pair pair = new Pair(
        getRefAudio(userID, projectIDFromUser, exid),
        closest == null ? null : closest.getPath());

    logger.info("getLatestScoreAudioPath returning " + pair);

    return pair;
  }

  @Override
  public void reload(int projid) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    db.getExerciseDAO(projid).reload();
  }

  @Nullable
  private CorrectAndScore getCorrectAndScoreClosestToTime(List<CorrectAndScore> correctAndScoresForEx, long nearTime) {
    CorrectAndScore closest = null;
    long closestTimeDiff = Long.MAX_VALUE;
    for (CorrectAndScore correctAndScore : correctAndScoresForEx) {
      long timeDiff = Math.abs(correctAndScore.getTimestamp() - nearTime);
      if (closest == null || timeDiff < closestTimeDiff) {
        closest = correctAndScore;
        closestTimeDiff = timeDiff;
      }
    }
    if (closestTimeDiff > 10000) {
      logger.warn("huh? returning " + closest + " despite req for " + new Date(nearTime));
    }
    return closest;
  }

  /**
   * @param userID
   * @param projectIDFromUser
   * @param exid
   * @return
   * @see #getLatestScoreAudioPath
   */
  @Nullable
  private String getRefAudio(int userID, int projectIDFromUser, int exid) {
    CommonExercise byID = db.getCustomOrPredefExercise(projectIDFromUser, exid);
    String refAudio = null;
    if (byID == null) {
      logger.warn("getRefAudio can't find ex id " + exid);
    } else {
      db.getAudioDAO().attachAudioToExercises(Collections.singleton(byID), getLanguageEnum(projectIDFromUser), projectIDFromUser);
      AudioAttribute audioAttributePrefGender = byID.getAudioAttributePrefGender(db.getUserDAO().isMale(userID), true);
      if (audioAttributePrefGender == null) {
        logger.warn("getRefAudio : no audio on ex " + exid + " ?");
      }
      refAudio = audioAttributePrefGender == null ? null : audioAttributePrefGender.getAudioRef();
    }
    return refAudio;
  }

  private List<CorrectAndScore> getCorrectAndScoresForEx(int userID,
                                                         int projectIDFromUser,
                                                         int exid) {

    return db.getResultDAO()
        .getResultsForExIDInForUser(userID, exid, getLanguageEnum(projectIDFromUser));
  }

  /**
   * Join between exercises and scores
   * <p>
   * Be consistent with ResultDAO.getScores
   *
   * @param ids
   * @param exercises
   * @param userID
   * @param language
   * @see mitll.langtest.server.database.result.SlickResultDAO#getScores
   * @see #makeExerciseListWrapper
   * @see #getFullExercises
   */
  private Map<Integer, CorrectAndScore> getScoreHistoryPerExercise(Collection<Integer> ids,
                                                                   List<ClientExercise> exercises,
                                                                   int userID, Language language) {
    long then = System.currentTimeMillis();

    Set<Integer> contextIDs = new HashSet<>();
    exercises.forEach(exercise -> exercise.getDirectlyRelated().forEach(dir -> contextIDs.add(dir.getID())));
    contextIDs.addAll(ids);

    Map<Integer, CorrectAndScore> scoreHistories = getScoreHistories(contextIDs, exercises.isEmpty(), userID, language);

    long now = System.currentTimeMillis();
    if (now - then > 20) {
      logger.info("getScoreHistoryPerExercise took " + (now - then) + " to get score histories for " + exercises.size() +
          "\n\t exercises: " + scoreHistories.keySet());
    }

    return scoreHistories;
  }


  /**
   * TODO : don't add annotations one at a time.
   *
   * @param ids
   * @param exercises
   * @param projectID
   * @return
   * @see #getFullExercises
   */
  @NotNull
  private Set<ClientExercise> getCommonExercisesWithAnnotationsAdded(Collection<Integer> ids,
                                                                     List<ClientExercise> exercises,
                                                                     int projectID) {
    Set<ClientExercise> toAddAudioTo = new HashSet<>();

    if (DEBUG) logger.info("getCommonExercisesWithAnnotationsAdded " + ids);

    for (int exid : ids) {
      CommonExercise byID = db.getCustomOrPredefExercise(projectID, exid);
//      logger.info("ex " + byID.getID() + " eng " + byID.getEnglish() + " fl " + byID.getForeignLanguage() + " " + byID.getMeaning());
      addAnnotations(byID); // todo do this in a better way
      //if (true || byID.getAudioAttributes().isEmpty()) {
      toAddAudioTo.add(byID);
      //  logger.info("getCommonExercisesWithAnnotationsAdded exercise " + exid + " has no audio...");
      //}
      exercises.add(byID);
//      logger.info("\tgetCommonExercisesWithoutAudio " + byID.getID());
    }
    return toAddAudioTo;
  }

  /**
   * @param exids
   * @param empty
   * @param userID
   * @param language
   * @return
   * @see #getScoreHistoryPerExercise
   */
  private Map<Integer, CorrectAndScore> getScoreHistories(Collection<Integer> exids,
                                                          boolean empty,
                                                          int userID,
                                                          Language language) {
    //  boolean empty = exercises.isEmpty();
    return empty ? Collections.emptyMap() :
        db.getResultDAO().getScoreHistories(userID, exids, language);
  }

  private CommonExercise getExercise(int userID, int projectID, String exid) {
    int exid1 = -1;
    try {
      exid1 = Integer.parseInt(exid);
      return getAnnotatedExercise(userID, projectID, exid1);
    } catch (NumberFormatException e) {
      logger.warn("getExercise can't parse '" + exid + "' as an exercise id.");
      return null;
    }
  }

  /**
   * @param id
   * @param then
   * @see #getExercise
   */
  private void checkPerformance(int projid, int id, long then) {
    long diff = System.currentTimeMillis() - then;

    if (diff > MIN_DEBUG_DURATION) {
      String language = getLanguage(projid);
      String message = "getExercise : (" + language + ") took " + diff + " millis to get exercise " + id;// + " : " + threadInfo;
      if (diff > SLOW_EXERCISE_EMAIL) {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        String threadInfo = threadGroup.getName() + " = " + threadGroup.activeCount();
        logger.error(message + " thread childCount " + threadInfo);
        sendEmailWhenSlow(id, language, diff, threadInfo);
      } else if (diff > MIN_WARN_DURATION) {
        logger.warn(message);
      } else if (diff > MIN_DEBUG_DURATION) {
        logger.debug(message);
      }
    }
  }

  private void sendEmailWhenSlow(int id, String language, long diff, String threadInfo) {
    String hostName = null;
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      logger.error("Got " + e, e);
    }

    sendEmail("slow exercise on " + language, "Getting ex " + id + " on " + language + " took " + diff +
        " millis, threads " + threadInfo + " on " + hostName);
  }


  private SmallVocabDecoder getSmallVocabDecoder(int projectID) {
    return getAudioFileHelper(projectID).getSmallVocabDecoder();
  }
}