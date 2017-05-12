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

import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @see mitll.langtest.client.analysis.WordContainer#getTableWithPager(List)
 * @since 10/21/15.
 */
public class WordScore implements Serializable, Comparable<WordScore> {
  private int exid;
  private float pronScore;
  private int resultID;
  private long timestamp;

  private String answerAudio;
  private String refAudio;

  private Map<NetPronImageType, List<TranscriptSegment>> transcript;

  public WordScore() {
  }

  /**
   * @param bs
   * @param transcript
   * @see mitll.langtest.server.database.analysis.Analysis#getWordScore
   */
  public WordScore(BestScore bs, Map<NetPronImageType, List<TranscriptSegment>> transcript) {
    this(bs.getExId(), bs.getScore(), bs.getTimestamp(), bs.getResultID(), bs.getFileRef(),
        bs.getNativeAudio(), transcript);
  }

  protected WordScore(int exid,
                      float pronScore,
                      long timestamp,
                      int resultID, String answerAudio, String refAudio,
                      Map<NetPronImageType, List<TranscriptSegment>> transcript) {
    this.exid = exid;
    this.pronScore = (pronScore < 0) ? 0 : pronScore;
    this.timestamp = timestamp;
    this.resultID = resultID;
    this.answerAudio = answerAudio;
    this.refAudio = refAudio;
    this.transcript = transcript;
  }

  @Override
  public int compareTo(WordScore o) {
    int i = Float.valueOf(getPronScore()).compareTo(o.getPronScore());
    if (i == 0) {
      i = Long.valueOf(timestamp).compareTo(o.timestamp);
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
    return pronScore;
  }

  public Map<NetPronImageType, List<TranscriptSegment>> getTranscript() {
    return transcript;
  }

  public void setTranscript(Map<NetPronImageType, List<TranscriptSegment>> transcript) {
    this.transcript = transcript;
  }

  public String getAnswerAudio() {
    return answerAudio;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.WordContainer#getPlayNativeAudio
   */
  public String getRefAudio() {
    return refAudio;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String toString() {
    return "WordScore exid " + exid + "/" + resultID + " score " + pronScore + "  : " + transcript +
        " native " + refAudio + " fileRef " + answerAudio;
  }
}
