package mitll.langtest.client;

import audio.image.ImageType;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.*;

import java.util.List;

/**
 * The async counterpart of <code>LangTestDatabase</code>.
 */
public interface LangTestDatabaseAsync {
  void getExercises(long userID, AsyncCallback<List<Exercise>> async);
  void getExercises( AsyncCallback<List<Exercise>> async);
  void addAnswer(int usedID, Exercise exercise, int questionID, String answer, String audioFile, AsyncCallback<Void> async);
  void addUser(int age, String gender, int experience, AsyncCallback<Long> async);
  void isAnswerValid(int userID, Exercise exercise, int questionID, AsyncCallback<Boolean> async);
  void getUsers(AsyncCallback<List<User>> async);
  void getResults(AsyncCallback<List<Result>> async);

  void writeAudioFile(String base64EncodedString, String plan, String exercise, String question, String user, AsyncCallback<AudioAnswer> async);

  void getNextUngradedExercise(String user, int expectedGrades, AsyncCallback<Exercise> async);

  void checkoutExerciseID(String user,String id, AsyncCallback<Void> async);

  void addGrader(String login, AsyncCallback<Void> async);

  void graderExists(String login, AsyncCallback<Boolean> async);

  void getResultsForExercise(String exid, AsyncCallback<ResultsAndGrades> async);

  //void addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct, String grader, String gradeType, AsyncCallback<CountAndGradeID> async);
  //void addGradeEasy( String exerciseID,Grade toAdd, AsyncCallback<CountAndGradeID> async);


  void addGrade(String exerciseID, Grade grade, AsyncCallback<CountAndGradeID> async);

  void changeGrade(Grade toChange, AsyncCallback<Void> async);

  void getImageForAudioFile(String audioFile, String imageType, int width, int height, AsyncCallback<String> async);
}
