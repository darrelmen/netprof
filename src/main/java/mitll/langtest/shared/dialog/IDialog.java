package mitll.langtest.shared.dialog;

import mitll.langtest.server.database.dialog.DialogType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.HasID;

import java.util.List;
import java.util.Map;

public interface IDialog extends HasID {
  List<ExerciseAttribute> getAttributes();

  int getUserid();

  int getProjid();

  int getDominoid();

  long getModified();

  DialogType getKind();

  String getOrientation();

  String getImageRef();

  List<CommonExercise> getExercises();

  String getFltitle();

  String getEntitle();

  String getUnit();

  String getChapter();

  List<String> getSpeakers();

  Map<String, List<CommonExercise>> groupBySpeaker();
}
