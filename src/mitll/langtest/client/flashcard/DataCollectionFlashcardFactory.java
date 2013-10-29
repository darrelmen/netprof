package mitll.langtest.client.flashcard;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.DataCollectionFlashcard;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import com.google.gwt.user.client.ui.Panel;

/**
 * Make Bootstrap exercise panels...
 * User: GO22670
 * Date: 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataCollectionFlashcardFactory extends ExercisePanelFactory {
  /**
   * @param service
   * @param userFeedback
   * @param controller
   * @see mitll.langtest.client.LangTest#setFactory
   */
  public DataCollectionFlashcardFactory(final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                        final ExerciseController controller) {
    super(service, userFeedback, controller);
  }

  /**
   * @see mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList.FlashcardResponseAsyncCallback#onSuccess(mitll.langtest.shared.flashcard.FlashcardResponse)
   * @param e
   * @return
   */
  public Panel getExercisePanel(Exercise e) {
    return new DataCollectionFlashcard(e, service, controller, 30);
  }
}
