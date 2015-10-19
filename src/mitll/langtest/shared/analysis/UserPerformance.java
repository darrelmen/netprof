package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.ResultDAO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by go22670 on 10/19/15.
 */

public class UserPerformance implements Serializable {
  private List<TimeAndScore> timeAndScores = new ArrayList<>();
  private List<TimeAndScore> rawTimeAndScores = new ArrayList<>();

  private long userID;

/*  public UserPerformance(List<TimeAndScore> timeAndScores) {
    this.timeAndScores = timeAndScores;
  }*/

  public UserPerformance(long userID) {
    this.userID = userID;
  }

  public UserPerformance() {
  }

  public void addBestScores(List<ResultDAO.BestScore> bestScores) {
    addTimeAndScore(new TimeAndScore(bestScores));

  }

  public void addTimeAndScore(TimeAndScore ts) {
    timeAndScores.add(ts);

  }

  public int getTotal() {
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

  public float getAverage() {
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

  public List<TimeAndScore> getTimeAndScores() {
    return timeAndScores;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("For user " + getUserID() + " " + getTotal() + " items, avg score " + getAverage());
    for (TimeAndScore ts : getTimeAndScores()) builder.append("\n").append(ts);
    return builder.toString();
  }

  public long getUserID() {
    return userID;
  }

  public String toCSV() {
    StringBuilder builder = new StringBuilder();
    builder.append(",,," + userID + "," + getTotal() + "," + getAverage());
    for (TimeAndScore ts : getTimeAndScores()) builder.append("\n").append(ts.toCSV());
    return builder.toString();
  }

  public String toRawCSV() {
    StringBuilder builder = new StringBuilder();
    builder.append(",,," + userID + "," + getRawTotal() + "," + getRawAverage());
    for (TimeAndScore ts : getRawBestScores()) builder.append("\n").append(ts.toCSV());
    return builder.toString();
  }

  public void setRawBestScores(List<ResultDAO.BestScore> rawBestScores) {
    float total = 0;
    float count = 0;
    for (ResultDAO.BestScore bs : rawBestScores) {
      total += bs.getScore();
      count++;
      float moving = total / count;
      rawTimeAndScores.add(new TimeAndScore(bs, moving));
    }
    Collections.sort(rawTimeAndScores);
  }

  public List<TimeAndScore> getRawBestScores() {
    return rawTimeAndScores;
  }
}