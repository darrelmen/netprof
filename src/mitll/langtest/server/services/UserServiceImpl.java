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

import mitll.langtest.client.InitialUI;
import mitll.langtest.client.services.UserService;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.database.security.UserSecurityManager;
import mitll.langtest.server.database.user.IUserSessionDAO;
import mitll.langtest.server.database.user.UserManagement;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickUserSession;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

@SuppressWarnings("serial")
public class UserServiceImpl extends MyRemoteServiceServlet implements UserService {
  private static final Logger logger = Logger.getLogger(UserServiceImpl.class);
  /**
   * The key to get/set the id of the user stored in the session
   */
  private static final String USER_SESSION_ATT = UserSecurityManager.USER_SESSION_ATT;

  /**
   * The key to get/set the request attribute that holds the
   * user looked up by the security filter.
   */
//  private static final String USER_REQUEST_ATT = UserSecurityManager.USER_REQUEST_ATT;

  /**
   * TODO record additional session info in database.
   *
   * @param userId
   * @param attemptedPassword
   * @return
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(String, String)
   */
  public LoginResult loginUser(String userId, String attemptedPassword) {
    HttpServletRequest request = getThreadLocalRequest();
    String remoteAddr = request.getHeader("X-FORWARDED-FOR");
    if (remoteAddr == null || remoteAddr.isEmpty()) {
      remoteAddr = request.getRemoteAddr();
    }
    String userAgent = request.getHeader("User-Agent");

    // ensure a session is created.
    HttpSession session = createSession();
    logger.info("Login session " + session.getId() + " isNew=" + session.isNew()
        //    + " host is secondary " + properties.isSecondaryHost()
    );
    User loggedInUser = db.getUserDAO().getStrictUserWithPass(userId, attemptedPassword);//, remoteAddr, userAgent, session.getId());

    boolean success = loggedInUser != null;
    String resultStr = success ? " was successful" : " failed";
    logger.info(">Session Activity> User login for id " + userId + resultStr +
        ". IP: " + remoteAddr +
        ", UA: " + userAgent +
        (success ? ", user: " + loggedInUser.getId() : ""));
    if (success) {
      setSessionUser(session, loggedInUser);
      return new LoginResult(loggedInUser, new Date(System.currentTimeMillis()));
    } else {
      loggedInUser = db.getUserDAO().getUser(userId, attemptedPassword);//, remoteAddr, userAgent, session.getId());
      return getLoginResult(loggedInUser);
    }
  }

  private LoginResult getLoginResult(User loggedInUser) {
    if (loggedInUser == null) {
      return new LoginResult(LoginResult.ResultType.Failed);
    } else {
      return new LoginResult(loggedInUser, LoginResult.ResultType.BadPassword);
    }
  }

  /**
   * @param session
   * @param loggedInUser
   * @see #loginUser(String, String)
   * @see #addUser(SignUpUser, String, boolean)
   */
  private void setSessionUser(HttpSession session, User loggedInUser) {
    int id1 = loggedInUser.getId();
    session.setAttribute(USER_SESSION_ATT, id1);
    StringBuilder atts = new StringBuilder("Atts: [ ");
    Enumeration<String> attEnum = session.getAttributeNames();
    while (attEnum.hasMoreElements()) {
      atts.append(attEnum.nextElement() + ", ");
    }
    atts.append("]");
//      logger.info("acct detail {}", loggedInUser.getAcctDetail());
    HttpSession session1 = getCurrentSession();
    String id = session.getId();

    IUserSessionDAO userSessionDAO = db.getUserSessionDAO();
    // logger.info("num user sessions before " + userSessionDAO.getNumRows());

    userSessionDAO.add(new SlickUserSession(-1, id1, id, new Timestamp(System.currentTimeMillis())));

    // logger.info("num user sessions now " + userSessionDAO.getNumRows() + " : session = " + userSessionDAO.getByUser(id1));

    logger.info("Adding user to " + id +
        " lookup is " + session1.getAttribute(USER_SESSION_ATT) +
        ", session.isNew=" + session1.isNew() +
        ", created=" + session1.getCreationTime() +
        ", " + atts.toString());
    db.setStartupInfo(loggedInUser);
  }

  /**
   * true = create a new session
   *
   * @return
   */
  private HttpSession createSession() {
    return getThreadLocalRequest().getSession(true);
  }

  /**
   * false = don't create the session
   *
   * @return
   */
  private HttpSession getCurrentSession() {
    return getThreadLocalRequest().getSession(false);
  }

  /**
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.SignInForm#gotLogin
   * @see mitll.langtest.client.user.SignInForm#makeSignInUserName
   */
  public User userExists(String login, String passwordH) {
    // findSharedDatabase();
    if (passwordH.isEmpty()) {
      User user = db.getUserDAO().getUser(login, passwordH);
      if (user != null) {
        int i = db.getUserProjectDAO().mostRecentByUser(user.getId());
        ProjectStartupInfo startupInfo = new ProjectStartupInfo();
        user.setStartupInfo(startupInfo);
        startupInfo.setProjectid(i);
      }

      return user;
    } else {
      LoginResult loginResult = loginUser(login, passwordH);
      User loggedInUser = loginResult.getLoggedInUser();
//      if (loginResult.getResultType() == LoginResult.ResultType.Success) {
//        db.rememberUserSelectedProject(loggedInUser, projectid);
//      }
      return loggedInUser;
    }
  }

  /**
   * @param login
   * @see InitialUI#logout
   */
  public void logout(String login) {
    securityManager.logoutUser(getThreadLocalRequest(), login, true);
  }

  /**
   * Send confirmation to your email too.
   *
   * @param url
   * @param isCD
   * @return null if existing user
   * @see mitll.langtest.client.user.SignUpForm#gotSignUp
   */
  @Override
  public User addUser(
      SignUpUser user,
      String url//,
      //boolean isCD
  ) {
    //  findSharedDatabase();
    UserManagement userManagement = db.getUserManagement();
    User newUser = userManagement.addUser(getThreadLocalRequest(), user);
    MailSupport mailSupport = getMailSupport();

    String userID = user.getUserID();
    String email = user.getEmail();
    String first = user.getFirst();
    if (newUser != null && !newUser.isEnabled()) { // newUser = null means existing newUser.
      logger.debug("newUser " + userID + "/" + newUser + " wishes to be a content developer. Asking for approval.");
      getEmailHelper().addContentDeveloper(url, email, newUser, mailSupport, getProject().getLanguage());
      getEmailHelper().sendConfirmationEmail(email, userID, first, mailSupport);
    } else if (newUser == null) {
      logger.debug("no newUser found for id " + userID);
    } else {
      logger.debug("newUser " + userID + "/" + newUser + " is enabled.");
      getEmailHelper().sendConfirmationEmail(email, userID, first, mailSupport);
    }
    if (newUser != null) {
      setSessionUser(createSession(), newUser);
    }
    return newUser;
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, db.getUserDAO(), getMailSupport(), new PathHelper(getServletContext(), serverProps));
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  /**
   * @return
   * @see mitll.langtest.client.user.UserTable#showDialog
   */
  public List<User> getUsers() {
    //  findSharedDatabase();
    return db.getUsers();
  }

  /**
   * @param user
   * @param email
   * @param url
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.SignInForm#getForgotPassword
   */
  public boolean resetPassword(String user, String email, String url) {
    logger.debug("resetPassword for " + user);
    return getEmailHelper().resetPassword(user, email, url);
  }

  /**
   * @param token
   * @param emailR - email encoded by rot13
   * @return
   * @see mitll.langtest.client.InitialUI#handleCDToken
   */
  public String enableCDUser(String token, String emailR, String url) {
    logger.info("enabling token " + token + " for email " + emailR + " and url " + url);
    return getEmailHelper().enableCDUser(token, emailR, url, getProject().getLanguage());
  }

  /**
   * @param token
   * @return
   * @see mitll.langtest.client.LangTest#showLogin()
   */
  @Override
  public long getUserIDForToken(String token) {
    //  findSharedDatabase();
    User user = db.getUserDAO().getUserWithResetKey(token);
    long l = (user == null) ? -1 : user.getId();
    // logger.info("for token " + token + " got user id " + l);
    return l;
  }

  @Override
  public boolean changePFor(String token, String passwordH) {
    //  findSharedDatabase();
    User userWhereResetKey = db.getUserDAO().getUserWithResetKey(token);
    if (userWhereResetKey != null) {
      db.getUserDAO().clearKey(userWhereResetKey.getId(), true);

      if (db.getUserDAO().changePassword(userWhereResetKey.getId(), passwordH)) {
        return true;
      } else {
        logger.error("couldn't update user password for user " + userWhereResetKey);
        return false;
      }
    } else return false;
  }

  /**
   * TODO: consider stronger password like in domino.
   * @param userid
   * @param currentPasswordH
   * @param passwordH
   * @return
   */
  public boolean changePassword(int userid, String currentPasswordH, String passwordH) {
    User userWhereResetKey = db.getUserDAO().getByID(userid);
    if (userWhereResetKey == null) {
      return false;
    } else if (userWhereResetKey.getPasswordHash().equals(currentPasswordH)) {
      if (db.getUserDAO().changePassword(userid, passwordH)) {
        return true;
      } else {
        logger.error("couldn't update user password for user " + userid);
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public void changeEnabledFor(int userid, boolean enabled) {
    //findSharedDatabase();
    User userWhere = db.getUserDAO().getUserWhere(userid);
    if (userWhere == null) logger.error("couldn't find " + userid);
    else {
      db.getUserDAO().changeEnabled(userid, enabled);
    }
  }

  /**
   * @param emailH
   * @param email
   * @param url
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#getForgotUser()
   */
  @Override
  public boolean forgotUsername(String emailH, String email, String url) {
    //findSharedDatabase();
    String userChosenIDIfValid = db.getUserDAO().isValidEmail(emailH);
    getEmailHelper().getUserNameEmail(email, url, userChosenIDIfValid);
    return userChosenIDIfValid != null;
  }

  /**
   * @param projectid
   * @see mitll.langtest.client.InitialUI#setProjectForUser(int)
   */
  public User setProject(int projectid) {
    try {
      User sessionUser = getSessionUser();
      if (sessionUser != null) {
        logger.info("set project (" + projectid + ") for " + sessionUser);
        db.rememberProject(sessionUser.getId(), projectid);
      }
      db.setStartupInfo(sessionUser, projectid);
      return sessionUser;
    } catch (DominoSessionException e) {
      logger.error("got " + e, e);
      return null;
    }
  }

  @Override
  public void forgetProject() {
    try {
      User sessionUser = getSessionUser();
      if (sessionUser != null) {
        db.forgetProject(sessionUser.getId());
      }
    } catch (DominoSessionException e) {
      logger.error("got  " + e, e);
    }
  }
}
