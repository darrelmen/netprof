package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.FlashcardResponse;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.Site;
import mitll.langtest.shared.User;
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
  long addUser(int age, String gender, int experience, String dialect);
  long addUser(int age, String gender, int experience, String firstName, String lastName, String nativeLang, String dialect, String userID);

  List<User> getUsers();

  // answer DAO
  void addTextAnswer(int userID, Exercise exercise, int questionID, String answer);
  boolean isAnswerValid(int userID, Exercise exercise, int questionID);
  AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, int question, int user,
                             int reqid, boolean flq, String audioType, boolean doFlashcard);
  double getScoreForAnswer(Exercise e, int questionID, String answer);

  Exercise getNextUngradedExercise(String user, int expectedGrades, boolean englishOnly);

  void checkoutExerciseID(String user,String id);

  void ensureMP3(String wavFile);
  ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height);

  PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence, int width, int height, boolean useScoreToColorBkg);

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

  Map<String,Number> getResultStats();

  Map<String, Map<Integer, Integer>> getResultCountsByGender();
  Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts();

  boolean isAdminUser(long id);

  void setUserEnabled(long id, boolean enabled);

  boolean isEnabledUser(long id);

  void logMessage(String message);

  Collection<String> getTypeOrder();

  /**
   * @deprecated
   * @param typeToSection
   * @param userID
   * @return
   */
  List<ExerciseShell> getExercisesForSelectionState(Map<String, Collection<String>> typeToSection, long userID);

  FlashcardResponse getNextExercise(long userID);
  FlashcardResponse getNextExercise(long userID,Map<String, Collection<String>> typeToSection);

  void resetUserState(long userID);
  void clearUserState(long userID);

  void sendEmail(int userID, String to, String replyTo, String subject, String message, String token);

  /**
   * @deprecated
   * @return
   */
  Map<String, Map<String,Integer>> getTypeToSectionToCount();

  /**
   * @deprecated
   * @return
   */
  Map<String, Collection<String>> getTypeToSectionsForTypeAndSection(Map<String, Collection<String>> typeToSection);

  List<SectionNode> getSectionNodes();

  int getNumExercisesForSelectionState(Map<String, Collection<String>> typeToSection);

  List<Exercise> getFullExercisesForSelectionState(Map<String, Collection<String>> typeToSection, int start, int end);
}
