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
import mitll.langtest.shared.Result;
import mitll.langtest.shared.scoring.AudioContext;

import java.util.logging.Logger;

/**
 * This binds a record button with the act of posting recorded audio to the server.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/18/12
 * Time: 6:51 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PostAudioRecordButton extends RecordButton implements RecordButton.RecordingListener {
  private final Logger logger = Logger.getLogger("PostAudioRecordButton");

  public static final int MIN_DURATION = 150;
  private static final int BUTTON_WIDTH = 93; // was 68

  private boolean validAudio = false;
  private static final int LOG_ROUNDTRIP_THRESHOLD = 3000;
  private final int index;
  private int reqid = 0;
  private String exerciseID;
  protected final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final boolean recordInResults;
  String audioType;

  /**
   * @param exerciseID
   * @param controller
   * @param service
   * @param index
   * @param recordInResults
   * @param recordButtonTitle
   * @param stopButtonTitle
   * @param audioType
   * @see GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton
   */
  public PostAudioRecordButton(String exerciseID, final ExerciseController controller, LangTestDatabaseAsync service,
                               int index, boolean recordInResults, String recordButtonTitle, String stopButtonTitle,
                               String audioType) {
    super(controller.getRecordTimeout(), controller.getProps().doClickAndHold(), recordButtonTitle, stopButtonTitle,
        controller.getProps());
    setRecordingListener(this);
    this.index = index;
    this.exerciseID = exerciseID;
    this.controller = controller;
    this.service = service;
    this.recordInResults = recordInResults;
    getElement().setId("PostAudioRecordButton");
    controller.register(this, exerciseID);
    Style style = getElement().getStyle();
    style.setMarginTop(1, Style.Unit.PX);
    style.setMarginBottom(1, Style.Unit.PX);
    setWidth(BUTTON_WIDTH + "px");
    this.audioType = audioType;
  }

  public void setExercise(String exercise) {
    this.exerciseID = exercise;
  }
  protected String getExerciseID() {
    return exerciseID;
  }

  /**
   * @see RecordButton#stop(long)
   * @param duration
   */
  public void stopRecording(long duration) {
    if (duration > MIN_DURATION) {
      controller.stopRecording(new WavCallback() {
        @Override
        public void getBase64EncodedWavFile(String bytes) {
          postAudioFile(bytes);
        }
      });
    }
    else {
      showPopup(AudioAnswer.Validity.TOO_SHORT.getPrompt());
      hideWaveform();
    }
  }

  protected void hideWaveform() {}

  /**
   * @see RecordingListener#stopRecording
   * @param base64EncodedWavFile
   */
  private void postAudioFile(String base64EncodedWavFile) {
    reqid++;
    final long then = System.currentTimeMillis();
    // logger.info("PostAudioRecordButton.postAudioFile : " +  getAudioType());

    AudioContext audioContext   = new AudioContext(reqid, controller.getUser(), getExerciseID(), index, getAudioType());

    service.writeAudioFile(
        base64EncodedWavFile,
        audioContext,
        controller.usingFlashRecorder(),
        "browser",
        controller.getBrowserInfo(),
        false,
        recordInResults,
        shouldAddToAudioTable(),
        false,
        new AsyncCallback<AudioAnswer>() {
          public void onFailure(Throwable caught) {
            logger.info("PostAudioRecordButton : (failure) posting audio took " + (System.currentTimeMillis() - then) + " millis");
            logMessage("failed to post audio for " + controller.getUser() + " exercise " + getExerciseID());
            showPopup(AudioAnswer.Validity.INVALID.getPrompt());
          }

          public void onSuccess(AudioAnswer result) {
            long now = System.currentTimeMillis();
            long roundtrip = now - then;
          //  logger.info("PostAudioRecordButton : Got audio answer " + result + " platform is " + getPlatform());

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


  private void addRT(AudioAnswer result, int roundtrip) {
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

  protected String getAudioType() {
    return audioType;
  }

  private Widget getOuter() {
    return this;
  }

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
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  public void startRecording() {
    controller.startRecording();
  }

  /**
   * TODO : consider why we have to do this from the client.
   *
   * @see PostAudioRecordButton#postAudioFile
   * @param result
   */
  protected void useInvalidResult(AudioAnswer result) {
    controller.logEvent(this, "recordButton", exerciseID, "invalid recording " + result.getValidity());
    //  logger.info("useInvalidResult platform is " + getPlatform());
    if (!checkAndShowTooLoud(result.getValidity())) {
      showPopup(result.getValidity().getPrompt());
    }
  }

  /**
   * Feedback for when audio isn't valid for some reason.
   *
   * @param toShow
   */
  private void showPopup(String toShow) {
    new PopupHelper().showPopup(toShow, getOuter(), 3000);
  }

  public abstract void useResult(AudioAnswer result);
  public boolean hasValidAudio() {
    return validAudio;
  }
}
