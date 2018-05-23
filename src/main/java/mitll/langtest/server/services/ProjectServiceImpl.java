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

package mitll.langtest.server.services;

import mitll.langtest.client.project.ProjectEditForm;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.server.database.copy.CreateProject;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.domino.ProjectSync;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.exercise.DominoUpdateResponse;
import mitll.langtest.shared.project.DominoProject;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ProjectServiceImpl extends MyRemoteServiceServlet implements ProjectService {
  private static final Logger logger = LogManager.getLogger(ProjectServiceImpl.class);

  public static final String UPDATING_PROJECT_INFO = "updating project info";
  private static final String CREATING_PROJECT = "Creating project";
  private static final String DELETING_A_PROJECT = "deleting a project";

  private IProjectDAO getProjectDAO() {
    return db.getProjectDAO();
  }

  /**
   * @param languageChoice
   * @param name
   * @return
   * @see ProjectEditForm#checkNameOnBlur
   */
  @Override
  public boolean existsByName(String languageChoice, String name) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      return getProjectDAO().getByLanguageAndName(languageChoice, name) != -1;
    } else {
      throw getRestricted("exists by name");
    }
  }

  /**
   * @param info
   * @return
   * @see ProjectEditForm#updateProject
   */
  @Override
  public boolean update(ProjectInfo info) throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasAdminPerm(userIDFromSessionOrDB)) {
      logger.info("update for " +
          "\n\tuser    " + userIDFromSessionOrDB + " update" +
          "\n\tproject " + info);
      boolean update = getProjectDAO().update(userIDFromSessionOrDB, info);
      configureAndRefresh(userIDFromSessionOrDB, info.getID(), update);
      return update;
    } else {
      throw getRestricted(UPDATING_PROJECT_INFO);
    }
  }

  /*
    public void configureAndRefresh(boolean update, int projID) throws DominoSessionException, RestrictedOperationException {
      int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
      if (hasAdminPerm(userIDFromSessionOrDB)) {
        configureAndRefresh(userIDFromSessionOrDB, projID, update);
      } else {
        throw getRestricted(UPDATING_PROJECT_INFO);
      }
    }
  */
  private void configureAndRefresh(int userIDFromSessionOrDB, int projID, boolean update) {
    if (update) {
/*
      logger.info("update for " +
          "\n\tuser              " + userIDFromSessionOrDB +
          "\n\tconfigure project " + projID);
          */
      db.configureProject(db.getProject(projID), true);
    } else {
      logger.info("update for " +
          "\n\tuser                    " + userIDFromSessionOrDB +
          "\n\tNOT configuring project " + projID);
    }
    db.getProjectManagement().refreshProjects();
  }

  /**
   * @param newProject
   * @return project id of newly created project
   * @see ProjectEditForm#newProject
   */
  @Override
  public int create(ProjectInfo newProject) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      if (newProject.getModelsDir().isEmpty()) {
        setDefaultsIfMissing(newProject);
      }
      CreateProject createProject = new CreateProject(db.getServerProps().getHydra2Languages());
      return createProject.createProject(db, db, newProject);
    } else {
      throw getRestricted(CREATING_PROJECT);
    }
  }

  private void setDefaultsIfMissing(ProjectInfo newProject) {
    try {
      String language = newProject.getLanguage();

      logger.info("setDefaultsIfMissing for " + newProject);
      logger.info("setDefaultsIfMissing for language " + language);

      Collection<Project> productionProjects = db.getProjectManagement().getProductionProjects();

      logger.info("setDefaultsIfMissing examing " + productionProjects.size() + " production projects.");

      Project mostRecent = productionProjects.stream()
          .filter(project -> project.getLanguage().equalsIgnoreCase(language) && project.getModelsDir() != null)
          .max(Comparator.comparingLong(p -> p.getProject().modified().getTime()))
          .get();

      newProject.setModelsDir(mostRecent.getModelsDir());

      if (newProject.getPort() == -1) {
        newProject.setPort(mostRecent.getWebservicePort());
      }
    } catch (Exception e) {
      logger.info("Got " + e, e);
    }
  }

  @Override
  public boolean delete(int id) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      // boolean delete = getProjectDAO().delete(id);
      markDeleted(id);
      //if (delete) {
      db.getProjectManagement().forgetProject(id);
      // }
      // return delete;
      return true;
    } else {
      throw getRestricted(DELETING_A_PROJECT);
    }
  }

  /**
   * Adding exercises to a project!
   * Copies any existing audio for the language (from a production project) that has matching transcripts.
   * <p>
   * I.e. if there's already a recording of "dog" in another english project, just reuse that recording for your
   * new "dog" vocabulary item.  Good for projects that are subsets (or collages?) of existing content.
   * <p>
   * Does some sanity checking - the exercises come from a domino project:
   * <p>
   * 1) Is the project an existing project and is it associated with the domino project for this bundle of exercises?
   * 2) Is the project a new project (and hence has no domino id yet) and is there already another project bound
   * to this exercise bundle's domino project?
   *
   * @param projectid
   * @see mitll.langtest.client.project.ProjectChoices#showImportDialog
   */
  @Override
  public DominoUpdateResponse addPending(int projectid, boolean doChange) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      return db.getProjectSync().addPending(projectid, getImportUser(), doChange);
    } else {
      throw getRestricted("adding pending exercises");
    }
  }

  @Override
  public List<DominoProject> getDominoForLanguage(String lang) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      return db.getProjectSync().getDominoForLanguage(lang);
    } else {
      throw getRestricted("getting domino projects");
    }
  }

  @Override
  public String getProperty(int projid, ProjectProperty key) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      return getProjectDAO().getPropValue(projid, key.getName());
    } else {
      throw getRestricted("getProperty");
    }
  }

/*
  @Override
  public Integer getIntegerProperty(int projid, ProjectProperty key) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      String propValue = getProjectDAO().getPropValue(projid, key.getName());
      if (propValue != null) {
        try {
          return Integer.parseInt(propValue);
        } catch (NumberFormatException e) {
          return -1;
        }
      }
      else return -1;
    } else {
      throw getRestricted("getIntegerProperty");
    }
  }*/

  @Override
  public List<String> getListProperty(int projid, ProjectProperty key) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      String propValue = getProjectDAO().getPropValue(projid, key.getName());
      if (propValue != null) {
        String[] split = propValue.split(",");
        return Arrays.stream(split).filter(prop -> !prop.isEmpty()).collect(Collectors.toList());
      } else {
        return new ArrayList<>();
      }
    } else {
      throw getRestricted("getListProperty");
    }
  }

  @Override
  public boolean setProperty(int projid, ProjectProperty key, String newValue) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      return getProjectDAO().addOrUpdateProperty(projid, key, newValue);
    } else {
      throw getRestricted("setProperty");
    }
  }

  @Override
  public boolean setListProperty(int projid, ProjectProperty key, List<String> newValue) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      return getProjectDAO().addOrUpdateProperty(projid, key, newValue.toString().replaceAll("\\[", "").replaceAll("]", ""));
    } else {
      throw getRestricted("setProperty");
    }
  }

  private void markDeleted(int projectid) {
    SlickProject slickProject = db.getProject(projectid).getProject();
    slickProject.updateStatus(ProjectStatus.DELETED.toString());
    getProjectDAO().easyUpdate(slickProject);
  }

  private int getImportUser() throws DominoSessionException {
    int importUser = getUserIDFromSessionOrDB();
    if (importUser == -1) {
      logger.info("\t addPending import user now = " + importUser);
      importUser = db.getUserDAO().getImportUser();
    }
    return importUser;
  }
}
