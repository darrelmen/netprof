/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 *
 * @see mitll.langtest.client.analysis.AnalysisPlot#addSeries
 */

public class TimeAndScore implements Serializable, Comparable<TimeAndScore> {
  private String id;

  private long timestamp;
  private float score;
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
    this("",timestamp,score,cumulativeAverage);
  }

  /**
   * @see mitll.langtest.server.database.PhoneDAO#getPhoneTimeSeries(List)
   * @param id
   * @param timestamp
   * @param score
   * @param cumulativeAverage
   */
  private TimeAndScore(String id, long timestamp, float score, float cumulativeAverage) {
    this.id = id;
    this.timestamp = timestamp;
    this.score = score;
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

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * TODO : make this an int
   * @return
   */
  public float getScore() {
    return score;
  }

/*
  public int getCount() {
    return count;
  }
*/

  public String toString() {
    String format = getTimeString();
    return id + "\tat\t" + format + " avg score for " +
        //count + "\t" +
        "=\t" + score + "\t" + getCumulativeAverage();
  }

  private String getTimeString() {
    return "" + getTimestamp();
  }

  public String toCSV() {
    return getTimeString() +
        //"," + count +
        "," + score + "," + getCumulativeAverage();
  }

  @Override
  public int compareTo(TimeAndScore o) {
    return new Long(timestamp).compareTo(o.getTimestamp());
  }

  public float getCumulativeAverage() {
    return cumulativeAverage;
  }

  /**
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores(List)
   * @return
   */
  public String getId() {
    return id;
  }
}