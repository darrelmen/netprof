package mitll.langtest.server.database.security;

import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by go22670 on 12/5/16.
 */
public interface IUserSecurityManager {
  /**
   * The key to get/set the id of the user stored in the session
   * @see NPUserSecurityManager#getUserIDFromRequest
   * @see IUserSecurityManager#getUserIDFromSession
   * @see IUserSecurityManager#logoutUser
   */
  String USER_SESSION_ATT = "user-db-id";

  /**
   * @deprecated
   * @param threadLocalRequest
   * @return
   * @throws DominoSessionException
   */
  int getUserIDFromSession(HttpServletRequest threadLocalRequest) throws DominoSessionException;

  int getUserIDFromSessionLight(HttpServletRequest threadLocalRequest) throws DominoSessionException;

  int getUserIDFromRequest(HttpServletRequest request);

  /**
   *
   * @param request
   * @return
   * @throws RestrictedOperationException
   * @throws DominoSessionException
   */
  User getLoggedInUser(HttpServletRequest request) throws RestrictedOperationException, DominoSessionException;

  int getLoggedInUserID(HttpServletRequest request) throws RestrictedOperationException, DominoSessionException;

  void logoutUser(HttpServletRequest request, int userId, boolean killAllSessions);

  /**
   * @see mitll.langtest.server.services.OpenUserServiceImpl#loginUser
   * @see mitll.langtest.server.rest.RestUserManagement
   * @param userId
   * @param attemptedFreeTextPassword
   * @param remoteAddr
   * @param userAgent
   * @param session
   * @return
   */
  LoginResult getLoginResult(String userId,
                             String attemptedFreeTextPassword,
                             String remoteAddr,
                             String userAgent,
                             HttpSession session);

  /**
   *
   * @param session
   * @param loggedInUser
   * @param madeNewSession
   * @return
   */
  void setSessionUser(HttpSession session, User loggedInUser, boolean madeNewSession);

  String getRemoteAddr(HttpServletRequest request);
}
