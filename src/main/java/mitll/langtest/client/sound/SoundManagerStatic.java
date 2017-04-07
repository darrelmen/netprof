/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.sound;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/12/12
 * Time: 2:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoundManagerStatic implements SoundManagerAPI {
  private final Logger logger = Logger.getLogger("SoundManagerStatic");

  private static final boolean DEBUG = false;

  public SoundManagerStatic() {
    exportStaticMethods();
    initialize();
  }

  public void initialize() {
    SoundManager.initialize();
  }

  /**
   * This always seems to be true, whether or not a flash blocker is active.
   *
   * @return
   */
  public boolean isReady() {
    return SoundManager.isReady();
  }

  /**
   * Did the SoundManager load properly (i.e. if it uses Flash, was that installed and allowed to load?)
   *
   * @return
   */
  public boolean isOK() {
    return SoundManager.isOK();
  }

  /**
   * If you call this when SoundManger is not OK, will throw an exception.
   *
   * @param sound
   * @param title
   * @param file
   * @see mitll.langtest.client.sound.PlayAudioPanel#createSound
   * @see SoundFeedback#createSound(String, mitll.langtest.client.sound.SoundFeedback.EndListener, boolean)
   */
  public void createSound(Sound sound, String title, String file) {
//    if (debug) System.out.println("SoundManagerStatic.createSound " + sound);
    if (SoundManager.isReady() && SoundManager.isOK()) {
      if (DEBUG) logger.info("SoundManagerStatic.createSound " + sound);
      SoundManager.createSound(sound, title, file);
    }
  }

  @Override
  public void setVolume(String title, int vol) {
    SoundManager.setVolume(title, vol);
  }

  /**
   * @param sound
   * @see mitll.langtest.client.sound.PlayAudioPanel#destroySound()
   * @see mitll.langtest.client.sound.SoundFeedback#destroySound()
   */
  public void destroySound(Sound sound) {
    if (SoundManager.isReady() && SoundManager.isOK()) {
      try {
        if (DEBUG) logger.info("SoundManagerStatic.destroy " + sound);
        SoundManager.destroySound(sound);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void pause(Sound sound) {
    SoundManager.pause(sound);
  }

  public void play(Sound sound) {
    if (DEBUG) logger.info("SoundManagerStatic.play " + sound);
    try {
      SoundManager.play(sound);
    } catch (Exception e) {
      logger.warning("SoundManagerStatic.play got exception playing sound.");
    }
  }

  public void setPosition(Sound sound, double position) {
    SoundManager.setPosition(sound, position);
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
    if (DEBUG) logger.info("SoundManagerStatic.loaded ");
    SoundManager.loaded();
  }

/*
  public void songFinished(Sound sound) {
    if (debug) logger.info("SoundManagerStatic.songFinished ");

    SoundManager.songFinished(sound);
  }

  public void songFirstLoaded(Sound sound, double durationEstimate) {
    if (debug) logger.info("SoundManagerStatic.songFirstLoaded " +sound);

    SoundManager.songFirstLoaded(sound, durationEstimate);
  }

  public void songLoaded(Sound sound, double duration) {
    if (debug) logger.info("SoundManagerStatic.songLoaded " +sound);

    SoundManager.songLoaded(sound,duration);
  }
*/

  public void update(Sound sound, double position) {
    SoundManager.update(sound, position);
  }
}
