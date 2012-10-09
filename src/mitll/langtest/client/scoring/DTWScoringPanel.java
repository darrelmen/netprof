package mitll.langtest.client.scoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/9/12
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class DTWScoringPanel extends ScoringAudioPanel {
  public DTWScoringPanel(LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth) {
    super(service, soundManager, useFullWidth);
  }

  @Override
  protected void scoreAudio(final String path, String refAudio, String refSentence,
                            final ImageAndCheck wordTranscript,
                            final ImageAndCheck phoneTranscript,
                            final ImageAndCheck speechTranscript, int toUse, int height, int reqid) {
    Collection<String> refs = new ArrayList<String>();
    refs.add(refAudio);
    service.getScoreForAudioFile(reqid, path, refs, toUse, height, new AsyncCallback<PretestScore>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(PretestScore result) {
        if (isMostRecentRequest("score", result.reqid)) {
          boolean contains = tested.contains(path);
        //  if (contains) System.out.println("already asked to score " + path);
          useResult(result, wordTranscript, phoneTranscript, speechTranscript, contains);
          tested.add(path);
        }
      }
    });
  }
}
