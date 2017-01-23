/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared;

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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/18/12
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Result implements UserAndTime {
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
  private transient String model;

  public static final String AUDIO_TYPE_UNSET = "unset";

  /**
   *
   */
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
                String answerType, int durationInMillis, boolean correct, float pronScore, String device,
                String model) {
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
    this.model = model;
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
  public String getModel() {
    return model;
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
    return "Result #" + getUniqueID() + " user " + getUserid() + " exid " + getExerciseID() + " " +
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

  public void setUserID(Long userID) {
    this.userid = userID;
  }
}
