/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/26/15.
 */
public class PhoneStats implements Serializable {
 // private final Logger logger = Logger.getLogger("PhoneStats");

  private int count;
  private transient List<TimeAndScore> timeSeries;
  private List<PhoneSession> sessions;

  public PhoneStats() {
  }

  /**
   * @param count
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneReport(Map, Map, float, float)
   */
  public PhoneStats(int count,
                    List<TimeAndScore> timeSeries) {
    this.count = count;
    this.timeSeries = timeSeries;
  }

  public int getInitial() {
    List<PhoneSession> sessions = getSessions();
    return getInitial(sessions);
  }

  public int getInitial(List<PhoneSession> sessions) {
    if (sessions == null || sessions.isEmpty()) return 0;

    PhoneSession next = sessions.iterator().next();
    double mean = next.getMean();
    return toHundred(mean);
  }

  public int toHundred(double mean) {
    return (int) Math.round(100 * mean);
  }

  public int getCurrent() {
    List<PhoneSession> sessions2 = getSessions();
    return getCurrent(sessions2);
  }

  public int getCurrent(List<PhoneSession> sessions2) {
    if (sessions2 == null || sessions2.isEmpty()) return 0;
    return toHundred(sessions2.get(sessions2.size() - 1).getMean());
  }

  public int getDiff() {
    return getCurrent() - getInitial();
  }

  public int getCount() {
    return count;
  }


  public int getCount(List<PhoneSession> sessions2) {
    if (sessions2 == null || sessions2.isEmpty()) return 0;
    int total =0;
    for (PhoneSession session:sessions2) total+= session.getCount();
    return total;
  }

  public List<TimeAndScore> getTimeSeries() {
    return timeSeries;
  }

  public String toString() {
    return "count " + count + " initial " + getInitial() + " current " + getCurrent() + " num sessions " + getSessions().size() + " : " + getSessions();
  }

  public void setSessions(List<PhoneSession> sessions) {
    this.sessions = sessions;
  }

  public List<PhoneSession> getSessions() {
    return sessions;
  }
/*
  public List<PhoneSession> getFiltered(long first, long last) {

    List<PhoneSession> filtered = new ArrayList<>();
    for (PhoneSession session : filtered) {
      if (session.getStart() >= first && session.getEnd() <= last) {
        filtered.add(session);
        logger.info("included " +session);
      }
      else {
        logger.info("Exclude " + session);
      }
    }
    return filtered;
  }*/
}
