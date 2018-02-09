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
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.exercise.ExceptionSupport;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import org.jetbrains.annotations.NotNull;
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.events.AxisSetExtremesEvent;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/19/15.
 */
public class AnalysisPlot extends BasicTimeSeriesPlot implements ExerciseLookup {
  private final Logger logger = Logger.getLogger("AnalysisPlot");

  private final Map<Long, Series> granToAverage = new HashMap<>();
  protected final int userid;

  private static final String SCORE_SUFFIX = " pronunciation score (Drag to zoom in, click to hear)";
  private static final int TIME_SLACK = 60;
  private static final int WIDTH = 740;

  private static final int MIN_SESSION_COUNT = 50;

  private static final String NO_RECORDINGS_YET = "No recordings yet to analyze. Please record yourself.";
  private static final String NO_RECORDINGS_YET_FOR_STUDENT = "No recordings yet made by student to analyze.";
  private static final String NO_RECORDINGS_YET_ON_LIST = "No recordings yet for this list. Choose another list or don't filter on lists.";
  private static final String NO_RECORDINGS_YET_FOR_STUDENT_ON_LIST = "No recordings yet for this list by this student. Choose another list or student or don't filter on lists.";

  private static final long MINUTE = 60 * 1000;
  static final long HOUR = 60 * MINUTE;
  private static final long QUARTER = 6 * HOUR;

  //  private static final long FIVEMIN = 5 * MINUTE;
  private static final long ONEMIN = MINUTE;
  private static final long TENMIN = 10 * MINUTE;
  private static final long DAY = 24 * HOUR;
  private static final long WEEK = 7 * DAY;
  private static final long MONTH = 4 * WEEK;
  private static final long YEAR = 52 * WEEK;
  private static final long YEARS = 20 * YEAR;

  private static final int SHORT_THRESHOLD = 822;
  private static final int CHART_HEIGHT_SHORT = 260;
  private static final int CHART_HEIGHT = 330;

  private final AnalysisServiceAsync service;
  private final PlayAudio playAudio;
  private Map<Long, String> granToLabel;

  private final Map<Series, Boolean> seriesToVisible = new HashMap<>();
  private Map<Long, List<PhoneSession>> granularityToSessions;
  private final Map<Long, Series> granToError = new HashMap<>();
  private final List<Series> detailSeries = new ArrayList<>();

  private AnalysisTab.TIME_HORIZON timeHorizon;
  /**
   * @see #setRawBestScores(List)
   */
  private long firstTime;
  private long lastTime;

  private final List<Long> oneMinutes = new ArrayList<>();
  private final List<Long> tenMinutes = new ArrayList<>();

  /**
   *
   */
  private final List<Long> weeks = new ArrayList<>();
  private final List<Long> months = new ArrayList<>();

  private int index = 0;

  private TimeWidgets timeWidgets;
  private MessageHelper messageHelper;
  private int width = 1365;
  protected Chart chart = null;
  private boolean isPolyglot;
  private SortedSet<TimeAndScore> rawBestScores;
  private float possible;

  /**
   * @param service
   * @param userid        either for yourself if you're a student or a selected student if you're a teacher
   * @param isTeacherView
   * @param possible
   * @see AnalysisTab#AnalysisTab
   * @see #setRawBestScores
   */
  public AnalysisPlot(ExerciseServiceAsync service,
                      int userid,
                      SoundManagerAPI soundManagerAPI,
                      Icon playFeedback,
                      ExceptionSupport exceptionSupport,
                      MessageHelper messageHelper,
                      boolean isTeacherView,
                      boolean isPolyglot, int possible) {
    super(exceptionSupport);
    this.userid = userid;
    this.messageHelper = messageHelper;
    this.isPolyglot = isPolyglot;
    if (isTeacherView) {
      width = WIDTH;
    }

    this.possible = (float) possible;

    if (!isPolyglot) {
      setWidth(width + "px");
    }
    {
//      int minHeight = isShort() ? CHART_HEIGHT_SHORT : CHART_HEIGHT;
      getElement().setId("AnalysisPlot");
      Style style = getElement().getStyle();
//      style.setProperty("minHeight", minHeight, Style.Unit.PX);
//      style.setProperty("minWidth", 300, Style.Unit.PX);
      style.setMarginTop(10, Style.Unit.PX);
      style.setMarginLeft(10, Style.Unit.PX);
      style.setMarginBottom(10, Style.Unit.PX);
      addStyleName("cardBorderShadow");
    }

    /**
     * setRawBestScores
     */
    this.service = GWT.create(AnalysisService.class);
    populateGranToLabel();

    this.playAudio = new PlayAudio(new SoundPlayer(soundManagerAPI), playFeedback, service, exceptionSupport);
  }

  private void populateGranToLabel() {
    this.granToLabel = new HashMap<>();
    granToLabel.put(HOUR, "Hour");
    granToLabel.put(QUARTER, "6 Hours");
    granToLabel.put(DAY, "Day");
    granToLabel.put(WEEK, "Week");
    granToLabel.put(MONTH, "Month");
    granToLabel.put(YEAR, "Year");
    granToLabel.put(TENMIN, "Ten Minutes");
    granToLabel.put(ONEMIN, "Minute");
  }

  /**
   * @param userPerformance
   * @param userChosenID
   * @param listid
   * @param isTeacherView
   * @see AnalysisTab#useReport
   */
  void showUserPerformance(UserPerformance userPerformance, String userChosenID, int listid, boolean isTeacherView) {
    List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
    this.rawBestScores = new TreeSet<>(rawBestScores);
    //  logger.info("showUserPerformance scores = " + rawBestScores.size());

    if (!rawBestScores.isEmpty()) {
      long last = rawBestScores.get(rawBestScores.size() - 1).getTimestamp();

      if (isPolyglot) {
        tenMinutes.addAll(getPeriods(userPerformance.getGranularityToSessions().get(TENMIN), TENMIN, last));
        oneMinutes.addAll(getPeriods(userPerformance.getGranularityToSessions().get(ONEMIN), ONEMIN, last));
        List<PhoneSession> phoneSessions = userPerformance.getGranularityToSessions().get(-1L);
        logger.info("got sessions "+ phoneSessions);
      }
      {
        List<PhoneSession> phoneSessions = userPerformance.getGranularityToSessions().get(WEEK);
        weeks.addAll(getPeriods(phoneSessions, WEEK, last));
      }
      {
        List<PhoneSession> phoneSessions1 = userPerformance.getGranularityToSessions().get(MONTH);
        months.addAll(getPeriods(phoneSessions1, MONTH, last));
      }
    }
    addChart(userPerformance, userChosenID, listid != -1, isTeacherView);
  }

  private String getScoreText(SortedSet<TimeAndScore> simpleTimeAndScores) {
    int fround1 = getPercentScore(simpleTimeAndScores);
//        logger.info("fround1 " + fround1);

    String text = "Score is " + fround1 + "%";
    return text;
    // timeWidgets.setScore(text);
//    Heading child = new Heading(2, text);
//    child.addStyleName("topFiveMargin");
    //return child;
  }

  private int getPercentScore(SortedSet<TimeAndScore> simpleTimeAndScores) {
    float totalScore = 0f;
    int total = 0;
    //float possible = 0f;
    //  SortedSet<TimeAndScore> simpleTimeAndScores = getTimeAndScoresInRange(start, end);
   // logger.info("getScoreText : found " + simpleTimeAndScores.size());
    for (TimeAndScore exerciseCorrectAndScore : simpleTimeAndScores) {
      float score = exerciseCorrectAndScore.getScore();
      if (score > 0) {
        totalScore += score;
        total++;
      }
      //possible += 1f;
    }

    //  logger.info("total " + totalScore);
    // logger.info("possible " + possible);
    float v = totalScore / possible;
    // logger.info("ratio " + v);
    float fround = Math.round(v * 100);
    // logger.info("fround " + fround);

    return (int) (fround);
  }

  private SortedSet<TimeAndScore> getTimeAndScoresInRange(long start, long end) {
    return start == -1 ? rawBestScores : rawBestScores.subSet(new TimeAndScore(start), new TimeAndScore(end));
  }

  /**
   * @param phoneSessions
   * @param granularity
   * @param lastTime      going backwards from the last recording
   * @return
   * @see #showUserPerformance
   */
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
  /*      logger.info("getPeriods session " + sessionWindow + " vs" + window +
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

  /**
   * @param userPerformance
   * @param userChosenID
   * @param isTeacherView
   * @see #showUserPerformance
   */
  private void addChart(UserPerformance userPerformance, String userChosenID, boolean filterOnList, boolean isTeacherView) {
    clear();

    List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();

    if (rawBestScores.isEmpty()) {
      String text = filterOnList ?
          (isTeacherView ? NO_RECORDINGS_YET_FOR_STUDENT_ON_LIST : NO_RECORDINGS_YET_ON_LIST) :
          (isTeacherView ? NO_RECORDINGS_YET_FOR_STUDENT : NO_RECORDINGS_YET);

      add(new Label(text));
    } else {
      chart = getChart("<b>" + userChosenID + " " + getUserName(userPerformance) + "</b>" + SCORE_SUFFIX,
          getSubtitle(userPerformance, rawBestScores),
          CUMULATIVE_AVERAGE,
          userPerformance);
      add(chart);
    }
    setRawBestScores(rawBestScores);
    showSeriesByVisible();

    if (isPolyglot) {
      setTimeHorizon(possible < 50 ? AnalysisTab.TIME_HORIZON.ONEMIN : AnalysisTab.TIME_HORIZON.TENMIN);
    }
  }

  @NotNull
  private String getSubtitle(UserPerformance userPerformance, List<TimeAndScore> rawBestScores) {
    float percentAvg = userPerformance.getRawAverage() * 100;
    int rawTotal = rawBestScores.size();
    //  return "Score and average : " + rawTotal + " items, avg " + (int) percentAvg + " %";
    return getChartSubtitle((int) percentAvg, rawTotal);
  }

  @NotNull
  private String getChartSubtitle(int percentAvg, int rawTotal) {
    return "Average : " + percentAvg + "% for " + rawTotal + " items";
  }

  @NotNull
  private String getUserName(UserPerformance userPerformance) {
    String first = userPerformance.getFirst();
    String last = userPerformance.getLast();
    return (first == null || first.isEmpty()) && (last == null || last.isEmpty()) ? "" :
        " (" + first + " " + last + ")";
  }

  /**
   *
   */
  private void showSeriesByVisible() {
    Scheduler.get().scheduleDeferred(() -> {
      if (chart != null) {
        //logger.info("showSeriesByVisible : doing deferred ---------- ");
        seriesToVisible.forEach((series, shouldShow) -> {
          if (shouldShow) {
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
        });
      }
    });
  }

  /**
   * Only get exercises this person has practiced.
   * Use this to get info for tooltips, etc.
   * Too heavy?
   * Why not get that info on demand?
   *
   * @param service
   * @param userid
   * @see AnalysisTab#useReport
   */
  public void populateExerciseMap(ExerciseServiceAsync service, int userid) {
    // logger.info("populateExerciseMap : get exercises for user " + userid);
    service.getExerciseIds(
        new ExerciseListRequest(1, userid)
            .setOnlyForUser(true),
        new AsyncCallback<ExerciseListWrapper<CommonShell>>() {
          @Override
          public void onFailure(Throwable throwable) {
            logger.warning("\n\n\n-> getExerciseIds " + throwable);
            messageHelper.handleNonFatalError("problem getting exercise ids", throwable);
          }

          @Override
          public void onSuccess(ExerciseListWrapper<CommonShell> exerciseListWrapper) {
            if (exerciseListWrapper != null && exerciseListWrapper.getExercises() != null) {
              //       Map<Integer, CommonShell> idToEx = getIdToEx();
//              logger.info("populateExerciseMap : got back " + exerciseListWrapper.getExercises().size() +
//                  "  exercises for user " + userid);
              exerciseListWrapper.getExercises().forEach(commonShell -> rememberExercise(commonShell));
            }
          }
        });
  }

  /**
   * @param title
   * @param subtitle
   * @param seriesName
   * @param userPerformance
   * @return
   * @see #addChart
   */
  private Chart getChart(String title, String subtitle, String seriesName, UserPerformance userPerformance) {
    final Chart chart = getHighchartChart(title, true);

    Highcharts.setOptions(
        new Highcharts.Options().setGlobal(
            new Global()
                .setUseUTC(false)
        ));


    List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();

    addSeries(rawBestScores,
        userPerformance.getiPadTimeAndScores(),
        userPerformance.getLearnTimeAndScores(),
        userPerformance.getAvpTimeAndScores(),
        chart,
        seriesName,
        isShortPeriod(rawBestScores));

    addErrorBars(userPerformance, chart);
    configureChart(chart, subtitle);
    return chart;
  }

  private boolean isShortPeriod(List<TimeAndScore> rawBestScores) {
    if (rawBestScores.size() < 2) {
      logger.info("only " + rawBestScores.size() + " scores");
      return true;
    }
    TimeAndScore first = rawBestScores.get(0);
    TimeAndScore last = rawBestScores.get(rawBestScores.size() - 1);

    long diff = last.getTimestamp() - first.getTimestamp();
    boolean b = diff < QUARTER;

/*    if (b) {
      logger.info("time span is < 6 hours " + diff);
    }*/
    return b;
  }

  /**
   * @param userPerformance
   * @param chart
   * @see #getChart(String, String, String, UserPerformance)
   */
  private void addErrorBars(UserPerformance userPerformance, Chart chart) {
    granToError.clear();
    granularityToSessions = userPerformance.getGranularityToSessions();

    {
      for (Long gran : getSortedGranularities()) {
        String label = granToLabel.get(gran);
        List<PhoneSession> phoneSessions = granularityToSessions.get(gran);
        granToError.put(gran, addErrorBarSeries(phoneSessions, chart, label, true));
        Series value = addMeans(phoneSessions, chart, label, true);
        //    logger.info("addErrorBars Adding for " + label + " = " + value.getName() + " points " + value.getPoints().length + " from " + phoneSessions.size() + " sessions");
        granToAverage.put(gran, value);
      }
    }

    long now = System.currentTimeMillis();
//    logger.info("setVisiblity for " + new Date(now));
    setVisibility(now - YEARS, now);
  }

  @NotNull
  private List<Long> getSortedGranularities() {
    List<Long> grans = new ArrayList<>(getGranularities());
    Collections.sort(grans);
    return grans;
  }

  /**
   * Choose which granularity of data to show - hours, weeks, month, etc.
   * <p>
   * This does overlap...
   *
   * @param start
   * @param end
   * @see #addErrorBars(UserPerformance, Chart)
   * @see #gotExtremes(AxisSetExtremesEvent)
   */
  private void setVisibility(long start, long end) {
    //  logger.info("setVisibility from " + start + "/" + new Date(start) + " - " + new Date(end));
    Series errorSeries = null;
    Series averageSeries = null;

    hideAllSeries();

    boolean oneSet = false;
    for (Long gran : getSortedGranularities()) {
      if (!oneSet) {
        List<PhoneSession> phoneSessions = granularityToSessions.get(gran);

        int size = 0;
        int total = 0;
        boolean anyBigger = false;
        for (PhoneSession session : phoneSessions) {
          if (session.getStart() >= start && session.getEnd() < end) {
            // logger.info("granularity " + gran + " session " + session);
            size++;
            total += session.getCount();
            if (session.getCount() > MIN_SESSION_COUNT) anyBigger = true;
          }
        }

        // String label = granToLabel.get(gran);
        //String seriesInfo = gran + "/" + label;
        //   logger.info("setVisibility  " + seriesInfo + " : " + size + " sessions " + phoneSessions.size() + " any bigger " + anyBigger);
        if (PhoneSession.chooseThisSize(size, total, anyBigger)) {
          oneSet = true;
          errorSeries = granToError.get(gran);
          averageSeries = granToAverage.get(gran);
          setPhoneSessions(granularityToSessions.get(gran));
          //  logger.info("setVisibility 1 chose " + seriesInfo + " : " + size);// + " visible " + series.isVisible());
        }
        //else {
        //logger.info("setVisibility 2 too small " + seriesInfo + " : " + size);
        //}
      }
    }

    showErrorBarsOrDetail(oneSet, errorSeries, averageSeries);
  }

  private void hideAllSeries() {
    getGranularities().forEach(gran -> {
      seriesToVisible.put(granToError.get(gran), false);
      seriesToVisible.put(granToAverage.get(gran), false);
    });
  }

  @NotNull
  private Set<Long> getGranularities() {
    return granularityToSessions.keySet();
  }

  private void showErrorBarsOrDetail(boolean oneSet, Series errorSeries, Series averageSeries) {
    if (oneSet) {
      detailSeries.forEach(series -> seriesToVisible.put(series, false));
      seriesToVisible.put(errorSeries, true);
      seriesToVisible.put(averageSeries, true);
    } else {
      detailSeries.forEach(series -> seriesToVisible.put(series, true));
      Scheduler.get().scheduleDeferred(this::showEachSeries);
    }
  }

  private void showEachSeries() {
    detailSeries.forEach(series -> series.setVisible(true, false));
    chart.redraw();
  }

  /**
   * TODO: remove distinction between learn vs avp
   *
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

    recordVisible(isVisible, getSplineSeries(chart, seriesTitle, data));
  }

  private void recordVisible(boolean isVisible, Series series) {
    seriesToVisible.put(series, isVisible);
    detailSeries.add(series);
  }

  private void addDeviceData(Collection<TimeAndScore> iPadData, Chart chart, boolean isVisible) {
    if (!iPadData.isEmpty()) {
      Number[][] data = getDataForTimeAndScore(iPadData);
      Series series = chart.createSeries()
          .setName(I_PAD_I_PHONE)
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
      recordVisible(isVisible, getScatterSeries(chart, data, prefix));
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
    configureYAxis(chart, title);

    chart.getXAxis()
        .setStartOnTick(true)
        .setEndOnTick(true)
        .setType(Axis.Type.DATE_TIME)
        .setAxisSetExtremesEventHandler(axisSetExtremesEvent -> {
          if (axisSetExtremesEvent != null) {
            //  logger.info("configureChart window " + new Date(firstTime) + " " + new Date(lastTime));
            gotExtremes(axisSetExtremesEvent);
          }
          return true;
        });

    int chartHeight = isShort() ? CHART_HEIGHT_SHORT : CHART_HEIGHT;
    chart.setHeight(chartHeight + "px");
//    chart.setWidth(width + // 1378
//        "px");
  }

  private boolean isShort() {
    return Window.getClientHeight() < SHORT_THRESHOLD;
  }

  private final DateTimeFormat shortFormat = DateTimeFormat.getFormat("MMM d, yy");

  /**
   * So when the x axis range changes, we get an event here.
   *
   * @param axisSetExtremesEvent
   * @see #configureChart
   */
  private void gotExtremes(AxisSetExtremesEvent axisSetExtremesEvent) {
    detailSeries.forEach(series -> series.setVisible(false, false));

    try {
      Number min = axisSetExtremesEvent.getMin();
      Number max = axisSetExtremesEvent.getMax();

//      logger.info("gotExtremes got min " + min + " max " + max);
      if (min != null && min.longValue() > 0) {
        long start = min.longValue();
        long end = max.longValue();
        //      logger.info("gotExtremes now min " + getFormat(start) + " max " + getFormat(end));
        setVisibility(start, end);
        timeChanged(start, end);
      } else {
        long end = System.currentTimeMillis();
        setVisibility(end - YEARS, end);
        setTimeWindowControlsToAll();
      }
      showSeriesByVisible();

    } catch (Exception e) {
      logger.warning("gotExtremes : got " + e);
      exceptionSupport.logException(e);
    }
  }

  /**
   * Remember time window of data (x-axis).
   * <p>
   * TODO : When would we ever need to go talk to the server to get the exercises?
   * <p>
   * TODO : audio exaample table needs to show fl/english for items... somehow we're getting them here...
   *
   * @param rawBestScores
   * @see #addChart
   */
  private void setRawBestScores(List<TimeAndScore> rawBestScores) {
    Set<Integer> toGet = new HashSet<>();

    for (TimeAndScore timeAndScore : rawBestScores) {
      Integer id = timeAndScore.getExid();
      addTimeToExID(timeAndScore.getTimestamp(), id);
      toGet.add(id);
    }
/*
    if (toGet.isEmpty()) {
      // logger.info("setRawBestScores got # raw best  " + rawBestScores.size() + " idToEx # = " + idToEx.size() + " - yielded none?");
    }
*/

    if (!rawBestScores.isEmpty()) {
      setTimeRange(rawBestScores);

      if (!toGet.isEmpty()) {
        logger.info("setRawBestScores is firstTime " + new Date(firstTime) + " - " + new Date(lastTime) + " getting " + toGet.size());
        service.getShells(toGet, new AsyncCallback<List<CommonShell>>() {
          @Override
          public void onFailure(Throwable throwable) {
            messageHelper.handleNonFatalError("problem getting exercise shells", throwable);
          }

          @Override
          public void onSuccess(List<CommonShell> commonShells) {
            commonShells.forEach(commonShell -> rememberExercise(commonShell));
            // logger.info("setRawBestScores getShells got " + commonShells.size());
          }
        });
      }
    } //else {
    //logger.info("rawBest is empty?");
    //}
  }

  private void setTimeRange(List<TimeAndScore> rawBestScores) {
    this.firstTime = rawBestScores.get(0).getTimestamp();
    this.lastTime = rawBestScores.get(rawBestScores.size() - 1).getTimestamp();
  }

  /**
   * @param timeHorizon
   * @return
   * @see AnalysisTab#getClickHandler(AnalysisTab.TIME_HORIZON)
   */
  long setTimeHorizon(AnalysisTab.TIME_HORIZON timeHorizon) {
    if (possible < 50 && timeHorizon == AnalysisTab.TIME_HORIZON.TENMIN) {
      timeHorizon = AnalysisTab.TIME_HORIZON.ONEMIN;
    }
    this.timeHorizon = timeHorizon;
    Long x = goToLast(timeHorizon);
    return (x != null) ? x : 0;
  }

  /**
   * @param timeHorizon
   * @return
   * @see #setTimeHorizon(AnalysisTab.TIME_HORIZON)
   */
  private Long goToLast(AnalysisTab.TIME_HORIZON timeHorizon) {
    // logger.info("goToLast set time from " + new Date(firstTime) + " to " + new Date(lastTime));
    XAxis xAxis;
    try {
      xAxis = chart.getXAxis();
    } catch (Exception e) {
      // somehow this is happening
      return 0L;
    }

    long lastPlusSlack = lastTime + TIME_SLACK;
    switch (timeHorizon) {
      case WEEK:
        return showLastWeek(xAxis, lastPlusSlack);
      case MONTH:
        return showLastMonth(xAxis, lastPlusSlack);
      case TENMIN:
        return showLastPeriod(xAxis, lastPlusSlack, TENMIN, this.tenMinutes);
      case ONEMIN:
        return showLastPeriod(xAxis, lastPlusSlack, ONEMIN, this.oneMinutes);
      case ALL:
        return showAll(xAxis, lastPlusSlack);
    }
    return null;
  }

  /**
   * logger.info("from lastMonth        " + getShortDate(lastMonth));
   * logger.info("from startOfPrevMonth " + getShortDate(startOfPrevMonth));
   * logger.info("to lastTime           " + getShortDate(lastTime));
   **/
  private long showLastWeek(XAxis xAxis, long lastPlusSlack) {
    return showLastPeriod(xAxis, lastPlusSlack, WEEK, this.weeks);
  }

  @NotNull
  private Long showLastMonth(XAxis xAxis, long lastPlusSlack) {
    return showLastPeriod(xAxis, lastPlusSlack, MONTH, this.months);
  }

  @NotNull
  private Long showLastPeriod(XAxis xAxis, long lastPlusSlack, long offset, List<Long> timePeriods) {
    long startOfPrevMonth = lastTime - offset;
    xAxis.setExtremes(startOfPrevMonth, lastPlusSlack);

    int lastMonthIndex = timePeriods.size() - 1;
    Long lastMonth = timePeriods.get(lastMonthIndex);
    this.index = lastMonthIndex;

    timeWidgets.prevButton.setEnabled(timePeriods.size() > 1);
    timeWidgets.nextButton.setEnabled(false);

    timeWidgets.setDisplay(getShortDate(lastMonth, shouldShowHour(offset)));
    setTitleScore(startOfPrevMonth, lastPlusSlack);

    timeChanged(startOfPrevMonth, lastPlusSlack);

    return startOfPrevMonth;
  }

  /**
   * TODO: Good idea to mess with time window with slack?
   *
   * @param xAxis
   * @param lastPlusSlack
   * @return
   */
  @NotNull
  private Long showAll(XAxis xAxis, long lastPlusSlack) {
    long min = firstTime - TIME_SLACK;
    xAxis.setExtremes(min, lastPlusSlack);
    setTimeWindowControlsToAll();
    timeChanged(min, lastPlusSlack);

    return firstTime;
  }

  private void setTimeWindowControlsToAll() {
    timeWidgets.prevButton.setEnabled(false);
    timeWidgets.nextButton.setEnabled(false);
    timeWidgets.setDisplay("");
    setTitleScore(-1, -1);
    timeWidgets.reset();
    timeChanged(firstTime, lastTime);
  }

  /**
   * @see AnalysisTab#getPrevButton
   */
  void gotPrevClick() {
    index--;

    timeWidgets.prevButton.setEnabled(index > 0);
    timeWidgets.nextButton.setEnabled(true);

    showTimePeriod(getOffset(), getPeriods());
  }

  /**
   * @see AnalysisTab#getNextButton
   */
  void gotNextClick() {
    List<Long> periods = getPeriods();

    index++;

    timeWidgets.prevButton.setEnabled(true);
    timeWidgets.nextButton.setEnabled(index < periods.size() - 1);

    showTimePeriod(getOffset(), periods);
  }

  private long getOffset() {
    switch (timeHorizon) {
      case ONEMIN:
        return ONEMIN;
      case TENMIN:
        return TENMIN;
      case WEEK:
        return WEEK;
      case MONTH:
        return MONTH;
      default:
        return MONTH;
    }

  }

  @NotNull
  private List<Long> getPeriods() {
    switch (timeHorizon) {
      case ONEMIN:
        return oneMinutes;
      case TENMIN:
        return tenMinutes;
      case WEEK:
        return weeks;
      case MONTH:
        return months;
      default:
        return months;
    }
  }

  /**
   * @param offset
   * @param periods
   * @see #gotNextClick
   * @see #gotPrevClick
   */
  private void showTimePeriod(long offset, List<Long> periods) {
    Long periodStart = periods.get(index);
    timeWidgets.setDisplay(getShortDate(periodStart, shouldShowHour(offset)));
    long end = periodStart + offset;
    //  logger.info("showTimePeriod From  " + getShortDate(periodStart));
    //  logger.info("showTimePeriod to    " + getShortDate(end));
    // logger.info("showTimePeriod offset    " + offset);
    chart.getXAxis().setExtremes(periodStart, end);

    setTitleScore(periodStart, end);

    timeChanged(periodStart, end);
  }

  private boolean shouldShowHour(long offset) {
    return offset <= TENMIN;
  }

  /**
   * @param timeWidgets
   * @see AnalysisTab#AnalysisTab
   */
  void setTimeWidgets(TimeWidgets timeWidgets) {
    this.timeWidgets = timeWidgets;
  }

  private final Set<TimeChangeListener> listeners = new HashSet<>();

  /**
   * @param listener
   * @see AnalysisTab#getPhoneReport(PhoneReport, ExerciseController, Panel, AnalysisPlot, ShowTab)
   */
  void addListener(TimeChangeListener listener) {
    listeners.add(listener);
  }

  /**
   * @param from
   * @param to
   * @see #gotExtremes
   */
  private void timeChanged(long from, long to) {
    listeners.forEach(timeChangeListener -> timeChangeListener.timeChanged(from, to));
    setTitleScore(from, to);
  }

  private void setTitleScore(long from, long to) {
    SortedSet<TimeAndScore> timeAndScoresInRange = getTimeAndScoresInRange(from, to);
    timeWidgets.setScore(getScoreText(timeAndScoresInRange));
    setYAxisTitle(chart, getChartSubtitle(getPercentScore(timeAndScoresInRange), timeAndScoresInRange.size()));
  }

  public interface TimeChangeListener {
    /**
     * @param from
     * @param to
     * @see #timeChanged
     */
    void timeChanged(long from, long to);
  }

  protected String getTooltip(ToolTipData toolTipData, Integer exid, CommonShell commonShell) {
    // logger.info("getTooltip for " + exid + " series " + toolTipData.getSeriesName());
    String seriesName = toolTipData.getSeriesName();

    if (granToLabel.values().contains(seriesName)) {
      Series series = chart.getSeries(toolTipData.getSeriesId());

      if (granToAverage.values().contains(series)) {
        return getAvgTooltip(toolTipData, seriesName);
      } else {
        return getErrorBarToolTip(toolTipData, seriesName);
      }
    } else {
      //else {
      //logger.info("getTooltip series is " + seriesName + " not in " + values);
      // }
      return super.getTooltip(toolTipData, exid, commonShell);
    }
  }

  protected void gotClickOnExercise(int exid, long nearestXAsLong) {
    playAudio.playLast(userid, exid, nearestXAsLong);
  }
}
