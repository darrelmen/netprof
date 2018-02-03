package mitll.langtest.client.analysis;

import mitll.langtest.client.exercise.ExceptionSupport;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Date;
import java.util.logging.Logger;

public class PolyglotChart extends BasicTimeSeriesPlot {
  private static final int HEIGHT = 100;
  private final Logger logger = Logger.getLogger("PolyglotChart");

  protected Chart chart = null;

  public PolyglotChart(ExceptionSupport exceptionSupport) {
    super(exceptionSupport);
  }

  private Series series;

  public void addChart() {
    clear();

    chart = getChart("", "");

    series = getScatterSeries(chart, "Score");
    chart.addSeries(series);
    add(chart);
  }

  private Series getScatterSeries(Chart chart, String prefix) {
    return chart.createSeries()
        .setName(prefix)
        .setType(Series.Type.SCATTER);
  }

  private long startTime = System.currentTimeMillis();

  public void addPoint(long time, float score) {
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
  }

  public void setExtremes() {
    // setExtremes(startTime);
  }

  private void setExtremes(long time) {
    long start = time - 60;
    long end = time + (10 * 60 * 1000);
    chart.getXAxis().setExtremes(start, end, true, false);
    logger.info("xAxis from " + new Date(start) + " - " + new Date(end));
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
