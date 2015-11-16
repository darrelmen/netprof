package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.IsWidget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.CommonShell;
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
 // private final Logger logger = Logger.getLogger("AnalysisPlot");
  private static final int NARROW_WIDTH = 330;
  private static final int CHART_HEIGHT = 340;
//  private static final int Y_OFFSET_FOR_LEGEND = 60;
  private static final String PRONUNCIATION_SCORE = "Pronunciation Score";

  private final Map<Long, String> timeToId = new TreeMap<>();
  private final Map<String, CommonShell> idToEx = new TreeMap<>();

  public PhonePlot() {
    getElement().setId("PhonePlot");
  }

  /**
   * @see PhoneContainer#gotClickOnItem(PhoneAndStats)
   * @param rawBestScores
   * @param userChosenID
   * @param isNarrow
   */
  public void showData(List<TimeAndScore> rawBestScores, String userChosenID, boolean isNarrow) {
    clear();
    int rawTotal = rawBestScores.size();//userPerformance.getRawTotal();
    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
//        logger.info("getPerformanceForUser raw total " + rawTotal + " num " + rawBestScores.size());
      Chart chart = getChart("<b>" + userChosenID + "</b>" +
              " pronunciation score"
          //+ " (Drag to zoom in)"
          ,
          "Score and average (" + rawTotal + " items " +
              //": avg score " + (int) v +
              " %)", "Cumulative Average", rawBestScores, isNarrow);
      add(chart);
    }
    setRawBestScores(rawBestScores);
  }

  /**
   * @param title
   * @param subtitle
   * @param seriesName
   * @return
   * @see #showData(List, String, boolean)
   */
  private Chart getChart(String title, String subtitle, String seriesName, List<TimeAndScore> rawBestScores, boolean narrow) {
    Chart chart = new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        .setType(Series.Type.SCATTER)
        .setChartTitleText(title)
    //    .setChartSubtitleText(subtitle)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false)
//        .setLegend(new Legend()
//            .setLayout(Legend.Layout.VERTICAL)
//            .setAlign(Legend.Align.LEFT)
//            .setVerticalAlign(Legend.VerticalAlign.TOP)
//            .setX(100)
//            .setY(Y_OFFSET_FOR_LEGEND)
//            .setBorderWidth(1)
//            .setFloating(true)
//            .setBackgroundColor("#FFFFFF")
//        )
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
        .setToolTip(new ToolTip()
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
            }));
    if (narrow) chart.setWidth(NARROW_WIDTH);

    addSeries(rawBestScores, chart, seriesName);

    configureChart(chart, subtitle);
    return chart;
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
      data[i][0]   = ts.getTimestamp();
      data[i++][1] = ts.getCumulativeAverage() * 100;
    }

    Series series = chart.createSeries()
        .setName(seriesTitle)
        .setPoints(data)
        .setType(Series.Type.LINE);

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

  /**
   * @param chart
   * @param title
   * @see #getChart
   */
  private void configureChart(Chart chart, String title) {
    chart.getYAxis().setAxisTitleText(title)
        .setMin(0).setMax(100);

    chart.getXAxis()
        .setType(Axis.Type.DATE_TIME);

    chart.setHeight(CHART_HEIGHT + "px");
  }

  private void setRawBestScores(List<TimeAndScore> rawBestScores) {
    for (TimeAndScore timeAndScore : rawBestScores) {
      timeToId.put(timeAndScore.getTimestamp(), timeAndScore.getId());
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
