/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/26/15.
 */
public class PhoneStats implements Serializable {
//  private int initial;
//  private int current;
  private int count;
  private transient List<TimeAndScore> timeSeries;
  private List<PhoneSession> sessions;

  public PhoneStats() {
  }

  /**
   * @param count
   * @param initial
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneReport(Map, Map, float, float)
   */
  public PhoneStats(int count, int initial, int current,
                    List<TimeAndScore> timeSeries) {
    this.count = count;
//    this.initial = initial;
//    this.current = current;
    this.timeSeries = timeSeries;
  }

//  public int getInitial() {
//    return initial;
 // }

  public int getInitial() {
    List<PhoneSession> sessions = getSessions();
    if (sessions == null) return 0;

    PhoneSession next = sessions.iterator().next();
    double mean = next.getMean();
    return toHundred(mean);
  }

  public int toHundred(double mean) {
    return (int) Math.round(100 * mean);
  }
/*
  public int getCurrent() {
    return current;
  }
*/


  public int getCurrent() {
    List<PhoneSession> sessions2 = getSessions();
    if (sessions2 == null) return 0;
    return toHundred(sessions2.get(sessions2.size()-1).getMean());
  }

  public int getDiff() { return getCurrent() - getInitial(); }

  public int getCount() {
    return count;
  }

  public List<TimeAndScore> getTimeSeries() {
    return timeSeries;
  }

  public String toString() {
    return "count " + count + " initial " + getInitial() + " current " +getCurrent() + " num sessions " + getSessions().size() + " : " + getSessions();
  }

  public void setSessions(List<PhoneSession> sessions) {
    this.sessions = sessions;
  }

  public List<PhoneSession> getSessions() {
    return sessions;
  }
}
