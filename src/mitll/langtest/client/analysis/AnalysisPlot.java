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

import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Label;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.events.AxisSetExtremesEvent;
import org.moxieapps.gwt.highcharts.client.events.AxisSetExtremesEventHandler;
import org.moxieapps.gwt.highcharts.client.events.SeriesClickEvent;
import org.moxieapps.gwt.highcharts.client.events.SeriesClickEventHandler;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.SeriesPlotOptions;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/19/15.
 */
public class AnalysisPlot extends TimeSeriesPlot {
  private static final int MIN_SESSION_COUNT = 50;
  private final Logger logger = Logger.getLogger("AnalysisPlot");

  private static final int X_OFFSET_LEGEND = 65;

  private static final long MINUTE = 60 * 1000;
  public static final long HOUR = 60 * MINUTE;
  private static final long QUARTER = 6 * HOUR;

  private static final long FIVEMIN = 5 * MINUTE;
  private static final long DAY = 24 * HOUR;
  private static final long WEEK = 7 * DAY;
  private static final long MONTH = 4 * WEEK;
  private static final long YEAR = 52 * WEEK;
  private static final long YEARS = 20 * YEAR;

  private static final String I_PAD_I_PHONE = "iPad/iPhone";
  private static final String VOCAB_PRACTICE = "Vocab Practice";
  private static final String LEARN = "Learn";
  private static final String CUMULATIVE_AVERAGE = "Average";
  private final Set<String> toShowExercise = new HashSet<>(Arrays.asList(I_PAD_I_PHONE, VOCAB_PRACTICE, LEARN, CUMULATIVE_AVERAGE));

  private static final int SHORT_THRESHOLD = 822;
  private static final int CHART_HEIGHT_SHORT = 260;
  private static final int CHART_HEIGHT = 330;

  private static final int Y_OFFSET_FOR_LEGEND = 20;

  private final Map<Long, String> timeToId = new TreeMap<>();
  private final Map<String, CommonShell> idToEx = new TreeMap<>();
  private final int userid;
  private final AnalysisServiceAsync service;
  private final PlayAudio playAudio;
  private Map<Long, String> granToLabel;

  private final Map<Series, Boolean> seriesToVisible = new HashMap<>();
  private Map<Long, List<PhoneSession>> granularityToSessions;
  private final Map<Long, Series> granToError = new HashMap<>();
  private final Map<Long, Series> granToAverage = new HashMap<>();
  private final List<Series> detailSeries = new ArrayList<>();

  private Chart chart = null;
  private AnalysisTab.TIME_HORIZON timeHorizon;
  /**
   * @see #setRawBestScores(List)
   */
  private long firstTime;
  private long lastTime;
  private final List<Long> weeks = new ArrayList<>();
  private final List<Long> months = new ArrayList<>();

  /**
   * @param service
   * @param userid
   * @param userChosenID
   * @param minRecordings
   * @see AnalysisTab#AnalysisTab
   */
  public AnalysisPlot(LangTestDatabaseAsync service, int userid, final String userChosenID, final int minRecordings,
                      SoundManagerAPI soundManagerAPI, Icon playFeedback) {
    getElement().setId("AnalysisPlot");
    int minHeight = isShort() ? CHART_HEIGHT_SHORT : CHART_HEIGHT;

    getElement().getStyle().setProperty("minHeight", minHeight, Style.Unit.PX);
    getElement().getStyle().setMargin(10, Style.Unit.PX);
    addStyleName("cardBorderShadow");

    this.service = GWT.create(AnalysisService.class);
    this.userid = userid;
    populateGranToLabel();

    this.playAudio = new PlayAudio(service, new SoundPlayer(soundManagerAPI), playFeedback);
    populateExerciseMap(service, userid);

    getPerformanceForUser(this.service, userid, userChosenID, minRecordings);
  }

  private void populateGranToLabel() {
    this.granToLabel = new HashMap<>();
    granToLabel.put(HOUR, "Hour");
    granToLabel.put(QUARTER, "6 Hours");
    granToLabel.put(DAY, "Day");
    granToLabel.put(WEEK, "Week");
    granToLabel.put(MONTH, "Month");
    granToLabel.put(YEAR, "Year");
    granToLabel.put(FIVEMIN, "Minute");
  }

  private void getPerformanceForUser(AnalysisServiceAsync service, int userid,
                                     final String userChosenID, int minRecordings) {
    service.getPerformanceForUser(userid, minRecordings, new AsyncCallback<UserPerformance>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("\n\n\n-> getPerformanceForUser " + throwable);
      }

      @Override
      public void onSuccess(UserPerformance userPerformance) {
        List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
        if (!rawBestScores.isEmpty()) {
          long last = rawBestScores.get(rawBestScores.size() - 1).getTimestamp();
          List<PhoneSession> phoneSessions = userPerformance.getGranularityToSessions().get(WEEK);
          weeks.addAll(getPeriods(phoneSessions, WEEK, last));
          List<PhoneSession> phoneSessions1 = userPerformance.getGranularityToSessions().get(MONTH);
          months.addAll(getPeriods(phoneSessions1, MONTH, last));
        }
        addChart(userPerformance, userChosenID);
      }
    });
  }

  private SortedSet<Long> getPeriods(List<PhoneSession> phoneSessions, long granularity, final long lastTime) {
    int i = phoneSessions.size() - 1;
    SortedSet<Long> months2 = new TreeSet<>();
    //   logger.info("Examine  " + granularity + " : " + phoneSessions.size() + " last " + new Date(lastTime));
    long start = lastTime - granularity;
    long last = lastTime;

    while (i >= 0) {
      PhoneSession session = phoneSessions.get(i);
      //  String sessionWindow = getShortDate(session.getStart()) + " - " + getShortDate(session.getEnd());
      //  String window = getShortDate(start) + " - " + getShortDate(last);
      if (
          session.doesOverlap(start, last)
          ) {
  /*      logger.info("session " + sessionWindow + " vs" + window +
            " at " + i);*/
        months2.add(start);
        start -= granularity;
        last -= granularity;
      } else {
    /*    logger.info("not session " +
            sessionWindow + " vs " +
            window +
            "  at " + i);*/
        if (session.getStart() > last) {
          i--;
        } else {
          start -= granularity;
          last -= granularity;
        }
      }
    }
    PhoneSession first = phoneSessions.get(0);
    if (first.getStart() < start) {
      months2.add(start);
    }
    return months2;
  }

  private void addChart(UserPerformance userPerformance, String userChosenID) {
    clear();

    List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
    float v = userPerformance.getRawAverage() * 100;
    int rawTotal = rawBestScores.size();

    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
      String subtitle = "Score and average (" + rawTotal + " items, average " + (int) v + " %)";
      String title = "<b>" + userChosenID + "</b>" + " pronunciation score (Drag to zoom in, click to hear)";
      chart = getChart(title, subtitle, CUMULATIVE_AVERAGE, userPerformance);
      add(chart);
    }
    setRawBestScores(rawBestScores);
    showSeriesByVisible();
  }

  private void showSeriesByVisible() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        if (chart != null) {
          // logger.info("doing deferred ---------- ");
          for (Series series : seriesToVisible.keySet()) {
            //  String name = series.getName();
            Boolean expected = seriesToVisible.get(series);
            if (expected) {
//              logger.info("showSeriesByVisible defer : series " + name + "/" + series + " : " + series.isVisible());
              if (chart.getSeries(series.getId()) == null) {
                chart.addSeries(series);
                if (!series.isVisible()) {
                  series.setVisible(true);
                }
                // logger.info("\tshowSeriesByVisible defer : series " + name + "/" + series + " : " + series.isVisible());
              }
            } else {
              chart.removeSeries(series);
              if (!series.isVisible()) {
                series.setVisible(true);
              }
            }
          }
        }
      }
    });
  }

  private void populateExerciseMap(LangTestDatabaseAsync service, int userid) {
    service.getExerciseIds(
        new ExerciseListRequest(1, userid),
        new AsyncCallback<ExerciseListWrapper<CommonShell>>() {
          @Override
          public void onFailure(Throwable throwable) {
            logger.warning("\n\n\n-> getExerciseIds " + throwable);
          }

          @Override
          public void onSuccess(ExerciseListWrapper<CommonShell> exerciseListWrapper) {
            for (CommonShell shell : exerciseListWrapper.getExercises()) {
              getIdToEx().put(shell.getOldID(), shell);
            }
          }
        });
  }

  /**
   * @param title
   * @param subtitle
   * @param seriesName
   * @return
   * @see AnalysisPlot#AnalysisPlot
   */
  private Chart getChart(String title, String subtitle, String seriesName, UserPerformance userPerformance) {
    final Chart chart = getChart(title);

    Highcharts.setOptions(
        new Highcharts.Options().setGlobal(
            new Global()
                .setUseUTC(false)
        ));


    List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();

    boolean shortPeriod = isShortPeriod(rawBestScores);
    addSeries(rawBestScores,
        userPerformance.getiPadTimeAndScores(),
        userPerformance.getLearnTimeAndScores(),
        userPerformance.getAvpTimeAndScores(),
        chart,
        seriesName,
        shortPeriod);

    if (!shortPeriod) {
      addErrorBars(userPerformance, chart);
    }

    configureChart(chart, subtitle);
    return chart;
  }

  private boolean isShortPeriod(List<TimeAndScore> rawBestScores) {
    if (rawBestScores.size() < 2) return true;
    TimeAndScore first = rawBestScores.get(0);
    TimeAndScore last = rawBestScores.get(rawBestScores.size() - 1);

    long diff = last.getTimestamp() - first.getTimestamp();
    return diff < QUARTER;
  }

  /**
   * @param userPerformance
   * @param chart
   * @see #getChart(String, String, String, UserPerformance)
   */
  private void addErrorBars(UserPerformance userPerformance, Chart chart) {
    granToError.clear();
    granularityToSessions = userPerformance.getGranularityToSessions();

    List<Long> grans = new ArrayList<>(granularityToSessions.keySet());
    Collections.sort(grans);
    for (Long gran : grans) {
      String label = granToLabel.get(gran);
      //   logger.info("addErrorBars Adding for " + label);
      List<PhoneSession> phoneSessions = granularityToSessions.get(gran);
      granToError.put(gran, addErrorBarSeries(phoneSessions, chart, label, true));

      Series avgSeries = addMeans(phoneSessions, chart, label, true);
      granToAverage.put(gran, avgSeries);
    }

    long now = System.currentTimeMillis();

//    logger.info("setVisiblity for " + new Date(now));
    setVisibility(now - YEARS, now);
  }

  /**
   * Choose which granularity of data to show - hours, weeks, month, etc.
   *
   * @param start
   * @param end
   * @see #addErrorBars(UserPerformance, Chart)
   * @see #gotExtremes(AxisSetExtremesEvent)
   */
  private void setVisibility(long start, long end) {
    //  logger.info("setVisibility from " + start + "/" + new Date(start) + " - " + new Date(end));
    List<Long> grans = new ArrayList<>(granularityToSessions.keySet());

    Collections.sort(grans);

    Series errorSeries = null;
    Series averageSeries = null;

    hideAllSeries();

    boolean oneSet = false;
    for (Long gran : grans) {
      if (!oneSet) {
        List<PhoneSession> phoneSessions = granularityToSessions.get(gran);

        int size = 0;
        int total = 0;
        boolean anyBigger = false;
        for (PhoneSession session : phoneSessions) {
          if (session.getStart() >= start && session.getEnd() < end) {
            //  logger.info("\t " + gran + " session " + session);
            size++;
            total += session.getCount();
            if (session.getCount() > MIN_SESSION_COUNT) anyBigger = true;
          }
        }
        //       String label = granToLabel.get(gran);
//        String seriesInfo = gran + "/" + label;
        // logger.info("setVisibility  " + seriesInfo + " : " + size + " sessions " + phoneSessions.size() + " any bigger " + anyBigger);

        if (PhoneSession.chooseThisSize(size, total, anyBigger)) {
          oneSet = true;
          errorSeries = granToError.get(gran);
          averageSeries = granToAverage.get(gran);
          setPhoneSessions(granularityToSessions.get(gran));
          //logger.info("setVisibility 1 chose " + seriesInfo + " : " + size + " visible " + series.isVisible());
        }
        //else {
        //logger.info("setVisibility 2 too small " + seriesInfo + " : " + size);
        //}
      }
    }

    showErrorBarsOrDetail(oneSet, errorSeries, averageSeries);
  }

  private void hideAllSeries() {
    for (Long gran : granularityToSessions.keySet()) {
      Series series = granToError.get(gran);
      seriesToVisible.put(series, false);
      Series avgSeries = granToAverage.get(gran);
      seriesToVisible.put(avgSeries, false);
    }
  }

  private void showErrorBarsOrDetail(boolean oneSet, Series errorSeries, Series averageSeries) {
    for (Series series : detailSeries) seriesToVisible.put(series, false);
    if (!oneSet) {
      for (Series series : detailSeries) {
        seriesToVisible.put(series, true);
      }

      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
          // logger.info("showErrorBarsOrDetail - redraw");

          for (Series series : detailSeries) {
            series.setVisible(true, false);
          }
          chart.redraw();
        }
      });
    } else {
      seriesToVisible.put(errorSeries, true);
      seriesToVisible.put(averageSeries, true);
    }
  }

  private Chart getChart(String title) {
    return new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        .setChartTitleText(title)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false)
        .setLegend(new Legend()
            .setLayout(Legend.Layout.VERTICAL)
            .setAlign(Legend.Align.LEFT)
            .setVerticalAlign(Legend.VerticalAlign.TOP)
            .setX(X_OFFSET_LEGEND)
            .setY(Y_OFFSET_FOR_LEGEND)
            .setBorderWidth(1)
            .setFloating(true)
            .setBackgroundColor("#FFFFFF")
        )
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
        .setToolTip(getToolTip())
        .setSeriesPlotOptions(new SeriesPlotOptions()
            .setSeriesClickEventHandler(getSeriesClickEventHandler())
        );
  }

  private SeriesClickEventHandler getSeriesClickEventHandler() {
    return new SeriesClickEventHandler() {
      public boolean onClick(SeriesClickEvent clickEvent) {
        long nearestXAsLong = clickEvent.getNearestXAsLong();
        String s = timeToId.get(nearestXAsLong);
        if (s != null) {
          playAudio.playLast(s, userid);
        } else {
          logger.info("no point at " + nearestXAsLong);
        }
        return true;
      }
    };
  }

  private ToolTip getToolTip() {
    return new ToolTip()
        .setFormatter(new ToolTipFormatter() {
          public String format(ToolTipData toolTipData) {
            try {
              String exerciseID = timeToId.get(toolTipData.getXAsLong());
              CommonShell commonShell = exerciseID == null ? null : getIdToEx().get(exerciseID);
              return getTooltip(toolTipData, exerciseID, commonShell);
            } catch (Exception e) {
              logger.warning(e.getMessage());
              e.printStackTrace();
              return "";
            }
          }
        });
  }

  private String getTooltip(ToolTipData toolTipData, String s, CommonShell commonShell) {
    String foreignLanguage = commonShell == null ? "" : commonShell.getForeignLanguage();
    String english = commonShell == null ? "" : commonShell.getEnglish();

    String seriesName1 = toolTipData.getSeriesName();

    String dateToShow = getDateToShow(toolTipData);
    Collection<String> values = granToLabel.values();

    if (values.contains(seriesName1)) {
      String seriesId = toolTipData.getSeriesId();
      Series series = chart.getSeries(seriesId);
      if (granToAverage.values().contains(series)) {
        return getAvgTooltip(toolTipData, seriesName1);
      } else {
        return getErrorBarToolTip(toolTipData, seriesName1);
      }
    } //else {
    //logger.info("getTooltip series is " + seriesName1 + " not in " + values);
    // }
    boolean showEx = toShowExercise.contains(seriesName1);

    String englishTool = (english == null || english.equals("N/A")) ? "" : "<br/>" + english;

    return
        "<b>" + seriesName1 + "</b>" +
            "<br/>" +
            dateToShow +
            (showEx ?
                "<br/>Exercise " + s + "<br/>" +
                    "<span style='font-size:200%'>" + foreignLanguage + "</span>" +
                    englishTool
                : "")
            +
            "<br/>Score <b>" + toolTipData.getYAsLong() + "</b>%" +
            (showEx ?
                "<br/>" + "<b>Click to hear</b>"
                : "")

        ;
  }

  /**
   * @param yValuesForUser
   * @param chart
   * @param seriesTitle
   * @see #getChart
   */
  private void addSeries(Collection<TimeAndScore> yValuesForUser,
                         Collection<TimeAndScore> iPadData,
                         Collection<TimeAndScore> learnData,
                         Collection<TimeAndScore> avpData,
                         Chart chart,
                         String seriesTitle, boolean visible) {
    addCumulativeAverage(yValuesForUser, chart, seriesTitle, visible);

    addDeviceData(iPadData, chart, visible);
    addBrowserData(learnData, chart, false, visible);
    addBrowserData(avpData, chart, true, visible);
  }

  private void addCumulativeAverage(Collection<TimeAndScore> yValuesForUser, Chart chart, String seriesTitle, boolean isVisible) {
    Number[][] data = new Number[yValuesForUser.size()][2];
    int i = 0;
    for (TimeAndScore ts : yValuesForUser) {
      data[i][0] = ts.getTimestamp();
      data[i++][1] = ts.getCumulativeAverage() * 100;
    }

    Series series = chart.createSeries()
        .setName(seriesTitle)
        .setPoints(data)
        .setType(Series.Type.SPLINE);

    recordVisible(isVisible, series);
  }

  private void recordVisible(boolean isVisible, Series series) {
    seriesToVisible.put(series, isVisible);
    detailSeries.add(series);
  }

  private void addDeviceData(Collection<TimeAndScore> iPadData, Chart chart, boolean isVisible) {
    if (!iPadData.isEmpty()) {
      Number[][] data = getDataForTimeAndScore(iPadData);

      String iPadName = I_PAD_I_PHONE;
      Series series = chart.createSeries()
          .setName(iPadName)
          .setPoints(data)
          .setOption("color", "#00B800")
          .setType(Series.Type.SCATTER);
      recordVisible(isVisible, series);
    }
  }

  private void addBrowserData(Collection<TimeAndScore> browserData, Chart chart, boolean isAVP, boolean isVisible) {
    if (!browserData.isEmpty()) {
      Number[][] data = getDataForTimeAndScore(browserData);

      String prefix = isAVP ? VOCAB_PRACTICE : LEARN;

      Series series = chart.createSeries()
          .setName(prefix)
          .setPoints(data)
          .setType(Series.Type.SCATTER);

      recordVisible(isVisible, series);
    }
  }

  private Number[][] getDataForTimeAndScore(Collection<TimeAndScore> yValuesForUser) {
    Number[][] data = new Number[yValuesForUser.size()][2];

    int i = 0;
    for (TimeAndScore ts : yValuesForUser) {
      data[i][0] = ts.getTimestamp();
      data[i++][1] = ts.getScore() * 100;
    }
    return data;
  }

  /**
   * As we change zoom, show more or less info
   *
   * @param chart
   * @param title
   * @see #getChart
   */
  private void configureChart(Chart chart, String title) {
    chart.getYAxis()
        .setAxisTitleText(title)
        .setMin(0)
        .setMax(100);

    chart.getXAxis()
        .setStartOnTick(true)
        .setEndOnTick(true)
        .setType(Axis.Type.DATE_TIME).setAxisSetExtremesEventHandler(new AxisSetExtremesEventHandler() {
      @Override
      public boolean onSetExtremes(AxisSetExtremesEvent axisSetExtremesEvent) {
        if (axisSetExtremesEvent != null) {
          // logger.info("configureChart window " + firstTime + " " + lastTime);

          gotExtremes(axisSetExtremesEvent);
        }
        //else {
        //  logger.warning("got null event");
        // }
        return true;
      }
    });

    int chartHeight = isShort() ? CHART_HEIGHT_SHORT : CHART_HEIGHT;
    chart.setHeight(chartHeight + "px");
  }

  private boolean isShort() {
    return Window.getClientHeight() < SHORT_THRESHOLD;
  }

  /**
   * So when the x axis range changes, we get an event here.
   *
   * @param axisSetExtremesEvent
   */
  private void gotExtremes(AxisSetExtremesEvent axisSetExtremesEvent) {
    for (Series series : detailSeries) {
      series.setVisible(false, false);
    }

    try {
      Number min = axisSetExtremesEvent.getMin();
      Number max = axisSetExtremesEvent.getMax();

      //   logger.info("gotExtremes got min " + min + " max " + max);
      if (min != null && min.longValue() > 0) {
        long end = max.longValue();
        setVisibility(min.longValue(), end);
        timeChanged(min.longValue(), end);
      } else {
        long end = System.currentTimeMillis();
        setVisibility(end - YEARS, end);
        setTimeWindowControlsToAll();
      }
      showSeriesByVisible();

    } catch (Exception e) {
      e.printStackTrace();
      logger.warning("Got " + e);
    }
  }

  /**
   * Remember time window of data (x-axis).
   *
   * @param rawBestScores
   * @see #addChart(UserPerformance, String)
   */
  private void setRawBestScores(List<TimeAndScore> rawBestScores) {
    List<String> toGet = new ArrayList<>();
    for (TimeAndScore timeAndScore : rawBestScores) {
      String id = timeAndScore.getId();
      timeToId.put(timeAndScore.getTimestamp(), id);
      if (!getIdToEx().containsKey(id)) {
        toGet.add(id);
      }
    }

    if (!rawBestScores.isEmpty()) {
      TimeAndScore timeAndScore = rawBestScores.get(rawBestScores.size() - 1);

      this.firstTime = rawBestScores.get(0).getTimestamp();
      this.lastTime = timeAndScore.getTimestamp();
      // logger.info("setRawBestScores is firstTime " + new Date(firstTime) + " - " + new Date(lastTime));

      service.getShells(toGet, new AsyncCallback<List<CommonShell>>() {
        @Override
        public void onFailure(Throwable throwable) {
        }

        @Override
        public void onSuccess(List<CommonShell> commonShells) {
          for (CommonShell shell : commonShells) idToEx.put(shell.getOldID(), shell);
        }
      });
    }
    else {
      //logger.info("rawBest is empty?");
    }
  }

  /**
   * @return
   * @see WordContainer#getShell(String)
   */
  Map<String, CommonShell> getIdToEx() {
    return idToEx;
  }

  /**
   * @param timeHorizon
   * @return
   * @see AnalysisTab#getClickHandler(AnalysisTab.TIME_HORIZON)
   */
  long setTimeHorizon(AnalysisTab.TIME_HORIZON timeHorizon) {
    this.timeHorizon = timeHorizon;
    Long x = goToLast(timeHorizon);
    if (x != null) return x;
    return 0;
  }

  private int index = 0;

  private TimeWidgets timeWidgets;

  /**
   * @param timeHorizon
   * @return
   * @see #setTimeHorizon(AnalysisTab.TIME_HORIZON)
   */
  private Long goToLast(AnalysisTab.TIME_HORIZON timeHorizon) {
    this.timeHorizon = timeHorizon;

    // logger.info("goToLast set time from " + new Date(firstTime) + " to " + new Date(lastTime));
    switch (timeHorizon) {
      case WEEK:
        long prevWeek = lastTime - WEEK;
        chart.getXAxis().setExtremes(prevWeek, lastTime + HOUR);

        int lastWeekIndex = weeks.size() - 1;
        Long lastWeek = weeks.get(lastWeekIndex);
        this.index = lastWeekIndex;

        timeWidgets.prevButton.setEnabled(weeks.size() > 1);
        timeWidgets.nextButton.setEnabled(false);
        timeWidgets.display.setText(getShortDate(lastWeek));

        timeChanged(prevWeek, lastTime);

        return prevWeek;
      case MONTH:
        long startOfPrevMonth = lastTime - MONTH;
        chart.getXAxis().setExtremes(startOfPrevMonth, lastTime + HOUR);

        int lastMonthIndex = months.size() - 1;
        Long lastMonth = months.get(lastMonthIndex);
        this.index = lastMonthIndex;

        timeWidgets.prevButton.setEnabled(months.size() > 1);
        timeWidgets.nextButton.setEnabled(false);
        timeWidgets.display.setText(getShortDate(lastMonth));

        timeChanged(startOfPrevMonth, lastTime);

        return startOfPrevMonth;
      case ALL:
        chart.getXAxis().setExtremes(firstTime - HOUR, lastTime + HOUR);
        setTimeWindowControlsToAll();
        timeChanged(firstTime, lastTime);

        return firstTime;
    }
    return null;
  }

  private void setTimeWindowControlsToAll() {
    timeWidgets.prevButton.setEnabled(false);
    timeWidgets.nextButton.setEnabled(false);
    HTML display = timeWidgets.display;
    if (display == null) logger.warning("huh? no display on " + timeWidgets);
    else {
      display.setText("");
    }
    timeWidgets.reset();
    timeChanged(firstTime, lastTime);
  }

  public void gotPrevClick() {
    long offset = timeHorizon == AnalysisTab.TIME_HORIZON.WEEK ? WEEK : MONTH;
    List<Long> periods = timeHorizon == AnalysisTab.TIME_HORIZON.WEEK ? weeks : months;

    index--;

    timeWidgets.prevButton.setEnabled(index > 0);
    timeWidgets.nextButton.setEnabled(true);

    showTimePeriod(offset, periods);
  }

  public void gotNextClick() {
    long offset = timeHorizon == AnalysisTab.TIME_HORIZON.WEEK ? WEEK : MONTH;
    List<Long> periods = timeHorizon == AnalysisTab.TIME_HORIZON.WEEK ? weeks : months;

    index++;

    timeWidgets.prevButton.setEnabled(true);
    timeWidgets.nextButton.setEnabled(index < periods.size() - 1);

    showTimePeriod(offset, periods);
  }

  /**
   * @param offset
   * @param periods
   * @see #gotNextClick()
   * @see #gotPrevClick()
   */
  private void showTimePeriod(long offset, List<Long> periods) {
    Long aLong = periods.get(index);
    timeWidgets.display.setText(getShortDate(aLong));
    long end = aLong + offset;
    chart.getXAxis().setExtremes(aLong, end);

    timeChanged(aLong, end);
  }

  public void setTimeWidgets(TimeWidgets timeWidgets) {
    this.timeWidgets = timeWidgets;
  }

  public interface TimeChangeListener {
    void timeChanged(long from, long to);
  }

  private final Set<TimeChangeListener> listeners = new HashSet<>();

  public void addListener(TimeChangeListener listener) {
    listeners.add(listener);
  }

  private void timeChanged(long from, long to) {
    for (TimeChangeListener listener : listeners) listener.timeChanged(from, to);
  }
}
