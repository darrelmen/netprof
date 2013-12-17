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
import com.google.gwt.user.client.ui.HorizontalPanel;
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
  private static final String CHECK = "Check";

  private EnterKeyButtonHelper enterKeyButtonHelper;
  private ScoreFeedback textScoreFeedback;
  private SoundFeedback soundFeedback;
  private int user;
  private AnswerPosted answerPosted;
  private Widget textResponseWidget;

  /**
   * @see TextCRTFlashcard#makeNavigationHelper(mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseController)
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#doText
   * @param user to whom to file this answer under
   * @param soundFeedback so we can play a sound when the answer is correct or incorrect
   * @param answerPosted callback for when the user types in an answer and the post to the server has completed
   */
  public TextResponse(int user, SoundFeedback soundFeedback, AnswerPosted answerPosted) {
    this(user, soundFeedback);
    this.answerPosted = answerPosted;
  }

  public TextResponse(int user, SoundFeedback soundFeedback) {
    this.user = user;
    this.soundFeedback = soundFeedback;
  }

  public interface AnswerPosted {
    void answerPosted();
  }

  public void setAnswerPostedCallback(AnswerPosted answerPosted) { this.answerPosted = answerPosted; }

  public void setSoundFeedback(SoundFeedback soundFeedback) { this.soundFeedback = soundFeedback; }

  /**
   * Has two rows -- the text input box and the score feedback
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#doText
   * @param toAddTo
   * @param exercise
   * @param service
   * @param controller
   * @param centered
   * @param useWhite
   * @param addKeyBinding
   * @param questionID
   * @return
   */
  public Widget addWidgets(Panel toAddTo, Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                           boolean centered, boolean useWhite, boolean addKeyBinding, int questionID) {
    textScoreFeedback = new ScoreFeedback(useWhite);

    textResponseWidget = getTextResponseWidget(exercise, service, controller, getTextScoreFeedback(), centered, addKeyBinding, questionID);
    toAddTo.add(textResponseWidget);
    textResponseWidget.addStyleName("floatLeft");
    FluidRow scoreFeedbackRow = getTextScoreFeedback().getScoreFeedbackRow(FEEDBACK_HEIGHT, true);
    toAddTo.add(scoreFeedbackRow);

    return textResponseWidget;
  }

  /**
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#doText
   * @return
   */
  public Widget getTextResponseWidget() { return textResponseWidget; }

  /**
   * Three parts - text input, check button, and feedback icon
   *
   *
   * @param exercise
   * @param service
   * @param controller
   * @param scoreFeedback
   * @param centered
   * @param addEnterKeyBinding
   * @param questionID
   * @return
   * @see #addWidgets(com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, boolean, boolean, boolean, int)
   */
  private Widget getTextResponseWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                       ScoreFeedback scoreFeedback, boolean centered, boolean addEnterKeyBinding, int questionID) {
    boolean allowPaste = controller.isDemoMode();
    final Button check = new Button(CHECK);
    final TextBox noPasteAnswer = getAnswerBox(controller, allowPaste, check);
    String answerType = controller.getAudioType();
    setupSubmitButton(exercise, service, check, noPasteAnswer, scoreFeedback, answerType, addEnterKeyBinding, questionID);

    HorizontalPanel row = new HorizontalPanel();
    row.getElement().setId("textResponseRow");
    row.add(noPasteAnswer);
    row.add(check);

    // TODO : move this down to feedback row...!
    row.add(scoreFeedback.getFeedbackImage());

    return centered ? getRecordButtonRow(row) : row;
  }

  private FluidRow getRecordButtonRow(Widget recordButton) {
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
                                 final TextBox noPasteAnswer, final ScoreFeedback scoreFeedback, final String answerType,
                                 boolean addEnterKeyBinding, final int questionID) {
    check.setType(ButtonType.PRIMARY);
    check.setEnabled(false);
    check.addStyleName("leftFiveMargin");

    if (addEnterKeyBinding) {
      enterKeyButtonHelper = new EnterKeyButtonHelper(false);
      enterKeyButtonHelper.addKeyHandler(check);
      check.setTitle("Hit Enter to submit answer.");
    }

    check.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String guess = noPasteAnswer.getText();
        getScoreForGuess(guess, service, exercise, check, scoreFeedback, answerType, questionID);
      }
    });
  }

  private TextBox getAnswerBox(ExerciseController controller, boolean allowPaste, final Button check) {
    final TextBox noPasteAnswer = allowPaste ? new TextBox() : new NoPasteTextBox();
    if (controller.isRightAlignContent()) {
      noPasteAnswer.addStyleName("rightAlign");
    }

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
                                final ScoreFeedback scoreFeedback, String answerType, int questionID) {
    check.setEnabled(false);
    scoreFeedback.setWaiting();

    service.getScoreForAnswer(user, exercise, questionID, guess, answerType, new AsyncCallback<Double>() {
      @Override
      public void onFailure(Throwable caught) {
        check.setEnabled(true);
      }

      @Override
      public void onSuccess(Double result) {
        check.setEnabled(true);
        gotScoreForGuess(result);
      }
    });
  }

  /**
   * @see #getScoreForGuess(String, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.shared.Exercise, com.github.gwtbootstrap.client.ui.Button, ScoreFeedback, String, int)
   * @param result
   */
  private void gotScoreForGuess(Double result) {
    getTextScoreFeedback().showCRTFeedback(result, soundFeedback, "Score ", false);
   // getTextScoreFeedback().hideFeedback();
    if (answerPosted != null) {
      System.out.println("calling answer posted for " + result);
      answerPosted.answerPosted();
    }
  }

  public void onUnload() {
    if (enterKeyButtonHelper != null) {
      enterKeyButtonHelper.removeKeyHandler();
    }
  }

  public ScoreFeedback getTextScoreFeedback() { return textScoreFeedback;  }
}
