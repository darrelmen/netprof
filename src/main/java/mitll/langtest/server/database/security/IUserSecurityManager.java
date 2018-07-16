package mitll.langtest.server.database.security;

import mitll.langtest.server.services.UserServiceImpl;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.user.ActiveUser;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by go22670 on 12/5/16.
 */
public interface IUserSecurityManager {
  /**
   * The key to get/set the id of the user stored in the session
   * @see NPUserSecurityManager#getUserIDFromRequest
   * @see IUserSecurityManager#logoutUser
   */
  String USER_SESSION_ATT = "user-db-id";

  /**
   *
   * @param threadLocalRequest
   * @return -1 if no user session, else user id
   * @throws DominoSessionException
   */
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

  String getSessionID(HttpServletRequest request);

  /**
   * @see UserServiceImpl#logout
   * @param request
   * @param userId
   * @param killAllSessions
   */
  void logoutUser(HttpServletRequest request, int userId, boolean killAllSessions);

  /**
   * @see mitll.langtest.server.services.OpenUserServiceImpl#loginUser
   * @see mitll.langtest.server.rest.RestUserManagement
   * @param userId
   * @param attemptedFreeTextPassword
   * @param remoteAddr
   * @param userAgent
   * @param session
   * @param strictValidity
   * @return
   */
  LoginResult getLoginResult(String userId,
                             String attemptedFreeTextPassword,
                             String remoteAddr,
                             String userAgent,
                             HttpSession session,
                             boolean strictValidity);

  /**
   *
   * @param session
   * @param loggedInUser
   * @param madeNewSession
   * @return
   */
  void setSessionUser(HttpSession session, User loggedInUser, boolean madeNewSession);

  String getRemoteAddr(HttpServletRequest request);

  /**
   *
   * @param when
   * @return
   */
  List<ActiveUser> getActiveSince(long when);
}
