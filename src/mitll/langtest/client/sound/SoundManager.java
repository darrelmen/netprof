/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.sound;

import java.util.Date;

/**
 * @author gregbramble
 *
 */
class SoundManager {
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
     if (debug) System.out.println(new Date() + " : Got loaded call!");
  }

  /**
   * Not helpful in determining whether SoundManager is actually available in the context of a flash blocker.
   */
  public static void ontimeout(){
    if (debug) System.out.println(new Date() + " : Got ontimeout call!");
    //Window.alert("Do you have a flashblocker on?  Please add this site to your whitelist.");
  }

  /**
   * Not helpful in determining whether SoundManager is actually available in the context of a flash blocker.
   */
  public static void myready(){
    if (debug) System.out.println(new Date() + " : Got myready call!");
    onreadyWasCalled = true;
  }

  public static boolean isReady() {
    return onreadyWasCalled;
  }

	public static void songFinished(Sound sound){
    if (debug) System.out.println("sound finished " +sound);
		sound.getParent().songFinished();
	}

	public static void songFirstLoaded(Sound sound, double durationEstimate){
    if (debug) System.out.println("songFirstLoaded sound " +sound);

    sound.getParent().songFirstLoaded(durationEstimate);
	}

	public static void songLoaded(Sound sound, double duration){
    if (debug) System.out.println("songLoaded sound " +sound + " with dur " +duration);

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
