package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.EnterKeyButtonHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NoPasteTextBox;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/8/13
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class TextResponse {
  private static final int FEEDBACK_HEIGHT = 40;

  private EnterKeyButtonHelper enterKeyButtonHelper;

  private ScoreFeedback textScoreFeedback;
  private SoundFeedback soundFeedback;
  private int user;

  AnswerPosted answerPosted;

  public TextResponse(int user, SoundFeedback soundFeedback, AnswerPosted answerPosted) {
    this.user = user;

    this.soundFeedback = soundFeedback;

    this.answerPosted = answerPosted;
  }

  public interface AnswerPosted {
    void answerPosted();
  }

  public void setSoundFeedback(SoundFeedback soundFeedback) { this.soundFeedback = soundFeedback; }

  public void addWidgets(Panel toAddTo, Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                         boolean centered, boolean useWhite) {
    textScoreFeedback = new ScoreFeedback(useWhite);

    toAddTo.add(getTextResponseWidget(exercise, service, controller, getTextScoreFeedback(), centered));
    toAddTo.add(getTextScoreFeedback().getScoreFeedbackRow(FEEDBACK_HEIGHT));
  }

  private Widget getTextResponseWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                       ScoreFeedback scoreFeedback, boolean centered) {
    boolean allowPaste = controller.isDemoMode();
    final Button check = new Button("Check Answer");
    final TextBox noPasteAnswer = getAnswerBox(controller, allowPaste, check);
    String answerType = controller.getAudioType();
    setupSubmitButton(exercise, service, check, noPasteAnswer, scoreFeedback, answerType);

    FluidRow row = new FluidRow();
    row.add(noPasteAnswer);
    row.add(check);

    row.add(scoreFeedback.getFeedbackImage());
    return centered ? getRecordButtonRow(row) : row;
  }

  protected FluidRow getRecordButtonRow(Widget recordButton) {
    FluidRow recordButtonRow = new FluidRow();
    Paragraph recordButtonContainer = new Paragraph();
    recordButtonContainer.addStyleName("alignCenter");
    recordButtonContainer.add(recordButton);
    recordButton.addStyleName("alignCenter");
    recordButtonRow.add(new Column(12, recordButtonContainer));
    recordButtonRow.getElement().setId("recordButtonRow");
    return recordButtonRow;
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
                                final ScoreFeedback scoreFeedback, String answerType) {
    check.setEnabled(false);
    scoreFeedback.setWaiting();

    service.getScoreForAnswer(user, exercise, 1, guess, answerType, new AsyncCallback<Double>() {
      @Override
      public void onFailure(Throwable caught) {
        check.setEnabled(true);
      }

      @Override
      public void onSuccess(Double result) {
        check.setEnabled(true);
        gotScoreForGuess(result, check);
      }
    });
  }

  /**
   * @see #getScoreForGuess(String, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.shared.Exercise, com.github.gwtbootstrap.client.ui.Button, ScoreFeedback, String)
   * @param result
   * @param check
   * @paramx responseType
   * @paramx navigationHelper
   */
  private void gotScoreForGuess(Double result, Button check) {
    getTextScoreFeedback().showCRTFeedback(result, soundFeedback, "Score ", false);
    getTextScoreFeedback().hideFeedback();
    if (answerPosted != null) answerPosted.answerPosted();
  }

  protected void onUnload() {
    enterKeyButtonHelper.removeKeyHandler();
  }

  public ScoreFeedback getTextScoreFeedback() { return textScoreFeedback;  }
}
