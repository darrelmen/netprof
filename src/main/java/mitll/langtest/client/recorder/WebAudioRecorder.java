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

package mitll.langtest.client.recorder;

import com.google.gwt.user.client.Timer;
import mitll.langtest.client.initial.BrowserCheck;
import mitll.langtest.client.initial.WavCallback;

import java.util.logging.Logger;

/**
 * Tries to do initWebaudio, and if no response has been received in 5 seconds, tries again.
 *
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/27/2014.
 */
public class WebAudioRecorder {
 // private final Logger logger = Logger.getLogger("WebAudioRecorder");
  private static final int DELAY_MILLIS = 4000;

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
/*  boolean tryWebAudio() {
    if (!tried) {
      tried = true;
      //attempts--;
      //logger.info("webAudioMicAvailable -- tryWebAudio!");
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
      return true;
    }
    else {
      return false;
    }
  }*/

  /**
   * Call initWebAudio in webaudiorecorder.js.
   * That will result in either webAudioMicAvailable being called (from startUserMedia), if you can record,
   * or one of webAudioMicNotAvailable or webAudioPermissionDenied if the browser doesn't support recording or
   * there's no mic, or the user doesn't permit it.
   */
  public native void initWebaudio() /*-{
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
    try {
      int ieVersion = BrowserCheck.getIEVersion();
      if (ieVersion == -1 || ieVersion > 9) {
        consoleLog(message);
      }
    } catch (Exception e) {
      //e.printStackTrace();
    }
  }

  private native static void consoleLog( String message) /*-{
      console.log( "WebAudioRecorder:" + message );
  }-*/;

  public boolean isWebAudioMicAvailable() { return webAudioMicAvailable; }

  public static void webAudioMicAvailable() {
    gotResponse = true;

    console("webAudioMicAvailable -- connected!");
    webAudioMicAvailable = true;
    getMicPermission().gotPermission();
  }

  public static void webAudioMicNotAvailable() {
    gotResponse = true;

    console("webAudioMicNotAvailable!");

    noWebRTC();
  }

  public static boolean isWebRTCAvailable() {
    return !gotResponse || webAudioMicAvailable;
  }

  public static void webAudioPermissionDenied() {
    gotResponse = true;
    console("webAudioPermissionDenied!");
    noWebRTC();
  }

  private static void noWebRTC() {
    webAudioMicAvailable = false;
    getMicPermission().noWebRTCAvailable();
  }

  private static MicPermission getMicPermission() {
    return BrowserRecording.getMicPermission();
  }

  /**
   * @param encoded
   * @see #advertise()
   */
  public static void getBase64(String encoded) {
    if (encoded.length() < 100) {
      console("getBase64 (short audio?) bytes = '" + encoded + "'");
    }
    console("getBase64   bytes = '" + encoded.length() + "'");

    if (WebAudioRecorder.wavCallback == null) {
      console("getBase64 no callback?");
    } else {
      WebAudioRecorder.wavCallback.getBase64EncodedWavFile(encoded);
      WebAudioRecorder.wavCallback = null;
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#stopRecording(WavCallback)
   */
  private static WavCallback wavCallback = null;

  public void stopRecording(WavCallback wavCallback) {
    WebAudioRecorder.wavCallback = wavCallback;
    //logger.info("got stopRecording ");
    stopRecording();
  }
}
