package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.UserPerformance;
import org.moxieapps.gwt.highcharts.client.*;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/19/15.
 */
public class AnalysisPlot extends DivWidget implements IsWidget {
  public static final String I_PAD_I_PHONE = "iPad/iPhone";
  public static final String VOCAB_PRACTICE = "Vocab Practice";
  public static final String LEARN = "Learn";
  public static final String CUMULATIVE_AVERAGE = "Average";
  private final Logger logger = Logger.getLogger("AnalysisPlot");

  private static final int CHART_HEIGHT = 340;

  private static final int Y_OFFSET_FOR_LEGEND = 60;
  // private static final String PRONUNCIATION_SCORE = "Pronunciation Score";

  private final Map<Long, String> timeToId = new TreeMap<>();
  private final Map<String, CommonShell> idToEx = new TreeMap<>();

  /**
   * @param service
   * @param id
   * @param userChosenID
   * @param minRecordings
   * @see AnalysisTab#AnalysisTab
   */
  public AnalysisPlot(LangTestDatabaseAsync service, long id, final String userChosenID, final int minRecordings) {
    //setHeight("350px");
    service.getExerciseIds(1, new HashMap<String, Collection<String>>(), "", -1,
        (int) id, "", false, false, false, false, new AsyncCallback<ExerciseListWrapper>() {
          @Override
          public void onFailure(Throwable throwable) {
            logger.warning("\n\n\n-> getExerciseIds " + throwable);
          }

          @Override
          public void onSuccess(ExerciseListWrapper exerciseListWrapper) {
            for (CommonShell shell : exerciseListWrapper.getExercises()) getIdToEx().put(shell.getID(), shell);
          }
        });

    service.getPerformanceForUser(id, minRecordings, new AsyncCallback<UserPerformance>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("\n\n\n-> getPerformanceForUser " + throwable);
      }

      @Override
      public void onSuccess(UserPerformance userPerformance) {
        clear();
        List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
        float v = userPerformance.getRawAverage() * 100;
        int rawTotal = rawBestScores.size();//userPerformance.getRawTotal();

        if (rawBestScores.isEmpty()) {
          add(new Label("No Recordings yet to analyze. Please record yourself."));
        } else {
//        logger.info("getPerformanceForUser raw total " + rawTotal + " num " + rawBestScores.size());
          String subtitle = "Score and average (" + rawTotal + " items : avg score " + (int) v + " %)";
          String title = "<b>" + userChosenID + "</b>" + " pronunciation score (Drag to zoom in)";
          add(getChart(title,
              subtitle, CUMULATIVE_AVERAGE, userPerformance));
        }
        setRawBestScores(rawBestScores);
      }
    });
  }

  /**
   * @param title
   * @param subtitle
   * @param seriesName
   * @return
   * @see AnalysisPlot#AnalysisPlot(LangTestDatabaseAsync, long, String, int)
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
        .setToolTip(getToolTip());

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

  private ToolTip getToolTip() {
    return new ToolTip()
        .setFormatter(new ToolTipFormatter() {
          public String format(ToolTipData toolTipData) {
            String s = timeToId.get(toolTipData.getXAsLong());
            CommonShell commonShell = getIdToEx().get(s);
            String foreignLanguage = commonShell == null ? "" : commonShell.getForeignLanguage();
            String english = commonShell == null ? "" : commonShell.getEnglish();

            String seriesName1 = toolTipData.getSeriesName();
            boolean showEx = (!seriesName1.contains(CUMULATIVE_AVERAGE));
            return "<b>" + seriesName1 + "</b><br/>" +
                DateTimeFormat.getFormat("E MMM d yy h:mm a").format(
                    new Date(toolTipData.getXAsLong())
                ) +
                (showEx ?
                    "<br/>Ex " + s +
                        "<br/>" + foreignLanguage +
                        "<br/>" + english
                    : "")
                +
                "<br/>Score = " + toolTipData.getYAsLong() + "%";
          }
        });
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
        .setType(Series.Type.LINE);

    chart.addSeries(series);
  }

  private void addDeviceData(List<TimeAndScore> iPadData, Chart chart) {
    // logger.info("iPadData " + iPadData.size());

    if (!iPadData.isEmpty()) {
      Number[][] data;
      Series series;
      data = getDataForTimeAndScore(iPadData);

      String iPadName = I_PAD_I_PHONE;// + PRONUNCIATION_SCORE;
      series = chart.createSeries()
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
      //String browserName = prefix + PRONUNCIATION_SCORE;

      Series series;
      series = chart.createSeries()
          .setName(prefix)
          .setPoints(data);

      chart.addSeries(series);
    }
  }

  private Number[][] getDataForTimeAndScore(List<TimeAndScore> yValuesForUser) {
    Number[][] data;
    int i;
    data = new Number[yValuesForUser.size()][2];

    i = 0;
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
    chart.getYAxis().setAxisTitleText(title)
        .setMin(0).setMax(100);

    chart.getXAxis()
        .setType(Axis.Type.DATE_TIME);

    chart.setHeight(CHART_HEIGHT +
        "px");
  }

  private void setRawBestScores(List<TimeAndScore> rawBestScores) {
    for (TimeAndScore timeAndScore : rawBestScores) {
      timeToId.put(timeAndScore.getTimestamp(), timeAndScore.getId());
    }
  }

  /**
   * @return
   * @see WordContainer#getShell(String)
   */
  public Map<String, CommonShell> getIdToEx() {
    return idToEx;
  }
}
