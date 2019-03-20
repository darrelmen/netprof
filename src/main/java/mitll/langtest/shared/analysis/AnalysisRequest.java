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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import mitll.langtest.client.analysis.ReqCounter;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;

import java.io.Serializable;
import java.util.Date;

/**
 * @see mitll.langtest.client.analysis.AnalysisTab#AnalysisTab(ExerciseController, int, DivWidget, int, String, int, boolean, int, ReqCounter, INavigation.VIEWS)
 */
public class AnalysisRequest implements Serializable {
  private int userid = -1;
  private int dialogID = -1;
  private int dialogSessionID = -1;
  private int listid = -1;

  private int minRecordings = 0;
  private String phone = "";
  private String bigram = "";

  private long from = 1262304000000L; // 01/01/2010
  private long to = 1830297600000L;   // 01/01/2028

  private int reqid = -1;

  public AnalysisRequest() {}

  /**
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getSlickPerfResultsForDialog
   * @return
   */
  public int getListid() {
    return listid;
  }

  public String getPhone() {
    return phone;
  }

  public String getBigram() {
    return bigram;
  }

  public long getFrom() {
    return from;
  }

  public long getTo() {
    return to;
  }

  public int getReqid() {
    return reqid;
  }

  public int getMinRecordings() {
    return minRecordings;
  }

  public int getUserid() {
    return userid;
  }

  /**
   * @see mitll.langtest.client.analysis.AnalysisTab#AnalysisTab
   * @param userid
   * @return
   */
  public AnalysisRequest setUserid(int userid) {
    this.userid = userid;
    return this;
  }

  public int getDialogID() {
    return dialogID;
  }

  public AnalysisRequest setDialogID(int dialogID) {
    this.dialogID = dialogID;
    return this;
  }

  /**
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getSlickPerfResultsForDialog(AnalysisRequest)
   * @return
   */
  public int getDialogSessionID() {
    return dialogSessionID;
  }

  public AnalysisRequest setDialogSessionID(int dialogSessionID) {
    this.dialogSessionID = dialogSessionID;
    return this;
  }

  public AnalysisRequest setListid(int listid) {
    this.listid = listid;
    return this;
  }

  public AnalysisRequest setMinRecordings(int minRecordings) {
    this.minRecordings = minRecordings;
    return this;
  }

  public AnalysisRequest setPhone(String phone) {
    this.phone = phone;
    return this;
  }

  public AnalysisRequest setBigram(String bigram) {
    this.bigram = bigram;
    return this;
  }

  public AnalysisRequest setFrom(long from) {
    this.from = from;
    return this;
  }

  public AnalysisRequest setTo(long to) {
    this.to = to;
    return this;
  }

  public AnalysisRequest setReqid(int reqid) {
    this.reqid = reqid;
    return this;
  }

  public String toString() {
    return "Request " +
        "\n\tuser    " + userid +
        "\n\tdialog  " + dialogID +
        "\n\tsession " + dialogSessionID +
        "\n\tlist    " + listid +
        "\n\tminRecordings " + minRecordings +
        "\n\tphone   " + phone +
        "\n\tbigram  " + bigram +
        "\n\tfrom    " + from + " " + new Date(from) +
        "\n\tto      " + to + " " + new Date(to) +
        "\n\treqid   " + reqid;
  }
}
