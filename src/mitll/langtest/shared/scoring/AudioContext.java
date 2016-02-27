package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 2/26/16.
 */
public class AudioContext  implements IsSerializable {
  private int reqid;

  private int userid;
  private String id;
  private int questionID;
  private String audioType;

  /**
   * @param userid
   * @param id
   * @param questionID
   * @param audioType
   * @see
   */
  public AudioContext(int reqid,
                      int userid,
                      String id,
                      int questionID,
                      String audioType) {
    this.reqid = reqid;
    this.userid = userid;
    this.id = id;
    this.questionID = questionID;
    this.audioType = audioType;
  }

  public AudioContext() {
  }

  public int getUserid() {
    return userid;
  }

  public String getId() {
    return id;
  }

  public int getQuestionID() {
    return questionID;
  }

  public int getReqid() {
    return reqid;
  }

  public String getAudioType() {
    return audioType;
  }

  public String toString() {
    return "user " + userid + " id " + id + " q " + questionID + " req " + reqid + " type " + audioType;
  }
}
