package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by go22670 on 10/22/15.
 */
public class WordAndScore implements Comparable<WordAndScore>, Serializable {
  private int wseq;
  private int seq;
  private String word;
  private float score;
  private long resultID;
  private String answerAudio;
  private String refAudio;
  private String scoreJson;

  /**
   * @param word
   * @param score
   * @param resultID
   * @param wseq        which word in phrase
   * @param seq         which phoneme in phrase (not in word)
   * @param answerAudio
   * @param refAudio
   * @param scoreJson
   * @see #getPhoneReport(String, Map)
   */
  public WordAndScore(String word, float score, long resultID, int wseq, int seq, String answerAudio, String refAudio, String scoreJson) {
    this.word = word;
    this.score = score;
    this.resultID = resultID;
    this.wseq = wseq;
    this.seq = seq;
    this.answerAudio = answerAudio;
    this.refAudio = refAudio;
    this.scoreJson = scoreJson;
  }

  public WordAndScore() {
  }

  @Override
  public int compareTo(WordAndScore o) {
    return getScore() < o.getScore() ? -1 : getScore() > o.getScore() ? +1 : 0;
  }

  public String toString() {
    return "#" + getWseq() + " : " + getWord() + " s " + getScore() + " res " + getResultID();// + " answer " + answerAudio + " ref " + refAudio;
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
}