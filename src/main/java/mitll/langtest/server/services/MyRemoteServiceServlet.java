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

package mitll.langtest.server.services;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.property.ServerInitializationManagerNetProf;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.User;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.UnknownHostException;

@SuppressWarnings("serial")
public class MyRemoteServiceServlet extends RemoteServiceServlet implements LogAndNotify {
  private static final Logger logger = LogManager.getLogger(MyRemoteServiceServlet.class);

  /**
   *
   */
  public static final String DATABASE_REFERENCE = "databaseReference";

  /**
   *
   */
  protected DatabaseServices db;
  protected ServerProperties serverProps;
  protected IUserSecurityManager securityManager;
  protected PathHelper pathHelper;

  /**
   * JUST FOR AMAS and interop with old h2 database...
   */
  @Deprecated
  protected AudioFileHelper audioFileHelper;

  @Override
  public void init() {
    findSharedDatabase();
    this.serverProps = readProperties(getServletContext());

    if (serverProps != null && serverProps.isAMAS()) {
      audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this, null);
    }
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   * <p>
   * NOTE : makes the database available to other servlets via the databaseReference servlet context attribute.
   * Note that this will only ever be called once.
   *
   * @param servletContext
   * @see #init
   */
  private ServerProperties readProperties(ServletContext servletContext) {
    return new ServerInitializationManagerNetProf().getServerProps(servletContext);
  }

  /**
   * @return
   */
  protected int getProjectID() {
    int userIDFromSession = getUserIDFromSession();
    return getProjectID(userIDFromSession);
  }

  protected int getProjectID(int userIDFromSession) {
    if (userIDFromSession == -1) {
      // it's not in the current session - can we recover it from the remember me cookie?
      logger.warn("getProjectID : no user in session, so we can't get the project id for the user.");
      return -1;
    }
    int i = db.getUserProjectDAO().mostRecentByUser(userIDFromSession);
    Project project = db.getProject(i);
    if (project != null) {
      db.configureProject(project, false); //check if we should configure it - might be a new project
    }
    return i;
  }

  protected Project getProject() {
    int userIDFromSession = getUserIDFromSession();
    if (userIDFromSession == -1) {
      // it's not in the current session - can we recover it from the remember me cookie?
      return null;
    } else {
      return getProjectForUser(userIDFromSession);
    }
  }

  private Project getProjectForUser(int userIDFromSession) {
    return db.getProjectForUser(userIDFromSession);
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
  int getUserIDFromSession() {
    int userIDFromSession = getUserIDFromSessionNoCheck();
    if (userIDFromSession == -1) {
      // it's not in the current session - can we recover it from the remember me cookie?
      try {
        User sessionUser = getSessionUser();
        int i = (sessionUser == null) ? -1 : sessionUser.getID();

        if (i == -1) { // OK, try the cookie???
          logger.error("getUserIDFromSession huh? couldn't get user from session or database?");
        }
        return i;
      } catch (DominoSessionException e) {
        logger.error("got " + e, e);
      }
      return -1;
    } else {
      return userIDFromSession;
    }
  }

  /**
   * @return
   * @see #getUserIDFromSession
   */
  private int getUserIDFromSessionNoCheck() {
    return securityManager.getUserIDFromRequest(getThreadLocalRequest());
  }

  public User getUserFromSession() {
    try {
      User loggedInUser = getSessionUser();
/*      if (loggedInUser != null) {
        // it's not in the current session - can we recover it from the remember me cookie?
        Cookie[] cookies = getThreadLocalRequest().getCookies();
        for (Cookie cookie : cookies) {
          if (cookie.getName().equals("r")) {
            logger.info("\n\n\n FOUND COOKIE " + cookie.getName());
            try {
              LoginResult byCookie = findByCookie(Long.parseLong(cookie.getValue()));
              if (byCookie.getResultType() == Success) {
                loggedInUser = byCookie.getLoggedInUser();
              } else {
                logger.warn("getUserFromSession couldn't find user by cookie " + cookie);
              }
            } catch (NumberFormatException e) {
              logger.error("getUserFromSession couldn't find cookie with " + cookie.getName() + " " + cookie.getValue());
            }
          }
        }
      }*/
      if (loggedInUser != null) {
        db.setStartupInfo(loggedInUser);
      }
//logger.info("Got here = "+loggedInUser);
      return loggedInUser;
    } catch (Exception e) {
      logger.error("Got " + e, e);
      return null;
    }
  }

  /**
   * Get the current user from the session
   *
   * @return
   * @throws DominoSessionException
   */
  User getSessionUser() throws DominoSessionException {
    return securityManager.getLoggedInUser(getThreadLocalRequest(), getThreadLocalResponse());
  }

  /**
   * This is safe!
   *
   * @return
   */
  protected String getLanguage() {
    Project project = getProject();
    if (project == null) {
      logger.error("getLanguage : no current project ");
      return "unset";
    } else {
      return project.getProject().language();
    }
  }

  /**
   * @param e
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  @Override
  public void logAndNotifyServerException(Exception e) {
    logAndNotifyServerException(e, "");
  }

  /**
   * @param e
   * @param additionalMessage
   */
  @Override
  public void logAndNotifyServerException(Throwable e, String additionalMessage) {
    String message1 = e == null ? "null_ex" : e.getMessage() == null ? "null_msg" : e.getMessage();
    if (!message1.contains("Broken Pipe")) {
      String prefix = additionalMessage.isEmpty() ? "" : additionalMessage + "\n";
      String installPath = pathHelper.getInstallPath();
      String prefixedMessage = prefix + "\nfor webapp at " + installPath +
          (e != null ? " got " + "Server Exception : " + ExceptionUtils.getStackTrace(e) : "");

      String subject = "Server Exception on " + getHostName() + " at " +installPath;
      sendEmail(subject, getInfo(prefixedMessage));

      logger.error(getInfo(prefixedMessage), e);
    } else {
      logger.error("\n\nlogAndNotifyServerException : got " + e, e);
    }
  }

  private String getHostName() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown host?";
    }
  }

  protected void sendEmail(String subject, String prefixedMessage) {
    getMailSupport().email(serverProps.getEmailAddress(), subject, prefixedMessage);
  }

  /**
   * @param session
   * @param loggedInUser
   * @return
   * @see UserServiceImpl#loginUser
   */
/*  @NotNull
  LoginResult getValidLogin(HttpSession session, User loggedInUser) {
    LoginResult loginResult = new LoginResult(loggedInUser, new Date(System.currentTimeMillis()));
    if (!loggedInUser.isValid()) {
      logger.info("user " + loggedInUser + "\n\tis missing email ");
      loginResult = new LoginResult(loggedInUser, LoginResult.ResultType.MissingInfo);
    } else {
      setSessionUser(session, loggedInUser);
    }
    return loginResult;
  }*/

/*  LoginResult getInvalidLoginResult(User loggedInUser) {
    if (loggedInUser == null) {
      return new LoginResult(LoginResult.ResultType.Failed);
    } else {
      return new LoginResult(loggedInUser, LoginResult.ResultType.BadPassword);
    }
  }*/

/*  public LoginResult findByCookie(long l) {
    logger.info("l " + l);

    ByteBuffer buffer = ByteBuffer.allocate(8).putLong(l);
    int x = buffer.getInt(0);
    int y = buffer.getInt(1);

    logger.info("x " + x);
    logger.info("y " + y);

    String sha256hex1 = org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + x);
    String sha256hex2 = org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + y);

    logger.info("first  : " + sha256hex1);
    logger.info("second : " + sha256hex2);

    int userForSV = db.getUserSessionDAO().getUserForSV(sha256hex1, sha256hex2);

    if (userForSV != -1) {
      User byID = db.getUserDAO().getByID(userForSV);

      if (byID != null) {
        return getValidLogin(createSession(), byID);
      } else {
        return getInvalidLoginResult(byID);
      }
    } else {
      return new LoginResult(null, SessionNotRestored);
    }
  }*/

  /**
   * @param session
   * @param loggedInUser
   * @seex #addUser
   * @see NPUserSecurityManager#lookupUser
   */
/*  public long setSessionUser(HttpSession session, User loggedInUser) {
    securityManager.setSessionUser(session,loggedInUser);
//    logger.debug("setSessionUser - made session - " + session + " user - " + loggedInUser);

    try {
      int id1 = loggedInUser.getID();
      session.setAttribute(USER_SESSION_ATT, id1);

      // HttpSession session1 = getCurrentSession();
      String sessionID = session.getId();

      *//*int selector = random.nextInt();
      int validator = random.nextInt();

      String sha256hex1 = org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + selector);
      String sha256hex2 = org.apache.commons.codec.digest.DigestUtils.sha256Hex("" + validator);

      logger.info("first  : " + sha256hex1);
      logger.info("second : " + sha256hex2);

//    long l = selector;
//    l = (l << 32) | validator;
//
//    long ll = (((long)selector) << 32) | (validator & 0xffffffffL);
//    int x = (int)(l >> 32);
//    int y = (int)l;

      long l = ByteBuffer.allocate(8).putInt(selector).putInt(validator).getLong(0);
      logger.info("l " + l);

      ByteBuffer buffer = ByteBuffer.allocate(8).putLong(l);
      int x = buffer.getInt(0);
      int y = buffer.getInt(1);*//*

      db.getUserSessionDAO().add(
          new SlickUserSession(-1,
              id1,
              sessionID,
              "",
              "",
              new Timestamp(System.currentTimeMillis())));

      //session.setAttribute("r", "" + l);
      // HttpServletResponse threadLocalResponse = getThreadLocalResponse();
      // logger.info("now - response " + threadLocalResponse);

      //  addCookie(threadLocalResponse, "r", "" + l);

      // logger.info("num user sessions now " + userSessionDAO.getNumRows() + " : session = " + userSessionDAO.getByUser(id1));

      logSetSession(session, sessionID);

      db.setStartupInfo(loggedInUser);

      return 1;//l;
    } catch (Exception e) {
      logger.error("got " + e, e);
      return -1;
    }
  }*/

  /**
   *
   */
/*
  public void addCookie(HttpServletResponse response, String name, String value) {
    Cookie cookie = new Cookie(name, value);
    cookie.setPath("/");
    cookie.setMaxAge(60 * 60 * 24 * 365);
    cookie.setSecure(true);
    response.addCookie(cookie);
  }
*/
/*
  private void logSetSession(HttpSession session1, String sessionID) {
    logger.info("setSessionUser : Adding user to " + sessionID +
        " lookup is " + session1.getAttribute(USER_SESSION_ATT) +
        ", session.isNew=" + session1.isNew() +
        ", created=" + session1.getCreationTime() +
        ", " + getAttributesFromSession(session1));
  }
*/

/*
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
*/

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  protected String getInfo(String message) {
    HttpServletRequest request = getThreadLocalRequest();
    if (request != null) {
      String remoteAddr = request.getHeader("X-FORWARDED-FOR");
      if (remoteAddr == null || remoteAddr.isEmpty()) {
        remoteAddr = request.getRemoteAddr();
      }
      String userAgent = request.getHeader("User-Agent");

      String strongName = getPermutationStrongName();
      String serverName = getThreadLocalRequest().getServerName();

      return message +
          "\nremoteAddr : " + remoteAddr +
          "\nuser agent : " + userAgent +
          "\ngwt        : " + strongName +
          "\nserver     : " + serverName;
    } else {
      return message;
    }
  }

  @Override protected void doUnexpectedFailure(Throwable ex) {
    logger.info("Look at exception {}", ex.getClass().getCanonicalName());
    if (ex.getClass().getCanonicalName().equals("org.apache.catalina.connector.ClientAbortException")) {
      logger.info("User reload during request.", ex);
    } else {
    //  logger.error("Got service Exception!", ex);
      logAndNotifyServerException(ex,"Got service exception in " +this.getClass().getCanonicalName()+ "!");

      // This may not be necessary in production, but some exceptions
      // traces did not include full cause details when running in dev mode.
      if (ex.getCause() != null) {
        logger.warn("Tracing exception cause!", ex.getCause());
      }
    }
    super.doUnexpectedFailure(ex);
  }


  protected ISection<CommonExercise> getSectionHelper() {
    return db.getSectionHelper(getProjectID());
  }

  protected AudioFileHelper getAudioFileHelper() {
    if (serverProps.isAMAS()) {
      return audioFileHelper;
    } else {
      Project project = getProject();
      if (project == null) {
        logger.warn("getAudioFileHelper no current project???");
        return null;
      }
      return project.getAudioFileHelper();
    }
  }

  protected IUserListManager getUserListManager() {
    return db.getUserListManager();
  }

  /**
   * Find shared db and make the user security manager.
   */
  private void findSharedDatabase() {
    if (db == null) {
      db = getDatabase();
      if (db == null) {
        logger.error("findSharedDatabase no database?");
      } else {
        securityManager = db.getUserSecurityManager();
      }
    }
  }

  /**
   * Find the shared db reference.
   * @return
   */
  private DatabaseImpl getDatabase() {
    DatabaseImpl db = null;

    Object databaseReference = getServletContext().getAttribute(DATABASE_REFERENCE);
    if (databaseReference != null) {
      db = (DatabaseImpl) databaseReference;
      this.pathHelper = new PathHelper(getServletContext(), db.getServerProps());
      //logger.debug("getDatabase : found existing database reference " + db + " under " +getServletContext());
    } else {
      logger.warn("getDatabase : no existing db reference yet - config error?");
    }
    return db;
  }

  /**
   * All the other servlets that come after this one can now use this db (DatabaseServices) reference.
   * This depends on the load order of the servlets being defined with this one going first. See
   * load-on-startup parameter in web.xml.
   *
   * @see #DATABASE_REFERENCE
   * @param servletContext db is shared via the servlet context
   * @param db             to share with other servlets that load after this one
   * @see #readProperties
   */
  protected void shareDB(ServletContext servletContext, DatabaseServices db) {
    Object databaseReference = servletContext.getAttribute(DATABASE_REFERENCE);
    if (databaseReference != null) {
      logger.debug("shareDB : hmm... found existing database reference " + databaseReference);
    }
    servletContext.setAttribute(DATABASE_REFERENCE, db);
//    logger.info("shareDB shared db " + servletContext.getAttribute(DATABASE_REFERENCE));
  }
}
