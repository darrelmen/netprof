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

package mitll.langtest.shared.answer;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.autocrt.DecodeCorrectnessChecker;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.scoring.PretestScore;

public class AudioAnswer implements IsSerializable {
  private int reqid = -1;
  private String path = null;
  private Validity validity = Validity.INVALID;
  private String decodeOutput = "";
  private String transcript = "";
  private double score = -1;
  private boolean correct = false;
  private boolean saidAnswer = false;
  private long durationInMillis;
  private int resultID;
  private AudioAttribute audioAttribute;
  private PretestScore pretestScore;
  private double dynamicRange;
  private long timestamp;

  public double getDynamicRange() {
    return dynamicRange;
  }

  public void setDynamicRange(double dynamicRange) {
    this.dynamicRange = dynamicRange;
  }

  public AudioAnswer() {
  }

  /**
   * @param path
   * @param validity
   * @param reqid
   * @param duration
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   */
  public AudioAnswer(String path, Validity validity, int reqid, long duration) {
    this.path = path;
    this.validity = validity;
    this.reqid = reqid;
    this.durationInMillis = duration;
  }

  public void setDecodeOutput(String decodeOutput) {
    this.decodeOutput = decodeOutput;
  }

  /**
   * @param score
   * @see DecodeCorrectnessChecker#getDecodeScore
   */
  public void setScore(double score) {
    this.score = score;
  }

  /**
   * @return score from hydec (see nnscore)
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer, com.google.gwt.user.client.ui.Panel)
   */
  public double getScore() {
    return score;
  }

  /**
   * @return
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer, com.google.gwt.user.client.ui.Panel)
   */
  public boolean isCorrect() {
    return correct;
  }

  /**
   * @param correct
   * @see DecodeCorrectnessChecker#getDecodeScore
   */
  public void setCorrect(boolean correct) {
    this.correct = correct;
  }

  public int getResultID() {
    return resultID;
  }

  /**
   * @param resultID
   * @see mitll.langtest.server.audio.AudioFileHelper#writeAudioFile
   */
  public void setResultID(int resultID) {
    this.resultID = resultID;
  }

  public boolean isSaidAnswer() {
    return saidAnswer;
  }

  public void setSaidAnswer(boolean saidAnswer) {
    this.saidAnswer = saidAnswer;
  }

  public boolean isValid() {
    return validity == Validity.OK;
  }

  public int getReqid() {
    return reqid;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  /**
   * @return
   */
  public Validity getValidity() {
    return validity;
  }

  public void setValidity(Validity validity) {
    this.validity = validity;
  }

  public String getDecodeOutput() {
    return decodeOutput;
  }

  public long getDurationInMillis() {
    return durationInMillis;
  }

  /**
   * Audio information that is attached to the exercise.
   *
   * @return
   */
  public AudioAttribute getAudioAttribute() {
    return audioAttribute;
  }

  public void setAudioAttribute(AudioAttribute audioAttribute) {
    this.audioAttribute = audioAttribute;
  }

  public PretestScore getPretestScore() {
    return pretestScore;
  }

  /**
   * @param pretestScore
   * @see mitll.langtest.server.audio.AudioFileHelper#getAlignment
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   * @see mitll.langtest.server.ScoreServlet#getAudioAnswer
   */
  public void setPretestScore(PretestScore pretestScore) {
    this.pretestScore = pretestScore;
    this.score = pretestScore.getHydecScore();
  }

  public String getTranscript() {    return transcript;  }

  public void setTranscript(String transcript) {
    this.transcript = transcript;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String toString() {
    return "Answer id " + getResultID() +
        " : audio attr " + audioAttribute +
        "\n\tPath " + path +
        "\n\tduration " + durationInMillis +
        "\n\tid " + reqid +
        "\n\tvalidity " + validity +
        "\n\tcorrect " + correct +
        "\n\tscore " + score +
        "\n\tsaid answer " + saidAnswer +
        "\n\tpretest " + pretestScore +
        "\n\ttranscript " + transcript;
  }
}
