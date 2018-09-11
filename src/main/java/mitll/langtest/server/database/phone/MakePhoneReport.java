package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.analysis.PhoneAnalysis;
import mitll.langtest.shared.analysis.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static mitll.langtest.server.database.phone.SlickPhoneDAO.UNDERSCORE;

/**
 * Created by go22670 on 3/29/16.
 */
public class MakePhoneReport {
  private static final Logger logger = LogManager.getLogger(MakePhoneReport.class);

  private static final boolean DEBUG = false;

  /**
   * @param phoneToScores
   * @param phoneToBigramToWS
   * @param phoneToBigramToScore
   * @param totalScore
   * @param totalItems
   * @return
   * @see SlickPhoneDAO#getPhoneReport
   */
  public PhoneReport getPhoneReport(Map<String, List<PhoneAndScore>> phoneToScores,
                                    Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS,
//                                    Map<String, Float> bigramToCount,
//                                    Map<String, Float> bigramToScore,

                                    Map<String, Map<String, Bigram>> phoneToBigramToScore,
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

    Set<String> allPhones = phoneToAvg.keySet();
    List<String> sorted = new ArrayList<>(allPhones);

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

    sortExamples(phoneToBigramToWS);

    if (DEBUG) {
      logger.info("getPhoneReport phone->words " + phoneToBigramToWS.size() + " : " + phoneToBigramToWS.keySet());
    }

    Map<String, List<Bigram>> phoneToBigram = getPhoneToBigrams(phoneToBigramToScore);//phoneToBigramToWS, bigramToCount, bigramToScore);

    if (DEBUG) {
      phoneToBigram.forEach((k, v) -> logger.info("phoneToBigram " + k + "->" + v));
    }
    maybeRemoveUnderscore(phoneToBigram);

    // bigramToCount.forEach((k, v) -> phoneToBigram.put(k));
    return new PhoneReport(percentOverall,
        phoneToBigram,
        phoneToAvgSorted,
        phoneToBigramToWS
    );
  }

  private void maybeRemoveUnderscore(Map<String, List<Bigram>> phoneToBigram) {
    phoneToBigram.forEach((k, v) -> {
      v.sort((o1, o2) -> Float.compare(o1.getScore(), o2.getScore()));
      logger.info("getPhoneReport " + k + " has " + v.size() + " bigrams : " + v);
      if (v.get(0).getBigram().startsWith(UNDERSCORE)) {
        if (v.size() > 1) {
          v.remove(0);
          logger.info("\tgetPhoneReport REMOVE : " + k + " has " + v.size() + " bigrams : " + v);
        }
      }
    });
  }

  @NotNull
  private Map<String, List<Bigram>> getPhoneToBigrams(
                                                      Map<String, Map<String, Bigram>> phoneToBigramToScore) {
    Map<String, List<Bigram>> phoneToBigram = new HashMap<>(phoneToBigramToScore.size());

  //  logger.info("phones? " + phoneToBigramToScore.keySet());

    phoneToBigramToScore.keySet().forEach(phone -> phoneToBigram.put(phone, new ArrayList<>()));

    phoneToBigramToScore.forEach((k, v) -> {
      List<Bigram> bigramsForPhone = phoneToBigram.get(k);
      bigramsForPhone.addAll(v.values());
//      bigramsForPhone.sort(Bigram::compareTo);
    });

    phoneToBigram.forEach((k, v) -> logger.info("after " + k + "->" + v));
/*
    phoneToBigram.values().forEach(bigrams -> bigrams.sort(Bigram::compareTo));

    phoneToBigram.forEach((k, v) -> logger.info("after 2 " + k + "->" + v));*/

/*

    logger.info("getPhoneReport found " + phoneToBigramToWS.keySet().size() + " phones : " + phoneToBigramToWS.keySet());
    phoneToBigramToWS.forEach((phone, bigramToExamples) -> {
      List<Bigram> bigrams = phoneToBigram.computeIfAbsent(phone, k -> new ArrayList<>());
      Set<String> bigramsForPhone = bigramToExamples.keySet();
      bigramsForPhone.forEach(bigram -> {
        bigrams.add(getClienBigram(bigramToCount, bigramToScore, bigram));
      });
    });
*/

    return phoneToBigram;
  }
/*

  @NotNull
  private Bigram getClienBigram(Map<String, Float> bigramToCount, Map<String, Float> bigramToScore, String bigram) {
    Float score = bigramToScore.get(bigram);
    Float countForBigram = bigramToCount.get(bigram);
    float score1 = score / countForBigram;
    if (bigram.startsWith("nj-e")) {
      logger.info("getPhoneReport bigram " + bigram + " score " + score + " count " + countForBigram + " score " + score1);
    }
    return new Bigram(bigram, countForBigram.intValue(), score1);
  }
*/

/*
  @NotNull
  private void getPhoneToWordAndScore(Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS,
                                      List<String> sorted) {
    //Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWSSorted = new LinkedHashMap<>();

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
    // return phoneToBigramToWSSorted;
  }
*/

  private void sortExamples(Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS) {
    phoneToBigramToWS
        .values()
        .forEach(wordAndScores -> wordAndScores
            .values()
            .forEach(scores -> scores
                .sort(WordAndScore::compareTo)));
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
