/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.phone.PhoneDAO;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/22/15.
 */
public class PhoneReport implements Serializable {
  private int overallPercent;
  private Map<String, List<WordAndScore>> phoneToWordAndScoreSorted;
  private Map<String, PhoneStats> phoneToAvgSorted;

  private boolean valid = false;

  public PhoneReport() {
    valid = false;
  }

  /**
   * @param overallPercent
   * @param phoneToWordAndScoreSorted
   * @see PhoneDAO#getPhoneReport(Map, Map, float, float)
   */
  public PhoneReport(int overallPercent,
                     Map<String, List<WordAndScore>> phoneToWordAndScoreSorted,
                     Map<String, PhoneStats> phoneToAvgSorted
  ) {
    this.overallPercent = overallPercent;
    this.phoneToWordAndScoreSorted = phoneToWordAndScoreSorted;
    this.phoneToAvgSorted = phoneToAvgSorted;
    valid = true;
  }

  public Map<String, List<WordAndScore>> getPhoneToWordAndScoreSorted() {
    return phoneToWordAndScoreSorted;
  }

  public int getOverallPercent() {
    return overallPercent;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.PhoneContainer#getTableWithPager(PhoneReport)
   */
  public Map<String, PhoneStats> getPhoneToAvgSorted() {
    return phoneToAvgSorted;
  }

  public boolean isValid() {
    return valid;
  }

  public String toString() {
    Map<String, PhoneStats> phoneToAvgSorted = getPhoneToAvgSorted();
    return "valid " + valid + " : " + (phoneToAvgSorted == null ? "null phoneToAvgSorted?" :phoneToAvgSorted.toString());
  }
}