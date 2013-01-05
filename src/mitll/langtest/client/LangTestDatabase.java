package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.*;
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

  // exerciseDAO
  List<ExerciseShell> getExerciseIds(long userID, boolean useFile, boolean arabicDataCollect);
  List<ExerciseShell> getExerciseIds(boolean useFile);

  List<Exercise> getExercises(long userID, boolean useFile, boolean arabicDataCollect);
  List<Exercise> getExercises(boolean useFile);
  ResultsAndGrades getResultsForExercise(String exid, boolean arabicTextDataCollect);

  // gradeDAO
 // CountAndGradeID addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct, String grader, String gradeType);
  CountAndGradeID addGrade(String exerciseID, Grade grade);
  void changeGrade(Grade toChange);

  // grader DAO
  void addGrader(String login);
  boolean graderExists(String login);

  // user DAO
  long addUser(int age, String gender, int experience);
  long addUser(int age, String gender, int experience, String firstName, String lastName, String nativeLang, String dialect, String userID);

  List<User> getUsers();
  List<Result> getResults();

  // answer DAO
  void addAnswer(int userID, Exercise exercise, int questionID, String answer, String audioFile);
  boolean isAnswerValid(int userID, Exercise exercise, int questionID);
  AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, String question, String user, boolean doAutoCRT, int reqid);
  double getScoreForAnswer(Exercise e, int questionID, String answer);

  Exercise getNextUngradedExercise(String user, int expectedGrades, boolean filterForArabicTextOnly);

  void checkoutExerciseID(String user,String id);

  void ensureMP3(String wavFile);
  ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height);

  PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence, int width, int height, boolean useScoreToColorBkg);

  /**
   * @deprecated
   * @param reqid
   * @param audioFile
   * @param refs
   * @param width
   * @param height
   * @return
   */
  PretestScore getScoreForAudioFile(int reqid, String audioFile, Collection<String> refs, int width, int height);

  Map<String,String> getProperties();

  Exercise getExercise(String id, boolean useFile);

  Exercise getExercise(String id, long userID, boolean useFile, boolean arabicDataCollect);

  int userExists(String login);

  Map<User, Integer> getUserToResultCount();

  Map<Integer, Integer> getResultCountToCount(boolean useFile);

  Map<String,Integer> getResultByDay();
  Map<String,Integer> getResultByHourOfDay();
}
