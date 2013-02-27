package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.Site;
import mitll.langtest.shared.User;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The async counterpart of <code>LangTestDatabase</code>.
 */
public interface LangTestDatabaseAsync {
  @Deprecated
  void getExercises(long userID, boolean arabicDataCollect, AsyncCallback<List<Exercise>> async);
  @Deprecated
  void getExercises(AsyncCallback<List<Exercise>> async);

  void addTextAnswer(int usedID, Exercise exercise, int questionID, String answer, AsyncCallback<Void> async);
  void addUser(int age, String gender, int experience, AsyncCallback<Long> async);
  void isAnswerValid(int userID, Exercise exercise, int questionID, AsyncCallback<Boolean> async);
  void getUsers(AsyncCallback<List<User>> async);

  void writeAudioFile(String base64EncodedString, String plan, String exercise, int question, int user,
                      boolean doAutoCRT, int reqid, boolean flq, String audioType, AsyncCallback<AudioAnswer> async);

  void getNextUngradedExercise(String user, int expectedGrades, boolean filterForArabicTextOnly, boolean englishOnly, AsyncCallback<Exercise> async);

  void checkoutExerciseID(String user,String id, AsyncCallback<Void> async);

  void getResultsForExercise(String exid, boolean arabicTextDataCollect, AsyncCallback<ResultsAndGrades> async);

  void addGrade(String exerciseID, Grade grade, AsyncCallback<CountAndGradeID> async);

  void changeGrade(Grade toChange, AsyncCallback<Void> async);

  void getASRScoreForAudio(int reqid, String testAudioFile, String sentence, int width, int height, boolean useScoreToColorBkg, AsyncCallback<PretestScore> async);

  void getScoreForAudioFile(int reqid, String audioFile, Collection<String> refs, int width, int height, AsyncCallback<PretestScore> async);

  void getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, AsyncCallback<ImageResponse> async);

  void getProperties(AsyncCallback<Map<String, String>> async);

  void ensureMP3(String wavFile, AsyncCallback<Void> async);

  void getExerciseIds(long userID, boolean arabicDataCollect, AsyncCallback<List<ExerciseShell>> async);

  void getExerciseIds(AsyncCallback<List<ExerciseShell>> async);

  void getExercise(String id, AsyncCallback<Exercise> async);

  void getExercise(String id, long userID, boolean arabicDataCollect, AsyncCallback<Exercise> async);

  void getScoreForAnswer(Exercise e, int questionID, String answer, AsyncCallback<Double> async);

  void addUser(int age, String gender, int experience, String firstName, String lastName, String nativeLang, String dialect, String userID, AsyncCallback<Long> async);

  void userExists(String login, AsyncCallback<Integer> async);

  void getUserToResultCount(AsyncCallback<Map<User, Integer>> async);

  void getResultCountToCount(AsyncCallback<Map<Integer, Integer>> async);

  void getResultByDay(AsyncCallback<Map<String, Integer>> async);

  void getResultByHourOfDay(AsyncCallback<Map<String, Integer>> async);

  void getResultPerExercise(AsyncCallback<Map<String, List<Integer>>> async);

  void getSessions(AsyncCallback<List<Session>> async);

  void getNumResults(AsyncCallback<Integer> async);

  void getResults(int start, int end, AsyncCallback<List<Result>> async);

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

  void getTypeToSection(AsyncCallback<Map<String, Collection<String>>> async);

  void getExercisesForSection(String type, String section, AsyncCallback<List<ExerciseShell>> async);

  void sendEmail(int userID, String from, String to, String subject, String message, AsyncCallback<Void> async);
}
