/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.beans.Transient;
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
  private int userid;
  private String plan;
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
  public Result(int uniqueID,
                int userid, String plan, String id,
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

  /**
   * Compound key of exercise id and question id within that exercise.
   * @return
   */
  @Transient
  public String getCompoundID() {  return getExerciseID() + "/" + qid;  }

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

/*
  private void setUniqueID(int uniqueID) {
    this.uniqueID = uniqueID;
  }
*/

  public int getUserid() {
    return userid;
  }

  public String getExerciseID() { return id;  }

  /**
   * TODO : make sure this is set
   * @return
   */
  public int getQid() {   return qid;  }

  public String getAnswer() {
    return answer;
  }

  public boolean isValid() {
    return valid;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String getExid() {
    return id;
  }

  public int getDurationInMillis() {
    return durationInMillis;
  }

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

/*  private void setUserid(long userid) {
    this.userid = userid;
  }

  private void setPlan(String plan) {
    this.plan = plan;
  }

  private void setExerciseID(String id) {
    this.id = id;
  }

  private void setQid(int qid) {
    this.qid = qid;
  }

  private void setAnswer(String answer) {
    this.answer = answer;
  }

  private void setValid(boolean valid) {
    this.valid = valid;
  }

  private void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  private void setAudioType(String audioType) {
    this.audioType = audioType;
  }

  private void setDurationInMillis(int durationInMillis) {
    this.durationInMillis = durationInMillis;
  }

  private void setCorrect(boolean correct) {
    this.correct = correct;
  }

  private void setPronScore(float pronScore) {
    this.pronScore = pronScore;
  }

  private void setDevice(String device) {
    this.device = device;
  }*/
  public void setUserID(Integer userID) {
    this.userid = userID;
  }
}
