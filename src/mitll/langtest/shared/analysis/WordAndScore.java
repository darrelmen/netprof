/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.PhoneDAO;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/22/15.
 * @see mitll.langtest.client.analysis.PhoneExampleContainer
 */
public class WordAndScore implements Comparable<WordAndScore>, Serializable {
  private String exid;
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
   * @see PhoneDAO#getPhoneReport(String, Map, boolean)
   */
  public WordAndScore(String exid, String word, float score, long resultID, int wseq, int seq, String answerAudio,
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

  public WordAndScore() {}

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
   * @see PhoneExampleContainer#getItemColumn()
   * @return
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

  public void setTranscript(Map<NetPronImageType, List<TranscriptSegment>> transcript) {
    this.transcript = transcript;
  }

  public Map<NetPronImageType, List<TranscriptSegment>> getTranscript() {
    return transcript;
  }

  public void clearJSON() {
    scoreJson = "";
  }

  public String toString() {
    return exid + " #" + getWseq() + " : " + getWord() + "\ts " + getScore() + "\tres " + getResultID() +
        "\tanswer " + answerAudio + " ref " + refAudio;
  }

  public String getExid() {
    return exid;
  }

  public long getTimestamp() {
    return timestamp;
  }

//  public String getNativeAudio() {
//    return nativeAudio;
//  }
}