package mitll.langtest.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

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
  List<Exercise> getExercises(long userID);
  void addAnswer(int userID, Exercise exercise, int questionID, String answer, String audioFile);
  long addUser(int age, String gender, int experience);
  boolean isAnswerValid(int userID, Exercise exercise, int questionID);
  List<User> getUsers();
  List<Result> getResults();

  //void postArray(String base64EncodedByteArray);
  AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, String question, String user);

 // String getPathToAnswer(String plan, String exercise, String question, String user);
}
