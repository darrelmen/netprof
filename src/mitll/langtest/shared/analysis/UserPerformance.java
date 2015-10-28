package mitll.langtest.shared.analysis;

import mitll.langtest.client.LangTestDatabaseAsync;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/19/15.
 */

public class UserPerformance implements Serializable {
  private transient final Logger logger = Logger.getLogger("UserPerformance");

  private List<TimeAndScore> timeAndScores = new ArrayList<>();
  private List<TimeAndScore> rawTimeAndScores = new ArrayList<>();
  private List<TimeAndScore> iPadTimeAndScores = new ArrayList<>();
  private List<TimeAndScore> browserTimeAndScores = new ArrayList<>();

  private long userID;

  public UserPerformance() {}
  public UserPerformance(long userID) {
    this.userID = userID;
  }

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getPerformanceForUser(long)
   * @param userID
   * @param resultsForQuery
   */
  public UserPerformance(long userID, List<BestScore> resultsForQuery) {
    this(userID);
    setRawBestScores(resultsForQuery);
  }

  /**
   * @see mitll.langtest.server.database.analysis.Analysis#getResultForUserByBin(long, int)
   * @param resultsForQuery
   * @param binSize
   */
/*  public void setAtBinSize(List<BestScore> resultsForQuery, long binSize) {
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
  }*/

  /**
   * @param bestScores
   * @see #setAtBinSize(List, long)
   */
 /* private void addBestScores(List<BestScore> bestScores, long binSize) {
    addTimeAndScore(new TimeAndScore(bestScores, binSize));
  }*/

/*
  private void addTimeAndScore(TimeAndScore ts) {
    timeAndScores.add(ts);
  }
*/
  private int getTotal() {
    return getTotal(timeAndScores);
  }

  private int getTotal(Collection<TimeAndScore> timeAndScores) {
    logger.info("getTotal " + timeAndScores.size());

    int total = 0;
    for (TimeAndScore ts : timeAndScores) {
      //logger.info("got " + ts);
      total += ts.getCount();
    }
    return total;
  }

  /**
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot(LangTestDatabaseAsync, long)
   * @return
   * @see #getRawAverage()
   */
  public int getRawTotal() {
    logger.info("Raw total " + rawTimeAndScores.size());

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

/*  private void setMoving() {
    float total = 0;
    float count = 0;
    for (TimeAndScore ts : getTimeAndScores()) {
      total += ts.getScore() * ts.getCount();
      count += ts.getCount();
      float moving = total / count;
      ts.setMoving(moving);
    }
  }*/

/*  public String toCSV() {
    setMoving();
    StringBuilder builder = new StringBuilder();
    builder.append(",,,," + userID + "," + getTotal() + "," + getAverage());
    for (TimeAndScore ts : getTimeAndScores()) builder.append("\n").append(ts.toCSV());
    return builder.toString();
  }*/

  public String toRawCSV() {
    StringBuilder builder = new StringBuilder();
    builder.append(",,,," + userID + "," + getRawTotal() + "," + getRawAverage());
    for (TimeAndScore ts : getRawBestScores()) builder.append("\n").append(ts.toCSV());
    return builder.toString();
  }

  /**
   * @see #UserPerformance()
   * @param rawBestScores
   */
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

      TimeAndScore e = new TimeAndScore(bs, moving);
      rawTimeAndScores.add(e);
      if (bs.isiPad()) {
        iPadTimeAndScores.add(e);
      } else {
        getBrowserTimeAndScores().add(e);
      }
    }
  }

  public List<TimeAndScore> getRawBestScores() {
    return rawTimeAndScores;
  }
  public List<TimeAndScore> getiPadTimeAndScores() {
    return iPadTimeAndScores;
  }
  public List<TimeAndScore> getBrowserTimeAndScores() {
    return browserTimeAndScores;
  }
}