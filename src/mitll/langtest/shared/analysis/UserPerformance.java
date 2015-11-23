package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.analysis.Analysis;

import java.io.Serializable;
import java.util.*;

/**
 * Created by go22670 on 10/19/15.
 */

public class UserPerformance implements Serializable {
  //  private transient final Logger logger = Logger.getLogger("UserPerformance");
  private static final int TOSHOW = 2;
  private List<TimeAndScore> rawTimeAndScores = new ArrayList<>();
  private List<TimeAndScore> iPadTimeAndScores = new ArrayList<>();
  private List<TimeAndScore> learnTimeAndScores = new ArrayList<>();
  private List<TimeAndScore> avpTimeAndScores   = new ArrayList<>();
  private Map<Long, List<PhoneSession>> granularityToSessions;

  private long userID;

  public UserPerformance() {}

  /**
   * @param userID
   * @see Analysis#getPerformanceForUser
   */
  public UserPerformance(long userID) {
    this.userID = userID;
  }

  /**
   * @param userID
   * @param resultsForQuery
   * @see Analysis#getPerformanceForUser(long, int)
   */
  public UserPerformance(long userID, List<BestScore> resultsForQuery) {
    this(userID);
    setRawBestScores(resultsForQuery);
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   * @see #getRawAverage()
   */
  private int getRawTotal() {
    return rawTimeAndScores.size();
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  public float getRawAverage() {
    float total = getTotalScore(rawTimeAndScores);
    return total / (float) getRawTotal();
  }

  private float getTotalScore(Collection<TimeAndScore> timeAndScores) {
    float total = 0;
    for (TimeAndScore ts : timeAndScores) total += ts.getScore();// * ts.getCount();
    return total;
  }

  private long getUserID() {
    return userID;
  }

  public String toRawCSV() {
    StringBuilder builder = new StringBuilder();
    builder.append(",,,," + userID + "," + getRawTotal() + "," + getRawAverage());
    for (TimeAndScore ts : getRawBestScores()) builder.append("\n").append(ts.toCSV());
    return builder.toString();
  }

  /**
   * @param rawBestScores
   * @see #UserPerformance()
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

      TimeAndScore timeAndScore = new TimeAndScore(bs, moving);
      rawTimeAndScores.add(timeAndScore);
      if (bs.isiPad()) {
        iPadTimeAndScores.add(timeAndScore);
      } else {
        if (bs.isFlashcard()) {
          avpTimeAndScores.add(timeAndScore);
        }
        else {
          learnTimeAndScores.add(timeAndScore);
        }
      }
    }
  }

  public List<TimeAndScore> getRawBestScores() {
    return rawTimeAndScores;
  }

  /**
   * @see mitll.langtest.client.analysis.AnalysisPlot#getChart(String, String, String, UserPerformance)
   * @return
   */
  public List<TimeAndScore> getiPadTimeAndScores()  { return iPadTimeAndScores;  }
  public List<TimeAndScore> getLearnTimeAndScores() {
    return learnTimeAndScores;
  }
  public List<TimeAndScore> getAvpTimeAndScores()   { return avpTimeAndScores;   }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("User " + getUserID() + " " + getRawTotal() + " items, avg score " + getRawAverage() );
    int count = 0;
    for (TimeAndScore ts : getRawBestScores()) {
      builder.append("\n").append(ts);
      if (count++ > TOSHOW) break;
    }
    return builder.toString();
  }

  public Map<Long, List<PhoneSession>> getGranularityToSessions() {
    return granularityToSessions;
  }

  /**
   * @see Analysis#getPerformanceForUser(long, int)
   * @param granularityToSessions
   */
  public void setGranularityToSessions(Map<Long, List<PhoneSession>> granularityToSessions) {
    this.granularityToSessions = granularityToSessions;
  }
}