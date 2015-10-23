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

  public PhoneReport() {}

  /**
   * @param overallPercent
   * @param phoneToWordAndScoreSorted
   * @see #getWorstPhones
   */
  public PhoneReport(int overallPercent, Map<String, List<WordAndScore>> phoneToWordAndScoreSorted, Map<String, Float> phoneToAvgSorted) {
    this.overallPercent = overallPercent;
    this.phoneToWordAndScoreSorted = phoneToWordAndScoreSorted;
    this.phoneToAvgSorted = phoneToAvgSorted;
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
}