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

package mitll.langtest.client.scoring;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Command;
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
import mitll.langtest.shared.scoring.PretestScore;

import java.util.logging.Logger;

import static mitll.langtest.shared.answer.Validity.OK;

public abstract class PostAudioRecordButton extends RecordButton
    implements RecordButton.RecordingListener {
  private static final String MESSAGE = "MESSAGE";
  private final Logger logger = Logger.getLogger("PostAudioRecordButton");

  // TODO : enum
  private static final String ABORT = "ABORT";

  private static final boolean USE_DELAY = true;
  private static final String END = "END";

  public static final int MIN_DURATION = 250;

  private boolean validAudio = false;
  private static final int LOG_ROUNDTRIP_THRESHOLD = 3000;
  private int reqid = 0;
  private int exerciseID;
  protected final ExerciseController controller;
  private final JSONAnswerParser jsonAnswerParser = new JSONAnswerParser();
  private int projid;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PACKET = false;

  /**
   * @param exerciseID
   * @param controller
   * @param recordButtonTitle
   * @param stopButtonTitle
   * @param buttonWidth
   * @see FeedbackPostAudioRecordButton#FeedbackPostAudioRecordButton(int, RecordingAudioListener, ExerciseController)
   * @see mitll.langtest.client.recorder.FlashcardRecordButton#FlashcardRecordButton(int, ExerciseController, RecordingListener, boolean)
   */
  public PostAudioRecordButton(int exerciseID,
                               final ExerciseController controller,
                               String recordButtonTitle,
                               String stopButtonTitle,
                               int buttonWidth) {
    super(controller.getRecordTimeout(),
        controller.getProps().doClickAndHold(),
        recordButtonTitle,
        stopButtonTitle,
        controller.getProps());
    setRecordingListener(this);
    this.exerciseID = exerciseID;
    this.controller = controller;
    this.projid = controller.getProjectID();

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
   * <p>
   * Send a message to any play buttons so they can stop playing any audio.
   *
   * @see mitll.langtest.client.sound.PlayAudioPanel#gotPlayAudioEvent
   */
  public void startRecording() {
    LangTest.EVENT_BUS.fireEvent(new PlayAudioEvent(-1));

    ClientAudioContext clientAudioContext = new ClientAudioContext(getExerciseID(),
        reqid,
        shouldAddToAudioTable(),
        getAudioType(),
        getDialogSessionID(),
        getDevice()  // device = session id in quiz
    );
    if (DEBUG) {
      logger.info("startRecording : " + clientAudioContext);
    }

    controller.startStream(clientAudioContext, this::gotPacketResponse);
  }

  private long stopRecordingReqTimestamp;

  /**
   * TODO : consider putting back reqid increment
   *
   * @param duration
   * @param abort
   * @see mitll.langtest.client.recorder.FlashcardRecordButton#stopRecording(long, boolean)
   * @see RecordButton#stop
   */
  public boolean stopRecording(long duration, boolean abort) {
    if (DEBUG) logger.info("stopRecording " + duration);
    stopRecordingReqTimestamp = System.currentTimeMillis();
    controller.stopRecording(shouldUseRecordingStopDelay(), abort);

    if (duration > MIN_DURATION) {
      if (DEBUG) logger.info("stopRecording duration " + duration + " > min = " + MIN_DURATION);
      return true;
    } else {
      hideWaveform();
      gotShortDurationRecording();
      logger.info("stopRecording duration " + duration + " < min = " + MIN_DURATION);
      return false;
    }
  }

  protected boolean shouldUseRecordingStopDelay() {
    return USE_DELAY;
  }

  /**
   * We can get back
   * START - starting a recording... but just like a STREAM packet
   * STREAM - a mid point packet of audio
   * ABORT - we cancelled it!
   * END - got the final packet, let's look at the ASR result
   *
   * or nothing - in which case we got a 500 or 503 or something...
   * <p>
   * So if we get something other than a 200 http response, stop recording and show a big failure warning.
   * which is different than a "hey, please speak in response to the prompt" or "check your mic"
   *
   * Need to make sure this response is for me!
   *
   * @param json
   * @see #startRecording
   */
  private void gotPacketResponse(String json) {
    if (DEBUG_PACKET) logger.info("gotPacketResponse " + json);
    JSONObject digestJsonResponse = jsonAnswerParser.digestJsonResponse(json);
    if (DEBUG_PACKET) logger.info("gotPacketResponse digestJsonResponse " + digestJsonResponse);

    String message = getMessage(digestJsonResponse);
    if (message.isEmpty()) {  // should be a message on every valid packet...?
      if (DEBUG_PACKET) logger.info("gotPacketResponse message empty!");
      handlePostError(System.currentTimeMillis(), digestJsonResponse);
    } else if (message.equalsIgnoreCase(ABORT)) {
      logger.info("gotPacketResponse gotAbort");
      gotAbort();
    } else if (message.equalsIgnoreCase(END)) {
      AudioAnswer audioAnswer = jsonAnswerParser.getAudioAnswer(digestJsonResponse);
      if (DEBUG_PACKET || audioAnswer.getValidity() != OK) {
        logger.info("gotPacketResponse audioAnswer " + audioAnswer);
        if (DEBUG) {
          logger.info("gotPacketResponse json        " + json);
        }
        controller.logEvent(this, "gotPacketResponse", "" + exerciseID, "invalid recording " + audioAnswer.getValidity());
      }
      onPostSuccess(audioAnswer, stopRecordingReqTimestamp);
    } else {
      if (DEBUG_PACKET) {
        logger.info("gotPacketResponse: post " + getElement().getId() +
            "\n\tgot " + json);
      }
      usePartial(jsonAnswerParser.getResponse(digestJsonResponse));
    }
  }

  private String getMessage(JSONObject digestJsonResponse) {
    return jsonAnswerParser.getField(digestJsonResponse, MESSAGE.toLowerCase());
  }

  /**
   * @param then
   * @param digestJsonResponse
   * @see #gotPacketResponse
   */
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

    if (status.getCode() == 0) {
      long now = System.currentTimeMillis();
      long l = now - then;
      logMessage("failed to post audio for " +
              "\n\tuser      " + getUser() +
              "\n\tduration  " + l +
              "\n\texercise  " + getExerciseID() +
              "\n\texception " + status +
              "\n\tfields    " + status.getKv()
          , true);
    } else {
      onPostFailure(
          then,
          getUser(),
          "error (" + status + ") posting audio for exercise " + getExerciseID(),
          status);
    }
  }

  /**
   * why so confusing -- ?
   *
   * @param result
   * @see #onPostSuccess
   */
  public abstract void useResult(AudioAnswer result);

  void usePartial(StreamResponse validity) {
    // logger.info("got " + validity);
  }

  void gotAbort() {
    logger.warning("gotAbort\n\n\n ");
  }

  public boolean hasValidAudio() {
    return validAudio;
  }

  protected void hideWaveform() {
  }

  void gotShortDurationRecording() {
    logger.info("gotShortDurationRecording");

    showPopupLater("Recording too short");
  }

  protected String getDevice() {
    //logger.info("getDevice default " + getClass());
    return controller.getBrowserInfo();
  }

  /**
   * @param then
   * @param user
   * @param exception
   */
  private void onPostFailure(long then, int user, String exception, HttpStatus status) {
    onPostFailure();

    long now = System.currentTimeMillis();
    long l = now - then;
    logger.info("PostAudioRecordButton : (failure) posting audio took " + l + " millis :\n" + exception);
    showWarning(Validity.INVALID.getPrompt() + "\n" + exception);

    logMessage("failed to post audio for " +
            "\n\tuser      " + user +
            "\n\tduration  " + l +
            "\n\texercise  " + getExerciseID() +
            "\n\texception " + exception +
            "\n\tfields    " + status.getKv()
        , true);
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

  void onPostFailure() {
    logger.info("onPostFailure --- !");
  }

  /**
   * This could be valid or invalid.
   *
   * TODO : fix reqid...
   *
   * @param result
   * @param then
   * @see #gotPacketResponse
   */
  private void onPostSuccess(AudioAnswer result, long then) {
    long now = System.currentTimeMillis();
    long roundtrip = now - then;

    if (DEBUG) {
      logger.info("PostAudioRecordButton : onPostSuccess Got audio " +
          "\n\tanswer for " + result.getExid() +
          "\n\tscore      " + result.getScore() +
          "\n\troundtrip  " + roundtrip);
    }

/*    if (result.getReqid() != reqid) {
      logger.info("onPostSuccess ignoring old response " + result);
      return;
    }*/

    validAudio = (result.getValidity() == OK || doQuietAudioCheck(result));

    if (result.getAudioAttribute() != null) {
      if (result.getAudioAttribute().getAudioType() == null) {
        result.getAudioAttribute().setAudioType(getAudioType());
      }
    }
    if (validAudio) {
      useResult(result);
      addRT(result.getResultID(), (int) roundtrip);

      maybeShowPopup(result.getPretestScore());
    } else {
      useInvalidResult(result.getExid(), result.getValidity(), result.getDynamicRange());
    }

    if (controller.isLogClientMessages() || roundtrip > LOG_ROUNDTRIP_THRESHOLD) {
      logRoundtripTime(result.getDurationInMillis(), roundtrip);
    }
  }

  private void maybeShowPopup(PretestScore pretestScore) {
    if (pretestScore != null &&
        pretestScore.getStatus() != null &&
        !pretestScore.getStatus().isEmpty()) {
      String toShow = "Status " + pretestScore.getStatus();
      String suffix = pretestScore.getMessage().isEmpty() ? "" : " : " + pretestScore.getMessage();
      showPopupLater(toShow + suffix);
    }
  }

  /**
   * TODO : consider why we have to do this from the client.
   *
   * @param exid
   * @param validity
   * @param dynamicRange
   * @see PostAudioRecordButton#onPostSuccess
   */
  public void useInvalidResult(int exid, Validity validity, double dynamicRange) {
    controller.logEvent(this, "recordButton", "" + exerciseID, "invalid recording " + validity);
    // logger.info("useInvalidResult platform is " + getPlatform() + " validity " + validity);
    if (!checkAndShowTooLoud(validity)) {
      showPopupLater(validity.getPrompt());
    }
//    gotInvalidResult(validity);
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

  /**
   * @param resultID
   * @param roundtrip
   * @see #onPostSuccess
   */
  private void addRT(int resultID, int roundtrip) {
    controller.getScoringService().addRoundTrip(projid, resultID, roundtrip, new AsyncCallback<Void>() {
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
   * @see #startRecording
   */
  abstract protected AudioType getAudioType();

  abstract protected int getDialogSessionID();

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
  public void showPopupLater(String toShow) {
  //  logger.info("showPopupLater " + toShow + " on " + getExerciseID());
    Scheduler.get().scheduleDeferred((Command) () -> showPopupDismissLater(toShow));
  }

  void showPopupDismissLater(String toShow) {
  //  logger.info("showPopup " + toShow + " on " + getExerciseID());
    new PopupHelper().showPopup(toShow, getPopupTargetWidget(), 5000);
  }

  /**
   * @return
   */
  Widget getPopupTargetWidget() {
    logger.info("getPopupTargetWidget target is " + this.getElement().getId());
    return this;
  }
}
