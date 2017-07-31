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

import java.io.Serializable;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/19/15.
 *
 * @see mitll.langtest.client.analysis.AnalysisPlot#addSeries
 */
public class SimpleTimeAndScore implements Serializable {
  private static final int SCALE = 1000;
  private long timestamp;
  private int score;
  private transient WordAndScore wordAndScore;

  /**
   * @param timestamp
   * @param score
   * @seex PhoneDAO#getPhoneTimeSeries
   */
  SimpleTimeAndScore(long timestamp, float score, WordAndScore wordAndScore) {
    this.timestamp = timestamp;
    this.score = toInt(score);
    this.wordAndScore = wordAndScore;
  }

  /**
   * @see BestScore#BestScore
   * @param timestamp
   * @param score
   */
  SimpleTimeAndScore( long timestamp, float score) {
    this.timestamp = timestamp;
    this.score = toInt(score);
    this.wordAndScore = null;
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
   * TODO : make this an int
   *
   * @return
   */
  public float getScore() {
    return fromInt(score);
  }

  protected int toInt(float value) {
    return (int)(value* SCALE);
  }

  protected float fromInt(int value) {
    return ((float)value)/ SCALE;
  }

  protected String getTimeString() {
    return "" + getTimestamp();
  }

  public String toString() {
    return "at\t" + getTimeString() + " avg score for " +
        //childCount + "\t" +
        "=\t" + score;
  }

}