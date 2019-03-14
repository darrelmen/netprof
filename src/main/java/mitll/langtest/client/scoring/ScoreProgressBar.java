/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
