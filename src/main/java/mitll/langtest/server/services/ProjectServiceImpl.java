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
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
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

    // logger.info("update " +info);
    boolean update = getProjectDAO().update(getUserIDFromSession(), info);
    if (update && wasRetired) {
      db.configureProject(db.getProject(info.getID()), false);
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

      int importUser = getUserIDFromSession();
      logger.info("import user = " + importUser);
      if (importUser == -1) {
        logger.info("\t import user now = " + importUser);
        importUser = ((DominoUserDAOImpl) db.getUserDAO()).getImportUser();
      }

      SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();

      List<CommonExercise> newEx = new ArrayList<>();
      List<CommonExercise> updateEx = new ArrayList<>();

      Map<Integer, SlickExercise> legacyToEx = slickUEDAO.getLegacyToEx(projectid);
      {
        Set<Integer> current = legacyToEx.keySet();

        for (Map.Entry<Integer, CommonExercise> pair : dominoToEx.entrySet()) {
          Integer dominoID = pair.getKey();
          CommonExercise importEx = pair.getValue();

          if (current.contains(dominoID)) {
            updateEx.add(importEx);
            importEx.getMutable().setID(legacyToEx.get(dominoID).id());
          } else {
            newEx.add(importEx);
          }
        }
      }

      logger.info("addPending importing " + newEx.size() + " exercises");
      logger.info("addPending updating  " + updateEx.size() + " exercises");

      //  Collection<String> typeOrder = db.getTypeOrder(projectid);
      Collection<String> typeOrder2 = db.getProject(projectid).getTypeOrder();

      //  logger.info("typeorder for " +projectid + " is " + typeOrder);
      logger.info("typeorder for " + projectid + " is " + typeOrder2);

      new ExerciseCopy().addPredefExercises(projectid, slickUEDAO, importUser, newEx, typeOrder2);

      // now update...
      // update the exercises...
      doUpdate(projectid, importUser, slickUEDAO, updateEx, typeOrder2);

      db.configureProject(db.getProject(projectid), true);
    }
  }

  private void doUpdate(int projectid, int importUser, SlickUserExerciseDAO slickUEDAO, List<CommonExercise> updateEx, Collection<String> typeOrder2) {
    int failed = 0;

    Map<Integer, ExerciseAttribute> allByProject = slickUEDAO.getIDToPair(projectid);
    Map<ExerciseAttribute, Integer> attrToID = new HashMap<>();
    for (Map.Entry<Integer, ExerciseAttribute> pair : allByProject.entrySet()) {
      attrToID.put(pair.getValue(), pair.getKey());
    }

    Collection<ExerciseAttribute> allKnownAttributes = new HashSet<>(allByProject.values());

    //  logger.info("addExerciseAttributes found " + allByProject.size() + " attributes");
    Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs = slickUEDAO.getAllJoinByProject(projectid);

    long now = System.currentTimeMillis();
    Timestamp modified = new Timestamp(now);

    List<SlickExerciseAttributeJoin> newJoins = new ArrayList<>();
    List<SlickExerciseAttributeJoin> removeJoins = new ArrayList<>();

    for (CommonExercise toUpdate : updateEx) {
      logger.info("update " + toUpdate);
      if (!slickUEDAO.update(toUpdate, false, typeOrder2)) failed++;

      // compare new attributes on toUpdate to existing attributes...
      //SlickExercise slickExercise = legacyToEx.get(toUpdate.getDominoID());
      int updateExerciseID = toUpdate.getID();
      Collection<SlickExerciseAttributeJoin> currentAttributesOnThisExercise = exToAttrs.get(updateExerciseID);

      List<ExerciseAttribute> newAttributes = new ArrayList<>();
      // List<ExerciseAttribute> updateAttributes = ;
//      if (currentAttributes == null) { // they're all new, since
//        newAttributes.addAll(updateAttributes);
//      }
//      else {
      List<ExerciseAttribute> knownAttributes = new ArrayList<>();

      // import attributes are either known or unknown...
      List<ExerciseAttribute> updateAttributes = toUpdate.getAttributes();
      for (ExerciseAttribute updateAttr : updateAttributes) {
        if (allKnownAttributes.contains(updateAttr)) {
          knownAttributes.add(updateAttr);
        } else {
          newAttributes.add(updateAttr);
        }
      }
      //  }

      // store and remember the new ones
      for (ExerciseAttribute newAttr : newAttributes) {
        int i = slickUEDAO.addAttribute(projectid, now, importUser, newAttr);
        allByProject.put(i, newAttr);
        allKnownAttributes.add(newAttr);
        attrToID.put(newAttr, i);
        logger.info("remember new import attribute " + i + " = " + newAttr);
      }

      // for join set, some are on there already, some need to be added, some need to be removed

      Set<Integer> dbIDs = updateAttributes.stream().map(attrToID::get).collect(Collectors.toCollection(HashSet::new));
      if (currentAttributesOnThisExercise == null) { // none on there yet, add all
        for (Integer dbID : dbIDs) {
          newJoins.add(new SlickExerciseAttributeJoin(-1, importUser, modified, updateExerciseID, dbID));
        }
      } else {
        Set<Integer> currentSet = currentAttributesOnThisExercise.stream().map(SlickExerciseAttributeJoin::attrid).collect(Collectors.toCollection(HashSet::new));

        Set<Integer> newIDs = new HashSet<>(dbIDs);
        newIDs.removeAll(currentSet);

        if (!newIDs.isEmpty()) logger.info("Adding new ids " + newIDs + " to " + updateExerciseID);

        for (Integer dbID : newIDs) {
          newJoins.add(new SlickExerciseAttributeJoin(-1, importUser, modified, updateExerciseID, dbID));
        }
        currentSet.removeAll(dbIDs);
        for (SlickExerciseAttributeJoin current : currentAttributesOnThisExercise) {
          if (currentSet.contains(current.attrid())) {
            removeJoins.add(current);
            logger.info("removing " + current);
          }
        }
      }

      // so now we have known and new attributes.
      // compare new attributes to known...
    }

    logger.info("now " + newJoins.size() + " and remove " + removeJoins.size());
    slickUEDAO.addBulkAttributeJoins(newJoins);
    slickUEDAO.removeBulkAttributeJoins(removeJoins);
    if (failed > 0) logger.warn("\n\n\n\nsomehow failed to update " + failed + " out of " + updateEx.size() + " exercises");
  }

  private boolean getWasRetired(Project currentProject) {
    boolean wasRetired = false;
    if (currentProject != null) {
      wasRetired = currentProject.getStatus() == ProjectStatus.RETIRED;
    }
    return wasRetired;
  }
}
