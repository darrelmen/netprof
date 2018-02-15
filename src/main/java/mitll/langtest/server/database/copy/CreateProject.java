package mitll.langtest.server.database.copy;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.ProjectType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static mitll.langtest.server.ServerProperties.H2_HOST;
import static mitll.langtest.server.database.exercise.Project.WEBSERVICE_HOST_DEFAULT;
import static mitll.langtest.shared.project.ProjectProperty.*;

/**
 * Created by go22670 on 10/26/16.
 */
public class CreateProject {
  private static final Logger logger = LogManager.getLogger(CreateProject.class);

  public static final String MODEL_PROPERTY_TYPE = "model";
  private static final String PROPERTY = "property";
  private final Set<String> h2Languages;

  public CreateProject(Set<String> h2Languages) {
    this.h2Languages = h2Languages;
  }

  /**
   * JUST FOR IMPORT FROM LEGACY h2 database language.
   *
   * @param db
   * @param countryCode
   * @param course
   * @param typeOrder
   * @param projectType
   * @param status
   * @return
   * @see CopyToPostgres#copyOneConfig
   */
  public int createProjectIfNotExists(DatabaseImpl db,
                                      String countryCode,
                                      String optName,
                                      String course,
                                      int displayOrder,
                                      Collection<String> typeOrder,
                                      ProjectType projectType, ProjectStatus status) {
    String oldLanguage = getOldLanguage(db);
    String name = optName != null ? optName : oldLanguage;

    IProjectDAO projectDAO = db.getProjectDAO();
    int byName = projectDAO.getByName(name);

    if (byName == -1) {
      logger.info("createProjectIfNotExists checking for project with name '" + name + "' opt '" + optName + "' language '" + oldLanguage +
          "' - non found");

      byName = createProject(db, projectDAO, countryCode, name, course, displayOrder, typeOrder, projectType, status);
      db.rememberProject(byName);
    } else {
      logger.info("createProjectIfNotExists found project " + byName + " for language '" + oldLanguage + "'");
    }
    return byName;
  }

  /**
   * JUST FOR IMPORT FROM LEGACY h2 database language.
   * <p>
   * Ask the database for what the type order should be, e.g. [Unit, Chapter] or [Week, Unit] (from Dari)
   *
   * @param db
   * @param projectDAO
   * @param name
   * @param course
   * @param displayOrder
   * @param typeOrder
   * @param projectType
   * @param status       - eval = audio per project
   * @see #createProjectIfNotExists
   */
  private int createProject(DatabaseImpl db,
                            IProjectDAO projectDAO,
                            String countryCode,
                            String name,
                            String course,
                            int displayOrder,
                            Collection<String> typeOrder,
                            ProjectType projectType, ProjectStatus status) {
    Iterator<String> iterator = typeOrder.iterator();
    String firstType = iterator.hasNext() ? iterator.next() : "";
    String secondType = iterator.hasNext() ? iterator.next() : "";
    String language = getLanguage(db);

    int beforeLoginUser = db.getUserDAO().getBeforeLoginUser();
    int projectID =
        addProject(projectDAO,
            beforeLoginUser,
            name,
            language,
            course,
            countryCode,
            projectType,
            status,
            displayOrder,
            firstType,
            secondType, -1);

    addProjectProperties(db, projectDAO, projectID);
    if (status == ProjectStatus.EVALUATION) {
      addModelProp(projectDAO, projectID, AUDIO_PER_PROJECT, Boolean.TRUE.toString());
    }
    addDefaultHostProperty(language, projectDAO, projectID);
    logger.info("createProject : created project " + projectID);
    return projectID;
  }

  @NotNull
  private String getLanguage(DatabaseImpl db) {
    String language = getOldLanguage(db);

    if (language.equals("msa")) language = "MSA";
    if (language.equals("levantine")) language = "Levantine";
    return language;
  }

  private void addProjectProperties(DatabaseImpl db, IProjectDAO projectDAO, int projectID) {
    Properties props = db.getServerProps().getProps();
    for (ProjectProperty prop : ServerProperties.CORE_PROPERTIES) {
      String property = props.getProperty(prop.getName());
      if (property != null) {
        addModelProp(projectDAO, projectID, prop, property);
      }
    }
  }

  /**
   * Let's by default put all new projects on
   *
   * @param daoContainer
   * @param projectServices
   * @param info
   * @return false if name already exists
   * @see mitll.langtest.server.services.ProjectServiceImpl#create
   */
  public int createProject(DAOContainer daoContainer,
                           ProjectServices projectServices,
                           ProjectInfo info) {
    IProjectDAO projectDAO = daoContainer.getProjectDAO();
    int byName = projectDAO.getByLanguageAndName(info.getLanguage(),info.getName());


    if (byName == -1) {
      logger.info("createProject : Create new " + info);
      int projectID = addProject(projectDAO,
          daoContainer.getUserDAO().getBeforeLoginUser(),
          info.getName(),
          info.getLanguage(),
          info.getCourse(),
          info.getCountryCode().isEmpty() ? getCC(info.getLanguage()) : info.getCountryCode(),
          info.getProjectType(),
          ProjectStatus.DEVELOPMENT,
          info.getDisplayOrder(),
          info.getFirstType(),
          info.getSecondType(),
          info.getDominoID());

      String host = info.getHost();
      if (host.isEmpty()) {
        host = h2Languages.contains(info.getLanguage()) ? "h2" : WEBSERVICE_HOST_DEFAULT;
        logger.info("createProject choosing host for " + info.getLanguage() + " = " + host);
      }
      else {
        logger.info("createProject host=" + host);
      }
      addModelProp(projectDAO, projectID, WEBSERVICE_HOST, host);
      addModelProp(projectDAO, projectID, WEBSERVICE_HOST_PORT, "" + info.getPort());
      addModelProp(projectDAO, projectID, MODELS_DIR, "" + info.getModelsDir());

      for (Map.Entry<String, String> pair : info.getPropertyValue().entrySet()) {
        projectDAO.addProperty(projectID, pair.getKey(), pair.getValue(), PROPERTY, "");
      }
      projectServices.rememberProject(projectID);
      return projectID;
    } else {
      logger.info("createProject : PROJECT EXISTS with name " + info.getName()+" : create new " + info);
      return -1;
    }
  }

  private void addDefaultHostProperty(String language, IProjectDAO projectDAO, int projectID) {
    if (h2Languages.contains(language.toLowerCase())) {
      logger.info("createProject: setting hydra host to " + H2_HOST);
      addModelProp(projectDAO, projectID, WEBSERVICE_HOST, H2_HOST);
    }
  }

  private void addModelProp(IProjectDAO projectDAO, int projectID, ProjectProperty webserviceHost, String host) {
    projectDAO.addProperty(projectID, webserviceHost, host, MODEL_PROPERTY_TYPE, "");
  }

  private int addProject(IProjectDAO projectDAO,
                         int beforeLoginUser,
                         String name,
                         String language,
                         String course,
                         String countryCode,
                         ProjectType projectType,
                         ProjectStatus status,
                         int displayOrder,
                         String firstType,
                         String secondType,
                         int dominoID) {
    return projectDAO.add(
        beforeLoginUser,
        name,
        language,
        course,
        firstType,
        secondType,
        countryCode,
        displayOrder,
        projectType,
        status,
        dominoID);
  }

  private String getOldLanguage(DatabaseImpl db) {
    return db.getLanguage();
  }

  /**
   * Add brazilian, serbo croatian, french, etc.
   * <p>
   * TODO : read this from a json config file
   *
   * @param language
   * @return
   * @see CopyToPostgres#copyOneConfigCommand
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
        new Pair("portuguese", "br"),
        new Pair("russian", "ru"),
        new Pair("serbian", "rs"),
        new Pair("sorani", "ku"),
        new Pair("spanish", "es"),
        new Pair("sudanese", "sd"),
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
