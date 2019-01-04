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
  public static final String PATH = "path";
  private final Logger logger = Logger.getLogger("JSONAnswerParser");

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
//    String message = getField(jsonObject, "MESSAGE");
    Validity validity = getValidity(jsonObject);

    JSONValue jsonValue2 = jsonObject.get(PATH);
    if (jsonValue2 == null) logger.info("no path on json? validity = " + validity);

    AudioAnswer converted = new AudioAnswer(
        getField(jsonObject, PATH),
        validity,
        getIntField(jsonObject, REQID),
        getIntField(jsonObject, "duration"),
        getIntField(jsonObject, "exid")
    );

    converted.setResultID(getIntField(jsonObject, "resultID"));

    converted.setDynamicRange(getFloatField(jsonObject, "dynamicRange"));
    //useInvalidResult(validity, getFloatField(jsonObject, "dynamicRange"));
    long timestamp = getLongField(jsonObject, "timestamp");
  //  logger.info("getAudioAnswer json timestamp " + timestamp + " " + new Date(timestamp));
    converted.setTimestamp(timestamp);

    if (validity == Validity.OK /*|| validity == Validity.CUT_OFF*/) {
      // logger.info("Got validity " + validity);
      float score = getFloatField(jsonObject, "score");
      converted.setScore(score);
      converted.setCorrect(getBoolean(jsonObject, "isCorrect"));

      Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes = new HashMap<>();
      sTypeToEndTimes.put(NetPronImageType.PHONE_TRANSCRIPT, getTranscriptSegments(jsonObject, PHONE_TRANSCRIPT));
      sTypeToEndTimes.put(NetPronImageType.WORD_TRANSCRIPT, getTranscriptSegments(jsonObject, WORD_TRANSCRIPT));

      float wavFileLengthSeconds = ((float) converted.getDurationInMillis()) / 1000F;

      // if somehow we don't get full match field, skip it
      JSONValue jsonValue = jsonObject.get("isfullmatch");
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

  private List<TranscriptSegment> getTranscriptSegments(JSONObject jsonObject, String phoneTranscript) {
    JSONValue phone_transcript = jsonObject.get(phoneTranscript);
    return (phone_transcript == null) ? Collections.emptyList() : getSegments(phone_transcript.isArray());
  }

  /**
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
