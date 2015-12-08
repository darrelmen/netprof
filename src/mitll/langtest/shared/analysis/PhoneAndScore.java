/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/26/15.
 */
public class PhoneAndScore implements Comparable<PhoneAndScore> {
  private long timestamp;
  private float pronScore;
  private WordAndScore wordAndScore;

  /**
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneReport(String, Map, boolean)
   * @param pronScore
   * @param timestamp
   */
  public PhoneAndScore(float pronScore, long timestamp) {
    this.pronScore = pronScore;
    this.timestamp = timestamp;
  }

  @Override
  public int compareTo(PhoneAndScore o) {
    return Long.valueOf(timestamp).compareTo(o.timestamp);
  }

  /**
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneTimeSeries(List)
   * @return
   */
  public long getTimestamp() {
    return timestamp;
  }

  public float getPronScore() {
    return pronScore;
  }

  public void setWordAndScore(WordAndScore wordAndScore) {
    this.wordAndScore = wordAndScore;
  }

  public WordAndScore getWordAndScore() {
    return wordAndScore;
  }
  
  public String toString() {
    return ""+pronScore;
  }
}
