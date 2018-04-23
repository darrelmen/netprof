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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.exercise.ExceptionSupport;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.sound.SoundPlayer;
import mitll.langtest.shared.exercise.Pair;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/20/15.
 */
class PlayAudio {
  private final Logger logger = Logger.getLogger("PlayAudio");
  private final SoundPlayer soundFeedback;
  private final UIObject playFeedback;
  private Timer t;
  private ExerciseServiceAsync exerciseService;
  private ExceptionSupport exceptionSupport;

  /**
   * @param soundFeedback
   * @param playFeedback
   * @param exceptionSupport
   * @see AnalysisPlot#AnalysisPlot
   */
  PlayAudio(SoundPlayer soundFeedback,
            UIObject playFeedback,
            ExerciseServiceAsync exerciseService, ExceptionSupport exceptionSupport) {
    this.exerciseService = exerciseService;
    this.soundFeedback = soundFeedback;
    this.playFeedback = playFeedback;
    this.exceptionSupport = exceptionSupport;
  }

  /**
   * @param userID
   * @param id
   * @param nearestXAsLong
   * @see AnalysisPlot#getSeriesClickEventHandler
   */
  void playLast(int userID, int id, long nearestXAsLong) {
    //   logger.info("playLast playing exercise " + id);
    exerciseService.getLatestScoreAudioPath(userID, id, nearestXAsLong, new AsyncCallback<Pair>() {
      @Override
      public void onFailure(Throwable caught) {
        exceptionSupport.handleNonFatalError("getting the audio path for score", caught);
      }

      @Override
      public void onSuccess(Pair result) {
        playBothCuts(result.getProperty(), result.getValue());
      }
    });
  }

  private void playBothCuts(String refAudio, String studentAudioPath) {
    //   logger.info("playBothCuts play audio    " + studentAudioPath);
    //   logger.info("playBothCuts play refAudio " + refAudio);
    if (t != null) {
      // logger.info("cancel timer");
      t.cancel();
    }
    if (refAudio != null) {
      playLastThenRef(studentAudioPath, refAudio);
    } else {
      playUserAudio(studentAudioPath);
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
    // final String path1 = getPath(answerAudio);
    soundFeedback.queueSong(getPath(answerAudio), new SoundFeedback.EndListener() {
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
            playAudio(path);
          }
        };
        t.schedule(100);
      }
    });
  }


  private void playUserAudio(String answerAudio) {
    playAudio(getPath(answerAudio));
  }

  private void playAudio(String path) {
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

  private void showPlayback() {
    playFeedback.setVisible(true);
  }

  private void hidePlayback() {
    playFeedback.setVisible(false);
  }

  /**
   * Safe - chooses right compressed format based on browser.
   *
   * @param path .wav ok
   * @return ogg or mp3
   */
  private String getPath(String path) {
    return CompressedAudio.getPath(path);
  }
}
