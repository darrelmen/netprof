/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import mitll.langtest.shared.User;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by go22670 on 10/29/15.
 */
public class UserInfo implements Serializable {
  private User user;
  private int start;
  private int current;
  private int num;
  private transient List<BestScore> bestScores;
  private long startTime;

  public UserInfo() {
  }

  /**
   * @param bestScores
   * @param initialSamples
   * @see mitll.langtest.server.database.analysis.Analysis#getBestForQuery
   */
  public UserInfo(List<BestScore> bestScores, long startTime, int initialSamples) {
    this.bestScores = bestScores;
    this.num = bestScores.size();
    this.startTime = startTime;

    Collections.sort(bestScores, new Comparator<BestScore>() {
      @Override
      public int compare(BestScore o1, BestScore o2) {
        return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
      }
    });

    List<BestScore> bestScores1 = bestScores.subList(0, Math.min(initialSamples, bestScores.size()));
    float total = 0;

    for (BestScore bs : bestScores1) {
      total += bs.getScore();
    }

    setStart(toPercent(total, bestScores1.size()));
    //  logger.info("start " + total + " " + start);
    total = 0;
    for (BestScore bs : bestScores) {
      total += bs.getScore();
    }
    setCurrent(toPercent(total, bestScores.size()));
    //  logger.info("current " + total + " " + current);
  }

  private static int toPercent(float total, float size) {
    return (int) Math.ceil(100 * total / size);
  }

  public String getUserID() {
    return getUser() == null ? "UNK" : getUser().getUserID();
  }

  public long getTimestampMillis() {
    return startTime;
//    return getUser().getTimestampMillis();
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public int getStart() {
    return start;
  }

  public int getCurrent() {
    return current;
  }

  public int getDiff() {
    return getCurrent() - getStart();
  }

  public int getNum() {
    return num;
  }

  public List<BestScore> getBestScores() {
    return bestScores;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public void setCurrent(int current) {
    this.current = current;
  }

  public String toString() {
    User user = getUser();
    String id = user == null ? "UNK" : "" + user.getId();
    return id + "/" + getUserID() + " : " + getNum() + " : " + getStart() + " " + getCurrent() + " " + getDiff();
  }
}
