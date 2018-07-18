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
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PlayAudioEvent;
import mitll.langtest.client.initial.PopupHelper;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;

import java.util.logging.Logger;

import static mitll.langtest.client.dialog.ExceptionHandlerDialog.getExceptionAsString;

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

  public static final int MIN_DURATION = 250;

  private boolean validAudio = false;
  private static final int LOG_ROUNDTRIP_THRESHOLD = 3000;
  private final int index;
  private int reqid = 0;
  private int exerciseID;
  protected final ExerciseController controller;
  private final boolean recordInResults;
  private final boolean scoreAudioNow;

  /**
   * @param exerciseID
   * @param controller
   * @param index
   * @param recordInResults
   * @param recordButtonTitle
   * @param stopButtonTitle
   * @param buttonWidth
   * @param scoreAudioNow
   * @seex GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton
   */
  public PostAudioRecordButton(int exerciseID,
                               final ExerciseController controller,
                               int index,
                               boolean recordInResults,
                               String recordButtonTitle,
                               String stopButtonTitle,
                               int buttonWidth,
                               boolean scoreAudioNow) {
    super(controller.getRecordTimeout(),
        controller.getProps().doClickAndHold(),
        recordButtonTitle,
        stopButtonTitle,
        controller.getProps());
    setRecordingListener(this);
    this.index = index;
    this.exerciseID = exerciseID;
    this.controller = controller;
    this.scoreAudioNow = scoreAudioNow;

    this.recordInResults = recordInResults;
    getElement().setId("PostAudioRecordButton");

    controller.register(this, exerciseID);
    Style style = getElement().getStyle();
    style.setMarginBottom(1, Style.Unit.PX);

    if (buttonWidth > 0) {
      setWidth(buttonWidth + "px");
    }

    getElement().setId("PostAudioRecordButton" + exerciseID);
  }

  public void setExerciseID(int exercise) {
    this.exerciseID = exercise;
  }

  protected int getExerciseID() {
    return exerciseID;
  }

  public void startRecording() {
    LangTest.EVENT_BUS.fireEvent(new PlayAudioEvent(-1));
    controller.startRecording();
  }

  /**
   * @param duration
   * @see RecordButton#stop
   */
  public boolean stopRecording(long duration) {
    if (duration > MIN_DURATION) {
      logger.info("stopRecording duration " + duration + " > min = " + MIN_DURATION);
      controller.stopRecording(this::postAudioFile);
      return true;
    } else {
      showPopup(Validity.TOO_SHORT.getPrompt());
      hideWaveform();
      gotShortDurationRecording();
      logger.info("stopRecording duration " + duration + " < min = " + MIN_DURATION);
      return false;
    }
  }

  /**
   * @param result
   * @see #onPostSuccess
   */
  public abstract void useResult(AudioAnswer result);

  /**
   * TODO : consider why we have to do this from the client.
   *
   * @param result
   * @see PostAudioRecordButton#postAudioFile
   */
  protected void useInvalidResult(AudioAnswer result) {
    controller.logEvent(this, "recordButton", "" + exerciseID, "invalid recording " + result.getValidity());
    //  logger.info("useInvalidResult platform is " + getPlatform());
    if (!checkAndShowTooLoud(result.getValidity())) {
      showPopup(result.getValidity().getPrompt());
    }
  }

  public boolean hasValidAudio() {
    return validAudio;
  }

  protected void hideWaveform() {
  }

  protected void gotShortDurationRecording() {
  }

  /**
   * @param base64EncodedWavFile
   * @see #stopRecording
   */
  private void postAudioFile(String base64EncodedWavFile) {
    reqid++;
    final long then = System.currentTimeMillis();

    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    final int user = getUser();

    AudioContext audioContext = new AudioContext(
        reqid,
        user,
        projectStartupInfo.getProjectid(),
        projectStartupInfo.getLanguage(),
        getExerciseID(),
        index,
        getAudioType());

 /*
    logger.info("\n\n\nPostAudioRecordButton.postAudioFile : " + getAudioType() + " : " + audioContext +
        "\n\t bytes " + base64EncodedWavFile.length());
        */

    DecoderOptions decoderOptions = new DecoderOptions()
        .setDoDecode(scoreAudioNow)
        .setDoAlignment(scoreAudioNow)
        .setRecordInResults(recordInResults)
        .setRefRecording(shouldAddToAudioTable())
        .setAllowAlternates(false);

    controller.getAudioService().writeAudioFile(
        base64EncodedWavFile,
        audioContext,

        controller.usingFlashRecorder(),
        "browser",
        controller.getBrowserInfo(),
        decoderOptions,
        new AsyncCallback<AudioAnswer>() {
          public void onFailure(Throwable caught) {
            onPostFailure(then, user, getExceptionAsString(caught));
            controller.handleNonFatalError("posting audio", caught);
          }

          public void onSuccess(AudioAnswer result) {
            onPostSuccess(result, then);
          }
        });
  }

  private void onPostFailure(long then, int user, String exception) {
    long now = System.currentTimeMillis();
    logger.info("PostAudioRecordButton : (failure) posting audio took " + (now - then) + " millis :\n" + exception);
    logMessage("failed to post audio for " + user + " exercise " + getExerciseID(), true);
    showPopup(Validity.INVALID.getPrompt());
  }

  private void onPostSuccess(AudioAnswer result, long then) {
    long now = System.currentTimeMillis();
    long roundtrip = now - then;

    //  logger.info("PostAudioRecordButton : onPostSuccess Got audio answer " + result);// + " platform is " + getPlatform());

    if (result.getReqid() != reqid) {
      logger.info("onPostSuccess ignoring old response " + result);
      return;
    }
    if (result.getValidity() == Validity.OK || doQuietAudioCheck(result)) {
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

  /**
   * Just for load testing
   *
   * @param result
   * @return
   */
  private boolean doQuietAudioCheck(AudioAnswer result) {
    return controller.getProps().isQuietAudioOK() && result.getValidity() == Validity.TOO_QUIET;
  }

  private int getUser() {
    return controller.getUserState().getUser();
  }

  private void addRT(AudioAnswer result, int roundtrip) {
    controller.getScoringService().addRoundTrip(result.getResultID(), roundtrip, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("addRoundTrip", caught);
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  protected boolean shouldAddToAudioTable() {
    return false;
  }

  /**
   * @return
   * @see #postAudioFile(String)
   */
  abstract protected AudioType getAudioType();

  private Widget getOuter() {
    return this;
  }

  private void logRoundtripTime(AudioAnswer result, long roundtrip) {
    String message = "PostAudioRecordButton : (success) User #" + getUser() +
        " post audio took " + roundtrip + " millis, audio dur " +
        result.getDurationInMillis() + " millis, " +
        " " + ((float) roundtrip / (float) result.getDurationInMillis()) + " roundtrip/audio duration ratio.";
    logMessage(message, false);
  }

  private void logMessage(String message, boolean sendEmail) {
    controller.getService().logMessage(message, sendEmail, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }


  /**
   * Feedback for when audio isn't valid for some reason.
   *
   * @param toShow
   */
  private void showPopup(String toShow) {
    new PopupHelper().showPopup(toShow, getOuter(), 3000);
  }
}
