package mitll.langtest.client.bootstrap;

import com.google.gwt.media.client.Audio;
import com.google.gwt.user.client.Timer;
import mitll.langtest.client.BrowserCheck;

public class AudioHelper {
  private Audio mistakeAudio;
  private BrowserCheck browserCheck = new BrowserCheck().checkForCompatibleBrowser();

  public AudioHelper() {
    mistakeAudio = Audio.createIfSupported();
  }

  public void playCorrect(final int n) {
    playCorrect();

    if (n > 0) {
      new Timer() {
        @Override
        public void run() {
          playCorrect(n - 1);
        }
      }.schedule(300);
    }
  }

  public void playCorrect() {
    play("langtest/sounds/correct4.wav", "langtest/sounds/correct4.mp3");
  }

  public void playIncorrect() {
    play("langtest/sounds/incorrect1.wav", "langtest/sounds/incorrect1.mp3");
  }

  public void play(String mp3Audio) {
    play(mp3Audio.replace(".mp3", ".wav"), mp3Audio.replace(".wav", ".mp3"));
  }

  public boolean hasEnded() {
    return mistakeAudio != null && mistakeAudio.hasEnded();
  }

  private void play(String openAudio, String mp3Audio) {
    if (mistakeAudio == null) {
      // Window.alert("audio playback not supported.");
    } else {
      playAudio(openAudio, mp3Audio);
      mistakeAudio.play();
    }
  }

  private void playAudio(String openAudio, String mp3Audio) {
 //   mistakeAudio.setSrc((browserCheck.isFirefox()) ? openAudio : mp3Audio);
    mistakeAudio.setSrc( mp3Audio);
  }
}