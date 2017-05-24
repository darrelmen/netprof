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
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;

import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/19/15.
 */
class PhonePlot extends TimeSeriesPlot {
  private final Logger logger = Logger.getLogger("PhonePlot");

  private static final String PRONUNCIATION_SCORE = " trend";
  private static final int CHART_HEIGHT = 315;
  private static final int NARROW_WIDTH = 330;
  private static final int NARROW_WIDTH_REALLY = 320;

  PhonePlot() {
    getElement().setId("PhonePlot");
  }

  /**
   * @param rawBestScores
   * @param userChosenID
   * @see PhoneContainer#showExamplesForSelectedSound
   */
  void showErrorBarData(List<PhoneSession> rawBestScores, String userChosenID) {
    clear();
    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
      Chart chart = getErrorBarChart(
          "<b>" + userChosenID + "</b>" + PRONUNCIATION_SCORE,
          "Average score and range",
          "Range",
          rawBestScores);
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
    long start = -1;
    long end = -1;

    if (rawBestScores.size() == 1) {
      PhoneSession next = rawBestScores.iterator().next();
      logger.info("getErrorBarChart Got " + next);

      start = next.getStart();
      end = next.getEnd();
    }

    Chart chart = getErrorBarChart(title, subtitle, start, end);

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

  private Chart getErrorBarChart(String title, String subtitle, long start, long end) {
    Chart chart = getErrorBarChart(title);
    configureErrorBarChart(chart, subtitle, start, end);
    return chart;
  }

  private Chart getErrorBarChart(String title) {
    return new Chart()
        .setZoomType(BaseChart.ZoomType.X)
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
  private void configureErrorBarChart(Chart chart, String title, long start, long end) {
    chart.getYAxis()
        .setAxisTitleText(title)
        .setMin(-1)
        .setMax(100);

    chart.getXAxis()
        //   .setStartOnTick(true)
        // .setEndOnTick(true)
        .setType(Axis.Type.DATE_TIME);

    if (end > 0) {
      start = end - 24 * 60 * 60 * 1000;
      end = end + 24 * 60 * 60 * 1000;
      chart.getXAxis().setExtremes(start, end);
    }

    chart.setHeight(CHART_HEIGHT + "px");
  }

  /**
   * @param chart
   * @see #getErrorBarChart(String, String, String, List)
   */
  private void configureWidth(Chart chart) {
    int narrowWidth = (Window.getClientWidth() < WordContainer.NARROW_THRESHOLD) ? NARROW_WIDTH_REALLY : NARROW_WIDTH;
    chart.setWidth(narrowWidth);
  }
}
