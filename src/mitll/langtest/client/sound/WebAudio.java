/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.sound;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by go22670 on 2/6/14.
 */
class WebAudio {
  public static JavaScriptObject context;

  public static native boolean checkIfWebAudioInstalled() /*-{
      try {
          // webkit shim
          window.AudioContext = window.AudioContext || window.webkitAudioContext;
          navigator.getMedia = ( navigator.getUserMedia ||
              navigator.webkitGetUserMedia ||
              navigator.mozGetUserMedia ||
              navigator.msGetUserMedia);
         // window.URL = window.URL || window.webkitURL;

          //audio_context = new AudioContext;
         console.log('Audio context set up.');
         // console.log('navigator.getUserMedia ' + (navigator.getMedia ? 'available.' : 'not present!'));
          return true;
      } catch (e) {
         // __log('No web audio support in this browser!');
          console.error(e);
          //webAudioMicNotAvailable();
          return false;
      }
  }-*/;

  public static native void loadSound(String file) /*-{
      var bufferLoader;

          // Fix up prefixing
          //window.AudioContext = window.AudioContext || window.webkitAudioContext;
          var context = new AudioContext();
          @mitll.langtest.client.sound.WebAudio::context = context;





  }-*/;

  /**
   *   bufferLoader = new BufferLoader(
   context,
   [
   file
   ],
   loaded
   );

   bufferLoader.load();
   */

  /**
   *  function finishedLoading(bufferList) {
   // Create two sources and play them both together.
   var source1 = context.createBufferSource();
   var source2 = context.createBufferSource();
   source1.buffer = bufferList[0];
   source2.buffer = bufferList[1];

   source1.connect(context.destination);
   source2.connect(context.destination);
   source1.start(0);
   source2.start(0);
   }
   */
  public static void setLoadedCallback(Loaded callback) {
              WebAudio.callback = callback;
  }

  private static Loaded callback = null;
  public static interface Loaded {
    void audioLoaded();
  }

  public static void loaded() {
    if (callback != null) callback.audioLoaded();
  }

  /**
   * @see mitll.langtest.client.sound.SoundManagerStatic#exportStaticMethods()
   */
  public static native void exportStaticMethods() /*-{
      $wnd.loaded = $entry(@mitll.langtest.client.sound.WebAudio::loaded());
  }-*/;

}
