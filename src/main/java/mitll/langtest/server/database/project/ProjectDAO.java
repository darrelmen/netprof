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
import mitll.langtest.server.database.user.UserProjectDAO;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickProjectProperty;
import mitll.npdata.dao.project.ProjectDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mitll.langtest.server.ServerProperties.MODELS_DIR;
import static mitll.langtest.server.database.exercise.Project.*;

public class ProjectDAO extends DAO implements IProjectDAO {
  private static final Logger logger = LogManager.getLogger(ProjectDAO.class);
  private static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";

  private final ProjectDAOWrapper dao;
  private final ProjectPropertyDAO propertyDAO;
  private final UserProjectDAO userProjectDAO;
  private SlickProject first;

  /**
   * @param database
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public ProjectDAO(Database database, DBConnection dbConnection) {
    super(database);
    propertyDAO = new ProjectPropertyDAO(database, dbConnection);
    dao = new ProjectDAOWrapper(dbConnection);
    userProjectDAO = new UserProjectDAO(dbConnection);
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
   * @see mitll.langtest.server.services.ProjectServiceImpl#updateProjectIfSomethingChanged(int, Collection, Collection, SlickProject, long)
   */
  @Override
  public boolean easyUpdate(SlickProject changed) {
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
        projectInfo.getName(),
        projectInfo.getLanguage(),
        projectInfo.getCourse(),
        currentProject.getProject().kind(),
        projectInfo.getStatus().toString(),
        projectInfo.getFirstType(),
        projectInfo.getSecondType(),
        getCountryCode(projectInfo),
        currentProject.getProject().ltsClass(),
        projectInfo.getDominoID(),
        projectInfo.getDisplayOrder()
    );

    boolean didUpdate = easyUpdate(changed);
    if (!didUpdate) logger.error("couldn't update " + changed);
    boolean didChange = didUpdate || updateProperties(projectInfo);

    if (didChange) {
      currentProject.clearPropCache();
      logger.info("update for " + projid);
    } else {
      logger.warn("update : didn't update " + projectInfo + " for current " + currentProject);
    }

    return didChange;
  }

  private boolean updateProperties(ProjectInfo projectInfo) {
    int projid = projectInfo.getID();
//    Map<String, String> props = getProps(projid);

    boolean didChange = addOrUpdateProperty(projid, WEBSERVICE_HOST, projectInfo.getHost());

//    String currentHost = props.get(WEBSERVICE_HOST);
//    didChange |= currentHost == null || !currentHost.equalsIgnoreCase(newHost);

    //String newPort = "" + projectInfo.getPort();
    didChange |= addOrUpdateProperty(projid, WEBSERVICE_HOST_PORT, "" + projectInfo.getPort());

//    String currentHostPort = props.get(WEBSERVICE_HOST_PORT);
//    didChange |= currentHostPort == null || !currentHostPort.equalsIgnoreCase(newPort);

    //String newModels = projectInfo.getModelsDir();
    didChange |= addOrUpdateProperty(projid, MODELS_DIR, projectInfo.getModelsDir());

//    String currentModels = props.get(MODELS_DIR);
//    didChange |= currentModels == null || !currentModels.equalsIgnoreCase(newModels);

//    String showOnIOS = projectInfo.isShowOniOS() ? "true" : "false";
    didChange |= addOrUpdateProperty(projid, SHOW_ON_IOS, projectInfo.isShowOniOS() ? "true" : "false");
//    String currentShowOnIOS = props.get(SHOW_ON_IOS);
//    didChange |= currentShowOnIOS == null || !currentShowOnIOS.equalsIgnoreCase(showOnIOS);
    return didChange;
  }

  @NotNull
  private String getCountryCode(ProjectInfo projectInfo) {
    String countryCode = projectInfo.getCountryCode();

    String ccFromLang = new CreateProject(database.getServerProps().getHydra2Languages()).getCC(projectInfo.getLanguage());
    if (!ccFromLang.equals(countryCode)) {
      logger.warn("update : setting country code to " + countryCode +
          " to be consistent with the language " + projectInfo.getLanguage());
      countryCode = ccFromLang;
    }
    return countryCode;
  }

  private boolean addOrUpdateProperty(int projid, String key, String newValue) {
    logger.info("addOrUpdateProperty project " + projid + " : " + key + "=" + newValue);

    ProjectPropertyDAO propertyDAO = getProjectPropertyDAO();
    Collection<SlickProjectProperty> slickProjectProperties = propertyDAO.byProjectAndKey(projid, key);
    if (slickProjectProperties.isEmpty()) {
      propertyDAO.add(projid, System.currentTimeMillis(), key, newValue, CreateProject.MODEL_PROPERTY_TYPE, "");
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
   * @param isDev
   * @param dominoID
   * @return
   * @see mitll.langtest.server.database.copy.CreateProject#addProject
   */
  public int add(int userid, String name, String language, String course,
                 String firstType, String secondType, String countryCode, int displayOrder, boolean isDev, int dominoID) {
    return add(userid, System.currentTimeMillis(), name, language, course, ProjectType.NP,
        isDev ? ProjectStatus.DEVELOPMENT : ProjectStatus.PRODUCTION,
        firstType, secondType, countryCode, displayOrder, dominoID);
  }


  public SlickProject getFirst() {
    if (first == null) {
      List<SlickProject> all = dao.getAll();
      first = all.isEmpty() ? null : all.iterator().next();
    }
    return first;
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
    return dao.insert(new SlickProject(
        -1,
        userid,
        created,
        created,
        created,
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

  @Override
  public Collection<SlickProject> getAll() {
    return dao.getAll();
  }

  @Override
  public int getByName(String name) {
    return dao.byName(name);
  }

  @Override
  public int getByLanguage(String language) {
    return dao.byLanguage(language);
  }

  /**
   * TODO : don't do two round trips to database.
   *
   * @param user
   * @return
   */
  @Override
  public SlickProject mostRecentByUser(int user) {
    int i = userProjectDAO.mostRecentByUser(user);
    if (i == -1) return null;
    else {
      Collection<SlickProject> slickProjectSeq = dao.byID(i);
      return slickProjectSeq.isEmpty() ? null : slickProjectSeq.iterator().next();
    }
  }
}
