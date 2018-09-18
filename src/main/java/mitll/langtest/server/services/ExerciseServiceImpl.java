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

import mitll.langtest.client.services.ExerciseService;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.scoring.AlignmentHelper;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.project.Language;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moxieapps.gwt.highcharts.client.Lang;

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

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_ID_LOOKUP = true;

  private static final boolean USE_PHONE_TO_DISPLAY = true;
  private static final boolean WARN_MISSING_REF_RESULT = false;
  private static final String RECORDED1 = "Recorded";
  //  private static final String RECORDED = RECORDED1;
  private static final String ANY = "Any";

  // private  AlignmentHelper alignmentHelper=new AlignmentHelper();

  /**
   * @param request
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getTypeToValues
   */
  public FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException {
    ISection<CommonExercise> sectionHelper = getSectionHelper();
    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      FilterResponse response = sectionHelper.getTypeToValues(request, false);
      addUserListFacet(request, response);
      int userFromSessionID = getUserIDFromSessionOrDB();
      if (userFromSessionID != -1) {
//        logger.info("getTypeToValues got " + userFromSession);
        //       logger.info("getTypeToValues isRecordRequest " + request.isRecordRequest());

        if (request.isRecordRequest()) { //how no user???
          response = getFilterResponseForRecording(request, response, userFromSessionID);
        }
      }

      return response;
    }
  }

  private FilterResponse getFilterResponseForRecording(FilterRequest request, FilterResponse response, int userFromSessionID) {
    int projectID = getProjectIDFromUser(userFromSessionID);

    Map<String, Collection<String>> typeToSelection = new HashMap<>();
    request.getTypeToSelection().forEach(pair -> {
      String value1 = pair.getValue();
      if (!value1.equalsIgnoreCase(ANY)) {
        typeToSelection.put(pair.getProperty(), Collections.singleton(value1));
      }
    });


    List<CommonExercise> exercisesForState = new ArrayList<>(getExercisesForSelection(projectID, typeToSelection));

    ExerciseListRequest request1 = new ExerciseListRequest()
        .setOnlyUnrecordedByMe(true)
        .setOnlyExamples(request.isExampleRequest())
        .setUserID(userFromSessionID);

    List<String> typeOrder = getProject(projectID).getTypeOrder();
    if (typeOrder.isEmpty()) {

    } else {
      String firstType = typeOrder.get(0);
      //   List<CommonExercise> rec = filterByUnrecorded(request1.setOnlyUnrecordedByMe(false).setOnlyRecordedByMatchingGender(true), exercisesForState, projectID);
      List<CommonExercise> unRec = filterByUnrecorded(request1, exercisesForState, projectID);

      //   logger.info("found " + unRec.size() + " unrecorded");

      Map<String, Long> collect = unRec.stream().collect(Collectors.groupingBy(ex -> ex.getUnitToValue().get(firstType) == null ? "Unknown" : ex.getUnitToValue().get(firstType), Collectors.counting()));

      // logger.info("map is " + collect);

      Map<String, Set<MatchInfo>> typeToValues = new HashMap<>();

      Set<MatchInfo> matches = new TreeSet<>();
      collect.forEach((k, v) -> matches.add(new MatchInfo(k, v.intValue())));
      //    logger.info("matches is " + matches);
      typeToValues.put(firstType, matches);

      HashSet<String> typesToInclude = new HashSet<>();
      typesToInclude.add(firstType);

      response = new FilterResponse(request.getReqID(), typeToValues, typesToInclude, -1);
    }
    return response;
  }

  private void addUserListFacet(FilterRequest request, FilterResponse typeToValues) {
    int userListID = request.getUserListID();
    UserList<CommonShell> next = userListID != -1 ? db.getUserListManager().getSimpleUserListByID(userListID) : null;

    if (next != null) {  // echo it back
      //logger.info("\tgetTypeToValues " + request + " include list " + next);
      typeToValues.getTypesToInclude().add(LISTS);
      Set<MatchInfo> value = new HashSet<>();
      value.add(new MatchInfo(next.getName(), next.getNumItems(), userListID, false, ""));
      typeToValues.getTypeToValues().put(LISTS, value);
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

        logger.info("getExerciseIds : 1 req  " + request + " took " + (System.currentTimeMillis() - then) +
            " millis to get " + exerciseWhenNoUnitChapter.getSize());

        return exerciseWhenNoUnitChapter;
      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        if (userListByID != null) {
          Collection<CommonExercise> exercisesForState =
              getExercisesFromUserListFiltered(request.getTypeToSelection(), userListByID);
          ExerciseListWrapper<T> exerciseListWrapperForPrefix = getExerciseListWrapperForPrefix(request,
              filterExercises(request, new ArrayList<>(exercisesForState), projectID), projectID);

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

  private ExerciseListWrapper<T> getDialogResponse(ExerciseListRequest request, int projectID) throws DominoSessionException {
    // List<ClientExercise> exercises = getDialog(request.getDialogID()).getExercises();
    List<CommonExercise> collect = getCommonExercises(getDialog(request.getDialogID()).getCoreVocabulary());
    collect.addAll(getCommonExercises(getDialog(request.getDialogID()).getExercises()));

    logger.info("getDialogResponse returning exercises for " + request.getDialogID() + " " + collect.size());
    ExerciseListWrapper<T> exerciseListWrapper = makeExerciseListWrapper(request, collect, projectID);
    return exerciseListWrapper;
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
        exercisesForSearch = getExercisesForSearch(prefix, exercises, predefExercises, projectID, request.getUserID(),
            !request.isPlainVocab());
        if (request.getLimit() > 0) {
          exercisesForSearch.setByExercise(getFirstFew(prefix, request, exercisesForSearch.getByExercise(), projectID));
        }
      }
    }

    if (DEBUG_ID_LOOKUP) logger.info("triple resp       " + exercisesForSearch);
    exercisesForSearch.setByExercise(filterExercises(request, exercisesForSearch.getByExercise(), projectID));
    if (DEBUG_ID_LOOKUP) logger.info("after triple resp " + exercisesForSearch);

    // TODO : I don't think we need this?
/*        if (!isUserListReq) {
          markRecordedState(userID, request.getActivityType(), exercises, request.isOnlyExamples());
        }*/

    // now sort : everything gets sorted the same way
    List<CommonExercise> commonExercises = getSortedExercises(request, exercisesForSearch, predefExercises, request.getUserID(), projectID);

    long now = System.currentTimeMillis();

    long diff = now - then;
    if (diff > 20 || DEBUG_ID_LOOKUP) {
      logger.info("getExerciseWhenNoUnitChapter took " + diff + " to get " + commonExercises.size());
    }

    ExerciseListWrapper<T> exerciseListWrapper = makeExerciseListWrapper(request, commonExercises, projectID);
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
   * @see #getExerciseWhenNoUnitChapter
   */
  private List<CommonExercise> getSortedExercises(ExerciseListRequest request,
                                                  TripleExercises<CommonExercise> exercises,
                                                  boolean predefExercises,
                                                  int userID,
                                                  int projID) {
    Project projectForUser = getProject(projID);

    List<CommonExercise> commonExercises = exercises.getByExercise();
    if (commonExercises.isEmpty()) {
      commonExercises = exercises.getByID();
    }
    if (request.isIncorrectFirstOrder()) {
      if (DEBUG)
        logger.info("getSortedExercises adding isIncorrectFirstOrder " + exercises.getByExercise().size() + " basicExercises");
      commonExercises =
          db.getResultDAO().getExercisesSortedIncorrectFirst(exercises.getByExercise(), userID,
              getAudioFileHelper(projID).getCollator(), getLanguageEnum(projectForUser));
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
      else if (hasSearch1 && hasSearch2) {
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
  private <U extends CommonExercise> List<U> getCommonExercises(UserList<U> userListByID) {
    return new ArrayList<>(userListByID.getExercises());
  }

  /**
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises
   * @see #getExerciseIds
   */
  private ExerciseListWrapper<T> getExercisesForSelectionState(ExerciseListRequest request, int projid) {
    Map<String, Collection<String>> typeToSelection = request.getTypeToSelection();
    Collection<CommonExercise> exercisesForState = getExercisesForSelection(projid, typeToSelection);

    List<CommonExercise> copy = new ArrayList<>(exercisesForState);  // TODO : avoidable???
    exercisesForState = filterExercises(request, copy, projid);
    return getExerciseListWrapperForPrefix(request, exercisesForState, projid);
  }

  private Collection<CommonExercise> getExercisesForSelection(int projid, Map<String, Collection<String>> typeToSelection) {
    ISection<CommonExercise> sectionHelper = getSectionHelper(projid);
    typeToSelection.remove(RECORDED1);

    return typeToSelection.isEmpty() ? getExercises(projid) :
        sectionHelper.getExercisesForSelectionState(typeToSelection);
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
      CommonExercise exercise = getExercise(userID, projID, prefix);
      if (exercise != null) exercisesForState = Collections.singletonList(exercise);
    }
    // why copy???
    List<CommonExercise> copy;

    if (incorrectFirst) {
      copy = db.getResultDAO().getExercisesSortedIncorrectFirst(exercisesForState, userID, getAudioFileHelper(projID).getCollator(), getLanguageEnum(projID));
    } else {
      copy = new ArrayList<>(exercisesForState);
//      if (!request.isQuiz()) {
      sortExercises(request.getActivityType() == ActivityType.RECORDER, copy, false, request.getPrefix());
      //    }
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
  private ExerciseListWrapper<T> makeExerciseListWrapper(ExerciseListRequest request,
                                                         Collection<CommonExercise> exercises,
                                                         int projID) {
    long then = System.currentTimeMillis();
    CommonExercise firstExercise = exercises.isEmpty() ? null : request.isAddFirst() ? exercises.iterator().next() : null;

    int userID = request.getUserID();

    if (firstExercise != null) {
      addAnnotationsAndAudio(userID, firstExercise, request.isQC(), projID);
      // NOTE : not ensuring MP3s or OGG versions of WAV file.
      // ensureMP3s(firstExercise, pathHelper.getInstallPath());
      if (request.shouldAddContext()) { // add the context exercises
        //  logger.info("adding context exercises...");
        List<CommonExercise> withContext = new ArrayList<>();
        exercises.forEach(commonExercise -> {
          withContext.add(commonExercise);
          //  logger.info("\t" + commonExercise.getID() + " " + commonExercise.getDirectlyRelated().size());
          commonExercise.getDirectlyRelated().forEach(clientExercise -> withContext.add(clientExercise.asCommon()));
          // withContext.addAll(commonExercise.getDirectlyRelated());
        });
        exercises = withContext;
      }
    }
    List<T> exerciseShells = getExerciseShells(exercises, request.isQC());

//    logger.debug("makeExerciseListWrapper : userID " + userID + " Role is " + request.getActivityType());
    markStateForActivity(request.isOnlyExamples(), userID, exerciseShells, request.getActivityType());

    // TODO : do this the right way vis-a-vis type safe collection...

    //List<T> exerciseShells1 = (List<T>) exerciseShells;
    if (exerciseShells.isEmpty() && firstExercise != null) {
      logger.error("makeExerciseListWrapper huh? no exercises");
    }

    ExerciseListWrapper<T> exerciseListWrapper = new ExerciseListWrapper<T>(request.getReqID(), exerciseShells, firstExercise);

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
   * @param onlyExamples
   * @param userID
   * @param exerciseShells
   * @param activityType
   * @see #makeExerciseListWrapper(ExerciseListRequest, Collection, int)
   */
  private void markStateForActivity(boolean onlyExamples, int userID, List<T> exerciseShells, ActivityType activityType) {
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
   * @param isQC
   * @param projID
   * @seex LoadTesting#getExercise(String, long, boolean)
   * @see #makeExerciseListWrapper
   */
  private void addAnnotationsAndAudio(int userID, CommonExercise firstExercise, boolean isQC, int projID) {
    long then = System.currentTimeMillis();

    logger.info("addAnnotationsAndAudio adding anno to " + firstExercise.getID() + " with " + firstExercise.getDirectlyRelated().size() + " context exercises");
    addAnnotations(firstExercise); // todo do this in a better way

    long now = System.currentTimeMillis();
    int oldID = firstExercise.getID();
    String language = getLanguage(projID);

    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + language + ") took " + (now - then) + " millis to add annotations to exercise " + oldID);
    }
    then = now;
    int i = attachAudio(firstExercise);
    logger.info("attached " + i + " audio cuts to " + firstExercise.getID());

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
   * @param byID
   * @see #addAnnotationsAndAudio(int, CommonExercise, boolean, int)
   */
  private void addAnnotations(CommonExercise byID) {
    IUserListManager userListManager = db.getUserListManager();
    userListManager.addAnnotations(byID);
    byID.getDirectlyRelated().forEach(userListManager::addAnnotations);
  }

  /**
   * @param firstExercise
   * @see #addAnnotationsAndAudio(int, CommonExercise, boolean, int)
   */
  private int attachAudio(CommonExercise firstExercise) {
    return db.getAudioDAO().attachAudioToExercise(firstExercise, getLanguageEnum(firstExercise), new HashMap<>());
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
   * @see #addAnnotationsAndAudio(int, CommonExercise, boolean, int)
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
//    logger.info("filter req " + request);
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
   * @param commonExercises
   * @param sortByFL
   * @param searchTerm
   * @paramx role
   * @see #getExerciseListWrapperForPrefix(ExerciseListRequest, Collection, int)
   * @see #getSortedExercises
   */
  private <U extends CommonShell> void sortExercises(boolean isRecorder,
                                                     List<U> commonExercises,
                                                     boolean sortByFL,
                                                     String searchTerm) {
    new ExerciseSorter<U>().getSorted(commonExercises, isRecorder, sortByFL, searchTerm);
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
                                                                int userID, boolean matchOnContext) {
    Search<CommonExercise> search = new Search<CommonExercise>(db);
    TripleExercises<CommonExercise> exercisesForSearch =
        search.getExercisesForSearch(prefix, exercises, predefExercises, projectID, matchOnContext);
    exercisesForSearch.setByID(Collections.emptyList());

    {
      int exid = search.getID(prefix.trim());
      if (exid != -1 && exid != 1) {
        if (DEBUG_ID_LOOKUP) logger.info("getExercisesForSearch looking for exercise in " + projectID + " = " + exid);
        CommonExercise exercise = getAnnotatedExercise(userID, projectID, exid);

        if (exercise != null && exercise.isContext()) {
          if (DEBUG_ID_LOOKUP)
            logger.info("\tgetExercisesForSearch found context sentence in " + projectID + " = " + exid);
          int parentExerciseID1 = exercise.getParentExerciseID();
          int parentExerciseID = parentExerciseID1 > 0 ? parentExerciseID1 : db.getExerciseDAO(projectID).getParentFor(exid);

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
      logger.info("filterByUnrecorded : Filter for matching gender to " + request.getUserID());
      return getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples(), false);
    } else if (request.isOnlyRecordedByMatchingGender()) {
      logger.info("filterByUnrecorded : Filter for matching gender to " + request.getUserID());
      return getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples(), true);
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
   * @param onlyRecorded
   * @return
   */
  @NotNull
  private List<CommonExercise> getRecordFilterExercisesMatchingGender(int userID,
                                                                      Collection<CommonExercise> exercises,
                                                                      int projid,
                                                                      boolean onlyExamples,
                                                                      boolean onlyRecorded) {

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

    logger.info("getRecordFilterExercisesMatchingGender " + onlyExamples +
        "\n\texToTranscript " + exToTranscript.size());
    Collection<Integer> recordedBySameGender = getRecordedByMatchingGender(userID, projid, onlyExamples, exToTranscript);

    logger.info("getRecordFilterExercisesMatchingGender" +
        "\n\tall exercises " + unrecordedIDs.size() +
        "\n\tfor project # " + projid +
        "\n\tuserid #      " + userID +
        "\n\tremoving      " + recordedBySameGender.size() +
        "\n\tretain        " + onlyRecorded
    );

    if (onlyRecorded) {
      unrecordedIDs.retainAll(recordedBySameGender);
    } else {
      unrecordedIDs.removeAll(recordedBySameGender);
    }

    logger.info("getRecordFilterExercisesMatchingGender after removing recorded exercises " + unrecordedIDs.size());

    List<CommonExercise> unrecordedExercises = new ArrayList<>();

    for (CommonExercise exercise : exercises) {
      if (onlyExamples) {
        for (ClientExercise dir : exercise.getDirectlyRelated()) {
          if (unrecordedIDs.contains(dir.getID())) {
            unrecordedExercises.add(dir.asCommon());
          }
        }
      } else {
        if (unrecordedIDs.contains(exercise.getID())) {
          unrecordedExercises.add(exercise);
        }
      }
    }

    logger.info("getRecordFilterExercisesMatchingGender to be recorded " + unrecordedExercises.size() + " from " + exercises.size());

    return unrecordedExercises;
  }

  /**
   * @param userID
   * @param projid
   * @param onlyExamples
   * @return
   * @see #getRecordFilterExercisesMatchingGender
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
        exercise.getDirectlyRelated().forEach(clientExercise -> copy.add(clientExercise.asCommon()));
        //copy.addAll(exercise.getDirectlyRelated());
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
   * @param
   * @return
   */
  private <U extends CommonExercise> Collection<U> getExercisesFromUserListFiltered(
      Map<String, Collection<String>> typeToSelection,
      UserList<U> userListByID) {
    Collection<U> exercises2 = getCommonExercises(userListByID);
    typeToSelection.remove(LISTS);
    if (typeToSelection.isEmpty()) {
      logger.info("getExercisesFromUserListFiltered returning  " + userListByID.getExercises().size() +
          " exercises for " + userListByID.getID());
      return exercises2;
    } else {
      SectionHelper<U> helper = new SectionHelper<>();

      logger.info("getExercisesFromUserListFiltered found " + exercises2.size() + " for list " + userListByID);
      long then = System.currentTimeMillis();
      exercises2.forEach(helper::addExercise);
      long now = System.currentTimeMillis();

      if (now - then > 100) {
        logger.debug("getExercisesFromUserListFiltered used " + exercises2.size() + " exercises to build a hierarchy in " + (now - then) + " millis");
      }
      //helper.report();
      Collection<U> exercisesForState = helper.getExercisesForSelectionState(typeToSelection);
      logger.debug("\tgetExercisesFromUserListFiltered after found " + exercisesForState.size() + " matches to " + typeToSelection);
      return /*typeToSelection.isEmpty() ? exercises2 :*/ exercisesForState;
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
      if (ex.getNumPhones() == 0 && warn++ < 100) {
        logger.warn("getExerciseShells : no phones for exercise " + ex.getID());
      }
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
   * @param isFlashcardReq
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   * @see mitll.langtest.client.list.ExerciseList#goGetNextAndCacheIt
   * @see mitll.langtest.client.analysis.PlayAudio#playLast
   */
  public T getExercise(int exid, boolean isFlashcardReq) throws DominoSessionException {
    return (T) getAnnotatedExercise(getUserIDFromSessionOrDB(), getProjectIDFromUser(), exid);
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
   * @param reqid
   * @param ids
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getExercises
   */
  @Override
  public ExerciseListWrapper<ClientExercise> getFullExercises(int reqid, Collection<Integer> ids) throws DominoSessionException {
    List<ClientExercise> exercises = new ArrayList<>();

    int userID = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser(userID);
    if (projectID == -1) {
      logger.info("getFullExercises : no project for user " + userID);
      return new ExerciseListWrapper<>();
    }
    Language language = getLanguageEnum(projectID);

    long then = System.currentTimeMillis();
    Set<ClientExercise> toAddAudioTo = getCommonExercisesWithoutAudio(ids, exercises, projectID);
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
    new AlignmentHelper(serverProps, db.getRefResultDAO()).addAlignmentOutput(projectID, getProject(projectID), toAddAudioTo);
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

    Map<Integer, CorrectAndScore> scoreHistoryPerExercise = getScoreHistoryPerExercise(ids, exercises, userID, language);
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
      db.getAudioDAO().attachAudioToExercises(Collections.singleton(byID), getLanguageEnum(projectIDFromUser));
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
    Map<Integer, CorrectAndScore> scoreHistories = getScoreHistories(ids, exercises, userID, language);
    long now = System.currentTimeMillis();
    if (now - then > 50)
      logger.info("getScoreHistoryPerExercise took " + (now - then) + " to get score histories for " + exercises.size() + " exercises");

    return scoreHistories;
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
  private Set<ClientExercise> getCommonExercisesWithoutAudio(Collection<Integer> ids,
                                                             List<ClientExercise> exercises,
                                                             int projectID) {
    Set<ClientExercise> toAddAudioTo = new HashSet<>();
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
   * @param language
   * @return
   * @see #getScoreHistoryPerExercise
   */
  private Map<Integer, CorrectAndScore> getScoreHistories(Collection<Integer> exids,
                                                          List<ClientExercise> exercises,
                                                          int userID,
                                                          Language language) {
    return (exercises.isEmpty()) ? Collections.emptyMap() :
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