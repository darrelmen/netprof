package mitll.langtest.client.recorder;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
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
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   * @param exerciseList
   */
  public SimpleRecordExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                   final ExerciseController controller, ListInterface exerciseList) {
    super(e,service,userFeedback,controller, exerciseList);
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see mitll.langtest.client.exercise.ExercisePanel#getQuestionPanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, java.util.List, java.util.List, int, mitll.langtest.shared.Exercise.QAPair, com.google.gwt.user.client.ui.HasWidgets)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    System.out.println("getAnswerWidget : ex " +exercise.getID()+ " index " + index);

    SimpleRecordExercisePanel questionState = this;
    Panel panel = new SimpleRecordPanel(service, controller, exercise, questionState, index);
    addAnswerWidget(index, panel);
    return panel;
  }

  /**
   * @see ExercisePanel#addQuestionPrompt(com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.Exercise)
   * @param promptInEnglish
   * @return
   */
  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) { return getSpokenPrompt(promptInEnglish);  }

  @Override
  protected void addInstructions() {}

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   * @see mitll.langtest.client.exercise.NavigationHelper#clickNext
   * @param controller
   * @param completedExercise
   */
  @Override
  public void postAnswers(ExerciseController controller, Exercise completedExercise) {
    exerciseList.loadNextExercise(completedExercise);
  }
}
