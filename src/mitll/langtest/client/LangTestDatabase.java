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
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.ScoringAudioPanel;
import mitll.langtest.shared.*;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.PretestScore;

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
   * @see mitll.langtest.client.list.PagingExerciseList#loadExercises(String, String, boolean)
   * @param request
   * @param <T>
   * @return
   */
  <T extends CommonShell> ExerciseListWrapper<T> getExerciseIds(ExerciseListRequest request);

  /**
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise(String)
   * @param id
   * @param userID
   * @param isFlashcardReq
   * @param <T>
   * @return
   */
  <T extends Shell> T getExercise(String id, int userID, boolean isFlashcardReq);

  // answer DAO

  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#postAudioFile(String)
   * @param base64EncodedString encoded audio bytes
   * @param audioContext
   * @param recordedWithFlash
   * @param deviceType
   * @param device
   * @param doFlashcard
   * @param recordInResults
   * @param addToAudioTable
   * @param allowAlternates
   * @return
   */
  AudioAnswer writeAudioFile(String base64EncodedString,
                             AudioContext audioContext,
                             boolean recordedWithFlash, String deviceType, String device,
                             boolean doFlashcard, boolean recordInResults,
                             boolean addToAudioTable,
                             boolean allowAlternates);

  /**
   * @see mitll.langtest.client.result.ResultManager#getTypeaheadUsing(String, TextBox)
   * @param unitToValue
   * @param userid
   * @param flText
   * @param which
   * @return
   */
  Collection<String> getResultAlternatives(Map<String, String> unitToValue, int userid, String flText, String which);

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
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   * @param resultID
   * @param width
   * @param height
   * @return
   */
  PretestScore getResultASRInfo(int resultID, int width, int height);

  /**
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
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
                                   int width, int height, boolean useScoreToColorBkg, String exerciseID);

  /**
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
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
                                   int width, int height, boolean useScoreToColorBkg, String exerciseID);

  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#addRT(AudioAnswer, int)
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
                           int reqid, String device);

  Map<String, Float> getMaleFemaleProgress();

  // Admin dialogs ---

  /**
   * @see ResultManager#showResults
   * @return
   */
  int getNumResults();

  /**
   * @see ResultManager#createProvider(int, CellTable)
   * @param start
   * @param end
   * @param sortInfo
   * @param unitToValue
   * @param userid
   * @param flText
   * @param req
   * @return
   */
  ResultAndTotal getResults(int start, int end, String sortInfo,Map<String, String> unitToValue, int userid,
                            String flText, int req);


  /**
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   * @param userid
   * @param ids
   * @param latestResultID
   * @param typeToSection
   * @param userListID
   * @return
   */
  AVPScoreReport getUserHistoryForList(int userid, Collection<String> ids, long latestResultID,
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
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   */
  void addAnnotation(String exerciseID, String field, String status, String comment, int userID);

  // QC State changes

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getDeleteButton(Panel, AudioAttribute, String, String)
   * @param audioAttribute
   * @param exid
   */
  void markAudioDefect(AudioAttribute audioAttribute, String exid);

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
  void markReviewed(String exid, boolean isCorrect, int creatorID);

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#markAttentionLL(ListInterface, HasID)
   * @param id
   * @param state
   * @param creatorID
   */
  void markState(String id, STATE state, int creatorID);

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#doAfterEditComplete(ListInterface, boolean)
   * @param id
   * @param state
   * @param userID
   */
  void setExerciseState(String id, STATE state, int userID);

  /**
   * @see ReviewEditableExercise#confirmThenDeleteItem()
   * @param exid
   * @return
   */
  boolean deleteItem(String exid);

  // Telemetry ---

  /**
   * @see LangTest#logMessageOnServer(String)
   * @param message
   */
  void logMessage(String message);

  /**
   * @see mitll.langtest.client.instrumentation.ButtonFactory#logEvent(String, String, String, String, long)
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
   * @see mitll.langtest.client.instrumentation.EventTable#showDialog(LangTestDatabaseAsync)
   * @return
   */
  List<Event> getEvents();

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
  QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection, int userID, Collection<String> exids);
}
