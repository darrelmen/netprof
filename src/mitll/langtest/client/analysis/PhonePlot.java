package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
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
  //  private final Logger logger = Logger.getLogger("PhonePlot");

  private static final String AVERAGE = "Average";
  private static final int NARROW_WIDTH = 330;
  private static final int CHART_HEIGHT = 340;
  public static final String PRONUNCIATION_SCORE = " score";
  //private static final String PRONUNCIATION_SCORE = "Pronunciation Score";

 // private final Map<Long, String> timeToId = new TreeMap<>();
  private final Map<Long, PhoneSession> timeToSession = new TreeMap<>();
 // private final Map<String, CommonShell> idToEx = new TreeMap<>();

  public PhonePlot() {
    getElement().setId("PhonePlot");
  }

  /**
   * @see PhoneContainer#showExamplesForSelectedSound()
   * @param rawBestScores
   * @param userChosenID
   * @param isNarrow
   */
  public void showData2(List<PhoneSession> rawBestScores, String userChosenID, boolean isNarrow) {
    clear();
    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
      Chart chart = getChart2("<b>" + userChosenID + "</b>" + PRONUNCIATION_SCORE,
          "Average score and range" //+
          , "Range", rawBestScores, isNarrow);
      add(chart);
    }
    setRawBestScores2(rawBestScores);
  }

  private final DateTimeFormat format = DateTimeFormat.getFormat("E MMM d yy h:mm a");
  private final DateTimeFormat noYearFormat = DateTimeFormat.getFormat("E MMM d h:mm a");
  private final DateTimeFormat shortFormat = DateTimeFormat.getFormat("MMM d, yy");
  private final String nowFormat = shortFormat.format(new Date());

  private ToolTip getToolTip2() {
    return new ToolTip()
        .setFormatter(new ToolTipFormatter() {
          public String format(ToolTipData toolTipData) {
            return getTooltipText(toolTipData);
          }
        });
  }

  private String getTooltipText(ToolTipData toolTipData) {
    try {
      PhoneSession session = timeToSession.get(toolTipData.getXAsLong());
      String seriesName1 = toolTipData.getSeriesName();

      Date date = new Date(toolTipData.getXAsLong());

      String shortForDate = shortFormat.format(date);

      DateTimeFormat toUse = sameYear(nowFormat, shortForDate) ? noYearFormat : format;
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

  private boolean sameYear(String nowFormat, String shortForDate) {
    return nowFormat.substring(nowFormat.length() - 2)
        .equals(shortForDate.substring(shortForDate.length() - 2));
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

    int clientWidth = Window.getClientWidth();
    if (clientWidth < 1450) narrow = true;
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

  private void addMeans(List<PhoneSession> iPadData, Chart chart) {
    Series series = chart.createSeries()
        .setName(AVERAGE)
        .setPoints(getData(iPadData))
        .setOption("color", "#00B800")
        .setType(Series.Type.SPLINE);

    chart.addSeries(series);
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

/*  private void setRawBestScores(List<TimeAndScore> rawBestScores) {
    for (TimeAndScore timeAndScore : rawBestScores) {
      timeToId.put(timeAndScore.getTimestamp(), timeAndScore.getId());
    }
  }*/

  private void setRawBestScores2(List<PhoneSession> rawBestScores) {
    for (PhoneSession timeAndScore : rawBestScores) {
      timeToSession.put(timeAndScore.getBin(), timeAndScore);
    }
  }

  /**
   * @return
   * @see WordContainer#getShell(String)
   */
/*
  private Map<String, CommonShell> getIdToEx() {
    return idToEx;
  }
*/
}
