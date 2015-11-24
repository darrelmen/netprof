/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

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

 // void createSoftSound(Sound sound, String title, String file);

  void setVolume(String title, int vol);

  void destroySound(Sound sound);

  void pause(Sound sound);

  /**
   * @param sound
   * @see mitll.langtest.client.sound.PlayAudioPanel#play()
   */
  void play(Sound sound);

  void setPosition(Sound sound, double position);

  void playInterval(Sound sound, int start, int end);

  /**
   * @see mitll.langtest.client.LangTest#setupSoundManager()
   */
  void exportStaticMethods();

  void loaded();

  void update(Sound sound, double position);

  boolean isOK();
}
