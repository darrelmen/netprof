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
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import org.apache.log4j.Logger;

import java.util.*;

public class ProjectManagement implements IProjectManagement {
  private static final Logger logger = Logger.getLogger(DatabaseImpl.class);
  public static final int IMPORT_PROJECT_ID = -100;
  private static final boolean ADD_DEFECTS = false;

  private static final boolean DEBUG_ONE_PROJECT = false;
  private PathHelper pathHelper;
  private final ServerProperties serverProps;
  private final LogAndNotify logAndNotify;

  private IProjectDAO projectDAO;

  DatabaseImpl db;
  private final Map<Integer, Project> idToProject = new HashMap<>();

  public ProjectManagement(PathHelper pathHelper, ServerProperties properties, LogAndNotify logAndNotify,
                           DatabaseImpl db) {
    this.pathHelper = pathHelper;
    this.serverProps = properties;
    this.logAndNotify = logAndNotify;
    this.db = db;
  }

  public IProjectDAO getProjectDAO() {
    return projectDAO;
  }

  /**
   * @param reload
   * @seex CopyToPostgres#createProjectIfNotExists
   */
  @Override
  public void populateProjects(boolean reload) {
    populateProjects(pathHelper, serverProps, logAndNotify, reload, db);
  }


  @Override
  public String getLanguage(CommonExercise ex) {
    return getLanguage(ex.getProjectID());
  }

  @Override
  public String getLanguage(int projectid) {
    return getProject(projectid).getLanguage();
  }


  /**
   * Fill in id->project map
   *
   * @see #populateProjects(boolean)
   */
  private void populateProjects(PathHelper pathHelper,
                                ServerProperties serverProps,
                                LogAndNotify logAndNotify,
                                boolean reload,
                                DatabaseImpl db) {
    Collection<SlickProject> all = projectDAO.getAll();
    logger.info("populateProjects : found " + all.size() + " projects");

    for (SlickProject slickProject : all) {
      if (!idToProject.containsKey(slickProject.id())) {
        if (DEBUG_ONE_PROJECT) {
          if (slickProject.language().equalsIgnoreCase("english")) {
            rememberProject(pathHelper, serverProps, logAndNotify, reload, slickProject, db);
          }
        } else {
          rememberProject(pathHelper, serverProps, logAndNotify, reload, slickProject, db);
        }
      }
    }

    if (reload) {
      doReload();
    }

    logger.info("populateProjects (reload = " + reload + ") now project ids " + idToProject.keySet());
    for (Project project : getProjects()) {
      logger.info("\tproject " + project);
    }
  }

  @Override
  public void doReload() {
    for (Project project : getProjects()) {
      if (project.getExerciseDAO() == null) {
        setExerciseDAO(project);
        configureProject(/*installPath,*/ project);
        logger.info("\tpopulateProjects : " + project + " : " + project.getAudioFileHelper());
      }
    }
  }

  @Override
  public void configureProjects() {
    // TODO : this seems like a bad idea --

    for (Project project : getProjects()) {
      configureProject(project);
    }
  }

  /**
   * @param installPath
   * @param project
   * @see #makeDAO(String, String, String)
   */
  @Override
  public void configureProject(/*String installPath,*/ Project project) {
    // logger.info("configureProject " + project + " install path " + installPath);

    ExerciseDAO<?> exerciseDAO1 = project.getExerciseDAO();
    SlickProject project1 = project.getProject();
    if (project1 == null) logger.info("note : no project for " + project);
    int id = project1 == null ? -1 : project1.id();
    setDependencies(exerciseDAO1, id);

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
    }
    logMemory();
  }

  public void setDependencies(ExerciseDAO exerciseDAO, int projid) {
    exerciseDAO.setDependencies(db.getUserExerciseDAO(), null /*addRemoveDAO*/, db.getAudioDAO(), projid);
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

  @Override
  public Project getProjectOrFirst(int projectid) {
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
   * @param reload
   * @param slickProject
   * @see #populateProjects(PathHelper, ServerProperties, LogAndNotify, boolean)
   */
  private void rememberProject(PathHelper pathHelper, ServerProperties serverProps, LogAndNotify logAndNotify,
                               boolean reload, SlickProject slickProject,
                               DatabaseImpl db) {
    Project project = new Project(slickProject, pathHelper, serverProps, db, logAndNotify);
    idToProject.put(project.getProject().id(), project);
    logger.info("populateProjects (reload = " + reload + ") : " + project + " : " + project.getAudioFileHelper());
  }

  @Override
  public void addSingleProject(ExerciseDAO<CommonExercise> jsonExerciseDAO) {
    idToProject.put(IMPORT_PROJECT_ID, new Project(jsonExerciseDAO));
  }


  /**
   * @see #makeExerciseDAO(String, boolean)
   */
  @Override
  public void setExerciseDAOs() {
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
  private void setExerciseDAO(Project project/*, DBExerciseDAO dbExerciseDAO*/) {
//    logger.info("setExerciseDAO on " + project);
    SlickProject project1 = project.getProject();
    DBExerciseDAO dbExerciseDAO = new DBExerciseDAO(serverProps, db.getUserListManager(), ADD_DEFECTS,
        (SlickUserExerciseDAO) db.getUserExerciseDAO(), project1);
    project.setExerciseDAO(dbExerciseDAO);
  }


  /**
   * @param id
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#getResultASRInfo
   * @see mitll.langtest.server.DownloadServlet#getFilenameForDownload
   * @see #deleteItem(int, int)
   * @see #getCustomOrPredefExercise(int, int)
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
   * @see #setStartupInfo(User)
   * @see mitll.langtest.server.services.UserServiceImpl#setProject(int)
   */
  @Override
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
