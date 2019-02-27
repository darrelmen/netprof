/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.security;

import mitll.langtest.server.services.UserServiceImpl;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.user.ActiveUser;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.User;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by go22670 on 12/5/16.
 */
public interface IUserSecurityManager {
  /**
   * The key to get/set the id of the user stored in the session
   * @see NPUserSecurityManager#setSessionUserAndRemember(HttpSession, int) (HttpSession)
   * @see NPUserSecurityManager#getUserIDFromSession(HttpSession)
   * @see NPUserSecurityManager#logoutUser
   * @see mitll.langtest.server.filter.ForceNocacheFilter#doFilter(ServletRequest, ServletResponse, FilterChain)
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
  List<ActiveUser> getActiveTeachers();

  List<ActiveUser> getTeachers();
}
