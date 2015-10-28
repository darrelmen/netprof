package mitll.langtest.shared.analysis;

import org.moxieapps.gwt.highcharts.client.Chart;

import java.io.Serializable;
import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 * @see mitll.langtest.client.custom.AnalysisPlot#addSeries(List, Chart, String)
 */

public class TimeAndScore implements Serializable,Comparable<TimeAndScore>{
  private String id;

  private long timestamp;
  private float score;
  private int count = 0;
  private float cumulativeAverage;
//  private boolean isIPad;

  /**
   * @see UserPerformance#setRawBestScores(List)
   * @param bs
   * @param cumulativeAverage
   */
  public TimeAndScore(BestScore bs, float cumulativeAverage) {
    this.id = bs.getId();
    timestamp = bs.getTimestamp();
    score = bs.getScore();
    count = bs.getCount();
  //  isIPad = bs.isiPad();
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

  public float getScore() {
    return score;
  }

  public int getCount() {
    return count;
  }

  public String toString() {
    String format = getTimeString();
    return id + " at " + format + " avg score for " + count + "\t=\t" + score +"\t"+ getCumulativeAverage();
  }

  private String getTimeString() {
    return ""+getTimestamp();
  }

  public String toCSV() {
    return getTimeString() + "," + count + "," + score +","+ getCumulativeAverage();
  }

  @Override
  public int compareTo(TimeAndScore o) {
    return new Long(timestamp).compareTo(o.getTimestamp());
  }

/*
  public void setMoving(float moving) {
    this.cumulativeAverage = moving;
  }
*/

  public float getCumulativeAverage() {
    return cumulativeAverage;
  }

  public String getId() {
    return id;
  }

/*
  public boolean isIPad() {
    return isIPad;
  }
*/
}