package mitll.langtest.server.database.phone;

/**
 * Created by go22670 on 3/29/16.
 */
public class Phone {
  private Long id;

  private long wid;
  private long rid;
  private String phone;
  private int seq;
  private float score;
  public Phone() {}

  public Phone(long rid, long wid, String phone, int seq, float score) {
    this.rid = rid;
    this.wid = wid;
    this.phone = phone;
    this.seq = seq;
    this.score = score;
  }

  public Long getId() {
    return id;
  }
//  private void setId(Long id) {
//    this.id = id;
//  }

  public String toString() {
    return // "# " + id +
        " rid " + rid + " wid " + wid + " : " + phone + " at " + seq + " score " + score;
  }

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
}
