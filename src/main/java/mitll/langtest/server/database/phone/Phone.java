package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.DatabaseServices;
import mitll.npdata.dao.SlickPhone;

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
   * @see DatabaseServices#recordWordAndPhoneInfo
   * @see SlickPhoneDAO#fromSlick(SlickPhone)
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
    return " proj " + projid +
        " rid " + rid + " wid " + wid + " : " + phone + " at " + seq + " score " + score;
  }
}
