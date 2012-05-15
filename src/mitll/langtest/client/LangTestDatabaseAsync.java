package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.Exercise;

import java.util.List;

/**
 * The async counterpart of <code>LangTestDatabase</code>.
 */
public interface LangTestDatabaseAsync {
  void getExercises(AsyncCallback<List<Exercise>> async);
  void addAnswer(Exercise exercise, int questionID, String answer, String audioFile, AsyncCallback<Void> async);

  void addUser(int age, String gender, AsyncCallback<Integer> async);
}
