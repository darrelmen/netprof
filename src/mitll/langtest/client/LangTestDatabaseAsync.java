package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.grade.CountAndGradeID;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The async counterpart of <code>LangTestDatabase</code>.
 */
public interface LangTestDatabaseAsync {
  void addTextAnswer(int usedID, CommonExercise exercise, int questionID, String answer, String answerType, AsyncCallback<Void> async);
  void userExists(String login, AsyncCallback<Integer> async);
  void addUser(int age, String gender, int experience, String nativeLang, String dialect, String userID, AsyncCallback<Long> async);
  void getUsers(AsyncCallback<List<User>> async);

  void writeAudioFile(String base64EncodedString, String plan, String exercise, int question, int user,
                      int reqid, boolean flq, String audioType, boolean doFlashcard, boolean recordInResults, AsyncCallback<AudioAnswer> async);

  void getNextUngradedExercise(String user, int expectedGrades, boolean englishOnly, AsyncCallback<CommonExercise> async);

  void checkoutExerciseID(String user,String id, AsyncCallback<Void> async);

  void addGrade(String exerciseID, Grade grade, AsyncCallback<CountAndGradeID> async);

  void changeGrade(Grade toChange, AsyncCallback<Void> async);

  void getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence, int width, int height, boolean useScoreToColorBkg, AsyncCallback<PretestScore> async);

  void getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, String exerciseID, AsyncCallback<ImageResponse> async);

  void getExercise(String id, AsyncCallback<CommonExercise> async);

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

  void logMessage(String message, AsyncCallback<Void> async);

  void getGradeCountPerExercise(AsyncCallback<Map<Integer, Map<String, Map<String, Integer>>>> async);

  void getExerciseIds(int reqID, Map<String, Collection<String>> typeToSelection, String prefix, long userListID,
                      AsyncCallback<ExerciseListWrapper> async);

  void addDLIUser(DLIUser dliUser, AsyncCallback<Void> async);

/*
  void getCompletedExercises(int user, boolean isReviewMode, AsyncCallback<Set<String>> async);
*/

  void getStartupInfo(AsyncCallback<StartupInfo> async);

  void getUserListsForText(String search, long userid, AsyncCallback<Collection<UserList>> async);

  void getListsForUser(long userid, boolean onlyCreated, boolean visited, AsyncCallback<Collection<UserList>> async);

  void addItemToUserList(long userListID, UserExercise userExercise, AsyncCallback<Void> async);

  void reallyCreateNewItem(long userListID, UserExercise userExercise, AsyncCallback<UserExercise> async);

  void addUserList(long userid, String name, String description, String dliClass, AsyncCallback<Long> async);

  void addVisitor(long userListID, long user, AsyncCallback<Void> asyncCallback);

  void editItem(UserExercise userExercise, AsyncCallback<Void> async);

  void addAnnotation(String exerciseID, String field, String status, String comment, long userID, AsyncCallback<Void> async);

  void markReviewed(String id, boolean isCorrect, long creatorID, AsyncCallback<Void> asyncCallback);

  void getDefectList(AsyncCallback<UserList> async);

  void getCommentedList(AsyncCallback<UserList> async);

  void setExerciseState(String id, CommonShell.STATE state, long userID, AsyncCallback<Void> async);

  void isValidForeignPhrase(String foreign, AsyncCallback<Boolean> async);

  void deleteList(long id, AsyncCallback<Boolean> async);

  void deleteItemFromList(long listid, String exid, AsyncCallback<Boolean> async);

  void duplicateExercise(UserExercise id, AsyncCallback<UserExercise> async);

  void deleteItem(String exid, AsyncCallback<Boolean> async);

  void getUserHistoryForList(long userid, Collection<String> ids, long latestResultID, AsyncCallback<List<AVPHistoryForList>> async);

  void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID, AsyncCallback<Void> async);

  void getEvents(AsyncCallback<List<Event>> async);

  void markState(String id, CommonShell.STATE state, long creatorID, AsyncCallback<Void> async);

  void setAVPSkip(Collection<Long> ids, AsyncCallback<Void> async);

  void getAttentionList(AsyncCallback<UserList> async);
}
