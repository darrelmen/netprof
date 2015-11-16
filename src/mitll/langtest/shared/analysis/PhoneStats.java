package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/26/15.
 */
public class PhoneStats implements /*Comparable<PhoneStats>,*/ Serializable {
  private int initial;
  private int current;
  private int count;
  private List<TimeAndScore> timeSeries;
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
    this.initial = initial;
    this.current = current;
    this.timeSeries = timeSeries;
  }

  public int getInitial() {
    return initial;
  }

  public int getCurrent() {
    return current;
  }

  public int getDiff() { return current - initial; }

  public int getCount() {
    return count;
  }

  public List<TimeAndScore> getTimeSeries() {
    return timeSeries;
  }

  public String toString() {
    return "count " + count + " initial " + initial + " current " +current + " num sessions " + getSessions().size();
  }

  public void setSessions(List<PhoneSession> sessions) {
    this.sessions = sessions;
  }

  public List<PhoneSession> getSessions() {
    return sessions;
  }
}
