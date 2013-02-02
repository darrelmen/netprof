package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.*;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The async counterpart of <code>LangTestDatabase</code>.
 */
public interface LangTestDatabaseAsync {
  @Deprecated
  void getExercises(long userID, boolean useFile, boolean arabicDataCollect, AsyncCallback<List<Exercise>> async);
  @Deprecated
  void getExercises(boolean useFile, AsyncCallback<List<Exercise>> async);

  void addTextAnswer(int usedID, Exercise exercise, int questionID, String answer, AsyncCallback<Void> async);
  void addUser(int age, String gender, int experience, AsyncCallback<Long> async);
  void isAnswerValid(int userID, Exercise exercise, int questionID, AsyncCallback<Boolean> async);
  void getUsers(AsyncCallback<List<User>> async);

  void writeAudioFile(String base64EncodedString, String plan, String exercise, int question, int user,
                      boolean doAutoCRT, int reqid, boolean flq, String audioType, AsyncCallback<AudioAnswer> async);

  void getNextUngradedExercise(String user, int expectedGrades, boolean filterForArabicTextOnly, AsyncCallback<Exercise> async);

  void checkoutExerciseID(String user,String id, AsyncCallback<Void> async);

  void getResultsForExercise(String exid, boolean arabicTextDataCollect, AsyncCallback<ResultsAndGrades> async);

  void addGrade(String exerciseID, Grade grade, AsyncCallback<CountAndGradeID> async);

  void changeGrade(Grade toChange, AsyncCallback<Void> async);

  void getASRScoreForAudio(int reqid, String testAudioFile, String sentence, int width, int height, boolean useScoreToColorBkg, AsyncCallback<PretestScore> async);

  void getScoreForAudioFile(int reqid, String audioFile, Collection<String> refs, int width, int height, AsyncCallback<PretestScore> async);

  void getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, AsyncCallback<ImageResponse> async);

  void getProperties(AsyncCallback<Map<String, String>> async);

  void ensureMP3(String wavFile, AsyncCallback<Void> async);

  void getExerciseIds(long userID, boolean useFile, boolean arabicDataCollect, AsyncCallback<List<ExerciseShell>> async);

  void getExerciseIds(boolean useFile, AsyncCallback<List<ExerciseShell>> async);

  void getExercise(String id, boolean useFile, AsyncCallback<Exercise> async);

  void getExercise(String id, long userID, boolean useFile, boolean arabicDataCollect, AsyncCallback<Exercise> async);

  void getScoreForAnswer(Exercise e, int questionID, String answer, AsyncCallback<Double> async);

  void addUser(int age, String gender, int experience, String firstName, String lastName, String nativeLang, String dialect, String userID, AsyncCallback<Long> async);

  void userExists(String login, AsyncCallback<Integer> async);

  void getUserToResultCount(AsyncCallback<Map<User, Integer>> async);

  void getResultCountToCount(boolean useFile, AsyncCallback<Map<Integer, Integer>> async);

  void getResultByDay(AsyncCallback<Map<String, Integer>> async);

  void getResultByHourOfDay(AsyncCallback<Map<String, Integer>> async);

  void getResultPerExercise(boolean useFile, AsyncCallback<Map<String, List<Integer>>> async);

  void getSessions(AsyncCallback<List<Session>> async);

  void getNumResults(AsyncCallback<Integer> async);

  void getResults(int start, int end, AsyncCallback<List<Result>> async);

  void getHoursToCompletion(boolean useFile, AsyncCallback<Map<Integer, Float>> async);

  void getResultStats(AsyncCallback<Map<String, Number>> async);

  void getResultCountsByGender(boolean useFile, AsyncCallback<Map<String, Map<Integer, Integer>>> async);
}
