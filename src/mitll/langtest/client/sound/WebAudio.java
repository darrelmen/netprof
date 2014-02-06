package mitll.langtest.client.sound;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by go22670 on 2/6/14.
 */
public class WebAudio {
  public JavaScriptObject context;

  public native boolean checkIfWebAudioInstalled() /*-{
      try {
          // webkit shim
          window.AudioContext = window.AudioContext || window.webkitAudioContext;
          navigator.getMedia = ( navigator.getUserMedia ||
              navigator.webkitGetUserMedia ||
              navigator.mozGetUserMedia ||
              navigator.msGetUserMedia);
         // window.URL = window.URL || window.webkitURL;

          //audio_context = new AudioContext;
          __log('Audio context set up.');
          __log('navigator.getUserMedia ' + (navigator.getMedia ? 'available.' : 'not present!'));
          return true;
      } catch (e) {
          __log('No web audio support in this browser!');
          console.error(e);
          //webAudioMicNotAvailable();
          return false;
      }
  }-*/;

  public static native void loadSound(WebAudio webAudio, String file) /*-{
      var bufferLoader;

      function init() {
          // Fix up prefixing
          window.AudioContext = window.AudioContext || window.webkitAudioContext;
          context = new AudioContext();
          webAudio.@mitll.langtest.client.sound.WebAudio::context = javascriptSound;

          bufferLoader = new BufferLoader(
              context,
              [
                  '../sounds/hyper-reality/br-jam-loop.wav',
                  '../sounds/hyper-reality/laughter.wav',
              ],
              finishedLoading
          );

          bufferLoader.load();
      }

      function finishedLoading(bufferList) {
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
  }-*/;


}
