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

import mitll.langtest.client.services.ProjectService;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.copy.CreateProject;
import mitll.langtest.server.database.copy.ExerciseCopy;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ProjectServiceImpl extends MyRemoteServiceServlet implements ProjectService {
  private static final Logger logger = LogManager.getLogger(ProjectServiceImpl.class);


  @Override
  public List<ProjectInfo> getAll() {
    return getProjectDAO().getAll()
        .stream()
        .map(project -> new ProjectInfo(project.id(),
            project.name(),
            project.language(),
            project.course(),
            project.countrycode(),
            ProjectStatus.valueOf(project.status()),
            project.displayorder(),

            project.modified().getTime(),
            getPort(project),
            project.getProp(ServerProperties.MODELS_DIR),
            project.first(),
            project.second())
        )
        .collect(Collectors.toList());
  }

  private int getPort(SlickProject project) {
    try {
      return Integer.parseInt(project.getProp(ServerProperties.WEBSERVICE_HOST_PORT));
    } catch (NumberFormatException e) {
      logger.error("got " + e, e);
      return -1;
    }
  }

  private IProjectDAO getProjectDAO() {
    return db.getProjectDAO();
  }

  /**
   * @param projectid
   * @return
   * @see mitll.langtest.client.project.ProjectChoices#setProjectForUser
   */
  @Override
  public boolean exists(int projectid) {
    return getProjectDAO().exists(projectid);
  }

  @Override
  public boolean existsByName(String name) {
    return getProjectDAO().getByName(name) != -1;
  }

  /**
   * @param info
   * @return
   * @see ProjectEditForm#updateProject
   */
  @Override
  public boolean update(ProjectInfo info) {
    Project currentProject = db.getProject(info.getID());
    boolean wasRetired = getWasRetired(currentProject);

    logger.info("update " +info);
    boolean update = getProjectDAO().update(getUserIDFromSession(), info);
    if (update && wasRetired) {
      db.configureProject(db.getProject(info.getID()));
    }
    db.getProjectManagement().refreshProjects();
    return update;
  }

  @Override
  public boolean create(ProjectInfo newProject) {
    return new CreateProject().createProject(db, db, newProject);
  }

  @Override
  public boolean delete(int id) {
    return getProjectDAO().delete(id);
  }

  @Override
  public void addPending(int projectid) {
    Collection<CommonExercise> toImport = db.getProjectManagement().getFileUploadHelper().getExercises(projectid);
    if (toImport != null) {
      Map<Integer, CommonExercise> dominoToEx = new HashMap<>();
      toImport.forEach(ex -> dominoToEx.put(ex.getDominoID(), ex));

      int importUser = ((DominoUserDAOImpl) db.getUserDAO()).getImportUser();
      SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();

      List<CommonExercise> newEx = new ArrayList<>();
      List<CommonExercise> updateEx = new ArrayList<>();

      Map<Integer, SlickExercise> legacyToEx = slickUEDAO.getLegacyToEx(projectid);
      Set<Integer> current = legacyToEx.keySet();

      for (Map.Entry<Integer, CommonExercise> pair : dominoToEx.entrySet()) {
        if (current.contains(pair.getKey())) {
          updateEx.add(pair.getValue());
        } else {
          newEx.add(pair.getValue());
        }
      }

      logger.info("addPending importing " + newEx.size() + " exercises");
      logger.info("addPending updating  " + updateEx.size() + " exercises");

      Collection<String> typeOrder = db.getTypeOrder(projectid);
      Collection<String> typeOrder2 = db.getProject(projectid).getTypeOrder();

      logger.info("typeorder for " +projectid + " is " + typeOrder);
      logger.info("typeorder for " +projectid + " is " + typeOrder2);

      new ExerciseCopy().addPredefExercises(projectid, slickUEDAO, importUser, newEx,
          typeOrder2);

      db.configureProject(db.getProject(projectid));
    }
  }

  private boolean getWasRetired(Project currentProject) {
    boolean wasRetired = false;
    if (currentProject != null) {
      wasRetired = currentProject.getStatus() == ProjectStatus.RETIRED;
    }
    return wasRetired;
  }
}
