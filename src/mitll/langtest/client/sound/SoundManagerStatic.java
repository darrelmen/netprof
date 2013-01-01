package mitll.langtest.client.sound;

/**
 * Created by IntelliJ IDEA.
 * User: GO22670
 * Date: 1/12/12
 * Time: 2:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoundManagerStatic implements SoundManagerAPI {
  public void initialize() {
    SoundManager.initialize();
  }

  public void createSound(Sound sound, String title, String file) {
  //  System.out.println("SoundManagerStatic.createSound " +sound);
    SoundManager.createSound(sound, title, file);
  }

  public void destroySound(Sound sound) {
  //  System.out.println("SoundManagerStatic.destroy " +sound);
    SoundManager.destroySound(sound);
  }

  public void pause(Sound sound) {
    SoundManager.pause(sound);
  }

  public void play(Sound sound) {
    SoundManager.play(sound);
  }

  public void setPosition(Sound sound, double position) {
    SoundManager.setPosition(sound, position);
  }

  public void setPositionAndPlay(Sound sound, double position) {
    SoundManager.setPositionAndPlay(sound, position);
  }

  public void playInterval(Sound sound, int start, int end) {
    SoundManager.playInterval(sound, start, end);
  }

  public void exportStaticMethods() {
    SoundManager.exportStaticMethods();
  }

  public void loaded() {
    SoundManager.loaded();
  }

  public void songFinished(Sound sound) {
    SoundManager.songFinished(sound);
  }

  public void songFirstLoaded(Sound sound, double durationEstimate) {
    SoundManager.songFirstLoaded(sound, durationEstimate);
  }

  public void songLoaded(Sound sound, double duration) {
    SoundManager.songLoaded(sound,duration);
  }

  public void update(Sound sound, double position) {
    SoundManager.update(sound,position);
  }
}
