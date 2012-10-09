package mitll.langtest.client.scoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
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
  public ASRScoringAudioPanel(LangTestDatabaseAsync service, SoundManagerAPI soundManager) {
    super(service, soundManager);
  }

  /**
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
    System.out.println("scoring audio " + path +" with ref sentence " + refSentence + " reqid " + reqid);
    service.getScoreForAudioFile(reqid, path, refSentence, toUse, height, new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(PretestScore result) {
        if (isMostRecentRequest("score",result.reqid)) {
          useResult(result, wordTranscript, phoneTranscript, speechTranscript, tested.contains(path));
          tested.add(path);
        }
      }
    });
  }
}
