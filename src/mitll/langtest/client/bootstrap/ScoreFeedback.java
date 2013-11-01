package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.sound.SoundFeedback;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 6:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScoreFeedback {
  private static final String PRONUNCIATION_SCORE = "Pronunciation score ";
  private static final int HIDE_FEEDBACK = 2500;
  private static final double CORRECT_SCORE_THRESHOLD = 0.5;

  private Image grayImage;
  private Image correctImage;
  private Image incorrectImage;
  private Image waitingForResponseImage;
  private RecordButtonPanel.ImageAnchor feedbackImage;
  private Column scoreFeedbackColumn;
  private ProgressBar scoreFeedback = new ProgressBar();

  private SimplePanel feedbackDummyPanel;

  public ScoreFeedback() {
    grayImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "gray_48x48.png"));
    correctImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark48.png"));
    incorrectImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx48.png"));
    waitingForResponseImage = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress48.gif"));
  }
  /**
   * Holds the pron score feedback.
   * Initially made with a placeholder.
   *
   * @return
   */
  public FluidRow getScoreFeedbackRow(int height) {
    FluidRow feedbackRow = new FluidRow();
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    scoreFeedbackColumn = new Column(6, 3, feedbackDummyPanel);
    feedbackRow.add(scoreFeedbackColumn);
    feedbackRow.getElement().setId("feedbackRow");
    return feedbackRow;
  }

  public void setWaiting() {
    feedbackImage.setResource(waitingForResponseImage);
  }

  public void showCRTFeedback(Double result, SoundFeedback soundFeedback, String pronunciationScore) {
    result = Math.max(0, result);
    result = Math.min(1.0, result);
    if (result > 0.9) result = 1.0; //let's round up when we're almost totally correct 97%->100%
    showScoreFeedback(pronunciationScore, result);
    if (result > CORRECT_SCORE_THRESHOLD) {
      soundFeedback.playCorrect();
      showScoreIcon(true);
    } else {
      soundFeedback.playIncorrect();
      showScoreIcon(false);
    }
    Timer t = new Timer() {
      @Override
      public void run() {
        clearFeedback();
        feedbackImage.setResource(grayImage);
      }
    };
    t.schedule(HIDE_FEEDBACK);
  }
/*
  public void showPronScoreFeedback(double score) {
    String pronunciationScore = PRONUNCIATION_SCORE;
    showScoreFeedback(pronunciationScore, score);
  }*/

  public void showScoreFeedback(String pronunciationScore, double score) {
    if (score < 0) score = 0;
    double percent = 100 * score;

    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(scoreFeedback);

    int percent1 = (int) percent;
    scoreFeedback.setPercent(percent1 < 40 ? 40 : percent1);   // just so the words will show up

    scoreFeedback.setText(pronunciationScore + (int) percent + "%");
    scoreFeedback.setVisible(true);
    scoreFeedback.setColor(
      score > 0.8 ? ProgressBarBase.Color.SUCCESS :
        score > 0.6 ? ProgressBarBase.Color.DEFAULT :
          score > 0.4 ? ProgressBarBase.Color.WARNING : ProgressBarBase.Color.DANGER);
  }

  public void clearFeedback() {
    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(feedbackDummyPanel);
  }

  private void showScoreIcon(boolean correct) {
    feedbackImage.setResource(correct ? correctImage : incorrectImage);
  }

  public RecordButtonPanel.ImageAnchor getFeedbackImage() {
    RecordButtonPanel.ImageAnchor image;
    image = new RecordButtonPanel.ImageAnchor();
    image.addStyleName("leftFiveMargin");
    image.setResource(grayImage);
    image.setHeight("112px");
    return image;
  }
}
