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
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.Collator;
import java.util.*;

@SuppressWarnings("serial")
public class ExerciseServiceImpl<T extends CommonShell> extends MyRemoteServiceServlet implements ExerciseService<T> {
  private static final Logger logger = LogManager.getLogger(ExerciseServiceImpl.class);

  private static final int SLOW_EXERCISE_EMAIL = 2000;
  private static final int SLOW_MILLIS = 50;
  private static final int WARN_DUR = 100;
  private static final boolean DEBUG = false;

  public static final int MIN_DEBUG_DURATION = 30;
  public static final int MIN_WARN_DURATION = 1000;

  private final Map<Integer, ExerciseListWrapper<T>> projidToWrapper = new HashMap<>();

  public FilterResponse getTypeToValues(FilterRequest request) {
    return getSectionHelper().getTypeToValues(request);
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
   * @see mitll.langtest.client.list.PagingExerciseList#loadExercises
   */
  @Override
  public ExerciseListWrapper<T> getExerciseIds(ExerciseListRequest request) {
    int projectID = getProjectID();

    if (projectID == -1) {
      logger.warn("getExerciseIds project id is -1?  It should probably have a real value.");
      List<T> ts = new ArrayList<T>();
      return new ExerciseListWrapper<T>(request.getReqID(), ts, null);
    }

    if (serverProps.isAMAS()) {
      ExerciseListWrapper<AmasExerciseImpl> amasExerciseIds = getAMASExerciseIds(request);
      return (ExerciseListWrapper<T>) amasExerciseIds; // TODO : how to do this without forcing it.
    }

    if (request.isNoFilter()) {
      ExerciseListWrapper<T> tExerciseListWrapper = getCachedExerciseWrapper(request, projectID);
      if (tExerciseListWrapper != null) return tExerciseListWrapper;
    }

    List<CommonExercise> exercises;

    logger.debug("getExerciseIds : (" + getLanguage() + ") " + "getting exercise ids for request " + request);

    try {
      boolean isUserListReq = request.getUserListID() != -1;
      UserList<CommonExercise> userListByID = isUserListReq ? db.getUserListByIDExercises(request.getUserListID(), getProjectID()) : null;

      if (request.getTypeToSelection().isEmpty()) {   // no unit-chapter filtering
        // get initial exercise set, either from a user list or predefined
        return getExerciseWhenNoUnitChapter(request, projectID, userListByID);
      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        if (userListByID != null) {
          Collection<CommonExercise> exercisesForState = getExercisesFromUserListFiltered(request.getTypeToSelection(), userListByID);
          ArrayList<CommonExercise> commonExercises = new ArrayList<>(exercisesForState);
          return getExerciseListWrapperForPrefix(request, filterExercises(request, commonExercises, projectID));
        } else {
          return getExercisesForSelectionState(request, projectID);
        }
      }
    } catch (Exception e) {
      logger.warn("got " + e, e);
      logAndNotifyServerException(e);
      return new ExerciseListWrapper<T>();
    }
  }

  private ExerciseListWrapper<T> getExerciseWhenNoUnitChapter(ExerciseListRequest request,
                                                              int projectID,
                                                              UserList<CommonExercise> userListByID) {
    List<CommonExercise> exercises;
    boolean predefExercises = userListByID == null;
    exercises = predefExercises ? getExercises() : getCommonExercises(userListByID);

    // now if there's a prefix, filter by prefix match
    int userID = request.getUserID();
    TripleExercises<CommonExercise> exercisesForSearch = new TripleExercises<CommonExercise>().setByExercise(exercises);
    if (!request.getPrefix().isEmpty()) {
      // now do a trie over matches
      exercisesForSearch = getExercisesForSearch(request.getPrefix(), exercises, predefExercises);
      //exercises = exercisesForSearch;

      if (request.getLimit() > 0) {
        exercisesForSearch.setByExercise(getFirstFew(request, exercisesForSearch.getByExercise()));
      }
//      exercises = getLimitedMatches(request, exercisesForSearch.getByExercise());
    }
    exercisesForSearch.setByExercise(filterExercises(request, exercisesForSearch.getByExercise(), projectID));

    // TODO : I don't think we need this?
/*        if (!isUserListReq) {
          markRecordedState(userID, request.getActivityType(), exercises, request.isOnlyExamples());
        }*/

    // now sort : everything gets sorted the same way
    List<CommonExercise> commonExercises = getSortedExercises(request, exercisesForSearch, predefExercises, userID);
    ExerciseListWrapper<T> exerciseListWrapper = makeExerciseListWrapper(request, commonExercises);

    if (request.isNoFilter()) {
      rememberCachedWrapper(projectID, exerciseListWrapper);
    }
    return exerciseListWrapper;
  }

  private void rememberCachedWrapper(int projectID, ExerciseListWrapper<T> exerciseListWrapper) {
    synchronized (projidToWrapper) {
      projidToWrapper.put(projectID, exerciseListWrapper);
    }
  }

  private List<CommonExercise> getSortedExercises(ExerciseListRequest request,
                                                  TripleExercises<CommonExercise> exercises,
                                                  boolean predefExercises, int userID) {
    List<CommonExercise> commonExercises;
    if (request.isIncorrectFirstOrder()) {
      commonExercises = db.getResultDAO().getExercisesSortedIncorrectFirst(exercises.getByExercise(), userID, getCollator(), getLanguage());
    } else {
      commonExercises = new ArrayList<>();
      if (predefExercises) {
        List<CommonExercise> basicExercises = new ArrayList<CommonExercise>(exercises.getByExercise());
        sortExercises(request.getActivityType() == ActivityType.RECORDER, basicExercises);
        commonExercises.addAll(exercises.getByID());
        commonExercises.addAll(basicExercises);

        List<CommonExercise> contextExercises = new ArrayList<CommonExercise>(exercises.getByContext());

        sortExercises(request.getActivityType() == ActivityType.RECORDER, contextExercises);

        commonExercises.addAll(contextExercises);
      }
    }
    return commonExercises;
  }

  @Nullable
  private ExerciseListWrapper<T> getCachedExerciseWrapper(ExerciseListRequest request, int projectID) {
    synchronized (projidToWrapper) {
      ExerciseListWrapper<T> exerciseListWrapper = projidToWrapper.get(projectID);
      if (exerciseListWrapper != null) {
        logger.info("getExerciseIds Returning cached exercises " + exerciseListWrapper);

        ExerciseListWrapper<T> tExerciseListWrapper = new ExerciseListWrapper<>(request.getReqID(),
            exerciseListWrapper.getExercises(),
            exerciseListWrapper.getFirstExercise());

        addScoresForAll(request.getUserID(), exerciseListWrapper.getExercises());
        return tExerciseListWrapper;
      }
    }
    return null;
  }

  private void addScoresForAll(int userid, List<T> exercises) {
    db.getResultDAO().addScoresForAll(userid, exercises);
  }

  /**
   * TODO: How does this work with multiple simultaneous users???
   * <p>
   * Maybe we should get scores separately?
   * Or copy exercises?
   * Or return on separate info channel?
   *
   * @param userid
   * @param exercises
   * @param <X>
   */
  private <X extends CommonShell> void addScores(int userid, List<X> exercises) {
    db.getResultDAO().addScores(userid, exercises);
  }

  /**
   * Return shortest matches first (on fl term).
   *
   * @param request
   * @param exercises
   * @return
   */
  private List<CommonExercise> getLimitedMatches(ExerciseListRequest request, List<CommonExercise> exercises) {
    if (request.getLimit() > 0) {
      exercises = getFirstFew(request, exercises);
    }
    return exercises;
  }

  @NotNull
  private List<CommonExercise> getFirstFew(ExerciseListRequest request, List<CommonExercise> exercises) {
    Collections.sort(exercises, (o1, o2) -> {
      String foreignLanguage = o1.getForeignLanguage();
      String foreignLanguage1 = o2.getForeignLanguage();
      int i = Integer.valueOf(foreignLanguage.length()).compareTo(foreignLanguage1.length());
      return i == 0 ? foreignLanguage.compareTo(foreignLanguage1) : i;
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
   * @see mitll.langtest.client.list.ListInterface#getExercises
   */
  private List<CommonExercise> getExercises() {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises = db.getExercises(getProjectID());
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("getExercises took " + (now - then) + " millis to get the raw exercise list for " + getLanguage());
    }

    return exercises;
  }

  private Collator getCollator() {
    return getAudioFileHelper().getCollator();
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
      //logger.debug("\tfound " + recordedForUser.size() + " recordings by " + userID + " only example " + onlyExample);
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
    logger.debug("\tmarkRecordedState marked " + c + "  recorded for '" + activityType + "' and user " + userID + " on " + exercises.size());
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
    Collection<CommonExercise> exercisesForState =
        getSectionHelper().getExercisesForSelectionState(request.getTypeToSelection());
    List<CommonExercise> copy = new ArrayList<>(exercisesForState);  // TODO : avoidable???
    exercisesForState = filterExercises(request, copy, projid);
    return getExerciseListWrapperForPrefix(request, exercisesForState);
  }


  /**
   * Always sort the result
   *
   * @param exercisesForState
   * @return
   * @see #getExerciseIds
   * @see #getExercisesForSelectionState
   */
  private <T extends CommonShell> ExerciseListWrapper<T> getExerciseListWrapperForPrefix(ExerciseListRequest request,
                                                                                         Collection<CommonExercise> exercisesForState
  ) {
    String prefix = request.getPrefix();
    int userID = request.getUserID();
    boolean incorrectFirst = request.isIncorrectFirstOrder();

    boolean hasPrefix = !prefix.isEmpty();
    if (hasPrefix) {
      logger.debug("getExerciseListWrapperForPrefix userID " + userID + " prefix '" + prefix + "' activity " + request.getActivityType());
    }

    int i = markRecordedState(userID, request.getActivityType(), exercisesForState, request.isOnlyExamples());
    //logger.debug("marked " +i + " as recorded role " +role);

    if (hasPrefix) {
      ExerciseTrie<CommonExercise> trie = new ExerciseTrie<>(exercisesForState, getLanguage(), getSmallVocabDecoder(), true);
      exercisesForState = trie.getExercises(prefix);
    }

    if (exercisesForState.isEmpty() && !prefix.isEmpty()) { // allow lookup by id
      CommonExercise exercise = getExercise(prefix, incorrectFirst);
      if (exercise != null) exercisesForState = Collections.singletonList(exercise);
    }
    // why copy???
    List<CommonExercise> copy;

    if (incorrectFirst) {
      copy = db.getResultDAO().getExercisesSortedIncorrectFirst(exercisesForState, userID, getCollator(), getLanguage());
    } else {
      copy = new ArrayList<>(exercisesForState);
      sortExercises(request.getActivityType() == ActivityType.RECORDER, copy);
    }

    return makeExerciseListWrapper(request, copy);
  }

  /**
   * TODO : what to do with request role here?
   * <p>
   * NOTE NOTE NOTE : not doing ensureMP3 - since we likely don't have access to file system for here.
   * ALSO - ideally this is done at the moment the wav is made.
   * <p>
   * Send the first exercise along so we don't have to ask for it after we get the initial list
   *
   * @param exercises
   * @return
   * @see #getExerciseIds
   * @see #getExerciseListWrapperForPrefix
   */
  private <T extends CommonShell> ExerciseListWrapper<T> makeExerciseListWrapper(ExerciseListRequest request,
                                                                                 Collection<CommonExercise> exercises) {
    CommonExercise firstExercise = exercises.isEmpty() ? null : request.isAddFirst() ? exercises.iterator().next() : null;

    int userID = request.getUserID();

    if (firstExercise != null) {
      addAnnotationsAndAudio(userID, firstExercise, request.isIncorrectFirstOrder());
      // NOTE : not ensuring MP3s or OGG versions of WAV file.
      // ensureMP3s(firstExercise, pathHelper.getInstallPath());
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);

    //   logger.debug("makeExerciseListWrapper : userID " +userID + " Role is " + activityType);
    markStateForActivity(request.isOnlyExamples(), userID, exerciseShells, request.getActivityType());

    // TODO : do this the right way vis-a-vis type safe collection...

    List<T> exerciseShells1 = (List<T>) exerciseShells;
    if (exerciseShells1.isEmpty() && firstExercise != null) {
      logger.error("makeExerciseListWrapper huh? no exercises");
    }
    ExerciseListWrapper<T> exerciseListWrapper = new ExerciseListWrapper<T>(request.getReqID(), exerciseShells1, firstExercise);

    addScores(userID, exerciseShells1);
    logger.debug("makeExerciseListWrapper returning " + exerciseListWrapper);
    return exerciseListWrapper;
  }

  private void markStateForActivity(boolean onlyExamples, int userID, List<CommonShell> exerciseShells, ActivityType activityType) {
    switch (activityType) {
      case RECORDER:
        markRecordedState(userID, activityType, exerciseShells, onlyExamples);
        break;
      case REVIEW:
      case QUALITY_CONTROL:
        getUserListManager().markState(exerciseShells);
        break;
      case MARK_DEFECTS:
        Collection<Integer> defectExercises = getUserListManager().getDefectExercises();
        //  int c = 0;
        for (CommonShell shell : exerciseShells) {
          if (defectExercises.contains(shell.getID())) {
            shell.setState(STATE.DEFECT);
            //    c++;
          }
        }
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
   * @seex LoadTesting#getExercise(String, long, boolean)
   * @see #makeExerciseListWrapper
   */
  private void addAnnotationsAndAudio(int userID, CommonExercise firstExercise, boolean isFlashcardReq) {
    long then = System.currentTimeMillis();

    addAnnotations(firstExercise); // todo do this in a better way
    long now = System.currentTimeMillis();
    int oldID = firstExercise.getID();
    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " + (now - then) + " millis to add annotations to exercise " + oldID);
    }
    then = now;
    attachAudio(firstExercise);

    if (DEBUG) {
      for (AudioAttribute audioAttribute : firstExercise.getAudioAttributes())
        logger.debug("\t addAnnotationsAndAudio ex " + oldID + " audio " + audioAttribute);
    }

    now = System.currentTimeMillis();
    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " +
          (now - then) + " millis to attach audio to exercise " + oldID);
    }
    then = now;

    User userWhere = db.getUserDAO().getUserWhere(userID);
    if (userWhere != null && userWhere.getPermissions().contains(User.Permission.QUALITY_CONTROL)) {
      addPlayedMarkings(userID, firstExercise);
      now = System.currentTimeMillis();
      if (now - then > SLOW_MILLIS) {
        logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " + (now - then) + " millis to add played markings to exercise " + oldID);
      }
    }

    then = now;

    db.getResultDAO().attachScoreHistory(userID, firstExercise, isFlashcardReq, getLanguage(firstExercise));

    now = System.currentTimeMillis();
    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " + (now - then) + " millis to attach score history to exercise " + oldID);
    }

    if (DEBUG) {
      for (AudioAttribute audioAttribute : firstExercise.getAudioAttributes())
        logger.debug("\t addAnnotationsAndAudio ret ex " + oldID + " audio " + audioAttribute);
    }
  }

  /**
   * @param byID
   * @see #addAnnotationsAndAudio(int, mitll.langtest.shared.exercise.CommonExercise, boolean)
   */
  private void addAnnotations(CommonExercise byID) {
    getUserListManager().addAnnotations(byID);
  }

  /**
   * @param firstExercise
   * @see #addAnnotationsAndAudio(int, mitll.langtest.shared.exercise.CommonExercise, boolean)
   */
  private void attachAudio(CommonExercise firstExercise) {
    db.getAudioDAO().attachAudioToExercise(firstExercise, getLanguage(firstExercise));
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
   * @see #addAnnotationsAndAudio(int, mitll.langtest.shared.exercise.CommonExercise, boolean)
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
   * @param commonExercises
   * @param <T>
   * @paramx role
   * @see #getExerciseIds(ExerciseListRequest)
   */
  private <T extends CommonShell> void sortExercises(boolean isRecorder, List<T> commonExercises) {
    new ExerciseSorter(db.getTypeOrder(getProjectID()))
        .getSorted(commonExercises, isRecorder, getProject().isEnglish());
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
                                                                              boolean predefExercises) {
    ExerciseTrie<T> fullTrie = (ExerciseTrie<T>) getProject().getFullTrie();
    return getExercisesForSearchWithTrie(prefix, exercises, predefExercises, fullTrie);
  }

  /**
   * If not the full exercise list, build a trie here and use it.
   *
   * @param prefix
   * @param exercises
   * @param predefExercises
   * @param fullTrie
   * @param <T>
   * @return
   */
  private <T extends CommonExercise> TripleExercises<T> getExercisesForSearchWithTrie(String prefix,
                                                                                      Collection<T> exercises,
                                                                                      boolean predefExercises,
                                                                                      ExerciseTrie<T> fullTrie) {
    ExerciseTrie<T> trie = predefExercises ? fullTrie : new ExerciseTrie<T>(exercises, getLanguage(), getSmallVocabDecoder(), true);
    List<T> basicExercises = trie.getExercises(prefix);
    logger.info("getExercisesForSearchWithTrie : prefix " + prefix + " matches " + basicExercises.size());
    ExerciseTrie<T> fullContextTrie = (ExerciseTrie<T>) getProject().getFullContextTrie();
    return new TripleExercises<T>(getExercieByExid(prefix), basicExercises, predefExercises ? fullContextTrie.getExercises(prefix) : Collections.emptyList());
  }

  private static class TripleExercises<T extends CommonExercise> {
    private List<T> byID = Collections.emptyList();
    private List<T> byExercise = Collections.emptyList();
    private List<T> byContext = Collections.emptyList();

    public TripleExercises() {
    }

    public TripleExercises(List<T> byID, List<T> byExercise, List<T> byContext) {
      this.byID = byID;
      this.byExercise = byExercise;
      this.byContext = byContext;
    }

    public List<T> getByID() {
      return byID;
    }

    public void setByID(List<T> byID) {
      this.byID = byID;
    }

    public List<T> getByExercise() {
      return byExercise;
    }

    public TripleExercises setByExercise(List<T> byExercise) {
      this.byExercise = byExercise;
      return this;
    }

    public List<T> getByContext() {
      return byContext;
    }

    public void setByContext(List<T> byContext) {
      this.byContext = byContext;
    }
  }

  private <T extends CommonExercise> List<T> getExercieByExid(String prefix) {
    int exid = getExid(prefix);

    if (exid > 0) {
      logger.info("return exid " + exid);
      T exercise = getExercise(exid, false);
      if (exercise != null) {
        return Collections.singletonList(exercise);
      }
    }
    return Collections.emptyList();
  }

  private int getExid(String prefix) {
    int exid = -1;
    if (!prefix.isEmpty()) {
      try {
        exid = Integer.parseInt(prefix);
      } catch (NumberFormatException e) {
        // logger.info("getExercisesForSearchWithTrie can't parse search number '" + prefix + "'");
      }
    }
    return exid;
  }

/*  private <T extends CommonShell> Collection<T> getAMASExercisesForSearch(String prefix, int userID, Collection<T> exercises, boolean predefExercises) {
    long then = System.currentTimeMillis();
    ExerciseTrie<T> trie = predefExercises ? fullTrie : new ExerciseTrie<T>(exercises, getLanguage(), audioFileHelper.getSmallVocabDecoder());
    exercises = trie.getExercises(prefix, audioFileHelper.getSmallVocabDecoder());
    long now = System.currentTimeMillis();
    if (now - then > 300) {
      logger.debug("took " + (now - then) + " millis to do trie lookup");
    }
    if (exercises.isEmpty()) { // allow lookup by id
      T exercise = getExercise(prefix, userID, false);
      if (exercise != null) exercises = Collections.singletonList(exercise);
    }
    return exercises;
  }*/


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
      return getUnrecordedExercises(request.getUserID(), exercises, projid, request.isOnlyExamples());
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
  private List<CommonExercise> getUnrecordedExercises(int userID,
                                                      Collection<CommonExercise> exercises,
                                                      int projid,
                                                      boolean onlyExamples) {
    Collection<Integer> recordedBySameGender =
        getRecordedByMatchingGender(userID, exercises, projid, onlyExamples);

    Set<Integer> allExercises = new HashSet<>();
    for (CommonShell exercise : exercises) {
      allExercises.add(exercise.getID());
    }

    //logger.debug("all exercises " + allExercises.size() + " removing " + recordedBySameGender.size());
    allExercises.removeAll(recordedBySameGender);
    // logger.debug("after all exercises " + allExercises.size());

    List<CommonExercise> copy = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    for (CommonExercise exercise : exercises) {
      int trim = exercise.getID();
      if (allExercises.contains(trim)) {
        if (seen.contains(trim)) logger.warn("saw " + trim + " " + exercise + " again!");
        if (!onlyExamples || hasContext(exercise)) {
          seen.add(trim);
          copy.add(exercise);
        }
      }
    }
    //logger.debug("to be recorded " + copy.size() + " from " + exercises.size());

    return copy;
  }

  private Collection<Integer> getRecordedByMatchingGender(int userID, Collection<CommonExercise> exercises, int projid, boolean onlyExamples) {
    // int userID = request.getUserID();
    logger.debug("filterByUnrecorded : for " + userID + " only by same gender " +
        " examples only " + onlyExamples + " from " + exercises.size());

    Map<Integer, String> exToTranscript = new HashMap<>();
    Map<Integer, String> exToContextTranscript = new HashMap<>();

    for (CommonExercise shell : exercises) {
      exToTranscript.put(shell.getID(), shell.getForeignLanguage());
      String context = shell.hasContext() ? shell.getContext() : null;
      if (context != null && !context.isEmpty()) {
        exToContextTranscript.put(shell.getID(), context);
      }
    }

    return onlyExamples ?
        db.getAudioDAO().getWithContext(userID, exToContextTranscript, projid) :
        db.getAudioDAO().getRecordedBySameGender(userID, exToTranscript, projid);
  }

  @NotNull
  private List<CommonExercise> getExercisesWithContext(Collection<CommonExercise> exercises) {
    List<CommonExercise> copy = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    for (CommonExercise exercise : exercises) {
      // String trim = exercise.getID().trim();

      if (seen.contains(exercise.getID())) logger.warn("saw " + exercise.getID() + " " + exercise + " again!");
      if (hasContext(exercise)) {
        seen.add(exercise.getID());
        copy.add(exercise);
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
      List<CommonExercise> copy = new ArrayList<CommonExercise>();
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
      List<CommonExercise> copy = new ArrayList<CommonExercise>();
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
    Collection<Integer> inspected = getUserListManager().getInspectedExercises();
    // logger.info("found " + inspected.size());
    List<CommonExercise> copy = new ArrayList<CommonExercise>();
    for (CommonExercise exercise : exercises) {
      if (!inspected.contains(exercise.getID())) {
        copy.add(exercise);
      }
    }
    return copy;
  }

  /**
   * On the fly we make a new section helper to do filtering of user list.
   *
   * @param typeToSelection
   * @param userListByID
   * @param <T>
   * @return
   */
  private <T extends CommonShell & HasUnitChapter> Collection<T> getExercisesFromUserListFiltered(Map<String, Collection<String>> typeToSelection,
                                                                                                  UserList<T> userListByID) {
    SectionHelper<T> helper = new SectionHelper<T>();
    Collection<T> exercises2 = getCommonExercises(userListByID);
    long then = System.currentTimeMillis();
    for (T commonExercise : exercises2) {
      helper.addExercise(commonExercise);
    }
    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.debug("used " + exercises2.size() + " exercises to build a hierarchy in " + (now - then) + " millis");
    }
    //helper.report();
    Collection<T> exercisesForState = helper.getExercisesForSelectionState(typeToSelection);
    // logger.debug("\tafter found " + exercisesForState.size() + " matches to " + typeToSelection);
    return exercisesForState;
  }

  /**
   * Save transmission bandwidth - don't send a list of fully populated items - just send enough to populate a list
   *
   * @param exercises
   * @return
   * @see #makeExerciseListWrapper
   */
  private <T extends CommonShell> List<CommonShell> getExerciseShells(
      // Collection<? extends CommonExercise> exercises
      Collection<T> exercises
  ) {
//    logger.info("getExerciseShells for " + exercises.size());
    List<CommonShell> ids = new ArrayList<>();
    for (T e : exercises) {
//      logger.info("got " +e.getOldID() + " mean " + e.getMeaning() + " eng " + e.getEnglish() + " fl " + e.getForeignLanguage());
      ids.add(e.getShell());
    }
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
        return new ExerciseListWrapper<AmasExerciseImpl>(reqID, new ArrayList<>(exercisesForSelectionState1), null);
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
  public <T extends Shell> T getExercise(int exid, boolean isFlashcardReq) {
    if (serverProps.isAMAS()) { // TODO : HOW TO AVOID CAST???
      return (T) db.getAMASExercise(exid);
    }
    int projectID = getProjectID();
    int userID = getUserIDFromSession();

    return getAnnotatedExercise(userID, projectID, exid, isFlashcardReq);
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
    Collection<CommonExercise> exercises = getExercises();
    CommonExercise byID = db.getCustomOrPredefExercise(projectID, exid);

    long now = System.currentTimeMillis();
    String language = getLanguage();
    if (now - then2 > WARN_DUR) {
      logger.debug("getAnnotatedExercise : (" + language + ") took " + (now - then2) + " millis to find exercise " +
          exid + " for " + userID);
    }

    if (byID == null) {
      logger.error("getAnnotatedExercise : huh? couldn't find exercise with id '" + exid + "' when examining " +
          exercises.size() + " items");
    } else {
      logger.debug("getAnnotatedExercise : (" + language +
              ") project " + projectID +
              " find exercise " + exid + " for " + userID + " : " + byID
          //+"\n\tcontext" + byID.getDirectlyRelated()
      );
      then2 = System.currentTimeMillis();
      addAnnotationsAndAudio(userID, byID, isFlashcardReq);
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
    }
    checkPerformance(exid, then2);

    if (byID != null) {
/*
      logger.debug("returning (" + language + ") exercise " + byID.getOldID() + " : " + byID);
      for (AudioAttribute audioAttribute : byID.getAudioAttributes()) {
        logger.info("\thas " + audioAttribute);
      }
*/
    } else {
      logger.warn(getLanguage() + " : couldn't find exercise with id '" + exid + "'");
    }
    // return byID;
    // TODO : why doesn't this work?
    return (T) byID;
  }

  /**
   * @param reqid
   * @param ids
   * @param isFlashcardReq
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getExercises(Collection)
   */
  @Override
  public ExerciseListWrapper<CommonExercise> getFullExercises(int reqid, Collection<Integer> ids, boolean isFlashcardReq) {
    List<CommonExercise> exercises = new ArrayList<>();

    int projectID = getProjectID();
    int userID = getUserIDFromSession();

    Set<CommonExercise> toAddAudioTo = getCommonExercisesWithoutAudio(ids, exercises, projectID);

    if (ids.size() > toAddAudioTo.size()) {
      logger.info("getFullExercises decreased from " + ids.size() + " to " + toAddAudioTo.size());
    } else {
      logger.info("getFullExercises getting " + ids.size() + " exercises");
    }

    if (!toAddAudioTo.isEmpty()) {
      db.getAudioDAO().attachAudioToExercises(toAddAudioTo, getLanguage(toAddAudioTo.iterator().next()));
    } else {
      logger.info("getFullExercises all " + ids.size() + " exercises have audio");
    }

    addScores(userID, exercises);

    return new ExerciseListWrapper<>(reqid, exercises, null, getScoreHistories(ids, exercises, userID));
  }

  @NotNull
  private Set<CommonExercise> getCommonExercisesWithoutAudio(Collection<Integer> ids, List<CommonExercise> exercises, int projectID) {
    Set<CommonExercise> toAddAudioTo = new HashSet<>();

    for (int exid : ids) {
      CommonExercise byID = db.getCustomOrPredefExercise(projectID, exid);
      if (byID.getAudioAttributes().isEmpty()) {
        toAddAudioTo.add(byID);
        logger.info("getCommonExercisesWithoutAudio exercise " + exid + " has no audio...");
      }
      exercises.add(byID);
    }
    return toAddAudioTo;
  }

  private Map<Integer, List<CorrectAndScore>> getScoreHistories(Collection<Integer> ids, List<CommonExercise> exercises, int userID) {
    return (exercises.isEmpty()) ? Collections.emptyMap() : db.getResultDAO().getScoreHistories(userID, ids, getLanguage(exercises.get(0)));
  }

  private <T extends Shell> T getExercise(String exid, boolean isFlashcardReq) {
    int exid1 = -1;
    try {
      exid1 = Integer.parseInt(exid);
      return getExercise(exid1, isFlashcardReq);
    } catch (NumberFormatException e) {
      logger.warn("getExercise can't parse '" + exid + "'");
      return null;
    }
  }

  /**
   * @param id
   * @param then
   * @see #getExercise(String, boolean)
   */
  private void checkPerformance(int id, long then) {
    long diff = System.currentTimeMillis() - then;

    if (diff > MIN_DEBUG_DURATION) {
      String language = getLanguage();
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
   */
  private List<AmasExerciseImpl> getAMASExercises() {
    logger.info("get getAMASExercises -------");
    long then = System.currentTimeMillis();
    List<AmasExerciseImpl> exercises = db.getAMASExercises();
/*    if (amasFullTrie == null) {
      amasFullTrie = new ExerciseTrie<>(exercises, getOldLanguage(), getSmallVocabDecoder());
    }*/

//    if (getServletContext().getAttribute(AUDIO_FILE_HELPER_REFERENCE) == null) {
//      shareAudioFileHelper(getServletContext());
//    }
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list for " + getOldLanguage());
    }
    return exercises;
  }

  private SmallVocabDecoder getSmallVocabDecoder() {
    return getAudioFileHelper().getSmallVocabDecoder();
  }

  private String getOldLanguage() {
    return serverProps.getLanguage();
  }
}