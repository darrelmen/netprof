package mitll.langtest.client.flashcard;

import com.google.gwt.event.dom.client.ClickHandler;
import mitll.langtest.shared.flashcard.Leaderboard;
import mitll.langtest.shared.flashcard.ScoreInfo;
import mitll.langtest.shared.flashcard.SetScore;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.PlotBand;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.labels.PlotBandLabel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class LeaderboardPlot {
  public static final float HALF = (1f / 4f);
  public static final String AVERAGE = "Class Average";
  public static final String TOP_SCORE = "Class Top Score";
  public static final String PERSONAL_BEST = "Personal Best";

  /**
   * @deprecated  for now
   * @param leaderboard
   * @param userID
   * @param gameTimeSeconds
   * @param currentSelection
   * @param prompt
   * @param onYes
   * @param onNo
   * @param autoHideDelay
   */
  public void showLeaderboardPlot(Leaderboard leaderboard, final long userID, int gameTimeSeconds,
                                  Map<String, Collection<String>> currentSelection,
                                  String prompt,
                                  final ClickHandler onYes, final ClickHandler onNo,int autoHideDelay) {
    List<ScoreInfo> scores = leaderboard.getScores(currentSelection);
  }

  /**
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#onSetComplete()
   * @param scores
   * @param userID
   * @param gameTimeSeconds
   * @param title
   * @param subtitle
   * @param useCorrect
   * @param topToUse
   * @return
   */
  public <T extends SetScore> Chart getChart(List<T> scores, long userID, int gameTimeSeconds,
                                             String title, String subtitle, boolean useCorrect, float topToUse) {
    GetPlotValues getPlotValues = new GetPlotValues<T>(scores, userID, useCorrect).invoke();
    return getChart(scores, gameTimeSeconds, getPlotValues, title, subtitle, useCorrect ? "Correct" : "Score", !useCorrect, topToUse);
  }

  private <T extends SetScore> Chart getChart(List<T> scores, int gameTimeSeconds,
                                              GetPlotValues getPlotValues,
                                              String title, String subtitle, String seriesName,
                                              boolean topIs100, float topToUse) {
    System.out.println("title "  +title+ " top is 100 " + topIs100 + " top " + topToUse);
    float pbCorrect = getPlotValues.getPbCorrect();
    float top = getPlotValues.getTop();
    float total = getPlotValues.getTotalCorrect();
    float avg = getPlotValues.getClassAvg();

    List<Float> yValuesForUser = getPlotValues.getyValuesForUser();

    Chart chart = new Chart()
      .setType(Series.Type.SPLINE)
      .setChartTitleText(title)
      .setChartSubtitleText(subtitle)
      .setMarginRight(10)
      .setOption("/credits/enabled", false)
      .setOption("/legend/enabled", false);

    addSeries(gameTimeSeconds, yValuesForUser, chart, seriesName);

    PlotBand personalBest = getPersonalBest(pbCorrect, chart);
    PlotBand topScore = getTopScore(top, chart);
    PlotBand avgScore = getAvgScore(scores, total, chart);

    float verticalRange = topIs100 ? 100f : topToUse == -1 ? top : topToUse;
    float fivePercent = 0.05f * verticalRange;
    float topVsPersonalBest = Math.abs(top - pbCorrect);
    float avgVsPersonalBest = Math.abs(avg - pbCorrect);

    System.out.println(title + " " + subtitle + " top vs pb " + topVsPersonalBest + " avg vs pb " + avgVsPersonalBest);

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

    configureChart(verticalRange, chart, subtitle);
    return chart;
  }

  /**
   * @see #getChart(java.util.List, int, mitll.langtest.client.flashcard.LeaderboardPlot.GetPlotValues, String, String, String, boolean, float)
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
    chart.getXAxis().setAllowDecimals(false);
    chart.getYAxis().setAllowDecimals(true);
    chart.getYAxis().setMin(0);
    chart.getYAxis().setMax(top);
  }

  private <T extends SetScore> PlotBand getAvgScore(List<T> scores, float total, Chart chart) {
    float avg = total / (float) scores.size();
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

  private float over(float pbCorrect) { return pbCorrect + HALF;  }
  private float under(float pbCorrect) { return pbCorrect - HALF;  }

  private static class GetPlotValues<T extends SetScore> {
    private List<T> scores;
    private long userID;
    private float pbCorrect;
    private float top;
    private float totalCorrect;
    private List<Float> yValuesForUser;
    boolean useCorrect = true;

    public GetPlotValues(List<T> scores, long userID, boolean useCorrect) {
      this.scores = scores;
      this.userID = userID;
      this.useCorrect = useCorrect;
      if (scores.isEmpty()) System.err.println("huh? scores is empty???");
    }

    public float getPbCorrect() {
      return pbCorrect;
    }

    public float getTop() {
      return top;
    }
    public void setTop(float top) { this.top = top;}

    public float getTotalCorrect() {
      return totalCorrect;
    }

    public float getClassAvg() {
      if (numNotMe == 0f) {
        return totalCorrect / (float) scores.size();
      }
      else {
        return totalNotMe / numNotMe;
      }
    }

    float totalNotMe = 0f;
    float numNotMe = 0f;

    public List<Float> getyValuesForUser() {
      return yValuesForUser;
    }

    // TODO : don't do this client side
    public GetPlotValues invoke() {
      pbCorrect = 0;
      top = 0;
      totalCorrect = 0;

      yValuesForUser = new ArrayList<Float>();
      for (SetScore score : scores) {
        float value = getValue(score);

        if (score.getUserid() == userID) {
          if (value > pbCorrect) pbCorrect = value;
          yValuesForUser.add(value);
          //System.out.println("showLeaderboardPlot : for user " +userID + " got " + score);
        }
        else {
          //System.out.println("\tshowLeaderboardPlot : for user " +score.getUserid() + " got " + score);
          totalNotMe += value;
          numNotMe++;
        }
        if (value > top) {
          top = value;
          //System.out.println("\tshowLeaderboardPlot : new top score for user " +score.getUserid() + " got " + score);
        }
        totalCorrect += value;
      }

      if (yValuesForUser.isEmpty()) System.err.println("huh? yValuesForUser (" +userID+
        ")is empty???");

      return this;
    }
    private float getValue(SetScore score) {
      return useCorrect ? Math.round(score.getCorrectPercent()) : Math.round(100f*score.getAvgScore());
    }
  }
}