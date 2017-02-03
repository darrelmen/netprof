/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Shows recorded audio with scores and alignments in the result manager audio review dialog.
 *
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 7/17/15.
 */
public class ReviewScoringPanel extends ScoringAudioPanel {
  private final Logger logger = Logger.getLogger("ReviewScoringPanel");

  private static final int WIDTH_MARGIN = 230;
  private HTML scoreInfo;
  private final Panel tablesContainer;
  private final Panel belowContainer;

  /**
   * @param refSentence
   * @param controller
   * @paramx exercise
   * @param instance
   * @see mitll.langtest.client.result.ResultManager#getAsyncTable(int, Widget)
   */
  public ReviewScoringPanel(String path,
                            String refSentence,
                            String transliteration,
                            ExerciseController controller,
                            int exerciseID,
                            String instance) {
    super(path, refSentence,transliteration, controller, false, 23, "", null, exerciseID, instance);
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
   * @see ScoringAudioPanel#scoreAudio
   * @param label
   * @param scoreColHeader
   * @param scores
   * @return
   * @see #scoreAudio
   * @see #addWordScoreTable
   */
  private Table makeTable(String label, String scoreColHeader, Map<String, Float> scores) {
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
        col = new HTMLPanel("td", "");
        String html = "" + getScore(scores, key);
        col.add(new HTML(html));
        row.add(col);

        table.add(row);
      }
    }
    return table;
  }

  /**
   * @see #getPhoneScoreTable
   * @param label
   * @param scoreColHeader
   * @param scores
   * @return
   */
  private Table makeTableHoriz(String label, String scoreColHeader, Map<String, Float> scores) {
    Table table = new Table();
    table.getElement().setId("LeaderboardTable_" + label + "_" + scoreColHeader.substring(0, 3));
    //table.add(new TableHeader(scoreColHeader));

    if (scores == null) {
      logger.warning("scores is null?");
    } else {
      List<String> keys = new ArrayList<>(scores.keySet());
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
  protected int getWidthForWaveform(int leftColumnWidth1, int leftColumnWidth) {
    return Window.getClientWidth() - WIDTH_MARGIN;
  }

  /**
   * @param path
   * @param resultID
   * @param refSentence     IGNORED!
   * @param wordTranscript
   * @param phoneTranscript
   * @param width
   * @param height
   * @param reqid
   * @see ScoringAudioPanel#getTranscriptImageURLForAudio
   */
  @Override
  protected void scoreAudio(String path, int resultID, String refSentence, String transliteration,final ImageAndCheck wordTranscript,
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

    controller.getScoringService().getResultASRInfo((int)resultID, new ImageOptions(width, height, true), new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {
        wordTranscript.getImage().setVisible(false);
        phoneTranscript.getImage().setVisible(false);
      }

      public void onSuccess(PretestScore result) {
       // logger.info("scoreAudio : req " + result);

        t.cancel();
        if (result != null) {
          useResult(result, wordTranscript, phoneTranscript, false, "");

          float hydecScore = result.getHydecScore();
          float zeroToHundred = hydecScore * 100f;
          String html = "Score : <b>" + getScore(Math.min(100.0f, zeroToHundred)) + "%</b>";
          scoreInfo.setHTML(html);

          // logger.info("Setting " + scoreInfo.getElement().getExID() + " to " + html);
          tablesContainer.clear();
          belowContainer.clear();

          if (result.getWordScores() != null) {
            if (!result.getWordScores().isEmpty()) {
              addWordScoreTable(result);
            }

            if (!result.getPhoneScores().isEmpty()) {
              DivWidget left = new DivWidget();
              left.addStyleName("floatLeft");
              left.add(getPhoneScoreTable(result));

              belowContainer.add(left);
              belowContainer.add(getStyledWordTable(result));
              belowContainer.add(new DivWidget());
            }
          }
        }
      }
    });
  }

  private Widget getStyledWordTable(PretestScore result) {
    Widget table2 = getWordTable(result);
    table2.addStyleName("topFiveMargin");
    table2.addStyleName("leftFiveMargin");
    table2.addStyleName("floatLeft");
    return table2;
  }

  private Table getPhoneScoreTable(PretestScore result) {
    Table phoneTable = makeTableHoriz("Phone", "Avg. Score", result.getPhoneScores());
    phoneTable.getElement().getStyle().setMarginBottom(3, Style.Unit.PX);
    phoneTable.addStyleName("topFiveMargin");
    return phoneTable;
  }

  private void addWordScoreTable(PretestScore result) {
    Table wordTable = makeTable("Word", "Score", result.getWordScores());

    ScrollPanel child = new ScrollPanel(wordTable);
    child.getElement().setId("TableScroller_Word");
    child.setWidth("170px");
    child.setHeight("200px");
    tablesContainer.add(child);
  }

  /**
   * @see ScoringAudioPanel#scoreAudio
   * @param score
   * @return
   * @see #scoreAudio
   */
  private Widget getWordTable(PretestScore score) {
    Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime = score.getsTypeToEndTimes();
    return new WordTable().getWordTable(netPronImageTypeToEndTime, true);
  }

  /**
   * @return
   * @see mitll.langtest.client.result.ResultManager#getAsyncTable
   */
  public Widget getTables() {
    return tablesContainer;
  }
  public Widget getBelow()  {
    return belowContainer;
  }
}
