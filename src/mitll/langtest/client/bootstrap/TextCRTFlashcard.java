package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.EnterKeyButtonHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.NoPasteTextBox;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/29/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class TextCRTFlashcard extends DataCollectionFlashcard {
  private static final int HIDE_FEEDBACK = 2500;
  private static final double CORRECT_SCORE_THRESHOLD = 0.6;
  private EnterKeyButtonHelper enterKeyButtonHelper;
  private Image grayImage;
  private Image correctImage;
  private Image incorrectImage;

  public TextCRTFlashcard(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    super(e, service, controller, 40);
  }

  @Override
  protected void makeNavigationHelper(Exercise e, ExerciseController controller) {
    navigationHelper = new NavigationHelper(e, controller, false, false);
  }

//  private Panel checkContainer;
  /**
   *
   * @param exercise
   * @param service
   * @param controller
   * @return
   */
  @Override
  protected Widget getAnswerAndRecordButtonRow(final Exercise exercise, final LangTestDatabaseAsync service, ExerciseController controller) {
    final TextBox noPasteAnswer = new NoPasteTextBox();
    noPasteAnswer.setFocus(true);
    noPasteAnswer.addStyleName("topMargin");
    final Button check = new Button("Check Answer");
    noPasteAnswer.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        check.setEnabled(noPasteAnswer.getText().length() > 0);
      }
    });

    check.setType(ButtonType.PRIMARY);
    check.setEnabled(true);
    check.addStyleName("leftFiveMargin");
    check.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String guess = noPasteAnswer.getText();

        service.getScoreForAnswer(exercise, 0, guess, new AsyncCallback<Double>() {
          @Override
          public void onFailure(Throwable caught) {
            check.setEnabled(true);
          }
          @Override
          public void onSuccess(Double result) {
            check.setEnabled(true);
            result *= 2.5;
            result -= 1.25;
            result = Math.max(0,result);
            result = Math.min(1.0,result);

            showPronScoreFeedback(result);
        //    recoOutputContainer.setVisible(true);

            if (result > CORRECT_SCORE_THRESHOLD) {
              soundFeedback.playCorrect();
              showScoreIcon(true);
            }
            else {
              soundFeedback.playIncorrect();
              showScoreIcon(false);
            }
            Timer t = new Timer() {
              @Override
              public void run() {
                clearFeedback();
               // recoOutputContainer.clear();
               // recoOutputContainer.setVisible(false);

          /*
                recoOutputContainer.clear();

                Heading recoOutput = new Heading(4, "Answer");
                recoOutput.addStyleName("cardHiddenText");   // same color as background so text takes up space but is invisible
                DOM.setStyleAttribute(recoOutput.getElement(), "color", "#ebebec");
                recoOutputContainer.add(recoOutput);*/
            //    checkContainer.clear();
                recordButton.setResource(grayImage);
              }
            };
            t.schedule(HIDE_FEEDBACK);
          }
        });
      }
    });

    FluidRow row = new FluidRow();
    row.add(noPasteAnswer);
    //FluidRow row1 = new FluidRow();
    //row1.add(check);
    row.add(check);
    //checkContainer = new FlowPanel();
    // row.add(checkContainer);

   grayImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "gray_48x48.png"));
   correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
   incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));

    row.add(recordButton = getRecordButton());
 //   FluidContainer fc = new FluidContainer();
  //  fc.add(row);
   // fc.add(row1);

    enterKeyButtonHelper = new EnterKeyButtonHelper(false);
    enterKeyButtonHelper.addKeyHandler(check);

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        noPasteAnswer.setFocus(true);
      }
    });

    //  return getRecordButtonRow(fc);
      return getRecordButtonRow(row);
  }

  RecordButtonPanel.ImageAnchor recordButton;
  private void showScoreIcon(boolean correct) {
/*    RecordButtonPanel.ImageAnchor recordButton = getRecordButton(correct);
 *//*   recoOutputContainer.clear();
    recoOutputContainer.add(recordButton);
*//*
    checkContainer.clear();
    checkContainer.add(recordButton);*/
  //  recordButton.setVisible(true);
    recordButton.setResource(correct ? correctImage : incorrectImage);
  }

  private RecordButtonPanel.ImageAnchor getRecordButton() {
    RecordButtonPanel.ImageAnchor recordButton;
    recordButton = new RecordButtonPanel.ImageAnchor();
    recordButton.addStyleName("leftFiveMargin");
    recordButton.setResource(grayImage);
    recordButton.setHeight("112px");
 //   recordButton.setVisible(false);
    return recordButton;
  }

/*  private Panel getAutoCRTCheckAnswerWidget(final Exercise exercise, final LangTestDatabaseAsync service,
                                                      final int index, final TextBox answer) {
   Panel hp = new FlowPanel();
  //  hp.setSpacing(5);
    hp.add(answer);
    final Button check = new Button("Check Answer");
    check.setType(ButtonType.PRIMARY);
    check.setEnabled(false);
    hp.add(check);
    final Label resp = new Label();
    hp.add(resp);

    answer.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        resp.setText("");
        check.setEnabled(answer.getText().length() > 0);
      }
    });

    check.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        check.setEnabled(false);
        service.getScoreForAnswer(exercise, index, answer.getText(), new AsyncCallback<Double>() {
          @Override
          public void onFailure(Throwable caught) {
            check.setEnabled(true);
          }
          @Override
          public void onSuccess(Double result) {
            check.setEnabled(true);

            showAutoCRTScore(result, resp);
          }
        });
      }
    });
    return hp;
  }*/

/*
  private void addAnswer(final Exercise exercise, LangTestDatabaseAsync service, TextBox textBox, final boolean correct) {
    service.addTextAnswer(controller.getUser(), exercise, exercise.getEnglishSentence(), textBox.getText(), correct, new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        controller.getFeedback().showErrorMessage("Server error", "Couldn't post answers for exercise.");
      }

      public void onSuccess(Void result) {
        //nextAfterDelay(exercise, correct ? 2000 : 3000);
      }
    }
    );
  }
*/


/*  private void showAutoCRTScore(Double result, Label resp) {
    result *= 2.5;
    result -= 1.25;
    result = Math.max(0,result);
    result = Math.min(1.0,result);
    String percent = ((int) (result * 100)) + "%";
    if (result > 0.6) {
      resp.setText("Correct! Score was " + percent);
      resp.setStyleName("correct");
      soundFeedback.playCorrect();
    }
    else {
      resp.setText("Try again - score was " + percent);
      resp.setStyleName("incorrect");
      soundFeedback.playIncorrect();
    }
  }*/

/*
  private void nextAfterDelay(final Exercise exercise, final int delayMillis) {
    // Schedule the timer to run once in 1 seconds.
    Timer t = new Timer() {
      @Override
      public void run() {
        controller.loadNextExercise(exercise);
      }
    };
    t.schedule(delayMillis);
  }
*/
/*
  @Override
  protected Widget getQuestionContent(Exercise e) {
    FluidContainer container = new FluidContainer();
    FluidRow row = new FluidRow();
    container.add(row);
    String stimulus = e.getEnglishSentence();
    Widget hero = new Heading(1, stimulus);
    hero.addStyleName("cardText");

    row.add(new Column(12, hero));
    return container;
  }*/

  @Override
  public void showPronScoreFeedback(double score) {
    showScoreFeedback("Score ",score);
    navigationHelper.enableNextButton(true);
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    enterKeyButtonHelper.removeKeyHandler();
  }
}
