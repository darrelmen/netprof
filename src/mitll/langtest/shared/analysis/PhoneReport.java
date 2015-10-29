package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/22/15.
 */
public class PhoneReport implements Serializable {
  private int overallPercent;
  private Map<String, List<WordAndScore>> phoneToWordAndScoreSorted;
  private Map<String, Float> phoneToAvgSorted;
  private Map<String, Integer> phoneToCount;
  private boolean valid = false;

  public PhoneReport() { valid = false; }

  /**
   * @param overallPercent
   * @param phoneToWordAndScoreSorted
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneReport(Map, Map, float, float)
   */
  public PhoneReport(int overallPercent, Map<String, List<WordAndScore>> phoneToWordAndScoreSorted, Map<String, Float> phoneToAvgSorted,
                     Map<String, Integer> phoneToCount) {
    this.overallPercent = overallPercent;
    this.phoneToWordAndScoreSorted = phoneToWordAndScoreSorted;
    this.phoneToAvgSorted = phoneToAvgSorted;
    this.phoneToCount = phoneToCount;
    valid = true;
  }

  public Map<String, List<WordAndScore>> getPhoneToWordAndScoreSorted() {
    return phoneToWordAndScoreSorted;
  }

  public List<WordAndScore> getWordExamples(String phone) { return phoneToWordAndScoreSorted.get(phone);}
  public String toString() { return getPhoneToAvgSorted().toString(); }

  public int getOverallPercent() {
    return overallPercent;
  }

  public Map<String, Float> getPhoneToAvgSorted() {
    return phoneToAvgSorted;
  }

  public Map<String, Integer> getPhoneToCount() {
    return phoneToCount;
  }

  public boolean isValid() {
    return valid;
  }
}