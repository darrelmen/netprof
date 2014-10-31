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

  /**
   * @see mitll.langtest.client.gauge.ASRScorePanel#gotScore(mitll.langtest.shared.scoring.PretestScore, boolean, String)
   * @param score
   * @param path
   */
  public CorrectAndScore(float score, String path) {
    this.score = score;
    this.path = path;
  }

  /**
   * @see mitll.langtest.server.database.ResultDAO#getScoreResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   * @param uniqueID
   * @param userid
   * @param exerciseID
   * @param correct
   * @param score
   * @param timestamp
   * @param path
   */
  public CorrectAndScore(int uniqueID, long userid, String exerciseID, boolean correct, float score, long timestamp, String path) {
    this.uniqueID = uniqueID;
    this.id = exerciseID;
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
