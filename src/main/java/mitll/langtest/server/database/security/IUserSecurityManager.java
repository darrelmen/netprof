package mitll.langtest.server.database.security;

import mitll.langtest.shared.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by go22670 on 12/5/16.
 */
public interface IUserSecurityManager {
  /**
   * The key to get/set the id of the user stored in the session
   * @see UserSecurityManager#getUserIDFromRequest
   * @see UserSecurityManager#getUserIDFromSession
   * @see UserSecurityManager#logoutUser
   */
  String USER_SESSION_ATT = "user-db-id";

  int getUserIDFromRequest(HttpServletRequest request);

  /**
   *
   * @param request
   * @param response
   * @return
   * @throws RestrictedOperationException
   * @throws DominoSessionException
   */
  User getLoggedInUser(HttpServletRequest request, HttpServletResponse response) throws RestrictedOperationException, DominoSessionException;

  void logoutUser(HttpServletRequest request, String userId, boolean killAllSessions);
}
