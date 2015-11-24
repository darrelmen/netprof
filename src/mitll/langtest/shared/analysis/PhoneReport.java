/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import mitll.langtest.client.analysis.*;

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

  public PhoneReport() { valid = false; }

  /**
   * @param overallPercent
   * @param phoneToWordAndScoreSorted
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneReport(Map, Map, float, float)
   */
  public PhoneReport(int overallPercent,
                     Map<String, List<WordAndScore>> phoneToWordAndScoreSorted,
                     Map<String, PhoneStats> phoneToAvgSorted/*,
                     Map<String, Integer> phoneToCount*/) {
    this.overallPercent = overallPercent;
    this.phoneToWordAndScoreSorted = phoneToWordAndScoreSorted;
    this.phoneToAvgSorted = phoneToAvgSorted;
  //  this.phoneToCount = phoneToCount;
    valid = true;
  }

  public Map<String, List<WordAndScore>> getPhoneToWordAndScoreSorted() {
    return phoneToWordAndScoreSorted;
  }

  /**
   * @see mitll.langtest.client.analysis.PhoneContainer#gotClickOnItem(PhoneAndStats)
   * @see PhoneContainer#showExamplesForSelectedSound()
   * @param phone
   * @return
   */
  public List<WordAndScore> getWordExamples(String phone) { return phoneToWordAndScoreSorted.get(phone);}
  public String toString() { return getPhoneToAvgSorted().toString(); }

  public int getOverallPercent() {
    return overallPercent;
  }

  /**
   * @see mitll.langtest.client.analysis.PhoneContainer#getTableWithPager(PhoneReport)
   * @return
   */
  public Map<String, PhoneStats> getPhoneToAvgSorted() {
    return phoneToAvgSorted;
  }

/*
  public Map<String, Integer> getPhoneToCount() {
    Map<String,Integer> phoneToCount = new LinkedHashMap<>();
    for (Map.Entry<String, PhoneStats> pair : phoneToAvgSorted.entrySet()) {
      phoneToCount.put(pair.getKey(),pair.getValue().getCount());
    }
    return phoneToCount;
  }
*/

  public boolean isValid() {
    return valid;
  }
}