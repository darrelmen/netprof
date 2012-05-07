package mitll.langtest.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface LangTestDatabaseAsync {
//	void test(AsyncCallback<Void> callback) throws IllegalArgumentException;
/*	void greetServer(String input, AsyncCallback<String> callback)
			throws IllegalArgumentException;*/

	void test(AsyncCallback<Void> asyncCallback);
}
