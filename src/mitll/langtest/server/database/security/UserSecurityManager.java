/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.security;

import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * UserSecurityManager: Provide top level security management.
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since Oct 31, 2013 6:30:34 PM
 */
public class UserSecurityManager {
  //	private static final Logger log = Logger.getLogger(UserSecurityManager.class);
  private static final Logger log = LogManager.getLogger(SecurityManager.class);

  /**
   * The key to get/set the id of the user stored in the session
   */
  public static final String USER_SESSION_ATT = "user-db-id";

  /**
   * The key to get/set the request attribute that holds the
   * user looked up by the security filter.
   */
  public static final String USER_REQUEST_ATT = "d-user";
  public static final Marker TIMING = MarkerManager.getMarker("TIMING");
  private IUserDAO userDAO;

  public UserSecurityManager(IUserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public void logoutUser(HttpServletRequest request, String userId, boolean killAllSessions) {
    long startMS = System.currentTimeMillis();
    HttpSession session = request.getSession(false);
    if (session != null) {
      log.info("Invalidating session {}", session.getId());
//			if (userId != null) {
//				getUserService().logoutUser(userId, session.getId(), killAllSessions);
//			}
      // not strictly necessary, but ...
      session.removeAttribute(USER_SESSION_ATT);
      session.invalidate();
    } else {
      log.error(">Session Activity> No session found on logout for id " + userId);
    }
    request.getSession(true);
    log.warn(">Session Activity> User logout complete for id {} on primary host in {} ms",
        () -> userId,
        //	"primary",
        () -> elapsedMS(startMS));
  }

  public static final long elapsedMS(long startMS) {
    return System.currentTimeMillis() - startMS;
  }

  public User getLoggedInUser(HttpServletRequest request) throws RestrictedOperationException, DominoSessionException {
    return getLoggedInUser(request, "", false);
  }

  /**
   * Get the currently logged in user.
   */
  public User getLoggedInUser(HttpServletRequest request, String opName, boolean throwOnFail)
      throws RestrictedOperationException, DominoSessionException {
    User user = lookupUser(request, throwOnFail);
    if (user == null && throwOnFail) {
      throwException(opName, user, null);
    }
    return user;
  }

  /**
   * Check to see if the user's session is active.
   */
  public boolean isSessionActive(User user) {
    return true;
  }

  /**
   * Get the current user out of the request. This is stored once the
   * initial filter is passed (see SecurityFilter).
   *
   * @param request The incoming request.
   * @return The user from the data store. Should not be null.
   * @throws DominoSessionException when session is empty or has no user token.
   */
  protected User getUser(HttpServletRequest request) throws DominoSessionException {
    User user = (User) request.getAttribute(USER_REQUEST_ATT);
    if (user == null) {
      throw new DominoSessionException("No user found");
    }
    return user;
  }

  /**
   * Get the current user out of the HTTP session or DB.
   *
   * @param request The incoming request.
   * @return The user from the data store. Should not be null.
   * @throws DominoSessionException when session is empty or has no user token.
   */
  protected User lookupUser(HttpServletRequest request, boolean throwOnFail)
      throws DominoSessionException {
    long startMS = System.currentTimeMillis();

    User sessUser = lookupUserFromHttpSession(request);
    if (false && sessUser == null) {
      //sessUser = lookupUserFromDBSession(request);
    } else {
      log.info("User found in HTTP session. User: {}. SID: {}", sessUser, request.getRequestedSessionId());
    }
//		if (sessUser != null && (!sessUser.isActive())) {
//			sessUser = null;
//		}
    log.info(TIMING, "Lookup User for {} complete in {}", request.getRequestURL(), elapsedMS(startMS));
    if (sessUser == null && throwOnFail) {
      log.error("About to fail due to missing user in session! SID: {}",
          request.getRequestedSessionId(), new Throwable());
      throw new DominoSessionException("Could not look up user!");
    }
    return sessUser;
  }

  /**
   * TODO : consider how to do this.
   *
   * @param request
   * @return
   * @throws DominoSessionException
   */
/*	protected User lookupUserFromDBSession(HttpServletRequest request)
      throws DominoSessionException {
		String sid = request.getRequestedSessionId();
		log.info("Lookup user from DB session. SID: {}", sid);
		return userDAO.lookupUser(sid);
	}*/

  /**
   *
   * @param request
   * @return
   * @throws DominoSessionException
   */
  protected User lookupUserFromHttpSession(HttpServletRequest request)
      throws DominoSessionException {
    User sessUser = null;
    HttpSession session = request.getSession(false);
    if (session != null) {
      Integer uidI = (Integer) session.getAttribute(USER_SESSION_ATT);
      log.info("Lookup user from HTTP session. SID={} Request SID={}, Session Created={}, isNew={}, result={}",
          session.getId(), request.getRequestedSessionId(),
          request.getSession().getCreationTime(), request.getSession().isNew(), uidI);
      if (uidI != null) {
        sessUser = userDAO.getByID(uidI);
      }
    } else {
      log.info("Lookup user from session returning null for null session. Request SID={}",
          request.getRequestedSessionId());
    }
    return sessUser;
  }

  public void throwException(String opName, User cUser, String checkDesc)
      throws RestrictedOperationException {
    RestrictedOperationException ex = new RestrictedOperationException(opName, true);
    String uname = (cUser != null) ? cUser.toString() : "null";
    log.error("Access check failed! User={}, Checking={}, op={}", uname, checkDesc, opName);
    throw ex;
  }
}
