package mitll.langtest.server.database.security;

import mitll.langtest.shared.user.User;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by go22670 on 12/5/16.
 */
public interface IUserSecurityManager {
  /**
   * The key to get/set the id of the user stored in the session
   */
  String USER_SESSION_ATT = "user-db-id";

  User getLoggedInUser(HttpServletRequest request) throws RestrictedOperationException, DominoSessionException;

  void logoutUser(HttpServletRequest request, String userId, boolean killAllSessions);
}
