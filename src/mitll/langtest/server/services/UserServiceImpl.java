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
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.services.UserService;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.security.DominoSessionException;
import mitll.langtest.server.database.user.IUserSessionDAO;
import mitll.langtest.server.database.user.UserManagement;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.user.*;
import mitll.npdata.dao.SlickUserSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.*;

import static mitll.langtest.server.database.security.IUserSecurityManager.USER_SESSION_ATT;

@SuppressWarnings("serial")
public class UserServiceImpl extends MyRemoteServiceServlet implements UserService {
  private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);
  /**
   * The key to get/set the id of the user stored in the session
   */
  //private static final String USER_SESSION_ATT = IUserSecurityManager.USER_SESSION_ATT;

  /**
   * The key to get/set the request attribute that holds the
   * user looked up by the security filter.
   */
//  private static final String USER_REQUEST_ATT = UserSecurityManager.USER_REQUEST_ATT;

/*  private String rot13(String val) {
    StringBuilder builder = new StringBuilder();
    for (char c : val.toCharArray()) {
      if (c >= 'a' && c <= 'm') c += 13;
      else if (c >= 'A' && c <= 'M') c += 13;
      else if (c >= 'n' && c <= 'z') c -= 13;
      else if (c >= 'N' && c <= 'Z') c -= 13;
      builder.append(c);
    }
    return builder.toString();
  }*/

  /**
   * TODO record additional session info in database.
   *
   * @param userId
   * @param attemptedHashedPassword
   * @param attemptedFreeTextPassword
   * @return
   * @seex #userExists
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser
   */
  public LoginResult loginUser(String userId, String attemptedHashedPassword, String attemptedFreeTextPassword) {
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

//    attemptedFreeTextPassword = rot13(attemptedFreeTextPassword);
    logger.info("userid " + userId + " password '" + attemptedHashedPassword + "'");
//    User loggedInUser = db.getUserDAO().getStrictUserWithPass(userId, attemptedFreeTextPassword);
    User loggedInUser = db.getUserDAO().loginUser(
        userId,
        attemptedFreeTextPassword,
        userAgent,
        remoteAddr,
        session.getId());

    boolean success = loggedInUser != null;
    String resultStr = success ? " was successful" : " failed";
    logger.info(">Session Activity> User login for id " + userId + resultStr +
        ". IP: " + remoteAddr +
        ", UA: " + userAgent +
        (success ? ", user: " + loggedInUser.getID() : ""));

    if (success) {
      LoginResult loginResult = new LoginResult(loggedInUser, new Date(System.currentTimeMillis()));
      if (!loggedInUser.isValid()) {
        logger.info("user " + loggedInUser + " is missing email ");
        loginResult = new LoginResult(loggedInUser, LoginResult.ResultType.MissingInfo);
      } else {
        setSessionUser(session, loggedInUser);
      }
      return loginResult;
    } else {
      loggedInUser = db.getUserDAO().getUserByID(userId);
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
   * @see #loginUser
   * @see #addUser
   */
  private void setSessionUser(HttpSession session, User loggedInUser) {
    int id1 = loggedInUser.getID();
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

  public User getUserByID(String id) {
    return db.getUserDAO().getUserByID(id);
  }

  /**
   * @param login
   * @see InitialUI#logout
   */
  public void logout(String login) {
    securityManager.logoutUser(getThreadLocalRequest(), login, true);
  }

  /**
   * Updates existing user if they are missing info.
   *
   * If user exists already with complete info, then someone is already using this userid
   *
   * I don't think we want to mess with the session until they've logged in with a password!
   *
   * @param url
   * @return null if existing user
   * @see mitll.langtest.client.user.SignUpForm#gotSignUp
   */
  @Override
  public LoginResult addUser(SignUpUser user, String url) {
    UserManagement userManagement = db.getUserManagement();
    User userByID = db.getUserDAO().getUserByID(user.getUserID());

    if (userByID != null) {
      if (userByID.isValid()) {
        return new LoginResult(userByID, LoginResult.ResultType.Exists);
      }
      else {
        userByID.setEmail(user.getEmail());
        userByID.setFirst(user.getFirst());
        userByID.setLast(user.getLast());
        db.getUserDAO().update(userByID);
        return new LoginResult(userByID, LoginResult.ResultType.Updated);
      }
//      setSessionUser(createSession(), userByID);

    } else {
      User newUser = userManagement.addUser(getThreadLocalRequest(), user);

      if (newUser == null) {
        logger.error("addUser somehow couldn't add " + user.getUserID());
        return new LoginResult(null, LoginResult.ResultType.Failed);
      } else {
  //      setSessionUser(createSession(), newUser);
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
   * @see mitll.langtest.client.user.UserTable#showDialog
   */
  public List<User> getUsers() {
    return db.getUsers();
  }

  public Map<User.Kind, Integer> getCounts() {
    return db.getUserDAO().getCounts();
  }

  public Map<String, Integer> getInvitationCounts(User.Kind requestRole) {
    return db.getInviteDAO().getInvitationCounts(requestRole);
  }

  @Override
  public Map<User.Kind, Collection<MiniUser>> getKindToUser() {
    return db.getUserDAO().getMiniByKind();
  }

  /**
   * @param user
   * @paramx url            IGNORED - remove me!
   * @paramx emailForLegacy
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.SignInForm#getForgotPassword
   */
  public boolean resetPassword(String user
  //    , String url, String emailForLegacy
  ) {
    String baseURL = getBaseURL();
    logger.debug("resetPassword for " + user + " " + baseURL);

    // Use Domino call to do reset password
    return db.getUserDAO().forgotPassword(user, baseURL
        //, emailForLegacy
    );
    //   return getEmailHelper().resetPassword(user, userEmail, url);
  }

  /**
   * @param token
   * @param emailR - email encoded by rot13
   * @param url    - remove me???
   * @return
   * @see mitll.langtest.client.InitialUI#handleCDToken
   * @deprecated don't do this anymore - just in domino
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
    User user = db.getUserDAO().getUserWithResetKey(token);
    long l = (user == null) ? -1 : user.getID();
    // logger.info("for token " + token + " got user id " + l);
    return l;
  }

  /**
   * @param userid
   * @param newHashedPassword
   * @return
   * @see mitll.langtest.client.user.ResetPassword#getChangePasswordButton
   */
/*  @Override
  public boolean changePFor(String userid, String newHashedPassword) {
    // hashedPassword = rot13(hashedPassword);
    User userByID = db.getUserDAO().getUserByID(userid);
    boolean b = db.getUserDAO().changePassword(userByID.getID(), newHashedPassword);

    if (!b) {
      logger.error("changePFor : couldn't update user password for user " + userByID);
    }

    return b;

*//*    User userWhereResetKey = db.getUserDAO().getUserWithResetKey(token);
    if (userWhereResetKey != null) {
      db.getUserDAO().clearKey(userWhereResetKey.getID(), true);

      if (db.getUserDAO().changePassword(userWhereResetKey.getID(), passwordH)) {
        return true;
      } else {
        logger.error("couldn't update user password for user " + userWhereResetKey);
        return false;
      }
    } else {

      return false;
    }*//*
  }*/

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
      return db.getUserDAO().getUserByID(userId);
    } else {
      //  log.info(TIMING, "[changePassword, {} ms, for {}", () -> elapsedMS(startMS), () -> result);
      return null;
    }
  }

  private String getBaseURL() {
    return ServletUtil.get().getBaseURL(getThreadLocalRequest());
  }


  /**
   * TODOx: consider stronger passwords like in domino.
   *
   * @param userid
   * @param currentHashedPassword
   * @param newHashedPassword
   * @return
   * @see ChangePasswordView#changePassword
   */
  public boolean changePasswordWithCurrent(int userid, String currentHashedPassword, String newHashedPassword) {
//    currentHashedPassword = rot13(currentHashedPassword);
//    newHashedPassword = rot13(newHashedPassword);

    User userWhereResetKey = db.getUserDAO().getByID(userid);
    if (userWhereResetKey == null) {
      return false;
      // TODOx : fix this to call new domino call
    }

/*    else if (userWhereResetKey.getPasswordHash().equals(currentHashedPassword)) {
      if (db.getUserDAO().changePassword(userid, newHashedPassword)) {
        getEmailHelper().sendChangedPassword(userWhereResetKey);
        return true;
      } else {
        logger.error("couldn't update user password for user " + userid);
        return false;
      }
    } else {
      return false;
    }*/

    return (db.getUserDAO().changePasswordWithCurrent(userid, currentHashedPassword, newHashedPassword, getBaseURL()));

  }

  @Override
  public void changeEnabledFor(int userid, boolean enabled) {
    User userWhere = db.getUserDAO().getUserWhere(userid);
    if (userWhere == null) logger.error("couldn't find " + userid);
    else {
      db.getUserDAO().changeEnabled(userid, enabled);
    }
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
    getEmailHelper().getUserNameEmail(email,  getBaseURL(), userChosenIDIfValid);
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
        db.rememberProject(sessionUser.getID(), projectid);
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

  /**
   * @param toUpdate
   * @param changingUser
   */
  public void update(User toUpdate, int changingUser) {
    db.getUserDAO().update(toUpdate);

/*    Collection<User.Permission> included = toUpdate.getPermissions();

    IUserPermissionDAO userPermissionDAO = db.getUserPermissionDAO();
    int updatedUserID = toUpdate.getID();
    Collection<SlickUserPermission> currentGranted = userPermissionDAO.grantedForUser(updatedUserID);

    logger.info("current perms for " + updatedUserID + " " + currentGranted);
    // deny all current permissions not included in update set
    List<User.Permission> currentPerms = new ArrayList<>();
    for (SlickUserPermission perm : currentGranted) {
      User.Permission current = User.Permission.valueOf(perm.name());
      currentPerms.add(current);
      if (!included.contains(current)) {
        logger.info("\t deny " + perm +
            " for " + updatedUserID + " " + currentGranted);
        userPermissionDAO.deny(perm.id(), changingUser);
      }
    }
    // all perms in update - current = what we need to insert at granted
    included.removeAll(currentPerms);

    Timestamp now = new Timestamp(System.currentTimeMillis());
    for (User.Permission perm : included) {
      logger.info("\tgrant " + perm + " for " + updatedUserID);
      userPermissionDAO.insert(new SlickUserPermission(-1,
          updatedUserID,
          changingUser,
          perm.name(),
          now,
          User.PermissionStatus.GRANTED.name(),
          now,
          changingUser
      ));
    }*/
  }

  @Deprecated
  @Override
  public Collection<Invitation> getPending(User.Kind requestRole) {
/*    List<Invitation> visible = new ArrayList<>();
    Collection<SlickInvite> pending = db.getInviteDAO().getPending();
    for (SlickInvite invite : pending) {
      String kind = invite.kind();
      User.Kind kind1 = User.Kind.valueOf(kind);
      if (kind1.compareTo(requestRole) < 0) {  // e.g. students below teachers
        visible.add(toInvitation(invite));
      }
    }
    return visible;*/
    return Collections.emptyList();
  }

/*
  private Invitation toInvitation(SlickInvite invite) {
    return new Invitation(User.Kind.valueOf(invite.kind()),
        invite.byuserid(),
        invite.modified().getTime(),
        invite.email()
    );
  }
*/

  /**
   * Invite you to NetProF as a student, or teacher, or program manager, etc.
   *
   * @param invite
   */
  @Override
  public void invite(String url,
                     Invitation invite) {
/*    int inviteID = db.getInviteDAO().add(new SlickInvite(-1,
        invite.getKind().toString(),
        invite.getByuser(),
        new Timestamp(System.currentTimeMillis()),

        "PENDING", db.getUserDAO().getBeforeLoginUser(),
        new Timestamp(0),

        invite.getEmail(),
        ""));

    String inviteKey = getEmailHelper().getHash(invite.getEmail() + "_" + inviteID);
    db.getInviteDAO().update(inviteID, inviteKey);
    User inviter = db.getUserDAO().getByID(invite.getByuser());

    // not checking if insert fails -- how could it?
    getEmailHelper().sendInviteEmail(url,
        invite.getEmail(),
        inviter,
        invite.getKind(), inviteKey, getMailSupport());*/

  }
}
