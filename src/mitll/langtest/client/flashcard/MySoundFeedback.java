package mitll.langtest.client.flashcard;

import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.sound.SoundManagerAPI;

/**
 * Created by go22670 on 2/2/16.
 */
public class MySoundFeedback extends SoundFeedback {
  public MySoundFeedback(SoundManagerAPI soundManagerAPI) {
    super(soundManagerAPI);
  //  this.statsFlashcardFactory = statsFlashcardFactory;
  }

  public synchronized void queueSong(String song, EndListener endListener) {
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

  // TODO : remove this empty listener
  private final EndListener endListener = new EndListener() {
    @Override
    public void songStarted() {
      //logger.info("song started --------- "+ System.currentTimeMillis());
    }

    @Override
    public void songEnded() {
      //logger.info("song ended   --------- " + System.currentTimeMillis());
    }
  };

  public EndListener getEndListener() { return endListener; }
}
