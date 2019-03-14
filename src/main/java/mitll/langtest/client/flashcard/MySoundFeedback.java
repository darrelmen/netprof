/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.flashcard;

import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.sound.SoundManagerAPI;

public class MySoundFeedback extends SoundFeedback {
//  private final Logger logger = Logger.getLogger("MySoundFeedback");
  public MySoundFeedback(SoundManagerAPI soundManagerAPI) {
    super(soundManagerAPI);
  }

  /**
   * @param song
   * @param endListener
   * @see BootstrapExercisePanel#playRefAndGoToNext
   */

  public synchronized void queueSong(String song, EndListener endListener) {
    //logger.info("\t queueSong song " +song+ " -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
    createSound(song, endListener);
  }

  synchronized void queueSong(String song) {
    //logger.info("\t queueSong (no callback) song " +song+ " -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
    createSound(song, null);
  }

  /**
   * @see FlashcardPanel#stopPlayback
   */
  public synchronized void clear() {
   // logger.info("\t stop playing current sound -------  "+ System.currentTimeMillis());
    destroySound(); // if there's something playing, stop it!
  }

  // TODO : remove this empty listener
/*  private EndListener endListener = new EndListener() {
    @Override
    public void songStarted() {
      //logger.info("empty listener - song started --------- "+ System.currentTimeMillis());
    }

    @Override
    public void songEnded() {
      //logger.info("empty listener - song ended   --------- " + System.currentTimeMillis());
    }
  };

  void setEndListener(EndListener endListener) {
    this.endListener = endListener;
  }*/
}
