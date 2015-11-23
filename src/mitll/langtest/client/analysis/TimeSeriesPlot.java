/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */
package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.UserPerformance;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 11/20/15.
 */
public class TimeSeriesPlot extends DivWidget {
  private final Logger logger = Logger.getLogger("TimeSeriesPlot");

  protected static final String AVERAGE = "Average";
  private final Map<Long, PhoneSession> timeToSession = new TreeMap<>();

  private final DateTimeFormat format = DateTimeFormat.getFormat("E MMM d yy h:mm a");
  private final DateTimeFormat noYearFormat = DateTimeFormat.getFormat("E MMM d h:mm a");
  private final DateTimeFormat shortFormat = DateTimeFormat.getFormat("MMM d, yy");
  private final String nowFormat = shortFormat.format(new Date());

  /**
   * @return
   * @see PhonePlot#getErrorBarChart(String)
   */
  protected ToolTip getErrorBarToolTip() {
    return new ToolTip()
        .setFormatter(new ToolTipFormatter() {
          public String format(ToolTipData toolTipData) {
            return getTooltipText(toolTipData);
          }
        });
  }

  private String getTooltipText(ToolTipData toolTipData) {
    try {
      String seriesName1 = toolTipData.getSeriesName();

      if (seriesName1.equals(AVERAGE)) {
        return getAvgTooltip(toolTipData, seriesName1);
      } else {
        return getErrorBarToolTip(toolTipData, seriesName1);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "error " + e.getMessage();
    }
  }

  protected String getAvgTooltip(ToolTipData toolTipData, String seriesName1) {
    String dateToShow = getDateToShow(toolTipData);
    return "<b>" + seriesName1 + "</b>" +
        "<br/>" + dateToShow +
        "<br/>Mean = " + toolTipData.getYAsLong() + "%";
  }

  /**
   * @param toolTipData
   * @param seriesName1
   * @return
   * @paramx dateToShow
   * @see AnalysisPlot#getTooltip(ToolTipData, String, CommonShell)
   */
  protected String getErrorBarToolTip(ToolTipData toolTipData, String seriesName1) {
    String dateToShow = getDateToShow(toolTipData);
    Point point = toolTipData.getPoint();
    PhoneSession session = timeToSession.get(toolTipData.getXAsLong());

    String s = session == null ? "" : (" n = " + session.getCount());
    String range = /*point == null ? "" :*/ ("range " + point.getLow() + "-" + point.getHigh());
    return "<b>" + seriesName1 + "</b>" +
        "<br/>" +
        dateToShow
        +
        "<br/>" +
        //"Mean = " + toolTipData.getYAsLong() + "%" +
        range + s;
  }

  protected String getDateToShow(ToolTipData toolTipData) {
    Date date = new Date(toolTipData.getXAsLong());
    String shortForDate = shortFormat.format(date);
    DateTimeFormat toUse = sameYear(shortForDate) ? noYearFormat : format;
    return toUse.format(date);
  }

  private boolean sameYear(String shortForDate) {
    return sameYear(nowFormat, shortForDate);
  }

  private boolean sameYear(String nowFormat, String shortForDate) {
    return nowFormat.substring(nowFormat.length() - 2)
        .equals(shortForDate.substring(shortForDate.length() - 2));
  }

  protected void setRawBestScores2(List<PhoneSession> rawBestScores) {
    timeToSession.clear();
    for (PhoneSession timeAndScore : rawBestScores) {
      //long bin = timeAndScore.getBin();
      long bin = timeAndScore.getMiddle();
      timeToSession.put(bin, timeAndScore);
    }
  }

  /**
   * @param yValuesForUser
   * @param chart
   * @param seriesTitle
   * @param hidden
   * @see PhonePlot#getErrorBarChart(String, String, String, List, boolean)
   * @see AnalysisPlot#addErrorBars(UserPerformance, Chart)
   */
  protected Series addErrorBarSeries(List<PhoneSession> yValuesForUser, Chart chart, String seriesTitle, boolean hidden) {
    Number[][] data = new Number[yValuesForUser.size()][3];

    int i = 0;
    for (PhoneSession ts : yValuesForUser) {
      //data[i][0] = ts.getBin();
      data[i][0] = ts.getMiddle();
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
        .setType(Series.Type.ERRORBAR)
        .setVisible(hidden, false);

//    if (hidden) series.hide();
    // if (hidden) series.setVisible(false,false);

    logger.info("before series " + seriesTitle + " is hidden = " + hidden + " " + series.isVisible());
    if (!hidden) {
      chart.addSeries(series);
    }

    if (hidden) series.setVisible(false, false);

    // logger.info("after  series " + seriesTitle + " is hidden = " + hidden + " " + series.isVisible());

    return series;
  }

  protected Series addMeans(List<PhoneSession> iPadData, Chart chart, String seriesName, boolean hidden) {
    Series series = chart.createSeries()
        .setName(seriesName)
        .setPoints(getData(iPadData))
        .setOption("color", "#00B800")
        .setType(Series.Type.SPLINE);

    if (!hidden) chart.addSeries(series);
    return series;
  }

  private Number[][] getData(List<PhoneSession> yValuesForUser) {
    Number[][] data = new Number[yValuesForUser.size()][2];

    int i = 0;
    for (PhoneSession ts : yValuesForUser) {
//      data[i][0] = ts.getBin();
      data[i][0] = ts.getMiddle();
      data[i++][1] = Math.round(ts.getMean() * 100);
    }
    return data;
  }

}
