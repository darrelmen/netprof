package mitll.langtest.server.database.word;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Map;

/**
 * Created by go22670 on 3/29/16.
 */
@Entity
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

  @Id
  @GeneratedValue(generator = "increment")
  @GenericGenerator(name = "increment", strategy = "increment")
  public long getId() { return id; }

  private void setId(long id) {
    this.id = id;
  }

  public long getRid() {
    return rid;
  }

  private void setRid(long rid) {
    this.rid = rid;
  }

  public String getWord() {
    return word;
  }

  private void setWord(String word) {
    this.word = word;
  }

  public int getSeq() {
    return seq;
  }

  private void setSeq(int seq) {
    this.seq = seq;
  }

  public float getScore() {
    return score;
  }

  private void setScore(float score) {
    this.score = score;
  }

  public String toString() {
    return "# " + id + " rid " + rid + " " + word + " at " + seq + " score " + score;
  }
}
