package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 3/10/15.
 */
public class Answer implements IsSerializable {
  double score = -1;
  boolean correct = false;
  private long resultID;

  Answer() {} // support RPC

  public Answer(double score, boolean correct, long resultID) {
    this.score = score;
    this.correct = correct;
    this.resultID = resultID;
  }
  /**
   * @see mitll.langtest.server.autocrt.AutoCRT#getFlashcardAnswer
   * @param score
   */
  public void setScore(double score) { this.score = score; }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, com.google.gwt.user.client.ui.Panel)
   * @return score from hydec (see nnscore)
   */
  double getScore() { return score; }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, com.google.gwt.user.client.ui.Panel)
   * @see mitll.langtest.server.autocrt.AutoCRT#getAutoCRTDecodeOutput(String, int, java.io.File, mitll.langtest.shared.AudioAnswer)
   * @return
   */
  boolean isCorrect() { return correct; }

  /**
   * @see mitll.langtest.server.autocrt.AutoCRT#getFlashcardAnswer
   * @see mitll.langtest.server.autocrt.AutoCRT#markCorrectnessOnAnswer
   * @param correct
   */
  public void setCorrect(boolean correct) {  this.correct = correct;  }

  public long getResultID() {   return resultID;  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#writeAudioFile
   * @param resultID
   */
  public void setResultID(long resultID) {  this.resultID = resultID;  }
  public String toString() {
    return "Answer id " + getResultID() + " correct " + isCorrect() + " score " + getScore();
  }
}
