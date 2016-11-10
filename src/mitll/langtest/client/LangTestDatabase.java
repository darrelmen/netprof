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

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.AnalysisPlot;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.shared.*;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
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
  <T extends Shell> T getExercise(String id, long userID, boolean isFlashcardReq);

  // User Management --

  /**
   * @see mitll.langtest.client.user.UserPassLogin#gotSignUp(String, String, String, User.Kind)
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
   * @return
   */
  User addUser(String userID, String passwordH, String emailH, User.Kind kind, String url, String email, boolean isMale,
               int age, String dialect, boolean isCD, String device);

  /**
   * @see mitll.langtest.client.user.UserTable#showDialog(LangTestDatabaseAsync)
   * @return
   */
  List<User> getUsers();

  /**
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin(String, String, boolean)
   * @param login
   * @param passwordH
   * @return
   */
  User userExists(String login, String passwordH);

  /**
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(int)
   * @param id
   * @return
   */
  User getUserBy(long id);


  /**
   * @see mitll.langtest.client.user.ResetPassword#getChangePasswordButton(String, Fieldset, BasicDialog.FormField, BasicDialog.FormField)
   * @param token
   * @param first
   * @return
   */
  boolean changePFor(String token, String first);

  /**
   * @see InitialUI#handleResetPass(Container, Panel, EventRegistration, String)
   * @param token
   * @return
   */
  long getUserIDForToken(String token);

  /**
   * @see mitll.langtest.client.user.UserTable#addAdminCol(LangTestDatabaseAsync, CellTable)
   * @param userid
   * @param enabled
   */
  void changeEnabledFor(int userid, boolean enabled);

  /**
   * @see UserPassLogin#getForgotUser()
   * @param emailH
   * @param email
   * @param url
   * @return
   */
  boolean forgotUsername(String emailH, String email, String url);

  /**
   * @see UserPassLogin#getForgotPassword()
   * @param userid
   * @param text
   * @param url
   * @return
   */
  boolean resetPassword(String userid, String text, String url);

  /**
   * @see InitialUI#handleCDToken(Container, Panel, String, String)
   * @param cdToken
   * @param emailR
   * @param url
   * @return
   */
  String enableCDUser(String cdToken, String emailR, String url);

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
  Collection<String> getResultAlternatives(Map<String, String> unitToValue, long userid, String flText, String which);

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
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#scoreAudio(String, long, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   * @param resultID
   * @param width
   * @param height
   * @return
   */
  PretestScore getResultASRInfo(long resultID, int width, int height);

  /**
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio(String, long, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
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
  PretestScore getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence, String transliteration,
                                   int width, int height, boolean useScoreToColorBkg, String exerciseID);

  /**
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio(String, long, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
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
  PretestScore getASRScoreForAudioPhonemes(int reqid, long resultID, String testAudioFile, String sentence, String transliteration,
                                   int width, int height, boolean useScoreToColorBkg, String exerciseID);

  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#addRT(AudioAnswer, int)
   * @param resultid
   * @param roundTrip
   */
  void addRoundTrip(long resultid, int roundTrip);

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
                           String transliteration,
                           String identifier,
                           int reqid, String device);

  // monitoring support

  /**
   * @see mitll.langtest.client.monitoring.MonitoringManager#showUserInfo(Panel)
   * @return
   */
  Map<User, Integer> getUserToResultCount();

  Collection<UserInfo> getUsersWithRecordings();

  Map<Integer, Integer> getResultCountToCount();

  Map<String,Integer> getResultByDay();
  Map<String,Integer> getResultByHourOfDay();

  Map<String, Float> getMaleFemaleProgress();

  /**
   * @see mitll.langtest.client.monitoring.MonitoringManager#doResultLineQuery(Panel)
   * @return
   */
  Map<String, Map<String, Integer>> getResultPerExercise();
  List<Session> getSessions();

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
  ResultAndTotal getResults(int start, int end, String sortInfo,Map<String, String> unitToValue, long userid, String flText, int req);

  Map<String,Number> getResultStats();

  Map<String, Map<Integer, Integer>> getResultCountsByGender();
  Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts();

  // Analysis support

  /**
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores(List)
   * @param ids
   * @return
   */
  List<CommonShell> getShells(List<String> ids);

  /**
   * @see mitll.langtest.client.analysis.AnalysisPlot#getPerformanceForUser(LangTestDatabaseAsync, long, String, int)
   * @param id
   * @param minRecordings
   * @return
   */
  UserPerformance getPerformanceForUser(long id, int minRecordings);

  /**
   * @see mitll.langtest.client.analysis.AnalysisTab#getWordScores(LangTestDatabaseAsync, ExerciseController, int, ShowTab, AnalysisPlot, Panel, int)
   * @param id
   * @param minRecordings
   * @return
   */
  List<WordScore> getWordScores(long id, int minRecordings);

  /**
   * @see mitll.langtest.client.analysis.AnalysisTab#getPhoneReport(LangTestDatabaseAsync, ExerciseController, int, Panel, AnalysisPlot, ShowTab, int)
   * @param id
   * @param minRecordings
   * @return
   */
  PhoneReport getPhoneScores(long id, int minRecordings);

  /**
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   * @param userid
   * @param ids
   * @param latestResultID
   * @param typeToSection
   * @param userListID
   * @return
   */
  AVPScoreReport getUserHistoryForList(long userid, Collection<String> ids, long latestResultID,
                                       Map<String, Collection<String>> typeToSection, long userListID);

  // User Exercise Lists -

  /**
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#addUserList(BasicDialog.FormField, TextArea, BasicDialog.FormField, boolean)
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @return
   */
  long addUserList(long userid, String name, String description, String dliClass, boolean isPublic);

  /**
   * @see mitll.langtest.client.custom.ListManager#setPublic(long, boolean)
   * @param userListID
   * @param isPublic
   */
  void setPublicOnList(long userListID, boolean isPublic);

  /**
   * @see mitll.langtest.client.custom.ListManager#addVisitor(UserList)
   * @param userListID
   * @param user
   */
  void addVisitor(long userListID, long user);

  /**
   * @see mitll.langtest.client.custom.ListManager#viewLessons(Panel, boolean, boolean, boolean, String)
   * @param userid
   * @param onlyCreated
   * @param visited
   * @return
   */
  Collection<UserList<CommonShell>> getListsForUser(long userid, boolean onlyCreated, boolean visited);

  /**
   * @see mitll.langtest.client.custom.ListManager#viewLessons(Panel, boolean, boolean, boolean, String)
   * @param search
   * @param userid
   * @return
   */
  Collection<UserList<CommonShell>> getUserListsForText(String search, long userid);

  /**
   * @see mitll.langtest.client.custom.ListManager#viewReview(Panel)
   * @return
   */
  List<UserList<CommonShell>> getReviewLists();

  /**
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices(String, ExerciseController, DropdownBase)
   * @param userListID
   * @param exID
   */
  void addItemToUserList(long userListID, String exID);

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase(UserList, ListInterface, Panel, boolean)
   * @param foreign
   * @return
   */
  boolean isValidForeignPhrase(String foreign, String transliteration);

  // Create User Exercises
  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase(UserList, ListInterface, Panel, boolean)
   * @param userListID
   * @param userExercise
   * @return
   */
  CommonExercise reallyCreateNewItem(long userListID, CommonExercise userExercise);

  /**
   * @see mitll.langtest.client.custom.ListManager#showImportItem(UserList, TabAndContent, TabAndContent, String, TabPanel)
   * @param creator
   * @param userListID
   * @param userExerciseText
   * @return
   */
  Collection<CommonExercise> reallyCreateNewItems(long creator,long userListID, String userExerciseText);

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise(Button)
   * @param id
   * @return
   */
  CommonExercise duplicateExercise(CommonExercise id);

  /**
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem(ListInterface, boolean)
   * @param userExercise
   * @param keepAudio
   */
  void editItem(CommonExercise userExercise, boolean keepAudio);

  /**
   * @see mitll.langtest.client.flashcard.FlashcardPanel#addAnnotation(String, String, String)
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   */
  void addAnnotation(String exerciseID, String field, String status, String comment, long userID);

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
  void markReviewed(String exid, boolean isCorrect, long creatorID);

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#markAttentionLL(ListInterface, HasID)
   * @param id
   * @param state
   * @param creatorID
   */
  void markState(String id, STATE state, long creatorID);

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#doAfterEditComplete(ListInterface, boolean)
   * @param id
   * @param state
   * @param userID
   */
  void setExerciseState(String id, STATE state, long userID);

  // Deleting lists and exercises from lists

  /**
   * @see mitll.langtest.client.custom.ListManager#deleteList(Button, UserList, boolean)
   * @param id
   * @return
   */
  boolean deleteList(long id);

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#deleteItem(String, long, UserList, PagingExerciseList, ReloadableContainer)
   * @param listid
   * @param exid
   * @return
   */
  boolean deleteItemFromList(long listid, String exid);

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
  void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID, String device);

  /**
   * @see mitll.langtest.client.instrumentation.EventTable#showDialog(LangTestDatabaseAsync)
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
  QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection, int userID, Collection<String> exids);
}
