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
  private void setId(Long id) {
    this.id = id;
  }

  public String toString() {
    return // "# " + id +
        " rid " + rid + " wid " + wid + " : " + phone + " at " + seq + " score " + score;
  }

  public long getWid() {
    return wid;
  }

//  @ManyToOne
//  @JoinColumn(name="result_id",foreignKey = @ForeignKey(name = "RESULT_ID_FK"))
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

/*
  private void setWid(long wid) {
    this.wid = wid;
  }

  private void setRid(long rid) {
    this.rid = rid;
  }

  private void setPhone(String phone) {
    this.phone = phone;
  }

  private void setSeq(int seq) {
    this.seq = seq;
  }

  private void setScore(float score) {
    this.score = score;
  }
*/
}
