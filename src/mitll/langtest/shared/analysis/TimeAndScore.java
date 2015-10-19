package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.ResultDAO;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 */

public class TimeAndScore implements Serializable,Comparable<TimeAndScore>{
  private long timestamp;
  private float score;
  private int count = 0;
  private float movingAverage;

/*
  public TimeAndScore(long timestamp, float score, int count) {
    this.timestamp = timestamp;
    this.score = score;
    this.count = count;
  }
*/

  public TimeAndScore(ResultDAO.BestScore bs, float movingAverage) {
    timestamp = bs.getTimestamp();
    score = bs.getScore();
    count = bs.getCount();
    this.movingAverage = movingAverage;
  }

  public TimeAndScore(List<ResultDAO.BestScore> bestScoreList) {
    float total = 0;
    for (ResultDAO.BestScore bs : bestScoreList) total += bs.getScore();
    score = total / (float) bestScoreList.size();
    count = bestScoreList.size();
    this.timestamp = bestScoreList.get(0).getTimestamp();
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
    SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
    return " at " + df.format(getTimestamp()) + " avg score for " + count + "\t=\t" + score +"\t"+movingAverage;
  }

  public String toCSV() {
    SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
    return df.format(getTimestamp()) + "," + count + "," + score +","+movingAverage;
  }

  @Override
  public int compareTo(TimeAndScore o) {
    return new Long(timestamp).compareTo(o.getTimestamp());
  }
}