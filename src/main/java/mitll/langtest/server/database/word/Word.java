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

package mitll.langtest.server.database.word;

import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.phone.Phone;

import java.util.List;

/**
 * Created by go22670 on 3/29/16.
 */
public class Word {
  private int id;
  private int projid;
  private int rid;

  private String word;
  private int seq;
  private float score;
  private List<Phone> phones;

  public Word() {
  }

  /**
   * @param id
   * @param projid
   * @param rid
   * @param word
   * @param seq
   * @param score
   * @see WordDAO#getWords
   * @see SlickWordDAO#fromSlick
   */
  public Word(int id, int projid, int rid, String word, int seq, float score) {
    this(projid, rid, word, seq, score);
    this.id = id;
  }

  /**
   * @param projid
   * @param rid
   * @param word
   * @param seq
   * @param score
   * @see DatabaseServices#recordWordAndPhoneInfo
   */
  public Word(int projid, int rid, String word, int seq, float score) {
    this.projid = projid;
    this.rid = rid;
    this.word = word;
    this.seq = seq;
    this.score = score;
  }

  public int getId() {
    return id;
  }

  public int getRid() {
    return rid;
  }

  public String getWord() {
    return word;
  }

  public int getSeq() {
    return seq;
  }

  public float getScore() {
    return score;
  }

  public void setRid(int rid) {
    this.rid = rid;
  }

  public int getProjid() {
    return projid;
  }


  public List<Phone> getPhones() {
    return phones;
  }

  public void setPhones(List<Phone> phones) {
    this.phones = phones;
  }

  public String toString() {
    return "# " + id + " proj " + projid +
        " rid " + rid + " '" + word + "' at " + seq + " score " + score;
  }
}
