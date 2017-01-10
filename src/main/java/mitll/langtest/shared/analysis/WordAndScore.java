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
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @see mitll.langtest.client.analysis.PhoneExampleContainer
 * @since 10/22/15.
 */
public class WordAndScore implements Comparable<WordAndScore>, Serializable {
  private int exid;
  private int wseq;
  private int seq;
  private String word;
  private float score;
  private long resultID;
  private long timestamp;
  private String answerAudio;
  private String refAudio;
  private String scoreJson;
  private Map<NetPronImageType, List<TranscriptSegment>> transcript;

  /**
   * @param word
   * @param score
   * @param resultID
   * @param wseq        which word in phrase
   * @param seq         which phoneme in phrase (not in word)
   * @param answerAudio
   * @param refAudio
   * @param scoreJson
   * @param timestamp
   * @see PhoneDAO#getAndRememberWordAndScore(Map, Map, Map, String, String, String, long, int, String, long, String, int, float)
   */
  public WordAndScore(int exid, String word, float score, long resultID, int wseq, int seq, String answerAudio,
                      String refAudio, String scoreJson, long timestamp) {
    this.exid = exid;
    this.word = word;
    this.score = score;
    this.resultID = resultID;
    this.wseq = wseq;
    this.seq = seq;
    this.answerAudio = answerAudio;
    this.refAudio = refAudio;
    this.scoreJson = scoreJson;
    this.timestamp = timestamp;
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
  public int compareTo(WordAndScore o) {
    int i = getScore() < o.getScore() ? -1 : getScore() > o.getScore() ? +1 : 0;
    if (i == 0) {
      i = word.compareTo(o.word);
    }
    if (i == 0) {
      i = Long.valueOf(resultID).compareTo(o.resultID);
    }
    if (i == 0) {
      i = Integer.valueOf(wseq).compareTo(o.wseq);
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
  public float getScore() {
    return score;
  }

  public long getResultID() {
    return resultID;
  }

  public String getAnswerAudio() {
    return answerAudio;
  }

  public String getRefAudio() {
    return refAudio;
  }

  public String getScoreJson() {
    return scoreJson;
  }

  /**
   * @param transcript
   * @see PhoneDAO#setTranscript(WordAndScore, Map)
   */
  public void setTranscript(Map<NetPronImageType, List<TranscriptSegment>> transcript) {
    this.transcript = transcript;
  }

  /**
   * @return
   * @see PhoneExampleContainer#getItemColumn
   */
  public Map<NetPronImageType, List<TranscriptSegment>> getTranscript() {
    return transcript;
  }

  public void clearJSON() {
    scoreJson = "";
  }

  public int getExid() {
    return exid;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String toString() {
    return exid + " #" + getWseq() + " : " + getWord() + "\ts " + getScore() + "\tres " + getResultID() +
        "\tanswer " + answerAudio + " ref " + refAudio;
  }
}