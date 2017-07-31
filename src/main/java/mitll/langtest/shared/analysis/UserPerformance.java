/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.analysis.Analysis;

import java.io.Serializable;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/19/15.
 */

public class UserPerformance implements Serializable {
  //  private transient final Logger logger = LogManager.getLogger("UserPerformance");
  private static final int TOSHOW = 2;
  /**
   * CAN'T be final
   */
  private List<TimeAndScore> rawTimeAndScores = new ArrayList<>();
  private List<TimeAndScore> iPadTimeAndScores = new ArrayList<>();
  private List<TimeAndScore> learnTimeAndScores = new ArrayList<>();
  private List<TimeAndScore> avpTimeAndScores = new ArrayList<>();
  private Map<Long, List<PhoneSession>> granularityToSessions;

  private long userID;
  private String first = "";
  private String last = "";

  public UserPerformance() {
  }

  /**
   * @param userID
   * @see mitll.langtest.server.database.analysis.H2Analysis#getPerformanceForUser
   */
  public UserPerformance(long userID) {
    this.userID = userID;
  }

  /**
   * @param userID
   * @param resultsForQuery
   * @see Analysis#getUserPerformance(long, Map)
   */
  public UserPerformance(long userID, List<BestScore> resultsForQuery, String first, String last) {
    this(userID);
    setRawBestScores(resultsForQuery);
    this.first = first;
    this.last = last;
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
    int rawTotal = getRawTotal();
    return rawTotal == 0 ? 0 : (total / (float) rawTotal);
  }

  private float getTotalScore(Collection<TimeAndScore> timeAndScores) {
    float total = 0;
    for (TimeAndScore ts : timeAndScores) total += ts.getScore();// * ts.getCount();
    return total;
  }

  private long getUserID() {
    return userID;
  }

/*  public String toRawCSV() {
    StringBuilder builder = new StringBuilder();
    builder.append(",,,," + userID + "," + getRawTotal() + "," + getRawAverage());
    for (TimeAndScore ts : getRawBestScores()) builder.append("\n").append(ts.toCSV());
    return builder.toString();
  }*/

  /**
   * @param rawBestScores
   * @see #UserPerformance()
   */
  private void setRawBestScores(List<BestScore> rawBestScores) {
    // TODO : necessary to sort????
    rawBestScores.sort(Comparator.comparingLong(SimpleTimeAndScore::getTimestamp));

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
        } else {
          learnTimeAndScores.add(timeAndScore);
        }
      }
    }
  }

  public List<TimeAndScore> getRawBestScores() {
    return rawTimeAndScores;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#getChart(String, String, String, UserPerformance)
   */
  public List<TimeAndScore> getiPadTimeAndScores() {
    return iPadTimeAndScores;
  }

  public List<TimeAndScore> getLearnTimeAndScores() {
    return learnTimeAndScores;
  }

  public List<TimeAndScore> getAvpTimeAndScores() {
    return avpTimeAndScores;
  }

  public Map<Long, List<PhoneSession>> getGranularityToSessions() {
    return granularityToSessions;
  }

  /**
   * @param granularityToSessions
   * @see Analysis#getUserPerformance(long, Map)
   */
  public void setGranularityToSessions(Map<Long, List<PhoneSession>> granularityToSessions) {
    this.granularityToSessions = granularityToSessions;
  }

  /**
   * @see mitll.langtest.client.analysis.AnalysisPlot#addChart
   * @return
   */
  public String getFirst() {
    return first;
  }

  /**
   * @see mitll.langtest.client.analysis.AnalysisPlot#addChart
   * @return
   */
  public String getLast() {
    return last;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("User " + getUserID() + " " + getRawTotal() + " items, avg score " + getRawAverage());
    int count = 0;
    List<TimeAndScore> rawBestScores = getRawBestScores();

    builder.append("\n\tscores = " + rawBestScores.size());

    for (TimeAndScore ts : rawBestScores) {
      builder.append("\n").append(ts);
      if (count++ > TOSHOW) break;
    }
    return builder.toString();
  }
}