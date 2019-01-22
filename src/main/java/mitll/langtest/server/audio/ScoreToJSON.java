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

package mitll.langtest.server.audio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.scoring.ASR;
import mitll.langtest.server.scoring.PrecalcScores;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/3/15.
 */
public class ScoreToJSON {
  private static final Logger logger = LogManager.getLogger(ScoreToJSON.class);

  private static final String START = "start";
  private static final String END1 = "end";
  private static final String END = END1;
  private static final String EVENT = "event";
 // public static final String CONTENT = "content";
  private static final String SCORE = "score";
  private static final String STR = "str";
  private static final String ID = "id";
  private static final String SEGMENT_SCORE = "s";
//  public static final String OUR_FORTH_TONE = "uu4";

  /**
   * We skip sils, since we wouldn't want to show them to the user.
   *
   * @param answer
   * @return
   * @see AudioFileHelper#getAudioAnswerAlignment
   * @see AudioFileHelper#getAudioAnswerDecoding
   */
  JsonObject getJsonFromAnswer(AudioAnswer answer) {
    return getJsonObject(answer.getPretestScore());
  }

  /**
   * @param pretestScore
   * @return
   * @see AudioFileHelper#getASRScoreForAudio(int, String, String, Collection, String, ImageOptions, String, PrecalcScores, DecoderOptions)
   */
  String asJson(PretestScore pretestScore) {
    return getJsonObject(pretestScore).toString();
  }

  private static final JsonObject jsonNULL = new JsonObject();

  /**
   * @param pretestScore
   * @return
   * @see DecodeAlignOutput#DecodeAlignOutput(PretestScore, boolean)
   */
  public JsonObject getJsonObject(PretestScore pretestScore) {
    JsonObject jsonObject = new JsonObject();
    if (pretestScore != null) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = pretestScore.getTypeToSegments();

      if (netPronImageTypeListMap.isEmpty()) {
        String json = pretestScore.getJson();
        if (json == null || json.equalsIgnoreCase("null")) {
//          logger.warn("no json for score");
          return jsonNULL;
        } else {
          JsonParser parser = new JsonParser();

          JsonObject jsonObject1 = parser.parse(json).getAsJsonObject();
          logger.info("getJsonObject returning" +
              "\n\t json " + jsonObject1 +
              "\n\t from " + json);
          return jsonObject1;
        }
      } else {
        List<TranscriptSegment> words = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT);

        if (words != null) {
          List<TranscriptSegment> phones = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);

          int windex = 0;
          int pindex = 0;
          JsonArray jsonWords = new JsonArray();

          for (TranscriptSegment segment : words) {
            String event = segment.getEvent();
            if (isValidEvent(event)) {
              JsonObject wordJson = getJSONForWord(segment, event, Integer.toString(windex++));

              {
                JsonArray jsonPhones = new JsonArray();

                for (TranscriptSegment pseg : phones) {
                  if (pseg.getStart() >= segment.getStart() && pseg.getEnd() <= segment.getEnd()) {
                    String pevent = pseg.getEvent();
                    if (isValidEvent(pevent)) {
                      JsonObject phoneJson = getJSONForPhone(pindex, pseg, pevent);
                      pindex++;
                      jsonPhones.add(phoneJson);
                    }
                  }
                }
                wordJson.add("phones", jsonPhones);
              }
              jsonWords.add(wordJson);
            }
          }

          jsonObject.add("words", jsonWords);
        } else if (pretestScore.getHydecScore() > -0.1f) {
          logger.warn("no word transcript for " + pretestScore, new Exception());
        }
      }
    } else {
//      logger.warn("pretest score is null?");
    }
    return jsonObject;
  }

  private boolean isValidEvent(String event) {
    return !event.equals(ASR.UNKNOWN_MODEL) && !event.equals("sil") && !event.equals("SIL");
  }

  @NotNull
  private JsonObject getJSONForWord(TranscriptSegment segment, String event, String wid) {
    JsonObject wordJson = new JsonObject();
    wordJson.addProperty(ID, wid);
    wordJson.addProperty("w", event);
    wordJson.addProperty(SEGMENT_SCORE, getScore(segment));
    wordJson.addProperty(STR, floatToString(segment.getStart()));
    wordJson.addProperty(END1, floatToString(segment.getEnd()));
    return wordJson;
  }

  @NotNull
  private JsonObject getJSONForPhone(int pindex, TranscriptSegment pseg, String pevent) {
    JsonObject phoneJson = new JsonObject();
    phoneJson.addProperty(ID, Integer.toString(pindex));
    phoneJson.addProperty("p", pevent);
    phoneJson.addProperty(SEGMENT_SCORE, getScore(pseg));
    phoneJson.addProperty(STR, floatToString(pseg.getStart()));
    phoneJson.addProperty(END1, floatToString(pseg.getEnd()));
    return phoneJson;
  }

  private String getScore(TranscriptSegment segment) {
    float score = segment.getScore();
    return floatToString(score);
  }

  private String floatToString(float round) {
    return Float.toString(round(round));
  }

  private static float round(float d) {
    return round(d, 3);
  }

  private static float round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d)).
        setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }

  /**
   * What the iPad wants to see.
   * <p>
   * For both words and phones, return event text, start, end times, and score for event.
   * Add overall score.
   *
   * @param score
   * @param languageEnum
   * @return
   * @see mitll.langtest.server.scoring.JsonScoring#getJsonForAudioForUser
   */
  public JsonObject getJsonForScore(PretestScore score, boolean usePhoneDisplay, ServerProperties serverProps,
                                    Language languageEnum) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(SCORE, score.getHydecScore());

    for (Map.Entry<NetPronImageType, List<TranscriptSegment>> pair : score.getTypeToSegments().entrySet()) {
      List<TranscriptSegment> segments = pair.getValue();
      JsonArray eventArray = new JsonArray();
      NetPronImageType imageType = pair.getKey();

      boolean usePhone = imageType == NetPronImageType.PHONE_TRANSCRIPT &&
          (serverProps.usePhoneToDisplay(languageEnum) || usePhoneDisplay);

      int numSegments = segments.size();
      for (int i = 0; i < numSegments; i++) {
        TranscriptSegment segment = segments.get(i);

        JsonObject object = new JsonObject();
        String event = segment.getEvent();

        if (isValidEvent(event)) {
          if (usePhone) {  // remap to display labels
            event = serverProps.getDisplayPhoneme(languageEnum, segments, numSegments, i, event);
          }

          object.addProperty(EVENT, event);
          object.addProperty(START, segment.getStart());
          object.addProperty(END, segment.getEnd());
          object.addProperty(SCORE, segment.getScore());

          eventArray.add(object);
        }
      }

      jsonObject.add(imageType.toString(), eventArray);
    }
    return jsonObject;
  }
}