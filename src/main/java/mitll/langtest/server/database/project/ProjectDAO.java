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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.copy.CreateProject;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.ProjectType;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickProjectProperty;
import mitll.npdata.dao.project.ProjectDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

import static java.util.Calendar.YEAR;
import static mitll.langtest.shared.project.ProjectProperty.*;

public class ProjectDAO extends DAO implements IProjectDAO {
  private static final Logger logger = LogManager.getLogger(ProjectDAO.class);
  private static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";
  public static final long MIN = 60 * 1000L;
  public static final long HOUR = 60 * MIN;
  public static final long DAY = 24 * HOUR;
  public static final long YEAR = 365 * DAY;

  private final ProjectDAOWrapper dao;
  private final ProjectPropertyDAO propertyDAO;

  /**
   * @param database
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public ProjectDAO(Database database, DBConnection dbConnection) {
    super(database);
    propertyDAO = new ProjectPropertyDAO(database, dbConnection);
    dao = new ProjectDAOWrapper(dbConnection);
  }

  public int ensureDefaultProject(int defaultUser) {
    SlickProject defaultProject = getDefaultProject();
    if (defaultProject == null) {
      add(defaultUser,
          System.currentTimeMillis(),
          DEFAULT_PROJECT,
          "",
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
    return !dao.byID(projid).isEmpty();
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

    SlickProject changed = new SlickProject(
        projid,
        userid,
        new Timestamp(projectInfo.getCreated()),   // created
        new Timestamp(System.currentTimeMillis()), // modified - now!
        new Timestamp(projectInfo.getLastImport()),// last import - maintain it
        new Timestamp(projectInfo.getLastNetprof()),// last netprof 1 update - maintain it
        projectInfo.getName(),
        projectInfo.getLanguage(),
        projectInfo.getCourse(),
        projectInfo.getProjectType().toString(),
        projectInfo.getStatus().toString(),
        projectInfo.getFirstType(),
        projectInfo.getSecondType(),
        getCountryCode(projectInfo),
        currentProject.getProject().ltsClass(),
        projectInfo.getDominoID(),
        projectInfo.getDisplayOrder()
    );

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
   * @param projectInfo
   * @return
   * @see #update(int, ProjectInfo)
   */
  private boolean updateProperties(ProjectInfo projectInfo) {
    int projid = projectInfo.getID();

    boolean didChange = addOrUpdateProperty(projid, WEBSERVICE_HOST, projectInfo.getHost());
    didChange |= addOrUpdateProperty(projid, WEBSERVICE_HOST_PORT, "" + projectInfo.getPort());
    didChange |= addOrUpdateProperty(projid, MODELS_DIR, projectInfo.getModelsDir());
    boolean showOniOS = projectInfo.isShowOniOS();
    didChange |= addOrUpdateBooleanProperty(projid, SHOW_ON_IOS, showOniOS);
    didChange |= addOrUpdateBooleanProperty(projid, AUDIO_PER_PROJECT, projectInfo.isAudioPerProject());
    return didChange;
  }

  @NotNull
  private String getCountryCode(ProjectInfo projectInfo) {
    String countryCode = projectInfo.getCountryCode();

    String ccFromLang = new CreateProject(database.getServerProps().getHydra2Languages()).getCC(projectInfo.getLanguage());
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
  public int add(int userid, String name, String language, String course,
                 String firstType, String secondType, String countryCode, int displayOrder,
                 ProjectType projectType, ProjectStatus status, int dominoID) {
    return add(userid, System.currentTimeMillis(), name, language, course, projectType,
        status,
        firstType, secondType, countryCode, displayOrder, dominoID);
  }

  /**
   * Really does delete it - could take a long time if a big project.
   * <p>
   * Mainly called from drop.sh or tests
   * In general we want to retire projects when we don't want them visible.
   *
   * @param id
   * @see mitll.langtest.server.database.copy.CopyToPostgres#dropOneConfig
   */
  public boolean delete(int id) {
    // logger.info("delete project #" + id);
    return dao.delete(id) > 0;
  }

  public boolean deleteAllBut(int id) {
    // logger.info("delete project #" + id);
    return dao.deleteAllBut(id) > 0;
  }

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
                 String language,
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
        language,
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
  public int getByLanguage(String language) {
    return dao.byLanguage(language);
  }
}
