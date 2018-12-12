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

import com.google.gwt.user.server.rpc.XsrfProtectedServiceServlet;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.property.ServerInitializationManagerNetProf;
import mitll.langtest.server.scoring.AlignmentHelper;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.user.User;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.exercise.Project.MANDARIN;

@SuppressWarnings("serial")
public class MyRemoteServiceServlet extends XsrfProtectedServiceServlet implements LogAndNotify {
  private static final Logger logger = LogManager.getLogger(MyRemoteServiceServlet.class);
  private static final String XSRF_COOKIE_NAME = "JSESSIONID";

  /**
   *
   */
  public static final String DATABASE_REFERENCE = "databaseReference";

  /**
   * @see mitll.langtest.server.database.user.UserManagement
   */
  public static final String USER_AGENT = "User-Agent";
  public static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";

  /**
   *
   */
  protected DatabaseServices db;
  protected ServerProperties serverProps;
  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#readProperties
   */
  protected IUserSecurityManager securityManager;
  protected PathHelper pathHelper;

  /**
   * JUST FOR AMAS and interop with old h2 database...
   */
  @Deprecated
  protected AudioFileHelper audioFileHelper;

  private static final boolean DEBUG = false;

  public MyRemoteServiceServlet() {
    super(XSRF_COOKIE_NAME);
  }

  @Override
  public void init() {
    findSharedDatabase();
    this.serverProps = readProperties(getServletContext());

    if (serverProps != null && serverProps.isAMAS()) {
      audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this, null);
    }
  }

  protected boolean hasAdminPerm(int userIDFromSessionOrDB) throws DominoSessionException {
    return getPermissions(userIDFromSessionOrDB)
        .contains(User.Permission.PROJECT_ADMIN);
  }

  boolean hasAdminOrCDPerm(int userIDFromSessionOrDB) throws DominoSessionException {
    Collection<User.Permission> permissions = getPermissions(userIDFromSessionOrDB);
    boolean hasPerm = permissions
        .contains(User.Permission.PROJECT_ADMIN) || permissions.contains(User.Permission.DEVELOP_CONTENT);
//    logger.info("hasAdminOrCDPerm for " + userIDFromSessionOrDB + " are " + permissions + " has perm " + hasPerm);
    return hasPerm;
  }

  boolean hasCDPerm(int userIDFromSessionOrDB) throws DominoSessionException {
    Collection<User.Permission> permissions = getPermissions(userIDFromSessionOrDB);
    boolean hasPerm = permissions.contains(User.Permission.DEVELOP_CONTENT);
    logger.info("hasCDPerm for " + userIDFromSessionOrDB + " are " + permissions + " has perm " + hasPerm);
    return hasPerm;
  }

  boolean hasQCPerm(int userIDFromSessionOrDB) throws DominoSessionException {
    Collection<User.Permission> permissions = getPermissions(userIDFromSessionOrDB);
    return permissions.contains(User.Permission.QUALITY_CONTROL) || permissions.contains(User.Permission.PROJECT_ADMIN);
  }

  /**
   * Also checks whether user is enabled and approved for netprof.
   *
   * @param userIDFromSessionOrDB
   * @return
   * @throws DominoSessionException
   */
  protected Collection<User.Permission> getPermissions(int userIDFromSessionOrDB) throws DominoSessionException {
//    logger.info("getPermissions for" + "\nuser " + userIDFromSessionOrDB);
    User userFromSession = db.getUserDAO().getByID(userIDFromSessionOrDB);
    if (userFromSession == null) {
      logger.error("getPermissions : no user in session?");
      throw new DominoSessionException();
    }
    boolean enabled = userFromSession.isEnabled();
    boolean isApprovedForNetprof = userFromSession.isHasAppPermission();
    if (!enabled) logger.info("getPermissions user " + userIDFromSessionOrDB + " not enabled");
    if (!isApprovedForNetprof) logger.info("getPermissions user " + userIDFromSessionOrDB + " not approved to use net");

    if (DEBUG) logger.info("getPermissions for" +
        "\nuser  " + userFromSession +
        "\nperms " + userFromSession.getPermissions());

    return enabled && isApprovedForNetprof ? userFromSession.getPermissions() : Collections.emptyList();
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
   * @see ExerciseServiceImpl#getExerciseIds(ExerciseListRequest)
   */
  protected int getProjectIDFromUser() throws DominoSessionException {
    return getProjectIDFromUser(getUserIDFromSessionOrDB());
  }

  protected int getProjectIDFromUser(int userIDFromSession) {
    if (userIDFromSession == -1) {
      // it's not in the current session - can we recover it from the remember me cookie?
      logger.warn("getProjectIDFromUser : no user in session, so we can't get the project id for the user.");
      return -1;
    } else {
      int mostRecentByUser = db.getUserProjectDAO().getCurrentProjectForUser(userIDFromSession);

      // why would we want to configure it as a side effect here???
      // if it's a new project, we'd want to configure it there?

    /*    Project project = db.getProject(getCurrentProjectForUser);
    if (project == null) {
      logger.warn("getProjectIDFromUser user " + userIDFromSession + " no project for id " + getCurrentProjectForUser);
    } else {
      logger.info("getProjectIDFromUser user " + userIDFromSession + " = project " + project.getID() + " " + project.getLanguage());
      db.configureProject(project, false); //check if we should configure it - might be a new project
    }*/
      return mostRecentByUser;
    }
  }

  protected Project getProject(int projID) {
    return db.getProject(projID);
  }

  private Project getProject() throws DominoSessionException {
    int userIDFromSession = getUserIDFromSessionOrDB();
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
   * @return -1 if no user session, else user id
   */
  protected int getUserIDFromSessionOrDB() throws DominoSessionException {
    return securityManager.getUserIDFromSessionLight(getThreadLocalRequest());
  }

  /**
   * Add startup info to user.
   *
   * @return
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser
   */
  public User getUserFromSession() throws DominoSessionException {
    try {
      User loggedInUser = getSessionUser();
      if (loggedInUser != null) {
        db.setStartupInfo(loggedInUser);
      }
      return loggedInUser;
    } catch (DominoSessionException d) {
      throw d;
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
    return securityManager == null ? null : securityManager.getLoggedInUser(getThreadLocalRequest());
  }

  int getSessionUserID() throws DominoSessionException {
    return securityManager.getLoggedInUserID(getThreadLocalRequest());
  }

  String getSessionID() {
    HttpServletRequest threadLocalRequest = getThreadLocalRequest();
    if (threadLocalRequest == null) return null;
    else return securityManager.getSessionID(threadLocalRequest);
  }

  /**
   * This is safe!
   *
   * @return
   */
  protected String getLanguage() throws DominoSessionException {
    return getLanguage(getProject());
  }

  protected String getLanguage(Project project) {
    if (project == null) {
      logger.error("getLanguage : no current project ");
      return "unset";
    } else {
      return project.getProject().language();
    }
  }

  protected Language getLanguageEnum(Project project) {
    if (project == null) {
      logger.error("getLanguage : no current project ");
      return Language.UNKNOWN;
    } else {
      return toEnum(project.getProject().language());
    }
  }

  protected Language getLanguageEnum(CommonExercise exercise) {
    if (exercise == null) {
      logger.error("getLanguage : no current project ");
      return Language.UNKNOWN;
    } else {
      int projectID = exercise.getProjectID();
      return getProject(projectID).getLanguageEnum();
      //return toEnum(project.getProject().language());
    }
  }

  protected Language getLanguageEnum(int projectID) {
    if (projectID == -1) {
      logger.error("getLanguage : no current project ");
      return Language.UNKNOWN;
    } else {
      return getProject(projectID).getLanguageEnum();
    }
  }

  private Language toEnum(String language) {
    Language language1;

    try {
      if (language.equalsIgnoreCase(MANDARIN)) language = Language.MANDARIN.name();

      language1 = Language.valueOf(language.toUpperCase());
    } catch (IllegalArgumentException e) {
      language1 = Language.UNKNOWN;
    }
    return language1;
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

      String subject = "Server Exception on " + getHostName() + " at " + installPath;
      String info = getInfo(prefixedMessage);

      logger.warn("logAndNotifyServerException : about to send message with" +
          "\n\tsubject   : " + subject +
          "\n\tmessage   : " + info +
          "\n\texception : " + e
      );

      sendEmail(subject, info);
//      if (e != null) {
//        logger.warn("logAndNotify : " + e, e);
//      }
//      logger.error(info, e);
    } else {
      logger.error("\n\nlogAndNotifyServerException : got " + e, e);
    }
  }

  protected String getHostName() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown host?";
    }
  }

  protected void sendEmail(String subject, String prefixedMessage) {
    getMailSupport().email(serverProps.getEmailAddress(), subject, prefixedMessage);
  }

  protected MailSupport getMailSupport() {
    return new MailSupport(serverProps);
  }

  protected String getInfo(String message) {
    HttpServletRequest request = getThreadLocalRequest();
    if (request != null) {
      String remoteAddr = getRemoteAddr(request);
      String userAgent = request.getHeader(USER_AGENT);

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

  @Override
  protected void doUnexpectedFailure(Throwable ex) {
    logger.info("Look at exception {} = {} ", ex.getClass().getCanonicalName(), ex.getMessage());
    if (ex.getClass().getCanonicalName().equals("org.apache.catalina.connector.ClientAbortException")) {
      logger.info("User reload during request.", ex);
    } else {
      //  logger.error("Got service Exception!", ex);
      logAndNotifyServerException(ex, "Got service exception in " + this.getClass().getCanonicalName());

      // This may not be necessary in production, but some exceptions
      // traces did not include full cause details when running in dev mode.
      if (ex.getCause() != null) {
        logger.warn("Tracing exception cause", ex.getCause());
      }
    }
    super.doUnexpectedFailure(ex);
  }


  protected ISection<CommonExercise> getSectionHelper() throws DominoSessionException {
    return getSectionHelper(getProjectIDFromUser());
  }

  private ISection<CommonExercise> getSectionHelper(int projectID) {
    return db.getSectionHelper(projectID);
  }

  ISection<IDialog> getDialogSectionHelper() throws DominoSessionException {
    return getDialogSectionHelper(getProjectIDFromUser());
  }

  private ISection<IDialog> getDialogSectionHelper(int projectID) {
    return db.getDialogSectionHelper(projectID);
  }

  /**
   * @return
   */
  protected AudioFileHelper getAudioFileHelper() throws DominoSessionException {
    Project project = getProject();
    if (serverProps.isAMAS()) {
      return audioFileHelper;
    } else {
      return getAudioFileHelper(project);
    }
  }

  protected AudioFileHelper getAudioFileHelper(int projectID) {
    if (serverProps.isAMAS()) {
      return audioFileHelper;
    } else {
      return getAudioFileHelper(db.getProject(projectID));
    }
  }

  @Nullable
  protected AudioFileHelper getAudioFileHelper(Project project) {
    if (project == null) {
      logger.error("getAudioFileHelper no current project???");
      return null;
    }
    return project.getAudioFileHelper();
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
   *
   * @return
   */
  protected DatabaseImpl getDatabase() {
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
   * @param servletContext db is shared via the servlet context
   * @param db             to share with other servlets that load after this one
   * @see #DATABASE_REFERENCE
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

  @NotNull
  protected RestrictedOperationException getRestricted(String updating_project_info) {
    return new RestrictedOperationException(updating_project_info, true);
  }

  String getRemoteAddr(HttpServletRequest request) {
    String remoteAddr = request.getHeader(X_FORWARDED_FOR);
    if (remoteAddr == null || remoteAddr.isEmpty()) {
      remoteAddr = request.getRemoteAddr();
    }
    return remoteAddr;
  }

  /**
   * @param id
   * @return
   * @throws DominoSessionException
   */
  public IDialog getDialog(int id) throws DominoSessionException {
    IDialog iDialog = getOneDialog(id);

  //  logger.info("getDialog get dialog " + id + "\n\treturns " + iDialog);

    if (iDialog != null) {
      int projid = iDialog.getProjid();
      Project project = db.getProject(projid);

      {
        Language language = project.getLanguageEnum();
        iDialog.getExercises().forEach(clientExercise ->
            db.getAudioDAO().attachAudioToExercise(clientExercise, language, new HashMap<>())
        );
      }

      new AlignmentHelper(serverProps, db.getRefResultDAO()).addAlignmentOutput(projid, project, iDialog.getExercises());
    }

    return iDialog;
  }


  private IDialog getOneDialog(int id) throws DominoSessionException {
    return getOneDialog(getUserIDFromSessionOrDB(), id);
  }

  /**
   * TODO : do this smarter!
   *
   * Return the first dialog if the id is -1 or bogus...
   *
   * @param userIDFromSessionOrDB
   * @param dialogID              -1 OK - just return the first dialog
   * @return the first dialog if the id is -1 or bogus...
   */
  @Nullable
  private IDialog getOneDialog(int userIDFromSessionOrDB, int dialogID) {
    List<IDialog> iDialogs = getDialogs(userIDFromSessionOrDB);
    if (dialogID == -1) {
      return iDialogs.isEmpty() ? null : iDialogs.get(0);
    } else {
      List<IDialog> collect = iDialogs.stream().filter(iDialog -> iDialog.getID() == dialogID).collect(Collectors.toList());
      return collect.isEmpty() ? iDialogs.isEmpty() ? null : iDialogs.iterator().next() : collect.iterator().next();
    }
  }

  protected List<IDialog> getDialogs(int userIDFromSessionOrDB) {
    return getDialogsForProject(getProjectIDFromUser(userIDFromSessionOrDB));
  }

   List<IDialog> getDialogsForProject(int projectIDFromUser) {
    List<IDialog> dialogList = new ArrayList<>();
    {
      if (projectIDFromUser != -1) {
        dialogList = getProject(projectIDFromUser).getDialogs();
      }
    }
    return dialogList;
  }
}
