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
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 7/17/15.
 */
public class ReviewScoringPanel extends ScoringAudioPanel {
  public static final int WIDTH_MARGIN = 230;
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

  private Table makeTable(String label, String scoreColHeader, Map<String, Float> scores) {
    Table table = new Table();
    table.getElement().setId("LeaderboardTable_" + label + "_" + scoreColHeader.substring(0, 3));
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

    boolean wasVisible = wordTranscript.image.isVisible();

    belowContainer.setWidth(width + "px");

    // only show the spinning icon if it's going to take awhile
    final Timer t = new Timer() {
      @Override
      public void run() {
        wordTranscript.image.setUrl(LangTest.LANGTEST_IMAGES + "animated_progress44.gif");
        wordTranscript.image.setVisible(true);
        phoneTranscript.image.setVisible(false);
      }
    };

    // Schedule the timer to run once in 1 seconds.
    t.schedule(wasVisible ? 1000 : 1);

    service.getResultASRInfo(resultID, width, height, new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {
        wordTranscript.image.setVisible(false);
        phoneTranscript.image.setVisible(false);
      }

      public void onSuccess(PretestScore result) {
        logger.info("scoreAudio : req " + result);

        t.cancel();
        if (result != null) {
          useResult(result, wordTranscript, phoneTranscript, false, "");

          float hydecScore = result.getHydecScore();
          float zeroToHundred = hydecScore * 100f;
          String html = "Score : <b>" + getScore(Math.min(100.0f, zeroToHundred)) +
              "%</b>";
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
              Widget table2 = getTable2(result);
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

  private Map<TranscriptSegment, List<TranscriptSegment>> getWordToPhones(PretestScore score) {
    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = new HashMap<>();

    List<TranscriptSegment> words = score.getsTypeToEndTimes().get(NetPronImageType.WORD_TRANSCRIPT);
    List<TranscriptSegment> phones = score.getsTypeToEndTimes().get(NetPronImageType.PHONE_TRANSCRIPT);
    for (TranscriptSegment word : words) {
      String event = word.getEvent();
      if (event.equals("sil") || event.equals("<s>") || event.equals("</s>")) {

      } else {
        for (TranscriptSegment phone : phones) {
          if (phone.getStart() >= word.getStart() && phone.getEnd() <= word.getEnd()) {
            List<TranscriptSegment> orDefault = wordToPhones.get(word);
            if (orDefault == null) wordToPhones.put(word, orDefault = new ArrayList<TranscriptSegment>());
            orDefault.add(phone);
          }
        }
      }
    }
    return wordToPhones;
  }

  private Widget getTable2(PretestScore score) {
    Table table = new Table();
    table.getElement().setId("WordTable");
    table.getElement().getStyle().clearWidth();
    table.removeStyleName("table");

    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(score);

    HTMLPanel srow = new HTMLPanel("tr", "");
    table.add(srow);

    HTMLPanel row = new HTMLPanel("tr", "");
    table.add(row);


    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : wordToPhones.entrySet()) {
      TranscriptSegment word = pair.getKey();

      TableHeader w = new TableHeader(word.getEvent());
      w.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
      String color = SimpleColumnChart.getColor(word.getScore());
      w.getElement().getStyle().setBackgroundColor(color);

      table.add(w);

      HTMLPanel col;
      srow.add(col = new HTMLPanel("td", ""));
      HTML wscore = new HTML("" + getPercent(word.getScore()));
      wscore.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);

      col.add(wscore);

      Table pTable = new Table();
      pTable.removeStyleName("table");

      col = new HTMLPanel("td", "");
      col.getElement().getStyle().clearBorderStyle();
      col.add(pTable);
      row.add(col);

      HTMLPanel row2 = new HTMLPanel("tr", "");
      pTable.add(row2);

      HTMLPanel row3 = new HTMLPanel("tr", "");
      pTable.add(row3);

      for (TranscriptSegment phone : pair.getValue()) {

        String event = phone.getEvent();
        if (!event.equals("sil")) {
          TableHeader h = new TableHeader(event);
          h.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
          pTable.add(h);
          String color1 = SimpleColumnChart.getColor(phone.getScore());
          h.getElement().getStyle().setBackgroundColor(color1);

          //  HTML widget = new HTML(" <b>" + getPercent(phone.getScore()) +"</b>");
          HTML widget = new HTML("" + getPercent(phone.getScore()));
          widget.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
          widget.getElement().getStyle().setWidth(25, Style.Unit.PX);

          col = new HTMLPanel("td", "");
          col.add(widget);
          row3.add(col);
        }
      }
    }

    return table;
  }

  /**
   * @return
   * @see mitll.langtest.client.result.ResultManager#getAsyncTable(int)
   */
  public Widget getTables() {
    return tablesContainer;
  }

  public Widget getBelow() {

    return belowContainer;
  }
}
