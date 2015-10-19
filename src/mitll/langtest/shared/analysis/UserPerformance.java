package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.ResultDAO;

import java.io.Serializable;
import java.util.*;

/**
 * Created by go22670 on 10/19/15.
 */

public class UserPerformance implements Serializable {
  private List<TimeAndScore> timeAndScores = new ArrayList<>();
  private List<TimeAndScore> rawTimeAndScores = new ArrayList<>();

  private long userID;

  public UserPerformance(long userID) {
    this.userID = userID;
  }

  public UserPerformance(long userID,List<BestScore> resultsForQuery) {
    this(userID);
    setRawBestScores(resultsForQuery);
  }

  public UserPerformance() {
  }

  public void setAtBinSize(List<BestScore> resultsForQuery, long binSize) {
    Map<Long, List<BestScore>> bins = new TreeMap<>();
    for (BestScore bs : resultsForQuery) {
      long dayBin = bs.getTimestamp() / binSize;
      List<BestScore> bestScores = bins.get(dayBin);
      if (bestScores == null) bins.put(dayBin, bestScores = new ArrayList<BestScore>());
      bestScores.add(bs);
    }

    for (Map.Entry<Long, List<BestScore>> pair : bins.entrySet()) {
      addBestScores(pair.getValue(), binSize);
    }
    setRawBestScores(resultsForQuery);
  }

  /**
   * @param bestScores
   * @see ResultDAO#getResultForUserByBin(long, int)
   */
  private void addBestScores(List<BestScore> bestScores, long binSize) {
    addTimeAndScore(new TimeAndScore(bestScores, binSize));
  }

  private void addTimeAndScore(TimeAndScore ts) {
    timeAndScores.add(ts);
  }

  private int getTotal() {
    return getTotal(timeAndScores);
  }

  private int getTotal(Collection<TimeAndScore> timeAndScores) {
    int total = 0;
    for (TimeAndScore ts : timeAndScores) total += ts.getCount();
    return total;
  }

  public int getRawTotal() {
    return getTotal(rawTimeAndScores);

  }

  private float getAverage() {
    float total = getTotalScore(timeAndScores);
    return total / (float) getTotal();
  }

  public float getRawAverage() {
    float total = getTotalScore(rawTimeAndScores);
    return total / (float) getRawTotal();
  }

  /*
  private float getAverage(Collection<TimeAndScore> timeAndScores) {
    float total = getTotalScore(timeAndScores);
    return total / (float) getTotal();
  }
*/

  private float getTotalScore(Collection<TimeAndScore> timeAndScores) {
    float total = 0;
    for (TimeAndScore ts : timeAndScores) total += ts.getScore() * ts.getCount();
    return total;
  }

  private List<TimeAndScore> getTimeAndScores() {
    return timeAndScores;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("For user " + getUserID() + " " + getTotal() + " items, avg score " + getAverage());
    for (TimeAndScore ts : getTimeAndScores()) {
      builder.append("\n").append(ts);
    }
    return builder.toString();
  }

  private long getUserID() {
    return userID;
  }

  private void setMoving() {
    float total = 0;
    float count = 0;
    for (TimeAndScore ts : getTimeAndScores()) {
      total += ts.getScore() * ts.getCount();
      count += ts.getCount();
      float moving = total / count;
      ts.setMoving(moving);
    }
  }

  public String toCSV() {
    setMoving();
    StringBuilder builder = new StringBuilder();
    builder.append(",,,," + userID + "," + getTotal() + "," + getAverage());
    for (TimeAndScore ts : getTimeAndScores()) builder.append("\n").append(ts.toCSV());
    return builder.toString();
  }

  public String toRawCSV() {
    StringBuilder builder = new StringBuilder();
    builder.append(",,,," + userID + "," + getRawTotal() + "," + getRawAverage());
    for (TimeAndScore ts : getRawBestScores()) builder.append("\n").append(ts.toCSV());
    return builder.toString();
  }

  private void setRawBestScores(List<BestScore> rawBestScores) {
    Collections.sort(rawBestScores, new Comparator<BestScore>() {
      @Override
      public int compare(BestScore o1, BestScore o2) {
        return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
      }
    });

    float total = 0;
    float count = 0;
    for (BestScore bs : rawBestScores) {
      total += bs.getScore();
      count++;
      float moving = total / count;
      rawTimeAndScores.add(new TimeAndScore(bs, moving));
    }
  }

  public List<TimeAndScore> getRawBestScores() {
    return rawTimeAndScores;
  }
}