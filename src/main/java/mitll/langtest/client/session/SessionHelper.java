package mitll.langtest.client.session;

import mitll.langtest.client.services.UserService;

/**
 * Created by go22670 on 1/12/17.
 */
public class SessionHelper {
  UserService userServiceAsync;

  SessionHelper(UserService userServiceAsync) {
    this.userServiceAsync = userServiceAsync;
  }
  public void restoreSession() {

  }
}
