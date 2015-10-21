package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.Result;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 9/28/15.
 */
class PrecalcScores {
//  private static final Logger logger = Logger.getLogger(PrecalcScores.class);

  private final Result precalcResult;
  private Scores scores;
  private JSONObject jsonObject;
  private final boolean isValid;
  private final ParseResultJson parseResultJson;

  /**
   * TODO : use this with webservice scoring too
   * @see ASRScoring#scoreRepeatExercise(String, String, String, String, String, int, int, boolean, boolean, String, boolean, String, Result, boolean)
   * @param serverProperties
   * @param precalcResult
   * @param usePhoneToDisplay
   */
  public PrecalcScores(ServerProperties serverProperties, Result precalcResult, boolean usePhoneToDisplay) {
    this.parseResultJson = new ParseResultJson(serverProperties);
    this.precalcResult = precalcResult;

    boolean valid = isValidPrecalc();

    if (valid) {
      jsonObject = JSONObject.fromObject(precalcResult.getJsonScore());
      scores = getCachedScores(precalcResult, jsonObject, usePhoneToDisplay);

      isValid = isPrecalcValidCheck();
//      logger.debug("for cached result " + precalcResult + " is valid " + isValid + " : " + precalcResult.getJsonScore());
    } else {
      isValid = false;
    }
  }

  /**
   * @param precalcResult
   * @param jsonObject
   * @return
   */
  private Scores getCachedScores(Result precalcResult, JSONObject jsonObject, boolean usePhones) {
    Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = parseResultJson.parseJson(jsonObject, "words", "w", usePhones);
    Map<String, Map<String, Float>> eventScores = getEventAverages(imageTypeMapMap);

    Scores scores = new Scores(precalcResult.getPronScore(), eventScores, 0);
    return scores;
  }

  /**
   * @param imageTypeMapMap
   * @return
   * @see #getCachedScores
   */
  private Map<String, Map<String, Float>> getEventAverages(Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap) {
    Map<String, Map<String, Float>> eventScores = new HashMap<String, Map<String, Float>>();
    // phones
    Map<Float, TranscriptEvent> floatTranscriptEventMap = imageTypeMapMap.get(ImageType.PHONE_TRANSCRIPT);
    Map<String, Float> value2 = new HashMap<>();
    eventScores.put(Scores.PHONES, value2);
    if (floatTranscriptEventMap != null) {
      getEventAverages(floatTranscriptEventMap, value2);
    }
    // words
    floatTranscriptEventMap = imageTypeMapMap.get(ImageType.WORD_TRANSCRIPT);
    value2 = new HashMap<>();
    eventScores.put(Scores.WORDS, value2);
    if (floatTranscriptEventMap != null) {
      getEventAverages(floatTranscriptEventMap, value2);
    }
    return eventScores;
  }

  /**
   * @param floatTranscriptEventMap
   * @param value2
   * @see #getEventAverages(Map, Map)
   */
  private void getEventAverages(Map<Float, TranscriptEvent> floatTranscriptEventMap, Map<String, Float> value2) {
    Map<String, Float> value = new HashMap<>();
    Map<String, Float> cvalue = new HashMap<>();

    for (TranscriptEvent ev : floatTranscriptEventMap.values()) {
      String event = ev.event;
      if (event.equals("sil") || event.equals("<s>") || event.equals("</s>")) {
      } else {
        Float orDefault = cvalue.getOrDefault(event, 0.0f);
        orDefault += 1.0f;
        cvalue.put(event, orDefault);

        Float orDefault1 = value.getOrDefault(event, 0.0f);
        value.put(event, orDefault1 + ev.score);
      }
    }

    for (Map.Entry<String, Float> pair : value.entrySet()) {
      String key = pair.getKey();
      value2.put(key, pair.getValue() / cvalue.get(key));
    }
  }

  private boolean isValidPrecalc() {
    boolean isInvalid = precalcResult == null ||
        (precalcResult.isValid() &&
            (precalcResult.getPronScore() < 0 || precalcResult.getJsonScore() == null || precalcResult.getJsonScore().isEmpty()));
    return !isInvalid;
  }

  private boolean isPrecalcValidCheck() {
    boolean avp = scores.eventScores.isEmpty() ||
        (scores.eventScores.get(Scores.WORDS).isEmpty() &&
            (!precalcResult.getAudioType().equals("avp") || precalcResult.isCorrect())
        );
    return !avp;
  }

  public Scores getScores() {
    return scores;
  }

  public JSONObject getJsonObject() {
    return jsonObject;
  }

  public boolean isValid() {
    return isValid;
  }
}
