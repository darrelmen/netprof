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

package mitll.langtest.server.database.project;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.JsonSupport;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ProjectManagement implements IProjectManagement {
  private static final Logger logger = LogManager.getLogger(ProjectManagement.class);
  private static final int IMPORT_PROJECT_ID = -100;
  private static final boolean ADD_DEFECTS = false;

  private static final boolean DEBUG_ONE_PROJECT = false;
  private final PathHelper pathHelper;
  private final ServerProperties serverProps;
  private final LogAndNotify logAndNotify;

  private final IProjectDAO projectDAO;

  private final DatabaseImpl db;
  private final Map<Integer, Project> idToProject = new HashMap<>();

  /**
   * @param pathHelper
   * @param properties
   * @param logAndNotify
   * @param db
   * @see DatabaseImpl#setInstallPath
   */
  public ProjectManagement(PathHelper pathHelper,
                           ServerProperties properties,
                           LogAndNotify logAndNotify,
                           DatabaseImpl db) {
    this.pathHelper = pathHelper;
    this.serverProps = properties;
    this.logAndNotify = logAndNotify;
    this.db = db;
    this.projectDAO = db.getProjectDAO();
  }

  /**
   * @seex CopyToPostgres#createProjectIfNotExists
   * @see DatabaseImpl#populateProjects()
   */
  @Override
  public void populateProjects() {
    populateProjects(pathHelper, serverProps, logAndNotify, db);
  }

  public void rememberProject(int id) {
    SlickProject found = null;
    for (SlickProject slickProject :projectDAO.getAll()) {
      if (slickProject.id() == id) {
        found = slickProject;
        break;
      }
    }
    if (found != null) {
      rememberProject(pathHelper, serverProps, logAndNotify, found, db);
    }

  }
  /**
   * Fill in id->project map
   *
   * @see IProjectManagement#populateProjects()
   */
  private void populateProjects(PathHelper pathHelper,
                                ServerProperties serverProps,
                                LogAndNotify logAndNotify,
                                DatabaseImpl db) {
    Collection<SlickProject> all = projectDAO.getAll();

    if (!all.isEmpty()) {
      logger.info("populateProjects : found " + all.size() + " projects");
    }

    for (SlickProject slickProject : all) {
      if (!idToProject.containsKey(slickProject.id())) {
        if (DEBUG_ONE_PROJECT) {
          if (slickProject.language().equalsIgnoreCase("english")) {
            rememberProject(pathHelper, serverProps, logAndNotify, slickProject, db);
          }
        } else {
          rememberProject(pathHelper, serverProps, logAndNotify, slickProject, db);
        }
      }
    }

    logger.info("populateProjects now project ids " + idToProject.keySet());
    for (Project project : getProjects()) {
      logger.info("\tproject " + project);
    }
  }

  /**
   * TODO : add mechanism to trigger reload of exercises for a NetProf project from a domino project
   *
   */
  //@Override
/*  private void doReload() {
    for (Project project : getProjects()) {
      if (project.getExerciseDAO() == null) {
        setExerciseDAO(project);
        configureProject(*//*installPath,*//* project);
        logger.info("\tpopulateProjects : " + project + " : " + project.getAudioFileHelper());
      }
    }
  }*/


  /**
   * // TODO : this seems like a bad idea --
   * @see DatabaseImpl#configureProjects
   */
  @Override
  public void configureProjects() {
    Collection<Project> projects = getProjects();
    logger.info("configureProjects got " + projects.size() + " projects");
    for (Project project : projects) {
      configureProject(project);
    }
  }

  /**
   * @param project
   * @see #configureProjects
   */
  private void configureProject(Project project) {
    logger.info("configureProject " + project);
    SlickProject project1 = project.getProject();
    if (project1 == null) logger.info("configureProject : note : no project for " + project);
    int id = project1 == null ? -1 : project1.id();
    setDependencies(project.getExerciseDAO(), id);

    List<CommonExercise> rawExercises = project.getRawExercises();
    if (!rawExercises.isEmpty()) {
      logger.debug("first exercise is " + rawExercises.iterator().next());
    }
    project.setJsonSupport(new JsonSupport(project.getSectionHelper(), db.getResultDAO(), db.getRefResultDAO(), db.getAudioDAO(),
        db.getPhoneDAO()));

    if (project1 != null) {
      Map<Integer, String> exerciseIDToRefAudio = db.getExerciseIDToRefAudio(id);
      project.setAnalysis(
          new SlickAnalysis(db,
              db.getPhoneDAO(),
              exerciseIDToRefAudio,
              (SlickResultDAO) db.getResultDAO())
      );
//      userExerciseDAO.getTemplateExercise();
      project.getAudioFileHelper().checkLTSAndCountPhones(rawExercises);

//      ExerciseTrie<CommonExercise> commonExerciseExerciseTrie = populatePhoneTrie(rawExercises);
      logMemory();

      Set<Integer> exids = new HashSet<>();
      for (CommonExercise exercise:rawExercises) exids.add(exercise.getID());

//      List<SlickRefResultJson> jsonResults = db.getRefResultDAO().getJsonResults();
//
//      Map<Integer, ExercisePhoneInfo> exToPhonePerProject = new ExerciseToPhone().getExToPhonePerProject(exids, jsonResults);
//      project.setExToPhone(exToPhonePerProject);
      //    project.setPhoneTrie(commonExerciseExerciseTrie);
      logMemory();
    }

    logMemory();
  }

  public ExerciseDAO<CommonExercise> setDependencies() {
    Project project = idToProject.get(IMPORT_PROJECT_ID);
    ExerciseDAO<CommonExercise> exerciseDAO = project.getExerciseDAO();
    logger.info("setDependencies " + project + " : " + exerciseDAO);
    setDependencies(exerciseDAO,-1);

    return exerciseDAO;
  }

  /**
   * @param exerciseDAO
   * @param projid
   * @see #configureProject
   */
  public void setDependencies(ExerciseDAO exerciseDAO, int projid) {
    logger.info("setDependencies - " + projid);
    IAudioDAO audioDAO = db.getAudioDAO();

    if (audioDAO == null) logger.error("no audio dao ", new Exception());

    exerciseDAO.setDependencies(
        db.getUserExerciseDAO(),
        null /*addRemoveDAO*/,
        audioDAO,
        projid);
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

  //  @Override
  private Project getProjectOrFirst(int projectid) {
    return projectid == -1 ? getFirstProject() : getProject(projectid);
  }

  @Override
  public Project getProjectForUser(int userid) {
    return getProject(db.getUserProjectDAO().mostRecentByUser(userid));
  }

  @Override
  public void stopDecode() {
    for (Project project : getProjects()) project.stopDecode();
  }

  /**
   * @param pathHelper
   * @param serverProps
   * @param logAndNotify
   * @param slickProject
   * @see #populateProjects
   */
  private void rememberProject(PathHelper pathHelper,
                               ServerProperties serverProps,
                               LogAndNotify logAndNotify,
                               SlickProject slickProject,
                               DatabaseImpl db) {
    Project project = new Project(slickProject, pathHelper, serverProps, db, logAndNotify);
    idToProject.put(project.getProject().id(), project);
    logger.info("populateProjects : " + project + " : " + project.getAudioFileHelper());
    setExerciseDAO(project);
  }

  @Override
  public void addSingleProject(ExerciseDAO<CommonExercise> jsonExerciseDAO) {
    idToProject.put(IMPORT_PROJECT_ID, new Project(jsonExerciseDAO));
  }

  /**
   * @see DatabaseImpl#makeExerciseDAO(String, boolean)
   * @see IProjectManagement#populateProjects()
   */
  //@Override
/*
  public void setExerciseDAOs() {
    Collection<Project> projects = getProjects();
    logger.info("setExerciseDAOs on " + projects.size());
    for (Project project : projects) {
      // if (project.getProject().id() == 3)
      logger.info("makeExerciseDAO project     " + project);
      setExerciseDAO(project);
      //    logger.info("makeExerciseDAO project now " + project);
    }
  }
*/

  /**
   * @param project
   * @see #rememberProject(PathHelper, ServerProperties, LogAndNotify, SlickProject, DatabaseImpl)
   */
  private void setExerciseDAO(Project project) {
    logger.info("setExerciseDAO on " + project);
    DBExerciseDAO dbExerciseDAO = new DBExerciseDAO(
        serverProps,
        db.getUserListManager(),
        ADD_DEFECTS,
        (SlickUserExerciseDAO) db.getUserExerciseDAO(),
        project.getProject());
    project.setExerciseDAO(dbExerciseDAO);
  }


  /**
   * @param id
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#getResultASRInfo
   * @see mitll.langtest.server.DownloadServlet#getFilenameForDownload
   * @see DatabaseImpl#getExercise
   * @seex #deleteItem(int, int)
   * @seex #getCustomOrPredefExercise(int, int)
   */
  @Override
  public CommonExercise getExercise(int projectid, int id) {
    Project project = getProjectOrFirst(projectid);
    return project.getExercise(id);
  }


  /**
   * TODO : exercises are in the context of a project
   *
   * @param projectid
   * @return
   * @see #getExercises(int)
   * @see Project#buildExerciseTrie
   */
  @Override
  public Collection<CommonExercise> getExercises(int projectid) {
    if (isAmas()) {
      return Collections.emptyList();
    }
    Project project = getProjectOrFirst(projectid);

    logger.info("getExercises " + projectid  + " = " +project);

    List<CommonExercise> rawExercises = project.getRawExercises();
    if (rawExercises.isEmpty()) {
      logger.warn("getExercises no exercises in " + serverProps.getLessonPlan());// + " at " + installPath);
    }
    return rawExercises;
  }

  private boolean isAmas() {
    return serverProps.isAMAS();
  }

  @Override
  public Project getProject(int projectid) {
    if (projectid == -1) return getFirstProject();
    Project project = idToProject.get(projectid);
    if (project == null) {
      Project firstProject = getFirstProject();
      logger.error("no project with id " + projectid + " in known projects (" + idToProject.keySet() +
              ") returning first " + firstProject,
          new IllegalArgumentException());
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

  @Override
  public Collection<Project> getProjects() {
    return idToProject.values();
  }

  @Override
  public Project getFirstProject() {
    return getProjects().iterator().next();
  }

  /**
   * @param userWhere
   * @param projid
   * @see DatabaseImpl#setStartupInfo
   * @see mitll.langtest.server.services.UserServiceImpl#setProject(int)
   */
  @Override
  public void setStartupInfo(User userWhere, int projid) {
    //logger.info("setStartupInfo : For user " + userWhere + " projid " + projid);

    if (projid == -1) {
      logger.info("For " + userWhere + " no current project.");
    } else {
      if (!idToProject.containsKey(projid)) {
        logger.info("\tsetStartupInfo : populateProjects...");
        populateProjects();
      }

      Project project = getProject(projid);

      SlickProject project1 = project.getProject();
      List<String> typeOrder = project.getTypeOrder();
      //logger.info("project " + projid + " type order " + typeOrder);

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
          serverProps.getProperties(),
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

}
