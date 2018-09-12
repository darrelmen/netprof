package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PhoneBigrams implements Serializable {
  private Map<String, List<Bigram>> phoneToBigrams;
  private long serverTime;
  private int reqid;

  public PhoneBigrams() {
  }

  public PhoneBigrams(Map<String, List<Bigram>> phoneToBigrams) {
    this.phoneToBigrams = phoneToBigrams;
  }

  public Map<String, List<Bigram>> getPhoneToBigrams() {
    return phoneToBigrams;
  }

  public int getReqid() {
    return reqid;
  }

  public PhoneBigrams setReqid(int reqid) {
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
    Map<String, List<Bigram>> phoneToBigrams = getPhoneToBigrams();
    return (phoneToBigrams == null ? "null phone->bigram?" : phoneToBigrams.keySet().toString());
  }
}
