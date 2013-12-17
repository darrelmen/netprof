package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.Site;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.FlashcardResponse;
import mitll.langtest.shared.flashcard.Leaderboard;
import mitll.langtest.shared.grade.CountAndGradeID;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.grade.ResultsAndGrades;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The async counterpart of <code>LangTestDatabase</code>.
 */
public interface LangTestDatabaseAsync {
  void addTextAnswer(int usedID, Exercise exercise, int questionID, String answer, AsyncCallback<Void> async);
  void addUser(int age, String gender, int experience, String dialect, AsyncCallback<Long> async);
  void userExists(String login, AsyncCallback<Integer> async);
  void addUser(int age, String gender, int experience, String nativeLang, String dialect, String userID, AsyncCallback<Long> async);
  void getUsers(AsyncCallback<List<User>> async);

  void writeAudioFile(String base64EncodedString, String plan, String exercise, int question, int user,
                      int reqid, boolean flq, String audioType, boolean doFlashcard, boolean recordInResults, AsyncCallback<AudioAnswer> async);

  void getNextUngradedExercise(String user, int expectedGrades, boolean englishOnly, AsyncCallback<Exercise> async);

  void checkoutExerciseID(String user,String id, AsyncCallback<Void> async);

  void getResultsForExercise(String exid, boolean arabicTextDataCollect, AsyncCallback<ResultsAndGrades> async);

  void addGrade(String exerciseID, Grade grade, AsyncCallback<CountAndGradeID> async);

  void changeGrade(Grade toChange, AsyncCallback<Void> async);

  void getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence, int width, int height, boolean useScoreToColorBkg, AsyncCallback<PretestScore> async);

  void getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, AsyncCallback<ImageResponse> async);

  void getExercise(String id, AsyncCallback<Exercise> async);

  void getScoreForAnswer(long userID, Exercise e, int questionID, String answer, String answerType, AsyncCallback<Double> async);

  void getUserToResultCount(AsyncCallback<Map<User, Integer>> async);

  void getResultCountToCount(AsyncCallback<Map<Integer, Integer>> async);

  void getResultByDay(AsyncCallback<Map<String, Integer>> async);

  void getResultByHourOfDay(AsyncCallback<Map<String, Integer>> async);

  void getResultPerExercise(AsyncCallback<Map<String, Map<String, Integer>>> async);

  void getSessions(AsyncCallback<List<Session>> async);

  void getNumResults(AsyncCallback<Integer> async);

  void getResults(int start, int end, String sortInfo, AsyncCallback<List<Result>> async);

  void getResultStats(AsyncCallback<Map<String, Number>> async);

  void getResultCountsByGender(AsyncCallback<Map<String, Map<Integer, Integer>>> async);

  void getDesiredCounts(AsyncCallback<Map<String, Map<Integer, Map<Integer, Integer>>>> async);

  void getSiteByID(long id, AsyncCallback<Site> async);

  void deploySite(long id, String name, String language, String notes, AsyncCallback<Boolean> async);

  void getSites(AsyncCallback<List<Site>> async);

  void isAdminUser(long id, AsyncCallback<Boolean> async);

  void setUserEnabled(long id, boolean enabled, AsyncCallback<Void> async);

  void isEnabledUser(long id, AsyncCallback<Boolean> async);

  void logMessage(String message, AsyncCallback<Void> async);

  void sendEmail(int userID, String to, String replyTo, String subject, String message, String token, AsyncCallback<Void> async);

  void getNextExercise(long userID, boolean getNext, AsyncCallback<FlashcardResponse> async);
  void getNextExercise(long userID, Map<String, Collection<String>> typeToSection, boolean getNext, AsyncCallback<FlashcardResponse> async);

  void resetUserState(long userID, AsyncCallback<Void> async);

  void clearUserState(long userID, AsyncCallback<Void> async);

  void getTypeToSectionToCount(AsyncCallback<Map<String, Map<String, Integer>>> async);

  void getNumExercisesForSelectionState(Map<String, Collection<String>> typeToSection, AsyncCallback<Integer> async);

  void getFullExercisesForSelectionState(Map<String, Collection<String>> typeToSection, int start, int end, AsyncCallback<List<Exercise>> async);

  void getGradeCountPerExercise(AsyncCallback<Map<Integer, Map<String, Map<String, Integer>>>> async);

  void getExerciseIds(int reqID, AsyncCallback<ExerciseListWrapper> async);

  void getExerciseIds(int reqID, long userID, AsyncCallback<ExerciseListWrapper> async);

  void getExerciseIds(int reqID, long userID, String prefix, long userListID, AsyncCallback<ExerciseListWrapper> async);

  /**
   * @param reqID
   * @param typeToSection
   * @param userID
   * @return
   */
  void getExercisesForSelectionState(int reqID, Map<String, Collection<String>> typeToSection, long userID, AsyncCallback<ExerciseListWrapper> async);


  void postTimesUp(long userid, long timeTaken, Map<String, Collection<String>> selectionState, AsyncCallback<Leaderboard> async);

  void addDLIUser(DLIUser dliUser, AsyncCallback<Void> async);

  void getCompletedExercises(int user, boolean isReviewMode, AsyncCallback<Set<String>> async);

  void getExercisesForSelectionState(int reqID, Map<String, Collection<String>> typeToSection, long userID, String prefix, AsyncCallback<ExerciseListWrapper> async);

  void getStartupInfo(AsyncCallback<StartupInfo> async);

  void getUserListsForText(String search, AsyncCallback<Collection<UserList>> async);

  void getListsForUser(long userid, boolean onlyCreated, boolean getExercises, AsyncCallback<Collection<UserList>> async);

  void addItemToUserList(long userListID, UserExercise userExercise, AsyncCallback<Void> async);

  void createNewItem(long userid, String english, String foreign, AsyncCallback<UserExercise> async);

  void reallyCreateNewItem(long userListID, UserExercise userExercise,AsyncCallback<UserExercise> async);

  void addUserList(long userid, String name, String description, String dliClass, AsyncCallback<Long> async);

  void addVisitor(UserList ul, long user, AsyncCallback<Void> asyncCallback);

  void editItem(UserExercise userExercise, AsyncCallback<Void> async);

  void addAnnotation(String exerciseID, String field, String status, String comment, AsyncCallback<Void> async);

  void markReviewed(String id, boolean isCorrect, AsyncCallback<Void> asyncCallback);

  void getReviewList(AsyncCallback<UserList> async);
}
