package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 3/21/17.
 */
public class ExerciseIDAndScore implements IsSerializable {
  protected int exid;
  protected float score;
  protected long timestamp;

  public ExerciseIDAndScore() {
  }

  public ExerciseIDAndScore(int exid, long timestamp, float score) {
    this.exid = exid;
    this.timestamp = timestamp;
    this.score = score;
  }
}
