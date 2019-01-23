package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.analysis.PhoneAnalysis;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.phone.SlickPhoneDAO.UNDERSCORE;

/**
 * Created by go22670 on 3/29/16.
 */
public class MakePhoneReport {
  private static final Logger logger = LogManager.getLogger(MakePhoneReport.class);

  private static final boolean DEBUG = true;

  /**
   * @param phoneToScores
   * @param phoneToBigramToWS
   * @param phoneToBigramToScore
   * @param totalScore
   * @param totalItems
   * @param language
   * @param ridToWords
   * @return
   * @see SlickPhoneDAO#getPhoneReport
   */
  public PhoneReport getPhoneReport(Map<String, List<PhoneAndScore>> phoneToScores,
                                    Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS,
                                    Map<String, Map<String, Bigram>> phoneToBigramToScore,
                                    float totalScore,
                                    float totalItems,
                                    Language language, Map<Integer, List<Word>> ridToWords) {
    float overallScore = totalItems > 0 ? totalScore / totalItems : 0;
    int percentOverall = (int) (100f * PhoneJSON.round(overallScore, 2));

    if (DEBUG || true) {
      logger.info(
          "getPhoneReport : \n\tscore " + overallScore +
              "\n\titems         " + totalItems +
              "\n\tpercent       " + percentOverall +
              "\n\tphoneToScores " + phoneToScores.size() +
              "\n\tphones        " + phoneToScores.keySet());
    }

    Map<String, PhoneStats> phoneToAvgSorted = getPhoneStats(phoneToScores);

    sortExamples(phoneToBigramToWS);

    if (DEBUG) {
      logger.info("getPhoneReport phone->words " + phoneToBigramToWS.size() + " : " + phoneToBigramToWS.keySet());
    }

    Map<String, List<Bigram>> phoneToBigram = getPhoneToBigramReally(phoneToBigramToScore);

    phoneToAvgSorted.keySet().forEach(p -> logger.info("getPhoneReport sorted " + p));
    List<String> sortedPhones = new ArrayList<>(phoneToAvgSorted.keySet().size());
    sortedPhones.addAll(phoneToAvgSorted.keySet());

    return new PhoneReport(percentOverall,
        phoneToBigram,
        phoneToAvgSorted,
        phoneToBigramToWS,
        sortedPhones,
        language,
        ridToWords);
  }

  @NotNull
  private Map<String, List<Bigram>> getPhoneToBigramReally(Map<String, Map<String, Bigram>> phoneToBigramToScore) {
    Map<String, List<Bigram>> phoneToBigram = getPhoneToBigrams(phoneToBigramToScore);

    if (DEBUG) {
      phoneToBigram.forEach((k, v) -> logger.info("getPhoneToBigramReally phoneToBigram " + k + "->" + v));
    }

    maybeRemoveUnderscore(phoneToBigram);
    return phoneToBigram;
  }

  /**
   * @param phoneToScores
   * @return
   * @see SlickPhoneDAO#getPhoneSummary(Collection)
   */
  public PhoneSummary getPhoneSummary(Map<String, List<PhoneAndScore>> phoneToScores) {
    return new PhoneSummary(getPhoneStats(phoneToScores));
  }

  public PhoneBigrams getPhoneBigrams(Map<String, Map<String, Bigram>> phoneToBigramToScore) {
    return new PhoneBigrams(getPhoneToBigramReally(phoneToBigramToScore));
  }

  @NotNull
  private Map<String, PhoneStats> getPhoneStats(Map<String, List<PhoneAndScore>> phoneToScores) {
    Map<String, PhoneStats> phoneToAvgSorted = new LinkedHashMap<>();

    final Map<String, PhoneStats> phoneToAvg = getPhoneToPhoneStats(phoneToScores);
    if (DEBUG && false) logger.info("getPhoneStats phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    // set sessions on each phone stats
    new PhoneAnalysis().setSessionsWithPrune(phoneToAvg);

    if (DEBUG) logger.info("getPhoneStats phoneToAvg " + phoneToAvg.size());// + " " + phoneToAvg);

    {
      List<String> sorted = new ArrayList<>(phoneToAvg.keySet());

      if (DEBUG) logger.info("getPhoneStats before sorted " + sorted);

      sortPhonesByAvg(phoneToAvg, sorted);

      if (DEBUG) logger.info("getPhoneStats sorted " + sorted.size() + " " + sorted);

      sorted.forEach(phone -> phoneToAvgSorted.put(phone, phoneToAvg.get(phone)));

      phoneToAvgSorted.forEach((k, v) -> {
        logger.info("getPhoneStats phone " + k + " = " + v.getAvg());
      });

      if (DEBUG) {
        logger.info("getPhoneStats phoneToAvgSorted " + phoneToAvgSorted.size() + " " + phoneToAvgSorted);
      }
    }
    return phoneToAvgSorted;
  }

  private void maybeRemoveUnderscore(Map<String, List<Bigram>> phoneToBigram) {
    phoneToBigram.forEach((k, v) -> {
      if (v.size() > 1) {
        List<Bigram> collect = v.stream().filter(bigram -> bigram.getBigram().startsWith(UNDERSCORE)).collect(Collectors.toList());
//        logger.info("\tgetPhoneReport REMOVE : " + k + " has " + v.size() + " bigrams : " + v + " remove " + collect);
        collect.forEach(v::remove);
      }

      v.sort((o1, o2) -> Float.compare(o1.getScore(), o2.getScore()));
//      logger.info("getPhoneSummary " + k + " has " + v.size() + " bigrams : " + v);

     /*    Bigram bigram = v.get(0);
      if (bigram.getBigram().startsWith(UNDERSCORE)) {
        if (v.size() > 1) {
          v.remove(0);
          logger.info("\tgetPhoneReport REMOVE : " + k + " has " + v.size() + " bigrams : " + v);
        }
      }*/
    });
  }

  @NotNull
  private Map<String, List<Bigram>> getPhoneToBigrams(Map<String, Map<String, Bigram>> phoneToBigramToScore) {
    Map<String, List<Bigram>> phoneToBigram = new HashMap<>(phoneToBigramToScore.size());

    //  logger.info("phones? " + phoneToBigramToScore.keySet());

    phoneToBigramToScore.keySet().forEach(phone -> phoneToBigram.put(phone, new ArrayList<>()));

    phoneToBigramToScore.forEach((k, v) -> {
      List<Bigram> bigramsForPhone = phoneToBigram.get(k);
      bigramsForPhone.addAll(v.values());
    });

    if (DEBUG) phoneToBigram.forEach((k, v) -> logger.info("after " + k + "->" + v));
/*
    phoneToBigram.values().forEach(bigrams -> bigrams.sort(Bigram::compareTo));
    phoneToBigram.forEach((k, v) -> logger.info("after 2 " + k + "->" + v));*/

/*

    logger.info("getPhoneSummary found " + phoneToBigramToWS.keySet().size() + " phones : " + phoneToBigramToWS.keySet());
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

  private void sortExamples(Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS) {
    phoneToBigramToWS.forEach((phone, bgToExamples) -> {
      bgToExamples.forEach((bg, examples) -> {

        if (bg.startsWith(phone)) {
          examples.sort((o1, o2) -> {
            int compare = Float.compare(o1.getPrevScore(), o2.getPrevScore());
            if (compare == 0) {
              return o1.getTieBreaker(o2, compare);
            } else return compare;
          });
        } else {
          examples.sort(WordAndScore::compareTo);
        }
      });
    });

/*    phoneToBigramToWS
        .values()
        .forEach(wordAndScores -> wordAndScores
            .values()
            .forEach(scores -> scores
                .sort(WordAndScore::compareTo)));*/
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


      int i = Float.compare(current, current1);

      if (i == 0) {
        logger.info("sortPhonesByAvg got same " + current + " for " + o1 + " and " + o2);
      } else {
//        logger.info("sortPhonesByAvg got " + i + " for " + o1 + " = " +
//            current +
//            " vs " + o2 + " = " + current1);

      }
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
/*  private Map<String, List<WordAndScore>> sortPhonesByLatest(final Map<String, PhoneStats> phoneToAvg, List<String> sortedPhones) {
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
  }*/

/*  private float getAverage(Map.Entry<String, List<WordAndScore>> pair) {
    float total = 0;
    for (WordAndScore example : pair.getValue()) total += example.getPronScore();
    total /= pair.getValue().size();
    return total;
  }*/
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
   * @see #getPhoneReport
   */
  private Map<String, PhoneStats> getPhoneToPhoneStats(Map<String, List<PhoneAndScore>> phoneToScores) {
    final Map<String, PhoneStats> phoneToAvg = new HashMap<String, PhoneStats>();

    phoneToScores.forEach((phone, scores) -> {
      scores.sort(Comparator.comparingLong(PhoneAndScore::getTimestamp));
      List<TimeAndScore> phoneTimeSeries = getPhoneTimeSeries(scores);
      PhoneStats value = new PhoneStats(phoneTimeSeries.size(), phoneTimeSeries);
      phoneToAvg.put(phone, value);
      //  logger.info("phone " + phone + " = " + value.getAvg());
    });

    return phoneToAvg;
  }

  /**
   * For now we don't carry forward the session size here...
   *
   * @param phoneAndScores
   * @return
   * @see #getPhoneReport
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
