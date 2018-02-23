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

import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.analysis.PhoneStats;
import mitll.langtest.shared.analysis.SimpleTimeAndScore;
import mitll.langtest.shared.analysis.TimeAndScore;
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

  private static final long MINUTE = 60 * 1000;
  private static final long HOUR = 60 * MINUTE;
  //  private static final long TENMIN = 10 * MINUTE;
  private static final long DAY = 24 * HOUR;
  //  private static final long QUARTER = 6 * HOUR;
  private static final long WEEK = 7 * DAY;
  private static final long MONTH = 4 * WEEK;
  private static final long SESSION = -1L;

  //  private static final long YEAR = 52 * WEEK;
  private static final List<Long> GRANULARITIES = Arrays.asList(
      SESSION,
//      MINUTE,
      //    TENMIN,
      HOUR,
      //    QUARTER,
      DAY,
      WEEK,
      MONTH//,
      //YEAR
  );
  private static final String OVERALL = "overall";
  private static final boolean DEBUG = false;

  /**
   * @param phoneToAvgSorted
   * @param useSessionGran
   * @see Analysis#getPhoneReport
   * @see mitll.langtest.server.database.phone.MakePhoneReport#getPhoneReport(Map, Map, float, float, boolean, boolean)
   */
  public void setSessionsWithPrune(Map<String, PhoneStats> phoneToAvgSorted, boolean useSessionGran) {
    // logger.info("setSessionsWithPrune " + useSessionGran);
    phoneToAvgSorted.forEach((phone, stats) ->
        stats.setSessions(partitionWithPrune(phone, stats.getTimeSeries(), useSessionGran)));
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
   * @see #setSessionsWithPrune
   */
  private List<PhoneSession> partitionWithPrune(String key,
                                                List<TimeAndScore> answersForUser,
                                                boolean useSessionGran) {
    return getPhoneSessionsWithPrune(key, answersForUser, GRANULARITIES, false, useSessionGran);
  }

  /**
   * @param answersForUser
   * @return
   * @see Analysis#getUserPerformance
   */
  <T extends SimpleTimeAndScore> Map<Long, List<PhoneSession>> getGranularityToSessions(List<T> answersForUser) {
    Map<Long, List<PhoneSessionInternal>> granularityToSessions = getGranularityToSessions(answersForUser, GRANULARITIES);

    Map<Long, List<PhoneSession>> granToSessions = new HashMap<>();
    granularityToSessions
        .forEach((k, v) -> granToSessions.put(k,
            getPhoneSessions(OVERALL, v, false)));

    return granToSessions;
  }

  /**
   * @param phone
   * @param answersForUser
   * @param possibleGrans
   * @param shouldPrune
   * @return
   * @see #partitionWithPrune
   */
  private List<PhoneSession> getPhoneSessionsWithPrune(String phone,
                                                       List<TimeAndScore> answersForUser,
                                                       List<Long> possibleGrans,
                                                       boolean shouldPrune,
                                                       boolean useSessionGran) {
    Map<Long, List<PhoneSessionInternal>> granularityToSessions = getGranularityToSessions(answersForUser, possibleGrans);

//    granularityToSessions.forEach((k, v) -> logger.info("getPhoneSessionsWithPrune " + k + " = " + v.size()));
    List<PhoneSessionInternal> toUse = useSessionGran ? granularityToSessions.get(SESSION) : chooseSession(possibleGrans, granularityToSessions);
//    logger.info("getPhoneSessionsWithPrune phone '" + phone + "' use " + useSessionGran + " got " + toUse.size());
    return getPhoneSessions(phone, toUse, shouldPrune);
  }

  private <T extends SimpleTimeAndScore> Map<Long, List<PhoneSessionInternal>> getGranularityToSessions(
      List<T> answersForUser,
      List<Long> times) {
    Map<Long, List<PhoneSessionInternal>> granularityToSessions = new HashMap<>();
    Map<Long, PhoneSessionInternal> granToCurrent = new HashMap<>();
    times.forEach(time -> granularityToSessions.put(time, new ArrayList<>()));

    partition(answersForUser, times, granularityToSessions, granToCurrent);

    if (DEBUG) logger.info("getGranularityToSessions # sessions " + granularityToSessions.get(SESSION).size());

    return granularityToSessions;
  }

  /**
   * @param thePhone
   * @param toUse    raw sessions to filter
   * @param prune    true if we want to not make sessions that are smaller than MIN_SESSION_SIZE (9)
   * @return
   * @see #getGranularityToSessions(List)
   * @see #getPhoneSessions(String, List, boolean)
   */
  private List<PhoneSession> getPhoneSessions(String thePhone,
                                              List<PhoneSessionInternal> toUse,
                                              boolean prune) {
    List<PhoneSession> sessions2 = new ArrayList<PhoneSession>();

    if (toUse == null) {
      logger.error("getPhoneSessions huh? no sessions?");
    } else {
//      logger.info("getPhoneSessions " + thePhone + " prune " + prune + " sessions " + toUse.size());
      int size = toUse.size();

      for (PhoneSessionInternal internal : toUse) {
        internal.remember();

        if (!prune || (internal.getCount() > MIN_SESSION_SIZE || size == 1)) {
          if (internal.getEnd() == 0) {
            logger.error("getPhoneSessions got 0 end time " + internal);
          }
          double mean = internal.getMean();
          double stdev1 = internal.getStdev();
          long meanTime = internal.getMeanTime();

          long bin = internal.getBin();
          sessions2.add(new PhoneSession(thePhone,
              bin,
              internal.getCount(),
              mean,
              stdev1,
              meanTime,
              internal.getStart(),
              internal.getEnd(),
              internal.getSessionSize()));

        } else {
          logger.warn("\tgetPhoneSessions: for " + thePhone + "skipping session " + internal);
        }
      }
    }
    return sessions2;
  }

  /**
   * For all time granularities - Hour, Day, Week, Month
   * bin by time, add answers to bin
   * bin = PhoneSession
   * make a
   *
   * @param answersForUser        all the answers by that user
   * @param times                 granularities - HOUR, DAY, WEEK, MONTH
   * @param granularityToSessions all sessions for this granularity
   * @param granToCurrent         bin to session
   * @see #getGranularityToSessions
   */
  private <T extends SimpleTimeAndScore> void partition(List<T> answersForUser,
                                                        List<Long> times,
                                                        Map<Long, List<PhoneSessionInternal>> granularityToSessions,
                                                        Map<Long, PhoneSessionInternal> granToCurrent) {
    long last = 0;

    long currentSession = 0;

    if (DEBUG) logger.info("partition " + answersForUser.size());

    for (T r : answersForUser) { // sorted by time
      long timestamp = r.getTimestamp();

      for (Long granularity : times) {
        PhoneSessionInternal phoneSessionInternal = granToCurrent.get(granularity);
        if (granularity == SESSION) {
          long sessionStart = r.getSessionStart();
//          if (sessionStart> 0 || sessionStart == -1) {
//            logger.info("granularity " + granularity + " Current " + phoneSessionInternal + " session " + sessionStart + " for " + r.getTimestamp() + " " + r.getScore());
//          }

          if ((phoneSessionInternal == null) || (currentSession != sessionStart)) {
            phoneSessionInternal = new PhoneSessionInternal(sessionStart, r.getSessionSize());
            granularityToSessions.get(granularity).add(phoneSessionInternal);
            granToCurrent.put(granularity, phoneSessionInternal);
            if (DEBUG)
              logger.info("partition : new session granularity current session " + phoneSessionInternal + " last " + currentSession + " now " + sessionStart);

            currentSession = sessionStart;
          } else {
            if (DEBUG) logger.info("partition for " + r + " current " + currentSession + " vs this " + sessionStart);
          }
        } else {
          if ((phoneSessionInternal == null) ||
              (timestamp - last > granularity /*&& phoneSessionInternal.getN() > MIN_SESSION_SIZE*/)
              ) {
            long gran = (timestamp / granularity) * granularity;
            phoneSessionInternal = new PhoneSessionInternal(gran);
            granularityToSessions.get(granularity).add(phoneSessionInternal);
            granToCurrent.put(granularity, phoneSessionInternal);
          } else {
            //     logger.info("for " + r + " diff " + diff + " and " + phoneSessionInternal.getN());
          }
        }

        phoneSessionInternal.addValue(r.getScore(), r.getTimestamp());
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