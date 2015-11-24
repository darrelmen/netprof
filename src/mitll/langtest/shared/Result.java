/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

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
public class Result implements IsSerializable, UserAndTime {
  private int uniqueID;
  private long userid;
  public String plan;
  private String id;
  private int qid;
  private String answer;
  private boolean valid;
  private long timestamp;

  private String audioType;
  private int durationInMillis;
  private boolean correct;
  private float pronScore;
  private String device;
  private transient String jsonScore;

  public static final String AUDIO_TYPE_UNSET = "unset";
  public static final String AUDIO_TYPE_REGULAR = "regular";
  public static final String AUDIO_TYPE_SLOW = "slow";
  public static final String AUDIO_TYPE_FAST_AND_SLOW = "fastAndSlow";
  public static final String AUDIO_TYPE_PRACTICE = "practice";
  public static final String AUDIO_TYPE_REVIEW = "review";
  public static final String AUDIO_TYPE_RECORDER = "recorder";

  public Result() {}

  /**
   * @see mitll.langtest.server.database.ResultDAO#getResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   * @param userid
   * @param plan
   * @param id
   * @param answer
   * @param valid
   * @param timestamp
   * @param answerType
   * @param durationInMillis
   * @param correct
   * @param pronScore
   * @param device
   */
  public Result(int uniqueID, long userid, String plan, String id,
                int qid,
                String answer,
                boolean valid, long timestamp,
                String answerType, int durationInMillis, boolean correct, float pronScore, String device) {
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.plan = plan;
    this.id = id;
    this.qid = qid;
    this.answer = answer;
    this.valid = valid;
    this.timestamp = timestamp;
    this.audioType = answerType == null || answerType.length() == 0 ? AUDIO_TYPE_UNSET : answerType;
    this.durationInMillis = durationInMillis;
    this.correct = correct;
    this.pronScore = pronScore;
    this.device = device;
  }

  /**
   * Compound key of exercise id and question id within that exercise.
   * @return
   */
  public String getID() {  return getExerciseID() + "/" + qid;  }

  public String getAudioType() {
    return audioType;
  }

  public boolean isCorrect() {
    return correct;
  }

  public float getPronScore() { return pronScore;  }

  public int getUniqueID() {
    return uniqueID;
  }

  public long getUserid() {
    return userid;
  }

  public String getExerciseID() { return id;  }

  public String getAnswer() {
    return answer;
  }

  public boolean isValid() {
    return valid;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getDurationInMillis() {
    return durationInMillis;
  }

/*
  public String getDevice() {
    return device;
  }
*/

  @Override
  public String toString() {
    return "Result #" + getUniqueID() + "\t\tby user " + getUserid() + "\texid " + getExerciseID() + " " +
        " at " + new Date(getTimestamp())+
        "  ans " + getAnswer() +
        " audioType : " + getAudioType() +" device " + device+
        " valid " + isValid() + " " + (isCorrect() ? "correct":"incorrect") + " score " + getPronScore();
  }

  public String getJsonScore() {
    return jsonScore;
  }

  public void setJsonScore(String jsonScore) {
    this.jsonScore = jsonScore;
  }
}
