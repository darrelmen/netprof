package mitll.langtest.server.database.copy;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.shared.project.ProjectInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by go22670 on 10/26/16.
 */
public class CreateProject {
  private static final Logger logger = LogManager.getLogger(CreateProject.class);
  public static final String MODEL_PROPERTY_TYPE = "model";

  /**
   * @param db
   * @param countryCode
   * @param course
   * @param isDev
   * @param typeOrder
   * @return
   * @see CopyToPostgres#copyOneConfig
   */
  public int createProjectIfNotExists(DatabaseImpl db,
                                      String countryCode,
                                      String optName,
                                      String course,
                                      int displayOrder,
                                      boolean isDev,
                                      Collection<String> typeOrder) {
    String oldLanguage = getOldLanguage(db);
    String name = optName != null ? optName : oldLanguage;

    IProjectDAO projectDAO = db.getProjectDAO();
    int byName = projectDAO.getByName(name);

    if (byName == -1) {
      logger.info("createProjectIfNotExists checking for project with name '" + name + "' opt '" + optName + "' language '" + oldLanguage +
          "' - non found");

      byName = createProject(db, projectDAO, countryCode, name, course, displayOrder, isDev, typeOrder);
      db.rememberProject(byName);
    } else {
      logger.info("createProjectIfNotExists found project " + byName + " for language '" + oldLanguage + "'");
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
   * @param typeOrder
   * @see #createProjectIfNotExists
   */
  private int createProject(DatabaseImpl db,
                            IProjectDAO projectDAO,
                            String countryCode,
                            String name,
                            String course,
                            int displayOrder,
                            boolean isDev,
                            Collection<String> typeOrder) {
    Iterator<String> iterator = typeOrder.iterator();
    String firstType = iterator.hasNext() ? iterator.next() : "";
    String secondType = iterator.hasNext() ? iterator.next() : "";
    String language = getOldLanguage(db);

    if (language.equals("msa")) language = "MSA";
    if (language.equals("levantine")) language = "Levantine";

    int beforeLoginUser = db.getUserDAO().getBeforeLoginUser();
    int projectID =
        addProject(projectDAO,
            beforeLoginUser,
            name,
            language,
            course,
            countryCode,
            isDev,
            displayOrder,
            firstType,
            secondType);

    Properties props = db.getServerProps().getProps();
    for (String prop : ServerProperties.CORE_PROPERTIES) {
      String property = props.getProperty(prop);
      if (property != null) {
        projectDAO.addProperty(projectID, prop, property, MODEL_PROPERTY_TYPE, "");
      }
    }

    logger.info("createProject : created project " + projectID);
    return projectID;
  }

  /**
   * @param daoContainer
   * @param projectServices
   * @param info
   * @return false if name already exists
   * @see mitll.langtest.server.services.ProjectServiceImpl#create(ProjectInfo)
   */
  public boolean createProject(DAOContainer daoContainer,
                               ProjectServices projectServices,
                               ProjectInfo info) {
    IProjectDAO projectDAO = daoContainer.getProjectDAO();
    int byName = projectDAO.getByName(info.getName());

    logger.info("Create new " +info);

    if (byName == -1) {
      int projectID = addProject(projectDAO,
          daoContainer.getUserDAO().getBeforeLoginUser(),
          info.getName(),
          info.getLanguage(),
          info.getCourse(),
          info.getCountryCode().isEmpty() ? getCC(info.getLanguage()) : info.getCountryCode(),
          true,
          info.getDisplayOrder(),
          info.getFirstType(),
          info.getSecondType()
      );

      projectDAO.addProperty(projectID, ServerProperties.WEBSERVICE_HOST_PORT,
          "" + info.getPort(), MODEL_PROPERTY_TYPE, "");

      projectDAO.addProperty(projectID, ServerProperties.MODELS_DIR,
          "" + info.getModelsDir(), MODEL_PROPERTY_TYPE, "");

      for (Map.Entry<String, String> pair : info.getPropertyValue().entrySet()) {
        projectDAO.addProperty(projectID, pair.getKey(), pair.getValue(), "property", "");
      }
      projectServices.rememberProject(projectID);
      return true;
    } else {
      return false;
    }
  }

  private int addProject(IProjectDAO projectDAO,
                         int beforeLoginUser,
                         String name,
                         String language,
                         String course,
                         String countryCode,
                         boolean isDev, int displayOrder, String firstType, String secondType) {
    return projectDAO.add(
        beforeLoginUser,
        name,
        language,
        course,
        firstType,
        secondType,
        countryCode,
        displayOrder,
        isDev);
  }

  private String getOldLanguage(DatabaseImpl db) {
    return db.getLanguage();
  }

  /**
   * Add brazilian, serbo croatian, french, etc.
   * <p>
   * TODO : make t
   *
   * @param language
   * @return
   */
  public String getCC(String language) {
    List<Pair> languages = Arrays.asList(
        new Pair("croatian", "hr"),
        new Pair("dari", "af"),
        new Pair("egyptian", "eg"),
        new Pair("english", "us"),
        new Pair("farsi", "ir"),
        new Pair("french", "fr"),
        new Pair("german", "de"),
        new Pair("hindi", "in"),
        new Pair("korean", "kr"),
        new Pair("iraqi", "iq"),
        new Pair("japanese", "jp"),
        new Pair("levantine", "sy"),
        new Pair("mandarin", "cn"),
        new Pair("msa", "al"),
        new Pair("pashto", "af"),
        new Pair("portuguese", "pt"),
        new Pair("russian", "ru"),
        new Pair("serbian", "rs"),
        new Pair("sorani", "ku"),
        new Pair("spanish", "es"),
        new Pair("sudanese", "ss"),
        new Pair("tagalog", "ph"),
        new Pair("turkish", "tr"),
        new Pair("urdu", "pk"));


    Map<String, String> langToCode = new HashMap<>();
    for (Pair pair : languages) langToCode.put(pair.language, pair.cc);

    String cc = langToCode.get(language.toLowerCase());
    if (cc == null) {
      logger.error("\n\n\n\ncan't find a flag for " + language);
      cc = "us";
    }
    return cc;
  }

  private static class Pair {
    String language;
    String cc;

    public Pair(String language, String cc) {
      this.language = language;
      this.cc = cc;
    }
  }
}
