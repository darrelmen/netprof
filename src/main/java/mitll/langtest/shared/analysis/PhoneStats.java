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

package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.phone.PhoneDAO;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/26/15.
 *
 * @see mitll.langtest.client.analysis.PhoneContainer#clickOnPhone(String)
 */
public class PhoneStats implements Serializable {
  // private final Logger logger = LogManager.getLogger("PhoneStats");
  private int count;
  private transient List<TimeAndScore> timeSeries;
  private List<PhoneSession> sessions;

  public PhoneStats() {
  }

  /**
   * @param count
   * @see PhoneDAO#getPhoneReport(Map, Map, float, float)
   */
  public PhoneStats(int count, List<TimeAndScore> timeSeries) {
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
    return getCurrent(getSessions());
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
    int total = 0;
    for (PhoneSession session : sessions2) total += session.getCount();
    return total;
  }

  public List<TimeAndScore> getTimeSeries() {
    return timeSeries;
  }

  public String toString() {
    return "childCount " + count + " initial " + getInitial() + " current " + getCurrent() +
        (getSessions() != null ? " num sessions " + getSessions().size() + " : " + getSessions() : "");
  }

  public void setSessions(List<PhoneSession> sessions) {
    this.sessions = sessions;
  }

  public List<PhoneSession> getSessions() {
    return sessions;
  }
}
