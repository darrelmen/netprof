package mitll.langtest.shared.dialog;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.exercise.*;

import java.util.List;
import java.util.Map;

/**
 * @see mitll.langtest.server.services.DialogServiceImpl#getScoreHistoryForDialogs
 */
public interface IDialog extends CommonShell, HasUnitChapter, Scored {
  enum METADATA implements IsSerializable {
    UNIT, CHAPTER, PAGE, PRESENTATION, FLPRESENTATION, SPEAKER, FLTITLE;

    public String getLC() {
      return toString().toLowerCase();
    }

    public String getCap() {
      return toString().substring(0, 1).toUpperCase() + toString().substring(1);
    }
  }

  String getAttributeValue(METADATA metadata);

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
