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
import mitll.langtest.server.*;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.JsonSupport;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.exercise.*;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.domino.DominoImport;
import mitll.langtest.server.domino.IDominoImport;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ImportProjectInfo;
import mitll.langtest.server.scoring.LTSFactory;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import scala.deprecated;

import javax.servlet.ServletContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.hlt.domino.server.ServerInitializationManager.MONGO_ATT_NAME;
import static mitll.langtest.server.database.exercise.Project.WEBSERVICE_HOST_DEFAULT;
import static mitll.langtest.shared.project.ProjectStatus.DELETED;

public class ProjectManagement implements IProjectManagement {
  private static final Logger logger = LogManager.getLogger(ProjectManagement.class);

  /**
   * JUST FOR TESTING
   */
  private static final String LANG_TO_LOAD = "";
  /**
   * JUST FOR TESTING
   */
  private int debugProjectID = 2;

  private static final int IMPORT_PROJECT_ID = DatabaseImpl.IMPORT_PROJECT_ID;
  private static final boolean ADD_DEFECTS = false;
  private static final String CREATED = "Created";
  public static final String MODIFIED = "Modified";
  public static final String NUM_ITEMS = "Num Items";
  private static final String DOMINO_ID = "Domino ID";

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
    this.projectDAO = db.getProjectDAO();

    if (servletContext == null) {
      logger.warn("ProjectManagement : no servlet context, no domino delegates");
      dominoImport = null;
    } else {
      dominoImport = setupDominoProjectImport(servletContext);
    }
  }

  private DominoImport setupDominoProjectImport(ServletContext servletContext) {
    IProjectWorkflowDAO workflowDelegate;
    SimpleDominoContext simpleDominoContext = new SimpleDominoContext();
    simpleDominoContext.init(servletContext);
    ProjectServiceDelegate projectDelegate = simpleDominoContext.getProjectDelegate();
    workflowDelegate = simpleDominoContext.getWorkflowDAO();
    DocumentServiceDelegate documentDelegate = simpleDominoContext.getDocumentDelegate();
    return new DominoImport(projectDelegate, workflowDelegate, documentDelegate,
        (Mongo) servletContext.getAttribute(MONGO_ATT_NAME));
  }


  /**
   * @param id
   * @see DatabaseImpl#rememberProject
   */
  public void rememberProject(int id) {
    SlickProject found = null;
    for (SlickProject slickProject : getAllProjects()) {
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
   *
   * @see #populateProjects()
   */
  private void populateProjects(PathHelper pathHelper,
                                ServerProperties serverProps,
                                LogAndNotify logAndNotify,
                                DatabaseImpl db) {
    getAllProjects().forEach(slickProject -> {
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
    }

    configureProjects();
  }

  /**
   * @return
   */
  private Collection<SlickProject> getAllProjects() {
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
    configureProject(getProject(projid), false, false);
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
    boolean skipRetired = project.isRetired() && !configureEvenRetired;
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

//    Timestamp lastimport = slickProject.lastimport();
//    logger.info("last import " + lastimport);

    // TODO : why would we want to keep going on a project that has no slick project -- if it's new???

    int id = slickProject == null ? -1 : slickProject.id();
    boolean myProject = project.isMyProject();
    setDependencies(project.getExerciseDAO(), id, myProject);

    if (forceReload) {
      project.getExerciseDAO().reload();
    }
    List<CommonExercise> rawExercises = project.getRawExercises();
    if (!rawExercises.isEmpty()) {
      logger.debug("configureProject (" + project.getLanguage() + ") first exercise is " + rawExercises.iterator().next());
    } else {
      if (project.getStatus() == ProjectStatus.PRODUCTION) {
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
              id
          )
      );

      if (myProject) {
        project.getAudioFileHelper().checkLTSAndCountPhones(rawExercises);
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
      db.getUserDAO().getFirstLastFor(db.getUserProjectDAO().getUserToProject().keySet());
      return rawExercises.size();
    } else {
      logger.warn("\n\n\nconfigureProject huh? no slick project for " + project);
      return 0;
    }
  }

  @Override
  public int getUserForFile(String requestURI) {
    for (Project project : getProjects()) {
      Integer userID = project.getUserForFile(requestURI);
      if (userID != null) {
        logger.info("getUserForFile : user in " + project.getID() + " for " + requestURI + " is " + userID);
        return userID;
      }
    }
    ;

    logger.info("getUserForFile couldn't find recorder of " + requestURI);

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
    Collection<SlickProject> all = getAllProjects();
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

  public static void logMemory() {
    int MB = (1024 * 1024);
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    logger.debug(" current thread group " + threadGroup.getName() + " = " + threadGroup.activeCount() +
        " : # cores = " + Runtime.getRuntime().availableProcessors() + " heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }

  private Project getProjectOrFirst(int projectid) {
    boolean getFirst = projectid == -1;
    if (getFirst) logger.warn("getProjectOrFirst returning first project for " + projectid);
    return getFirst ? getFirstProject() : getProject(projectid);
  }

  @Override
  public Project getProjectForUser(int userid) {
    Project project = getProject(db.getUserProjectDAO().mostRecentByUser(userid));

    if (project != null &&
        project.getStatus() == ProjectStatus.RETIRED) {
      return null;
    }

    return project;
  }

  @Override
  public void stopDecode() {
    for (Project project : getProjects()) project.stopDecode();
  }

  /**
   * JUST FOR IMPORT
   *
   * @param jsonExerciseDAO
   */
  @Override
  public void addSingleProject(ExerciseDAO<CommonExercise> jsonExerciseDAO) {
    idToProject.put(IMPORT_PROJECT_ID, new Project(jsonExerciseDAO));
  }

  /**
   * @param project
   * @see #rememberProject(PathHelper, ServerProperties, LogAndNotify, SlickProject, DatabaseImpl)
   */
  private void setExerciseDAO(Project project) {
//    logger.info("setExerciseDAO on " + project);
    DBExerciseDAO dbExerciseDAO = new DBExerciseDAO(
        serverProps,
        db.getUserListManager(),
        ADD_DEFECTS,
        (SlickUserExerciseDAO) db.getUserExerciseDAO(),
        project);
    project.setExerciseDAO(dbExerciseDAO);
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

  /**
   * Try to deal with project set changing out from underneath us...
   *
   * @param projectid
   * @return
   */
  @Override
  public Project getProject(int projectid) {
/*    if (projectid == IMPORT_PROJECT_ID && !idToProject.isEmpty()) {
      logger.info("getProject not returning project for " + projectid);
      return null;//getFirstProject();
    } else {*/
    Project project = idToProject.get(projectid);

    if (project == null) {
      if (anyNewProjectsAdded()) {
        project = lazyGetProject(projectid);
      }

      if (project == null && !idToProject.isEmpty()) {
        //Project firstProject = getFirstProject();
        logger.error("getProject no project with id " + projectid + " in known projects (" + idToProject.keySet() +
            ") ");//,
        //new IllegalArgumentException());
        return null;//firstProject;
      }
    }
    return project;
    //  }
  }

  /**
   * @return
   * @see #getProject
   */
  private boolean anyNewProjectsAdded() {
    return !getNewProjects(idToProject.keySet()).isEmpty();
  }

  private Project lazyGetProject(int projectid) {
    logger.warn("getProject no project with id " + projectid + " in known projects (" + idToProject.keySet() +
        ") - refreshing projects");
    populateProjects();
    return idToProject.get(projectid);
  }

  /**
   * @param knownProjects
   * @return
   * @see #getProject(int)
   */
  @NotNull
  private Set<Integer> getNewProjects(Set<Integer> knownProjects) {
    Set<Integer> dbProjects = getAllProjects().stream().map(SlickProject::id).collect(Collectors.toSet());
    dbProjects.removeAll(knownProjects);
    return dbProjects;
  }

  @Override
  public Collection<Project> getProductionProjects() {
    return getProductionProjects(getProjects());
  }

  @Override
  public Collection<Project> getProjects() {    return idToProject.values();  }

  private List<Project> getProductionProjects(Collection<Project> toFilter) {
    return toFilter
        .stream()
        .filter(p -> p.getStatus() == ProjectStatus.PRODUCTION)
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
    //logger.info("setStartupInfo : For user " + userWhere.getUserID() + " projid " + projid);
    if (projid == -1) {
      logger.info("setStartupInfo for\n\t" + userWhere + "\n\tno current project.");
      userWhere.setStartupInfo(null);
    } else {
      if (!idToProject.containsKey(projid)) {
        logger.info("\tsetStartupInfo : populateProjects...");
        populateProjects();
      }

      Project project = getProject(projid);

      if (project.getStatus() == ProjectStatus.RETIRED && !userWhere.isAdmin()) {
        logger.info("setStartupInfo project is retired - so kicking the user back to project choice screen.");
        userWhere.setStartupInfo(null);
      } else {
        setStartupInfoOnUser(userWhere, projid, project);
      }
    }
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

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();

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
        sectionHelper.getParentToChildTypes());

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
      logger.info("getNestedProjectInfo : project loaded? db projects " + numProjects + " current " +currentNumProjects);
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
//        for (Project project : projects) {
//          parent.addChild(getProjectInfo(project));
//          //  logger.info("\t add child to " + parent);
//        }
      }
    });

    return projectInfos;
  }

  private List<SlickProject> getProductionProjects(List<SlickProject> slickProjects) {
    return slickProjects
        .stream()
        .filter(project -> project.status()
            .equalsIgnoreCase(ProjectStatus.PRODUCTION.name()))
        .collect(Collectors.toList());
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
    TreeMap<String, String> info = new TreeMap<>();

    SlickProject project = pproject.getProject();
    addDateProps(project, info);

    boolean isRTL = addOtherProps(project, info);

    return new SlimProject(
        project.id(),
        project.name(),
        project.language(),
        project.course(), project.countrycode(),
        ProjectStatus.valueOf(project.status()),
        project.displayorder(),

        pproject.hasModel(),
        isRTL,

        project.created().getTime(),
        project.lastimport().getTime(),

        pproject.getWebserviceHost(),

        pproject.getPort(),
        pproject.getModelsDir(),

        project.first(),
        project.second(),
        pproject.isOnIOS(),
        project.dominoid(),
        info);
  }

  private boolean addOtherProps(SlickProject project, Map<String, String> info) {
    boolean isRTL = false;
    if (getProjectStatus(project) != ProjectStatus.RETIRED) {
      List<CommonExercise> exercises = db.getExercises(project.id());
      isRTL = isRTL(exercises);
      info.put(NUM_ITEMS, "" + exercises.size());
//      logger.info("got " + exercises.size() + " ex for project #" + project.id());
      //info.put("# context", "" + exercises.size());
      if (project.dominoid() > 0) {
        info.put(DOMINO_ID, "" + project.dominoid());
      }
    }
    return isRTL;
  }

  private DateFormat format = new SimpleDateFormat();

  private void addDateProps(SlickProject project, Map<String, String> info) {
    info.put(CREATED, format.format(project.created()));
    info.put(MODIFIED, format.format(project.modified()));
  }
//
//  @NotNull
//  private String getHostOrDefault(Project project) {
//    String host = project.getProp(Project.WEBSERVICE_HOST);
//    if (host == null || host.isEmpty()) {
//      host = WEBSERVICE_HOST_DEFAULT;
//    }
//    return host;
//  }

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

//  private boolean isOnIOS(Project project) {
//    String prop2 = project.getProp(Project.SHOW_ON_IOS);
//    if (prop2 == null) prop2 = "false";
//    return prop2.equalsIgnoreCase("true");
//  }

//  private int getPort(Project project) {
//    try {
//      project.getPort();
//      String prop = getPropFromDB(project.id(), Project.WEBSERVICE_HOST_PORT);
//      if (prop == null || prop.isEmpty()) return -1;
//      else return Integer.parseInt(prop);
//    } catch (NumberFormatException e) {
//      logger.error("for " + project + " got " + e);
//      return -1;
//    }
//  }

//  private int getPort(SlickProject project) {
//    try {
//      String prop = getPropFromDB(project.id(), Project.WEBSERVICE_HOST_PORT);
//      if (prop == null || prop.isEmpty()) return -1;
//      else return Integer.parseInt(prop);
//    } catch (NumberFormatException e) {
//      logger.error("for " + project + " got " + e);
//      return -1;
//    }
//  }

//  private String getPropFromDB(int id, String modelsDir) {
//    return db.getProjectDAO().getPropValue(id, modelsDir);
//  }

  /**
   * @param projID
   * @param dominoID
   * @param sinceInUTC
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#addPending
   */
  @Override
  public ImportInfo getImportFromDomino(int projID, int dominoID, String sinceInUTC) {
    return dominoImport.getImportFromDomino(projID, dominoID, sinceInUTC, db.getUserDAO().getDominoAdminUser());
  }

  /**
   * @return
   */
  public List<ImportProjectInfo> getVocabProjects() {
    return dominoImport.getImportProjectInfos(db.getUserDAO().getDominoAdminUser());
  }

/*
  @NotNull
  private List<ImportProjectInfo> getImportProjectInfos(Bson query) {
    MongoCollection<Document> projects = pool
        .getMongoCollection("projects");

    FindIterable<Document> projection = projects
        .find(query)
        .projection(include(ID, CREATOR_ID, NAME, "content." + LANGUAGE_NAME, CREATE_TIME));

    List<ImportProjectInfo> imported = new ArrayList<>();

    SimpleDateFormat original = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    DominoExerciseDAO dominoExerciseDAO = new DominoExerciseDAO(serializer);

    for (Document project : projection) {
      logger.info("Got " + project);
      Date now = new Date();
      String string = project.getString(CREATE_TIME);
      try {
        now = original.parse(string);
      } catch (ParseException e) {
        logger.warn("got " + e);
      }

      Object content = project.get("content");

      Integer dominoID = project.getInteger(ID);
      ImportProjectInfo creatorId = new ImportProjectInfo(
          dominoID,
          project.getInteger(CREATOR_ID),
          project.getString(NAME),
          ((Document) content).getString(LANGUAGE_NAME),
          now.getTime()
      );

      imported.add(creatorId);

      FindIterable<Document> documents = pool.getMongoCollection("project_workflows").find(eq("projId", dominoID));

// OK go to json
      for (Document doc : documents) {
        String s = doc.toJson();
        ProjectWorkflow deserialize = serializer.deserialize(ProjectWorkflow.class, s);
        ImportProjectInfo importProjectInfoFromWorkflow = dominoExerciseDAO.getImportProjectInfoFromWorkflow(deserialize);
        creatorId.setUnitName(importProjectInfoFromWorkflow.getUnitName());
        creatorId.setChapterName(importProjectInfoFromWorkflow.getChapterName());
      }
    }

//    logger.info("Got " + imported);
    imported.forEach(proj -> logger.info(proj));

    return imported;
  }
*/

  /**
   * @return
   * @see IProjectManagement#getImportFromDomino
   * @see #getVocabProjects
   */
 /* @NotNull
  private List<ImportProjectInfo> getImportProjectInfos() {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Skill, VOCABULARY, FilterDetail.Operator.EQ));
    return getImportProjectInfos(options);
  }

  @NotNull
  private List<ImportProjectInfo> getImportProjectInfosByID(int id) {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Id, "" + id, FilterDetail.Operator.EQ));
    return getImportProjectInfos(options);
  }

  @NotNull
  private List<ImportProjectInfo> getImportProjectInfos(FindOptions<ProjectColumn> options) {
    List<ProjectDescriptor> projects1 = projectDelegate.getProjects(db.getUserDAO().getDominoAdminUser(),
        null,
        options,
        false, false, false
    );

    List<ImportProjectInfo> imported = new ArrayList<>();

    for (ProjectDescriptor project : projects1) {
//      logger.info("Got " + project);
      Date now = project.getCreateTime();

      int documentDBID = project.getCreator().getDocumentDBID();

      project.getContent().getLanguageName();

      int id = project.getId();
      ImportProjectInfo creatorId = new ImportProjectInfo(
          id,
          documentDBID,
          project.getName(),
          project.getContent().getLanguageName(),
          now.getTime()
      );

      imported.add(creatorId);

      ProjectWorkflow forProject = workflowDelegate.getForProject(id);

      if (forProject == null) {
        logger.warn("no workflow for project " + id);
      } else {
        List<TaskSpecification> taskSpecs = forProject.getTaskSpecs();

        for (TaskSpecification specification : taskSpecs) {
          Collection<MetadataList> metadataLists = specification.getMetadataLists();

          for (MetadataList list : metadataLists) {
//            logger.info("got " + list);

            List<MetadataSpecification> list1 = list.getList();
            for (MetadataSpecification specification1 : list1) {
              if (specification1.getDBName().equalsIgnoreCase("v-unit")) {
                creatorId.setUnitName(specification1.getLongName());
              } else if (specification1.getDBName().equalsIgnoreCase("v-chapter")) {
                creatorId.setChapterName(specification1.getLongName());
              }
            }
          }
        }
      }
    }

    return imported;
  }*/

  //@Override
/*
  public List<ImportDoc> getDocs(int dominoID) {
    List<ImportDoc> docs = new ArrayList<>();
    FindIterable<Document> documents =
        pool.getMongoCollection("document_heads").find(eq("projId", dominoID));

    for (Document doc : documents) {
      //logger.info("got doc " + doc);
      String s = doc.toJson();

      Integer id1 = doc.getInteger("_id");

      Object docContent = doc.get("docContent");
      Document docContent1 = (Document) docContent;

      //logger.info("got vocab " + docContent1);
      String s1 = docContent1.toJson();

      SimpleHeadDocumentRevision deserialize = serializer.deserialize(SimpleHeadDocumentRevision.class, s);
      // SimpleHeadDocumentRevision deserialize = serializer.deserialize(SimpleHeadDocumentRevision.class, s);
      //int id = deserialize.getId();
      //logger.info("Got doc id " + id1);
      //logger.info("Got SimpleHeadDocumentRevision " + deserialize);

      // deserialize.getDocument();
      VocabularyItem vocabularyItem = serializer.deserialize(VocabularyItem.class, s1);

      //logger.info("sub vocabularyItem " + vocabularyItem);
      //logger.info("sub meta data  " + vocabularyItem.getMetadataFields());
      //logger.info("sub components  " + vocabularyItem.getComponents());

      Date updateTime = deserialize.getUpdateTime();
      docs.add(new ImportDoc(id1, updateTime.getTime(), vocabularyItem));

      //break;
    }

    List<ImportDoc> docs2 = getDocs2(dominoID);

    docs2.forEach(importDoc -> logger.info("service "+ importDoc));
    return docs;
  }
*/

  /**
   * @see #getImportFromDomino
   */
//  private ChangedAndDeleted getDocs(int dominoID, long since) {
//    DBUser dominoAdminUser = db.getUserDAO().getDominoAdminUser();
//    ClientPMProject next = getClientPMProject(dominoID, db.getUserDAO().getDominoAdminUser());
//    return getChangedDocs(since, dominoAdminUser, next);
//  }
 /* private ClientPMProject getClientPMProject(int dominoID, DBUser dominoAdminUser) {
    FindOptions<ProjectColumn> options = new FindOptions<>();
    options.addFilter(new FilterDetail<>(ProjectColumn.Id, "" + dominoID, FilterDetail.Operator.EQ));
    List<ClientPMProject> projectDescriptor = projectDelegate.getHeavyProjects(dominoAdminUser, options);

    return projectDescriptor.iterator().next();
  }*/

/*  @NotNull
  private ChangedAndDeleted getChangedDocs(String sinceInUTC, DBUser dominoAdminUser, ClientPMProject next) {
    long then = System.currentTimeMillis();

    FindOptions<DocumentColumn> options1 = getSince(sinceInUTC);

    List<HeadDocumentRevision> documents1 = documentDelegate.getHeavyDocuments(next, dominoAdminUser, false, false, options1);

    List<ImportDoc> docs = new ArrayList<>();
    List<ImportDoc> deleted = new ArrayList<>();
    for (HeadDocumentRevision doc : documents1) {
      Integer id1 = doc.getId();
      VocabularyItem vocabularyItem = (VocabularyItem) doc.getDocument();
      docs.add(new ImportDoc(id1, doc.getUpdateTime().getTime(), vocabularyItem));
      logger.info("\t found changed " + vocabularyItem);
    }
    long now = System.currentTimeMillis();

    logger.info("getDocs : took " + (now - then) + " to get " + docs.size());

    return new ChangedAndDeleted(docs, deleted);
  }*/

/*  public class ChangedAndDeleted {
    private List<ImportDoc> changed;
    private List<ImportDoc> deleted;

    public ChangedAndDeleted(List<ImportDoc> changed, List<ImportDoc> deleted) {
      this.changed = changed;
      this.deleted = deleted;
    }

    public List<ImportDoc> getChanged() {
      return changed;
    }

    public List<ImportDoc> getDeleted() {
      return deleted;
    }
  }*/

  /*@NotNull
  private List<ImportDoc> getDeletedDocs(long since, DBUser dominoAdminUser, ClientPMProject next) {
    long then = System.currentTimeMillis();

    FindOptions<DocumentColumn> options1 = getSince(since);
    options1.addFilter(new FilterDetail<>());

    List<HeadDocumentRevision> documents1 = documentDelegate.getHeavyDocuments(next, dominoAdminUser, false, false, options1);

    List<ImportDoc> docs = new ArrayList<>();
    for (HeadDocumentRevision doc : documents1) {
      Integer id1 = doc.getId();
      VocabularyItem vocabularyItem = (VocabularyItem) doc.getDocument();
      docs.add(new ImportDoc(id1, doc.getUpdateTime().getTime(), vocabularyItem));
    }
    long now = System.currentTimeMillis();

    logger.info("getDocs : took " + (now - then) + " to get " + docs.size());

    return docs;
  }
*/

/*  @NotNull
  private FindOptions<DocumentColumn> getSince(String sinceInUTC) {
    FindOptions<DocumentColumn> options1 = new FindOptions<>();
    options1.addFilter(new FilterDetail<>(DocumentColumn.RevisionTime, sinceInUTC, FilterDetail.Operator.GT));
    return options1;
  }*/
}
