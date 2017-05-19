package mitll.langtest.client.scoring;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/30/17.
 */
public class WordScoresTable {
  private final Logger logger = Logger.getLogger("WordScoresTable");

  /**
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#showCorrectFeedback(double, String, PretestScore)
   * @param result
   * @param audioControl
   * @param typeToSegmentToWidget
   * @param isRTL
   * @return
   */
  public Widget getStyledWordTable(PretestScore result,
                                   AudioControl audioControl,
                                   Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget,
                                   boolean isRTL) {
    Widget table2 = new WordTable().getDivWord(result.getTypeToSegments(), audioControl, typeToSegmentToWidget, isRTL);
    table2.addStyleName("topFiveMargin");
    table2.addStyleName("leftFiveMargin");
    table2.addStyleName("floatLeftAndClear");
    return table2;
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

/*
  private int getScore(Map<String, Float> scores, String key) {
    return getPercent(scores.get(key));
  }
*/

/*
  private int getPercent(Float aFloat) {
    return getScore(aFloat * 100);
  }
*/

/*
  private int getScore(float a) {
    return Math.round(a);
  }
*/
}
