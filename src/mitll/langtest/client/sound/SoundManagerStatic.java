package mitll.langtest.client.sound;

/**
 * Created by IntelliJ IDEA.
 * User: GO22670
 * Date: 1/12/12
 * Time: 2:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoundManagerStatic implements SoundManagerAPI {
  private boolean debug = true;
  public void initialize() {
    SoundManager.initialize();
  }

  /**
   * This always seems to be true, whether or not a flash blocker is active.
   * @return
   */
  public boolean isReady() {
    boolean ready = SoundManager.isReady();
    if (debug) System.out.println("SoundManagerStatic.isReady " +ready);

    return ready;
  }

  /**
   * Did the SoundManager load properly (i.e. if it uses Flash, was that installed and allowed to load?)
   * @return
   */
  public boolean isOK() {
    boolean ok = SoundManager.isOK();
    if (debug) System.out.println("SoundManagerStatic.isOK " +ok);

    return ok;
  }

 // boolean isActive = false;
  Sound currentSound;
  /**
   * If you call this when SoundManger is not OK, will throw an exception.
   *
   * @see mitll.langtest.client.sound.PlayAudioPanel#createSound
   * @see SoundFeedback#createSound(String, mitll.langtest.client.sound.SoundFeedback.EndListener)
   * @param sound
   * @param title
   * @param file
   */
  public void createSound(Sound sound, String title, String file) {
  if (debug) System.out.println("SoundManagerStatic.createSound " +sound);
    if (currentSound != null) {
      System.out.println("\n\nSoundManagerStatic.createSound must destroy current sound " +currentSound);
      //destroySound(currentSound);
    }
    SoundManager.createSound(sound, title, file);
    currentSound = sound;
  }

  /**
   * @see mitll.langtest.client.sound.PlayAudioPanel#destroySound()
   * @see mitll.langtest.client.sound.SoundFeedback#destroySound()
   * @param sound
   */
  public void destroySound(Sound sound) {
    if (debug)  System.out.println("SoundManagerStatic.destroy " +sound);
    SoundManager.destroySound(sound);
    currentSound = null;
  }

  public void pause(Sound sound) {
    SoundManager.pause(sound);
  }

  public void play(Sound sound) {
    if (debug) System.out.println("SoundManagerStatic.play " + sound);
    SoundManager.play(sound);
  }

  public void setPosition(Sound sound, double position) {
    if (debug) System.out.println("SoundManagerStatic.setPosition " +sound);

    SoundManager.setPosition(sound, position);
  }

  public void setPositionAndPlay(Sound sound, double position) {
    SoundManager.setPositionAndPlay(sound, position);
  }

  public void playInterval(Sound sound, int start, int end) {
    SoundManager.playInterval(sound, start, end);
  }

  /**
   * @see mitll.langtest.client.LangTest#setupSoundManager
   */
  public void exportStaticMethods() {
    SoundManager.exportStaticMethods();
  }

  public void loaded() {
    if (debug) System.out.println("SoundManagerStatic.loaded ");

    SoundManager.loaded();
  }

  public void songFinished(Sound sound) {
    if (debug) System.out.println("SoundManagerStatic.songFinished ");

    SoundManager.songFinished(sound);
  }

  public void songFirstLoaded(Sound sound, double durationEstimate) {
    if (debug) System.out.println("SoundManagerStatic.songFirstLoaded " +sound);

    SoundManager.songFirstLoaded(sound, durationEstimate);
  }

  public void songLoaded(Sound sound, double duration) {
    if (debug) System.out.println("SoundManagerStatic.songLoaded " +sound);

    SoundManager.songLoaded(sound,duration);
  }

  public void update(Sound sound, double position) {
    SoundManager.update(sound,position);
  }
}
