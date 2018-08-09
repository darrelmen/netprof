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

package mitll.langtest.server;

import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.UserMenu;
import mitll.langtest.client.services.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.project.ProjectHelper;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import mitll.langtest.server.property.ServerInitializationManagerNetProf;
import mitll.langtest.server.services.MyRemoteServiceServlet;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.project.StartupInfo;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static mitll.hlt.domino.server.ServerInitializationManager.CONFIG_HOME_ATTR_NM;
import static mitll.hlt.domino.server.ServerInitializationManager.USER_SVC;

/**
 * Supports all the database interactions.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/7/12
 * Time: 5:49 PM
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends MyRemoteServiceServlet implements LangTestDatabase {
  private static final Logger logger = LogManager.getLogger(LangTestDatabaseImpl.class);

  private static final String NO_POSTGRES = "Can't connect to postgres.<br/>please check the database configuration in application.conf or netprof.properties.";
  private static final String GOT_BROWSER_EXCEPTION = "got browser exception";

  /**
   * @see
   */
  private String relativeConfigDir;
  private String startupMessage = "";

  /**
   * Reco test option lets you run through and score all the reference audio -- if you want to see model performance
   */
  @Override
  public void init() {
    try {
      ServletContext servletContext = getServletContext();
      String property = System.getProperty(CONFIG_HOME_ATTR_NM);
      logger.info("init : prop for domino = '" + property + "'");
      this.pathHelper = new PathHelper(servletContext);
      this.serverProps = readProperties(servletContext);
      pathHelper.setProperties(serverProps);
      setInstallPath(db, servletContext);
      // logger.info("finished init");

    } catch (Exception e) {
      startupMessage = e.getMessage();
      logger.error("Got " + e, e);
    }

    // optional (bogus?) initialization...
    optionalInit();
  }

  private void optionalInit() {
    try {
      logger.info("optionalInit -- ");
      if (db != null) db.doReport();
    } catch (Exception e) {
      logger.error("optionalInit couldn't load database " + e, e);
    }

    try {
      if (serverProps.isAMAS()) getAudioFileHelper().makeAutoCRT(relativeConfigDir);
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
  }

  /**
   * This allows us to upload an exercise file.
   *
   * This might be helpful if we want to stream audio in a simple way outside a GWT RPC call.
   *
   * @throws ServletException
   * @throws IOException
   * @paramx request
   * @paramx response
   */
/*  @Override
  protected void service(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
    ServletRequestContext ctx = new ServletRequestContext(request);
    boolean isMultipart = ServletFileUpload.isMultipartContent(ctx);
//    String contentType = ctx.getContentType();
    //logger.info("service content type " + contentType + " multi " + isMultipart);
    if (isMultipart) {
      logger.debug("isMultipart : Request " + request.getQueryString() + " path " + request.getPathInfo());
      FileUploadHelper.UploadInfo uploadInfo = db.getProjectManagement().getFileUploadHelper().gotFile(request);
      if (uploadInfo == null) {
        super.service(request, response);
      } else {
        db.getProjectManagement().getFileUploadHelper().doUploadInfoResponse(response, uploadInfo);
      }
    } else {
      super.service(request, response);
    }
  }*/


  /**
   * This report is for on demand sending the report to the current user.
   *
   * @see UserMenu#getProjectSpecificChoices
   */
  public void sendReport() throws DominoSessionException, RestrictedOperationException {
    int userIDFromSession = securityManager.getUserIDFromSessionLight(getThreadLocalRequest());

    User byID = db.getUserDAO().getByID(userIDFromSession);
    if (byID == null) {
      logger.error("huh? no user by " + userIDFromSession);
    } else {
      boolean hasPerm = byID.getPermissions().contains(User.Permission.PROJECT_ADMIN);
      if (hasPerm) {
        db.sendReport(userIDFromSession);
      } else {
        throw getRestricted("send report");
      }
    }
  }

  /**
   * The very first thing that gets called from the client.
   * <p>
   * Shows a better message if configuration is bad.
   *
   * @return
   * @see LangTest#askForStartupInfo
   * @see LangTest#refreshStartupInfo
   */
  @Override
  public StartupInfo getStartupInfo() {
    List<SlimProject> projectInfos = new ProjectHelper().getProjectInfos(db, securityManager);
    if (db == null || !db.isHasValidDB()) {
      startupMessage = NO_POSTGRES + "<br/>Using : " + serverProps.getDBConfig();
    }
    //long then = System.currentTimeMillis();
    StartupInfo startupInfo =
        new StartupInfo(serverProps.getUIProperties(), projectInfos, startupMessage, serverProps.getAffiliations());
//    long now = System.currentTimeMillis();
//    if (now - then > 100L) {
//      logger.info("getStartupInfo took " + (now - then) + " millis to get startup info.");
//    }
//    logger.debug("getStartupInfo sending " + startupInfo);
    return startupInfo;
  }

  /**
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param device
   * @param projID
   * @see mitll.langtest.client.instrumentation.ButtonFactory#logEvent
   */
  @Override
  public void logEvent(String id, String widgetType, String exid, String context, int userid, String device, int projID) {
//    logger.debug("log event " + id + " " + widgetType + " exid "  +exid + " context " +context + " by user " + userid);
    try {
      if (db == null) {
        logger.error("no db set? " + id + " " + widgetType + " exid " + exid + " context " + context + " by user " + userid);
      } else {
        db.logEvent(id, widgetType, exid, context, userid, device, projID);
      }
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @return
   * @see mitll.langtest.client.instrumentation.EventTable#show
   */
  public List<Event> getEvents() throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasAdminPerm(userIDFromSessionOrDB)) {
      return db.getEventDAO().getAll(getProjectIDFromUser(userIDFromSessionOrDB));
    } else {
      throw getRestricted("getting events");
    }
  }

  /**
   * Filter out the default audio recordings...
   *
   * @return
   * @see mitll.langtest.client.custom.recording.RecorderNPFHelper#getProgressInfo
   */
  @Override
  public Map<String, Float> getMaleFemaleProgress() throws DominoSessionException {
    return db.getMaleFemaleProgress(getProjectIDFromUser(getUserIDFromSessionOrDB()));
  }

  private static final String TEST_EXCEPTION = "Test Exception";

  public void logMessage(String message, boolean sendEmail) {
    if (message.length() > 10000) message = message.substring(0, 10000);
    String prefixedMessage = "on " + getHostName() + " from client : " + message;

    if (sendEmail) {
      logger.error(prefixedMessage);
    } else {
      logger.info(prefixedMessage);
    }

    if (message.startsWith(TEST_EXCEPTION) && sendEmail) {
      sendEmail(TEST_EXCEPTION, getInfo(prefixedMessage));

    } else if (message.startsWith(GOT_BROWSER_EXCEPTION) || sendEmail) {
      sendEmail("Javascript Exception", getInfo(prefixedMessage));
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    if (db == null) {
      logger.warn("DatabaseImpl was never made properly...");
    } else {
      try {
        logger.info("DatabaseImpl.destroy");
        db.getDatabase().close(); // TODO : redundant with h2 shutdown hook?
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }
      db.stopDecode();
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
    ServerInitializationManagerNetProf serverInitializationManagerNetProf = new ServerInitializationManagerNetProf();
    ServerProperties serverProps = serverInitializationManagerNetProf.getServerProps(servletContext);

    File configDir = serverInitializationManagerNetProf.getConfigDir();
    logger.info("readProperties : configDir from props " + configDir);

    this.relativeConfigDir = "config" + File.separator + servletContext.getInitParameter("config");
    //åString configDir1 = configDir.getAbsolutePath() + File.separator + relativeConfigDir;
    logger.info("readProperties relativeConfigDir " + relativeConfigDir + " configDir         " + configDir);

    try {
      Object attribute = servletContext.getAttribute(USER_SVC);

      if (attribute != null) {
        logger.info("got " + attribute + " : " + attribute.getClass());
      } else {
        logger.warn("readProperties : no " + USER_SVC + " attribute...? ");
      }

      db = makeDatabaseImpl(serverProps);
      logger.info("readProperties made database " + db);

      securityManager = new NPUserSecurityManager(db.getUserDAO(), db.getUserSessionDAO());
      logger.info("readProperties made securityManager " + securityManager);

      db.setUserSecurityManager(securityManager);
    } catch (Exception e) {
      logger.error("readProperties got " + e, e);
    }

    shareDB(servletContext, db);
    logger.info("readProperties shareDB ");
    return serverProps;
  }

  /**
   * @param serverProps
   * @return
   * @see #readProperties
   */
  private DatabaseImpl makeDatabaseImpl(ServerProperties serverProps) {
    return new DatabaseImpl(serverProps, pathHelper, this, getServletContext());
  }

  /**
   * @param db
   * @return
   * @see LangTestDatabaseImpl#init
   */
  private void setInstallPath(DatabaseServices db, ServletContext servletContext) {
    // logger.debug("setInstallPath " + installPath);
    if (db == null) {
      logger.error("no database services created.");
    } else {
      db.setInstallPath("", servletContext);
    }
  }
}
