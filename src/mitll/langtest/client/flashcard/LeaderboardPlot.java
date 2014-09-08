package mitll.langtest.client.flashcard;

import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.flashcard.SetScore;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.PlotBand;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.labels.PlotBandLabel;

import java.util.List;

public class LeaderboardPlot {
  public static final float HALF = 3f;
  public static final String AVERAGE = "Class Average";
  private static final String TOP_SCORE = "Class Top Score";
  private static final String PERSONAL_BEST = "Personal Best";
  private static final String CORRECT = "Correct";
  private static final String SCORE = "Score";
  public static final float GRAPH_MAX = 100f;

  /**
   * @see MyFlashcardExercisePanelFactory.StatsPracticePanel#makeChart(int, int, mitll.langtest.shared.flashcard.AVPHistoryForList)
   * @param historyForList
   * @param title
   * @param subtitle
   * @return
   */
  public Chart getChart(AVPHistoryForList historyForList, String title, String subtitle) {
    boolean useCorrect = historyForList.isUseCorrect();
    return getChart(historyForList.getNumScores(),title, subtitle,
      useCorrect ? CORRECT : SCORE,
      historyForList.getPbCorrect(),
      historyForList.getTop(),
      historyForList.getTotalCorrect(),
      historyForList.getClassAvg(),
      historyForList.getyValuesForUser()
      );
  }

  private Chart getChart(int numScores, String title, String subtitle, String seriesName,
                         float pbCorrect, float top, float total, float avg, List<Float> yValuesForUser) {
    Chart chart = new Chart()
      .setType(Series.Type.SPLINE)
      .setChartTitleText(title)
      .setChartSubtitleText(subtitle)
      .setMarginRight(10)
      .setOption("/credits/enabled", false)
      .setOption("/plotOptions/series/pointStart", 1)
      .setOption("/legend/enabled", false);

    addSeries(yValuesForUser, chart, seriesName);

    float verticalRange = setPlotBands(numScores, title, subtitle,
      pbCorrect, top, total, avg, chart);

    configureChart(verticalRange, chart, subtitle);
    return chart;
  }

  private float setPlotBands(int numScores, String title, String subtitle,
                             float pbCorrect, float top, float total, float avg, Chart chart) {
    PlotBand personalBest = getPersonalBest(pbCorrect, chart);
    PlotBand topScore = getTopScore(top, chart);
    PlotBand avgScore = getAvgScore(numScores, total, chart);

    float fivePercent = 0.05f * GRAPH_MAX;
    float topVsPersonalBest = Math.abs(top - pbCorrect);
    float avgVsPersonalBest = Math.abs(avg - pbCorrect);

    System.out.println(title + " " + subtitle + " top vs pb " + topVsPersonalBest + " avg vs pb " + avgVsPersonalBest);

    setPlotPands(chart, personalBest, topScore, avgScore, fivePercent, topVsPersonalBest, avgVsPersonalBest);
    return GRAPH_MAX;
  }

  private void setPlotPands(Chart chart, PlotBand personalBest, PlotBand topScore, PlotBand avgScore,
                            float fivePercent, float topVsPersonalBest, float avgVsPersonalBest) {
    if (topVsPersonalBest > fivePercent) {
      if (avgVsPersonalBest > fivePercent){
        chart.getYAxis().setPlotBands(
          avgScore,
          personalBest,
          topScore
        );
      }
      else {
        chart.getYAxis().setPlotBands(
          personalBest,
          topScore
        );
      }
    } else if (avgVsPersonalBest > fivePercent){
      chart.getYAxis().setPlotBands(
        avgScore,
        personalBest
      );
    } else {
      chart.getYAxis().setPlotBands(
        personalBest
      );
    }
  }

  /**
   * @see #getChart
   * @paramx gameTimeSeconds
   * @param yValuesForUser
   * @param chart
   * @param seriesTitle
   */
  private void addSeries( List<Float> yValuesForUser, Chart chart, String seriesTitle) {
    Float[] yValues = yValuesForUser.toArray(new Float[0]);

    if (yValuesForUser.isEmpty()) {
      System.err.println("huh??? addSeries is empty for " + seriesTitle);
    }
    //else {
   //   //System.out.println("addSeries " + yValuesForUser);
   // }

    Series series = chart.createSeries()
      .setName(seriesTitle)
      .setPoints(yValues);
    chart.addSeries(series);
  }

  /**
   * @see #getChart(int, String, String, String, float, float, float, float, java.util.List)
   * @param top
   * @param chart
   * @param title
   */
  private void configureChart(float top, Chart chart,String title) {
    chart.getYAxis().setAxisTitleText(title)
      .setAllowDecimals(true)
      .setMin(0)
      .setMax(top);

    chart.getXAxis().setAllowDecimals(false);
  }

  private <T extends SetScore> PlotBand getAvgScore(int numScores, float total, Chart chart) {
    float avg = total / (float) numScores;
    return getPlotBand(avg, chart, "#8EB4E3", AVERAGE);
  }

  private PlotBand getTopScore(float top, Chart chart) {
    return getPlotBand(top, chart, "#46bf00", TOP_SCORE);
  }

  private PlotBand getPersonalBest(float pbCorrect, Chart chart) {
    return getPlotBand(pbCorrect, chart, "#f18d24", PERSONAL_BEST);
  }

  private PlotBand getPlotBand(float pbCorrect, Chart chart, String color, String labelText) {
    Range range = getRange(pbCorrect);
    PlotBand personalBest = chart.getYAxis().createPlotBand()
      .setColor(color)
      .setFrom(range.from)
      .setTo(range.to);

    personalBest.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText(labelText));
    return personalBest;
  }

  private Range getRange(float pbCorrect) {
    float from = under(pbCorrect);
    float to   = over (pbCorrect);
    if (pbCorrect > GRAPH_MAX -HALF) {
      to = GRAPH_MAX;
      from = GRAPH_MAX -2*HALF;
    }
    if (pbCorrect < HALF) {
      to = 2*HALF;
      from = 0;
    }
    return new Range(from,to);
  }

  private static class Range {
    float from,to;
    public Range(float from, float to) {
      this.from = from;
      this.to = to;
    }
  }

  private float over (float pbCorrect) { return pbCorrect + HALF;  }
  private float under(float pbCorrect) { return pbCorrect - HALF;  }
}