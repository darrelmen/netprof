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
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PlayAudioEvent;
import mitll.langtest.client.initial.PopupHelper;
import mitll.langtest.client.initial.WavCallback;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  public static final String REQID = "reqid";
  public static final String VALID = "valid";
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

    logger.info("startRecording!");
    controller.startRecording();
    controller.startStream(getExerciseID(), reqid);
  }

  /**
   * @param duration
   * @see RecordButton#stop
   */
  public boolean stopRecording(long duration) {
    if (duration > MIN_DURATION) {
//      logger.info("stopRecording duration " + duration + " > min = " + MIN_DURATION);

      long then = System.currentTimeMillis();
      controller.stopRecording(new WavCallback() {
        @Override
        public void getBase64EncodedWavFile(String bytes) {
          postAudioFile(bytes);
        }

        @Override
        public void gotStreamResponse(String json) {
      //    logger.info("gotStreamResponse " + json);
         // reqid++;


          JSONObject digestJsonResponse = digestJsonResponse(json);
          String message = getField(digestJsonResponse, "MESSAGE");
          if (!message.isEmpty()) {
            // got interim OK
        //    logger.info("got interim " + message + " " + json);
          } else if (!getField(digestJsonResponse,"status").isEmpty()) {
            String code = getField(digestJsonResponse, "code");
            onPostFailure(then, getUser(), "error (" + code + ") posting audio for exercise " + getExerciseID());
          } else {
            onPostSuccess(getAudioAnswer(digestJsonResponse), then);
          }
        }
      });

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
   * @param validity
   * @param dynamicRange
   * @see PostAudioRecordButton#postAudioFile
   */
  protected void useInvalidResult(Validity validity, double dynamicRange) {
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
   * This must be kept in sync with audio service and ScoreToJson
   *
   * @param json
   * @see mitll.langtest.server.scoring.JsonScoring#getJsonObject
   */
  private AudioAnswer gotResponse(String json) {
    return getAudioAnswer(digestJsonResponse(json));
  }

  @NotNull
  private AudioAnswer getAudioAnswer(JSONObject jsonObject) {
    String message = getField(jsonObject, "MESSAGE");
    if (message.equalsIgnoreCase("OK") && false) return null;
    else {
      Validity validity = getValidity(jsonObject);

      // logger.info("Validity is " + validity);

      AudioAnswer converted = new AudioAnswer(
          getField(jsonObject, "path"),
          validity,
          getIntField(jsonObject, REQID),
          getIntField(jsonObject, "duration"),
          getIntField(jsonObject, "exid")
      );

      converted.setResultID(getIntField(jsonObject, "resultID"));

      //useInvalidResult(validity, getFloatField(jsonObject, "dynamicRange"));

      if (validity == Validity.OK || validity == Validity.CUT_OFF) {
        // logger.info("Got validity " + validity);
        converted.setTimestamp(getLongField(jsonObject, "timestamp"));

        float score = getFloatField(jsonObject, "score");
        converted.setScore(score);
        converted.setCorrect(getBoolean(jsonObject, "isCorrect"));


        List<TranscriptSegment> psegments = getSegments(jsonObject.get("PHONE_TRANSCRIPT").isArray());
        List<TranscriptSegment> wsegments = getSegments(jsonObject.get("WORD_TRANSCRIPT").isArray());
        float wavFileLengthSeconds = ((float) converted.getDurationInMillis()) / 1000F;
        Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes = new HashMap<>();
        sTypeToEndTimes.put(NetPronImageType.PHONE_TRANSCRIPT, psegments);
        sTypeToEndTimes.put(NetPronImageType.WORD_TRANSCRIPT, wsegments);
        PretestScore pretestScore = new PretestScore(score, new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            sTypeToEndTimes, "", wavFileLengthSeconds,
            0, true);
        converted.setPretestScore(pretestScore);
        // useResult(converted);
      } else {
        logger.info("gotResponse Got " + jsonObject);
      }

      //  logger.info("Got " + jsonObject);

      return converted;
    }
  }

  @NotNull
  private Validity getValidity(JSONObject jsonObject) {
    String valid = getField(jsonObject, VALID);
    Validity validity;
    try {
      validity = Validity.valueOf(valid);
    } catch (IllegalArgumentException e) {
      validity = Validity.INVALID;
    }
    return validity;
  }

  private List<TranscriptSegment> getSegments(JSONArray phone_transcript) {
    List<TranscriptSegment> pseg = new ArrayList<>();
    for (int i = 0; i < phone_transcript.size(); i++) {
      JSONObject object = phone_transcript.get(i).isObject();
      String event = getField(object, "event");
      pseg.add(new TranscriptSegment(
              getFloatField(object, "start"),
              getFloatField(object, "end"),
              event,
              getFloatField(object, "score"),
              event,
              i
          )
      );
    }
    return pseg;
  }

  private int getIntField(JSONObject jsonObject, String reqid) {
    JSONValue jsonValue = jsonObject.get(reqid);
    if (jsonValue == null) return 0;
    else if (jsonValue.isNumber() == null) {
      if (!reqid.equalsIgnoreCase(REQID)) {
        logger.warning("huh? " + reqid + " is not a number? " + jsonValue.getClass());
      }
      try {
        JSONString string = jsonObject.get(reqid).isString();
        String s = string.stringValue();
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        logger.warning("can't parse " + jsonObject.get(reqid).isString().stringValue());
        return 0;
      }
    } else
      return (int) jsonValue.isNumber().doubleValue();
    // return Integer.parseInt(jsonObject.get(reqid).().stringValue());
  }

  private long getLongField(JSONObject jsonObject, String reqid) {
    JSONValue jsonValue = jsonObject.get(reqid);
    return (long) (jsonValue == null ? 0L : jsonValue.isNumber().doubleValue());

    //return Long.parseLong(getField(jsonObject, reqid));
  }

  private float getFloatField(JSONObject jsonObject, String reqid) {
    JSONValue jsonValue = jsonObject.get(reqid);
    return (float) (jsonValue == null ? 0F : jsonValue.isNumber().doubleValue());
//    return Float.parseFloat(getField(jsonObject, reqid));
  }

  /*
  private double getDoubleField(JSONObject jsonObject, String reqid) {
    return jsonObject.get(reqid).isNumber().doubleValue();
//    return Float.parseFloat(getField(jsonObject, reqid));
  }
*/

  private String getField(JSONObject jsonObject, String valid1) {
    JSONValue jsonValue = jsonObject.get(valid1);
    JSONString string = jsonValue == null ? new JSONString("") : jsonValue.isString();
    return string == null ? "" : string.stringValue();
  }

  private boolean getBoolean(JSONObject jsonObject, String valid1) {
    JSONValue jsonValue = jsonObject.get(valid1);
    return jsonValue != null && jsonValue.isBoolean().booleanValue();
  }

  /**
   * Digest a json response from a servlet checking for a session expiration code
   *
   * @see mitll.langtest.server.scoring.JsonScoring#getJsonObject(int, int, DecoderOptions, boolean, net.sf.json.JSONObject, boolean, AudioAnswer, boolean)
   */
  private JSONObject digestJsonResponse(String json) {
    //  logger.info("Digesting response " + json);
    try {
      JSONValue val = JSONParser.parseStrict(json);
      JSONObject obj = (val != null) ? val.isObject() : null;


//      JSONValue code = obj == null ? null : obj.get(Constants.SESSION_EXPIRED_CODE);
//      if (code != null && code.isBoolean() != null && code.isBoolean().booleanValue()) {
//        getSessionHelper().logoutUserInClient(null, true);
//        return null;
//      } else {
//        return obj;
//      }

      return obj;
    } catch (Exception ex) {
      logger.warning("couldn't parse " + json);
      return new JSONObject();
    }
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

  private void onPostFailure(long then, int user, String exception) {
    long now = System.currentTimeMillis();
    logger.info("PostAudioRecordButton : (failure) posting audio took " + (now - then) + " millis :\n" + exception);
    logMessage("failed to post audio for " + user + " exercise " + getExerciseID(), true);
    showPopup(Validity.INVALID.getPrompt());
  }

  /**
   * TODO : fix reqid...
   *
   * @param result
   * @param then
   */
  private void onPostSuccess(AudioAnswer result, long then) {
    long now = System.currentTimeMillis();
    long roundtrip = now - then;

    if (false) {
      logger.info("PostAudioRecordButton : onPostSuccess Got audio " +
          "\n\tanswer    " + result +
          "\n\troundtrip " + roundtrip);
    }

/*    if (result.getReqid() != reqid) {
      logger.info("onPostSuccess ignoring old response " + result);
      return;
    }*/
    if (result.getValidity() == Validity.OK || doQuietAudioCheck(result)) {
      validAudio = true;
      useResult(result);
      addRT(result.getResultID(), (int) roundtrip);
    } else {
      validAudio = false;
      useInvalidResult(result.getValidity(), result.getDynamicRange());
    }
    if (controller.isLogClientMessages() || roundtrip > LOG_ROUNDTRIP_THRESHOLD) {
      logRoundtripTime(result.getDurationInMillis(), roundtrip);
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

  private void addRT(int resultID, int roundtrip) {
    // int resultID = result.getResultID();
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

  private Widget getOuter() {
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
  private void showPopup(String toShow) {
    logger.info("showPopup " + toShow + " on " + getExerciseID());

    new PopupHelper().showPopup(toShow, getOuter(), 3000);
  }
}
