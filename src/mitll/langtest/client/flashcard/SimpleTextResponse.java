package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NoPasteTextBox;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/8/13
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SimpleTextResponse {
  public static final String CHECK = "Check";

  private int user;
  private AnswerPosted answerPosted;
  private Widget textResponseWidget;

  /**
   * @see mitll.langtest.client.recorder.ComboRecordPanel#doText
   * @param user
   */
  public SimpleTextResponse(int user) { this.user = user;  }

  protected abstract String getAnswerType();

  public interface AnswerPosted {
    void answerPosted();
    void answerTyped();
  }

  public void setAnswerPostedCallback(AnswerPosted answerPosted) { this.answerPosted = answerPosted; }

  /**
   * Has two rows -- the text input box and the score feedback
   * @see mitll.langtest.client.recorder.ComboRecordPanel#doText
   * @param toAddTo
   * @param exercise
   * @param service
   * @param controller
   * @param questionID
   * @param getFocus
   * @return
   */
  public Widget addWidgets(Panel toAddTo, Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                           int questionID,
                            boolean getFocus, String prompt) {
    textResponseWidget = getTextResponseWidget(exercise, service, controller,   questionID,
      getFocus, prompt);
    toAddTo.add(textResponseWidget);
    textResponseWidget.addStyleName("floatLeft");

    return textResponseWidget;
  }

  /**
   * @see mitll.langtest.client.recorder.ComboRecordPanel#doText
   * @return
   */
  public Widget getTextResponseWidget() { return textResponseWidget; }

  /**
   *
   * @param exercise
   * @param service
   * @param controller
   * @param questionID
   * @param getFocus
   * @return
   */
  private Widget getTextResponseWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                       int questionID,boolean getFocus, String prompt) {
    boolean allowPaste = controller.isDemoMode();
    final TextBox noPasteAnswer = getAnswerBox(allowPaste, getFocus);
    setupSubmitButton(exercise, service, noPasteAnswer, questionID);

    Panel row = new HorizontalPanel();
    Heading answer = new Heading(5, prompt);
    row.add(answer);
    row.getElement().setId("textResponseRow");
    row.add(noPasteAnswer);
    noPasteAnswer.addStyleName("leftFiveMargin");

    return row;
  }

  private String lastGuess = "";
  private void setupSubmitButton(final Exercise exercise, final LangTestDatabaseAsync service,
                                 final TextBox noPasteAnswer,
                                 final int questionID) {

    noPasteAnswer.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        String guess = noPasteAnswer.getText();
        if (guess.length() > 0 && !guess.equals(lastGuess)) {
          lastGuess = guess;
          service.addTextAnswer(user, exercise, questionID, guess, getAnswerType(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {}

            @Override
            public void onSuccess(Void result) {
              if (answerPosted != null) {
                answerPosted.answerPosted();
              }
            }
          });
        }
      }
    });
    noPasteAnswer.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        if (answerPosted != null) {
          answerPosted.answerTyped();
        }
      }
    });
  }

  private TextBox getAnswerBox(boolean allowPaste, boolean getFocus) {
    final TextBox noPasteAnswer = allowPaste ? new TextBox() : new NoPasteTextBox();

    if (getFocus) {
      Scheduler.get().scheduleDeferred(new Command() {
        public void execute() {
          //System.out.println("grabbing focus for " + check.getElement().getId());
          noPasteAnswer.setFocus(true);
        }
      });
    }

    noPasteAnswer.setDirectionEstimator(true);
    return noPasteAnswer;
  }
}
