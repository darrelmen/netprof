package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.analysis.UserPerformance;
import org.moxieapps.gwt.highcharts.client.Axis;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Series;

import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 */
public class AnalysisPlot extends DivWidget implements IsWidget {
  public AnalysisPlot(LangTestDatabaseAsync service, long id) {
    service.getPerformanceForUser(id, new AsyncCallback<UserPerformance>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(UserPerformance userPerformance) {
        clear();
        add(getChart("Pronunciation over time", "Cumulative Average", "Items", userPerformance.getRawBestScores()));

      }
    });
  }

  private Chart getChart(
                         String title, String subtitle, String seriesName,
                         List<TimeAndScore> yValuesForUser) {
    Chart chart = new Chart()
        .setType(Series.Type.LINE)
        .setChartTitleText(title)
        .setChartSubtitleText(subtitle)
        .setMarginRight(10)
        .setOption("/credits/enabled", false)
        .setOption("/plotOptions/series/pointStart", 1)
        .setOption("/legend/enabled", false);

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
      data[i++][1] = ts.getCumulativeAverage()*100;
    }

    Series series = chart.createSeries()
        .setName(seriesTitle)
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
//        .setMax(top);
//
//    chart.getXAxis().setAllowDecimals(false);

    chart.getXAxis()
        .setType(Axis.Type.DATE_TIME);
  }
}
