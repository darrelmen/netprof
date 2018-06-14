package mitll.langtest.server.database.phone;

import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordAndScore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  JSONObject getWorstPhonesJson(PhoneReport worstPhonesAndScore) {
    JSONObject jsonObject = new JSONObject();

    if (worstPhonesAndScore != null) {
      Map<String, List<WordAndScore>> worstPhones = worstPhonesAndScore.getPhoneToWordAndScoreSorted();
      Map<Long, String> resToAnswer = new HashMap<>();
      Map<Long, String> resToRef = new HashMap<>();
      Map<Long, String> resToResult = new HashMap<>();
      if (DEBUG) logger.debug("worstPhones phones are " + worstPhones.keySet());

      {
        JSONObject phones = new JSONObject();

        for (Map.Entry<String, List<WordAndScore>> pair : worstPhones.entrySet()) {
          phones.put(pair.getKey(), getWordsJsonArray(resToAnswer, resToRef, resToResult, pair.getValue()));
        }

        jsonObject.put(PHONES, phones);
      }

      {
        JSONArray order = new JSONArray();
        order.addAll(worstPhones.keySet());
        jsonObject.put(ORDER, order);

        if (DEBUG) logger.debug("order phones are " + order);
      }

      {
        JSONObject results = new JSONObject();
        for (Map.Entry<Long, String> pair : resToAnswer.entrySet()) {
          JSONObject result = new JSONObject();

          Long key = pair.getKey();
          result.put(ANSWER, pair.getValue());
          result.put(REF,    resToRef.get(key));
          result.put(RESULT, resToResult.get(key));

          results.put(Long.toString(key), result);
        }
        jsonObject.put(RESULTS, results);
      }
      jsonObject.put(PHONE_SCORE, Integer.toString(worstPhonesAndScore.getOverallPercent())); // TODO : not sure where this is used
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
  private JSONArray getWordsJsonArray(Map<Long, String> resToAnswer,
                                      Map<Long, String> resToRef,
                                      Map<Long, String> resToResult,
                                      List<WordAndScore> value) {
    JSONArray words = new JSONArray();

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
   * @see #getWordsJsonArray(Map, Map, Map, List)
   * @param wordAndScore
   * @return
   */
  private JSONObject getJsonForWord(WordAndScore wordAndScore) {
    JSONObject word = new JSONObject();
    word.put(WID, Integer.toString(wordAndScore.getWseq()));
    word.put(SEQ, Integer.toString(wordAndScore.getSeq()));
    word.put(W, wordAndScore.getWord());
    word.put(S, Float.toString(round(wordAndScore.getPronScore())));
    word.put(RESULT1, Long.toString(wordAndScore.getResultID()));
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
