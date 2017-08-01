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

import mitll.hlt.domino.server.util.ServletUtil;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.services.UserService;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static mitll.langtest.shared.user.LoginResult.ResultType.Failed;
import static mitll.langtest.shared.user.LoginResult.ResultType.SessionNotRestored;

@SuppressWarnings("serial")
public class UserServiceImpl extends MyRemoteServiceServlet implements UserService {
  private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

  /**
   * TODO record additional session info in database.
   *
   * @param userId
   * @param attemptedFreeTextPassword
   * @return
   * @seex #userExists
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser
   */
  public LoginResult loginUser(String userId, String attemptedFreeTextPassword) {
    try {
      HttpServletRequest request = getThreadLocalRequest();
      String remoteAddr = getRemoteAddr(request);
      String userAgent = request.getHeader("User-Agent");

      // ensure a session is created.
      HttpSession session = createSession();
      logger.info("Login session " + session.getId() + " isNew=" + session.isNew());
    /*
    UsernamePasswordToken token = new UsernamePasswordToken(userId, attemptedHashedPassword);
    token.setRememberMe(true);
    Subject subject = SecurityUtils.getSubject();
    if (subject != null) {
      subject.login(token);
    }

    logger.info("sub " + subject);*/

      return securityManager.getLoginResult(userId, attemptedFreeTextPassword, remoteAddr, userAgent, session);
    } catch (Exception e) {
      logger.error("got " + e, e);
      logAndNotifyServerException(e);
      return new LoginResult(Failed);
    }
  }

  private String getRemoteAddr(HttpServletRequest request) {
    String remoteAddr = request.getHeader("X-FORWARDED-FOR");
    if (remoteAddr == null || remoteAddr.isEmpty()) {
      remoteAddr = request.getRemoteAddr();
    }
    return remoteAddr;
  }

  /*public LoginResult getLoginResult(String userId,
                                     String attemptedFreeTextPassword,
                                     String remoteAddr,
                                     String userAgent,
                                     HttpSession session) {
    IUserDAO userDAO = db.getUserDAO();
    User loggedInUser = userDAO.loginUser(
        userId,
        attemptedFreeTextPassword,
        userAgent,
        remoteAddr,
        session.getId());

    boolean success = loggedInUser != null;

    logActivity(userId, remoteAddr, userAgent, loggedInUser, success);

    if (success) {
      return getValidLogin(session, loggedInUser);
    } else {
      return getInvalidLoginResult(userDAO.getUserByID(userId));
    }
  }

  private void logActivity(String userId, String remoteAddr, String userAgent, User loggedInUser, boolean success) {
    String resultStr = success ? " was successful" : " failed";
    logger.info(">Session Activity> User login for id " + userId + resultStr +
        ". IP: " + remoteAddr +
        ", UA: " + userAgent +
        (success ? ", user: " + loggedInUser.getID() : ""));
  }*/

  @Override
  public LoginResult restoreUserSession() {
    try {
      User dbUser = getSessionUser();
      boolean isSessionActive = dbUser != null;
      String uid = (dbUser != null) ? dbUser.getUserID() : null;
      logger.info(">Session Activity> User session restoration for id " +
          uid + ((isSessionActive) ? " was successful" : " failed"));
      // ensure a session is created.
      if (!isSessionActive) {
        securityManager.logoutUser(getThreadLocalRequest(), uid, true);
        logger.info(">Session Activity> Sending Session Not Restored. Return early");
        return new LoginResult(SessionNotRestored);
      }
      logger.info(">Session Activity> Session restoration successful. Checking login status.");
      return new LoginResult(LoginResult.ResultType.Success);
    } catch (DominoSessionException e) {
      logger.info("got " + e, e);
      return new LoginResult(LoginResult.ResultType.SessionExpired);
    }
  }

  public User getUserByID(String id) {
    return db.getUserDAO().getUserByID(id);
  }

  /**
   * @param login
   * @see InitialUI#logout
   */
  public void logout(String login) {
    securityManager.logoutUser(getThreadLocalRequest(), login, true);
    //removeCookie(getThreadLocalResponse(), "r");
  }

  /**
   * Updates existing user if they are missing info.
   * <p>
   * If user exists already with complete info, then someone is already using this userid
   * <p>
   * I don't think we want to mess with the session until they've logged in with a password!
   *
   * @param url
   * @return null if existing user
   * @see mitll.langtest.client.user.SignUpForm#gotSignUp
   */
  @Override
  public LoginResult addUser(SignUpUser user, String url) {
    User userByID = getUserByID(user.getUserID());

    if (userByID != null) {
      if (userByID.isValid()) {
        return new LoginResult(userByID, LoginResult.ResultType.Exists);
      } else {
        userByID.setEmail(user.getEmail());
        userByID.setFirst(user.getFirst());
        userByID.setLast(user.getLast());
        userByID.setMale(user.isMale());
        userByID.setRealGender(user.isMale() ? MiniUser.Gender.Male : MiniUser.Gender.Female);

        db.getUserDAO().update(userByID);
        return new LoginResult(userByID, LoginResult.ResultType.Updated);
      }
    } else {
      User newUser = db.getUserManagement().addUser(getThreadLocalRequest(), user);

      if (newUser == null) {
        logger.error("addUser somehow couldn't add " + user.getUserID());
        return new LoginResult(null, LoginResult.ResultType.Failed);
      } else {
        return new LoginResult(newUser, LoginResult.ResultType.Added);
      }
    }
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, db.getUserDAO(), getMailSupport(), new PathHelper(getServletContext(), serverProps));
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  /**
   * @return
   * @see UserTable#showUsers(UserServiceAsync)
   */
  public List<User> getUsers() {
    return db.getUserManagement().getUsers();
  }

  /**
   * @return
   * @see mitll.langtest.client.dliclass.DLIClassOps#showUsers
   */
/*
  @Override
  public Map<User.Kind, Collection<MiniUser>> getKindToUser() {
    return db.getUserDAO().getMiniByKind();
  }
*/

  /**
   * @param user
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.SignInForm#getForgotPassword
   * @see mitll.langtest.client.user.SendResetPassword#onChangePassword
   */
  public boolean resetPassword(String user) {
    return db.getUserDAO().forgotPassword(user, getBaseURL());
  }

  /**
   * @param userId
   * @param userKey
   * @param newPassword
   * @return
   * @see mitll.langtest.client.user.ResetPassword#onChangePassword
   */
  @Override
  public User changePasswordWithToken(String userId, String userKey, String newPassword) {
    //long startMS = System.currentTimeMillis();
    logger.info("changePasswordWithToken - userId " + userId + " key " + userKey + " pass length " + newPassword.length());
    boolean result = db.getUserDAO().changePasswordForToken(userId, userKey, newPassword, getBaseURL());

    if (result) {
      User userByID = getUserByID(userId);
      if (userByID != null) {
        HttpSession currentSession = getCurrentSession();
        if (currentSession == null) currentSession = createSession();
        securityManager.setSessionUser(currentSession, userByID);
      }
      return userByID;
    } else {
      //  log.info(TIMING, "[changePassword, {} ms, for {}", () -> elapsedMS(startMS), () -> result);
      return null;
    }
  }

  /**
   * true = create a new session
   *
   * @return
   * @see UserServiceImpl#changePasswordWithToken(String, String, String)
   * @see UserServiceImpl#loginUser
   */
  protected HttpSession createSession() {
    return getThreadLocalRequest().getSession(true);
  }

  /**
   * false = don't create the session
   *
   * @return
   */
  protected HttpSession getCurrentSession() {
    return getThreadLocalRequest().getSession(false);
  }

  private String getBaseURL() {
    return ServletUtil.get().getBaseURL(getThreadLocalRequest());
  }

  /**
   * TODOx: consider stronger passwords like in domino.
   *
   * @param currentHashedPassword
   * @param newHashedPassword
   * @return
   * @see ChangePasswordView#changePassword
   */
  public boolean changePasswordWithCurrent(String currentHashedPassword, String newHashedPassword) {
    int userIDFromSession = getUserIDFromSession();
    User userWhereResetKey = db.getUserDAO().getByID(userIDFromSession);
    return
        userWhereResetKey != null &&
        (db.getUserDAO().changePasswordWithCurrent(userIDFromSession, currentHashedPassword, newHashedPassword, getBaseURL()));

  }

  /**
   * @param emailH
   * @param email
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#getForgotUser()
   */
  @Override
  public boolean forgotUsername(String emailH, String email) {
    String userChosenIDIfValid = db.getUserDAO().isValidEmail(email);
    getEmailHelper().getUserNameEmail(email, getBaseURL(), userChosenIDIfValid);
    return userChosenIDIfValid != null;
  }

  /**
   * @param projectid
   * @see mitll.langtest.client.project.ProjectChoices#reallySetTheProject
   */
  public User setProject(int projectid) {
    try {
      User sessionUser = getSessionUser();
      if (sessionUser != null) { // when could this be null?
        logger.info("setProject set project (" + projectid + ") for " + sessionUser);
        db.getProjectManagement().configureProjectByID(projectid);
        db.rememberProject(sessionUser.getID(), projectid);
        db.setStartupInfo(sessionUser, projectid);
      }
      return sessionUser;
    } catch (Exception e) {
      logger.error("got " + e, e);
      return null;
    }
  }

  @Override
  public void forgetProject() {
    try {
      User sessionUser = getSessionUser();
      if (sessionUser != null) {
        db.forgetProject(sessionUser.getID());
      }
    } catch (DominoSessionException e) {
      logger.error("got  " + e, e);
    }
  }

  @Override
  public User getUser(int id) {
    return db.getUserDAO().getByID(id);
  }
}
