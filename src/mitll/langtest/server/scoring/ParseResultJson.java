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
import audio.imagewriter.EventAndFileInfo;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.phone.PhoneDAO;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/28/15.
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
   * @see PhoneDAO#PhoneDAO(Database)
   */
  public ParseResultJson(ServerProperties properties) {
    this.props = properties;
  }

  /**
   * @param typeToEvent
   * @return
   * @see ASRScoring#getTypeToEndTimes(EventAndFileInfo)
   */
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

  int warn = 0;

  /**
   * @param json
   * @param usePhones
   * @return
   * @see #parseJson(String)
   */
  private Map<ImageType, Map<Float, TranscriptEvent>> parseJsonString(String json, boolean usePhones) {
    if (json.isEmpty()) throw new IllegalArgumentException("json is empty");
    //   else {
//      logger.warn("json = " + json);
    // }
    Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap =
        parseJson(JSONObject.fromObject(json), "words", "w", usePhones);

    if (imageTypeMapMap.isEmpty()) logger.warn("json " + json + " produced empty events map");
    else if (imageTypeMapMap.get(ImageType.WORD_TRANSCRIPT).isEmpty()) {
      if (warn++ < 10) logger.warn("no words for " + json);
      // throw new Exception();
    }
    return imageTypeMapMap;
  }

  /**
   * @param json
   * @return
   * @see PhoneDAO#getPhoneReport(String, Map, boolean)
   * @see DatabaseImpl#putBackWordAndPhone()
   * @see mitll.langtest.server.database.analysis.Analysis#getWordScore(List)
   */
  public Map<NetPronImageType, List<TranscriptSegment>> parseJson(String json) {
    if (json.isEmpty()) {
      logger.warn("json is empty?");
    }
    if (json.equals("{}")) {
      logger.warn("json is " + json);
    }
    return getNetPronImageTypeToEndTimes(parseJsonString(json, false));
  }

  /**
   * uses the parsed json to get transcript info
   *
   * @param jsonObject
   * @param words1
   * @param w1
   * @see #parseJsonString(String, boolean)
   * @see PrecalcScores#getCachedScores
   * @see Scoring#getTypeToTranscriptEvents(JSONObject, boolean)
   */
  Map<ImageType, Map<Float, TranscriptEvent>> parseJson(JSONObject jsonObject, String words1, String w1, boolean usePhones) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<ImageType, Map<Float, TranscriptEvent>>();
    SortedMap<Float, TranscriptEvent> wordEvents = new TreeMap<Float, TranscriptEvent>();
    SortedMap<Float, TranscriptEvent> phoneEvents = new TreeMap<Float, TranscriptEvent>();

    typeToEvent.put(ImageType.WORD_TRANSCRIPT, wordEvents);
    typeToEvent.put(ImageType.PHONE_TRANSCRIPT, phoneEvents);

    // boolean valid = true;
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
    } else {
      logger.warn("skipping " + words1 + " " + w1 + " has " + jsonObject.keySet());
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
    String token = phone.getString(tokenKey);
    double pscore = phone.getDouble(S);
    double pstart = phone.containsKey(STR) ? phone.getDouble(STR) : 0d;
    double pend = phone.containsKey(END) ? phone.getDouble(END) : 0d;
    if (usePhone) {
      token = props.getDisplayPhoneme(token);
    }

    phoneEvents.put((float) pstart, new TranscriptEvent((float) pstart, (float) pend, token, (float) pscore));
  }
}
