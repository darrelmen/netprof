package mitll.langtest.shared.dialog;

import mitll.langtest.shared.exercise.*;

import java.util.List;
import java.util.Map;

/**
 * @see mitll.langtest.server.services.DialogServiceImpl#getScoreHistoryForDialogs
 */
public interface IDialog extends CommonShell, HasUnitChapter, Scored {
  int getUserid();

  int getProjid();

  /**
   * @deprecated
   * @return
   */
  int getDominoid();

  long getModified();

  DialogType getKind();


  String getImageRef();


  String getOrientation();


  String getUnit();

  String getChapter();
  String getCountryCode();


  String getAttributeValue(DialogMetadata metadata);

  /**
   * Attributes/meta-data of a dialog
   * @return
   */
  List<ExerciseAttribute> getAttributes();

  /**
   * All the exercises in a dialog.
   * @return
   */
  List<ClientExercise> getExercises();

  List<ClientExercise> getCoreVocabulary();

  List<ClientExercise> getBothExercisesAndCore();

  /**
   * The names of the speakers in a dialog - could be more than 2!
   * @return
   */
  List<String> getSpeakers();

  Map<String, List<ClientExercise>> groupBySpeaker();
}
