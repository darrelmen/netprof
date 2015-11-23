package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Label;
import mitll.langtest.shared.analysis.PhoneSession;
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;

import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 */
public class PhonePlot extends TimeSeriesPlot/* implements IsWidget*/ {
  //  private final Logger logger = Logger.getLogger("PhonePlot");
  public static final String PRONUNCIATION_SCORE = " score";
  private static final int CHART_HEIGHT = 315;
  private static final int NARROW_WIDTH = 330;

  public PhonePlot() {
    getElement().setId("PhonePlot");
  }

  /**
   * @see PhoneContainer#showExamplesForSelectedSound()
   * @param rawBestScores
   * @param userChosenID
   * @param isNarrow
   */
  public void showErrorBarData(List<PhoneSession> rawBestScores, String userChosenID, boolean isNarrow) {
    clear();
    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
      Chart chart = getErrorBarChart("<b>" + userChosenID + "</b>" + PRONUNCIATION_SCORE,
          "Average score and range" //+
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
   * @see #showErrorBarData
   */
  protected Chart getErrorBarChart(String title, String subtitle, String seriesName,
                                   List<PhoneSession> rawBestScores, boolean narrow) {
    Chart chart = getErrorBarChart(title, subtitle);

    configureWidth(narrow, chart);

    Highcharts.setOptions(
        new Highcharts.Options().setGlobal(
            new Global()
                .setUseUTC(false)
        ));

    addErrorBarSeries(rawBestScores, chart, seriesName, false);
    addMeans(rawBestScores, chart, AVERAGE, false);
    return chart;
  }

  private Chart getErrorBarChart(String title, String subtitle) {
    Chart chart = getErrorBarChart(title);
    configureErrorBarChart(chart, subtitle);
    return chart;
  }

  private Chart getErrorBarChart(String title) {
    return new Chart()
        .setZoomType(BaseChart.ZoomType.X)
            // .setType(Series.Type.ERRORBAR)
        .setChartTitleText(title)
      //  .setMarginRight(10)
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
        .setToolTip(getErrorBarToolTip());
  }

  /**
   * @param chart
   * @param title
   * @see #getErrorBarChart
   */
  private void configureErrorBarChart(Chart chart, String title) {
    chart.getYAxis()
        .setAxisTitleText(title)
        .setMin(-1)
        .setMax(100);

    chart.getXAxis().setType(Axis.Type.DATE_TIME);

    chart.setHeight(CHART_HEIGHT + "px");
  }

  protected void configureWidth(boolean narrow, Chart chart) {
//    int clientWidth = Window.getClientWidth();
//    if (clientWidth < 1450) narrow = true;
//    if (narrow) chart.setWidth(NARROW_WIDTH);
    chart.setWidth(NARROW_WIDTH);
//    chart.getElement().getStyle().setProperty("minWidth",NARROW_WIDTH, Style.Unit.PX);
  }
}
