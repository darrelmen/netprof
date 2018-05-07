package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.analysis.PhoneAnalysis;
import mitll.langtest.shared.analysis.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created by go22670 on 3/29/16.
 */
public class MakePhoneReport {
  private static final Logger logger = LogManager.getLogger(MakePhoneReport.class);

  private static final boolean DEBUG = false;

  /**
   * @param phoneToScores
   * @param phoneToBigramToWS
   * @param bigramToCount
   * @param bigramToScore
   * @param totalScore
   * @param totalItems
   * @return
   * @see SlickPhoneDAO#getPhoneReport
   */
  public PhoneReport getPhoneReport(Map<String, List<PhoneAndScore>> phoneToScores,
                                    Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS,
                                    // Map<String, List<WordAndScore>> phoneToWordAndScore,
                                    Map<String, Float> bigramToCount,
                                    Map<String, Float> bigramToScore,
                                    float totalScore,
                                    float totalItems) {
    float overallScore = totalItems > 0 ? totalScore / totalItems : 0;
    int percentOverall = (int) (100f * PhoneJSON.round(overallScore, 2));

    if (DEBUG) {
      logger.info(
          "getPhoneReport : \n\tscore " + overallScore +
              "\n\titems         " + totalItems +
              //   "\n\tuseSessionGran         " + useSessionGran +
              "\n\tpercent       " + percentOverall +
              "\n\tphoneToScores " + phoneToScores.size() +
              "\n\tphones        " + phoneToScores.keySet());
    }

    final Map<String, PhoneStats> phoneToAvg = getPhoneToPhoneStats(phoneToScores);
    if (DEBUG) logger.info("getPhoneReport phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    // set sessions on each phone stats
    //  setSessions(phoneToAvg, useSessionGran);
    new PhoneAnalysis().setSessionsWithPrune(phoneToAvg);

    if (DEBUG && false) logger.info("getPhoneReport phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    List<String> sorted = new ArrayList<>(phoneToAvg.keySet());

    if (DEBUG) logger.info("getPhoneReport before sorted " + sorted);

    //if (sortByLatestExample) {
    //  phoneToWordAndScore = sortPhonesByLatest(phoneToAvg, sorted);
    //} else {
    sortPhonesByAvg(phoneToAvg, sorted);
    //}

    if (DEBUG) logger.info("getPhoneReport sorted " + sorted.size() + " " + sorted);

    Map<String, PhoneStats> phoneToAvgSorted = new LinkedHashMap<>();
    sorted.forEach(phone -> phoneToAvgSorted.put(phone, phoneToAvg.get(phone)));

    if (DEBUG) {
      logger.info("getPhoneReport phoneToAvgSorted " + phoneToAvgSorted.size() + " " + phoneToAvgSorted);
    }

    Map<String, Map<String, List<WordAndScore>>> phoneToWordAndScoreSorted = getPhoneToWordAndScore(phoneToBigramToWS, sorted);

    if (DEBUG) {
      logger.info("getPhoneReport phone->words " + phoneToBigramToWS.size() + " : " + phoneToBigramToWS.keySet());
    }

    Map<String, List<Bigram>> phoneToBigram = new HashMap<>();
    phoneToAvg.keySet().forEach(phone -> {
      Map<String, List<WordAndScore>> bigramToExamples = phoneToBigramToWS.get(phone);
      List<Bigram> bigrams = phoneToBigram.computeIfAbsent(phone, k -> new ArrayList<>());
      bigramToScore.forEach((k, v) -> bigrams.add(
          new Bigram(k, bigramToCount.get(k).intValue(), v / bigramToCount.get(k))));
    });
    // bigramToCount.forEach((k, v) -> phoneToBigram.put(k));
    return new PhoneReport(percentOverall,
        phoneToBigram, phoneToAvgSorted//,
        //   bigramToCount,bigramToScore
    );
  }

  @NotNull
  private Map<String, Map<String, List<WordAndScore>>> getPhoneToWordAndScore(//Map<String, List<WordAndScore>> phoneToWordAndScore,

                                                                              Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS,
                                                                              List<String> sorted) {
    Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWSSorted = new LinkedHashMap<>();

    for (String phone : sorted) {
      Map<String, List<WordAndScore>> bigramToExamples = phoneToBigramToWS.get(phone);

      bigramToExamples.values().forEach(wordAndScores -> wordAndScores.sort(WordAndScore::compareTo));

      // Collections.sort(value);
//      if (DEBUG) {
//        logger.info("getPhoneReport phone->words for " + phone + " : " + value.size());
//        for (WordAndScore wordAndScore : value) {
//          logger.info("getPhoneReport for " + phone + " got " + wordAndScore);
//        }
//      }
//      phoneToBigramToWSSorted.put(phone, value);
    }
    return phoneToBigramToWSSorted;
  }

  /**
   * Compare by score for phone, then by phone name.
   *
   * @param phoneToAvg
   * @param sorted
   * @see #getPhoneReport
   */
  private void sortPhonesByAvg(final Map<String, PhoneStats> phoneToAvg, List<String> sorted) {
    sorted.sort((o1, o2) -> {
      PhoneStats first = phoneToAvg.get(o1);
      PhoneStats second = phoneToAvg.get(o2);
      float current = first.getAvg();
      float current1 = second.getAvg();
      //if (current == current1) {
      //  logger.info("got same " + current + " for " + o1 + " and " + o2);
      //} else {
      // logger.info("\tgot " + current + " for " + o1 + " and " + current1 + " for "+ o2);
      //}
      int i = Float.compare(current, current1);
      return i == 0 ? o1.compareTo(o2) : i;
    });
/*
    for (String phone : sorted) {
      logger.info("phone " + phone + " : " + phoneToAvg.get(phone).getAvg());
    }
*/
  }

  /**
   * TODO : make this work
   * For the iPad, we don't want to return every single example, just the latest one, and the summary score
   * for the phone is just for the latest ones, or else they can seem inconsistent, since the iPad only shows
   * distinct words.
   *
   * @param phoneToAvg
   * @param sortedPhones
   * @return
   */
  private Map<String, List<WordAndScore>> sortPhonesByLatest(final Map<String, PhoneStats> phoneToAvg, List<String> sortedPhones) {
    Map<String, List<WordAndScore>> phoneToMinimal = getPhoneToMinimal(phoneToAvg);

    final Map<String, Float> phoneToScore = new HashMap<>();
    for (Map.Entry<String, List<WordAndScore>> pair : phoneToMinimal.entrySet()) {
      phoneToScore.put(pair.getKey(), getAverage(pair));
    }

    sortedPhones.sort((o1, o2) -> {
      Float current = phoneToScore.get(o1);
      Float current1 = phoneToScore.get(o2);
      int i = current.compareTo(current1);
      return i == 0 ? o1.compareTo(o2) : i;
    });

    if (DEBUG) {
      for (String phone : sortedPhones) {
        logger.info("phone " + phone + " : " + phoneToScore.get(phone) + " " + phoneToMinimal.get(phone));
      }
    }
    return phoneToMinimal;
  }

  private float getAverage(Map.Entry<String, List<WordAndScore>> pair) {
    float total = 0;
    for (WordAndScore example : pair.getValue()) total += example.getPronScore();
    total /= pair.getValue().size();
    return total;
  }

  @NotNull
  private Map<String, List<WordAndScore>> getPhoneToMinimal(Map<String, PhoneStats> phoneToAvg) {
    Map<String, List<WordAndScore>> phoneToMinimal = new HashMap<>();
    for (Map.Entry<String, PhoneStats> pair : phoneToAvg.entrySet()) {
      PhoneStats value = pair.getValue();
      Map<String, WordAndScore> wordToExample = new HashMap<>();
      for (TimeAndScore item : value.getTimeSeries()) {
//        logger.info("got " + item + " at " + new Date(item.getTimestamp()));
        WordAndScore wordAndScore = item.getWordAndScore();
        wordToExample.put(wordAndScore.getWord(), wordAndScore);
      }
      phoneToMinimal.put(pair.getKey(), new ArrayList<>(wordToExample.values()));
    }
    return phoneToMinimal;
  }

  /**
   * @param phoneToScores
   * @return
   * @see #getPhoneReport(Map, Map, Map, Map, float, float)
   */
  private Map<String, PhoneStats> getPhoneToPhoneStats(Map<String, List<PhoneAndScore>> phoneToScores) {
    final Map<String, PhoneStats> phoneToAvg = new HashMap<String, PhoneStats>();

    phoneToScores.forEach((phone, scores) -> {
      scores.sort(Comparator.comparingLong(PhoneAndScore::getTimestamp));
      List<TimeAndScore> phoneTimeSeries = getPhoneTimeSeries(scores);
      phoneToAvg.put(phone, new PhoneStats(phoneTimeSeries.size(), phoneTimeSeries));
    });

    return phoneToAvg;
  }

  /**
   * For now we don't carry forward the session size here...
   *
   * @param phoneAndScores
   * @return
   * @see #getPhoneReport(Map, Map, Map, Map, float, float)
   */
  private List<TimeAndScore> getPhoneTimeSeries(List<PhoneAndScore> phoneAndScores) {
    float total = 0;
    float count = 0;
    List<TimeAndScore> phoneTimeSeries = new ArrayList<>();
    for (PhoneAndScore phoneAndScore : phoneAndScores) {
      float pronScore = phoneAndScore.getPronScore();
      total += pronScore;
      count++;
      float moving = total / count;

      TimeAndScore timeAndScore =
          new TimeAndScore(-1, phoneAndScore.getTimestamp(), pronScore, moving,
              phoneAndScore.getWordAndScore(), phoneAndScore.getSessionStart(), 0);
      phoneTimeSeries.add(timeAndScore);
    }
    return phoneTimeSeries;
  }
}
