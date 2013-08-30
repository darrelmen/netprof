package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import mitll.langtest.shared.flashcard.Leaderboard;
import mitll.langtest.shared.flashcard.ScoreInfo;
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

  public void showLeaderboardPlot(Leaderboard leaderboard, final long userID, int gameTimeSeconds,
                                  Map<String, Collection<String>> currentSelection,
                                  String prompt,
                                  final ClickHandler onYes, final ClickHandler onNo) {
    List<ScoreInfo> scores = leaderboard.getScores(currentSelection);

    showLeaderboardPlot(scores, userID, gameTimeSeconds, currentSelection, prompt,onYes, onNo);
  }

  private void showLeaderboardPlot(List<ScoreInfo> scores, final long userID, int gameTimeSeconds,
                                  Map<String, Collection<String>> currentSelection,
                                  String prompt,
                                  final ClickHandler onYes, final ClickHandler onNo) {
    int pbCorrect = 0;
    int top = 0;
    float total = 0;
    List<Float> yValuesForUser = new ArrayList<Float>();
    for (ScoreInfo score : scores) {
      if (score.userid == userID) {
        if (score.correct > pbCorrect) pbCorrect = score.correct;
        yValuesForUser.add((float) score.correct);
        System.out.println("got " + score);
      }
      if (score.correct > top) top = score.correct;
      total += score.correct;
    }
    float avg = total / (float) scores.size();

    final Modal modal = new Modal();
    modal.setAnimation(false);
    modal.setCloseVisible(true);
    modal.setWidth("720px");
    String hashMapToStringCleaned =
      currentSelection.toString().replace("{", "").replace("}", "").replace("=", " ").replace("[", "").replace("]", "");
    Chart chart = new Chart()
      .setType(Series.Type.SPLINE)
      .setChartTitleText("Leaderboard")
      .setChartSubtitleText(hashMapToStringCleaned)
      .setMarginRight(10);

    Float[] yValues = yValuesForUser.toArray(new Float[0]);

    String seriesLabel = gameTimeSeconds > 0 ? "Correct in " + gameTimeSeconds + " seconds" : "Score";
    Series series = chart.createSeries()
      .setName(seriesLabel)
      .setPoints(yValues);
    chart.addSeries(series);

    float from = under(pbCorrect);
    float to = over(pbCorrect);
    PlotBand personalBest = chart.getYAxis().createPlotBand()
      .setColor("#f18d24")
      .setFrom(from)
      .setTo(to);

    personalBest.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText("Personal Best"));

    PlotBand topScore = chart.getYAxis().createPlotBand()
      .setColor("#46bf00")
      .setFrom(under(top))
      .setTo(over(top));

    topScore.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText("Top Score"));

    PlotBand avgScore = chart.getYAxis().createPlotBand()
      .setColor("#2031ff")
      .setFrom(under(avg))
      .setTo(over(avg));

    avgScore.setLabel(new PlotBandLabel().setAlign(PlotBandLabel.Align.LEFT).setText("Course Average"));

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

    chart.getYAxis().setAxisTitleText("# Correct");
    chart.getXAxis().setAllowDecimals(false);
    chart.getYAxis().setAllowDecimals(true);
    chart.getYAxis().setMin(0);
    chart.getYAxis().setMax(top + 2);

    modal.setMaxHeigth("650px");
    modal.setHeight("550px");
    modal.add(chart);
    Button yesButton = new Button();
/*    yesButton.setHeight("30px");
    yesButton.setWidth("50px");*/
    yesButton.setType(ButtonType.PRIMARY);
    yesButton.setText("Yes");
    yesButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        modal.hide();
       // bootstrapFlashcardExerciseList.goAgain(userID);
        onYes.onClick(event);
      }
    });

    FluidRow row = new FluidRow();
    row.add(new Column(4, yesButton));
    row.add(new Column(4, new Heading(4)));
    Button noButton = new Button("No");
    noButton.setType(ButtonType.INVERSE);

    row.add(new Column(4, noButton));
    noButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        modal.hide();
     //   bootstrapFlashcardExerciseList.stopForNow(userID);
        onNo.onClick(event);
      }
    });

    yesButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        modal.hide();
      }
    });

    FluidRow row1 = new FluidRow();
    Column column = new Column(12);
    row1.add(column);
//    String prompt = "Would you like to try again?";
    column.add(new Heading(4, prompt));
    modal.add(row1);
    modal.add(row);
    modal.show();
  }

  private float over(float pbCorrect) {
    return pbCorrect + HALF;
  }

  private float under(float pbCorrect) {
    return pbCorrect - HALF;
  }
}