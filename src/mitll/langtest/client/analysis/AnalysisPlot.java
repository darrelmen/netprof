package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.events.ChartSelectionEvent;
import org.moxieapps.gwt.highcharts.client.events.ChartSelectionEventHandler;
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
public class AnalysisPlot extends DivWidget implements IsWidget {
  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;
  private static final String I_PAD_I_PHONE = "iPad/iPhone";
  private static final String VOCAB_PRACTICE = "Vocab Practice";
  private static final String LEARN = "Learn";
  private static final String CUMULATIVE_AVERAGE = "Average";
  private final Logger logger = Logger.getLogger("AnalysisPlot");

  private static final int CHART_HEIGHT = 330;

  private static final int Y_OFFSET_FOR_LEGEND = 60;

  private final Map<Long, String> timeToId = new TreeMap<>();
  private final Map<String, CommonShell> idToEx = new TreeMap<>();
  private final long userid;
  private final LangTestDatabaseAsync service;
  private final SoundPlayer soundFeedback;

  /**
   * @param service
   * @param userid
   * @param userChosenID
   * @param minRecordings
   * @see AnalysisTab#AnalysisTab
   */
  public AnalysisPlot(LangTestDatabaseAsync service, long userid, final String userChosenID, final int minRecordings, SoundManagerAPI soundManagerAPI) {
    getElement().setId("AnalysisPlot");
    //   addStyleName("floatLeft");
    this.service = service;
    this.userid = userid;
    this.soundFeedback = new SoundPlayer(soundManagerAPI);
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

    if (rawBestScores.isEmpty()) {
      add(new Label("No Recordings yet to analyze. Please record yourself."));
    } else {
//        logger.info("getPerformanceForUser raw total " + rawTotal + " num " + rawBestScores.size());
      String subtitle = "Score and average (" + rawTotal + " items, average " + (int) v + " %)";
      String title = "<b>" + userChosenID + "</b>" + " pronunciation score (Drag to zoom in, click to hear)";
      add(getChart(title, subtitle, CUMULATIVE_AVERAGE, userPerformance));
    }
    setRawBestScores(rawBestScores);
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
    Chart chart = new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        .setType(Series.Type.SCATTER)
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
            .setSeriesClickEventHandler(new SeriesClickEventHandler() {
                                          public boolean onClick(SeriesClickEvent clickEvent) {
                                            long nearestXAsLong = clickEvent.getNearestXAsLong();
                                            String s = timeToId.get(nearestXAsLong);
                                            if (s != null) {
                                              playLast(s);
                                            }
                                            return true;
                                          }
                                        }
            )
        ).setSelectionEventHandler(new ChartSelectionEventHandler() {
          @Override
          public boolean onSelection(ChartSelectionEvent selectionEvent) {
            logger.info("User selected from " + selectionEvent.getXAxisMin() + " to " + selectionEvent.getXAxisMax());
            return true;
          }
        });


    Highcharts.setOptions(
        new Highcharts.Options().setGlobal(
            new Global()
                .setUseUTC(false)
        ));

    addSeries(userPerformance.getRawBestScores(),
        userPerformance.getiPadTimeAndScores(),
        userPerformance.getLearnTimeAndScores(),
        userPerformance.getAvpTimeAndScores(),
        chart, seriesName);

    configureChart(chart, subtitle);
    return chart;
  }

  com.google.gwt.user.client.Timer t;

  public void playLast(String id) {
    service.getExercise(id, userid, false, new AsyncCallback<CommonExercise>() {
      @Override
      public void onFailure(Throwable throwable) {
      }

      @Override
      public void onSuccess(CommonExercise commonExercise) {
        List<CorrectAndScore> scores = commonExercise.getScores();
        CorrectAndScore correctAndScore = scores.get(scores.size() - 1);
        String refAudio = commonExercise.getRefAudio();

        if (t != null) t.cancel();
        if (refAudio != null) {
          playLastThenRef(correctAndScore, refAudio);
        }
      }
    });
  }

  public void playLastThenRef(CorrectAndScore correctAndScore, String refAudio) {
    final String path = getPath(refAudio);
    soundFeedback.queueSong(getPath(correctAndScore.getPath()), new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {

      }

      @Override
      public void songEnded() {
        t = new com.google.gwt.user.client.Timer() {
          @Override
          public void run() {
            soundFeedback.queueSong(path);
          }
        };
        t.schedule(100);
      }

    });
  }

  private String getPath(String path) {
    path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
    path = ensureForwardSlashes(path);
    return path;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  private ToolTip getToolTip() {
    return new ToolTip()
        .setFormatter(new ToolTipFormatter() {
          public String format(ToolTipData toolTipData) {
            String exerciseID = timeToId.get(toolTipData.getXAsLong());
            CommonShell commonShell = getIdToEx().get(exerciseID);
            return getTooltip(toolTipData, exerciseID, commonShell);
          }
        });
  }

  private DateTimeFormat format = DateTimeFormat.getFormat("E MMM d yy h:mm a");
  private DateTimeFormat noYearFormat = DateTimeFormat.getFormat("E MMM d h:mm a");
  DateTimeFormat shortFormat = DateTimeFormat.getFormat("MMM d, yy");
  Date now = new Date();

  public String getTooltip(ToolTipData toolTipData, String s, CommonShell commonShell) {
    String foreignLanguage = commonShell == null ? "" : commonShell.getForeignLanguage();
    String english = commonShell == null ? "" : commonShell.getEnglish();

    String seriesName1 = toolTipData.getSeriesName();
    boolean showEx = (!seriesName1.contains(CUMULATIVE_AVERAGE));
    String englishTool = "<br/>" + english;
    if (english.equals("N/A")) englishTool = "";

    Date date = new Date(toolTipData.getXAsLong());

    // this year?
    String nowFormat = shortFormat.format(now);
    String shortForDate = shortFormat.format(date);

    DateTimeFormat toUse = (nowFormat.substring(nowFormat.length() - 2).equals(shortForDate.substring(shortForDate.length() - 2))) ? noYearFormat : format;
    String dateToShow = toUse.format(date);

    return
        "<b>" + seriesName1 + "</b>" +
            "<br/>" +
            dateToShow +
            (showEx ?
                "<br/>Exercise " + s +
                    "<br/>" +
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
                         String seriesTitle) {
    addCumulativeAverage(yValuesForUser, chart, seriesTitle);

    addDeviceData(iPadData, chart);

    addBrowserData(learnData, chart, false);
    addBrowserData(avpData, chart, true);
  }

  private void addCumulativeAverage(List<TimeAndScore> yValuesForUser, Chart chart, String seriesTitle) {
    Number[][] data = new Number[yValuesForUser.size()][2];

    // logger.info("got " + yValuesForUser.size());
    int i = 0;
    for (TimeAndScore ts : yValuesForUser) {
      data[i][0] = ts.getTimestamp();
      data[i++][1] = ts.getCumulativeAverage() * 100;
    }

    Series series = chart.createSeries()
        .setName(seriesTitle)
        .setPoints(data)
        .setType(Series.Type.SPLINE);

    chart.addSeries(series);
  }

  private void addDeviceData(List<TimeAndScore> iPadData, Chart chart) {
    // logger.info("iPadData " + iPadData.size());

    if (!iPadData.isEmpty()) {
      Number[][] data;
      data = getDataForTimeAndScore(iPadData);

      String iPadName = I_PAD_I_PHONE;// + PRONUNCIATION_SCORE;
      Series series = chart.createSeries()
          .setName(iPadName)
          .setPoints(data)
          .setOption("color", "#00B800");

      chart.addSeries(series);
    }
  }

  private void addBrowserData(List<TimeAndScore> browserData, Chart chart, boolean isAVP) {
    //   logger.info("browserData " + browserData.size());

    if (!browserData.isEmpty()) {
      Number[][] data;
      data = getDataForTimeAndScore(browserData);

      String prefix = isAVP ? VOCAB_PRACTICE : LEARN;

      Series series = chart.createSeries()
          .setName(prefix)
          .setPoints(data);

      chart.addSeries(series);
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
        .setType(Axis.Type.DATE_TIME);

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
