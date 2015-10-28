package mitll.langtest.client.analysis;

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
  private final Logger logger = Logger.getLogger("AnalysisPlot");
  private static final String PRONUNCIATION_SCORE = "Pronunciation Score";

  private Map<Long, String> timeToId = new TreeMap<>();
  private Map<String, CommonShell> idToEx = new TreeMap<>();

  /**
   * @param service
   * @param id
   * @see Navigation#addTabs()
   */
  public AnalysisPlot(LangTestDatabaseAsync service, long id) {
    service.getExerciseIds(1, new HashMap<String, Collection<String>>(), "", -1,
        (int) id, "", false, false, false, false, new AsyncCallback<ExerciseListWrapper>() {
          @Override
          public void onFailure(Throwable throwable) {

          }

          @Override
          public void onSuccess(ExerciseListWrapper exerciseListWrapper) {
            for (CommonShell shell : exerciseListWrapper.getExercises()) getIdToEx().put(shell.getID(), shell);
          }
        });

    service.getPerformanceForUser(id, new AsyncCallback<UserPerformance>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(UserPerformance userPerformance) {
        clear();
        List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
        float v = userPerformance.getRawAverage() * 100;
        add(getChart("Pronunciation over time (Drag to zoom in)",
            "Score and average (" + userPerformance.getRawTotal() + " items : avg score " + (int) v +
                " %)", "Cumulative Average", userPerformance));
        setRawBestScores(rawBestScores);
      }
    });
  }

  /**
   * @param title
   * @param subtitle
   * @param seriesName
   * @param yValuesForUser
   * @return
   * @see AnalysisPlot#AnalysisPlot(LangTestDatabaseAsync, long)
   */
  private Chart getChart(
      String title, String subtitle, String seriesName, UserPerformance userPerformance
  ) {
   // List<TimeAndScore> yValuesForUser;

    Chart chart = new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        .setType(Series.Type.SCATTER)
        .setChartTitleText(title)
        .setChartSubtitleText(subtitle)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false)
        .setLegend(new Legend()
                .setLayout(Legend.Layout.VERTICAL)
                .setAlign(Legend.Align.LEFT)
                .setVerticalAlign(Legend.VerticalAlign.TOP)
                .setX(100)
                .setY(70)
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
        .setToolTip(new ToolTip()
            .setFormatter(new ToolTipFormatter() {
              public String format(ToolTipData toolTipData) {
                String s = timeToId.get(toolTipData.getXAsLong());
                CommonShell commonShell = getIdToEx().get(s);
                String foreignLanguage = commonShell == null ? "" : commonShell.getForeignLanguage();
                String english = commonShell == null ? "" : commonShell.getEnglish();

                String seriesName1 = toolTipData.getSeriesName();
                boolean showEx = (seriesName1.contains(PRONUNCIATION_SCORE));
                return "<b>" + seriesName1 + "</b><br/>" +
                    DateTimeFormat.getFormat("E MMM d h:mm a").format(
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
            }));

    addSeries(userPerformance.getRawBestScores(), userPerformance.getiPadTimeAndScores(), userPerformance.getBrowserTimeAndScores(),
        chart, seriesName);

    configureChart(
        chart, subtitle);
    return chart;
  }

  /**
   * @param yValuesForUser
   * @param chart
   * @param seriesTitle
   * @paramx gameTimeSeconds
   * @see #getChart
   */
  private void addSeries(List<TimeAndScore> yValuesForUser, List<TimeAndScore> iPadData, List<TimeAndScore> browserData, Chart chart, String seriesTitle) {
    Number[][] data = new Number[yValuesForUser.size()][2];

    logger.info("got " + yValuesForUser.size());
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

    logger.info("iPadData " + iPadData.size());

    if (!iPadData.isEmpty()) {
      data = getDataForTimeAndScore(iPadData);

      String iPadName = "iPad/iPhone " + PRONUNCIATION_SCORE;
      series = chart.createSeries()
          .setName(iPadName)
          .setPoints(data)
          .setOption("color","#00B800");

      chart.addSeries(series);
    }

    logger.info("browserData " + browserData.size());

    if (!browserData.isEmpty()) {
      data = getDataForTimeAndScore(browserData);

      String browserName = "Browser " + PRONUNCIATION_SCORE;
      series = chart.createSeries()
          .setName(browserName)
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
   * @param top
   * @param chart
   * @param title
   * @see #getChart(int, String, String, String, float, float, float, float, java.util.List)
   */
  private void configureChart(Chart chart, String title) {
    chart.getYAxis().setAxisTitleText(title)
        .setMin(0);

    chart.getXAxis()
        .setType(Axis.Type.DATE_TIME);
  }

  private void setRawBestScores(List<TimeAndScore> rawBestScores) {
    for (TimeAndScore timeAndScore : rawBestScores) {
      timeToId.put(timeAndScore.getTimestamp(), timeAndScore.getId());
    }
  }

  public Map<String, CommonShell> getIdToEx() {
    return idToEx;
  }
}
