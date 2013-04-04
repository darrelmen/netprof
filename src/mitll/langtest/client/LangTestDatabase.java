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
  List<ExerciseShell> getExerciseIds(long userID);
  List<ExerciseShell> getExerciseIds();

  List<Exercise> getExercises(long userID);
  List<Exercise> getExercises();
  ResultsAndGrades getResultsForExercise(String exid, boolean arabicTextDataCollect);

  // gradeDAO
  CountAndGradeID addGrade(String exerciseID, Grade grade);
  void changeGrade(Grade toChange);

  // user DAO
  long addUser(int age, String gender, int experience);
  long addUser(int age, String gender, int experience, String firstName, String lastName, String nativeLang, String dialect, String userID);

  List<User> getUsers();

  // answer DAO
  void addTextAnswer(int userID, Exercise exercise, int questionID, String answer);
  boolean isAnswerValid(int userID, Exercise exercise, int questionID);
  AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, int question, int user,
                             int reqid, boolean flq, String audioType);
  double getScoreForAnswer(Exercise e, int questionID, String answer);

  Exercise getNextUngradedExercise(String user, int expectedGrades, boolean englishOnly);

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

  Exercise getExercise(String id);

  Exercise getExercise(String id, long userID);

  int userExists(String login);

  Site getSiteByID(long id);
  boolean deploySite(long id, String name, String language, String notes);
  List<Site> getSites();

  // monitoring support

  Map<User, Integer> getUserToResultCount();

  Map<Integer, Integer> getResultCountToCount();

  Map<String,Integer> getResultByDay();
  Map<String,Integer> getResultByHourOfDay();

  Map<String, List<Integer>> getResultPerExercise();
  List<Session> getSessions();

  int getNumResults();

  List<Result> getResults(int start, int end);

//  Map<Integer, Float> getHoursToCompletion(boolean useFile);
  Map<String,Number> getResultStats();

  Map<String, Map<Integer, Integer>> getResultCountsByGender();
  Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts();

  boolean isAdminUser(long id);

  void setUserEnabled(long id, boolean enabled);

  boolean isEnabledUser(long id);

  void logMessage(String message);

  Map<String, Collection<String>> getTypeToSection();

  Map<String, Collection<String>> getTypeToSectionsForTypeAndSection(String type, String section);

  List<ExerciseShell> getExercisesForSelectionState(Map<String, String> typeToSection, long userID);

  FlashcardResponse getNextExercise(long userID);

  void resetUserState(long userID);
  void clearUserState(long userID);

  void sendEmail(int userID, String to, String replyTo, String subject, String message, String token);

  Map<String, Map<String,Integer>> getTypeToSectionToCount();
}
