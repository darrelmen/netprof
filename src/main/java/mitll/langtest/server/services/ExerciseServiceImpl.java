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
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.User;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.Collator;
import java.util.*;

@SuppressWarnings("serial")
public class ExerciseServiceImpl extends MyRemoteServiceServlet implements ExerciseService {
  private static final Logger logger = LogManager.getLogger(ExerciseServiceImpl.class);

  private static final int SLOW_EXERCISE_EMAIL = 2000;
  private static final int SLOW_MILLIS = 40;
  private static final int WARN_DUR = 100;

  private ExerciseTrie<AmasExerciseImpl> amasFullTrie = null;
  private static final boolean DEBUG = false;

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
  public <T extends CommonShell> ExerciseListWrapper<T> getExerciseIds(ExerciseListRequest request) {

    int projectID = getProjectID();
    if (projectID == -1) {

    }
    if (serverProps.isAMAS()) {
      ExerciseListWrapper<AmasExerciseImpl> amasExerciseIds = getAMASExerciseIds(request);
      return (ExerciseListWrapper<T>) amasExerciseIds; // TODO : how to do this without forcing it.
    }

    Collection<CommonExercise> exercises;

    logger.debug("getExerciseIds : (" + getLanguage() + ") " + "getting exercise ids for request " + request);

    try {
      boolean isUserListReq = request.getUserListID() != -1;
      UserList<CommonExercise> userListByID = isUserListReq ? db.getUserListByIDExercises(request.getUserListID(), getProjectID()) : null;

      if (request.getTypeToSelection().isEmpty()) {   // no unit-chapter filtering
        // get initial exercise set, either from a user list or predefined
        boolean predefExercises = userListByID == null;
        exercises = predefExercises ? getExercises() : getCommonExercises(userListByID);

        // now if there's a prefix, filter by prefix match
        int userID = request.getUserID();
        if (!request.getPrefix().isEmpty()) {
          // now do a trie over matches
          exercises = getExercisesForSearch(request.getPrefix(), exercises, predefExercises);
        }
        exercises = filterExercises(request, exercises);

        String role = request.getRole();

        if (!isUserListReq) {
          int i = markRecordedState(userID, role, exercises, request.isOnlyExamples());
        }

        // now sort : everything gets sorted the same way
        List<CommonExercise> commonExercises;
        if (request.isIncorrectFirstOrder()) {
          commonExercises = db.getResultDAO().getExercisesSortedIncorrectFirst(exercises, userID, getCollator());
        } else {
          commonExercises = new ArrayList<>(exercises);
          sortExercises(role, commonExercises);
        }

        return makeExerciseListWrapper(request, commonExercises);
      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        if (userListByID != null) {
          Collection<CommonExercise> exercisesForState = getExercisesFromUserListFiltered(request.getTypeToSelection(), userListByID);
          return getExerciseListWrapperForPrefix(request, filterExercises(request, exercisesForState));
        } else {
          return getExercisesForSelectionState(request);
        }
      }
    } catch (Exception e) {
      logger.warn("got " + e, e);
      logAndNotifyServerException(e);
      return new ExerciseListWrapper<T>();
    }
  }
//  private boolean didCheckLTS = false;

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
  private Collection<CommonExercise> getExercises() {
    long then = System.currentTimeMillis();
    Collection<CommonExercise> exercises = getExercisesForUser();
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("getExercises took " + (now - then) + " millis to get the raw exercise list for " + getLanguage());
    }

/*    if (!didCheckLTS) {
//      buildExerciseTrie();
      AudioFileHelper audioFileHelper = getAudioFileHelper();
      if (audioFileHelper == null) {
        logger.error("no audio file helper for " + getProject());
      } else {
        audioFileHelper.checkLTSAndCountPhones(exercises);
        didCheckLTS = true;
      }
    }*/

    now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list for " + getLanguage());
    }
    return exercises;
  }

  private Collection<CommonExercise> getExercisesForUser() {
    return db.getExercises(getProjectID());
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
   * @param role
   * @param exercises
   * @param onlyExample
   * @return
   * @see #getExerciseIds
   */
  private int markRecordedState(int userID,
                                String role,
                                Collection<? extends CommonShell> exercises,
                                boolean onlyExample) {
    int c = 0;
    if (role.equals(AudioType.RECORDER.toString())) {
      Collection<Integer> recordedForUser = onlyExample ?
          db.getAudioDAO().getRecordedExampleForUser(userID) : db.getAudioDAO().getRecordedExForUser(userID);
      //logger.debug("\tfound " + recordedForUser.size() + " recordings by " + userID + " only example " + onlyExample);
      for (CommonShell shell : exercises) {
        if (recordedForUser.contains(shell.getID())) {
          shell.setState(STATE.RECORDED);
          c++;
        }
      }
    }
    //else {
    //logger.debug("\tnot marking recorded for '" + role + "' and user " + userID);
    //}
    return c;
  }

  //  @Override
  public void logAndNotifyServerException(Exception e) {
    logAndNotifyServerException(e, "");
  }

  /**
   * TODO remove duplicate
   *
   * @param e
   * @param additionalMessage
   */
//  @Override
  public void logAndNotifyServerException(Exception e, String additionalMessage) {
    String message1 = e == null ? "null_ex" : e.getMessage() == null ? "null_msg" : e.getMessage();
    if (!message1.contains("Broken Pipe")) {
      String prefix = additionalMessage.isEmpty() ? "" : additionalMessage + "\n";
      String prefixedMessage = prefix + //"for " + pathHelper.getInstallPath() +
          (e != null ? " got " + "Server Exception : " + ExceptionUtils.getStackTrace(e) : "");
      String subject = "Server Exception on ";// + pathHelper.getInstallPath();
      sendEmail(subject, getInfo(prefixedMessage));

      logger.debug(getInfo(prefixedMessage));
    }
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
  private <T extends CommonShell> ExerciseListWrapper<T> getExercisesForSelectionState(ExerciseListRequest request) {
    Collection<CommonExercise> exercisesForState =
        getSectionHelper().getExercisesForSelectionState(request.getTypeToSelection());
    exercisesForState = filterExercises(request, exercisesForState);
    return getExerciseListWrapperForPrefix(request, exercisesForState);
  }


  /**
   * Always sort the result
   *
   * @param exercisesForState
   * @return
   * @see #getExerciseIds
   */
  private <T extends CommonShell> ExerciseListWrapper<T> getExerciseListWrapperForPrefix(ExerciseListRequest request,
                                                                                         Collection<CommonExercise> exercisesForState
  ) {
    String prefix = request.getPrefix();
    int userID = request.getUserID();
    String role = request.getRole();
    boolean onlyExamples = request.isOnlyExamples();
    boolean incorrectFirst = request.isIncorrectFirstOrder();

    boolean hasPrefix = !prefix.isEmpty();
    if (hasPrefix) {
      logger.debug("getExerciseListWrapperForPrefix userID " + userID + " prefix '" + prefix + "' role " + role);
    }

    int i = markRecordedState(userID, role, exercisesForState, onlyExamples);
    //logger.debug("marked " +i + " as recorded role " +role);

    if (hasPrefix) {
      ExerciseTrie<CommonExercise> trie = new ExerciseTrie<>(exercisesForState, getLanguage(), getSmallVocabDecoder());
      exercisesForState = trie.getExercises(prefix, getSmallVocabDecoder());
    }

    if (exercisesForState.isEmpty()) { // allow lookup by id
      CommonExercise exercise = getExercise(prefix, userID, incorrectFirst);
      if (exercise != null) exercisesForState = Collections.singletonList(exercise);
    }
    // why copy???
    List<CommonExercise> copy;

    if (incorrectFirst) {
      copy = db.getResultDAO().getExercisesSortedIncorrectFirst(exercisesForState, userID, getCollator());
    } else {
      copy = new ArrayList<>(exercisesForState);
      sortExercises(role, copy);
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
    CommonExercise firstExercise = exercises.isEmpty() ? null : exercises.iterator().next();

    int reqID = request.getReqID();
    int userID = request.getUserID();
    String role = request.getRole();
    boolean onlyExamples = request.isOnlyExamples();

    if (!exercises.isEmpty()) {
      addAnnotationsAndAudio(userID, firstExercise, request.isIncorrectFirstOrder());

      // NOTE : not ensuring MP3s or OGG versions of WAV file.
      // ensureMP3s(firstExercise, pathHelper.getInstallPath());
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);

    //   logger.debug("makeExerciseListWrapper : userID " +userID + " Role is " + role);
    if (role.equals(AudioType.RECORDER.toString())) {
      markRecordedState((int) userID, role, exerciseShells, onlyExamples);
    } else if (role.equalsIgnoreCase(User.Permission.QUALITY_CONTROL.toString()) ||
        role.startsWith(AudioType.REVIEW.toString())) {
      getUserListManager().markState(exerciseShells);
    } else if (role.equals("markDefects")) {
      Collection<Integer> defectExercises = getUserListManager().getDefectExercises();
      int c = 0;
      for (CommonShell shell : exerciseShells) {
        if (defectExercises.contains(shell.getID())) {
          shell.setState(STATE.DEFECT);
          //    if (shell.getID().startsWith("50")) logger.info("adding defect to " +shell.getID() + " : " + shell.getState());
          c++;
        }
      }
    }

    // TODO : do this the right way vis-a-vis type safe collection...

    List<T> exerciseShells1 = (List<T>) exerciseShells;
    ExerciseListWrapper<T> exerciseListWrapper = new ExerciseListWrapper<T>(reqID, exerciseShells1, firstExercise);
    //logger.debug("returning " + exerciseListWrapper);
    return exerciseListWrapper;
  }

  private IUserListManager getUserListManager() {
    return db.getUserListManager();
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
      logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " + (now - then) + " millis to attach audio to exercise " + oldID);
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

    db.getResultDAO().attachScoreHistory(userID, firstExercise, isFlashcardReq);

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
    db.getAudioDAO().attachAudioToExercise(firstExercise, db.getLanguage(firstExercise));
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
   * @return
   */
  private Collection<CommonExercise> filterExercises(ExerciseListRequest request,
                                                     Collection<CommonExercise> exercises) {
    exercises = filterByUnrecorded(request, exercises);
    if (request.isOnlyWithAudioAnno()) {
      exercises = filterByOnlyAudioAnno(request.isOnlyWithAudioAnno(), exercises);
    }
    if (request.isOnlyDefaultAudio()) {
      exercises = filterByOnlyDefaultAudio(request.isOnlyDefaultAudio(), exercises);
    }
    if (request.isOnlyUninspected()) {
      exercises = filterByUninspected(exercises);
    }
    return exercises;
  }

  /**
   * TODO : slow?
   *
   * @param role
   * @param commonExercises
   * @param <T>
   */
  private <T extends CommonShell> void sortExercises(String role, List<T> commonExercises) {
    int projectID = getProjectID();
    new ExerciseSorter(db.getTypeOrder(projectID))
        .getSortedByUnitThenAlpha(commonExercises,
            role.equals(AudioType.RECORDER.toString()));
  }

  private <T extends CommonShell> Collection<T> getExercisesForSearch(String prefix,
                                                                      Collection<T> exercises,
                                                                      boolean predefExercises) {
    ExerciseTrie<T> fullTrie = getProject().getFullTrie();
    return getExercisesForSearchWithTrie(prefix, exercises, predefExercises, fullTrie);
  }

  private <T extends CommonShell> Collection<T> getExercisesForSearchWithTrie(String prefix,
                                                                              Collection<T> exercises,
                                                                              boolean predefExercises,
                                                                              ExerciseTrie<T> fullTrie) {
    ExerciseTrie<T> trie = predefExercises ? fullTrie : new ExerciseTrie<T>(exercises, getLanguage(), getSmallVocabDecoder());
    exercises = trie.getExercises(prefix, getSmallVocabDecoder());

    if (exercises.isEmpty()) { // allow lookup by id
      int exid = 0;
      if (!prefix.isEmpty()) {
        try {
          exid = Integer.parseInt(prefix);
        } catch (NumberFormatException e) {
          logger.info("getExercisesForSearchWithTrie can't parse search number '" + prefix + "'");
        }
      }
      T exercise = getExercise(exid,/* userID, */false);
      if (exercise != null) exercises = Collections.singletonList(exercise);
    }
    return exercises;
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
   * @return exercises missing audio, what we want to record
   * @paramx userID                   exercise not recorded by this user and matching the user's gender
   * @paramx onlyUnrecordedByMyGender do we filter by gender
   * @paramx onlyExamples             only example audio
   * @see #getExerciseIds
   * @see #getExercisesForSelectionState
   */
  private Collection<CommonExercise> filterByUnrecorded(
      ExerciseListRequest request,
      Collection<CommonExercise> exercises) {

    // boolean onlyUnrecordedByMyGender = request.isOnlyUnrecordedByMe();
    boolean onlyExamples = request.isOnlyExamples();

    if (request.isOnlyUnrecordedByMe()) {
      int userID = request.getUserID();
      logger.debug("filterByUnrecorded : for " + userID + " only by same gender " + //onlyUnrecordedByMyGender +
          " examples only " + onlyExamples + " from " + exercises.size());

      Map<Integer, String> exToTranscript = new HashMap<>();
      Map<Integer, String> exToContextTranscript = new HashMap<>();

      for (CommonExercise shell : exercises) {
        exToTranscript.put(shell.getID(), shell.getForeignLanguage());
        String context = shell.hasContext() ? shell.getDirectlyRelated().iterator().next().getForeignLanguage() : null;
        if (context != null && !context.isEmpty()) {
          exToContextTranscript.put(shell.getID(), context);
        }
      }

      Collection<Integer> recordedBySameGender = onlyExamples ?
          db.getAudioDAO().getWithContext(userID, exToContextTranscript) :
          db.getAudioDAO().getRecordedBy(userID, exToTranscript);

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
    } else {
      if (onlyExamples) {
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
      } else {
        return exercises;
      }
    }
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
  private Collection<CommonExercise> filterByOnlyAudioAnno(boolean onlyAudioAnno,
                                                           Collection<CommonExercise> exercises) {
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

  private Collection<CommonExercise> filterByOnlyDefaultAudio(boolean onlyDefault,
                                                              Collection<CommonExercise> exercises) {
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
  private Collection<CommonExercise> filterByUninspected(Collection<CommonExercise> exercises) {
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
  private <T extends CommonShell> Collection<T> getExercisesFromUserListFiltered(Map<String, Collection<String>> typeToSelection,
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
        if (!request.getPrefix().isEmpty()) {
          // now do a trie over matches
          exercises = getExercisesForSearchWithTrie(request.getPrefix(), exercises, true, amasFullTrie);
        }
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

        return new ExerciseListWrapper<AmasExerciseImpl>(reqID, new ArrayList<>(exercises), null);
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

    long then = System.currentTimeMillis();
    Collection<CommonExercise> exercises = getExercises();

    long then2 = System.currentTimeMillis();

    CommonExercise byID = db.getCustomOrPredefExercise(projectID, exid);

    long now = System.currentTimeMillis();
    String language = getLanguage();
    if (now - then2 > WARN_DUR) {
      logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to find exercise " +
          exid + " for " + userID);
    }

    if (byID == null) {
      logger.error("getExercise : huh? couldn't find exercise with id '" + exid + "' when examining " +
          exercises.size() + " items");
    } else {
      logger.debug("getExercise : (" + language +
              ") project " + projectID +
              " find exercise " + exid + " for " + userID + " : " + byID
          //+"\n\tcontext" + byID.getDirectlyRelated()
      );
      then2 = System.currentTimeMillis();
      addAnnotationsAndAudio(userID, byID, isFlashcardReq);
      now = System.currentTimeMillis();
      if (now - then2 > WARN_DUR) {
        logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to add annotations to " +
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
    checkPerformance(exid, then);

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

  private <T extends Shell> T getExercise(String exid, int userID, boolean isFlashcardReq) {
    int exid1 = -1;
    try {
      exid1 = Integer.parseInt(exid);
    } catch (NumberFormatException e) {
      logger.warn("can't parse '" + exid + "'");
    }
    return getExercise(exid1, isFlashcardReq);
  }


  /**
   * @param id
   * @param then
   * @see #getExercise(String, int, boolean)
   */
  private void checkPerformance(int id, long then) {
    long now;
    now = System.currentTimeMillis();
    long diff = now - then;
    String language = getLanguage();

    String message = "getExercise : (" + language + ") took " + diff + " millis to get exercise " + id;// + " : " + threadInfo;
    if (diff > SLOW_EXERCISE_EMAIL) {
      ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
      String threadInfo = threadGroup.getName() + " = " + threadGroup.activeCount();
      logger.error(message + " thread count " + threadInfo);
      sendEmailWhenSlow(id, language, diff, threadInfo);
    } else if (diff > 1000) {
      logger.warn(message);
    } else if (diff > 15) {
      logger.debug(message);
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

  private List<AmasExerciseImpl> getAMASExercises() {
    logger.info("get getAMASExercises -------");
    long then = System.currentTimeMillis();
    List<AmasExerciseImpl> exercises = db.getAMASExercises();
    if (amasFullTrie == null) {
      amasFullTrie = new ExerciseTrie<AmasExerciseImpl>(exercises, getOldLanguage(), getSmallVocabDecoder());
    }

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

  /**
   * TODO : remove duplicate
   *
   * @return
   */
/*
  private AudioFileHelper getAudioFileHelper() {
    if (serverProps.isAMAS()) {
      return audioFileHelper;
    } else {
      Project project = getProject();
      if (project == null) {
        logger.warn("getAudioFileHelper no current project???");
        return null;
      }
      return project.getAudioFileHelper();
    }
  }
*/

/*
  private void sendEmail(String subject, String prefixedMessage) {
    getMailSupport().email(serverProps.getEmailAddress(), subject, prefixedMessage);
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }
*/
  private String getOldLanguage() {
    return serverProps.getLanguage();
  }
}