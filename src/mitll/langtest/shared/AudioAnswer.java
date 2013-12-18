package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 7/6/12
* Time: 7:01 PM
* To change this template use File | Settings | File Templates.
*/
public class AudioAnswer implements IsSerializable {
  public int reqid;
  public String path;
  public Validity validity;
  public String decodeOutput = "";
  private double score = -1;
  private boolean correct = false;
  private boolean saidAnswer = false;
  public int durationInMillis;
  private long resultID;

  public enum Validity implements IsSerializable {
    OK("Audio OK."),
    TOO_SHORT("Audio too short. Please record again."),
    TOO_QUIET("Audio too quiet. Check your mic settings or speak closer to the mic."),
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
   * @param path
   * @param validity
   * @param reqid
   * @param duration
   */
  public AudioAnswer(String path, Validity validity, int reqid, int duration) {
    this.path = path;
    this.validity = validity;
    this.reqid = reqid;
    this.durationInMillis = duration;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @param path
   * @param validity
   * @param decodeOutput
   * @param score
   * @param reqid
   */
  public AudioAnswer(String path, Validity validity, String decodeOutput, double score, int reqid, int duration) {
    this(path, validity, reqid, duration);
    this.decodeOutput = decodeOutput;
    this.score = score;
  }

  public void setDecodeOutput(String decodeOutput) { this.decodeOutput = decodeOutput; }

  /**
   * @see mitll.langtest.server.autocrt.AutoCRT#getFlashcardAnswer
   * @param score
   */
  public void setScore(double score) { this.score = score; }

  /**
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#receivedAudioAnswer(AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
   * @return score from hydec (see nnscore)
   */
  public double getScore() { return score; }

  /**
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#receivedAudioAnswer(AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
   * @see mitll.langtest.server.autocrt.AutoCRT#getAutoCRTDecodeOutput(String, int, java.io.File, AudioAnswer)
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

  public void setResultID(long resultID) {
    this.resultID = resultID;
  }

  public boolean isSaidAnswer() {
    return saidAnswer;
  }

  public void setSaidAnswer(boolean saidAnswer) {
    this.saidAnswer = saidAnswer;
  }

  public String toString() {
    return "Answer id " + getResultID() + " : Path " + path + " id " + reqid + " validity " + validity +
      " correct " + correct + " score " + score + " said answer " + saidAnswer;
  }
}
