package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.sound.SoundFeedback;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 6:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScoreFeedback {
  private static final int HIDE_FEEDBACK = 2500;
  private static final double CORRECT_SCORE_THRESHOLD = 0.5;

  private IconAnchor feedbackImage;
  private Panel scoreFeedbackColumn;
  private ProgressBar scoreFeedback = new ProgressBar();

  private SimplePanel feedbackDummyPanel;
  private boolean useWhite;

  public ScoreFeedback(boolean useWhite) {  this.useWhite = useWhite;  }
  boolean useShortWidth;
  /**
   * Holds the pron score feedback.
   * Initially made with a placeholder.
   *
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#addRecordingAndFeedbackWidgets(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @return
   */
  public FluidRow getScoreFeedbackRow(int height, boolean useShortWidth) {
    this.useShortWidth = useShortWidth;


    System.out.println("getScoreFeedbackRow mode " + useShortWidth);

    FluidRow feedbackRow = new FluidRow();
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    scoreFeedbackColumn = new Column(6, 3, feedbackDummyPanel);

    //scoreFeedbackColumn.setWidth(Math.min(Window.getClientWidth() *0.8,Window.getClientWidth() * 0.5) + "px");

    feedbackRow.add(scoreFeedbackColumn);
    feedbackRow.getElement().setId("ScoreFeedbackfeedbackRow");
    return feedbackRow;
  }

  public FluidRow getScoreFeedbackRow2(Widget left,int height) {
    FluidRow feedbackRow = new FluidRow();
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    feedbackRow.add(new Column(3));
    feedbackRow.add(new Column(1,left));

    scoreFeedbackColumn = new Column(5, feedbackDummyPanel);
    feedbackRow.add(scoreFeedbackColumn);
    feedbackRow.getElement().setId("feedbackRow2");
    return feedbackRow;
  }

  /**
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#getFeedbackContainer
   * @param left
   * @param height
   * @return
   */
  public Panel getSimpleRow(Widget left, int height) {
    FlowPanel feedbackRow = new FlowPanel();
    feedbackRow.add(left);
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    feedbackDummyPanel.addStyleName("floatLeft");

    scoreFeedbackColumn = new SimplePanel(feedbackDummyPanel);
    double width = useShortWidth ? 300 : Math.min(300, Window.getClientWidth() * 0.5);
    scoreFeedbackColumn.setWidth((int)width + "px");
    scoreFeedbackColumn.addStyleName("floatRight");

    feedbackRow.add(scoreFeedbackColumn);
    feedbackRow.getElement().setId("feedbackRowSimple");
    return feedbackRow;
  }

  public void setWaiting() {
    feedbackImage.setBaseIcon(MyCustomIconType.waiting);
  }

  /**
   * @see mitll.langtest.client.flashcard.TextResponse#getScoreForGuess
   * @see mitll.langtest.client.recorder.AutoCRTRecordPanel#receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
   * @param result
   * @param soundFeedback
   * @param pronunciationScore
   * @param centerVertically
   */
  public void showCRTFeedback(Double result, SoundFeedback soundFeedback, String pronunciationScore, boolean centerVertically) {
    result = Math.max(0, result);
    result = Math.min(1.0, result);
    if (result > 0.9) result = 1.0; //let's round up when we're almost totally correct 97%->100%
    showScoreFeedback(pronunciationScore, result, centerVertically, useShortWidth);
    if (result > CORRECT_SCORE_THRESHOLD) {
      soundFeedback.playCorrect();
      showScoreIcon(true);
    } else {
      soundFeedback.playIncorrect();
      showScoreIcon(false);
    }
  }

  /**
   * @see TextResponse#gotScoreForGuess
   */
  public void hideFeedback() {
    Timer t = new Timer() {
      @Override
      public void run() {
        clearFeedback();
        feedbackImage.setBaseIcon(useWhite ? MyCustomIconType.white : MyCustomIconType.gray);
      }
    };
    t.schedule(HIDE_FEEDBACK);
  }

  /**
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#showPronScoreFeedback(double, String)
   * @see #showCRTFeedback(Double, mitll.langtest.client.sound.SoundFeedback, String, boolean)
   * @param pronunciationScore
   * @param score
   * @param centerVertically
   * @param useShortWidth
   */
  public void showScoreFeedback(String pronunciationScore, double score, boolean centerVertically, boolean useShortWidth) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(getScoreFeedback());
    double val = useShortWidth ? 300 : Math.min(Window.getClientWidth() * 0.8, Window.getClientWidth() * 0.5);
    getScoreFeedback().setWidth((int)val + "px");

    int percent1 = (int) percent;
    getScoreFeedback().setPercent(percent1 < 40 ? 40 : percent1);   // just so the words will show up

    getScoreFeedback().setText(pronunciationScore + (int) percent + "%");
    getScoreFeedback().setVisible(true);
    getScoreFeedback().setColor(
      score > 0.8 ? ProgressBarBase.Color.SUCCESS :
        score > 0.6 ? ProgressBarBase.Color.DEFAULT :
          score > 0.4 ? ProgressBarBase.Color.WARNING : ProgressBarBase.Color.DANGER);

    if (centerVertically) {
      DOM.setStyleAttribute(scoreFeedback.getElement(), "marginTop", "18px");
      DOM.setStyleAttribute(scoreFeedback.getElement(), "marginBottom", "10px");
    }
    DOM.setStyleAttribute(scoreFeedback.getElement(), "marginLeft", "10px");
  }

  public void clearFeedback() {
    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(feedbackDummyPanel);
  }

  private void showScoreIcon(boolean correct) {
    feedbackImage.setBaseIcon(correct ? MyCustomIconType.correct : MyCustomIconType.incorrect);
  }

  /**
   * @see
   * @return
   */
  public IconAnchor getFeedbackImage() {
    IconAnchor image;
    image = new IconAnchor();
    image.addStyleName("leftFiveMargin");
    image.setBaseIcon(useWhite ? MyCustomIconType.white : MyCustomIconType.gray);

    image.setHeight("48px");
    feedbackImage = image;
    return image;
  }

  public ProgressBar getScoreFeedback() {
    return scoreFeedback;
  }
}
