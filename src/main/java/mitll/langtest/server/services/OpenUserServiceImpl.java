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
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.user.*;
import mitll.langtest.shared.user.LoginResult.ResultType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import static mitll.langtest.shared.user.ChoosePasswordResult.PasswordResultType.*;
import static mitll.langtest.shared.user.ChoosePasswordResult.PasswordResultType.Success;
import static mitll.langtest.shared.user.LoginResult.ResultType.*;
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
   * @see mitll.langtest.client.user.SignInForm#gotLogin
   */
  public LoginResult loginUser(String userId, String attemptedFreeTextPassword) {
    try {
      HttpServletRequest request = getThreadLocalRequest();
      String remoteAddr = getRemoteAddr(request);
      String userAgent = request.getHeader(USER_AGENT);

/*      String localAddr = request.getLocalAddr();
      String localName = request.getLocalName();
      String serverName = request.getServerName();
      int localPort = request.getLocalPort();
      int serverPort = request.getServerPort();
      String contextPath = request.getContextPath();
      logger.info("loginUser : " +
          "\n\tlocalAddr   " + localAddr +
          "\n\tlocalName   " + localName +
          "\n\tserverName  " + serverName +
          "\n\tlocalPort   " + localPort +
          "\n\tserverPort  " + serverPort +
          "\n\tcontextPath " + contextPath
      );*/

      return securityManager.getLoginResult(userId, attemptedFreeTextPassword, remoteAddr, userAgent, createSession(), true);
    } catch (Exception e) {
      logger.error("got " + e, e);
      logAndNotifyServerException(e);
      return new LoginResult(Failed);
    }
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
  public LoginResult isKnownUser(String id, boolean emailOK) {
    boolean knownUser = db.getUserDAO().isKnownUser(id);
    if (!knownUser) {
      String normalized = normalizeSpaces(id);
      if (!normalized.equals(id)) {
        knownUser = db.getUserDAO().isKnownUser(normalized);
      }
    }
    if (!knownUser && emailOK && db.getUserDAO().isValidAsEmail(id)) {
      List<String> usersWithThisEmail = db.getUserDAO().getUsersWithThisEmail(id);
      // LoginResult resultType = getResultType(usersWithThisEmail);
      //   logger.info("isKnownUser : result type for " + id + " = " + usersWithThisEmail + " : " + resultType);
      return getResultType(usersWithThisEmail);
    } else {
      if (knownUser) {
        User userByID = getUserByID(id);
        if (userByID == null) {
          return new LoginResult(Failed);// HOW?
        } else {
          return new LoginResult(userByID, userByID.isValid() ? ExistsValid : Exists);
        }
      } else {
        return new LoginResult(Failed);
      }
    }
  }

  @NotNull
  private LoginResult getResultType(List<String> usersWithThisEmail) {
    int numMatches = usersWithThisEmail.size();
    if (numMatches == 0) {
      return new LoginResult(Failed);
    } else if (numMatches == 1) {
      return new LoginResult(Email, usersWithThisEmail.iterator().next());
    } else {
      String mostRecentUserID = db.getUserDAO().getMostRecentUserID(usersWithThisEmail);

      return new LoginResult(mostRecentUserID.isEmpty() ? Failed : Email, mostRecentUserID);
    }
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

  public boolean accountExistsWithEmail(String email) {
    return !db.getUserDAO().getUsersWithThisEmail(email).isEmpty();
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

    if (userByID == null) {
      User newUser = db.getUserManagement().addUser(getThreadLocalRequest(), user);

      if (newUser == null) {
        logger.error("addUser somehow couldn't add " + user.getUserID());
        return new LoginResult(null, Failed);
      } else {
        return new LoginResult(newUser, Added);
      }
    } else {
      ResultType resultType = Exists;

      if (!userByID.isValid()) {
        userByID.setEmail(user.getEmail());
        userByID.setFirst(user.getFirst());
        userByID.setLast(user.getLast());
        userByID.setMale(user.isMale());
        userByID.setRealGender(user.isMale() ? MiniUser.Gender.Male : MiniUser.Gender.Female);
        userByID.setAffiliation(user.getAffiliation());
        logger.info("addUser user " + userByID + " updating.");
        db.getUserDAO().update(userByID);
        resultType = Updated;
      }

      return new LoginResult(userByID, resultType);
    }
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, getMailSupport());
  }

  /**
   * @param user
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.SignInForm#getForgotPassword
   * @see mitll.langtest.client.user.SendResetPassword#onChangePassword
   */
  public boolean resetPassword(String user) {
    return db.getUserDAO().forgotPassword(user, getBaseURL(), "");
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
    User userByID = getUserByID(userId);
    boolean exists = userByID != null;
    String email = exists ? userByID.getEmail() : "";
    boolean success = db.getUserDAO().changePasswordForToken(userId, userKey, newPassword, getBaseURL(), email);

    if (success) {
      if (exists) {
        createNewSession(userByID);
      }
      return new ChoosePasswordResult(userByID, !exists ? NotExists : Success);
    } else {
      //  log.info(TIMING, "[changePassword, {} ms, for {}", () -> elapsedMS(startMS), () -> success);
      return new ChoosePasswordResult(null, !exists ? NotExists : AlreadySet);
    }
  }

  private void createNewSession(User userByID) {
    boolean newSession = false;
    HttpSession currentSession = getCurrentSession();
    if (currentSession == null) {
      currentSession = createSession();
      newSession = true;
    }
    securityManager.setSessionUser(currentSession, userByID, newSession);
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
   * @param email
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#getForgotUser
   */
  @Override
  public boolean forgotUsername(String email) {
    List<String> userChosenIDIfValid = db.getUserDAO().isValidEmail(email);
    boolean found = userChosenIDIfValid != null && !userChosenIDIfValid.isEmpty();
    if (found) {
      getEmailHelper().getUserNameEmail(email, getBaseURL(), userChosenIDIfValid);
    }
    return found;
  }

  /**
   * Assumes project id is valid.
   * Maybe someone else has deleted it while you were looking at it?
   *
   * @param projectid
   * @see mitll.langtest.client.project.ProjectChoices#setProjectForUser
   */
  public User setProject(int projectid) {
    try {
      User sessionUser = getSessionUser();

      if (sessionUser != null) { // when could this be null?
        int id = sessionUser.getID();

        if (db.getProjectDAO().exists(projectid)) {
          logger.info("setProject set project (" + projectid + ") for '" + sessionUser + "' = " + id);
          db.getProjectManagement().configureProjectByID(projectid);
          db.rememberUsersCurrentProject(id, projectid);
          db.getProjectManagement().setStartupInfo(sessionUser, projectid);
        } else {
          logger.warn("setProject : project " + projectid + " is gone....");
          db.getUserProjectDAO().forget(id);
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
        db.getUserProjectDAO().forget(sessionUserID);
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
   * <p>
   * So what can happen
   * - a user can choose a different project in a different tab, and this makes sure the current client one is the current one
   * - a user can have a browser open so long it's javascript is out of date and should reload.
   *
   * @param projid
   * @return false if no session
   * @see InitialUI#confirmCurrentProject
   */
  @Override
  public HeartbeatStatus setCurrentUserToProject(int projid, String implVersion) {
    try {
      int sessionUserID = getSessionUserID();
      if (sessionUserID == -1) { // dude - they have no session
        logger.info("setCurrentProjectForUser : no current session user " + sessionUserID + " for " + projid);
        return new HeartbeatStatus(false, false);
      } else {
        if (projid == -1) {
          logger.warn("huh? trying to set invalid current project for user #" + sessionUserID);
        } else {
          // logger.info("setCurrentProjectForUser : session user " + sessionUserID + " for " + projid);
          long then = System.currentTimeMillis();
          int before = db.getUserProjectDAO().setCurrentProjectForUser(sessionUserID, projid);
          long now = System.currentTimeMillis();
          if (now - then > 20 || projid != before) {
            logger.info("setCurrentProjectForUser : took " + (now - then) + " to set current session user " + sessionUserID +
                " and set project to " + projid + " from " + before);
          }

     /*   if (!b) {
          if (hasSession) {
            logger.info("setCurrentProjectForUser : no most recent project for " + sessionUserID + ", tried " + projid);
          } else {
            logger.info("setCurrentProjectForUser : no current session user " + sessionUserID + " for " + projid);
          }
        }
*/
        }

        updateVisitedLater();

        return new HeartbeatStatus(true, checkCodeHasUpdated(projid, implVersion, sessionUserID));
      }
    } catch (DominoSessionException e) {
      logger.error("setCurrentProjectForUser got  " + e, e);
      return new HeartbeatStatus(false, false);
    }
  }

  private void updateVisitedLater() {
    final String sid = getSessionID();
    // TODO : expensive?
    new Thread(() -> updateVisited(sid), "updateVisited").start();
  }

  private void updateVisited(String sid) {
    if (sid == null) {
      logger.error("updateVisited : no session?");
    } else {
      if (!db.getUserSessionDAO().updateVisitedForSession(sid)) {
        logger.warn("updateVisited didn't update session " + sid);
      }
    }
  }

  private boolean checkCodeHasUpdated(int projid, String implVersion, int sessionUserID) {
    boolean codeHasUpdated = !implVersion.equalsIgnoreCase(serverProps.getImplementationVersion());
    if (codeHasUpdated) {
      logger.info("\n\n\nsetCurrentProjectForUser : " +
          "sess user " + sessionUserID +
          " for " + projid + " client was " + implVersion + " but current is " + serverProps.getImplementationVersion());
    }
    return codeHasUpdated;
  }

  @Override
  public HeartbeatStatus checkHeartbeat(String implVersion) {
    try {
      int sessionUserID = getSessionUserID();
      if (sessionUserID == -1) { // dude - they have no session
        logger.info("setCurrentProjectForUser : no current session user " + sessionUserID);
        return new HeartbeatStatus(false, false);
      } else {
        boolean codeHasUpdated = !implVersion.equalsIgnoreCase(serverProps.getImplementationVersion());
        if (codeHasUpdated) {
          logger.info("setCurrentProjectForUser : sess user " + sessionUserID + " client was " + implVersion + " but current is " + serverProps.getImplementationVersion());
        }

        //      simulateNetworkIssue();
        return new HeartbeatStatus(true, codeHasUpdated);
      }
    } catch (DominoSessionException e) {
      logger.error("setCurrentProjectForUser got  " + e, e);
      return new HeartbeatStatus(false, false);
    }
  }

  @Override
  public boolean isValidServer(String server) {
    return db.getUserDAO().isValidServer(server);
  }

/*  private void simulateNetworkIssue() {
    try {
//      logger.info("checkHeartbeat sleep...");
      java.util.Random random = new java.util.Random();
      int millis = random.nextInt(BOUND);
      Thread.sleep(millis);
      logger.info("checkHeartbeat finished sleep... for " + millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }*/
}
