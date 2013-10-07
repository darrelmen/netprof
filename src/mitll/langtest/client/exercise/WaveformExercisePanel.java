package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Does fancy flashing record bulb while recording.
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class WaveformExercisePanel extends ExercisePanel {

  private boolean isBusy = false;
  private RecordAudioPanel audioPanel;


  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public WaveformExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                   final ExerciseController controller) {
    super(e, service, userFeedback, controller);
  }

  public void setBusy(boolean v) {
    this.isBusy = v;
    setButtonsEnabled(!isBusy);
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see mitll.langtest.client.exercise.ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    audioPanel = new RecordAudioPanel(exercise, controller, this, service, index, true);
    return audioPanel;
  }

  @Override
  public void onResize() {
    audioPanel.onResize();
  }

  /**
   * @see ExercisePanel#addQuestionPrompt(com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.Exercise)
   * @param promptInEnglish
   * @return
   */
  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param controller
   * @param completedExercise
   */
  @Override
  public void postAnswers(ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }

  public boolean isBusy() {
    return isBusy;
  }

}
