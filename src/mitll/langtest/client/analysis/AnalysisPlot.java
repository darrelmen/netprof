package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Label;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.UserPerformance;
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.events.*;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.SeriesPlotOptions;
import scala.tools.nsc.backend.icode.Primitives;

import java.util.*;
import java.util.logging.Logger;

//import org.moxieapps.gwt.highcharts.client.*;

/**
 * Created by go22670 on 10/19/15.
 */
public class AnalysisPlot extends TimeSeriesPlot {
  private final Logger logger = Logger.getLogger("AnalysisPlot");

  private static final long MINUTE = 60 * 1000;
  private static final long HOUR = 60 * MINUTE;
  private static final long QUARTER = 6 * HOUR;

  private static final long FIVEMIN = 5 * MINUTE;
  private static final long DAY = 24 * HOUR;
  private static final long WEEK = 7 * DAY;
  private static final long MONTH = 4 * WEEK;

//  private static final int NARROW_WIDTH = 330;

  private static final String I_PAD_I_PHONE = "iPad/iPhone";
  private static final String VOCAB_PRACTICE = "Vocab Practice";
  private static final String LEARN = "Learn";
  private static final String CUMULATIVE_AVERAGE = "Average";
  Set<String> toShowExercise = new HashSet<>(Arrays.asList(I_PAD_I_PHONE,VOCAB_PRACTICE, LEARN,CUMULATIVE_AVERAGE));

  private static final int CHART_HEIGHT = 330;

  private static final int Y_OFFSET_FOR_LEGEND = 60;

  private final Map<Long, String> timeToId = new TreeMap<>();
  private final Map<String, CommonShell> idToEx = new TreeMap<>();
  private final long userid;
  private final LangTestDatabaseAsync service;
  private final PlayAudio playAudio;
  private final HashMap<Long, String> granToLabel;

  /**
   * @param service
   * @param userid
   * @param userChosenID
   * @param minRecordings
   * @see AnalysisTab#AnalysisTab
   */
  public AnalysisPlot(LangTestDatabaseAsync service, long userid, final String userChosenID, final int minRecordings,
                      SoundManagerAPI soundManagerAPI) {
    getElement().setId("AnalysisPlot");
    //   addStyleName("floatLeft");
    this.service = service;
    this.userid = userid;
    this.granToLabel = new HashMap<Long, String>();
    granToLabel.put(HOUR, "Hour");
    granToLabel.put(QUARTER, "6 Hours");
    granToLabel.put(DAY, "Day");
    granToLabel.put(WEEK, "Week");
    granToLabel.put(MONTH, "Month");
    granToLabel.put(FIVEMIN, "Minute");

    SoundPlayer soundFeedback = new SoundPlayer(soundManagerAPI);
    this.playAudio = new PlayAudio(service, soundFeedback);
    //setHeight("350px");
    populateExerciseMap(service, (int) userid);

    getPerformanceForUser(service, userid, userChosenID, minRecordings);
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
        addChart(userPerformance, userChosenID);
      }
    });
  }

  private void addChart(UserPerformance userPerformance, String userChosenID) {
    clear();
    List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
    float v = userPerformance.getRawAverage() * 100;
    int rawTotal = rawBestScores.size();//userPerformance.getRawTotal();

    Chart chart = null;
    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
//        logger.info("getPerformanceForUser raw total " + rawTotal + " num " + rawBestScores.size());
      String subtitle = "Score and average (" + rawTotal + " items, average " + (int) v + " %)";
      String title = "<b>" + userChosenID + "</b>" + " pronunciation score (Drag to zoom in, click to hear)";
      chart = getChart(title, subtitle, CUMULATIVE_AVERAGE, userPerformance);
//
//      for (Series series : chart.getSeries()) {
//        String name = series.getName();
//        logger.info("initial : series " + name + "/" + series + " : " + series.isVisible());
//
//      }

      add(chart);
    }
    setRawBestScores(rawBestScores);

    final Chart outer = chart;
/*    add(new Button("toggle", new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        if (outer != null) {
          for (Series series : outer.getSeries()) {
            String name = series.getName();
            logger.info("series " + name + "/" + series + " : " + series.isVisible());
            if (name.startsWith("Week")) {
              series.setVisible(!series.isVisible());
            }
          }
        }
      }
    }) {

    });*/

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        if (outer != null) {
          logger.info("doing deferred ---------- ");
//          outer.getXAxis().min();
          for (Series series : outer.getSeries()) {
            String name = series.getName();
            Boolean expected = seriesToVisible.get(series);
        //    logger.info("defer : series " + name + "/" + series + " : " + series.isVisible() + " : " + expected);
            if (expected != null) {
              series.setVisible(expected, false);
            } else {
              logger.info("\t - skipping " + name);
            }
          }

          logger.info("doing redraw ---------- ");

          outer.redraw();
        }
      }
    });

  }

  private void populateExerciseMap(LangTestDatabaseAsync service, int userid) {
    service.getExerciseIds(1, new HashMap<String, Collection<String>>(), "", -1,
        userid, "", false, false, false, false, new AsyncCallback<ExerciseListWrapper>() {
          @Override
          public void onFailure(Throwable throwable) {
            logger.warning("\n\n\n-> getExerciseIds " + throwable);
          }

          @Override
          public void onSuccess(ExerciseListWrapper exerciseListWrapper) {
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

 /*   Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        logger.info("redraw --- ");
        chart.redraw();
      }
    });
*/
    return chart;
  }

  Map<Series, Boolean> seriesToVisible = new HashMap<>();

  private void addErrorBars(UserPerformance userPerformance, Chart chart) {
    Map<Long, List<PhoneSession>> granularityToSessions = userPerformance.getGranularityToSessions();
    Map<Long, Series> granToError = new HashMap<>();
    boolean oneSet = false;
    long granChosen = 0;

    List<Long> grans = new ArrayList<>(granularityToSessions.keySet());
    Collections.sort(grans);
    for (Long gran : grans) {
      String label = granToLabel.get(gran);
      //logger.info("Adding for " + label);
      List<PhoneSession> phoneSessions = granularityToSessions.get(gran);
      Series series = addErrorBarSeries(phoneSessions, chart, label, true);
      seriesToVisible.put(series, false);

      Series avgSeries = addMeans(phoneSessions, chart, "Avg " + label);
      seriesToVisible.put(avgSeries, false);


      int size = phoneSessions.size();
      String seriesInfo = gran + "/" + label;
      if (!oneSet) {
        if (size < 15) {
          oneSet = true;
          granChosen = gran;
          series.setVisible(true);
          seriesToVisible.put(series, true);
          seriesToVisible.put(avgSeries, true);
          logger.info("1 chose " + seriesInfo + " : " + size + " visible " + series.isVisible());
        } else {
          logger.info("2 chose " + seriesInfo + " : " + size);
//          series.setVisible(false, false);
        }
      } else {
        logger.info("3 chose " + seriesInfo + " : " + size);
        //    series.setVisible(false, false);
      }
      granToError.put(gran, series);
    }

    if (granChosen > 0) {
      setRawBestScores2(granularityToSessions.get(granChosen));
    }
  }

  private Chart getChart(String title) {
    return new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        //.setType(Series.Type.SCATTER)
        .setChartTitleText(title)
        //     .setChartSubtitleText(subtitle)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false)
        .setLegend(new Legend()
            .setLayout(Legend.Layout.VERTICAL)
            .setAlign(Legend.Align.LEFT)
            .setVerticalAlign(Legend.VerticalAlign.TOP)
            .setX(100)
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
        ).setSelectionEventHandler(new ChartSelectionEventHandler() {
          @Override
          public boolean onSelection(ChartSelectionEvent selectionEvent) {
            if (selectionEvent != null) {
              logger.info("User selected " + selectionEvent);
//                  "from " + selectionEvent.getXAxisMin() + " to " + selectionEvent.getXAxisMax());
            } else logger.warning("got null selection event");
            return true;
          }
        });
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

              //    logger.info("timeToId " + (timeToId != null));
              //   logger.info("getIdToEx " + (getIdToEx() != null));
              // logger.info("toolTipData " + toolTipData);

              String exerciseID = timeToId.get(toolTipData.getXAsLong());

              ///  logger.info("exerciseID " + exerciseID);

              CommonShell commonShell = exerciseID == null ? null : getIdToEx().get(exerciseID);
              //    logger.info("getTooltip " + commonShell);

              return getTooltip(toolTipData, exerciseID, commonShell);
            } catch (Exception e) {
              logger.warning(e.getMessage());
              e.printStackTrace();
              return "";
            }
          }
        });
  }

//  private DateTimeFormat format = DateTimeFormat.getFormat("E MMM d yy h:mm a");
//  private DateTimeFormat noYearFormat = DateTimeFormat.getFormat("E MMM d h:mm a");
//  DateTimeFormat shortFormat = DateTimeFormat.getFormat("MMM d, yy");
//  String nowFormat = shortFormat.format(new Date());

  public String getTooltip(ToolTipData toolTipData, String s, CommonShell commonShell) {
    String foreignLanguage = commonShell == null ? "" : commonShell.getForeignLanguage();
    String english = commonShell == null ? "" : commonShell.getEnglish();

    String seriesName1 = toolTipData.getSeriesName();

    String dateToShow = getDateToShow(toolTipData);
    if (granToLabel.values().contains(seriesName1)) {
      //logger.info("get error bar tool tip");
      return getErrorBarToolTip(toolTipData, seriesName1, dateToShow);
    } else {
//      logger.info("series is " + seriesName1);
    }
 //   boolean showEx = (!seriesName1.contains(CUMULATIVE_AVERAGE));
    boolean showEx = toShowExercise.contains(seriesName1);

    String englishTool = (english == null || english.equals("N/A")) ? "" : "<br/>" + english;
//
//    logger.info("series " + seriesName1);
//    logger.info("dateToShow " + dateToShow);
//    logger.info("foreignLanguage " + foreignLanguage);
//    logger.info("englishTool " + englishTool);
//    logger.info("showEx " + showEx);
//    logger.info("toolTipData " + toolTipData);

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

    logger.info("addCumulativeAverage " + seriesTitle + " :  " + isVisible);

    int i = 0;
    for (TimeAndScore ts : yValuesForUser) {
      data[i][0] = ts.getTimestamp();
      data[i++][1] = ts.getCumulativeAverage() * 100;
    }

    Series series = chart.createSeries()
        .setName(seriesTitle)
        .setPoints(data)
        .setType(Series.Type.SPLINE)
        .setVisible(isVisible, false);
//    series.setVisible(false,false);

    chart.addSeries(series);

    recordVisible(isVisible, series);

  }

  private void recordVisible(boolean isVisible, Series series) {
    series.setVisible(isVisible, false);
    seriesToVisible.put(series, isVisible);
  }

  private void addDeviceData(List<TimeAndScore> iPadData, Chart chart, boolean isVisible) {
    // logger.info("iPadData " + iPadData.size());

    if (!iPadData.isEmpty()) {
      Number[][] data = getDataForTimeAndScore(iPadData);

      String iPadName = I_PAD_I_PHONE;// + PRONUNCIATION_SCORE;
      Series series = chart.createSeries()
          .setName(iPadName)
          .setPoints(data)
          .setOption("color", "#00B800")
          .setType(Series.Type.SCATTER)
          .setVisible(isVisible, false);

      chart.addSeries(series);
      recordVisible(isVisible, series);
    }
  }

  private void addBrowserData(List<TimeAndScore> browserData, Chart chart, boolean isAVP, boolean isVisible) {
    //   logger.info("browserData " + browserData.size());

    if (!browserData.isEmpty()) {
      Number[][] data = getDataForTimeAndScore(browserData);

      String prefix = isAVP ? VOCAB_PRACTICE : LEARN;

      Series series = chart.createSeries()
          .setName(prefix)
          .setPoints(data)
          .setType(Series.Type.SCATTER)
          .setVisible(isVisible, false);

      chart.addSeries(series);
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
          try {
            if (axisSetExtremesEvent.getMin() != null) {
              logger.info("onSetExtremes got " + axisSetExtremesEvent.getMin() + " : " + axisSetExtremesEvent.getMax());
            } else {
              logger.info("no min for event");
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          logger.warning("got null event");
        }
        return true;
      }
    });

    chart.setHeight(CHART_HEIGHT + "px");
  }

  private void setRawBestScores(List<TimeAndScore> rawBestScores) {
    List<String> toGet = new ArrayList<>();
    for (TimeAndScore timeAndScore : rawBestScores) {
      String id = timeAndScore.getId();
      timeToId.put(timeAndScore.getTimestamp(), id);
      if (!getIdToEx().containsKey(id)) {
        toGet.add(id);
      }
    }
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

  /**
   * @return
   * @see WordContainer#getShell(String)
   */
  public Map<String, CommonShell> getIdToEx() {
    return idToEx;
  }
}
