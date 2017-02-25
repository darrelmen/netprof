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
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.user.UserProjectDAO;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.project.ProjectDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.collection.Seq;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

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

    //  ensureDefaultProject(defaultUser);
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
          0);
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

  @Override
  public boolean update(int userid, ProjectInfo projectInfo) {
    Project currentProject = database.getProject(projectInfo.getID());

    SlickProject project = currentProject.getProject();
    Timestamp now = new Timestamp(System.currentTimeMillis());
    Timestamp created = new Timestamp(projectInfo.getCreated());

    SlickProject changed = new SlickProject(projectInfo.getID(),
        userid,
        created,
        now,
        projectInfo.getName(),
        projectInfo.getLanguage(),
        project.course(),
        project.kind(),
        projectInfo.getStatus().toString(),
        project.first(),
        project.second(),
        project.countrycode(),
        project.ltsClass(),
        project.dominoid(),
        project.displayorder()
    );

    boolean didChange = dao.update(changed) > 0;

    if (!didChange) {
      logger.error("didn't update " + projectInfo + " for current " + currentProject);
    }

    return didChange;
  }

  private SlickProject getDefaultProject() {
    Seq<SlickProject> aDefault = dao.getDefault();
    return aDefault.isEmpty() ? null : aDefault.iterator().next();
  }

  public ProjectPropertyDAO getProjectPropertyDAO() {
    return propertyDAO;
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
   * @return
   * @see mitll.langtest.server.database.copy.CreateProject#createProject
   */
  public int add(int userid, String name, String language, String course,
                 String firstType, String secondType, String countryCode, int displayOrder, boolean isDev) {
    return add(userid, System.currentTimeMillis(), name, language, course, ProjectType.NP,
        isDev ? ProjectStatus.DEVELOPMENT : ProjectStatus.PRODUCTION,
        firstType, secondType, countryCode, displayOrder);
  }


  public SlickProject getFirst() {
    if (first == null) {
      List<SlickProject> all = dao.getAll();
      first = all.isEmpty() ? null : all.iterator().next();
    }
    return first;
  }

  /**
   * Mainly called from drop.sh or tests
   * In general we want to retire projects when we don't want them visible.
   *
   * @param id
   * @seex PostgresTest#testDeleteEnglish
   */
  public void delete(int id) {
    dao.delete(id);
  }

  /**
   * TODO: Consider keeping an update history.
   *
   * @param project
   * @return
   */
  public boolean update(SlickProject project) {
    int update = dao.update(project);
    if (update == 0) {
      logger.error("update : no project with id for " + project);
    }
    return update > 0;
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
   * @return
   * @see #ensureDefaultProject(int)
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
                 int displayOrder) {
    Timestamp created = new Timestamp(modified);
    return dao.insert(new SlickProject(
        -1,
        userid,
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
        -1,
        displayOrder));
  }

  @Override
  public Collection<SlickProject> getAll() {
    return dao.getAll();
  }

  public void addProperty(int project, String key, String value) {
    propertyDAO.add(project, System.currentTimeMillis(), key, value);
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
    return i == -1 ? null : dao.byID(i).headOption().getOrElse(null);
  }
}
