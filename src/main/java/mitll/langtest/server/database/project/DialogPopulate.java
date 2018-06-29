package mitll.langtest.server.database.project;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.AudioInfo;
import mitll.langtest.server.database.copy.ExerciseCopy;
import mitll.langtest.server.database.dialog.DialogStatus;
import mitll.langtest.server.database.dialog.DialogType;
import mitll.langtest.server.database.dialog.IDialogDAO;
import mitll.langtest.server.database.dialog.KPDialogs;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.SlickDialogAttributeJoin;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

import static mitll.langtest.server.database.project.ProjectManagement.FIVE_YEARS;

public class DialogPopulate {
  private static final Logger logger = LogManager.getLogger(DialogPopulate.class);

  DatabaseImpl db;

  DialogPopulate(DatabaseImpl db) {
    this.db = db;
  }

  /**
   * Take our canned data and put it in the database.
   *
   * @param project
   */
  public void addDialogInfo(Project project) {
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
    //  String dialogDir = db.getServerProps().getAudioBaseDir() + "dialog";

      Map<String, CommonExercise> oldToEx = new HashMap<>();

      Map<Integer, ExerciseAttribute> idToPair = db.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid);

      Map<ExerciseAttribute, Integer> attrToInt = new HashMap<>();

      {
        HashSet<ExerciseAttribute> known = new HashSet<>(idToPair.values());

        Set<ExerciseAttribute> allToAdd = new HashSet<>();
        dialogs.forEach(dialog -> {
          List<ExerciseAttribute> attributes = dialog.getAttributes();
          HashSet<ExerciseAttribute> toAdd = new HashSet<>(attributes);
          toAdd.removeAll(known);
          allToAdd.addAll(toAdd);
        });

        logger.info("adding " + allToAdd.size() + " attr");
        allToAdd.forEach(exerciseAttribute -> attrToInt.put(exerciseAttribute, db.getUserExerciseDAO().getExerciseAttribute().addAttribute(projid,
            now, defaultUser, exerciseAttribute)));
      }

      dialogs.forEach(dialog -> {
        // add the image
        int imageID = db.getImageDAO().insert(getSlickImage(projid, now, dialog, modified));

        dialog.getSlickDialog().imageid_$eq(imageID);

        // add the dialog to the database
        int dialogID = dialogDAO.add(defaultUser, projid, 1, imageID, now, now,
            DialogType.DIALOG, DialogStatus.DEFAULT,
            dialog.getEntitle(), dialog.getOrientation());

        // add dialog attributes
        {
          List<SlickDialogAttributeJoin> joins = new ArrayList<>();
          dialog.getAttributes().forEach(exerciseAttribute -> {
            Integer attrid = attrToInt.get(exerciseAttribute);
            if (attrid == null) logger.error("can't find id for " + exerciseAttribute);
            else
              joins.add(new SlickDialogAttributeJoin(-1, defaultUser, modified, dialogID, attrid));
          });

          dialogDAO.getDialogAttributeJoinHelper().addBulkAttributeJoins(joins);
        }

        // add the exercises
        Map<String, Integer> parentToChild = exerciseCopy.addExercises(
            defaultUser,
            projid,
            new HashMap<>(),
            (SlickUserExerciseDAO) db.getUserExerciseDAO(),
            dialog.getExercises(),
            project.getTypeOrder(),
            new HashMap<>(),
            new HashMap<>(),
            dialogID);
        dialog.getExercises().forEach(commonExercise -> {
          String oldID = commonExercise.getOldID();
          logger.info("old ex " +oldID);
          oldToEx.put(oldID, commonExercise);
        });

        if (parentToChild.size() != dialog.getExercises().size())
          logger.error("tried to add " + dialog.getExercises().size() + " but only did " + parentToChild.size());

        // add the audio
        // add results so have fk ref
        // add the audio
      });

      Set<Integer> exids = new HashSet<>();
      db.getUserExerciseDAO().getRelatedExercise()
          .getDialogIDToRelated(projid)
          .forEach((k, v) -> v
              .forEach(slickRelatedExercise -> exids
                  .add(slickRelatedExercise.exid())));

      // after adding to db get old -> db id
      Map<String, Integer> oldToID = new HashMap<>();
      List<SlickExercise> exercisesByIDs = db.getUserExerciseDAO().getExercisesByIDs(exids);
      logger.info("found " + exercisesByIDs.size() + " new exercises");
      exercisesByIDs.forEach(slickExercise -> oldToID.put(slickExercise.exid(), slickExercise.id()));
      logger.info("now " + oldToID.size() + " old->id entries");

      // get old ex to db id
      Map<CommonExercise, Integer> exToID = new HashMap<>();
      oldToID.forEach((k, v) -> {
        logger.info("k   " + k);
        logger.info("v   " + v);
        CommonExercise key = oldToEx.get(k);
        logger.info("key " + key);
        exToID.put(key, v);
      });

      exToAudio.forEach((k, v) -> {
        File file = new File(db.getServerProps().getAudioBaseDir(), v);
        if (!file.exists()) logger.error("can't find audio file " + file.getAbsolutePath());
        AudioCheck.ValidityAndDur valid = audioCheck.isValid(file, true, false);

        Integer exid = exToID.get(k);
        if (exid == null) logger.error("can't find ex by '" + k + "' in " + exToID.size());
        else {
     /*     Result result = new Result(-1, defaultUser,
              exid,
              0, v, true, now, AudioType.REGULAR, valid.durationInMillis,
              true, 0.99F, "", "", 0, 0, false,
              (float) valid.getDynamicRange(), valid.getValidity().toString(), "");*/
          addResultAndAudio(project, projid, defaultUser, now, k, v, valid, exid);
        }
      });
    } else {
      project.setDialogs(dialogs1);
    }
    // dialogDAO.getDialogs();
  }

  private void addResultAndAudio(Project project, int projid, int defaultUser, long now, CommonExercise k, String v, AudioCheck.ValidityAndDur valid, Integer exid) {
    int resultID = db.getAnswerDAO()
        .addAnswer(new AnswerInfo(
            new AudioContext(0, defaultUser, projid, project.getLanguage(), exid, 0, AudioType.REGULAR),
            new AnswerInfo.RecordingInfo(v, v, "", "", false, k.getForeignLanguage()), valid, ""), now);

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
