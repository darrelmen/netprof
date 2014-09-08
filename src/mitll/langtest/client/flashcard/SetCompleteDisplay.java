package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import org.moxieapps.gwt.highcharts.client.Chart;

import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 9/8/14.
 */
public class SetCompleteDisplay {
  private static final String CORRECT_NBSP = "Correct&nbsp;%";
  // private static final String SKIP_THIS_ITEM = "Skip this item";

  private static final String RANK = "Rank";
  private static final String NAME = "Name";
  private static final String SCORE = "Score";
  private static final String SCORE_SUBTITLE = "score %";
  private static final String CORRECT_SUBTITLE = "% correct";
  private static final int ROWS_IN_TABLE = 7;
 // private static final String SKIP_TO_END = "See your scores";
  private static final int TABLE_WIDTH = 2 * 275;
  private static final int HORIZ_SPACE_FOR_CHARTS = (1250 - TABLE_WIDTH);

  public Widget showFeedbackCharts(List<AVPHistoryForList> result,
                                   Map<String, Double> exToScore, int numCorrect, int numIncorrect, int numExercises) {
    Panel container = new HorizontalPanel();

    // add left chart and table
    AVPHistoryForList sessionAVPHistoryForList = result.get(0);
    Chart chart = makeCorrectChart(result, sessionAVPHistoryForList, numCorrect, numIncorrect, numExercises);
    container.add(chart);
    container.add(makeTable(sessionAVPHistoryForList, CORRECT_NBSP));

    // add right chart and table
    AVPHistoryForList sessionAVPHistoryForListScore = result.get(1);
    Chart chart2 = makePronChart(getAvgScore(exToScore), sessionAVPHistoryForListScore);
    container.add(chart2);
    container.add(makeTable(sessionAVPHistoryForListScore, SCORE));
    return container;
  }

  private Chart makeCorrectChart(List<AVPHistoryForList> result, AVPHistoryForList sessionAVPHistoryForList,
                                 int totalCorrect, int totalIncorrect, int numExercises) {
   // int totalCorrect = getCorrect();
   // int totalIncorrect = getIncorrect();
    int all = totalCorrect + totalIncorrect;
    System.out.println("onSetComplete.onSuccess : results " + result + " " + (numExercises
        //    -skipped.size()
    ) +
        " all " + all + " correct " + totalCorrect + " inc " + totalIncorrect);

    return makeChart(totalCorrect, all, sessionAVPHistoryForList,totalIncorrect,numExercises);
  }

  private Chart makePronChart(double avgScore, AVPHistoryForList sessionAVPHistoryForListScore) {
    String pronunciation = "Pronunciation " + toPercent(avgScore);
    Chart chart2 = new LeaderboardPlot().getChart(sessionAVPHistoryForListScore, pronunciation, SCORE_SUBTITLE);
    scaleCharts(chart2);
    return chart2;
  }

  /**
   * @param totalCorrect
   * @param numAttempted
   * @param sessionAVPHistoryForList
   * @return
   * @see #makeCorrectChart(java.util.List, mitll.langtest.shared.flashcard.AVPHistoryForList, int, int, int)
   */
  private Chart makeChart(int totalCorrect, int numAttempted, AVPHistoryForList sessionAVPHistoryForList,
                           int incorrent, int numExercises) {
    String suffix = getSkippedSuffix(totalCorrect,incorrent,numExercises);
    String correct = totalCorrect + " of " + numAttempted +
        " Correct (" + toPercent(totalCorrect, numAttempted) + ")" + suffix;
    Chart chart = new LeaderboardPlot().getChart(sessionAVPHistoryForList, correct, CORRECT_SUBTITLE);

    scaleCharts(chart);

    return chart;
  }

  private String getSkippedSuffix(int correct, int incorrect, int numExercises) {
    int attempted = correct + incorrect;
    int skipped = numExercises - attempted;
    String suffix = "";
    if (skipped > 0 && attempted > 0) {
      suffix += ", " + skipped + " skipped";
    }
    return suffix;
  }

  private void scaleCharts(Chart chart) {
    float yRatio = needToScaleY();

    boolean neither = true;

    int chartWidth = (Window.getClientWidth() - TABLE_WIDTH) / 2 - 10;
    chart.setWidth(chartWidth);

    if (yRatio < 1) {
      chart.setHeight(Math.min(400f, Math.round(400f * yRatio)));
      neither = false;
    }

    if (neither) chart.addStyleName("chartDim");
  }

  private float needToScaleX() {
    float width = (float) Window.getClientWidth() - TABLE_WIDTH;
    return width / HORIZ_SPACE_FOR_CHARTS;
  }

  private float needToScaleY() {
    float height = (float) Window.getClientHeight();
    return height / 707;
  }

  /**
   * Make a three column table -- rank, name, and score
   *
   * @param sessionAVPHistoryForList
   * @param scoreColHeader
   * @return
   */
  private Table makeTable(AVPHistoryForList sessionAVPHistoryForList, String scoreColHeader) {
    Table table = new Table();
    table.getElement().setId("LeaderboardTable");
    TableHeader w = new TableHeader(RANK);
    table.add(w);
    table.add(new TableHeader(NAME));
    table.add(new TableHeader(scoreColHeader));
    boolean scale = needToScaleX() < 1;

    List<AVPHistoryForList.UserScore> scores = sessionAVPHistoryForList.getScores();
    int size = scale ? Math.min(ROWS_IN_TABLE, scores.size()) : scores.size();

/*      if (scale) System.out.println("scale! client " +Window.getClientWidth()+
        " : using " + size + " vs " + scores.size());*/

    int used = 0;
    for (int i = 0; i < scores.size(); i++) {
      AVPHistoryForList.UserScore userScore = scores.get(i);
      if (used++ < size || userScore.isCurrent()) {
        HTMLPanel row = new HTMLPanel("tr", "");
        if (i % 2 == 0) row.addStyleName("tableAltRowColor");

        // add index col
        HTMLPanel col = new HTMLPanel("td", "");
        col.add(new HTML(bold(userScore, "" + userScore.getIndex())));
        row.add(col);

        // add user name col
        col = new HTMLPanel("td", "");
        HTML widget = new HTML("<b>" + userScore.getUser() + "</b>");
        widget.addStyleName(userScore.isCurrent() ? "tableRowUserCurrentColor" : "tableRowUserColor");
        col.add(widget);
        row.add(col);

        // add score
        col = new HTMLPanel("td", "");
        String html = "" + Math.round(userScore.getScore());
        html = bold(userScore, html);
        col.add(new HTML(html));
        row.add(col);

        table.add(row);
      }
    }
    return table;
  }

  private String bold(AVPHistoryForList.UserScore score, String html) {
    return score.isCurrent() ? "<b>" + html + "</b>" : html;
  }


  /**
   * @see MyFlashcardExercisePanelFactory.StatsPracticePanel#showFeedbackCharts
   * @return
   */
  private double getAvgScore(Map<String,Double> exToScore) {
    double count = 0;
    float num = 0f;
    for (Double val : exToScore.values()) {
      if (val > 0) {
        count += val;
        num++;
      }
    }
    return count/num;
  }

  private String toPercent(int numer, int denom) {
    return ((int) ((((float)numer) * 100f) / denom)) + "%";
  }
  private String toPercent(double num) {
    return ((int) (num * 100f)) + "%";
  }

}