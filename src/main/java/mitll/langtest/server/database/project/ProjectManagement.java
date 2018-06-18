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

import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import mitll.hlt.domino.server.data.DocumentServiceDelegate;
import mitll.hlt.domino.server.data.IProjectWorkflowDAO;
import mitll.hlt.domino.server.data.ProjectServiceDelegate;
import mitll.hlt.domino.server.data.SimpleDominoContext;
import mitll.hlt.domino.server.util.Mongo;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.JsonSupport;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.domino.DominoImport;
import mitll.langtest.server.domino.IDominoImport;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ImportProjectInfo;
import mitll.langtest.server.scoring.LTSFactory;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.*;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletContext;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.hlt.domino.server.ServerInitializationManager.MONGO_ATT_NAME;

public class ProjectManagement implements IProjectManagement {
  private static final Logger logger = LogManager.getLogger(ProjectManagement.class);

  /**
   * JUST FOR TESTING
   */
  private static final String LANG_TO_LOAD = "";
  /**
   * @see #addOtherProps
   */
  private static final String DOMINO_NAME = "Domino Project";
  /**
   * JUST FOR TESTING
   */
  private int debugProjectID = 17;

  private static final int IMPORT_PROJECT_ID = DatabaseImpl.IMPORT_PROJECT_ID;
  private static final boolean ADD_DEFECTS = false;

  /**
   *
   */
  private static final String SYNCED = "Last Sync";
  /**
   * @see #addDateProps(SlickProject, Map)
   */
  private static final String CREATED = "Created";
  public static final String MODIFIED = "Modified";
  /**
   * @see mitll.langtest.client.project.ProjectChoices#showImportDialog
   * @see #addExerciseDerivedProperties
   * @see mitll.langtest.server.domino.ProjectSync#getProps
   */
  public static final String NUM_ITEMS = "Num Items";

  /**
   * @see #addOtherProps
   */
  private static final String DOMINO_ID = "Domino ID";

  private static final String MONGO_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  private static final long FIVE_YEARS = (5L * 365L * 24L * 60L * 60L * 1000L);
  private static final ZoneId UTC = ZoneId.of("UTC");

  private final PathHelper pathHelper;
  private final ServerProperties serverProps;
  private final LogAndNotify logAndNotify;

  private final IProjectDAO projectDAO;

  private final DatabaseImpl db;
  /**
   *
   */
  private final Map<Integer, Project> idToProject = new HashMap<>();

  private final boolean debugOne;

  private final IDominoImport dominoImport;

  /**
   * @param pathHelper
   * @param properties
   * @param logAndNotify
   * @param db
   * @see DatabaseServices#setInstallPath
   */
  public ProjectManagement(PathHelper pathHelper,
                           ServerProperties properties,
                           LogAndNotify logAndNotify,
                           DatabaseImpl db,
                           ServletContext servletContext) {
    this.pathHelper = pathHelper;
    this.serverProps = properties;
    this.logAndNotify = logAndNotify;
    this.db = db;
    this.debugOne = properties.debugOneProject();
    this.debugProjectID = properties.debugProjectID();

    logger.info("ProjectManagement debug one " + debugOne + " = " + debugProjectID);

    this.projectDAO = db.getProjectDAO();

    if (servletContext == null) {
      logger.warn("\n\n\nProjectManagement : no servlet context, no domino delegates");
      dominoImport = null;
    } else {
      dominoImport = setupDominoProjectImport(servletContext);
    }
  }

  private DominoImport setupDominoProjectImport(ServletContext servletContext) {
    SimpleDominoContext simpleDominoContext = new SimpleDominoContext();
    simpleDominoContext.init(servletContext);

    ProjectServiceDelegate projectDelegate = simpleDominoContext.getProjectDelegate();
    IProjectWorkflowDAO workflowDelegate = simpleDominoContext.getWorkflowDAO();
    DocumentServiceDelegate documentDelegate = simpleDominoContext.getDocumentDelegate();

    return new DominoImport(
        projectDelegate,
        workflowDelegate,
        documentDelegate,
        (Mongo) servletContext.getAttribute(MONGO_ATT_NAME),
        db.getUserExerciseDAO());
  }


  /**
   * @param id
   * @see DatabaseImpl#rememberProject
   */
  public void rememberProject(int id) {
    SlickProject found = null;
    for (SlickProject slickProject : getAllSlickProjects()) {
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
   * @see DatabaseImpl#populateProjects
   */
  @Override
  public void populateProjects() {
    populateProjects(pathHelper, serverProps, logAndNotify, db);
  }

  /**
   * Safe to call this multiple times - if a project is already known it's skipped
   * If a project is already configured, won't be configured again.
   * Fill in id->project map
   *
   * @see #populateProjects()
   */
  private void populateProjects(PathHelper pathHelper,
                                ServerProperties serverProps,
                                LogAndNotify logAndNotify,
                                DatabaseImpl db) {
    getAllSlickProjects().forEach(slickProject -> {
      if (!idToProject.containsKey(slickProject.id())) {
        if (debugOne) {
          if (slickProject.id() == debugProjectID ||
              slickProject.language().equalsIgnoreCase(LANG_TO_LOAD)
              ) {
            rememberProject(pathHelper, serverProps, logAndNotify, slickProject, db);
          }
        } else {
          rememberProject(pathHelper, serverProps, logAndNotify, slickProject, db);
        }
      }
    });

    logger.info("populateProjects now project ids " + idToProject.keySet());
/*    for (Project project : getProjects()) {
      logger.info("\tproject " + project);
    }*/

    if (!idToProject.isEmpty()) {
      // logger.info("using exercise dao from first project " + exerciseDAO);
      db.getUserExerciseDAO().setExerciseDAO(getFirstProject().getExerciseDAO());
    } else {

      // TODO :make it so we can begin from empty container!

      //db.getUserExerciseDAO().setExerciseDAO(getExerciseDAO());
    }

    configureProjects();
  }

  /**
   * @return
   */
  private Collection<SlickProject> getAllSlickProjects() {
    return projectDAO.getAll();
  }

  /**
   * Latchy - would be better to do this when the project is remembered...
   * // TODO : this seems like a bad idea --
   *
   * @see #populateProjects(PathHelper, ServerProperties, LogAndNotify, DatabaseImpl)
   */
  private void configureProjects() {
    long then = System.currentTimeMillis();
    getProjects().forEach(project -> configureProject(project, false, false));
    long now = System.currentTimeMillis();
    logger.info("FINISHED : configureProjects " + getProjects().size() + " configured in " + ((now - then) / 1000) + " seconds.");
    logMemory();
  }

  public void configureProjectByID(int projid) {
    Project project = getProject(projid);
    if (project != null) {
      configureProject(project, false, false);
    }
  }

  /**
   * Lazy - only configure project if it isn't already or forceReload is true
   * <p>
   * only configured if we have a slick project for it... how could we not???
   *
   * @param project
   * @param configureEvenRetired
   * @param forceReload
   * @return number of exercises in the project
   * @see #configureProjects
   */
  public int configureProject(Project project, boolean configureEvenRetired, boolean forceReload) {
    long then = System.currentTimeMillis();

    boolean skipRetired = !project.shouldLoad() && !configureEvenRetired;
    boolean isConfigured = project.getExerciseDAO().isConfigured();
    if (!forceReload) {
      if (skipRetired || isConfigured) {
        if (isConfigured) {
          //logger.debug("configureProject project already configured " + project.getProject().id());
        } else {
          logger.info("configureProject skipping fully loading project " + project + " since it's retired");
        }
        return 0;
      }
    }

    int projectID = project.getID();
    logger.info("configure START " + projectID + "/" + getProjects().size() + " : " + project.getLanguage());

    project.clearPropCache();
//    logger.info("configureProject " + project.getProject().name() + " ---- ");
    SlickProject slickProject = project.getProject();

    if (slickProject == null) {
      logger.info("configureProject : note : no project for " + project);
    }

    // TODO : why would we want to keep going on a project that has no slick project -- if it's new???

    int id = slickProject == null ? -1 : slickProject.id();
    boolean myProject = project.isMyProject();
    setDependencies(project.getExerciseDAO(), id, myProject);

    if (forceReload) {
      project.getExerciseDAO().reload();
    }

    if (project.getExerciseDAO() == null) {
      setExerciseDAO(project);
    }

    List<CommonExercise> rawExercises = project.getRawExercises();
    if (!rawExercises.isEmpty()) {
      logger.debug("configureProject (" + project.getLanguage() + ") first exercise is " + rawExercises.iterator().next());
    } else {
      if (isProduction(project)) {
        logger.error("configureProject no exercises in project? " + project);
      } else {
        logger.warn("configureProject no exercises in project? " + project);
      }
    }
    project.setJsonSupport(new JsonSupport(project.getSectionHelper(),
        db.getResultDAO(), db.getAudioDAO(),
        db.getPhoneDAO(),
        project));

    if (slickProject != null) {
      project.setAnalysis(
          new SlickAnalysis(db,
              db.getPhoneDAO(),
              db.getAudioDAO(),
              (SlickResultDAO) db.getResultDAO(),
              project.getLanguage(),
              id,
              isPolyglot(project))
      );

      if (myProject) {
        new Thread(() -> project.getAudioFileHelper().checkLTSAndCountPhones(rawExercises)).start();
      }
//      ExerciseTrie<CommonExercise> commonExerciseExerciseTrie = populatePhoneTrie(rawExercises);
      //  logMemory();

      //Set<Integer> exids = new HashSet<>();
      //for (CommonExercise exercise : rawExercises) exids.add(exercise.getID());
      project.setRTL(isRTL(rawExercises));

      project.setFileToRecorder(db.getResultDAO().getStudentAnswers(projectID));
//      List<SlickRefResultJson> jsonResults = db.getRefResultDAO().getJsonResults();
//      Map<Integer, ExercisePhoneInfo> exToPhonePerProject = new ExerciseToPhone().getExToPhonePerProject(exids, jsonResults);
//      project.setExToPhone(exToPhonePerProject);
      //   db.getUserExerciseDAO().useExToPhones();
      //    project.setPhoneTrie(commonExerciseExerciseTrie);
      //logMemory();

      logger.info("configure END " + projectID + " " + project.getLanguage() + " in " + (System.currentTimeMillis() - then) + " millis.");

      // side effect is to cache the users.
      new Thread(() -> rememberUsers(projectID)).start();

      /**
       * Cache it for later....?
       */
 /*      new Thread(() -> {
         db
          .getAnalysis(projectID)
          .getUserInfo(db.getUserDAO(), 1);
       }).start();*/

      return rawExercises.size();
    } else {
      logger.warn("\n\n\nconfigureProject huh? no slick project for " + project);
      return 0;
    }
  }

  private boolean isProduction(Project project) {
    return project.getStatus() == ProjectStatus.PRODUCTION;
  }

  private boolean isPolyglot(Project project) {
    return project.getKind() == ProjectType.POLYGLOT;
  }

  private void rememberUsers(int projectID) {
    db.getUserDAO().getFirstLastFor(db.getUserProjectDAO().getUsersForProject(projectID));
  }

  @Override
  public int getUserForFile(String requestURI) {
    for (Project project : getProjects()) {
      Integer userID = project.getUserForFile(requestURI);
      if (userID != null) {
//        logger.info("getUserForFile : user in project #" + project.getID() + " for " + requestURI + " is " + userID);
        return userID;
      }
    }

//    logger.info("getUserForFile couldn't find recorder of " + requestURI);

    return -1;
  }

  /**
   * Just look at the first exercise.
   *
   * @param exercises
   * @return
   */
  private boolean isRTL(Collection<? extends CommonShell> exercises) {
    boolean isRTL = false;
    if (!exercises.isEmpty()) {
      CommonShell next = exercises.iterator().next();
      HasDirection.Direction direction = WordCountDirectionEstimator.get().estimateDirection(next.getForeignLanguage());
      // String rtl = properties.get("rtl");
      isRTL = direction == HasDirection.Direction.RTL;
      // logger.info("examined text and found it to be " + direction);
    }
    return isRTL;
  }

  /**
   * ONLY used on import - copying old netprof v1 data into netprof v2.
   *
   * @return
   * @see DatabaseImpl#makeDAO
   * @see #addSingleProject
   */
  public ExerciseDAO<CommonExercise> setDependencies() {
    ExerciseDAO<CommonExercise> exerciseDAO = idToProject.get(IMPORT_PROJECT_ID).getExerciseDAO();
    //logger.info("setDependencies " + project + " : " + exerciseDAO);
    setDependencies(exerciseDAO, -1, false);
    return exerciseDAO;
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
    logger.info("rememberProject : " + project + " now " + idToProject.size() + " projects");
    setExerciseDAO(project);
  }

  /**
   * @param projid
   */
  @Override
  public void forgetProject(int projid) {
    idToProject.remove(projid);
  }

  /**
   * After changing project status - e.g. to retired - we need to update the SlickProject on the project.
   *
   * @see mitll.langtest.server.services.ProjectServiceImpl#update
   */
  @Override
  public void refreshProjects() {
    Map<Integer, SlickProject> idToSlickProject = getIdToProjectMapFromDB();
    getProjects().forEach(project -> {
      SlickProject project1 = idToSlickProject.get(project.getID());
      if (project1 == null) {
        logger.warn("huh? no project for " + project.getID() + " : " + project);
      } else {
        project.setProject(project1);
      }
    });
  }

  @NotNull
  private Map<Integer, SlickProject> getIdToProjectMapFromDB() {
    Collection<SlickProject> all = getAllSlickProjects();
    Map<Integer, SlickProject> idToSlickProject = new HashMap<>();
    for (SlickProject project : all) idToSlickProject.put(project.id(), project);
    return idToSlickProject;
  }

  /**
   * @param exerciseDAO
   * @param projid
   * @param isMyProject
   * @see #configureProject
   */
  private void setDependencies(ExerciseDAO exerciseDAO, int projid, boolean isMyProject) {
    IAudioDAO audioDAO = db.getAudioDAO();
//    logger.info("setDependencies - project #" + projid  + " audio dao " + audioDAO);
    if (audioDAO == null) {
      logger.error("setDependencies no audio dao ", new Exception());
    }

    exerciseDAO.setDependencies(
        db.getUserExerciseDAO(),
        null /*addRemoveDAO*/,
        audioDAO,
        projid,
        db,
        isMyProject);
  }

  public static long logMemory() {
    long MB = (1024L * 1024L);
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    logger.info("logMemory : current group " + threadGroup.getName() + " (" + threadGroup.activeCount() +
        " threads in group) : # cores = " + Runtime.getRuntime().availableProcessors() + " heap : free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");

    return used / MB;
  }

  private Project getProjectOrFirst(int projectid) {
    boolean getFirst = projectid == -1;
    if (getFirst) logger.warn("getProjectOrFirst returning first project for " + projectid);
    return getFirst ? getFirstProject() : getProject(projectid);
  }

  /**
   * If the project has become retired or deleted, they get kicked out.
   *
   * @param userid
   * @return
   */
  @Override
  public Project getProjectForUser(int userid) {
    Project project = getProject(db.getUserProjectDAO().getCurrentProjectForUser(userid));

    if (project != null && !project.getStatus().shouldLoad()) {
      return null;
    } else {
      return project;
    }
  }

  @Override
  public void stopDecode() {
    for (Project project : getProjects()) project.stopDecode();
  }

  /**
   * JUST FOR IMPORT
   *
   * @param excelImport
   */
  @Override
  public void addSingleProject(ExerciseDAO<CommonExercise> excelImport) {
    idToProject.put(IMPORT_PROJECT_ID, new Project(excelImport));
  }

  /**
   * @param project
   * @see #rememberProject(PathHelper, ServerProperties, LogAndNotify, SlickProject, DatabaseImpl)
   */
  private ExerciseDAO<CommonExercise> setExerciseDAO(Project project) {
//    logger.info("setExerciseDAO on " + project);
    DBExerciseDAO dbExerciseDAO = getExerciseDAO(project);
    project.setExerciseDAO(dbExerciseDAO);
    return dbExerciseDAO;
  }

  @NotNull
  private DBExerciseDAO getExerciseDAO(Project project) {
    return new DBExerciseDAO(
        serverProps,
        db.getUserListManager(),
        ADD_DEFECTS,
        (SlickUserExerciseDAO) db.getUserExerciseDAO(),
        project);
  }


  /**
   * @param id
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#getResultASRInfo
   * @see mitll.langtest.server.DownloadServlet#getFilenameForDownload
   * @see DatabaseImpl#getExercise
   */
  @Override
  public CommonExercise getExercise(int projectid, int id) {
    return getProjectOrFirst(projectid).getExerciseByID(id);
  }

  /**
   * @param id
   * @return
   * @see DatabaseImpl#getCustomOrPredefExercise
   */
  @Override
  public CommonExercise getExercise(int id) {
    int projectForExercise = db.getUserExerciseDAO().getProjectForExercise(id);
    if (projectForExercise == -1) {
      logger.error("getExercise : can't find project for exercise " + id);
      return null;
    } else {
      Project project = getProject(projectForExercise);
      return project == null ? null : project.getExerciseByID(id);
    }
  }

  /**
   * exercises are in the context of a project
   * <p>
   * deals with projects added while webapp is running -
   *
   * @param projectid
   * @return
   * @see #getExercises(int)
   * @see Project#buildExerciseTrie
   */
  @Override
  public List<CommonExercise> getExercises(int projectid) {
    if (isAmas()) {
      return Collections.emptyList();
    }
    Project project = getProjectOrFirst(projectid);
    if (project == null) {
      logger.error("getExercises no project for " + projectid + " so returning empty exercises.");
      return Collections.emptyList();
    }
//    logger.info("getExercises " + projectid  + " = " +project);

    if (!project.isConfigured()) {
      logger.info("\tgetExercises configure " + projectid + " and project " + project);
      configureProject(project, false, false);
    }

    List<CommonExercise> rawExercises = project.getRawExercises();
    if (rawExercises.isEmpty()) {
      logger.warn("getExercises for project id " + projectid +
          " no exercises in '" + serverProps.getLessonPlan() + "' = " + rawExercises.size());
    }
    return rawExercises;
  }

  private boolean isAmas() {
    return serverProps.isAMAS();
  }

  /**
   * @param name
   * @return null if no match
   */
  @Override
  public Project getProjectByName(String name) {
    return idToProject
        .values()
        .stream()
        .filter(project -> project.getProject().name().toLowerCase().equals(name.toLowerCase()))
        .findFirst()
        .orElseGet(null);
  }

  @Override
  public List<Project> getProjectByLangauge(Language name) {
    return idToProject
        .values()
        .stream()
        .filter(project -> project.getLanguageEnum() == name)
        .collect(Collectors.toList());
  }

  @Override
  public List<Project> getMatchingProjects(Language languageMatchingGroup, boolean isPoly) {
    List<Project> projectByLangauge = getProjectByLangauge(languageMatchingGroup);
    return projectByLangauge.stream()
        .filter(project ->
            (!isPoly || isPolyglot(project)) &&
                isProduction(project))
        .collect(Collectors.toList());
  }

  /**
   * Try to deal with project set changing out from underneath us...
   *
   * @param projectid
   * @return
   */
  @Override
  public Project getProject(int projectid) {
    if (projectid == -1) {
      logger.warn("getProject called with -1 projectid?", new IllegalArgumentException("project id = -1"));
    }

    Project project = idToProject.get(projectid);

    if (project == null) {
      if (anyNewProjectsAdded()) {
        project = lazyGetProject(projectid);
      }

      if (project == null && !idToProject.isEmpty()) {
        logger.error("getProject no project with id " + projectid + " in known projects (" + idToProject.keySet() +
            ") ");
        return null;
      }
    }
    return project;
  }

  /**
   * @return
   * @see #getProject
   */
  private boolean anyNewProjectsAdded() {
    return !getNewProjects(idToProject.keySet()).isEmpty();
  }

  private Project lazyGetProject(int projectid) {
    logger.info("lazyGetProject no project with id " + projectid + " in known projects (" + idToProject.keySet() +
        ") - refreshing projects");
    populateProjects();
    Project project = idToProject.get(projectid);

    if (project == null) {
      logger.warn("lazyGetProject, even after project set refresh, no project with id " + projectid + " in known projects (" + idToProject.keySet() +
          ") - refreshing projects", new Exception());
    }

    return project;
  }

  /**
   * @param knownProjects
   * @return
   * @see #getProject(int)
   */
  @NotNull
  private Set<Integer> getNewProjects(Set<Integer> knownProjects) {
    Set<Integer> dbProjects = getAllSlickProjects().stream().map(SlickProject::id).collect(Collectors.toSet());
    dbProjects.removeAll(knownProjects);
    return dbProjects;
  }

  @Override
  public Collection<Project> getProductionProjects() {
    return getProductionProjects(getProjects());
  }

  @Override
  public Collection<Project> getProjects() {
    return idToProject.values();
  }

  private List<Project> getProductionProjects(Collection<Project> toFilter) {
    return toFilter
        .stream()
        .filter(this::isProduction)
        .collect(Collectors.toList());
  }

  /**
   * @return
   * @see #getProject(int)
   * @see #getProjectOrFirst(int)
   * @see #populateProjects(PathHelper, ServerProperties, LogAndNotify, DatabaseImpl)
   */
  @Override
  public Project getFirstProject() {
    return getProjects().iterator().next();
  }

  /**
   * @param userWhere
   * @param projid
   * @see DatabaseImpl#setStartupInfo
   */
  @Override
  public void setStartupInfo(User userWhere, int projid) {
    logger.info("setStartupInfo : For user " + userWhere.getUserID() + " projid " + projid);
    if (projid == -1) {
      logger.info("setStartupInfo for\n\t" + userWhere + "\n\tno current project.");
      clearStartupInfo(userWhere);
    } else {
      if (!idToProject.containsKey(projid)) {
        logger.info("\tsetStartupInfo : populateProjects...");
        populateProjects();
      }

      Project project = getProject(projid);

      if (project != null) {
        if (!project.getStatus().shouldLoad() && !userWhere.isAdmin()) {
          logger.info("setStartupInfo project is retired - so kicking the user back to project choice screen.");
          clearStartupInfo(userWhere);
        } else {
          setStartupInfoOnUser(userWhere, projid, project);
        }
      }
    }
  }

  @Override
  public void clearStartupInfo(User userWhere) {
    userWhere.setStartupInfo(null);
  }

  /**
   * @param userWhere
   * @param projid
   * @param project
   * @see #setStartupInfo
   */
  private void setStartupInfoOnUser(User userWhere, int projid, Project project) {
    configureProject(project, true, false);

    SlickProject project1 = project.getProject();
    List<String> typeOrder = project.getTypeOrder();

    logger.info("project   " + project1);
    logger.info("typeOrder " + typeOrder);

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();
    logger.info("sectionHelper typeOrder " + typeOrder);

    String language = project1.language();

    for (String type : typeOrder) {
      if (type.isEmpty()) logger.error("setStartupInfo huh? type order has blank?? " + type);
    }

    boolean hasModel = project.hasModel();
    ProjectStartupInfo startupInfo = new ProjectStartupInfo(
        serverProps.getProperties(),
        typeOrder,
        sectionHelper.getSectionNodesForTypes(),
        project1.id(),
        language,
        toEnum(language),
        LTSFactory.getLocale(language),
        hasModel,
        sectionHelper.getTypeToDistinct(),
        sectionHelper.getRootTypes(),
        sectionHelper.getParentToChildTypes(),
        ProjectType.valueOf(project1.kind()));

    logger.info("setStartupInfo : For" +
        "\n\tUser      " + userWhere +
        "\n\tprojid    " + projid +
        "\n\ttypeOrder " + typeOrder +
        "\n\tstartup   " + startupInfo);

    userWhere.setStartupInfo(startupInfo);
  }

  private Language toEnum(String language) {
    Language language1;

    try {
      language1 = Language.valueOf(language.toUpperCase());
    } catch (IllegalArgumentException e) {
      language1 = Language.UNKNOWN;
    }
    return language1;
  }

  /**
   * @return
   * @see LangTestDatabaseImpl#getStartupInfo
   */
  public List<SlimProject> getNestedProjectInfo() {
    int numProjects = projectDAO.getNumProjects();
    int currentNumProjects = idToProject.size();

    if (numProjects != currentNumProjects) {
      logger.info("getNestedProjectInfo : project loaded? db projects " + numProjects + " current " + currentNumProjects);
      populateProjects();
    }

    List<SlimProject> projectInfos = new ArrayList<>();
    Map<String, List<Project>> langToProject = getLangToProjects();
//    logger.info("getNestedProjectInfo lang->project is " + langToProject.keySet());

    langToProject.values().forEach(projects -> {
      List<Project> production = getProductionProjects(projects);
      Project firstProject = (production.isEmpty()) ? projects.iterator().next() : production.iterator().next();

      SlimProject parent = getProjectInfo(firstProject);
      projectInfos.add(parent);

      if (projects.size() > 1) {
        // add child to self?
        projects.forEach(project -> parent.addChild(getProjectInfo(project)));
      }
    });

    return projectInfos;
  }

  @NotNull
  private Map<String, List<Project>> getLangToProjects() {
    Map<String, List<Project>> langToProject = new TreeMap<>();
    getProjects().forEach(project -> {
      List<Project> slimProjects = langToProject.computeIfAbsent(project.getLanguage(), k -> new ArrayList<>());
      slimProjects.add(project);
    });
    return langToProject;
  }

  /**
   * TODO : SlimProject not so slim anymore. simplify.
   *
   * @param pproject
   * @return
   * @see #getNestedProjectInfo
   */
  private SlimProject getProjectInfo(Project pproject) {
    Map<String, String> info = new LinkedHashMap<>();

    SlickProject project = pproject.getProject();
    User creator = db.getUserDAO().getByID(project.userid());
    String userInfo = creator == null ? "" : " : " + creator.getUserID();
    info.put(CREATED + " by", project.userid() + userInfo);

    addDateProps(project, info);

    boolean isRTL = addOtherProps(project, info);

    return new SlimProject(
        project.id(),
        project.name(),
        project.language(),
        project.course(), project.countrycode(),
        ProjectStatus.valueOf(project.status()),
        ProjectType.valueOf(project.kind()),
        project.displayorder(),

        pproject.hasModel(),
        isRTL,

        project.created().getTime(),
        project.lastimport().getTime(),

        project.lastnetprof().getTime(),

        pproject.getWebserviceHost(),
        pproject.getPort(),
        pproject.getModelsDir(),

        project.first(),
        project.second(),
        pproject.isOnIOS(),
        project.dominoid(),
        info,
        project.userid());
  }

  private boolean addOtherProps(SlickProject project, Map<String, String> info) {
    if (project.dominoid() > 0) {
//      info.put(DOMINO_ID, "" + project.dominoid());
      info.put(DOMINO_NAME, project.dominoid() + " : " + getDominoProjectName(project.dominoid()));
    }

    return addExerciseDerivedProperties(project, info);
  }

  private boolean addExerciseDerivedProperties(SlickProject project, Map<String, String> info) {
    boolean isRTL = false;
    if (getProjectStatus(project).shouldLoad()) {
      List<CommonExercise> exercises = db.getExercises(project.id());
      isRTL = isRTL(exercises);
      info.put(NUM_ITEMS, "" + exercises.size());
//      logger.info("got " + exercises.size() + " ex for project #" + project.id());
      //info.put("# context", "" + exercises.size());

    }
    return isRTL;
  }

  private DateFormat format = new SimpleDateFormat();

  private void addDateProps(SlickProject project, Map<String, String> info) {
    info.put(CREATED, format.format(project.created()));
    info.put(MODIFIED, format.format(project.modified()));
    info.put(SYNCED, format.format(project.lastimport()));
  }

  @NotNull
  private ProjectStatus getProjectStatus(SlickProject project) {
    ProjectStatus status;
    try {
      status = ProjectStatus.valueOf(project.status());
    } catch (IllegalArgumentException e) {
      logger.error("got " + e, e);
      status = ProjectStatus.DEVELOPMENT;
    }
    return status;
  }

  public ImportInfo getImportFromDomino(int projID) {
    Project project = getProject(projID);

    if (project != null) {
      Timestamp modified = project.getProject().lastimport();

      String sinceInUTC = getModifiedTimestamp(project, modified);

      int dominoid = project.getProject().dominoid();
      logger.info("getImportFromDomino getting changes" +
          "\n\tsinceInUTC last import " + new Date(modified.getTime()) + " = " + sinceInUTC +
          "\n\tdomino id              " + dominoid);

      return getImportFromDomino(projID, dominoid, sinceInUTC);
    } else {
      return null;
    }
  }

  /**
   * If the project is empty, go to the beginning of time, more or less.
   * So if you make a project in domino, then in netprof, will pick up everything in domino.
   *
   * @param project
   * @param modified
   * @return
   */
  @NotNull
  private String getModifiedTimestamp(Project project, Timestamp modified) {
    ZonedDateTime zdt;

    if (project.getRawExercises().isEmpty()) {
      Date fiveYearsAgo = new Date(System.currentTimeMillis() - FIVE_YEARS);
      //  logger.info("Start from " + fiveYearsAgo);
      zdt = ZonedDateTime.ofInstant(fiveYearsAgo.toInstant(), UTC);
    } else {
      zdt = ZonedDateTime.ofInstant(modified.toInstant(), UTC);
    }
    return zdt.format(DateTimeFormatter.ofPattern(MONGO_TIME));
  }

  /**
   * @param projID
   * @param dominoID
   * @param sinceInUTC
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#addPending
   */
  private ImportInfo getImportFromDomino(int projID, int dominoID, String sinceInUTC) {
    boolean shouldSwap = db.getProjectDAO().getDefPropValue(projID, ProjectProperty.SWAP_PRIMARY_AND_ALT).equalsIgnoreCase("TRUE");
    return dominoImport == null ? null : dominoImport.getImportFromDomino(projID, dominoID, sinceInUTC, db.getUserDAO().getDominoAdminUser(), shouldSwap);
  }

  /**
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#getDominoForLanguage
   */
  public List<ImportProjectInfo> getVocabProjects() {
    return dominoImport == null ? Collections.emptyList() : dominoImport.getImportProjectInfos(db.getUserDAO().getDominoAdminUser());
  }

  @Override
  public String getDominoProjectName(int dominoProjectID) {
    return dominoImport == null ? "" : dominoImport.getDominoProjectName(dominoProjectID);
  }
}