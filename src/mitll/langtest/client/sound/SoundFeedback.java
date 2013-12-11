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
  private final HTML warnNoFlash;
  private Sound currentSound = null;
  private SoundManagerAPI soundManager;

  public SoundFeedback(SoundManagerAPI soundManager,HTML warnNoFlash) {
    this.soundManager = soundManager;
    this.warnNoFlash = warnNoFlash;
  }
  public void playCorrect() {
    startSong("langtest/sounds/correct4.mp3", new EndListener() {
      @Override
      public void songEnded() {}
    });
  }

  public void playIncorrect() {
    startSong("langtest/sounds/incorrect1.mp3", new EndListener() {
      @Override
      public void songEnded() {}
    });
  }


  private void startSong(String path, EndListener endListener) {
    // System.out.println("PlayAudioPanel : start song : " + path);
    if (soundManager.isReady()) {
      //System.out.println(new Date() + " Sound manager is ready.");
      if (soundManager.isOK()) {
        destroySound();
        createSound(path, endListener);
      } else {
        System.out.println(new Date() + " Sound manager is not OK!.");
        warnNoFlash.setVisible(true);
      }
    }
  }

  /**
   * @param song
   * @see #startSong
   */
  public void createSound(final String song, final EndListener endListener) {
    currentSound = new Sound(new AudioControl() {
      @Override
      public void reinitialize() {
       // System.out.println("song " + song + " ended---");
        destroySound();
        endListener.songEnded();
      }

      @Override
      public void songFirstLoaded(double durationEstimate) {}

      @Override
      public void songLoaded(double duration) {
        soundManager.play(currentSound);
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
