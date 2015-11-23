package mitll.langtest.server.database.analysis;

import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.TimeAndScore;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by go22670 on 11/18/15.
 */
public class PhoneAnalysis {
  private static final Logger logger = Logger.getLogger(PhoneAnalysis.class);

  private static final int DESIRED_NUM_SESSIONS = 15;
  private static final int MIN_SESSION_SIZE = 9;
  private static final int REAL_MIN_SESSION_SIZE = MIN_SESSION_SIZE;

  private static final long MINUTE = 60 * 1000;
  private static final long HOUR = 60 * MINUTE;
  private static final long FIVEMIN = 5 * MINUTE;
  private static final long DAY = 24 * HOUR;
  private static final long QUARTER = 6 * HOUR;
  private static final long WEEK = 7 * DAY;
  private static final long MONTH = 4 * WEEK;
  private static final List<Long> GRANULARITIES = Arrays.asList(
      //FIVEMIN,
      //HOUR,
  //    QUARTER,
      DAY,
      WEEK,
      MONTH
  );

  /**
   * Adaptive granularity -- try to choose sessions separated by a time gap.
   * Choose a time gap that is close to the desired sessions = 15 or ({@link #DESIRED_NUM_SESSIONS}
   * <p>
   * The last session isn't guaranteed to have the required min size - potentially we'd want to combine the last two
   * sessions if the last one is too small.
   *
   * @param key
   * @param answersForUser
   * @return
   * @see Analysis#getPhonesForUser(long, int)
   */
  public List<PhoneSession> partition(String key, List<TimeAndScore> answersForUser) {
    // Collections.sort(answersForUser);
    List<PhoneSession> sessions2 = getPhoneSessions(key, answersForUser, GRANULARITIES);
//    for (PhoneSession session : sessions2) {
//      logger.info(session);
//    }
    return sessions2;
  }

/*  public List<PhoneSession> getOverallSessions(List<TimeAndScore> answersForUser) {
    Map<Long, List<PhoneSessionInternal>> granularityToSessions = new HashMap<>();
    Map<Long, PhoneSessionInternal> granToCurrent = new HashMap<>();
    for (Long time : times) {
      granularityToSessions.put(time, new ArrayList<>());
    }

    partition(key, answersForUser, times, granularityToSessions, granToCurrent);

    List<PhoneSessionInternal> toUse = chooseSession(times, granularityToSessions);
    return getPhoneSessions(key, toUse, false);
  }*/

  public Map<Long, List<PhoneSession>> getGranularityToSessions(List<TimeAndScore> answersForUser) {
    String overall1 = "overall";
    Map<Long, List<PhoneSessionInternal>> overall = getGranularityToSessions(overall1, answersForUser, GRANULARITIES);
    Map<Long, List<PhoneSession>> serial = new HashMap<>();
    for (Map.Entry<Long, List<PhoneSessionInternal>> pair : overall.entrySet()) {
      serial.put(pair.getKey(), getPhoneSessions(overall1, pair.getValue(), false));
    }
    return serial;
  }

  /**
   * @see #partition(String, List)
   * @param key
   * @param answersForUser
   * @param times
   * @return
   */
  private List<PhoneSession> getPhoneSessions(String key, List<TimeAndScore> answersForUser, List<Long> times) {
    Map<Long, List<PhoneSessionInternal>> granularityToSessions =
        getGranularityToSessions(key, answersForUser, times);

    List<PhoneSessionInternal> toUse = chooseSession(times, granularityToSessions);
    return getPhoneSessions(key, toUse, true);
  }

  private Map<Long, List<PhoneSessionInternal>> getGranularityToSessions(String key,
                                                                         List<TimeAndScore> answersForUser,
                                                                         List<Long> times) {
    Map<Long, List<PhoneSessionInternal>> granularityToSessions = new HashMap<>();
    Map<Long, PhoneSessionInternal> granToCurrent = new HashMap<>();
    for (Long time : times) {
      granularityToSessions.put(time, new ArrayList<>());
    }

    partition(key, answersForUser, times, granularityToSessions, granToCurrent);
    return granularityToSessions;
  }

  /**
   * @see #getGranularityToSessions(List)
   * @param key
   * @param toUse
   * @param prune
   * @return
   */
  private List<PhoneSession> getPhoneSessions(String key, List<PhoneSessionInternal> toUse, boolean prune) {
    List<PhoneSession> sessions2 = new ArrayList<PhoneSession>();
    if (toUse == null) {
      logger.error("huh? no sessions?");
    } else {
      int size = toUse.size();
      for (PhoneSessionInternal i : toUse) {
        i.remember();
        double mean = i.getMean();
        double stdev1 = i.getStdev();
        double meanTime = i.getMeanTime();
        if (!prune || (i.getCount() > REAL_MIN_SESSION_SIZE || size == 1)) {
          sessions2.add(new PhoneSession(key, i.getBin(), i.getCount(), mean, stdev1, meanTime));
        }
      }
    }
    return sessions2;
  }

  private void partition(String key,
                         List<TimeAndScore> answersForUser,
                         List<Long> times, Map<Long, List<PhoneSessionInternal>> granularityToSessions,
                         Map<Long, PhoneSessionInternal> granToCurrent) {
    long last = 0;

    for (TimeAndScore r : answersForUser) {
      long timestamp = r.getTimestamp();

      for (Long time : times) {
        List<PhoneSessionInternal> phoneSessionInternals = granularityToSessions.get(time);
        PhoneSessionInternal phoneSessionInternal = granToCurrent.get(time);
        long gran = (timestamp / time) * time;
        long diff = timestamp - last;
        if ((phoneSessionInternal == null) || (diff > time && phoneSessionInternal.getN() > MIN_SESSION_SIZE)) {
          phoneSessionInternal = new PhoneSessionInternal(key, gran);
          phoneSessionInternals.add(phoneSessionInternal);
          granToCurrent.put(time, phoneSessionInternal);
        } else {
          //     logger.info("for " + r + " diff " + diff + " and " + phoneSessionInternal.getN());
        }
        phoneSessionInternal.addValue(r.getScore(), r.getTimestamp());
      }
      last = timestamp;
    }
  }

  private List<PhoneSessionInternal> chooseSession(List<Long> times, Map<Long, List<PhoneSessionInternal>> granularityToSessions) {
    List<PhoneSessionInternal> toUse = null;
    for (Long time : times) {
      List<PhoneSessionInternal> phoneSessionInternals = granularityToSessions.get(time);
      if (phoneSessionInternals.size() < DESIRED_NUM_SESSIONS) {
        //  logger.warn(key +" choosing " + time / 1000 + " with size " + phoneSessionInternals.size());
        toUse = phoneSessionInternals;
        break;
      } else {
        //  logger.warn(key +"\t not choosing " + time / 1000 + " with size " + phoneSessionInternals.size());
      }
    }
    return toUse;
  }

  public static class PhoneSessionInternal {
    final transient SummaryStatistics summaryStatistics = new SummaryStatistics();
    final transient SummaryStatistics summaryStatistics2 = new SummaryStatistics();
  //  private final String phone;
    private double mean;
    private double stdev;
    private double meanTime;
    private long count;
    private final long bin;
    final List<TimeAndScore> values = new ArrayList<>();

    public PhoneSessionInternal(String phone, long bin) {
    //  this.phone = phone;
      this.bin = bin;
    }

    public void addValue(float value, long timestamp) {
      summaryStatistics.addValue(value);

      summaryStatistics2.addValue(timestamp);
      values.add(new TimeAndScore("", timestamp, value, 0));
    }

    public void remember() {
      this.count = summaryStatistics.getN();

      this.mean = summaryStatistics.getMean();

      this.stdev = summaryStatistics.getStandardDeviation();

      this.meanTime = summaryStatistics2.getMean();
    }

    public double getMean() {
      return mean;
    }

    public double getStdev() {
      return stdev;
    }

    public double getMeanTime() {
      return meanTime;
    }

    public long getCount() {
      return count;
    }

    public long getN() {
      return summaryStatistics.getN();
    }

    public long getBin() {
      return bin;
    }

/*    public String getPhone() {
      return phone;
    }*/
  }
}
