package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.EnterKeyButtonHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.NoPasteTextBox;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/29/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class TextCRTFlashcard extends DataCollectionFlashcard {
  private EnterKeyButtonHelper enterKeyButtonHelper;
  private UserManager userManager;
  protected ScoreFeedback textScoreFeedback;
  private String responseType;

  public TextCRTFlashcard(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, UserManager userManager) {
    super(e, service, controller, 40);
    responseType = controller.getProps().getResponseType();
    this.userManager = userManager;
  }

  @Override
  protected void makeNavigationHelper(Exercise e, ExerciseController controller) {
    navigationHelper = new NavigationHelper(e, controller, false, false);
  }

  @Override
  protected void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service, ExerciseController controller,
                                                int feedbackHeight) {
    add(getAnswerAndRecordButtonRow(e, service, controller));
    add(textScoreFeedback.getScoreFeedbackRow(feedbackHeight));
  }

  /**
   * @param exercise
   * @param service
   * @param controller
   * @return
   */
  @Override
  protected Widget getAnswerAndRecordButtonRow(final Exercise exercise, final LangTestDatabaseAsync service,
                                               ExerciseController controller) {
    textScoreFeedback = new ScoreFeedback(false);
    return getTextResponseWidget(exercise, service, controller, textScoreFeedback);
  }

  private Widget getTextResponseWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                       ScoreFeedback scoreFeedback) {
    boolean allowPaste = controller.isDemoMode();
    final Button check = new Button("Check Answer");
    final TextBox noPasteAnswer = getAnswerBox(controller, allowPaste, check);
    String answerType = controller.getAudioType();
    setupSubmitButton(exercise, service, check, noPasteAnswer, scoreFeedback, answerType);

    FluidRow row = new FluidRow();
    row.add(noPasteAnswer);
    row.add(check);

    row.add(scoreFeedback.getFeedbackImage());
    return getRecordButtonRow(row);
  }

  private void setupSubmitButton(final Exercise exercise, final LangTestDatabaseAsync service, final Button check,
                                 final TextBox noPasteAnswer, final ScoreFeedback scoreFeedback, final String answerType) {
    check.setType(ButtonType.PRIMARY);
    check.setTitle("Hit Enter to submit answer.");
    check.setEnabled(false);
    check.addStyleName("leftFiveMargin");

    enterKeyButtonHelper = new EnterKeyButtonHelper(false);
    enterKeyButtonHelper.addKeyHandler(check);

    check.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String guess = noPasteAnswer.getText();
        getScoreForGuess(guess, service, exercise, check, scoreFeedback, answerType);
      }
    });
  }

  private TextBox getAnswerBox(ExerciseController controller, boolean allowPaste, final Button check) {
    final TextBox noPasteAnswer = allowPaste ? new TextBox() : new NoPasteTextBox();
    if (controller.isRightAlignContent()) {
      noPasteAnswer.addStyleName("rightAlign");
    }

    noPasteAnswer.setFocus(true);
    noPasteAnswer.addStyleName("topMargin");
    noPasteAnswer.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        check.setEnabled(noPasteAnswer.getText().length() > 0);
      }
    });

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        noPasteAnswer.setFocus(true);
      }
    });

    return noPasteAnswer;
  }

  private void getScoreForGuess(String guess, LangTestDatabaseAsync service, Exercise exercise, final Button check,
                                final ScoreFeedback scoreFeedback,String answerType) {
    check.setEnabled(false);
    scoreFeedback.setWaiting();

    service.getScoreForAnswer(userManager.getUser(), exercise, 1, guess, answerType, new AsyncCallback<Double>() {
      @Override
      public void onFailure(Throwable caught) {
        check.setEnabled(true);
      }

      @Override
      public void onSuccess(Double result) {
        check.setEnabled(true);
        gotScoreForGuess(result, check, scoreFeedback);
      }
    });
  }

  protected void gotScoreForGuess(Double result, Button check, ScoreFeedback scoreFeedback) {
    scoreFeedback.showCRTFeedback(result, soundFeedback, "Score ", false);
    scoreFeedback.hideFeedback();
    if (responseType.equalsIgnoreCase("Text")) navigationHelper.enableNextButton(true);
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    enterKeyButtonHelper.removeKeyHandler();
  }
}
