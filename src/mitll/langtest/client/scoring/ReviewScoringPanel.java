/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 7/17/15.
 */
public class ReviewScoringPanel extends ScoringAudioPanel {
  private static final int WIDTH_MARGIN = 230;
  private Logger logger = Logger.getLogger("ReviewScoringPanel");
  private HTML scoreInfo;
  private Panel tablesContainer, belowContainer;

  /**
   * @param refSentence
   * @param service
   * @param controller
   * @param exerciseID
   * @param exercise
   *@param instance @see mitll.langtest.client.result.ResultManager#getAsyncTable(int, Widget)
   */
  public ReviewScoringPanel(String path, String refSentence, LangTestDatabaseAsync service, ExerciseController controller, String exerciseID, CommonExercise exercise, String instance) {
    super(path, refSentence, service, controller, false, new EmptyScoreListener(), 23, "", exerciseID, exercise, instance);
    tablesContainer = new HorizontalPanel();
    tablesContainer.getElement().setId("TablesContainer");
    belowContainer = new DivWidget();
    addStyleName("topFiveMargin");
    addStyleName("leftFiveMargin");
    addStyleName("rightFiveMargin");
  }

  @Override
  protected Widget getAfterPlayWidget() {
    scoreInfo = new HTML();
    scoreInfo.addStyleName("leftFiveMargin");
    scoreInfo.getElement().setId("scoreInfo");
    return scoreInfo;
  }

  /**
   * @see #scoreAudio(String, long, String, ImageAndCheck, ImageAndCheck, int, int, int)
   * @param label
   * @param scoreColHeader
   * @param scores
   * @return
   */
  private Table makeTable(String label, String scoreColHeader, Map<String, Float> scores) {
    Table table = new Table();
    table.getElement().setId("ReviewScoreTable_" + label + "_" + scoreColHeader.substring(0, 3));
    table.add(new TableHeader(label));
    table.add(new TableHeader(scoreColHeader));

    if (scores == null) {
      logger.warning("scores is null?");
    } else {
      List<String> keys = new ArrayList<String>(scores.keySet());
      Collections.sort(keys);
      for (String key : keys) {
        HTMLPanel row = new HTMLPanel("tr", "");

        // add index col
        HTMLPanel col = new HTMLPanel("td", "");
        col.add(new HTML(key));
        row.add(col);

        // add score
        col = new HTMLPanel("td", "");
        String html = "" + getScore(scores, key);
        col.add(new HTML(html));
        row.add(col);

        table.add(row);
      }
    }
    return table;
  }

  private Table makeTableHoriz(String label, String scoreColHeader, Map<String, Float> scores) {
    Table table = new Table();
    table.getElement().setId("LeaderboardTable_" + label + "_" + scoreColHeader.substring(0, 3));
    //table.add(new TableHeader(scoreColHeader));

    if (scores == null) {
      logger.warning("scores is null?");
    } else {
      List<String> keys = new ArrayList<String>(scores.keySet());
      Collections.sort(keys);

      HTMLPanel row = new HTMLPanel("tr", "");
      table.add(row);
      table.add(new TableHeader(label));

      HTMLPanel col = new HTMLPanel("td", "");
      col.add(new HTML("<b>" + scoreColHeader + "</b>"));
      row.add(col);

      for (String key : keys) {
        table.add(new TableHeader(key));
        // add score
        col = new HTMLPanel("td", "");
        String html = "" + getScore(scores, key);
        col.add(new HTML(html));
        row.add(col);
      }
    }
    return table;
  }

  private int getScore(Map<String, Float> scores, String key) {
    Float aFloat = scores.get(key);
    return getPercent(aFloat);
  }

  private int getPercent(Float aFloat) {
    return getScore(aFloat * 100);
  }

  private int getScore(float a) {
    return Math.round(a);
  }

  @Override
  protected int getWidthForWaveform(int leftColumnWidth1, int leftColumnWidth, int rightSide) {
    return Window.getClientWidth() - WIDTH_MARGIN;
  }

  /**
   * @param path
   * @param resultID
   * @param refSentence IGNORED!
   * @param wordTranscript
   * @param phoneTranscript
   * @param width
   * @param height
   * @param reqid
   * @see ScoringAudioPanel#getTranscriptImageURLForAudio(String, String, int, ImageAndCheck, ImageAndCheck)
   */
  @Override
  protected void scoreAudio(String path, long resultID, String refSentence, final ImageAndCheck wordTranscript,
                            final ImageAndCheck phoneTranscript, int width, int height, int reqid) {
    // logger.info("ReviewScoringPanel.scoreAudio : path " + path + " width " + width + " height " + height);
    boolean wasVisible = wordTranscript.getImage().isVisible();
    belowContainer.setWidth(width + "px");

    // only show the spinning icon if it's going to take awhile
    final Timer t = new Timer() {
      @Override
      public void run() {
        wordTranscript.setUrl(LangTest.LANGTEST_IMAGES + "animated_progress44.gif");
        phoneTranscript.setVisible(false);
      }
    };

    // Schedule the timer to run once in 1 seconds.
    t.schedule(wasVisible ? 1000 : 1);

    service.getResultASRInfo(resultID, width, height, new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {
        wordTranscript.getImage().setVisible(false);
        phoneTranscript.getImage().setVisible(false);
      }

      public void onSuccess(PretestScore result) {
        logger.info("scoreAudio : req " + result);

        t.cancel();
        if (result != null) {
          useResult(result, wordTranscript, phoneTranscript, false, "");

          float hydecScore = result.getHydecScore();
          float zeroToHundred = hydecScore * 100f;
          String html = "Score : <b>" + getScore(Math.min(100.0f, zeroToHundred)) + "%</b>";
          scoreInfo.setHTML(html);

          // logger.info("Setting " + scoreInfo.getElement().getId() + " to " + html);
          tablesContainer.clear();
          belowContainer.clear();

          if (result.getWordScores() != null) {
            if (!result.getWordScores().isEmpty()) {
              Table wordTable = makeTable("Word", "Score", result.getWordScores());

              ScrollPanel child = new ScrollPanel(wordTable);
              child.getElement().setId("TableScroller_Word");
              child.setWidth("170px");
              child.setHeight("200px");
              tablesContainer.add(child);
            }

            if (!result.getPhoneScores().isEmpty()) {
              Table phoneTable = makeTableHoriz("Phone", "Avg. Score", result.getPhoneScores());
              phoneTable.getElement().getStyle().setMarginBottom(3, Style.Unit.PX);
              phoneTable.addStyleName("topFiveMargin");

              DivWidget left = new DivWidget();
              left.addStyleName("floatLeft");
              left.add(phoneTable);

              belowContainer.add(left);
              Widget table2 = getWordTable(result);
              table2.addStyleName("topFiveMargin");
              table2.addStyleName("leftFiveMargin");
              table2.addStyleName("floatLeft");
              belowContainer.add(table2);
              belowContainer.add(new DivWidget());
            }
          }
        }
      }
    });
  }

  /**
   * @see #scoreAudio(String, long, String, ImageAndCheck, ImageAndCheck, int, int, int)
   * @param score
   * @return
   */
  private Widget getWordTable(PretestScore score) {
    Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime = score.getsTypeToEndTimes();
    return new WordTable().getWordTable(netPronImageTypeToEndTime);
  }

  /**
   * @return
   * @see mitll.langtest.client.result.ResultManager#getAsyncTable(int)
   */
  public Widget getTables() {
    return tablesContainer;
  }
  public Widget getBelow()  { return belowContainer; }
}
