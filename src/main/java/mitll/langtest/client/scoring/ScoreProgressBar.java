package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.gauge.SimpleColumnChart;

import java.util.logging.Logger;

public class ScoreProgressBar {
  //private Logger logger = Logger.getLogger("ScoreProgressBar");

  private static final String AUDIO_CUT_OFF = "Audio cut off.";
  protected ProgressBar progressBar;

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
   * @param showNow
   * @see SimpleRecordAudioPanel#useScoredResult
   */
  public DivWidget showScore(double score, boolean isFullMatch, boolean showNow) {
    double percent = isFullMatch ? score / 100d : 0.41D;
    progressBar.setVisible(true);
    if (isFullMatch) {
      progressBar.getElement().getStyle().clearProperty("width");
    } else {
      progressBar.setWidth("250px");
    }

    double round = isFullMatch ? Math.round(score) : 100.0D;
    String text = isFullMatch ? "" + round : AUDIO_CUT_OFF;

    progressBar.setText(text);
    setColor(progressBar, percent, round, showNow);
    return progressBar;
  }

  public void setColor(ProgressBar progressBar, double percent, double round, boolean showNow) {
     String color = SimpleColumnChart.getColor(Double.valueOf(percent).floatValue());

//    if (showNow) {
//      setPercentLater(progressBar, percent, round, color);
//    } else {
   //    logger.info("showScore : color " + color + " for %" + percent + " and " + round);
      Scheduler.get().scheduleDeferred((Command) () -> setPercentLater(progressBar, percent, round, color));
//    }
  }

  private void setPercentLater(ProgressBar progressBar, double percent, double round, String color) {
    Style style = getStyleWidget(progressBar);
     style.setBackgroundImage("linear-gradient(to bottom," +
        color +
        "," +
        color +
        ")");

    setPercent(progressBar, percent, round, style);
  }

  private Style getStyleWidget(ProgressBar progressBar) {
    return progressBar.getWidget(0).getElement().getStyle();
  }

  private void setPercent(ProgressBar progressBar, double percent, double round, Style style) {
    if (percent > 0.4) style.setColor("black");
    progressBar.setPercent(round);
  }
}
