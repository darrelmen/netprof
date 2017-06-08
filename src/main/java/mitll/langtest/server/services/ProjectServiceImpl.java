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

import com.google.gwt.media.client.Audio;
import mitll.langtest.client.project.ProjectEditForm;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.copy.CreateProject;
import mitll.langtest.server.database.copy.ExerciseCopy;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.answer.AudioType;
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
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ProjectServiceImpl extends MyRemoteServiceServlet implements ProjectService {
  private static final Logger logger = LogManager.getLogger(ProjectServiceImpl.class);
  private static final int REASONABLE_PROPERTY_SPACE_LIMIT = 50;
  public static final String ANY = "Any";

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
    //Project currentProject = db.getProject(info.getID());
    //boolean wasRetired = getWasRetired(currentProject);
    // logger.info("update " +info);
    boolean update = getProjectDAO().update(getUserIDFromSession(), info);
    if (update/* && wasRetired*/) {
      db.configureProject(db.getProject(info.getID()), true);
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
    try {
      Project max = db.getProjectManagement().getProductionProjects().stream()
          .filter(project -> project.getLanguage().equals(newProject.getLanguage()) && project.getModelsDir() != null)
          .max(Comparator.comparingLong(p -> p.getProject().modified().getTime()))
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

        for (Map.Entry<Integer, CommonExercise> pair : getDominoIDToExercise(toImport).entrySet()) {
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

      Project project = db.getProject(projectid);
      Collection<String> typeOrder2 = project.getTypeOrder();

      setAttributes(projectid, newEx, updateEx, typeOrder2);

      new ExerciseCopy().addExercises(importUser,
          projectid,
          new HashMap<>(),
          slickUEDAO, newEx, typeOrder2, new HashMap<>());

      // now update...
      // update the exercises...
      logger.info("addPending updating  " + updateEx.size() + " exercises");
      doUpdate(projectid, importUser, slickUEDAO, updateEx, typeOrder2);
      copyAudio(projectid, newEx, slickUEDAO.getOldToNew(projectid));

      db.configureProject(project, true);
    }
  }

  @NotNull
  private Map<Integer, CommonExercise> getDominoIDToExercise(Collection<CommonExercise> toImport) {
    Map<Integer, CommonExercise> dominoToEx = new HashMap<>();
    toImport.forEach(ex -> dominoToEx.put(ex.getDominoID(), ex));
    return dominoToEx;
  }

  @NotNull
  private void setAttributes(int projectid,
                             List<CommonExercise> newEx,
                             List<CommonExercise> updateEx,
                             Collection<String> typeOrder2) {
    Set<String> toLower = new HashSet<>();
    typeOrder2.stream().forEach(s -> toLower.add(s.toLowerCase()));
    //  logger.info("typeorder for " +projectid + " is " + typeOrder);
    logger.info("addPending typeorder for " + projectid + " is " + typeOrder2);

    Map<String, ExerciseAttribute> pairToAttr = new HashMap<>();
    for (CommonExercise ex : newEx) {
      postProcessUnitToValueToGetAttributes(toLower, pairToAttr, ex);
    }
    for (CommonExercise ex : updateEx) {
      postProcessUnitToValueToGetAttributes(toLower, pairToAttr, ex);
    }
    logger.info("addPending importing " + newEx.size() + " exercises, updating " +
        updateEx.size() +
        " ex->attr " + pairToAttr.size());
    // return pairToAttr;
  }

  private void postProcessUnitToValueToGetAttributes(Collection<String> typeOrder,
                                                     Map<String, ExerciseAttribute> pairToAttr,
                                                     CommonExercise ex) {
    Map<String, String> unitToValue = ex.getUnitToValue();

    logger.info("postProcessUnitToValueToGetAttributes BEFORE for" +
        "\n\tex          " + ex.getID() +
        "\n\tunit->value " + ex.getUnitToValue() +
        "\n\tattr        " + ex.getAttributes());

    Map<String, String> filteredUnitToValue = new HashMap<>();
    List<ExerciseAttribute> toAdd = new ArrayList<>();

    for (Map.Entry<String, String> pair : unitToValue.entrySet()) {
      String value = pair.getValue();
      if (value.isEmpty()) value = ANY;
      String typeName = pair.getKey();
      if (typeOrder.contains(typeName.toLowerCase())) {
        filteredUnitToValue.put(typeName, value);
      } else {
        String propertyValuePair = typeName + "-" + pair.getValue();
        ExerciseAttribute exerciseAttribute1 = pairToAttr.get(propertyValuePair);

        if (exerciseAttribute1 == null) {
          exerciseAttribute1 = new ExerciseAttribute(typeName, value);
          logger.info("postProcessUnitToValueToGetAttributes Remember attr " + exerciseAttribute1);
          pairToAttr.put(propertyValuePair, exerciseAttribute1);
          if (pairToAttr.size() > REASONABLE_PROPERTY_SPACE_LIMIT) {
            logger.warn("getExerciseAttributes more than " + pairToAttr.size() +
                " distinct values for property " + typeName);
          }
        }
        toAdd.add(exerciseAttribute1);
      }
    }
    ex.getMutable().setUnitToValue(filteredUnitToValue);
    ex.setAttributes(toAdd);

    logger.info("postProcessUnitToValueToGetAttributes for" +
        "\n\tex          " + ex.getID() +
        "\n\tunit->value " + ex.getUnitToValue() +
        "\n\tattr        " + ex.getAttributes());
  }

  private void copyAudio(int projectid, List<CommonExercise> newEx, Map<String, Integer> exToInt) {
    try {
      List<Project> matches = getProjectsForSameLanguage(projectid);

      //  Map<String, List<SlickAudio>> transcriptToAudioAll = new HashMap<>();
      Map<String, AudioMatches> transcriptToAudioMatch = new HashMap<>();
      Map<String, AudioMatches> transcriptToContextAudioMatch = new HashMap<>();

      for (Project match : matches) {
//        int maxID = -1;
//        Project maxProject = getProjectWithMostExercises(slickUEDAO, matches);
//        if (maxProject != null) maxID = maxProject.getID();

        Map<String, List<SlickAudio>> transcriptToAudio = getTranscriptToAudio(match.getID());
        logger.info("copyAudio For " + match + " got " + transcriptToAudio.size() + " candidates");
        getSlickAudios(projectid, newEx, exToInt, transcriptToAudio,
            transcriptToAudioMatch, transcriptToContextAudioMatch);
      }

      if (!matches.isEmpty()) {
        List<SlickAudio> copies = new ArrayList<>();

        for (AudioMatches m : transcriptToAudioMatch.values()) {
          logger.info("got " + m);
          m.deposit(copies);
        }
        for (AudioMatches m : transcriptToContextAudioMatch.values()) {
          logger.info("got 2 " + m);
          m.deposit(copies);
        }
        logger.info("CopyAudio :" +
            "\n\tcopying " + transcriptToAudioMatch.size() + "/" +
            transcriptToContextAudioMatch.size() +
            "audio " + copies.size() +
            "\n\tfrom " + newEx.size() +
            "\n\tfrom " + matches.size() +
            " projects, e.g. " + matches.iterator().next().getProject().name());

        db.getAudioDAO().addBulk(copies);
      }

    } catch (Exception e) {
      logger.info("Got " + e);
    }
  }

  private static class AudioMatches {
    private SlickAudio mr = null;
    private SlickAudio fr = null;

    private SlickAudio ms = null;
    private SlickAudio fs = null;

    public void add(SlickAudio candidate) {
      AudioType audioType = AudioType.UNSET;
      boolean regularSpeed = audioType.isRegularSpeed();
//      try {
//        audioType = AudioType.valueOf(candidate.audiotype());
//      } catch (IllegalArgumentException e) {
//        logger.error("Got " + e, e);
//      }
      if (candidate.gender() == 0) {
        if (regularSpeed) {
          mr = mr == null ? candidate : mr.dnr() < candidate.dnr() ? candidate : mr;
        } else {
          ms = ms == null ? candidate : ms.dnr() < candidate.dnr() ? candidate : ms;
        }
      } else {
        if (regularSpeed) {
          fr = fr == null ? candidate : fr.dnr() < candidate.dnr() ? candidate : fr;
        } else {
          fs = fs == null ? candidate : fs.dnr() < candidate.dnr() ? candidate : fs;
        }
      }
    }

 /*   public SlickAudio getMr() {
      return mr;
    }

    public SlickAudio getFr() {
      return fr;
    }

    public SlickAudio getMs() {
      return ms;
    }

    public SlickAudio getFs() {
      return fs;
    }*/

    public String toString() {
      return
          (mr == null ? "x" : "1") +
              (fr == null ? "x" : "2") +
              (ms == null ? "x" : "3") +
              (fs == null ? "x" : "4");
    }

    void deposit(List<SlickAudio> copies) {
      if (mr != null) copies.add(mr);
      if (fr != null) copies.add(fr);
      if (ms != null) copies.add(ms);
      if (fs != null) copies.add(fs);
    }
  }

  @NotNull
  private Map<String, List<SlickAudio>> getTranscriptToAudio(int maxID) {
    Map<String, List<SlickAudio>> transcriptToAudio = new HashMap<>();

    Collection<SlickAudio> audioAttributesByProjectThatHaveBeenChecked = maxID == -1 ? Collections.EMPTY_LIST : db.getAudioDAO().getAll(maxID);

    logger.info("copyAudio found " + audioAttributesByProjectThatHaveBeenChecked.size() + " audio entries for " + maxID);
    for (SlickAudio audioAttribute : audioAttributesByProjectThatHaveBeenChecked) {
      List<SlickAudio> audioAttributes = transcriptToAudio.get(audioAttribute.transcript());
      if (audioAttributes == null) {
        audioAttributes = new ArrayList<>();
        transcriptToAudio.put(audioAttribute.transcript(), audioAttributes);
      }
      audioAttributes.add(audioAttribute);
    }
    return transcriptToAudio;
  }

  private List<Project> getProjectsForSameLanguage(int projectid) {
    String language = db.getProject(projectid).getLanguage();
    return db.getProjectManagement().getProductionProjects().stream()
        .filter(project -> project.getLanguage().equals(language) && project.getID() != projectid).collect(Collectors.toList());
  }

  @Nullable
  private Project getProjectWithMostExercises(SlickUserExerciseDAO slickUEDAO, List<Project> matches) {
    int maxRows = 0;
    Project maxProject = null;
    for (Project project : matches) {
      int num = slickUEDAO.getNumRows();
      if (num > maxRows) {
        maxRows = num;
        maxProject = project;
        //  maxID = project.getID();
      }
    }
    return maxProject;
  }

  @NotNull
  private void getSlickAudios(int projectid,
                              List<CommonExercise> newEx,
                              Map<String, Integer> exToInt,
                              Map<String, List<SlickAudio>> transcriptToAudio,
                              Map<String, AudioMatches> transcriptToMatches,
                              Map<String, AudioMatches> transcriptToContextMatches) {
    int match = 0;
    int nomatch = 0;
    //List<SlickAudio> copies = new ArrayList<>();

    for (CommonExercise ex : newEx) {
      String oldID = ex.getOldID();
      Integer exid = exToInt.get(oldID);

      if (exid == null) {
        logger.error("huh? can't find " + oldID + " in " + exToInt.size());
      } else {
        {
          String fl = ex.getForeignLanguage();
          if (transcriptToAudio.get(fl) != null) {
            List<SlickAudio> audioAttributes = transcriptToAudio.get(fl);
            if (audioAttributes != null) {
              AudioMatches audioMatches = transcriptToMatches.get(fl);
              if (audioMatches == null) transcriptToMatches.put(fl, audioMatches = new AudioMatches());

              copyMatchingAudio(projectid, audioMatches, exid, audioAttributes);
              match++;
            } else {
              nomatch++;
            }
          }
        }
        // for some reason, we attach the context audio to the parent exercise... might want to fix that later
        for (CommonExercise context : ex.getDirectlyRelated()) {
          String cfl = context.getForeignLanguage();
          List<SlickAudio> audioAttributes = transcriptToAudio.get(cfl);
          if (audioAttributes != null) {
            AudioMatches audioMatches = transcriptToContextMatches.get(cfl);
            if (audioMatches == null) transcriptToContextMatches.put(cfl, audioMatches = new AudioMatches());
            copyMatchingAudio(projectid, audioMatches, context.getID(), audioAttributes);
            match++;
          } else {
            nomatch++;
          }
        }
      }
    }
    logger.info("getSlickAudio  : match " + match + " no match " + nomatch);
    // return copies;
  }

  private void copyMatchingAudio(int projectid,
                                 //List<SlickAudio> copies,
                                 AudioMatches matches,
                                 int exid,
                                 List<SlickAudio> audioAttributes) {
    for (SlickAudio audio : audioAttributes) {
      SlickAudio audio1 = new SlickAudio(
          -1,
          audio.userid(),
          exid,
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
          audio.resultid(),
          audio.gender()
      );
      matches.add(audio1);

      //copies.add(
      //    audio1);
    }
  }

  private void doUpdate(int projectid, int importUser, SlickUserExerciseDAO slickUEDAO,
                        List<CommonExercise> updateEx, Collection<String> typeOrder2) {
    int failed = 0;

    Map<Integer, ExerciseAttribute> allByProject = slickUEDAO.getIDToPair(projectid);
    Map<ExerciseAttribute, Integer> attrToID = new HashMap<>();
    for (Map.Entry<Integer, ExerciseAttribute> pair : allByProject.entrySet()) {
      attrToID.put(pair.getValue(), pair.getKey());
    }

    Collection<ExerciseAttribute> allKnownAttributes = new HashSet<>(allByProject.values());

    logger.info("addExerciseAttributes for " + projectid +
        "found " + allKnownAttributes.size() + " attributes");

    Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs = slickUEDAO.getAllJoinByProject(projectid);

    long now = System.currentTimeMillis();
    Timestamp modified = new Timestamp(now);

    List<SlickExerciseAttributeJoin> newJoins = new ArrayList<>();
    List<SlickExerciseAttributeJoin> removeJoins = new ArrayList<>();

    for (CommonExercise toUpdate : updateEx) {
      if (!slickUEDAO.update(toUpdate, false, typeOrder2)) {
        logger.warn("doUpdate update failed to update " + toUpdate);
        failed++;
      }

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
      storeAndRememberAttributes(projectid, importUser, slickUEDAO, attrToID, allKnownAttributes, now, newAttributes);

      // for join set, some are on there already, some need to be added, some need to be removed

      Set<Integer> dbIDs = updateAttributes.stream().map(attrToID::get).collect(Collectors.toCollection(HashSet::new));
      if (currentAttributesOnThisExercise == null) { // none on there yet, add all
        makeNewAttrJoins(importUser, modified, newJoins, updateExerciseID, dbIDs);
      } else {
        Set<Integer> currentSet = currentAttributesOnThisExercise.stream().map(SlickExerciseAttributeJoin::attrid).collect(Collectors.toCollection(HashSet::new));

        Set<Integer> newIDs = new HashSet<>(dbIDs);
        newIDs.removeAll(currentSet);

        if (!newIDs.isEmpty()) {
          logger.info("doUpdate Adding new ids " + newIDs + " to " + updateExerciseID);
        }

        makeNewAttrJoins(importUser, modified, newJoins, updateExerciseID, newIDs);

        currentSet.removeAll(dbIDs);

        // only remove ones that stale - not on there...
     /*   for (SlickExerciseAttributeJoin current : currentAttributesOnThisExercise) {
          if (currentSet.contains(current.attrid())) {
            removeJoins.add(current);
            logger.info("doUpdate removing " + current);
          }
        }*/
      }

      // so now we have known and new attributes.
      // compare new attributes to known...
    }

    logger.info("doUpdate now " + newJoins.size() + " and remove " + removeJoins.size());
    slickUEDAO.addBulkAttributeJoins(newJoins);
    slickUEDAO.removeBulkAttributeJoins(removeJoins);
    if (failed > 0)
      logger.warn("\n\n\n\ndoUpdate somehow failed to update " + failed + " out of " + updateEx.size() + " exercises");
  }

  private void makeNewAttrJoins(int importUser, Timestamp modified, List<SlickExerciseAttributeJoin> newJoins, int updateExerciseID, Set<Integer> dbIDs) {
    for (Integer dbID : dbIDs) {
      newJoins.add(new SlickExerciseAttributeJoin(-1, importUser, modified, updateExerciseID, dbID));
    }
  }

  private void storeAndRememberAttributes(int projectid, int importUser,
                                          SlickUserExerciseDAO slickUEDAO,
                                          Map<ExerciseAttribute, Integer> attrToID,
                                          Collection<ExerciseAttribute> allKnownAttributes, long now, List<ExerciseAttribute> newAttributes) {
    for (ExerciseAttribute newAttr : newAttributes) {
      int i = slickUEDAO.addAttribute(projectid, now, importUser, newAttr);
    //  allByProject.put(i, newAttr);
      allKnownAttributes.add(newAttr);
      attrToID.put(newAttr, i);
      logger.info("doUpdate remember new import attribute " + i + " = " + newAttr);
    }
  }

 /* private boolean getWasRetired(Project currentProject) {
    boolean wasRetired = false;
    if (currentProject != null) {
      wasRetired = currentProject.getStatus() == ProjectStatus.RETIRED;
    }
    return wasRetired;
  }*/
}
