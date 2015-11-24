/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.shared.scoring.PretestScore;

import java.io.File;

/**
 * What a client might want to know about some audio that was just posted.
 * <p/>
 * For instance, if it was a valid recording and if not, what type of problem (too quiet, too loud, no mic, etc.)
 * <p/>
 * Also returns the score if the audio was scored, and any decode output if it was decoding.
 * <p/>
 * Mainly it the path to the audio on the server so the client can play it as an mp3.
 * User: GO22670
 * Date: 7/6/12
 * Time: 7:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioAnswer implements IsSerializable {
  private static final String PRESS_AND_HOLD = "Press and hold to record, release to stop recording.";
  private int reqid;
  private String path;
  private Validity validity;
  private String decodeOutput = "";
  private double score = -1;
  private boolean correct = false;
  private boolean saidAnswer = false;
  private long durationInMillis;
  private long resultID;
  private AudioAttribute audioAttribute;
  private PretestScore pretestScore;

  /**
   * @see mitll.langtest.server.audio.AudioCheck.ValidityAndDur
   */
  public enum Validity implements IsSerializable {
    OK("Audio OK."),
    TOO_SHORT(PRESS_AND_HOLD),
    MIC_DISCONNECTED("Is your mic disconnected?"),
    TOO_QUIET("Audio too quiet. Check your mic settings or speak closer to the mic."),
    SNR_TOO_LOW("You are either speaking too quietly or the room is too noisy.<br/>Speak louder or closer to the mic or go to a quieter room."),
    TOO_LOUD("Audio too loud. Check your mic settings or speak farther from the mic."),
    INVALID("There was a problem with the audio. Please record again.");
    private String prompt;

    Validity() {} // for gwt serialization
    Validity(String p) {
      prompt = p;
    }
    public String getPrompt() { return prompt; }
  }

  public AudioAnswer() {}

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer
   * @param path
   * @param validity
   * @param reqid
   * @param duration
   */
  public AudioAnswer(String path, Validity validity, int reqid, long duration) {
    this.path = path;
    this.validity = validity;
    this.reqid = reqid;
    this.durationInMillis = duration;
  }

  public void setDecodeOutput(String decodeOutput) { this.decodeOutput = decodeOutput; }

  /**
   * @see mitll.langtest.server.autocrt.AutoCRT#getFlashcardAnswer
   * @param score
   */
  public void setScore(double score) { this.score = score; }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer, com.google.gwt.user.client.ui.Panel)
   * @return score from hydec (see nnscore)
   */
  public double getScore() { return score; }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer, com.google.gwt.user.client.ui.Panel)
   * @return
   */
  public boolean isCorrect() { return correct; }

  /**
   * @see mitll.langtest.server.autocrt.AutoCRT#getFlashcardAnswer
   * @param correct
   */
  public void setCorrect(boolean correct) {
    this.correct = correct;
  }

  public long getResultID() {
    return resultID;
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#writeAudioFile
   * @param resultID
   */
  public void setResultID(long resultID) {
    this.resultID = resultID;
  }

  public boolean isSaidAnswer() {
    return saidAnswer;
  }

  public void setSaidAnswer(boolean saidAnswer) {
    this.saidAnswer = saidAnswer;
  }

  public boolean isValid() { return validity == Validity.OK; }

  public int getReqid() {
    return reqid;
  }

  public String getPath() { return path; }

  public void setPath(String path) { this.path = path;  }

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
   * @return
   */
  public AudioAttribute getAudioAttribute() {
    return audioAttribute;
  }

  public void setAudioAttribute(AudioAttribute audioAttribute) {
    this.audioAttribute = audioAttribute;
  }

  public PretestScore getPretestScore() {  return pretestScore;  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#getAlignment
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswer(CommonExercise, int, File, AudioCheck.ValidityAndDur, String, boolean, boolean, boolean, boolean)
   * @see mitll.langtest.server.ScoreServlet#getAudioAnswer(int, String, int, boolean, String, File, String, String, CommonExercise, boolean, boolean)
   * @param pretestScore
   */
  public void setPretestScore(PretestScore pretestScore) {
    this.pretestScore = pretestScore;
    this.score = pretestScore.getHydecScore();
  }

  public String toString() {
    return "Answer id " + getResultID() + " : audio attr " +audioAttribute+
      " Path " + path + " duration " + durationInMillis + " id " + reqid + " validity " + validity +
      " correct " + correct + " score " + score + " said answer " + saidAnswer + " pretest "+ pretestScore;
  }
}
