/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.shared.analysis;

import java.util.List;

public class TimeAndScore extends SimpleTimeAndScore implements Comparable<SimpleTimeAndScore> {
  private int exid;
  private int cumulativeAverage;

  /**
   * @param bs
   * @param cumulativeAverage
   * @see UserPerformance#setRawBestScores
   */
  TimeAndScore(BestScore bs, float cumulativeAverage) {
    this(bs.getExId(), bs.getTimestamp(), bs.getScore(), cumulativeAverage, null, bs.getSessionStart(), bs.getSessionSize());
  }

  /**
   * @param exid
   * @param timestamp
   * @param score
   * @param cumulativeAverage
   * @param sessionSize
   * @see mitll.langtest.server.database.phone.MakePhoneReport#getPhoneTimeSeries(List)
   */
  public TimeAndScore(int exid, long timestamp, float score, float cumulativeAverage, WordAndScore wordAndScore,
                      long sessionStart, int sessionSize) {
    super(timestamp, score, wordAndScore, sessionStart, sessionSize);
    this.exid = exid;
    this.cumulativeAverage = toInt(cumulativeAverage);
  }

  public TimeAndScore(long timestamp) {
    super(timestamp, 0f, null, 0L, 0);
  }

  public TimeAndScore() {
  }

  @Override
  public int compareTo(SimpleTimeAndScore o) {
    return Long.compare(getTimestamp(), o.getTimestamp());
  }

  public float getCumulativeAverage() {
    return fromInt(cumulativeAverage);
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores(List)
   */
  public int getExid() {
    return exid;
  }

  public String toString() {
 //   String format = getTimeString();
    return exid + "\t" +super.toString()+
        "\tcumulative avg " + getCumulativeAverage();
  }
}