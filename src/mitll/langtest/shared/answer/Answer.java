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
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer, com.google.gwt.user.client.ui.Panel)
   * @return score from hydec (see nnscore)
   */
  double getScore() { return score; }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#receivedAudioAnswer(AudioAnswer, com.google.gwt.user.client.ui.Panel)
   * @see mitll.langtest.server.autocrt.AutoCRT#getAutoCRTDecodeOutput(String, int, java.io.File, AudioAnswer)
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
