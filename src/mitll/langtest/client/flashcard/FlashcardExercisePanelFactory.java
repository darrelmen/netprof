package mitll.langtest.client.flashcard;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Make Bootstrap exercise panels...
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlashcardExercisePanelFactory extends ExercisePanelFactory {
  /**
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param exerciseList
   * @see mitll.langtest.client.LangTest#setFactory
   */
  public FlashcardExercisePanelFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                       final ExerciseController controller, ListInterface exerciseList) {
    super(service, userFeedback, controller, exerciseList);
  }

  /**
   * @see BootstrapFlashcardExerciseList.FlashcardResponseAsyncCallback#onSuccess(mitll.langtest.shared.flashcard.FlashcardResponse)
   * @param e
   * @return
   */
  public Panel getExercisePanel(Exercise e) {
    return new BootstrapExercisePanel(e, service, controller, 40);
  }
}
