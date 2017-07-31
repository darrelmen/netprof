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

package mitll.langtest.server.database.analysis;

import mitll.langtest.server.database.phone.PhoneDAO;
import mitll.langtest.shared.analysis.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/18/15.
 */
public class PhoneAnalysis {
  private static final Logger logger = LogManager.getLogger(PhoneAnalysis.class);

  private static final int DESIRED_NUM_SESSIONS = 15;
  private static final int MIN_SESSION_SIZE = 9;
  private static final int REAL_MIN_SESSION_SIZE = MIN_SESSION_SIZE;

  private static final long MINUTE = 60 * 1000;
  private static final long HOUR = 60 * MINUTE;
  //  private static final long FIVEMIN = 5 * MINUTE;
  private static final long DAY = 24 * HOUR;
  //  private static final long QUARTER = 6 * HOUR;
  private static final long WEEK = 7 * DAY;
  private static final long MONTH = 4 * WEEK;
  //  private static final long YEAR = 52 * WEEK;
  private static final List<Long> GRANULARITIES = Arrays.asList(
      //FIVEMIN,
      HOUR,
      //    QUARTER,
      DAY,
      WEEK,
      MONTH//,
      //YEAR
  );
  private static final String OVERALL = "overall";

  /**
   * @param phoneToAvgSorted
   * @see PhoneDAO#setSessions(Map)
   */
  public void setSessions(Map<String, PhoneStats> phoneToAvgSorted) {
    for (Map.Entry<String, PhoneStats> pair : phoneToAvgSorted.entrySet()) {
      String phone = pair.getKey();
      PhoneStats stats = pair.getValue();
      stats.setSessions(partitionDontPrune(phone, stats.getTimeSeries()));
    }
  }

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
  private List<PhoneSession> partitionDontPrune(String key, List<TimeAndScore> answersForUser) {
    return getPhoneSessionsDontPrune(key, answersForUser, GRANULARITIES);
  }

  /**
   * @param answersForUser
   * @return
   * @see Analysis#getPerformanceForUser(int, int, int)
   */
  public <T extends SimpleTimeAndScore> Map<Long, List<PhoneSession>> getGranularityToSessions(List<T> answersForUser) {
    //String overall1 = OVERALL;
    Map<Long, List<PhoneSessionInternal>> overall = getGranularityToSessions(answersForUser, GRANULARITIES);
    Map<Long, List<PhoneSession>> serial = new HashMap<>();
    for (Map.Entry<Long, List<PhoneSessionInternal>> pair : overall.entrySet()) {
      serial.put(pair.getKey(), getPhoneSessions(OVERALL, pair.getValue(), false));
    }
    return serial;
  }

  /**
   * @param key
   * @param answersForUser
   * @param times
   * @return
   * @see #partitionDontPrune(String, List)
   */
  private List<PhoneSession> getPhoneSessionsDontPrune(String key, List<TimeAndScore> answersForUser, List<Long> times) {
    Map<Long, List<PhoneSessionInternal>> granularityToSessions = getGranularityToSessions(answersForUser, times);
    List<PhoneSessionInternal> toUse = chooseSession(times, granularityToSessions);
    return getPhoneSessions(key, toUse, true);
  }

  private <T extends SimpleTimeAndScore> Map<Long, List<PhoneSessionInternal>> getGranularityToSessions(
      List<T> answersForUser,
      List<Long> times) {
    Map<Long, List<PhoneSessionInternal>> granularityToSessions = new HashMap<>();
    Map<Long, PhoneSessionInternal> granToCurrent = new HashMap<>();
    for (Long time : times) {
      List<PhoneSessionInternal> value = new ArrayList<>();
      granularityToSessions.put(time, value);
    }

    partition(answersForUser, times, granularityToSessions, granToCurrent);
    return granularityToSessions;
  }

  /**
   * @param key
   * @param toUse raw sessions to filter
   * @param prune true if we want to not make sessions that are smaller than MIN_SESSION_SIZE (9)
   * @return
   * @see #getGranularityToSessions(List)
   * @see #getPhoneSessions(String, List, boolean)
   */
  private List<PhoneSession> getPhoneSessions(String key, List<PhoneSessionInternal> toUse, boolean prune) {
    List<PhoneSession> sessions2 = new ArrayList<PhoneSession>();
    if (toUse == null) {
      logger.error("getPhoneSessions huh? no sessions?");
    } else {
      int size = toUse.size();
      for (PhoneSessionInternal internal : toUse) {
        internal.remember();
        double mean = internal.getMean();
        double stdev1 = internal.getStdev();
        long meanTime = internal.getMeanTime();
        if (!prune || (internal.getCount() > REAL_MIN_SESSION_SIZE || size == 1)) {
          if (internal.getEnd() == 0) {
            logger.error("getPhoneSessions got 0 end time " + internal);
          }
          List<WordAndScore> examples = new ArrayList<>();
          examples.addAll(internal.getQueue());
          sessions2.add(new PhoneSession(key,
              internal.getBin(),
              internal.getCount(),
              mean,
              stdev1,
              meanTime,
              internal.getStart(), internal.getEnd(), examples));
        }
      }
    }
    return sessions2;
  }

  /**
   * @param answersForUser
   * @param times
   * @param granularityToSessions
   * @param granToCurrent
   * @see #getGranularityToSessions(List, List)
   */
  private <T extends SimpleTimeAndScore> void partition(List<T> answersForUser,
                                                        List<Long> times,
                                                        Map<Long, List<PhoneSessionInternal>> granularityToSessions,
                                                        Map<Long, PhoneSessionInternal> granToCurrent) {
    long last = 0;

    for (T r : answersForUser) {
      long timestamp = r.getTimestamp();

      for (Long time : times) {
        List<PhoneSessionInternal> phoneSessionInternals = granularityToSessions.get(time);
        PhoneSessionInternal phoneSessionInternal = granToCurrent.get(time);
        long gran = (timestamp / time) * time;
        long diff = timestamp - last;
        if ((phoneSessionInternal == null) || (diff > time && phoneSessionInternal.getN() > MIN_SESSION_SIZE)) {
          phoneSessionInternal = new PhoneSessionInternal(gran);
          phoneSessionInternals.add(phoneSessionInternal);
          granToCurrent.put(time, phoneSessionInternal);
        } else {
          //     logger.info("for " + r + " diff " + diff + " and " + phoneSessionInternal.getN());
        }
        phoneSessionInternal.addValue(r.getScore(), r.getTimestamp(), r.getWordAndScore());
      }
      last = timestamp;
    }
  }

  /**
   * Choose first granularity that has fewer than desired sessions (15)
   *
   * @param times
   * @param granularityToSessions
   * @return choosen session list
   */
  private List<PhoneSessionInternal> chooseSession(List<Long> times,
                                                   Map<Long, List<PhoneSessionInternal>> granularityToSessions) {
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

}
