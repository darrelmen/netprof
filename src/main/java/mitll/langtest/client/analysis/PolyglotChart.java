package mitll.langtest.client.analysis;

import mitll.langtest.client.exercise.ExceptionSupport;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Collection;
import java.util.Map;

public class PolyglotChart<T extends CommonShell>  extends BasicTimeSeriesPlot<T> {
  //private final Logger logger = Logger.getLogger("PolyglotChart");
  private static final String RECORDINGS = "Recordings";
  private static final String AVG_SCORE = "Avg. Score";
  private static final int HEIGHT = 100;

  protected Chart chart = null;
  private final ListInterface<?,?> listInterface;

  /**
   * @param exceptionSupport
   * @see mitll.langtest.client.flashcard.PolyglotPracticePanel#getChart
   */
  public PolyglotChart(ExceptionSupport exceptionSupport, ListInterface<?,?> listInterface) {
    super(exceptionSupport);
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

    int size = answers.size();
//    logger.info("addChart : plotting " + size + " items.");
    Number[][] data = new Number[size][2];
    Number[][] data2 = new Number[size][2];
    int i = 0;
    long first = -1;
    float total = 0;
    float count = 0;
    for (AudioAnswer ts : answers) {
      long timestamp = ts.getTimestamp();
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

    chart.addSeries(getScatterSeries(chart, RECORDINGS, data2));
    chart.addSeries(getSplineSeries(chart, AVG_SCORE, data));
    if (first > 0)
      setExtremes(first, duration);
    add(chart);
  }

  private void setExtremes(long time, long duration) {
    long start = time - 1;
    long end = time + duration + (60);//(10 * 60 * 1000);
    chart.getXAxis().setExtremes(start, end, true, false);
    //  logger.info("xAxis from " + new Date(start) + " - " + new Date(end) + " duration  " + duration);
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

  boolean showDate() {
    return false;
  }

  protected boolean gotClickAt(long nearestXAsLong) {
    AudioAnswer audioAnswer = timeToAnswer.get(nearestXAsLong);
    if (audioAnswer != null) {
      gotClickOnExercise(audioAnswer.getExid(), nearestXAsLong);
    } else {
      // logger.info("getSeriesClickEventHandler no point at " + nearestXAsLong);
    }
    return true;
  }


  @Override
  protected T getCommonShellAtTime(Integer exerciseID, long xAsLong) {
    if (exerciseID == null) {
      AudioAnswer audioAnswer = timeToAnswer.get(xAsLong);
      if (audioAnswer != null) exerciseID = audioAnswer.getExid();
    }
    return exerciseID == null ? null : getIdToEx().get(exerciseID);
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
    return "<br/><b>Click to re-record.</b>";
  }
}
