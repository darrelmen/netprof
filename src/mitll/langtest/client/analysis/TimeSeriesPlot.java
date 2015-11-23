/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */
package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
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
      String dateToShow = getDateToShow(toolTipData);

      if (seriesName1.equals(AVERAGE)) {
        return "<b>" + seriesName1 + "</b>" +
            "<br/>" + dateToShow +
            "<br/>Mean = " + toolTipData.getYAsLong() + "%";
      } else {
        return getErrorBarToolTip(toolTipData, seriesName1, dateToShow);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "error " + e.getMessage();
    }
  }

  protected String getErrorBarToolTip(ToolTipData toolTipData, String seriesName1, String dateToShow) {
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
    for (PhoneSession timeAndScore : rawBestScores) {
      timeToSession.put(timeAndScore.getBin(), timeAndScore);
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
        .setType(Series.Type.ERRORBAR)
        .setVisible(hidden, false);

//    if (hidden) series.hide();
    // if (hidden) series.setVisible(false,false);

    logger.info("before series " + seriesTitle + " is hidden = " + hidden + " " + series.isVisible());
    chart.addSeries(series);

    if (hidden) series.setVisible(false,false);

   // logger.info("after  series " + seriesTitle + " is hidden = " + hidden + " " + series.isVisible());

    return series;
  }

  protected Series addMeans(List<PhoneSession> iPadData, Chart chart, String seriesName) {
    Series series = chart.createSeries()
        .setName(seriesName)
        .setPoints(getData(iPadData))
        .setOption("color", "#00B800")
        .setType(Series.Type.SPLINE);

    chart.addSeries(series);
    return series;
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

}
