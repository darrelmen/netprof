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

package mitll.langtest.server.database.project;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.audio.TrackInfo;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.AudioInfo;
import mitll.langtest.server.database.copy.ExerciseCopy;
import mitll.langtest.server.database.dialog.*;
import mitll.langtest.server.domino.AudioCopy;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickDialog;
import mitll.npdata.dao.SlickDialogAttributeJoin;
import mitll.npdata.dao.SlickImage;
import mitll.npdata.dao.SlickRelatedExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.project.ProjectManagement.FIVE_YEARS;
import static mitll.langtest.shared.answer.AudioType.REGULAR;

/**
 * Add dialog info to project.
 */
public class DialogPopulate {
  private static final Logger logger = LogManager.getLogger(DialogPopulate.class);
  // private static final boolean DO_AUDIO_COPY = false;

  private final DatabaseImpl db;
  /**
   * @see #addResultAndAudio
   */
  private final PathWriter pathWriter;
  /**
   * @see #addResultAndAudio
   */
  protected final PathHelper pathHelper;

  public DialogPopulate(DatabaseImpl db, PathHelper pathHelper) {
    this.db = db;
    pathWriter = new PathWriter(db.getServerProps(), pathHelper.getContext());
    this.pathHelper = pathHelper;
  }

  /**
   * Take our canned data and put it in the database.
   *
   * @param project
   * @see ProjectManagement#addDialogInfo
   */
  boolean addDialogInfo(Project project) {
    List<IDialog> dialogs1 = db.getDialogDAO().getDialogs(project.getID());
    if (dialogs1.isEmpty()) {
      logger.info("addDialogInfo no dialog info yet loaded for " + project);
      return true;
    } else {
      project.setDialogs(dialogs1);
      return false;
    }
  }

  /**
   * Populate database tables that represent the dialog.
   * We need to add
   * * an image for each dialog
   * * the dialog itself
   * * dialog attributes (since a dialog is a lot like an exercise with attributes supporting faceted browsing)
   * * the exercises in a dialog
   * * the core vocab for a dialog (more exercises)
   * * and any audio that can be copied in from current known audio
   *
   * @param project
   * @param keepAudio
   * @param excel
   * @param appendOK
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyDialog
   */
  public boolean populateDatabase(Project project, Project englishProject, boolean keepAudio, String excel, boolean appendOK) {
    int projid = project.getID();
    IDialogDAO dialogDAO = db.getDialogDAO();
    if (!appendOK && !dialogDAO.getDialogs(projid).isEmpty()) {
      logger.warn("\n\nProject #" + projid + " already has dialog data so not adding any.");
      return false;
    } else {
      waitUntilTrieReady(project);
      maybeDoInterpreterImport(project, englishProject, dialogDAO, keepAudio, excel);
      return true;
    }
  }

  /**
   * Loads data for any excel file we find under
   *
   * /opt/netprof/dialog/LANG/interpreter.xlsx
   *
   * @param project
   * @param dialogDAO
   * @param keepAudio
   * @param excel
   * @see #populateDatabase(Project, Project, boolean, String, boolean)
   */
  private void maybeDoInterpreterImport(Project project, Project englishProject, IDialogDAO dialogDAO,
                                        boolean keepAudio, String excel) {
    //logger.info("maybeDoInterpreterImport found interpreter candidate " + project);
    Project engProject = db.getProjectManagement().getProductionByLanguage(Language.ENGLISH);

    if (engProject == null) {
      logger.error("maybeDoInterpreterImport huh? no english project?");
    } else {
      int defaultUser = db.getUserDAO().getDefaultUser();
      if (defaultUser == -1) {
        logger.error("maybeDoInterpreterImport huh? default user is -1?");
      }

      Map<Dialog, SlickDialog> dialogToSlick =
          new InterpreterReader(InterpreterReader.DEFAULT_UNIT, getDefaultChapter(excel))
              .getInterpreterDialogs(defaultUser, project, engProject, excel);

      if (!dialogToSlick.isEmpty()) {
        logger.info("maybeDoInterpreterImport (" + project.getName() + ") read " + dialogToSlick.size());
        addDialogs(project, englishProject, dialogDAO, Collections.emptyMap(), defaultUser, DialogType.INTERPRETER, dialogToSlick, keepAudio);
      }
    }
  }

  @NotNull
  private String getDefaultChapter(String excel) {
    String aShort = excel.startsWith("short") ? "Short Interpreter" : InterpreterReader.DEFAULT_CHAPTER;
    logger.info("chapter is " + aShort);
    return aShort;
  }

  /**
   * @param project
   * @param englishProject
   * @param dialogDAO
   * @param exToAudio
   * @param defaultUser
   * @param dialogType
   * @param dialogToSlick
   * @param keepAudio
   * @see #maybeDoInterpreterImport(Project, Project, IDialogDAO, boolean, String)
   */
  private void addDialogs(Project project,
                          Project englishProject,
                          IDialogDAO dialogDAO,
                          Map<ClientExercise, String> exToAudio,
                          int defaultUser,
                          DialogType dialogType,

                          Map<Dialog, SlickDialog> dialogToSlick,
                          boolean keepAudio) {
    Set<Dialog> dialogs = dialogToSlick.keySet();

    long now = System.currentTimeMillis();
    Timestamp modified = new Timestamp(now);

    int projid = project.getID();
    Map<ExerciseAttribute, Integer> attrToInt = getExerciseAttributeToID(projid, defaultUser, dialogs, now);

    Map<ClientExercise, Integer> allImportExToID = new HashMap<>();
    List<String> typeOrder = project.getTypeOrder();
    Set<CommonExercise> allNewInDatabase = new HashSet<>();
    dialogs.forEach(dialog -> {
      // add the image

      // logger.info("addDialogs add dialog image " + projid + " dialog " + dialog.getID() + " modified " + modified);
      int imageID = db.getImageDAO().insert(getSlickImage(projid, now, dialog, modified));
      //  logger.info("addDialogs add dialog image " + projid + " dialog " + dialog.getID() + " id " + imageID);

      dialogToSlick.get(dialog).imageid_$eq(imageID);

      // add the dialog to the database
      int dialogID = dialogDAO.add(defaultUser, projid, 1, imageID, now, now,
          dialog.getUnit(), dialog.getChapter(),
          dialogType, DialogStatus.DEFAULT,
          dialog.getEnglish(), dialog.getOrientation(), false);

      // add dialog attributes
      addDialogAttributes(dialogDAO, defaultUser, modified, attrToInt, dialog, dialogID);

      // add the exercises
      allImportExToID.putAll(addExercises(projid, defaultUser, typeOrder, modified, dialog, dialogID));

      // add core vocab
      allNewInDatabase.addAll(addCoreVocab(projid, defaultUser, typeOrder, modified, dialog, dialogID));
    });

    {
      AudioCheck audioCheck = new AudioCheck(db.getServerProps().shouldTrimAudio(), db.getServerProps().getMinDynamicRange());
      addAudio(project, projid, exToAudio, defaultUser, now, audioCheck, allImportExToID);
    }

    if (keepAudio) {
      Set<ClientExercise> newEx = allImportExToID.keySet();
      logger.info("addDialogs found " + newEx.size() + " dialog exercises, " + allNewInDatabase.size() + " core");
      allNewInDatabase.addAll(toCommon(newEx));

      Map<Boolean, List<CommonExercise>> englishAndNon = allNewInDatabase.stream().collect(Collectors.partitioningBy(ClientExercise::hasEnglishAttr));
      {
        List<CommonExercise> english = englishAndNon.get(true);
        logger.info("addDialogs copy audio for " + english.size() + " english exercises");
        new AudioCopy(db, db.getProjectManagement(), db)
            .copyAudio(englishProject.getID(), english,
                new HashMap<>());
      }
      {
        List<CommonExercise> fl = englishAndNon.get(false);
        logger.info("addDialogs copy audio for " + fl.size() + " fl exercises");
        new AudioCopy(db, db.getProjectManagement(), db)
            .copyAudio(projid, fl,
                new HashMap<>());
      }
    }
  }

  /**
   * @param projid
   * @param defaultUser
   * @param typeOrder
   * @param modified
   * @param allImportExToID
   * @param dialog
   * @param dialogID
   * @see #addDialogs
   */
  private Map<CommonExercise, Integer> addExercises(int projid,
                                                    int defaultUser,
                                                    List<String> typeOrder,
                                                    Timestamp modified,
                                                    //  Map<ClientExercise, Integer> allImportExToID,
                                                    IDialog dialog,
                                                    int dialogID) {
    return addExercises2(projid, defaultUser, typeOrder, modified, dialogID, dialog.getExercises());
  }

  public Map<CommonExercise, Integer> addExercises2(int projid, int defaultUser, List<String> typeOrder, Timestamp modified,
                                                    int dialogID,
                                                    List<ClientExercise> exercises) {
    List<CommonExercise> commonExercisesFromDialog = toCommon(exercises);

    Map<CommonExercise, Integer> importExToID = addExercises(projid, defaultUser, typeOrder, commonExercisesFromDialog);

    importExToID.forEach((k, v) -> k.getMutable().setID(v));

    logger.info("addExercises made " + importExToID.size() + " exercises for " + dialogID);

    importExToID.keySet().forEach(logger::info);

    db.getUserExerciseDAO().getRelatedExercise().addBulkRelated(
        getSlickRelatedExercises(projid, modified, exercises, dialogID, importExToID));

    return importExToID;
  }

  private Map<CommonExercise, Integer> addExercises(int projid,
                                                    int defaultUser,
                                                    List<String> typeOrder,
                                                    List<CommonExercise> commonExercisesFromDialog) {
//    commonExercisesFromDialog
//        .forEach(ex -> logger.info("ex fl " + ex.getForeignLanguage() + " = " + ex.getEnglish() + " " + ex.hasEnglishAttr()));
    return new ExerciseCopy().addExercisesAndAttributes(
        defaultUser,
        projid,
        db.getUserExerciseDAO(),
        commonExercisesFromDialog,
        typeOrder,
        new HashMap<>(),
        new HashMap<>(),
        true);
  }

  @NotNull
  private List<CommonExercise> getCommonExercisesFromDialog(IDialog dialog) {
    return toCommon(dialog.getExercises());
  }

  @NotNull
  private List<CommonExercise> toCommon(Collection<ClientExercise> exercises) {
    List<CommonExercise> commonExercises = new ArrayList<>(exercises.size());
    exercises.forEach(clientExercise -> commonExercises.add(clientExercise.asCommon()));
    return commonExercises;
  }

  private void waitUntilTrieReady(Project project) {
    for (int i = 0; i < 20; i++) {
      if (project.getFullTrie() == null) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else break;
    }
  }

  @NotNull
  private Map<ExerciseAttribute, Integer> getExerciseAttributeToID(int projid, int defaultUser, Set<Dialog> dialogs, long now) {
    Map<Integer, ExerciseAttribute> idToPair = db.getUserExerciseDAO().getExerciseAttributeDAO().getIDToPair(projid);

    Map<ExerciseAttribute, Integer> attrToInt = new HashMap<>();

    addNewAttributes(projid, defaultUser, dialogs, now, idToPair, attrToInt);
    idToPair.forEach((k, v) -> attrToInt.put(v, k));
    return attrToInt;
  }

  @NotNull
  private IDialogReader getDialogReader(Language languageEnum) {
    return languageEnum == Language.KOREAN ? new KPDialogs("1", "1") : new EnglishDialog();
  }

  /**
   * Deals with any new exercises for core vocab.
   *
   * @param projid
   * @param modified
   * @param dialog
   * @param dialogID
   * @see #addDialogs(Project, Project, IDialogDAO, Map, int, DialogType, Map, boolean)
   */
  private Set<CommonExercise> addCoreVocab(int projid, int userID, List<String> typeOrder,
                                           Timestamp modified, Dialog dialog, int dialogID) {
    List<ClientExercise> coreVocabulary = dialog.getCoreVocabulary();

    List<SlickRelatedExercise> relatedExercises = new ArrayList<>();

//    Map<Boolean, List<ClientExercise>> newAndOldExercises =
//        coreVocabulary.stream().collect(Collectors.partitioningBy(clientExercise -> clientExercise.getID() > -1));

    List<ClientExercise> newExercises = coreVocabulary;//newAndOldExercises.get(false);
//    List<ClientExercise> oldExercises = newAndOldExercises.get(true);

    Map<CommonExercise, Integer> importExToID = addExercises(projid, userID, typeOrder, toCommon(newExercises));

    List<Collection<CommonExercise>> both = new ArrayList<>();
    Set<CommonExercise> newInDatabase = importExToID.keySet();
    both.add(newInDatabase);
    //  both.add(toCommon(oldExercises));

    both.forEach(commonExercises ->
        commonExercises.forEach(clientExercise ->
            {
              int id = getExID(importExToID, clientExercise);
              if (id == -1) {
                logger.error("addCoreVocab huh? exercise has no id");
              }

              relatedExercises.add(new SlickRelatedExercise(-1,
                  id,
                  id,
                  projid,
                  dialogID,
                  modified));
            }
        ));

    logger.info("addCoreVocab about to add " + relatedExercises.size() + " related exercises!");
    db
        .getUserExerciseDAO()
        .getRelatedCoreExercise()
        .addBulkRelated(relatedExercises);
    logger.info("addCoreVocab done adding " + relatedExercises.size() + " related exercises!");

    return newInDatabase;
  }

  @NotNull
  private Integer getExID(Map<CommonExercise, Integer> importExToID, CommonExercise clientExercise) {
    Integer id = clientExercise.getID();
    if (id == -1) {
//      logger.info("addCoreVocab lookup ex '" + clientExercise.getEnglish() + "' " + clientExercise.getForeignLanguage());
      id = importExToID.get(clientExercise);
      if (id == null) {
        logger.error("can't find " + clientExercise.getEnglish() + " " + clientExercise.getForeignLanguage());
        id = -1;
      }
    }
    return id;
  }

  @NotNull
  private List<SlickRelatedExercise> getSlickRelatedExercises(int projid,
                                                              Timestamp modified,
                                                              List<ClientExercise> exercises,
                                                              int dialogID,
                                                              Map<CommonExercise, Integer> importExToID) {
    List<SlickRelatedExercise> relatedExercises = new ArrayList<>();

    if (exercises.size() == 1) {
      int id = exercises.get(0).getID();
      relatedExercises.add(new SlickRelatedExercise(-1, id, id, projid, dialogID, modified));
    } else {
      ClientExercise prev = null;
      for (ClientExercise ex : exercises) {
        if (prev != null) {
          int prevID = importExToID.get(prev.asCommon());
          int currID = importExToID.get(ex.asCommon());
          relatedExercises.add(new SlickRelatedExercise(-1, prevID, currID, projid, dialogID, modified));
        }
        prev = ex;
      }
    }

    return relatedExercises;
  }

  private void addAudio(Project project,
                        int projid,
                        Map<ClientExercise, String> exToAudio,
                        int defaultUser, long now,
                        AudioCheck audioCheck,
                        Map<ClientExercise, Integer> allImportExToID) {
    exToAudio.forEach((k, v) -> {
      File file = new File(db.getServerProps().getAudioBaseDir(), v);
      if (!file.exists()) {
        logger.error("addAudio can't find audio file " + file.getAbsolutePath());
      } else {
        logger.info("addAudio : found audio at " + file.getAbsolutePath());
      }
      AudioCheck.ValidityAndDur valid = audioCheck.isValid(file, true, false);

      Integer exid = allImportExToID.get(k);
      if (exid == null) {
        logger.error("addAudio can't find ex by '" + k + "' in " + allImportExToID.size());
      } else {
        addResultAndAudio(project, projid, defaultUser, now, k, v, valid, exid);
      }
    });
  }

  /**
   * @param dialogDAO
   * @param defaultUser
   * @param modified
   * @param attrToInt
   * @param dialog
   * @param dialogID
   * @see #populateDatabase
   */
  private void addDialogAttributes(IDialogDAO dialogDAO, int defaultUser, Timestamp modified,
                                   Map<ExerciseAttribute, Integer> attrToInt,
                                   Dialog dialog, int dialogID) {
    List<SlickDialogAttributeJoin> joins = new ArrayList<>();
    dialog.getAttributes().forEach(exerciseAttribute -> {
      Integer attrid = attrToInt.get(exerciseAttribute);
      if (attrid == null) {
        logger.error("addDialogAttributes can't find id for " + exerciseAttribute);
      } else {
        joins.add(new SlickDialogAttributeJoin(-1, defaultUser, modified, dialogID, attrid));
      }
    });
    logger.info("addDialogAttributes dialog added " + joins.size());

    dialogDAO.getDialogAttributeJoinHelper().addBulkAttributeJoins(joins);
  }

  private void addNewAttributes(int projid, int defaultUser, Collection<Dialog> dialogs, long now,
                                Map<Integer, ExerciseAttribute> idToPair,
                                Map<ExerciseAttribute, Integer> attrToInt) {
    Set<ExerciseAttribute> known = new HashSet<>(idToPair.values());

    Set<ExerciseAttribute> allToAdd = new HashSet<>();
    dialogs.forEach(dialog -> {
      List<ExerciseAttribute> attributes = dialog.getAttributes();
//      logger.info("dialog attr " + dialog.getEntitle() + " " + attributes.size());
      Set<ExerciseAttribute> toAdd = new HashSet<>(attributes);
      toAdd.removeAll(known);
      allToAdd.addAll(toAdd);
    });

    int before = attrToInt.size();
    logger.info("addNewAttributes adding " + allToAdd.size() + " new attr");
    allToAdd.forEach(exerciseAttribute -> {
      logger.info("addNewAttributes adding new exercise attribute " + exerciseAttribute);

      attrToInt
          .put(exerciseAttribute, db.getUserExerciseDAO().getExerciseAttributeDAO()
              .findOrAddAttribute(projid,
                  now, defaultUser, exerciseAttribute, true));
    });
    int after = attrToInt.size();
    logger.info("addNewAttributes really added " + (after - before));
  }

  /**
   * @param project
   * @param projid
   * @param defaultUser
   * @param now
   * @param k
   * @param pathOnDisk
   * @param valid
   * @param exid
   * @see #addAudio
   */
  private void addResultAndAudio(Project project, int projid, int defaultUser, long now,
                                 ClientExercise k, String pathOnDisk, AudioCheck.ValidityAndDur valid,
                                 Integer exid) {
    Language languageEnum = project.getLanguageEnum();
    String foreignLanguage = k.getForeignLanguage();

    int resultID = db.getAnswerDAO()
        .addAnswer(new AnswerInfo(
            new AudioContext(0, defaultUser, projid, languageEnum, exid, 0, REGULAR),
            new AnswerInfo.RecordingInfo(pathOnDisk, pathOnDisk, "", "", foreignLanguage, ""), valid, ""), now);
    logger.info("addResultAndAudio Remember path        " + pathOnDisk);
    File absoluteFile = pathHelper.getAbsoluteAudioFile(pathOnDisk);
    logger.info("addResultAndAudio Remember absoluteFile " + absoluteFile.getAbsolutePath());

    String permanentAudioPath = pathWriter.
        getPermanentAudioPath(
            absoluteFile,
            getPermanentName(defaultUser, REGULAR),
            true,
            languageEnum,
            exid,
            db.getServerProps(),
            getTrackInfo(defaultUser, k, languageEnum, foreignLanguage));

    db.getAudioDAO().addOrUpdate(new AudioInfo(
        defaultUser,
        exid,
        projid,
        REGULAR,
        permanentAudioPath,
        now,
        valid.getDurationInMillis(),
        foreignLanguage,
        (float) valid.getDynamicRange(),
        resultID,
        MiniUser.Gender.Male,
        false));
  }

  @NotNull
  private TrackInfo getTrackInfo(int defaultUser, ClientExercise k, Language languageEnum, String foreignLanguage) {
    return new TrackInfo(foreignLanguage, getArtist(defaultUser), k.getEnglish(), languageEnum.getLanguage(), k.getUnitToValue());
  }

  private String getPermanentName(int user, AudioType audioType) {
    return audioType.toString() + "_" + System.currentTimeMillis() + "_by_" + user + ".wav";
  }

  private String getArtist(int user) {
    User userWhere = db.getUserDAO().getUserWhere(user);
    return userWhere == null ? "" + user : userWhere.getUserID();
  }

  @NotNull
  private SlickImage getSlickImage(int projid, long now, Dialog dialog, Timestamp modified) {
    Timestamp ago = new Timestamp(now - FIVE_YEARS);

    String imageRef = dialog.getImageRef();

    //  logger.info("getSlickImage image ref " + imageRef);
    return new SlickImage(
        -1,
        projid,
        -1,

        modified,
        modified,

        "",
        imageRef,

        0,
        0,

        false,
        false,
        ago
    );
  }

  /**
   * TODO : remove related exercise, related exercise entries, and dialogs
   *
   * @param project
   * @return
   */
  public boolean cleanDialog(Project project) {
    int id = project.getID();
    {
      Collection<Integer> toDelete = new HashSet<>();
      Collection<Integer> toRemove = new HashSet<>();
      db.getDialogDAO().getDialogs(id)
          .forEach(iDialog -> {

            toRemove.add(iDialog.getID());

            iDialog.getExercises().forEach(clientExercise -> toDelete.add(clientExercise.getID()));
            iDialog.getCoreVocabulary().forEach(clientExercise -> toDelete.add(clientExercise.getID()));
          });

      logger.info("cleanDialog deleting " + toDelete.size() + " exercises");
      db.getUserExerciseDAO().deleteByExID(toDelete);

      logger.info("cleanDialog removing " + toRemove.size() + " context exercise relations for these dialogs");
      toRemove.forEach(dir -> db.getUserExerciseDAO().getRelatedExercise().deleteRelatedForDialog(dir));
    }

    db.getDialogDAO().removeForProject(id);
    return true;
  }
}
