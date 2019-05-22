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

package mitll.langtest.shared.exercise;

import org.jetbrains.annotations.NotNull;

public class OOV implements HasID {
  private int id;
  private int userid;
  private long modified;
  private String oov;
  private String equivalent = "";
  private boolean dirty;

  public OOV() {
  }

  public OOV(int id, int userid, long modified, String oov, String equivalent) {
    this.id = id;
    this.userid = userid;
    this.modified = modified;
    this.oov = oov;
    this.equivalent = equivalent;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    if (o instanceof OOV) {
      OOV o1 = (OOV) o;
      int i = getOOV().compareTo(o1.getOOV());
      return i == 0 ? getEquivalent().compareTo(o1.getEquivalent()) : i;
    } else {
      return Integer.compare(getID(), o.getID());
    }
  }

  @Override
  public int getID() {
    return id;
  }

  public int getUserid() {
    return userid;
  }

  public long getModified() {
    return modified;
  }

  public String getOOV() {
    return oov;
  }

  public String getEquivalent() {
    return equivalent;
  }

  /**
   * @see mitll.langtest.client.banner.OOVViewHelper#gotBlur
   * @param text
   */
  public void setEquivalent(String text) {
    dirty = !text.equals(equivalent);
    this.equivalent = text;
  }

  public boolean isDirty() {
    return dirty;
  }

  public String toString() {
    return id + " : " + getOOV() + " = " + getEquivalent();
  }
}
