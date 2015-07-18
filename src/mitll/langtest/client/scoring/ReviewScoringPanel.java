package mitll.langtest.client.scoring;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.logging.Logger;

/**
 * Created by go22670 on 7/17/15.
 */
public class ReviewScoringPanel extends ScoringAudioPanel {

    private Logger logger = Logger.getLogger("ReviewScoringPanel");

    public ReviewScoringPanel(String refSentence, LangTestDatabaseAsync service, ExerciseController controller, ScoreListener gaugePanel,
                              String playButtonSuffix, String exerciseID) {
        super(refSentence, service, controller, gaugePanel, playButtonSuffix, exerciseID);

    }

    @Override
    protected void scoreAudio(String path, long resultID, String refSentence, final ImageAndCheck wordTranscript,
                              final ImageAndCheck phoneTranscript, int toUse, int height, int reqid) {
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
        logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " type " + "score" + " width " + toUse);

        service.getResultASRInfo(resultID, toUse, height, new AsyncCallback<PretestScore>() {
            public void onFailure(Throwable caught) {
                wordTranscript.image.setVisible(false);
                phoneTranscript.image.setVisible(false);
            }

            public void onSuccess(PretestScore result) {
                logger.info("scoreAudio : req " + result);

                t.cancel();
                useResult(result, wordTranscript, phoneTranscript, false, "");
            }
        });
    }
}
