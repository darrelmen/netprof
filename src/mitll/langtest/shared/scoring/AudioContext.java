package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * reqid               request id from the client, so it can potentially throw away out of order responses
 * user                who is answering the question
 * id            exercise within the plan
 * questionID          question within the exercise
 * audioType           regular or fast then slow audio recording
 * Created by go22670 on 2/26/16.
 */
public class AudioContext implements IsSerializable {
  /**
   * request id from the client, so it can potentially throw away out of order responses
   */
  private int reqid;

  /**
   * who is answering the question
   */
  private int userid;

  /**
   * exercise id
   */
  private String id;
  /**
   * question within the exercise
   */
  private int questionID;
  /**
   * regular or fast then slow audio recording
   */
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
