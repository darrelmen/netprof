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
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.services.OpenUserService;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.user.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static mitll.langtest.shared.user.ChoosePasswordResult.ResultType.*;
import static mitll.langtest.shared.user.LoginResult.ResultType.Failed;

@SuppressWarnings("serial")
public class OpenUserServiceImpl extends MyRemoteServiceServlet implements OpenUserService {
  private static final Logger logger = LogManager.getLogger(OpenUserServiceImpl.class);

  /**
   * If successful, establishes a session.
   * <p>
   * TODO record additional session info in database.
   *
   * @param userId
   * @param attemptedFreeTextPassword
   * @return
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

  private User getUserByID(String id) {
    return db.getUserDAO().getUserByID(id);
  }

  /**
   * This call is open - you do not need a session.
   * It's called from the sign in form.
   *
   * @param id
   * @return
   */
  @Override
  public boolean isKnownUser(String id) {
    boolean knownUser = db.getUserDAO().isKnownUser(id);
    if (!knownUser) {
      String normalized = normalizeSpaces(id);
      if (!normalized.equals(id)) {
        knownUser = db.getUserDAO().isKnownUser(normalized);
      }
    }
    return knownUser;
  }

  @Override
  public boolean isValidUser(String id) {
    User userByID = getUserDealWithSpaces(id);
    return userByID != null && userByID.isValid();
  }

  @Override
  public boolean isKnownUserWithEmail(String id) {
    User userByID = getUserDealWithSpaces(id);
    return userByID != null && userByID.hasValidEmail();
  }

  @Nullable
  private User getUserDealWithSpaces(String id) {
    User userByID = db.getUserDAO().getUserByID(id);
    if (userByID == null) {
      String normalized = normalizeSpaces(id);
      if (!normalized.equals(id)) {
        userByID = db.getUserDAO().getUserByID(normalized);
      }
    }
    return userByID;
  }

  private String normalizeSpaces(String trim) {
    return trim.replaceAll("\\s+", "_");
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
      LoginResult.ResultType resultType = LoginResult.ResultType.Exists;
      if (!userByID.isValid()) {
        //logger.info("addUser user " + userByID + " resultType.");
        //  } else {
        userByID.setEmail(user.getEmail());
        userByID.setFirst(user.getFirst());
        userByID.setLast(user.getLast());
        userByID.setMale(user.isMale());
        userByID.setRealGender(user.isMale() ? MiniUser.Gender.Male : MiniUser.Gender.Female);
        userByID.setAffiliation(user.getAffiliation());
        logger.info("addUser user " + userByID + " updating.");
        db.getUserDAO().update(userByID);
        resultType = LoginResult.ResultType.Updated;
      }
      return new LoginResult(userByID, resultType);
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
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail(), serverProps.getMailServer());
  }

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
   * Also creates a session.
   *
   * @param userId
   * @param userKey
   * @param newPassword
   * @return
   * @see mitll.langtest.client.user.ResetPassword#onChangePassword
   */
  @Override
  public ChoosePasswordResult changePasswordWithToken(String userId, String userKey, String newPassword) {
    //long startMS = System.currentTimeMillis();
    logger.info("changePasswordWithToken - userId '" + userId + "' key " + userKey + " pass length " + newPassword.length());
    boolean result = db.getUserDAO().changePasswordForToken(userId, userKey, newPassword, getBaseURL());

    User userByID = getUserByID(userId);
    if (result) {
      if (userByID != null) {
        HttpSession currentSession = getCurrentSession();
        if (currentSession == null) currentSession = createSession();
        securityManager.setSessionUser(currentSession, userByID);
      }
      return new ChoosePasswordResult(userByID, userByID == null ? NotExists : Success);
    } else {
      //  log.info(TIMING, "[changePassword, {} ms, for {}", () -> elapsedMS(startMS), () -> result);
      return new ChoosePasswordResult(null, userByID == null ? NotExists : AlreadySet);
    }
  }

  /**
   * true = create a new session
   *
   * @return
   * @see OpenUserServiceImpl#changePasswordWithToken(String, String, String)
   * @see OpenUserServiceImpl#loginUser
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

  private String getBaseURL() {
    return ServletUtil.get().getBaseURL(getThreadLocalRequest());
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
   * Assumes project id is valid.
   * Maybe someone else has deleted it while you were looking at it?
   *
   * @param projectid
   * @see mitll.langtest.client.project.ProjectChoices#reallySetTheProject
   */
  public User setProject(int projectid) {
    try {
      User sessionUser = getSessionUser();

      if (sessionUser != null) { // when could this be null?
        int id = sessionUser.getID();

        if (db.getProjectDAO().exists(projectid)) {
          logger.info("setProject set project (" + projectid + ") for " + sessionUser);
          db.getProjectManagement().configureProjectByID(projectid);
          db.rememberUsersCurrentProject(id, projectid);
          db.setStartupInfo(sessionUser, projectid);
        } else {
          logger.warn("setProject : project " + projectid + " is gone....");
          db.forgetProject(id);
          db.getProjectManagement().clearStartupInfo(sessionUser);
        }
      }
      return sessionUser;
    } catch (Exception e) {
      logger.error("got " + e, e);
      return null;
    }
  }

  /**
   * @see InitialUI#chooseProjectAgain
   */
  @Override
  public void forgetProject() {
    try {
      int sessionUserID = getSessionUserID();
      if (sessionUserID != -1) {
        db.forgetProject(sessionUserID);
      }
    } catch (DominoSessionException e) {
      logger.error("forgetProject got  " + e, e);
    }
  }

  /**
   * If the user has two or more tabs open, and switches languages between tabs, behind the scenes
   * we maintain the one-to-one user->project mapping by confirming the current project here and switching it
   * in the database if it's not consistent with the UI.
   * <p>
   * This should support Paul's language eval comparison. (1/22/18).
   *
   * @param projid
   * @return
   * @see InitialUI#confirmCurrentProject
   */
  @Override
  public boolean setCurrentUserToProject(int projid) {
    try {
      long then = System.currentTimeMillis();
      int sessionUserID = getSessionUserID();
      boolean b = sessionUserID != -1 && db.getUserProjectDAO().setCurrentUserToProject(sessionUserID, projid);

      long now = System.currentTimeMillis();
      if (now - then > 10) {
        logger.info("setCurrentUserToProject : took " + (now - then) + " to get current session user " + sessionUserID + " and set project to " + projid);
      }
      if (!b) {
        if (sessionUserID == -1) {
          logger.info("setCurrentUserToProject : no current session user " + sessionUserID + " for " + projid);
        } else {
          logger.info("setCurrentUserToProject : no most recent project for " + sessionUserID + ", tried " + projid);
        }
      }

      return b;
    } catch (DominoSessionException e) {
      logger.error("setCurrentUserToProject got  " + e, e);
      return false;
    }
  }
}
