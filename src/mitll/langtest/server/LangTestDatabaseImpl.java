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
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.server.amas.QuizCorrect;
import mitll.langtest.server.audio.*;
import mitll.langtest.server.autocrt.AutoCRT;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.*;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.lang.exception.ExceptionUtils;
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
  public static final String AUDIO_FILE_HELPER_REFERENCE = "audioFileHelperReference";

  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final int MP3_LENGTH = MP3.length();

  private static final int SLOW_EXERCISE_EMAIL = 2000;
  private static final int MAX = 30;
  private static final int SLOW_MILLIS = 40;
  private static final int WARN_DUR = 100;
  private static final int MIN_RECORDINGS = 5;
  private static final String WAV1 = "wav";
  private RefResultDecoder refResultDecoder;

  private static final boolean WARN_MISSING_FILE = true;

  private DatabaseImpl<CommonExercise> db;
  private AudioFileHelper audioFileHelper;
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
  private ExerciseTrie fullTrie = null;
  private ExerciseTrie<AmasExerciseImpl> amasFullTrie = null;

  private static final boolean DEBUG = false;

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
      sendEmail(subject, prefixedMessage);

      logger.debug(prefixedMessage);
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
  private <T extends CommonShell> List<CommonShell> getExerciseShells(Collection<T> exercises) {
    List<CommonShell> ids = new ArrayList<>();
    for (CommonShell e : exercises) {
//      logger.info("got " +e.getID() + " mean " + e.getMeaning() + " eng " + e.getEnglish() + " fl " + e.getForeignLanguage());
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
        //  logger.debug("marked " +i + " as recorded");

        // now sort : everything gets sorted the same way
        //    List<AmasExerciseImpl> commonExercises;
//        if (incorrectFirstOrder) {
//          commonExercises = db.getResultDAO().getExercisesSortedIncorrectFirst(exercises, userID, audioFileHelper.getCollator());
//        } else {
        //    commonExercises = new ArrayList<AmasExerciseImpl>(exercises);
        //   sortExercises("", commonExercises);
//        }

        return new ExerciseListWrapper<>(reqID, exercises, null);
      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        Collection<AmasExerciseImpl> exercisesForSelectionState1 =
            new AmasSupport().getExercisesForSelectionState(typeToSelection, request.getPrefix(), request.getUserID(),
                db.getAMASSectionHelper(), db.getResultDAO());
        return new ExerciseListWrapper<>(reqID, exercisesForSelectionState1, null);
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
  public <T extends CommonShell> ExerciseListWrapper<T> getExerciseIds(ExerciseListRequest request) {
    if (serverProps.isAMAS()) {
      ExerciseListWrapper<AmasExerciseImpl> amasExerciseIds = getAMASExerciseIds(request);
      return (ExerciseListWrapper<T>) amasExerciseIds; // TODO : how to do this without forcing it.
    }

    Collection<CommonExercise> exercises;

    logger.debug("getExerciseIds : (" + getLanguage() + ") " +
        "getting exercise ids for config " + relativeConfigDir +
        " request " + request);

    try {
      boolean isUserListReq = request.getUserListID() != -1;
      UserList userListByID = isUserListReq ? db.getUserListByID(request.getUserListID()) : null;

      if (request.getTypeToSelection().isEmpty()) {   // no unit-chapter filtering
        // get initial exercise set, either from a user list or predefined
        boolean predefExercises = userListByID == null;
        exercises = predefExercises ? getExercises() : getCommonExercises(userListByID);

        logger.debug("\tgetExerciseIds : (" + getLanguage() + ") " +
            "got " + exercises.size() +
            " for request " + request);
        // now if there's a prefix, filter by prefix match
        int userID = request.getUserID();
        if (!request.getPrefix().isEmpty()) {
          // now do a trie over matches
          exercises = getExercisesForSearch(request.getPrefix(), userID, exercises, predefExercises);
        }
        exercises = filterExercises(request, exercises);

/*        logger.debug("\tgetExerciseIds : (" + getLanguage() + ") " +
            "after filtering " + exercises.size() +
            " for request " + request);*/

        String role = request.getRole();

        if (!isUserListReq) {
          int i = markRecordedState(userID, role, exercises, request.isOnlyExamples());
          logger.debug("\tgetExerciseIds : (" + getLanguage() + ") " +
              "mark recorded on " + exercises.size() + " = " + i +
              " for request " + request);
        }

        // now sort : everything gets sorted the same way
        List<CommonExercise> commonExercises;
        if (request.isIncorrectFirstOrder()) {
          commonExercises = db.getResultDAO().getExercisesSortedIncorrectFirst(exercises, userID, audioFileHelper.getCollator());
        } else {
          commonExercises = new ArrayList<>(exercises);

  /*        logger.debug("\tgetExerciseIds : (" + getLanguage() + ") " +
              "sorting   " + commonExercises.size() +
              " for request " + request);
  */
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

  /**
   * @param request
   * @param exercises
   * @return
   */
  private Collection<CommonExercise> filterExercises(ExerciseListRequest request,
                                                     Collection<CommonExercise> exercises) {
    exercises = filterByUnrecorded(request, exercises);
    if (request.isOnlyWithAudioAnno()) {
      exercises = filterByOnlyAudioAnno(exercises);
    }
    if (request.isOnlyDefaultAudio()) {
      exercises = filterByOnlyDefaultAudio(exercises);
    }
    if (request.isOnlyUninspected()) {
      exercises = filterByUninspected(exercises);
    }
    return exercises;
  }

  private <T extends CommonShell> void sortExercises(String role, List<T> commonExercises) {
    new ExerciseSorter(db.getTypeOrder()).getSortedByUnitThenAlpha(commonExercises, role.equals(Result.AUDIO_TYPE_RECORDER));
  }

  private <T extends CommonShell> Collection<T> getExercisesForSearch(String prefix, int userID, Collection<T> exercises, boolean predefExercises) {
    ExerciseTrie<T> fullTrie = this.fullTrie;
    return getExercisesForSearchWithTrie(prefix, userID, exercises, predefExercises, fullTrie);
  }

  private <T extends CommonShell> Collection<T> getExercisesForSearchWithTrie(String prefix,
                                                                              int userID,
                                                                              Collection<T> exercises,
                                                                              boolean predefExercises,
                                                                              ExerciseTrie<T> fullTrie) {
    ExerciseTrie<T> trie = predefExercises ? fullTrie : new ExerciseTrie<T>(exercises, getLanguage(), audioFileHelper.getSmallVocabDecoder());
    exercises = trie.getExercises(prefix, audioFileHelper.getSmallVocabDecoder());

    if (exercises.isEmpty()) { // allow lookup by id
      T exercise = getExercise(prefix, userID, false);
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
   * TODO : this must include a check for transcript mismatch.
   * <p>
   * For all the exercises the user has not recorded, do they have the required reg and slow speed recordings by a matching gender.
   * <p>
   * Or if looking for example audio, find ones missing examples.
   *
   * @param exercises to filter
   * @return exercises missing audio, what we want to record
   * @see #getExerciseIds
   * @see #getExercisesForSelectionState
   */
  private Collection<CommonExercise> filterByUnrecorded(ExerciseListRequest request, Collection<CommonExercise> exercises) {
    boolean onlyExamples = request.isOnlyExamples();

    if (request.isOnlyUnrecordedByMe()) {
      int userID = request.getUserID();
      logger.debug("filterByUnrecorded : for " + userID + " only by same gender " +
          " examples only " + onlyExamples + " from " + exercises.size());

      Map<String, String> exToTranscript = new HashMap<>();
      Map<String, String> exToContextTranscript = new HashMap<>();

      for (CommonShell shell : exercises) {
        exToTranscript.put(shell.getID(), shell.getForeignLanguage());
        String context = shell.getContext();
        if (context != null && !context.isEmpty()) {
          exToContextTranscript.put(shell.getID(), context);
        }
      }

      Set<String> alreadyRecordedBySameGender = onlyExamples ?
          db.getAudioDAO().getWithContext(userID, exToContextTranscript) :
          db.getAudioDAO().getRecordedBy(userID, exToTranscript);

      Set<String> allExercises = getExerciseIDs(exercises);

      //logger.debug("all exercises " + allExercises.size() + " removing " + alreadyRecordedBySameGender.size());
      allExercises.removeAll(alreadyRecordedBySameGender);
      // logger.debug("after all exercises " + allExercises.size());

      List<CommonExercise> copy = new ArrayList<>();
      Set<String> seen = new HashSet<String>();
      for (CommonExercise exercise : exercises) {
        String trim = exercise.getID().trim();
        if (allExercises.contains(trim)) {
          if (seen.contains(trim)) logger.warn("saw " + trim + " " + exercise + " again!");
          if (!onlyExamples || hasContext(exercise)) {
            seen.add(trim);
            copy.add(exercise);
          }
        }
      }
      return copy;
    } else {
      if (onlyExamples) {
        List<CommonExercise> copy = new ArrayList<>();
        Set<String> seen = new HashSet<String>();
        for (CommonExercise exercise : exercises) {
          String trim = exercise.getID().trim();
          if (seen.contains(trim)) logger.warn("saw " + trim + " " + exercise + " again!");
          if (hasContext(exercise)) {
            seen.add(trim);
            copy.add(exercise);
          }
        }
        return copy;
      } else {
        return exercises;
      }
    }
  }

  private Set<String> getExerciseIDs(Collection<CommonExercise> exercises) {
    Set<String> allExercises = new HashSet<String>();
    for (CommonShell exercise : exercises) {
      allExercises.add(exercise.getID().trim());
    }
    return allExercises;
  }

  private <X extends CommonShell> boolean hasContext(X exercise) {
    return exercise.getContext() != null && !exercise.getContext().isEmpty();
  }

  /**
   * @param exercises
   * @return
   * @paramx onlyAudioAnno
   * @see #getExerciseIds
   */
  private Collection<CommonExercise> filterByOnlyAudioAnno(Collection<CommonExercise> exercises) {
    Set<String> audioAnnos = getUserListManager().getAudioAnnos();
    List<CommonExercise> copy = new ArrayList<CommonExercise>();
    for (CommonExercise exercise : exercises) {
      if (audioAnnos.contains(exercise.getID())) copy.add(exercise);
    }
    return copy;
  }

  private Collection<CommonExercise> filterByOnlyDefaultAudio(Collection<CommonExercise> exercises) {
    List<CommonExercise> copy = new ArrayList<CommonExercise>();
    for (CommonExercise exercise : exercises) {
      for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
        if (audioAttribute.getUserid() == UserDAO.DEFAULT_USER_ID) {
          copy.add(exercise);
          break;
        }
      }
    }
    return copy;
  }

  private Collection<CommonExercise> filterByUninspected(Collection<CommonExercise> exercises) {
    Collection<String> uninspected = getUserListManager().getUninspected();
    logger.info("found " + uninspected.size());

    List<CommonExercise> copy = new ArrayList<CommonExercise>();
    for (CommonExercise exercise : exercises) {
      if (uninspected.contains(exercise.getID())) {
        copy.add(exercise);
      }
    }
    return copy;
  }

  private <T extends CommonShell> Collection<CommonExercise> getExercisesFromUserListFiltered(Map<String, Collection<String>> typeToSelection,
                                                                                              UserList<T> userListByID) {
    SectionHelper<CommonExercise> helper = new SectionHelper<>();
    Collection<CommonExercise> exercises2 = getCommonExercises(userListByID);
    long then = System.currentTimeMillis();
    for (CommonExercise commonExercise : exercises2) {
      helper.addExercise(commonExercise);
    }
    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.debug("used " + exercises2.size() + " exercises to build a hierarchy in " + (now - then) + " millis");
    }
    //helper.report();
    Collection<CommonExercise> exercisesForState = helper.getExercisesForSelectionState(typeToSelection);
    // logger.debug("\tafter found " + exercisesForState.size() + " matches to " + typeToSelection);
    return exercisesForState;
  }

  public QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection, int userID, Collection<String> exids) {
    return new QuizCorrect(db).getScoresForUser(typeToSection, userID, exids);
  }

  @Override
  public void addStudentAnswer(long resultID, boolean correct) {
    db.getAnswerDAO().addUserScore(resultID, correct ? 1.0f : 0.0f);
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
    //  logger.warn("getScoreForAnswer user " + userID + " ex " + exercise.getID() + " qid " +questionID + " type " +typeToSection + " session " + session);
    boolean correct = scoreForAnswer > 0.5;
    long resultID = db.getAnswerDAO().addTextAnswer(audioContext,
        answer,
        correct,
        (float) scoreForAnswer, (float) scoreForAnswer, session, timeSpent);

    Answer answer1 = new Answer(scoreForAnswer, correct, resultID);
    return answer1;
  }


  /**
   * TODO : seems like we're doing the exercise->transcript map twice???
   * <p>
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
   * @return number recorded
   * @see #getExerciseIds
   */
  private int markRecordedState(int userID, String role, Collection<? extends CommonShell> exercises, boolean onlyExample) {
    int c = 0;

    if (role.equals(Result.AUDIO_TYPE_RECORDER)) {
      Map<String, String> exToTranscript = new HashMap<>();
      for (CommonShell shell : exercises) {
        if (onlyExample) {
          String context = shell.getContext();
          if (context != null && !context.isEmpty()) {
            exToTranscript.put(shell.getID(), context);
          }
        } else {
          exToTranscript.put(shell.getID(), shell.getForeignLanguage());
        }
      }

      Set<String> recordedForUser = onlyExample ?
          db.getAudioDAO().getRecordedExampleForUser(userID, exToTranscript) :
          db.getAudioDAO().getRecordedForUser(userID, exToTranscript);
      logger.debug("\tmarkRecordedState : found " + recordedForUser.size() + " recordings by " + userID + " only example " + onlyExample);
      for (CommonShell shell : exercises) {
        if (recordedForUser.contains(shell.getID())) {
          shell.setState(STATE.RECORDED);
          c++;
        }
      }
    } else {
      logger.debug("\tmarkRecordedState not marking recorded for '" + role + "' and user " + userID);
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
  private <T extends CommonShell> List<CommonExercise> getCommonExercises(UserList<T> userListByID) {
    Collection<T> exercises = userListByID.getExercises();
    List<CommonExercise> ts = new ArrayList<>(exercises.size());
    for (CommonShell shell : exercises) {
      CommonExercise byID = db.getCustomOrPredefExercise(shell.getID());  // allow custom items to mask out non-custom items
      if (byID != null) {
        ts.add(byID);
      }
    }
    return ts;
  }

  /**
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises
   * @see #getExerciseIds
   */
  private <T extends CommonShell> ExerciseListWrapper<T> getExercisesForSelectionState(ExerciseListRequest request) {
    Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(request.getTypeToSelection());
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

    int i = markRecordedState((int) userID, role, exercisesForState, onlyExamples);
    //logger.debug("marked " +i + " as recorded role " +role);

    if (hasPrefix) {
      ExerciseTrie<CommonExercise> trie = new ExerciseTrie<CommonExercise>(exercisesForState, getLanguage(), audioFileHelper.getSmallVocabDecoder());
      exercisesForState = trie.getExercises(prefix, audioFileHelper.getSmallVocabDecoder());
    }

    if (exercisesForState.isEmpty()) { // allow lookup by id
      CommonExercise exercise = getExercise(prefix, userID, incorrectFirst);
      if (exercise != null) exercisesForState = Collections.singletonList(exercise);
    }
    // why copy???
    List<CommonExercise> copy;

    if (incorrectFirst) {
      copy = db.getResultDAO().getExercisesSortedIncorrectFirst(exercisesForState, userID, audioFileHelper.getCollator());
    } else {
      copy = new ArrayList<CommonExercise>(exercisesForState);
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
    CommonExercise firstExercise = null;
    if (exercises.isEmpty()) {

    } else {
      firstExercise = db.getCustomOrPredefExercise(exercises.iterator().next().getID());  // allow custom items to mask out non-custom items
    }

//    CommonExercise firstExercise = exercises.isEmpty() ? null : exercises.iterator().next();

    int reqID = request.getReqID();
    int userID = request.getUserID();
    String role = request.getRole();
    boolean onlyExamples = request.isOnlyExamples();

    if (firstExercise != null) {
      addAnnotationsAndAudio(userID, firstExercise, request.isIncorrectFirstOrder());
      ensureMP3s(firstExercise, pathHelper.getInstallPath());
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);

    logger.debug("makeExerciseListWrapper : userID " + userID + " request is " + request);
    if (role.equals(Result.AUDIO_TYPE_RECORDER)) {
      markRecordedState(userID, role, exerciseShells, onlyExamples);
    } else if (
        role.equalsIgnoreCase(User.Permission.QUALITY_CONTROL.toString()) ||
            role.startsWith(Result.AUDIO_TYPE_REVIEW)) {
      getUserListManager().markState(exerciseShells);
    } else if (role.equals("markDefects")) {
      Collection<String> defectExercises = getUserListManager().getDefectExercises();
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

  /**
   * 0) Add annotations to fields on exercise
   * 1) Attach audio recordings to exercise.
   * 2) Adds information about whether the audio has been played or not...
   * 3) Attach history info (when has the user recorded audio for the item under the learn tab and gotten a score)
   *
   * @param userID
   * @param firstExercise
   * @param isFlashcardReq
   * @seex LoadTesting#getExercise(String, long, boolean)
   * @see #makeExerciseListWrapper
   */
  private void addAnnotationsAndAudio(long userID, CommonExercise firstExercise, boolean isFlashcardReq) {
    long then = System.currentTimeMillis();

    addAnnotations(firstExercise); // todo do this in a better way
    long now = System.currentTimeMillis();
    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " + (now - then) + " millis to add annotations to exercise " + firstExercise.getID());
    }
    then = now;
    attachAudio(firstExercise);

    if (DEBUG) {
      for (AudioAttribute audioAttribute : firstExercise.getAudioAttributes())
        logger.debug("\t addAnnotationsAndAudio ex " + firstExercise.getID() + " audio " + audioAttribute);
    }

    now = System.currentTimeMillis();
    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " + (now - then) + " millis to attach audio to exercise " + firstExercise.getID());
    }
    then = now;

    User userWhere = db.getUserDAO().getUserWhere(userID);
    if (userWhere != null && userWhere.getPermissions().contains(User.Permission.QUALITY_CONTROL)) {
      addPlayedMarkings(userID, firstExercise);
      now = System.currentTimeMillis();
      if (now - then > SLOW_MILLIS) {
        logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " + (now - then) + " millis to add played markings to exercise " + firstExercise.getID());
      }
    }

    then = now;

    attachScoreHistory(userID, firstExercise, isFlashcardReq);

    now = System.currentTimeMillis();
    if (now - then > SLOW_MILLIS) {
      logger.debug("addAnnotationsAndAudio : (" + getLanguage() + ") took " + (now - then) + " millis to attach score history to exercise " + firstExercise.getID());
    }

    if (DEBUG) {
      for (AudioAttribute audioAttribute : firstExercise.getAudioAttributes())
        logger.debug("\t addAnnotationsAndAudio ret ex " + firstExercise.getID() + " audio " + audioAttribute);
    }
  }

  /**
   * @param userID
   * @param firstExercise
   * @param isFlashcardReq
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.exercise.CommonExercise, boolean)
   */
  private void attachScoreHistory(long userID, CommonExercise firstExercise, boolean isFlashcardReq) {
    db.getResultDAO().attachScoreHistory(userID, firstExercise, isFlashcardReq);
  }

  /**
   * @param firstExercise
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.exercise.CommonExercise, boolean)
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
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.exercise.CommonExercise, boolean)
   */
  private void addPlayedMarkings(long userID, CommonExercise firstExercise) {
    db.getEventDAO().addPlayedMarkings(userID, firstExercise);
  }

  /**
   * @param ids
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores(List)
   */
  @Override
  public List<CommonShell> getShells(List<String> ids) {
    List<CommonShell> shells = new ArrayList<>();
    for (String id : ids) {
      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(id);
      if (customOrPredefExercise == null) {
        logger.warn("Couldn't find exercise for " + id);
      } else {
        shells.add(customOrPredefExercise.getShell());
      }
    }
    return shells;
  }

  /**
   * Joins with annotation data when doing QC.
   *
   * @param id
   * @param userID
   * @param isFlashcardReq
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   * @see mitll.langtest.client.list.ExerciseList#goGetNextAndCacheIt(String)
   * @see mitll.langtest.client.analysis.PlayAudio#playLast(String, long)
   */
  public <T extends Shell> T getExercise(String id, long userID, boolean isFlashcardReq) {
    if (serverProps.isAMAS()) { // TODO : HOW TO AVOID CAST???
      return (T) db.getAMASExercise(id);
    }

    long then = System.currentTimeMillis();
    Collection<CommonExercise> exercises = getExercises();

    long then2 = System.currentTimeMillis();

    CommonExercise byID = db.getCustomOrPredefExercise(id);  // allow custom items to mask out non-custom items

    long now = System.currentTimeMillis();
    String language = getLanguage();
    if (now - then2 > WARN_DUR) {
      logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to find exercise " + id + " for " + userID);
    }

    if (byID == null) {
      if (!id.isEmpty()) {
        String message = "getExercise : huh? couldn't find exercise with id '" + id + "' when examining " + exercises.size() + " items";
        logger.warn(message);
        //  logAndNotifyServerException(new IllegalArgumentException(message));
      }
    } else {
      then2 = System.currentTimeMillis();
      addAnnotationsAndAudio(userID, byID, isFlashcardReq);
      now = System.currentTimeMillis();
      if (now - then2 > WARN_DUR) {
        logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to add annotations to exercise " + id + " for " + userID);
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
              "to ensure there are mp3s for exercise " + id + " for " + userID);
        }
      }
    }
    checkPerformance(id, then);

    if (byID != null) {
      logger.debug("returning (" + language + ") exercise " + byID.getID() + " : " + byID);
    } else {
      logger.warn(getLanguage() + " : couldn't find exercise with id '" + id + "'");
    }
    // return byID;
    // TODO : why doesn't this work?
    return (T) byID;
  }

  private void checkPerformance(String id, long then) {
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

  private void sendEmailWhenSlow(String id, String language, long diff, String threadInfo) {
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
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.exercise.CommonExercise, boolean)
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
      if (!ensureMP3(audioAttribute.getAudioRef(), parentDir, getTrackInfo(byID, audioAttribute))) {
//        if (byID.getID().equals("1310")) {
//          logger.warn("ensureMP3 : can't find " + audioAttribute + " under " + parentDir + " for " + byID);
//        }
        audioAttribute.setAudioRef(AudioConversion.FILE_MISSING);
      }
    }

//    if (audioAttributes.isEmpty() && byID.getID().equals("1310")) {
//      logger.warn("ensureMP3s : (" + getLanguage() + ") no ref audio for " + byID);
//    }
  }

  private Collection<AmasExerciseImpl> getAMASExercises() {
//    logger.info("get exercises -------");
    long then = System.currentTimeMillis();
    Collection<AmasExerciseImpl> exercises = db.getAMASExercises();
    if (amasFullTrie == null) {
      amasFullTrie = new ExerciseTrie<AmasExerciseImpl>(exercises, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
    }

//    try {
    // audioFileHelper.checkLTS(exercises);
    //  } catch (Exception e) {
    //  logger.error("Got " + e, e);
    // }
    if (getServletContext().getAttribute(AUDIO_FILE_HELPER_REFERENCE) == null) {
      shareAudioFileHelper(getServletContext());
    }
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list for " + serverProps.getLanguage());
    }
    return exercises;
  }

  /**
   * Called from the client:
   *
   * @return
   * @see mitll.langtest.client.list.ListInterface#getExercises
   */
  private Collection<CommonExercise> getExercises() {
    long then = System.currentTimeMillis();
    Collection<CommonExercise> exercises = db.getExercises();
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("getExercises took " + (now - then) + " millis to get the raw exercise list for " + getLanguage());
    }
    if (fullTrie == null) {
      buildExerciseTrie();
      audioFileHelper.checkLTSAndCountPhones(exercises);
      shareAudioFileHelper(getServletContext());
    }

    now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list for " + getLanguage());
    }
    return exercises;
  }

  private <T extends CommonShell> void buildExerciseTrie() {
    fullTrie = new ExerciseTrie<CommonExercise>(db.getExercises(), getLanguage(), audioFileHelper.getSmallVocabDecoder());
  }

  public ContextPractice getContextPractice() {
    return db.getContextPractice();
  }

  @Override
  public void reloadExercises() {
    db.reloadExercises();
  }

  /**
   * @param wavFile
   * @param trackInfo
   * @return true if mp3 file exists
   * @see #ensureMP3s(CommonExercise, String)
   * @see #writeAudioFile
   */
  private boolean ensureMP3(String wavFile, TrackInfo trackInfo) {
    return ensureMP3(wavFile, pathHelper.getInstallPath(), trackInfo);
  }

  // int spew = 0;

  private boolean ensureMP3(String wavFile, String parent, TrackInfo trackInfo) {
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

      String s = audioConversion.ensureWriteMP3(wavFile, parent, false, trackInfo);
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
   * @param imageOptions
   * @param exerciseID
   * @return path to an image file
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio
   */
  public ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType,
                                            ImageOptions imageOptions, String exerciseID) {
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

    int width = imageOptions.getWidth();
    int height = imageOptions.getHeight();

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
    if (absolutePathToImage != null && absolutePathToImage.startsWith(installPath)) {
      relativeImagePath = absolutePathToImage.substring(installPath.length());
    } else {
      logger.error("getImageForAudioFile huh? file path " + absolutePathToImage + " doesn't start with " + installPath + "?");
    }

    if (relativeImagePath != null) {
      relativeImagePath = pathHelper.ensureForwardSlashes(relativeImagePath);
      if (relativeImagePath.startsWith("/")) {
        relativeImagePath = relativeImagePath.substring(1);
      }
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
      audioFile = test.exists() ? test.getAbsolutePath() : audioFileHelper.getWavForMP3(audioFile);
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
    Collection<CommonExercise> exercises = db.getExercises();
    Map<String, String> properties = serverProps.getProperties();
    if (!exercises.isEmpty()) {
      CommonExercise next = exercises.iterator().next();
      HasDirection.Direction direction = WordCountDirectionEstimator.get().estimateDirection(next.getForeignLanguage());
      String rtl = properties.get("rtl");
      if (rtl == null) {
        boolean isRTL = direction == HasDirection.Direction.RTL;
        // logger.info("examined text and found it to be " + direction);
        properties.put("rtl", "" + isRTL);
      }
    }
    return new StartupInfo(properties, db.getTypeOrder(), db.getSectionNodes());
  }

  /**
   * Go back and score an old result, which mainly means generating images for it.
   *
   * @param resultID
   * @param imageOptions
   * @return
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#scoreAudio
   */
  @Override
  public PretestScore getResultASRInfo(long resultID, ImageOptions imageOptions) {
    PretestScore asrScoreForAudio = null;
    try {
      Result result = db.getResultDAO().getResultByID(resultID);

      String exerciseID = result.getExerciseID();

      CommonShell exercise = serverProps.isAMAS() ? db.getAMASExercise(exerciseID) :
          db.getExercise(exerciseID);
      if (exercise == null) {
        logger.warn(getLanguage() + " can't find exercise id " + exerciseID);
        return new PretestScore();
      } else {
        String sentence = exercise.getForeignLanguage();
        String comment = exercise.getEnglish();

        if (result.getAudioType().contains("context")) {
          sentence = exercise.getContext();
          comment = exercise.getContextTranslation();
        }

        String audioFilePath = result.getAnswer();
        ensureMP3(audioFilePath, new TrackInfo(sentence, "" + result.getUserid(), comment));
        //logger.info("resultID " +resultID+ " temp dir " + tempDir.getAbsolutePath());
        asrScoreForAudio = audioFileHelper.getASRScoreForAudio(
            1,
            audioFilePath,
            sentence,
            exercise.getTransliteration(),
            imageOptions,//new ImageOptions(width, height, true),
            exerciseID,
            result,

            // make transcript images with colored segments
            new DecoderOptions()
                .setDoFlashcard(false)
                .setCanUseCache(serverProps.useScoreCache())
                .setUsePhoneToDisplay(serverProps.usePhoneToDisplay()));
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    return asrScoreForAudio;
  }

  private TrackInfo getTrackInfo(CommonExercise ex, AudioAttribute latestContext) {
    return new TrackInfo(ex.getForeignLanguage(), latestContext.getUser().getUserID(), ex.getEnglish());
  }

  /**
   * So first we check and see if we've already done alignment for this audio (if reference audio), and if so, we grab the Result
   * object out of the result table and use it and it's json to generate the score info and transcript inmages.
   *
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param imageOptions
   * @return
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio
   */
  public PretestScore getASRScoreForAudio(int reqid,
                                          long resultID,
                                          String testAudioFile,
                                          String sentence,
                                          String transliteration,
                                          String exerciseID,
                                          ImageOptions imageOptions) {
    return getPretestScore(reqid,
        resultID,
        testAudioFile,
        sentence,
        transliteration,
        imageOptions,
        exerciseID,
        false);
  }

  /**
   * Be careful - we lookup audio file by .wav extension
   *
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhoneToDisplay
   * @return
   * @paramx width
   * @paramx height
   * @paramx useScoreToColorBkg
   */
  private PretestScore getPretestScore(int reqid, long resultID, String testAudioFile,
                                       String sentence, String transliteration,

                                       ImageOptions imageOptions, String exerciseID,

                                       boolean usePhoneToDisplay) {
    if (testAudioFile.equals(AudioConversion.FILE_MISSING)) return new PretestScore(-1);
    long then = System.currentTimeMillis();

    String[] split = testAudioFile.split(File.separator);
    String answer = split[split.length - 1];
    String wavEndingAudio = answer.replaceAll(".mp3", ".wav").replaceAll(".ogg", ".wav");

    //Result cachedResult = db.getRefResultDAO().getResult(exerciseID, wavEndingAudio);
    long now = System.currentTimeMillis();
    //logger.info("traditional " + (now-then) + " result " + cachedResult.getUniqueID());
    then = now;
    Result cachedResult = db.getRefResultDAO().getRefForExAndAudio(exerciseID, wavEndingAudio);
    now = System.currentTimeMillis();

    //now = System.currentTimeMillis();
    if (cachedResult != null) {
      long l = now - then;
      logger.debug("getPretestScore Cache HIT  : align " +
          "\n\texercise " + exerciseID +
          "\n\tfile     " + answer +
          "\n\timage    " + imageOptions +
          "\n\tprevious " + cachedResult.getUniqueID() + (l > 10 ? " in " + l : ""));
    } else {
      logger.debug("getPretestScore Cache MISS : align" +
          "\n\texercise " + exerciseID +
          "\n\tfile     " + answer +
          "\n\timage    " + imageOptions
      );
    }

    boolean usePhoneToDisplay1 = usePhoneToDisplay || serverProps.usePhoneToDisplay();

    PretestScore asrScoreForAudio = audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence,
        transliteration, imageOptions, exerciseID, cachedResult,
        new DecoderOptions()
            .setDoFlashcard(false)
            .setCanUseCache(serverProps.useScoreCache())
            .setUsePhoneToDisplay(usePhoneToDisplay1));

    long timeToRunHydec = System.currentTimeMillis() - then;

    logger.debug("getPretestScore : scoring" +
        "\n\tfile     " + testAudioFile +
        "\n\texid     " + exerciseID +
        "\n\tsentence " + sentence.length() + " characters long" +
        "\n\tscore    " + asrScoreForAudio.getHydecScore() +
        "\n\ttook     " + timeToRunHydec + " millis " +
        "\n\tusePhoneToDisplay " + usePhoneToDisplay1);

    if (resultID > -1 && cachedResult == null) { // alignment has two steps : 1) post the audio, then 2) do alignment
      db.rememberScore(resultID, asrScoreForAudio, false);
    }
    return asrScoreForAudio;
  }


  /**
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param imageOptions
   * @return
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio
   */
  @Override
  public PretestScore getASRScoreForAudioPhonemes(int reqid,
                                                  long resultID,
                                                  String testAudioFile,

                                                  String sentence,
                                                  String transliteration,
                                                  String exerciseID,

                                                  ImageOptions imageOptions) {
    return getPretestScore(reqid, resultID, testAudioFile, sentence, transliteration, imageOptions, exerciseID, true);
  }

  @Override
  public void addRoundTrip(long resultID, int roundTrip) {
    db.getAnswerDAO().addRoundTrip(resultID, roundTrip);
  }

  // Users ---------------------

  /**
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin
   * @see mitll.langtest.client.user.UserPassLogin#makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   */
  public User userExists(String login, String passwordH) {
    return db.userExists(getThreadLocalRequest(), login, passwordH);
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @return
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  @Override
  public long addUserList(long userid, String name, String description, String dliClass, boolean isPublic) {
    return getUserListManager().addUserList(userid, name, description, dliClass, isPublic);
  }

  /**
   * @param userListID
   * @param isPublic
   * @see mitll.langtest.client.custom.ListManager#setPublic
   */
  @Override
  public void setPublicOnList(long userListID, boolean isPublic) {
    getUserListManager().setPublicOnList(userListID, isPublic);
  }

  /**
   * @param userListID
   * @param user
   * @see mitll.langtest.client.custom.ListManager#addVisitor(mitll.langtest.shared.custom.UserList)
   */
  public void addVisitor(long userListID, long user) {
    getUserListManager().addVisitor(userListID, user);
  }

  /**
   * @param userid
   * @param onlyCreated
   * @param visited
   * @return
   * @see mitll.langtest.client.custom.Navigation#showInitialState()
   * @see mitll.langtest.client.custom.ListManager#viewLessons
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public Collection<UserList<CommonShell>> getListsForUser(long userid, boolean onlyCreated, boolean visited) {
    //  if (!onlyCreated && !visited) logger.error("getListsForUser huh? asking for neither your lists nor  your visited lists.");
    return getUserListManager().getListsForUser(userid, onlyCreated, visited);
  }

  /**
   * @param search
   * @param userid
   * @return
   * @see mitll.langtest.client.custom.ListManager#viewLessons
   */
  @Override
  public Collection<UserList<CommonShell>> getUserListsForText(String search, long userid) {
    return getUserListManager().getUserListsForText(search, userid);
  }

  /**
   * @param userListID
   * @param exID
   * @return
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public void addItemToUserList(long userListID, String exID) {
    getUserListManager().addItemToUserList(userListID, exID);
  }

  /**
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation
   */
  @Override
  public void addAnnotation(String exerciseID, String field, String status, String comment, long userID) {
    getUserListManager().addAnnotation(exerciseID, field, status, comment, userID);
  }

  /**
   * @param id
   * @param isCorrect
   * @param creatorID
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  public void markReviewed(String id, boolean isCorrect, long creatorID) {
    getUserListManager().markCorrectness(id, isCorrect, creatorID);
  }

  /**
   * @param id
   * @param state
   * @param creatorID
   * @see mitll.langtest.client.qc.QCNPFExercise#markAttentionLL
   */
  public void markState(String id, STATE state, long creatorID) {
    getUserListManager().markState(id, state, creatorID);
  }

  /**
   * @param id
   * @param state
   * @param userID
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   */
  @Override
  public void setExerciseState(String id, STATE state, long userID) {
    getUserListManager().markState(id, state, userID);
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.ListManager#viewReview
   */
  @Override
  public List<UserList<CommonShell>> getReviewLists() {
    UserListManager userListManager = getUserListManager();

    long then = System.currentTimeMillis();
    List<UserList<CommonShell>> lists = new ArrayList<>();
    {
      UserList<CommonShell> defectList = userListManager.getDefectList(db.getTypeOrder());
      long now = System.currentTimeMillis();
      long diff = (now - then);
      if (diff > 30) logger.info("took " + (now - then) + " to get defect list size = " + defectList.getNumItems());

      lists.add(defectList);
    }
    {
      then = System.currentTimeMillis();
      UserList<CommonShell> commentedList = userListManager.getCommentedList(db.getTypeOrder());
      lists.add(commentedList);
      long now = System.currentTimeMillis();

      long diff = (now - then);
      if (diff > 30) {
        logger.info("took " + diff + " to get comment list size = " + commentedList.getNumItems());
      }
    }
    if (!serverProps.isNoModel()) {
      then = System.currentTimeMillis();
      UserList<CommonShell> attentionList = userListManager.getAttentionList(db.getTypeOrder());
      long now = System.currentTimeMillis();

      lists.add(attentionList);
      long diff = (now - then);

      if (diff > 30)
        logger.info("took " + (now - then) + " to get attention list size = " + attentionList.getNumItems());
    }

    return lists;
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.custom.ListManager#deleteList(Button, UserList, boolean)
   */
  @Override
  public boolean deleteList(long id) {
    return getUserListManager().deleteList(id);
  }

  /**
   * @param listid
   * @param exid
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#deleteItem
   */
  @Override
  public boolean deleteItemFromList(long listid, String exid) {
    return getUserListManager().deleteItemFromList(listid, exid, db.getTypeOrder());
  }

  /**
   * Can't check if it's valid if we don't have a model.
   *
   * @param foreign
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
   */
  @Override
  public boolean isValidForeignPhrase(String foreign, String transliteration) {
    return audioFileHelper.checkLTSOnForeignPhrase(foreign, transliteration);
  }

  /**
   * Put the new item in the database,
   * copy the audio under bestAudio
   * assign the item to a user list
   *
   * @param userListID
   * @param userExercise
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
   */
  public CommonExercise reallyCreateNewItem(long userListID, CommonExercise userExercise) {
    //logger.debug("reallyCreateNewItem : made user exercise " + userExercise + " on list " + userListID);
    getUserListManager().reallyCreateNewItem(userListID, userExercise, serverProps.getMediaDir());

    for (AudioAttribute audioAttribute : userExercise.getAudioAttributes()) {
//      logger.debug("\treallyCreateNewItem : update " + audioAttribute + " to " + userExercise.getID());
      db.getAudioDAO().updateExerciseID(audioAttribute.getUniqueID(), userExercise.getID());
    }
    //  logger.debug("\treallyCreateNewItem : made user exercise " + userExercise + " on list " + userListID);

    return userExercise;
  }

  @Override
  public Collection<CommonExercise> reallyCreateNewItems(long creator, long userListID, String userExerciseText) {
    String[] lines = userExerciseText.split("\n");
    logger.info("got " + lines.length + " lines");
    List<CommonExercise> newItems = new ArrayList<>();
    UserList<CommonShell> userListByID = db.getUserListManager().getUserListByID(userListID, Collections.emptyList());
    int n = userListByID.getNumItems();
    Set<String> currentKnownFL = new HashSet<>();
    for (CommonShell shell : userListByID.getExercises()) currentKnownFL.add(shell.getForeignLanguage());
    boolean onFirst = true;
    boolean firstColIsEnglish = false;
    for (String line : lines) {
      String[] parts = line.split("\\t");
//      logger.info("\tgot " + parts.length + " parts");
      if (parts.length > 1) {
        String fl = parts[0];
        String english = parts[1];
        if (onFirst && english.equalsIgnoreCase(getLanguage())) {
          logger.info("reallyCreateNewItems skipping header line");
          firstColIsEnglish = true;
        } else {
          if (firstColIsEnglish || (isValidForeignPhrase(english, "") && !isValidForeignPhrase(fl, ""))) {
            String temp = english;
            english = fl;
            fl = temp;
            //logger.info("flip english '" +english+ "' to fl '" +fl+ "'");
          }
          UserExercise newItem = new UserExercise(-1, UserExercise.CUSTOM_PREFIX + "_" + (n++), creator, english, fl, "");
          newItems.add(newItem);
          logger.info("reallyCreateNewItems new " + newItem);
        }
      }
      onFirst = false;
    }

    List<CommonExercise> actualItems = new ArrayList<>();
    for (CommonExercise candidate : newItems) {
      String foreignLanguage = candidate.getForeignLanguage();
      if (!currentKnownFL.contains(foreignLanguage)) {
        if (isValidForeignPhrase(foreignLanguage, candidate.getTransliteration())) {
          getUserListManager().reallyCreateNewItem(userListID, candidate, serverProps.getMediaDir());
          actualItems.add(candidate);
        } else {
          logger.info("item #" + candidate.getID() + " '" + candidate.getForeignLanguage() + "' is invalid");
        }
      }
    }
    logger.info("Returning " + actualItems.size() + "/" + lines.length);
    return actualItems;
  }

  UserListManager getUserListManager() {
    return db.getUserListManager();
  }

  /**
   * @param exercise
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise
   */
  @Override
  public CommonExercise duplicateExercise(CommonExercise exercise) {
    return db.duplicateExercise(exercise);
  }

  /**
   * @param id
   * @return
   * @see ReviewEditableExercise#confirmThenDeleteItem
   */
  public boolean deleteItem(String id) {
    boolean b = db.deleteItem(id);
    if (b) {
      // force rebuild of full trie
      buildExerciseTrie();
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
   * @see mitll.langtest.client.instrumentation.ButtonFactory#logEvent(String, String, String, String, long)
   */
  @Override
  public void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID, String device) {
    try {
      db.logEvent(id, widgetType, exid, context, userid, hitID, device);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public Collection<Event> getEvents() {
    return db.getEventDAO().getLastRows();
  }

  /**
   * @param userExercise
   * @param keepAudio
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  @Override
  public void editItem(CommonExercise userExercise, boolean keepAudio) {
    logger.debug("editItem : user exercise " + userExercise.getID() + " keep " + keepAudio);

    db.editItem(userExercise, keepAudio);
    logger.debug("editItem : now user exercise " + userExercise);
  }

  /**
   * Remember to clear any incorrect annotations.
   *
   * @param audioAttribute
   * @param exid
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   */
  @Override
  public void markAudioDefect(AudioAttribute audioAttribute, String exid) {
    logger.debug("markAudioDefect mark audio defect for " + exid + " on " + audioAttribute);

    //CommonExercise before = db.getCustomOrPredefExercise(exid);  // allow custom items to mask out non-custom items
    //int beforeNumAudio = before.getAudioAttributes().size();
    db.markAudioDefect(audioAttribute);

    CommonExercise byID = db.getCustomOrPredefExercise(exid);  // allow custom items to mask out non-custom items

    if (!byID.getMutableAudio().removeAudio(audioAttribute)) {
      String key = audioAttribute.getKey();
      logger.warn("markAudioDefect huh? couldn't remove key '" + key +
          "' : " + audioAttribute + " from ex #" + exid +
          "\n\tkeys were " + byID.getAudioRefToAttr().keySet() + " contains " + byID.getAudioRefToAttr().containsKey(key));
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
    db.getAudioDAO().addOrUpdateUser(isMale ? UserDAO.DEFAULT_MALE_ID : UserDAO.DEFAULT_FEMALE_ID, attr, pathHelper);

    String exid = attr.getExid();
    CommonExercise byID = db.getCustomOrPredefExercise(exid);
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

      db.getExerciseDAO().addOverlay(byID);

/*      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(exid);
      String adrr3 = Integer.toHexString(customOrPredefExercise.hashCode());
      logger.info("markGender getting " + adrr3 + " : " + customOrPredefExercise);
      for (AudioAttribute audioAttribute : customOrPredefExercise.getAudioAttributes()) {
        logger.debug("markGender 2 after gender change, now " + audioAttribute + " : " +audioAttribute.getUserid() + " on "+ adrr3);
      }*/

    }
    db.getSectionHelper().refreshExercise(byID);
  }

  /**
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param url
   * @param email
   * @param isMale
   * @param age
   * @param dialect
   * @param isCD
   * @param device
   * @return null if existing user
   * @see mitll.langtest.client.user.UserPassLogin#gotSignUp(String, String, String, mitll.langtest.shared.User.Kind)
   */
  @Override
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String url, String email,
                      boolean isMale, int age, String dialect, boolean isCD, String device) {
    User user = db.addUser(getThreadLocalRequest(), userID, passwordH, emailH, kind, isMale, age, dialect, "browser");
    if (user != null && !user.isEnabled()) { // user = null means existing user.
      logger.debug("user " + userID + "/" + user +
          " wishes to be a content developer. Asking for approval.");
      getEmailHelper().addContentDeveloper(url, email, user, getMailSupport());
    } else if (user == null) {
      logger.debug("no user found for id " + userID);
    } else {
      logger.debug("user " + userID + "/" + user + " is enabled.");
    }
    return user;
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, db.getUserDAO(), getMailSupport(), pathHelper);
  }

  /**
   * @return
   * @see mitll.langtest.client.user.UserTable#showDialog(mitll.langtest.client.LangTestDatabaseAsync)
   */
  public List<User> getUsers() {
    return db.getUsers();
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(int)
   */
  @Override
  public User getUserBy(long id) {
    return db.getUserDAO().getUserWhere(id);
  }

  /**
   * @param user
   * @param email
   * @param url
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.UserPassLogin#getForgotPassword()
   */
  public boolean resetPassword(String user, String email, String url) {
    logger.debug("resetPassword for " + user);
    return getEmailHelper().resetPassword(user, email, url);
  }

  /**
   * @param token
   * @param emailR - email encoded by rot13
   * @return
   * @see mitll.langtest.client.InitialUI#handleCDToken
   */
  public String enableCDUser(String token, String emailR, String url) {
    logger.info("enabling token " + token + " for email " + emailR + " and url " + url);
    return getEmailHelper().enableCDUser(token, emailR, url);
  }

  /**
   * @param token
   * @return
   * @see mitll.langtest.client.LangTest#showLogin()
   */
  @Override
  public long getUserIDForToken(String token) {
    User user = db.getUserDAO().getUserWhereResetKey(token);
    long l = (user == null) ? -1 : user.getId();
    // logger.info("for token " + token + " got user id " + l);
    return l;
  }

  @Override
  public boolean changePFor(String token, String passwordH) {
    User userWhereResetKey = db.getUserDAO().getUserWhereResetKey(token);
    if (userWhereResetKey != null) {
      db.getUserDAO().clearKey(userWhereResetKey.getId(), true);

      if (!db.getUserDAO().changePassword(userWhereResetKey.getId(), passwordH)) {
        logger.error("couldn't update user password for user " + userWhereResetKey);
      }
      return true;
    } else return false;
  }

  @Override
  public void changeEnabledFor(int userid, boolean enabled) {
    User userWhere = db.getUserDAO().getUserWhere(userid);
    if (userWhere == null) logger.error("couldn't find " + userid);
    else {
      db.getUserDAO().changeEnabled(userid, enabled);
    }
  }

  /**
   * @param emailH
   * @param email
   * @param url
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#getForgotUser()
   */
  @Override
  public boolean forgotUsername(String emailH, String email, String url) {
    User valid = db.getUserDAO().isValidEmail(emailH);
    getEmailHelper().getUserNameEmail(email, url, valid);
    return valid != null;
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
  public ResultAndTotal getResults(int start, int end, String sortInfo, Map<String, String> unitToValue, long userid, String flText, int req) {
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
    logger.info("ensure compressed audio for " + resultList.size() + " items.");
    for (MonitorResult result : resultList) {

      ensureCompressedAudio((int) result.getUserid(), db.getCustomOrPredefExercise(result.getExID()), result.getAnswer(), result.getAudioType());
    }
    return new ResultAndTotal(new ArrayList<MonitorResult>(resultList), n, req);
  }

  @Override
  public int getNumResults() {
    return db.getResultDAO().getNumResults();
  }

  /**
   * TODO : don't fetch everything from the database if you don't have to.
   * Use offset and limit to restrict?
   *
   * @param unitToValue
   * @param userid
   * @param flText
   * @return
   * @see #getResults(int, int, String, java.util.Map, long, String, int)
   */
  private List<MonitorResult> getResults(Map<String, String> unitToValue, long userid, String flText) {
    //logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText);
    boolean isNumber = false;
    try {
      Integer.parseInt(flText);
      isNumber = true;
    } catch (NumberFormatException e) {
    }
    if (isNumber) {
      List<MonitorResult> monitorResultsByID = db.getMonitorResultsWithText(db.getResultDAO().getMonitorResultsByID(flText));
      logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText + " returning " + monitorResultsByID.size() + " results...");
      return monitorResultsByID;
    }

    boolean filterByUser = userid > -1;

    Collection<MonitorResult> results = db.getMonitorResults();

    Trie<MonitorResult> trie;

    for (String type : db.getTypeOrder()) {
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
    return new ArrayList<MonitorResult>(results);
  }

  /**
   * Respond to type ahead.
   *
   * @param unitToValue
   * @param userid
   * @param flText
   * @param which
   * @return
   * @see mitll.langtest.client.result.ResultManager#getTypeaheadUsing(String, TextBox)
   */
  @Override
  public Collection<String> getResultAlternatives(Map<String, String> unitToValue, long userid, String flText, String which) {
    Collection<MonitorResult> results = db.getMonitorResults();

    logger.debug("getResultAlternatives request " + unitToValue + " userid=" + userid + " fl '" + flText + "' :'" + which + "'");

    Collection<String> matches = new TreeSet<String>();
    Trie<MonitorResult> trie;

    for (String type : db.getTypeOrder()) {
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

      Set<Long> imatches = new TreeSet<Long>();
      Collection<MonitorResult> matchesLC = trie.getMatchesLC(Long.toString(userid));

      // stop!
      if (which.equals(MonitorResult.USERID)) {
        for (MonitorResult result : matchesLC) {
          imatches.add(result.getUserid());
        }
        //logger.debug("returning " + imatches);

        for (Long m : imatches) matches.add(Long.toString(m));
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
      trie.addEntryToTrie(new ResultWrapper(result.getExID(), result));
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
        matches.add(result.getExID().trim());
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
   * @see RecordButton.RecordingListener#stopRecording(long)
   * @see RecordButton.RecordingListener#stopRecording(long)
   */
  @Override
  public AudioAnswer writeAudioFile(String base64EncodedString,
                                    AudioContext audioContext,

                                    boolean recordedWithFlash,

                                    String deviceType, String device,

                                    boolean doFlashcard,
                                    boolean recordInResults,
                                    boolean addToAudioTable,
                                    boolean allowAlternates

  ) {
    String exercise = audioContext.getId();

    boolean amas = serverProps.isAMAS();
    CommonShell exercise1 = amas ?
        db.getAMASExercise(exercise) :
        db.getCustomOrPredefExercise(exercise);  // allow custom items to mask out non-custom items

    if (exercise1 == null) {
      logger.warn(getLanguage() + " : couldn't find exercise with id '" + exercise + "'");
    }
//		else {
//			logger.info("allow alternates " + allowAlternates + " " +exercise +
//					" exercise1 " + exercise1.getForeignLanguage() + " refs " + exercise1.getRefSentences());
//		}

    AnswerInfo.RecordingInfo recordingInfo = new AnswerInfo.RecordingInfo("", "", deviceType, device, recordedWithFlash);

    DecoderOptions options = new DecoderOptions()
        .setRecordInResults(recordInResults)
        .setDoFlashcard(doFlashcard)
        .setRefRecording(addToAudioTable)
        .setAllowAlternates(allowAlternates);

    AudioAnswer audioAnswer = amas ?
        audioFileHelper.writeAMASAudioFile(base64EncodedString, db.getAMASExercise(exercise), audioContext, recordingInfo) :
        audioFileHelper.writeAudioFile(base64EncodedString,
            exercise1,
            audioContext, recordingInfo,
            options
        );

    int user = audioContext.getUserid();
    if (addToAudioTable && audioAnswer.isValid()) {
      audioAnswer.setAudioAttribute(addToAudioTable(user, audioContext.getAudioType(), exercise1, exercise, audioAnswer));
    } //else {
    // So Wade has observed that this really messes up the ASR -- silence doesn't appear as silence after you multiply
    // the signal.  Also, the user doesn't get feedback that their mic gain is too high/too low or that they
    // are speaking too softly or too loudly.

    // normalizeLevel(audioAnswer);
    // }

    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + user + " " + exercise);
      logEvent("audioRecording", "writeAudioFile", exercise, "Writing audio - got zero duration!", user, "unknown", device);
    } else {
      ensureCompressedEquivalent(user, exercise1, audioAnswer, audioContext.getAudioType());
    }

    return audioAnswer;
  }

  /**
   * @param user
   * @param exercise1
   * @param audioAnswer
   * @param audioType
   * @see #writeAudioFile
   */
  private void ensureCompressedEquivalent(int user, CommonShell exercise1, AudioAnswer audioAnswer, String audioType) {
    ensureCompressedAudio(user, exercise1, audioAnswer.getPath(), audioType);
  }

  /**
   * Have mp3 title be context sentence when recording context sentences.
   *
   * @param user
   * @param commonShell
   * @param path
   * @param audioType
   */
  private void ensureCompressedAudio(int user, CommonShell commonShell, String path, String audioType) {
    String title = commonShell == null ? "unknown" : commonShell.getForeignLanguage();
    String comment = commonShell == null ? "unknown" : commonShell.getEnglish();
    if (audioType.equals(AudioAttribute.CONTEXT_AUDIO_TYPE) && commonShell != null) {
      title = commonShell.getContext();
      comment = commonShell.getContextTranslation();
    }
    String userID = getUserID(user);
    if (userID == null) {
      logger.warn("ensureCompressedEquivalent huh? no user for " + user);
      userID = "unknown";
    }

    ensureMP3(path, new TrackInfo(title, userID, comment));
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
                                  String transliteration,
                                  String identifier,
                                  int reqid, String device) {
    AudioAnswer audioAnswer = audioFileHelper.getAlignment(base64EncodedString, textToAlign, transliteration, identifier, reqid,
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
   * <p>
   * Don't return a path to the normalized audio, since this doesn't let the recorder have feedback about how soft
   * or loud they are : https://gh.ll.mit.edu/DLI-LTEA/Development/issues/601
   *
   * @param user        who recorded audio
   * @param audioType   regular or slow
   * @param exercise1   for which exercise - how could this be null?
   * @param exerciseID  perhaps sometimes we want to override the exercise id?
   * @param audioAnswer holds the path of the temporary recorded file
   * @return AudioAttribute that represents the audio that has been added to the exercise
   * @see #writeAudioFile
   */
  private AudioAttribute addToAudioTable(int user, String audioType,
                                         CommonShell exercise1,
                                         String exerciseID,
                                         AudioAnswer audioAnswer) {
    String idToUse = exercise1 == null ? exerciseID : exercise1.getID();
    String audioTranscript = getAudioTranscript(audioType, exercise1);
    //  logger.debug("addToAudioTable user " + user + " ex " + exerciseID + " for " + audioType + " path before " + audioAnswer.getPath());

    String context = exercise1 == null ? "" : audioType.contains("context") ? exercise1.getContextTranslation() : exercise1.getEnglish();
    String permanentAudioPath = new PathWriter().
        getPermanentAudioPath(pathHelper,
            getAbsoluteFile(audioAnswer.getPath()),
            getPermanentName(user, audioType),
            true,
            idToUse,
            serverProps,
            new TrackInfo(audioTranscript, getArtist(user), context));

    AudioAttribute audioAttribute =
        db.getAudioDAO().addOrUpdate(user, idToUse, audioType, permanentAudioPath, System.currentTimeMillis(),
            audioAnswer.getDurationInMillis(), audioTranscript, (float) audioAnswer.getDynamicRange());
    // audioAnswer.setPath(audioAttribute.getAudioRef());
    logger.debug("addToAudioTable user " + user + " ex " + exerciseID + " for " + audioType + " path after " + audioAnswer.getPath() +
        " audio answer has " + audioAttribute);

    // what state should we mark recorded audio?
    setExerciseState(idToUse, user, exercise1);
    return audioAttribute;
  }

  private File getAbsoluteFile(String path) {
    return pathHelper.getAbsoluteFile(path);
  }

  private String getAudioTranscript(String audioType, CommonShell exercise1) {
    return exercise1 == null ? "" :
        audioType.equals(AudioAttribute.CONTEXT_AUDIO_TYPE) ? exercise1.getContext() : exercise1.getForeignLanguage();
  }

  private String getPermanentName(int user, String audioType) {
    return audioType + "_" + System.currentTimeMillis() + "_by_" + user + ".wav";
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
  private void setExerciseState(String exercise, int user, Shell exercise1) {
    if (exercise1 != null) {
      UserListManager userListManager = getUserListManager();
      STATE currentState = userListManager.getCurrentState(exercise);
      if (currentState == STATE.APPROVED) { // clear approved on new audio -- we need to review it again
        userListManager.setState(exercise1, STATE.UNSET, user);
      }
      userListManager.setSecondState(exercise1, STATE.RECORDED, user);
    }
  }

  @Override
  public Map<User, Integer> getUserToResultCount() {
    return db.getUserToResultCount();
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.StudentAnalysis#StudentAnalysis(LangTestDatabaseAsync, ExerciseController, ShowTab)
   */
  @Override
  public Collection<UserInfo> getUsersWithRecordings() {
    return db.getAnalysis().getUserInfo(db.getUserDAO(), MIN_RECORDINGS);
  }

  public Map<Integer, Integer> getResultCountToCount() {
    return db.getResultCountToCount();
  }

  @Override
  public Map<String, Integer> getResultByDay() {
    return db.getResultByDay();
  }

  @Override
  public Map<String, Integer> getResultByHourOfDay() {
    return db.getResultByHourOfDay();
  }

  /**
   * Filter out the default audio recordings...
   *
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doMaleFemale
   */
  @Override
  public Map<String, Float> getMaleFemaleProgress() {
    return db.getMaleFemaleProgress();
  }

  /**
   * Map of overall, male, female to list of counts (ex 0 had 7, ex 1, had 5, etc.)
   *
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doResultLineQuery
   */
  public Map<String, Map<String, Integer>> getResultPerExercise() {
    return db.getResultPerExercise();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doGenderQuery(com.google.gwt.user.client.ui.Panel)
   */
  @Override
  public Map<String, Map<Integer, Integer>> getResultCountsByGender() {
    return db.getResultCountsByGender();
  }

  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {
    return db.getDesiredCounts();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   */
  public List<Session> getSessions() {
    return db.getSessions();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   */
  public Map<String, Number> getResultStats() {
    return db.getResultStats();
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
  public AVPScoreReport getUserHistoryForList(long userid,
                                              Collection<String> ids,
                                              long latestResultID,
                                              Map<String, Collection<String>> typeToSection,
                                              long userListID) {
    if (DEBUG)
      logger.debug("getUserHistoryForList " + userid + " and " + ids + " type to section '" + typeToSection + "'");
    UserList<CommonShell> userListByID = userListID != -1 ? db.getUserListByID(userListID) : null;
    List<String> allIDs = new ArrayList<String>();
    Map<String, CollationKey> idToKey = new HashMap<String, CollationKey>();

    Collator collator = audioFileHelper.getCollator();
    if (userListByID != null) { // get exercises off the user list
      for (CommonShell exercise : userListByID.getExercises()) {
        populateCollatorMap(allIDs, idToKey, collator, exercise);
      }
    } else {
      Collection<CommonExercise> exercisesForState = (typeToSection == null || typeToSection.isEmpty()) ? getExercises() :
          db.getSectionHelper().getExercisesForSelectionState(typeToSection);
      if (DEBUG) logger.debug("\tgetUserHistoryForList found " + exercisesForState.size() + " exercises");

      for (CommonExercise exercise : exercisesForState) {
        populateCollatorMap(allIDs, idToKey, collator, exercise);
      }
    }
    AVPScoreReport userHistoryForList = db.getUserHistoryForList(userid, ids, latestResultID, allIDs, idToKey);
    if (DEBUG)
      logger.debug("getUserHistoryForList for " + typeToSection + " found " + allIDs.size() + " : " + userHistoryForList);
    return userHistoryForList;
  }

  private void populateCollatorMap(List<String> allIDs, Map<String, CollationKey> idToKey, Collator collator, CommonShell exercise) {
    String id = exercise.getID();
    allIDs.add(id);
    CollationKey collationKey = collator.getCollationKey(exercise.getForeignLanguage());
    idToKey.put(id, collationKey);
  }

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  @Override
  public UserPerformance getPerformanceForUser(long id, int minRecordings) {
    return db.getResultDAO().getPerformanceForUser(id, db.getPhoneDAO(), minRecordings, db.getExerciseIDToRefAudio());
  }

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.client.analysis.AnalysisTab#getWordScores
   */
  @Override
  public List<WordScore> getWordScores(long id, int minRecordings) {
    List<WordScore> wordScoresForUser = db.getAnalysis().getWordScoresForUser(id, minRecordings);
//    for (WordScore ws : wordScoresForUser) if (ws.getNativeAudio() != null) logger.info("got " +ws.getExID() + " " + ws.getNativeAudio());
    return wordScoresForUser;
  }

  @Override
  public PhoneReport getPhoneScores(long id, int minRecordings) {
    return db.getAnalysis().getPhonesForUser(id, minRecordings);
  }

  public void logMessage(String message) {
    if (message.length() > 10000) message = message.substring(0, 10000);
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " from client " + message;
    logger.debug(prefixedMessage);

    if (message.startsWith("got browser exception")) {
      sendEmail("Javascript Exception", prefixedMessage);
    }
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  @Override
  public void destroy() {
    refResultDecoder.setStopDecode(true);
    //stopOggCheck = true;
    super.destroy();
    db.destroy(); // TODO : redundant with h2 shutdown hook?
  }

  /**
   * Reco test option lets you run through and score all the reference audio -- if you want to see model performance
   */
  @Override
  public void init() {
    this.pathHelper = new PathHelper(getServletContext());
    readProperties(getServletContext());
    setInstallPath(db);
    audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this);

    try {
      db.preloadExercises();
      db.preloadContextPractice();
      getUserListManager().setStateOnExercises();
      db.doReport(serverProps, getServletContext().getRealPath(""), getMailSupport(), pathHelper);
    } catch (Exception e) {
      logger.error("couldn't load database " + e, e);
    }

    String mediaDir = relativeConfigDir + File.separator + serverProps.getMediaDir();
    this.refResultDecoder = new RefResultDecoder(db, serverProps, pathHelper, audioFileHelper);
    refResultDecoder.doRefDecode(getExercises(), relativeConfigDir);
    if (serverProps.isAMAS()) audioFileHelper.makeAutoCRT(relativeConfigDir);

//    refResultDecoder.fixTruncated(pathHelper);
    //checkForOgg(getExercises());
  }

  /**
   * Make sure we have ogg versions of files.
   *
   * @paramx exercises
   */
/*  private void checkForOgg(final Collection<CommonExercise> exercises) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        sleep(1000);
        ensureMP3ForAll(exercises);
      }
    }).start();

    new Thread(new Runnable() {
      @Override
      public void run() {
        ensureMP3ForAnswers();
      }
    }).start();
  }*/

/*  private void ensureMP3ForAnswers() {
    Map<Long, User> userMap = db.getUserDAO().getUserMap();
    String installPath = pathHelper.getInstallPath();
    List<Result> results = db.getResultDAO().getResults();
    logger.info("checkForOgg checking " + results.size() + " results");
    int c = 0;
    for (Result r : results) {
      if (stopOggCheck) break;

      if (r.getAnswer().endsWith(".wav") || r.getAnswer().endsWith(".mp3")) {
//        if (c++ < 10) logger.info("checking " + r.getExerciseID() + " : " + r.getAnswer());
        CommonExercise byID = db.getCustomOrPredefExercise(r.getExerciseID());  // allow custom items to mask out non-custom items
        User user = userMap.get(r.getUserid());
        boolean isThere = ensureMP3(r.getAnswer(), byID == null ? "" : byID.getForeignLanguage(), user == null ? "unk" : user.getUserID(), installPath);
        if (c++ < 10 && !isThere) logger.info("no file at " + r.getAnswer() + " for " + r.getExerciseID());
        sleep(50);
      }
    }
  }*/

/*  private void ensureMP3ForAll(Collection<CommonExercise> exercises) {
    String installPath = pathHelper.getInstallPath();
    int c = 0;
    for (CommonExercise ex : exercises) {
      if (stopOggCheck) break;
      // if (c++ < 10) logger.info("checking audio attr for " + ex.getID());

      try {
        ensureMP3s(ex, installPath);
      } catch (Exception e) {
        logger.info("got " + e.getMessage() + " for " + ex.getID());
      }
      sleep(50);
    }
  }*/
  private void sleep(int millis) {
    try {
      Thread.sleep(millis); // ???
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  private String getLanguage() {
    return serverProps.getLanguage();
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
    String h2Database = serverProps.getH2Database();
    if (h2Database == null) {
      logger.error("huh? no h2 database file specified in properties???");
    }
    db = makeDatabaseImpl(h2Database);
    shareDB(servletContext);
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

    servletContext.setAttribute(AUDIO_FILE_HELPER_REFERENCE, audioFileHelper);
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, h2DatabaseFile, serverProps, pathHelper, true, this);
  }

  /**
   * @param db
   * @return
   * @see LangTestDatabaseImpl#init()
   */
  private void setInstallPath(DatabaseImpl db) {
    String lessonPlanFile = getLessonPlan();
    if (!serverProps.getLessonPlan().startsWith("http") &&
        !new File(lessonPlanFile).exists()) {
      logger.error("couldn't find lesson plan file " + lessonPlanFile);
    }

    String mediaDir = relativeConfigDir + File.separator + serverProps.getMediaDir();
    logger.debug("setInstallPath " + pathHelper.getInstallPath() + " " + lessonPlanFile + " media " + serverProps.getMediaDir() + " rel media " + mediaDir);
    db.setInstallPath(pathHelper.getInstallPath(),
        lessonPlanFile,
        mediaDir);
  }

  private String getLessonPlan() {
    return configDir + File.separator + serverProps.getLessonPlan();
  }
}
