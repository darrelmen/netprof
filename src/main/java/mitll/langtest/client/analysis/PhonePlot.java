/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Label;
import com.google.gwt.user.client.Window;
import mitll.langtest.shared.analysis.PhoneSession;
import org.moxieapps.gwt.highcharts.client.Axis;
import org.moxieapps.gwt.highcharts.client.BaseChart;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Color;
import org.moxieapps.gwt.highcharts.client.Global;
import org.moxieapps.gwt.highcharts.client.Highcharts;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;

import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/19/15.
 */
class PhonePlot extends TimeSeriesPlot {
  //  private final Logger logger = Logger.getLogger("PhonePlot");
  private static final String PRONUNCIATION_SCORE = " trend";
  private static final int CHART_HEIGHT = 315;
  private static final int NARROW_WIDTH = 330;
  private static final int NARROW_WIDTH_REALLY = 320;

  PhonePlot() {
    getElement().setId("PhonePlot");
  }

  /**
   * @see PhoneContainer#showExamplesForSelectedSound
   * @param rawBestScores
   * @param userChosenID
   */
  void showErrorBarData(List<PhoneSession> rawBestScores, String userChosenID) {
    clear();
    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
      Chart chart = getErrorBarChart("<b>" + userChosenID + "</b>" + PRONUNCIATION_SCORE,
          "Average score and range", "Range", rawBestScores);
      add(chart);
    }
    setPhoneSessions(rawBestScores);
  }

  /**
   * @param title
   * @param subtitle
   * @param seriesName
   * @return
   * @see #showErrorBarData
   */
  private Chart getErrorBarChart(String title, String subtitle, String seriesName,
                                 List<PhoneSession> rawBestScores) {
    Chart chart = getErrorBarChart(title, subtitle);

    configureWidth(chart);

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

  /**
   * @see #getErrorBarChart(String, String, String, List)
   * @param chart
   */
  private void configureWidth(Chart chart) {
    int narrowWidth =(Window.getClientWidth() < WordContainer.NARROW_THRESHOLD) ? NARROW_WIDTH_REALLY : NARROW_WIDTH;
    chart.setWidth(narrowWidth);
  }
}
