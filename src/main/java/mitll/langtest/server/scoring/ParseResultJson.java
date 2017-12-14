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

import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.phone.PhoneDAO;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/28/15.
 */
public class ParseResultJson {
  private static final Logger logger = LogManager.getLogger(ParseResultJson.class);
  private static final String STR = "str";
  private static final String END = "end";
  private static final String S = "s";
  private static final String P = "p";
  private static final String PHONES = "phones";
  private static final String WORDS = "words";
  private static final String W = "w";
  private final ServerProperties props;

  /**
   * @param properties
   * @see PhoneDAO#PhoneDAO(Database)
   */
  public ParseResultJson(ServerProperties properties) {
    this.props = properties;
  }

  /**
   * @param typeToEvent
   * @return
   * @see ASRScoring#getTypeToEndTimes
   * @see #readFromJSON(String)
   * @see #parseJsonAndGetProns
   */
  private Map<NetPronImageType, List<TranscriptSegment>> getNetPronImageTypeToEndTimes(
      Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<TranscriptSegment> endTimes = typeToEndTimes.computeIfAbsent(key, k -> new ArrayList<>());
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        endTimes.add(new TranscriptSegment(value.start, value.end, value.event, value.score));
      }
    }

    return typeToEndTimes;
  }

  private Map<NetPronImageType, List<SlimSegment>> slimGetNetPronImageTypeToEndTimes(
      Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent) {
    Map<NetPronImageType, List<SlimSegment>> typeToEndTimes = new HashMap<>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<SlimSegment> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) {
        typeToEndTimes.put(key, endTimes = new ArrayList<>());
      }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        endTimes.add(new SlimSegment(value.event, value.score));
      }
    }

    return typeToEndTimes;
  }

 private  int warn = 0;

  /**
   * @param json
   * @param usePhones
   * @return
   * @see #readFromJSON(String)
   */
  private Map<ImageType, Map<Float, TranscriptEvent>> parseJsonString(String json,
                                                                      boolean usePhones,
                                                                      Map<String, List<List<String>>> wordToPronunciations) {
    if (json.isEmpty()) {
      //logger.warn("json is empty");
      return Collections.emptyMap();
    }

    //   else {
//      logger.warn("json = " + json);
    // }
    JsonParser parser = new JsonParser();
    Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = Collections.emptyMap();

    if (!json.equals("null")) {  // not exactly sure how this can happen...
      try {
        JsonElement parse1 = parser.parse(json);
        JsonObject parse = parse1.getAsJsonObject();
        imageTypeMapMap = readFromJSON(parse, WORDS, W, usePhones, wordToPronunciations);
      } catch (Exception e) {
        logger.error("Couldn't parse" +
            "\n\tjson " + json +
            "\n\tgot   " + e, e);
      }
    }

    if (imageTypeMapMap.isEmpty()) {
      logger.warn("parseJsonString json '" + json + "' produced empty events map " + wordToPronunciations);
    } else if (imageTypeMapMap.get(ImageType.WORD_TRANSCRIPT).isEmpty()) {
      if (warn++ < 2) logger.warn("parseJsonString no words for " + json);
      // throw new Exception();
    }
    return imageTypeMapMap;
  }

  private final Map<NetPronImageType, List<TranscriptSegment>> emptyMap = new HashMap<>();


  /**
   * @param json
   * @return
   * @see PhoneDAO#getPhoneReport
   * @see mitll.langtest.server.database.userexercise.ExerciseToPhone#getExerciseToPhoneForProject
   * @see mitll.langtest.server.database.analysis.Analysis#getWordScore
   */
  public Map<NetPronImageType, List<TranscriptSegment>> readFromJSON(String json) {
    if (json.isEmpty()) {
      //logger.warn("json is empty?");
      return emptyMap;
    } else if (json.equals("{}") || json.equals("null")) {
     // logger.warn("json is " + json);
      return emptyMap;
    } else {
      return getNetPronImageTypeToEndTimes(parseJsonString(json, false, null));
    }
  }

  public Map<NetPronImageType, List<SlimSegment>> slimReadFromJSON(String json) {
    if (json.isEmpty()) {
      //logger.warn("json is empty?");
      return Collections.emptyMap();
    } else if (json.equals("{}") || json.equals("null")) {
      // logger.warn("json is " + json);
      return Collections.emptyMap();
    } else {
      return slimGetNetPronImageTypeToEndTimes(parseJsonString(json, false, null));
    }
  }


  /**
   * @param json
   * @param wordToPronunciations
   * @return
   * @see mitll.langtest.server.database.userexercise.ExerciseToPhone#getExToPhonePerProject
   */
  public Map<NetPronImageType, List<TranscriptSegment>> parseJsonAndGetProns(String json,
                                                                             Map<String, List<List<String>>> wordToPronunciations) {
    return getNetPronImageTypeToEndTimes(parseJsonString(json, false, wordToPronunciations));
  }

  /**
   * uses the parsed json to get transcript info
   *
   * @param jsonObject
   * @param words1
   * @param w1
   * @see #parseJsonString
   * @see PrecalcScores#getCachedScores
   * @see Scoring#getTypeToTranscriptEvents
   */
  public Map<ImageType, Map<Float, TranscriptEvent>> readFromJSON(JsonObject jsonObject,
                                                           String words1,
                                                           String w1,
                                                           boolean usePhones,
                                                           Map<String, List<List<String>>> wordToPronunciations) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<ImageType, Map<Float, TranscriptEvent>>();
    SortedMap<Float, TranscriptEvent> wordEvents = new TreeMap<Float, TranscriptEvent>();
    SortedMap<Float, TranscriptEvent> phoneEvents = new TreeMap<Float, TranscriptEvent>();

    typeToEvent.put(ImageType.WORD_TRANSCRIPT, wordEvents);
    typeToEvent.put(ImageType.PHONE_TRANSCRIPT, phoneEvents);

    if (jsonObject.has(words1)) {
      try {
        JsonArray words = jsonObject.getAsJsonArray(words1);
        for (int i = 0; i < words.size(); i++) {
          JsonObject word = words.get(i).getAsJsonObject();
          String wordToken = objectToEvent(wordEvents, w1, word, false);
          List<String> phones1 = getPhones(phoneEvents, word.getAsJsonArray(PHONES), usePhones);

          if (wordToPronunciations != null) {
            if (!phones1.isEmpty()) {
              List<List<String>> lists = wordToPronunciations.get(wordToken);
              if (lists == null) {
                wordToPronunciations.put(wordToken, lists = new ArrayList<List<String>>());
              }
              lists.add(phones1);
              //  logger.info("Adding " + wordToken + " -> " + phones1);
            }
          }
        }
      } catch (Exception e) {
        logger.debug("no json array at '" + words1 + "' in " + jsonObject, e);
      }
    } else {
      logger.warn("skipping '" + words1 + "' '" + w1);// + " has " + jsonObject.());
    }

    return typeToEvent;
  }

  private List<String> getPhones(SortedMap<Float, TranscriptEvent> phoneEvents, JsonArray phones, boolean usePhone) {
    return getEventsFromJson(phoneEvents, phones, P, usePhone);
  }

  private List<String> getEventsFromJson(SortedMap<Float, TranscriptEvent> phoneEvents,
                                         JsonArray phones,
                                         String tokenKey,
                                         boolean usePhone) {
    List<String> phonesForWord = new ArrayList<>();
    for (int j = 0; j < phones.size(); j++) {
      JsonObject phone = phones.get(j).getAsJsonObject();
      String phoneToken = objectToEvent(phoneEvents, tokenKey, phone, usePhone);
      phonesForWord.add(phoneToken);
    }
    return phonesForWord;
  }

  private String objectToEvent(SortedMap<Float, TranscriptEvent> phoneEvents,
                               String tokenKey,
                               JsonObject phone,
                               boolean usePhone) {
    JsonElement jsonElement = phone.get(tokenKey);
    String token = (jsonElement.isJsonPrimitive()) ? jsonElement.getAsString() : "word";
    double pscore = phone.get(S).getAsDouble();
    double pstart = phone.has(STR) ? phone.get(STR).getAsDouble() : 0d;
    double pend = phone.has(END) ? phone.get(END).getAsDouble() : 0d;
    if (usePhone) {
      token = props.getDisplayPhoneme(token);
    }

    phoneEvents.put((float) pstart, new TranscriptEvent((float) pstart, (float) pend, token, (float) pscore));
    return token;

  }
}
