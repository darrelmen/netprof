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

package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.SimpleImageWriter;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.ScoringAudioPanel;
import mitll.langtest.server.amas.QuizCorrect;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.autocrt.AutoCRT;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.database.security.UserSecurityManager;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.ContextPractice;
import mitll.langtest.shared.ResultAndTotal;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.answer.Answer;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.SlimProject;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;

/**
 * Supports all the database interactions.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/7/12
 * Time: 5:49 PM
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase, LogAndNotify {
  private static final Logger logger = Logger.getLogger(LangTestDatabaseImpl.class);
  public static final String DATABASE_REFERENCE = "databaseReference";
  @Deprecated
  public static final String AUDIO_FILE_HELPER_REFERENCE = "audioFileHelperReference";

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final int MP3_LENGTH = MP3.length();

  private static final int SLOW_EXERCISE_EMAIL = 2000;
  private static final int MAX = 30;
  private static final int SLOW_MILLIS = 40;
  private static final int WARN_DUR = 100;
  private static final String WAV1 = "wav";

  private static final boolean WARN_MISSING_FILE = true;

  private DatabaseImpl db;

  /**
   * Put inside a Project
   */
  @Deprecated
  AudioFileHelper audioFileHelper;
  private String relativeConfigDir;
  private String configDir;
  private ServerProperties serverProps;
  private AudioConversion audioConversion;
  private PathHelper pathHelper;
  //private boolean stopOggCheck = false;

  /**
   * TODO : somehow make this typesafe
   *
   * @see #getExercises()
   */
  /**
   * Put inside a Project
   */
//  @Deprecated
//  private ExerciseTrie fullTrie = null;
  private ExerciseTrie<AmasExerciseImpl> amasFullTrie = null;

  private static final boolean DEBUG = false;
  private UserSecurityManager securityManager;

  /**
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void service(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
    try {
      super.service(request, response);
    } catch (ServletException | IOException e) {
      logAndNotifyServerException(e);
      throw e;
    } catch (Exception eee) {
      logAndNotifyServerException(eee);
      throw new ServletException("rethrow exception", eee);
    }
  }

  @Override
  public void logAndNotifyServerException(Exception e) {
    logAndNotifyServerException(e, "");
  }

  @Override
  public void logAndNotifyServerException(Exception e, String additionalMessage) {
    String message1 = e == null ? "null_ex" : e.getMessage() == null ? "null_msg" : e.getMessage();
    if (!message1.contains("Broken Pipe")) {
      String prefix = additionalMessage.isEmpty() ? "" : additionalMessage + "\n";
      String prefixedMessage = prefix + "for " + pathHelper.getInstallPath() +
          (e != null ? " got " + "Server Exception : " + ExceptionUtils.getStackTrace(e) : "");
      String subject = "Server Exception on " + pathHelper.getInstallPath();
      sendEmail(subject, getInfo(prefixedMessage));

      logger.debug(getInfo(prefixedMessage));
    }
  }

  private void sendEmail(String subject, String prefixedMessage) {
    getMailSupport().email(serverProps.getEmailAddress(), subject, prefixedMessage);
  }

  /**
   * Save transmission bandwidth - don't send a list of fully populated items - just send enough to populate a list
   *
   * @param exercises
   * @return
   * @see #makeExerciseListWrapper
   */
  private <T extends CommonShell> List<CommonShell> getExerciseShells(Collection<? extends CommonExercise> exercises) {
    List<CommonShell> ids = new ArrayList<>();
    for (CommonExercise e : exercises) {
//      logger.info("got " +e.getOldID() + " mean " + e.getMeaning() + " eng " + e.getEnglish() + " fl " + e.getForeignLanguage());
      ids.add(e.getShell());
    }
    return ids;
  }

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
          exercises = getExercisesForSearchWithTrie(request.getPrefix(), request.getUserID(), exercises, true, amasFullTrie);
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
  public <T extends CommonShell> ExerciseListWrapper<T> getExerciseIds(
      ExerciseListRequest request
  ) {
    if (serverProps.isAMAS()) {
      ExerciseListWrapper<AmasExerciseImpl> amasExerciseIds = getAMASExerciseIds(request);
      return (ExerciseListWrapper<T>) amasExerciseIds; // TODO : how to do this without forcing it.
    }

    Collection<CommonExercise> exercises;

    logger.debug("getExerciseIds : (" + getLanguage() + ") " +
        "getting exercise ids for " +
        " config " + relativeConfigDir + " request " + request);

    try {
      UserList userListByID = request.getUserListID() != -1 ? db.getUserListByID(request.getUserListID(), getProjectID()) : null;

      if (request.getTypeToSelection().isEmpty()) {   // no unit-chapter filtering
        // get initial exercise set, either from a user list or predefined
        boolean predefExercises = userListByID == null;
        exercises = predefExercises ? getExercises() : getCommonExercises(userListByID);

        // now if there's a prefix, filter by prefix match
        int userID = request.getUserID();
        if (!request.getPrefix().isEmpty()) {
          // now do a trie over matches
          exercises = getExercisesForSearch(request.getPrefix(), userID, exercises, predefExercises);
        }
        exercises = filterExercises(request, exercises);

        String role = request.getRole();
        int i = markRecordedState(userID, role, exercises, request.isOnlyExamples());

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

  Collection<CommonExercise> filterExercises(ExerciseListRequest request,
                                             Collection<CommonExercise> exercises) {
    exercises = filterByUnrecorded(request, exercises);
    exercises = filterByOnlyAudioAnno(request.isOnlyWithAudioAnno(), exercises);
    exercises = filterByOnlyDefaultAudio(request.isOnlyDefaultAudio(), exercises);

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
    new ExerciseSorter(db.getTypeOrder(getProjectID())).getSortedByUnitThenAlpha(commonExercises,
        role.equals(AudioType.RECORDER.toString()));
  }

  private <T extends CommonShell> Collection<T> getExercisesForSearch(String prefix, int userID, Collection<T> exercises,
                                                                      boolean predefExercises) {
    ExerciseTrie<T> fullTrie = getProject().getFullTrie();
    return getExercisesForSearchWithTrie(prefix, userID, exercises, predefExercises, fullTrie);
  }

  private <T extends CommonShell> Collection<T> getExercisesForSearchWithTrie(String prefix,
                                                                              int userID,
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
          logger.info("can't parse search number '" + prefix + "'");
        }
      }
      T exercise = getExercise(exid, userID, false);
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
      Collection<Integer> recordedBySameGender = onlyExamples ?
          db.getAudioDAO().getWithContext(userID) :
          db.getAudioDAO().getRecordedBy(userID);

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

  public QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection,
                                              int userID,
                                              Collection<Integer> exids) {
    return new QuizCorrect(db).getScoresForUser(typeToSection, userID, exids, getProjectID());
  }

  @Override
  public void addStudentAnswer(long resultID, boolean correct) {
    db.getAnswerDAO().addUserScore((int) resultID, correct ? 1.0f : 0.0f);
  }

  /**
   * TODO : put this back
   *
   * @param audioContext
   * @param answer
   * @param timeSpent
   * @param typeToSection
   * @return
   * @see mitll.langtest.client.amas.TextResponse#getScoreForGuess
   */
  public Answer getScoreForAnswer(AudioContext audioContext, String answer,
                                  long timeSpent,
                                  Map<String, Collection<String>> typeToSection) {
    // AutoCRT.CRTScores scoreForAnswer1 = audioFileHelper.getScoreForAnswer(exercise, questionID, answer);
    AutoCRT.CRTScores scoreForAnswer1 = new AutoCRT.CRTScores();
    double scoreForAnswer = serverProps.useMiraClassifier() ? scoreForAnswer1.getNewScore() : scoreForAnswer1.getOldScore();

    String session = "";// getLatestSession(typeToSection, userID);
    //  logger.warn("getScoreForAnswer user " + userID + " ex " + exercise.getOldID() + " qid " +questionID + " type " +typeToSection + " session " + session);
    boolean correct = scoreForAnswer > 0.5;
    long resultID = db.getAnswerDAO().addTextAnswer(audioContext,
        answer,
        correct,
        (float) scoreForAnswer, (float) scoreForAnswer, session, timeSpent);

    Answer answer1 = new Answer(scoreForAnswer, correct, resultID);
    return answer1;
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
  private int markRecordedState(int userID, String role, Collection<? extends CommonShell> exercises,
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

  private SectionHelper<CommonExercise> getSectionHelper() {
    return db.getSectionHelper(getProjectID());
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
      ExerciseTrie<CommonExercise> trie = new ExerciseTrie<CommonExercise>(exercisesForState, getLanguage(), getSmallVocabDecoder());
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

    if (firstExercise != null) {
      addAnnotationsAndAudio(userID, firstExercise, request.isIncorrectFirstOrder());
      ensureMP3s(firstExercise, pathHelper.getInstallPath());
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);

    //   logger.debug("makeExerciseListWrapper : userID " +userID + " Role is " + role);
    if (role.equals(AudioType.RECORDER.toString())) {
      markRecordedState((int) userID, role, exerciseShells, onlyExamples);
    } else if (role.equalsIgnoreCase(User.Permission.QUALITY_CONTROL.toString()) ||
        role.startsWith(AudioType.REVIEW.toString())) {
      getUserListManager().markState(exerciseShells);
    }

    // TODO : do this the right way vis-a-vis type safe collection...

    List<T> exerciseShells1 = (List<T>) exerciseShells;

    ExerciseListWrapper<T> exerciseListWrapper = new ExerciseListWrapper<T>(reqID, exerciseShells1, firstExercise);
    //logger.debug("returning " + exerciseListWrapper);
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
   * @param firstExercise
   * @see #addAnnotationsAndAudio(int, mitll.langtest.shared.exercise.CommonExercise, boolean)
   */
  private void attachAudio(CommonExercise firstExercise) {
    db.getAudioDAO().attachAudio(firstExercise, pathHelper.getInstallPath(), relativeConfigDir);
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

  private <T extends Shell> T getExercise(String exid, int userID, boolean isFlashcardReq) {
    int exid1 = -1;
    try {
      exid1 = Integer.parseInt(exid);
    } catch (NumberFormatException e) {
      logger.warn("can't parse " + exid);
    }
    return getExercise(exid1, userID, isFlashcardReq);
  }

  /**
   * Joins with annotation data when doing QC.
   *
   * @param exid
   * @param userID
   * @param isFlashcardReq
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   * @see mitll.langtest.client.list.ExerciseList#goGetNextAndCacheIt
   * @see mitll.langtest.client.analysis.PlayAudio#playLast
   */
  public <T extends Shell> T getExercise(int exid, int userID, boolean isFlashcardReq) {
    if (serverProps.isAMAS()) { // TODO : HOW TO AVOID CAST???
      return (T) db.getAMASExercise(exid);
    }

    long then = System.currentTimeMillis();
    Collection<CommonExercise> exercises = getExercises();

    long then2 = System.currentTimeMillis();

    CommonExercise byID = db.getCustomOrPredefExercise(getProjectID(), exid);

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
      logger.debug("getExercise : find exercise " + exid + " for " + userID + " : " + byID
          //+"\n\tcontext" + byID.getDirectlyRelated()
      );
      then2 = System.currentTimeMillis();
      addAnnotationsAndAudio(userID, byID, isFlashcardReq);
      now = System.currentTimeMillis();
      if (now - then2 > WARN_DUR) {
        logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to add annotations to " +
            "exercise " + exid + " for " + userID);
      }
      then2 = System.currentTimeMillis();

      //logger.debug("getExercise : returning " + byID);
      ensureMP3s(byID, pathHelper.getInstallPath());

      if (DEBUG) {
        for (AudioAttribute audioAttribute : byID.getAudioAttributes())
          logger.debug("\t addAnnotationsAndAudio after ensure mp3 ex " + byID.getID() + " audio " + audioAttribute);
        logger.debug("getExercise : returning " + byID);
      }

      now = System.currentTimeMillis();
      if (now - then2 > WARN_DUR) {
        if (WARN_MISSING_FILE) {
          logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis " +
              "to ensure there are mp3s for exercise " + exid + " for " + userID);
        }
      }
    }
    checkPerformance(exid, then);

    if (byID != null) {
      //logger.debug("returning (" + language + ") exercise " + byID.getOldID() + " : " + byID);
    } else {
      logger.warn(getLanguage() + " : couldn't find exercise with id '" + exid + "'");
    }
    // return byID;
    // TODO : why doesn't this work?
    return (T) byID;
  }

  protected int getMostRecentProjectByUser(int id) {
    return db.getUserProjectDAO().mostRecentByUser(id);
  }

  /**
   * @param id
   * @param then
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

  /**
   * @param byID
   * @see #addAnnotationsAndAudio(int, mitll.langtest.shared.exercise.CommonExercise, boolean)
   */
  private void addAnnotations(CommonExercise byID) {
    getUserListManager().addAnnotations(byID);
  }

  /**
   * @param byID
   * @param parentDir
   * @seex LoadTesting#getExercise
   * @see #makeExerciseListWrapper
   */
  private void ensureMP3s(CommonExercise byID, String parentDir) {
    Collection<AudioAttribute> audioAttributes = byID.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes) {
      if (!ensureMP3(audioAttribute.getAudioRef(), byID.getForeignLanguage(), audioAttribute.getUser().getUserID(), parentDir)) {
//        if (byID.getOldID().equals("1310")) {
//          logger.warn("ensureMP3 : can't find " + audioAttribute + " under " + parentDir + " for " + byID);
//        }
        audioAttribute.setAudioRef(AudioConversion.FILE_MISSING);
      }
    }

//    if (audioAttributes.isEmpty() && byID.getOldID().equals("1310")) {
//      logger.warn("ensureMP3s : (" + getLanguage() + ") no ref audio for " + byID);
//    }
  }

  private List<AmasExerciseImpl> getAMASExercises() {
    logger.info("get getAMASExercises -------");
    long then = System.currentTimeMillis();
    List<AmasExerciseImpl> exercises = db.getAMASExercises();
    if (amasFullTrie == null) {
      amasFullTrie = new ExerciseTrie<AmasExerciseImpl>(exercises, getOldLanguage(), getSmallVocabDecoder());
    }

    if (getServletContext().getAttribute(AUDIO_FILE_HELPER_REFERENCE) == null) {
      shareAudioFileHelper(getServletContext());
    }
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list for " + getOldLanguage());
    }
    return exercises;
  }

  private String getOldLanguage() {
    return serverProps.getLanguage();
  }

  private SmallVocabDecoder getSmallVocabDecoder() {
    return getAudioFileHelper().getSmallVocabDecoder();
  }

  boolean didCheckLTS = false;

  /**
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
    if (!didCheckLTS) {
//      buildExerciseTrie();
      AudioFileHelper audioFileHelper = getAudioFileHelper();

      if (audioFileHelper == null) logger.error("no audio file helper for " + getProject());
      else {
        audioFileHelper.checkLTSAndCountPhones(exercises);
        shareAudioFileHelper(getServletContext());
        didCheckLTS = true;
      }
    }

    now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list for " + getLanguage());
    }
    return exercises;
  }

  private Collection<CommonExercise> getExercisesForUser() {
    return db.getExercises(getProjectID());
  }

  /**
   * @paramx <T>
   * @see #getExercises()
   */
/*  private <T extends CommonShell> void buildExerciseTrie() {
    logger.info("db " + db);
    logger.info("audioFileHelper " + getAudioFileHelper());
    SmallVocabDecoder smallVocabDecoder = getSmallVocabDecoder();
    fullTrie = new ExerciseTrie<CommonExercise>(getExercisesForUser(), getLanguage(), smallVocabDecoder);
  }*/
  public ContextPractice getContextPractice() {
    return db.getContextPractice();
  }

  @Override
  public void reloadExercises() {
    logger.info("reloadExercises");
    db.reloadExercises(getProjectID());
  }

  /**
   * @param wavFile
   * @param title
   * @param artist
   * @return true if mp3 file exists
   * @see #ensureMP3s(CommonExercise, String)
   * @see #writeAudioFile
   */
  private boolean ensureMP3(String wavFile, String title, String artist) {
    return ensureMP3(wavFile, title, artist, pathHelper.getInstallPath());
  }

  // int spew = 0;

  private boolean ensureMP3(String wavFile, String title, String artist, String parent) {
    if (wavFile != null) {
      if (!audioConversion.exists(wavFile, parent)) {
        //if (WARN_MISSING_FILE) {
        //   logger.warn("ensureMP3 : can't find " + wavFile + " under " + parent + " trying config... ");
        // }
        parent = configDir;
      }
/*      if (!audioConversion.exists(wavFile, parent)) {// && wavFile.contains("1310")) {
        if (WARN_MISSING_FILE && spew++ < 10) {
          logger.error("ensureMP3 : can't find " + wavFile + " under " + parent + " for " + title + " " + artist);
        }
      }*/

      String s = audioConversion.ensureWriteMP3(wavFile, parent, false, title, artist);
      boolean isMissing = s.equals(AudioConversion.FILE_MISSING);
/*      if (isMissing && wavFile.contains("1310")) {
        logger.error("ensureMP3 : can't find " + wavFile + " under " + parent + " for " + title + " " + artist);
      }*/
      return !isMissing;
    }
    return false;
  }

  /**
   * Get an image of desired dimensions for the audio file - only for Waveform and spectrogram.
   * Also returns the audio file duration -- so we can deal with the difference in length between mp3 and wav
   * versions of the same audio file.  (The browser soundmanager plays mp3 and reports audio offsets into
   * the mp3 file, but all the images are generated from the shorter wav file.)
   * <p>
   * TODO : Worrying about absolute vs relative path is maddening.  Must be a better way!
   *
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param width
   * @param height
   * @param exerciseID
   * @return path to an image file
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio
   */
  public ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height,
                                            String exerciseID) {
    if (audioFile.isEmpty()) logger.error("huh? audio file is empty for req id " + reqid + " exid " + exerciseID);

    SimpleImageWriter imageWriter = new SimpleImageWriter();

    String wavAudioFile = getWavAudioFile(audioFile);
    File testFile = new File(wavAudioFile);
    if (!testFile.exists() || testFile.length() == 0) {
      if (testFile.length() == 0) logger.error("getImageForAudioFile : huh? " + wavAudioFile + " is empty???");
      return new ImageResponse();
    }
    ImageType imageType1 =
        imageType.equalsIgnoreCase(ImageType.WAVEFORM.toString()) ? ImageType.WAVEFORM :
            imageType.equalsIgnoreCase(ImageType.SPECTROGRAM.toString()) ? ImageType.SPECTROGRAM : null;
    if (imageType1 == null) {
      logger.error("getImageForAudioFile '" + imageType + "' is unknown?");
      return new ImageResponse(); // success = false!
    }
    String imageOutDir = pathHelper.getImageOutDir();

    if (DEBUG) {
      logger.debug("getImageForAudioFile : getting images (" + width + " x " + height + ") (" + reqid + ") type " + imageType +
          " for " + wavAudioFile + "");
    }

    long then = System.currentTimeMillis();

    String absolutePathToImage = imageWriter.writeImage(wavAudioFile, getAbsoluteFile(imageOutDir).getAbsolutePath(),
        width, height, imageType1, exerciseID);
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 100) {
      logger.debug("getImageForAudioFile : got images (" + width + " x " + height + ") (" + reqid + ") type " + imageType +
          " for " + wavAudioFile + " took " + diff + " millis");
    }
    String installPath = pathHelper.getInstallPath();

    String relativeImagePath = absolutePathToImage;
    if (absolutePathToImage.startsWith(installPath)) {
      relativeImagePath = absolutePathToImage.substring(installPath.length());
    } else {
      logger.error("getImageForAudioFile huh? file path " + absolutePathToImage + " doesn't start with " + installPath + "?");
    }

    relativeImagePath = pathHelper.ensureForwardSlashes(relativeImagePath);
    if (relativeImagePath.startsWith("/")) {
      relativeImagePath = relativeImagePath.substring(1);
    }
    String imageURL = relativeImagePath;
    double duration = new AudioCheck(serverProps).getDurationInSeconds(wavAudioFile);
    if (duration == 0) {
      logger.error("huh? " + wavAudioFile + " has zero duration???");
    }
    /*    logger.debug("for " + wavAudioFile + " type " + imageType + " rel path is " + relativeImagePath +
        " url " + imageURL + " duration " + duration);*/

    return new ImageResponse(reqid, imageURL, duration);
  }

  private String getWavAudioFile(String audioFile) {
    if (audioFile.endsWith("." + AudioTag.COMPRESSED_TYPE) || audioFile.endsWith(MP3)) {
      String wavFile = removeSuffix(audioFile) + WAV;
      File test = getAbsoluteFile(wavFile);
      audioFile = test.exists() ? test.getAbsolutePath() : getAudioFileHelper().getWavForMP3(audioFile);
    }

    return ensureWAV(audioFile);
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - MP3_LENGTH);
  }

  private String ensureWAV(String audioFile) {
    if (!audioFile.endsWith(WAV1)) {
      return audioFile.substring(0, audioFile.length() - MP3_LENGTH) + WAV;
    } else {
      return audioFile;
    }
  }

  /**
   * Get properties (first time called read properties file -- e.g. see war/config/levantine/config.properties).
   *
   * @return
   * @paramx userID
   * @see mitll.langtest.client.LangTest#onModuleLoad
   */
  @Override
  public StartupInfo getStartupInfo() {
    List<SlimProject> projectInfos = new ArrayList<>();
    if (db == null) {
      logger.info("no db yet...");
    } else {
      projectInfos = getNestedProjectInfo();
    }

    return new StartupInfo(serverProps.getProperties(), projectInfos, startupMessage);
  }

  /**
   * TODO : consider moving this into user service?
   * what if later an admin changes it while someone else is looking at it...
   * @return
   */
  private List<SlimProject> getNestedProjectInfo() {
    List<SlimProject> projectInfos = new ArrayList<>();

    Map<String, List<SlickProject>> langToProject = new TreeMap<>();
    Collection<SlickProject> all = db.getProjectDAO().getAll();
    logger.info("found " + all.size() + " projects");
    for (SlickProject project : all) {
      List<SlickProject> slimProjects = langToProject.get(project.language());
      if (slimProjects == null) langToProject.put(project.language(), slimProjects = new ArrayList<>());
      slimProjects.add(project);
    }

    logger.info("lang->project is " + langToProject);

    for (String lang : langToProject.keySet()) {
      List<SlickProject> slickProjects = langToProject.get(lang);
      SlickProject project = slickProjects.get(0);
      SlimProject parent = getProjectInfo(project);
      projectInfos.add(parent);

      if (slickProjects.size() > 1) {
        for (SlickProject slickProject : slickProjects) {
          parent.addChild(getProjectInfo(slickProject));
          logger.info("\t add child to " + parent);

        }
      }
    }

    return projectInfos;
  }

  private SlimProject getProjectInfo(SlickProject project) {
    return new SlimProject(project.id(), project.name(), project.language(), project.countrycode(), project.course());
  }

  /**
   * @param resultID
   * @param width
   * @param height
   * @return
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   */
  @Override
  public PretestScore getResultASRInfo(int resultID, int width, int height) {
    PretestScore asrScoreForAudio = null;
    try {
      Result result = db.getResultDAO().getResultByID(resultID);

      int exerciseID = result.getExerciseID();

      boolean isAMAS = serverProps.isAMAS();
      CommonShell exercise;
      String sentence = "";
      if (isAMAS) {
        exercise = db.getAMASExercise(exerciseID);
        sentence = exercise.getForeignLanguage();
      } else {
        CommonExercise exercise1 = db.getExercise(getProjectID(), exerciseID);
        exercise = exercise1;

        Collection<CommonExercise> directlyRelated = exercise1.getDirectlyRelated();
        sentence =
            result.getAudioType().isContext() && !directlyRelated.isEmpty() ?
                directlyRelated.iterator().next().getForeignLanguage() :
                exercise.getForeignLanguage();
      }


      // maintain backward compatibility - so we can show old recordings of ref audio for the context sentence
//      String sentence = isAMAS ? exercise.getForeignLanguage() :
//          (result.getAudioType().isContext()) ?
//              db.getExercise(exerciseID).getDirectlyRelated().iterator().next().getForeignLanguage() :
//              exercise.getForeignLanguage();

      if (exercise == null) {
        logger.warn(getLanguage() + " can't find exercise id " + exerciseID);
        return new PretestScore();
      } else {
        String audioFilePath = result.getAnswer();
        ensureMP3(audioFilePath, sentence, "" + result.getUserid());
        //logger.info("resultID " +resultID+ " temp dir " + tempDir.getAbsolutePath());
        asrScoreForAudio = getAudioFileHelper().getASRScoreForAudio(1,
            audioFilePath, sentence,
            width, height,
            true,  // make transcript images with colored segments
            false, // false = do alignment
            serverProps.useScoreCache(),
            "" + exerciseID, result, serverProps.usePhoneToDisplay(), false);
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    return asrScoreForAudio;
  }

  /**
   * So first we check and see if we've already done alignment for this audio (if reference audio), and if so, we grab the Result
   * object out of the result table and use it and it's json to generate the score info and transcript inmages.
   *
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @return
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   */
  public PretestScore getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence,
                                          int width, int height, boolean useScoreToColorBkg, int exerciseID) {
    return getPretestScore(reqid, (int) resultID, testAudioFile, sentence, width, height, useScoreToColorBkg, exerciseID, false);
  }

  /**
   * Be careful - we lookup audio file by .wav extension
   *
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @param usePhoneToDisplay
   * @return
   */
  private PretestScore getPretestScore(int reqid, int resultID, String testAudioFile, String sentence,
                                       int width, int height, boolean useScoreToColorBkg, int exerciseID,
                                       boolean usePhoneToDisplay) {
    if (testAudioFile.equals(AudioConversion.FILE_MISSING)) return new PretestScore(-1);
    long then = System.currentTimeMillis();

    String[] split = testAudioFile.split(File.separator);
    String answer = split[split.length - 1];
    String wavEndingAudio = answer.replaceAll(".mp3", ".wav").replaceAll(".ogg", ".wav");
    Result cachedResult = db.getRefResultDAO().getResult(exerciseID, wavEndingAudio);
    if (cachedResult != null) {
      if (DEBUG)
        logger.debug("getPretestScore Cache HIT  : align exercise id = " + exerciseID + " file " + answer +
            " found previous " + cachedResult.getUniqueID());
    } else {
      logger.debug("getPretestScore Cache MISS : align exercise id = " + exerciseID + " file " + answer);
    }

    boolean usePhoneToDisplay1 = usePhoneToDisplay || serverProps.usePhoneToDisplay();

    PretestScore asrScoreForAudio = getAudioFileHelper().getASRScoreForAudio(reqid, testAudioFile, sentence, width, height, useScoreToColorBkg,
        false, serverProps.useScoreCache(), "" + exerciseID, cachedResult, usePhoneToDisplay1, false);

    long timeToRunHydec = System.currentTimeMillis() - then;

    logger.debug("getPretestScore : scoring" +
        " file " + testAudioFile + " for " +
        " exid " + exerciseID +
        " sentence " + sentence.length() + " characters long : " +
        " score " + asrScoreForAudio.getHydecScore() +
        " took " + timeToRunHydec + " millis " +
        " usePhoneToDisplay " + usePhoneToDisplay1);

    if (resultID > -1 && cachedResult == null) { // alignment has two steps : 1) post the audio, then 2) do alignment
      db.rememberScore(resultID, asrScoreForAudio);
    }
    return asrScoreForAudio;
  }


  /**
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @return
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   */
  @Override
  public PretestScore getASRScoreForAudioPhonemes(int reqid, long resultID, String testAudioFile, String sentence,
                                                  int width, int height, boolean useScoreToColorBkg, int exerciseID) {
    return getPretestScore(reqid, (int) resultID, testAudioFile, sentence, width, height, useScoreToColorBkg, exerciseID, true);
  }

  @Override
  public void addRoundTrip(int resultID, int roundTrip) {
    db.getAnswerDAO().addRoundTrip(resultID, roundTrip);
  }

  // Users ---------------------


  /**
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation(String, String, String)
   */
  @Override
  public void addAnnotation(int exerciseID, String field, String status, String comment, int userID) {
    getUserListManager().addAnnotation(exerciseID, field, status, comment, userID);
  }

  /**
   * @param id
   * @param isCorrect
   * @param creatorID
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  public void markReviewed(int id, boolean isCorrect, int creatorID) {
    getUserListManager().markCorrectness(id, isCorrect, creatorID);
  }

  /**
   * @param exid
   * @param state
   * @param creatorID
   * @see mitll.langtest.client.qc.QCNPFExercise#markAttentionLL
   */
  public void markState(int exid, STATE state, int creatorID) {
    getUserListManager().markState(exid, state, creatorID);
  }

  /**
   * @param id
   * @param state
   * @param userID
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   */
/*  @Override
  public void setExerciseState(String id, STATE state, int userID) {
    getUserListManager().markState(id, state, userID);
  }*/

  /**
   * Can't check if it's valid if we don't have a model.
   *
   * @param foreign
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
   */
  @Override
  public boolean isValidForeignPhrase(String foreign) {
    return getAudioFileHelper().checkLTSOnForeignPhrase(foreign);
  }

  IUserListManager getUserListManager() {
    return db.getUserListManager();
  }

  /**
   * @param id
   * @return
   * @seex ReviewEditableExercise#confirmThenDeleteItem
   */
  public boolean deleteItem(int id) {
    boolean b = db.deleteItem(id, getProjectID());
    if (b) {
      // force rebuild of full trie
      getProject().buildExerciseTrie(db);
    }
    return b;
  }

  /**
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param hitID
   * @param device
   * @see mitll.langtest.client.instrumentation.ButtonFactory#logEvent
   */
  @Override
  public void logEvent(String id, String widgetType, String exid, String context, int userid, String hitID, String device) {
    try {
      db.logEvent(id, widgetType, exid, context, userid, device);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @return
   * @see mitll.langtest.client.instrumentation.EventTable#show
   */
  public List<Event> getEvents() {
    return db.getEventDAO().getAll(getProjectID());
  }

  /**
   * @param audioAttribute
   * @param exid
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   */
  @Override
  public void markAudioDefect(AudioAttribute audioAttribute, HasID exid) {
    logger.debug("markAudioDefect mark audio defect for " + exid + " on " + audioAttribute);
    //CommonExercise before = db.getCustomOrPredefExercise(exid);  // allow custom items to mask out non-custom items
    //int beforeNumAudio = before.getAudioAttributes().size();
    db.markAudioDefect(audioAttribute);

    CommonExercise byID = db.getCustomOrPredefExercise(getProjectID(), exid.getID());  // allow custom items to mask out non-custom items

    if (!byID.getMutableAudio().removeAudio(audioAttribute)) {
      String key = audioAttribute.getKey();
      logger.warn("huh? couldn't remove key '" + key +
          "' : " + audioAttribute + " from " + exid +
          " keys were " + byID.getAudioRefToAttr().keySet() + " contains " + byID.getAudioRefToAttr().containsKey(key));
    }
    /*   int afterNumAudio = byID.getAudioAttributes().size();
    if (afterNumAudio != beforeNumAudio - 1) {
      logger.error("\thuh? before there were " + beforeNumAudio + " but after there were " + afterNumAudio);
    }*/
  }

  /**
   * @param attr
   * @param isMale
   * @see mitll.langtest.client.qc.QCNPFExercise#getGenderGroup
   */
  @Override
  public void markGender(AudioAttribute attr, boolean isMale) {
    CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(getProjectID(), attr.getExid());
    int projid = -1;
    if (customOrPredefExercise == null) {
      logger.error("markGender can't find exercise id " + attr.getExid() + "?");
    } else {
      projid = customOrPredefExercise.getProjectID();
    }
    db.getAudioDAO().addOrUpdateUser(isMale ? BaseUserDAO.DEFAULT_MALE_ID : BaseUserDAO.DEFAULT_FEMALE_ID, projid, attr);

    int exid = attr.getExid();
    CommonExercise byID = db.getCustomOrPredefExercise(projid, exid);
    if (byID == null) {
      logger.error(getLanguage() + " : couldn't find exercise " + exid);
      logAndNotifyServerException(new Exception("couldn't find exercise " + exid));
    } else {
      byID.getAudioAttributes().clear();

//      logger.debug("re-attach " + attr + " given isMale " + isMale);

      attachAudio(byID);

/*
      String addr = Integer.toHexString(byID.hashCode());
      for (AudioAttribute audioAttribute : byID.getAudioAttributes()) {
        logger.debug("markGender 1 after gender change, now " + audioAttribute + " : " +audioAttribute.getUserid() + " on " + addr);
      }
*/

      db.getExerciseDAO(getProjectID()).addOverlay(byID);

/*      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(exid);
      String adrr3 = Integer.toHexString(customOrPredefExercise.hashCode());
      logger.info("markGender getting " + adrr3 + " : " + customOrPredefExercise);
      for (AudioAttribute audioAttribute : customOrPredefExercise.getAudioAttributes()) {
        logger.debug("markGender 2 after gender change, now " + audioAttribute + " : " +audioAttribute.getUserid() + " on "+ adrr3);
      }*/

    }
    getSectionHelper().refreshExercise(byID);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser
   */
  private User getUserBy(int id) {
    return db.getUserDAO().getUserWhere(id);
  }
  // Results ---------------------

  /**
   * TODO : consider doing offset/limit on database query.
   * TODO : super expensive on long lists
   * <p>
   * Sometimes we type faster than we can respond, so we can throw away stale requests.
   * <p>
   * Filter results by search criteria -- unit->value map (e.g. chapter=5), userid, and foreign language text
   *
   * @param req      - to echo back -- so that if we get an old request we can discard it
   * @param sortInfo - encoding which fields we want to sort, and ASC/DESC choice
   * @return
   * @see mitll.langtest.client.result.ResultManager#createProvider(int, com.google.gwt.user.cellview.client.CellTable)
   */
  @Override
  public ResultAndTotal getResults(int start, int end, String sortInfo, Map<String, String> unitToValue, int userid,
                                   String flText, int req) {
    List<MonitorResult> results = getResults(unitToValue, userid, flText);
    if (!results.isEmpty()) {
      Comparator<MonitorResult> comparator = results.get(0).getComparator(Arrays.asList(sortInfo.split(",")));
      try {
        Collections.sort(results, comparator);
      } catch (Exception e) {
        logger.error("Doing " + sortInfo + " " + unitToValue + " " + userid + " " + flText + " " + start + "-" + end +
            " Got " + e, e);
      }
    }
    int n = results.size();
    int min = Math.min(end, n);
    if (start > min) {
      logger.debug("original req from " + start + " to " + end);
      start = 0;
    }
    List<MonitorResult> resultList = results.subList(start, min);
    logger.info("getResults ensure compressed audio for " + resultList.size() + " items.");
    int projectID = getProjectID();
    for (MonitorResult result : resultList) {
      ensureCompressedAudio(result.getUserid(), db.getCustomOrPredefExercise(projectID, result.getExID()), result.getAnswer());
    }
    return new ResultAndTotal(new ArrayList<>(resultList), n, req);
  }

  @Override
  public int getNumResults() {
    return db.getResultDAO().getNumResults(getProjectID());
  }

  /**
   * TODO : don't fetch everything from the database if you don't have to.
   * Use offset and limit to restrict?
   *
   * @param unitToValue
   * @param userid
   * @param flText
   * @return
   * @see #getResults(int, int, String, java.util.Map, int, String, int)
   */
  private List<MonitorResult> getResults(Map<String, String> unitToValue, int userid, String flText) {
    logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText);
    int projectID = getProjectID();

    boolean isNumber = false;
    try {
      int i = Integer.parseInt(flText);
      isNumber = true;
    } catch (NumberFormatException e) {
    }
    if (isNumber) {
      int i = Integer.parseInt(flText);

      List<MonitorResult> monitorResultsByID = db.getMonitorResultsWithText(db.getResultDAO().getMonitorResultsByID(i));
      logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText + " returning " + monitorResultsByID.size() + " results...");
      return monitorResultsByID;
    }

    boolean filterByUser = userid > -1;

    Collection<MonitorResult> results = getMonitorResults();

    Trie<MonitorResult> trie;

    for (String type : db.getTypeOrder(projectID)) {
      if (unitToValue.containsKey(type)) {

        // logger.debug("getResults making trie for " + type);
        // make trie from results
        trie = new Trie<MonitorResult>();

        trie.startMakingNodes();
        for (MonitorResult result : results) {
          String s = result.getUnitToValue().get(type);
          if (s != null) {
            trie.addEntryToTrie(new ResultWrapper(s, result));
          }
        }
        trie.endMakingNodes();

        results = trie.getMatchesLC(unitToValue.get(type));
      }
    }

    if (filterByUser) { // asking for userid
      // make trie from results
      //      logger.debug("making trie for userid " + userid);

      trie = new Trie<MonitorResult>();
      trie.startMakingNodes();
      for (MonitorResult result : results) {
        trie.addEntryToTrie(new ResultWrapper(Long.toString(result.getUserid()), result));
      }
      trie.endMakingNodes();

      results = trie.getMatchesLC(Long.toString(userid));
    }

    // must be asking for text
    boolean filterByText = flText != null && !flText.isEmpty();
    if (filterByText) { // asking for text
      trie = new Trie<MonitorResult>();
      trie.startMakingNodes();
      //     logger.debug("searching over " + results.size());
      for (MonitorResult result : results) {
        String foreignText = result.getForeignText();
        if (foreignText != null) {
          trie.addEntryToTrie(new ResultWrapper(foreignText.trim(), result));
        }
      }
      trie.endMakingNodes();

      results = trie.getMatchesLC(flText);
    }
    logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText + " returning " + results.size() + " results...");
    return new ArrayList<>(results);
  }

  private Collection<MonitorResult> getMonitorResults() {
    return db.getMonitorResults(getProjectID());
  }

  /**
   * Respond to type ahead.
   *
   * @param unitToValue
   * @param userid
   * @param flText
   * @param which
   * @return
   * @see mitll.langtest.client.result.ResultManager#getTypeaheadUsing
   */
  @Override
  public Collection<String> getResultAlternatives(Map<String, String> unitToValue,
                                                  int userid,
                                                  String flText,
                                                  String which) {
    Collection<MonitorResult> results = getMonitorResults();

    logger.debug("getResultAlternatives request " + unitToValue + " userid=" + userid + " fl '" + flText + "' :'" + which + "'");

    Collection<String> matches = new TreeSet<>();
    Trie<MonitorResult> trie;

    for (String type : db.getTypeOrder(getProjectID())) {
      if (unitToValue.containsKey(type)) {

        //    logger.debug("getResultAlternatives making trie for " + type);
        // make trie from results
        trie = new Trie<MonitorResult>();

        trie.startMakingNodes();
        for (MonitorResult result : results) {
          String s = result.getUnitToValue().get(type);
          if (s != null) {
            trie.addEntryToTrie(new ResultWrapper(s, result));
          }
        }
        trie.endMakingNodes();

        String s = unitToValue.get(type);
        Collection<MonitorResult> matchesLC = trie.getMatchesLC(s);

        // stop!
        if (which.equals(type)) {
          //        logger.debug("\tmatch for " + type);

          boolean allInt = true;
          for (MonitorResult result : matchesLC) {
            String e = result.getUnitToValue().get(type);
            if (allInt) {
              try {
                Integer.parseInt(e);
              } catch (NumberFormatException e1) {
                allInt = false;
              }
            }
            matches.add(e);
          }

          if (allInt) {
            List<String> sorted = new ArrayList<String>(matches);
            Collections.sort(sorted, new Comparator<String>() {
              @Override
              public int compare(String o1, String o2) {
                return compareTwoMaybeInts(o1, o2);
              }
            });
            return sorted;
          }

          //          logger.debug("returning " + matches);

          return matches;
        } else {
          results = matchesLC;
        }
      }
    }

    if (userid > -1) { // asking for userid
      // make trie from results

      logger.debug("making trie for userid " + userid);

      trie = new Trie<MonitorResult>();
      trie.startMakingNodes();
      for (MonitorResult result : results) {
        trie.addEntryToTrie(new ResultWrapper(Long.toString(result.getUserid()), result));
      }
      trie.endMakingNodes();

      Set<Integer> imatches = new TreeSet<>();
      Collection<MonitorResult> matchesLC = trie.getMatchesLC(Long.toString(userid));

      // stop!
      if (which.equals(MonitorResult.USERID)) {
        for (MonitorResult result : matchesLC) {
          imatches.add(result.getUserid());
        }
        //logger.debug("returning " + imatches);

        for (Integer m : imatches) matches.add(Long.toString(m));
        matches = getLimitedSizeList(matches);
        return matches;
      } else {
        results = matchesLC;
      }
    }

    // must be asking for text
    trie = new Trie<MonitorResult>();
    trie.startMakingNodes();
    //logger.debug("text searching over " + results.size());
    for (MonitorResult result : results) {
      trie.addEntryToTrie(new ResultWrapper(result.getForeignText(), result));
      trie.addEntryToTrie(new ResultWrapper("" + result.getExID(), result));
    }
    trie.endMakingNodes();

    Collection<MonitorResult> matchesLC = trie.getMatchesLC(flText);
    //logger.debug("matchesLC for '" +flText+  "' " + matchesLC);

    boolean isNumber = false;
    try {
      Integer.parseInt(flText);
      isNumber = true;
    } catch (NumberFormatException e) {
    }

    if (isNumber) {
      for (MonitorResult result : matchesLC) {
        matches.add("" + result.getExID());
      }
    } else {
      for (MonitorResult result : matchesLC) {
        matches.add(result.getForeignText().trim());
      }
    }
    //logger.debug("returning text " + matches);

    return getLimitedSizeList(matches);
  }

  private Collection<String> getLimitedSizeList(Collection<String> matches) {
    if (matches.size() > MAX) {
      List<String> matches2 = new ArrayList<String>();
      int nn = 0;
      for (String match : matches) {
        if (nn++ < MAX) {
          matches2.add(match);
        }
      }
      matches = matches2;
    }
    return matches;
  }

  private int compareTwoMaybeInts(String id1, String id2) {
    int comp;
    try {   // this could be slow
      int i = Integer.parseInt(id1);
      int j = Integer.parseInt(id2);
      comp = i - j;
    } catch (NumberFormatException e) {
      comp = id1.compareTo(id2);
    }
    return comp;
  }

  private static class ResultWrapper implements TextEntityValue<MonitorResult> {
    private final String value;
    private final MonitorResult e;

    ResultWrapper(String value, MonitorResult e) {
      this.value = value;
      this.e = e;
    }

    @Override
    public MonitorResult getValue() {
      return e;
    }

    @Override
    public String getNormalizedValue() {
      return value;
    }

    public String toString() {
      return "result " + e.getExID() + " : " + value;
    }
  }


  /**
   * Record an answer entry in the database.<br></br>
   * Write the posted data to a wav and an mp3 file (since all the browser audio works with mp3).
   * <p>
   * A side effect is to set the first state to UNSET if it was APPROVED
   * and to set the second state (not really used right now) to RECORDED
   * <p>
   * <p>
   * Wade has observed that audio normalization really messes up the ASR -- silence doesn't appear as silence after you multiply
   * the signal.  Also, the user doesn't get feedback that their mic gain is too high/too low or that they
   * are speaking too softly or too loudly
   * <p>
   * Client references below:
   *
   * @param base64EncodedString generated by flash on the client
   * @param recordedWithFlash   mark if we recorded it using flash recorder or webrtc
   * @param deviceType
   * @param device
   * @param doFlashcard         true if called from practice (flashcard) and we want to do decode and not align
   * @param recordInResults     if true, record in results table -- only when recording in a learn or practice tab
   * @param addToAudioTable     if true, add to audio table -- only when recording reference audio for an item.
   * @param allowAlternates
   * @return AudioAnswer object with information about the audio on the server, including if audio is valid (not too short, etc.)
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   */
  @Override
  public AudioAnswer writeAudioFile(String base64EncodedString,
                                    AudioContext audioContext,

                                    boolean recordedWithFlash, String deviceType, String device,

                                    boolean doFlashcard,
                                    boolean recordInResults, boolean addToAudioTable,
                                    boolean allowAlternates) {
    int exerciseID = audioContext.getExid();

    logger.info("writeAudioFile got request " + audioContext + " payload " + base64EncodedString.length());

    boolean amas = serverProps.isAMAS();

    CommonExercise commonExercise = amas ? null : db.getCustomOrPredefExercise(getProjectID(), exerciseID);
    CommonShell exercise1 = amas ? db.getAMASExercise(exerciseID) : commonExercise;

    if (exercise1 == null) {
      logger.warn(getLanguage() + " : couldn't find exerciseID with id '" + exerciseID + "'");
    }
//		else {
//			logger.info("allow alternates " + allowAlternates + " " +exerciseID +
//					" exercise1 " + exercise1.getForeignLanguage() + " refs " + exercise1.getRefSentences());
//		}

    AnswerInfo.RecordingInfo recordingInfo = new AnswerInfo.RecordingInfo("", "", deviceType, device, recordedWithFlash);

    AudioAnswer audioAnswer = amas ?
        getAudioFileHelper().writeAMASAudioFile(base64EncodedString, db.getAMASExercise(exerciseID), audioContext, recordingInfo) :
        getAudioFileHelper().writeAudioFile(base64EncodedString,
            exercise1,
            audioContext, recordingInfo,
            recordInResults, doFlashcard, allowAlternates, addToAudioTable);

    int user = audioContext.getUserid();
    if (addToAudioTable && audioAnswer.isValid()) {
      audioAnswer.setAudioAttribute(addToAudioTable(user, audioContext.getAudioType(), commonExercise, exerciseID, audioAnswer));
    } //else {
    // So Wade has observed that this really messes up the ASR -- silence doesn't appear as silence after you multiply
    // the signal.  Also, the user doesn't get feedback that their mic gain is too high/too low or that they
    // are speaking too softly or too loudly.

    // normalizeLevel(audioAnswer);
    // }

    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + user + " " + exerciseID);
      logEvent("audioRecording", "writeAudioFile", "" + exerciseID, "Writing audio - got zero duration!", user, "unknown", device);
    } else {
      ensureCompressedEquivalent(user, exercise1, audioAnswer);
    }

    return audioAnswer;
  }

  private void ensureCompressedEquivalent(int user, CommonShell exercise1, AudioAnswer audioAnswer) {
    ensureCompressedAudio(user, exercise1, audioAnswer.getPath());
  }

  private void ensureCompressedAudio(int user, CommonShell exercise1, String path) {
    String foreignLanguage = exercise1 == null ? "unknown" : exercise1.getForeignLanguage();
    String userID = getUserID(user);
    if (userID == null) {
      logger.warn("ensureCompressedEquivalent huh? no user for " + user);
    }

    ensureMP3(path, foreignLanguage, userID);
  }

  private String getUserID(int user) {
    User userBy = getUserBy(user);
    return userBy == null ? "" + user : userBy.getUserID();
  }

  /**
   * A low overhead way of doing alignment.
   * <p>
   * Useful for conversational dialogs - Jennifer Melot's project.
   *
   * @param base64EncodedString
   * @param textToAlign
   * @param identifier
   * @param reqid
   * @param device
   * @return
   * @see mitll.langtest.client.scoring.SimplePostAudioRecordButton#postAudioFile(String)
   */
  @Override
  public AudioAnswer getAlignment(String base64EncodedString,
                                  String textToAlign,
                                  String identifier,
                                  int reqid, String device) {
    AudioAnswer audioAnswer = getAudioFileHelper().getAlignment(base64EncodedString, textToAlign, identifier, reqid,
        serverProps.usePhoneToDisplay());

    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + identifier);
      logEvent("audioRecording", "writeAudioFile", identifier, "Writing audio - got zero duration!", -1, "unknown", device);
    }
    return audioAnswer;
  }

  /**
   * Remember this audio as reference audio for this exercise, and possibly clear the APRROVED (inspected) state
   * on the exercise indicating it needs to be inspected again (we've added new audio).
   *
   * @param user        who recorded audio
   * @param audioType   regular or slow
   * @param exercise1   for which exercise - how could this be null?
   * @param exerciseID  perhaps sometimes we want to override the exercise id?
   * @param audioAnswer holds the path of the temporary recorded file
   * @return AudioAttribute that represents the audio that has been added to the exercise
   * @see #writeAudioFile
   */
  private AudioAttribute addToAudioTable(int user,
                                         AudioType audioType,
                                         CommonExercise exercise1,
                                         int exerciseID,
                                         AudioAnswer audioAnswer) {
    int idToUse = exercise1 == null ? exerciseID : exercise1.getID();
    int projid = exercise1 == null ? -1 : exercise1.getProjectID();
    String audioTranscript = getAudioTranscript(audioType, exercise1);

    String permanentAudioPath = new PathWriter().
        getPermanentAudioPath(pathHelper,
            getAbsoluteFile(audioAnswer.getPath()),
            getPermanentName(user, audioType), true, projid, idToUse, audioTranscript, getArtist(user), serverProps);

    AudioAttribute audioAttribute =
        db.getAudioDAO().addOrUpdate(user, idToUse, projid, audioType, permanentAudioPath, System.currentTimeMillis(),
            audioAnswer.getDurationInMillis(), audioTranscript);
    audioAnswer.setPath(audioAttribute.getAudioRef());
    logger.debug("addToAudioTable user " + user + " ex " + exerciseID + " for " + audioType + " audio answer has " +
        audioAttribute);

    // what state should we mark recorded audio?
    setExerciseState(idToUse, user, exercise1);
    return audioAttribute;
  }

  private File getAbsoluteFile(String path) {
    return pathHelper.getAbsoluteFile(path);
  }

  private String getAudioTranscript(AudioType audioType, CommonExercise exercise1) {
    return exercise1 == null ? "" :
        audioType.equals(AudioAttribute.CONTEXT_AUDIO_TYPE) ? exercise1.getContext() : exercise1.getForeignLanguage();
  }

  private String getPermanentName(int user, AudioType audioType) {
    return audioType.toString() + "_" + System.currentTimeMillis() + "_by_" + user + ".wav";
  }

  private String getArtist(int user) {
    User userWhere = db.getUserDAO().getUserWhere(user);
    return userWhere == null ? "" + user : userWhere.getUserID();
  }

  /**
   * Only change APPROVED to UNSET.
   *
   * @param exercise
   * @param user
   * @param exercise1
   */
  private void setExerciseState(int exercise, int user, Shell exercise1) {
    if (exercise1 != null) {
      IUserListManager userListManager = getUserListManager();
      STATE currentState = userListManager.getCurrentState(exercise);
      if (currentState == STATE.APPROVED) { // clear approved on new audio -- we need to review it again
        userListManager.setState(exercise1, STATE.UNSET, user);
      }
      userListManager.setSecondState(exercise1, STATE.RECORDED, user);
    }
  }

  /**
   * Filter out the default audio recordings...
   *
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doMaleFemale
   */
  @Override
  public Map<String, Float> getMaleFemaleProgress() {
    return db.getMaleFemaleProgress(getProjectID());
  }


  /**
   * @param userid         who's asking?
   * @param ids            items the user has actually practiced/recorded audio for
   * @param latestResultID
   * @param typeToSection  indicates the unit and chapter(s) we're asking about
   * @param userListID     if we're asking about a list and not predef items
   * @return
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  @Override
  public AVPScoreReport getUserHistoryForList(int userid,
                                              Collection<Integer> ids,
                                              long latestResultID,
                                              Map<String, Collection<String>> typeToSection, long userListID) {
    //logger.debug("getUserHistoryForList " + userid + " and " + ids + " type to section " + typeToSection);
    UserList<CommonShell> userListByID = userListID != -1 ? db.getUserListByID(userListID, getProjectID()) : null;
    List<Integer> allIDs = new ArrayList<>();
    Map<Integer, CollationKey> idToKey = new HashMap<>();

    Collator collator = getCollator();
    if (userListByID != null) {
      for (CommonShell exercise : userListByID.getExercises()) {
        populateCollatorMap(allIDs, idToKey, collator, exercise);
      }
    } else {
      Collection<CommonExercise> exercisesForState = (typeToSection == null || typeToSection.isEmpty()) ? getExercises() :
          getSectionHelper().getExercisesForSelectionState(typeToSection);

      for (CommonExercise exercise : exercisesForState) {
        populateCollatorMap(allIDs, idToKey, collator, exercise);
      }
    }
    //logger.debug("for " + typeToSection + " found " + allIDs.size());
    return db.getUserHistoryForList(userid, ids, (int) latestResultID, allIDs, idToKey);
  }

  Collator getCollator() {
    return getAudioFileHelper().getCollator();
  }

  private void populateCollatorMap(List<Integer> allIDs, Map<Integer, CollationKey> idToKey, Collator collator,
                                   CommonShell exercise) {
    allIDs.add(exercise.getID());
    CollationKey collationKey = collator.getCollationKey(exercise.getForeignLanguage());
    idToKey.put(exercise.getID(), collationKey);
  }


  public void logMessage(String message) {
    if (message.length() > 10000) message = message.substring(0, 10000);
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " from client " + message;
    logger.debug(prefixedMessage);

    if (message.startsWith("got browser exception")) {
      sendEmail("Javascript Exception", getInfo(prefixedMessage));
    }
  }

  private String getInfo(String message) {
    HttpServletRequest request = getThreadLocalRequest();
    if (request != null) {
      String remoteAddr = request.getHeader("X-FORWARDED-FOR");
      if (remoteAddr == null || remoteAddr.isEmpty()) {
        remoteAddr = request.getRemoteAddr();
      }
      String userAgent = request.getHeader("User-Agent");

      String strongName = getPermutationStrongName();
      String serverName = getThreadLocalRequest().getServerName();
      String msgStr = message +
          "\nremoteAddr : " + remoteAddr +
          "\nuser agent : " + userAgent +
          "\ngwt        : " + strongName +
          "\nserver     : " + serverName;

      return msgStr;
    } else {
      return "";
    }
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  @Override
  public void destroy() {
//    refResultDecoder.setStopDecode(true);
    //stopOggCheck = true;
    super.destroy();
    if (db == null) {
      logger.error("DatabaseImpl was never made properly...");
    } else {
      db.destroy(); // TODO : redundant with h2 shutdown hook?
      db.stopDecode();
    }
  }

  private String startupMessage = "";

  /**
   * Reco test option lets you run through and score all the reference audio -- if you want to see model performance
   */
  @Override
  public void init() {
    try {
      this.pathHelper = new PathHelper(getServletContext());
      readProperties(getServletContext());
      setInstallPath(db);
      if (serverProps.isAMAS()) {
        audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this, null);
      }
    } catch (Exception e) {
      startupMessage = e.getMessage();
      logger.error("Got " + e, e);
    }

    try {
      db.preloadContextPractice();
      getUserListManager().setStateOnExercises();
      db.doReport(serverProps, getServletContext().getRealPath(""), getMailSupport(), pathHelper);
    } catch (Exception e) {
      logger.error("couldn't load database " + e, e);
    }

    try {
//      this.refResultDecoder = new RefResultDecoder(db, serverProps, pathHelper, getAudioFileHelper());
//      refResultDecoder.doRefDecode(getExercises(), relativeConfigDir);
      if (serverProps.isAMAS()) getAudioFileHelper().makeAutoCRT(relativeConfigDir);
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
  }

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

  private String getLanguage() {
    Project project = getProject();
    if (project == null) {
      logger.error("no current project ");
      return "";
    } else {
      SlickProject project1 = project.getProject();
      return project1.language();
    }
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   * <p>
   * NOTE : makes the database available to other servlets via the databaseReference servlet context attribute.
   * Note that this will only ever be called once.
   *
   * @param servletContext
   * @see #init()
   */
  private void readProperties(ServletContext servletContext) {
    this.relativeConfigDir = "config" + File.separator + servletContext.getInitParameter("config");
    this.configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;
    pathHelper.setConfigDir(configDir);

    serverProps = new ServerProperties(servletContext, configDir);
    audioConversion = new AudioConversion(serverProps);
    db = makeDatabaseImpl(serverProps.getH2Database());
    shareDB(servletContext);
    securityManager = new UserSecurityManager(db.getUserDAO());
    logger.info("made " + securityManager);
//    shareLoadTesting(servletContext);
  }

/*
  private void shareLoadTesting(ServletContext servletContext) {
    Object loadTesting = servletContext.getAttribute(ScoreServlet.LOAD_TESTING);
    if (loadTesting != null) {
      logger.debug("hmm... found existing load testing reference " + loadTesting);
    }
    servletContext.setAttribute(ScoreServlet.LOAD_TESTING, this);
  }
*/

  /**
   * @param servletContext
   * @see #readProperties
   */
  private void shareDB(ServletContext servletContext) {
    Object databaseReference = servletContext.getAttribute(DATABASE_REFERENCE);
    if (databaseReference != null) {
      logger.debug("hmm... found existing database reference " + databaseReference);
    }

    servletContext.setAttribute(DATABASE_REFERENCE, db);
  }

  /**
   * @param servletContext
   * @see #getExercises
   */
  private void shareAudioFileHelper(ServletContext servletContext) {
    Object databaseReference = servletContext.getAttribute(AUDIO_FILE_HELPER_REFERENCE);
    if (databaseReference != null) {
      logger.debug("hmm... found existing reference " + databaseReference);
    }

    servletContext.setAttribute(AUDIO_FILE_HELPER_REFERENCE, getAudioFileHelper());
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, h2DatabaseFile, serverProps, pathHelper, true, this, false);
  }

  /**
   * @param db
   * @return
   * @see LangTestDatabaseImpl#init()
   */
  private void setInstallPath(DatabaseImpl db) {
    String lessonPlanFile = getLessonPlan();
    if (lessonPlanFile != null &&
        !serverProps.getLessonPlan().startsWith("http") &&
        !new File(lessonPlanFile).exists()) {
      logger.error("couldn't find lesson plan file " + lessonPlanFile);
    }

    String mediaDir = "";//relativeConfigDir + File.separator + serverProps.getMediaDir();
    String installPath = pathHelper.getInstallPath();
    logger.debug("setInstallPath " + installPath + " " + lessonPlanFile + " media " + serverProps.getMediaDir() + " rel media " + mediaDir);
    db.setInstallPath(installPath,
        lessonPlanFile,
        mediaDir);
  }

  /**
   * Find user from session, then find the current project for the user.
   *
   * @return
   */
  private int getProjectID() {
    try {
      User loggedInUser = getSessionUser();
      if (loggedInUser == null) return -1;
      int i = db.getProjectIDForUser(loggedInUser);
      return i;
    } catch (DominoSessionException e) {
      logger.error("Got " + e, e);
      return -1;
    }
  }

  private User getSessionUser() throws DominoSessionException {
    HttpServletRequest threadLocalRequest = getThreadLocalRequest();
    //   logger.info("getProjectID got request " + threadLocalRequest);
    return securityManager.getLoggedInUser(threadLocalRequest);
  }

  private Project getProject() {
    try {
      User loggedInUser = getSessionUser();
      if (loggedInUser == null) return null;
      else return db.getProjectForUser(loggedInUser.getId());
    } catch (DominoSessionException e) {
      logger.error("got " + e, e);
      return null;
    }
  }

  private String getLessonPlan() {
    return serverProps.getLessonPlan() == null ? null : configDir + File.separator + serverProps.getLessonPlan();
  }
}
