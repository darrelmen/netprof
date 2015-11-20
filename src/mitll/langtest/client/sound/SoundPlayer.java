package mitll.langtest.client.sound;

/**
 * Created by go22670 on 11/19/15.
 */
public class SoundPlayer extends SoundFeedback {
  public SoundPlayer(SoundManagerAPI soundManager) {
    super(soundManager);
  }

  public synchronized void queueSong(String song, SoundFeedback.EndListener endListener) {
    //logger.info("\t queueSong song " +song+ " -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
    createSound(song, endListener);
  }

  public synchronized void queueSong(String song) {
    //logger.info("\t queueSong song " +song+ " -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
    createSound(song, null);
  }

  public synchronized void clear() {
    //  logger.info("\t stop playing current sound -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
  }
}



