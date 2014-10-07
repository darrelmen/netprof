package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 9/13/14.
 */
public class CorrectAndScore implements IsSerializable, Comparable<CorrectAndScore> {
  private int uniqueID;
  private long userid;

  private String id;
  private boolean correct;
  private float score;
  private long timestamp;
  private String path;

  public CorrectAndScore() {}

  public CorrectAndScore(float score, String path) {
    this.score = score;
    this.path = path;
  }

  public CorrectAndScore(int uniqueID, long userid, String id, boolean correct, float score, long timestamp, String path) {
    this.uniqueID = uniqueID;
    this.id = id;
    this.userid = userid;
    this.correct = correct;
    this.score = score;
    this.timestamp = timestamp;
    this.path = path;
  }

  @Override
  public int compareTo(CorrectAndScore o) {
    return getTimestamp() < o.getTimestamp() ? -1 : getTimestamp() > o.getTimestamp() ? +1 : 0;
  }

  public boolean isCorrect() {
    return correct;
  }

  /**
   * @return 0-100
   */
  public float getScore() {
    return score;
  }

  public int getPercentScore() {
    return Math.round(100f * score);
  }

  public String getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getPath() {
    return path;
  }

  public int getUniqueID() {
    return uniqueID;
  }

  public long getUserid() {
    return userid;
  }

  public String toString() {
    return "id " + getId() + " " + (isCorrect() ? "C" : "I") + " score " + getPercentScore();
  }
}
