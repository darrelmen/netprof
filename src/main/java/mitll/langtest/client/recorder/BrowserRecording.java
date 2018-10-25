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
   * @see LangTest#startRecording()
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
      logger.info("startStream post" +
          "\n\tto      " + url +
          "\n\tfor     " + clientAudioContext.getExerciseID()+
          "\n\tsession " + clientAudioContext.getDialogSessionID()
      );

      WebAudioRecorder.setStreamCallback(wavStreamCallback);

      webAudio.startStream(
          url,
          ""+clientAudioContext.getExerciseID(),
          ""+clientAudioContext.getReqid(),
          clientAudioContext.isShouldAddToTable() ? "true" : "false",
          clientAudioContext.getAudioType().toString(),
          ""+clientAudioContext.getDialogSessionID());
    }
  }

  public static boolean gotPermission() {
    boolean b = usingWebRTC();
    if (!b) {
      logger.info("gotPermission permission FALSE usingWebRTC " + usingWebRTC());
    }
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
    final long then = System.currentTimeMillis();
    //logger.info("stopWebRTCRecordingLater - initial ");

    Timer t = new Timer() {
      @Override
      public void run() {
        long now = System.currentTimeMillis();
        logger.info("stopWebRTCRecordingLater timer at " + now + " diff " + (now - then) + " abort " + abort);
        stopWebRTCRecording(abort);
      }
    };
    t.schedule(DELAY_MILLIS); // add flash delay
  }

  /**
   * @param abort
   * @see #stopWebRTCRecordingLater(boolean)
   */
  public static void stopWebRTCRecording(boolean abort) {
    webAudio.stopRecording(abort);
  }

  private static boolean usingWebRTC() {
    return webAudio.isWebAudioMicAvailable();
  }
}
