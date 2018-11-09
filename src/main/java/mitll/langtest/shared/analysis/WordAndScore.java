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
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @see mitll.langtest.client.analysis.PhoneExampleContainer
 * @since 10/22/15.
 */
public class WordAndScore extends WordScore {
  private int wseq;
  private transient int seq;
  private String word;
  private transient String scoreJson;
  private transient float prevScore;

  private Map<NetPronImageType, List<TranscriptSegment>> fullTranscript;

  /**
   * @param word
   * @param prevScore
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
  public WordAndScore(int exid, String word, float prevScore, float pronScore,
                      int resultID, int wseq, int seq, String answerAudio,
                      String refAudio, String scoreJson, long timestamp) {
    super(exid, pronScore, timestamp, resultID, answerAudio, refAudio, null);
    this.word = word;
    this.wseq = wseq;
    this.seq = seq;
    this.scoreJson = scoreJson;

    this.prevScore = prevScore;
  }

  public WordAndScore() {
  }

  public boolean equals(Object other) {
    if (!(other instanceof WordAndScore)) {
      return false;
    } else {
      WordScore other1 = (WordScore) other;
      return compareTo(other1) == 0;
    }
  }

  /**
   * Sort first by score, then by word, then by seq
   *
   * @param o
   * @return
   */
  @Override
  public int compareTo(WordScore o) {
    int i = Integer.compare(getIntPronScore(), o.getIntPronScore());

    return getTieBreaker((WordAndScore) o, i);
  }

  public int getTieBreaker(WordAndScore o, int i) {
    if (i == 0) {
      i = word.compareTo(o.word);
    }
    if (i == 0) {
      i = Long.compare(getResultID(), o.getResultID());
    }
    if (i == 0) {
      i = Integer.compare(wseq, o.wseq);
    }
    return i;
  }

  public int getWseq() {
    return wseq;
  }

  /**
   * So this is for iOS...
   *
   * @return
   * @see mitll.langtest.server.database.phone.PhoneJSON#getJsonForWord
   */
  public int getSeq() {
    return seq;
  }

  public String getWord() {
    return word;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.phone.PhoneJSON#getWordsJsonArray(Map, Map, Map, List)
   */
  public String getScoreJson() {
    return scoreJson;
  }

  public void clearJSON() {
    scoreJson = "";
  }

  /**
   * @return
   * @see PhoneExampleContainer#getItemColumn
   */
  public Map<NetPronImageType, List<TranscriptSegment>> getFullTranscript() {
    return fullTranscript;
  }

  /**
   * @param fullTranscript
   * @see mitll.langtest.server.database.phone.BasePhoneDAO#setTranscript
   */
  public void setFullTranscript(Map<NetPronImageType, List<TranscriptSegment>> fullTranscript) {
    this.fullTranscript = fullTranscript;
  }

  public float getPrevScore() {
    return prevScore;
  }

  public String toString() {
    return
        "\n\tExercise   #" + getExid() +
            "\n\t       #" + getWseq() +
            "\n\tseq    " + getSeq() +
            "\n\t:      " + getWord() +
            "\n\tscore  " + getPronScore() +
            "\n\tres    " + getResultID() +
            "\n\tanswer " + getAnswerAudio() +
            "\n\tref    " + getRefAudio();
  }
}