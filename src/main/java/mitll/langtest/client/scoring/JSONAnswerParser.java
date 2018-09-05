package mitll.langtest.client.scoring;

import com.google.gwt.json.client.*;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

public class JSONAnswerParser {
  public static final String PHONE_TRANSCRIPT = "PHONE_TRANSCRIPT";
  public static final String WORD_TRANSCRIPT = "WORD_TRANSCRIPT";
  private final Logger logger = Logger.getLogger("JSONAnswerParser");

  private static final String REQID = "reqid";
  private static final String VALID = "valid";

  @NotNull
  AudioAnswer getAudioAnswer(JSONObject jsonObject) {
//    String message = getField(jsonObject, "MESSAGE");
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

    converted.setDynamicRange(getFloatField(jsonObject, "dynamicRange"));
    //useInvalidResult(validity, getFloatField(jsonObject, "dynamicRange"));

    if (validity == Validity.OK || validity == Validity.CUT_OFF) {
      // logger.info("Got validity " + validity);
      converted.setTimestamp(getLongField(jsonObject, "timestamp"));

      float score = getFloatField(jsonObject, "score");
      converted.setScore(score);
      converted.setCorrect(getBoolean(jsonObject, "isCorrect"));


      JSONValue phone_transcript = jsonObject.get(PHONE_TRANSCRIPT);

      List<TranscriptSegment> psegments =
          (phone_transcript == null) ? Collections.emptyList() : getSegments(phone_transcript.isArray());

      JSONValue word_transcript = jsonObject.get(WORD_TRANSCRIPT);

      List<TranscriptSegment> wsegments =
          word_transcript == null ? Collections.emptyList() : getSegments(word_transcript.isArray());

      Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes = new HashMap<>();
      sTypeToEndTimes.put(NetPronImageType.PHONE_TRANSCRIPT, psegments);
      sTypeToEndTimes.put(NetPronImageType.WORD_TRANSCRIPT, wsegments);

      float wavFileLengthSeconds = ((float) converted.getDurationInMillis()) / 1000F;
      PretestScore pretestScore = new PretestScore(score, new HashMap<>(),
          new HashMap<>(),
          new HashMap<>(),
          sTypeToEndTimes, "", wavFileLengthSeconds,
          0, true);

      converted.setPretestScore(pretestScore);
    } else {
      logger.info("gotResponse Got " + jsonObject);
    }

    return converted;
  }

  /**
   *
   * @param jsonObject
   * @return
   */
  StreamResponse getResponse(JSONObject jsonObject) {
    return new StreamResponse(getValidity(jsonObject), getStreamTimestamp(jsonObject));
  }

  private long getStreamTimestamp(JSONObject jsonObject) {
    return getLongField(jsonObject, "STREAMTIMESTAMP".toLowerCase());
  }

  @NotNull
  public Validity getValidity(JSONObject jsonObject) {
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
  }

  private long getLongField(JSONObject jsonObject, String reqid) {
//    JSONValue jsonValue = jsonObject.get(reqid);
//    return (long) (jsonValue == null ? 0L : jsonValue.isNumber().doubleValue());
//
//
//
    JSONValue jsonValue = jsonObject.get(reqid);
    if (jsonValue == null) return 0;
    else if (jsonValue.isNumber() == null) {
//      if (!reqid.equalsIgnoreCase(REQID)) {
//        logger.warning("huh? " + reqid + " is not a number? " + jsonValue.getClass());
//      }
      JSONString string = jsonObject.get(reqid).isString();
      String s = string.stringValue();
      try {
        return Long.parseLong(s);
      } catch (NumberFormatException e) {
        logger.warning("can't parse " + s);
        return 0;
      }
    } else
      return (int) jsonValue.isNumber().doubleValue();
  }

  private float getFloatField(JSONObject jsonObject, String reqid) {
    JSONValue jsonValue = jsonObject.get(reqid);
    if (jsonValue == null) logger.warning("no field " + reqid);
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
