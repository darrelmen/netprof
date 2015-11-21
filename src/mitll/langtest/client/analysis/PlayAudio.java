package mitll.langtest.client.analysis;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.List;

/**
 * Created by go22670 on 11/20/15.
 */
public class PlayAudio {
  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;

  private final LangTestDatabaseAsync service;
  private final SoundPlayer soundFeedback;

  com.google.gwt.user.client.Timer t;

  public PlayAudio(LangTestDatabaseAsync service, SoundPlayer soundFeedback) {
    this.service = service;
    this.soundFeedback = soundFeedback;
  }
  public void playLast(String id, long userid) {
    service.getExercise(id, userid, false, new AsyncCallback<CommonExercise>() {
      @Override
      public void onFailure(Throwable throwable) {
      }

      @Override
      public void onSuccess(CommonExercise commonExercise) {
        List<CorrectAndScore> scores = commonExercise.getScores();
        CorrectAndScore correctAndScore = scores.get(scores.size() - 1);
        String refAudio = commonExercise.getRefAudio();

        if (t != null) t.cancel();
        if (refAudio != null) {
          playLastThenRef(correctAndScore, refAudio);
        }
      }
    });
  }

  public void playLastThenRef(CorrectAndScore correctAndScore, String refAudio) {
    final String path = getPath(refAudio);
    soundFeedback.queueSong(getPath(correctAndScore.getPath()), new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {

      }

      @Override
      public void songEnded() {
        t = new com.google.gwt.user.client.Timer() {
          @Override
          public void run() {
            soundFeedback.queueSong(path);
          }
        };
        t.schedule(100);
      }

    });
  }

  private String getPath(String path) {
    path = (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
    path = ensureForwardSlashes(path);
    return path;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }
}
