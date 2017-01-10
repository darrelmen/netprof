package mitll.langtest.server.database.word;

import java.util.Map;

/**
 * Created by go22670 on 3/29/16.
 */
public class Word {
  private long id;
  private long rid;
  private String word;
  private int seq;
  private float score;

  public Word() {}

  /**
   * @see WordDAO#getWords(String)
   * @param id
   * @param rid
   * @param word
   * @param seq
   * @param score
   */
  public Word(long id, long rid, String word, int seq, float score) {
    this(rid, word, seq, score);
    this.id = id;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#recordWordAndPhoneInfo(long, Map)
   * @param rid
   * @param word
   * @param seq
   * @param score
   */
  public Word(long rid, String word, int seq, float score) {

    this.rid = rid;
    this.word = word;
    this.seq = seq;
    this.score = score;
  }

  public long getId() { return id; }

  public long getRid() {
    return rid;
  }

  public String getWord() {
    return word;
  }

  public int getSeq() {
    return seq;
  }

  public float getScore() {
    return score;
  }

  public String toString() {
    return "# " + id + " rid " + rid + " " + word + " at " + seq + " score " + score;
  }

  public void setRid(int rid) {
    this.rid = rid;
  }
}
