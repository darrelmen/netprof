package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.*;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.List;

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
  List<Exercise> getExercises(long userID);
  List<Exercise> getExercises();
  ResultsAndGrades getResultsForExercise(String exid);

  // gradeDAO
 // CountAndGradeID addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct, String grader, String gradeType);
  CountAndGradeID addGrade(String exerciseID, Grade grade);
  void changeGrade(Grade toChange);

  // grader DAO
  void addGrader(String login);
  boolean graderExists(String login);

  // user DAO
  long addUser(int age, String gender, int experience);
  List<User> getUsers();
  List<Result> getResults();

  // answer DAO
  void addAnswer(int userID, Exercise exercise, int questionID, String answer, String audioFile);
  boolean isAnswerValid(int userID, Exercise exercise, int questionID);
  AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, String question, String user);

  Exercise getNextUngradedExercise(String user, int expectedGrades);

  void checkoutExerciseID(String user,String id);

  ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height);

  PretestScore getScoreForAudioFile(int reqid, String testAudioFile, String refAudioFile, String sentence, int width, int height);

  PretestScore getScoreForAudioFile(int reqid, String audioFile, Collection<String> refs, int width, int height);
}
