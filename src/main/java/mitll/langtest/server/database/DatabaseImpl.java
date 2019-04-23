/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database;

import com.google.gson.JsonObject;
import mitll.langtest.client.project.ProjectEditForm;
import mitll.langtest.client.user.UserPassLogin;
import mitll.langtest.server.*;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.server.audio.AudioExportOptions;
import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.annotation.IAnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.audio.*;
import mitll.langtest.server.database.copy.CopyToPostgres;
import mitll.langtest.server.database.custom.IStateManager;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.custom.StateManager;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.dialog.*;
import mitll.langtest.server.database.dliclass.DLIClassDAO;
import mitll.langtest.server.database.dliclass.DLIClassJoinDAO;
import mitll.langtest.server.database.dliclass.IDLIClassDAO;
import mitll.langtest.server.database.dliclass.IDLIClassJoinDAO;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.image.IImageDAO;
import mitll.langtest.server.database.image.ImageDAO;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.server.database.instrumentation.SlickEventImpl;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.phone.Phone;
import mitll.langtest.server.database.phone.RecordWordAndPhone;
import mitll.langtest.server.database.phone.SlickPhoneDAO;
import mitll.langtest.server.database.project.*;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.database.refaudio.SlickRefResultDAO;
import mitll.langtest.server.database.report.ReportHelper;
import mitll.langtest.server.database.result.IAnswerDAO;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.SlickAnswerDAO;
import mitll.langtest.server.database.result.SlickResultDAO;
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
import mitll.langtest.server.domino.ProjectSync;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.services.UserServiceImpl;
import mitll.langtest.server.sorter.SimpleSorter;
import mitll.langtest.shared.analysis.PhoneReportRequest;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static mitll.langtest.server.PathHelper.ANSWERS;
import static mitll.langtest.server.database.custom.IUserListManager.COMMENT_MAGIC_ID;

public class DatabaseImpl implements Database, DatabaseServices {
  private static final Logger logger = LogManager.getLogger(DatabaseImpl.class);
  private static final String UNKNOWN = "unknown";
  public static final int IMPORT_PROJECT_ID = -100;
  private static final boolean ADD_DEFECTS = false;

  public static final String QUIZ = "QUIZ";
  private static final String UNIT = "Unit";
  public static final List<String> QUIZ_TYPES = Arrays.asList(QUIZ, UNIT);
  public static final String DRY_RUN = "Dry Run";
  public static final int MAX_PHONES = 7;
  private static final boolean TEST_SYNC = false;
  private static final int WARN_THRESH = 10;
  private static final String CRASH = "crash";

  private IUserDAO userDAO;
  private IUserSessionDAO userSessionDAO;
  private IResultDAO resultDAO;

  private IRefResultDAO refresultDAO;
  private IWordDAO wordDAO;
  private IPhoneDAO<Phone> phoneDAO;

  private IAudioDAO audioDAO;
  private ITrainingAudioDAO trainingAudioDAO;
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

  private ReportHelper reportHelper;
  private IUserSecurityManager userSecurityManager;
  private DominoExerciseDAO dominoExerciseDAO;
  private boolean hasValidDB = false;
  private IDialogDAO dialogDAO;
  private IDialogSessionDAO dialogSessionDAO;
  private IRelatedResultDAO relatedResultDAO;
  private IImageDAO imageDAO;
  private IPendingUserDAO pendingUserDAO;
  private IOOVDAO oovDAO;

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

    getMailSupport();
  }

  /**
   * @param pathHelper
   * @param servletContext
   * @see #DatabaseImpl
   */
  protected void connectToDatabases(PathHelper pathHelper, ServletContext servletContext) {
    long then = System.currentTimeMillis();
    // first connect to postgres

    setPostgresDBConnection();
    // then connect to mongo
    DominoUserDAOImpl dominoUserDAO = new DominoUserDAOImpl(this, servletContext);

    initializeDAOs(dominoUserDAO);

   // dominoUserDAO.setUserProjectDAO(getUserProjectDAO());

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
   * @param projID
   * @see DatabaseImpl#makeDAO
   * @see DatabaseImpl#DatabaseImpl
   * @see LangTestDatabaseImpl#init
   */
  public DatabaseImpl populateProjects(int projID) {
    if (projectManagement == null) {
      logger.info("populateProjects no project management yet...");
    } else {
      projectManagement.populateProjects(projID);
   //   setProjectManagement();
//
//      if (TEST_SYNC) {  // right now I can't run the test since I need mongo.. etc.
//        new TestSync(this);
//      }
    }
    return this;
  }

//  private void setProjectManagement() {
//    userDAO.setProjectManagement(getProjectManagement());
//  }

  /**
   * Slick db connection.
   */
  private DBConnection dbConnection;

  /**
   * Create or alter tables as needed.
   *
   * @see #DatabaseImpl(ServerProperties, PathHelper, LogAndNotify, ServletContext)
   */
  private void initializeDAOs(IUserDAO dominoUserDAO) {
    eventDAO = new SlickEventImpl(dbConnection);

    this.userDAO = dominoUserDAO;
    userProjectDAO = new UserProjectDAO(dbConnection);
    this.userSessionDAO = new SlickUserSessionDAOImpl(this, userProjectDAO, dbConnection);
    SlickAudioDAO slickAudioDAO = new SlickAudioDAO(this, dbConnection, this.userDAO);
    audioDAO = slickAudioDAO;
    trainingAudioDAO = new SlickTrainingAudioDAO(this, dbConnection, this.userDAO);
    resultDAO = new SlickResultDAO(this, dbConnection);
    answerDAO = new SlickAnswerDAO(this, dbConnection);

    refresultDAO = new SlickRefResultDAO(this, dbConnection);
    userExerciseDAO = new SlickUserExerciseDAO(this, dbConnection);
    projectDAO = new ProjectDAO(this, dbConnection, userExerciseDAO, this);
    wordDAO = new SlickWordDAO(this, dbConnection);
    phoneDAO = new SlickPhoneDAO(this, dbConnection);

    SlickUserListExerciseJoinDAO userListExerciseJoinDAO = new SlickUserListExerciseJoinDAO(this, dbConnection);
    IUserListDAO userListDAO = new SlickUserListDAO(this, dbConnection, this.userDAO, userExerciseDAO, projectDAO);
    IAnnotationDAO annotationDAO = new SlickAnnotationDAO(this, dbConnection, this.userDAO);

    IReviewedDAO reviewedDAO = new SlickReviewedDAO(this, dbConnection, true);
    IReviewedDAO secondStateDAO = new SlickReviewedDAO(this, dbConnection, false);

    stateManager = new StateManager(reviewedDAO, secondStateDAO);
    userListManager = new UserListManager(this.userDAO,
        userListDAO,
        userListExerciseJoinDAO,
        annotationDAO,
        stateManager,
        new SlickUserListExerciseVisitorDAO(this, dbConnection),
        this
    );

    dliClassDAO = new DLIClassDAO(dbConnection);
    dliClassJoinDAO = new DLIClassJoinDAO(dbConnection);
    pendingUserDAO = new PendingUserDAO(dbConnection);
    oovDAO = new OOVDAO(dbConnection);
    finalSetup(slickAudioDAO);
  }

  private void finalSetup(SlickAudioDAO slickAudioDAO) {
    makeDialogDAOs();
    createTables();

    new Thread(() -> {
      waitForDefaultUser();

      {
        int defaultUser = getUserDAO().getDefaultUser();
        imageDAO.ensureDefault(projectDAO.getDefault());
        logger.info("finalSetup : default user " + defaultUser);
        dialogDAO.ensureDefault(defaultUser);
      }
    }, "ensureDefaultUser").start();

    afterDAOSetup(slickAudioDAO);

    logger.info("finalSetup : tables = " + getTables());
  }

  public void waitForDefaultUser() {
    while (getUserDAO().getDefaultUser() < 1) {
      try {
        sleep(1000);
        logger.info("finalSetup ---> no default user yet.....");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Image DAO must go first - dialog default references it.
   */
  private void makeDialogDAOs() {
    imageDAO = new ImageDAO(this, dbConnection);
    dialogDAO = new DialogDAO(this, dbConnection, this);
    dialogSessionDAO = new DialogSessionDAO(this, dbConnection);
    relatedResultDAO = new RelatedResultDAO(this, dbConnection);
  }

  private void afterDAOSetup(SlickAudioDAO slickAudioDAO) {
    if (userDAO instanceof UserDAO) {
      userDAO.ensureDefaultUsers();
    }

    {
      int defaultProject = getDefaultProject();
      // make sure we have a template exercise

      slickAudioDAO.setDefaultResult(resultDAO.ensureDefault(defaultProject, userDAO.getBeforeLoginUser(),
          userExerciseDAO.ensureTemplateExercise(defaultProject)));
    }

    try {
      ((UserListManager) userListManager).setUserExerciseDAO(userExerciseDAO);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    recordWordAndPhone = new RecordWordAndPhone(wordDAO, phoneDAO);
    dominoExerciseDAO = new DominoExerciseDAO(userExerciseDAO);

    reportHelper = new ReportHelper(this, getProjectDAO(),
        getUserDAO(), pathHelper, getMailSupport());

    // trainingAudioDAO.checkAndAddForEachProject();
  }

  private int getDefaultProject() {
    int defaultProject = projectDAO.ensureDefaultProject(userDAO.getBeforeLoginUser());
    String propValue = projectDAO.getPropValue(defaultProject, ProjectProperty.REPORT_LIST.getName());

    if (propValue == null) {
      projectDAO.addOrUpdateProperty(defaultProject, ProjectProperty.REPORT_LIST,
          serverProps.getReportEmails()
              .toString()
              .replaceAll("\\[", "").replaceAll("]", ""));
      //    logger.info("default properties : " + projectDAO.getPropValue(defaultProject, ProjectProperty.REPORT_LIST.getName()));
    } else {
      //  logger.info("existing default properties : " + projectDAO.getPropValue(defaultProject, ProjectProperty.REPORT_LIST.toString()));
    }
    return defaultProject;
  }

  /**
   * For each old recording, find the old exercise id, then use that to figure out the matching exercise in the target project.
   *
   * @param oldID
   * @param newprojid
   * @param onDay     optionally filter on a day
   */
  public void updateRecordings(int oldID, int newprojid, Date onDay) {
    IProject fromProject = getIProject(oldID);
    IProject toProject = getIProject(newprojid);

    Map<Integer, String> idToOldIDFrom = new HashMap<>();
    Map<String, Integer> oldToIDTo = new HashMap<>();
    fromProject.getRawExercises().forEach(commonExercise -> idToOldIDFrom.put(commonExercise.getID(), commonExercise.getOldID()));
    toProject.getRawExercises().forEach(commonExercise -> oldToIDTo.put(commonExercise.getOldID(), commonExercise.getID()));

    boolean noDay = onDay == null;

    long start = noDay ? 0 : onDay.getTime() - 24 * 60 * 60 * 1000;
    long end = noDay ? 0 : onDay.getTime() + 24 * 60 * 60 * 1000;
    logger.info("updateRecordings : check for day " + onDay);

    Map<Integer, List<Integer>> oldExToResults = new HashMap<>();
    resultDAO
        .getMonitorResults(oldID)
        .forEach(monitorResult -> {
          List<Integer> resultsForEx = oldExToResults.computeIfAbsent(monitorResult.getExID(), k -> new ArrayList<>());

          if (noDay) {
            resultsForEx.add(monitorResult.getUniqueID());
          } else {
            if (monitorResult.getTimestamp() > start && monitorResult.getTimestamp() < end) {
              resultsForEx.add(monitorResult.getUniqueID());
              logger.info("updateRecordings : recording on day " + onDay + " : " + monitorResult);
            }
          }
        });

    List<Integer> ridsUpdated = new ArrayList<>();
    oldExToResults.forEach((exid, results) -> {
      String oldNPID = idToOldIDFrom.get(exid);
      if (oldNPID != null) {
        Integer idInTarget = oldToIDTo.get(oldNPID);

        if (idInTarget != null) {
          remapResultExerciseIDs(newprojid, results, idInTarget, ridsUpdated);
        }
      }
    });

    logger.info("updated " + ridsUpdated.size() + " results.");
  }

  private void remapResultExerciseIDs(int newprojid, List<Integer> results, Integer idInTarget, List<Integer> ridsUpdated) {
    results.forEach(rid -> {
      if (remapOneResult(newprojid, idInTarget, rid)) {
        ridsUpdated.add(rid);
      }
    });
  }

  private boolean remapOneResult(int newprojid, Integer idInTarget, Integer rid) {
    if (resultDAO.updateProjectAndEx(rid, newprojid, idInTarget)) {
      boolean b = wordDAO.updateProjectForRID(rid, newprojid);
      boolean b1 = phoneDAO.updateProjectForRID(rid, newprojid);

      if (!b) logger.warn("updateRecordings word :   didn't update rid " + rid + " and ex " + idInTarget);
      if (!b1) logger.warn("updateRecordings phone : didn't update rid " + rid + " and ex " + idInTarget);
      return true;
    } else {
      logger.warn("updateRecordings didn't update rid " + rid + " and ex " + idInTarget);
      return false;
    }
  }

  /**
   * This is how we merge pashto projects...
   * <p>
   * so only copy over the audio for the imported exercises...
   * <p>
   * only copy over ref result for the import exercises
   *
   * @param oldID
   * @param newprojid
   * @param isChinese
   * @see CopyToPostgres#merge
   */
  public void updateProject(int oldID, int newprojid, boolean isChinese) {
    List<Integer> justTheseIDs = new ArrayList<>();

    if (isChinese) {
      IUserExerciseDAO userExerciseDAO = getUserExerciseDAO();
      if (!userExerciseDAO.updateProjectChinese(oldID, newprojid, justTheseIDs)) {
        logger.warn("couldn't update chinese exercises dao to " + newprojid);
      } else {
        logger.info("updated exercises");
      }
    } else {
      if (!getUserExerciseDAO().updateProject(oldID, newprojid)) {
        logger.error("couldn't update exercise dao to " + newprojid);
      } else {
        logger.info("updated exercises");
      }
    }

    if (isChinese) {  // try to do some fancy setting of the result ids..?
      Map<String, Integer> tradOldToID = new HashMap<>();
      Map<String, Integer> simplifiedOldToID = new HashMap<>();

      Map<Integer, Integer> tradToSimpl = new HashMap<>();

      {
        IProject traditional = getIProject(oldID);
        traditional.getRawExercises().forEach(commonExercise -> tradOldToID.put(commonExercise.getOldID(), commonExercise.getID()));
        logger.info("traditional " + traditional);
        logger.info("trad " + tradOldToID.size());
      }
      {
        Project simplified = getProject(newprojid);
        simplified.getRawExercises().forEach(commonExercise -> {
          String oldID1 = commonExercise.getOldID();
          String[] split = oldID1.split("-");
          if (split.length == 2) oldID1 = split[1];
          simplifiedOldToID.put(oldID1, commonExercise.getID());
        });
        logger.info("simplified " + simplified);
        logger.info("simplified " + simplifiedOldToID.size());
      }
      tradOldToID.forEach((k, v) -> {
        Integer newID = simplifiedOldToID.get(k);
        if (newID == null) {
          logger.warn("updateProject no matching exercise for old id " + k);
        } else {
          tradToSimpl.put(v, newID);
        }
      });

      // take all results for basic course items, and move them to point to simple exercise ids.
      logger.info("trad->simpl size " + tradToSimpl.size());
      List<MonitorResult> tradResults = resultDAO.getMonitorResults(oldID);
      logger.info("found " + tradResults.size() + " in project " + oldID);
      TreeSet<Integer> remapped = new TreeSet<>();
      TreeSet<Integer> unmapped = new TreeSet<>();
      tradResults.forEach(monitorResult -> {
        int exID = monitorResult.getExID();

//        if (justTheseIDs.contains(exID)) {
//          logger.info("keep id " + exID + " " + monitorResult.getForeignText() + " ");
//        } else {
        Integer simpleID = tradToSimpl.get(exID);

        if (simpleID == null) {
          logger.warn("updateProject no ex " + exID + " in trad for " + monitorResult + "?");
          unmapped.add(exID);
        } else {
          remapOneResult(newprojid, simpleID, monitorResult.getUniqueID());
          remapped.add(monitorResult.getUniqueID());
        }
        //    }
      });
      logger.info("trad->simpl remapped " + remapped.size());
      logger.info("trad->simpl unmapped " + unmapped.size());

    } else {
      // TODO : remap exercise references from old to new for the non-custom ids
      if (!resultDAO.updateProject(oldID, newprojid)) {
        logger.error("couldn't update result dao to " + newprojid);
      } else {
        logger.info("updated results");
      }
    }

    if (isChinese) {
      // only copy over the custom exercises for integrated chinese 2.
      logger.info("update audio dao to " + newprojid + " for " + justTheseIDs.size() + " exercises");

      if (!((SlickAudioDAO) audioDAO).updateProjectIn(oldID, newprojid, justTheseIDs)) {
        logger.error("couldn't update audio dao to " + newprojid);
      } else {
        logger.info("updated audio");
      }
    } else {
      // TODO : only copy over the audio for the custom items...
      if (!audioDAO.updateProject(oldID, newprojid)) {
        logger.error("couldn't update audio dao to " + newprojid);
      } else {
        logger.info("updated audio");
      }
    }

    if (!wordDAO.updateProject(oldID, newprojid)) {
      logger.error("couldn't update word dao to " + newprojid);
    } else {
      logger.info("updated word");
    }

    if (!phoneDAO.updateProject(oldID, newprojid)) {
      logger.error("couldn't update phone dao to " + newprojid);
    } else {
      logger.info("updated phones");
    }

    if (!getUserListManager().updateProject(oldID, newprojid)) {
      logger.error("couldn't update user list dao to " + newprojid);
    } else {
      logger.info("updated user lists.");
    }

    if (!isChinese) {
      if (!refresultDAO.updateProject(oldID, newprojid)) {
        logger.error("couldn't update ref result dao to " + newprojid);
      } else {
        logger.info("updated ref results");
      }
    }

    if (!userProjectDAO.updateProject(oldID, newprojid)) {
      logger.error("couldn't update user->project dao to " + newprojid);
    } else {
      logger.info("updated user->project");
    }
  }

  private void setPostgresDBConnection() {
    dbConnection = getDbConnection();
  }

  /**
   * @return
   * @see #initializeDAOs
   */
  private DBConnection getDbConnection() {
    String dbConfig = getDbConfig();
    logger.info("getDbConnection using " + serverProps.getDBConfig() + " : " + dbConnection);

    DBConnection dbConnection = new DBConnection(dbConfig);
    dbConnection.addColumn();
//    logger.info("getDbConnection using " + serverProps.getDBConfig() + " : " + dbConnection);
    return dbConnection;
  }

  public String getDbConfig() {
    return serverProps.getDBConfig();
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

  /**
   * ONLY FOR TESTING
   *
   * @param lessonPlanFileOnlyForImport
   * @return
   */
  public DatabaseImpl setInstallPath(String lessonPlanFileOnlyForImport) {
    return setInstallPath(lessonPlanFileOnlyForImport, null, true);
  }

  public DatabaseImpl setInstallPath(String lessonPlanFileOnlyForImport, boolean loadAll) {
    return setInstallPath(lessonPlanFileOnlyForImport, null, loadAll);
  }

  /**
   * @param lessonPlanFileOnlyForImport
   * @param servletContext
   * @param loadAll
   * @see mitll.langtest.server.LangTestDatabaseImpl#setInstallPath
   */
  @Override
  public DatabaseImpl setInstallPath(String lessonPlanFileOnlyForImport, ServletContext servletContext, boolean loadAll) {
    this.projectManagement = new ProjectManagement(pathHelper, serverProps, getLogAndNotify(), this, servletContext);
    makeDAO(lessonPlanFileOnlyForImport, loadAll);
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
    } else {
      getExercises(projectid, false);
      return getSectionHelperForProject(projectid);
    }
  }

  @Override
  public ISection<IDialog> getDialogSectionHelper(int projectid) {
    if (projectid == -1) {
      return null;
    } else {
      return getDialogSectionHelperForProject(projectid);
    }
  }

  @Nullable
  private ISection<CommonExercise> getSectionHelperForProject(int projectid) {
    Project project = getProject(projectid);
    if (project == null) {
      logger.error("getSectionHelper huh? couldn't find project with id " + projectid);
      return null;
    } else {
      return project.getSectionHelper();
    }
  }

  @Nullable
  private ISection<IDialog> getDialogSectionHelperForProject(int projectid) {
    Project project = getProject(projectid);
    if (project == null) {
      logger.error("getSectionHelper huh? couldn't find project with id " + projectid);
      return null;
    } else {
      return project.getDialogSectionHelper();
    }
  }

  public Collection<String> getTypeOrder(int projectid) {
    ISection<CommonExercise> sectionHelper = getSectionHelper(projectid);
    if (sectionHelper == null) {
      logger.warn("getTypeOrder no section helper for " + this + " and " + projectid);
    }
    Collection<String> types = (sectionHelper == null) ? Collections.emptyList() : sectionHelper.getTypeOrder();

//    if (types.isEmpty()) {
//      logger.error("getTypeOrder empty type order : " + projectid + " = " + types);
//    }
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
   * DUDE: super expensive...
   * <p>
   * SUPER EXPENSIVE the first time
   *
   * @param projectid
   * @return
   * @seex ScoreServlet#getJSONForExercises
   * @see ScoreServlet#getJsonNestedChapters
   */
  public JsonExport getJSONExport(int projectid) {
    Project project = getProject(projectid);
    if (project == null) {
      logger.warn("asking for unknown project " + projectid);
      return new JsonExport(null, Collections.emptyList(), false, null);
    } else {
      if (project.getJsonExport() == null) {
        getExercises(projectid, false);
        JsonExport jsonExport = new JsonExport(
            getSectionHelper(projectid),
            serverProps.getPreferredVoices(),
            getLanguageEnum(projectid) == Language.ENGLISH,
            project.getAudioFileHelper()
        );

        attachAllAudio(projectid);
        project.setJsonExport(jsonExport);
      }

      return project.getJsonExport();
    }
  }

  /**
   * JUST FOR TESTING
   *
   * @return
   */
  @Deprecated
  public Collection<CommonExercise> getExercises() {
    return getExercises(-1, false);
  }

  /**
   * exercises are in the context of a project
   *
   * @param projectid
   * @param onlyOne
   * @return
   * @see Project#buildExerciseTrie
   */
  @Override
  public List<CommonExercise> getExercises(int projectid, boolean onlyOne) {
    return projectManagement.getExercises(projectid, onlyOne);
  }

  @Override
  public ExerciseDAO<CommonExercise> getExerciseDAO(int projectid) {
    Project project = getProject(projectid);
    // logger.debug("getExerciseDAO " + projectid + " found project " + project);
    ExerciseDAO<CommonExercise> exerciseDAO = project.getExerciseDAO();
    // logger.debug("getExerciseDAO " + projectid + " found exercise dao " + exerciseDAO);
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
   * @see mitll.langtest.server.rest.RestUserManagement#tryToLogin
   * @see mitll.langtest.server.services.OpenUserServiceImpl#setProject
   */
  @Override
  public void rememberUsersCurrentProject(int userid, int projectid) {
    logger.info("rememberUsersCurrentProject user " + userid + " -> " + projectid);
    getUserProjectDAO().upsert(userid, projectid);
    getUserListManager().createFavorites(userid, projectid);
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
  private void setStartupInfo(User userWhere, int projid) {
    projectManagement.setStartupInfo(userWhere, projid);
  }

  @Override
  public IStateManager getStateManager() {
    return stateManager;
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
   * @param loadAll
   * @see DatabaseServices#setInstallPath(String, ServletContext, boolean)
   */
  private void makeDAO(String lessonPlanFileOnlyForImport, boolean loadAll) {
    //logger.info("makeDAO - " + lessonPlanFileOnlyForImport);
    if (userManagement == null) {
      synchronized (this) {
        //  logger.info("makeDAO makeExerciseDAO -- " + lessonPlanFileOnlyForImport);
        makeExerciseDAO(lessonPlanFileOnlyForImport);

        if (!serverProps.useH2()) {
          if (loadAll) {
            populateProjects(-1);
          }
//          else {
//            setProjectManagement();
//          }
          //    logger.info("set exercise dao " + exerciseDAO + " on " + userExerciseDAO);
          if (projectManagement.getProjects().isEmpty()) {
            logger.warn("\nmakeDAO no projects loaded yet...?");
          }
        }

        if (serverProps.useH2()) {
          userExerciseDAO.setExerciseDAO(projectManagement.setDependencies());
        }

        trainingAudioDAO.checkAndAddAudio(projectManagement.getProjects(), audioDAO);
      }
      userManagement = new mitll.langtest.server.database.user.UserManagement(userDAO);
    }
  }

  /**
   * For when a new project is added or changes state.
   *
   * @param project
   * @param forceReload
   * @return number of exercises in the project
   * @see ProjectEditForm#updateProject
   * @see mitll.langtest.server.services.ProjectServiceImpl#update
   * @see mitll.langtest.server.services.ProjectServiceImpl#configureAndRefresh
   */
  @Override
  public int configureProject(Project project, boolean forceReload) {
    return projectManagement.configureProject(project, false, forceReload);
  }

  /**
   * Here to support import from old individual sites for CopyToPostgres
   *
   * @param lessonPlanFile
   * @see #makeDAO(String, boolean)
   */
  private void makeExerciseDAO(String lessonPlanFile) {
//    logger.info("makeExerciseDAO - " + lessonPlanFile + " : use h2 = " + serverProps.useH2());
    if (serverProps.useH2()) {
      logger.info("makeExerciseDAO reading from excel sheet " + lessonPlanFile);
      projectManagement.addSingleProject(new ExcelImport(lessonPlanFile, getServerProps(), userListManager, ADD_DEFECTS));
    } else {
//      logger.info("*not* making import project");
    }
  }

  public IProject getIProject(int projID) {
    return getProject(projID);
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
      return getProject(projectid, false);
    }
  }

  public Project getProject(int projectid, boolean onlyOne) {
    ensureProjectManagement(!onlyOne);
    return projectManagement.getProject(projectid, onlyOne);
  }

  public void ensureProjectManagement(boolean loadAll) {
    if (projectManagement == null) {
      setInstallPath("", null, loadAll);
    }
  }

  public Project getProjectByName(String name) {
    return projectManagement.getProjectByName(name);
  }

  public Collection<Project> getProjects() {
    return projectManagement.getProjects();
  }

  public Map<String, Integer> getNpToDomino(int dominoProjectID) {
    return projectManagement.getNpToDomino(dominoProjectID);
  }

  /**
   * TODO : why is this so confusing???
   *
   * @param clientExercise
   * @param keepAudio
   * @seex mitll.langtest.server.services.ListServiceImpl#editItem
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#editItem
   * @see mitll.langtest.server.services.AudioServiceImpl#editItem
   */
  @Override
  public void editItem(ClientExercise clientExercise, boolean keepAudio) {
    CommonExercise userExercise = clientExercise.asCommon();
    int id = userExercise.getID();
    logger.info("editItem exercise #" + id +
        " keep audio " + keepAudio +
        " mediaDir : " + getServerProps().getMediaDir() +
        " audio " + userExercise.getAudioAttributes());

    int projectID = userExercise.getProjectID();
    if (projectID < 0) {
      logger.warn("editItem huh? no project id on user exer " + userExercise);
    }
    // create if doesn't exist
    getUserListManager().editItem(userExercise, getTypeOrder(projectID));

    // Set<AudioAttribute> originalAudio = new HashSet<>(userExercise.getAudioAttributes());
    Set<AudioAttribute> defectAudio = audioDAO.getAndMarkDefects(userExercise, userExercise.getFieldToAnnotation());
    /*
    if (!originalAudio.isEmpty()) {
      logger.debug("editItem originally had " + originalAudio.size() + " attributes, and " + defectAudio.size() + " defectAudio");
    }*/

    if (userExercise.isPredefined()) {
      clearDefects(defectAudio, userExercise);
      getSectionHelper(projectID).refreshExercise(userExercise);
    }
    //return userExercise;
  }

  private void clearDefects(Set<AudioAttribute> defects, CommonExercise exercise) {
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
   * @param projid
   * @param userid
   * @param typeToSection
   * @param forContext
   * @param sortByLatestScore
   * @return
   * @paramx collator
   * @see mitll.langtest.server.ScoreServlet#getChapterHistory
   */
  public JsonObject getJsonScoreHistory(int projid,
                                        int userid,
                                        Map<String, Collection<String>> typeToSection,
                                        boolean forContext,
                                        boolean sortByLatestScore, SimpleSorter sorter) {

    if (projid == -1) {
      projid = projectForUser(userid);
    }
    if (projid == -1) {
      return new JsonObject();
    } else {
      JsonSupport jsonSupportForProject = getJsonSupportForProject(projid);
      // TODO :  maybe if the project is retired...?  how to handle this on iOS???
      return jsonSupportForProject == null ? new JsonObject() : jsonSupportForProject.getJsonScoreHistory(userid, typeToSection, forContext);
    }
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
  public JsonObject getJsonPhoneReport(PhoneReportRequest request) {
    int projid = request.getProjid();
    if (projid == -1) {
      projid = projectForUser(request.getUserid());
    }
    return getJsonSupportForProject(projid).getJsonPhoneReport(request);
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
        projID = projectDAO.getDefault(); // default project
      }
    }

    if (projID == -1) {
      logger.warn("logEvent no projects loaded yet ???");
      return false;
    } else {
      int realExid = -1;
      try {
        if (!exid.equalsIgnoreCase("N/A")) {
          realExid = Integer.parseInt(exid);
        }
      } catch (NumberFormatException e) {
        //logger.warn("couldn't parse exid '" + exid + "'");
        //e.printStackTrace();
      }
      if (!projectManagement.exists(projID)) projID = projectDAO.getDefault();
      Event event = new Event(id, widgetType, exid, context, userid, System.currentTimeMillis(), device, realExid);
      if (id.equalsIgnoreCase(CRASH)) {
        logger.error("got " + context);
      } else if (id.equalsIgnoreCase("WARNING")) {
        logger.warn("logEvent " + event);
      } else {
//        logger.info("logEvent id " + id + " : " + event);
      }
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
    // logger.info("createTables create slick tables - has " + getTables());
    List<String> created = new ArrayList<>();

    List<String> known = dbConnection.getJavaListOfTables();
    {
      Arrays.asList(
          getProjectDAO(),
          userExerciseDAO,
          userExerciseDAO.getRelatedExercise(),
          userExerciseDAO.getRelatedCoreExercise(),
          userExerciseDAO.getExerciseAttributeDAO(),
          userExerciseDAO.getExerciseAttributeJoin(),
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
          dliClassJoinDAO,
          dialogDAO,
          dialogDAO.getDialogAttributeJoinHelper(),
          dialogSessionDAO,
          relatedResultDAO,
          imageDAO,
          pendingUserDAO,
          oovDAO,
          trainingAudioDAO
      ).forEach(idao -> {
        if (createIfNotThere(idao, known)) {
          created.add(idao.getName());
        }
      });
    }

    userListManager.createTables(dbConnection, created);

    if (!created.isEmpty()) {
      logger.info("createTables created slick tables          : " + created);
      logger.info("createTables after create slick tables - has " + getTables());
    }
  }

  private boolean createIfNotThere(IDAO slickUserDAO, List<String> known) {
    String name = slickUserDAO.getName();
    if (!known.contains(name)) {
      logger.info("createIfNotThere create " + name);
      slickUserDAO.createTable();
      return true;
    } else {
//      logger.info("createIfNotThere has table " + name);
      return false;
    }
  }

  /**
   * TODO : put back in limit without breaking reporting
   *
   * @param projid
   * @return
   * @see mitll.langtest.server.services.ResultServiceImpl#getResults(int, Map, int, String)
   * @see DownloadServlet#returnSpreadsheet
   */
  @Override
  public Collection<MonitorResult> getMonitorResults(int projid) {
    List<MonitorResult> monitorResults = resultDAO.getMonitorResultsKnownExercises(projid);

    logger.info("getMonitorResults got back            " + monitorResults.size() + " for project " + projid);
    List<MonitorResult> monitorResultsWithText = getMonitorResultsWithText(monitorResults, projid);
    logger.info("getMonitorResults got back after join " + monitorResultsWithText.size() + " for project " + projid);

    return monitorResultsWithText;
  }

  /**
   * @param monitorResults
   * @param projid
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getResults
   * @see DatabaseServices#getMonitorResults(int)
   */
  @Override
  public List<MonitorResult> getMonitorResultsWithText(List<MonitorResult> monitorResults, int projid) {
    Map<Integer, CommonExercise> idToExerciseMap = getIdToExerciseMap(projid);
    logger.info("getMonitorResultsWithText Got size = " + idToExerciseMap.size() + " id->ex map");
    addUnitAndChapterToResults(monitorResults, idToExerciseMap, projid);
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
                                          Map<Integer, CommonExercise> join, int projid) {
    int n = 0;
    int m = 0;
    Set<Integer> unknownIDs = new HashSet<>();
    for (MonitorResult result : monitorResults) {
      int id = result.getExID();

      CommonExercise exercise = join.get(id);
      if (exercise == null) {
        exercise = getExercise(projid, id);
      }

      if (exercise == null) {
        if (n < WARN_THRESH) {
          logger.warn("addUnitAndChapterToResults : for exid " + id + " couldn't find " + result);
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
    logger.info("addUnitAndChapterToResults joined with " + m + " results");
  }

  /**
   * @param projectid
   * @return
   * @see #getMonitorResultsWithText
   */
  private Map<Integer, CommonExercise> getIdToExerciseMap(int projectid) {
    Map<Integer, CommonExercise> join = new HashMap<>();
    getExercises(projectid, false).forEach(exercise -> join.put(exercise.getID(), exercise));

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

    if (mailSupport != null) {
      mailSupport.stopHeartbeat();
    }
    try {

      //  logger.info(this.getClass() + " : closing db connection : " + dbConnection);
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
   * @param projid
   * @param id
   * @return null if it's a bogus exercise - the unknown exercise
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExercise
   */
  public CommonExercise getCustomOrPredefExercise(int projid, int id) {
    CommonExercise toRet = getExercise(projid, id);
/*    if (toRet == null && id != userExerciseDAO.getUnknownExerciseID()) {
      // if (warns++ < 50)
      logger.warn("getCustomOrPredefExercise couldn't find exercise " + id + " vs unk ex " +
          userExerciseDAO.getUnknownExerciseID() +
          " in project #" + projid +
          " looking in user exercise table");
      toRet = getUserExerciseByExID(id);
    }*/

    if (toRet == null) {
      logger.info("getCustomOrPredefExercise can't find ex in project #" + projid +
          ", so try all projects for exercise #" + id);
      toRet = projectManagement.getExercise(id);
    }

    if (toRet == null) {
      String message = "getCustomOrPredefExercise couldn't find exercise " + id + " (context?) in project #" + projid +
          " after looking in exercise table.";

      if (id == 0) {
        logger.info(message);
      } else {
        logger.warn(message);
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
  public NativeAudioResult getNativeAudio(Map<Integer, MiniUser.Gender> userToGender,
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

      boolean swap = projectDAO.getDefPropValue(project.getID(), ProjectProperty.SWAP_PRIMARY_AND_ALT).equalsIgnoreCase("TRUE");
      exercise = getUserExerciseByExID(exid, swap);
    }

    String nativeAudio = audioDAO.getNativeAudio(userToGender, userid, exercise, project.getLanguageEnum(), idToMini, project.getAudioFileHelper().getSmallVocabDecoder());
    return new NativeAudioResult(nativeAudio, exercise != null && exercise.isContext());
    // return nativeAudio;
  }

  /**
   * Ask the database for the user exercise.
   *
   * @param id
   * @param shouldSwap
   * @return
   * @see #editItem
   * @see #getCustomOrPredefExercise(int, int)
   */
  private CommonExercise getUserExerciseByExID(int id, boolean shouldSwap) {
    return userExerciseDAO.getByExID(id, shouldSwap);
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
/*  public void writeZip(OutputStream out,
                       Map<String, Collection<String>> typeToSection,
                       int projectid,
                       AudioExportOptions options,
                       IEnsureAudioHelper ensureAudioHelper) throws Exception {
    Collection<CommonExercise> exercisesForSelectionState = typeToSection.isEmpty() ?
        getExercises(projectid, false) :
        getSectionHelper(projectid).getExercisesForSelectionState(typeToSection);

    if (!options.getSearch().isEmpty()) {
      TripleExercises<CommonExercise> exercisesForSearch = new Search<>(this)
          .getExercisesForSearch(
              options.getSearch(),
              exercisesForSelectionState, !options.isUserList() && typeToSection.isEmpty(), projectid, true);
      exercisesForSelectionState = exercisesForSearch.getByExercise();
    }

    Project project = getProject(projectid);

    String language = getLanguage(project);

//    if (!typeToSection.isEmpty()) {
    logger.info("writeZip for project " + projectid +
        " ensure audio for " + exercisesForSelectionState.size() +
        " exercises for " + language +
        " selection " + typeToSection);

    if (options.getIncludeAudio()) {
      Language languageEnum = project.getLanguageEnum();
      audioDAO.attachAudioToExercises(exercisesForSelectionState, languageEnum, projectid);
      ensureAudioHelper.ensureCompressedAudio(exercisesForSelectionState, languageEnum);
    }

    new AudioExport(getServerProps(), pathHelper.getContext())
        .writeZip(out,
            typeToSection,
            getSectionHelper(projectid),
            exercisesForSelectionState,
            language,
            false,
            options,
            project.isEnglish());
  }*/

  @Override
  public String getLanguage(CommonExercise ex) {
    return getLanguage(ex.getProjectID());
  }

  @Override
  public Language getLanguageEnum(CommonExercise ex) {
    return getLanguageEnum(ex.getProjectID());
  }

  @Override
  public String getLanguage(int projectid) {
    return getLanguage(getProject(projectid));
  }

  public Language getLanguageEnum(int projectid) {
    Project project = getProject(projectid);
    return project == null ? Language.UNKNOWN : project.getLanguageEnum();
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
   * JUST FOR TESTING?
   *
   * @param request
   * @param projid
   * @param userid
   * @return
   */
  public FilterResponse getTypeToValues(FilterRequest request, int projid, int userid) {
    return getFilterResponseHelper().getTypeToValues(request, projid, userid);
  }

  public List<CommonExercise> filterExercises(ExerciseListRequest request,
                                              List<CommonExercise> exercises,
                                              int projid) {
    return getFilterResponseHelper().filter(request, exercises, projid);
  }

  public IResponseFilter getFilterResponseHelper() {
    return new FilterResponseHelper(this);
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
    Language language = getLanguageEnum(projectid);
    if (listid == -1) return language + "_Unknown";

    // logger.info("writeUserList " + listid + " in " + projectid);

    UserList<CommonShell> userListByID = getUserListManager().getSimpleUserListByID(listid);
    String name = "";
    if (userListByID == null) {
      logger.error("huh? can't find user list " + listid);
      return language + "_Unknown";
    } else {
      //logger.debug("writing contents of " + userListByID);
      name = userListByID.getName();
      boolean isCommentList = listid == COMMENT_MAGIC_ID;
      doAudioExport(out, isCommentList, projectid, options, language, name, userListByID.getExercises());
    }
    return language + "_" + name;
  }

  public String writeDialogItems(OutputStream out,
                                 int dialogID,
                                 int projectid,
                                 AudioExportOptions options) throws Exception {
    Language language = getLanguageEnum(projectid);
    if (dialogID == -1) return language + "_Unknown";
    String name = "";
    List<IDialog> collect = getProject(projectid).getDialogs().stream().filter(d -> d.getID() == dialogID).collect(Collectors.toList());
    if (collect.isEmpty()) {
      logger.error("huh? can't find user list " + dialogID);
      return language + "_Unknown";
    } else {
      IDialog iDialog = collect.get(0);
      name = language + "_" + (iDialog.getEnglish().isEmpty() ? iDialog.getForeignLanguage() : iDialog.getEnglish());
      doAudioExport(out, false, projectid, options, language, name, iDialog.getBothExercisesAndCore());
    }

    return language + "_" + name;
  }

  private <T extends CommonShell> void doAudioExport(OutputStream out, boolean isCommentList, int projectid, AudioExportOptions options,
                                                     Language language, String name, List<T> exercises) throws Exception {

    long then = System.currentTimeMillis();
    List<CommonExercise> copyAsExercises = new ArrayList<>();
    for (CommonShell ex : exercises) {
      CommonExercise customOrPredefExercise = getCustomOrPredefExercise(projectid, ex.getID());
      if (customOrPredefExercise != null) {
        copyAsExercises.add(customOrPredefExercise);
      } else logger.warn("writeUserListAudio no exercise found = " + ex.getID());
    }
    Map<Integer, MiniUser> idToMini = new HashMap<>();
    Project project = getProject(projectid);
    SmallVocabDecoder smallVocabDecoder = project == null ? null : project.getAudioFileHelper().getSmallVocabDecoder();
    for (CommonExercise ex : copyAsExercises) {
      userListManager.addAnnotations(ex);
      if (smallVocabDecoder != null) {
        getAudioDAO().attachAudioToExercise(ex, language, idToMini, smallVocabDecoder);
      }
    }
    long now = System.currentTimeMillis();
    logger.debug("\nTook " + (now - then) + " millis to annotate and attach.");
    new AudioExport(getServerProps(), pathHelper.getContext()).writeUserListAudio(
        out,
        name,
        getSectionHelper(projectid),
        copyAsExercises,
        language.getLanguage(),
        isCommentList,
        options);
  }

  /**
   * TODO : Expensive ?
   *
   * @param projectid
   * @see #getJSONExport
   */
  private void attachAllAudio(int projectid) {
    long then = System.currentTimeMillis();

    Collection<CommonExercise> exercises = getExercises(projectid, false);

    Project project = getProject(projectid);
    String language = project.getLanguage();

    getAudioDAO().attachAudioToExercises(exercises, project.getLanguageEnum(), project.getID());

    {
      logger.info("attachAllAudio " + project.getName() + "/" + language +
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
      return getLanguage(userListByID.getProjid()) + "_" + userListByID.getName();
    }
  }

  /**
   * Sort by english if normal list
   * <p>
   * TODO : when would we want the commented list???
   *
   * @param listid
   * @param projectid
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   */
  @Override
  public UserList<CommonExercise> getUserListByIDExercises(int listid, int projectid) {
    boolean isNormalList = listid != COMMENT_MAGIC_ID;
    if (isNormalList) {
      UserList<CommonExercise> list = getUserListManager().getUserListDAO().getList(listid);
      if (list != null) {  // could be null if we make it private ?
        Collection<Integer> exids = getUserListManager().getUserListExerciseJoinDAO().getExidsForList(listid);

        logger.info("getUserListByIDExercises list " + listid + " got " + exids.size() + " : " + exids);
        list.setExercises(getCommonExercisesForList(projectid, exids, list.getName()));
      }
      return list;
    } else {
      logger.error("getUserListByIDExercises returning commented list? " + listid + " vs " + COMMENT_MAGIC_ID);
      //return getUserListManager().getCommentedListEx(projectid, false);
      return new UserList<>();
    }
  }

  @NotNull
  private List<CommonExercise> getCommonExercisesForList(int projectid, Collection<Integer> exids, String listName) {
    //Collection<String> typeOrder = getTypeOrder(projectid);
    //String first = typeOrder.isEmpty() ? "" : typeOrder.iterator().next();
    List<CommonExercise> exercises = new ArrayList<>();
    exids.forEach(exid -> {
      CommonExercise exercise = getExercise(projectid, exid);
      if (exercise != null) {
        exercises.add(exercise);
//        if (exercise.getUnitToValue().isEmpty()) {
//          exercise.getUnitToValue().put(first, listName);
//        }
        //else logger.info("getCommonExercisesForList for ex "  +exercise.getID() + " " + exercise.getForeignLanguage() + " unit->value " + exercise.getUnitToValue());
      }
    });
    exercises.sort((o1, o2) -> o1.getEnglish().compareToIgnoreCase(o2.getEnglish()));
    return exercises;
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
    return new AudioExport(getServerProps(), pathHelper.getContext()).getPrefix(getSectionHelper(projectid), typeToSection);
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
      if (reportHelper.isTodayAGoodDay()) {
        sendReports();
      } else {
        logger.info("doReport : not sending email report since this is not the day to send them...");
      }
    } else {
      // logger.info("doReport host " + serverProps.getHostName() + " not generating a report.");
    }
  }

  public void sendReports() {
    reportHelper.sendReports(getReport());
  }

  /**
   * @param userID
   * @see LangTestDatabaseImpl#sendReport
   */
  @Override
  public void sendReport(int userID) {
    reportHelper.sendReport(getReport(), userID);
  }

  private MailSupport mailSupport;

  private MailSupport getMailSupport() {
    if (mailSupport == null) {
      mailSupport = new MailSupport(serverProps);
      mailSupport.addHeartbeat();
    }
    return mailSupport;
  }

  /**
   * @param year
   * @param jsonObject
   * @return
   * @see mitll.langtest.server.ScoreServlet#doGet(HttpServletRequest, HttpServletResponse)
   */
  @Override
  public String getReport(int year, JsonObject jsonObject) {
    return getReport().getAllReports(getProjectDAO().getAll(), jsonObject, year, new ArrayList<>());
  }

  public IReport getReport() {
    IUserDAO.ReportUsers reportUsers = userDAO.getReportUsers();
    return new Report(resultDAO, eventDAO, audioDAO,
        reportUsers.getAllUsers(),
        reportUsers.getDeviceUsers(),
        userProjectDAO.getUserToProject(),
        this.getLogAndNotify());
  }

  /**
   * FOR TESTING
   *
   * @return
   * @deprecated
   */
//  public List<JsonObject> doReportAllYears() {
//    return doReportForYear(-1);
//  }

  /**
   * JUST FOR TESTING
   *
   * @deprecated JUST FOR TESTING
   */
  /*public List<JsonObject> doReportForYear(int year) {
    List<JsonObject> jsons = new ArrayList<>();
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
    getAnswerDAO().changeAnswer(resultID, asrScoreForAudio.getOverallScore(), asrScoreForAudio.getProcessDur(), asrScoreForAudio.getJson(), isCorrect);
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

  public RecordWordAndPhone getRecordWordAndPhone() {
    return recordWordAndPhone;
  }

  /**
   * @param language
   * @param path
   * @return
   * @see mitll.langtest.server.database.phone.BasePhoneDAO#getAndRememberWordAndScore
   */
  @Override
  public String getWebPageAudioRef(String language, String path) {
    return getWebPageAudioRefWithPrefix(getRelPrefix(language), path);
  }

  @Override
  public String getWebPageAudioRefWithPrefix(String relPrefix, String path) {
    boolean isLegacy = path.startsWith(ANSWERS);

    return isLegacy ?
        relPrefix + path :
        trimPathForWebPage(path);
  }

  /**
   * @param language
   * @return
   * @see SlickResultDAO#getCorrectAndScores
   */
  @Override
  public String getRelPrefix(String language) {
    String installPath = getServerProps().getAnswerDir();

    String prefix = installPath + File.separator + language.toLowerCase();
    int netProfDurLength = getServerProps().getAudioBaseDir().length();

    return prefix.substring(netProfDurLength) + File.separator;
  }

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
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
    return getAudioDAO().getMaleFemaleProgress(projectid, getExercises(projectid, false));
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

  public IDialogDAO getDialogDAO() {
    return dialogDAO;
  }

  public IDialogSessionDAO getDialogSessionDAO() {
    return dialogSessionDAO;
  }

  public IRelatedResultDAO getRelatedResultDAO() {
    return relatedResultDAO;
  }

  @Override
  public IImageDAO getImageDAO() {
    return imageDAO;
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
  public IPendingUserDAO getPendingUserDAO() {
    return pendingUserDAO;
  }


  @Override
  public IOOVDAO getOOVDAO() {
    return oovDAO;
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


  @NotNull
  public ProjectSync getProjectSync() {
    return new ProjectSync(this, this.getProjectManagement(), this, this.getUserExerciseDAO(), this);
  }

  public ReportHelper getReportHelper() {
    return reportHelper;
  }

  public String toString() {
    return "Database : " + this.getClass().toString();
  }

  public PathHelper getPathHelper() {
    return pathHelper;
  }
}
