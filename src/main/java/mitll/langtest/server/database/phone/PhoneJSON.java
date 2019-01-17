package mitll.langtest.server.database.phone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordAndScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by go22670 on 3/29/16.
 */
public class PhoneJSON {
  private static final Logger logger = LogManager.getLogger(PhoneJSON.class);

  private static final int MAX_EXAMPLES = 30;

  private static final String WID = "wid";

  private static final boolean DEBUG = false;

  /**
   * Note these are referenced in the iOS app - change them and you'll break it
   */
  private static final String PHONES = "phones";
  private static final String ORDER = "order";
  private static final String ANSWER = "answer";
  private static final String REF = "ref";
  private static final String RESULT = "result";
  private static final String RESULTS = "results";
  private static final String PHONE_SCORE = "phoneScore";
  private static final String SEQ = "seq";
  private static final String W = "w";
  private static final String S = "s";
  private static final String RESULT1 = "result";

  /**
   * NOTE: JSON is read by iOS - EAFPhoneScoreTableViewController.useJsonChapterData
   *
   * TODO: sort phones by average score of the latest unique item - if you say pijama 10 times, don't return it as
   * an example 10 times, and only have the last one contribute to the average score for the phone.
   *
   * @param worstPhonesAndScore
   * @return
   * @see SlickPhoneDAO#getWorstPhonesJson(int, java.util.Collection, String, mitll.langtest.server.database.exercise.Project)
   */
  JsonObject getWorstPhonesJson(PhoneReport worstPhonesAndScore) {
    JsonObject jsonObject = new JsonObject();

    if (worstPhonesAndScore != null) {
      Map<String, Map<String, List<WordAndScore>>> phoneToWordAndScoreSorted =
          worstPhonesAndScore.getPhoneToWordAndScoreSorted();

      Map<String, List<WordAndScore>> worstPhones = new HashMap<>();
      phoneToWordAndScoreSorted.forEach((k, v) -> {
        //Collection<List<WordAndScore>> values = v.values();
        List<WordAndScore> wordAndScores = worstPhones.computeIfAbsent(k, k1 -> new ArrayList<>());

        for (List<WordAndScore> value : v.values()) {
          wordAndScores.addAll(value);
        }
//        wordAndScores.addAll(values);
      });

      ///Map<String, List<WordAndScore>> worstPhones = (Map<String, List<WordAndScore>>) phoneToWordAndScoreSorted;
      Map<Long, String> resToAnswer = new HashMap<>();
      Map<Long, String> resToRef = new HashMap<>();
      Map<Long, String> resToResult = new HashMap<>();
      if (DEBUG) logger.debug("worstPhones phones are " + worstPhones.keySet());

      {
        JsonObject phones = new JsonObject();

        for (Map.Entry<String, List<WordAndScore>> pair : worstPhones.entrySet()) {
          phones.add(pair.getKey(), getWordsJsonArray(resToAnswer, resToRef, resToResult, pair.getValue()));
        }

        jsonObject.add(PHONES, phones);
      }

      {
        JsonArray order = new JsonArray();
        worstPhones.keySet().forEach(order::add);

        jsonObject.add(ORDER, order);

        if (DEBUG) logger.debug("order phones are " + order);
      }

      {
        JsonObject results = new JsonObject();
        for (Map.Entry<Long, String> pair : resToAnswer.entrySet()) {
          JsonObject result = new JsonObject();

          Long resultID = pair.getKey();
          result.addProperty(ANSWER, pair.getValue());
          result.addProperty(REF, resToRef.get(resultID));

          {
            String value = resToResult.get(resultID);
            JsonParser parser = new JsonParser();
            result.add(RESULT, parser.parse(value).getAsJsonObject());
          }

          results.add(Long.toString(resultID), result);
        }
        jsonObject.add(RESULTS, results);
      }
      jsonObject.addProperty(PHONE_SCORE, Integer.toString(worstPhonesAndScore.getOverallPercent())); // TODO : not sure where this is used
    }

    return jsonObject;
  }

  /**
   * for iPhone/iPad
   *
   * LIMITED TO 30 examples!
   * {@link #MAX_EXAMPLES}
   *
   * @param resToAnswer
   * @param resToRef
   * @param resToResult
   * @param value
   * @return
   * @see #getWorstPhonesJson
   */
  private JsonArray getWordsJsonArray(Map<Long, String> resToAnswer,
                                      Map<Long, String> resToRef,
                                      Map<Long, String> resToResult,
                                      List<WordAndScore> value) {
    JsonArray words = new JsonArray();

    int count = 0;
    for (WordAndScore wordAndScore : value) {
      words.add(getJsonForWord(wordAndScore));

      long resultID = wordAndScore.getResultID();
      resToAnswer.put(resultID, wordAndScore.getAnswerAudio());
      resToRef.put(resultID, wordAndScore.getRefAudio());
      resToResult.put(resultID, wordAndScore.getScoreJson());

      if (count++ > MAX_EXAMPLES) {
        break;
      }
    }
    return words;
  }

  /**
   * @param wordAndScore
   * @return
   * @see #getWordsJsonArray(Map, Map, Map, List)
   */
  private JsonObject getJsonForWord(WordAndScore wordAndScore) {
    JsonObject word = new JsonObject();
    word.addProperty(WID, Integer.toString(wordAndScore.getWseq()));
    word.addProperty(SEQ, Integer.toString(wordAndScore.getSeq()));
    word.addProperty(W, wordAndScore.getWord());
    word.addProperty(S, Float.toString(round(wordAndScore.getPronScore())));
    word.addProperty(RESULT1, Long.toString(wordAndScore.getResultID()));
    return word;
  }

  private static float round(float d) {
    return round(d, 3);
  }

  public static float round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }
}
