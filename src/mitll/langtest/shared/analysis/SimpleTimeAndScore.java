/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.phone.PhoneDAO;

import java.io.Serializable;
import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 *
 * @see mitll.langtest.client.analysis.AnalysisPlot#addSeries
 */
public class SimpleTimeAndScore implements Serializable {
  private long timestamp;
  private float score;
  private transient WordAndScore wordAndScore;

  /**
   * @param timestamp
   * @param score
   * @see PhoneDAO#getPhoneTimeSeries(List)
   */
  SimpleTimeAndScore(long timestamp, float score, WordAndScore wordAndScore) {
    this.timestamp = timestamp;
    this.score = score;
    this.wordAndScore = wordAndScore;
  }

  /**
   * @see BestScore#BestScore
   * @param timestamp
   * @param score
   */
  SimpleTimeAndScore( long timestamp, float score) {
    this.timestamp = timestamp;
    this.score = score;
    this.wordAndScore = null;
  }

  public SimpleTimeAndScore() {
  }

  public WordAndScore getWordAndScore() {
    return wordAndScore;
  }

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * TODO : make this an int
   *
   * @return
   */
  public float getScore() {
    return score;
  }

  public String toString() {
    String format = getTimeString();
    return "at\t" + format + " avg score for " +
        //count + "\t" +
        "=\t" + score;
  }

  private String getTimeString() {
    return "" + getTimestamp();
  }

}