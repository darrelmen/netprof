package mitll.langtest.client.sound;

/**
 * Created by IntelliJ IDEA.
 * User: GO22670
 * Date: 1/12/12
 * Time: 2:29 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SoundManagerAPI {
  void initialize();

  boolean isReady();

  void createSound(Sound sound, String title, String file);

  void destroySound(Sound sound);

  void pause(Sound sound);

  /**
   * @param sound
   * @see mitll.langtest.client.sound.PlayAudioPanel#play()
   */
  void play(Sound sound);

  void setPosition(Sound sound, double position);

  //void setPositionAndPlay(Sound sound, double position);
  void playInterval(Sound sound, int start, int end);

  /**
   * @see mitll.langtest.client.LangTest#setupSoundManager()
   */

  void exportStaticMethods();

  void loaded();

  // void songFinished(Sound sound);
  // void songFirstLoaded(Sound sound, double durationEstimate);
  // void songLoaded(Sound sound, double duration);
  void update(Sound sound, double position);

  boolean isOK();
}
