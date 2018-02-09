package mitll.langtest.client.analysis;

import mitll.langtest.client.exercise.ExceptionSupport;
import mitll.langtest.shared.analysis.BestScore;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.answer.AudioAnswer;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class PolyglotChart extends BasicTimeSeriesPlot {
  private static final int HEIGHT = 100;
  private final Logger logger = Logger.getLogger("PolyglotChart");

  protected Chart chart = null;

  public PolyglotChart(ExceptionSupport exceptionSupport) {
    super(exceptionSupport);
  }

  private Series series;

  public void addChart(Collection<AudioAnswer> answers, long duration) {
    clear();

    chart = getChart("", "");

    Number[][] data = new Number[answers.size()][2];
    int i = 0;
    long first = -1;
    float total = 0;
    float count = 0;
    for (AudioAnswer ts : answers) {
      data[i][0] = ts.getTimestamp();
      if (first == -1) first = ts.getTimestamp();

      total += ts.getScore();
      count++;
      float moving = total / count;

      data[i++][1] = moving * 100;
    }

    //logger.info("first is " + first + " " + new Date(first));
    series = getSplineSeries(chart, "Score", data);
    chart.addSeries(series);
    if (first > 0)
      setExtremes(first, duration);
    add(chart);
  }

//  private Series getScatterSeries(Chart chart, String prefix) {
//    return chart.createSeries()
//        .setName(prefix)
//        .setType(Series.Type.SCATTER);
//  }

 // private long startTime = System.currentTimeMillis();

  /*public void addPoint(long time, float score) {
    boolean isEmpty = series.getPoints().length == 0;
    if (isEmpty) {
      // setExtremes(time);
      startTime = time;
    }

    series.addPoint(time, score * 100);//,false,false,false);

    // setExtremes();

    Number min = chart.getXAxis().getExtremes().getMin();
    Number max = chart.getXAxis().getExtremes().getMax();
    logger.info("add point at " + new Date(time) + " : " + score +
        " now " + series.getPoints().length + " : " + new Date(min.longValue()) + " - " + new Date(max.longValue()));
  }*/

//  public void setExtremes() {
//    // setExtremes(startTime);
//  }

  private void setExtremes(long time, long duration) {
    long start = time - 60;
    long end = time + duration + (60);//(10 * 60 * 1000);
    chart.getXAxis().setExtremes(start, end, true, false);
  //  logger.info("xAxis from " + new Date(start) + " - " + new Date(end) + " duration  " + duration);
  }

  private Chart getChart(String title, String subtitle) {
    final Chart chart = getHighchartChart(title, false);

    Highcharts.setOptions(
        new Highcharts.Options().setGlobal(
            new Global()
                .setUseUTC(false)
        ));


//    List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();

//    addSeries(rawBestScores,
//        userPerformance.getiPadTimeAndScores(),
//        userPerformance.getLearnTimeAndScores(),
//        userPerformance.getAvpTimeAndScores(),
//        chart,
//        seriesName,
//        isShortPeriod(rawBestScores));
//
//    addErrorBars(userPerformance, chart);
    configureChart(chart, subtitle);
    return chart;
  }

  private void configureChart(Chart chart, String title) {
    configureYAxis(chart, title);

    chart.getXAxis()
        .setStartOnTick(true)
        .setEndOnTick(true)
        .setType(Axis.Type.DATE_TIME)
    ;

    //  int chartHeight = isShort() ? CHART_HEIGHT_SHORT : CHART_HEIGHT;
    chart.setHeight(HEIGHT + "px");
//    chart.setWidth(width + // 1378
//        "px");
  }

}
