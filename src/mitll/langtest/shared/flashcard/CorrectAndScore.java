package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.Result;

/**
 * Created by go22670 on 9/13/14.
 */
public class CorrectAndScore implements IsSerializable, Comparable<CorrectAndScore> {
  private boolean correct;
  private int score;
  private long timestamp;

  public CorrectAndScore() {
  }

  public CorrectAndScore(boolean correct, int score, long timestamp) {
    this.correct = correct;
    this.score = score;
    this.timestamp = timestamp;
  }

  public CorrectAndScore(Result r) {
    this.correct =r.isCorrect();
    this.score = (int)(r.getPronScore()*100f);
    this.timestamp = r.timestamp;
  }

  @Override
  public int compareTo(CorrectAndScore o) {
    return timestamp < o.timestamp ? -1:timestamp>o.timestamp ? +1 :0;
  }

  public String toString() { return isCorrect() ? "C":"I"+ " score " + getScore(); }

  public boolean isCorrect() {
    return correct;
  }

  public int getScore() {
    return score;
  }
}
