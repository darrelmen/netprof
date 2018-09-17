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
import org.apache.poi.ss.formula.functions.T;

import java.util.*;

import static mitll.langtest.server.scoring.PronunciationLookup.SIL;

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
  private final String language;
  private Map<String, String> phoneToDisplay;

  /**
   * @param properties
   * @param language
   * @see PhoneDAO#PhoneDAO(Database)
   */
  public ParseResultJson(ServerProperties properties, String language) {
    this.props = properties;
    this.language = language;
    phoneToDisplay = props.getPhoneToDisplay(language);
  }

  /**
   * @param typeToEvent
   * @return
   * @seex xASRScoring#getTypeToEndTimes
   * @see #readFromJSON(String)
   * @see #parseJsonAndGetProns
   */
  private Map<NetPronImageType, List<TranscriptSegment>> getNetPronImageTypeToEndTimes(
      Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent) {

    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      boolean isPhone = key == NetPronImageType.PHONE_TRANSCRIPT;
      List<TranscriptSegment> endTimes = typeToEndTimes.computeIfAbsent(key, k -> new ArrayList<>());


      StringBuilder builder = new StringBuilder();
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        String event1 = value.getEvent();

        String displayName = isPhone ? getDisplayName(event1) : event1;
        endTimes.add(new TranscriptSegment(value.getStart(), value.getEnd(), event1, value.getScore(), displayName, builder.length()));
        if (!isPhone) builder.append(event1);
      }
    }

    return typeToEndTimes;
  }

  private String getDisplayName(String event) {
    String displayName = phoneToDisplay.get(event);
    displayName = displayName == null ? event : displayName;
    return displayName;
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
        endTimes.add(new SlimSegment(value.getEvent(), value.getScore()));
      }
    }

    return typeToEndTimes;
  }

  private int warn = 0;

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
        imageTypeMapMap = readFromJSON(parse, WORDS, W, usePhones, wordToPronunciations, false);
      } catch (Exception e) {
        logger.error("Couldn't parse" +
            "\n\tjson " + json +
            "\n\tgot   " + e, e);
      }
    }

    if (imageTypeMapMap.isEmpty()) {
      logger.warn("parseJsonString json '" + json + "' produced empty events map " + wordToPronunciations);
    } else if (imageTypeMapMap.get(ImageType.WORD_TRANSCRIPT).isEmpty()) {
      // if (warn++ < 2) logger.warn("parseJsonString no words for " + json);
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
                                                                  Map<String, List<List<String>>> wordToPronunciations,
                                                                  boolean useKaldi) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<>();
    SortedMap<Float, TranscriptEvent> wordEvents = new TreeMap<>();
    SortedMap<Float, TranscriptEvent> phoneEvents = new TreeMap<>();
    List<TranscriptEvent> allwsubs = new ArrayList<>();

    typeToEvent.put(ImageType.WORD_TRANSCRIPT, wordEvents);
    typeToEvent.put(ImageType.PHONE_TRANSCRIPT, phoneEvents);
    String phones = useKaldi ? "phone_align" : PHONES;

    if (jsonObject.has(words1)) {
      try {
        JsonArray words = jsonObject.getAsJsonArray(words1);
        int size = words.size();
        logger.info("readFromJSON under " + words1 + " and " + w1 + " found " + size);
        for (int i = 0; i < size; i++) {
          JsonObject word = words.get(i).getAsJsonObject();
          List<TranscriptEvent> wsubs = new ArrayList<>();
          String wordToken = objectToEvent(wordEvents, w1, word, false, useKaldi, wsubs);
          allwsubs.addAll(wsubs);
//          logger.info("\treadFromJSON wordToken " + wordToken);
//          logger.info("\treadFromJSON wsubs     " + wsubs);
          List<TranscriptEvent> psubs = new ArrayList<>();
          List<String> phones1 = getPhones(phoneEvents,
              word.getAsJsonArray(phones), usePhones, psubs,
              useKaldi ? "phone" : P, useKaldi);

          if (!psubs.isEmpty()) {
            TranscriptEvent phoneEvent = psubs.get(psubs.size() - 1);
            float end = phoneEvent.getEnd();
            TranscriptEvent wordEvent = wsubs.get(wsubs.size() - 1);
            TranscriptEvent firstPhone = psubs.get(0);
//            logger.info("for " + wordToken + " wordEvent " + wordEvent);
//            logger.info("for " + wordToken + " 1 phone " + firstPhone);
//            logger.info("for " + wordToken + " last phone " + phoneEvent);
            wordEvent.setStart(firstPhone.getStart());
            wordEvent.setEnd(end);
            wordEvents.put(firstPhone.getStart(), wordEvent);

//            logger.info("word event keys " + wordEvents.keySet());
//            logger.info("word event values " + wordEvents.values());
//            wordEvents.forEach((k, v) -> {
//              logger.info(k + " = " + v);
//            });
          }

          if (wordToPronunciations != null) {
            if (!phones1.isEmpty()) {
              List<List<String>> lists = wordToPronunciations.computeIfAbsent(wordToken, k -> new ArrayList<List<String>>());
              lists.add(phones1);
              //  logger.info("Adding " + wordToken + " -> " + phones1);
            }
          }
        }
      } catch (Exception e) {
        logger.debug("no json array at '" + words1 + "' in " + jsonObject, e);
      }

    } else {
      logger.warn("readFromJSON skipping '" + words1 + "' '" + w1 + "'");// + " has " + jsonObject.());

    }
    if (useKaldi) {
      Collection<TranscriptEvent> values = allwsubs;
      TreeMap<Float, TranscriptEvent> wordEvents2 = new TreeMap<>();
      values.forEach(transcriptEvent -> wordEvents2.put(transcriptEvent.getStart(), transcriptEvent));
      typeToEvent.put(ImageType.WORD_TRANSCRIPT, wordEvents2);

      logger.info("2 word event keys " + wordEvents2.keySet());
      logger.info("2 word event values " + wordEvents2.values());
      wordEvents2.forEach((k, v) -> {
        logger.info(k + " = " + v);
      });
    }
    return typeToEvent;
  }

  private List<String> getPhones(SortedMap<Float, TranscriptEvent> phoneEvents,
                                 JsonArray phones,
                                 boolean usePhone,
                                 List<TranscriptEvent> subs, String phoneToken,
                                 boolean useKaldi) {

    return getEventsFromJson(phoneEvents, phones, phoneToken, usePhone, subs, useKaldi);
  }

  private List<String> getEventsFromJson(SortedMap<Float, TranscriptEvent> phoneEvents,
                                         JsonArray phones,
                                         String tokenKey,
                                         boolean usePhone,
                                         List<TranscriptEvent> subs,
                                         boolean useKaldi) {
    List<String> phonesForWord = new ArrayList<>();
    for (int j = 0; j < phones.size(); j++) {
      JsonObject phone = phones.get(j).getAsJsonObject();
      String phoneToken = objectToEvent(phoneEvents, tokenKey, phone, usePhone, useKaldi, subs);
      phonesForWord.add(phoneToken);
    }
    return phonesForWord;
  }

  private String objectToEvent(SortedMap<Float, TranscriptEvent> phoneEvents,
                               String tokenKey,
                               JsonObject phone,
                               boolean usePhone,
                               boolean useKaldi,
                               List<TranscriptEvent> subs) {
    JsonElement jsonElement = phone.get(tokenKey);
    String token = (jsonElement.isJsonPrimitive()) ? jsonElement.getAsString() : "word";

    if (token.equalsIgnoreCase("<eps>")) token = SIL;

    double pscore = phone.get(useKaldi ? "score" : S).getAsDouble();
    String str = useKaldi ? "start_time" : STR;
    double pstart = phone.has(str) ? phone.get(str).getAsDouble() : 0d;
    double pend;
    if (useKaldi) {
      double v = phone.has("duration") ? phone.get("duration").getAsDouble() : 0d;
      pend = pstart + v;
    } else {
      pend = phone.has(END) ? phone.get(END).getAsDouble() : 0d;
    }

    if (usePhone) {
      token = props.getDisplayPhoneme(language, token);
    }

    TranscriptEvent value = new TranscriptEvent((float) pstart, (float) pend, token, (float) pscore);
    phoneEvents.put((float) pstart, value);
    subs.add(value);
    return token;
  }
}
