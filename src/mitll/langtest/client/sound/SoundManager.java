package mitll.langtest.client.sound;

/*
 	This GWT class references files and uses code from the SoundManager2 project (version 297a.20110424) which is subject to this license:
	
	Software License Agreement (BSD License)

	Copyright (c) 2007, Scott Schiller (schillmania.com)
	All rights reserved.

	Redistribution and use in source and binary forms, with or without modification,
	are permitted provided that the following conditions are met:

		-Redistributions of source code must retain the above copyright notice, this 
		list of conditions and the following disclaimer.

		-Redistributions in binary form must reproduce the above copyright notice, this
		list of conditions and the following disclaimer in the documentation and/or
		other materials provided with the distribution.

		-Neither the name of schillmania.com nor the names of its contributors may be
		used to endorse or promote products derived from this software without
		specific prior written permission from schillmania.com.

	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
	ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
	WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
	ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
	(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
	ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
	(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
	-------------------------------------------------------------------------------------------------------------------------
	
	The project also uses files from the above project and subject to the above license. These files are:
	swf/soundmanager2_debug.swf
	swf/soundmanager2_flash_xdomain.zip
	swf/soundmanager2_flash9_debug.swf
	swf/soundmanager2_flash9.swf
	swf/soundmanager2.swf
	soundmanager2.js
*/

/**
 * @author gregbramble
 *
 */
public class SoundManager{
	public static native void initialize() /*-{
		$wnd.soundManager.onload = $wnd.loaded();
	}-*/;
	
	public static native void createSound(Sound sound, String title, String file) /*-{
		//if(typeof(sound) != 'undefined'){
		//	sound.destruct();
		//}
//    bufferTime: 3,

    var javascriptSound = $wnd.soundManager.createSound({
			id: title,
			url: file,
			autoLoad: true,
  			autoPlay: false,
  			onfinish: function(){$wnd.songFinished(sound);},
			onload: function(){$wnd.songLoaded(sound, this.duration);},
			whileloading: function(){$wnd.songFirstLoaded(sound, this.durationEstimate);},
			whileplaying: function(){$wnd.update(sound, this.position);}
		});

		sound.@mitll.langtest.client.sound.Sound::sound = javascriptSound;
	}-*/;

  /**
   * Actually calls destruct on sound object
   * @param sound
   */
  public static native void destroySound(Sound sound) /*-{
		sound.@mitll.langtest.client.sound.Sound::sound.destruct();
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

	public static native void setPositionAndPlay(Sound sound, double position) /*-{
		sound.@mitll.langtest.client.sound.Sound::sound.setPosition(position);
		sound.@mitll.langtest.client.sound.Sound::sound.play();
	}-*/;

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


  public static void loaded(){
		//Window.alert("SoundManager loaded.");
	}

	public static void songFinished(Sound sound){
		sound.parent.reinitialize();
	}

	public static void songFirstLoaded(Sound sound, double durationEstimate){
		sound.parent.songFirstLoaded(durationEstimate);
	}

	public static void songLoaded(Sound sound, double duration){
		sound.parent.songLoaded(duration);
	}

	public static void update(Sound sound, double position){
		sound.parent.update(position);
	}
	
	public static native void exportStaticMethods() /*-{
    $wnd.loaded = $entry(@mitll.langtest.client.sound.SoundManager::loaded());
    $wnd.songFinished = $entry(@mitll.langtest.client.sound.SoundManager::songFinished(Lmitll/langtest/client/sound/Sound;));
    $wnd.songFirstLoaded = $entry(@mitll.langtest.client.sound.SoundManager::songFirstLoaded(Lmitll/langtest/client/sound/Sound;D));
    $wnd.songLoaded = $entry(@mitll.langtest.client.sound.SoundManager::songLoaded(Lmitll/langtest/client/sound/Sound;D));
    $wnd.update = $entry(@mitll.langtest.client.sound.SoundManager::update(Lmitll/langtest/client/sound/Sound;D));
 	}-*/;
}
