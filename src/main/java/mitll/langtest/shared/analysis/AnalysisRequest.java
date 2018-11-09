package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.Date;

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

  public int getUserid() {
    return userid;
  }

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
