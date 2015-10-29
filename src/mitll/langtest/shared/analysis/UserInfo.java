package mitll.langtest.shared.analysis;

import mitll.langtest.shared.User;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/29/15.
 */
public class UserInfo implements Serializable {
  private transient final Logger logger = Logger.getLogger("UserInfo");

  private User user;
  private int start;
  private int current;
//  private int diff;
  private int num;
  private transient List<BestScore> bestScores;

  public UserInfo() {
  }

  public UserInfo(List<BestScore> bestScores) {
    this.bestScores = bestScores;
    this.num = bestScores.size();
    Collections.sort(bestScores, new Comparator<BestScore>() {
      @Override
      public int compare(BestScore o1, BestScore o2) {
        return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
      }
    });
    List<BestScore> bestScores1 = bestScores.subList(0, Math.min(10, bestScores.size()));
    float total = 0;

    for (BestScore bs : bestScores1) {
//      logger.info("initial " + bs);
      total += bs.getScore();
    }

    start = toPercent(total,  bestScores1.size());
  //  logger.info("start " + total + " " + start);
    total = 0;
    for (BestScore bs : bestScores) {
      total += bs.getScore();
    }
    current = toPercent(total, bestScores.size());
  //  logger.info("current " + total + " " + current);

  }

  private static int toPercent(float total, float size) {
    return (int) Math.ceil(100 * total / size);
  }

  public String toString() {
    return getUser().getId() + "/"+getUserID() + " : "+ getNum() + " : " +getStart() + " " + getCurrent() + " " + getDiff();
  }

  public String getUserID() {
    return getUser().getUserID();
  }

  public long getTimestampMillis() {
    return getUser().getTimestampMillis();
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
    return getCurrent()-getStart();
  }

  public int getNum() {
    return num;
  }

  public List<BestScore> getBestScores() {
    return bestScores;
  }
}
