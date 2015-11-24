/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by go22670 on 9/28/15.
 */
public class ParseResultJson {
  private static final Logger logger = Logger.getLogger(ParseResultJson.class);
  private static final String STR = "str";
  private static final String END = "end";
  private static final String S = "s";
  private static final String P = "p";
  private static final String PHONES = "phones";
  private final ServerProperties props;

  /**
   * @param properties
   * @see mitll.langtest.server.database.PhoneDAO#PhoneDAO(Database)
   */
  public ParseResultJson(ServerProperties properties) {
    this.props = properties;
  }

  public Map<NetPronImageType, List<TranscriptSegment>> getNetPronImageTypeToEndTimes(Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<TranscriptSegment> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) {
        typeToEndTimes.put(key, endTimes = new ArrayList<TranscriptSegment>());
      }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        endTimes.add(new TranscriptSegment(value.start, value.end, value.event, value.score));
      }
    }

    return typeToEndTimes;
  }

  /**
   * @see #parseJson(String)
   * @param json
   * @param usePhones
   * @return
   */
  private Map<ImageType, Map<Float, TranscriptEvent>> parseJsonString(String json, boolean usePhones) {
    if (json.isEmpty()) throw new IllegalArgumentException("json is empty");
 //   else {
//      logger.warn("json = " + json);
   // }
    Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = parseJson(JSONObject.fromObject(json), "words", "w", usePhones);

    if (imageTypeMapMap.isEmpty()) logger.warn("json " + json + " produced empty events map");
    else if (imageTypeMapMap.get(ImageType.WORD_TRANSCRIPT).isEmpty()) {
      logger.warn("no words for " +json);
     // throw new Exception();
    }
    return imageTypeMapMap;
  }

  /**
   * @param json
   * @return
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneReport(String, Map, boolean)
   * @see DatabaseImpl#putBackWordAndPhone()
   * @see mitll.langtest.server.database.analysis.Analysis#getWordScore(List)
   */
  public Map<NetPronImageType, List<TranscriptSegment>> parseJson(String json) {
    if (json.isEmpty()) {
      logger.warn("json is empty?");
    }
    if (json.equals("{}")) {
      logger.warn("json is " +json);
    }
    return getNetPronImageTypeToEndTimes(parseJsonString(json, false));
  }

  /**
   * TODOx : actually use the parsed json to get transcript info
   *
   * @param jsonObject
   * @param words1
   * @param w1
   * @paramx eventScores
   * @see #parseJsonString(String, boolean)
   * @see ASRScoring#getCachedScores
   * @see #writeTranscripts(String, int, int, String, boolean, String, String, boolean, boolean, boolean)
   */
  Map<ImageType, Map<Float, TranscriptEvent>> parseJson(JSONObject jsonObject, String words1, String w1, boolean usePhones) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<ImageType, Map<Float, TranscriptEvent>>();
    SortedMap<Float, TranscriptEvent> wordEvents = new TreeMap<Float, TranscriptEvent>();
    SortedMap<Float, TranscriptEvent> phoneEvents = new TreeMap<Float, TranscriptEvent>();

    typeToEvent.put(ImageType.WORD_TRANSCRIPT,  wordEvents);
    typeToEvent.put(ImageType.PHONE_TRANSCRIPT, phoneEvents);

    boolean valid = true;
    if (jsonObject.containsKey(words1)) {
      try {
        JSONArray words = jsonObject.getJSONArray(words1);
        for (int i = 0; i < words.size() /*&& valid*/; i++) {
          JSONObject word = words.getJSONObject(i);
          //if (word.containsKey(STR)) {
            objectToEvent(wordEvents, w1, word, false);
            JSONArray phones = word.getJSONArray(PHONES);
            getPhones(phoneEvents, phones, usePhones);
         // } else {
         //   valid = false;
         // }
        }
      } catch (Exception e) {
        logger.debug("no json array at " + words1 + " in " + jsonObject, e);
      }
    }
    else {
      logger.warn("skipping " + words1 + " " + w1 + " has " +jsonObject.keySet());
    }

   // return valid ? typeToEvent : new HashMap<>();
    return typeToEvent;
  }

  private void getPhones(SortedMap<Float, TranscriptEvent> phoneEvents, JSONArray phones, boolean usePhone) {
    getEventsFromJson(phoneEvents, phones, P, usePhone);
  }

  private void getEventsFromJson(SortedMap<Float, TranscriptEvent> phoneEvents, JSONArray phones, String tokenKey, boolean usePhone) {
    for (int j = 0; j < phones.size(); j++) {
      JSONObject phone = phones.getJSONObject(j);
      objectToEvent(phoneEvents, tokenKey, phone, usePhone);
    }
  }

  private void objectToEvent(SortedMap<Float, TranscriptEvent> phoneEvents, String tokenKey, JSONObject phone,
                             boolean usePhone) {
    String token  = phone.getString(tokenKey);
    double pscore = phone.getDouble(S);
    double pstart = phone.containsKey(STR) ? phone.getDouble(STR) : 0d;
    double pend   = phone.containsKey(END) ? phone.getDouble(END) : 0d;
    if (usePhone) {
      token = props.getDisplayPhoneme(token);
    }

    phoneEvents.put((float) pstart, new TranscriptEvent((float) pstart, (float) pend, token, (float) pscore));
  }
}
