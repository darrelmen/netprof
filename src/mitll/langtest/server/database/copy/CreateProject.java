package mitll.langtest.server.database.copy;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Properties;

/**
 * Created by go22670 on 10/26/16.
 */
class CreateProject {
  private static final Logger logger = LogManager.getLogger(CreateProject.class);

  /**
   * xTODO : what to do about pashto 1,2,3?
   *
   * @param db
   * @param countryCode
   * @param course
   * @param isDev
   * @return
   * @see CopyToPostgres#copyOneConfig
   */
  int createProjectIfNotExists(DatabaseImpl db, String countryCode, String optName, String course, int displayOrder, boolean isDev) {
    IProjectDAO projectDAO = db.getProjectDAO();
    String oldLanguage = getOldLanguage(db);
    String name = optName != null ? optName : oldLanguage;

    int byName = projectDAO.getByName(name);

    if (byName == -1) {
      logger.info("checking for project with name '" + name + "' opt '" + optName + "' language '" + oldLanguage +
          "' - non found");

      byName = createProject(db, projectDAO, countryCode, name, course, displayOrder, isDev);
      db.populateProjects();
    } else {
      logger.info("found project " + byName + " for language '" + oldLanguage + "'");
    }
    return byName;
  }

  /**
   * Ask the database for what the type order should be, e.g. [Unit, Chapter] or [Week, Unit] (from Dari)
   *
   * @param db
   * @param projectDAO
   * @param name
   * @param course
   * @param displayOrder
   * @param isDev
   * @see #createProjectIfNotExists
   */
  private int createProject(DatabaseImpl db, IProjectDAO projectDAO, String countryCode, String name, String course,
                            int displayOrder, boolean isDev) {
    Iterator<String> iterator = db.getTypeOrder(-1).iterator();
    String firstType = iterator.hasNext() ? iterator.next() : "";
    String secondType = iterator.hasNext() ? iterator.next() : "";
    String language = getOldLanguage(db);

    DominoUserDAOImpl dominoUserDAO = (DominoUserDAOImpl) db.getUserDAO();

    if (language.equals("msa")) language = "MSA";

    int byName = projectDAO.add(dominoUserDAO.getBeforeLoginUser(),
        name,
        language,
        course,
        firstType, secondType,
        countryCode, displayOrder, isDev);

    Properties props = db.getServerProps().getProps();
    for (String prop : ServerProperties.CORE_PROPERTIES) {
      String property = props.getProperty(prop);
      if (property != null) {
        projectDAO.addProperty(byName, prop, property);
      }
    }

    logger.info("created project " + byName);
    return byName;
  }

  private String getOldLanguage(DatabaseImpl db) {
    return db.getLanguage();
  }

}
