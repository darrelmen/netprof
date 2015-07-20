package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
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
    private Logger logger = Logger.getLogger("ReviewScoringPanel");
    private HTML scoreInfo;
    private Panel tablesContainer;

    /**
     *
     * @param refSentence
     * @param service
     * @param controller
     * @param gaugePanel
     * @param playButtonSuffix
     * @param exerciseID
     */
    public ReviewScoringPanel(String refSentence, LangTestDatabaseAsync service, ExerciseController controller, ScoreListener gaugePanel,
                              String playButtonSuffix, String exerciseID) {
        super(refSentence, service, controller, gaugePanel, playButtonSuffix, exerciseID);
        tablesContainer = new HorizontalPanel();
        tablesContainer.getElement().setId("TablesContainer");
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
        }
        else {
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
                String html = "" + Math.round(scores.get(key)*100);
                col.add(new HTML(html));
                row.add(col);

                table.add(row);
            }
        }
        return table;
    }

    @Override
    protected int getWidthForWaveform(int leftColumnWidth1, int leftColumnWidth, int rightSide) {
        return Window.getClientWidth() - 380;
    }

        /**
         * @see ScoringAudioPanel#getTranscriptImageURLForAudio(String, String, int, ImageAndCheck, ImageAndCheck)
         * @param path
         * @param resultID
         * @param refSentence
         * @param wordTranscript
         * @param phoneTranscript
         * @param width
         * @param height
         * @param reqid
         */
    @Override
    protected void scoreAudio(String path, long resultID, String refSentence, final ImageAndCheck wordTranscript,
                              final ImageAndCheck phoneTranscript, int width, int height, int reqid) {

        boolean wasVisible = wordTranscript.image.isVisible();

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

        logger.info("ReviewScoringPanel.scoreAudio : path " + path + " width " + width + " height " + height);

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
                    String html = "Score : <b>" + Math.round(Math.min(100.0f, zeroToHundred)) +
                            "%</b>";
                    scoreInfo.setHTML(html);

                    // logger.info("Setting " + scoreInfo.getElement().getId() + " to " + html);
                    tablesContainer.clear();

                    if (result.getWordScores() != null) {
                        if (!result.getWordScores().isEmpty()) {
                            Table wordTable = makeTable("Word", "Score", result.getWordScores());

                            ScrollPanel child = new ScrollPanel(wordTable);
                            child.getElement().setId("TableScroller_Word");
                            child.setWidth("150px");
                            child.setHeight("200px");
                            tablesContainer.add(child);
                        }

                        if (!result.getPhoneScores().isEmpty()) {
                            Table phoneTable = makeTable("Phone", "Score", result.getPhoneScores());

                            ScrollPanel child = new ScrollPanel(phoneTable);
                            child.getElement().setId("TableScroller_Phone");
                            child.setWidth("120px");
                            child.setHeight("200px");
                            tablesContainer.add(child);
                        }
                    }
                }
            }
        });
    }

    public Widget getTables() {
        return tablesContainer;
    }
}
