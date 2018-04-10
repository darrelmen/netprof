/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.services;

import com.google.gson.JsonObject;
import mitll.langtest.client.services.ExerciseService;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.result.ISlimResult;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.server.scoring.PrecalcScores;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ExerciseServiceImpl<T extends CommonShell> extends MyRemoteServiceServlet implements ExerciseService<T> {
  private static final Logger logger = LogManager.getLogger(ExerciseServiceImpl.class);

  private static final int SLOW_EXERCISE_EMAIL = 2000;
  private static final int SLOW_MILLIS = 50;
  private static final int WARN_DUR = 100;

  private static final int MIN_DEBUG_DURATION = 30;
  private static final int MIN_WARN_DURATION = 1000;
  private static final String LISTS = "Lists";

  private static final boolean DEBUG = false;
  private static final boolean USE_PHONE_TO_DISPLAY = true;
  private static final boolean WARN_MISSING_REF_RESULT = false;

  /**
   * @param request
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getTypeToValues
   */
  public FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException {

    if (request.isQuiz()) {
      return getQuizTypeToValues(request);
    } else {
      //List<Pair> typeToSelection = request.getTypeToSelection();
//    logger.info("getTypeToValues \n\trequest" + request);// + "\n\ttype->selection" + typeToSelection);
      ISection<CommonExercise> sectionHelper = getSectionHelper();
      if (sectionHelper == null) {
        logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
        return new FilterResponse();
      } else {
        FilterResponse typeToValues = sectionHelper.getTypeToValues(request);

        int userListID = request.getUserListID();
        UserList<CommonShell> next = userListID != -1 ? db.getUserListManager().getSimpleUserListByID(userListID) : null;

        if (next != null) {  // echo it back
          //logger.info("\tgetTypeToValues " + request + " include list " + next);
          typeToValues.getTypesToInclude().add(LISTS);
          Set<MatchInfo> value = new HashSet<>();
          value.add(new MatchInfo(next.getName(), next.getNumItems(), userListID, false, ""));
          typeToValues.getTypeToValues().put(LISTS, value);
        }
        return typeToValues;
      }
    }
  }

  public FilterResponse getQuizTypeToValues(FilterRequest request) throws DominoSessionException {
    List<Pair> typeToSelection = request.getTypeToSelection();

    logger.info("getQuizTypeToValues " +
        "\n\trequest         " + request +
        "\n\ttype->selection " + typeToSelection);

    int projectIDFromUser = getProjectIDFromUser();
    Project project = db.getProject(projectIDFromUser);


     ISection<CommonExercise> sectionHelper = db.getQuizSectionHelper(projectIDFromUser,project.getSectionHelper().getFirst());
    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      return sectionHelper.getTypeToValues(request);
    }
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
   * @seex mitll.langtest.client.list.PagingExerciseList#loadExercises
   */
  @Override
  public ExerciseListWrapper<T> getExerciseIds(ExerciseListRequest request) throws DominoSessionException {
    long then = System.currentTimeMillis();
    int projectID = getProjectIDFromUser();

    if (projectID == -1) { // not sure how this can happen now that we throw DominoSessionException
      logger.warn("getExerciseIds project id is -1?  It should probably have a real value.");
      List<T> ts = new ArrayList<>();
      return new ExerciseListWrapper<>(request.getReqID(), ts, null);
    }

    if (serverProps.isAMAS()) {
      return (ExerciseListWrapper<T>) getAMASExerciseIds(request); // TODO : how to do this without forcing it.
    }
    logger.debug("getExerciseIds : (" + getLanguage() + ") " + "getting exercise ids for request " + request);

    try {
      boolean isUserListReq = request.getUserListID() != -1;
      UserList<CommonExercise> userListByID = isUserListReq ? db.getUserListByIDExercises(request.getUserListID(), getProjectIDFromUser()) : null;

      // logger.info("found user list " + isUserListReq + " " + userListByID);
      if (request.getTypeToSelection().isEmpty()) {   // no unit-chapter filtering
        // get initial exercise set, either from a user list or predefined
        ExerciseListWrapper<T> exerciseWhenNoUnitChapter = getExerciseWhenNoUnitChapter(request, projectID, userListByID);
        logger.info("getExerciseIds : 1 req  " + request + " took " + (System.currentTimeMillis() - then) + " millis");
        return exerciseWhenNoUnitChapter;
      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        if (userListByID != null) {
          Collection<CommonExercise> exercisesForState =
              getExercisesFromUserListFiltered(request.getTypeToSelection(), userListByID);
          ExerciseListWrapper<T> exerciseListWrapperForPrefix = getExerciseListWrapperForPrefix(request, filterExercises(request, new ArrayList<>(exercisesForState), projectID), projectID);
          logger.info("getExerciseIds : 2 req  " + request + " took " + (System.currentTimeMillis() - then) + " millis");
          return exerciseListWrapperForPrefix;
        } else {
          ExerciseListWrapper<T> exercisesForSelectionState = getExercisesForSelectionState(request, projectID);
          logger.info("getExerciseIds : 3 req  " + request + " took " + (System.currentTimeMillis() - then) + " millis");
          return exercisesForSelectionState;
        }
      }
    } catch (Exception e) {
      logger.warn("got " + e, e);
      logAndNotifyServerException(e);
      return new ExerciseListWrapper<>();
    }
  }

  private ExerciseListWrapper<T> getExerciseWhenNoUnitChapter(ExerciseListRequest request,
                                                              int projectID,
                                                              UserList<CommonExercise> userListByID) {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises;
    boolean predefExercises = userListByID == null;
    if (request.isQuiz()) {
      exercises = Collections.emptyList();
    } else {
      exercises = predefExercises ? getExercises(projectID) : getCommonExercises(userListByID);
    }
    // now if there's a prefix, filter by prefix match

    TripleExercises<CommonExercise> exercisesForSearch = new TripleExercises<>().setByExercise(exercises);
    String prefix = request.getPrefix();
    if (!prefix.isEmpty()) {
      logger.info("getExerciseWhenNoUnitChapter found prefix '" + prefix + "' for user list " + userListByID);
      // now do a trie over matches
      exercisesForSearch = getExercisesForSearch(prefix, exercises, predefExercises, projectID, request.getUserID());
      if (request.getLimit() > 0) {
        exercisesForSearch.setByExercise(getFirstFew(prefix, request, exercisesForSearch.getByExercise()));
      }
    }
//    logger.info("triple resp " + exercisesForSearch);
    exercisesForSearch.setByExercise(filterExercises(request, exercisesForSearch.getByExercise(), projectID));
    //   logger.info("after triple resp " + exercisesForSearch);

    // TODO : I don't think we need this?
/*        if (!isUserListReq) {
          markRecordedState(userID, request.getActivityType(), exercises, request.isOnlyExamples());
        }*/

    // now sort : everything gets sorted the same way
    List<CommonExercise> commonExercises = getSortedExercises(request, exercisesForSearch, predefExercises, request.getUserID(), projectID);

    long now = System.currentTimeMillis();

    logger.info("getExerciseWhenNoUnitChapter took " + (now - then) + " to get " + commonExercises.size());

    ExerciseListWrapper<T> exerciseListWrapper = makeExerciseListWrapper(request, commonExercises, projectID);

//    if (request.isNoFilter()) {
//      rememberCachedWrapper(projectID, exerciseListWrapper);
//    }
    return exerciseListWrapper;
  }

  /**
   * Complicated -- put the vocab matches first, then context matches second
   *
   * @param request
   * @param exercises
   * @param predefExercises
   * @param userID
   * @return
   */
  private List<CommonExercise> getSortedExercises(ExerciseListRequest request,
                                                  TripleExercises<CommonExercise> exercises,
                                                  boolean predefExercises,
                                                  int userID,
                                                  int projID) {
    Project projectForUser = getProject(projID);

    List<CommonExercise> commonExercises = exercises.getByExercise();
    if (request.isIncorrectFirstOrder()) {
      if (DEBUG)
        logger.info("getSortedExercises adding isIncorrectFirstOrder " + exercises.getByExercise().size() + " basicExercises");
      commonExercises = db.getResultDAO().getExercisesSortedIncorrectFirst(exercises.getByExercise(), userID, getAudioFileHelper(projID).getCollator(), getLanguage(projectForUser));
    } else {
      if (predefExercises) {
        commonExercises = new ArrayList<>(exercises.getByID());
        List<CommonExercise> basicExercises = new ArrayList<>(exercises.getByExercise());
        boolean sortByFL = projectForUser.isEnglish();
        String searchTerm = request.getPrefix();
        boolean hasSearch = !searchTerm.isEmpty();

        {
          // only do this if we control the sort in the facet exercise list drop down
          //     sortByFL = isSortByFL(basicExercises, sortByFL, searchTerm);

          sortExercises(request.getActivityType() == ActivityType.RECORDER, basicExercises, sortByFL, searchTerm);
        }

        Set<Integer> unique = new HashSet<>();
        if (DEBUG) logger.info("getSortedExercises adding " + exercises.getByID().size() + " by id exercises");

        // 1) first add any exact by id matches - should only be one
        if (exercises.getByID().size() > 1)
          logger.error("expecting only 0 or 1 matches for by id " + exercises.getByID().size());

        exercises.getByID().forEach(e -> unique.add(e.getID()));
        if (DEBUG) logger.info("getSortedExercises adding " + basicExercises.size() + " basicExercises");

        basicExercises
            .stream()
            .filter(e -> !unique.contains(e.getID()))
            .forEach(commonExercises::add);

        commonExercises.forEach(e -> unique.add(e.getID()));

        // last come context matches
        {
          List<CommonExercise> contextExercises = new ArrayList<>(exercises.getByContext());
          {
            if (!contextExercises.isEmpty() && hasSearch) {
              // if the search term is in the fl, sort by fl
              sortByFL = contextExercises.iterator().next().getForeignLanguage().contains(searchTerm);
              if (DEBUG) logger.info("getSortedExercises found search term " + searchTerm + " = " + sortByFL);
            }
            sortExercises(request.getActivityType() == ActivityType.RECORDER, contextExercises, sortByFL, searchTerm);
          }
          if (DEBUG)
            logger.info("getSortedExercises adding " + contextExercises.size() + " contextExercises, " + unique.size() + " unique");

          contextExercises
              .stream()
              .filter(e -> !unique.contains(e.getID()))
              .forEach(commonExercises::add);
        }
      }
    }
    return commonExercises;
  }

  private boolean isSortByFL(List<CommonExercise> basicExercises, boolean sortByFL, String searchTerm) {
    boolean hasSearch = !searchTerm.isEmpty();
    if (!basicExercises.isEmpty() && hasSearch) {
      int max = Math.min(basicExercises.size(), 10);

      int inFL = 0;
      for (int i = 0; i < max; i++) {
        if (basicExercises.get(i).getForeignLanguage().contains(searchTerm)) inFL++;
      }

      // if the search term is in the fl, sort by fl
      sortByFL = ((float) inFL / (float) max) > 0.5;//.iterator().next().getForeignLanguage().contains(searchTerm);
      logger.info("getSortedExercises found search term " + searchTerm + " = " + sortByFL + " : " + inFL + "/" + max);
    }
    return sortByFL;
  }

  /**
   * Return shortest matches first (on fl term).
   *
   * @param request
   * @param exercises
   * @return
   */
  @NotNull
  private List<CommonExercise> getFirstFew(String prefix, ExerciseListRequest request, List<CommonExercise> exercises) {
    //logger.info("getFirstFew only taking " + request.getLimit() + " from " + exercises.size() + " that match " + prefix);

    exercises.sort((o1, o2) -> {
      String foreignLanguage = o1.getForeignLanguage();
      boolean hasSearch1 = foreignLanguage.toLowerCase().contains(prefix.toLowerCase());
      String foreignLanguage1 = o2.getForeignLanguage();
      boolean hasSearch2 = foreignLanguage1.toLowerCase().contains(prefix.toLowerCase());

      if (hasSearch1 && !hasSearch2) return -1;
      else if (hasSearch2 && !hasSearch1) return +1;
      else if (hasSearch1 && hasSearch2) {
        int i = Integer.compare(foreignLanguage.length(), foreignLanguage1.length());
        return i == 0 ? foreignLanguage.compareTo(foreignLanguage1) : i;
      } else {
        String cforeignLanguage = o1.getContext();
        String cforeignLanguage1 = o2.getContext();
        int i = Integer.compare(cforeignLanguage.length(), cforeignLanguage1.length());
        return i == 0 ? cforeignLanguage.compareTo(cforeignLanguage1) : i;
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
   */
  private List<CommonExercise> getExercises(int projectID) {
    long then = System.currentTimeMillis();
    //int projectID = getProjectIDFromUser();
    List<CommonExercise> exercises = db.getExercises(projectID);
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("getExercises took " + (now - then) + " millis to get the raw exercise list for " + projectID);// getLanguage(projectID));
    }

    return exercises;
  }

//  private Collator getCollator() {
//    return getAudioFileHelper().getCollator();
//  }

  /**
   * Marks each exercise - first state - with whether this user has recorded audio for this item
   * Defective audio is not included.
   * Also if just one of regular or slow is recorded it's not "recorded".
   * <p>
   * What you want to see in the record audio tab.  One bit of info - recorded or not recorded.
   *
   * @param userID
   * @param activityType
   * @param exercises
   * @param onlyExample
   * @return
   * @see #getExerciseIds
   */
  private int markRecordedState(int userID,
                                ActivityType activityType,
                                Collection<? extends Shell> exercises,
                                boolean onlyExample) {
    int c = 0;
    if (activityType == ActivityType.RECORDER) {
      IAudioDAO audioDAO = db.getAudioDAO();
      Collection<Integer> recordedForUser = onlyExample ?
          audioDAO.getRecordedExampleForUser(userID) :
          audioDAO.getRecordedExForUser(userID);
      logger.info("\tmarkRecordedState found " + recordedForUser.size() + " recordings by " + userID + " only example " + onlyExample);
      for (Shell shell : exercises) {
        if (recordedForUser.contains(shell.getID())) {
          shell.setState(STATE.RECORDED);
          c++;
        }
      }
    }
    //else {
    //logger.debug("\tnot marking recorded for '" + role + "' and user " + userID);
    //}
    if (!activityType.equals(ActivityType.UNSET)) {
      logger.info("\tmarkRecordedState marked " + c + "  recorded for activity '" + activityType + "' and user " + userID + " on " + exercises.size());
    }
    return c;
  }

  /**
   * Copies the exercises....?
   *
   * @param userListByID
   * @return
   * @see #getExerciseIds
   * @see #getExercisesFromUserListFiltered(java.util.Map, mitll.langtest.shared.custom.UserList)
   */
  private <T extends CommonShell> List<T> getCommonExercises(UserList<T> userListByID) {
    return new ArrayList<>(userListByID.getExercises());
  }

  /**
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises
   * @see #getExerciseIds
   */
  private <T extends CommonShell> ExerciseListWrapper<T> getExercisesForSelectionState(ExerciseListRequest request, int projid) {
    ISection<CommonExercise> sectionHelper = getSectionHelper(projid);
    if (request.isQuiz()) {
      sectionHelper = db.getQuizSectionHelper(projid, getProject(projid).getSectionHelper().getFirst());
    }
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(request.getTypeToSelection());

    List<CommonExercise> copy = new ArrayList<>(exercisesForState);  // TODO : avoidable???

    if (request.isQuiz()) {
      copy.sort(Comparator.comparingInt(CommonShell::getNumPhones));
    }

    exercisesForState = filterExercises(request, copy, projid);
    return getExerciseListWrapperForPrefix(request, exercisesForState, projid);
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
  private <T extends CommonShell> ExerciseListWrapper<T> getExerciseListWrapperForPrefix(ExerciseListRequest request,
                                                                                         Collection<CommonExercise> exercisesForState,
                                                                                         int projID) {
    String prefix = request.getPrefix();
    int userID = request.getUserID();
    boolean incorrectFirst = request.isIncorrectFirstOrder();

    boolean hasPrefix = !prefix.isEmpty();
    if (hasPrefix) {
      logger.debug("getExerciseListWrapperForPrefix" +
          "\n\tuserID   " + userID +
          "\n\tprefix   '" + prefix + "'" +
          (request.getActivityType() != ActivityType.UNSET ? "" : "\n\tactivity " + request.getActivityType()));
    }

    int i = markRecordedState(userID, request.getActivityType(), exercisesForState, request.isOnlyExamples());
    logger.debug("getExerciseListWrapperForPrefix marked " + i + " as recorded");

    if (hasPrefix) {
      //logger.info("check for prefix match over " + exercisesForState.size());
      exercisesForState = getSearchMatches(exercisesForState, prefix, projID);
    }

    if (exercisesForState.isEmpty() && !prefix.isEmpty()) { // allow lookup by id
      CommonExercise exercise = getExercise(userID, projID, prefix, incorrectFirst);
      if (exercise != null) exercisesForState = Collections.singletonList(exercise);
    }
    // why copy???
    List<CommonExercise> copy;

    if (incorrectFirst) {
      copy = db.getResultDAO().getExercisesSortedIncorrectFirst(exercisesForState, userID, getAudioFileHelper(projID).getCollator(), getLanguage(projID));
    } else {
      copy = new ArrayList<>(exercisesForState);
      sortExercises(request.getActivityType() == ActivityType.RECORDER, copy, false, request.getPrefix());
    }

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
  private Collection<CommonExercise> getSearchMatches(Collection<CommonExercise> exercisesForState, String prefix, int projID) {
    Collection<CommonExercise> originalSet = exercisesForState;
    // logger.info("original set" +originalSet.size());
    long then = System.currentTimeMillis();
    String language = getLanguage(projID);
    SmallVocabDecoder smallVocabDecoder = getSmallVocabDecoder(projID);
    ExerciseTrie<CommonExercise> trie =
        new ExerciseTrie<>(exercisesForState, language, smallVocabDecoder, true);
    long now = System.currentTimeMillis();
    if (now - then > 20)
      logger.info("took " + (now - then) + " millis to build trie for " + exercisesForState.size() + " exercises");
    exercisesForState = trie.getExercises(prefix);

    if (exercisesForState.isEmpty()) {
      String prefix1 = StringUtils.stripAccents(prefix);
      exercisesForState = trie.getExercises(prefix1);
      logger.info("getSearchMatches trying " + prefix1 + " instead of " + prefix + " found " + exercisesForState.size());
    }

    Set<Integer> unique = new HashSet<>();
    exercisesForState.forEach(e -> unique.add(e.getID()));

    {
      then = System.currentTimeMillis();
      trie = new ExerciseTrie<>(originalSet, language, smallVocabDecoder, false);
      now = System.currentTimeMillis();
      if (now - then > 20) {
        logger.info("took " + (now - then) + " millis to build trie for " + originalSet.size() + " context exercises");
      }

      List<CommonExercise> contextExercises = trie.getExercises(prefix);
      if (contextExercises.isEmpty()) {
        contextExercises = trie.getExercises(StringUtils.stripAccents(prefix));
        logger.info("getSearchMatches context trying " + StringUtils.stripAccents(prefix) + " instead of " + prefix + " found " + contextExercises.size());
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
  private <T extends CommonShell> ExerciseListWrapper<T> makeExerciseListWrapper(ExerciseListRequest request,
                                                                                 Collection<CommonExercise> exercises,
                                                                                 int projID) {
    long then = System.currentTimeMillis();
    CommonExercise firstExercise = exercises.isEmpty() ? null : request.isAddFirst() ? exercises.iterator().next() : null;

    int userID = request.getUserID();

    if (firstExercise != null) {
      addAnnotationsAndAudio(userID, firstExercise, request.isIncorrectFirstOrder(), request.isQC(), projID);
      // NOTE : not ensuring MP3s or OGG versions of WAV file.
      // ensureMP3s(firstExercise, pathHelper.getInstallPath());
      if (request.shouldAddContext()) { // add the context exercises
        //  logger.info("adding context exercises...");
        List<CommonExercise> withContext = new ArrayList<>();
        exercises.forEach(commonExercise -> {
          withContext.add(commonExercise);
          //  logger.info("\t" + commonExercise.getID() + " " + commonExercise.getDirectlyRelated().size());
          withContext.addAll(commonExercise.getDirectlyRelated());
        });
        exercises = withContext;
      }
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);

//    logger.debug("makeExerciseListWrapper : userID " + userID + " Role is " + request.getActivityType());
    markStateForActivity(request.isOnlyExamples(), userID, exerciseShells, request.getActivityType());

    // TODO : do this the right way vis-a-vis type safe collection...

    List<T> exerciseShells1 = (List<T>) exerciseShells;
    if (exerciseShells1.isEmpty() && firstExercise != null) {
      logger.error("makeExerciseListWrapper huh? no exercises");
    }

    ExerciseListWrapper<T> exerciseListWrapper = new ExerciseListWrapper<>(request.getReqID(), exerciseShells1, firstExercise);

    if (!request.isQC()) {
      // should be consistent with getScoreHistoryPerExercise
      // @see #getScoreHistoryPerExercise
      Map<Integer, Float> scores = db.getResultDAO().getScores(userID, exerciseShells1);

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
   * @param onlyExamples
   * @param userID
   * @param exerciseShells
   * @param activityType
   * @see #makeExerciseListWrapper(ExerciseListRequest, Collection, int)
   */
  private void markStateForActivity(boolean onlyExamples, int userID, List<CommonShell> exerciseShells, ActivityType activityType) {
    switch (activityType) {
      case RECORDER:
        markRecordedState(userID, activityType, exerciseShells, onlyExamples);
        break;
      case REVIEW:
      case QUALITY_CONTROL:
      case MARK_DEFECTS:
        db.getStateManager().markState(exerciseShells);
        break;
      default:
        break;
    }
  }


  /**
   * 0) Add annotations to fields on exercise
   * 1) Attach audio recordings to exercise.
   * 2) Adds information about whether the audio has been played or not...
   * 3) Attach history info (when has the user recorded audio for the item under the learn tab and gotten a score)
   *
   * @param userID
   * @param firstExercise
   * @param isFlashcardReq if true, filter for only recordings made during avp
   * @param isQC
   * @param projID
   * @seex LoadTesting#getExercise(String, long, boolean)
   * @see #makeExerciseListWrapper
   */
  private void addAnnotationsAndAudio(int userID, CommonExercise firstExercise, boolean isFlashcardReq, boolean isQC, int projID) {
    long then = System.currentTimeMillis();

    logger.info("adding anno to " + firstExercise.getID() + " with " + firstExercise.getDirectlyRelated().size() + " context exercises");
    addAnnotations(firstExercise); // todo do this in a better way

    long now = System.currentTimeMillis();
    int oldID = firstExercise.getID();
    String language = getLanguage(projID);

    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + language + ") took " + (now - then) + " millis to add annotations to exercise " + oldID);
    }
    then = now;
    int i = attachAudio(firstExercise);
    //logger.info("attached " + i + " audio cuts to " + firstExercise.getID());

    if (DEBUG) {
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

    db.getResultDAO().attachScoreHistory(userID, firstExercise, isFlashcardReq, getLanguage(firstExercise));

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
   * @param byID
   * @see #addAnnotationsAndAudio(int, CommonExercise, boolean, boolean, int)
   */
  private void addAnnotations(CommonExercise byID) {
    IUserListManager userListManager = db.getUserListManager();
    userListManager.addAnnotations(byID);
    byID.getDirectlyRelated().forEach(userListManager::addAnnotations);
  }

  /**
   * @param firstExercise
   * @see #addAnnotationsAndAudio(int, CommonExercise, boolean, boolean, int)
   */
  private int attachAudio(CommonExercise firstExercise) {
    return db.getAudioDAO().attachAudioToExercise(firstExercise, getLanguage(firstExercise), new HashMap<>());
  }

  private String getLanguage(CommonExercise firstExercise) {
    return db.getLanguage(firstExercise);
  }

  /**
   * Only add the played markings if doing QC.
   * <p>
   * TODO : This is an expensive query - we need a smarter way of remembering when audio has been played.
   *
   * @param userID
   * @param firstExercise
   * @see #addAnnotationsAndAudio(int, CommonExercise, boolean, boolean, int)
   */
  private void addPlayedMarkings(int userID, CommonExercise firstExercise) {
    db.getEventDAO().addPlayedMarkings(userID, firstExercise);
  }

  /**
   * @param request
   * @param exercises
   * @param projid
   * @return
   * @see #getExerciseIds
   */
  private List<CommonExercise> filterExercises(ExerciseListRequest request,
                                               List<CommonExercise> exercises,
                                               int projid) {
    exercises = filterByUnrecorded(request, exercises, projid);

    if (request.isOnlyWithAudioAnno()) {
      exercises = filterByOnlyAudioAnno(request.isOnlyWithAudioAnno(), exercises);
    }
    if (request.isOnlyDefaultAudio()) {
      exercises = filterByOnlyDefaultAudio(request.isOnlyDefaultAudio(), exercises);
    }
    if (request.isOnlyUninspected()) {
      exercises = filterByUninspected(exercises);
    }
    if (request.isOnlyForUser()) {
      exercises = filterOnlyPracticedByUser(request, exercises, projid);
    }
    return exercises;
  }

  @NotNull
  private List<CommonExercise> filterOnlyPracticedByUser(ExerciseListRequest request, List<CommonExercise> exercises, int projid) {
    Collection<Integer> practicedByUser = db.getResultDAO().getPracticedByUser(request.getUserID(), projid);
    List<CommonExercise> copy = new ArrayList<>();

    // TODO:  gosh - wasteful most of the time....
    long then = System.currentTimeMillis();
    for (CommonExercise ex : exercises) {
      if (practicedByUser.contains(ex.getID())) copy.add(ex);
    }
    long now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.info("filterExercises : took " + (now - then));
    }
    return copy;
  }

  /**
   * TODO : slow?
   *
   * @param <T>
   * @param commonExercises
   * @param sortByFL
   * @param searchTerm
   * @paramx role
   * @see #getExerciseListWrapperForPrefix(ExerciseListRequest, Collection, int)
   * @see #getSortedExercises
   */
  private <T extends CommonShell> void sortExercises(boolean isRecorder, List<T> commonExercises, boolean sortByFL, String searchTerm) {
    new ExerciseSorter().getSorted(commonExercises, isRecorder, sortByFL, searchTerm);
  }

  /**
   * TODO : revisit the parameterized types here.
   *
   * @param prefix
   * @param exercises
   * @param predefExercises
   * @param <T>
   * @return
   * @see #getExerciseIds
   */
  private <T extends CommonExercise> TripleExercises<T> getExercisesForSearch(String prefix,
                                                                              Collection<T> exercises,
                                                                              boolean predefExercises,
                                                                              int projectID,
                                                                              int userID) {
    Search<T> search = new Search<T>(db, db);
    TripleExercises<T> exercisesForSearch = search.getExercisesForSearch(prefix, exercises, predefExercises, projectID);
    exercisesForSearch.setByID(Collections.emptyList());

    {
      int exid = search.getExid(prefix);
      if (exid != -1 && exid != 1) {
        T exercise = getAnnotatedExercise(userID, projectID, exid, false);
        if (exercise != null && exercise.getProjectID() == projectID) {
          exercisesForSearch.setByID(Collections.singletonList(exercise));
        } else {
          logger.info("getExercisesForSearch no exercise with " + projectID + " " + exid);
        }
      }
    }
    return exercisesForSearch;
  }

  /**
   * For all the exercises the user has not recorded, do they have the required reg and slow speed recordings by a matching gender.
   * <p>
   * Or if looking for example audio, find ones missing examples.
   *
   * @param exercises to filter
   * @param projid
   * @return exercises missing audio, what we want to record
   * @paramx userID                   exercise not recorded by this user and matching the user's gender
   * @paramx onlyUnrecordedByMyGender do we filter by gender
   * @paramx onlyExamples             only example audio
   * @see #getExerciseIds
   * @see #getExercisesForSelectionState
   * @see #filterExercises
   */
  private List<CommonExercise> filterByUnrecorded(
      ExerciseListRequest request,
      List<CommonExercise> exercises,
      int projid) {
    if (request.isOnlyUnrecordedByMe()) {
      logger.info("Filter for matching gender to " + request.getUserID());
      return getUnrecordedExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples());
    } else {
      return request.isOnlyExamples() ? getExercisesWithContext(exercises) : exercises;
    }
  }

  /**
   * TODO : way too much work here... why go through all exercises?
   * TODO : why return all exercises?
   *
   * @param userID
   * @param exercises
   * @param projid
   * @param onlyExamples
   * @return
   */
  @NotNull
  private List<CommonExercise> getUnrecordedExercisesMatchingGender(int userID,
                                                                    Collection<CommonExercise> exercises,
                                                                    int projid,
                                                                    boolean onlyExamples) {

    Set<Integer> unrecordedIDs = new HashSet<>(exercises.size());

    Map<Integer, String> exToTranscript = new HashMap<>();

    for (CommonExercise exercise : exercises) {
      if (onlyExamples) {
        exercise.getDirectlyRelated().forEach(dir -> {
          int id = dir.getID();
          unrecordedIDs.add(id);
          exToTranscript.put(id, dir.getForeignLanguage());
        });
      } else {
        int id = exercise.getID();
        unrecordedIDs.add(id);
        exToTranscript.put(id, exercise.getForeignLanguage());
      }
    }

    logger.debug("getUnrecordedExercisesMatchingGender " + onlyExamples +
        "\n\texToTranscript " + exToTranscript.size());
    Collection<Integer> recordedBySameGender = getRecordedByMatchingGender(userID, projid, onlyExamples, exToTranscript);
    logger.debug("getUnrecordedExercisesMatchingGender" +
        "\n\tall exercises " + unrecordedIDs.size() +
        "\n\tfor project #" + projid +
        "\n\tuserid # " + userID +
        "\n\tremoving " + recordedBySameGender.size());
    unrecordedIDs.removeAll(recordedBySameGender);
    logger.debug("getUnrecordedExercisesMatchingGender after removing recorded exercises " + unrecordedIDs.size());

    List<CommonExercise> unrecordedExercises = new ArrayList<>();

    for (CommonExercise exercise : exercises) {
      if (onlyExamples) {
        for (CommonExercise dir : exercise.getDirectlyRelated()) {
          if (unrecordedIDs.contains(dir.getID())) {
            unrecordedExercises.add(dir);
          }
        }
      } else {
        if (unrecordedIDs.contains(exercise.getID())) {
          unrecordedExercises.add(exercise);
        }
      }
    }

    logger.debug("getUnrecordedExercisesMatchingGender to be recorded " + unrecordedExercises.size() + " from " + exercises.size());

    return unrecordedExercises;
  }

  /**
   * @param userID
   * @param projid
   * @param onlyExamples
   * @return
   * @see #getUnrecordedExercisesMatchingGender
   */
  private Collection<Integer> getRecordedByMatchingGender(int userID,
                                                          int projid,
                                                          boolean onlyExamples,
                                                          Map<Integer, String> exToTranscript) {
    logger.debug("getRecordedByMatchingGender : for " + userID +
        " only by same gender examples only " + onlyExamples);// + " from " + exercises.size());

    return onlyExamples ?
        db.getAudioDAO().getRecordedBySameGenderContext(userID, projid, exToTranscript) :
        db.getAudioDAO().getRecordedBySameGender(userID, projid, exToTranscript);
  }

  /**
   * @param exercises
   * @return
   * @see #filterByUnrecorded
   */
  @NotNull
  private List<CommonExercise> getExercisesWithContext(Collection<CommonExercise> exercises) {
    List<CommonExercise> copy = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    for (CommonExercise exercise : exercises) {
      if (seen.contains(exercise.getID())) logger.warn("saw " + exercise.getID() + " " + exercise + " again!");
      if (hasContext(exercise)) {
        seen.add(exercise.getID());
        //    copy.add(exercise);
        copy.addAll(exercise.getDirectlyRelated());
      }
    }
    //   logger.debug("ONLY EXAMPLES - to be recorded " + copy.size() + " from " + exercises.size());

    return copy;
  }

  private <X extends CommonExercise> boolean hasContext(X exercise) {
    return !exercise.getDirectlyRelated().isEmpty();//.getContext() != null && !exercise.getContext().isEmpty();
  }

  /**
   * @param onlyAudioAnno
   * @param exercises
   * @return
   * @see #getExerciseIds
   */
  private List<CommonExercise> filterByOnlyAudioAnno(boolean onlyAudioAnno,
                                                     List<CommonExercise> exercises) {
    if (onlyAudioAnno) {
      Collection<Integer> audioAnnos = getUserListManager().getAudioAnnos();
      List<CommonExercise> copy = new ArrayList<>();
      for (CommonExercise exercise : exercises) {
        if (audioAnnos.contains(exercise.getID())) copy.add(exercise);
      }
      return copy;
    } else {
      return exercises;
    }
  }

  private List<CommonExercise> filterByOnlyDefaultAudio(boolean onlyDefault,
                                                        List<CommonExercise> exercises) {
    if (onlyDefault) {
      List<CommonExercise> copy = new ArrayList<>();
      for (CommonExercise exercise : exercises) {
        for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
          if (audioAttribute.getUserid() == BaseUserDAO.DEFAULT_USER_ID) {
            copy.add(exercise);
            break;
          }
        }
      }
      return copy;
    } else {
      return exercises;
    }
  }

  /**
   * Remove any items that have been inspected already.
   *
   * @param exercises
   * @return
   */
  private List<CommonExercise> filterByUninspected(Collection<CommonExercise> exercises) {
    Collection<Integer> inspected = db.getStateManager().getInspectedExercises();
    // logger.info("found " + inspected.size());
    List<CommonExercise> copy = new ArrayList<>();
    for (CommonExercise exercise : exercises) {
      if (!inspected.contains(exercise.getID())) {
        copy.add(exercise);
      }
    }
    return copy;
  }

  /**
   * On the fly we make a new section helper to do filtering of user list.
   * <p>
   * TODO : include lists as just another facet for the purpose of filtering
   * If the filter and then the list results in no exercises, unselect the list.
   *
   * @param typeToSelection
   * @param userListByID
   * @param <T>
   * @return
   */
  private <T extends CommonShell & HasUnitChapter>
  Collection<T> getExercisesFromUserListFiltered(Map<String, Collection<String>> typeToSelection,
                                                 UserList<T> userListByID) {
    Collection<T> exercises2 = getCommonExercises(userListByID);
    typeToSelection.remove(LISTS);
    if (typeToSelection.isEmpty()) {
      logger.info("getExercisesFromUserListFiltered returning  " + userListByID.getExercises().size() + " exercises for " + userListByID.getID());
      return exercises2;
    } else {
      SectionHelper<T> helper = new SectionHelper<>();

      logger.info("getExercisesFromUserListFiltered found " + exercises2.size() + " for list " + userListByID);
      long then = System.currentTimeMillis();
      exercises2.forEach(helper::addExercise);
      long now = System.currentTimeMillis();

      if (now - then > 100) {
        logger.debug("getExercisesFromUserListFiltered used " + exercises2.size() + " exercises to build a hierarchy in " + (now - then) + " millis");
      }
      //helper.report();
      Collection<T> exercisesForState = helper.getExercisesForSelectionState(typeToSelection);
      logger.debug("\tgetExercisesFromUserListFiltered after found " + exercisesForState.size() + " matches to " + typeToSelection);
      return /*typeToSelection.isEmpty() ? exercises2 :*/ exercisesForState;
    }
  }

  int warn = 0;

  /**
   * Save transmission bandwidth - don't send a list of fully populated items - just send enough to populate a list
   *
   * @param exercises
   * @return
   * @see #makeExerciseListWrapper
   */
  private <T extends CommonShell> List<CommonShell> getExerciseShells(Collection<T> exercises) {
    List<CommonShell> ids = new ArrayList<>(exercises.size());


    exercises.forEach(ex -> {
      if (ex.getNumPhones() == 0 && warn++ < 100) {
        logger.warn("getExerciseShells : no phones for exercise " + ex.getID());
      }
      CommonShell shell = ex.getShell();
      // if (shell.getNumPhones() == 0) logger.warn("no phones for shell " + ex.getID());
      ids.add(shell);
    });
    return ids;
  }

  /**
   * @param request
   * @return
   * @deprecated
   */
  private ExerciseListWrapper<AmasExerciseImpl> getAMASExerciseIds(
      ExerciseListRequest request
  ) {
    Collection<AmasExerciseImpl> exercises;
    int reqID = request.getReqID();
    Map<String, Collection<String>> typeToSelection = request.getTypeToSelection();

    try {
      if (typeToSelection.isEmpty()) {   // no unit-chapter filtering
        // get initial exercise set, either from a user list or predefined
        exercises = getAMASExercises();

        // now if there's a prefix, filter by prefix match

        // TODO : put this back if needed?

        /*       if (!request.getPrefix().isEmpty()) {
          // now do a trie over matches
          exercises = getExercisesForSearchWithTrie(request.getPrefix(), exercises, true, amasFullTrie);
        }*/
        AmasSupport amasSupport = new AmasSupport();
        exercises = amasSupport.filterByUnrecorded(request.getUserID(), exercises, typeToSelection, db.getResultDAO());
        // exercises = filterByOnlyAudioAnno(onlyWithAudioAnno, exercises);
        //    int i = markRecordedState(userID, role, exercises, onlyExamples);
        //  logger.debug("marked " +i + " as recorded");

        // now sort : everything gets sorted the same way
        //    List<AmasExerciseImpl> commonExercises;
//        if (incorrectFirstOrder) {
//          commonExercises = db.getResultDAO().getExercisesSortedIncorrectFirst(exercises, userID, audioFileHelper.getCollator());
//        } else {
        //    commonExercises = new ArrayList<AmasExerciseImpl>(exercises);
        //   sortExercises("", commonExercises);
//        }

        return new ExerciseListWrapper<>(reqID, new ArrayList<>(exercises), null);
      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        Collection<AmasExerciseImpl> exercisesForSelectionState1 =
            new AmasSupport().getExercisesForSelectionState(typeToSelection, request.getPrefix(), request.getUserID(),
                db.getAMASSectionHelper(), db.getResultDAO());
        return new ExerciseListWrapper<>(reqID, new ArrayList<>(exercisesForSelectionState1), null);
      }
    } catch (Exception e) {
      logger.warn("got " + e, e);
      logAndNotifyServerException(e);
      return new ExerciseListWrapper<>();
    }
  }

  /**
   * NOTE NOTE NOTE : not doing ensureMP3 - since we likely don't have access to file system for here.
   * Joins with annotation data when doing QC.
   *
   * @param exid
   * @param isFlashcardReq
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   * @see mitll.langtest.client.list.ExerciseList#goGetNextAndCacheIt
   * @see mitll.langtest.client.analysis.PlayAudio#playLast
   */
  public <T extends Shell> T getExercise(int exid, boolean isFlashcardReq) throws DominoSessionException {
    if (serverProps.isAMAS()) { // TODO : HOW TO AVOID CAST???
      return (T) db.getAMASExercise(exid);
    }
    return getAnnotatedExercise(getUserIDFromSessionOrDB(), getProjectIDFromUser(), exid, isFlashcardReq);
  }

  /**
   * @param userID
   * @param projectID
   * @param exid
   * @param isFlashcardReq
   * @param <T>
   * @return
   * @see #getExercise(int, boolean)
   */
  @Nullable
  private <T extends Shell> T getAnnotatedExercise(int userID, int projectID, int exid, boolean isFlashcardReq) {
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
    addAnnotationsAndAudio(userID, byID, isFlashcardReq, false, projectID);
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
    return (T) byID;
  }

  /**
   * @param reqid
   * @param ids
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getExercises
   */
  @Override
  public ExerciseListWrapper<CommonExercise> getFullExercises(int reqid, Collection<Integer> ids) throws DominoSessionException {
    List<CommonExercise> exercises = new ArrayList<>();

    int userID = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser(userID);
    if (projectID == -1) {
      logger.info("getFullExercises : no project for user " + userID);
      return new ExerciseListWrapper<>();
    }
    String language = getLanguage(projectID);

    long then = System.currentTimeMillis();
    Set<CommonExercise> toAddAudioTo = getCommonExercisesWithoutAudio(ids, exercises, projectID);
    long now = System.currentTimeMillis();

    if (now - then > 10)
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
    db.getAudioDAO().attachAudioToExercises(toAddAudioTo, language);
    now = System.currentTimeMillis();

    if (now - then > 10)
      logger.info("getFullExercises took " + (now - then) + " to attach audio to " + toAddAudioTo.size() + " exercises");

    then = System.currentTimeMillis();
    addAlignmentOutput(projectID, toAddAudioTo);
    now = System.currentTimeMillis();
    if (now - then > 20)
      logger.info("getFullExercises took " + (now - then) + " to attach alignment output to " + toAddAudioTo.size() + " exercises");

//    } else {
//      // logger.info("getFullExercises all " + ids.size() + " exercises have audio");
//    }

    then = System.currentTimeMillis();

    // I don't think we even have to do this...
    //getScores(userID, exercises);
//    Map<Integer, Float> scores = db.getResultDAO().getScores(userID, exercises);

    now = System.currentTimeMillis();
    if (now - then > 50)
      logger.info("getFullExercises took " + (now - then) + " to add scores to " + exercises.size() + " exercises");

    Map<Integer, CorrectAndScore> scoreHistoryPerExercise = getScoreHistoryPerExercise(ids, exercises, userID);
    logger.info("getFullExercises found " + exercises.size() + " exercises and " + scoreHistoryPerExercise.size() + " scores");

/*
    List<Integer> sorted = new ArrayList<>(scoreHistoryPerExercise.keySet());
    Collections.sort(sorted);
    sorted.forEach(exid -> logger.info("correct & score : exids #" + exid+" -> " + scoreHistoryPerExercise.get(exid)));

*/

    // for (CommonExercise exercise : exercises) logger.info("\treturning " + exercise.getID());
    return new ExerciseListWrapper<>(reqid, exercises, null, scoreHistoryPerExercise);
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

    CorrectAndScore closest = null;
    long closestTimeDiff = Long.MAX_VALUE;
    List<CorrectAndScore> correctAndScoresForEx = getCorrectAndScoresForEx(userID, projectIDFromUser, exid);

    logger.info("getLatestScoreAudioPath user " + userID + " project " + projectIDFromUser +
        " at " + new Date(nearTime) + " found " + correctAndScoresForEx.size());

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

    String refAudio = getRefAudio(userID, projectIDFromUser, exid);
    Pair pair = new Pair(
        refAudio,
        closest == null ? null : closest.getPath());
    logger.info("getLatestScoreAudioPath returning " + pair);
    return pair;
  }

  @Nullable
  private String getRefAudio(int userID, int projectIDFromUser, int exid) {
    CommonExercise byID = db.getCustomOrPredefExercise(projectIDFromUser, exid);
    String refAudio = null;
    if (byID == null) {
      logger.warn("getRefAudio can't find ex id " + exid);
    } else {
      db.getAudioDAO().attachAudioToExercises(Collections.singleton(byID), getLanguage(projectIDFromUser));
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
        .getResultsForExIDInForUser(userID, exid, false, getLanguage(projectIDFromUser));
  }

  /**
   * Join between exercises and scores
   * <p>
   * Be consistent with ResultDAO.getScores
   *
   * @param ids
   * @param exercises
   * @param userID
   * @see mitll.langtest.server.database.result.SlickResultDAO#getScores
   * @see #makeExerciseListWrapper
   * @see #getFullExercises
   */
  private Map<Integer, CorrectAndScore> getScoreHistoryPerExercise(Collection<Integer> ids,
                                                                   List<CommonExercise> exercises,
                                                                   int userID) {
    long then = System.currentTimeMillis();
    Map<Integer, CorrectAndScore> scoreHistories = getScoreHistories(ids, exercises, userID);
    long now = System.currentTimeMillis();
    if (now - then > 50)
      logger.info("getFullExercises took " + (now - then) + " to get score histories for " + exercises.size() + " exercises");

    return scoreHistories;
  }

  /**
   * @param projectID
   * @param toAddAudioTo
   * @see #getFullExercises
   */
  private void addAlignmentOutput(int projectID, Set<CommonExercise> toAddAudioTo) {
    Project project = db.getProject(projectID);

    if (project != null) {
      Map<Integer, AlignmentOutput> audioToAlignment = project.getAudioToAlignment();
      Map<Integer, AudioAttribute> idToAudio = new HashMap<>();

      logger.info("addAlignmentOutput : For project " + projectID + " found " + audioToAlignment.size() +
          " audio->alignment entries, trying to marry to " + toAddAudioTo.size() + " exercises");

      for (CommonExercise exercise : toAddAudioTo) {
        setAlignmentInfo(audioToAlignment, idToAudio, exercise);
        exercise.getDirectlyRelated().forEach(context -> setAlignmentInfo(audioToAlignment, idToAudio, context));
      }

      Map<Integer, AlignmentOutput> alignments = rememberAlignments(projectID, idToAudio, project.getLanguage());

      synchronized (audioToAlignment) {
        audioToAlignment.putAll(alignments);
      }
    }
  }

  /**
   * TODO : why not concurrent hash map...
   *
   * @param audioToAlignment
   * @param idToAudio
   * @param exercise
   */
  private void setAlignmentInfo(Map<Integer, AlignmentOutput> audioToAlignment,
                                Map<Integer, AudioAttribute> idToAudio,
                                CommonExercise exercise) {
    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      AlignmentOutput currentAlignment = audioAttribute.getAlignmentOutput();
      if (currentAlignment == null) {
        synchronized (audioToAlignment) {
          AlignmentOutput alignmentOutput1 = audioToAlignment.get(audioAttribute.getUniqueID());

          if (alignmentOutput1 == null) {
            idToAudio.put(audioAttribute.getUniqueID(), audioAttribute);
          } else {  // not sure how this can happen
            audioAttribute.setAlignmentOutput(alignmentOutput1);
          }
        }
      }
    }
  }

  /**
   * @param projectID
   * @param idToAudio
   * @param language
   * @return
   * @see #addAlignmentOutput
   */
  private Map<Integer, AlignmentOutput> rememberAlignments(int projectID,
                                                           Map<Integer, AudioAttribute> idToAudio, String language) {
    if (!idToAudio.isEmpty() && idToAudio.size() > 50)
      logger.info("rememberAlignments : asking for " + idToAudio.size() + " alignment outputs from database");

    Map<Integer, AlignmentOutput> alignments = getAlignmentsFromDB(projectID, idToAudio.keySet(), language);
    addAlignmentToAudioAttribute(idToAudio, alignments);
    return alignments;
  }


  private void addAlignmentToAudioAttribute(Map<Integer, AudioAttribute> idToAudio,
                                            Map<Integer, AlignmentOutput> alignments) {
    idToAudio.forEach((k, v) -> {
      AlignmentOutput alignmentOutput = alignments.get(k);
      if (alignmentOutput == null) {
        // logger.warn("addAlignmentToAudioAttribute : couldn't get alignment for audio #" + v.getUniqueID());
      } else {
        v.setAlignmentOutput(alignmentOutput);
      }
    });
  }

  /**
   * get alignment from db
   *
   * @param projid
   * @param audioIDs
   * @param language
   * @return
   * @see #rememberAlignments
   */
  private Map<Integer, AlignmentOutput> getAlignmentsFromDB(int projid, Set<Integer> audioIDs, String language) {
    //logger.info("getAlignmentsFromDB asking for " + audioIDs.size());
    if (audioIDs.isEmpty()) logger.warn("getAlignmentsFromDB not asking for any audio ids?");
    Map<Integer, ISlimResult> audioIDMap = getAudioIDMap(db.getRefResultDAO().getAllSlimForProjectIn(projid, audioIDs));
    logger.info("getAlignmentsFromDB found " + audioIDs.size() + "/" + audioIDMap.size() + " ref result alignments...");
    return parseJsonToGetAlignments(audioIDs, audioIDMap, language);
  }

  @NotNull
  private Map<Integer, ISlimResult> getAudioIDMap(Collection<ISlimResult> jsonResultsForProject) {
    Map<Integer, ISlimResult> audioToResult = new HashMap<>();
    jsonResultsForProject.forEach(slimResult -> audioToResult.put(slimResult.getAudioID(), slimResult));
    return audioToResult;
  }

  private Map<Integer, AlignmentOutput> parseJsonToGetAlignments(Collection<Integer> audioIDs,
                                                                 Map<Integer, ISlimResult> audioToResult,
                                                                 String language) {
    Map<Integer, AlignmentOutput> idToAlignment = new HashMap<>();
    for (Integer audioID : audioIDs) {
      // do we have alignment for this audio in the map
      ISlimResult cachedResult = audioToResult.get(audioID);

      if (cachedResult == null) { // nope, ask the database
        cachedResult = db.getRefResultDAO().getResult(audioID);
      }

      if (cachedResult == null || !cachedResult.isValid()) { // not in the database, recalculate it now?
        if (WARN_MISSING_REF_RESULT) logger.info("parseJsonToGetAlignments : nothing in database for audio " + audioID);
      } else {
        getCachedAudioRef(idToAlignment, audioID, cachedResult, language);  // OK, let's translate the db info into the alignment output
      }
    }
    return idToAlignment;
  }

  private void getCachedAudioRef(Map<Integer, AlignmentOutput> idToAlignment, Integer audioID, ISlimResult cachedResult, String language) {
    PrecalcScores precalcScores = getPrecalcScores(USE_PHONE_TO_DISPLAY, cachedResult, language);
    Map<ImageType, Map<Float, TranscriptEvent>> typeToTranscriptEvents =
        getTypeToTranscriptEvents(precalcScores.getJsonObject(), USE_PHONE_TO_DISPLAY, language);
    Map<NetPronImageType, List<TranscriptSegment>> typeToSegments = getTypeToSegments(typeToTranscriptEvents, language);
//    logger.info("getCachedAudioRef : cache HIT for " + audioID + " returning " + typeToSegments);
    idToAlignment.put(audioID, new AlignmentOutput(typeToSegments));
  }

  @NotNull
  private PrecalcScores getPrecalcScores(boolean usePhoneToDisplay, ISlimResult cachedResult, String language) {
    return new PrecalcScores(serverProps, cachedResult,
        usePhoneToDisplay || serverProps.usePhoneToDisplay(), language);
  }

  private Map<ImageType, Map<Float, TranscriptEvent>> getTypeToTranscriptEvents(JsonObject object,
                                                                                boolean usePhoneToDisplay, String language) {
    return
        new ParseResultJson(db.getServerProps(), language)
            .readFromJSON(object, "words", "w", usePhoneToDisplay, null);
  }

  /**
   * TODO : why four copies!!!
   *
   * @param typeToEvent
   * @param language
   * @return
   * @see #getCachedAudioRef
   */
  @NotNull
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToSegments(Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent, String language) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<>();

    Map<String, String> phoneToDisplay = serverProps.getPhoneToDisplay(language);
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<TranscriptSegment> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) {
        typeToEndTimes.put(key, endTimes = new ArrayList<>());
      }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        String displayName = key == NetPronImageType.PHONE_TRANSCRIPT ? getDisplayName(value.event, phoneToDisplay) : value.event;

        endTimes.add(new TranscriptSegment(value.start, value.end, value.event, value.score, displayName));
      }
    }

    return typeToEndTimes;
  }

  private String getDisplayName(String event, Map<String, String> phoneToDisplay) {
    String displayName = phoneToDisplay.get(event);
    displayName = displayName == null ? event : displayName;
    return displayName;
  }

  /**
   * TODO : don't add annotations one at a time.
   *
   * @param ids
   * @param exercises
   * @param projectID
   * @return
   * @see #getFullExercises(int, Collection)
   */
  @NotNull
  private Set<CommonExercise> getCommonExercisesWithoutAudio(Collection<Integer> ids,
                                                             List<CommonExercise> exercises,
                                                             int projectID) {
    Set<CommonExercise> toAddAudioTo = new HashSet<>();
//    logger.info("getCommonExercisesWithoutAudio " + ids);
    for (int exid : ids) {
      CommonExercise byID = db.getCustomOrPredefExercise(projectID, exid);
      addAnnotations(byID); // todo do this in a better way
      //if (true || byID.getAudioAttributes().isEmpty()) {
      toAddAudioTo.add(byID);
      //  logger.info("getCommonExercisesWithoutAudio exercise " + exid + " has no audio...");
      //}
      exercises.add(byID);
//      logger.info("\tgetCommonExercisesWithoutAudio " + byID.getID());
    }
    return toAddAudioTo;
  }

  /**
   * @param exids
   * @param exercises
   * @param userID
   * @return
   * @see #getScoreHistoryPerExercise
   */
  private Map<Integer, CorrectAndScore> getScoreHistories(Collection<Integer> exids,
                                                          List<CommonExercise> exercises,
                                                          int userID) {
    return (exercises.isEmpty()) ? Collections.emptyMap() :
        db.getResultDAO().getScoreHistories(userID, exids, getLanguage(exercises.get(0)));
  }

  private <T extends Shell> T getExercise(int userID, int projectID, String exid, boolean isFlashcardReq) {
    int exid1 = -1;
    try {
      exid1 = Integer.parseInt(exid);
      return getAnnotatedExercise(userID, projectID, exid1, isFlashcardReq);
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

  /**
   * TODO : put back trie
   *
   * @return
   * @deprecated
   */
  private List<AmasExerciseImpl> getAMASExercises() {
    logger.info("get getAMASExercises -------");
    long then = System.currentTimeMillis();
    List<AmasExerciseImpl> exercises = db.getAMASExercises();
/*    if (amasFullTrie == null) {
      amasFullTrie = new ExerciseTrie<>(exercises, getOldLanguage(), getSmallVocabDecoder());
    }*/

    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list");// for " + getOldLanguage());
    }
    return exercises;
  }

  private SmallVocabDecoder getSmallVocabDecoder(int projectID) {
    return getAudioFileHelper(projectID).getSmallVocabDecoder();
  }
}