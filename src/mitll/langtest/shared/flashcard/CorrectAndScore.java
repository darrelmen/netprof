package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.Result;

/**
 * Created by go22670 on 9/13/14.
 */
public class CorrectAndScore implements IsSerializable, Comparable<CorrectAndScore> {
  boolean wasCorrect;
  int score;
  long timestamp;

  public CorrectAndScore() {
  }

  public CorrectAndScore(boolean wasCorrect, int score, long timestamp) {
    this.wasCorrect = wasCorrect;
    this.score = score;
    this.timestamp = timestamp;
  }

  public CorrectAndScore(Result r) {
    this.wasCorrect =r.isCorrect();
    this.score = (int)r.getPronScore()*100;
    this.timestamp = r.timestamp;
  }

  @Override
  public int compareTo(CorrectAndScore o) {
    return timestamp < o.timestamp ? -1:timestamp>o.timestamp ? +1 :0;
  }
}
