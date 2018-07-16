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

package mitll.langtest.client.sound;

import java.util.logging.Logger;

/**
 * Does sound feedback - correct/incorrect to user.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/14/13
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoundFeedback {
 // private final Logger logger = Logger.getLogger("SoundFeedback");

  public static final String CORRECT   = "langtest/sounds/correct4.mp3";
  public static final String INCORRECT = "langtest/sounds/incorrect1.mp3";
  private static final int SOFT_VOL = 50;
  public static final int VOLUME = 100;

  private Sound currentSound = null;
  private final SoundManagerAPI soundManager;

  /**
   * @param soundManager
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#BootstrapExercisePanel
   */
  public SoundFeedback(SoundManagerAPI soundManager) {
    this.soundManager = soundManager;
  }

  /**
   * @param song
   * @param endListener
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#playRef(String)
   */
  public Sound createSound(final String song, EndListener endListener) {
    return createSound(song, endListener, false);
  }

  /**
   * @param song
   * @param soft
   * @seex #startSong
   * @seex mitll.langtest.client.flashcard.BootstrapExercisePanel#playAllAudio
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#playRefAndGoToNext
   */
  private Sound createSound(final String song, final EndListener endListener, final boolean soft) {
    //logger.info("playing " + song);
    currentSound = new Sound(new AudioControl() {
      @Override
      public void reinitialize() {
        //logger.info("got reinitialize");
      }

      @Override
      public void songFirstLoaded(double durationEstimate) {
       // logger.info("got songFirstLoaded");
      }

      @Override
      public void repeatSegment(float startInSeconds, float endInSeconds) {
       // logger.info("got repeatSegment");

      }

      @Override
      public void songLoaded(double duration) {
   //     logger.info("songLoaded " +song + " dur " + duration);
        if (soft) {
          soundManager.setVolume(song, SOFT_VOL);
        }
        if (endListener != null) {
          endListener.songStarted();
        }

        soundManager.play(currentSound);
      }

      @Override
      public void songFinished() {
 //       logger.info("songFinished " +song);
        destroySound();
        if (endListener != null) {
          endListener.songEnded();
        }
      }

      @Override
      public void update(double position) {
      }
    });

    soundManager.createSound(currentSound, song, song, true, VOLUME);

    return currentSound;
  }

  public void destroySound() {
    if (currentSound != null) {
      this.soundManager.destroySound(currentSound);
      currentSound = null;
    }
  }

  public interface EndListener {
    void songStarted();
    void songEnded();
  }
}
