package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 */

public class TimeAndScore implements Serializable,Comparable<TimeAndScore>{
  private long timestamp;
  private float score;
  private int count = 0;
  private float cumulativeAverage;

  /**
   * @see UserPerformance#setRawBestScores(List)
   * @param bs
   * @param cumulativeAverage
   */
  public TimeAndScore(BestScore bs, float cumulativeAverage) {
    timestamp = bs.getTimestamp();
    score = bs.getScore();
    count = bs.getCount();
    this.cumulativeAverage = cumulativeAverage;
  }

  /**
   * @see UserPerformance#addBestScores
   * @param bestScoreList
   * @param binSize
   */
  public TimeAndScore(List<BestScore> bestScoreList, long binSize) {
    float total = 0;
    for (BestScore bs : bestScoreList) total += bs.getScore();
    score = total / (float) bestScoreList.size();
    count = bestScoreList.size();
    this.timestamp = (bestScoreList.get(0).getTimestamp()/binSize)*binSize;
  }

  public TimeAndScore() {
  }

  public long getTimestamp() {
    return timestamp;
  }

  public float getScore() {
    return score;
  }

  public int getCount() {
    return count;
  }

  public String toString() {
    String format = getTimeString();
    return " at " + format + " avg score for " + count + "\t=\t" + score +"\t"+ getCumulativeAverage();
  }

  private String getTimeString() {
   // SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
   // String format = df.format(getTimestamp());
    return ""+getTimestamp();
  }

  public String toCSV() {
    return getTimeString() + "," + count + "," + score +","+ getCumulativeAverage();
  }

  @Override
  public int compareTo(TimeAndScore o) {
    return new Long(timestamp).compareTo(o.getTimestamp());
  }

  public void setMoving(float moving) {
    this.cumulativeAverage = moving;
  }

  public float getCumulativeAverage() {
    return cumulativeAverage;
  }
}