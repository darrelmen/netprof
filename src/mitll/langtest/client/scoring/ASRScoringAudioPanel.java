package mitll.langtest.client.scoring;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.HashSet;
import java.util.Set;

/**
 * Does ASR scoring -- adds phone and word transcript images below waveform and spectrum
 * User: GO22670
 * Date: 10/9/12
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoringAudioPanel extends ScoringAudioPanel {
  public static final String SCORE = "score";
  private final Set<String> tested = new HashSet<String>();
  private boolean useScoreToColorBkg = true;

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.ASRRecordAudioPanel#ASRRecordAudioPanel(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param refSentence
   * @param service
   * @param gaugePanel
   * @param playButtonSuffix
   * @param exerciseID
   */
  public ASRScoringAudioPanel(String refSentence, LangTestDatabaseAsync service, ExerciseController controller, ScoreListener gaugePanel,
                              String playButtonSuffix, String exerciseID) {
    super(refSentence, service, controller, gaugePanel, playButtonSuffix, exerciseID);
  }

  /**
   * @see GoodwaveExercisePanel#getAudioPanel(mitll.langtest.shared.CommonExercise, String)
   * @param path
   * @param refSentence
   * @param service
   * @param controller
   * @param showSpectrogram
   * @param gaugePanel
   * @param rightMargin
   * @param playButtonSuffix
   * @param exerciseID
   */
  public ASRScoringAudioPanel(String path, String refSentence, LangTestDatabaseAsync service,
                              ExerciseController controller, boolean showSpectrogram, ScoreListener gaugePanel,
                              int rightMargin, String playButtonSuffix, String exerciseID) {
    super(path, refSentence, service, controller, showSpectrogram, gaugePanel, rightMargin, playButtonSuffix, exerciseID);
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
                            int toUse, int height, int reqid) {
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

    //System.out.println("ASRScoringAudioPanel.scoreAudio : req " + reqid + " path " + path + " type " + "score" + " width " + toUse);

    service.getASRScoreForAudio(reqid, resultID, path, refSentence, toUse, height, useScoreToColorBkg, exerciseID, new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {
        //if (!caught.getMessage().trim().equals("0")) {
        //  Window.alert("Server error -- couldn't contact server.");
        //}
        wordTranscript.image.setVisible(false);
        phoneTranscript.image.setVisible(false);
      }

      public void onSuccess(PretestScore result) {
        t.cancel();

        if (isMostRecentRequest(SCORE, result.getReqid())) {
          useResult(result, wordTranscript, phoneTranscript, tested.contains(path), path);
          tested.add(path);
        }
      }
    });
  }
}
