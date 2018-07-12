package mitll.langtest.shared.dialog;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.HasUnitChapter;

import java.util.List;
import java.util.Map;

public interface IDialog extends CommonShell, HasUnitChapter {
  enum METADATA implements IsSerializable {
    UNIT, CHAPTER, PAGE, PRESENTATION, FLPRESENTATION, SPEAKER, FLTITLE;

    public String getLC() {
      return toString().toLowerCase();
    }

    public String getCap() {
      return toString().substring(0, 1).toUpperCase() + toString().substring(1);
    }
  }

  int getUserid();

  int getProjid();

  int getDominoid();

  long getModified();

  DialogType getKind();

  String getImageRef();

  String getOrientation();

  String getUnit();

  String getChapter();

  List<ExerciseAttribute> getAttributes();

  List<ClientExercise> getExercises();

  List<ClientExercise> getCoreVocabulary();


  List<String> getSpeakers();

  Map<String, List<ClientExercise>> groupBySpeaker();
}
