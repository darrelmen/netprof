/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.recorder;

import com.google.gwt.user.client.Timer;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.WavStreamCallback;
import mitll.langtest.client.scoring.ClientAudioContext;

import java.util.logging.Logger;

public class BrowserRecording {
  private static final Logger logger = Logger.getLogger("BrowserRecording");

  private static final int DELAY_MILLIS = 130;

  private static WebAudioRecorder webAudio;
  private static MicPermission micPermission;


  private static final boolean DEBUG = false;


  public static void init(MicPermission micPermission) {
    BrowserRecording.webAudio = new WebAudioRecorder();
    BrowserRecording.webAudio.advertise();
    BrowserRecording.micPermission = micPermission;
  }

  public static WebAudioRecorder getWebAudio() {
    return webAudio;
  }

  static MicPermission getMicPermission() {
    return micPermission;
  }

  /**
   * @see LangTest#startStream
   */
  public static void recordOnClick() {
    if (usingWebRTC()) {
      webAudio.startRecording();
    }
  }

  /**
   * @param url
   * @param wavStreamCallback
   * @see LangTest#startStream
   */
  public static void startStream(String url, ClientAudioContext clientAudioContext, WavStreamCallback wavStreamCallback) {
    if (usingWebRTC()) {
      if (DEBUG) {
        logger.info("startStream post" +
            "\n\tto      " + url +
            "\n\tfor     " + clientAudioContext.getExerciseID() +
            "\n\tsession " + clientAudioContext.getDialogSessionID()
        );
      }

      WebAudioRecorder.setStreamCallback(wavStreamCallback);

      webAudio.startStream(
          url,
          "" + clientAudioContext.getExerciseID(),
          "" + clientAudioContext.getReqid(),
          clientAudioContext.isShouldAddToTable() ? "true" : "false",
          clientAudioContext.getAudioType().toString(),
          "" + clientAudioContext.getDialogSessionID(),
          clientAudioContext.getRecordingSessionID()
          );
    }
  }

  public static boolean gotPermission() {
    boolean b = usingWebRTC();
//    if (!b) {
//      logger.info("gotPermission permission FALSE usingWebRTC " + usingWebRTC());
//    }
    return b;
  }

  /**
   * Handles either state - either we have flash, in which case we ask flash for the wav file,
   * otherwise we ask webRTC to stop recording and post the audio to us.
   *
   * @see ExerciseController#stopRecording
   */
  public static void stopRecording(boolean abort) {
    if (usingWebRTC()) {
      stopWebRTCRecordingLater(abort);
    }
  }

  private static void stopWebRTCRecordingLater(boolean abort) {
  //  final long then = System.currentTimeMillis();
  //   logger.info("stopWebRTCRecordingLater -  after " + DELAY_MILLIS);
    Timer t = new Timer() {
      @Override
      public void run() {
    //    long now = System.currentTimeMillis();
      //  logger.info("stopWebRTCRecordingLater timer at " + now + " diff " + (now - then) + " abort " + abort);
        stopWebRTCRecording(abort);
      }
    };
    t.schedule(DELAY_MILLIS); // add flash delay
  }

  /**
   * @param abort
   * @see #stopWebRTCRecordingLater(boolean)
   * @see LangTest#stopRecording
   */
  public static void stopWebRTCRecording(boolean abort) {
    webAudio.stopRecording(abort);
  }

  private static boolean usingWebRTC() {
    return webAudio.isWebAudioMicAvailable();
  }
}
