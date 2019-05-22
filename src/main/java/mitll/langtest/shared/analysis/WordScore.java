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

package mitll.langtest.shared.analysis;

import mitll.langtest.client.analysis.AudioExampleContainer;
import mitll.langtest.client.analysis.PhoneExampleContainer;
import mitll.langtest.client.analysis.WordContainerAsync;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class WordScore implements Serializable, Comparable<WordScore> {
  private int exid;
  private int pronScore;
  private int resultID;
  private long timestamp;

  private String answerAudio;
  private String refAudio;

  private static final int SCALE = 1000;

  public static final String WORD = "WORD";
  public static final String SCORE = "Score";
  public static final String TIMESTAMP = "timestamp";
  public static final String ASC = "ASC";

  private Map<NetPronImageType, List<SlimSegment>> transcript;
  private boolean isContext;

  public WordScore() {
  }

  /**
   * @param bs
   * @param transcript
   * @see Analysis#getWordScore
   */
  public WordScore(BestScore bs, Map<NetPronImageType, List<SlimSegment>> transcript) {
    this(bs.getExId(),
        bs.getScore(),
        bs.getTimestamp(),
        bs.getResultID(),
        bs.getFileRef(),
        bs.getNativeAudio(), transcript);
  }

  /**
   * @param exid
   * @param pronScore
   * @param timestamp
   * @param resultID
   * @param answerAudio
   * @param refAudio
   * @param transcript
   * @see WordAndScore
   */
  protected WordScore(int exid,
                      float pronScore,
                      long timestamp,
                      int resultID,
                      String answerAudio,
                      String refAudio,
                      Map<NetPronImageType, List<SlimSegment>> transcript) {
    this.exid = exid;
    this.pronScore = (pronScore < 0) ? 0 : (int) (pronScore * SCALE);
    this.timestamp = timestamp;
    this.resultID = resultID;
    this.answerAudio = answerAudio;
    this.refAudio = refAudio;
    this.transcript = transcript;
  }

  /**
   * By score
   *
   * @param o
   * @return
   */
  @Override
  public int compareTo(WordScore o) {
    int i = Integer.compare(pronScore, o.pronScore);
    if (i == 0) {
      i = Long.compare(timestamp, o.timestamp);
    }
    return i;
  }

  public int getExid() {
    return exid;
  }

  public long getResultID() {
    return resultID;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public float getPronScore() {
    return ((float) pronScore) / SCALE;
  }

  int getIntPronScore() {
    return pronScore;
  }

  /**
   * @return
   * @see PhoneExampleContainer#getItemColumn
   * @see WordContainerAsync#getItemColumn
   */
  public Map<NetPronImageType, List<SlimSegment>> getTranscript() {
    return transcript;
  }

  /**
   * @param transcript
   * @seex BasePhoneDAO#setTranscript(WordAndScore, Map)
   */
  public void setTranscript(Map<NetPronImageType, List<SlimSegment>> transcript) {
    this.transcript = transcript;
  }

  public String getAnswerAudio() {
    return answerAudio;
  }

  /**
   * @return
   * @see AudioExampleContainer#playAudio
   */
  public String getRefAudio() {
    return refAudio;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setIsContext(boolean isContext) {
    this.isContext = isContext;
  }

  public boolean getIsContext() {
    return isContext;
  }

  public String toString() {
    return "WordScore exid " + exid + "/" + resultID + " score " + getPronScore() + "  : " + transcript +
        " native " + refAudio + " fileRef " + answerAudio;
  }
}
