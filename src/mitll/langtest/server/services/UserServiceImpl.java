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
import mitll.langtest.client.services.UserService;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.List;

@SuppressWarnings("serial")
public class UserServiceImpl extends RemoteServiceServlet implements UserService {
  private static final Logger logger = Logger.getLogger(UserServiceImpl.class);
  private DatabaseImpl<CommonExercise> db;
  private ServerProperties serverProps;

  private DatabaseImpl<CommonExercise> getDatabase() {
    DatabaseImpl<CommonExercise> db = null;

    Object databaseReference = getServletContext().getAttribute(LangTestDatabaseImpl.DATABASE_REFERENCE);
    if (databaseReference != null) {
      db = (DatabaseImpl<CommonExercise>) databaseReference;
      // logger.debug("found existing database reference " + db + " under " +getServletContext());
    } else {
      logger.info("getDatabase : no existing db reference yet...");
    }
    return db;
  }

  @Override
  public void init() {
    logger.info("init called for UserServiceImpl");
    findSharedDatabase();
    readProperties(getServletContext());
  }

  void findSharedDatabase() {
    if (db == null) db = getDatabase();
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   * <p>
   * NOTE : makes the database available to other servlets via the databaseReference servlet context attribute.
   * Note that this will only ever be called once.
   *
   * @param servletContext
   * @see #init()
   */
  private void readProperties(ServletContext servletContext) {
    String relativeConfigDir = "config" + File.separator + servletContext.getInitParameter("config");
    PathHelper pathHelper = new PathHelper(getServletContext());
    String configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;
    serverProps = new ServerProperties(servletContext, configDir);
  }

  /**
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin
   * @see mitll.langtest.client.user.UserPassLogin#makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   */
  public User userExists(String login, String passwordH) {
    findSharedDatabase();
    return db.userExists(getThreadLocalRequest(), login, passwordH);
  }

  /**
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
   * @return null if existing user
   * @see mitll.langtest.client.user.UserPassLogin#gotSignUp(String, String, String, mitll.langtest.shared.User.Kind)
   */
  @Override
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String url, String email,
                      boolean isMale, int age, String dialect, boolean isCD, String device) {
    findSharedDatabase();
    User user = db.addUser(getThreadLocalRequest(), userID, passwordH, emailH, kind, isMale, age, dialect, "browser");
    if (user != null && !user.isEnabled()) { // user = null means existing user.
      logger.debug("user " + userID + "/" + user +
          " wishes to be a content developer. Asking for approval.");
      getEmailHelper().addContentDeveloper(url, email, user, getMailSupport());
    } else if (user == null) {
      logger.debug("no user found for id " + userID);
    } else {
      logger.debug("user " + userID + "/" + user + " is enabled.");
    }
    return user;
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, db.getUserDAO(), getMailSupport(), new PathHelper(getServletContext()));
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  /**
   * @return
   * @see mitll.langtest.client.user.UserTable#showDialog(mitll.langtest.client.LangTestDatabaseAsync)
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
  @Override
  public User getUserBy(int id) {
    findSharedDatabase();
    return db.getUserDAO().getUserWhere(id);
  }

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
}
