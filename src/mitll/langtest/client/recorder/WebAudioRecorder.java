/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.recorder;

import com.google.gwt.user.client.Timer;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.WavCallback;

import java.util.logging.Logger;

/**
 * Tries to do initWebaudio, and if no response has been received in 5 seconds, tries again.
 *
 * Created by GO22670 on 5/27/2014.
 */
public class WebAudioRecorder {
  private Logger logger = Logger.getLogger("WebAudioRecorder");
  private static final int DELAY_MILLIS = 5000;

  private static boolean webAudioMicAvailable;
  private static boolean tried = false;
  private static boolean gotResponse = false;
  private Timer theTimer = null;

  /**
   *
   * The valid responses to this are : webAudioMicAvailable, webAudioMicNotAvailable, webAudioPermissionDenied
   * IF we get no response in 5 seconds, ask again!
   *
   * The user can easily ignore the dialog by clicking away.
   */
  public void tryWebAudio() {
    if (!tried) {
      tried = true;
      logger.info("webAudioMicAvailable -- tryWebAudio!");
      initWebaudio();

      if (theTimer != null) theTimer.cancel();

      theTimer = new Timer() {
        @Override
        public void run() {
          if (!gotResponse) {
            tried = false;
            tryWebAudio();
          }
        }
      };
      theTimer.schedule(DELAY_MILLIS);
    }
  }

  /**
   * Call initWebAudio in webaudiorecorder.js.
   * That will result in either webAudioMicAvailable being called (from startUserMedia), if you can record,
   * or one of webAudioMicNotAvailable or webAudioPermissionDenied if the browser doesn't support recording or
   * there's no mic, or the user doesn't permit it.
   */
  private native void initWebaudio() /*-{
      $wnd.initWebAudio();
  }-*/;

  public native void advertise() /*-{
      $wnd.webAudioMicAvailable = $entry(@mitll.langtest.client.recorder.WebAudioRecorder::webAudioMicAvailable());
      $wnd.webAudioMicNotAvailable = $entry(@mitll.langtest.client.recorder.WebAudioRecorder::webAudioMicNotAvailable());
      $wnd.webAudioPermissionDenied = $entry(@mitll.langtest.client.recorder.WebAudioRecorder::webAudioPermissionDenied());
      $wnd.getBase64 = $entry(@mitll.langtest.client.recorder.WebAudioRecorder::getBase64(Ljava/lang/String;));
  }-*/;

  public native void startRecording() /*-{
      $wnd.startRecording();
  }-*/;

  public native void stopRecording() /*-{
      $wnd.stopRecording();
  }-*/;

  private static void console(String message) {
    int ieVersion = BrowserCheck.getIEVersion();
    if (ieVersion == -1 || ieVersion > 9) {
      consoleLog(message);
    }
  }

  private native static void consoleLog( String message) /*-{
      console.log( "WebAudioRecorder:" + message );
  }-*/;

  //public boolean isMicConnected() { return micConnected; }
  public boolean isWebAudioMicAvailable() { return webAudioMicAvailable; }

  public static void webAudioMicAvailable() {
    gotResponse = true;

    console("webAudioMicAvailable -- connected!");

    webAudioMicAvailable = true;
//    logger.info("webAudioMicAvailable -- connected!");
    FlashRecordPanelHeadless.micPermission.gotPermission();
  }

  public static void webAudioMicNotAvailable() {
    gotResponse = true;

    console("webAudioMicNotAvailable!");

    webAudioMicAvailable = false;
    FlashRecordPanelHeadless.micPermission.noRecordingMethodAvailable();
  }

  public static void webAudioPermissionDenied() {
    gotResponse = true;

    console("webAudioPermissionDenied!");

    webAudioMicAvailable = false;
   // FlashRecordPanelHeadless.micPermission.noRecordingMethodAvailable();
  }

  /**
   * @see #advertise()
   * @param encoded
   */
  public static void getBase64(String encoded) {
    //logger.info("WebAudioRecorder.getBase64 " + encoded.length());
    if (encoded.length() < 100) {
      console("bytes = '" + encoded + "'");
    }
/*    if (getAllZero()) {
      logger.info("Seems like the mic is not plugged in?");
    }*/
    if (WebAudioRecorder.wavCallback == null) {
      console("getBase64 no callback?");
    } else {
      WebAudioRecorder.wavCallback.getBase64EncodedWavFile(encoded);
      WebAudioRecorder.wavCallback = null;
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#stopRecording(mitll.langtest.client.WavCallback)
   */
  static WavCallback wavCallback = null;
  public void stopRecording(WavCallback wavCallback) {
    WebAudioRecorder.wavCallback = wavCallback;
    stopRecording();
  }
}
