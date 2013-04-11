package mitll.langtest.client.flashcard;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlashcardExercisePanelFactory extends ExercisePanelFactory {
  /**
   * @see mitll.langtest.client.LangTest#setFactory()
   * @param service
   * @param userFeedback
   * @param controller
   */
  public FlashcardExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                       final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  public Panel getExercisePanel(Exercise e) {
    if (e.getType() == Exercise.EXERCISE_TYPE.RECORD) {
      return new BootstrapExercisePanel(e, service, controller);
    } else {
      return new ExercisePanel(e, service, userFeedback, controller);
    }
  }
}
