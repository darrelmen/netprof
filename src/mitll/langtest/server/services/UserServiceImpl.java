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

import mitll.langtest.client.flashcard.Banner;
import mitll.langtest.client.services.UserService;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.database.security.UserSecurityManager;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.SlimProject;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

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

  @Override
  public void init() {
    logger.info("init called for UserServiceImpl");
    findSharedDatabase();
    readProperties(getServletContext());
  }

  /**
   * TODO record additional session info in database.
   *
   * @param userId
   * @param attemptedPassword
   * @return
   */
  public LoginResult loginUser(String userId, String attemptedPassword) {
    HttpServletRequest request = getThreadLocalRequest();
    String remoteAddr = request.getHeader("X-FORWARDED-FOR");
    if (remoteAddr == null || remoteAddr.isEmpty()) {
      remoteAddr = request.getRemoteAddr();
    }
    String userAgent = request.getHeader("User-Agent");

    // ensure a session is created.
    HttpSession session = getThreadLocalRequest().getSession(true);
    logger.info("Login session " + session.getId() + " isNew=" + session.isNew()
        //    + " host is secondary " + properties.isSecondaryHost()
    );
    User loggedInUser = db.getUserDAO().getStrictUserWithPass(userId, attemptedPassword);//, remoteAddr, userAgent, session.getId());

    String resultStr = (loggedInUser != null) ? " was successful" : " failed";
    logger.info(">Session Activity> User login for id " + userId + resultStr +
        ". IP: " + remoteAddr + ", UA: " + userAgent);
    if (loggedInUser != null) {
      setSessionUser(session, loggedInUser);
      return new LoginResult(loggedInUser, new Date(System.currentTimeMillis()));
    } else {
      loggedInUser = db.getUserDAO().getUser(userId, attemptedPassword);//, remoteAddr, userAgent, session.getId());
      if (loggedInUser == null) {
        return new LoginResult(LoginResult.ResultType.Failed);
      } else {
        return new LoginResult(loggedInUser, LoginResult.ResultType.BadPassword);
      }
    }
  }

  void setSessionUser(HttpSession session, User loggedInUser) {
    setUserOnSession(session, loggedInUser);
    StringBuilder atts = new StringBuilder("Atts: [ ");
    Enumeration<String> attEnum = session.getAttributeNames();
    while (attEnum.hasMoreElements()) {
      atts.append(attEnum.nextElement() + ", ");
    }
    atts.append("]");
//      logger.info("acct detail {}", loggedInUser.getAcctDetail());
    HttpSession session1 = getThreadLocalRequest().getSession(false);
    logger.info("Adding user to " + session.getId() +
        " lookup is " + session1.getAttribute(USER_SESSION_ATT) +
        ", session.isNew=" + session1.isNew() +
        ", created=" + session1.getCreationTime() +
        ", " + atts.toString());
    db.setStartupInfo(loggedInUser);
  }

  private void setUserOnSession(HttpSession session, User loggedInUser) {
    session.setAttribute(USER_SESSION_ATT, loggedInUser.getId());
  }

  /*public User getLoggedInUser() {
    try {
      return securityManager.getLoggedInUser(getThreadLocalRequest());
    } catch (DominoSessionException e) {
      logger.error("got " + e, e);
    }
    return null;
  }
*/

  /**
   *
   * @param login
   * @param passwordH
   * @param projectid - chosen by user in login screen or at signup
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin
   * @see mitll.langtest.client.user.UserPassLogin#makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   */
  public User userExists(String login, String passwordH, int projectid) {
    findSharedDatabase();
    if (passwordH.isEmpty()) {
      return db.getUserDAO().getUser(login, passwordH);
    } else {
      // return db.getUserManagement().userExists(getThreadLocalRequest(), login, passwordH, serverProps);
      LoginResult loginResult = loginUser(login, passwordH);
      User loggedInUser = loginResult.getLoggedInUser();
      if (loginResult.getResultType() == LoginResult.ResultType.Success) {
        db.rememberUserSelectedProject(loggedInUser, projectid);
      }
      return loggedInUser;
    }
  }

  public void logout(String login) {
    securityManager.logoutUser(getThreadLocalRequest(), login, true);
  }

  /**
   * Send confirmation to your email too.
   *
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param url
   * @param email
   * @param isMale
   * @param age
   * @param dialect
   * @param isCD
   * @param device
   * @param projid
   * @return null if existing user
   * @see mitll.langtest.client.user.UserPassLogin#gotSignUp(String, String, String, User.Kind)
   */
  @Override
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String url, String email,
                      boolean isMale, int age, String dialect, boolean isCD, String device, int projid) {
    findSharedDatabase();
    User newUser = db.addUser(getThreadLocalRequest(), userID, passwordH, emailH, kind, isMale, age, dialect, "browser", projid);
    MailSupport mailSupport = getMailSupport();

    if (newUser != null && !newUser.isEnabled()) { // newUser = null means existing newUser.
      logger.debug("newUser " + userID + "/" + newUser + " wishes to be a content developer. Asking for approval.");
      getEmailHelper().addContentDeveloper(url, email, newUser, mailSupport);
      getEmailHelper().sendConfirmationEmail(email, userID, mailSupport);
    } else if (newUser == null) {
      logger.debug("no newUser found for id " + userID);
    } else {
      logger.debug("newUser " + userID + "/" + newUser + " is enabled.");
      getEmailHelper().sendConfirmationEmail(email, userID, mailSupport);
    }
    if (newUser != null) {
      setSessionUser(getThreadLocalRequest().getSession(true), newUser);
      db.rememberUserSelectedProject(newUser, projid);
    }
    return newUser;
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, db.getUserDAO(), getMailSupport(), new PathHelper(getServletContext()));
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  /**
   * @return
   * @see mitll.langtest.client.user.UserTable#showDialog
   */
  public List<User> getUsers() {
    findSharedDatabase();
    return db.getUsers();
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(int)
   */
/*
  @Override
  public User getUserBy(int id) {
    findSharedDatabase();
    User userWhere = db.getUserDAO().getUserWhere(id);
    db.setStartupInfo(userWhere);

    return userWhere;
  }
*/

  /**
   * @param user
   * @param email
   * @param url
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.UserPassLogin#getForgotPassword()
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
    return getEmailHelper().enableCDUser(token, emailR, url);
  }

  /**
   * @param token
   * @return
   * @see mitll.langtest.client.LangTest#showLogin()
   */
  @Override
  public long getUserIDForToken(String token) {
    findSharedDatabase();
    User user = db.getUserDAO().getUserWithResetKey(token);
    long l = (user == null) ? -1 : user.getId();
    // logger.info("for token " + token + " got user id " + l);
    return l;
  }

  @Override
  public boolean changePFor(String token, String passwordH) {
    findSharedDatabase();
    User userWhereResetKey = db.getUserDAO().getUserWithResetKey(token);
    if (userWhereResetKey != null) {
      db.getUserDAO().clearKey(userWhereResetKey.getId(), true);

      if (!db.getUserDAO().changePassword(userWhereResetKey.getId(), passwordH)) {
        logger.error("couldn't update user password for user " + userWhereResetKey);
      }
      return true;
    } else return false;
  }

  @Override
  public void changeEnabledFor(int userid, boolean enabled) {
    findSharedDatabase();
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
    findSharedDatabase();
    String userChosenIDIfValid = db.getUserDAO().isValidEmail(emailH);
    getEmailHelper().getUserNameEmail(email, url, userChosenIDIfValid);
    return userChosenIDIfValid != null;
  }

  public Collection<SlimProject> getProjects() {
    List<SlimProject> projects = new ArrayList<>();
    for (SlickProject project : db.getProjectDAO().getAll()) {
      projects.add(new SlimProject(project.name(), project.language(), project.id()));
    }
    return projects;
  }

  /**
   * @see Banner#populateListChoices
   * @param projectid
   */
  public User setProject(int projectid) {
    try {
      User sessionUser = getSessionUser();
      if (sessionUser != null) {
        db.rememberProject(sessionUser.getId(), projectid);
      }
      db.setStartupInfo(sessionUser,projectid);
      return sessionUser;
    } catch (DominoSessionException e) {
      logger.error("got " +e,e);
      return null;
    }
  }
}
