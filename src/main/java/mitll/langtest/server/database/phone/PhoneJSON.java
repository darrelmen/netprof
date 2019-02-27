/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.phone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.PhoneStats;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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
  public static final String ORDER_SCORES = "orderScores";

  private ServerProperties serverProperties;

  PhoneJSON(ServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  /**
   * NOTE: JSON is read by iOS - EAFPhoneScoreTableViewController.useJsonChapterData
   *
   * TODO: sort phones by average score of the latest unique item - if you say pijama 10 times, don't return it as
   * an example 10 times, and only have the last one contribute to the average score for the phone.
   *
   * @param worstPhonesAndScore
   * @return
   * @see IPhoneDAO#getWorstPhonesJson(Collection, mitll.langtest.server.database.exercise.Project, mitll.langtest.shared.analysis.PhoneReportRequest)
   */
  JsonObject getWorstPhonesJson(PhoneReport worstPhonesAndScore) {
    JsonObject jsonObject = new JsonObject();

    if (worstPhonesAndScore != null) {
      Language language = worstPhonesAndScore.getLanguage();
//      Map<String, Map<String, List<WordAndScore>>> phoneToWordAndScoreSorted =
//          worstPhonesAndScore.getPhoneToWordAndScoreSorted();

      Map<String, List<WordAndScore>> worstPhones = worstPhonesAndScore.getPhoneToExamples();
//          getPhoneToExamples(language, phoneToWordAndScoreSorted);

      ///Map<String, List<WordAndScore>> worstPhones = (Map<String, List<WordAndScore>>) phoneToWordAndScoreSorted;
      Map<Long, String> resToAnswer = new HashMap<>();
      Map<Long, String> resToRef = new HashMap<>();

      if (DEBUG) logger.info("getWorstPhonesJson phones are " + worstPhones.keySet());

      {
        JsonObject phones = new JsonObject();

        for (Map.Entry<String, List<WordAndScore>> pair : worstPhones.entrySet()) {
//         logger.info("dump " + pair.getKey() + " = " + pair.getValue().size());
          phones.add(pair.getKey(), getWordsJsonArray(resToAnswer, resToRef, pair.getValue()));
        }

        jsonObject.add(PHONES, phones);
      }

      {
        List<String> sortedPhones = worstPhonesAndScore.getSortedPhones();

        {
          JsonArray order = new JsonArray();
          // worstPhones.keySet().forEach(order::add);
          sortedPhones.forEach(order::add);
          jsonObject.add(ORDER, order);
          if (DEBUG) logger.info("getWorstPhonesJson order phones are " + order);
        }

        {
          JsonArray order = new JsonArray();
          jsonObject.add(ORDER_SCORES, order);
          Map<String, PhoneStats> phoneToAvgSorted = worstPhonesAndScore.getPhoneToAvgSorted();
          sortedPhones.forEach(phone -> {
            PhoneStats phoneStats = phoneToAvgSorted.get(phone);
            if (phoneStats == null) {
              logger.error("getWorstPhonesJson huh? no avg for " + phone);
            } else {
              order.add(phoneStats.getAvg());
            }
          });
        }
      }

      jsonObject.add(RESULTS, addResults(resToAnswer, resToRef, worstPhonesAndScore.getRidToWords(), language));
      jsonObject.addProperty(PHONE_SCORE, Integer.toString(worstPhonesAndScore.getOverallPercent())); // TODO : not sure where this is used
    }

    return jsonObject;
  }

  private JsonObject addResults(Map<Long, String> resToAnswer, Map<Long, String> resToRef,
                                Map<Integer, List<Word>> ridToWords,
                                Language language) {
    JsonObject results = new JsonObject();
    for (Map.Entry<Long, String> pair : resToAnswer.entrySet()) {
      JsonObject result = new JsonObject();

      Long resultID = pair.getKey();
      result.addProperty(ANSWER, pair.getValue());
      result.addProperty(REF, resToRef.get(resultID));
      result.add(RESULT, addResultWordAndPhones(ridToWords, language, resultID));

      results.add(Long.toString(resultID), result);
    }

    return results;
  }

  private JsonObject addResultWordAndPhones(Map<Integer, List<Word>> ridToWords, Language language, Long resultID) {
    JsonObject wordsObject = new JsonObject();
    JsonArray words = new JsonArray();

    wordsObject.add("words", words);
    List<Word> words1 = ridToWords.get(resultID.intValue());
    words1.forEach(word -> {
      JsonObject wordJSON = new JsonObject();
      words.add(wordJSON);
      wordJSON.addProperty("id", word.getSeq());
      wordJSON.addProperty("w", word.getWord());
      wordJSON.addProperty("s", word.getScore());

      JsonArray phones = new JsonArray();
      wordJSON.add("phones", phones);
      word.getPhones().forEach(phone -> {
        JsonObject phoneJSON = new JsonObject();
        phones.add(phoneJSON);
        phoneJSON.addProperty("id", phone.getSeq());

        phoneJSON.addProperty("p", serverProperties.getDisplayPhoneme(language, phone.getPhone()));
        phoneJSON.addProperty("s", phone.getScore());
      });
    });
    return wordsObject;
  }

  @NotNull
  private Map<String, List<WordAndScore>> getPhoneToExamples(Language language,
                                                             Map<String, Map<String, List<WordAndScore>>> phoneToBigramToScoreSorted) {
    Map<String, List<WordAndScore>> worstPhones = new LinkedHashMap<>();
    phoneToBigramToScoreSorted.forEach((k, v) -> {
      if (!k.equalsIgnoreCase("_")) {
        String phone = serverProperties.getDisplayPhoneme(language, k);

        List<WordAndScore> wordAndScores = worstPhones.computeIfAbsent(phone, k1 -> new ArrayList<>());

        logger.info("getPhoneToExamples " + k + " = " + wordAndScores.size());
        for (List<WordAndScore> value : v.values()) {
          wordAndScores.addAll(value);
        }
      }
//        wordAndScores.addAll(values);
    });
    return worstPhones;
  }

  /**
   * for iPhone/iPad
   *
   * LIMITED TO 30 examples!
   * {@link #MAX_EXAMPLES}
   *
   * @param resToAnswer
   * @param resToRef
   * @param value
   * @return
   * @paramx resToResult
   * @see #getWorstPhonesJson
   */
  private JsonArray getWordsJsonArray(Map<Long, String> resToAnswer,
                                      Map<Long, String> resToRef,
                                      //Map<Long, String> resToResult,
                                      List<WordAndScore> value) {
    JsonArray words = new JsonArray();

    int count = 0;
    for (WordAndScore wordAndScore : value) {
      words.add(getJsonForWord(wordAndScore));

      long resultID = wordAndScore.getResultID();
      resToAnswer.put(resultID, wordAndScore.getAnswerAudio());
      resToRef.put(resultID, wordAndScore.getRefAudio());
      //resToResult.put(resultID, wordAndScore.getScoreJson());

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
