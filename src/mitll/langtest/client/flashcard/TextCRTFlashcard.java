package mitll.langtest.client.flashcard;

import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/29/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class TextCRTFlashcard extends DataCollectionFlashcard {
  private TextResponse textResponse;

  public TextCRTFlashcard(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    super(e, service, controller, 40);
    getElement().setId("TextCRTFlashcard");
  }

  /**
   * @see #getCardPrompt(mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseController)
   * @param e
   * @param controller
   */
  @Override
  protected void makeNavigationHelper(Exercise e, ExerciseController controller) {
    navigationHelper = new NavigationHelper(e, controller, null,controller.getExerciseList(),false, true);  // todo how to control whether to add buttons???
    textResponse = new TextResponse(controller.getUser(), soundFeedback, new TextResponse.AnswerPosted() {
      @Override
      public void answerPosted() {
        TextCRTFlashcard.this.answerPosted();
      }
    });
  }

  protected void answerPosted() {
    navigationHelper.enableNextButton(true);
  }

  @Override
  protected void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                                int feedbackHeight) {
    textResponse.addWidgets(this, e, service, controller, true, false, true, 1);
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    textResponse.onUnload();
  }
}
