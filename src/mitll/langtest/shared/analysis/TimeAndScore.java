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

//  private long timestamp;
//  private float score;
  private float cumulativeAverage;

  /**
   * @param bs
   * @param cumulativeAverage
   * @see UserPerformance#setRawBestScores(List)
   */
  public TimeAndScore(BestScore bs, float cumulativeAverage) {
    this(bs.getExId(), bs.getTimestamp(), bs.getScore(), cumulativeAverage);
  }

  public TimeAndScore(long timestamp, float score, float cumulativeAverage) {
    this("", timestamp, score, cumulativeAverage);
  }

  /**
   * @param id
   * @param timestamp
   * @param score
   * @param cumulativeAverage
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneTimeSeries(List)
   */
  private TimeAndScore(String id, long timestamp, float score, float cumulativeAverage) {
    super(timestamp,score);
    this.id = id;
//    this.timestamp = timestamp;
//    this.score = score;
    this.cumulativeAverage = cumulativeAverage;
  }

  /**
   * @seex UserPerformance#addBestScores
   * @paramx bestScoreList
   * @paramx binSize
   */
/*  public TimeAndScore(List<BestScore> bestScoreList, long binSize) {
    float total = 0;
    for (BestScore bs : bestScoreList) total += bs.getScore();
    score = total / (float) bestScoreList.size();
    count = bestScoreList.size();
    this.timestamp = (bestScoreList.get(0).getTimestamp()/binSize)*binSize;
  }*/
  public TimeAndScore() {
  }
/*
  public long getTimestamp() {
    return timestamp;
  }

  *//**
   * TODO : make this an int
   *
   * @return
   *//*
  public float getScore() {
    return score;
  }*/

/*
  public int getCount() {
    return count;
  }
*/


  @Override
  public int compareTo(SimpleTimeAndScore o) {
    return Long.valueOf(getTimestamp()).compareTo(o.getTimestamp());
  }

  public String toString() {
    String format = getTimeString();
    return id + "\tat\t" + format + " avg score for " +
        //count + "\t" +
        "=\t" + getScore() + "\t" + getCumulativeAverage();
  }

  private String getTimeString() {
    return "" + getTimestamp();
  }

  public String toCSV() {
    return getTimeString() +
        //"," + count +
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
}