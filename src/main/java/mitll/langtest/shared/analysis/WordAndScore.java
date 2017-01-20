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

package mitll.langtest.shared.analysis;

import mitll.langtest.client.analysis.PhoneExampleContainer;
import mitll.langtest.server.database.phone.PhoneDAO;

import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @see mitll.langtest.client.analysis.PhoneExampleContainer
 * @since 10/22/15.
 */
public class WordAndScore extends WordScore {
/*  private int exid;

  private float pronScore;
  private long resultID;
  private long timestamp;
  private String answerAudio;
  private String refAudio;
  private Map<NetPronImageType, List<TranscriptSegment>> transcript;*/

  private int wseq;
  private int seq;
  private String word;
  private String scoreJson;


  /**
   * @param word
   * @param pronScore
   * @param resultID
   * @param wseq        which word in phrase
   * @param seq         which phoneme in phrase (not in word)
   * @param answerAudio
   * @param refAudio
   * @param scoreJson
   * @param timestamp
   * @see mitll.langtest.server.database.phone.BasePhoneDAO#getAndRememberWordAndScore
   */
  public WordAndScore(int exid, String word, float pronScore, int resultID, int wseq, int seq, String answerAudio,
                      String refAudio, String scoreJson, long timestamp) {
    super(exid, pronScore, timestamp, resultID, answerAudio, refAudio, null);
//    this.exid = exid;
    this.word = word;
  //  this.pronScore = pronScore;
    //this.resultID = resultID;
    this.wseq = wseq;
    this.seq = seq;
   // this.answerAudio = answerAudio;
   // this.refAudio = refAudio;
    this.scoreJson = scoreJson;
   //  this.timestamp = timestamp;
  }

  public WordAndScore() {
  }

  /**
   * Sort first by score
   *
   * @param o
   * @return
   */
  @Override
  public int compareTo(WordScore o) {
    int i = getPronScore() < o.getPronScore() ? -1 : getPronScore() > o.getPronScore() ? +1 : 0;

    WordAndScore realOther = (WordAndScore) o;
    if (i == 0) {
      i = word.compareTo(realOther.word);
    }
    if (i == 0) {
      i = Long.valueOf(getResultID()).compareTo(realOther.getResultID());
    }
    if (i == 0) {
      i = Integer.valueOf(wseq).compareTo(realOther.wseq);
    }
    return i;
  }

  public int getWseq() {
    return wseq;
  }

  public int getSeq() {
    return seq;
  }

  public String getWord() {
    return word;
  }

  /**
   * @return
   * @see PhoneExampleContainer#getItemColumn()
   */
/*  public float getPronScore() {
    return pronScore;
  }

  public long getResultID() {
    return resultID;
  }

  public String getAnswerAudio() {
    return answerAudio;
  }

  public String getRefAudio() {
    return refAudio;
  }*/

  public String getScoreJson() {
    return scoreJson;
  }

  /**
   * @param transcript
   * @see PhoneDAO#setTranscript(WordAndScore, Map)
   */
  /*public void setTranscript(Map<NetPronImageType, List<TranscriptSegment>> transcript) {
    this.transcript = transcript;
  }
*/
  /**
   * @return
   * @see PhoneExampleContainer#getItemColumn
   */
  /*public Map<NetPronImageType, List<TranscriptSegment>> getTranscript() {
    return transcript;
  }
*/
  public void clearJSON() {
    scoreJson = "";
  }

  /*public int getExid() {
    return exid;
  }

  public long getTimestamp() {
    return timestamp;
  }
*/
  public String toString() {
    return getExid() + " #" + getWseq() + " : " + getWord() + "\ts " + getPronScore() + "\tres " + getResultID() +
        "\tanswer " + getAnswerAudio() + " ref " + getRefAudio();
  }
}