package mitll.langtest.client;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.recorder.SimpleRecordPanel;
import mitll.langtest.shared.Exercise;

/**
 * Mainly delegates recording to the {@link SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleRecordExercisePanel extends ExercisePanel {
  /**
   * @see ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public SimpleRecordExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                   final ExerciseController controller) {
    super(e,service,userFeedback,controller);
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see mitll.langtest.client.ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.UserFeedback, mitll.langtest.client.ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
      return new SimpleRecordPanel(service, controller,exercise, this, index);
  }

  @Override
  protected String getQuestionPrompt(Exercise e) {
    return "&nbsp;&nbsp;&nbsp;Speak and record your answer in " +(e.promptInEnglish ? "English" : " the foreign language") +" :";
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param completedExercise
   */
  @Override
  protected void postAnswers(LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }
}
