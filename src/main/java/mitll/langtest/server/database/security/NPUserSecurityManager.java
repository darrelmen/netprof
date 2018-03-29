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
import mitll.langtest.server.database.user.IUserSessionDAO;
import mitll.langtest.server.services.MyRemoteServiceServlet;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.user.FirstLastUser;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickUserSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.*;

/**
 * NPUserSecurityManager: Provide top level security management.
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since Oct 31, 2013 6:30:34 PM
 */
public class NPUserSecurityManager implements IUserSecurityManager {
  private static final Logger log = LogManager.getLogger(NPUserSecurityManager.class);

  private final IUserDAO userDAO;
  private final IUserSessionDAO userSessionDAO;
  private static boolean DEBUG = false;

  /**
   * Only made once but shared with servlets.
   *
   * @param userDAO
   * @param userSessionDAO
   * @see
   */
  public NPUserSecurityManager(IUserDAO userDAO,
                               IUserSessionDAO userSessionDAO) {
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
   * @param userId
   * @param attemptedFreeTextPassword
   * @param remoteAddr
   * @param userAgent
   * @param session
   * @param strictValidity
   * @return
   */
  @Override
  public LoginResult getLoginResult(String userId,
                                    String attemptedFreeTextPassword,
                                    String remoteAddr,
                                    String userAgent,
                                    HttpSession session, boolean strictValidity) {
    User loggedInUser = userDAO.loginUser(
        userId,
        attemptedFreeTextPassword,
        userAgent,
        remoteAddr,
        session.getId());

    boolean success = loggedInUser != null;

    logActivity(userId, remoteAddr, userAgent, loggedInUser, success);

    if (success) {
      return getValidLogin(session, loggedInUser, strictValidity);
    } else {
      return getInvalidLoginResult(userDAO.getUserByID(userId));
    }
  }

  private void logActivity(String userId, String remoteAddr, String userAgent, User loggedInUser, boolean success) {
    String resultStr = success ? " was successful" : " failed";
    log.info(">Session Activity> User login for id " + userId + resultStr +
        ". IP: " + remoteAddr +
        ", UA: " + userAgent +
        (success ? ", user: " + loggedInUser.getID() : ""));
  }

  /**
   * @param session
   * @param loggedInUser
   * @param strictValidity
   * @return
   * @see IUserSecurityManager#getLoginResult
   */
  @NotNull
  private LoginResult getValidLogin(HttpSession session, User loggedInUser, boolean strictValidity) {
    LoginResult loginResult = new LoginResult(loggedInUser);
    boolean valid = strictValidity ? loggedInUser.isValid() : loggedInUser.isForgivingValid();
    if (valid) {
      setSessionUser(session, loggedInUser, true);
    } else {
      log.info("getValidLogin user " + loggedInUser + "\n\tis not valid ");
      loginResult = new LoginResult(loggedInUser, LoginResult.ResultType.MissingInfo);
    }
    return loginResult;
  }

  private LoginResult getInvalidLoginResult(User loggedInUser) {
    if (loggedInUser == null) {
      return new LoginResult(LoginResult.ResultType.Failed);
    } else {
      return new LoginResult(loggedInUser, LoginResult.ResultType.BadPassword);
    }
  }

  /**
   * Adds startup info to user... when would this be needed outside of login?
   *
   * @param session
   * @param loggedInUser
   * @param madeNewSession
   * @return
   * @see #getValidLogin
   * @see #lookupUserFromSessionOrDB
   */
  public void setSessionUser(HttpSession session, User loggedInUser, boolean madeNewSession) {
    log.info("setSessionUser - made session - " + session + "\n\tnewSession " + madeNewSession +
        "\n\tuser - " + loggedInUser);
    try {
      long then = System.currentTimeMillis();
      setSessionUserAndRemember(session, loggedInUser.getID());

      // why do this?
      userDAO.getDatabase().setStartupInfo(loggedInUser);
      long now = System.currentTimeMillis();
      if (now - then > 40) {
        log.info("setSessionUser took " + (now - then) + " to add session to db");
      }
    } catch (Exception e) {
      log.error("setSessionUser got " + e, e);
    }
  }

  private void setSessionUserAndRemember(HttpSession session, int id1) {
    session.setAttribute(USER_SESSION_ATT, id1);
    String sessionID = session.getId();

    userSessionDAO.add(
        new SlickUserSession(-1,
            id1,
            sessionID,
            "",
            "",
            new Timestamp(System.currentTimeMillis())));

    logSetSession(session, sessionID);
  }

  private void logSetSession(HttpSession session1, String sessionID) {
    log.info("setSessionUser : Adding user to " +
        "\nsession        " + sessionID +
        "\nlookup user    " + getUserIDFromSession(session1) +
        "\nsession.isNew= " + session1.isNew() +
        "\ncreated        " + session1.getCreationTime() + " or " + (new Date(session1.getCreationTime())) +
        "\nattributes     " + getAttributesFromSession(session1));
  }


  @NotNull
  private String getAttributesFromSession(HttpSession session) {
    StringBuilder atts = new StringBuilder("Atts: [ ");
    Enumeration<String> attEnum = session.getAttributeNames();
    while (attEnum.hasMoreElements()) {
      atts.append(attEnum.nextElement() + ", ");
    }
    atts.append("]");
    return atts.toString();
  }

  /**
   * TODO : remove userId param?
   * <p>
   * When would we want to kill all sessions?
   *
   * @param request
   * @param userId          not really needed
   * @param killAllSessions remove???
   * @see mitll.langtest.server.services.UserServiceImpl#logout
   */
  @Override
  public void logoutUser(HttpServletRequest request, int userId, boolean killAllSessions) {
    long startMS = System.currentTimeMillis();
    HttpSession session = getCurrentSession(request);
    if (session != null) {
      log.info("logoutUser : Invalidating session {}", session.getId());
///      if (killAllSessions) {
      userSessionDAO.removeAllSessionsForUser(userId);
//      } else {
//        userSessionDAO.removeSession(session.getId());
//      }
      // not strictly necessary, but ...
      session.removeAttribute(USER_SESSION_ATT);
      session.invalidate();
    } else {
      log.warn(">Session Activity> No session found on logout for id " + userId);
    }

    log.warn(">Session Activity> User logout complete for id {} on primary host in {} ms",
        () -> userId,
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
   * @see MyRemoteServiceServlet#getSessionUser
   */
  @Override
  public User getLoggedInUser(HttpServletRequest request) throws RestrictedOperationException, DominoSessionException {
    return getLoggedInUser(request, "", false);
  }

  @Override
  public int getLoggedInUserID(HttpServletRequest request) throws RestrictedOperationException, DominoSessionException {
    return getLoggedInUserLight(request, "", false);
  }


  /**
   * Get the currently logged in user.
   *
   * @see #getUserIDFromSession(HttpServletRequest)
   * @see #getLoggedInUser(HttpServletRequest)
   */
  private User getLoggedInUser(HttpServletRequest request,
                               String opName,
                               boolean throwOnFail)
      throws RestrictedOperationException, DominoSessionException {
    User user = lookupUserFromSessionOrDB(request, throwOnFail);
    if (user == null && throwOnFail) {
      throwException(opName, user, null);
    }
    return user;
  }

  private Integer getLoggedInUserLight(HttpServletRequest request,
                                       String opName,
                                       boolean throwOnFail)
      throws RestrictedOperationException, DominoSessionException {
    int userID = lookupUserIDFromSessionOrDB(request, throwOnFail);
    if (userID == -1 && throwOnFail) {
      throwException(opName, userID, null);
    }
    return userID;
  }

  /**
   * Get the current user out of the HTTP session or DB.
   * <p>
   * If we can't find it in the session, we're probably the pNetProf instance.
   *
   * @param request The incoming request.
   * @return The user from the data store. Should not be null.
   * @throws DominoSessionException when session is empty or has no user token.
   * @see #getLoggedInUser(HttpServletRequest, String, boolean)
   */
  private User lookupUserFromSessionOrDB(HttpServletRequest request, boolean throwOnFail)
      throws DominoSessionException {
    if (request == null) {
      log.warn("lookupUserFromSessionOrDB huh? no request???");
      return null;
    }
    User sessUser = lookupUserFromHttpSession(request);
    if (sessUser == null) {
      sessUser = lookupUserFromDBSession(request);
      if (sessUser != null) {
        // HttpSession session = getCurrentOrNewSession(request);

        boolean madeNewSession = false;
        HttpSession session = getCurrentSession(request);
        if (session == null) {
          madeNewSession = true;
          session = getNewHttpSession(request);
        }

        // why???
        if (sessUser.getStartupInfo() == null || madeNewSession) {
          setSessionUser(session, sessUser, madeNewSession);
        }
      } else {
        if (DEBUG)
          log.info("lookupUserFromSessionOrDB no user for session - " + getCurrentSession(request) + " logged out?");
      }

    } else {
      if (DEBUG)
        log.info("lookupUserFromSessionOrDB User found in HTTP session. User: {}. SID: {}", sessUser, request.getRequestedSessionId());
    }

    //  log.info(TIMING, "Lookup User for {} complete in {}", request.getRequestURL(), elapsedMS(startMS));
    if (sessUser == null && throwOnFail) {
/*      log.warn("About to fail due to missing user in session! SID: {}",
          request.getRequestedSessionId()
          //, new Throwable()
      );*/
      throw new DominoSessionException("Could not look up user!");
    }
    return sessUser;
  }

  private int lookupUserIDFromSessionOrDB(HttpServletRequest request, boolean throwOnFail)
      throws DominoSessionException {
    if (request == null) {
      log.warn("lookupUserFromSessionOrDB huh? no request???");
      return -1;
    }
    Integer sessUserID = lookupUserIDFromHttpSession(request);
    if (sessUserID == null) {
      String sid = request.getRequestedSessionId();
      // OK look in the database - we're on one of the hydra machines which has a different tomcat?
      sessUserID = sid == null ? -1 : userSessionDAO.getUserForSession(sid);

      if (sessUserID != -1) {
        HttpSession currentOrNewSession = getCurrentOrNewSession(request);

        if (currentOrNewSession == null) {
          log.error("huh? couldn't create a new session?");
        } else {
          setSessionUserAndRemember(currentOrNewSession, sessUserID);
        }

      } else {
        if (DEBUG)
          log.info("lookupUserFromSessionOrDB no user for session - " + getCurrentSession(request) + " logged out?");
      }

    } else {
      if (DEBUG)
        log.info("lookupUserFromSessionOrDB User found in HTTP session. User: {}. SID: {}", sessUserID, request.getRequestedSessionId());
    }

    //  log.info(TIMING, "Lookup User for {} complete in {}", request.getRequestURL(), elapsedMS(startMS));
    if (sessUserID == -1 && throwOnFail) {
      log.warn("About to fail due to missing user in session! SID: {}",
          request.getRequestedSessionId()
          //, new Throwable()
      );
      throw new DominoSessionException("Could not look up user!");
    }
    return sessUserID;
  }

  @Nullable
  private HttpSession getCurrentOrNewSession(HttpServletRequest request) {
    HttpSession session = getCurrentSession(request);
    if (session == null) {
      session = getNewHttpSession(request);
    } else {
      if (DEBUG) log.info("lookupUserFromSessionOrDB found current session - ");
    }
    return session;
  }

  private HttpSession getNewHttpSession(HttpServletRequest request) {
    if (DEBUG) log.info("lookupUserFromSessionOrDB note - no current session - ");
    HttpSession session = null;
    try {
      session = request.getSession();
      if (DEBUG) log.info("lookupUserFromSessionOrDB note - made session - ");
    } catch (Exception e) {
      log.error("got " + e, e);
    }
    return session;
  }

  public String getRemoteAddr(HttpServletRequest request) {
    String remoteAddr = request.getHeader("X-FORWARDED-FOR");
    if (remoteAddr == null || remoteAddr.isEmpty()) {
      remoteAddr = request.getRemoteAddr();
    }
    return remoteAddr;
  }

  /**
   * TODO : what do we do if they come back after an hour and browser has a requested session id but
   * the server doesn't anymore???
   *
   * @param request
   * @return
   * @throws DominoSessionException
   * @see #lookupUserFromSessionOrDB(HttpServletRequest, boolean)
   */
  private User lookupUserFromHttpSession(HttpServletRequest request) {
    User sessUser = null;
    long then = System.currentTimeMillis();

    HttpSession session = request != null ? getCurrentSession(request) : null;

    if (session != null) {
      int uidI = getUserIDFromSession(session);
      log.info("lookupUserFromHttpSession Lookup user from HTTP session. " +
              //"SID={} " +
              "Request SID={}, Session Created={}, isNew={}, result={}",
          //session.getID(),
          request.getRequestedSessionId(),
          request.getSession().getCreationTime(), request.getSession().isNew(), uidI);

      if (uidI > 0) {
        sessUser = getUserForID(uidI);
        if (sessUser == null) {
          log.info("lookupUserFromHttpSession Lookup user from HTTP session. " +
                  "SID={} " +
                  "Request SID={}, " +
                  "Session Created={}, " +
                  "isNew={}, " +
                  "result={}",
              session.getId(),
              request.getRequestedSessionId(),
              request.getSession().getCreationTime(),
              request.getSession().isNew(),
              uidI);
          //    sessUser = userDAO.getByID(uidI);
        }
      }
    } else if (request != null) {
      log.info("Lookup user from session returning null for null session. Request SID={}", request.getRequestedSessionId());
    }
    long now = System.currentTimeMillis();

    if (now - then > 10L) {
      log.info("took " + (now - then) + " to lookup user from session");
    }

    return sessUser;
  }

  private Integer lookupUserIDFromHttpSession(HttpServletRequest request) {
    //long then = System.currentTimeMillis();
    HttpSession session = request != null ? getCurrentSession(request) : null;

    if (session != null) {
      Integer uidI = getUserIDFromSession(session);

      if (DEBUG) {
        log.info("lookupUserIDFromHttpSession Lookup user from HTTP session. " +
                //"SID={} " +
                "Request SID={}, Session Created={}, isNew={}, result={}",
            //session.getID(),
            request.getRequestedSessionId(),
            request.getSession().getCreationTime(), request.getSession().isNew(), uidI);
      }

      return uidI;
    } else if (request != null) {
      log.info("lookupUserIDFromHttpSession Lookup user from session returning null for null session. Request SID={}", request.getRequestedSessionId());
    }

    return null;
  }

  /**
   * Initially, we have no userid in the session, then we log in, and we add the userid
   * to the session (which is transient) and store a cookie on the client
   * <p>
   * So - several cases-
   * 1) active session, with id (we've logged in recently) - get id from session
   * 2) session has timed out on server, but client doesn't know it - server has no idea about the session
   * - we lookup session id in database, and we're ok
   * - every time we make a new session, we store a new cookie on the client
   * 3) we're accessing a service on a tomcat instance that doesn't have the session
   * - lookup session in database
   * 4) close browser, bring it back up - client has no session, but server did
   * - use cookie to find userid, and put back on a new session
   * - ideally only the startup method should know about this case...
   * 5) if log out, just one session info should be cleared - or all???
   * ? what if have two browsers open - logged in in one, logged out in other?
   *
   * @return
   */
  @Override
  public int getUserIDFromSession(HttpServletRequest threadLocalRequest) throws DominoSessionException {
    int userIDFromSession = getUserIDFromRequest(threadLocalRequest);
    if (userIDFromSession == -1) {
      // it's not in the current session - can we recover it?
      try {
        User sessionUser = getLoggedInUser(threadLocalRequest, "", true);
        int i = (sessionUser == null) ? -1 : sessionUser.getID();
        if (i == -1) { // this shouldn't happen if we throw on bad sessions
          log.error("getUserIDFromSession huh? couldn't get user from session or database?");
        }
        return i;
      } catch (DominoSessionException e) {
        log.warn("getUserIDFromSession : session exception : " + e);
        throw e;
      }
    } else {
//      log.info("getUserIDFromSession found user id from session = " + userIDFromSession);
      return userIDFromSession;
    }
  }

  //  @Override
  @Override
  public int getUserIDFromSessionLight(HttpServletRequest threadLocalRequest) throws DominoSessionException {
    int userIDFromSession = getUserIDFromRequest(threadLocalRequest);
    if (userIDFromSession == -1) {
      // it's not in the current session - can we recover it?
      try {
        int sessionUserID = getLoggedInUserLight(threadLocalRequest, "", true);

        if (sessionUserID == -1) { // this shouldn't happen if we throw on bad sessions
          log.error("getUserIDFromSession huh? couldn't get user from session or database?");
        }

        return sessionUserID;
      } catch (DominoSessionException e) {
        log.warn("getUserIDFromSession : session exception : " + e);
        throw e;
      }
    } else {
//      log.info("getUserIDFromSession found user id from session = " + userIDFromSession);
      return userIDFromSession;
    }
  }

  /**
   * Get the userid from the session.
   *
   * @param request
   * @return
   * @see mitll.langtest.server.DatabaseServlet#getProjectID
   */
  public int getUserIDFromRequest(HttpServletRequest request) {
    if (request == null) {
      log.error("getUserIDFromRequest how can request be null?", new Exception());
      return -1;
    } else {
      HttpSession session = getCurrentSession(request);
      if (session == null) {
//        log.info("getUserIDFromRequest no current session for request " + request.getRequestURI());
        return -1;
      } else {
        return getUserIDFromSession(session);
      }
    }
  }

  public List<FirstLastUser> getActiveSince(long when) {
    Map<Integer, Long> activeSince = userSessionDAO.getActiveSince(when);
    List<FirstLastUser> since = new ArrayList<>();
    Map<Integer, FirstLastUser> firstLastFor = userDAO.getFirstLastFor(activeSince.keySet());
    activeSince.forEach((k, v) -> {
      FirstLastUser firstLastUser = firstLastFor.get(k);
      if (firstLastUser != null) {
        firstLastUser.setLastChecked(v);
        since.add(firstLastUser);
      }
    });

    since.sort((o1, o2) -> -1 * Long.compare(o1.getLastChecked(), o2.getLastChecked()));

    return since;
  }

  /**
   * Not sure when the attribute would be missing...
   *
   * @param session
   * @return
   * @see mitll.langtest.server.filter.ForceNocacheFilter#doFilter
   */
  private int getUserIDFromSession(HttpSession session) {
    Object attribute = session.getAttribute(USER_SESSION_ATT);
    if (attribute == null) {
      log.warn("getUserIDFromSession huh? no attribute " + USER_SESSION_ATT + " on session?");
      return -1;
    } else {
      return (Integer) attribute;
    }
  }

  /**
   * @param request
   * @return null if there is no user for the session
   * @see #lookupUserFromSessionOrDB(HttpServletRequest, boolean)
   */
  private User lookupUserFromDBSession(HttpServletRequest request) {
    long then = System.currentTimeMillis();
    String sid = request.getRequestedSessionId();
    int userForSession = sid == null ? -1 : userSessionDAO.getUserForSession(sid);
    log.info("lookupUserFromDBSession Lookup user from DB session. SID: {} = {}", sid, userForSession);
    if (userForSession == -1 && sid != null) {
      log.warn("lookupUserFromDBSession no user for session " + sid + " in database?");
      return null;
    } else if (userForSession == -1) {
      log.info("lookupUserFromDBSession no user and no session ");
      return null;
    } else {
      User byID = userDAO.getByID(userForSession);
      long now = System.currentTimeMillis();
      log.warn("lookupUserFromDBSession took " + (now - then) + " millis to get user ");
      return byID;
    }
  }

  /**
   * @param id
   * @return
   * @see #lookupUserFromHttpSession
   */
  private User getUserForID(int id) {
    long then = System.currentTimeMillis();
    User sessUser = userDAO.getByID(id);
    long now = System.currentTimeMillis();
    //if (now - then > 20)
    log.warn("getUserForID took " + (now - then) + " millis to get user " + id);
    return sessUser;
  }

  private void throwException(String opName, User cUser, String checkDesc)
      throws RestrictedOperationException {
    RestrictedOperationException ex = new RestrictedOperationException(opName, true);
    String uname = (cUser != null) ? cUser.toString() : "null";
    log.error("Access check failed! User={}, Checking={}, op={}", uname, checkDesc, opName);
    throw ex;
  }

  private void throwException(String opName, int cUserID, String checkDesc)
      throws RestrictedOperationException {
    RestrictedOperationException ex = new RestrictedOperationException(opName, true);
    log.error("Access check failed! User={}, Checking={}, op={}", cUserID, checkDesc, opName);
    throw ex;
  }
}
