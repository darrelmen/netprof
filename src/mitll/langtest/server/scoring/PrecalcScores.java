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

package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.shared.answer.AudioType;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/28/15.
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
      float pronScore = precalcResult.getPronScore();
      scores = getCachedScores(pronScore, jsonObject, usePhoneToDisplay);
      isValid = isPrecalcValidCheck();
//      logger.debug("for cached result " + precalcResult + " is valid " + isValid + " : " + precalcResult.getJsonScore());
    } else {
      isValid = false;
    }
  }

  /**
   * Sometimes we write the unknown model alignment into the ref result table - really shouldn't do that.
   * @return
   */
  private boolean isPrecalcValidCheck() {
    Map<String, Float> stringFloatMap = scores.eventScores.get(Scores.WORDS);
    boolean avp = scores.eventScores.isEmpty() ||
        (stringFloatMap.isEmpty() &&
            (precalcResult.getAudioType() != AudioType.PRACTICE || precalcResult.isCorrect())
        );

//    boolean onlyUnknown = stringFloatMap != null && stringFloatMap.size() == 1 && stringFloatMap.containsKey(SLFFile.UNKNOWN_MODEL);
    return !avp && !stringFloatMap.containsKey(SLFFile.UNKNOWN_MODEL);
  }

  /**
   * @param pronScore
   * @param jsonObject
   * @return
   * @see #PrecalcScores(ServerProperties, Result, boolean)
   */
  private Scores getCachedScores(float pronScore, JSONObject jsonObject, boolean usePhones) {
    Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = parseResultJson.parseJson(jsonObject, "words", "w", usePhones);
    Map<String, Map<String, Float>> eventScores = getEventAverages(imageTypeMapMap);
    return new Scores(pronScore, eventScores, 0);
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
