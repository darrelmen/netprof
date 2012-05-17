package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.User;

import java.util.List;

/**
 * The async counterpart of <code>LangTestDatabase</code>.
 */
public interface LangTestDatabaseAsync {
  void getExercises(long userID, AsyncCallback<List<Exercise>> async);
  void addAnswer(int usedID, Exercise exercise, int questionID, String answer, String audioFile, AsyncCallback<Void> async);
  void addUser(int age, String gender, int experience, AsyncCallback<Long> async);
  void isAnswerValid(int userID, Exercise exercise, int questionID, AsyncCallback<Boolean> async);
  void getUsers(AsyncCallback<List<User>> async);
}
