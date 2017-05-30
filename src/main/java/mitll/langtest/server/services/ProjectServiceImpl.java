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
import mitll.langtest.server.database.audio.AudioInfo;
import mitll.langtest.server.database.copy.CreateProject;
import mitll.langtest.server.database.copy.ExerciseCopy;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.SlimProject;
import mitll.npdata.dao.SlickAudio;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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
    if (newProject.getModelsDir().isEmpty()) {
      setDefaultsIfMissing(newProject);
    }
    return new CreateProject().createProject(db, db, newProject);
  }

  private void setDefaultsIfMissing(ProjectInfo newProject) {
    Project max = null;
    try {
      max = db.getProjectManagement().getProductionProjects().stream()
          .filter(project -> project.getLanguage().equals(newProject.getLanguage()) && project.getModelsDir() != null)
          .max((p1, p2) -> Long.compare(p1.getProject().modified().getTime(), p2.getProject().modified().getTime()))
          .get();

      newProject.setModelsDir(max.getModelsDir());

      if (newProject.getPort() == -1) {
        newProject.setPort(max.getWebservicePort());
      }
    } catch (Exception e) {
      logger.info("Got " + e);
    }
  }

  @Override
  public boolean delete(int id) {
    return getProjectDAO().delete(id);
  }

  /**
   * @param projectid
   * @see mitll.langtest.client.project.ProjectChoices#showImportDialog(SlimProject)
   */
  @Override
  public void addPending(int projectid) {
    Collection<CommonExercise> toImport = db.getProjectManagement().getFileUploadHelper().getExercises(projectid);

    if (toImport != null) {
      Map<Integer, CommonExercise> dominoToEx = new HashMap<>();
      toImport.forEach(ex -> dominoToEx.put(ex.getDominoID(), ex));

      int importUser = getUserIDFromSession();
      logger.info("addPending import user = " + importUser);
      if (importUser == -1) {
        logger.info("\t addPending import user now = " + importUser);
        importUser = db.getUserDAO().getImportUser();
      }

      SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();

      List<CommonExercise> newEx = new ArrayList<>();
      List<CommonExercise> updateEx = new ArrayList<>();

      Map<Integer, SlickExercise> legacyToEx = slickUEDAO.getLegacyToEx(projectid);

      logger.info("addPending found " + legacyToEx.size() + " current exercises for " + projectid);
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

      //  Collection<String> typeOrder = db.getTypeOrder(projectid);
      Collection<String> typeOrder2 = db.getProject(projectid).getTypeOrder();

      //  logger.info("typeorder for " +projectid + " is " + typeOrder);
      logger.info("addPending typeorder for " + projectid + " is " + typeOrder2);

      // new ExerciseCopy().addPredefExercises(projectid, slickUEDAO, importUser, newEx, typeOrder2, new HashMap<>());
      logger.info("addPending importing " + newEx.size() + " exercises");
      new ExerciseCopy().addExercises(importUser,
          projectid,
          new HashMap<>(),
          slickUEDAO, newEx, typeOrder2, new HashMap<>());

      // now update...
      // update the exercises...
      logger.info("addPending updating  " + updateEx.size() + " exercises");
      doUpdate(projectid, importUser, slickUEDAO, updateEx, typeOrder2);

      Map<String, Integer> exToInt = slickUEDAO.getOldToNew(projectid);

      copyAudio(projectid, newEx, exToInt);

      db.configureProject(db.getProject(projectid), true);
    }
  }

  private void copyAudio(int projectid, List<CommonExercise> newEx, Map<String, Integer> exToInt) {
    try {
      String language = db.getProject(projectid).getLanguage();
      Project max = db.getProjectManagement().getProductionProjects().stream()
          .filter(project -> project.getLanguage().equals(language) && project.getID() != projectid)
          .max(Comparator.comparingLong(p -> p.getProject().modified().getTime()))
          .get();
      Collection<SlickAudio> audioAttributesByProjectThatHaveBeenChecked = db.getAudioDAO().getAll(max.getID());
      Map<String, List<SlickAudio>> transcriptToAudio = new HashMap<>();

      logger.info("found " + audioAttributesByProjectThatHaveBeenChecked.size() + " audio entries for " + max.getID());
      for (SlickAudio audioAttribute : audioAttributesByProjectThatHaveBeenChecked) {
        List<SlickAudio> audioAttributes = transcriptToAudio.get(audioAttribute.transcript());
        if (audioAttributes == null) {
          audioAttributes = new ArrayList<>();
          transcriptToAudio.put(audioAttribute.transcript(), audioAttributes);
        }
        audioAttributes.add(audioAttribute);
      }

      List<SlickAudio> copies = getSlickAudios(projectid, newEx, exToInt, transcriptToAudio);
      logger.info("CopyAudio : copying " + copies.size() + " audio from " + newEx.size() + " from project " + max.getID() + " " + max.getProject().name());
      db.getAudioDAO().addBulk(copies);
    } catch (Exception e) {
      logger.info("Got " + e);
    }
  }

  @NotNull
  private List<SlickAudio> getSlickAudios(int projectid, List<CommonExercise> newEx, Map<String, Integer> exToInt, Map<String, List<SlickAudio>> transcriptToAudio) {
    int match = 0;
    int nomatch = 0;
    List<SlickAudio> copies = new ArrayList<>();
    for (CommonExercise ex : newEx) {
     // int id = ex.getID();

      String oldID = ex.getOldID();
      Integer id = exToInt.get(oldID);

      if (id == null) {
        logger.error("huh? can't find " +oldID+  " in " + exToInt.size());
      }
      else {
        if (transcriptToAudio.get(ex.getForeignLanguage()) != null) {
          List<SlickAudio> audioAttributes = transcriptToAudio.get(ex.getForeignLanguage());
          if (audioAttributes != null) {
            copyMatchingAudio(projectid, copies, id, audioAttributes);
            match++;
          } else {
            nomatch++;
          }
        }
        // for some reason, we attach the context audio to the parent exercise... might want to fix that later
        for (CommonExercise context : ex.getDirectlyRelated()) {
          List<SlickAudio> audioAttributes = transcriptToAudio.get(context.getForeignLanguage());
          if (audioAttributes != null) {
            copyMatchingAudio(projectid, copies, id, audioAttributes);
            match++;
          } else {
            nomatch++;
          }
        }
      }

    }
    logger.info("getSlickAudio  : match " + match + " no match " + nomatch);
    return copies;
  }

  private void copyMatchingAudio(int projectid, List<SlickAudio> copies, int id, List<SlickAudio> audioAttributes) {
    for (SlickAudio audio : audioAttributes) {

      copies.add(//new AudioInfo(audio, projectid, ex.getID()));
          new SlickAudio(
              -1,
              audio.userid(),
              id,
              audio.modified(),
              audio.audioref(),
              audio.audiotype(),
              audio.duration(),
              audio.defect(),
              audio.transcript(),
              projectid,
              audio.exists(),
              audio.lastcheck(),
              audio.actualpath(),
              audio.dnr(),
              audio.resultid()
          ));
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
    if (failed > 0)
      logger.warn("\n\n\n\nsomehow failed to update " + failed + " out of " + updateEx.size() + " exercises");
  }

  private boolean getWasRetired(Project currentProject) {
    boolean wasRetired = false;
    if (currentProject != null) {
      wasRetired = currentProject.getStatus() == ProjectStatus.RETIRED;
    }
    return wasRetired;
  }
}
