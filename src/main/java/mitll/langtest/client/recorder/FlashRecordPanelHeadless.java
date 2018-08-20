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

/**
 *
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.UserMenu;
import mitll.langtest.client.initial.BrowserCheck;
import mitll.langtest.client.initial.WavCallback;

import java.util.logging.Logger;

/**
 * Somewhat related to Cykod example at <a href='https://github.com/cykod/FlashWavRecorder/blob/master/html/index.html'>Cykod example html</a><p></p>
 * <p>
 * Remember when recompiling the flash to do:
 * <p>
 * mxmlc Recorder.as -static-link-runtime-shared-libraries=true -output test.swf;
 * <p>
 * Download flash from <a href=''>Flash Download</a>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class FlashRecordPanelHeadless extends AbsolutePanel {
  private final Logger logger = Logger.getLogger("FlashRecordPanelHeadless");

  private static final int WIDTH = 250;
  private static final int HEIGHT = 170;
  private static final String PX = "8px";
  /**
   *
   */
  private static final int FLASH_RECORDING_STOP_DELAY = 210; // was 160
  private static final int DELAY_MILLIS = 100;

  private final String id = "flashcontent";
  static MicPermission micPermission;
  private boolean didPopup = false;
  private static boolean permissionReceived;

  private static WebAudioRecorder webAudio;
  private static FlashRecordPanelHeadless selfPointer;

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer()
   */
  public FlashRecordPanelHeadless(MicPermission micPermission) {
    SimplePanel flashContent = new SimplePanel();
    flashContent.getElement().setId(id); // indicates the place for flash player to install in the page
    add(flashContent);
    hide();

    webAudio = new WebAudioRecorder();
    webAudio.advertise();
    selfPointer = this;
    FlashRecordPanelHeadless.micPermission = micPermission;
  }

  /**
   * Show this widget (make it big enough to accommodate the permission dialog) and install the flash player.
   *
   * @see mitll.langtest.client.LangTest#checkInitFlash()
   */
  public boolean initFlash() {
    //  logger.info("initFlash");
    if (!didPopup) {
      show();
      installFlash();
      logger.info("initFlash : did   installFlash");
      didPopup = true;
      return false;
    } else {
      logger.info("initFlash didPopup " + didPopup);
      return true;
    }
  }

  /**
   * @see #initFlash()
   */
  private void show() {
    setSize(WIDTH + "px", HEIGHT + "px");
    int ieVersion = BrowserCheck.getIEVersion();
    if (ieVersion != -1) {
      logger.info("Found IE Version " + ieVersion);
    }
  }

  /**
   * @see mitll.langtest.client.LangTest#hideFlash()
   * @see #FlashRecordPanelHeadless
   */
  public void hide() {
    //logger.info("hide...");
    setSize(PX, PX);
  }

  /**
   * @see mitll.langtest.client.LangTest#makeFlashContainer
   */
  public void hide2() {
    if (usingFlash()) {
      //logger.info("hide2...");
      flashHide2();
    }
  }

  /**
   * @see FlashRecordPanelHeadless#initFlash()
   */
  private void installFlash() {
    if (gotPermission()) {
      logger.info("installFlash :  got permission!");
      micPermission.gotPermission();
    } else {
      //  logger.info("installFlash : didn't get Flash Player permission!");
      if (checkIfFlashInstalled()) {
        //  logger.info("installFlash : looking for " + id);
        installFlash(GWT.getModuleBaseURL(), id);
      } else {
        logger.info("installFlash : no flash, trying web audio");
        // webAudio.tryWebAudio(); // kick web audio!
        micPermission.noRecordingMethodAvailable();
      }
    }
  }

  // web audio calls
/*  public boolean tryWebAudio() {
    return webAudio.tryWebAudio();
  }*/

  public static WebAudioRecorder getWebAudio() {
    return webAudio;
  }

  /**
   * @return
   * @see UserMenu#getAbout
   */
  public static boolean usingWebRTC() {
    return webAudio.isWebAudioMicAvailable();
  }

  /**
   * @see LangTest#startRecording()
   */
  public void recordOnClick() {
    if (usingWebRTC()) {
      webAudio.startRecording();
    } else if (usingFlash()) {
      flashRecordOnClick();
    }
  }

  public void startStream(String url, String exid, String reqid) {
    if (usingWebRTC()) {
      logger.info("startStream post" +
          "\n\tto  " + url +
          "\n\tfor " + exid);
      webAudio.startStream(url, exid, reqid);
    } else if (usingFlash()) {
      //flashRecordOnClick();
      logger.warning("no stream with flash!!!!\\n\n");
    }
  }

  /**
   * @param wavCallback
   * @see #stopWebRTCRecordingLater(WavCallback)
   */
  private void stopWebRTCRecording(WavCallback wavCallback) {
    webAudio.stopRecording(wavCallback);
  }

  public void stopRecordingAndPost(String url, String exid) {
    webAudio.stopRecordingAndPost(url, exid);
  }

  /**
   * @return
   * @see #installFlash()
   */
  public native boolean checkIfFlashInstalled() /*-{
      var hasFlash = false;
      try {
          var fo = new ActiveXObject('ShockwaveFlash.ShockwaveFlash');
          if (fo) hasFlash = true;
      } catch (e) {
          if (navigator.mimeTypes ["application/x-shockwave-flash"] != undefined) hasFlash = true;
      }
      return hasFlash;
  }-*/;

  /**
   * Uses SWFObject to embed flash -- <a href='http://code.google.com/p/swfobject/'>SWFObject</a>
   *
   * @param moduleBaseURL where to get the swf file the player will run
   * @param id            marks the div that the flash player will live inside
   * @see #initFlash
   */
  private native void installFlash(String moduleBaseURL, String id) /*-{
      var appWidth = 240;
      var appHeight = 160;

      var flashvars = {'event_handler': 'microphone_recorder_events'};
      var params = {};
      var attributes = {'id': "recorderApp", 'name': "recorderApp"};

      $wnd.micConnected = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::micConnected());
      $wnd.micNotConnected = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::micNotConnected());
      $wnd.noMicrophoneFound = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::noMicrophoneFound());
      $wnd.installFailure = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::installFailure());
      $wnd.installFailure2 = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::installFailure2());
      $wnd.swfCallbackCalled = $entry(@mitll.langtest.client.recorder.FlashRecordPanelHeadless::swfCallbackCalled());

      //This function is invoked by SWFObject once the <object> has been created
      var callback = function (e) {
          //Only execute if SWFObject embed was successful

          //var recorderHasConsole = (window.console || console.log);
          //if (recorderHasConsole) {
          //    console.log("got success " + e.success );
          //    console.log("got ref " + e.ref );
          //}

          if (!e.success || !e.ref) {
              $wnd.installFailure();
          }
          else {  //deal with flash blocked
              $wnd.swfCallbackCalled();

              if (typeof e.ref.PercentLoaded !== "undefined") {
                  //              if (recorderHasConsole) {
                  //                console.log("got percent loaded " + e.ref.PercentLoaded() );
                  //          }

//                  if (e.ref.PercentLoaded() < 100) {
                  //$wnd.installFailure();
                  //                }
              }
              else {
                  $wnd.installFailure2();
              }
          }
      };

      $wnd.swfobject.embedSWF(moduleBaseURL + "test.swf", id, appWidth, appHeight, "10.1.0", "", flashvars, params, attributes, callback);
  }-*/;

  public static native void installFailure() /*-{
      //var recorderHasConsole = (window.console || console.log);
      //if (recorderHasConsole) {
      //    console.log("got event installFailure");
      //}

      $wnd.micNotConnected();
  }-*/;

  public static native void installFailure2() /*-{
      //var recorderHasConsole = (window.console || console.log);
      //if (recorderHasConsole) {
      //    console.log("got event installFailure2");
      //}

      //    $wnd.micNotConnected();
  }-*/;

  public static native void swfCallbackCalled() /*-{
      //var recorderHasConsole = (window.console || console.log);
      //if (recorderHasConsole) {
      //    console.log("got event swfCallbackCalled");
      //}
  }-*/;

  /**
   * Event from flash when user clicks Accept
   */
  public static void micConnected() {
    // consoleLog("micConnected!");
    permissionReceived = true;

    micPermission.gotPermission();
  }

  /**
   * Event from flash when user clicks Deny or
   * if security settings in mms.cfg don't permit recording
   */
  public static void micNotConnected() {
    //  consoleLog("---> mic *NOT* Connected! <--- ");
    permissionReceived = false;

    selfPointer.hide();
    selfPointer.hide2(); // must be a separate call!

    micPermission.noRecordingMethodAvailable();
  }

  public static void noMicrophoneFound() {
//    consoleLog("no mic available");
    permissionReceived = false;
    micPermission.noMicAvailable();
  }

  public static boolean usingFlash() {
    return permissionReceived;
  }

  public boolean gotPermission() {
    boolean b = usingFlash() || usingWebRTC();
/*    if (!b) {
      logger.info("gotPermission permission received " + usingFlash() + " usingWebRTC " + usingWebRTC());
    }*/
    return b;
  }

  /**
   * Handles either state - either we have flash, in which case we ask flash for the wav file,
   * otherwise we ask webRTC to stop recording and post the audio to us.
   *
   * @see mitll.langtest.client.LangTest#stopRecording(WavCallback)
   */
  public void stopRecording(final WavCallback wavCallback) {
    if (usingWebRTC()) {
      stopWebRTCRecordingLater(wavCallback);
    } else if (usingFlash()) {
      stopFlashRecording(wavCallback);
    }
  }

  private void stopWebRTCRecordingLater(final WavCallback wavCallback) {
    final long then = System.currentTimeMillis();
    //logger.info("stopWebRTCRecordingLater - initial ");

    Timer t = new Timer() {
      @Override
      public void run() {
        long now = System.currentTimeMillis();
        logger.info("stopWebRTCRecordingLater timer at " + now + " diff " + (now - then));
        stopWebRTCRecording(wavCallback);
      }
    };
    t.schedule(DELAY_MILLIS); // add flash delay
  }

  private void stopFlashRecording(final WavCallback wavCallback) {
    // final long then = System.currentTimeMillis();
    logger.info("stopFlashRecording - initial ");

    Timer t = new Timer() {
      @Override
      public void run() {
        //  long now = System.currentTimeMillis();
        //  logger.info("stopFlashRecording timer at " + now + " diff " + (now - then));
        flashStopRecording();
        wavCallback.getBase64EncodedWavFile(flashGetWav());
      }
    };
    t.schedule(FLASH_RECORDING_STOP_DELAY); // add flash delay
  }

  public native void flashHide2() /*-{
      $wnd.FlashRecorderLocal.hide2();
  }-*/;

  /**
   *
   */
  public native void flashRecordOnClick() /*-{
      $wnd.FlashRecorderLocal.record('audio', 'audio.wav');
  }-*/;

  public native void flashStopRecording() /*-{
      $wnd.FlashRecorderLocal.stop();
  }-*/;

  /**
   * Base64 encoded byte array from action script.
   *
   * @return
   */
  public native String flashGetWav() /*-{
      return $wnd.FlashRecorderLocal.getWav();
  }-*/;
}
