package mitll.langtest.client;

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
   * @see LangTest#onModuleLoad2()
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GradingExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                     final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  public ExercisePanel getExercisePanel(Exercise e) {
     return new GradingExercisePanel(e, service, userFeedback, controller);
  }
}
