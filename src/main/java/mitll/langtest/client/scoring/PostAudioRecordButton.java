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
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.dialog.ModalInfoDialog;
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
public abstract class PostAudioRecordButton extends RecordButton
    implements RecordButton.RecordingListener {
  private final Logger logger = Logger.getLogger("PostAudioRecordButton");

  // TODO : enum
  private static final String ABORT = "ABORT";

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PACKET = false;

  private static final boolean USE_DELAY = false;
  private static final String END = "END";

  public static final String REQID = "reqid";
  public static final String VALID = "valid";

  public static final int MIN_DURATION = 250;

  private boolean validAudio = false;
  private static final int LOG_ROUNDTRIP_THRESHOLD = 3000;
  private final int index;
  private int reqid = 0;
  private int exerciseID;
  protected final ExerciseController controller;
  private final boolean recordInResults;
  private final boolean scoreAudioNow;
  private JSONAnswerParser jsonAnswerParser = new JSONAnswerParser();

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

  /**
   * When we start recording, we ask to be called with a callback for updates...
   * This callback may not necessarily return a response that's for this exercise!
   */
  public void startRecording() {
    LangTest.EVENT_BUS.fireEvent(new PlayAudioEvent(-1));
    long then = System.currentTimeMillis();

    if (DEBUG) logger.info("startRecording!");
    controller.startRecording();
    controller.startStream(getExerciseID(), reqid, shouldAddToAudioTable(), getAudioType(), bytes -> gotPacketResponse(bytes, then));
  }

  /**
   * TODO : consider putting back reqid increment
   *
   * @param duration
   * @param abort
   * @see RecordButton#stop
   */
  public boolean stopRecording(long duration, boolean abort) {
    controller.stopRecording(bytes -> postAudioFile(bytes), USE_DELAY, abort);

    if (duration > MIN_DURATION) {
      logger.info("stopRecording duration " + duration + " > min = " + MIN_DURATION);
      return true;
    } else {
      hideWaveform();
      gotShortDurationRecording();
      logger.info("stopRecording duration " + duration + " < min = " + MIN_DURATION);
      return false;
    }
  }

  /**
   * We can get back START, STREAM, END or nothing - in which case we got a 500 or 503 or something...
   * <p>
   * So if we get something other than a 200 http response, stop recording and show a big failure warning.
   * which is different than a "hey, please speak in response to the prompt" or "check your mic"
   *
   * @param json
   * @see #startRecording
   */
  private void gotPacketResponse(String json, long then) {
    if (DEBUG_PACKET) logger.info("gotPacketResponse " + json);
    JSONObject digestJsonResponse = jsonAnswerParser.digestJsonResponse(json);
    if (DEBUG_PACKET) logger.info("gotPacketResponse digestJsonResponse " + digestJsonResponse);
    String message = getMessage(digestJsonResponse);
    if (message.isEmpty()) {
      if (DEBUG_PACKET) logger.info("gotPacketResponse message empty!");
      handlePostError(System.currentTimeMillis(), digestJsonResponse);
    } else if (message.equalsIgnoreCase(ABORT)) {
      logger.info("gotPacketResponse gotAbort");
      gotAbort();
    } else if (message.equalsIgnoreCase(END)) {
      AudioAnswer audioAnswer = jsonAnswerParser.getAudioAnswer(digestJsonResponse);
      if (DEBUG_PACKET) logger.info("gotPacketResponse audioAnswer " + audioAnswer);
      onPostSuccess(audioAnswer, then);
    } else {
      if (DEBUG_PACKET) {
        logger.info("gotPacketResponse: post " + getElement().getId() +
            "\n\tgot " + json);
      }
      usePartial(jsonAnswerParser.getResponse(digestJsonResponse));
    }
  }

  private String getMessage(JSONObject digestJsonResponse) {
    return jsonAnswerParser.getField(digestJsonResponse, "MESSAGE".toLowerCase());
  }

  private void handlePostError(long then, JSONObject digestJsonResponse) {
    HttpStatus status = new HttpStatus(digestJsonResponse);
    if (status.isWellFormed() && status.getCode() != 200) {
      onStreamPostFailure(status, then);
    }
  }

  private void onStreamPostFailure(HttpStatus status, long then) {
    if (isRecording()) {
      stopRecordingFirstStep();
    }

    onPostFailure(
        then,
        getUser(),
        "error (" + status.getCode() + " : " + status.getStatusText() +
            ") posting audio for exercise " + getExerciseID());
  }

  /**
   * why so confusing -- ?
   *
   * @param result
   * @see #onPostSuccess
   */
  public abstract void useResult(AudioAnswer result);

  public void usePartial(StreamResponse validity) {
    // logger.info("got " + validity);
  }

  public void gotAbort() {
    logger.warning("gotAbort\n\n\n ");

  }

  /**
   * TODO : consider why we have to do this from the client.
   *
   * @param exid
   * @param validity
   * @param dynamicRange
   * @see PostAudioRecordButton#postAudioFile
   */
  protected void useInvalidResult(int exid, Validity validity, double dynamicRange) {
    controller.logEvent(this, "recordButton", "" + exerciseID, "invalid recording " + validity);
    logger.info("useInvalidResult platform is " + getPlatform());
    if (!checkAndShowTooLoud(validity)) {
      showPopup(validity.getPrompt());
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
   * @see RecordingListener#stopRecording
   * @deprecated
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


    logger.info("\n\n\n\n\nPostAudioRecordButton.postAudioFile : " + getAudioType() + " : " + audioContext +
        "\n\t bytes " + base64EncodedWavFile.length());


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
        getDevice(),
        decoderOptions, new AsyncCallback<AudioAnswer>() {
          public void onFailure(Throwable caught) {
            onPostFailure(then, user, getExceptionAsString(caught));
            controller.handleNonFatalError("posting audio", caught);
          }

          public void onSuccess(AudioAnswer result) {
            onPostSuccess(result, then);
          }
        });
  }

  protected String getDevice() {
    return controller.getBrowserInfo();
  }

  /**
   * @param then
   * @param user
   * @param exception
   */
  void onPostFailure(long then, int user, String exception) {
    onPostFailure();

    long now = System.currentTimeMillis();
    logger.info("PostAudioRecordButton : (failure) posting audio took " + (now - then) + " millis :\n" + exception);
    showWarning(Validity.INVALID.getPrompt() + "\n" + exception);

    logMessage("failed to post audio for " + user + " exercise " + getExerciseID(), true);
  }


  /**
   * TODO : don't do this...
   */
  private boolean showing = false;

  private void showWarning(String toShow) {
    if (!showing) {
      new ModalInfoDialog("Warning", toShow, hiddenEvent -> showing = false);
      showing = true;
    }
  }

  protected void onPostFailure() {
    logger.info("onPostFailure --- !");
  }

  /**
   * TODO : fix reqid...
   *
   * @param result
   * @param then
   * @see #gotPacketResponse(String, long)
   * @see #postAudioFile(String)
   */
  private void onPostSuccess(AudioAnswer result, long then) {
    long now = System.currentTimeMillis();
    long roundtrip = now - then;

    if (true) {
      logger.info("PostAudioRecordButton : onPostSuccess Got audio " +
          "\n\tanswer for " + result.getExid() +
          "\n\tscore      " + result.getScore() +
          "\n\troundtrip  " + roundtrip);
    }

/*    if (result.getReqid() != reqid) {
      logger.info("onPostSuccess ignoring old response " + result);
      return;
    }*/
    if (result.getValidity() == Validity.OK || doQuietAudioCheck(result)) {
      setValidAudio(true);
      useResult(result);

      addRT(result.getResultID(), (int) roundtrip);
    } else {
      setValidAudio(false);
      useInvalidResult(result.getExid(), result.getValidity(), result.getDynamicRange());
    }


    if (controller.isLogClientMessages() || roundtrip > LOG_ROUNDTRIP_THRESHOLD) {
      logRoundtripTime(result.getDurationInMillis(), roundtrip);
    }
  }

  private void setValidAudio(boolean validAudio) {
    this.validAudio = validAudio;
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

  private void addRT(int resultID, int roundtrip) {
    controller.getScoringService().addRoundTrip(resultID, roundtrip, new AsyncCallback<Void>() {
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

  protected Widget getPopupTargetWidget() {
    return this;
  }

  private void logRoundtripTime(long durationInMillis, long roundtrip) {
    //  long durationInMillis = result.getDurationInMillis();
    String message = "PostAudioRecordButton : (success) User #" + getUser() +
        " post audio took " + roundtrip + " millis, audio dur " +
        durationInMillis + " millis, " +
        " " + ((float) roundtrip / (float) durationInMillis) + " roundtrip/audio duration ratio.";
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
  protected void showPopup(String toShow) {
    logger.info("showPopup " + toShow + " on " + getExerciseID());
    new PopupHelper().showPopup(toShow, getPopupTargetWidget(), 3000);
  }
}
