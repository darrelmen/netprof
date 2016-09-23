package mitll.langtest.server.database.phone;

import mitll.npdata.dao.SlickPhone;

import java.util.Map;

/**
 * Created by go22670 on 3/29/16.
 */
public class Phone {
  private int duration;
  // private Long id;

  private long wid;
  private long rid;
  private String phone;
  private int seq;
  private float score;
  public Phone() {}

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#recordWordAndPhoneInfo(long, Map)
   * @see SlickPhoneDAO#fromSlick(SlickPhone)
   * @param rid
   * @param wid
   * @param phone
   * @param seq
   * @param score
   * @param duration
   */
  public Phone(long rid, long wid, String phone, int seq, float score, int duration) {
    this.rid = rid;
    this.wid = wid;
    this.phone = phone;
    this.seq = seq;
    this.score = score;
    this.duration = duration;
  }

//  public Long getId() {
//    return id;
//  }
////  private void setId(Long id) {
////    this.id = id;
////  }

  public long getWid() {
    return wid;
  }

  public long getRid() {
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

  public void setWID(Integer WID) { this.wid = WID;  }

  public int getDuration() {
    return duration;
  }

  public String toString() {
    return // "# " + id +
        " rid " + rid + " wid " + wid + " : " + phone + " at " + seq + " score " + score;
  }
}
