package mitll.langtest.client.grading;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class GradingExercisePanelFactory extends ExercisePanelFactory {
  /**
   * @see mitll.langtest.client.LangTest
   * @param service
   * @param userFeedback
   * @param controller
   * @param exerciseList
   */
  public GradingExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                     final ExerciseController controller, ListInterface exerciseList) {
    super(service, userFeedback, controller, exerciseList);
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#useExercise
   * @param e
   * @return
   */
  @Override
  public Panel getExercisePanel(Exercise e) {
     return new GradingExercisePanel(e, service, userFeedback, controller, exerciseList);
  }
}
