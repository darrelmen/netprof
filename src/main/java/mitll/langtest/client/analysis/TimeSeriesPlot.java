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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import mitll.langtest.shared.analysis.PhoneSession;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/20/15.
 */
class TimeSeriesPlot extends DivWidget {
  // private final Logger logger = Logger.getLogger("TimeSeriesPlot");
  static final String AVERAGE = "Average";
  private final Map<Long, PhoneSession> timeToSession = new TreeMap<>();

  protected final DateTimeFormat format = DateTimeFormat.getFormat("E MMM d yy h:mm a");
 // private final DateTimeFormat noYearFormat = DateTimeFormat.getFormat("E MMM d h:mm a");
  private final DateTimeFormat noYearFormat = DateTimeFormat.getFormat("MMM d h:mm a");
  private final DateTimeFormat shortFormat = DateTimeFormat.getFormat("MMM d, yy");
  private final DateTimeFormat superShortFormat = DateTimeFormat.getFormat("MMM d");
  private final String nowFormat = shortFormat.format(new Date());


  /**
   * @return
   * @see PhonePlot#getErrorBarChart(String)
   */
  ToolTip getErrorBarToolTip() {
    return new ToolTip().setFormatter(this::getTooltipText);
  }

  /**
   * @param toolTipData
   * @return
   * @see #getErrorBarToolTip
   */
  private String getTooltipText(ToolTipData toolTipData) {
    try {
      String seriesName1 = toolTipData.getSeriesName();
      if (seriesName1.equals(AVERAGE)) {
        return getAvgTooltip(toolTipData, seriesName1);
      } else {
        return getErrorBarToolTip(toolTipData, seriesName1);
      }
    } catch (Exception e) {
      return "error " + e.getMessage();
    }
  }

  /**
   * How can phoneSession be null? Transient?
   * <p>
   * Show childCount in session if small - since error bars won't be displayed.
   *
   * @param toolTipData
   * @param seriesName1
   * @return
   */
  String getAvgTooltip(ToolTipData toolTipData, String seriesName1) {
    String dateToShow = getDateToShow(toolTipData);
    PhoneSession session = timeToSession.get(toolTipData.getXAsLong());
    //  String countInfo = (session.getCount() < 10) ? "<br/>n = " + session.getCount() : "";
    String countInfo = session == null ? "" : "<br/>n = " + session.getCount();

    return getTooltipPrefix(seriesName1, dateToShow) +
        "Mean = " + toolTipData.getYAsLong() + "%" +
        countInfo;
  }

  private String getTooltipPrefix(String seriesName1, String dateToShow) {
    return "<b>" + seriesName1 + "</b>" +
        "<br/>" + dateToShow + "<br/>";
  }

  /**
   * @param toolTipData
   * @param seriesName1
   * @return
   * @see AnalysisPlot#getTooltip
   * @see #getTooltipText
   */
  String getErrorBarToolTip(ToolTipData toolTipData, String seriesName1) {
    String dateToShow = getDateToShow(toolTipData);
    Point point = toolTipData.getPoint();
    String s = getSessionCount(toolTipData);
    String range = "range " + point.getLow() + "-" + point.getHigh();
    return getTooltipPrefix(seriesName1, dateToShow) + range + "<br/>" + s;
  }

  private String getSessionCount(ToolTipData toolTipData) {
    PhoneSession session = timeToSession.get(toolTipData.getXAsLong());
    return session == null ? "" : (" n = " + session.getCount());
  }

  String getDateToShow(ToolTipData toolTipData) {
    return getDateToShow(toolTipData.getXAsLong());
  }

  private String getDateToShow(long xAsLong) {
    Date date = new Date(xAsLong);
    String shortForDate = shortFormat.format(date);
    DateTimeFormat toUse = sameYear(shortForDate) ? noYearFormat : format;
    return toUse.format(date);
  }

  String getShortDate(long xAsLong, boolean showHour) {
    Date date = new Date(xAsLong);
    String shortForDate = shortFormat.format(date);
    DateTimeFormat toUse = showHour ? noYearFormat : sameYear(shortForDate) ? superShortFormat : shortFormat;

    return "<span style='white-space:nowrap;'>" + toUse.format(date) + "</span>";
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
   * @see PhonePlot#showErrorBarData(List, String)
   */
  void setPhoneSessions(List<PhoneSession> phoneSessions) {
    timeToSession.clear();
    if (!phoneSessions.isEmpty()) {
      PhoneSession lastSession = getLastSession(phoneSessions);
      // logger.info("setPhoneSessions num sessions " + phoneSessions.size());
      for (PhoneSession session : phoneSessions) {
        timeToSession.put(getSessionTime(lastSession, session), session);
      }
    }
  }

  /**
   * @param phoneSessions
   * @param chart
   * @param seriesTitle
   * @param hidden
   * @see PhonePlot#getErrorBarChart
   * @see AnalysisPlot#addErrorBars
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


  /**
   * @param iPadData
   * @param chart
   * @param seriesName
   * @param hidden
   * @return
   * @see #addErrorBarSeries
   * @see PhonePlot#getErrorBarChart(String, String, String, List)
   */
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

  /**
   * ? what is this doing ?
   *
   * @param lastSession
   * @param ts
   * @return
   */
  private long getSessionTime(PhoneSession lastSession, PhoneSession ts) {
    long middle = ts.getMiddle();
    if (ts == lastSession &&
        ts.getEnd() - middle > AnalysisTab.HOUR) {
      middle = ts.getEnd();
    }
    return middle;
  }

  private PhoneSession getLastSession(List<PhoneSession> yValuesForUser) {
    return yValuesForUser.get(yValuesForUser.size() - 1);
  }
}
