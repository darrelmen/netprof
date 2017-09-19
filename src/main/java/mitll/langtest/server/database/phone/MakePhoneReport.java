package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.analysis.PhoneAnalysis;
import mitll.langtest.shared.analysis.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by go22670 on 3/29/16.
 */
public class MakePhoneReport {
  private static final Logger logger = LogManager.getLogger(MakePhoneReport.class);

  private static final boolean DEBUG = false;

  /**
   * @param phoneToScores
   * @param phoneToWordAndScore
   * @param totalScore
   * @param totalItems
   * @param sortByLatestExample
   * @return
   * @see SlickPhoneDAO#getPhoneReport
   */
  public PhoneReport getPhoneReport(Map<String, List<PhoneAndScore>> phoneToScores,
                                    Map<String, List<WordAndScore>> phoneToWordAndScore,
                                    float totalScore,
                                    float totalItems,
                                    boolean sortByLatestExample) {
    float overallScore = totalItems > 0 ? totalScore / totalItems : 0;
    int percentOverall = (int) (100f * PhoneJSON.round(overallScore, 2));
    if (DEBUG) {
      logger.debug("score " + overallScore + " items " + totalItems + " percent " + percentOverall +
          " phoneToScores " + phoneToScores.size() + " : " + phoneToScores);
    }

    final Map<String, PhoneStats> phoneToAvg = getPhoneToPhoneStats(phoneToScores);
    //if (DEBUG || true) logger.debug("phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    List<String> sorted = new ArrayList<String>(phoneToAvg.keySet());

    if (DEBUG) logger.debug("before sorted " + sorted);

    setSessions(phoneToAvg);

    if (DEBUG) logger.debug("phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    if (sortByLatestExample) {
      phoneToWordAndScore = sortPhonesByLatest(phoneToAvg, sorted);
    } else {
      sortPhonesByCurrentScore(phoneToAvg, sorted);
    }

    if (DEBUG) logger.debug("sorted " + sorted.size() + " " + sorted);

    Map<String, PhoneStats> phoneToAvgSorted = new LinkedHashMap<String, PhoneStats>();
    for (String phone : sorted) {
      phoneToAvgSorted.put(phone, phoneToAvg.get(phone));
    }

    if (DEBUG) logger.debug("phoneToAvgSorted " + phoneToAvgSorted.size() + " " + phoneToAvgSorted);

    Map<String, List<WordAndScore>> phoneToWordAndScoreSorted = new LinkedHashMap<String, List<WordAndScore>>();

    for (String phone : sorted) {
      List<WordAndScore> value = phoneToWordAndScore.get(phone);
      Collections.sort(value);
      phoneToWordAndScoreSorted.put(phone, value);
    }

    if (DEBUG) logger.debug("phone->words " + phoneToWordAndScore);

    return new PhoneReport(percentOverall, phoneToWordAndScoreSorted, phoneToAvgSorted);
  }

  private void sortPhonesByCurrentScore(final Map<String, PhoneStats> phoneToAvg, List<String> sorted) {
    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        PhoneStats first = phoneToAvg.get(o1);
        PhoneStats second = phoneToAvg.get(o2);
        int current = first.getCurrent();
        int current1 = second.getCurrent();
        //if (current == current1) {
        //  logger.info("got same " + current + " for " + o1 + " and " + o2);
        //} else {
        // logger.info("\tgot " + current + " for " + o1 + " and " + current1 + " for "+ o2);
        //}
        int i = Integer.compare(current, current1);
        return i == 0 ? o1.compareTo(o2) : i;
      }
    });
/*
    for (String phone : sorted) {
      logger.info("phone " + phone + " : " + phoneToAvg.get(phone).getCurrent());
    }
*/
  }

  /**
   * For the iPad, we don't want to return every single example, just the latest one, and the summary score
   * for the phone is just for the latest ones, or else they can seem inconsistent, since the iPad only shows
   * distinct words.
   *
   * @param phoneToAvg
   * @param sorted
   * @return
   */
  private Map<String, List<WordAndScore>> sortPhonesByLatest(final Map<String, PhoneStats> phoneToAvg, List<String> sorted) {
    Map<String, List<WordAndScore>> phoneToMinimal = new HashMap<>();
    for (Map.Entry<String, PhoneStats> pair : phoneToAvg.entrySet()) {
      PhoneStats value = pair.getValue();
      Map<String, WordAndScore> wordToExample = new HashMap<>();
      for (TimeAndScore item : value.getTimeSeries()) {
//        logger.info("got " + item + " at " + new Date(item.getTimestamp()));
        wordToExample.put(item.getWordAndScore().getWord(), item.getWordAndScore());
      }
      phoneToMinimal.put(pair.getKey(), new ArrayList<>(wordToExample.values()));
    }

    final Map<String, Float> phoneToScore = new HashMap<>();
    for (Map.Entry<String, List<WordAndScore>> pair : phoneToMinimal.entrySet()) {
      float total = 0;
      for (WordAndScore example : pair.getValue()) total += example.getPronScore();
      total /= pair.getValue().size();
      phoneToScore.put(pair.getKey(), total);
    }

    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        Float current = phoneToScore.get(o1);
        Float current1 = phoneToScore.get(o2);
        int i = current.compareTo(current1);
        return i == 0 ? o1.compareTo(o2) : i;
      }
    });

    if (DEBUG) {
      for (String phone : sorted) {
        logger.info("phone " + phone + " : " + phoneToScore.get(phone) + " " + phoneToMinimal.get(phone));
      }
    }
    return phoneToMinimal;
  }

  /**
   * @param phoneToScores
   * @return
   * @see #getPhoneReport(Map, Map, float, float, boolean)
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
   * @param phoneToAvgSorted
   * @see #getPhoneReport(Map, Map, float, float, boolean)
   */
  private void setSessions(Map<String, PhoneStats> phoneToAvgSorted) {
    new PhoneAnalysis().setSessionsWithPrune(phoneToAvgSorted);
  }

  /**
   * @param rawBestScores
   * @return
   * @see #getPhoneReport(Map, Map, float, float, boolean)
   */
  private List<TimeAndScore> getPhoneTimeSeries(List<PhoneAndScore> rawBestScores) {
    float total = 0;
    float count = 0;
    List<TimeAndScore> phoneTimeSeries = new ArrayList<>();
    for (PhoneAndScore bs : rawBestScores) {
      float pronScore = bs.getPronScore();
      total += pronScore;
      count++;
      float moving = total / count;

      WordAndScore wordAndScore = bs.getWordAndScore();
      TimeAndScore timeAndScore = new TimeAndScore(-1, bs.getTimestamp(), pronScore, moving, wordAndScore);
      phoneTimeSeries.add(timeAndScore);
    }
    return phoneTimeSeries;
  }
}
