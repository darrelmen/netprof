/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
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
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:50 PM
 * To change this template use File | Settings | File Templates.
 */
@RemoteServiceRelativePath("langtestdatabase")
public interface LangTestDatabase extends RemoteService {
  boolean WRITE_ALTERNATE_COMPRESSED_AUDIO = false;

  <T extends CommonShell> ExerciseListWrapper<T> getExerciseIds(int reqID, Map<String, Collection<String>> typeToSelection,
                                     String prefix, long userListID, int userID, String role,
                                     boolean onlyUnrecordedByMe, boolean onlyExamples, boolean incorrectFirstOrder,
                                     boolean onlyWithAudioAnno);

  List<CommonShell> getShells(List<String> ids);

  <T extends Shell> T getExercise(String id, long userID, boolean isFlashcardReq);


  void markAudioDefect(AudioAttribute audioAttribute, String exid);
  void markGender(AudioAttribute attr, boolean isMale);
  // user DAO

  User addUser(String userID, String passwordH, String emailH, User.Kind kind, String url, String email, boolean isMale,
               int age, String dialect, boolean isCD, String device);

  List<User> getUsers();
  User userExists(String login, String passwordH);
  User getUserBy(long id);

  // answer DAO
  AudioAnswer writeAudioFile(String base64EncodedString, String exercise, int question, int user,
                             int reqid, boolean flq, String audioType, boolean doFlashcard, boolean recordInResults,
                             boolean addToAudioTable, boolean recordedWithFlash, String deviceType, String device,
                             boolean allowAlternates);


  /**
   * TODO : return result id
   * @see mitll.langtest.client.flashcard.TextResponse#getScoreForGuess
   * @param userID
   * @param e
   * @param questionID
   * @param answer
   * @param answerType
   * @param timeSpent
   * @param typeToSection
   * @return
   */
  Answer getScoreForAnswer(long userID, String exerciseID, int questionID, String answer, String answerType, long timeSpent, Map<String, Collection<String>> typeToSection);

  void addStudentAnswer(long resultID, boolean correct);
  QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection, int userID, Collection<String> exids);

  Collection<String> getResultAlternatives(Map<String, String> unitToValue, long userid, String flText, String which);


  ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, String exerciseID);

  PretestScore getResultASRInfo(long resultID, int width, int height);

  PretestScore getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence,
                                   int width, int height, boolean useScoreToColorBkg, String exerciseID);

  PretestScore getASRScoreForAudioPhonemes(int reqid, long resultID, String testAudioFile, String sentence,
                                   int width, int height, boolean useScoreToColorBkg, String exerciseID);

  void addRoundTrip(long resultid, int roundTrip);

  AudioAnswer getAlignment(String base64EncodedString,
                           String textToAlign,
                           String identifier,
                           int reqid, String device);

  // monitoring support
  Map<User, Integer> getUserToResultCount();

  Collection<UserInfo> getUsersWithRecordings();

  Map<Integer, Integer> getResultCountToCount();

  Map<String,Integer> getResultByDay();
  Map<String,Integer> getResultByHourOfDay();

  Map<String, Float> getMaleFemaleProgress();

  Map<String, Map<String, Integer>> getResultPerExercise();
  List<Session> getSessions();

  int getNumResults();

  boolean changePFor(String token, String first);

  long getUserIDForToken(String token);

  void changeEnabledFor(int userid, boolean enabled);

  boolean forgotUsername(String emailH, String email, String url);

  ResultAndTotal getResults(int start, int end, String sortInfo,Map<String, String> unitToValue, long userid, String flText, int req);

  Map<String,Number> getResultStats();

  Map<String, Map<Integer, Integer>> getResultCountsByGender();
  Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts();

  UserPerformance getPerformanceForUser(long id, int minRecordings);

  List<WordScore> getWordScores(long id, int minRecordings);

  PhoneReport getPhoneScores(long id, int minRecordings);

  void logMessage(String message);
  void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID, String device);
  //void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID);

  AVPScoreReport getUserHistoryForList(long userid, Collection<String> ids, long latestResultID,
                                       Map<String, Collection<String>> typeToSection, long userListID);

  StartupInfo getStartupInfo();
  long addUserList(long userid, String name, String description, String dliClass, boolean isPublic);
  void setPublicOnList(long userListID, boolean isPublic);
  void addVisitor(long userListID, long user);


  Collection<UserList<CommonShell>> getListsForUser(long userid, boolean onlyCreated, boolean visited);
  Collection<UserList<CommonShell>> getUserListsForText(String search, long userid);
  List<UserList<CommonShell>> getReviewLists();


  void addItemToUserList(long userListID, String exID);

  boolean isValidForeignPhrase(String foreign);

  CommonExercise reallyCreateNewItem(long userListID, CombinedMutableUserExercise userExercise);

  CommonExercise duplicateExercise(CommonExercise id);

  void editItem(CommonExercise userExercise);

  void addAnnotation(String exerciseID, String field, String status, String comment, long userID);
  void markReviewed(String exid, boolean isCorrect, long creatorID);
  void markState(String id, STATE state, long creatorID);

  void setExerciseState(String id, STATE state, long userID);

  boolean deleteList(long id);
  boolean deleteItemFromList(long listid, String exid);
  boolean deleteItem(String exid);

  List<Event> getEvents();

  boolean resetPassword(String userid, String text, String url);
  String enableCDUser(String cdToken, String emailR, String url);

  ContextPractice getContextPractice();
}
