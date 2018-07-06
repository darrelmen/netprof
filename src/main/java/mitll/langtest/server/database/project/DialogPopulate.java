package mitll.langtest.server.database.project;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.AudioInfo;
import mitll.langtest.server.database.copy.ExerciseCopy;
import mitll.langtest.server.database.dialog.DialogStatus;
import mitll.langtest.server.database.dialog.IDialogDAO;
import mitll.langtest.server.database.dialog.KPDialogs;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.user.MiniUser;
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

public class DialogPopulate {
  private static final Logger logger = LogManager.getLogger(DialogPopulate.class);

  private final DatabaseImpl db;

  DialogPopulate(DatabaseImpl db) {
    this.db = db;
  }

  /**
   * Take our canned data and put it in the database.
   *
   * @param project
   * @see ProjectManagement#addDialogInfo
   */
  boolean addDialogInfo(Project project) {
    int projid = project.getID();
    IDialogDAO dialogDAO = db.getDialogDAO();

    List<IDialog> dialogs1 = dialogDAO.getDialogs(projid);
    if (dialogs1.isEmpty()) {
      Map<CommonExercise, String> exToAudio = new HashMap<>();
      int defaultUser = db.getUserDAO().getDefaultUser();
      List<Dialog> dialogs = new KPDialogs().getDialogs(defaultUser, projid, exToAudio);

      long now = System.currentTimeMillis();
      Timestamp modified = new Timestamp(now);
      ExerciseCopy exerciseCopy = new ExerciseCopy();
      AudioCheck audioCheck = new AudioCheck(db.getServerProps().shouldTrimAudio(), db.getServerProps().getMinDynamicRange());

      Map<Integer, ExerciseAttribute> idToPair = db.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid);

      Map<ExerciseAttribute, Integer> attrToInt = new HashMap<>();

      addNewAttributes(projid, defaultUser, dialogs, now, idToPair, attrToInt);
      idToPair.forEach((k, v) -> attrToInt.put(v, k));

      Map<CommonExercise, Integer> allImportExToID = new HashMap<>();
      List<String> typeOrder = project.getTypeOrder();

      //  List<String> types = typeOrder.subList(0, Math.min(2, typeOrder.size()));
      dialogs.forEach(dialog -> {
        // add the image
        int imageID = db.getImageDAO().insert(getSlickImage(projid, now, dialog, modified));

        dialog.getSlickDialog().imageid_$eq(imageID);

        // add the dialog to the database
        int dialogID = dialogDAO.add(defaultUser, projid, 1, imageID, now, now,
            dialog.getUnit(), dialog.getChapter(),
            DialogType.DIALOG, DialogStatus.DEFAULT,
            dialog.getEnglish(), dialog.getOrientation());

        // add dialog attributes
        addDialogAttributes(dialogDAO, defaultUser, modified, attrToInt, dialog, dialogID);

        if (false) {
          dialog.getExercises().forEach(commonExercise -> logger.info(commonExercise.getOldID() + " " + commonExercise.getForeignLanguage() + " " + commonExercise.getUnitToValue()));
        }
        // add the exercises
        Map<CommonExercise, Integer> importExToID = exerciseCopy.addExercisesAndAttributes(
            defaultUser,
            projid,
            db.getUserExerciseDAO(),
            dialog.getExercises(),
            typeOrder,
            new HashMap<>(),
            new HashMap<>(), true);

        allImportExToID.putAll(importExToID);
        {
          CommonExercise prev = null;
          List<SlickRelatedExercise> relatedExercises = new ArrayList<>();
          for (CommonExercise ex : dialog.getExercises()) {
            if (prev != null) {
              int prevID = importExToID.get(prev);
              int currID = importExToID.get(ex);

              relatedExercises.add(new SlickRelatedExercise(-1, prevID, currID, projid, dialogID, modified));
            }
            prev = ex;
          }
          db.getUserExerciseDAO().getRelatedExercise().addBulkRelated(relatedExercises);
        }

//        if (parentToChild.size() != dialog.getExercises().size())
//          logger.error("tried to add " + dialog.getExercises().size() + " but only did " + parentToChild.size());

        // add the audio
        // add results so have fk ref
        // add the audio
      });


      addAudio(project, projid, exToAudio, defaultUser, now, audioCheck, allImportExToID);
      return true;
    } else {
      project.setDialogs(dialogs1);
      return false;
    }
  }

/*
  private List<ExerciseAttribute> findMatchingAttr(CommonExercise commonExercise, String type) {
    return commonExercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(type)).collect(Collectors.toList());
  }*/

  private void addAudio(Project project,
                        int projid,
                        Map<CommonExercise, String> exToAudio,
                        int defaultUser, long now,
                        AudioCheck audioCheck, Map<CommonExercise, Integer> allImportExToID) {
    exToAudio.forEach((k, v) -> {
      File file = new File(db.getServerProps().getAudioBaseDir(), v);
      if (!file.exists()) logger.error("can't find audio file " + file.getAbsolutePath());
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

  private void addNewAttributes(int projid, int defaultUser, List<Dialog> dialogs, long now,
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
    logger.info("adding " + allToAdd.size() + " new attr");
    allToAdd.forEach(exerciseAttribute -> {
      logger.info("adding new exercise attribute " + exerciseAttribute);

      attrToInt
          .put(exerciseAttribute, db.getUserExerciseDAO().getExerciseAttribute()
              .addAttribute(projid,
                  now, defaultUser, exerciseAttribute, true));
    });
    int after = attrToInt.size();
    logger.info("really added " + (after - before));
  }

  private void addResultAndAudio(Project project, int projid, int defaultUser, long now, CommonExercise k, String v, AudioCheck.ValidityAndDur valid, Integer exid) {
    int resultID = db.getAnswerDAO()
        .addAnswer(new AnswerInfo(
            new AudioContext(0, defaultUser, projid, project.getLanguage(), exid, 0, AudioType.REGULAR),
            new AnswerInfo.RecordingInfo(v, v, "", "", false, k.getForeignLanguage(), ""), valid, ""), now);


    db.getAudioDAO().addOrUpdate(new AudioInfo(
        defaultUser,
        exid,
        projid,
        AudioType.REGULAR,
        v,
        now,
        valid.durationInMillis,
        k.getForeignLanguage(),
        (float) valid.getDynamicRange(),
        resultID,
        MiniUser.Gender.Male, false));
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

}
