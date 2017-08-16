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

package mitll.langtest.server.database.result;

import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.answer.AudioType;

import java.beans.Transient;
import java.util.Date;

/**
 * Only server side.
 */
public class Result extends SlimResult implements UserAndTime {
  protected final int uniqueID;
  private int userid;

  private int exid;
  @Deprecated  private String oldExID;
  private final int qid;
  private final String answer;
  private final long timestamp;

  private AudioType audioType = AudioType.UNSET;
  private final long durationInMillis;
  private final boolean correct;

  private String deviceType;
  private String device;

  private long processDur;
  private final long roundTrip;
  private final boolean withFlash;
  private final float dynamicRange;
  private String validity;
  private boolean isMale;

  private final String model;

  private DecodeAlignOutput alignOutput;
  private DecodeAlignOutput decodeOutput;

  /**
   * @param userid
   * @param exid
   * @param answer
   * @param valid
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @param correct
   * @param pronScore
   * @param device
   * @param deviceType
   * @param processDur
   * @param roundTripDur
   * @param withFlash
   * @param dynamicRange
   * @param model
   * @paramx answerType
   * @paramx plan
   * @see ResultDAO#getResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   */
  public Result(int uniqueID,
                int userid,
                int exid,
                int qid,
                String answer,
                boolean valid,
                long timestamp,
                AudioType audioType,
                long durationInMillis, boolean correct, float pronScore, String device,
                String deviceType,
                long processDur, long roundTripDur, boolean withFlash, float dynamicRange,
                String validity, String model) {
    super(-1, valid, "", pronScore);
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.exid = exid;
    this.qid = qid;
    this.answer = answer;
    this.timestamp = timestamp;
    this.audioType = audioType;
    this.durationInMillis = durationInMillis;
    this.correct = correct;
    this.device = device;
    this.deviceType = deviceType;
    this.processDur = processDur;
    this.roundTrip = roundTripDur;
    this.withFlash = withFlash;
    this.dynamicRange = dynamicRange;
    this.validity = validity;
    this.model = model;
  }

  /**
   * Compound key of exercise id and question id within that exercise.
   *
   * @return
   */
  public String getID() {
    return getCompoundID();
  }

  /**
   * Compound key of exercise id and question id within that exercise.
   *
   * @return
   */
  @Transient
  public String getCompoundID() {
    return getExid() + "/" + qid;
  }

  public AudioType getAudioType() {
    return audioType;
  }

  public boolean isCorrect() {
    return correct;
  }

  public int getUserid() {
    return userid;
  }

  public int getUniqueID() {
    return uniqueID;
  }

  /**
   * TODO : make sure this is set
   *
   * @return
   */
  public int getQid() {
    return qid;
  }

  public String getAnswer() {
    return answer;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getExerciseID() {
    return exid;
  }

  @Override
  public int getExid() {
    return exid;
  }

  public long getDurationInMillis() {    return durationInMillis;  }



  public void setUserID(Integer userID) {
    this.userid = userID;
  }

  public long getProcessDur() {
    return processDur;
  }

  public void setProcessDur(long processDur) {
    this.processDur = processDur;
  }

  public long getRoundTrip() {
    return roundTrip;
  }

  public String getDeviceType() {
    return deviceType;
  }

  public void setDeviceType(String deviceType) {
    this.deviceType = deviceType;
  }

  public String getDevice() {
    return device;
  }

  public void setDevice(String device) {
    this.device = device;
  }

  public boolean isWithFlash() {
    return withFlash;
  }

  public float getDynamicRange() {
    return dynamicRange;
  }

  public String getValidity() {
    return validity;
  }

  public void setValidity(String validity) {
    this.validity = validity;
  }

  public DecodeAlignOutput getAlignOutput() {
    return alignOutput;
  }

  public void setAlignOutput(DecodeAlignOutput alignOutput) {
    this.alignOutput = alignOutput;
  }

  public DecodeAlignOutput getDecodeOutput() {
    return decodeOutput;
  }

  public void setDecodeOutput(DecodeAlignOutput decodeOutput) {
    this.decodeOutput = decodeOutput;
  }

  public boolean isMale() {
    return isMale;
  }

  public void setMale(boolean male) {
    isMale = male;
  }

  public String getOldExID() {
    return oldExID;
  }

  public void setOldExID(String oldExID) {
    this.oldExID = oldExID;
  }

  public void setExid(int exid) {
    this.exid = exid;
  }

  public String getModel() {
    return model;
  }


  @Override
  public String toString() {
    return "Result #" + getUniqueID() + "\t\tby user " + getUserid() + "\texid " + getExid() + " " +
        " at " + new Date(getTimestamp()) +
        "  ans " + getAnswer() +
        " audioType : " + getAudioType() + " device " + device +
        " valid " + isValid() + " " + (isCorrect() ? "correct" : "incorrect") + " score " + getPronScore();
  }
}
