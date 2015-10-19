package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.UserPerformance;
import org.moxieapps.gwt.highcharts.client.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/19/15.
 */
public class AnalysisPlot extends DivWidget implements IsWidget {
  public static final String PRONUNCIATION_SCORE = "Pronunciation Score";
  private final Logger logger = Logger.getLogger("AnalysisPlot");

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
            for (CommonShell shell : exerciseListWrapper.getExercises()) idToEx.put(shell.getID(), shell);
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
        add(getChart("Pronunciation over time", "Score and average", "Cumulative Average", rawBestScores));
        setRawBestScores(rawBestScores);
      }
    });
  }

  private Chart getChart(
      String title, String subtitle, String seriesName,
      List<TimeAndScore> yValuesForUser) {
    Chart chart = new Chart()
        .setZoomType(BaseChart.ZoomType.X)
        .setType(Series.Type.LINE)
        .setChartTitleText(title)
        .setChartSubtitleText(subtitle)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false)
        .setToolTip(new ToolTip()
            .setFormatter(new ToolTipFormatter() {
              public String format(ToolTipData toolTipData) {
                String s = timeToId.get(toolTipData.getXAsLong());
                CommonShell commonShell = idToEx.get(s);
                String foreignLanguage = commonShell == null ? "" : commonShell.getForeignLanguage();
                String english = commonShell == null ? "" : commonShell.getEnglish();

                String seriesName1 = toolTipData.getSeriesName();
                boolean showEx = (seriesName1.equals(PRONUNCIATION_SCORE));
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

    addSeries(yValuesForUser, chart, seriesName);

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
  private void addSeries(List<TimeAndScore> yValuesForUser, Chart chart, String seriesTitle) {
    //  Float[] yValues = yValuesForUser.toArray(new Float[0]);

    if (yValuesForUser.isEmpty()) {
      System.err.println("huh??? addSeries is empty for " + seriesTitle);
    }
    //else {
    //   //System.out.println("addSeries " + yValuesForUser);
    // }

    Number[][] data = new Number[yValuesForUser.size()][2];

    int i = 0;
    for (TimeAndScore ts : yValuesForUser) {
      data[i][0] = ts.getTimestamp();
      data[i++][1] = ts.getCumulativeAverage() * 100;
    }

    Series series = chart.createSeries()
        .setName(seriesTitle)
        .setPoints(data);

    chart.addSeries(series);

    data = new Number[yValuesForUser.size()][2];

    i = 0;
    for (TimeAndScore ts : yValuesForUser) {
      data[i][0] = ts.getTimestamp();
      data[i++][1] = ts.getScore() * 100;
    }

    series = chart.createSeries()
        .setName(PRONUNCIATION_SCORE)
        .setPoints(data);

    chart.addSeries(series);
  }

  /**
   * @param top
   * @param chart
   * @param title
   * @see #getChart(int, String, String, String, float, float, float, float, java.util.List)
   */
  private void configureChart(//float top,
                              Chart chart, String title) {
    chart.getYAxis().setAxisTitleText(title)
        .setAllowDecimals(true)
        .setMin(0);
    chart.getXAxis()
        .setType(Axis.Type.DATE_TIME);
  }

  public void setRawBestScores(List<TimeAndScore> rawBestScores) {
    for (TimeAndScore timeAndScore : rawBestScores) {
      timeToId.put(timeAndScore.getTimestamp(), timeAndScore.getId());
    }
  }
}
