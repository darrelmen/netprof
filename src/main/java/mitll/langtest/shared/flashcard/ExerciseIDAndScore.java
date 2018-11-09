package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 3/21/17.
 * <p>
 * TODO : use this instead of correctAndScore in UI.
 */
public class ExerciseIDAndScore implements IsSerializable {
  protected int exid;
  private float score;
  protected long timestamp;

  public ExerciseIDAndScore() {
  }

  ExerciseIDAndScore(float score) {
    this.score = score;
  }

  ExerciseIDAndScore(int exid, long timestamp, float score) {
    this.exid = exid;
    this.timestamp = timestamp;
    this.score = score;
  }

  /**
   * @return 0-100
   */
  public float getScore() {
    return score;
  }

  public int getPercentScore() {    return Math.round(100f * score);  }

  public int getExid() {
    return exid;
  }
}
