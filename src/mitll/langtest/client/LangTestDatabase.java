package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.grade.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.flashcard.FlashcardResponse;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.flashcard.Leaderboard;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.grade.ResultsAndGrades;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.monitoring.Session;
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
  ExerciseListWrapper getExerciseIds(int reqID, long userID);
  ExerciseListWrapper getExerciseIds(int reqID);
  Exercise getExercise(String id);

  ResultsAndGrades getResultsForExercise(String exid, boolean arabicTextDataCollect);

  // gradeDAO
  CountAndGradeID addGrade(String exerciseID, Grade grade);
  void changeGrade(Grade toChange);

  // user DAO
  long addUser(int age, String gender, int experience, String dialect);
  long addUser(int age, String gender, int experience, String dialect, String nativeLang, String userID);

  List<User> getUsers();
  int userExists(String login);

  // answer DAO
  void addTextAnswer(int userID, Exercise exercise, int questionID, String answer);
  AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, int question, int user,
                             int reqid, boolean flq, String audioType, boolean doFlashcard);
  double getScoreForAnswer(long userID, Exercise e, int questionID, String answer);

  Exercise getNextUngradedExercise(String user, int expectedGrades, boolean englishOnly);

  void checkoutExerciseID(String user,String id);

  void ensureMP3(String wavFile);
  ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height);

  PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence, int width, int height, boolean useScoreToColorBkg);

  Map<String,String> getProperties();

  // data collect admin (site administration) ------------------------------

  Site getSiteByID(long id);
  boolean deploySite(long id, String name, String language, String notes);
  List<Site> getSites();

  boolean isAdminUser(long id);

  void setUserEnabled(long id, boolean enabled);

  boolean isEnabledUser(long id);

  // monitoring support

  Map<User, Integer> getUserToResultCount();

  Map<Integer, Integer> getResultCountToCount();

  Map<String,Integer> getResultByDay();
  Map<String,Integer> getResultByHourOfDay();

  Map<String, Map<String, Integer>> getResultPerExercise();
  List<Session> getSessions();

  int getNumResults();

  List<Result> getResults(int start, int end, String sortInfo);

  Map<String,Number> getResultStats();

  Map<String, Map<Integer, Integer>> getResultCountsByGender();
  Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts();
  Map<Integer, Map<String, Map<String, Integer>>> getGradeCountPerExercise();

  void logMessage(String message);

  Collection<String> getTypeOrder();

  /**
   * @param reqID
   * @param typeToSection
   * @param userID
   * @return
   * */
  ExerciseListWrapper getExercisesForSelectionState(int reqID, Map<String, Collection<String>> typeToSection, long userID);

  // flashcard support ------------------------------------------

  FlashcardResponse getNextExercise(long userID, boolean getNext);
  FlashcardResponse getNextExercise(long userID, Map<String, Collection<String>> typeToSection, boolean getNext);

  void resetUserState(long userID);
  void clearUserState(long userID);

  void sendEmail(int userID, String to, String replyTo, String subject, String message, String token);

  /**
   * @deprecated
   * @return
   */
  Map<String, Map<String,Integer>> getTypeToSectionToCount();

  List<SectionNode> getSectionNodes();

  int getNumExercisesForSelectionState(Map<String, Collection<String>> typeToSection);

  List<Exercise> getFullExercisesForSelectionState(Map<String, Collection<String>> typeToSection, int start, int end);

  /**
   * Game is over notification...
   * @param userid
   * @param timeTaken
   * @param selectionState
   * @return
   */
  Leaderboard postTimesUp(long userid, long timeTaken, Map<String, Collection<String>> selectionState);

  void addDLIUser(DLIUser dliUser);
}
