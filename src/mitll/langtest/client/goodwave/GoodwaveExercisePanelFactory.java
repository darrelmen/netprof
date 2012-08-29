package mitll.langtest.client.goodwave;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.grading.GradingExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class GoodwaveExercisePanelFactory extends ExercisePanelFactory {
  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2()
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GoodwaveExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                      final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  public ExercisePanel getExercisePanel(Exercise e) {
     return new GoodwaveExercisePanel(e, service, userFeedback, controller);
  }
}
