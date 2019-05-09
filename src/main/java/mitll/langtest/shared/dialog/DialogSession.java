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

package mitll.langtest.shared.dialog;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class DialogSession implements IDialogSession {
  private int id = -1;
  private int userid =-1;
  private int projid = -1;
  private int dialogid = -1;
  private long modified = 0L, end = 0L;

 // private INavigation.VIEWS views = INavigation.VIEWS.STUDY;
  private INavigation.VIEWS views = INavigation.VIEWS.LISTEN;
  private DialogStatus status = DialogStatus.DEFAULT;
  private int numRecordings = 0;

  private float score = 0F;
  private float speakingRate = 0F;

  public DialogSession() {
  }

  public DialogSession(int userid, int projid, int dialogid, INavigation.VIEWS views) {
    this.userid = userid;
    this.projid = projid;
    this.dialogid = dialogid;
    this.views = views;
  }

  public DialogSession(int id, int userid, int projid, int dialogid, long modified, long end, INavigation.VIEWS views,
                       DialogStatus status, int numRecordings, float score, float speakingRate) {
    this(userid, projid, dialogid, views);
    this.id = id;
    this.modified = modified;
    this.end = end;
    this.status = status;
    this.numRecordings = numRecordings;
    this.score = score;
    this.speakingRate = speakingRate;
  }

  @Override
  public int getUserid() {
    return userid;
  }

  @Override
  public int getProjid() {
    return projid;
  }

  @Override
  public int getDialogid() {
    return dialogid;
  }

  @Override
  public long getModified() {
    return modified;
  }

  @Override
  public long getEnd() {
    return end;
  }

  @Override
  public INavigation.VIEWS getView() {
    return views;
  }

  @Override
  public DialogStatus getStatus() {
    return status;
  }

  @Override
  public float getScore() {
    return score;
  }

  @Override
  public float getSpeakingRate() {
    return speakingRate;
  }

  @Override
  public int getID() {
    return id;
  }

  public DialogSession setID(int id) {
    this.id = id;
    return this;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(id, o.getID());
  }

  @Override
  public int getNumRecordings() {
    return numRecordings;
  }

  public String toString() {
    return "DialogSession #" + getID() +
        "\n\tdialog " + getDialogid() +
        "\n\tby   " + getUserid() +
        "\n\tview " + getView() +
        "\n\tat   " + new Date(getModified());
  }
}
