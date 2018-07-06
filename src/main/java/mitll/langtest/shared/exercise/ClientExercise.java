package mitll.langtest.shared.exercise;

import java.util.List;

public interface ClientExercise extends CommonShell, MutableAnnotationExercise, AudioRefExercise, ScoredExercise, Details {
  /**
   * @return
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#addAltFL
   */
  String getAltFL();

  String getTransliteration();

  /**
   * @return
   * @deprecated
   */
  String getAltFLToShow();

  boolean hasContext();

  boolean isContext();

  /**
   * Get the first context sentence.
   *
   * @return
   */
  String getContext();

  String getContextTranslation();

  List<ClientExercise> getDirectlyRelated();

  /**
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton
   */
  MutableAudioExercise getMutableAudio();

  /**
   * @return
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#getCommentBox
   */
  MutableAnnotationExercise getMutableAnnotation();

  CommonExercise asCommon();

  /**
   * SERVER?
   * @return
   */
  List<ExerciseAttribute> getAttributes();
}
