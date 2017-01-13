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

import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.server.*;
import mitll.langtest.server.amas.FileExerciseDAO;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.annotation.IAnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.audio.AudioDAO;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.audio.SlickAudioDAO;
import mitll.langtest.server.database.connection.DatabaseConnection;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.connection.PostgreSQLConnection;
import mitll.langtest.server.database.contextPractice.ContextPracticeImport;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.dliclass.DLIClassDAO;
import mitll.langtest.server.database.dliclass.DLIClassJoinDAO;
import mitll.langtest.server.database.dliclass.IDLIClassDAO;
import mitll.langtest.server.database.dliclass.IDLIClassJoinDAO;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.server.database.instrumentation.SlickEventImpl;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.phone.Phone;
import mitll.langtest.server.database.phone.RecordWordAndPhone;
import mitll.langtest.server.database.phone.SlickPhoneDAO;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.project.ProjectDAO;
import mitll.langtest.server.database.project.ProjectManagement;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.database.refaudio.SlickRefResultDAO;
import mitll.langtest.server.database.result.*;
import mitll.langtest.server.database.reviewed.IReviewedDAO;
import mitll.langtest.server.database.reviewed.SlickReviewedDAO;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.security.UserSecurityManager;
import mitll.langtest.server.database.user.*;
import mitll.langtest.server.database.userexercise.ExerciseToPhone;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.SlickUserListDAO;
import mitll.langtest.server.database.userlist.SlickUserListExerciseJoinDAO;
import mitll.langtest.server.database.userlist.SlickUserListExerciseVisitorDAO;
import mitll.langtest.server.database.word.IWordDAO;
import mitll.langtest.server.database.word.SlickWordDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.services.UserServiceImpl;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.ContextPractice;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
  private static final Logger logger = LogManager.getLogger(DatabaseImpl.class);
  private static final int LOG_THRESHOLD = 10;
  private static final String UNKNOWN = "unknown";
  public static final int IMPORT_PROJECT_ID = -100;
  private static final boolean ADD_DEFECTS = false;

  /**
   * @see #getContextPractice
   */
  private String installPath;

  private IUserDAO userDAO;
  private IUserPermissionDAO userPermissionDAO;
  private IUserSessionDAO userSessionDAO;
  private IResultDAO resultDAO;

  private IRefResultDAO refresultDAO;
  private IWordDAO wordDAO;
  private IPhoneDAO<Phone> phoneDAO;

  private IAudioDAO audioDAO;
  private IAnswerDAO answerDAO;
  private IUserListManager userListManager;

  private IUserExerciseDAO userExerciseDAO;
  // private AddRemoveDAO addRemoveDAO;

  private IEventDAO eventDAO;
  private IProjectDAO projectDAO;
  private IUserProjectDAO userProjectDAO;
  private IDLIClassDAO dliClassDAO;
  private IDLIClassJoinDAO dliClassJoinDAO;

  private ContextPractice contextPractice;

  /**
   * Only for h2
   */
  @Deprecated
  private DatabaseConnection connection = null;
  private MonitoringSupport monitoringSupport;

  private final ServerProperties serverProps;
  private final LogAndNotify logAndNotify;

  private mitll.langtest.server.database.user.UserManagement userManagement = null;

  /**
   * @see #writeUserListAudio(OutputStream, long, int, AudioExport.AudioExportOptions)
   */
 // private final String configDir;
  /**
   * Only for AMAS.
   *
   * @see #readAMASExercises(String, String, String, boolean)
   */
  private final String absConfigDir;
  private SimpleExerciseDAO<AmasExerciseImpl> fileExerciseDAO;
  private PathHelper pathHelper;
  private IProjectManagement projectManagement;
  private RecordWordAndPhone recordWordAndPhone;
  private IInviteDAO inviteDAO;
  private IUserSecurityManager userSecurityManager;

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
                      PathHelper pathHelper, boolean mustAlreadyExist, LogAndNotify logAndNotify, boolean readOnly/*,
                      ServletContext servletContext*/) {
    this(serverProps.useH2() ?
            new H2Connection(configDir, dbName, mustAlreadyExist, logAndNotify, readOnly) :
            serverProps.usePostgres() ?
                new PostgreSQLConnection(dbName, logAndNotify) :
                null,
        configDir, relativeConfigDir, dbName,
        serverProps,
        pathHelper, logAndNotify/*,servletContext*/);
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
   // this.configDir = relativeConfigDir;
    this.serverProps = serverProps;
    this.logAndNotify = logAndNotify;
    this.pathHelper = pathHelper;

    if (maybeGetH2Connection(relativeConfigDir, dbName, serverProps)) return;
    then = System.currentTimeMillis();
    initializeDAOs(pathHelper);
    now = System.currentTimeMillis();
    if (now - then > 300) {
      logger.info("took " + (now - then) + " millis to initialize DAOs for " + getOldLanguage(serverProps));
    }

    monitoringSupport = new MonitoringSupport(userDAO, resultDAO);
    this.pathHelper = pathHelper;
//    if (!serverProps.useH2()) {
//      populateProjects(false);
//    }
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
   * @seex CopyToPostgres#createProjectIfNotExists
   * @see DatabaseImpl#makeDAO
   * @see DatabaseImpl#DatabaseImpl
   * @see LangTestDatabaseImpl#init
   */
  public void populateProjects() {
    if (projectManagement == null) {
      logger.info("populateProjects no project management yet...");
    } else {
      logger.info("populateProjects --- ");
      projectManagement.populateProjects();
    }
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
    //   SlickUserDAOImpl slickUserDAO = new SlickUserDAOImpl(this, dbConnection);
    this.userDAO = new DominoUserDAOImpl(this);
    userPermissionDAO = new SlickUserPermissionDAOImpl(this, dbConnection);
    //  slickUserDAO.setPermissionDAO(userPermissionDAO);

    this.userSessionDAO = new SlickUserSessionDAOImpl(this, dbConnection);
    this.inviteDAO = new SlickInviteDAOImpl(this, dbConnection);
    audioDAO = new SlickAudioDAO(this, dbConnection, this.userDAO);
    resultDAO = new SlickResultDAO(this, dbConnection);
    answerDAO = new SlickAnswerDAO(this, dbConnection);
//    addRemoveDAO = new AddRemoveDAO(this);

    refresultDAO = new SlickRefResultDAO(this, dbConnection, serverProps.shouldDropRefResult());
    userExerciseDAO = new SlickUserExerciseDAO(this, dbConnection);
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
    dliClassDAO = new DLIClassDAO(dbConnection);
    dliClassJoinDAO = new DLIClassJoinDAO(dbConnection);

    createTables();

    userDAO.ensureDefaultUsers();
    int defaultProject = projectDAO.ensureDefaultProject(userDAO.getBeforeLoginUser());
    // make sure we have a template exercise
    userExerciseDAO.ensureTemplateExercise(defaultProject);

    try {
      ((UserListManager) userListManager).setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    recordWordAndPhone = new RecordWordAndPhone(wordDAO, phoneDAO);
//    long now = System.currentTimeMillis();
//    if (now - then > 1000) logger.info("took " + (now - then) + " millis to put back word and phone");
  }

  /**
   * @return
   * @see #initializeDAOs
   */
  private DBConnection getDbConnection() {
    DBConnection dbConnection = new DBConnection(serverProps.getDBConfig());
    logger.info("getDbConnection using " + serverProps.getDBConfig() + " : " + dbConnection);
    return dbConnection;
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
  @Deprecated
  public void closeConnection() throws SQLException {
  }

  public MonitoringSupport getMonitoringSupport() {
    return monitoringSupport;
  }

  /**
   * @param installPath
   * @param lessonPlanFile
   * @see mitll.langtest.server.LangTestDatabaseImpl#setInstallPath
   */
  public void setInstallPath(String installPath, String lessonPlanFile) {
    logger.debug("setInstallPath got install path " + installPath);// + " media " + mediaDir);
    this.installPath = installPath;
    this.projectManagement = new ProjectManagement(pathHelper, serverProps, getLogAndNotify(), this);
    makeDAO(lessonPlanFile);
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
    Collection<String> strings = (sectionHelper == null) ? objects : sectionHelper.getTypeOrder();
//    logger.info("getTypeOrder : " + projectid + " = " + strings);
    return strings;
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#getResultASRInfo
   * @see mitll.langtest.server.DownloadServlet#getFilenameForDownload
   * @see #deleteItem(int, int)
   * @see #getCustomOrPredefExercise(int, int)
   */
  public CommonExercise getExercise(int projectid, int id) {
    return projectManagement.getExercise(projectid, id);
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
   * exercises are in the context of a project
   *
   * @param projectid
   * @return
   * @see #getExercises(int)
   * @see Project#buildExerciseTrie
   */
  public Collection<CommonExercise> getExercises(int projectid) {
    return projectManagement.getExercises(projectid);
  }

  public ExerciseDAO<CommonExercise> getExerciseDAO(int projectid) {
    Project project = getProject(projectid);
    //logger.debug("getExerciseDAO " + projectid + " found project " + project);
    ExerciseDAO<CommonExercise> exerciseDAO = project.getExerciseDAO();
    //logger.debug("getExerciseDAO " + projectid + " found exercise dao " + exerciseDAO);
    return exerciseDAO;
  }

  public Project getProjectForUser(int userid) {
    return projectManagement.getProjectForUser(userid);
  }

  public void stopDecode() {
    projectManagement.stopDecode();
  }

  /**
   * Just for import
   *
   * @param projectid
   */
  public void rememberProject(int projectid) {
    projectManagement.rememberProject(projectid);
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
   * @see mitll.langtest.server.services.UserServiceImpl#setSessionUser
   * @see UserServiceImpl#getUserFromSession
   */
  public void setStartupInfo(User userWhere) {
    setStartupInfo(userWhere, getUserProjectDAO().mostRecentByUser(userWhere.getID()));
  }

  /**
   * @param userWhere
   * @param projid
   * @see #setStartupInfo(User)
   * @see mitll.langtest.server.services.UserServiceImpl#setProject(int)
   */
  public void setStartupInfo(User userWhere, int projid) {
    //  logger.info("setStartupInfo on " + userWhere + " for project " + projid);
    projectManagement.setStartupInfo(userWhere, projid);
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
   * TODO : make sure this works for AMAS?
   * <p>
   * Lazy, latchy instantiation of DAOs.
   * Not sure why it really has to be this way.
   * <p>
   * Special check for amas exercises...
   *
   * @param lessonPlanFile only for import
   * @see #setInstallPath(String, String)
   */
  private void makeDAO(String lessonPlanFile) {
    // logger.info("makeDAO - " + lessonPlanFile);
    if (userManagement == null) {
      synchronized (this) {
        boolean isURL = serverProps.getLessonPlan().startsWith("http");
        boolean amas = isAmas();
        int numExercises;

        if (amas) {
//          logger.info("Got " + lessonPlanFile);
          // TODO : get media directory from properties
          // TODO : get install path directory from properties
          numExercises = readAMASExercises(lessonPlanFile, "", "", isURL);
        } else {
          logger.info("makeDAO makeExerciseDAO -- " + lessonPlanFile);

          makeExerciseDAO(lessonPlanFile, isURL);

          if (!serverProps.useH2()) {
            //        userExerciseDAO.setExToPhones(new ExerciseToPhone().getExerciseToPhone(refresultDAO));
            populateProjects();
            //    logger.info("set exercise dao " + exerciseDAO + " on " + userExerciseDAO);
            if (projectManagement.getProjects().isEmpty()) {
              logger.warn("\n\n\nmakeDAO no projects loaded yet...?");
            } else {
              ExerciseDAO<CommonExercise> exerciseDAO = projectManagement.getFirstProject().getExerciseDAO();
              logger.info("using exercise dao from first project " + exerciseDAO);
              userExerciseDAO.setExerciseDAO(exerciseDAO);
            }
          }

          // TODO
          // TODO : will this break import???
          // TODO

          if (serverProps.useH2()) {
            userExerciseDAO.setExerciseDAO(projectManagement.setDependencies());
          } else {
            configureProjects();
          }
        }
        userManagement = new mitll.langtest.server.database.user.UserManagement(userDAO, resultDAO, userPermissionDAO);
      }
    }
  }

  /**
   * Why a separate, later step???
   *
   * @see #makeDAO
   */
  private void configureProjects() {
    // TODO : this seems like a bad idea --
    userExerciseDAO.setExToPhones(new ExerciseToPhone().getExerciseToPhone(refresultDAO));
    projectManagement.configureProjects();
  }

  /**
   * Here to support import from old individual sites for CopyToPostgres
   *
   * @param lessonPlanFile
   * @param isURL          deprecated
   * @see #makeDAO(String)
   */
  private void makeExerciseDAO(String lessonPlanFile, boolean isURL) {
    logger.info("makeExerciseDAO - " + lessonPlanFile + " : use h2 = " + serverProps.useH2());

    if (isURL) {
      projectManagement.addSingleProject(new JSONURLExerciseDAO(getServerProps(), userListManager, ADD_DEFECTS));
    } else if (!serverProps.useH2()) {
//      projectManagement.setExerciseDAOs();
/*
    } else if (lessonPlanFile.endsWith(".json")) {
      logger.info("got " + lessonPlanFile);
      JSONExerciseDAO jsonExerciseDAO = new JSONExerciseDAO(lessonPlanFile, getServerProps(), userListManager, ADD_DEFECTS);
      projectManagement.addSingleProject(jsonExerciseDAO);
      */
    } else {
      logger.info("makeExerciseDAO reading from excel sheet " + lessonPlanFile);
      projectManagement.addSingleProject(new ExcelImport(lessonPlanFile, getServerProps(), userListManager, ADD_DEFECTS));
    }
  }

  /**
   *
   * @param projectid
   * @return
   */
  public Project getProject(int projectid) {
    if (projectid == -1) {
      logger.warn("getProject asking for project -1?", new Exception());
    }
    return projectManagement.getProject(projectid);
  }

  /**
   * A little dusty...
   *
   * @param lessonPlanFile
   * @param mediaDir
   * @param installPath
   * @param isURL
   * @return
   */
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
   * Dialog practice
   *
   * @param contextPracticeFile
   * @param installPath
   */
  private void makeContextPractice(String contextPracticeFile, String installPath) {
    if (contextPractice == null && contextPracticeFile != null) {
      synchronized (this) {
        this.contextPractice = new ContextPracticeImport(installPath + File.separator + contextPracticeFile).getContextPractice();
      }
    }
  }

  /**
   * TODO : why is this so confusing???
   *
   * @param userExercise
   * @param keepAudio
   * @seex mitll.langtest.server.LangTestDatabaseImpl#editItem
   * @see mitll.langtest.server.services.ListServiceImpl#editItem
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  public void editItem(CommonExercise userExercise, boolean keepAudio) {
    int id = userExercise.getID();
    logger.debug("editItem exercise #" + id +
        " keep audio " + keepAudio +
        " mediaDir : " + getServerProps().getMediaDir() +
        " initially audio was\n\t " + userExercise.getAudioAttributes());

    getUserListManager().editItem(userExercise,
        true, // create if doesn't exist
        getServerProps().getMediaDir());

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
      logger.debug("\teditItem made overlay " + exercise);
    }

    if (notOverlay) {
      logger.error("huh? couldn't make overlay or find user exercise for " + userExercise);
    } else {
      boolean b = original.removeAll(defects);  // TODO - does this work really without a compareTo?
      logger.debug(b ? "editItem removed defects " + original.size() + " now" : "editItem didn't remove any defects - " + defects.size());

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
   * After marking an audio defective, we want to make an annotation that indicates it's no longer something that
   * needs to be fixed.
   *
   * @param audioAttribute
   * @see mitll.langtest.server.services.QCServiceImpl#markAudioDefect
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   */
  public void markAudioDefect(AudioAttribute audioAttribute) {
    if (audioDAO.markDefect(audioAttribute) < 1) {
      logger.error("markAudioDefect huh? couldn't mark error on " + audioAttribute);
    } else {
      userListManager.addAnnotation(
          audioAttribute.getExid(),
          audioAttribute.getAudioRef(),
          UserListManager.CORRECT,
          "audio marked with defect",
          audioAttribute.getUserid());
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
    return new UserSessionHistory().getUserHistoryForList(userid,
        userDAO.getByID(userid),
        ids,
        latestResultID,
        allIDs,
        idToKey,
        resultDAO
    );
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

      if (widgetType.equals(UserPassLogin.USER_NAME_BOX)) {
        return true;
      } else {
        //  logger.debug("logEvent for user " + userid);
        userid = userDAO.getBeforeLoginUser();
      }
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

  public IPhoneDAO<Phone> getPhoneDAO() {
    return phoneDAO;
  }

  /**
   * the User info lives in domino...
   *
   * @see #initializeDAOs(PathHelper)
   */
  public void createTables() {
    //  logger.info("createTables create slick tables - has " + dbConnection.getTables());
    List<IDAO> idaos = Arrays.asList(
        //getUserDAO(),
        userPermissionDAO,
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
        userSessionDAO,
        dliClassDAO,
        dliClassJoinDAO
    );

    List<String> created = new ArrayList<>();
    for (IDAO dao : idaos) createIfNotThere(dao, created);
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

  /**
   * @param projid
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getResultAlternatives
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getResults
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
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getResults
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
   * @see ProjectManagement#configureProject
   */
  public Map<Integer, String> getExerciseIDToRefAudio(int projectid) {
    Collection<CommonExercise> exercises = getExercises(projectid);
    logger.info("getExerciseIDToRefAudio for project #" + projectid + " exercises " + exercises.size());

    Map<Integer, String> join = new HashMap<>();
    populateIDToRefAudio(join, exercises);

    Collection<CommonExercise> all = userExerciseDAO.getAllUserExercises(projectid);
    getExerciseDAO(projectid).attachAudio(all);
    populateIDToRefAudio(join, all);
    return join;
  }

  /**
   * @param join
   * @param all
   * @param <T>
   */
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
    if (join.isEmpty()) {
      logger.warn("populateIDToRefAudio huh? no ref audio on " + all.size() + " exercises???");
    }
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

  /**
   * @see LangTestDatabaseImpl#destroy()
   */
  public void destroy() {
    try {
      userDAO.cleanUp();
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
   * @seex mitll.langtest.server.LangTestDatabaseImpl#deleteItem
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

  private int warns = 0;

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
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExercise
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
      toRet = getUserExerciseByExID(id);
//      if (toRet != null) {
//        attachAudio(toRet);
//      }
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
  public void writeZip(OutputStream out,
                       Map<String, Collection<String>> typeToSection,
                       int projectid,
                       AudioExport.AudioExportOptions options) throws Exception {
    Collection<CommonExercise> exercisesForSelectionState = typeToSection.isEmpty() ?
        getExercises(projectid) :
        getSectionHelper(projectid).getExercisesForSelectionState(typeToSection);
    String language = getLanguage(projectid);
    new AudioExport(getServerProps())
        .writeZip(out,
            typeToSection,
            getSectionHelper(projectid),
            exercisesForSelectionState,
            language,
            getAudioDAO(),
            false,
            options);
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
    new AudioExport(getServerProps()).writeZipJustOneAudio(out, getSectionHelper(projectid),
        getExercises(projectid), installPath,
        getProject(projectid).getLanguage());
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
      new AudioExport(getServerProps()).writeUserListAudio(
          out,
          userListByID.getName(),
          getSectionHelper(projectid),
          copyAsExercises,
          language,
          getAudioDAO(),
          listid == IUserListManager.REVIEW_MAGIC_ID,
          options);
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
    return getAudioDAO().attachAudioToExercise(ex, getLanguage(ex));
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
    return getUserListManager().getUserListByID(listid, getSectionHelper(projectid).getTypeOrder(), getIDs(projectid));
  }

  public UserList<CommonExercise> getUserListByIDExercises(long listid, int projectid) {
    return getUserListManager().getUserListByIDExercises(listid,
        projectid,
        getSectionHelper(projectid).getTypeOrder(), getIDs(projectid));
  }

  public Set<Integer> getIDs(int projectid) {
    ExerciseDAO<CommonExercise> exerciseDAO = getExerciseDAO(projectid);
    return exerciseDAO == null ? Collections.emptySet() : exerciseDAO.getIDs();
  }

  /**
   * @param typeToSection
   * @param projectid
   * @return
   * @see DownloadServlet#getBaseName
   */
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
   * @param isCorrect
   * @see mitll.langtest.server.services.ScoringServiceImpl#getPretestScore
   */
  public void rememberScore(int resultID, PretestScore asrScoreForAudio, boolean isCorrect) {
    getAnswerDAO().changeAnswer(resultID, asrScoreForAudio.getHydecScore(), asrScoreForAudio.getProcessDur(), asrScoreForAudio.getJson(), isCorrect);
    recordWordAndPhone.recordWordAndPhoneInfo(resultID, asrScoreForAudio);
  }

  /**
   * @param answer
   * @param answerID
   * @see mitll.langtest.server.audio.AudioFileHelper#recordInResults(AudioContext, AnswerInfo.RecordingInfo, AudioCheck.ValidityAndDur, AudioAnswer)
   */

  public void recordWordAndPhoneInfo(AudioAnswer answer, long answerID) {
    recordWordAndPhone.recordWordAndPhoneInfo(answer, answerID);
  }

  /**
   * @param projectid
   * @return
   * @see LangTestDatabaseImpl#getMaleFemaleProgress()
   */
  public Map<String, Float> getMaleFemaleProgress(int projectid) {
    IUserDAO userDAO = getUserDAO();
    logger.info("getMaleFemaleProgress getting exercises -- " + projectid);
    Collection<CommonExercise> exercises = getExercises(projectid);

    List<DBUser> all = userDAO.getAll();
    Map<Integer, User> userMapMales = userDAO.getUserMapFromUsers(true, all);
    logger.info("getMaleFemaleProgress getting userMapMales -- " + userMapMales.size());

    Map<Integer, User> userMapFemales = userDAO.getUserMapFromUsers(false, all);
    logger.info("getMaleFemaleProgress getting userMapFemales -- " + userMapFemales.size());

    float total = exercises.size();
    Set<Integer> uniqueIDs = new HashSet<>();

    int context = 0;
    Map<Integer, String> exToTranscript = new HashMap<>();
    Map<Integer, String> exToContextTranscript = new HashMap<>();

    for (CommonExercise shell : exercises) {
      if (shell.hasContext()) context++;
      boolean add = uniqueIDs.add(shell.getID());
      if (!add) {
        logger.warn("getMaleFemaleProgress found duplicate id " + shell.getID() + " : " + shell);
      }
      exToTranscript.put(shell.getID(), shell.getForeignLanguage());
      exToContextTranscript.put(shell.getID(), shell.getContext());
    }

    logger.info("getMaleFemaleProgress found " + total + " total exercises, " +
        uniqueIDs.size() +
        " unique" +
        " males " + userMapMales.size() + " females " + userMapFemales.size());


    return getAudioDAO().getRecordedReport(userMapMales, userMapFemales, total, uniqueIDs,
        exToTranscript, exToContextTranscript, context);
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

  /**
   * @return
   */
  public IUserSessionDAO getUserSessionDAO() {
    return userSessionDAO;
  }

/*
  public IUserPermissionDAO getUserPermissionDAO() {
    return userPermissionDAO;
  }
*/

  public IInviteDAO getInviteDAO() {
    return inviteDAO;
  }

  public String toString() {
    return "Database : " + this.getClass().toString();
  }

  public IUserSecurityManager getUserSecurityManager() {
    return userSecurityManager;
  }

  public void setUserSecurityManager(IUserSecurityManager userSecurityManager) {
    this.userSecurityManager = userSecurityManager;
  }
}
