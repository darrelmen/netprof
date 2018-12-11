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
import mitll.langtest.server.database.exercise.Project;
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

import static mitll.langtest.server.database.project.ProjectManagement.FIVE_YEARS;
import static mitll.langtest.shared.answer.AudioType.REGULAR;

/**
 * Add dialog info to project.
 */
public class DialogPopulate {
  private static final Logger logger = LogManager.getLogger(DialogPopulate.class);

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
    pathWriter = new PathWriter(db.getServerProps());
    this.pathHelper = pathHelper;
  }

  /**
   * Take our canned data and put it in the database.
   *
   * @param project
   * @see ProjectManagement#addDialogInfo
   */
  public boolean addDialogInfo(Project project) {
    int projid = project.getID();
    IDialogDAO dialogDAO = db.getDialogDAO();

    List<IDialog> dialogs1 = dialogDAO.getDialogs(projid);
    if (dialogs1.isEmpty()) {
      logger.warn("addDialogInfo no dialog info yet loaded for " + project);
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
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyDialog
   */
  public boolean populateDatabase(Project project) {
    int projid = project.getID();
    IDialogDAO dialogDAO = db.getDialogDAO();
    if (!dialogDAO.getDialogs(projid).isEmpty()) {
      logger.info("Project #" + projid + " already has dialog data so not adding any.");
      return false;
    } else {
      waitUntilTrieReady(project);

      maybeDoDialogImport(project, dialogDAO);

      maybeDoInterpreterImport(project, dialogDAO);

      return true;
    }
  }

  private void maybeDoInterpreterImport(Project project, IDialogDAO dialogDAO) {
    int defaultUser = db.getUserDAO().getDefaultUser();
    Language languageEnum = project.getLanguageEnum();

    if (languageEnum == Language.MANDARIN) {
      logger.info("maybeDoInterpreterImport found interpreter candidate " + project);

      Map<ClientExercise, String> exToAudio = new HashMap<>();

      Map<Dialog, SlickDialog> dialogToSlick = new InterpreterReader().getDialogs(defaultUser, exToAudio, project);

      logger.info("maybeDoInterpreterImport read " + dialogToSlick.size());
      addDialogs(project, dialogDAO, exToAudio, defaultUser, dialogToSlick);
    }
  }

  private void maybeDoDialogImport(Project project, IDialogDAO dialogDAO) {
    Map<ClientExercise, String> exToAudio = new HashMap<>();
    int defaultUser = db.getUserDAO().getDefaultUser();
    Language languageEnum = project.getLanguageEnum();

    if (shouldTryToReadDialogInfo(languageEnum)) {
      Map<Dialog, SlickDialog> dialogToSlick = getDialogReader(languageEnum).getDialogs(defaultUser, exToAudio, project);
      addDialogs(project, dialogDAO, exToAudio, defaultUser, dialogToSlick);
    }
  }

  private void addDialogs(Project project,
                          IDialogDAO dialogDAO,
                          Map<ClientExercise, String> exToAudio,
                          int defaultUser,

                          Map<Dialog, SlickDialog> dialogToSlick) {
    Set<Dialog> dialogs = dialogToSlick.keySet();

    long now = System.currentTimeMillis();
    Timestamp modified = new Timestamp(now);
    AudioCheck audioCheck = new AudioCheck(db.getServerProps().shouldTrimAudio(), db.getServerProps().getMinDynamicRange());

    int projid = project.getID();
    Map<ExerciseAttribute, Integer> attrToInt = getExerciseAttributeToID(projid, defaultUser, dialogs, now);

    Map<ClientExercise, Integer> allImportExToID = new HashMap<>();
    List<String> typeOrder = project.getTypeOrder();

    dialogs.forEach(dialog -> {
      // add the image
      int imageID = db.getImageDAO().insert(getSlickImage(projid, now, dialog, modified));

      SlickDialog slickDialog = dialogToSlick.get(dialog);
      slickDialog.imageid_$eq(imageID);

      // add the dialog to the database
      int dialogID = dialogDAO.add(defaultUser, projid, 1, imageID, now, now,
          dialog.getUnit(), dialog.getChapter(),
          DialogType.DIALOG, DialogStatus.DEFAULT,
          dialog.getEnglish(), dialog.getOrientation());

      // add dialog attributes
      addDialogAttributes(dialogDAO, defaultUser, modified, attrToInt, dialog, dialogID);

      // add the exercises
      addExercises(projid, defaultUser, modified, allImportExToID, typeOrder, dialog, dialogID);

      // add core vocab
      addCoreVocab(projid, modified, dialog, dialogID);
    });

    addAudio(project, projid, exToAudio, defaultUser, now, audioCheck, allImportExToID);

    new AudioCopy(db, db.getProjectManagement(), db)
        .copyAudio(projid, allImportExToID.keySet(), new HashMap<>());
  }

  private void addExercises(int projid,
                            int defaultUser,
                            Timestamp modified,
                            Map<ClientExercise, Integer> allImportExToID,
                            List<String> typeOrder,
                            Dialog dialog,
                            int dialogID) {
    ExerciseCopy exerciseCopy = new ExerciseCopy();

    Map<CommonExercise, Integer> importExToID = exerciseCopy.addExercisesAndAttributes(
        defaultUser,
        projid,
        db.getUserExerciseDAO(),
        getCommonExercisesFromDialog(dialog),
        typeOrder,
        new HashMap<>(),
        new HashMap<>(), true);

    allImportExToID.putAll(importExToID);

    importExToID.forEach((k, v) -> k.getMutable().setID(v));

    db.getUserExerciseDAO().getRelatedExercise().addBulkRelated(
        getSlickRelatedExercises(projid, modified, dialog, dialogID, importExToID));

  }

  @NotNull
  private List<CommonExercise> getCommonExercisesFromDialog(Dialog dialog) {
    List<CommonExercise> commonExercises = new ArrayList<>();

    dialog.getExercises().forEach(clientExercise -> commonExercises.add(clientExercise.asCommon()));
    return commonExercises;
  }

  private boolean shouldTryToReadDialogInfo(Language languageEnum) {
    return languageEnum == Language.KOREAN || languageEnum == Language.ENGLISH;
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
    Map<Integer, ExerciseAttribute> idToPair = db.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid);

    Map<ExerciseAttribute, Integer> attrToInt = new HashMap<>();

    addNewAttributes(projid, defaultUser, dialogs, now, idToPair, attrToInt);
    idToPair.forEach((k, v) -> attrToInt.put(v, k));
    return attrToInt;
  }

  @NotNull
  private IDialogReader getDialogReader(Language languageEnum) {
    return languageEnum == Language.KOREAN ? new KPDialogs() : new EnglishDialog();
  }

  private void addCoreVocab(int projid, Timestamp modified, Dialog dialog, int dialogID) {
    List<SlickRelatedExercise> relatedExercises = new ArrayList<>();

    dialog.getCoreVocabulary().forEach(clientExercise ->
        relatedExercises.add(new SlickRelatedExercise(-1, clientExercise.getID(),
            clientExercise.getID(), projid, dialogID, modified))
    );

    db
        .getUserExerciseDAO()
        .getRelatedCoreExercise()
        .addBulkRelated(relatedExercises);
  }

  @NotNull
  private List<SlickRelatedExercise> getSlickRelatedExercises(int projid,
                                                              Timestamp modified,
                                                              Dialog dialog,
                                                              int dialogID,
                                                              Map<CommonExercise, Integer> importExToID) {
    ClientExercise prev = null;
    List<SlickRelatedExercise> relatedExercises = new ArrayList<>();
    for (ClientExercise ex : dialog.getExercises()) {
      if (prev != null) {
        int prevID = importExToID.get(prev.asCommon());
        int currID = importExToID.get(ex.asCommon());

        relatedExercises.add(new SlickRelatedExercise(-1, prevID, currID, projid, dialogID, modified));
      }
      prev = ex;
    }
    return relatedExercises;
  }

/*
  private List<ExerciseAttribute> findMatchingAttr(CommonExercise commonExercise, String type) {
    return commonExercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(type)).collect(Collectors.toList());
  }*/

  private void addAudio(Project project,
                        int projid,
                        Map<ClientExercise, String> exToAudio,
                        int defaultUser, long now,
                        AudioCheck audioCheck,
                        Map<ClientExercise, Integer> allImportExToID) {
    exToAudio.forEach((k, v) -> {
      File file = new File(db.getServerProps().getAudioBaseDir(), v);
      if (!file.exists()) {
        logger.error("can't find audio file " + file.getAbsolutePath());
      } else {
        logger.info("addAudio : found audio at " + file.getAbsolutePath());
      }
      AudioCheck.ValidityAndDur valid = audioCheck.isValid(file, true, false);

      Integer exid = allImportExToID.get(k);
      //  Integer exid = exToID.get(k);
      if (exid == null) logger.error("can't find ex by '" + k + "' in " + allImportExToID.size());
      else {
   /*     Result result = new Result(-1, defaultUser,
            exid,
            0, v, true, now, AudioType.REGULAR, valid.durationInMillis,
            true, 0.99F, "", "", 0, 0, false,
            (float) valid.getDynamicRange(), valid.getValidity().toString(), "");*/
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
          .put(exerciseAttribute, db.getUserExerciseDAO().getExerciseAttribute()
              .addAttribute(projid,
                  now, defaultUser, exerciseAttribute, true));
    });
    int after = attrToInt.size();
    logger.info("addNewAttributes really added " + (after - before));
  }

  private void addResultAndAudio(Project project, int projid, int defaultUser, long now,
                                 ClientExercise k, String pathOnDisk, AudioCheck.ValidityAndDur valid,
                                 Integer exid) {
    String language = project.getLanguage();
    int resultID = db.getAnswerDAO()
        .addAnswer(new AnswerInfo(
            new AudioContext(0, defaultUser, projid, language, exid, 0, REGULAR),
            new AnswerInfo.RecordingInfo(pathOnDisk, pathOnDisk, "", "", k.getForeignLanguage(), ""), valid, ""), now);
    logger.info("Remember path " + pathOnDisk);
    File absoluteFile = pathHelper.getAbsoluteAudioFile(pathOnDisk);
    logger.info("Remember absoluteFile " + absoluteFile.getAbsolutePath());

    String permanentAudioPath = pathWriter.
        getPermanentAudioPath(
            absoluteFile,
            getPermanentName(defaultUser, REGULAR),
            true,
            language,
            exid,
            db.getServerProps(),
            new TrackInfo(k.getForeignLanguage(), getArtist(defaultUser), k.getEnglish(), language));

    db.getAudioDAO().addOrUpdate(new AudioInfo(
        defaultUser,
        exid,
        projid,
        REGULAR,
        permanentAudioPath,
        now,
        valid.getDurationInMillis(),
        k.getForeignLanguage(),
        (float) valid.getDynamicRange(),
        resultID,
        MiniUser.Gender.Male,
        false));
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

    return new SlickImage(
        -1,
        projid,
        -1,

        modified,
        modified,

        "",
        dialog.getImageRef(),

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
    {
      Collection<Integer> toDelete = new HashSet<>();
      Collection<Integer> toRemove = new HashSet<>();
      db.getDialogDAO().getDialogs(project.getID())
          .forEach(iDialog -> {

            toRemove.add(iDialog.getID());

            iDialog.getExercises()
                .forEach(clientExercise -> {
                  toDelete.add(clientExercise.getID());
                });
          });

      logger.info("deleting " + toDelete.size() + " exercises");
      db.getUserExerciseDAO().deleteByExID(toDelete);

      logger.info("removing " + toRemove.size() + " context exercise relations for these dialogs");
      toRemove.forEach(dir -> db.getUserExerciseDAO().getRelatedExercise().deleteRelatedForDialog(dir));
    }

    db.getDialogDAO().removeForProject(project.getID());
    return true;
  }
}
