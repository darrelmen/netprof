package mitll.langtest.client.recorder;

import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.WavCallback;

/**
 * Created by GO22670 on 5/27/2014.
 */
public class WebAudioRecorder {
  private static boolean webAudioMicAvailable;
  private static boolean micConnected = true;  // TODO how to determine if mic not connected in web audio world?
  private static boolean tried = false;

  public void tryWebAudio() {
    if (!tried) {
      tried = true;
      System.out.println("webAudioMicAvailable -- tryWebAudio!");

      initWebaudio();
    }
  }

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

  public boolean isMicConnected() { return micConnected; }
  public boolean isWebAudioMicAvailable() { return webAudioMicAvailable; }

  public static void webAudioMicAvailable() {
    webAudioMicAvailable = true;

    System.out.println("webAudioMicAvailable -- connected!");
    console("webAudioMicAvailable -- connected!");
    FlashRecordPanelHeadless.micPermission.gotPermission();
  }

  public static void webAudioMicNotAvailable() {
    System.out.println("webAudioMicNotAvailable!");
    webAudioMicAvailable = false;
    FlashRecordPanelHeadless.micPermission.noRecordingMethodAvailable();
  }

  public static void webAudioPermissionDenied() {
    System.out.println("webAudioPermissionDenied!");
    webAudioMicAvailable = false;
   // FlashRecordPanelHeadless.micPermission.noRecordingMethodAvailable();
  }

  /**
   * @see #advertise()
   * @param encoded
   */
  public static void getBase64(String encoded) {
    //System.out.println("WebAudioRecorder.getBase64 " + encoded.length());
    if (encoded.length() < 100) {
      System.out.print("bytes = '" +encoded+ "'");
    }
/*    if (getAllZero()) {
      System.out.println("Seems like the mic is not plugged in?");
    }*/
    if (WebAudioRecorder.wavCallback == null) {
      System.err.println("getBase64 no callback?");
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
