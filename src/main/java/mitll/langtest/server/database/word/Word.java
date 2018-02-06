package mitll.langtest.server.database.word;

import mitll.langtest.server.database.DatabaseServices;

/**
 * Created by go22670 on 3/29/16.
 */
public class Word {
  private int id;
  private int projid;
  private int rid;

  private String word;
  private int seq;
  private float score;

  public Word() {}

  /**
   * @see WordDAO#getWords
   * @see SlickWordDAO#fromSlick
   * @param id
   * @param projid
   * @param rid
   * @param word
   * @param seq
   * @param score
   */
  public Word(int id, int projid, int rid, String word, int seq, float score) {
    this(projid, rid, word, seq, score);
    this.id = id;
  }

  /**
   * @see DatabaseServices#recordWordAndPhoneInfo
   * @param projid
   * @param rid
   * @param word
   * @param seq
   * @param score
   */
  public Word(int projid, int rid, String word, int seq, float score) {
    this.projid = projid;
    this.rid = rid;
    this.word = word;
    this.seq = seq;
    this.score = score;
  }

  public int getId() { return id; }

  public int getRid() {
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

  public void setRid(int rid) {
    this.rid = rid;
  }

  public int getProjid() {
    return projid;
  }

  public String toString() {
    return "# " + id + " proj " + projid+
        "  rid " + rid + " " + word + " at " + seq + " score " + score;
  }
}
