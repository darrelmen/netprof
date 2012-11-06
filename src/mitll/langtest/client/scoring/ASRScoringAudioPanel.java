package mitll.langtest.client.scoring;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.scoring.PretestScore;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/9/12
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRScoringAudioPanel extends ScoringAudioPanel {
  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.ASRRecordAudioPanel#ASRRecordAudioPanel(mitll.langtest.client.LangTestDatabaseAsync, int)
   *
   * @param service
   * @param soundManager
   * @param useFullWidth
   */
  public ASRScoringAudioPanel(LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth) {
    super(service, soundManager, useFullWidth);
  }

  /**
   * @see GoodwaveExercisePanel#getQuestionContent(mitll.langtest.shared.Exercise)
   * @param path
   * @param refSentence
   * @param service
   * @param soundManager
   * @param useFullWidth
   */
  public ASRScoringAudioPanel(String path, String refSentence, LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth) {
    super(path, refSentence, service, soundManager, useFullWidth);
  }

  /**
   * Shows spinning beachball (ish) gif while we wait...
   * @see ScoringAudioPanel#getTranscriptImageURLForAudio(String, String, String, int, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck)
   * @param path
   * @param refAudio
   * @param refSentence
   * @param wordTranscript
   * @param phoneTranscript
   * @param speechTranscript
   * @param toUse
   * @param height
   * @param reqid
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

    service.getASRScoreForAudio(reqid, path, refSentence, toUse, height, new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {}
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
