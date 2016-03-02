/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.exercise.CommonShell;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 11/20/15.
 */
class TimeSeriesPlot extends DivWidget {
  private final Logger logger = Logger.getLogger("TimeSeriesPlot");
  static final String AVERAGE = "Average";
  private final Map<Long, PhoneSession> timeToSession = new TreeMap<>();

  private final DateTimeFormat format = DateTimeFormat.getFormat("E MMM d yy h:mm a");
  private final DateTimeFormat noYearFormat = DateTimeFormat.getFormat("E MMM d h:mm a");
  private final DateTimeFormat shortFormat = DateTimeFormat.getFormat("MMM d, yy");
  private final DateTimeFormat superShortFormat = DateTimeFormat.getFormat("MMM d");
  private final String nowFormat = shortFormat.format(new Date());

  /**
   * @return
   * @see PhonePlot#getErrorBarChart(String)
   */
  ToolTip getErrorBarToolTip() {
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

  /**
   * Show count in session if small - since error bars won't be displayed.
   * @param toolTipData
   * @param seriesName1
   * @return
   */
  String getAvgTooltip(ToolTipData toolTipData, String seriesName1) {
    String dateToShow = getDateToShow(toolTipData);
    PhoneSession session = timeToSession.get(toolTipData.getXAsLong());

    String countInfo = (session.getCount() < 10) ? "<br/>n = " + session.getCount() : "";

    return getTooltipPrefix(seriesName1, dateToShow) +
        "Mean = " + toolTipData.getYAsLong() + "%" +
        countInfo;
  }

  private String getTooltipPrefix(String seriesName1, String dateToShow) {
    return "<b>" + seriesName1 + "</b>" +
        "<br/>" + dateToShow +
        "<br/>";
  }

  /**
   * @param toolTipData
   * @param seriesName1
   * @return
   * @paramx dateToShow
   * @see AnalysisPlot#getTooltip(ToolTipData, String, CommonShell)
   */
  String getErrorBarToolTip(ToolTipData toolTipData, String seriesName1) {
    String dateToShow = getDateToShow(toolTipData);
    Point point = toolTipData.getPoint();
    String s = getSessionCount(toolTipData);
    String range = "range " + point.getLow() + "-" + point.getHigh();
    return getTooltipPrefix(seriesName1, dateToShow) + range + s;
  }

  private String getSessionCount(ToolTipData toolTipData) {
    PhoneSession session = timeToSession.get(toolTipData.getXAsLong());
    return session == null ? "" : (" n = " + session.getCount());
  }

  String getDateToShow(ToolTipData toolTipData) {
    long xAsLong = toolTipData.getXAsLong();
    return getDateToShow(xAsLong);
  }

  private String getDateToShow(long xAsLong) {
    Date date = new Date(xAsLong);
    String shortForDate = shortFormat.format(date);
    DateTimeFormat toUse = sameYear(shortForDate) ? noYearFormat : format;
    return toUse.format(date);
  }

  String getShortDate(long xAsLong) {
    Date date = new Date(xAsLong);
    String shortForDate = shortFormat.format(date);
    DateTimeFormat toUse = sameYear(shortForDate) ? superShortFormat : shortFormat;
    return toUse.format(date);
  }

  private boolean sameYear(String shortForDate) {
    return sameYear(nowFormat, shortForDate);
  }

  /**
   * Could also use Calendar...
   *
   * @param nowFormat
   * @param shortForDate
   * @return
   */
  private boolean sameYear(String nowFormat, String shortForDate) {
    return nowFormat.substring(nowFormat.length() - 2)
        .equals(shortForDate.substring(shortForDate.length() - 2));
  }

  /**
   * @param phoneSessions
   * @see AnalysisPlot#setVisibility(long, long)
   * @see PhonePlot#showErrorBarData(List, String, boolean)
   */
  void setPhoneSessions(List<PhoneSession> phoneSessions) {
    timeToSession.clear();
    PhoneSession lastSession = getLastSession(phoneSessions);
    for (PhoneSession session : phoneSessions) {
      long bin = getSessionTime(lastSession, session);
      timeToSession.put(bin, session);
    }
  }

  /**
   * @param phoneSessions
   * @param chart
   * @param seriesTitle
   * @param hidden
   * @see PhonePlot#getErrorBarChart
   * @see AnalysisPlot#addErrorBars(UserPerformance, Chart)
   */
  Series addErrorBarSeries(List<PhoneSession> phoneSessions, Chart chart, String seriesTitle, boolean hidden) {
    Number[][] data = new Number[phoneSessions.size()][3];

    int i = 0;
    PhoneSession lastSession = getLastSession(phoneSessions);
    for (PhoneSession ts : phoneSessions) {
    //  logger.info("addErrorBarSeries - " + ts);
      data[i][0] = getSessionTime(lastSession, ts);
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

//    logger.info("before series " + seriesTitle + " is hidden = " + hidden + " " + series.isVisible());
    if (!hidden) {
      chart.addSeries(series);
    }

    if (hidden) series.setVisible(false, false);

    return series;
  }

  private long getSessionTime(PhoneSession lastSession, PhoneSession ts) {
    long middle = ts.getMiddle();
    if (ts == lastSession &&
        ts.getEnd() - middle > AnalysisPlot.HOUR) {
      middle = ts.getEnd();
    }
    return middle;
  }

  Series addMeans(List<PhoneSession> iPadData, Chart chart, String seriesName, boolean hidden) {
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

    PhoneSession lastSession = getLastSession(yValuesForUser);
    int i = 0;
    for (PhoneSession ts : yValuesForUser) {
      data[i][0] = getSessionTime(lastSession, ts);
      data[i++][1] = Math.round(ts.getMean() * 100);
    }
    return data;
  }

  private PhoneSession getLastSession(List<PhoneSession> yValuesForUser) {
    return yValuesForUser.get(yValuesForUser.size() - 1);
  }
}
