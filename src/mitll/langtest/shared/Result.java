package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * An answer to a question. <br></br>
 * Records who answered it, which plan, which exercise, which question within a multi-question exercise, and
 *  the answer, which may either be a) the text of a written response or b) a path to an audio file response
 * <br></br>
 * May be marked with whether the audio file was "valid" - long enough and not silence.<br></br>
 * Also records the timestamp, and optionally whether the result was to a fl/english and spoken/written question.
 * These may be added later via enrichment (joining) against the schedule, which says for a specific user, which
 * of these two flags was presented.
 *
 * User: go22670
 * Date: 5/18/12
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Result implements IsSerializable {
  //userid INT, plan VARCHAR, id VARCHAR, qid INT, answer VARCHAR, audioFile VARCHAR, valid BOOLEAN, timestamp
  public int uniqueID;
  public long userid;
  public String plan;
  public String id;
  public int qid;
  public String answer;
  public boolean valid;
  public long timestamp;
  public boolean flq;
  public boolean spoken;
  //private AudioType audioType; // so having another object in here seemed to slow down serialization a lot
  public String audioType;

  public static final String AUDIO_TYPE_UNSET = "unset";
  public static final String AUDIO_TYPE_REGULAR = "regular";
  public static final String AUDIO_TYPE_FAST_AND_SLOW = "fastAndSlow";

/*  public enum AudioType implements IsSerializable {
    UNSET,
    REGULAR,
    FAST_AND_SLOW;

    AudioType() {} // for gwt serialization
  }*/

  public Result() {}

  /**
   * @see mitll.langtest.server.database.ResultDAO#getResults()
   * @param userid
   * @param plan
   * @param id
   * @param qid
   * @param answer
   * @param valid
   * @param timestamp
   * @param flq
   * @param spoken
   * @param answerType
   */
  public Result(int uniqueID, long userid, String plan, String id, int qid, String answer,
                boolean valid, long timestamp, boolean flq, boolean spoken, String answerType) {
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.plan = plan;
    this.id = id;
    this.qid = qid;
    this.answer = answer;
    this.valid = valid;
    this.timestamp = timestamp;
    this.flq = flq;
    this.spoken = spoken;
    this.audioType = answerType == null || answerType.length() == 0 ? "unset" : answerType;
  }

  public void setFLQ(boolean flq)  { this.flq = flq; }
  public void setSpoken(boolean v) { this.spoken = v; }
 // public boolean isRegularAudio() { return audioType == null || audioType.equals(AUDIO_TYPE_UNSET) || audioType.equals(AUDIO_TYPE_REGULAR); }
  public boolean isFastAndSlowAudio() { return audioType != null && audioType.equals(AUDIO_TYPE_FAST_AND_SLOW); }

  @Override
  public String toString() {
    return "Result #"+uniqueID + "\t\tby user " + userid + "\texid " + id + " " + (flq ? "flq":"english") + " " + (spoken ? "spoken":"written") + " " + audioType;
  }
}
