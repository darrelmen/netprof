package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.gauge.SimpleColumnChart;
import org.jetbrains.annotations.NotNull;

public class ScoreProgressBar {
  //private Logger logger = Logger.getLogger("ScoreProgressBar");

  private static final String AUDIO_CUT_OFF = "Audio cut off.";
  ProgressBar progressBar;

  /**
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#getProgressBar
   */
  public ScoreProgressBar() {
    progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
  }

  public ScoreProgressBar(boolean nope) {
  }

  /**
   * Color the feedback with same color scheme as words and phones.
   * Not the 4 color styles that come with the progress bar.
   *
   * @param score
   * @param isFullMatch
   * @see SimpleRecordAudioPanel#useScoredResult
   */
  public DivWidget showScore(double score, boolean isFullMatch) {
    double percent = isFullMatch ? score / 100D : 0.41D;
    progressBar.setVisible(true);
    if (isFullMatch) {
      progressBar.getElement().getStyle().clearProperty("width");
    } else {
      progressBar.setWidth("250px");
    }

    double round = isFullMatch ? Math.round(score) : 100.0D;
    String text = isFullMatch ? "" + round : AUDIO_CUT_OFF;

    progressBar.setText(text);
    setColor(progressBar, percent, round);
    return progressBar;
  }

  /**
   * @param progressBar
   * @param percent
   * @param round
   */
  public void setColor(ProgressBar progressBar, double percent, double round) {
   setColor(progressBar, percent);

//    if (showNow) {
//      setPercentLater(progressBar, percent, round, color);
//    } else {
    //    logger.info("showScore : color " + color + " for %" + percent + " and " + round);
    Scheduler.get().scheduleDeferred((Command) () -> setPercentLater(progressBar, percent, round));
//    }
  }

  public String setColor(ProgressBar progressBar, double percent) {
    String color = SimpleColumnChart.getColor(Double.valueOf(percent).floatValue());
    setColorGradient(progressBar, color);
    return color;
  }

  private void setPercentLater(ProgressBar progressBar, double percent, double round) {
    setPercent(progressBar, percent, round, getStyleWidget(progressBar));
  }



  @NotNull
  private void setColorGradient(ProgressBar progressBar, String color) {
    getStyleWidget(progressBar).setBackgroundImage("linear-gradient(to bottom," +
        color +
        "," +
        color +
        ")");
  }

  private Style getStyleWidget(ProgressBar progressBar) {
    return progressBar.getWidget(0).getElement().getStyle();
  }

  private void setPercent(ProgressBar progressBar, double percent, double round, Style style) {
    if (percent > 0.4) style.setColor("black");
   // progressBar.setPercent(round);
    cheesySetPercent(progressBar, round);
  }

  @NotNull
  private Widget cheesySetPercent(ComplexPanel practicedProgress, double percent1) {
    Widget theBar = practicedProgress.getWidget(0);
    theBar.getElement().getStyle().setWidth(Double.valueOf(percent1).intValue(), Style.Unit.PCT);
    return theBar;
  }

}
