/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.sound;

import java.util.logging.Logger;

/**
 * Created by go22670 on 11/19/15.
 */
public class SoundPlayer extends SoundFeedback {
 // private final Logger logger = Logger.getLogger("SoundPlayer");

  public SoundPlayer(SoundManagerAPI soundManager) {
    super(soundManager);
  }

  public synchronized void queueSong(String song, SoundFeedback.EndListener endListener) {
   // logger.info("\t queueSong song " +song+ " -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
    createSound(song, endListener);
  }

  public synchronized void queueSong(String song) {
   // logger.info("\t queueSong song " +song+ " -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
    createSound(song, null);
  }

  public synchronized void clear() {
   // logger.info("\t stop playing current sound -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
  }
}



