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

import com.google.gwt.json.client.*;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

class JSONAnswerParser {
  private final Logger logger = Logger.getLogger("JSONAnswerParser");

  public static final String TIMESTAMP = "timestamp";
  public static final String RESULT_ID = "resultID";
  public static final String IS_CORRECT = "isCorrect";

  public static final String PATH = "path";
  public static final String DYNAMIC_RANGE = "dynamicRange";
  public static final String DURATION = "duration";
  public static final String EXID = "exid";
  public static final String ISFULLMATCH = "isfullmatch";


  private static final String EVENT = "event";
  private static final String START = "start";
  private static final String END = "end";
  private static final String SCORE = "score";
  private static final String STREAMTIMESTAMP = "STREAMTIMESTAMP";
  private static final String STREAMSTOP = "STREAMSTOP";

  private static final String PHONE_TRANSCRIPT = "PHONE_TRANSCRIPT";
  private static final String WORD_TRANSCRIPT = "WORD_TRANSCRIPT";

  private static final String REQID = "reqid";
  private static final String VALID = "valid";

  /**
   * @param jsonObject
   * @return
   * @see PostAudioRecordButton#gotPacketResponse
   */
  @NotNull
  AudioAnswer getAudioAnswer(JSONObject jsonObject) {
    Validity validity = getValidity(jsonObject);

    boolean isValid = validity == Validity.OK;
    if (isValid && jsonObject.get(PATH) == null) {
      logger.warning("getAudioAnswer no path on json? validity = " + validity);
    }

    AudioAnswer converted = getAudioAnswer(jsonObject, validity);

    if (isValid /*|| validity == Validity.CUT_OFF*/) {
      // logger.info("Got validity " + validity);
      float score = getFloatField(jsonObject, SCORE);
      converted.setScore(score);
      converted.setCorrect(getBoolean(jsonObject, IS_CORRECT));

      Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes = new HashMap<>();
      sTypeToEndTimes.put(NetPronImageType.PHONE_TRANSCRIPT, getTranscriptSegments(jsonObject, PHONE_TRANSCRIPT));
      sTypeToEndTimes.put(NetPronImageType.WORD_TRANSCRIPT, getTranscriptSegments(jsonObject, WORD_TRANSCRIPT));

      float wavFileLengthSeconds = ((float) converted.getDurationInMillis()) / 1000F;

      // if somehow we don't get full match field, skip it
      JSONValue jsonValue = jsonObject.get(ISFULLMATCH);
      boolean isFullMatch = jsonValue == null || jsonValue.isBoolean().booleanValue();

      PretestScore pretestScore = new PretestScore(score, new HashMap<>(),
          new HashMap<>(),
          new HashMap<>(),
          sTypeToEndTimes, "", wavFileLengthSeconds,
          0, isFullMatch);

      converted.setPretestScore(pretestScore);

      converted.setAudioAttribute(new AudioAttribute().setAudioRef(converted.getPath()));
    } else {
      logger.info("getAudioAnswer invalid : " + jsonObject);
    }

    return converted;
  }

  @NotNull
  private AudioAnswer getAudioAnswer(JSONObject jsonObject, Validity validity) {
    AudioAnswer converted = new AudioAnswer(
        getField(jsonObject, PATH),
        validity,
        getFloatField(jsonObject, DYNAMIC_RANGE),
        getIntField(jsonObject, DURATION),
        getIntField(jsonObject, REQID),
        getIntField(jsonObject, EXID)
    );

    converted.setResultID(getIntField(jsonObject, RESULT_ID));
    // converted.setDynamicRange(getFloatField(jsonObject, DYNAMIC_RANGE));
    //  long timestamp = getLongField(jsonObject, "timestamp");
    //  logger.info("getAudioAnswer json timestamp " + timestamp + " " + new Date(timestamp));
    converted.setTimestamp(getLongField(jsonObject, TIMESTAMP));
    return converted;
  }

  private List<TranscriptSegment> getTranscriptSegments(JSONObject jsonObject, String phoneTranscript) {
    JSONValue phone_transcript = jsonObject.get(phoneTranscript);
    return (phone_transcript == null) ? Collections.emptyList() : getSegments(phone_transcript.isArray());
  }

  /**
   * @see PostAudioRecordButton#gotPacketResponse
   * @param jsonObject
   * @return
   */
  StreamResponse getResponse(JSONObject jsonObject) {
    return new StreamResponse(getValidity(jsonObject), getStreamTimestamp(jsonObject), getStreamStop(jsonObject));
  }

  private long getStreamTimestamp(JSONObject jsonObject) {
    return getLongField(jsonObject, STREAMTIMESTAMP.toLowerCase());
  }

  private boolean getStreamStop(JSONObject jsonObject) {
    return getBoolean(jsonObject, STREAMSTOP.toLowerCase());
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
      String event = getField(object, EVENT);
      pseg.add(new TranscriptSegment(
              getFloatField(object, START),
              getFloatField(object, END),
              event,
              getFloatField(object, SCORE),
              event
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
  }

  private long getLongField(JSONObject jsonObject, String reqid) {
    JSONValue jsonValue = jsonObject.get(reqid);
    if (jsonValue == null) return 0;
    else if (jsonValue.isNumber() == null) {
      //   logger.info("getLongField obj "  +jsonValue + " is not a number?");
      String s = jsonValue.isString().stringValue();
      try {
     //   logger.info("getLongField parse obj " + jsonValue + " : " + s);
        return Long.parseLong(s);
      } catch (NumberFormatException e) {
        logger.warning("can't parse " + s);
        return 0;
      }

    } else {
      return (long) jsonValue.isNumber().doubleValue();
    }
  }

  private float getFloatField(JSONObject jsonObject, String reqid) {
    JSONValue jsonValue = jsonObject.get(reqid);
   // if (jsonValue == null) logger.info("getFloatField no field " + reqid);
    return (float) (jsonValue == null ? 0F : jsonValue.isNumber().doubleValue());
  }

  String getField(JSONObject jsonObject, String valid1) {
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
  JSONObject digestJsonResponse(String json) {
    //  logger.info("Digesting response " + json);
    try {
      JSONValue val = JSONParser.parseStrict(json);
      return (val != null) ? val.isObject() : null;
    } catch (Exception ex) {
      logger.warning("couldn't parse '" + json + "'");
      return new JSONObject();
    }
  }

}
