package mitll.langtest.shared.dialog;

import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.HasUnitChapter;

import java.util.List;
import java.util.Map;

public interface IDialog extends CommonShell, HasUnitChapter  {
  int getUserid();

  int getProjid();

  int getDominoid();

  long getModified();

  DialogType getKind();

  String getImageRef();

  String getOrientation();

 // String getEntitle();

 // String getFltitle();

  String getUnit();

  String getChapter();

  List<ExerciseAttribute> getAttributes();

  List<CommonExercise> getExercises();


  List<String> getSpeakers();

  Map<String, List<CommonExercise>> groupBySpeaker();
}
