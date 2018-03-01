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

import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.server.*;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.server.audio.AudioExportOptions;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.annotation.IAnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.audio.AudioDAO;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.audio.IEnsureAudioHelper;
import mitll.langtest.server.database.audio.SlickAudioDAO;
import mitll.langtest.server.database.copy.CopyToPostgres;
import mitll.langtest.server.database.custom.IStateManager;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.custom.StateManager;
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
import mitll.langtest.server.database.user.*;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.SlickUserListDAO;
import mitll.langtest.server.database.userlist.SlickUserListExerciseJoinDAO;
import mitll.langtest.server.database.userlist.SlickUserListExerciseVisitorDAO;
import mitll.langtest.server.database.word.IWordDAO;
import mitll.langtest.server.database.word.SlickWordDAO;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.services.UserServiceImpl;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.MutableAudioExercise;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.text.CollationKey;
import java.util.*;

import static mitll.langtest.server.database.Report.DAY_TO_SEND_REPORT;
import static mitll.langtest.server.database.custom.IUserListManager.COMMENT_MAGIC_ID;

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
public class DatabaseImpl implements Database, DatabaseServices {
  private static final Logger logger = LogManager.getLogger(DatabaseImpl.class);
  private static final String UNKNOWN = "unknown";
  public static final int IMPORT_PROJECT_ID = -100;
  private static final boolean ADD_DEFECTS = false;
  private static final int DAY = 24 * 60 * 60 * 1000;

  private IUserDAO userDAO;
  private IUserSessionDAO userSessionDAO;
  private IResultDAO resultDAO;

  private IRefResultDAO refresultDAO;
  private IWordDAO wordDAO;
  private IPhoneDAO<Phone> phoneDAO;

  private IAudioDAO audioDAO;
  private IAnswerDAO answerDAO;
  private IUserListManager userListManager;
  private IStateManager stateManager;
  private IUserExerciseDAO userExerciseDAO;

  private IEventDAO eventDAO;
  private IProjectDAO projectDAO;
  private IUserProjectDAO userProjectDAO;
  private IDLIClassDAO dliClassDAO;
  private IDLIClassJoinDAO dliClassJoinDAO;

  protected ServerProperties serverProps;
  protected LogAndNotify logAndNotify;

  private UserManagement userManagement = null;

  protected PathHelper pathHelper;
  private IProjectManagement projectManagement;
  private RecordWordAndPhone recordWordAndPhone;

  private IUserSecurityManager userSecurityManager;
  private DominoExerciseDAO dominoExerciseDAO;
  private boolean hasValidDB = false;

  public DatabaseImpl() {
  }

  /**
   * @param serverProps
   * @see CopyToPostgres#getSimpleDatabase
   */
  public DatabaseImpl(ServerProperties serverProps) {
    this.serverProps = serverProps;
    this.logAndNotify = null;
    setPostgresDBConnection();
  }

  /**
   * @param serverProps
   * @param pathHelper
   * @param logAndNotify
   * @param servletContext
   * @see LangTestDatabaseImpl#makeDatabaseImpl
   */
  public DatabaseImpl(ServerProperties serverProps,
                      PathHelper pathHelper,
                      LogAndNotify logAndNotify,
                      ServletContext servletContext) {
    this.serverProps = serverProps;
    this.logAndNotify = logAndNotify;
    this.pathHelper = pathHelper;

    connectToDatabases(pathHelper, servletContext);
  }

  /**
   * @param pathHelper
   * @param servletContext
   * @see #DatabaseImpl
   */
  void connectToDatabases(PathHelper pathHelper, ServletContext servletContext) {
    long then = System.currentTimeMillis();
    // first connect to postgres

    setPostgresDBConnection();
//    logger.debug("initializeDAOs --- " + servletContext);

    // then connect to mongo
    DominoUserDAOImpl dominoUserDAO = new DominoUserDAOImpl(this, servletContext);

    initializeDAOs(pathHelper, dominoUserDAO);

    dominoUserDAO.setUserProjectDAO(getUserProjectDAO());

    {
      long now = System.currentTimeMillis();

      if (now - then > 300) {
        logger.info("DatabaseImpl : took " + (now - then) + " millis to initialize DAOs");
      }
    }

    hasValidDB = true;
  }

  @Override
  public void dropProject(int projID) {
    logger.info("drop project #" + projID);
    long then = System.currentTimeMillis();
    long initial = then;
    getRefResultDAO().deleteForProject(projID);
    long now = System.currentTimeMillis();

    logger.info("took " + (now - then) + " to delete from ref result dao for #" + projID);

    then = now;
    getAudioDAO().deleteForProject(projID);
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " to delete from audio dao for #" + projID);

    then = now;
    // result table.
    logger.info("start deleting from phone table...");
    getPhoneDAO().deleteForProject(projID);
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " to delete from phones for #" + projID);

    then = now;
    // result table.
    logger.info("start deleting from word table...");
    getWordDAO().deleteForProject(projID);
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " to delete from words for #" + projID);


    then = now;
    // result table.
    logger.info("start deleting from result table...");
    getAnswerDAO().deleteForProject(projID);
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " to delete from result for #" + projID);


    then = now;
    // event table.
    logger.info("start deleting from event table...");
    getEventDAO().deleteForProject(projID);
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " to delete from event dao for #" + projID);

    then = now;
    // exercise table.
    logger.info("start deleting from exercise table...");
    userExerciseDAO.deleteForProject(projID);
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " to delete from exercise dao for #" + projID);

    then = now;
    // project table.
    getProjectDAO().delete(projID);
    now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " to delete from project dao for #" + projID);

    logger.info("took " + (now - initial) + " to drop #" + projID);

  }

  /**
   * @see DatabaseImpl#makeDAO
   * @see DatabaseImpl#DatabaseImpl
   * @see LangTestDatabaseImpl#init
   */
  public DatabaseImpl populateProjects() {
    if (projectManagement == null) {
      logger.info("populateProjects no project management yet...");
    } else {
//      logger.info("populateProjects --- ");

      projectManagement.populateProjects();

      userDAO.setProjectManagement(getProjectManagement());

    }
    return this;
  }

  /**
   * Slick db connection.
   */
  private DBConnection dbConnection;

  /**
   * Create or alter tables as needed.
   *
   * @see #DatabaseImpl(ServerProperties, PathHelper, LogAndNotify, ServletContext)
   */
  private void initializeDAOs(PathHelper pathHelper, DominoUserDAOImpl dominoUserDAO) {
    eventDAO = new SlickEventImpl(dbConnection);

    this.userDAO = dominoUserDAO;
    this.userSessionDAO = new SlickUserSessionDAOImpl(this, dbConnection);
    SlickAudioDAO slickAudioDAO = new SlickAudioDAO(this, dbConnection, this.userDAO);
    audioDAO = slickAudioDAO;
    resultDAO = new SlickResultDAO(this, dbConnection);
    answerDAO = new SlickAnswerDAO(this, dbConnection);

    refresultDAO = new SlickRefResultDAO(this, dbConnection);
    userExerciseDAO = new SlickUserExerciseDAO(this, dbConnection);
    wordDAO = new SlickWordDAO(this, dbConnection);
    phoneDAO = new SlickPhoneDAO(this, dbConnection);

    SlickUserListExerciseJoinDAO userListExerciseJoinDAO = new SlickUserListExerciseJoinDAO(this, dbConnection);
    IUserListDAO userListDAO = new SlickUserListDAO(this, dbConnection, this.userDAO, userExerciseDAO);
    IAnnotationDAO annotationDAO = new SlickAnnotationDAO(this, dbConnection, this.userDAO.getDefectDetector());

    IReviewedDAO reviewedDAO = new SlickReviewedDAO(this, dbConnection, true);
    IReviewedDAO secondStateDAO = new SlickReviewedDAO(this, dbConnection, false);

    stateManager = new StateManager(reviewedDAO, secondStateDAO);
    userListManager = new UserListManager(this.userDAO,
        userListDAO,
        userListExerciseJoinDAO,
        annotationDAO,
        stateManager,
        new SlickUserListExerciseVisitorDAO(this, dbConnection),
        this,
        pathHelper);

    projectDAO = new ProjectDAO(this, dbConnection);
    userProjectDAO = new UserProjectDAO(dbConnection);
    dliClassDAO = new DLIClassDAO(dbConnection);
    dliClassJoinDAO = new DLIClassJoinDAO(dbConnection);

    createTables();


    userDAO.ensureDefaultUsers();
    int defaultProject = getDefaultProject();
    // make sure we have a template exercise

    slickAudioDAO.setDefaultResult(resultDAO.ensureDefault(defaultProject, userDAO.getBeforeLoginUser(),
        userExerciseDAO.ensureTemplateExercise(defaultProject)));

    try {
      ((UserListManager) userListManager).setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    recordWordAndPhone = new RecordWordAndPhone(wordDAO, phoneDAO);
    dominoExerciseDAO = new DominoExerciseDAO(dominoUserDAO.getSerializer());

    logger.debug("initializeDAOs : tables = " + getTables());
  }

  private int getDefaultProject() {
    int defaultProject = projectDAO.ensureDefaultProject(userDAO.getBeforeLoginUser());
    String propValue = projectDAO.getPropValue(defaultProject, ProjectProperty.REPORT_LIST.getName());

    if (propValue == null) {
      List<String> reportEmails = serverProps.getReportEmails();

//      logger.info("default properties : " + reportEmails);

      projectDAO.addOrUpdateProperty(defaultProject, ProjectProperty.REPORT_LIST,
          reportEmails.toString()
              .replaceAll("\\[", "").replaceAll("]", ""));
      //    logger.info("default properties : " + projectDAO.getPropValue(defaultProject, ProjectProperty.REPORT_LIST.getName()));
    } else {
      //  logger.info("existing default properties : " + projectDAO.getPropValue(defaultProject, ProjectProperty.REPORT_LIST.toString()));
    }
    return defaultProject;
  }

  private void setPostgresDBConnection() {
    dbConnection = getDbConnection();
  }

  /**
   * @return
   * @see #initializeDAOs
   */
  private DBConnection getDbConnection() {
    DBConnection dbConnection = new DBConnection(serverProps.getDBConfig());
//    logger.info("getDbConnection using " + serverProps.getDBConfig() + " : " + dbConnection);
    return dbConnection;
  }

  public IAudioDAO getH2AudioDAO() {
    return new AudioDAO(this, new UserDAO(this));
  }

  @Override
  public IResultDAO getResultDAO() {
    return resultDAO;
  }

  /**
   * @param projectid
   * @return
   */
  @Override
  public IAnalysis getAnalysis(int projectid) {
    return getProject(projectid).getAnalysis();
  }

  @Override
  public IRefResultDAO getRefResultDAO() {
    return refresultDAO;
  }

  @Override
  public IUserDAO getUserDAO() {
    return userDAO;
  }

  public DatabaseImpl setInstallPath(String lessonPlanFileOnlyForImport) {
    return setInstallPath(lessonPlanFileOnlyForImport, null);
  }

  /**
   * @param lessonPlanFileOnlyForImport
   * @param servletContext
   * @see mitll.langtest.server.LangTestDatabaseImpl#setInstallPath
   */
  @Override
  public DatabaseImpl setInstallPath(String lessonPlanFileOnlyForImport, ServletContext servletContext) {
    this.projectManagement = new ProjectManagement(pathHelper, serverProps, getLogAndNotify(), this, servletContext);
    makeDAO(lessonPlanFileOnlyForImport);
    return this;
  }

  /**
   * JUST FOR TESTING
   *
   * @return
   */
  @Deprecated
  public ISection<CommonExercise> getSectionHelper() {
    return getSectionHelper(-1);
  }

  /**
   * sections are valid in the context of a project.
   *
   * @param projectid
   * @return
   * @see mitll.langtest.server.DownloadServlet#returnSpreadsheet
   * @see Database#getTypeOrder
   */
  @Override
  public ISection<CommonExercise> getSectionHelper(int projectid) {
    if (projectid == -1) {
      return null;
    }

    if (isAmas()) {
      return new SectionHelper<>();
    }

    getExercises(projectid);
    Project project = getProject(projectid);
    if (project == null) {
      logger.error("getSectionHelper huh? couldn't find project with id " + projectid);
      return null;
    } else {
      return project.getSectionHelper();
    }
  }

  private boolean isAmas() {
    return serverProps.isAMAS();
  }

  public Collection<String> getTypeOrder(int projectid) {
    ISection sectionHelper = (isAmas()) ? getAMASSectionHelper() : getSectionHelper(projectid);
    if (sectionHelper == null) {
      logger.warn("getTypeOrder no section helper for " + this + " and " + projectid);
    }
    Collection<String> types = (sectionHelper == null) ? Collections.emptyList() : sectionHelper.getTypeOrder();

    if (types.isEmpty()) {
      logger.error("getTypeOrder empty type order : " + projectid + " = " + types);
    }
    return types;
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#getResultASRInfo
   * @see mitll.langtest.server.DownloadServlet#getFilenameForDownload
   * @see #getCustomOrPredefExercise
   */
  @Override
  public CommonExercise getExercise(int projectid, int id) {
    return projectManagement.getExercise(projectid, id);
  }

  /**
   * @param projectid
   * @return
   * @see ScoreServlet#getJSONForExercises
   * @see ScoreServlet#getJsonNestedChapters
   */
  public JsonExport getJSONExport(int projectid) {
    getExercises(projectid);

    Map<String, Integer> stringIntegerMap = Collections.emptyMap();
    AudioFileHelper audioFileHelper = getProject(projectid).getAudioFileHelper();

    JsonExport jsonExport = new JsonExport(
        audioFileHelper == null ? stringIntegerMap : audioFileHelper.getPhoneToCount(),
        getSectionHelper(projectid),
        serverProps.getPreferredVoices(),
        getLanguage(projectid).equalsIgnoreCase("english")
    );

    attachAllAudio(projectid);
    return jsonExport;
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
   * @see Project#buildExerciseTrie
   */
  @Override
  public List<CommonExercise> getExercises(int projectid) {
    return projectManagement.getExercises(projectid);
  }

  @Override
  public ExerciseDAO<CommonExercise> getExerciseDAO(int projectid) {
    Project project = getProject(projectid);
    logger.debug("getExerciseDAO " + projectid + " found project " + project);
    ExerciseDAO<CommonExercise> exerciseDAO = project.getExerciseDAO();
    logger.debug("getExerciseDAO " + projectid + " found exercise dao " + exerciseDAO);
    return exerciseDAO;
  }

  @Override
  public Project getProjectForUser(int userid) {
    return projectManagement.getProjectForUser(userid);
  }

  @Override
  public void stopDecode() {
    projectManagement.stopDecode();
  }

  /**
   * Mainly for import
   *
   * @param projectid
   * @see mitll.langtest.server.database.copy.CreateProject#createProjectIfNotExists
   */
  @Override
  public void rememberProject(int projectid) {
    projectManagement.rememberProject(projectid);
  }

  /**
   * Make sure there's a favorites list per user per project
   *
   * @param userid
   * @param projectid
   * @see mitll.langtest.server.services.OpenUserServiceImpl#setProject
   */
  @Override
  public void rememberUsersCurrentProject(int userid, int projectid) {
    //  logger.info("rememberUsersCurrentProject user " + userid + " -> " + projectid);
    getUserProjectDAO().add(userid, projectid);
    getUserListManager().createFavorites(userid, projectid);
  }

  /**
   * @param userid
   * @see mitll.langtest.server.services.OpenUserServiceImpl#forgetProject
   */
  @Override
  public void forgetProject(int userid) {
    getUserProjectDAO().forget(userid);
  }

  /**
   * @param userWhere
   * @see IUserSecurityManager#setSessionUser
   * @see UserServiceImpl#getUserFromSession
   */
  public void setStartupInfo(User userWhere) {
//    logger.info("setStartupInfo on " + userWhere.getUserID());
    setStartupInfo(userWhere, projectForUser(userWhere.getID()));
  }

  /**
   * @param userWhere
   * @param projid
   * @see #setStartupInfo(User)
   * @see mitll.langtest.server.services.OpenUserServiceImpl#setProject
   */
  @Override
  public void setStartupInfo(User userWhere, int projid) {
    projectManagement.setStartupInfo(userWhere, projid);
  }

  @Override
  public IStateManager getStateManager() {
    return stateManager;
  }

  /**
   * Amas dedicated calls
   *
   * @return
   */

  @Override
  public List<AmasExerciseImpl> getAMASExercises() {
    return null;
  }

  @Override
  public AmasExerciseImpl getAMASExercise(int id) {
    return null;
  }

  @Override
  public ISection<AmasExerciseImpl> getAMASSectionHelper() {
    return null;
  }

  /**
   * TODO : make sure this works for AMAS?
   * <p>
   * Lazy, latchy instantiation of DAOs.
   * Not sure why it really has to be this way.
   * <p>
   * Special check for amas exercises...
   *
   * @param lessonPlanFileOnlyForImport only for import
   * @see #setInstallPath(String, ServletContext)
   */
  private void makeDAO(String lessonPlanFileOnlyForImport) {
    // logger.info("makeDAO - " + lessonPlanFileOnlyForImport);
    if (userManagement == null) {
      synchronized (this) {
        // boolean isURL = serverProps.getLessonPlan().startsWith("http");
        boolean amas = isAmas();
        // int numExercises;

        if (amas) {
//          logger.info("Got " + lessonPlanFileOnlyForImport);
          // TODO : get media directory from properties
          // TODO : get install path directory from properties
          // readAMASExercises(lessonPlanFileOnlyForImport, "", "", isURL);
        } else {
          //  logger.info("makeDAO makeExerciseDAO -- " + lessonPlanFileOnlyForImport);
          makeExerciseDAO(lessonPlanFileOnlyForImport);

          if (!serverProps.useH2()) {
            //        userExerciseDAO.useExToPhones(new ExerciseToPhone().getExerciseToPhone(refresultDAO));
            populateProjects();
            //    logger.info("set exercise dao " + exerciseDAO + " on " + userExerciseDAO);
            if (projectManagement.getProjects().isEmpty()) {
              logger.warn("\nmakeDAO no projects loaded yet...?");
            }
      /*      else {
              ExerciseDAO<CommonExercise> exerciseDAO = projectManagement.getFirstProject().getExerciseDAO();
              logger.info("using exercise dao from first project " + exerciseDAO);
              userExerciseDAO.setExerciseDAO(exerciseDAO);
            }*/
          }

          if (serverProps.useH2()) {
            userExerciseDAO.setExerciseDAO(projectManagement.setDependencies());
          }
        }
        userManagement = new mitll.langtest.server.database.user.UserManagement(userDAO, resultDAO);
      }
    }
  }

  /**
   * For when a new project is added or changes state.
   *
   * @param project
   * @param forceReload
   * @return number of exercises in the project
   * @see mitll.langtest.server.services.ProjectServiceImpl#update
   */
  @Override
  public int configureProject(Project project, boolean forceReload) {
    return projectManagement.configureProject(project, false, forceReload);
  }

  /**
   * Here to support import from old individual sites for CopyToPostgres
   *
   * @param lessonPlanFile
   * @see #makeDAO(String)
   */
  private void makeExerciseDAO(String lessonPlanFile) {
//    logger.info("makeExerciseDAO - " + lessonPlanFile + " : use h2 = " + serverProps.useH2());
    if (serverProps.useH2()) {
      logger.info("makeExerciseDAO reading from excel sheet " + lessonPlanFile);
      projectManagement.addSingleProject(new ExcelImport(lessonPlanFile, getServerProps(), userListManager, ADD_DEFECTS));
    } else {
      logger.info("*not* making import project");
    }
  }

  /**
   * @param projectid if it's -1 only do something if it's on import
   * @return
   */
  public Project getProject(int projectid) {
    if (projectid == -1) {
      if (serverProps.useH2()) {
        logger.info("getProject asking for project -1?");
      } else {
        logger.warn("getProject asking for project -1?");//, new Exception());
      }
      return null;
    } else {
      if (projectManagement == null) {
        setInstallPath("", null);
      }
      return projectManagement.getProject(projectid);
    }
  }

  public Project getProjectByName(String name) {
    return projectManagement.getProjectByName(name);
  }

  public Collection<Project> getProjects() {
    return projectManagement.getProjects();
  }

  /**
   * A little dusty...
   *
   * @paramx lessonPlanFile
   * @paramx mediaDir
   * @paramx installPath
   * @paramx isURL
   * @return
   */
/*  private int readAMASExercises(String lessonPlanFile, String mediaDir, String installPath, boolean isURL) {
    int numExercises;
//    if (isURL) {
//      this.fileExerciseDAO = new AMASJSONURLExerciseDAO(getServerProps());
//      numExercises = fileExerciseDAO.getNumExercises();
//    } else {
    fileExerciseDAO = new FileExerciseDAO<>(mediaDir, getOldLanguage(serverProps), absConfigDir,
        lessonPlanFile, installPath);
    numExercises = fileExerciseDAO.getNumExercises();
//    }
    return numExercises;
  }*/


  /**
   * TODO : why is this so confusing???
   *
   * @param userExercise
   * @param keepAudio
   * @see mitll.langtest.server.services.ListServiceImpl#editItem
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#editItem
   */
  @Override
  public CommonExercise editItem(CommonExercise userExercise, boolean keepAudio) {
    int id = userExercise.getID();
    logger.debug("editItem exercise #" + id +
        " keep audio " + keepAudio +
        " mediaDir : " + getServerProps().getMediaDir() +
        " audio " + userExercise.getAudioAttributes());
    int projectID = userExercise.getProjectID();
    if (projectID < 0) {
      logger.warn("huh? no project id on user exer " + userExercise);
    }
    getUserListManager().editItem(userExercise,
        // create if doesn't exist
        getServerProps().getMediaDir(), getTypeOrder(projectID));

    // Set<AudioAttribute> originalAudio = new HashSet<>(userExercise.getAudioAttributes());
    Set<AudioAttribute> defectAudio = audioDAO.getAndMarkDefects(userExercise, userExercise.getFieldToAnnotation());

    /*
    if (!originalAudio.isEmpty()) {
      logger.debug("editItem originally had " + originalAudio.size() + " attributes, and " + defectAudio.size() + " defectAudio");
    }*/
    boolean isPredef = userExercise.isPredefined();

/*
    CommonExercise exercise = isPredef ? getExerciseDAO(projectID).addOverlay(userExercise) : null;
    //boolean notOverlay = exercise == null;
    if (isPredef) {
      // exercise = userExercise;
      logger.debug("\teditItem made overlay " + exercise);
    } else {
// not an overlay! it's a new user exercise
      exercise = getUserExerciseByExID(userExercise.getID());
      logger.debug("editItem user custom exercise is " + exercise);
    }*/


    if (isPredef) {
      clearDefects(
          defectAudio, userExercise);

      // why would this make sense to do???
/*      String overlayID = exercise.getOldID();

      logger.debug("editItem copying " + originalAudio.size() + " audio attrs under exercise overlay id " + overlayID);

        for (AudioAttribute toCopy : originalAudio) {
          if (toCopy.getUserid() < UserDAO.DEFAULT_FEMALE_ID) {
            logger.error("bad user id for " + toCopy);
          }
        logger.debug("\t copying " + toCopy);
        audioDAO.add((int) toCopy.getUserid(), toCopy.getAudioRef(), overlayID, toCopy.getTimestamp(), toCopy.getAudioType(), toCopy.getDurationInMillis());
      }*/

    }

    if (isPredef) {
      getSectionHelper(projectID).refreshExercise(userExercise);
    }
    return userExercise;
  }

  private void clearDefects(//Set<AudioAttribute> original,
                            Set<AudioAttribute> defects, CommonExercise exercise) {
    //boolean b = original.removeAll(defects);  // TODO - does this work really without a compareTo?
    //logger.debug(b ? "editItem removed defects " + original.size() + " now" : "editItem didn't remove any defects - " + defects.size());

    MutableAudioExercise mutableAudio = exercise.getMutableAudio();
    for (AudioAttribute attribute : defects) {
      if (!mutableAudio.removeAudio(attribute)) {
        logger.warn("editItem huh? couldn't remove " + attribute.getKey() + " from " + exercise.getID());
      }
    }
  }

  /**
   * After marking an audio defective, we want to make an annotation that indicates it's no longer something that
   * needs to be fixed.
   *
   * @param audioAttribute
   * @see mitll.langtest.server.services.QCServiceImpl#markAudioDefect
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   */
  @Override
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
   * @param projid
   * @return
   * @paramx collator
   * @see mitll.langtest.server.ScoreServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public JSONObject getJsonScoreHistory(int userid,
                                        Map<String, Collection<String>> typeToSection,
                                        ExerciseSorter sorter,
                                        int projid) {

    if (projid == -1) {
      projid = projectForUser(userid);
    }
    return getJsonSupportForProject(projid).getJsonScoreHistory(userid, typeToSection, sorter);
  }

  private int projectForUser(int userid) {
    return getUserProjectDAO().getCurrentProjectForUser(userid);
  }

  private JsonSupport getJsonSupportForProject(int i) {
    return getProject(i).getJsonSupport();
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
  public JSONObject getJsonPhoneReport(int userid, int projid, Map<String, Collection<String>> typeToValues) {
    if (projid == -1) {
      projid = projectForUser(userid);
    }
    return getJsonSupportForProject(projid)
        .getJsonPhoneReport(userid, typeToValues, getLanguage(projid));
  }

  /**
   * does all average calc on server
   *
   * @return
   * @paramx listid
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   */
  @Override
  public AVPScoreReport getUserHistoryForList(int userid,
                                              Collection<Integer> ids,
                                              int latestResultID,
                                              Collection<Integer> allIDs,
                                              Map<Integer, CollationKey> idToKey,
                                              String language) {
    return new UserSessionHistory().getUserHistoryForList(userid,
        ids,
        latestResultID,
        allIDs,
        idToKey,
        resultDAO,
        language);
  }

  @Override
  public Connection getConnection(String who) {
    return null;
  }

  @Override
  public void closeConnection(Connection connection) {
  }

  public void logEvent(String exid, String context, int userid, String device, int projID) {
    if (context.length() > 100) context = context.substring(0, 100).replace("\n", " ");
    logEvent(UNKNOWN, "server", exid, context, userid, device, projID);
  }

  /**
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param device
   * @param projID
   * @return
   * @see mitll.langtest.server.ScoreServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
  public boolean logEvent(String id, String widgetType, String exid, String context, int userid, String device, int projID) {
    if (userid == -1) {
      if (widgetType.equals(UserPassLogin.USER_NAME_BOX)) {
        return true;
      } else {
        //  logger.debug("logEvent for user " + userid);
        userid = userDAO.getBeforeLoginUser();
      }
    }

    if (projID == -1) {
      projID = projectForUser(userid);
      if (projID == -1 && !getProjects().isEmpty()) {
        projID = 1; // default project
      }
    }

    if (projID == -1) {
      logger.warn("no projects loaded yet ???");
      return false;
    } else {
      int realExid = -1;
      try {
        if (!exid.equalsIgnoreCase("N/A")) {
          realExid = Integer.parseInt(exid);
        }
      } catch (NumberFormatException e) {
        logger.warn("couldn't parse exid '" + exid + "'");
        //e.printStackTrace();
      }
      Event event = new Event(id, widgetType, exid, context, userid, System.currentTimeMillis(), device, realExid);
      return eventDAO != null && eventDAO.addToProject(event, projID);
    }
  }

  public void logAndNotify(Exception e) {
    getLogAndNotify().logAndNotifyServerException(e);
  }


  /**
   * the User info lives in domino...
   * <p>
   * This is a little more sophisticated in that it will recreate individual tables that might get dropped.
   *
   * @see #initializeDAOs
   */
  public void createTables() {
    //  logger.info("createTables create slick tables - has " + dbConnection.getTables());
    List<IDAO> idaos = Arrays.asList(
        getProjectDAO(),
        userExerciseDAO,
        ((SlickUserExerciseDAO) userExerciseDAO).getRelatedExercise(),
        ((SlickUserExerciseDAO) userExerciseDAO).getExerciseAttribute(),
        ((SlickUserExerciseDAO) userExerciseDAO).getExerciseAttributeJoin(),
        getEventDAO(),
        getResultDAO(),
        getAudioDAO(),
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
      logger.info("createTables created slick tables          : " + created);
      logger.info("createTables after create slick tables - has " + getTables());
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
   * @see mitll.langtest.server.services.ResultServiceImpl#getResults(int, Map, int, String)
   * @see DownloadServlet#returnSpreadsheet
   */
  @Override
  public Collection<MonitorResult> getMonitorResults(int projid) {
    List<MonitorResult> monitorResults = resultDAO.getMonitorResultsKnownExercises(projid);

    logger.debug("getMonitorResults got back " + monitorResults.size() + " for project " + projid);
/*    for (MonitorResult result : monitorResults) {
      int exID = result.getExID();
      CommonShell exercise = isAmas() ? getAMASExercise(exID) : getExercise(projid, exID);
      if (exercise != null) {
        int dominoID = isAmas() ? -1 : ((CommonExercise) exercise).getDominoID();
        result.setDisplayID("" + dominoID);
      }
    }*/
    List<MonitorResult> monitorResultsWithText = getMonitorResultsWithText(monitorResults, projid);

    logger.debug("getMonitorResults got back after join " + monitorResultsWithText.size() + " for project " + projid);

    return monitorResultsWithText;
  }

  /**
   * @param monitorResults
   * @param projid
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getResults
   * @see #getMonitorResults(int)
   */
  @Override
  public List<MonitorResult> getMonitorResultsWithText(List<MonitorResult> monitorResults, int projid) {
    Map<Integer, CommonExercise> idToExerciseMap = getIdToExerciseMap(projid);
    logger.debug("getMonitorResultsWithText Got size = " + idToExerciseMap.size() + " id->ex map");
    addUnitAndChapterToResults(monitorResults, idToExerciseMap);
    return monitorResults;
  }

  /**
   * TODO : NO - don't do this - use a query.
   * <p>
   * Add info from exercises.
   *
   * @param monitorResults
   * @param join
   * @see DatabaseImpl#getMonitorResultsWithText
   */
  private void addUnitAndChapterToResults(Collection<MonitorResult> monitorResults,
                                          Map<Integer, CommonExercise> join) {
    int n = 0;
    int m = 0;
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
        m++;
      }
    }
    if (n > 0) {
      logger.warn("addUnitAndChapterToResults : skipped " + n + " out of " + monitorResults.size() +
          " # bad join ids = " + unknownIDs.size());
    }
    logger.debug("addUnitAndChapterToResults joined with " + m + " results");
  }

  /**
   * @param projectid
   * @return
   * @see #getMonitorResultsWithText
   */
  private Map<Integer, CommonExercise> getIdToExerciseMap(int projectid) {
    Map<Integer, CommonExercise> join = new HashMap<>();
    getExercises(projectid).forEach(exercise -> join.put(exercise.getID(), exercise));

/*    // TODO : why would we want to do this?
    if (userExerciseDAO != null && getExerciseDAO(projectid) != null) {
      for (CommonExercise exercise : userExerciseDAO.getAllUserExercises(projectid)) {
        join.put(exercise.getID(), exercise);
      }
    }*/

    return join;
  }

  /**
   * @see LangTestDatabaseImpl#destroy()
   */
  public void close() {
    try {
      if (userDAO != null) userDAO.close();
    } catch (Exception e) {
      logger.error("close got " + e, e);
    }

    try {
//      logger.info(this.getClass() + " : closing db connection : " + dbConnection);
      dbConnection.close();
    } catch (Exception e) {
      logger.error("close got " + e, e);
    }
  }

  /**
   * Nuclear option.
   * <p>
   * Use with extreme caution.
   */
  public void dropAll() {
    if (dbConnection.hasAnyTables()) {
      dbConnection.dropAll();
    }
    if (dbConnection.hasAnyTables()) {
      logger.error("dropAll couldn't delete all the tables, now there are  " + getTables());
    }
  }

  public String getTables() {
    return dbConnection.getTables();
  }

  private int warns = 0;

  /**
   * TODOx : Fix this to get the right project first!
   * <p>
   * allow custom items to mask out non-custom items
   * Special code to mask out unit/chapter from database in userexercise table.
   * <p>
   * Must check update times to make sure we don't mask out a newer entry.
   *
   * @param projid
   * @param id
   * @return null if it's a bogus exercise - the unknown exercise
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExercise
   */
  public CommonExercise getCustomOrPredefExercise(int projid, int id) {
    CommonExercise toRet = getExercise(projid, id);
    if (toRet == null && id != userExerciseDAO.getUnknownExerciseID()) {
      // if (warns++ < 50)
      logger.warn("getCustomOrPredefExercise couldn't find exercise " + id + " vs unk ex " +
          userExerciseDAO.getUnknownExerciseID() +
          " in project #" + projid +
          " looking in user exercise table");
      toRet = getUserExerciseByExID(id);
    }
    if (toRet == null) {
      String message = "getCustomOrPredefExercise couldn't find exercise " + id + " (context?) in project #" + projid +
          " after looking in exercise table.";
      if (id == 0) {
        logger.info(message);
      } else {
        logger.error(message);
      }
    }

    return toRet;
  }

  /**
   * @param userToGender
   * @param userid
   * @param exid
   * @param project
   * @param idToMini
   * @return
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getUserToResults
   * @see SlickPhoneDAO#getPhoneReport
   */
  @Nullable
  public String getNativeAudio(Map<Integer, MiniUser.Gender> userToGender,
                               int userid,
                               int exid,
                               Project project,
                               Map<Integer, MiniUser> idToMini) {
    CommonExercise exercise = project.getExerciseByID(exid);

    if (exercise == null && exid != getUserExerciseDAO().getUnknownExerciseID()) {
      if (warns++ < 50 || warns % 100 == 0) {
        int projid = project.getID();
        logger.info("getCustomOrPredefExercise couldn't find exercise " + exid + " in project #" + projid + " looking in user exercise table");
      }

      exercise = getUserExerciseByExID(exid);
    }

    return audioDAO.getNativeAudio(userToGender, userid, exercise, project.getLanguage(), idToMini);
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

  /**
   * @param out
   * @param typeToSection
   * @param projectid
   * @param ensureAudioHelper
   * @throws Exception
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeZip(OutputStream out,
                       Map<String, Collection<String>> typeToSection,
                       int projectid,
                       AudioExportOptions options,
                       IEnsureAudioHelper ensureAudioHelper) throws Exception {
    Collection<CommonExercise> exercisesForSelectionState = typeToSection.isEmpty() ?
        getExercises(projectid) :
        getSectionHelper(projectid).getExercisesForSelectionState(typeToSection);

    if (!options.getSearch().isEmpty()) {
      TripleExercises<CommonExercise> exercisesForSearch = new Search<CommonExercise>(this, this)
          .getExercisesForSearch(
              options.getSearch(),
              exercisesForSelectionState, !options.isUserList() && typeToSection.isEmpty(), projectid);
      exercisesForSelectionState = exercisesForSearch.getByExercise();
    }

    Project project = getProject(projectid);

    String language = getLanguage(project);

//    if (!typeToSection.isEmpty()) {
    logger.info("writeZip for project " + projectid +
        " ensure audio for " + exercisesForSelectionState.size() +
        " exercises for " + language +
        " selection " + typeToSection);

    audioDAO.attachAudioToExercises(exercisesForSelectionState, language);
    ensureAudioHelper.ensureCompressedAudio(exercisesForSelectionState, language);
    //  }

    new AudioExport(getServerProps())
        .writeZip(out,
            typeToSection,
            getSectionHelper(projectid),
            exercisesForSelectionState,
            language,
            false,
            options,
            project.isEnglish());
  }

  @Override
  public String getLanguage(CommonExercise ex) {
    return getLanguage(ex.getProjectID());
  }

  @Override
  public String getLanguage(int projectid) {
    return getLanguage(getProject(projectid));
  }

  private String getLanguage(Project project) {
    return project == null ? "Unknown" : project.getLanguage();
  }

  @Override
  @Deprecated
  public String getLanguage() {
    return getOldLanguage(getServerProps());
  }

  private String getOldLanguage(ServerProperties serverProps) {
    return serverProps.getLanguage();
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
                                   int listid,
                                   int projectid,
                                   AudioExportOptions options) throws Exception {
    String language = getLanguage(projectid);
    if (listid == -1) return language + "_Unknown";

    UserList<CommonShell> userListByID = getUserListManager().getSimpleUserListByID(listid);

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
      Map<Integer, MiniUser> idToMini = new HashMap<>();
      for (CommonExercise ex : copyAsExercises) {
        userListManager.addAnnotations(ex);
        getAudioDAO().attachAudioToExercise(ex, language, idToMini);
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
          listid == COMMENT_MAGIC_ID,
          options);
    }
    return language + "_" + userListByID.getName();
  }

  /**
   * TODO : Expensive ?
   *
   * @param projectid
   * @see #getJSONExport
   */
  private void attachAllAudio(int projectid) {
    long then = System.currentTimeMillis();

    Collection<CommonExercise> exercises = getExercises(projectid);

    Project project = getProject(projectid);
    String language = project.getLanguage();

    getAudioDAO().attachAudioToExercises(exercises, language);

    {
      String name = project.getName();
      logger.info("attachAllAudio " + name + "/" + language +
          " took " + (System.currentTimeMillis() - then) +
          " millis to attachAllAudio to " + exercises.size() + " exercises");
    }
  }

  public String getUserListName(int listid) {
    UserList userListByID = getUserListManager().getSimpleUserListByID(listid);
    if (userListByID == null) {
      logger.error("getUserListName : can't find user list " + listid);
      return "_Unknown";
    } else {
      String language1 = getLanguage(userListByID.getProjid());
      return language1 + "_" + userListByID.getName();
    }
  }

  /**
   * @param listid
   * @param projectid
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   */
  @Override
  public UserList<CommonExercise> getUserListByIDExercises(int listid, int projectid) {
    boolean isNormalList = listid != COMMENT_MAGIC_ID;
    if (isNormalList) {
      Collection<Integer> exids = getUserListManager().getUserListExerciseJoinDAO().getExidsForList(listid);
      UserList<CommonExercise> list = getUserListManager().getUserListDAO().getList(listid);
      List<CommonExercise> exercises = new ArrayList<>();
      exids.forEach(exid -> {
        CommonExercise exercise = getExercise(projectid, exid);
        if (exercise != null) exercises.add(exercise);
      });
      list.setExercises(exercises);
      return list;
    } else {
      return getUserListManager().getCommentedListEx(projectid);
    }
  }

  @Override
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
   * Sends to recipient list.
   * Won't send unless it's Sunday morning...
   *
   * @see ProjectProperty#REPORT_LIST
   * @see LangTestDatabaseImpl#optionalInit
   */
  @Override
  public void doReport() {
    if (serverProps.isFirstHydra()) {
      if (isTodayAGoodDay()) {
        sendReports(getReport(), false, -1);
      } else {
        logger.debug("not sending email report since this is not monday...");
      }
      tryTomorrow();
    } else {
      logger.debug("doReport Host " + serverProps.getHostName() + " not generating a report.");
    }
  }

  private void tryTomorrow() {
    new Thread(() -> {
      try {
        Thread.sleep(DAY);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      doReport(); // try again later
    }).start();
  }

  private boolean isTodayAGoodDay() {
    return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == DAY_TO_SEND_REPORT;
  }

  /**
   * @param userID
   * @see LangTestDatabaseImpl#sendReport
   */
  @Override
  public void sendReport(int userID) {
    sendReports(getReport(), true, userID);
  }

  /**
   * @param report
   * @param forceSend
   * @param userID    if -1 uses report list property to determine recipients
   * @see #doReport
   * @see #sendReport
   */
  private void sendReports(IReport report, boolean forceSend, int userID) {
    MailSupport mailSupport = getMailSupport();
    List<ReportStats> stats = getReportStats(report, forceSend, mailSupport);

    {
      List<String> reportEmails = new ArrayList<>();
      List<String> receiverNames = new ArrayList<>();

      populateRecipients(userID, reportEmails, receiverNames);

      logger.info("sendReports to" +
          "\n\temails : " + reportEmails +
          "\n\tnames  : " + receiverNames
      );
      report.sendExcelViaEmail(mailSupport, reportEmails, receiverNames, stats, pathHelper);
    }
  }

  private void populateRecipients(int userID, List<String> reportEmails, List<String> receiverNames) {
    if (userID != -1) {
      User byID = userDAO.getByID(userID);
      if (byID == null) {
        logger.error("huh? can't find user " + userID + " in db?");
      } else {
//          logger.info("using user email " + byID.getEmail());
        reportEmails.add(byID.getEmail());
        receiverNames.add(byID.getFullName());
      }
    } else {
      reportEmails.addAll(projectDAO.getListProp(getDefaultProject(), ProjectProperty.REPORT_LIST));

      for (String email : reportEmails) {
        String trim = email.trim();
        String nameForEmail = userDAO.getNameForEmail(trim);
        if (nameForEmail == null) nameForEmail = trim;
        receiverNames.add(nameForEmail);
      }
    }
  }

  /**
   * @param report
   * @param forceSend
   * @param mailSupport
   * @return
   * @see #sendReports(IReport, boolean, int)
   */
  @NotNull
  private List<ReportStats> getReportStats(IReport report, boolean forceSend, MailSupport mailSupport) {
    List<ReportStats> stats = new ArrayList<>();
    List<String> reportEmails = getReportEmails();

    getProjects().forEach(project -> {
      stats.addAll(report
          .doReport(project.getID(),
              project.getLanguage(),
              project.getProject().name(),
              pathHelper,
              forceSend, true));
    });
    return stats;
  }

  private List<String> getReportEmails() {
    return projectDAO.getListProp(projectDAO.getDefault(), ProjectProperty.REPORT_LIST);
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail(), serverProps.getMailServer());
  }

  /**
   * @param year
   * @param jsonObject
   * @return
   * @see mitll.langtest.server.ScoreServlet#doGet(HttpServletRequest, HttpServletResponse)
   */
  @Override
  public String getReport(int year, JSONObject jsonObject) {
    List<ReportStats> reportStats = new ArrayList<>();
    return getReport().getAllReports(getProjectDAO().getAll(), jsonObject, year, reportStats);
  }

  public IReport getReport() {
    IUserDAO.ReportUsers reportUsers = userDAO.getReportUsers();
    return new Report(resultDAO, eventDAO, audioDAO,
        reportUsers.getAllUsers(), reportUsers.getDeviceUsers(), userProjectDAO.getUserToProject(),
        serverProps.getNPServer(), this.getLogAndNotify());
  }

  /**
   * FOR TESTING
   *
   * @return
   * @deprecated
   */
  public List<JSONObject> doReportAllYears() {
    return doReportForYear(-1);
  }

  /**
   * JUST FOR TESTING
   *
   * @deprecated JUST FOR TESTING
   */
  public List<JSONObject> doReportForYear(int year) {
    List<JSONObject> jsons = new ArrayList<>();
    IReport report = getReport();

    List<ReportStats> allReports = new ArrayList<>();
    Collection<Project> projects = getProjects();
    List<Project> copy = new ArrayList<>(projects);

    // sort by name...?
    copy.sort(Comparator.comparing(o -> o.getName().toLowerCase()));

    copy.forEach(project -> {
          try {
            jsons.add(report.writeReportToFile(new ReportStats(project.getProject(), year), pathHelper, allReports));
          } catch (IOException e) {
            logger.error("got " + e);
          }
        }
    );

    report.getSummaryReport(allReports, pathHelper);
    return jsons;
  }

/*  private File getSummaryReport(IReport report, List<ReportStats> allReports) {
    try {
      File file2 = report.getReportPathDLI(pathHelper, ".xlsx");
      new ReportToExcel(logAndNotify).toXLSX(allReports, new FileOutputStream(file2));
      logger.info("writeReportToFile wrote to " + file2.getAbsolutePath());
      return file2;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      logAndNotify(e);
      return null;
    }

  }*/

  /**
   * @param projID
   * @param resultID
   * @param asrScoreForAudio
   * @param isCorrect
   * @see mitll.langtest.server.services.ScoringServiceImpl#getPretestScore
   */
  @Override
  public void rememberScore(int projID, int resultID, PretestScore asrScoreForAudio, boolean isCorrect) {
    getAnswerDAO().changeAnswer(resultID, asrScoreForAudio.getHydecScore(), asrScoreForAudio.getProcessDur(), asrScoreForAudio.getJson(), isCorrect);
    recordWordAndPhone.recordWordAndPhoneInfo(projID, resultID, asrScoreForAudio);
  }

  /**
   * @param projID
   * @param answer
   * @param answerID
   * @see mitll.langtest.server.audio.AudioFileHelper#recordInResults(AudioContext, AnswerInfo.RecordingInfo, AudioCheck.ValidityAndDur, AudioAnswer)
   */

  @Override
  public void recordWordAndPhoneInfo(int projID, AudioAnswer answer, int answerID) {
    long then = System.currentTimeMillis();
    recordWordAndPhone.recordWordAndPhoneInfo(projID, answer, answerID);
    long now = System.currentTimeMillis();
    if (now - then > 50) {
      logger.info("recordWordAndPhoneInfo took " + (now - then) + " millis");
    }
  }

  /**
   * consider how to do context exercises better - they really aren't different than regular exercises...
   * Also, this can't handle multiple context sentences...
   *
   * @param projectid
   * @return
   * @see LangTestDatabaseImpl#getMaleFemaleProgress()
   */
  @Override
  public Map<String, Float> getMaleFemaleProgress(int projectid) {
    return getAudioDAO().getMaleFemaleProgress(projectid, getExercises(projectid));
  }

  @Override
  public LogAndNotify getLogAndNotify() {
    return logAndNotify;
  }

  @Override
  public IUserExerciseDAO getUserExerciseDAO() {
    return userExerciseDAO;
  }

  public IAnnotationDAO getAnnotationDAO() {
    return userListManager.getAnnotationDAO();
  }

  public IReviewedDAO getReviewedDAO() {
    return stateManager.getReviewedDAO();
  }

  public IReviewedDAO getSecondStateDAO() {
    return stateManager.getSecondStateDAO();
  }

  @Override
  public IProjectDAO getProjectDAO() {
    return projectDAO;
  }

  @Override
  public IUserProjectDAO getUserProjectDAO() {
    return userProjectDAO;
  }

  @Override
  public IAnswerDAO getAnswerDAO() {
    return answerDAO;
  }

  @Override
  public IEventDAO getEventDAO() {
    return eventDAO;
  }

  public IAudioDAO getAudioDAO() {
    return audioDAO;
  }

  @Override
  public IWordDAO getWordDAO() {
    return wordDAO;
  }

  @Override
  public IPhoneDAO<Phone> getPhoneDAO() {
    return phoneDAO;
  }

  @Override
  public IDLIClassJoinDAO getDliClassJoinDAO() {
    return dliClassJoinDAO;
  }

  @Override
  public IUserListManager getUserListManager() {
    return userListManager;
  }

  @Override
  public mitll.langtest.server.database.user.UserManagement getUserManagement() {
    return userManagement;
  }

  /**
   * @return
   */
  @Override
  public IUserSessionDAO getUserSessionDAO() {
    return userSessionDAO;
  }

  @Override
  public IUserSecurityManager getUserSecurityManager() {
    return userSecurityManager;
  }

  /**
   * @param userSecurityManager
   * @see LangTestDatabaseImpl#readProperties
   */
  @Override
  public void setUserSecurityManager(IUserSecurityManager userSecurityManager) {
    this.userSecurityManager = userSecurityManager;
  }

  @Override
  public IProjectManagement getProjectManagement() {
    return projectManagement;
  }

  public Database getDatabase() {
    return this;
  }

  public DominoExerciseDAO getDominoExerciseDAO() {
    return dominoExerciseDAO;
  }

  public boolean isHasValidDB() {
    return hasValidDB;
  }

  public String toString() {
    return "Database : " + this.getClass().toString();
  }
}
