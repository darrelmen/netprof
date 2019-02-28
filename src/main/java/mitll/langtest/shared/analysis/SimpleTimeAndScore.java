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

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

public class SimpleTimeAndScore implements Serializable {
  public static final int SCALE = 1000;
  private long sessionStart;
  private int sessionSize = -1;
  private long timestamp;
  private int score;
  private transient WordAndScore wordAndScore;

  /**
   * @param timestamp
   * @param score
   * @param sessionSize
   * @see TimeAndScore#TimeAndScore(int, long, float, float, WordAndScore, long, int)
   */
  SimpleTimeAndScore(long timestamp, float score, WordAndScore wordAndScore, long sessionStart, int sessionSize) {
    this.timestamp = timestamp;
    this.score = toInt(score);
    this.wordAndScore = wordAndScore;
    this.sessionStart = sessionStart;
    this.sessionSize = sessionSize;
  }

  /**
   * @param timestamp
   * @param score
   * @param sessionSize
   * @see BestScore#BestScore
   */
  SimpleTimeAndScore(long timestamp, float score, long sessionStart, int sessionSize) {
    this.timestamp = timestamp;
    this.score = toInt(score);
    this.wordAndScore = null;
    this.sessionStart = sessionStart;
    this.sessionSize = sessionSize;
  }

  public SimpleTimeAndScore() {
  }

  public WordAndScore getWordAndScore() {
    return wordAndScore;
  }

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @return
   */
  public float getScore() {
    return fromInt(score);
  }

  protected int toInt(float value) {
    return (int) (value * SCALE);
  }

  float fromInt(int value) {
    return ((float) value) / SCALE;
  }

  private  String getTimeString() {
    return "" + getTimestamp();
  }

  @Override
  public boolean equals(Object obj) {
    SimpleTimeAndScore other = (SimpleTimeAndScore) obj;
    return getTimestamp() == other.getTimestamp() && getScore() == other.getScore();
  }

  /**
   * Overkill - can't have more than on response at the same time.
   *
   * @param o
   * @return
   */
  public int compareTo(@NotNull SimpleTimeAndScore o) {
    int compare = Long.compare(getTimestamp(), o.getTimestamp());
    return compare == 0 ? Float.compare(getScore(), o.getScore()) : compare;
  }

  public long getSessionStart() {
    return sessionStart;
  }

  public int getSessionSize() {
    return sessionSize;
  }

  public String toString() {
    return "at" +
        "\t" + getTimeString() + " " + new Date(getSessionStart()) +
        "\tn " + getSessionSize() +
        "\tavg score \t" + getScore();
  }
}