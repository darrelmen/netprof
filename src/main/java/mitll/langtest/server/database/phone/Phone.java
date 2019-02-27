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

package mitll.langtest.server.database.phone;

/**
 * Created by go22670 on 3/29/16.
 */
public class Phone {
  private int projid;

  private int duration;

  private int wid;
  private int rid;
  private String phone;
  private int seq;
  private float score;

  public Phone() {
  }

  /**
   * @param projid
   * @param rid
   * @param wid
   * @param phone
   * @param seq
   * @param score
   * @param duration
   * @see RecordWordAndPhone#recordWordAndPhoneInfo
   * @seex SlickPhoneDAO#fromSlick
   */
  public Phone(int projid, int rid, int wid, String phone, int seq, float score, int duration) {
    this.projid = projid;
    this.rid = rid;
    this.wid = wid;
    this.phone = phone;
    this.seq = seq;
    this.score = score;
    this.duration = duration;
  }

  public int getProjid() {
    return projid;
  }

  public int getWid() {
    return wid;
  }

  public int getRid() {
    return rid;
  }

  public String getPhone() {
    return phone;
  }

  public int getSeq() {
    return seq;
  }

  public float getScore() {
    return score;
  }

  public void setRID(Integer RID) {
    this.rid = RID;
  }

  public void setWID(Integer WID) {
    this.wid = WID;
  }

  public int getDuration() {
    return duration;
  }

  public String toString() {
    return "proj " + projid +
        " rid " + rid + " wid " + wid + " '" + phone + "' at " + seq + " score " + score;
  }
}
