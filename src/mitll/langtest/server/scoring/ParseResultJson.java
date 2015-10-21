package mitll.langtest.server.scoring;

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by go22670 on 9/28/15.
 */
public class ParseResultJson {
  private static final Logger logger = Logger.getLogger(ParseResultJson.class);
  private final ServerProperties props;

  public ParseResultJson(ServerProperties properties) {
    this.props = properties;
  }

/*
  public Map<ImageType, Map<Float, TranscriptEvent>> parseJsonString(String json, String words1, String w1, boolean usePhones) {
    Map<ImageType, Map<Float, TranscriptEvent>> imageTypeMapMap = parseJson(JSONObject.fromObject(json), words1, w1, usePhones);
    return imageTypeMapMap;
  }
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

  private Map<ImageType, Map<Float, TranscriptEvent>> parseJsonString(String json, boolean usePhones) {
    return parseJson(JSONObject.fromObject(json), "words", "w", usePhones);
  }

  public  Map<NetPronImageType, List<TranscriptSegment>> parseJson(String json) {
    return getNetPronImageTypeToEndTimes(parseJsonString(json,true));
  }


  /**
   * TODOx : actually use the parsed json to get transcript info
   *
   * @param jsonObject
   * @param words1
   * @param w1
   * @paramx eventScores
   * @see ASRScoring#getCachedScores
   * @see #writeTranscripts(String, int, int, String, boolean, String, String, boolean, boolean, boolean)
   */
  Map<ImageType, Map<Float, TranscriptEvent>> parseJson(JSONObject jsonObject, String words1, String w1, boolean usePhones) {
    Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent = new HashMap<ImageType, Map<Float, TranscriptEvent>>();
    SortedMap<Float, TranscriptEvent> wordEvents = new TreeMap<Float, TranscriptEvent>();
    SortedMap<Float, TranscriptEvent> phoneEvents = new TreeMap<Float, TranscriptEvent>();

    typeToEvent.put(ImageType.WORD_TRANSCRIPT, wordEvents);
    typeToEvent.put(ImageType.PHONE_TRANSCRIPT, phoneEvents);

    boolean valid = true;
    if (jsonObject.containsKey(words1)) {
      try {
        JSONArray words = jsonObject.getJSONArray(words1);
        for (int i = 0; i < words.size() && valid; i++) {
          JSONObject word = words.getJSONObject(i);
          if (word.containsKey("str")) {
            objectToEvent(wordEvents, w1, word, false);
            JSONArray phones = word.getJSONArray("phones");
            getPhones(phoneEvents, phones, usePhones);
          } else {
            valid = false;
          }
        }
      } catch (Exception e) {
        logger.debug("no json array at " + words1 + " in " + jsonObject, e);
      }
    }

    return valid ? typeToEvent : new HashMap<>();
  }

  private void getPhones(SortedMap<Float, TranscriptEvent> phoneEvents, JSONArray phones, boolean usePhone) {
    getEventsFromJson(phoneEvents, phones, "p", usePhone);
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
    double pscore = phone.getDouble("s");
    double pstart = phone.getDouble("str");
    double pend = phone.getDouble("end");
    if (usePhone) token = props.getDisplayPhoneme(token);

    phoneEvents.put((float) pstart, new TranscriptEvent((float) pstart, (float) pend, token, (float) pscore));
  }
}
