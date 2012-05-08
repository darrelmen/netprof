package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.Exercise;

import java.util.List;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface LangTestDatabaseAsync {
//	void test(AsyncCallback<Void> callback) throws IllegalArgumentException;
/*	void greetServer(String input, AsyncCallback<String> callback)
			throws IllegalArgumentException;*/

	void test(AsyncCallback<Void> asyncCallback);
  void getExercises(AsyncCallback<List<Exercise>> async);

  void addAnswer(String id, int questionID, String answer, AsyncCallback<Void> async);
}
