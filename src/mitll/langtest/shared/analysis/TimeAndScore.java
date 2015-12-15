/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 *
 * @see mitll.langtest.client.analysis.AnalysisPlot#addSeries
 */

public class TimeAndScore extends SimpleTimeAndScore implements Comparable<SimpleTimeAndScore> {
  private String id;
  private float cumulativeAverage;

  /**
   * @param bs
   * @param cumulativeAverage
   * @see UserPerformance#setRawBestScores(List)
   */
  public TimeAndScore(BestScore bs, float cumulativeAverage) {
    this(bs.getExId(), bs.getTimestamp(), bs.getScore(), cumulativeAverage, null);
  }

  /**
   * @param id
   * @param timestamp
   * @param score
   * @param cumulativeAverage
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneTimeSeries(List)
   */
  public TimeAndScore(String id, long timestamp, float score, float cumulativeAverage, WordAndScore wordAndScore) {
    super(timestamp, score, wordAndScore);
    this.id = id;
    this.cumulativeAverage = cumulativeAverage;
  }

  public TimeAndScore() {
  }

  @Override
  public int compareTo(SimpleTimeAndScore o) {
    return Long.valueOf(getTimestamp()).compareTo(o.getTimestamp());
  }

  private String getTimeString() {
    return "" + getTimestamp();
  }

  public String toCSV() {
    return getTimeString() +
        "," + getScore() + "," + getCumulativeAverage();
  }

  public float getCumulativeAverage() {
    return cumulativeAverage;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores(List)
   */
  public String getId() {
    return id;
  }

  public String toString() {
    String format = getTimeString();
    return id + "\tat\t" + format + " avg score for " + //getWordAndScore().getWord() +
        //count + "\t" +
        " =\t" + getScore() + "\t" + getCumulativeAverage();
  }
}