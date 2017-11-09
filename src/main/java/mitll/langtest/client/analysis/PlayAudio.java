/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.analysis;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.analysis.WordScore;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/20/15.
 */
class PlayAudio {
  private final Logger logger = Logger.getLogger("PlayAudio");
  //private final ExerciseServiceAsync service;
  private final SoundPlayer soundFeedback;
  private final Widget playFeedback;
  private Timer t;
  private ExerciseLookup exerciseLookup;

  /**
   * @param soundFeedback
   * @param playFeedback
   * @see AnalysisPlot#AnalysisPlot
   */
  PlayAudio(SoundPlayer soundFeedback, Widget playFeedback, ExerciseLookup exerciseLookup) {
    //this.service = service;
    this.soundFeedback = soundFeedback;
    this.playFeedback = playFeedback;
    this.exerciseLookup = exerciseLookup;
  }

  /**
   * @param id
   * @param userid
   * @param nearestXAsLong
   * @see AnalysisPlot#getSeriesClickEventHandler
   */
  void playLast(int id, int userid, long nearestXAsLong) {
    logger.info("playLast playing exercise " + id + " for " + userid);
    // CommonShell shell = exerciseLookup.getShell(id);
    WordScore wordScore = exerciseLookup.getAnswerPath(id, nearestXAsLong);

/*    if (shell == null) {
      service.getExercise(id, false, new AsyncCallback<CommonExercise>() {
        @Override
        public void onFailure(Throwable throwable) {
        }

        @Override
        public void onSuccess(CommonExercise commonExercise) {
          if (commonExercise == null) {
            logger.info("playLast no exercise " + id + " for " + userid);
            // if the exercise has been deleted...?
            // show popup?
          } else {
            List<CorrectAndScore> scores = commonExercise.getScores();
            if (scores.isEmpty()) {
              String msg = "playLast no Correct and scores for exercise : " + id + " and user " + userid;
              logger.warning(msg);
              // TODO : consider putting this back
            *//*            service.logMessage(msg, new AsyncCallback<Void>() {
              @Override
              public void onFailure(Throwable throwable) {
              }

              @Override
              public void onSuccess(Void aVoid) {
              }
            });*//*
            } else {
              CorrectAndScore correctAndScore = scores.get(scores.size() - 1);

              playBothCuts(commonExercise, correctAndScore);
            }
          }
        }
      });
    } else {*/
    playBothCuts(wordScore.getAnswerAudio(), wordScore.getRefAudio());
    //  }
  }
/*
  private void playBothCuts(CommonExercise commonExercise, CorrectAndScore correctAndScore) {
    String path = correctAndScore.getPath();
    String refAudio = commonExercise.getRefAudio();

    playBothCuts(path, refAudio);
  }*/

  private void playBothCuts(String path, String refAudio) {
    if (t != null) {
      // logger.info("cancel timer");
      t.cancel();
    }
    if (refAudio != null) {
      playLastThenRef(path, refAudio);
    } else {
      playUserAudio(path);
      //   logger.info("no ref audio for " + commonExercise.getOldID());
    }
  }

  /**
   * @param answerAudio
   * @param refAudio
   * @see #playLast
   */
  private void playLastThenRef(String answerAudio, String refAudio) {
    final String path = getPath(refAudio);
    final String path1 = getPath(answerAudio);
    soundFeedback.queueSong(path1, new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        showPlayback();
        //logger.info("\t songStarted song " + path1 + " -------  " + System.currentTimeMillis());
      }

      @Override
      public void songEnded() {
        hidePlayback();
        //logger.info("\t songEnded song " + path1 + " -------  " + System.currentTimeMillis());
        t = new Timer() {
          @Override
          public void run() {
            //   logger.info("\t songEnded queue song " + path + " -------  " + System.currentTimeMillis());
            soundFeedback.queueSong(path, new SoundFeedback.EndListener() {
              @Override
              public void songStarted() {
                showPlayback();
              }

              @Override
              public void songEnded() {
                hidePlayback();
              }
            });
          }
        };
        t.schedule(100);
      }
    });
  }


  private void playUserAudio(String answerAudio) {
    soundFeedback.queueSong(getPath(answerAudio), new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
        showPlayback();
        //logger.info("playUserAudio songStarted song " + path1 + " -------  " + System.currentTimeMillis());
      }

      @Override
      public void songEnded() {
        //logger.info("playUserAudio songEnded song " + path1 + " -------  " + System.currentTimeMillis());
        hidePlayback();
      }
    });
  }

  private void showPlayback() {
    playFeedback.setVisible(true);
  }
  private void hidePlayback() {
    playFeedback.setVisible(false);
  }

  private String getPath(String path) {
    return CompressedAudio.getPath(path);
  }
}
