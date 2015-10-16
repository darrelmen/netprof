package mitll.langtest.client.scoring;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Does ASR scoring -- adds phone and word transcript images below waveform and spectrum
 * User: GO22670
 * Date: 10/9/12
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoringAudioPanel extends ScoringAudioPanel {
  private Logger logger = Logger.getLogger("ASRScoringAudioPanel");
  public static final String SCORE = "score";
  private final Set<String> tested = new HashSet<String>();
  private boolean useScoreToColorBkg = true;

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.FastAndSlowASRScoringAudioPanel#FastAndSlowASRScoringAudioPanel
   * @param refSentence
   * @param service
   * @param gaugePanel
   * @param playButtonSuffix
   * @param exerciseID
   * @param exercise
   * @param instance
   */
  public ASRScoringAudioPanel(String refSentence, LangTestDatabaseAsync service, ExerciseController controller, ScoreListener gaugePanel,
                              String playButtonSuffix, String exerciseID, CommonExercise exercise, String instance) {
    super(refSentence, service, controller, gaugePanel, playButtonSuffix, exerciseID, exercise, instance);
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.FastAndSlowASRScoringAudioPanel#FastAndSlowASRScoringAudioPanel
   * @param path
   * @param refSentence
   * @param service
   * @param controller
   * @param showSpectrogram
   * @param gaugePanel
   * @param rightMargin
   * @param playButtonSuffix
   * @param exerciseID
   * @param exercise
   * @param instance
   */
  public ASRScoringAudioPanel(String path, String refSentence, LangTestDatabaseAsync service,
                              ExerciseController controller, boolean showSpectrogram, ScoreListener gaugePanel,
                              int rightMargin, String playButtonSuffix, String exerciseID, CommonExercise exercise, String instance) {
    super(path, refSentence, service, controller, showSpectrogram, gaugePanel, rightMargin, playButtonSuffix, exerciseID, exercise, instance);
    this.useScoreToColorBkg = controller.useBkgColorForRef();
  }

  public void setShowColor(boolean v) { this.useScoreToColorBkg = v;}

  /**
   * Shows spinning beachball (ish) gif while we wait...
   * @see ScoringAudioPanel#getTranscriptImageURLForAudio
   * @param path to audio file on server
   * @param resultID
   * @param refSentence what should be aligned
   * @param wordTranscript image panel that needs a URL pointing to an image generated on the server
   * @param phoneTranscript image panel that needs a URL pointing to an image generated on the server
   * @param toUse width of images made on serer
   * @param height of images returned
   * @param reqid so if many requests are made quickly and the returns are out of order, we can ignore older requests
   */
  protected void scoreAudio(final String path, long resultID, String refSentence,
                            final ImageAndCheck wordTranscript, final ImageAndCheck phoneTranscript,
                            int toUse, int height, final int reqid) {
    if (path == null) return;
    //System.out.println("scoring audio " + path +" with ref sentence " + refSentence + " reqid " + reqid);
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

    //logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " type " + "score" + " width " + toUse);

    AsyncCallback<PretestScore> async = new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {
        //if (!caught.getMessage().trim().equals("0")) {
        //  Window.alert("Server error -- couldn't contact server.");
        //}
        //  logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " failure? "+ caught.getMessage());
        wordTranscript.image.setVisible(false);
        phoneTranscript.image.setVisible(false);
      }

      public void onSuccess(PretestScore result) {
        t.cancel();

        if (isMostRecentRequest(SCORE, result.getReqid())) {
          //  logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " success " + result);
          useResult(result, wordTranscript, phoneTranscript, tested.contains(path), path);
          tested.add(path);
        } else {
          //logger.info("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " success " + result + " DISCARDING : " + reqs);
        }
      }
    };

    if (controller.getProps().shouldUsePhoneToDisplay()) {
      service.getASRScoreForAudioPhonemes(reqid, resultID, path, refSentence, toUse, height, useScoreToColorBkg, exerciseID, async);
    } else {
      service.getASRScoreForAudio(reqid, resultID, path, refSentence, toUse, height, useScoreToColorBkg, exerciseID, async);
    }
  }
}
