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

import java.util.Date;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 *
 */
class SoundManager {
  protected static final Logger logger = Logger.getLogger("SoundManager");

  private static boolean onreadyWasCalled = false;
  private final static boolean   debug = false;
  public static native void initialize() /*-{
    $wnd.soundManager.onload = $wnd.loaded();
    $wnd.soundManager.ontimeout = $wnd.ontimeout();
    $wnd.soundManager.onready = $wnd.myready();
	}-*/;

  public static native boolean isOK()/*-{
      return $wnd.soundManager.ok();
  }-*/;

  public static native void setVolume(String title, int volume)/*-{
      $wnd.soundManager.setVolume(title,volume);
  }-*/;

  /**
   * @see SoundManagerStatic#createSound(Sound, String, String)
   * @param sound
   * @param title
   * @param file
   */
	public static native void createSound(Sound sound, String title, String file) /*-{
      var javascriptSound = $wnd.soundManager.createSound({
          id: title,
          url: file,
          autoLoad: true,
          autoPlay: false,
          onfinish: function () {
              $wnd.songFinished(sound);
          },
          onload: function () {
              $wnd.songLoaded(sound, this.duration);
          },
          whileloading: function () {
              $wnd.songFirstLoaded(sound, this.durationEstimate);
          },
          whileplaying: function () {
              $wnd.update(sound, this.position);
          }
      });

		sound.@mitll.langtest.client.sound.Sound::sound = javascriptSound;
	}-*/;

  /**
   * Actually calls destruct on sound object
   *
   * @param sound
   * @see mitll.langtest.client.sound.SoundManagerStatic#destroySound(Sound)
   */
  public static native void destroySound(Sound sound) /*-{
      var newVar = sound.@mitll.langtest.client.sound.Sound::sound;
      if (newVar) {
          sound.@mitll.langtest.client.sound.Sound::sound.destruct();
      }
  }-*/;

	public static native void pause(Sound sound) /*-{
		sound.@mitll.langtest.client.sound.Sound::sound.pause();
	}-*/;

	public static native void play(Sound sound) /*-{
		sound.@mitll.langtest.client.sound.Sound::sound.play();
	}-*/;

	public static native void setPosition(Sound sound, double position) /*-{
		sound.@mitll.langtest.client.sound.Sound::sound.setPosition(position);
	}-*/;

/*	public static native void setPositionAndPlay(Sound sound, double position) *//*-{
		sound.@mitll.langtest.client.sound.Sound::sound.setPosition(position);
		sound.@mitll.langtest.client.sound.Sound::sound.play();
	}-*//*;*/

  /**
   * When the segment finished, calls songFinished
   * @param sound
   * @param start
   * @param end
   */
  public static native void playInterval(Sound sound, int start, int end) /*-{
    sound.@mitll.langtest.client.sound.Sound::sound.stop();
    sound.@mitll.langtest.client.sound.Sound::sound.play({
      from: start, // start playing at start msec
      to: end,  // end at end msec
      onstop: function() {
        $wnd.songFinished(sound);
//        soundManager._writeDebug('sound stopped at position ' + this.position);
        // note that the "to" target may be over-shot by 200+ msec, depending on polling and other factors.
      }
    });
  }-*/;

  /**
   * Not helpful in determining whether SoundManager is actually available in the context of a flash blocker.
   */
  public static void loaded(){

  //  if (debug) System.out.println(new Date() + " : Got loaded call!");
  }

  /**
   * Not helpful in determining whether SoundManager is actually available in the context of a flash blocker.
   */
  public static void ontimeout(){
   // if (debug) System.out.println(new Date() + " : Got ontimeout call!");
    //Window.alert("Do you have a flashblocker on?  Please add this site to your whitelist.");
  }

  /**
   * Not helpful in determining whether SoundManager is actually available in the context of a flash blocker.
   */
  public static void myready(){
   // if (debug) System.out.println(new Date() + " : Got myready call!");
    onreadyWasCalled = true;
  }

  public static boolean isReady() {
    return onreadyWasCalled;
  }

	public static void songFinished(Sound sound){
   // if (debug) System.out.println("sound finished " +sound);
		sound.getParent().songFinished();
	}

	public static void songFirstLoaded(Sound sound, double durationEstimate){
  //  if (debug) System.out.println("songFirstLoaded sound " +sound);

    sound.getParent().songFirstLoaded(durationEstimate);
	}

	public static void songLoaded(Sound sound, double duration){
  //  logger.info("songLoaded sound " +sound + " with dur " +duration);
    sound.getParent().songLoaded(duration);
	}

	public static void update(Sound sound, double position){
		sound.getParent().update(position);
	}

  /**
   * @see mitll.langtest.client.sound.SoundManagerStatic#exportStaticMethods()
   */
	public static native void exportStaticMethods() /*-{
    $wnd.loaded = $entry(@mitll.langtest.client.sound.SoundManager::loaded());
    $wnd.ontimeout = $entry(@mitll.langtest.client.sound.SoundManager::ontimeout());
    $wnd.myready = $entry(@mitll.langtest.client.sound.SoundManager::myready());
    $wnd.songFinished = $entry(@mitll.langtest.client.sound.SoundManager::songFinished(Lmitll/langtest/client/sound/Sound;));
    $wnd.songFirstLoaded = $entry(@mitll.langtest.client.sound.SoundManager::songFirstLoaded(Lmitll/langtest/client/sound/Sound;D));
    $wnd.songLoaded = $entry(@mitll.langtest.client.sound.SoundManager::songLoaded(Lmitll/langtest/client/sound/Sound;D));
    $wnd.update = $entry(@mitll.langtest.client.sound.SoundManager::update(Lmitll/langtest/client/sound/Sound;D));
 	}-*/;
}
