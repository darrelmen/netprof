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

package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.shared.ContextPractice;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.answer.Answer;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.MiniUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/7/12
 * Time: 5:50 PM
 * To change this template use File | Settings | File Templates.
 */
@RemoteServiceRelativePath("langtestdatabase")
public interface LangTestDatabase extends RemoteService {
  /**
   * @see LangTest#onModuleLoad()
   * @return
   */
  StartupInfo getStartupInfo();

  /**
   * @see LangTest#getImage(int, String, String, String, int, int, String, AsyncCallback)
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param width
   * @param height
   * @param exerciseID
   * @return
   */
  ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, String exerciseID);

  /**
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   * @param resultID
   * @param width
   * @param height
   * @return
   */
  PretestScore getResultASRInfo(int resultID, int width, int height);

  /**
   * @see ASRScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @return
   */
  PretestScore getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence,
                                   int width, int height, boolean useScoreToColorBkg, int exerciseID);

  /**
   * @see ASRScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @return
   */
  PretestScore getASRScoreForAudioPhonemes(int reqid, long resultID, String testAudioFile, String sentence,
                                           int width, int height, boolean useScoreToColorBkg, int exerciseID);

  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#addRT(AudioAnswer, int)
   * @see mitll.langtest.client.recorder.RecordButtonPanel#postAudioFile(Panel, int, String)
   * @param resultid
   * @param roundTrip
   */
  void addRoundTrip(int resultid, int roundTrip);

  /**
   * @see mitll.langtest.client.scoring.SimplePostAudioRecordButton#postAudioFile(String)
   * @param base64EncodedString
   * @param textToAlign
   * @param identifier
   * @param reqid
   * @param device
   * @return
   */
  AudioAnswer getAlignment(String base64EncodedString,
                           String textToAlign,
                           String identifier,
                           int reqid,
                           String device);

  /**
   * @see RecorderNPFHelper#getProgressInfo
   * @return
   */
  Map<String, Float> getMaleFemaleProgress();

  /**
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   * @param userid
   * @param ids
   * @param latestResultID
   * @param typeToSection
   * @param userListID
   * @return
   */
  AVPScoreReport getUserHistoryForList(int userid, Collection<Integer> ids, long latestResultID,
                                       Map<String, Collection<String>> typeToSection, long userListID);

  // User Exercise Lists -

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase(UserList, ListInterface, Panel, boolean)
   * @param foreign
   * @return
   */
  boolean isValidForeignPhrase(String foreign);

  /**
   * @see mitll.langtest.client.flashcard.FlashcardPanel#addAnnotation(String, String, String)
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation(String, String, String)
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   */
  void addAnnotation(int exerciseID, String field, String status, String comment, int userID);

  // QC State changes

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getDeleteButton
   * @param audioAttribute
   * @param exid
   */
  void markAudioDefect(AudioAttribute audioAttribute, HasID exid);

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#markGender(MiniUser, Button, AudioAttribute, RememberTabAndContent, List, Button, boolean)
   * @param attr
   * @param isMale
   */
  void markGender(AudioAttribute attr, boolean isMale);

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed(HasID)
   * @param exid
   * @param isCorrect
   * @param creatorID
   */
  void markReviewed(int exid, boolean isCorrect, int creatorID);

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#markAttentionLL(ListInterface, HasID)
   * @param exid
   * @param state
   * @param creatorID
   */
  void markState(int exid, STATE state, int creatorID);

  /**
   * @see ReviewEditableExercise#confirmThenDeleteItem()
   * @param exid
   * @return
   */
  boolean deleteItem(int exid);

  // Telemetry ---

  /**
   * @see LangTest#logMessageOnServer(String)
   * @param message
   */
  void logMessage(String message);

  /**
   * @see mitll.langtest.client.instrumentation.ButtonFactory#logEvent
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param hitID
   * @param device
   */
  void logEvent(String id, String widgetType, String exid, String context, int userid, String hitID, String device);

  /**
   * @see mitll.langtest.client.instrumentation.EventTable#show
   * @return
   */
  Collection<Event> getEvents();

  /**
   * Dialog support...
   * @see mitll.langtest.client.custom.Navigation#makeDialogWindow(LangTestDatabaseAsync, ExerciseController)
   * @return
   */
  ContextPractice getContextPractice();

  void reloadExercises();

  /**
   * AMAS ONLY
   * @see mitll.langtest.client.amas.TextResponse#getScoreForGuess
   * @param audioContext
   * @param answer
   * @param timeSpent
   * @param typeToSection
   * @return
   */
  Answer getScoreForAnswer(AudioContext audioContext, String answer, long timeSpent, Map<String, Collection<String>> typeToSection);

  /**
   * AMAS ONLY
   * @see mitll.langtest.client.amas.FeedbackRecordPanel.AnswerPanel#getChoice
   * @param resultID
   * @param correct
   */
  void addStudentAnswer(long resultID, boolean correct);

  /**
   * AMAS
   * @see mitll.langtest.client.amas.FeedbackRecordPanel#getScores(boolean)
   * @param typeToSection
   * @param userID
   * @param exids
   * @return
   */
  QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection, int userID, Collection<Integer> exids);
}
