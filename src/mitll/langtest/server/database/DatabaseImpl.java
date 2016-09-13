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

package mitll.langtest.server.database;

import mitll.langtest.server.*;
import mitll.langtest.server.amas.FileExerciseDAO;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.annotation.IAnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.audio.AudioDAO;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.audio.SlickAudioDAO;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.connection.MySQLConnection;
import mitll.langtest.server.database.connection.PostgreSQLConnection;
import mitll.langtest.server.database.contextPractice.ContextPracticeImport;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.server.database.instrumentation.SlickEventImpl;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.phone.Phone;
import mitll.langtest.server.database.phone.SlickPhoneDAO;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.ProjectDAO;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.database.refaudio.SlickRefResultDAO;
import mitll.langtest.server.database.result.*;
import mitll.langtest.server.database.reviewed.IReviewedDAO;
import mitll.langtest.server.database.reviewed.SlickReviewedDAO;
import mitll.langtest.server.database.user.*;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.SlickUserListDAO;
import mitll.langtest.server.database.userlist.SlickUserListExerciseJoinDAO;
import mitll.langtest.server.database.userlist.SlickUserListExerciseVisitorDAO;
import mitll.langtest.server.database.word.IWordDAO;
import mitll.langtest.server.database.word.SlickWordDAO;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.ContextPractice;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickRefResultJson;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.CollationKey;
import java.util.*;

/**
 * Note with H2 that :  <br></br>
 * * you can corrupt the database if you try to copy a file that's in use by another process. <br></br>
 * * one process can lock the database and make it inaccessible to a second one, seemingly this can happen
 * more easily when H2 lives inside a servlet container (e.g. tomcat). <br></br>
 * * it's not a good idea to close connections, especially in the context of a servlet inside a container, since
 * H2 will return "new" connections that have already been closed.   <br></br>
 * * it's not a good idea to reuse one connection...?  <br></br>
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/14/12
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseImpl implements Database {
  private static final Logger logger = Logger.getLogger(DatabaseImpl.class);
  private static final int LOG_THRESHOLD = 10;
  private static final String UNKNOWN = "unknown";
  private static final String SIL = "sil";
  public static final int IMPORT_PROJECT_ID = -100;
  private static final boolean DEBUG_ONE_PROJECT = false;

  private String installPath;

  private IUserDAO userDAO;
  private IUserSessionDAO userSessionDAO;
  private IResultDAO resultDAO;

  private IRefResultDAO refresultDAO;
  private IWordDAO wordDAO;
  private IPhoneDAO phoneDAO;

  private IAudioDAO audioDAO;
  private IAnswerDAO answerDAO;
  private IUserListManager userListManager;

  private IUserExerciseDAO userExerciseDAO;
  // private AddRemoveDAO addRemoveDAO;

  private IEventDAO eventDAO;
  private IProjectDAO projectDAO;
  private IUserProjectDAO userProjectDAO;

  private ContextPractice contextPractice;

  /**
   * Only for h2
   */
  @Deprecated
  private DatabaseConnection connection = null;
  private MonitoringSupport monitoringSupport;

  private final String configDir;
  private final ServerProperties serverProps;
  private final LogAndNotify logAndNotify;

  private static final boolean ADD_DEFECTS = false;
  private mitll.langtest.server.database.user.UserManagement userManagement = null;
  private final String absConfigDir;
  private SimpleExerciseDAO<AmasExerciseImpl> fileExerciseDAO;
  private PathHelper pathHelper;

  private final Map<Integer, Project> idToProject = new HashMap<>();

  /**
   * JUST FOR TESTING
   *
   * @param configDir
   * @param relativeConfigDir
   * @param dbName
   * @param serverProps
   * @param pathHelper
   * @param mustAlreadyExist
   * @param logAndNotify
   */
  public DatabaseImpl(String configDir, String relativeConfigDir, String dbName, ServerProperties serverProps,
                      PathHelper pathHelper, boolean mustAlreadyExist, LogAndNotify logAndNotify) {
    this(configDir, relativeConfigDir, dbName, serverProps, pathHelper, mustAlreadyExist, logAndNotify, false);
  }

  /**
   * @param configDir
   * @param relativeConfigDir
   * @param dbName
   * @param serverProps
   * @param pathHelper
   * @param mustAlreadyExist
   * @param logAndNotify
   * @param readOnly
   * @see mitll.langtest.server.LangTestDatabaseImpl#makeDatabaseImpl(String)
   */
  public DatabaseImpl(String configDir, String relativeConfigDir, String dbName, ServerProperties serverProps,
                      PathHelper pathHelper, boolean mustAlreadyExist, LogAndNotify logAndNotify, boolean readOnly) {
    this(serverProps.useH2() ?
            new H2Connection(configDir, dbName, mustAlreadyExist, logAndNotify, readOnly) :
            serverProps.usePostgres() ?
                new PostgreSQLConnection(dbName, logAndNotify) :
                serverProps.useMYSQL() ?
                    new MySQLConnection(dbName, logAndNotify) :
                    null,
        configDir, relativeConfigDir, dbName,
        serverProps,
        pathHelper, logAndNotify);
  }

  public DatabaseImpl(DatabaseConnection connection,
                      String configDir,
                      String relativeConfigDir,
                      String dbName,
                      ServerProperties serverProps,
                      PathHelper pathHelper,
                      LogAndNotify logAndNotify) {
    long then;
    long now;
    this.connection = connection;
    absConfigDir = configDir;
    this.configDir = relativeConfigDir;
    this.serverProps = serverProps;
    this.logAndNotify = logAndNotify;

    if (maybeGetH2Connection(relativeConfigDir, dbName, serverProps)) return;
    then = System.currentTimeMillis();

    initializeDAOs(pathHelper);
    now = System.currentTimeMillis();
    if (now - then > 300) {
      logger.info("took " + (now - then) + " millis to initialize DAOs for " + getOldLanguage(serverProps));
    }

    monitoringSupport = new MonitoringSupport(userDAO, resultDAO);
    this.pathHelper = pathHelper;

    if (!serverProps.useH2()) {
      populateProjects(false);
    }
//    logger.info("made DatabaseImpl : " + this);
  }

  private String getOldLanguage(ServerProperties serverProps) {
    return serverProps.getLanguage();
  }

  private boolean maybeGetH2Connection(String relativeConfigDir, String dbName, ServerProperties serverProps) {
    try {
      Connection connection1 = getConnection();
      if (connection1 == null && serverProps.useH2()) {
        logger.warn("couldn't open connection to database at " + relativeConfigDir + " : " + dbName);
        return true;
      } else {
        closeConnection(connection1);
      }
    } catch (Exception e) {
      logger.error("couldn't open connection to database, got " + e.getMessage(), e);
      return true;
    }
    return false;
  }

  /**
   * @param reload
   * @seex CopyToPostgres#createProjectIfNotExists
   */
  public void populateProjects(boolean reload) {
    populateProjects(pathHelper, serverProps, logAndNotify, reload);
  }

  /**
   * Fill in id->project map
   *
   * @see #populateProjects(boolean)
   */
  private void populateProjects(PathHelper pathHelper,
                                ServerProperties serverProps,
                                LogAndNotify logAndNotify,
                                boolean reload) {
    Collection<SlickProject> all = projectDAO.getAll();
    logger.info("populateProjects : found " + all.size() + " projects");

    for (SlickProject slickProject : all) {
      if (!idToProject.containsKey(slickProject.id())) {
        if (DEBUG_ONE_PROJECT) {
          if (slickProject.language().equalsIgnoreCase("english")) {
            rememberProject(pathHelper, serverProps, logAndNotify, reload, slickProject);
          }
        } else {
          rememberProject(pathHelper, serverProps, logAndNotify, reload, slickProject);
        }
      }
    }

    if (reload) {
      doReload(reload);
    }

    logger.info("populateProjects (reload = " + reload + ") now project ids " + idToProject.keySet());
    for (Project project : getProjects()) {
      logger.info("\tproject " + project);
    }
  }

  private void doReload(boolean reload) {
    for (Project project : getProjects()) {
      if (project.getExerciseDAO() == null) {
        setExerciseDAO(project);
        configureProject(installPath, project);
        logger.info("\tpopulateProjects : " + project + " : " + project.getAudioFileHelper());
      }
    }
  }

  /**
   *  @param pathHelper
   * @param serverProps
   * @param logAndNotify
   * @param reload
   * @param slickProject
   * @see #populateProjects(PathHelper, ServerProperties, LogAndNotify, boolean)
   */
  private void rememberProject(PathHelper pathHelper, ServerProperties serverProps, LogAndNotify logAndNotify,
                               boolean reload, SlickProject slickProject) {
    Project project = new Project(slickProject, pathHelper, serverProps, this, logAndNotify);
    idToProject.put(project.getProject().id(), project);
    logger.info("populateProjects (reload = " + reload + ") : " + project + " : " + project.getAudioFileHelper());
  }

  private void addSingleProject(ExerciseDAO<CommonExercise> jsonExerciseDAO) {
    idToProject.put(IMPORT_PROJECT_ID, new Project(jsonExerciseDAO));
  }

  private Connection getConnection() {
    return getConnection(this.getClass().toString());
  }

  /**
   * Slick db connection.
   */
  private DBConnection dbConnection;

  /**
   * Create or alter tables as needed.
   *
   * @see #DatabaseImpl(DatabaseConnection, String, String, String, ServerProperties, PathHelper, LogAndNotify)
   */
  private void initializeDAOs(PathHelper pathHelper) {
    dbConnection = getDbConnection();

    eventDAO = new SlickEventImpl(dbConnection);
    this.userDAO = new SlickUserDAOImpl(this, dbConnection);
    this.userSessionDAO = new SlickUserSessionDAOImpl(this, dbConnection);
    audioDAO = new SlickAudioDAO(this, dbConnection, this.userDAO);
    resultDAO = new SlickResultDAO(this, dbConnection);
    answerDAO = new SlickAnswerDAO(this, dbConnection);
//    addRemoveDAO = new AddRemoveDAO(this);

    refresultDAO = new SlickRefResultDAO(this, dbConnection, serverProps.shouldDropRefResult());
    userExerciseDAO = new SlickUserExerciseDAO(this, dbConnection);//, getExerciseToPhone(refresultDAO));
    wordDAO = new SlickWordDAO(this, dbConnection);
    phoneDAO = new SlickPhoneDAO(this, dbConnection);

    SlickUserListExerciseJoinDAO userListExerciseJoinDAO = new SlickUserListExerciseJoinDAO(this, dbConnection);
    IUserListDAO userListDAO = new SlickUserListDAO(this, dbConnection, this.userDAO, userExerciseDAO);
    IAnnotationDAO annotationDAO = new SlickAnnotationDAO(this, dbConnection, this.userDAO.getDefectDetector());

    IReviewedDAO reviewedDAO = new SlickReviewedDAO(this, dbConnection, true);
    IReviewedDAO secondStateDAO = new SlickReviewedDAO(this, dbConnection, false);

    userListManager = new UserListManager(this.userDAO,
        userListDAO,
        userListExerciseJoinDAO,
        annotationDAO,
        reviewedDAO,
        secondStateDAO,
        new SlickUserListExerciseVisitorDAO(this, dbConnection),
        pathHelper);

    projectDAO = new ProjectDAO(this, dbConnection);
    userProjectDAO = new UserProjectDAO(dbConnection);

    createTables();

    userDAO.findOrMakeDefectDetector();

    try {
      ((UserListManager) userListManager).setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

//    long then = System.currentTimeMillis();
//
//    long now = System.currentTimeMillis();
//    if (now - then > 1000) logger.info("took " + (now - then) + " millis to put back word and phone");
  }

  /**
   * @param refResultDAO
   * @return
   * @see #initializeDAOs(PathHelper)
   */
  Map<Integer, ExercisePhoneInfo> getExerciseToPhone(IRefResultDAO refResultDAO) {
    long then = System.currentTimeMillis();
    List<SlickRefResultJson> jsonResults = refResultDAO.getJsonResults();
    long now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " millis to get ref results");
    Map<Integer, ExercisePhoneInfo> exToPhones = new HashMap<>();

    ParseResultJson parseResultJson = new ParseResultJson(null);

    for (SlickRefResultJson exjson : jsonResults) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJson(exjson.scorejson());
      List<TranscriptSegment> transcriptSegments = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);

      int exid = exjson.exid();
      ExercisePhoneInfo phonesForEx = exToPhones.get(exid);
      if (phonesForEx == null) exToPhones.put(exid, phonesForEx = new ExercisePhoneInfo());

      {
        Set<String> phones = new HashSet<>();
        for (TranscriptSegment segment : transcriptSegments) phones.add(segment.getEvent());
        phonesForEx.addPhones(phones);
      }
      phonesForEx.setNumPhones(exjson.numalignphones());
    }
    logger.info("took " + (System.currentTimeMillis() - then) + " millis to populate ex->phone map");

    return exToPhones;
  }

  /**
   * @return
   * @see #initializeDAOs
   */
  private DBConnection getDbConnection() {
    logger.info("getDbConnection using " + serverProps.getDBConfig());
    return new DBConnection(serverProps.getDBConfig());
  }

  public IAudioDAO getH2AudioDAO() {
    return new AudioDAO(this, this.userDAO);
  }

  public IResultDAO getResultDAO() {
    return resultDAO;
  }

  /**
   * @param projectid
   * @return
   */
  public IAnalysis getAnalysis(int projectid) {
    return getProject(projectid).getAnalysis();
  }

  public IRefResultDAO getRefResultDAO() {
    return refresultDAO;
  }

  public IUserDAO getUserDAO() {
    return userDAO;
  }

  @Override
  public Connection getConnection(String who) {
    if (connection == null) {
      logger.warn("no connection created " + who + " use h2 property = " + serverProps.useH2());
      return null;
    } else {
      return connection.getConnection(who);
    }
  }

  /**
   * It seems like this isn't required?
   *
   * @param conn
   * @throws SQLException
   */
  public void closeConnection(Connection conn) {
    try {
      if (connection != null) {
        int before = connection.connectionsOpen();
        if (conn != null && !conn.isClosed()) {
          if (connection.usingCP()) {
            conn.close();
          }
        }
        //else {
        //logger.warn("trying to close a null connection...");
        // }
        if (connection.connectionsOpen() > LOG_THRESHOLD) {
          logger.debug("closeConnection : now " + connection.connectionsOpen() + " open vs before " + before);
        }
      }
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
  }

  /**
   * @throws SQLException
   * @seex mitll.langtest.server.database.custom.UserListManagerTest#tearDown
   */
  public void closeConnection() throws SQLException {
/*
    logger.debug("   ------- testing only closeConnection : now " + connection.connectionsOpen() + " open.");
    Connection connection1 = connection.getConnection(this.getClass().toString());
    if (connection1 != null) {
      connection1.close();
    }
*/
  }

  public MonitoringSupport getMonitoringSupport() {
    return monitoringSupport;
  }

  //private String mediaDir;

  /**
   * @param installPath
   * @param lessonPlanFile
   * @param mediaDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#setInstallPath
   */
  public void setInstallPath(String installPath, String lessonPlanFile, String mediaDir) {
    logger.debug("got install path " + installPath + " media " + mediaDir);
    this.installPath = installPath;
    //this.mediaDir = mediaDir;
    makeDAO(lessonPlanFile, mediaDir, installPath);
  }

  /**
   * JUST FOR TESTING
   *
   * @return
   */
  @Deprecated
  public SectionHelper<CommonExercise> getSectionHelper() {
    return getSectionHelper(-1);
  }

  /**
   * TODO : sections are valid in the context of a project.
   *
   * @param projectid
   * @return
   * @see mitll.langtest.server.DownloadServlet#returnSpreadsheet
   * @see Database#getTypeOrder
   */
  public SectionHelper<CommonExercise> getSectionHelper(int projectid) {
    if (isAmas()) {
      return new SectionHelper<>();
    }

    getExercises(projectid);
    return getProject(projectid).getSectionHelper();
  }

  private boolean isAmas() {
    return serverProps.isAMAS();
  }

  public Collection<String> getTypeOrder(int projectid) {
    SectionHelper sectionHelper = (isAmas()) ? getAMASSectionHelper() : getSectionHelper(projectid);
    if (sectionHelper == null) logger.warn("no section helper for " + this);
    List<String> objects = Collections.emptyList();
    return (sectionHelper == null) ? objects : sectionHelper.getTypeOrder();
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultASRInfo
   * @see mitll.langtest.server.DownloadServlet#getFilenameForDownload
   * @see #deleteItem(int, int)
   * @see #getCustomOrPredefExercise(int, int)
   */
  public CommonExercise getExercise(int projectid, int id) {
    Project project = getProjectOrFirst(projectid);
    return project.getExercise(id);
  }

  /**
   * JUST FOR TESTING
   *
   * @return
   */
  @Deprecated
  public Collection<CommonExercise> getExercises() {
    return getExercises(-1);
  }

  /**
   * TODO : exercises are in the context of a project
   *
   * @param projectid
   * @return
   * @see #getExercises(int)
   * @see Project#buildExerciseTrie
   */
  public Collection<CommonExercise> getExercises(int projectid) {
    if (isAmas()) {
      return Collections.emptyList();
    }
    Project project = getProjectOrFirst(projectid);


    List<CommonExercise> rawExercises = project.getRawExercises();
    if (rawExercises.isEmpty()) {
      logger.warn("getExercises no exercises in " + getServerProps().getLessonPlan() + " at " + installPath);
    }
    return rawExercises;
  }

  private Project getProjectOrFirst(int projectid) {
    return projectid == -1 ? getFirstProject() : getProject(projectid);
  }

  public ExerciseDAO<CommonExercise> getExerciseDAO(int projectid) {
    Project project = getProject(projectid);
    logger.debug("getExerciseDAO " + projectid + " found project " + project);
    ExerciseDAO<CommonExercise> exerciseDAO = project.getExerciseDAO();
    logger.debug("getExerciseDAO " + projectid + " found exercise dao " + exerciseDAO);

    return exerciseDAO;
  }

  public Project getProjectForUser(int userid) {
    return getProject(getUserProjectDAO().mostRecentByUser(userid));
  }

  public void stopDecode() {
    for (Project project : getProjects()) project.stopDecode();
  }

  /**
   * Make sure there's a favorites list per user per project
   *
   * @param userid
   * @param projectid
   * @see mitll.langtest.server.services.UserServiceImpl#setProject
   */
  public void rememberProject(int userid, int projectid) {
    //  logger.info("rememberProject user " + userid + " -> " + projectid);
    getUserProjectDAO().add(userid, projectid);
    getUserListManager().createFavorites(userid, projectid);
  }

  public void forgetProject(int userid) {
    getUserProjectDAO().forget(userid);
  }

  /**
   * @param userWhere
   * @see mitll.langtest.server.services.UserServiceImpl#setSessionUser(HttpSession, User)
   */
  public void setStartupInfo(User userWhere) {
    int i = getUserProjectDAO().mostRecentByUser(userWhere.getId());
    setStartupInfo(userWhere, i);
  }

  /**
   * @param userWhere
   * @param projid
   * @see #setStartupInfo(User)
   * @see mitll.langtest.server.services.UserServiceImpl#setProject(int)
   */
  public void setStartupInfo(User userWhere, int projid) {
    logger.info("setStartupInfo : For user " + userWhere + " projid " + projid);

    if (projid == -1) {
      logger.info("For " + userWhere + " no current project.");
    } else {
      if (!idToProject.containsKey(projid)) {
        logger.info("\tsetStartupInfo : populateProjects...");
        populateProjects(false);
      }

      Project project = getProject(projid);

      SlickProject project1 = project.getProject();
      List<String> typeOrder = project.getTypeOrder();
      boolean sound = typeOrder.remove(SlickUserExerciseDAO.SOUND);
      boolean diff = typeOrder.remove(SlickUserExerciseDAO.DIFFICULTY);
      if (!sound) logger.warn("sound missing???");
      else {
        typeOrder.add(SlickUserExerciseDAO.SOUND);
      }

      if (!diff) {

      } else {
        //typeOrder.add(SlickUserExerciseDAO.DIFFICULTY);
      }

      ProjectStartupInfo startupInfo = new ProjectStartupInfo(
          getServerProps().getProperties(),
          typeOrder,
          project.getSectionHelper().getSectionNodes(typeOrder),
          project1.id(),
          project1.language(),
          hasModel(project1));
      logger.info("setStartupInfo : For " + userWhere +
          "\n\t " + typeOrder +
          "\n\tSet startup info " + startupInfo);
      userWhere.setStartupInfo(startupInfo);
    }
  }

  private boolean hasModel(SlickProject project1) {
    return project1.getProp(ServerProperties.MODELS_DIR) != null;
  }

  private Project getFirstProject() {
    return getProjects().iterator().next();
  }

  /**
   * Amas dedicated calls
   *
   * @return
   */
  public List<AmasExerciseImpl> getAMASExercises() {
    return fileExerciseDAO.getRawExercises();
  }

  public AmasExerciseImpl getAMASExercise(int id) {
    return fileExerciseDAO.getExercise(id);
  }

  public SectionHelper<AmasExerciseImpl> getAMASSectionHelper() {
    return fileExerciseDAO.getSectionHelper();
  }

  /**
   * Lazy, latchy instantiation of DAOs.
   * Not sure why it really has to be this way.
   * <p>
   * Special check for amas exercises...
   *
   * @param mediaDir
   * @see #setInstallPath(String, String, String)
   */
  private void makeDAO(String lessonPlanFile, String mediaDir, String installPath) {
    if (userManagement == null) {
      synchronized (this) {
        boolean isURL = serverProps.getLessonPlan().startsWith("http");
        boolean amas = isAmas();
        int numExercises;

        if (amas) {
//          logger.info("Got " + lessonPlanFile);
          numExercises = readAMASExercises(lessonPlanFile, mediaDir, installPath, isURL);
        } else {
          makeExerciseDAO(lessonPlanFile, isURL);

          //       logger.info("set exercise dao " + exerciseDAO + " on " + userExerciseDAO);
          if (getProjects().isEmpty()) {
            logger.warn("no projects loaded yet...?");
          } else {
            ExerciseDAO<CommonExercise> exerciseDAO = getProjects().iterator().next().getExerciseDAO();
            userExerciseDAO.setExerciseDAO(exerciseDAO);
          }
          // if (!serverProps.useH2()) {
          configureProjects(installPath);
          //}
        }
        userManagement = new mitll.langtest.server.database.user.UserManagement(userDAO, resultDAO);
      }
    }
  }

  private void configureProjects(String installPath) {
    // TODO : this seems like a bad idea --
    Map<Integer, ExercisePhoneInfo> exerciseToPhone = getExerciseToPhone(refresultDAO);
    userExerciseDAO.setExToPhones(exerciseToPhone);

    for (Project project : getProjects()) {
      configureProject(installPath, project);
    }
  }

  /**
   * @param installPath
   * @param project
   * @see #makeDAO(String, String, String)
   */
  private void configureProject(String installPath, Project project) {
    logger.info("configureProject " + project + " install path " + installPath);

    ExerciseDAO<?> exerciseDAO1 = project.getExerciseDAO();
    SlickProject project1 = project.getProject();
    if (project1 == null) logger.info("note : no project for " + project);
    int id = project1 == null ? -1 : project1.id();
    setDependencies(exerciseDAO1, id);

    List<CommonExercise> rawExercises = project.getRawExercises();
    if (!rawExercises.isEmpty()) {
      logger.debug("first exercise is " + rawExercises.iterator().next());
    }
    project.setJsonSupport(new JsonSupport(project.getSectionHelper(), getResultDAO(), getRefResultDAO(), getAudioDAO(),
        getPhoneDAO()));

    if (project1 != null) {
      Map<Integer, String> exerciseIDToRefAudio = getExerciseIDToRefAudio(id);
      project.setAnalysis(
          new SlickAnalysis(this,
              phoneDAO,
              exerciseIDToRefAudio,
              (SlickResultDAO) resultDAO)
      );
      userExerciseDAO.getTemplateExercise(id);
    }
    logMemory();
  }

  private void logMemory() {
    int MB = (1024 * 1024);
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    logger.debug(" current thread group " + threadGroup.getName() + " = " + threadGroup.activeCount() +
        " : # cores = " + Runtime.getRuntime().availableProcessors() + " heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }

  /**
   * @param lessonPlanFile
   * @param isURL
   * @see #makeDAO(String, String, String)
   */
  private void makeExerciseDAO(String lessonPlanFile, boolean isURL) {
    if (isURL) {
      addSingleProject(new JSONURLExerciseDAO(getServerProps(), userListManager, ADD_DEFECTS));
    } else if (!serverProps.useH2()) {
      setExerciseDAOs();
    } else if (lessonPlanFile.endsWith(".json")) {
      logger.info("got " + lessonPlanFile);
      JSONExerciseDAO jsonExerciseDAO = new JSONExerciseDAO(lessonPlanFile, getServerProps(), userListManager, ADD_DEFECTS);
      addSingleProject(jsonExerciseDAO);
    } else {
      logger.info("makeExerciseDAO reading from excel sheet " + lessonPlanFile);
      addSingleProject(new ExcelImport(lessonPlanFile, getServerProps(), userListManager, ADD_DEFECTS));
    }
  }

  /**
   * @see #makeExerciseDAO(String, boolean)
   */
  private void setExerciseDAOs() {
    for (Project project : getProjects()) {
      // if (project.getProject().id() == 3)
//      logger.info("makeExerciseDAO project     " + project);
      setExerciseDAO(project);
      //    logger.info("makeExerciseDAO project now " + project);
    }
  }

  /**
   * @param project
   * @see #populateProjects(boolean)
   */
  private void setExerciseDAO(Project project) {
//    logger.info("setExerciseDAO on " + project);
    SlickProject project1 = project.getProject();
    DBExerciseDAO dbExerciseDAO = new DBExerciseDAO(getServerProps(), userListManager, ADD_DEFECTS,
        (SlickUserExerciseDAO) getUserExerciseDAO(), project1);
    project.setExerciseDAO(dbExerciseDAO);
  }

  public Project getProject(int projectid) {
    if (projectid == -1) return getFirstProject();
    Project project = idToProject.get(projectid);
    if (project == null) {
      Project firstProject = getFirstProject();
      logger.error("no project with id " + projectid + " in known projects (" + idToProject.keySet() +
          ")" +
          " returning first " + firstProject);
      return firstProject;
    }
    return project;
  }

/*  public Project getProjectForgiving(int projectid) {
    Project project = idToProject.get(projectid);
    if (project == null) {
      populateProjects(false);
    }
    return idToProject.get(projectid);
  }*/

  private Collection<Project> getProjects() {
    return idToProject.values();
  }

  private int readAMASExercises(String lessonPlanFile, String mediaDir, String installPath, boolean isURL) {
    int numExercises;
    if (isURL) {
      this.fileExerciseDAO = new AMASJSONURLExerciseDAO(getServerProps());
      numExercises = fileExerciseDAO.getNumExercises();
    } else {
      fileExerciseDAO = new FileExerciseDAO<>(mediaDir, getOldLanguage(serverProps), absConfigDir,
          lessonPlanFile, installPath);
      numExercises = fileExerciseDAO.getNumExercises();
    }
    return numExercises;
  }

  public void reloadExercises(int projectid) {
    ExerciseDAO<CommonExercise> exerciseDAO = getExerciseDAO(projectid);
    if (exerciseDAO != null) {
      logger.info("reloading from exercise dao");
      exerciseDAO.reload();
    } else {
      if (fileExerciseDAO != null) {
        logger.info("reloading from fileExerciseDAO");
        fileExerciseDAO.reload();
        // numExercises = fileExerciseDAO.getNumExercises();
      } else {
        logger.error("huh? no exercise DAO yet???");
      }
    }
  }

  /**
   * Public for testing only...
   *
   * @param exerciseDAO
   * @param projid
   * @see #configureProject(String, Project)
   */
  public void setDependencies(ExerciseDAO exerciseDAO, int projid) {
    exerciseDAO.setDependencies(userExerciseDAO, null /*addRemoveDAO*/, audioDAO, projid);
  }

  private void makeContextPractice(String contextPracticeFile, String installPath) {
    if (contextPractice == null && contextPracticeFile != null) {
      synchronized (this) {
        this.contextPractice = new ContextPracticeImport(installPath + File.separator + contextPracticeFile).getContextPractice();
      }
    }
  }

  /**
   * @param userExercise
   * @see mitll.langtest.server.services.ListServiceImpl#editItem
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  public void editItem(CommonExercise userExercise) {
    logger.debug("editItem ex #" + userExercise.getID() + " mediaDir : " + getServerProps().getMediaDir() +
        " initially audio was\n\t " + userExercise.getAudioAttributes());

    getUserListManager().editItem(userExercise, true, getServerProps().getMediaDir());

    Set<AudioAttribute> original = new HashSet<>(userExercise.getAudioAttributes());
    Set<AudioAttribute> defects = audioDAO.getAndMarkDefects(userExercise, userExercise.getFieldToAnnotation());

    logger.debug("editItem originally had " + original.size() + " attribute, and " + defects.size() + " defects");

    int projectID = userExercise.getProjectID();
    CommonExercise exercise = getExerciseDAO(projectID).addOverlay(userExercise);
    boolean notOverlay = exercise == null;
    if (notOverlay) {
      // not an overlay! it's a new user exercise
      exercise = getUserExerciseByExID(userExercise.getID());
      logger.debug("not an overlay " + exercise);
    } else {
      exercise = userExercise;
      logger.debug("made overlay " + exercise);
    }

    if (notOverlay) {
      logger.error("huh? couldn't make overlay or find user exercise for " + userExercise);
    } else {
      boolean b = original.removeAll(defects);  // TODO - does this work really without a compareTo?
      logger.debug(b ? "removed defects " + original.size() + " now" : " didn't remove any defects - " + defects.size());

      MutableAudioExercise mutableAudio = exercise.getMutableAudio();
      for (AudioAttribute attribute : defects) {
        if (!mutableAudio.removeAudio(attribute)) {
          logger.warn("huh? couldn't remove " + attribute.getKey() + " from " + exercise.getID());
        }
      }

      // why would this make sense to do???
/*      String overlayID = exercise.getOldID();

      logger.debug("editItem copying " + original.size() + " audio attrs under exercise overlay id " + overlayID);

      for (AudioAttribute toCopy : original) {
        if (toCopy.getUserid() < UserDAO.DEFAULT_FEMALE_ID) {
          logger.error("bad user id for " + toCopy);
        }
        logger.debug("\t copying " + toCopy);
        audioDAO.add((int) toCopy.getUserid(), toCopy.getAudioRef(), overlayID, toCopy.getTimestamp(), toCopy.getAudioType(), toCopy.getDurationInMillis());
      }*/
    }

    getSectionHelper(projectID).refreshExercise(exercise);
  }

  /**
   * @param audioAttribute
   * @see mitll.langtest.server.LangTestDatabaseImpl#markAudioDefect
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   */
  public void markAudioDefect(AudioAttribute audioAttribute) {
    if (audioDAO.markDefect(audioAttribute) < 1) {
      logger.error("markAudioDefect huh? couldn't mark error on " + audioAttribute);
    }
  }

  /**
   * @param userid
   * @param typeToSection
   * @return
   * @paramx collator
   * @see mitll.langtest.server.ScoreServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public JSONObject getJsonScoreHistory(int userid,
                                        Map<String, Collection<String>> typeToSection,
                                        ExerciseSorter sorter) {
    return getJsonSupport(userid).getJsonScoreHistory(userid, typeToSection, sorter);
  }

  private JsonSupport getJsonSupport(int userid) {
    int i = getUserProjectDAO().mostRecentByUser(userid);
    return getJsonSupportForProject(i);
  }

  private JsonSupport getJsonSupportForProject(int i) {
    Project project = getProject(i);
    return project.getJsonSupport();
  }

  /**
   * @param typeToSection
   * @param projectid
   * @return
   * @see ScoreServlet#getRefInfo(String, JSONObject)
   */
  public JSONObject getJsonRefResult(Map<String, Collection<String>> typeToSection, int projectid) {
    return getJsonSupportForProject(projectid).getJsonRefResults(typeToSection);
  }

  /**
   * TODO : make sure that iOS app has same idea of current project as does website
   * For all the exercises in a chapter
   * <p>
   * Get latest results
   * Get phones for latest
   * <p>
   * //Score phones
   * Sort phone scores – asc
   * <p>
   * Map phone->example
   * <p>
   * Join phone->word
   * <p>
   * Sort word by score asc
   *
   * @return
   * @see mitll.langtest.server.ScoreServlet#getPhoneReport
   */
  public JSONObject getJsonPhoneReport(long userid, int projid, Map<String, Collection<String>> typeToValues) {
    return getJsonSupport((int) userid).getJsonPhoneReport(userid, projid, typeToValues, getLanguage(projid));
  }

  /**
   * does all average calc on server
   *
   * @return
   * @paramx listid
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   */
  public AVPScoreReport getUserHistoryForList(int userid,
                                              Collection<Integer> ids,
                                              int latestResultID,
                                              Collection<Integer> allIDs,
                                              Map<Integer, CollationKey> idToKey) {
    logger.debug("getUserHistoryForList " + userid + " and " + ids.size() + " ids, latest " + latestResultID);

    SessionsAndScores sessionsAndScores = resultDAO.getSessionsForUserIn2(ids, latestResultID, userid, allIDs, idToKey);
    List<Session> sessionsForUserIn2 = sessionsAndScores.getSessions();

    Map<Integer, User> userMap = userDAO.getUserMap();

    AVPHistoryForList sessionAVPHistoryForList = new AVPHistoryForList(sessionsForUserIn2, userid, true);
    AVPHistoryForList sessionAVPHistoryForList2 = new AVPHistoryForList(sessionsForUserIn2, userid, false);

    // sort by correct %
    Collections.sort(sessionsForUserIn2, new Comparator<Session>() {
      @Override
      public int compare(Session o1, Session o2) {
        float correctPercent = o1.getCorrectPercent();
        float correctPercent1 = o2.getCorrectPercent();
        return correctPercent < correctPercent1 ? +1 : correctPercent > correctPercent1 ? -1 :
            compareTimestamps(o1, o2);
      }
    });

    int count = 0;
    List<AVPHistoryForList.UserScore> scores = new ArrayList<>();

    for (Session session : sessionsForUserIn2) {
      if (count++ < 10 || session.isLatest()) {
        scores.add(makeScore(count, userMap, session, true));
      }
    }

    logger.debug("getUserHistoryForList correct scores " + scores);

    if (scores.size() == 11) {
      scores.remove(9);
    }
    sessionAVPHistoryForList.setScores(scores);

    Collections.sort(sessionsForUserIn2, new Comparator<Session>() {
      @Override
      public int compare(Session o1, Session o2) {
        return o1.getAvgScore() < o2.getAvgScore() ? +1 : o1.getAvgScore() > o2.getAvgScore() ? -1 : compareTimestamps(o1, o2);
      }
    });

    count = 0;
    scores = new ArrayList<>();

    for (Session session : sessionsForUserIn2) {
      if (count++ < 10 || session.isLatest()) {
        scores.add(makeScore(count, userMap, session, false));
      }
    }
    logger.debug("getUserHistoryForList pron    scores " + scores);

    if (scores.size() == 11) {
      scores.remove(9);
    }

    sessionAVPHistoryForList2.setScores(scores);

    List<AVPHistoryForList> historyForLists = new ArrayList<>();
    historyForLists.add(sessionAVPHistoryForList);
    historyForLists.add(sessionAVPHistoryForList2);

//    logger.debug("returning " + historyForLists);
//    logger.debug("correct/incorrect history " + sessionsAndScores.sortedResults);
    return new AVPScoreReport(historyForLists, sessionsAndScores.getSortedResults());
  }

  private int compareTimestamps(Session o1, Session o2) {
    return o1.getTimestamp() < o2.getTimestamp() ? +1 : o1.getTimestamp() > o2.getTimestamp() ? -1 : 0;
  }

  private AVPHistoryForList.UserScore makeScore(int count, Map<Integer, User> userMap, Session session, boolean useCorrect) {
    float value = useCorrect ? session.getCorrectPercent() : 100f * session.getAvgScore();
    int userid = session.getUserid();
    User user = userMap.get(userid);
    String userID;
    if (user == null) {
      logger.warn("huh? couldn't find userid " + userid + " in map with keys " + userMap.keySet());
      userID = "Default User";
    } else {
      userID = user.getUserID();
    }
    return new AVPHistoryForList.UserScore(count,
        userID,
        value,
        session.isLatest());
  }

  /**
   * @see LangTestDatabaseImpl#init()
   */
  public void preloadContextPractice() {
    makeContextPractice(getServerProps().getDialogFile(), installPath);
  }

  /**
   * @return
   * @see LangTestDatabaseImpl#getContextPractice()
   */
  public ContextPractice getContextPractice() {
    if (this.contextPractice == null) {
      makeContextPractice(getServerProps().getDialogFile(), installPath);
    }
    return this.contextPractice;
  }

  /**
   * @param request
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param isMale
   * @param age
   * @param dialect
   * @param device
   * @param projid
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#addUser
   */
/*  public User addUser(HttpServletRequest request, String userID, String passwordH, String emailH, User.Kind kind,
                      boolean isMale, int age, String dialect, String device, int projid) {
    return userManagement.addUser(request, userID, passwordH, emailH, email, kind, isMale, age, dialect, device, projid);
  }*/

  /**
   * @param user
   * @return
   * @seex mitll.langtest.server.database.ImportCourseExamples#copyUser
   */
/*  public int addUser(User user) {
    return userManagement.addUser(user);
  }*/

  /**
   * @param out
   * @see mitll.langtest.server.DownloadServlet#returnSpreadsheet
   */
  public void usersToXLSX(OutputStream out) {
    userManagement.usersToXLSX(out);
  }

  public JSON usersToJSON() {
    return userManagement.usersToJSON();
  }

  /**
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   *
   * @return
   * @see mitll.langtest.server.services.UserServiceImpl#getUsers()
   */
  public List<User> getUsers() {
    return userManagement.getUsers();
  }

  public void logEvent(String exid, String context, int userid, String device) {
    if (context.length() > 100) context = context.substring(0, 100).replace("\n", " ");
    logEvent(UNKNOWN, "server", exid, context, userid, device);
  }

  /**
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param device
   * @return
   * @see mitll.langtest.server.ScoreServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public boolean logEvent(String id, String widgetType, String exid, String context, int userid, String device) {
    if (userid == -1) {
      userid = userDAO.getBeforeLoginUser();
    }

    IProjectDAO projectDAO = getProjectDAO();
    SlickProject next = userid != -1 ? projectDAO.mostRecentByUser(userid) : projectDAO.getFirst();
    if (userid == -1) {
      logger.warn("logEvent userid : " + userid + " using the first project! " + next);
    }
    if (next == null) {
      next = projectDAO.getFirst();
    }
    if (next == null) {
      logger.error("no projects???");
      return false;
    } else {
      Event event = new Event(id, widgetType, exid, context, userid, System.currentTimeMillis(), device, -1);
      return eventDAO != null && eventDAO.add(event, next.id());
    }
  }

  public void logAndNotify(Exception e) {
    getLogAndNotify().logAndNotifyServerException(e);
  }

  public IEventDAO getEventDAO() {
    return eventDAO;
  }

  public IAudioDAO getAudioDAO() {
    return audioDAO;
  }

  public IWordDAO getWordDAO() {
    return wordDAO;
  }

  public IPhoneDAO getPhoneDAO() {
    return phoneDAO;
  }

  /**
   * @see #initializeDAOs(PathHelper)
   */
  public void createTables() {
    //  logger.info("createTables create slick tables - has " + dbConnection.getTables());

    List<IDAO> idaos = Arrays.asList(
        getUserDAO(),
        getProjectDAO(),
        userExerciseDAO,
        ((SlickUserExerciseDAO) userExerciseDAO).getRelatedExercise(),
        getAudioDAO(),
        getEventDAO(),
        getResultDAO(),
        getAnnotationDAO(),
        getWordDAO(),
        getPhoneDAO(),
        getRefResultDAO(),
        getReviewedDAO(),
        getSecondStateDAO(),
        ((ProjectDAO) getProjectDAO()).getProjectPropertyDAO(),
        getUserProjectDAO(),
        userSessionDAO
    );

    List<String> created = new ArrayList<>();
    for (IDAO dao : idaos) {
      createIfNotThere(dao, created);
    }

    userListManager.createTables(dbConnection, created);
    if (!created.isEmpty()) {
      logger.info("createTables created slick tables : " + created);
      logger.info("createTables after create slick tables - has " + dbConnection.getTables());
    }

    dbConnection.addColumn();
  }

  private void createIfNotThere(IDAO slickUserDAO, List<String> created) {
    String name = slickUserDAO.getName();
    if (!dbConnection.hasTable(name)) {
      logger.info("createIfNotThere create " + name);
      slickUserDAO.createTable();
      created.add(name);
    } else {
      //   logger.debug("createIfNotThere has table " + name);
    }
  }

//  public void copyOneConfig(String cc, String optName) {
//    new CopyToPostgres().copyOneConfig(this, cc, optName);
//  }

  /**
   * @param projid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultAlternatives
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults
   */
  public Collection<MonitorResult> getMonitorResults(int projid) {
    List<MonitorResult> monitorResults = resultDAO.getMonitorResults(projid);

    for (MonitorResult result : monitorResults) {
      int exID = result.getExID();
      CommonShell exercise = isAmas() ? getAMASExercise(exID) : getExercise(projid, exID);
      if (exercise != null) {
        result.setDisplayID("" + exercise.getDominoID());
      }
    }
    return getMonitorResultsWithText(monitorResults);
  }

  /**
   * @param monitorResults
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults
   * @see #getMonitorResults(int)
   */
  public List<MonitorResult> getMonitorResultsWithText(List<MonitorResult> monitorResults) {
    addUnitAndChapterToResults(monitorResults, getIdToExerciseMap(1));
    return monitorResults;
  }

  /**
   * Add info from exercises.
   *
   * @param monitorResults
   * @param join
   * @see DatabaseImpl#getMonitorResultsWithText
   */
  private void addUnitAndChapterToResults(Collection<MonitorResult> monitorResults,
                                          Map<Integer, CommonExercise> join) {
    int n = 0;
    Set<Integer> unknownIDs = new HashSet<>();
    for (MonitorResult result : monitorResults) {
      int id = result.getExID();
      // if (id.contains("\\/")) id = id.substring(0, id.length() - 2);
      CommonExercise exercise = join.get(id);
      if (exercise == null) {
        if (n < 5) {
          logger.error("addUnitAndChapterToResults : for exid " + id + " couldn't find " + result);
        }
        unknownIDs.add(id);
        n++;
        result.setUnitToValue(Collections.emptyMap());
        result.setForeignText("");
      } else {
        result.setUnitToValue(exercise.getUnitToValue());
        result.setForeignText(exercise.getForeignLanguage());
      }
    }
    if (n > 0) {
      logger.warn("addUnitAndChapterToResults : skipped " + n + " out of " + monitorResults.size() +
          " # bad join ids = " + unknownIDs.size());
    }
  }

  /**
   * @param projectid
   * @return
   * @see #getMonitorResultsWithText(List)
   */
  private Map<Integer, CommonExercise> getIdToExerciseMap(int projectid) {
    Map<Integer, CommonExercise> join = new HashMap<>();

    for (CommonExercise exercise : getExercises(projectid)) {
      join.put(exercise.getID(), exercise);
    }

    // TODO : why would we want to do this?
    if (userExerciseDAO != null && getExerciseDAO(projectid) != null) {
      for (CommonExercise exercise : userExerciseDAO.getAllUserExercises(projectid)) {
        join.put(exercise.getID(), exercise);
      }
    }

    return join;
  }

  /**
   * @param projectid
   * @return
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceForUser
   * @see DatabaseImpl#configureProject(String, Project)
   */
  public Map<Integer, String> getExerciseIDToRefAudio(int projectid) {
    logger.info("getExerciseIDToRefAudio for " + projectid);

    Map<Integer, String> join = new HashMap<>();
    populateIDToRefAudio(join, getExercises(projectid));

    Collection<CommonExercise> all = userExerciseDAO.getAllUserExercises(projectid);
    getExerciseDAO(projectid).attachAudio(all);
    populateIDToRefAudio(join, all);
    return join;
  }

  private <T extends Shell & AudioAttributeExercise> void populateIDToRefAudio(Map<Integer, String> join,
                                                                               Collection<CommonExercise> all) {
    for (CommonExercise exercise : all) {
      String refAudio = exercise.getRefAudio();
      if (refAudio == null) {
        //   logger.warn("getExerciseIDToRefAudio huh? user exercise : no ref audio for " +id);
      } else {
        int id = exercise.getID();
        join.put(id, refAudio);
      }
    }
    if (join.isEmpty()) logger.warn("huh? no ref audio on " + all.size() + " exercises???");
  }

  public IAnswerDAO getAnswerDAO() {
    return answerDAO;
  }

  /**
   * @param userID
   * @param exerciseID
   * @param audioFile
   * @param durationInMillis
   * @param correct
   * @param isMale
   * @param speed
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#getRefAudioAnswerDecoding
   */
  public long addRefAnswer(int userID, int exerciseID,
                           String audioFile,
                           long durationInMillis, boolean correct,
                           DecodeAlignOutput alignOutput,
                           DecodeAlignOutput decodeOutput,

                           DecodeAlignOutput alignOutputOld,
                           DecodeAlignOutput decodeOutputOld,

                           boolean isMale, String speed) {
    return refresultDAO.addAnswer(userID, exerciseID, audioFile, durationInMillis, correct,
        alignOutput, decodeOutput,
        alignOutputOld, decodeOutputOld,
        isMale, speed);
  }

/*  public int userExists(String login) {
    return userDAO.getIdForUserID(login);
  }*/

  /**
   * @see LangTestDatabaseImpl#destroy()
   */
  public void destroy() {
    try {
      if (connection != null) {
        connection.contextDestroyed();
      }
      logger.info("closing db connection : " + dbConnection);
      dbConnection.close();
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public IUserListManager getUserListManager() {
    return userListManager;
  }

  /**
   * @param exercise
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#duplicateExercise
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise
   */
  public CommonExercise duplicateExercise(CommonExercise exercise) {
    logger.debug("to duplicate  " + exercise);

    //logger.debug("anno before " + exercise.getFieldToAnnotation());
    CommonExercise duplicate = getUserListManager().duplicate(exercise);

    if (!exercise.isPredefined()) {
      logger.warn("huh? got non-predef " + exercise);
    }

    SectionHelper sectionHelper = getSectionHelper(exercise.getProjectID());

    List<SectionHelper.Pair> pairs = new ArrayList<>();
    for (Map.Entry<String, String> pair : exercise.getUnitToValue().entrySet()) {
      pairs.add(sectionHelper.addExerciseToLesson(duplicate, pair.getKey(), pair.getValue()));
    }
    sectionHelper.addAssociations(pairs);

    AddRemoveDAO addRemoveDAO = getAddRemoveDAO();
    if (addRemoveDAO != null) {
      logger.warn("call domino to add the new item");
      //addRemoveDAO.add(duplicate.getOldID(), AddRemoveDAO.ADD);
    } else {
      logger.warn("add remove not implemented yet!");
    }
    getExerciseDAO(exercise.getProjectID()).add(duplicate);

    logger.debug("exercise state " + exercise.getState());

    userListManager.setState(duplicate, exercise.getState(), exercise.getCreator());

    logger.debug("duplicate after " + duplicate);

    return duplicate;
  }

  /**
   * @param exid
   * @param projectid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#deleteItem
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#deleteItem
   */
  @Deprecated
  public boolean deleteItem(int exid, int projectid) {
    AddRemoveDAO addRemoveDAO = getAddRemoveDAO();
    if (addRemoveDAO != null) {
      // addRemoveDAO.add(exid, AddRemoveDAO.REMOVE);
    } else {
      logger.warn("add remove not implemented yet!");
    }
    getUserListManager().removeReviewed(exid);
    getSectionHelper(projectid).removeExercise(getExercise(projectid, exid));
    return getExerciseDAO(projectid).remove(exid);
  }

  int warns = 0;

  /**
   * TODO : Fix this to get the right project first!
   * <p>
   * allow custom items to mask out non-custom items
   * Special code to mask out unit/chapter from database in userexercise table.
   * <p>
   * Must check update times to make sure we don't mask out a newer entry.
   *
   * @param projid
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercise
   */
  public CommonExercise getCustomOrPredefExercise(int projid, int id) {
/*    CommonExercise userEx = getUserExerciseByExID(id);  // allow custom items to mask out non-custom items

    CommonExercise toRet;

    if (userEx == null) {
      toRet = getExercise(id);
    } else {
      //logger.info("got user ex for " + id);
      long updateTime = userEx.getUpdateTime();
      CommonExercise predef = getExercise(id);

      boolean usePredef = predef != null && predef.getUpdateTime() > updateTime;
      toRet = usePredef ? predef : userEx;

      if (predef != null && !usePredef) {
        // DON'T use the unit/chapter from database, at least for now
        userEx.getCombinedMutableUserExercise().setUnitToValue(predef.getUnitToValue());
      }
    }*/

    CommonExercise toRet = getExercise(projid, id);
    if (toRet == null) {
      if (warns++ < 50)
        logger.info("couldn't find exercise " + id + " in project #" + projid + " looking in user exercise table");
      //    toRet = getUserExerciseByExID(id);
    }

    return toRet;
  }

  /**
   * Ask the database for the user exercise.
   *
   * @param id
   * @return
   * @see #editItem
   * @see #getCustomOrPredefExercise(int, int)
   */
  private CommonExercise getUserExerciseByExID(int id) {
    return userExerciseDAO.getByExID(id);
  }

  @Override
  public ServerProperties getServerProps() {
    return serverProps;
  }

  private AddRemoveDAO getAddRemoveDAO() {
    return null;//addRemoveDAO;
  }

  /**
   * @param out
   * @param typeToSection
   * @param projectid
   * @throws Exception
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeZip(OutputStream out, Map<String, Collection<String>> typeToSection, int projectid, AudioExport.AudioExportOptions options) throws Exception {
    Collection<CommonExercise> exercisesForSelectionState = typeToSection.isEmpty() ?
        getExercises(projectid) :
        getSectionHelper(projectid).getExercisesForSelectionState(typeToSection);
    String language = getLanguage(projectid);
    new AudioExport(getServerProps()).writeZip(out,
        typeToSection,
        getSectionHelper(projectid),
        exercisesForSelectionState,
        language,
        getAudioDAO(), installPath, configDir, false, options);
  }

  public String getLanguage(CommonExercise ex) {
    return getLanguage(ex.getProjectID());
  }

  public String getLanguage(int projectid) {
    return getProject(projectid).getLanguage();
  }

  @Override
  @Deprecated
  public String getLanguage() {
    return getOldLanguage(getServerProps());
  }

  /**
   * @param out
   * @throws Exception
   * @see DownloadServlet#writeAllAudio
   */
  public void writeUserListAudio(OutputStream out, int projectid) throws Exception {
    new AudioExport(getServerProps()).writeZipJustOneAudio(out, getSectionHelper(projectid), getExercises(projectid), installPath);
  }

  /**
   * For downloading a user list.
   *
   * @param out
   * @param listid
   * @param projectid
   * @param options
   * @return
   * @throws Exception
   * @see mitll.langtest.server.DownloadServlet#writeUserList
   */
  public String writeUserListAudio(OutputStream out,
                                   long listid,
                                   int projectid,
                                   AudioExport.AudioExportOptions options) throws Exception {
    String language = getLanguage(projectid);
    if (listid == -1) return language + "_Unknown";

    UserList<CommonShell> userListByID = getUserListByID(listid, projectid);

    if (userListByID == null) {
      logger.error("huh? can't find user list " + listid);
      return language + "_Unknown";
    } else {
      //logger.debug("writing contents of " + userListByID);
      long then = System.currentTimeMillis();
      List<CommonExercise> copyAsExercises = new ArrayList<>();

      for (CommonShell ex : userListByID.getExercises()) {
        copyAsExercises.add(getCustomOrPredefExercise(projectid, ex.getID()));
      }
      for (CommonExercise ex : copyAsExercises) {
        userListManager.addAnnotations(ex);
        getAudioDAO().attachAudioToExercise(ex, language);
      }
      long now = System.currentTimeMillis();
      logger.debug("\nTook " + (now - then) + " millis to annotate and attach.");
      new AudioExport(getServerProps()).writeUserListAudio(out, userListByID.getName(), getSectionHelper(projectid),
          copyAsExercises, language,
          getAudioDAO(), installPath, configDir, listid == IUserListManager.REVIEW_MAGIC_ID, options);
    }
    return language + "_" + userListByID.getName();
  }

  /**
   * JUST FOR TESTING
   *
   * @param ex
   * @return
   */
  public int attachAudio(CommonExercise ex) {
    return getAudioDAO().attachAudioToExercise(ex, /*installPath, configDir,*/ getLanguage(ex));
  }


  /**
   * Expensive ?
   *
   * @param projectid
   * @see ScoreServlet#getJSONExport
   */
  public void attachAllAudio(int projectid) {
    IAudioDAO audioDAO = getAudioDAO();
    Project project = getProject(projectid);
    Map<Integer, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio(projectid);

    long then = System.currentTimeMillis();
    Collection<CommonExercise> exercises = getExercises(projectid);
    for (CommonExercise exercise : exercises) {
      List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(exercise, /*installPath, configDir,*/ audioAttributes, project.getLanguage());
      }
      //if (!debug) ensureMP3s(exercise);
      // exercises.add(getJsonForExercise(exercise));
    }
    long now = System.currentTimeMillis();
    logger.info(getLanguage() + " took " + (now - then) + " millis to attachAllAudio to " + exercises.size() + " exercises");
  }

  public String getUserListName(long listid, int projectid) {
    UserList userListByID = getUserListByID(listid, projectid);
    String language1 = getLanguage(projectid);
    if (userListByID == null) {
      logger.error("huh? can't find user list " + listid);
      return language1 + "_Unknown";
    } else {
      return language1 + "_" + userListByID.getName();
    }
  }

  public UserList<CommonShell> getUserListByID(long listid, int projectid) {
    return getUserListManager().getUserListByID(listid, getSectionHelper(projectid).getTypeOrder());
  }

  public String getPrefix(Map<String, Collection<String>> typeToSection, int projectid) {
    return new AudioExport(getServerProps()).getPrefix(getSectionHelper(projectid), typeToSection);
  }

  /**
   * TODO : fix this to do all projects
   *
   * @param serverProps
   * @param site
   * @param mailSupport
   * @param pathHelper
   * @see mitll.langtest.server.LangTestDatabaseImpl#init
   */
  public void doReport(ServerProperties serverProps, String site, MailSupport mailSupport, PathHelper pathHelper) {
    getReport("").doReport(serverProps, site, mailSupport, pathHelper);
  }

  /**
   * @param year
   * @param jsonObject
   * @return
   * @see mitll.langtest.server.ScoreServlet#getReport(JSONObject, int)
   */
  public String getReport(int year, JSONObject jsonObject) {
    //  return getReport("").getReport(serverProps.getLanguage(), jsonObject, year);
    return getReport("").getAllReports(getProjectDAO().getAll(), jsonObject, year);
  }

  private Report reportCache;

  private Report getReport(String prefix) {
    if (reportCache == null) {
      Report report = new Report(userDAO, resultDAO, eventDAO, audioDAO, getOldLanguage(serverProps), prefix);
      this.reportCache = report;
    }
    return reportCache;
  }

  /**
   * FOR TESTING
   *
   * @param pathHelper
   * @return
   * @deprecated
   */
  public JSONObject doReport(PathHelper pathHelper) {
    return doReport(pathHelper, "", -1);
  }

  /**
   * JUST FOR TESTING
   *
   * @param pathHelper
   * @param prefix
   * @deprecated JUST FOR TESTING
   */
  public JSONObject doReport(PathHelper pathHelper, String prefix, int year) {
    try {
      Report report = getReport(prefix);
      return report.writeReportToFile(pathHelper, getOldLanguage(serverProps), year);
    } catch (IOException e) {
      logger.error("got " + e);
      return null;
    }
  }

  /**
   * @param resultID
   * @param asrScoreForAudio
   * @see LangTestDatabaseImpl#getPretestScore
   */
  public void rememberScore(int resultID, PretestScore asrScoreForAudio) {
    getAnswerDAO().changeAnswer(resultID, asrScoreForAudio.getHydecScore(), asrScoreForAudio.getProcessDur(), asrScoreForAudio.getJson());
    recordWordAndPhoneInfo(resultID, asrScoreForAudio);
  }

  /**
   * @param answer
   * @param answerID
   * @see mitll.langtest.server.audio.AudioFileHelper#recordInResults(AudioContext, AnswerInfo.RecordingInfo, AudioCheck.ValidityAndDur, AudioAnswer)
   */
  public void recordWordAndPhoneInfo(AudioAnswer answer, long answerID) {
    PretestScore pretestScore = answer.getPretestScore();
    if (pretestScore == null) {
      logger.debug(" : recordWordAndPhoneInfo pretest score is null for " + answer + " and result id " + answerID);
    } else {
      logger.debug(" : recordWordAndPhoneInfo pretest score is " + pretestScore + " for " + answer + " and result id " + answerID);
    }
    recordWordAndPhoneInfo(answerID, pretestScore);
  }

  /**
   * @param answerID
   * @param pretestScore
   * @see #rememberScore(int, PretestScore)
   */
  private void recordWordAndPhoneInfo(long answerID, PretestScore pretestScore) {
    if (pretestScore != null) {
      recordWordAndPhoneInfo(answerID, pretestScore.getsTypeToEndTimes());
    }
  }

  /**
   * @param answerID
   * @param netPronImageTypeListMap
   * @seex #putBackWordAndPhone
   * @see #recordWordAndPhoneInfo(long, PretestScore)
   */
  private void recordWordAndPhoneInfo(long answerID, Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    List<TranscriptSegment> words = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT);
    List<TranscriptSegment> phones = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (words != null) {
      int windex = 0;
      int pindex = 0;

      for (TranscriptSegment segment : words) {
        String event = segment.getEvent();
        if (!event.equals(SLFFile.UNKNOWN_MODEL) && !event.equals(SIL)) {
          long wid = getWordDAO().addWord(new Word(answerID, event, windex++, segment.getScore()));
          for (TranscriptSegment pseg : phones) {
            if (pseg.getStart() >= segment.getStart() && pseg.getEnd() <= segment.getEnd()) {
              String pevent = pseg.getEvent();
              if (!pevent.equals(SLFFile.UNKNOWN_MODEL) && !pevent.equals(SIL)) {
                getPhoneDAO().addPhone(new Phone(answerID, wid, pevent, pindex++, pseg.getScore()));
              }
            }
          }
        }
      }
    }
  }

  /**
   * @param projectid
   * @return
   * @see LangTestDatabaseImpl#getMaleFemaleProgress()
   */
  public Map<String, Float> getMaleFemaleProgress(int projectid) {
    IUserDAO userDAO = getUserDAO();
    Map<Integer, User> userMapMales = userDAO.getUserMap(true);
    Map<Integer, User> userMapFemales = userDAO.getUserMap(false);

    Collection<CommonExercise> exercises = getExercises(projectid);
    float total = exercises.size();
    Set<Integer> uniqueIDs = new HashSet<>();

    int context = 0;
    for (CommonExercise shell : exercises) {
      if (shell.hasContext()) context++;
      boolean add = uniqueIDs.add(shell.getID());
      if (!add) {
        logger.warn("getMaleFemaleProgress found duplicate id " + shell.getID() + " : " + shell);
      }
    }
    logger.info("getMaleFemaleProgress found " + total + " total exercises, " +
        uniqueIDs.size() +
        " unique" +
        " males " + userMapMales.size() + " females " + userMapFemales.size());

    return getAudioDAO().getRecordedReport(userMapMales, userMapFemales, total, uniqueIDs, context);
  }

  /**
   * @param projectid
   * @return
   * @see LangTestDatabaseImpl#getMaleFemaleProgress()
   */
  public Map<String, Float> getH2MaleFemaleProgress(int projectid) {
    IUserDAO userDAO = getUserDAO();
    Map<Integer, User> userMapMales = userDAO.getUserMap(true);
    Map<Integer, User> userMapFemales = userDAO.getUserMap(false);

    Collection<CommonExercise> exercises = getExercises(projectid);
    float total = exercises.size();
    Set<Integer> uniqueIDs = new HashSet<>();

    int context = 0;
    for (CommonExercise shell : exercises) {
      if (shell.hasContext()) context++;
      boolean add = uniqueIDs.add(shell.getID());
      if (!add) {
        logger.warn("getMaleFemaleProgress found duplicate id " + shell.getID() + " : " + shell);
      }
    }
    logger.info("getH2MaleFemaleProgress found " + total + " total exercises, " +
        uniqueIDs.size() +
        " unique" +
        " males " + userMapMales.size() + " females " + userMapFemales.size());

    return new AudioDAO(this, new UserDAO(this)).getRecordedReport(userMapMales, userMapFemales, total, uniqueIDs, context);
  }

  public String toString() {
    return "Database : " + this.getClass().toString();
  }

  @Override
  public LogAndNotify getLogAndNotify() {
    return logAndNotify;
  }

  public IUserExerciseDAO getUserExerciseDAO() {
    return userExerciseDAO;
  }

  public IAnnotationDAO getAnnotationDAO() {
    return userListManager.getAnnotationDAO();
  }

  public IReviewedDAO getReviewedDAO() {
    return userListManager.getReviewedDAO();
  }

  public IReviewedDAO getSecondStateDAO() {
    return userListManager.getSecondStateDAO();
  }

  public IProjectDAO getProjectDAO() {
    return projectDAO;
  }

  public IUserProjectDAO getUserProjectDAO() {
    return userProjectDAO;
  }

  public mitll.langtest.server.database.user.UserManagement getUserManagement() {
    return userManagement;
  }

  public IUserSessionDAO getUserSessionDAO() {
    return userSessionDAO;
  }
}
