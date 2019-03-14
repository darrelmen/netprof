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

import static mitll.langtest.shared.analysis.SimpleTimeAndScore.SCALE;

public class Bigram implements Serializable, Comparable<Bigram> {
  private int count = 0;
  private String bigram;
  private int score = 0;
  private transient float rawTotalScore = 0F;

  public Bigram() {
  }

  /**
   * @paramx bigram
   * @paramx count
   * @paramx score
   * @see mitll.langtest.server.database.phone.MakePhoneReport#getPhoneReport
   */
/*  public Bigram(String bigram, int count, float score) {
    this.count = count;
    this.bigram = bigram;
    this.score = score;
  }*/
  public Bigram(String bigram) {
    this.bigram = bigram;
  }

  public void increment(float newScore) {
    count++;
    rawTotalScore += newScore;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.BigramContainer#getPhoneStatuses
   */
  public int getCount() {
    return count;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.BigramContainer#getPhoneStatuses
   */
  public String getBigram() {
    return bigram;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.BigramContainer#getPhoneStatuses
   */
  public float getScore() {
    return fromInt(score);
  }

  public void setScore() {
    float avg = rawTotalScore / (float) count;
    this.score = (avg < 0) ? 0 : toInt(avg);
  }

  protected int toInt(float value) {
    return (int) (value * SCALE);
  }

  private float fromInt(int value) {
    return ((float) value) / SCALE;
  }


  @Override
  public int compareTo(@NotNull Bigram o) {
    return bigram.compareTo(o.bigram);
  }

  @Override
  public boolean equals(Object obj) {
    return bigram.equals(((Bigram) obj).bigram);
  }

  public String toString() {
    return bigram;// +" (" + count + ") = " + score;
  }
}
