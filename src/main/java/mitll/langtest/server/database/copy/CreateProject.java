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

package mitll.langtest.server.database.copy;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.IProject;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.shared.project.*;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static mitll.langtest.server.ServerProperties.H2_HOST;
import static mitll.langtest.shared.project.ProjectProperty.*;

/**
 * Created by go22670 on 10/26/16.
 */
public class CreateProject {
  private static final Logger logger = LogManager.getLogger(CreateProject.class);

  public static final String MODEL_PROPERTY_TYPE = "model";
  private static final String PROPERTY = "property";
  public static final String TRUE = Boolean.TRUE.toString();
  private static final String MANDARIN_TRAD = "mandarinTrad";
  private static final String DEFAULT_MODEL_TYPE = ModelType.HYDRA.toString();
  public static final String H_2 = "h2";
  /**
   *
   */
  private final Set<Language> h2Languages;

  public CreateProject(Set<Language> h2Languages) {
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
                                      ProjectType projectType,
                                      ProjectStatus status) {
    String oldLanguage = getOldLanguage(db);
    String name = optName != null ? optName : oldLanguage;

    IProjectDAO projectDAO = db.getProjectDAO();
    int byName = projectDAO.getByName(name);

    if (byName == -1) {
      logger.info("createProjectIfNotExists checking for project with name '" + name + "' opt '" + optName + "' language '" + oldLanguage +
          "' - non found");

      boolean swapPrimaryAndAlt = false;
      swapPrimaryAndAlt = optName != null && (optName.equalsIgnoreCase(MANDARIN_TRAD));

      if (swapPrimaryAndAlt) {
        logger.info("createProjectIfNotExists : swap primary...");
      }
      byName = createProject(db, projectDAO, countryCode, name, course, displayOrder, typeOrder, projectType, status,
          swapPrimaryAndAlt);
      db.rememberProject(byName);
    } else {
      logger.info("createProjectIfNotExists found existing " +
          "\n\tproject  " + byName + " for" +
          "\n\tlanguage '" + oldLanguage + "'");
    }
    return byName;
  }

  int getExisting(DatabaseImpl db, String optName) {
    String oldLanguage = getOldLanguage(db);
    String name = optName != null ? optName : oldLanguage;
    return db.getProjectDAO().getByName(name);
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
   * @param status            - eval = audio per project
   * @param swapPrimaryAndAlt
   * @see #createProjectIfNotExists
   */
  private int createProject(DatabaseImpl db,
                            IProjectDAO projectDAO,
                            String countryCode,
                            String name,
                            String course,
                            int displayOrder,
                            Collection<String> typeOrder,
                            ProjectType projectType,
                            ProjectStatus status,
                            boolean swapPrimaryAndAlt) {
    Iterator<String> iterator = typeOrder.iterator();
    String firstType = iterator.hasNext() ? iterator.next() : "";
    String secondType = iterator.hasNext() ? iterator.next() : "";
    String language = getLanguage(db);

    if (language.equalsIgnoreCase("Mandarin")) language = Language.MANDARIN.toString();
    Language language1 = Language.valueOf(language.toUpperCase());

    int beforeLoginUser = db.getUserDAO().getBeforeLoginUser();
    int projectID =
        addProject(projectDAO,
            beforeLoginUser,
            name,
            language1,
            course,
            countryCode,
            projectType,
            status,
            displayOrder,
            firstType,
            secondType, -1);

    addProjectProperties(db, projectDAO, projectID);
    if (status == ProjectStatus.EVALUATION) {
      addModelProp(projectDAO, projectID, AUDIO_PER_PROJECT, TRUE);
    }
    if (swapPrimaryAndAlt) {
      addDefinedProperty(projectDAO, projectID, SWAP_PRIMARY_AND_ALT, TRUE);
    }
    addDefaultHostProperty(language1, projectDAO, projectID);
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
   * All new projects start in development
   *
   * @param daoContainer
   * @param projectServices
   * @param info
   * @param creator
   * @return false if name already exists
   * @see mitll.langtest.server.services.ProjectServiceImpl#create
   */
  public int createProject(DAOContainer daoContainer,
                           ProjectServices projectServices,
                           ProjectInfo info,
                           int creator) {
    IProjectDAO projectDAO = daoContainer.getProjectDAO();
    Language language = info.getLanguage();
    int byName = projectDAO.getByLanguageAndName(language.toString(), info.getName());


    if (byName == -1) {
      logger.info("createProject : Create new " + info);
      if (creator == -1) {
        creator = daoContainer.getUserDAO().getBeforeLoginUser();
      }
      int dominoID = info.getDominoID();
      int projectID = addProject(projectDAO,
          creator,
          info.getName(),
          language,
          info.getCourse(),
          info.getCountryCode().isEmpty() ? language.getCC() : info.getCountryCode(),
          info.getProjectType(),
          ProjectStatus.DEVELOPMENT,
          info.getDisplayOrder(),
          info.getFirstType(),
          info.getSecondType(),
          dominoID);

      String host = info.getHost();
      if (host.isEmpty()) {
        host = h2Languages.contains(language) ? H_2 : IProject.WEBSERVICE_HOST_DEFAULT;
        logger.info("createProject choosing host for " + language + " = " + host);
      } else {
        logger.info("createProject host=" + host);
      }
      addModelProp(projectDAO, projectID, WEBSERVICE_HOST, host);
      addModelProp(projectDAO, projectID, WEBSERVICE_HOST_PORT, "" + info.getPort());
      addModelProp(projectDAO, projectID, MODELS_DIR, "" + info.getModelsDir());

      String modelTypeValue = info.getPropertyValue().get(MODEL_TYPE.toString());
      if (modelTypeValue == null) {
        logger.warn("no model type in " + info.getPropertyValue().keySet());
        modelTypeValue = DEFAULT_MODEL_TYPE;
      }

      addModelProp(projectDAO, projectID, MODEL_TYPE, modelTypeValue);

      info.getPropertyValue().forEach((k, v) -> addProperty(projectDAO, projectID, k, v));

      projectServices.rememberProject(projectID);

      if (dominoID != -1) {
        logger.info("sync with domino project " + dominoID);

      }
      return projectID;
    } else {
      logger.info("createProject : PROJECT EXISTS with name " + info.getName() + " : create new " + info);
      return -1;
    }
  }

  private void addProperty(IProjectDAO projectDAO, int projectID, String k, String v) {
    projectDAO.addProperty(projectID, k, v, PROPERTY, "");
  }

  private void addDefinedProperty(IProjectDAO projectDAO, int projectID, ProjectProperty defProp, String v) {
    projectDAO.addProperty(projectID, defProp, v, PROPERTY, "");
  }

  private void addDefaultHostProperty(Language language, IProjectDAO projectDAO, int projectID) {
    if (h2Languages.contains(language)) {
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
                         Language language,
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

  String getOldLanguage(DatabaseImpl db) {
    return db.getLanguage();
  }


  /**
   * netprof update
   *
   * @param db
   * @param projectID
   * @return
   * @see CopyToPostgres#copyOneConfig
   */
  long getSinceWhenLastNetprof(DatabaseImpl db, int projectID) {
    return getSlickProject(db, projectID).lastnetprof().getTime();
  }

  long getSinceCreated(DatabaseImpl db, int projectID) {
    SlickProject project1 = getSlickProject(db, projectID);
    long netprofUpdate = project1.created().getTime();
    logger.info("\n\n\n getSinceCreated is at " + new Date(netprofUpdate));
    return netprofUpdate;
  }

  /**
   * Assumes no change in target project...
   *
   * @param db
   * @param projectID
   * @return
   */
  long getSinceWhenResults(DatabaseImpl db, int projectID) {
    List<Long> latest = new ArrayList<>();
    db.getResultDAO().getMonitorResults(projectID).stream().max((o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp())).ifPresent(monitorResult -> latest.add(monitorResult.getTimestamp()));
    long netprofUpdate = latest.isEmpty() ? 0 : latest.iterator().next();

    logger.info("\n\n\n latest result is at " + new Date(netprofUpdate));
    return netprofUpdate;
  }

  void updateNetprof(DatabaseImpl db, int projectID, long sinceWhen) {
    SlickProject byID = getSlickProject(db, projectID);
    if (!db.getProjectDAO().easyUpdateNetprof(byID, sinceWhen)) {
      logger.warn("couldn't update project " + projectID);
    }
    db.close();
  }

  private SlickProject getSlickProject(DatabaseImpl db, int projectID) {
    return db.getProjectDAO().getByID(projectID);
  }

/*  private static class Pair {
    final String language;
    final String cc;

    public Pair(String language, String cc) {
      this.language = language;
      this.cc = cc;
    }
  }*/
}
