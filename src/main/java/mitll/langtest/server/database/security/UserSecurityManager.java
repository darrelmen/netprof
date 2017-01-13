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

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.IUserSessionDAO;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * UserSecurityManager: Provide top level security management.
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since Oct 31, 2013 6:30:34 PM
 */
public class UserSecurityManager implements IUserSecurityManager {
  private static final Logger log = LogManager.getLogger(UserSecurityManager.class);

  /**
   * The key to get/set the request attribute that holds the
   * user looked up by the security filter.
   */
  //public static final String USER_REQUEST_ATT = "d-user";
  //private static final Marker TIMING = MarkerManager.getMarker("TIMING");

  private Map<Integer, User> idToSession = new HashMap<>();

  private final IUserDAO userDAO;
  private final IUserSessionDAO userSessionDAO;

  /**
   * @see
   * @param userDAO
   * @param userSessionDAO
   */
  public UserSecurityManager(IUserDAO userDAO, IUserSessionDAO userSessionDAO) {
    this.userDAO = userDAO;
    this.userSessionDAO = userSessionDAO;

    //startShiro();
  }

/*  private void startShiro() {
    log.info("My First Apache Shiro Application");

    try {
      //1.
      IniSecurityManagerFactory iniSecurityManagerFactory = new IniSecurityManagerFactory("classpath:shiro.ini");

      //2.
      SecurityManager instance = iniSecurityManagerFactory.getInstance();

      //3.
      SecurityUtils.setSecurityManager(instance);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/

  /**
   * Creates a new session at the end???? why?
   * TODO : remove userId param?
   * <p>
   * When would we want to kill all sessions?
   *
   * @param request
   * @param userId          not really needed
   * @param killAllSessions remove???
   * @see mitll.langtest.server.services.UserServiceImpl#logout(String)
   */
  @Override
  public void logoutUser(HttpServletRequest request, String userId, boolean killAllSessions) {
    long startMS = System.currentTimeMillis();
    HttpSession session = getCurrentSession(request);

/*
    SecurityUtils.getSubject().logout();
*/

    if (session != null) {
      log.info("Invalidating session {}", session.getId());
      if (userId != null) {
        userSessionDAO.removeSession(session.getId());
      }
      // not strictly necessary, but ...
      session.removeAttribute(USER_SESSION_ATT);
      session.invalidate();
    } else {
      log.error(">Session Activity> No session found on logout for id " + userId);
    }

//    request.getSession(true);
    log.warn(">Session Activity> User logout complete for id {} on primary host in {} ms",
        () -> userId,
        //	"primary",
        () -> elapsedMS(startMS));
  }

  private HttpSession getCurrentSession(HttpServletRequest request) {
    return request.getSession(false);
  }

  private static long elapsedMS(long startMS) {
    return System.currentTimeMillis() - startMS;
  }

  /**
   * @param request
   * @return
   * @throws RestrictedOperationException
   * @throws DominoSessionException
   * @see LangTestDatabaseImpl#getProject()
   */
  @Override
  public User getLoggedInUser(HttpServletRequest request) throws RestrictedOperationException, DominoSessionException {
    return getLoggedInUser(request, "", false);
  }

  /**
   * Get the currently logged in user.
   *
   * @see #getLoggedInUser(HttpServletRequest)
   */
  private User getLoggedInUser(HttpServletRequest request,
                               String opName,
                               boolean throwOnFail)
      throws RestrictedOperationException, DominoSessionException {
/*
    Subject subject = SecurityUtils.getSubject();

    log.info("subject is " +subject);
    if (subject != null) {
      log.info("subject is " + subject.isRemembered());
    }*/
    User user = lookupUser(request, throwOnFail);
    if (user == null && throwOnFail) {
      throwException(opName, user, null);
    }
    return user;
  }

  /**
   * Check to see if the user's session is active.
   */
  private boolean isSessionActive(User user) {
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
/*  private User getUser(HttpServletRequest request) throws DominoSessionException {
    User user = (User) request.getAttribute(USER_REQUEST_ATT);
    if (user == null) {
      throw new DominoSessionException("No user found");
    }
    return user;
  }*/

  /**
   * Get the current user out of the HTTP session or DB.
   * <p>
   * If we can't find it in the session, we're probably the pNetProf instance.
   *
   * @param request The incoming request.
   * @return The user from the data store. Should not be null.
   * @throws DominoSessionException when session is empty or has no user token.
   */
  private User lookupUser(HttpServletRequest request, boolean throwOnFail)
      throws DominoSessionException {
    if (request == null) return null;
    //long startMS = System.currentTimeMillis();

    User sessUser = lookupUserFromHttpSession(request);
    if (sessUser == null) {
      sessUser = lookupUserFromDBSession(request);
    } else {
      log.info("User found in HTTP session. User: {}. SID: {}", sessUser, request.getRequestedSessionId());
    }
//		if (sessUser != null && (!sessUser.isActive())) {
//			sessUser = null;
//		}

    //  log.info(TIMING, "Lookup User for {} complete in {}", request.getRequestURL(), elapsedMS(startMS));
    if (sessUser == null && throwOnFail) {
      log.error("About to fail due to missing user in session! SID: {}",
          request.getRequestedSessionId(), new Throwable());
      throw new DominoSessionException("Could not look up user!");
    }
    return sessUser;
  }

  /**
   * TODO : what do we do if they come back after an hour and browser has a requested session id but
   * the server doesn't anymore???
   *
   * @param request
   * @return
   * @throws DominoSessionException
   * @see #lookupUser(HttpServletRequest, boolean)
   */
  private User lookupUserFromHttpSession(HttpServletRequest request) {
    User sessUser = null;
    HttpSession session = request != null ? getCurrentSession(request) : null;
    if (session != null) {
      Integer uidI = getUserIDFromSession(session);
/*      log.info("Lookup user from HTTP session. SID={} Request SID={}, Session Created={}, isNew={}, result={}",
          session.getID(), request.getRequestedSessionId(),
          request.getSession().getCreationTime(), request.getSession().isNew(), uidI);*/

      if (uidI != null && uidI > 0) {
        sessUser = getUserForID(uidI);
        if (sessUser == null) {
          // log.info("lookupUserFromHttpSession got cache miss for " + uidI);
          log.info("Lookup user from HTTP session. " +
                  "SID={} " +
                  "Request SID={}, " +
                  "Session Created={}, " +
                  "isNew={}, " +
                  "result={}",
              session.getId(),
              request.getRequestedSessionId(),
              request.getSession().getCreationTime(), request.getSession().isNew(), uidI);

          //  sessUser = userDAO.getByID(uidI);
          sessUser = rememberUser(uidI);
        }
//        else {
//          return userForID;
//        }
      }
    } else if (request != null) {
      log.info("Lookup user from session returning null for null session. Request SID={}",
          request.getRequestedSessionId());
    }
    return sessUser;
  }

  private int getUserIDFromSession(HttpSession session) {
    return (Integer) session.getAttribute(USER_SESSION_ATT);
  }

  /**
   * Get the userid from the session.
   * @param request
   * @return
   */
  public int getUserIDFromRequest(HttpServletRequest request) {
    HttpSession session = request != null ? getCurrentSession(request) : null;
    if (session != null) {
      return (Integer) session.getAttribute(USER_SESSION_ATT);
    }
    else {
      return -1;
    }
  }

  /**
   * TODO : consider how to do this.
   *
   * @param request
   * @return null if there is no user for the session
   * @throws DominoSessionException
   */
  private User lookupUserFromDBSession(HttpServletRequest request)
      throws DominoSessionException {
    String sid = request.getRequestedSessionId();
//    log.info("Lookup user from DB session. SID: {}", sid);
    int userForSession = userSessionDAO.getUserForSession(sid);
    if (userForSession == -1 && sid != null) {
      log.error("lookupUserFromDBSession no user for session " + sid + " in database?");
      return null;
    }
    else {
      return rememberUser(userForSession);
    }
  }

  /**
   * @param uidI
   * @return
   * @see #lookupUserFromHttpSession(HttpServletRequest)
   */
  private User rememberUser(int uidI) {
    User sessUser = userDAO.getByID(uidI);

    if (sessUser == null) {
      log.error("rememberUser huh? no user with id " + uidI);
      return null;
    } else {
      rememberIDToUser(uidI, sessUser);
      return sessUser;
    }
  }

  private synchronized void rememberIDToUser(int id, User user) {
    idToSession.put(id, user);
  }

  /**
   * @param id
   * @return
   * @see #rememberUser(int)
   */
  private User getUserForID(int id) {
    long then = System.currentTimeMillis();
    User sessUser = userDAO.getByID(id);
    long now = System.currentTimeMillis();
    if (now - then > 20) log.warn("getUserForID took " + (now - then) + " millis to get user " + id);
    return sessUser;
    //return idToSession.get(id);
  }

  private void throwException(String opName, User cUser, String checkDesc)
      throws RestrictedOperationException {
    RestrictedOperationException ex = new RestrictedOperationException(opName, true);
    String uname = (cUser != null) ? cUser.toString() : "null";
    log.error("Access check failed! User={}, Checking={}, op={}", uname, checkDesc, opName);
    throw ex;
  }
}
