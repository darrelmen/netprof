package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickDialog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static mitll.langtest.shared.dialog.DialogMetadata.FLTITLE;

public class BaseDialogReader {
  private static final String OPT_NETPROF_DIALOG = "/opt/netprof/dialog/";

  protected void addDialogPair(int defaultUser,
                               int projID,
                               Timestamp modified,

                               String imageRef,
                               String unit, String chapter,
                               List<ExerciseAttribute> attributes,
                               List<ClientExercise> exercises,
                               Set<ClientExercise> coreExercises,

                               String orientation, String title, String fltitle,

                               Map<Dialog, SlickDialog> dialogToSlick) {
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
        DialogType.DIALOG.toString(),
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
        new ArrayList<>(coreExercises));

    dialogToSlick.put(dialog, slickDialog);
  }

  @NotNull
  protected String getDialogDataDir(Project project) {
    return OPT_NETPROF_DIALOG + project.getLanguage().toLowerCase() + File.separator;
  }
}
