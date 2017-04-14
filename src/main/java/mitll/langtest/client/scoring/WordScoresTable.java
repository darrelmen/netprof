package mitll.langtest.client.scoring;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/30/17.
 */
public class WordScoresTable {
  private final Logger logger = Logger.getLogger("WordScoresTable");

/*  private Panel addWordScoreTable(PretestScore result) {
    Table wordTable = makeTable("Word", "Score", result.getWordScores(), true);

    ScrollPanel child = new ScrollPanel(wordTable);
    child.getElement().setId("TableScroller_Word");
    child.setWidth("170px");
    child.setHeight("200px");
    return child;
  }*/

  public Widget getStyledWordTable(PretestScore result) {
    Widget table2 = getWordTable(result);
    table2.addStyleName("topFiveMargin");
    table2.addStyleName("leftFiveMargin");
    table2.addStyleName("floatLeftAndClear");
    return table2;
  }

  /**
   * @see ScoringAudioPanel#scoreAudio
   * @param score
   * @return
   * @see #scoreAudio
   */
  private Widget getWordTable(PretestScore score) {
   // return new WordTable().getWordTable(score.getTypeToSegments(), false);
    return new WordTable().getDivWord(score.getTypeToSegments());
  }

  /**
   * TODO : don't sort!
   * @see ScoringAudioPanel#scoreAudio
   * @param label
   * @param scoreColHeader
   * @param scores
   * @param showScore
   * @return
   * @see #scoreAudio
   * @see #addWordScoreTable
   */
/*  private Table makeTable(String label, String scoreColHeader, Map<String, Float> scores, boolean showScore) {
    Table table = new Table();
    table.getElement().setId("ReviewScoreTable_" + label + "_" + scoreColHeader.substring(0, 3));
    table.add(new TableHeader(label));
    table.add(new TableHeader(scoreColHeader));

    if (scores == null) {
      logger.warning("scores is null?");
    } else {
      List<String> keys = new ArrayList<>(scores.keySet());
      Collections.sort(keys);
      for (String key : keys) {
        HTMLPanel row = new HTMLPanel("tr", "");

        // add index col
        HTMLPanel col = new HTMLPanel("td", "");
        col.add(new HTML(key));
        row.add(col);

        // add score
        if (showScore) {
          col = new HTMLPanel("td", "");
          String html = "" + getScore(scores, key);
          col.add(new HTML(html));
          row.add(col);
        }

        table.add(row);
      }
    }
    return table;
  }*/

  private int getScore(Map<String, Float> scores, String key) {
    return getPercent(scores.get(key));
  }

  private int getPercent(Float aFloat) {
    return getScore(aFloat * 100);
  }

  private int getScore(float a) {
    return Math.round(a);
  }
}
