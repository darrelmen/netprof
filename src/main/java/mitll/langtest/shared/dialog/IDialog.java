package mitll.langtest.shared.dialog;

import mitll.langtest.shared.exercise.*;

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

  List<ClientExercise> getExercises();


  List<String> getSpeakers();

  Map<String, List<ClientExercise>> groupBySpeaker();
}
