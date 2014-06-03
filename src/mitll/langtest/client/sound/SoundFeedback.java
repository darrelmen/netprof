package mitll.langtest.client.sound;

import com.google.gwt.user.client.ui.HTML;

/**
 * Does sound feedback - correct/incorrect to user.
 * User: GO22670
 * Date: 8/14/13
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoundFeedback {
  public static final String CORRECT = "langtest/sounds/correct4.mp3";
  public static final String INCORRECT = "langtest/sounds/incorrect1.mp3";
  private static final int SOFT_VOL = 50;

  // private final HTML warnNoFlash;
  private Sound currentSound = null;
  private final SoundManagerAPI soundManager;

  public SoundFeedback(SoundManagerAPI soundManager) {
    this(soundManager, null);
  }

  /**
   * @param soundManager
   * @param warnNoFlash
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#BootstrapExercisePanel(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, boolean, mitll.langtest.client.flashcard.ControlState, SoundFeedback, mitll.langtest.client.sound.SoundFeedback.EndListener)
   */
  private SoundFeedback(SoundManagerAPI soundManager, HTML warnNoFlash) {
    this.soundManager = soundManager;
    //   this.warnNoFlash = warnNoFlash;
  }

/*
  public boolean playCorrect(EndListener endListener) {
    return startSong(CORRECT, endListener, false);
  }
*/

  /**
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   * @param endListener
   */
/*  public boolean playIncorrect(EndListener endListener) {
    return startSong(INCORRECT, endListener, true);
  }*/

/*  private boolean startSong(String path, EndListener endListener, boolean soft) {
   //  System.out.println("SoundFeedback : start song : " + path);
    if (soundManager.isReady()) {
      if (soundManager.isOK()) {
        destroySound();
        createSound(path, endListener, soft);
        return true;
      } else {
        System.out.println(new Date() + " Sound manager is not OK!.");
        if (warnNoFlash != null) {  // TODO : remove this...
          warnNoFlash.setVisible(true);
        }
        return false;
      }
    } else {
      return false;
    }
  }*/

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
    //System.out.println("playing " + song);
    currentSound = new Sound(new AudioControl() {
      @Override
      public void reinitialize() {
      }

      @Override
      public void songFirstLoaded(double durationEstimate) {
        // System.out.println("songFirstLoaded " +song);

      }

      @Override
      public void songLoaded(double duration) {
        //  System.out.println("songLoaded " +song);

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
        //  System.out.println("songFinished " +song);

        destroySound();
        if (endListener != null) {
          endListener.songEnded();
        }
      }

      @Override
      public void update(double position) {
      }
    });

    soundManager.createSound(currentSound, song, song);

    return currentSound;
  }

  public void destroySound() {
    if (currentSound != null) {
      //System.out.println("destroySound " +currentSound);
      this.soundManager.destroySound(currentSound);
      currentSound = null;
    }
  }

  public static interface EndListener {
    void songStarted();
    void songEnded();
  }
}
