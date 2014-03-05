package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
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
  public static final String AVERAGE = "Average";
  public static final String TOP_SCORE = "Top Score";
  public static final String PERSONAL_BEST = "Personal Best";
  // public static final int AUTO_HIDE_DELAY = 3000;

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

   // showLeaderboardPlot(scores, userID, gameTimeSeconds, currentSelection, prompt,onYes, onNo,autoHideDelay);
  }

/*  public Modal showLeaderboardPlot(Leaderboard leaderboard,final long userID, int gameTimeSeconds,
                                   Map<String, Collection<String>> currentSelection,
                                   String prompt,int autoHideDelay) {
    List<ScoreInfo> scores = leaderboard.getScores(currentSelection);

    return showLeaderboardPlot(scores, userID, gameTimeSeconds, currentSelection, prompt, null, null,autoHideDelay);
  }*/

/*
  public Modal showLeaderboardPlot(List<ScoreInfo> scores, final long userID, int gameTimeSeconds,
                                   Map<String, Collection<String>> currentSelection,
                                   String prompt,int autoHideDelay) {
    return showLeaderboardPlot(scores, userID, gameTimeSeconds, currentSelection, prompt, null, null, autoHideDelay);
  }*/

/*  private Modal showLeaderboardPlot(List<SetScore> scores, final long userID, int gameTimeSeconds,
                                  Map<String, Collection<String>> currentSelection,
                                  String prompt,
                                  final ClickHandler onYes, final ClickHandler onNo, int autoHideDelay) {
    final Modal modal = new Modal();
    modal.setAnimation(false);
    modal.setCloseVisible(true);

    makeChart(scores, userID, gameTimeSeconds, currentSelection, prompt, onYes, onNo, autoHideDelay, modal);

    modal.show();
    return modal;
  }*/

  private void makeChart(List<SetScore> scores, long userID, int gameTimeSeconds,
                         Map<String, Collection<String>> currentSelection,
                         String prompt, final ClickHandler onYes, final ClickHandler onNo,
                         int autoHideDelay, final Modal modal) {
    modal.setWidth("720px");
    String hashMapToStringCleaned =
      currentSelection.toString().replace("{", "").replace("}", "").replace("=", " ").replace("[", "").replace("]", "");
    String title = "Leaderboard";

    Chart chart = getChart(scores, userID, gameTimeSeconds, title, hashMapToStringCleaned);

    modal.setMaxHeigth("650px");
    modal.setHeight("550px");
    modal.add(chart);

    Button yesButton = getYesButton(onYes, modal);
    Button noButton = getNoButton(onNo, modal);

    FluidRow promptRow = new FluidRow();
    Column column = new Column(12,new Heading(4, prompt));
    promptRow.add(column);
    modal.add(promptRow);

    if (onYes != null || onNo != null) {
      FluidRow row = new FluidRow();
      if (yesButton != null) row.add(new Column(4, yesButton));
      row.add(new Column(4, new Heading(4)));
      if (noButton != null)  row.add(new Column(4, noButton));
      modal.add(row);
    }
    else {
      Timer t = new Timer() {
        @Override
        public void run() {
          modal.hide();
        }
      };
      t.schedule(autoHideDelay);
    }
  }

  /**
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#onSetComplete()
   * @param scores
   * @param userID
   * @param gameTimeSeconds
   * @param title
   * @param subtitle
   * @param <T>
   * @return
   */
  public <T extends SetScore> Chart getChart(List<T> scores, long userID, int gameTimeSeconds, String title, String subtitle) {
    int pbCorrect = 0;
    int top = 0;
    float totalCorrect = 0;
    System.out.println("getChart : for user " +userID + " scores " + scores.size());

    List<Float> yValuesForUser = new ArrayList<Float>();
    for (SetScore score : scores) {
      if (score.getUserid() == userID) {
        if (score.getCorrect() > pbCorrect) pbCorrect = score.getCorrect();
        yValuesForUser.add((float) score.getCorrect());
        System.out.println("showLeaderboardPlot : for user " +userID + " got " + score);
      }
      else {
        System.out.println("\tshowLeaderboardPlot : for user " +userID + " got " + score);
      }
      if (score.getCorrect() > top) {
        top = score.getCorrect();
      }
      totalCorrect += score.getCorrect();
    }

    return getChart(scores, gameTimeSeconds, pbCorrect, top, totalCorrect, yValuesForUser, title,subtitle);
  }

  private <T extends SetScore> Chart getChart(List<T> scores, int gameTimeSeconds,
                         int pbCorrect, int top, float total, List<Float> yValuesForUser,String title, String subtitle) {
    Chart chart = new Chart()
      .setType(Series.Type.SPLINE)
      .setChartTitleText(title)
      .setChartSubtitleText(subtitle)
      .setMarginRight(10);

    addSeries(gameTimeSeconds, yValuesForUser, chart, "Correct");

    PlotBand personalBest = getPersonalBest(pbCorrect, chart);
    PlotBand topScore = getTopScore(top, chart);
    PlotBand avgScore = getAvgScore(scores, total, chart);

    if (total > 2) {
      if (top != pbCorrect) {
        chart.getYAxis().setPlotBands(
          avgScore,
          personalBest,
          topScore
        );
      } else {
        chart.getYAxis().setPlotBands(
          avgScore,
          personalBest
        );
      }
    }

    configureChart(top, chart);
    return chart;
  }

  private void addSeries(int gameTimeSeconds, List<Float> yValuesForUser, Chart chart, String seriesTitle) {
    Float[] yValues = yValuesForUser.toArray(new Float[0]);

    String seriesLabel = gameTimeSeconds > 0 ? "Correct in " + gameTimeSeconds + " seconds" : seriesTitle;
    Series series = chart.createSeries()
      .setName(seriesLabel)
      .setPoints(yValues);
    chart.addSeries(series);
  }

  private void configureChart(int top, Chart chart) {
    chart.getYAxis().setAxisTitleText("# Correct");
    chart.getXAxis().setAllowDecimals(false);
    chart.getYAxis().setAllowDecimals(true);
    chart.getYAxis().setMin(0);
    chart.getYAxis().setMax(top + 1);
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

  private PlotBand getTopScore(int top, Chart chart) {
    PlotBand topScore = chart.getYAxis().createPlotBand()
      .setColor("#46bf00")
      .setFrom(under(top))
      .setTo(over(top));

    topScore.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText(TOP_SCORE));
    return topScore;
  }

  private PlotBand getPersonalBest(int pbCorrect, Chart chart) {
    float from = under(pbCorrect);
    float to = over(pbCorrect);
    PlotBand personalBest = chart.getYAxis().createPlotBand()
      .setColor("#f18d24")
      .setFrom(from)
      .setTo(to);

    personalBest.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText(PERSONAL_BEST));
    return personalBest;
  }

  private Button getNoButton(final ClickHandler onNo, final Modal modal) {
    Button noButton = null;
    if (onNo != null) {
      noButton = new Button("No");
      noButton.setType(ButtonType.INVERSE);
      noButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          modal.hide();
          onNo.onClick(event);
        }
      });
    }
    return noButton;
  }

  private Button getYesButton(final ClickHandler onYes, final Modal modal) {
    Button yesButton = null;
    if (onYes != null) {
      yesButton = new Button("Yes");
      yesButton.setType(ButtonType.PRIMARY);
      yesButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          modal.hide();
          onYes.onClick(event);
        }
      });
      yesButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          modal.hide();
        }
      });
    }
    return yesButton;
  }

  private float over(float pbCorrect) { return pbCorrect + HALF;  }
  private float under(float pbCorrect) { return pbCorrect - HALF;  }
}