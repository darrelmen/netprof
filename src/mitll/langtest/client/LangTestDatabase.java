package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.*;

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
  boolean WRITE_ALTERNATE_COMPRESSED_AUDIO = true;

  List<Exercise> getExercises(long userID);
  List<Exercise> getExercises();
  ResultsAndGrades getResultsForExercise(String exid);
  void addAnswer(int userID, Exercise exercise, int questionID, String answer, String audioFile);
  CountAndGradeID addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct, String grader);
  void addGrader(String login);
  boolean graderExists(String login);
  long addUser(int age, String gender, int experience);
  boolean isAnswerValid(int userID, Exercise exercise, int questionID);
  List<User> getUsers();
  List<Result> getResults();

  AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, String question, String user);

  Exercise getNextUngradedExercise(String user, int expectedGrades);

  void checkoutExerciseID(String user,String id);
}
