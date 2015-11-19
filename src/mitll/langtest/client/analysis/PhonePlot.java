package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.IsWidget;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.TimeAndScore;
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/19/15.
 */
public class PhonePlot extends DivWidget implements IsWidget {
  private final Logger logger = Logger.getLogger("PhonePlot");

  public static final String AVERAGE = "Average";
  private static final int NARROW_WIDTH = 330;
  private static final int CHART_HEIGHT = 340;
  private static final String PRONUNCIATION_SCORE = "Pronunciation Score";

  private final Map<Long, String> timeToId = new TreeMap<>();
  private final Map<Long, PhoneSession> timeToSession = new TreeMap<>();
  private final Map<String, CommonShell> idToEx = new TreeMap<>();

  public PhonePlot() {
    getElement().setId("PhonePlot");
  }

  /**
   * @param rawBestScores
   * @param userChosenID
   * @param isNarrow
   * @see PhoneContainer#gotClickOnItem(PhoneAndStats)
   */
/*
  public void showData(List<TimeAndScore> rawBestScores, String userChosenID, boolean isNarrow) {
    clear();
    int rawTotal = rawBestScores.size();
    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
      Chart chart = getChart("<b>" + userChosenID + "</b>" +
              " pronunciation score"
          ,
          "Score and average (" + rawTotal + " items " +
              //": avg score " + (int) v +
              " %)", "Cumulative Average", rawBestScores, isNarrow);
      add(chart);
    }
    setRawBestScores(rawBestScores);
  }
*/
  public void showData2(List<PhoneSession> rawBestScores, String userChosenID, boolean isNarrow) {
    clear();
 //   int rawTotal = rawBestScores.size();//userPerformance.getRawTotal();
    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
      Chart chart = getChart2("<b>" + userChosenID + "</b>" + " pronunciation score",
          "Average score and range" //+
          //    " for " + rawTotal + " sessions"
          , "Range", rawBestScores, isNarrow);
      add(chart);
    }
    setRawBestScores2(rawBestScores);
  }


  /**
   * @param title
   * @param subtitle
   * @param seriesName
   * @return
   * @see #showData(List, String, boolean)
   */
/*  private Chart getChart(String title, String subtitle, String seriesName, List<TimeAndScore> rawBestScores, boolean narrow) {
    Chart chart = new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        .setType(Series.Type.SCATTER)
        .setChartTitleText(title)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false)
        .setScatterPlotOptions(new ScatterPlotOptions()
            .setMarker(new Marker()
                .setRadius(5)
                .setHoverState(new Marker()
                    .setEnabled(true)
                    .setLineColor(new Color(100, 100, 100))
                )
            )
            .setHoverStateMarker(new Marker()
                .setEnabled(false)
            ))
        .setToolTip(getToolTip());
    if (narrow) chart.setWidth(NARROW_WIDTH);

    addSeries(rawBestScores, chart, seriesName);

    configureChart(chart, subtitle);
    return chart;
  }*/

  private ToolTip getToolTip() {
    return new ToolTip()
        .setFormatter(new ToolTipFormatter() {
          public String format(ToolTipData toolTipData) {
            String s = timeToId.get(toolTipData.getXAsLong());
            CommonShell commonShell = getIdToEx().get(s);
            String foreignLanguage = commonShell == null ? "" : commonShell.getForeignLanguage();
            String english = commonShell == null ? "" : commonShell.getEnglish();

            String seriesName1 = toolTipData.getSeriesName();
            boolean showEx = (seriesName1.contains(PRONUNCIATION_SCORE));
            return "<b>" + seriesName1 + "</b><br/>" +
                DateTimeFormat.getFormat("E MMM d yy h:mm a").format(
                    new Date(toolTipData.getXAsLong())
                ) +
                (showEx ?
                    "<br/>Ex " + s +
                        "<br/>" + foreignLanguage +
                        "<br/>" + english
                    : "")
                +
                "<br/>Score = " + toolTipData.getYAsLong() + "%";
          }
        });
  }

  private DateTimeFormat format = DateTimeFormat.getFormat("E MMM d yy h:mm a");
  private DateTimeFormat noYearFormat = DateTimeFormat.getFormat("E MMM d h:mm a");
  DateTimeFormat shortFormat = DateTimeFormat.getFormat("MMM d, yy");
  Date now = new Date();

  private ToolTip getToolTip2() {
    return new ToolTip()
        .setFormatter(new ToolTipFormatter() {
          public String format(ToolTipData toolTipData) {
            try {
              PhoneSession session = timeToSession.get(toolTipData.getXAsLong());
              String seriesName1 = toolTipData.getSeriesName();

              Date date = new Date(toolTipData.getXAsLong());

              String nowFormat = shortFormat.format(now);
              String shortForDate = shortFormat.format(date);

              DateTimeFormat toUse = (nowFormat.substring(nowFormat.length() - 2).equals(shortForDate.substring(shortForDate.length() - 2))) ? noYearFormat : format;
              String dateToShow = toUse.format(date);


              if (seriesName1.equals(AVERAGE)) {
                return "<b>" + seriesName1 + "</b>" +
                    "<br/>" +
                    dateToShow
                    +
                    "<br/>Mean = " + toolTipData.getYAsLong() + "%";
              } else {
                Point point = toolTipData.getPoint();
                String s = /*session == null ? "" : */(" n = " + session.getCount());
                String range = /*point == null ? "" :*/ ("range " + point.getLow() + "-" + point.getHigh());
                String s1 = "<b>" + seriesName1 + "</b>" +
                    "<br/>" +
                    dateToShow
                    +
                    "<br/>" +
                    //"Mean = " + toolTipData.getYAsLong() + "%" +
                    range + s;
                return s1;
              }
            } catch (Exception e) {
              e.printStackTrace();
              return "error " + e.getMessage();
            }
          }
        });
  }


  /**
   * @param title
   * @param subtitle
   * @param seriesName
   * @return
   * @see #showData2
   */
  private Chart getChart2(String title, String subtitle, String seriesName, List<PhoneSession> rawBestScores, boolean narrow) {
    Chart chart = new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        // .setType(Series.Type.ERRORBAR)
        .setChartTitleText(title)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false)
        .setScatterPlotOptions(new ScatterPlotOptions()
            .setMarker(new Marker()
                .setRadius(5)
                .setHoverState(new Marker()
                    .setEnabled(true)
                    .setLineColor(new Color(100, 100, 100))
                )
            )
            .setHoverStateMarker(new Marker()
                .setEnabled(false)
            ))
        .setToolTip(getToolTip2());

    if (narrow) chart.setWidth(NARROW_WIDTH);

    Highcharts.setOptions(
        new Highcharts.Options().setGlobal(
            new Global()
                .setUseUTC(false)
        ));

    addErrorBarSeries(rawBestScores, chart, seriesName);
    addMeans(rawBestScores, chart);
    configureChart(chart, subtitle);
    return chart;
  }

  private void addErrorBarSeries(List<PhoneSession> yValuesForUser, Chart chart, String seriesTitle) {
    Number[][] data = new Number[yValuesForUser.size()][3];

    int i = 0;
    for (PhoneSession ts : yValuesForUser) {
      data[i][0] = ts.getBin();
      double mean = ts.getMean();
      double stdev = ts.getStdev();
      double first = mean - stdev;
      if (first < 0) first = 0;
      data[i][1] = (int) (first * 100);
      double second = mean + stdev;
      if (second > 1) second = 1;
      data[i++][2] = (int) (second * 100);
    }

    Series series = chart.createSeries()
        .setName(seriesTitle)
        .setPoints(data)
        .setType(Series.Type.ERRORBAR);

    chart.addSeries(series);
  }


  /**
   * @param yValuesForUser
   * @param chart
   * @param seriesTitle
   * @paramx gameTimeSeconds
   * @see #getChart
   */
  private void addSeries(List<TimeAndScore> yValuesForUser,
                         Chart chart,
                         String seriesTitle) {
    addCumulativeAverage(yValuesForUser, chart, seriesTitle);
    addDeviceData(yValuesForUser, chart);
  }

  private void addCumulativeAverage(List<TimeAndScore> yValuesForUser, Chart chart, String seriesTitle) {
    Number[][] data = new Number[yValuesForUser.size()][2];

    int i = 0;
    for (TimeAndScore ts : yValuesForUser) {
      data[i][0] = ts.getTimestamp();
      data[i++][1] = ts.getCumulativeAverage() * 100;
    }

    Series series = chart.createSeries()
        .setName(seriesTitle)
        .setPoints(data)
        .setType(Series.Type.LINE);

    chart.addSeries(series);
  }


  private void addMeans(List<PhoneSession> iPadData, Chart chart) {
    Series series;
    Number[][] data = getData(iPadData);

    series = chart.createSeries()
        .setName(AVERAGE)
        .setPoints(data)
        .setOption("color", "#00B800")
        .setType(Series.Type.SPLINE);

    chart.addSeries(series);
  }

  private void addDeviceData(List<TimeAndScore> iPadData, Chart chart) {
    if (!iPadData.isEmpty()) {
      Number[][] data;
      Series series;
      data = getDataForTimeAndScore(iPadData);

      String iPadName = PRONUNCIATION_SCORE;
      series = chart.createSeries()
          .setName(iPadName)
          .setPoints(data)
          .setOption("color", "#00B800");

      chart.addSeries(series);
    }
  }

  private Number[][] getDataForTimeAndScore(List<TimeAndScore> yValuesForUser) {
    Number[][] data = new Number[yValuesForUser.size()][2];

    int i = 0;
    for (TimeAndScore ts : yValuesForUser) {
      data[i][0] = ts.getTimestamp();
      data[i++][1] = ts.getScore() * 100;
    }
    return data;
  }

  private Number[][] getData(List<PhoneSession> yValuesForUser) {
    Number[][] data = new Number[yValuesForUser.size()][2];

    int i = 0;
    for (PhoneSession ts : yValuesForUser) {
      data[i][0] = ts.getBin();
      data[i++][1] = Math.round(ts.getMean() * 100);
    }
    return data;
  }

  /**
   * @param chart
   * @param title
   * @see #getChart
   */
  private void configureChart(Chart chart, String title) {
    chart.getYAxis()
        .setAxisTitleText(title)
        .setMin(-1)
        .setMax(100);

    chart.getXAxis().setType(Axis.Type.DATE_TIME);

    chart.setHeight(CHART_HEIGHT + "px");
  }

  private void setRawBestScores(List<TimeAndScore> rawBestScores) {
    for (TimeAndScore timeAndScore : rawBestScores) {
      timeToId.put(timeAndScore.getTimestamp(), timeAndScore.getId());
    }
  }

  private void setRawBestScores2(List<PhoneSession> rawBestScores) {
    for (PhoneSession timeAndScore : rawBestScores) {
      timeToSession.put(timeAndScore.getBin(), timeAndScore);
    }
  }

  /**
   * @return
   * @see WordContainer#getShell(String)
   */
  private Map<String, CommonShell> getIdToEx() {
    return idToEx;
  }
}
