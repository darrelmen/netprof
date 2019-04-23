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

package mitll.langtest.server.database.project;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.copy.CreateProject;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.*;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickProjectProperty;
import mitll.npdata.dao.SlickUpdateDominoPair;
import mitll.npdata.dao.project.ProjectDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

import static mitll.langtest.shared.project.ProjectProperty.*;

public class ProjectDAO extends DAO implements IProjectDAO {
  private static final Logger logger = LogManager.getLogger(ProjectDAO.class);
  private static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";
  private static final long MIN = 60 * 1000L;
  private static final long HOUR = 60 * MIN;
  private static final long DAY = 24 * HOUR;
  private static final long YEAR = 365 * DAY;

  private final ProjectDAOWrapper dao;
  private final ProjectPropertyDAO propertyDAO;
  private final IUserExerciseDAO userExerciseDAO;
  private final DatabaseImpl databaseImpl;

  /**
   * @param database
   * @param dbConnection
   * @param userExerciseDAO
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public ProjectDAO(Database database, DBConnection dbConnection, IUserExerciseDAO userExerciseDAO, DatabaseImpl databaseImpl) {
    super(database);
    propertyDAO = new ProjectPropertyDAO(database, dbConnection);
    dao = new ProjectDAOWrapper(dbConnection);
    this.userExerciseDAO = userExerciseDAO;
    this.databaseImpl = databaseImpl;
  }

  public int ensureDefaultProject(int defaultUser) {
    SlickProject defaultProject = getDefaultProject();
    if (defaultProject == null) {
      add(defaultUser,
          System.currentTimeMillis(),
          DEFAULT_PROJECT,
          Language.UNKNOWN,
          "",
          ProjectType.DEFAULT,
          ProjectStatus.RETIRED,
          "", "", "",
          0, -1);
      defaultProject = getDefaultProject();
      return defaultProject == null ? -1 : defaultProject.id();
    } else {
      return defaultProject.id();
    }
  }

  public int getDefault() {
    SlickProject defaultProject = getDefaultProject();
    return defaultProject == null ? -1 : defaultProject.id();
  }

  @Override
  public boolean exists(int projid) {
    Collection<SlickProject> slickProjects = dao.byID(projid);
    return !slickProjects.isEmpty();
  }

  public SlickProject getByID(int projid) {
    Collection<SlickProject> slickProjects = dao.byID(projid);
    return slickProjects.isEmpty() ? null : slickProjects.iterator().next();
  }

  /**
   * Don't update the project properties...
   *
   * @param changed
   * @return
   * @see mitll.langtest.server.domino.ProjectSync#updateProjectIfSomethingChanged
   */
  @Override
  public boolean easyUpdate(SlickProject changed) {
    return dao.update(changed) > 0;
  }

  @Override
  public boolean easyUpdateNetprof(SlickProject changed, long sinceWhen) {
    changed.updateNetprof(sinceWhen);
    return dao.update(changed) > 0;
  }

  /**
   * Why some things are slots on SlickProject and why some things are project properties is kinda arbitrary...
   *
   * @param userid
   * @param projectInfo
   * @return
   * @see mitll.langtest.server.services.ProjectServiceImpl#update
   * @see mitll.langtest.client.project.ProjectEditForm#updateProject
   */
  @Override
  public boolean update(int userid, ProjectInfo projectInfo) {
    int projid = projectInfo.getID();
    Project currentProject = database.getProject(projid);

    int dominoID = projectInfo.getDominoID();
    SlickProject changed = new SlickProject(
        projid,
        userid,
        new Timestamp(projectInfo.getCreated()),   // created
        new Timestamp(System.currentTimeMillis()), // modified - now!
        new Timestamp(projectInfo.getLastImport()),// last import - maintain it
        new Timestamp(projectInfo.getLastNetprof()),// last netprof 1 update - maintain it
        projectInfo.getName(),
        projectInfo.getLanguage().toString(),
        projectInfo.getCourse(),
        projectInfo.getProjectType().toString(),
        projectInfo.getStatus().toString(),
        projectInfo.getFirstType(),
        projectInfo.getSecondType(),
        getCountryCode(projectInfo),
        currentProject.getProject().ltsClass(),
        dominoID,
        projectInfo.getDisplayOrder()
    );

    boolean differentDomino = dominoID != currentProject.getProject().dominoid();
    if (differentDomino) {
      logger.info("changed domino project to " + dominoID);
    }
    boolean checkForDominoIDs = differentDomino || userExerciseDAO.areThereAnyUnmatched(projid);

    if (checkForDominoIDs) {
      setDominoIDOnExercises(projid, dominoID);
    }

    boolean didUpdate = easyUpdate(changed);
    if (!didUpdate) logger.error("update : couldn't update " + changed);
    boolean updateProps = updateProperties(projectInfo);
    boolean didChange = didUpdate || updateProps;

    if (didChange) {
      currentProject.clearPropCache();
//      logger.info("update for " + projid);
    } else {
      logger.warn("update : didn't update " + projectInfo + " for current " + currentProject);
    }

    return didChange;
  }

  /**
   * @param project
   * @return true if it changed domino ids
   */
  @Override
  public boolean maybeSetDominoIDs(Project project) {
    int projectID = project.getID();
    int dominoID = project.getProject().dominoid();
    long then = System.currentTimeMillis();
    if (dominoID == -1) {
      logger.info("no domino id yet on " + project);
      return false;
    } else if (userExerciseDAO.areThereAnyUnmatched(projectID)) {
      setDominoIDOnExercises(projectID, dominoID);
      long now = System.currentTimeMillis();
      logger.info("maybeSetDominoIDs on " + project.getName() + " took " + (now - then) + " millis");

      return true;
    } else {
      return false;
    }
  }

  /**
   * Set the domino id on each exercise. Might change if we change the matching domino project.
   *
   * So it could be we populated domino directly from a netprof excel dump.  Then the netprof ids are from the database.
   *
   * @param projid
   * @param dominoProjectID
   */
  private void setDominoIDOnExercises(int projid, int dominoProjectID) {
    Map<String, Integer> oldToID = userExerciseDAO.getNpToExID(projid);
    logger.info("setDominoIDOnExercises : need to match up domino IDs - found " + oldToID.size());
    Set<String> missing = new HashSet<>();
    Map<String, Integer> npToDomino = databaseImpl.getProjectManagement().getNpToDomino(dominoProjectID);
    List<SlickUpdateDominoPair> updateDominoPairs = new ArrayList<>();
    Project project = databaseImpl.getProject(projid);

    List<Integer> foundByNativeExID = new ArrayList<>();
    npToDomino.forEach((npid, dominoID) -> {
      Integer exIDForNPID = oldToID.get(npid);
      if (exIDForNPID == null) {
        boolean foundByEXID = false;
        try {
          int netprofExID = Integer.parseInt(npid);

          CommonExercise exerciseByID = project.getExerciseByID(netprofExID);
          if (exerciseByID != null) {
            updateDominoPairs.add(new SlickUpdateDominoPair(netprofExID, dominoID));
            foundByEXID = true;
            foundByNativeExID.add(netprofExID);
          }
        } catch (NumberFormatException e) {
          // logger.info("couldn't parse it")
        }

        if (!foundByEXID) {
//          logger.warn("setDominoIDOnExercises missing exercise for " + npid + " and domino " + dominoID);
          missing.add(npid);
        }
      } else {
        updateDominoPairs.add(new SlickUpdateDominoPair(exIDForNPID, dominoID));
      }
    });
    logger.info("setDominoIDOnExercises updateDominoPairs - found          " + updateDominoPairs.size());
    logger.info("setDominoIDOnExercises updateDominoPairs - native match = " + foundByNativeExID.size());
    if (!missing.isEmpty()) {
      logger.warn("\n\n\nsetDominoIDOnExercises - missing " + missing.size());
    }
    int i = userExerciseDAO.updateDominoBulk(updateDominoPairs);
    if (i != updateDominoPairs.size()) {
      logger.warn("setDominoIDOnExercises only did " + i + " not " + updateDominoPairs.size());
    }
  }

  /**
   * @param projectInfo
   * @return
   * @see #update(int, ProjectInfo)
   */
  private boolean updateProperties(ProjectInfo projectInfo) {
    int projid = projectInfo.getID();

    boolean didChange = addOrUpdateProperty(projid, WEBSERVICE_HOST, projectInfo.getHost());
    didChange |= addOrUpdateProperty(projid, WEBSERVICE_HOST_PORT, "" + projectInfo.getPort());
    didChange |= addOrUpdateProperty(projid, MODELS_DIR, projectInfo.getModelsDir());
    didChange |= addOrUpdateProperty(projid, MODEL_TYPE, projectInfo.getModelType().toString());
    didChange |= addOrUpdateBooleanProperty(projid, SHOW_ON_IOS, projectInfo.isShowOniOS());
    didChange |= addOrUpdateBooleanProperty(projid, AUDIO_PER_PROJECT, projectInfo.isAudioPerProject());
    return didChange;
  }

  @NotNull
  private String getCountryCode(ProjectInfo projectInfo) {
    String countryCode = projectInfo.getCountryCode();

    String ccFromLang = projectInfo.getLanguage().getCC();//new CreateProject(database.getServerProps().getHydra2Languages()).getCC(projectInfo.getLanguage());
    if (!ccFromLang.equals(countryCode)) {
      logger.warn("getCountryCode : setting country code to " + countryCode +
          " to be consistent with the language " + projectInfo.getLanguage() + " = " + ccFromLang);
      countryCode = ccFromLang;
    }
    return countryCode;
  }

//  private boolean addOrUpdateBooleanProperty(int projid, String key, boolean newValue) {
//    return addOrUpdateProperty(projid, key, newValue ? "true" : "false");
//  }

  private boolean addOrUpdateBooleanProperty(int projid, ProjectProperty projectProperty, boolean newValue) {
    return addOrUpdateProperty(projid, projectProperty.getName(), newValue ? "true" : "false");
  }

  @Override
  public boolean addOrUpdateProperty(int projid, ProjectProperty projectProperty, String newValue) {
    return addOrUpdateProperty(projid, projectProperty.getName(), newValue);
  }

  private boolean addOrUpdateProperty(int projid, String key, String newValue) {
    return addOrUpdateProperty(projid, key, CreateProject.MODEL_PROPERTY_TYPE, newValue);
  }

  /**
   * @param projid
   * @param key
   * @param type
   * @param newValue
   * @return true if changed
   */
  private boolean addOrUpdateProperty(int projid, String key, String type, String newValue) {
    logger.info("addOrUpdateProperty project " + projid + " : " + key + "=" + newValue);

    ProjectPropertyDAO propertyDAO = getProjectPropertyDAO();
    Collection<SlickProjectProperty> slickProjectProperties = propertyDAO.byProjectAndKey(projid, key);
    if (slickProjectProperties.isEmpty()) {
      propertyDAO.add(projid, System.currentTimeMillis(), key, newValue, type, "");
      return true;
    } else {
      if (slickProjectProperties.size() > 1)
        logger.error("addOrUpdateProperty got back " + slickProjectProperties.size() + " properties for " + key);

      SlickProjectProperty next = slickProjectProperties.iterator().next();

      if (next.value().equals(newValue)) {
        return false;
      } else {
        // logger.info("addOrUpdateProperty before " + next);
        SlickProjectProperty copy = propertyDAO.getCopy(next, key, newValue);
        //logger.info("addOrUpdateProperty after  " + next);
        return propertyDAO.update(copy);
      }
    }
  }

  @Override
  public List<String> getListProp(int projid, ProjectProperty projectProperty) {
    if (projectProperty.getType() != PropertyType.LIST) {
      logger.warn("getListProp " + projectProperty + " is not a list property ");
    }
    return Arrays.asList(getPropValue(projid, projectProperty.getName()).split(","));
  }

  public String getDefPropValue(int projid, ProjectProperty projectProperty) {
    return getPropValue(projid, projectProperty.getName());
  }

  public boolean getShouldSwap(int projid) {
    return getPropValue(projid, SWAP_PRIMARY_AND_ALT.getName()).equalsIgnoreCase("TRUE");
  }

  @Override
  public String getPropValue(int projid, String key) {
    Collection<SlickProjectProperty> slickProjectProperties = propertyDAO.byProjectAndKey(projid, key);
    return slickProjectProperties.isEmpty() ? "" : slickProjectProperties.iterator().next().value();
  }

  /**
   * @param projid
   * @return
   * @see #update
   */
  public Map<String, String> getProps(int projid) {
    Collection<SlickProjectProperty> slickProjectProperties = propertyDAO.getAllForProject(projid);
    Map<String, String> keyToValue = new HashMap<>(slickProjectProperties.size());
    slickProjectProperties.forEach(slickProjectProperty -> keyToValue.put(slickProjectProperty.key(), slickProjectProperty.value()));
    return keyToValue;
  }

  public ProjectPropertyDAO getProjectPropertyDAO() {
    return propertyDAO;
  }

  /**
   * @param project
   * @param key
   * @param value
   * @param propertyType
   * @param parent
   * @see mitll.langtest.server.database.copy.CreateProject#createProject
   */
  public void addProperty(int project, String key, String value, String propertyType, String parent) {
    propertyDAO.add(project, System.currentTimeMillis(), key, value, propertyType, parent);
  }

  public void addProperty(int project, ProjectProperty projectProperty, String value, String propertyType, String parent) {
    propertyDAO.add(project, System.currentTimeMillis(), projectProperty.getName(), value, propertyType, parent);
  }

  /**
   * @return
   * @see #ensureDefaultProject
   */
  private SlickProject getDefaultProject() {
    Collection<SlickProject> aDefault = dao.getDefault();
    if (aDefault.isEmpty()) {
      return null;
    } else {
      return aDefault.iterator().next();
    }
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param userid
   * @param name
   * @param language
   * @param course
   * @param firstType
   * @param secondType
   * @param countryCode
   * @param displayOrder
   * @param projectType
   * @param dominoID
   * @return
   * @paraxm isDev
   * @see mitll.langtest.server.database.copy.CreateProject#addProject
   */
  public int add(int userid, String name, Language language, String course,
                 String firstType, String secondType, String countryCode, int displayOrder,
                 ProjectType projectType, ProjectStatus status, int dominoID) {
    return add(userid, System.currentTimeMillis(), name, language, course, projectType,
        status,
        firstType, secondType, countryCode, displayOrder, dominoID);
  }

  /**
   * Really does delete it - could take a long time if a big project.
   *
   * NOTE :
   * NOTE : this will lock up the database for days on real databases BE CAREFUL
   * NOTE :
   *
   * <p>
   * Mainly called from drop.sh or tests
   * In general we want to retire projects when we don't want them visible.
   *
   * @param id
   * @seex mitll.langtest.server.database.copy.CopyToPostgres#dropOneConfig
   */
  public boolean delete(int id) {
    // logger.info("delete project #" + id);
    return dao.delete(id) > 0;
  }

/*
  public boolean deleteAllBut(int id) {
    // logger.info("delete project #" + id);
    return dao.deleteAllBut(id) > 0;
  }
*/

  /**
   * TODO : consider adding lts class
   * TODO : consider adding domino project id
   *
   * @param userid
   * @param modified
   * @param name
   * @param language
   * @param course
   * @param type
   * @param status
   * @param firstType
   * @param secondType
   * @param countryCode
   * @param dominoID
   * @return
   * @see #ensureDefaultProject
   */
  @Override
  public int add(int userid,
                 long modified,
                 String name,
                 Language language,
                 String course,
                 ProjectType type,
                 ProjectStatus status,
                 String firstType,
                 String secondType,
                 String countryCode,
                 int displayOrder,
                 int dominoID) {
    Timestamp created = new Timestamp(modified);
    Timestamp lastNetprof = new Timestamp(modified - (5 * YEAR));
    return dao.insert(new SlickProject(
        -1,
        userid,
        created,
        created,
        created,
        lastNetprof,
        name,
        language.toString(),
        course,
        type.toString(),
        status.toString(),
        firstType,
        secondType,
        countryCode,
        "",
        dominoID,
        displayOrder));
  }

  /**
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getReport
   */
  @Override
  public Collection<SlickProject> getAll() {
    return dao.getAll();
  }

  @Override
  public int getNumProjects() {
    return dao.countAll();
  }

  @Override
  public int getByName(String name) {
    return dao.byName(name);
  }

  @Override
  public int getByLanguageAndName(String language, String name) {
    return dao.byLanguageAndName(language, name);
  }

  @Override
  public int getByLanguageProductionOnly(String language) {
    return dao.byLanguageProductionOnly(language);
  }

  /**
   * @see ProjectManagement#getProjectsForLanguage(String)
   * @param language
   * @return
   */
  @Override
  public List<Integer> getByLanguage(String language) {
    List<Integer> ids = dao.byLanguage(language);
    return ids;
  }
}
