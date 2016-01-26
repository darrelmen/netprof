/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Label;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.exercise.Shell;
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
 * Created by go22670 on 10/19/15.
 */
public class AnalysisPlot extends TimeSeriesPlot {
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
  private final long userid;
  private final LangTestDatabaseAsync service;
  private final PlayAudio playAudio;
  private Map<Long, String> granToLabel;

  private final Map<Series, Boolean> seriesToVisible = new HashMap<>();
  private Map<Long, List<PhoneSession>> granularityToSessions;
  private final Map<Long, Series> granToError = new HashMap<>();
  private final Map<Long, Series> granToAverage = new HashMap<>();
  private final List<Series> detailSeries = new ArrayList<>();

  private Chart chart = null;
  private AnalysisTab.TIME_HORIZON timeHorizon;
  private long lastTime;
  private long firstTime;
  private final List<Long> weeks = new ArrayList<>();
  private final List<Long> months = new ArrayList<>();
  // private final Icon playFeedback;

  /**
   * @param service
   * @param userid
   * @param userChosenID
   * @param minRecordings
   * @see AnalysisTab#AnalysisTab
   */
  public AnalysisPlot(LangTestDatabaseAsync service, long userid, final String userChosenID, final int minRecordings,
                      SoundManagerAPI soundManagerAPI, Icon playFeedback) {
    getElement().setId("AnalysisPlot");
    int minHeight = isShort() ? CHART_HEIGHT_SHORT : CHART_HEIGHT;

    getElement().getStyle().setProperty("minHeight", minHeight, Style.Unit.PX);
    getElement().getStyle().setMargin(10, Style.Unit.PX);
    addStyleName("cardBorderShadow");

    this.service = service;
    this.userid = userid;
    //  this.playFeedback = playFeedback;
    populateGranToLabel();

    this.playAudio = new PlayAudio(service, new SoundPlayer(soundManagerAPI), playFeedback);
    populateExerciseMap(service, (int) userid);

    getPerformanceForUser(service, userid, userChosenID, minRecordings);
  }

  private void populateGranToLabel() {
    this.granToLabel = new HashMap<Long, String>();
    granToLabel.put(HOUR, "Hour");
    granToLabel.put(QUARTER, "6 Hours");
    granToLabel.put(DAY, "Day");
    granToLabel.put(WEEK, "Week");
    granToLabel.put(MONTH, "Month");
    granToLabel.put(YEAR, "Year");
    granToLabel.put(FIVEMIN, "Minute");
  }

  private void getPerformanceForUser(LangTestDatabaseAsync service, long userid,
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
    SortedSet<Long> months2 = new TreeSet<Long>();
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
    //   listeners.clear();
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
            String name = series.getName();
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
    service.getExerciseIds(1, new HashMap<String, Collection<String>>(), "", -1,
        userid, "", false, false, false, false, new AsyncCallback<ExerciseListWrapper<CommonShell>>() {
          @Override
          public void onFailure(Throwable throwable) {
            logger.warning("\n\n\n-> getExerciseIds " + throwable);
          }

          @Override
          public void onSuccess(ExerciseListWrapper<CommonShell> exerciseListWrapper) {
            for (CommonShell shell : exerciseListWrapper.getExercises()) {
              getIdToEx().put(shell.getID(), shell);
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

    TimeAndScore first = rawBestScores.get(0);
    TimeAndScore last = rawBestScores.get(rawBestScores.size() - 1);

    long diff = last.getTimestamp() - first.getTimestamp();
    boolean shortPeriod = diff < QUARTER;
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

  private void addErrorBars(UserPerformance userPerformance, Chart chart) {
    granToError.clear();
    granularityToSessions = userPerformance.getGranularityToSessions();

    List<Long> grans = new ArrayList<>(granularityToSessions.keySet());
    Collections.sort(grans);
    for (Long gran : grans) {
      String label = granToLabel.get(gran);
      //logger.info("Adding for " + label);
      List<PhoneSession> phoneSessions = granularityToSessions.get(gran);
      Series series = addErrorBarSeries(phoneSessions, chart, label, true);
      granToError.put(gran, series);

      Series avgSeries = addMeans(phoneSessions, chart, label, true);
      granToAverage.put(gran, avgSeries);
    }

    long now = System.currentTimeMillis();
    setVisibility(now - YEARS, now);
  }

  /**
   * @param start
   * @param end
   * @see #addErrorBars(UserPerformance, Chart)
   * @see #gotExtremes(AxisSetExtremesEvent)
   */
  private void setVisibility(long start, long end) {
  //  logger.info("setVisibility from " + start + "/" + new Date(start) + " - " + new Date(end));
    List<Long> grans = new ArrayList<>(granularityToSessions.keySet());

    Collections.sort(grans);
    boolean oneSet = false;

    Series errorSeries = null;
    Series averageSeries = null;

    for (Long gran : grans) {
      Series series = granToError.get(gran);
      seriesToVisible.put(series, false);
      Series avgSeries = granToAverage.get(gran);
      seriesToVisible.put(avgSeries, false);

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
            if (session.getCount() > 50) anyBigger = true;
          }
        }
        //       String label = granToLabel.get(gran);
//        String seriesInfo = gran + "/" + label;
        // logger.info("setVisibility  " + seriesInfo + " : " + size + " sessions " + phoneSessions.size() + " any bigger " + anyBigger);

        if (PhoneSession.chooseThisSize(size, total, anyBigger)) {
          oneSet = true;
          errorSeries = series;
          averageSeries = avgSeries;
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

  private void showErrorBarsOrDetail(boolean oneSet, Series errorSeries, Series averageSeries) {
    for (Series series : detailSeries) seriesToVisible.put(series, false);
    if (!oneSet) {
      for (Series series : detailSeries) {
        seriesToVisible.put(series, true);
      }

      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
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
   * @paramx gameTimeSeconds
   * @see #getChart
   */
  private void addSeries(List<TimeAndScore> yValuesForUser,
                         List<TimeAndScore> iPadData,
                         List<TimeAndScore> learnData,
                         List<TimeAndScore> avpData,
                         Chart chart,
                         String seriesTitle, boolean visible) {
    addCumulativeAverage(yValuesForUser, chart, seriesTitle, visible);

    addDeviceData(iPadData, chart, visible);
    addBrowserData(learnData, chart, false, visible);
    addBrowserData(avpData, chart, true, visible);
  }

  private void addCumulativeAverage(List<TimeAndScore> yValuesForUser, Chart chart, String seriesTitle, boolean isVisible) {
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

  private void addDeviceData(List<TimeAndScore> iPadData, Chart chart, boolean isVisible) {
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

  private void addBrowserData(List<TimeAndScore> browserData, Chart chart, boolean isAVP, boolean isVisible) {
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
        .setType(Axis.Type.DATE_TIME).setAxisSetExtremesEventHandler(new AxisSetExtremesEventHandler() {
      @Override
      public boolean onSetExtremes(AxisSetExtremesEvent axisSetExtremesEvent) {
        if (axisSetExtremesEvent != null) {
          gotExtremes(axisSetExtremesEvent);
        } else {
          logger.warning("got null event");
        }
        return true;
      }
    });

    //   int clientHeight = Window.getClientHeight();
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
      logger.warning("Got " +e);
    }
  }

  /**
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
      service.getShells(toGet, new AsyncCallback<List<CommonShell>>() {
        @Override
        public void onFailure(Throwable throwable) {
        }

        @Override
        public void onSuccess(List<CommonShell> commonShells) {
          for (CommonShell shell : commonShells) idToEx.put(shell.getID(), shell);
        }
      });
    }
  }

  /**
   * @return
   * @see WordContainer#getShell(String)
   */
  public Map<String, CommonShell> getIdToEx() {
    return idToEx;
  }

  /**
   * @see AnalysisTab#getClickHandler(AnalysisTab.TIME_HORIZON)
   * @param timeHorizon
   * @return
   */
  public long setTimeHorizon(AnalysisTab.TIME_HORIZON timeHorizon) {
    this.timeHorizon = timeHorizon;
    Long x = goToLast(timeHorizon);
    if (x != null) return x;
    return 0;
  }

  private int index = 0;

  private TimeWidgets timeWidgets;

  private Long goToLast(AnalysisTab.TIME_HORIZON timeHorizon) {
    this.timeHorizon = timeHorizon;

    switch (timeHorizon) {
      case WEEK:
        long from = lastTime - WEEK;
        chart.getXAxis().setExtremes(from, lastTime);

        Long aLong = weeks.get(weeks.size() - 1);
        index = weeks.size() - 1;

        timeWidgets.prevButton.setEnabled(weeks.size() > 1);
        timeWidgets.nextButton.setEnabled(false);
        timeWidgets.display.setText(getShortDate(aLong));
        timeChanged(from, lastTime);
        return from;
      case MONTH:
        long from2 = lastTime - MONTH;
        chart.getXAxis().setExtremes(from2, lastTime);

        Long aLong2 = months.get(months.size() - 1);

        index = months.size() - 1;

        timeWidgets.prevButton.setEnabled(months.size() > 1);
        timeWidgets.nextButton.setEnabled(false);
        timeWidgets.display.setText(getShortDate(aLong2));

        timeChanged(from2, lastTime);

        return from2;
      case ALL:
        chart.getXAxis().setExtremes(firstTime, lastTime);
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
//
//  public TimeWidgets getTimeWidgets() {
//    return timeWidgets;
//  }

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
