package mitll.langtest.client.flashcard;

import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.flashcard.SetScore;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.PlotBand;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.labels.PlotBandLabel;

import java.util.List;

public class LeaderboardPlot {
  private static final float HALF = 3f;
  private static final String AVERAGE = "Class Average";
  private static final String TOP_SCORE = "Class Top Score";
  private static final String PERSONAL_BEST = "Personal Best";
  private static final String CORRECT = "Correct";
  private static final String SCORE = "Score";

  public Chart getChart(AVPHistoryForList historyForList, String title, String subtitle) {
    boolean useCorrect = historyForList.isUseCorrect();
    return getChart(historyForList.getNumScores(),0,title, subtitle, useCorrect ? CORRECT : SCORE, !useCorrect, 100f,
      historyForList.getPbCorrect(),
      historyForList.getTop(),
      historyForList.getTotalCorrect(),
      historyForList.getClassAvg(),
      historyForList.getyValuesForUser()
      );
  }

  private Chart getChart(int numScores, int gameTimeSeconds, String title, String subtitle, String seriesName,
                         boolean topIs100, float topToUse,
                         float pbCorrect, float top, float total, float avg, List<Float> yValuesForUser) {
    Chart chart = new Chart()
      .setType(Series.Type.SPLINE)
      .setChartTitleText(title)
      .setChartSubtitleText(subtitle)
      .setMarginRight(10)
      .setOption("/credits/enabled", false)
      .setOption("/legend/enabled", false);

    addSeries(gameTimeSeconds, yValuesForUser, chart, seriesName);

    float verticalRange = setPlotBands(numScores, title, subtitle, topIs100, topToUse, pbCorrect, top, total, avg, chart);

    configureChart(verticalRange, chart, subtitle);
    return chart;
  }

  private float setPlotBands(int numScores, String title, String subtitle, boolean topIs100,
                             float topToUse, float pbCorrect, float top, float total, float avg, Chart chart) {
    PlotBand personalBest = getPersonalBest(pbCorrect, chart);
    PlotBand topScore = getTopScore(top, chart);
    PlotBand avgScore = getAvgScore(numScores, total, chart);

    float verticalRange = topIs100 ? (100f+HALF) : topToUse == -1 ? top : topToUse;

    float fivePercent = 0.05f * verticalRange;
    float topVsPersonalBest = Math.abs(top - pbCorrect);
    float avgVsPersonalBest = Math.abs(avg - pbCorrect);

    System.out.println(title + " " + subtitle + " top vs pb " + topVsPersonalBest + " avg vs pb " + avgVsPersonalBest);

    setPlotPands(chart, personalBest, topScore, avgScore, fivePercent, topVsPersonalBest, avgVsPersonalBest);
    return verticalRange;
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
   * @param gameTimeSeconds
   * @param yValuesForUser
   * @param chart
   * @param seriesTitle
   */
  private void addSeries(int gameTimeSeconds, List<Float> yValuesForUser, Chart chart, String seriesTitle) {
    Float[] yValues = yValuesForUser.toArray(new Float[0]);

    if (yValuesForUser.isEmpty()) {
      System.err.println("huh??? addSeries is empty for " + seriesTitle);
    }
    else {
      System.out.println("addSeries " + yValuesForUser);
    }

    String seriesLabel = gameTimeSeconds > 0 ? "Correct in " + gameTimeSeconds + " seconds" : seriesTitle;
    Series series = chart.createSeries()
      .setName(seriesLabel)
      .setPoints(yValues);
    chart.addSeries(series);
  }

  private void configureChart(float top, Chart chart,String title) {
    chart.getYAxis().setAxisTitleText(title);
    chart.getYAxis().setAllowDecimals(true);
    chart.getYAxis().setMin(0);
    chart.getYAxis().setMax(top);

    chart.getXAxis().setAllowDecimals(false);
  }

  private <T extends SetScore> PlotBand getAvgScore(int numScores, float total, Chart chart) {
    float avg = total / (float) numScores;
    PlotBand avgScore = chart.getYAxis().createPlotBand()
      .setColor("#2031ff")
      .setFrom(under(avg))
      .setTo(over(avg));

    avgScore.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText(AVERAGE));
    return avgScore;
  }

  private PlotBand getTopScore(float top, Chart chart) {
    PlotBand topScore = chart.getYAxis().createPlotBand()
      .setColor("#46bf00")
      .setFrom(under(top))
      .setTo(over(top));

    topScore.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText(TOP_SCORE));
    return topScore;
  }

  private PlotBand getPersonalBest(float pbCorrect, Chart chart) {
    float from = under(pbCorrect);
    float to   = over (pbCorrect);
    PlotBand personalBest = chart.getYAxis().createPlotBand()
      .setColor("#f18d24")
      .setFrom(from)
      .setTo(to);

    personalBest.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText(PERSONAL_BEST));
    return personalBest;
  }

  private float over (float pbCorrect) { return pbCorrect + HALF;  }
  private float under(float pbCorrect) { return pbCorrect - HALF;  }
}