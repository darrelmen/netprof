/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import java.util.Map;

/**
 * Created by go22670 on 10/26/15.
 */
public class PhoneAndScore implements Comparable<PhoneAndScore>/*, Serializable */{
  private long timestamp;
  private float pronScore;

  /**
   * @see mitll.langtest.server.database.PhoneDAO#addResultTime(Map, long, String, float)
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

  public long getTimestamp() {
    return timestamp;
  }

  public float getPronScore() {
    return pronScore;
  }
}
