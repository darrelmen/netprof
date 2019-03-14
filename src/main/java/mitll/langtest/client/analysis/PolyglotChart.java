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

package mitll.langtest.client.analysis;

import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.exercise.ExceptionSupport;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

public class PolyglotChart<T extends CommonShell> extends BasicTimeSeriesPlot<T> {
  public static final String CLICK_TO_RE_RECORD = "Click to re-record.";
  private final Logger logger = Logger.getLogger("PolyglotChart");

  private static final String RECORDINGS = "Recordings";
  private static final String AVG_SCORE = "Avg. Score";
  private static final int HEIGHT = 100;

  protected Chart chart = null;
  private final ListInterface<?, ?> listInterface;

  /**
   * @param exceptionSupport
   * @see mitll.langtest.client.flashcard.PolyglotPracticePanel#getChart
   */
  public PolyglotChart(ExceptionSupport exceptionSupport,
                       MessageHelper messageHelper,
                       ListInterface<?, ?> listInterface) {
    super(exceptionSupport, messageHelper);
    this.listInterface = listInterface;
  }

  /**
   * @param answers
   * @param duration
   * @see mitll.langtest.client.flashcard.PolyglotPracticePanel#getChart
   */
  public void addChart(Collection<AudioAnswer> answers, long duration) {
    clear();

    chart = getChart();

    long first = setData(answers);
    if (first > 0) {
      setExtremes(first, duration);
    }
    add(chart);
  }

  public long setData(Collection<AudioAnswer> answers) {
    int size = answers.size();

   // logger.info("addChart : plotting " + size + " items.");

    Number[][] data = new Number[size][2];
    Number[][] data2 = new Number[size][2];
    int i = 0;
    long first = -1;
    float total = 0;
    float count = 0;
    for (AudioAnswer ts : answers) {
      long timestamp = ts.getTimestamp();
   //   logger.info("addChart : timestamp " + timestamp + " : " + new Date(timestamp));

      if (first == -1) first = timestamp;

      double score = ts.getScore();
      if (score < 0) score = 0;

      data2[i][0] = timestamp;
      data2[i][1] = score * 100;

      data[i][0] = timestamp;
      {
        total += score;
        count++;
        float moving = total / count;
        data[i][1] = moving * 100;
      }

      i++;
    }

    chart.removeAllSeries();
    chart.addSeries(getScatterSeries(chart, RECORDINGS, data2));
    chart.addSeries(getSplineSeries(chart, AVG_SCORE, data));

    return first;
  }

  private void setExtremes(long time, long duration) {
    long start = time - 1;
    long end = time + duration + (60);//(10 * 60 * 1000);
    chart.getXAxis().setExtremes(start, end, true, false);

 //   logger.info("setExtremes xAxis from " + new Date(start) + " - " + new Date(end) + " duration  " + duration);
  }

  private Chart getChart() {
    final Chart chart = getHighchartChart("", false);

    Highcharts.setOptions(
        new Highcharts.Options().setGlobal(
            new Global()
                .setUseUTC(false)
        ));

    configureChart(chart, "");
    return chart;
  }

  private void configureChart(Chart chart, String title) {
    configureYAxis(chart, title);

    chart.getXAxis()
        .setStartOnTick(true)
        .setEndOnTick(true)
        .setType(Axis.Type.DATE_TIME);

    chart.setHeight(HEIGHT + "px");
  }

  private Map<Long, AudioAnswer> timeToAnswer;

  public void setTimeToAnswer(Map<Long, AudioAnswer> timeToAnswer) {
    this.timeToAnswer = timeToAnswer;
  }

  @Override
  boolean shouldShowExercise(String seriesName) {
    return true;
  }

  protected boolean gotClickAt(long nearestXAsLong) {
    AudioAnswer audioAnswer = timeToAnswer.get(nearestXAsLong);
    if (audioAnswer != null) {
      gotClickOnExercise(audioAnswer.getExid(), nearestXAsLong);
    } else {
      //logger.info("getSeriesClickEventHandler no point at " + nearestXAsLong);
    }
    return true;
  }


  @Override
  protected T getCommonShellAtTime(Integer exerciseID, long xAsLong) {
    if (exerciseID == null) {
      AudioAnswer audioAnswer = timeToAnswer.get(xAsLong);
      if (audioAnswer != null) exerciseID = audioAnswer.getExid();
    }
    return exerciseID == null ? null : getShell(exerciseID);
  }

  protected void gotClickOnExercise(int exid, long nearestXAsLong) {
    listInterface.loadByID(exid);
  }

  protected int getHideDelay() {
    return 2000;
  }

  protected String getTooltip(ToolTipData toolTipData, Integer exid, T commonShell) {
    return getFLTooltip(commonShell.getFLToShow()) + getTooltipHint();
  }

  /**
   * @return
   */
  @Override
  @NotNull
  String getTooltipHint() {
    return "<br/><b>" + CLICK_TO_RE_RECORD + "</b>";
  }
}
