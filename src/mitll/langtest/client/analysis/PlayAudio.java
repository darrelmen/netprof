/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Icon;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 11/20/15.
 */
class PlayAudio {
//  private final Logger logger = Logger.getLogger("PlayAudio");

  private final LangTestDatabaseAsync service;
  private final SoundPlayer soundFeedback;
  private final Widget playFeedback;

  private Timer t;

  /**
   * @param service
   * @param soundFeedback
   * @param playFeedback
   * @see AnalysisPlot#AnalysisPlot(LangTestDatabaseAsync, long, String, int, SoundManagerAPI, Icon)
   */
  PlayAudio(LangTestDatabaseAsync service, SoundPlayer soundFeedback, Widget playFeedback) {
    this.service = service;
    this.soundFeedback = soundFeedback;
    this.playFeedback = playFeedback;
  }

  /**
   * @param id
   * @param userid
   */
  void playLast(String id, int userid) {
    service.getExercise(id, userid, false, new AsyncCallback<CommonExercise>() {
      @Override
      public void onFailure(Throwable throwable) {
      }

      @Override
      public void onSuccess(CommonExercise commonExercise) {
        List<CorrectAndScore> scores = commonExercise.getScores();
        CorrectAndScore correctAndScore = scores.get(scores.size() - 1);
        String refAudio = commonExercise.getRefAudio();

        if (t != null) {
         // logger.info("cancel timer");
          t.cancel();
        }
        if (refAudio != null) {
          playLastThenRef(correctAndScore, refAudio);
        } else {
          playUserAudio(correctAndScore);
       //   logger.info("no ref audio for " + commonExercise.getID());
        }
      }
    });
  }

  /**
   * @param correctAndScore
   * @param refAudio
   */
  private void playLastThenRef(CorrectAndScore correctAndScore, String refAudio) {
    final String path = getPath(refAudio);
    final String path1 = getPath(correctAndScore.getPath());
    soundFeedback.queueSong(path1, new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        playFeedback.setVisible(true);
       // logger.info("\t songStarted song " + path1 + " -------  " + System.currentTimeMillis());
      }

      @Override
      public void songEnded() {
        playFeedback.setVisible(false);

      //  logger.info("\t songEnded song " + path1 + " -------  " + System.currentTimeMillis());
        t = new Timer() {
          @Override
          public void run() {
         //   logger.info("\t songEnded queue song " + path + " -------  " + System.currentTimeMillis());

            soundFeedback.queueSong(path, new SoundFeedback.EndListener() {
              @Override
              public void songStarted() {
                playFeedback.setVisible(true);
              }

              @Override
              public void songEnded() {
                playFeedback.setVisible(false);
              }
            });
          }
        };
        t.schedule(100);
      }

    });
  }

  private void playUserAudio(CorrectAndScore correctAndScore) {
    final String path1 = getPath(correctAndScore.getPath());
    soundFeedback.queueSong(path1, new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        playFeedback.setVisible(true);
       // logger.info("playUserAudio songStarted song " + path1 + " -------  " + System.currentTimeMillis());
      }

      @Override
      public void songEnded() {
      //  logger.info("playUserAudio songEnded song " + path1 + " -------  " + System.currentTimeMillis());
        playFeedback.setVisible(false);
      }
    });
  }

  //private CompressedAudio compressedAudio = new CompressedAudio();

  private String getPath(String path) { return CompressedAudio.getPath(path);  }
}
