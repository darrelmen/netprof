package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.SimpleRecordExercisePanel;
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
public class WaveformExercisePanelFactory extends ExercisePanelFactory {
  /**
   * @see mitll.langtest.client.LangTest#setGrading(boolean)
   * @param service
   * @param userFeedback
   * @param controller
   */
  public WaveformExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                     final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  @Override
  public Panel getExercisePanel(Exercise e) {
    return new WaveformExercisePanel(e, service, userFeedback, controller);
  }
}
