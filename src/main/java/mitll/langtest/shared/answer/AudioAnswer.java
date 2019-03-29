/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.shared.answer;

import com.google.gwt.json.client.JSONObject;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.autocrt.DecodeCorrectnessChecker;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.PretestScore;

import java.io.File;

/**
 *
 */
public class AudioAnswer extends SimpleAudioAnswer {
  private int exid = -1;
  private int reqid = -1;
  private Validity validity = Validity.INVALID;
  private String decodeOutput = "";

  /**
   * Server side only...
   */
  private transient String transcript = "";

  private String normTranscript = "";
  private double score = -1;
  private boolean correct = false;
  private boolean saidAnswer = false;
  private long durationInMillis;

  private int resultID;
  private AudioAttribute audioAttribute;
  private double dynamicRange;
  private long timestamp;

  public AudioAnswer() {
  }

  /**
   * @param path
   * @param validity
   * @param reqid
   * @param duration
   * @param exid
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   */
  public AudioAnswer(String path, Validity validity, int reqid, long duration, int exid) {
    this.path = path;
    this.validity = validity;
    this.reqid = reqid;
    this.durationInMillis = duration;
    this.exid = exid;
  }

  /**
   * @return
   * @see mitll.langtest.client.exercise.RecordAudioPanel.MyWaveformPostAudioRecordButton#useResult
   */
  public double getDynamicRange() {
    return dynamicRange;
  }

  /**
   * @param dynamicRange
   * @see mitll.langtest.client.scoring.JSONAnswerParser#getAudioAnswer(JSONObject)
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswerDecoding(ClientExercise, AudioContext, AnswerInfo.RecordingInfo, String, File, AudioCheck.ValidityAndDur, DecoderOptions)
   */
  public AudioAnswer setDynamicRange(double dynamicRange) {
    this.dynamicRange = dynamicRange;
    return this;
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
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer)
   */
  public double getScore() {
    return score;
  }

  /**
   * @return
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer)
   */
  public boolean isCorrect() {
    return correct;
  }

  /**
   * @param correct
   * @see DecodeCorrectnessChecker#getDecodeScore
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
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

  /**
   * Not really used very much anymore ...
   * Maybe on iOS???
   *
   * @return
   */
  public boolean isSaidAnswer() {
    return saidAnswer;
  }

  public void setSaidAnswer(boolean saidAnswer) {
    this.saidAnswer = saidAnswer;
  }

  public boolean isValid() {
    return validity == Validity.OK;
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#onPostSuccess
   */
  public int getReqid() {
    return reqid;
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
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise.MyRecordAudioPanel#makePostAudioRecordButton
   */
  public AudioAttribute getAudioAttribute() {
    return audioAttribute;
  }

  public void setAudioAttribute(AudioAttribute audioAttribute) {
    this.audioAttribute = audioAttribute;
  }

  /**
   * @param pretestScore
   * @seex mitll.langtest.server.audio.AudioFileHelper#getAlignment
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   * @see mitll.langtest.server.scoring.JsonScoring#getAudioAnswer
   */
  public void setPretestScore(PretestScore pretestScore) {
    this.pretestScore = pretestScore;
    this.score = pretestScore.getOverallScore();
  }

  public String getTranscript() {
    return transcript;
  }

  /**
   * @param transcript
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswerDecoding
   */
  public void setTranscript(String transcript) {
    this.transcript = transcript;
  }

  /**
   * @param timestamp
   * @see mitll.langtest.server.audio.AudioFileHelper#rememberAnswer
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }


  public int getExid() {
    return exid;
  }

  /**
   * @return
   * @see mitll.langtest.client.flashcard.PolyglotPracticePanel#receivedAudioAnswer
   */
  public long getRoundTripMillis() {
    return 0;//roundTripMillis;
  }
/*
  public void setRoundTripMillis(long roundTripMillis) {
    this.roundTripMillis = roundTripMillis;
  }*/

/*
  public String getNormTranscript() {
    return normTranscript;
  }
*/

  public void setNormTranscript(String normTranscript) {
    this.normTranscript = normTranscript;
  }

  public String toString() {
    return "Answer id " + getResultID() +
        " : audio attr " + audioAttribute +
        "\n\tpath        " + path +
        "\n\tduration    " + durationInMillis +
        "\n\tid          " + reqid +
        "\n\tvalidity    " + validity +
        "\n\tcorrect     " + correct +
        "\n\tscore       " + score +
        "\n\tsaid answer " + saidAnswer +
        "\n\tpretest     " + pretestScore +
        "\n\ttranscript  '" + transcript + "'" +
        "\n\tnormTrans   " + normTranscript;
  }
}
