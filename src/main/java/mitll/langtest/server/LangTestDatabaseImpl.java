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

import mitll.langtest.client.banner.UserMenu;
import mitll.langtest.client.services.LangTestDatabase;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import mitll.langtest.server.property.ServerInitializationManagerNetProf;
import mitll.langtest.server.services.MyRemoteServiceServlet;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.project.StartupInfo;
import mitll.langtest.shared.user.User;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;

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
  public static final String NO_POSTGRES = "Can't connect to postgres - please check the database configuration in application.conf or netprof.properties.";

  /**
   * @see
   */
  private String relativeConfigDir;
  private String configDir;
  private String startupMessage = "";

  /**
   * Reco test option lets you run through and score all the reference audio -- if you want to see model performance
   */
  @Override
  public void init() {
    try {
      ServletContext servletContext = getServletContext();
      this.pathHelper = new PathHelper(servletContext);
      this.serverProps = readProperties(servletContext);
      pathHelper.setProperties(serverProps);
      setInstallPath(db);

      if (serverProps.isAMAS()) {
        audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this, null);
      }
    } catch (Exception e) {
      startupMessage = e.getMessage();
      logger.error("Got " + e, e);
    }

    // optional (bogus?) initialization...
    optionalInit();
  }

  private void optionalInit() {
    try {
      db.preloadContextPractice();
      db.getStateManager().setStateOnExercises();
      db.doReport();
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
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void service(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
    ServletRequestContext ctx = new ServletRequestContext(request);
    boolean isMultipart = ServletFileUpload.isMultipartContent(ctx);
//    String contentType = ctx.getContentType();
    //logger.info("service content type " + contentType + " multi " + isMultipart);
    if (isMultipart) {
      logger.debug("isMultipart : Request " + request.getQueryString() + " path " + request.getPathInfo());
      FileUploadHelper.UploadInfo site = db.getProjectManagement().getFileUploadHelper().gotFile(request);
      if (site == null) {
        super.service(request, response);
      } else {
        db.getProjectManagement().getFileUploadHelper().doUploadInfoResponse(response, site);
      }
    } else {
      super.service(request, response);
    }
  }

  protected ISection<CommonExercise> getSectionHelper() throws DominoSessionException {
    return super.getSectionHelper();
  }

  private Collection<CommonExercise> getExercisesForUser() throws DominoSessionException {
    return db.getExercises(getProjectIDFromUser());
  }

  /**
   * @see UserMenu#getProjectSpecificChoices
   */
  public void sendReport() throws DominoSessionException, RestrictedOperationException {
    int userIDFromSession = securityManager.getUserIDFromSession(getThreadLocalRequest());

    User byID = db.getUserDAO().getByID(userIDFromSession);
    if (byID == null) {
      logger.error("huh? no user by " + userIDFromSession);
    } else {
      boolean hasPerm = byID.getPermissions().contains(User.Permission.PROJECT_ADMIN);
      if (hasPerm) {
        db.sendReport(userIDFromSession);
      } else {
        throw new RestrictedOperationException("send report", true);
      }
    }
  }

  /**
   * The very first thing that gets called from the client.
   * <p>
   * Get properties (first time called read properties file -- e.g. see war/config/levantine/config.properties).
   * <p>
   * Shows a better message if configuration is bad.
   *
   * @return
   * @see mitll.langtest.client.LangTest#onModuleLoad
   */
  @Override
  public StartupInfo getStartupInfo() {
    List<SlimProject> projectInfos = new ArrayList<>();
    if (db == null) {
      logger.info("getStartupInfo no db yet...");
    } else {
      projectInfos = db.getProjectManagement().getNestedProjectInfo();
    }

    if (db == null || !db.isHasValidDB()) {
      startupMessage = NO_POSTGRES;
    }
    StartupInfo startupInfo =
        new StartupInfo(serverProps.getUIProperties(), projectInfos, startupMessage, serverProps.getAffiliations());

//    logger.debug("getStartupInfo sending " + startupInfo);
    return startupInfo;
  }

  /**
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param hitID
   * @param device
   * @see mitll.langtest.client.instrumentation.ButtonFactory#logEvent
   */
  @Override
  public void logEvent(String id, String widgetType, String exid, String context, int userid, String hitID, String device) {
//    logger.debug("log event " + id + " " + widgetType + " exid "  +exid + " context " +context + " by user " + userid);

    try {
      if (db == null) {
        logger.error("no db set? " + id + " " + widgetType + " exid " + exid + " context " + context + " by user " + userid);
      } else {
        db.logEvent(id, widgetType, exid, context, userid, device);
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
      throw new RestrictedOperationException("getting events", true);
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
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasRecordPerm(userIDFromSessionOrDB)) {
      return db.getMaleFemaleProgress(getProjectIDFromUser(userIDFromSessionOrDB));
    } else {
      throw new RestrictedOperationException("getting recording progress", true);
    }
  }

  /**
   * @param ids            items the user has actually practiced/recorded audio for
   * @param latestResultID
   * @param typeToSection  indicates the unit and chapter(s) we're asking about
   * @param userListID     if we're asking about a list and not predef items
   * @return
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   */
  @Override
  public AVPScoreReport getUserHistoryForList(Collection<Integer> ids,
                                              long latestResultID,
                                              Map<String, Collection<String>> typeToSection,
                                              int userListID) throws DominoSessionException {
/*    logger.debug("getUserHistoryForList" +
        "\n\tuser " + userid + " and" +
        "\n\tids " + ids.size() +
        "\n\tlist " + userListID+
        " type to section " + typeToSection);
    */
    int userIDFromSession = getUserIDFromSessionOrDB();

    UserList<CommonShell> userListByID = userListID != -1 ? db.getUserListManager().getSimpleUserListByID(userListID) : null;

    if (userListByID == null && userListID != -1) logger.error("no user list for " + userListID);

    List<Integer> allIDs = new ArrayList<>();
    Map<Integer, CollationKey> idToKey = new HashMap<>();

    int projectID = getProjectIDFromUser(userIDFromSession);
    Collator collator = getCollator(projectID);
    if (userListByID != null) {
      for (CommonShell exercise : userListByID.getExercises()) {
        populateCollatorMap(allIDs, idToKey, collator, exercise);
      }
    } else {
      Collection<CommonExercise> exercisesForState = (typeToSection == null || typeToSection.isEmpty()) ? getExercisesForUser() :
          getSectionHelper().getExercisesForSelectionState(typeToSection);

      for (CommonExercise exercise : exercisesForState) {
        populateCollatorMap(allIDs, idToKey, collator, exercise);
      }
    }
    String language = db.getLanguage(projectID);
    //logger.debug("for " + typeToSection + " found " + allIDs.size());
    return db.getUserHistoryForList(userIDFromSession, ids, (int) latestResultID, allIDs, idToKey, language);
  }

  private Collator getCollator(int projid) {
    AudioFileHelper audioFileHelper = getAudioFileHelper(db.getProject(projid));
    return audioFileHelper == null ? null : audioFileHelper.getCollator();
  }

  private void populateCollatorMap(List<Integer> allIDs, Map<Integer, CollationKey> idToKey, Collator collator,
                                   CommonShell exercise) {
    allIDs.add(exercise.getID());
    CollationKey collationKey = collator.getCollationKey(exercise.getForeignLanguage());
    idToKey.put(exercise.getID(), collationKey);
  }

  public void logMessage(String message, boolean sendEmail) {
    if (message.length() > 10000) message = message.substring(0, 10000);
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " from client : " + message;
    logger.debug(prefixedMessage);

    if (message.startsWith("got browser exception") || sendEmail) {
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
   * @see #init()
   */
  private ServerProperties readProperties(ServletContext servletContext) {
    ServerInitializationManagerNetProf serverInitializationManagerNetProf = new ServerInitializationManagerNetProf();
    ServerProperties serverProps = serverInitializationManagerNetProf.getServerProps(servletContext);

    File configDir = serverInitializationManagerNetProf.getConfigDir();
    logger.info("readProperties : configDir from props " + configDir);

    this.relativeConfigDir = "config" + File.separator + servletContext.getInitParameter("config");
    this.configDir = configDir.getAbsolutePath() + File.separator + relativeConfigDir;
    //logger.info("readProperties relativeConfigDir " + relativeConfigDir + " configDir         " + configDir);

    try {
      db = makeDatabaseImpl(serverProps);
      // logger.info("readProperties made database " + db);
      securityManager = new NPUserSecurityManager(db.getUserDAO(), db.getUserSessionDAO());
      //  logger.info("readProperties made securityManager " + securityManager);
      db.setUserSecurityManager(securityManager);
    } catch (Exception e) {
      logger.error("readProperties got " + e, e);
    }

    shareDB(servletContext, db);
    // logger.info("readProperties shareDB ");
//    shareLoadTesting(servletContext);

    return serverProps;
  }
/*
  private void shareLoadTesting(ServletContext servletContext) {
    Object loadTesting = servletContext.getAttribute(ScoreServlet.LOAD_TESTING);
    if (loadTesting != null) {
      logger.debug("hmm... found existing load testing reference " + loadTesting);
    }
    servletContext.setAttribute(ScoreServlet.LOAD_TESTING, this);
  }
*/

  private DatabaseImpl makeDatabaseImpl(ServerProperties serverProps) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, serverProps.getH2Database(), serverProps, pathHelper, true, this, false
    );
  }

  /**
   * @param db
   * @return
   * @see LangTestDatabaseImpl#init()
   */
  private void setInstallPath(DatabaseServices db) {
    // logger.debug("setInstallPath " + installPath);
    if (db == null) {
      logger.error("no database services created.");
    } else {
      db.setInstallPath(pathHelper.getInstallPath(), "");
    }
  }
}
