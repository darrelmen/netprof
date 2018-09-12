package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.Map;

public class PhoneSummary implements Serializable {
  private Map<String, PhoneStats> phoneToAvgSorted = null;
  private long serverTime;
  private int reqid;

  public PhoneSummary() {
  }

  public PhoneSummary(Map<String, PhoneStats> phoneToAvgSorted) {
    this.phoneToAvgSorted = phoneToAvgSorted;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.PhoneContainer#getTableWithPager
   */
  public Map<String, PhoneStats> getPhoneToAvgSorted() {
    return phoneToAvgSorted;
  }


  public int getReqid() {
    return reqid;
  }

  public PhoneSummary setReqid(int reqid) {
    this.reqid = reqid;
    return this;
  }

  public long getServerTime() {
    return serverTime;
  }

  public void setServerTime(long serverTime) {
    this.serverTime = serverTime;
  }

  public String toString() {
    Map<String, PhoneStats> phoneToAvgSorted = getPhoneToAvgSorted();
    return (phoneToAvgSorted == null ? "null phoneToAvgSorted?" : phoneToAvgSorted.keySet().toString());
  }
}
