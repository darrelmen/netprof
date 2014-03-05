package mitll.langtest.client.sound;

import com.google.gwt.user.client.ui.HTML;

import java.util.Date;

/**
 * Does sound feedback - correct/incorrect to user.
 * User: GO22670
 * Date: 8/14/13
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoundFeedback {
  private static final String CORRECT   = "langtest/sounds/correct4.mp3";
  private static final String INCORRECT = "langtest/sounds/incorrect1.mp3";
  private static final int SOFT_VOL = 50;

  private final HTML warnNoFlash;
  private Sound currentSound = null;
  private SoundManagerAPI soundManager;

  public SoundFeedback(SoundManagerAPI soundManager,HTML warnNoFlash) {
    this.soundManager = soundManager;
    this.warnNoFlash = warnNoFlash;
  }
  public void playCorrect() {
    startSong(CORRECT, new EndListener() {
      @Override
      public void songEnded() {}
    }, false);
  }

  /**
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   */
  public void playIncorrect() {
    startSong(INCORRECT, new EndListener() {
      @Override
      public void songEnded() {}
    }, true);
  }


  private void startSong(String path, EndListener endListener, boolean soft) {
    // System.out.println("PlayAudioPanel : start song : " + path);
    if (soundManager.isReady()) {
      if (soundManager.isOK()) {
        destroySound();
        createSound(path, endListener, soft);
      } else {
        System.out.println(new Date() + " Sound manager is not OK!.");
        warnNoFlash.setVisible(true);
      }
    }
  }

  public void createSound(final String song, EndListener endListener) { createSound(song, endListener, false); }

    /**
     *
     * @param song
     * @param soft
     * @see #startSong
     * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#playAllAudio
     * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#playRefAndGoToNext
     */
  public void createSound(final String song, final EndListener endListener, final boolean soft) {
    //System.out.println("playing " +song);
    currentSound = new Sound(new AudioControl() {
      @Override
      public void reinitialize() {}

      @Override
      public void songFirstLoaded(double durationEstimate) {}

      @Override
      public void songLoaded(double duration) {
        if (soft) {
          soundManager.setVolume(song, SOFT_VOL);
        }
        soundManager.play(currentSound);
      }

      @Override
      public void songFinished() {
        destroySound();
        if (endListener != null) endListener.songEnded();
      }

      @Override
      public void update(double position) {}
    });

     soundManager.createSound(currentSound, song, song);
  }

  private void destroySound() {
    if (currentSound != null) {
      this.soundManager.destroySound(currentSound);
    }
  }

  public static interface EndListener {
    void songEnded();
  }
}
