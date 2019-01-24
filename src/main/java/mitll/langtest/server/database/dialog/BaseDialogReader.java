package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

import static mitll.langtest.shared.dialog.DialogMetadata.FLTITLE;

public class BaseDialogReader {
  private static final Logger logger = LogManager.getLogger(BaseDialogReader.class);
  private static final String OPT_NETPROF_DIALOG = "/opt/netprof/dialog/";
  private static final List<String> SPEAKER_LABELS = Arrays.asList("A", "B", "C", "D", "E", "F", "I");
  private static final String IMAGES = "images/";
  private static final String JPG = ".jpg";

  /**
   * @param defaultUser
   * @param projID
   * @param modified
   * @param imageRef
   * @param unit
   * @param chapter
   * @param attributes
   * @param exercises
   * @param coreExercises
   * @param orientation
   * @param title
   * @param fltitle
   * @param outputDialogToSlick
   * @param dialogType
   * @see DialogReader#getDialogsByProp
   */
  protected void addDialogPair(int defaultUser,
                               int projID,
                               Timestamp modified,

                               String imageRef,
                               String unit, String chapter,
                               List<ExerciseAttribute> attributes,
                               List<ClientExercise> exercises,
                               Set<ClientExercise> coreExercises,

                               String orientation, String title, String fltitle,

                               Map<Dialog, SlickDialog> outputDialogToSlick, DialogType dialogType) {
    List<ExerciseAttribute> dialogAttr = new ArrayList<>(attributes);
    dialogAttr.add(new ExerciseAttribute(FLTITLE.toString().toLowerCase(), fltitle, false));

    SlickDialog slickDialog = new SlickDialog(-1,
        defaultUser,
        projID,
        -1,
        -1,
        modified,
        modified,
        unit, chapter,
        dialogType.toString(),
        DialogStatus.DEFAULT.toString(),
        title,
        orientation
    );

    Dialog dialog = new Dialog(-1, defaultUser, projID, -1, -1, modified.getTime(),
        unit, chapter,
        orientation,
        imageRef,
        fltitle,
        title,

        dialogAttr,
        exercises,
        new ArrayList<>(coreExercises), dialogType);

    outputDialogToSlick.put(dialog, slickDialog);
  }

  @NotNull
  String getDialogDataDir(Project project) {
    return OPT_NETPROF_DIALOG + project.getLanguage().toLowerCase() + File.separator;
  }

  void addSpeakerAttrbutes(List<ExerciseAttribute> attributes, Set<String> speakers) {
    List<String> speakersList = new ArrayList<>(speakers);
    speakersList
        .forEach(s -> {
          int index = speakersList.indexOf(s);
          if (index == -1) logger.warn("no speaker label for " + s);
          attributes
              .add(new ExerciseAttribute(
                  DialogMetadata.SPEAKER.getCap() +
                      " " + SPEAKER_LABELS.get(index), s, false));
        });
  }

  @NotNull
   String getImageRef(String imageBaseDir, String dir) {
    return imageBaseDir + dir + File.separator + dir + JPG;
  }

  @NotNull
   String getImageBaseDir(Project project) {
    return IMAGES + project.getLanguage().toLowerCase() + File.separator;
  }
}
