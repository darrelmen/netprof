/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.scoring;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PopupHelper;
import mitll.langtest.client.WavCallback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;

import java.util.logging.Logger;

/**
 * This binds a record button with the act of posting recorded audio to the server.
 *
 * User: GO22670
 * Date: 12/18/12
 * Time: 6:51 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PostAudioRecordButton extends RecordButton implements RecordButton.RecordingListener {
  private Logger logger = Logger.getLogger("PostAudioRecordButton");

  private boolean validAudio = false;
  private static final int LOG_ROUNDTRIP_THRESHOLD = 3000;
  private final int index;
  private int reqid = 0;
  private CommonExercise exercise;
  protected final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final boolean recordInResults;

  /**
   * @see GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton
   * @param exercise
   * @param controller
   * @param service
   * @param index
   * @param recordInResults
   * @param recordButtonTitle
   * @param stopButtonTitle
   */
  public PostAudioRecordButton(CommonExercise exercise, final ExerciseController controller, LangTestDatabaseAsync service,
                               int index, boolean recordInResults, String recordButtonTitle, String stopButtonTitle) {
    super(controller.getRecordTimeout(), controller.getProps().doClickAndHold(), recordButtonTitle, stopButtonTitle, controller.getProps());
    setRecordingListener(this);
    this.index = index;
    this.exercise = exercise;
    this.controller = controller;
    this.service = service;
    this.recordInResults = recordInResults;
    getElement().setId("PostAudioRecordButton");
    controller.register(this, exercise.getID());
    getElement().getStyle().setMarginTop(1, Style.Unit.PX);
    getElement().getStyle().setMarginBottom(1, Style.Unit.PX);
  }

  public void setExercise(CommonExercise exercise) { this.exercise = exercise; }
  public CommonExercise getExercise() { return exercise; }

  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
  public void stopRecording() {
    controller.stopRecording(new WavCallback() {
      @Override
      public void getBase64EncodedWavFile(String bytes) {
        postAudioFile(bytes);
      }
    });
  }

  private void postAudioFile(String base64EncodedWavFile) {
    reqid++;
    final long then = System.currentTimeMillis();
    // logger.info("PostAudioRecordButton.postAudioFile : " +  getAudioType());

    service.writeAudioFile(base64EncodedWavFile,
        exercise.getID(),
        index,
        controller.getUser(),
        reqid,
        true,
        getAudioType(),
        false, recordInResults,
        shouldAddToAudioTable(), controller.usingFlashRecorder(), "browser", controller.getBrowserInfo(), false,
        new AsyncCallback<AudioAnswer>() {
          public void onFailure(Throwable caught) {
            long now = System.currentTimeMillis();
            logger.info("PostAudioRecordButton : (failure) posting audio took " + (now - then) + " millis");

            logMessage("failed to post audio for " + controller.getUser() + " exercise " + exercise.getID());
          showPopup(AudioAnswer.Validity.INVALID.getPrompt());
        }


        public void onSuccess(AudioAnswer result) {
          long now = System.currentTimeMillis();
          long roundtrip = now - then;

          logger.info("PostAudioRecordButton : Got audio answer " + result + " platform is " + getPlatform());

          if (result.getReqid() != reqid) {
            logger.info("ignoring old response " + result);
            return;
          }
          if (result.getValidity() == AudioAnswer.Validity.OK ||
              (controller.getProps().isQuietAudioOK() && result.getValidity() == AudioAnswer.Validity.TOO_QUIET)) {
            validAudio = true;
            useResult(result);
            addRT(result, (int) roundtrip);
          } else {
            validAudio = false;
            useInvalidResult(result);
          }
          if (controller.isLogClientMessages() || roundtrip > LOG_ROUNDTRIP_THRESHOLD) {
            logRoundtripTime(result, roundtrip);
          }
        }
      });
  }

  public void addRT(AudioAnswer result, int roundtrip) {
    service.addRoundTrip(result.getResultID(), roundtrip, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  protected boolean shouldAddToAudioTable() {
    return false;
  }

  protected String getAudioType() { return controller.getAudioType(); }

  private Widget getOuter() { return this; }

  private void logRoundtripTime(AudioAnswer result, long roundtrip) {
    String message = "PostAudioRecordButton : (success) User #" + controller.getUser() +
      " post audio took " + roundtrip + " millis, audio dur " +
      result.getDurationInMillis() + " millis, " +
      " " + ((float) roundtrip / (float) result.getDurationInMillis()) + " roundtrip/audio duration ratio.";
    logMessage(message);
  }

  private void logMessage(String message) {
    service.logMessage(message, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Void result) {}
    });
  }

  public void startRecording() {  controller.startRecording();  }

  protected void useInvalidResult(AudioAnswer result) {
    controller.logEvent(this, "recordButton", getExercise().getID(), "invalid recording " + result.getValidity());
  //  logger.info("useInvalidResult platform is " + getPlatform());
    if (!checkAndShowTooLoud(result.getValidity())) {
      showPopup(result.getValidity().getPrompt());
    }
  }

  /**
   * Feedback for when audio isn't valid for some reason.
   * @param toShow
   */
  private void showPopup(String toShow) {
    new PopupHelper().showPopup(toShow,getOuter(),3000);
  }

  public abstract void useResult(AudioAnswer result);

  public boolean hasValidAudio() { return validAudio; }
}
