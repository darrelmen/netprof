package mitll.langtest.client.scoring;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.scoring.PretestScore;

/**
 * Does ASR scoring -- adds phone and word transcript images below waveform and spectrum
 * User: GO22670
 * Date: 10/9/12
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoringAudioPanel extends ScoringAudioPanel {
  private boolean useScoreToColorBkg = true;

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.ASRRecordAudioPanel#ASRRecordAudioPanel(mitll.langtest.client.LangTestDatabaseAsync, int)
   * @param service
   * @param soundManager
   * @param useFullWidth
   * @param numRepeats
   * @param useKeyboard
   */
  public ASRScoringAudioPanel(LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth,
                              int numRepeats, boolean useKeyboard) {
    super(service, soundManager, useFullWidth, numRepeats, useKeyboard);
  }

  /**
   * @see GoodwaveExercisePanel#getScoringAudioPanel(mitll.langtest.shared.Exercise, String)
   * @param path
   * @param refSentence
   * @param service
   * @param controller
   * @param useKeyboard
   */
  public ASRScoringAudioPanel(String path, String refSentence, LangTestDatabaseAsync service,
                              ExerciseController controller, boolean useKeyboard) {
    super(path, refSentence, service, controller.getSoundManager(), controller.showOnlyOneExercise(),
        controller.getSegmentRepeats(), useKeyboard);
    this.useScoreToColorBkg = controller.useBkgColorForRef();
  }

  /**
   * Shows spinning beachball (ish) gif while we wait...
   * @see ScoringAudioPanel#getTranscriptImageURLForAudio(String, String, String, int, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck)
   * @param path to audio file on server
   * @param refAudio IGNORED HERE
   * @param refSentence what should be aligned
   * @param wordTranscript image panel that needs a URL pointing to an image generated on the server
   * @param phoneTranscript image panel that needs a URL pointing to an image generated on the server
   * @param speechTranscript IGNORED HERE
   * @param toUse width of images made on serer
   * @param height of images returned
   * @param reqid so if many requests are made quickly and the returns are out of order, we can ignore older requests
   */
  protected void scoreAudio(final String path, String refAudio, String refSentence,
                            final ImageAndCheck wordTranscript, final ImageAndCheck phoneTranscript,
                            final ImageAndCheck speechTranscript, int toUse, int height, int reqid) {
    //System.out.println("scoring audio " + path +" with ref sentence " + refSentence + " reqid " + reqid);
    boolean wasVisible = wordTranscript.image.isVisible();

    // only show the spinning icon if it's going to take awhile
    final Timer t = new Timer() {
      @Override
      public void run() {
        wordTranscript.image.setUrl(LangTest.LANGTEST_IMAGES +"animated_progress.gif");
        wordTranscript.image.setVisible(true);
        phoneTranscript.image.setVisible(false);
      }
    };

    // Schedule the timer to run once in 1 seconds.
    t.schedule(wasVisible ? 1000 : 1);

    service.getASRScoreForAudio(reqid, path, refSentence, toUse, height, useScoreToColorBkg, new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {
        Window.alert("Server error -- please report.");
        wordTranscript.image.setVisible(false);
        phoneTranscript.image.setVisible(false);
      }
      public void onSuccess(PretestScore result) {
        t.cancel();

        if (isMostRecentRequest("score", result.getReqid())) {
          useResult(result, wordTranscript, phoneTranscript, speechTranscript, tested.contains(path));
          tested.add(path);
        } else {
          System.out.println("ignoring " + path + " with req " + result.getReqid());
        }
      }
    });
  }
}
