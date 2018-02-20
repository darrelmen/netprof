package mitll.langtest.client.analysis;

import mitll.langtest.client.exercise.ExceptionSupport;
import mitll.langtest.shared.answer.AudioAnswer;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Collection;
import java.util.logging.Logger;

public class PolyglotChart extends BasicTimeSeriesPlot {
  private final Logger logger = Logger.getLogger("PolyglotChart");

  private static final String RECORDINGS = "Recordings";
  private static final String AVG_SCORE = "Avg. Score";

  private static final int HEIGHT = 100;

  protected Chart chart = null;

  public PolyglotChart(ExceptionSupport exceptionSupport) {
    super(exceptionSupport);
  }

  public void addChart(Collection<AudioAnswer> answers, long duration) {
    clear();

    chart = getChart("");

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
    chart.addSeries(getSplineSeries (chart, AVG_SCORE, data));
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

  private Chart getChart(String title) {
    final Chart chart = getHighchartChart(title, false);

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
        .setType(Axis.Type.DATE_TIME)
    ;

    //  int chartHeight = isShort() ? CHART_HEIGHT_SHORT : CHART_HEIGHT;
    chart.setHeight(HEIGHT + "px");
//    chart.setWidth(width + // 1378
//        "px");
  }
}
