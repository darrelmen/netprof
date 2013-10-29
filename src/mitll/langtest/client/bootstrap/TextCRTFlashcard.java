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
  private static final int HIDE_FEEDBACK = 2500;
  private static final double CORRECT_SCORE_THRESHOLD = 0.5;
  private EnterKeyButtonHelper enterKeyButtonHelper;
  private Image grayImage;
  private Image correctImage;
  private Image incorrectImage;
  private UserManager userManager;

  public TextCRTFlashcard(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, UserManager userManager) {
    super(e, service, controller, 40);
    this.userManager = userManager;
  }

  @Override
  protected void makeNavigationHelper(Exercise e, ExerciseController controller) {
    navigationHelper = new NavigationHelper(e, controller, false, false);
  }

  /**
   *
   * @param exercise
   * @param service
   * @param controller
   * @return
   */
  @Override
  protected Widget getAnswerAndRecordButtonRow(final Exercise exercise, final LangTestDatabaseAsync service, ExerciseController controller) {
    boolean allowPaste = controller.isDemoMode();
    final TextBox noPasteAnswer = allowPaste ? new TextBox() : new NoPasteTextBox();
    noPasteAnswer.setFocus(true);
    noPasteAnswer.addStyleName("topMargin");
    final Button check = new Button("Check Answer");
    noPasteAnswer.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        check.setEnabled(noPasteAnswer.getText().length() > 0);
      }
    });

    check.setType(ButtonType.PRIMARY);
    check.setTitle("Hit Enter to submit answer.");
    check.setEnabled(true);
    check.addStyleName("leftFiveMargin");
    check.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String guess = noPasteAnswer.getText();

        service.getScoreForAnswer(userManager.getUser(), exercise, 1, guess, new AsyncCallback<Double>() {
          @Override
          public void onFailure(Throwable caught) {
            check.setEnabled(true);
          }
          @Override
          public void onSuccess(Double result) {
            check.setEnabled(true);
      /*      result *= 2.5;
            result -= 1.25;*/
            //result -= 0.3; // the floor
            //result *= 1.43;
            result = Math.max(0,result);
            result = Math.min(1.0,result);

            showPronScoreFeedback(result);
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
    row.add(check);

   grayImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "gray_48x48.png"));
   correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
   incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));

    row.add(recordButton = getRecordButton());

    enterKeyButtonHelper = new EnterKeyButtonHelper(false);
    enterKeyButtonHelper.addKeyHandler(check);

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        noPasteAnswer.setFocus(true);
      }
    });

      return getRecordButtonRow(row);
  }

  RecordButtonPanel.ImageAnchor recordButton;
  private void showScoreIcon(boolean correct) {
    recordButton.setResource(correct ? correctImage : incorrectImage);
  }

  private RecordButtonPanel.ImageAnchor getRecordButton() {
    RecordButtonPanel.ImageAnchor recordButton;
    recordButton = new RecordButtonPanel.ImageAnchor();
    recordButton.addStyleName("leftFiveMargin");
    recordButton.setResource(grayImage);
    recordButton.setHeight("112px");
    return recordButton;
  }

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
