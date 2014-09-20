package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.Result;

/**
 * Created by go22670 on 9/13/14.
 */
public class CorrectAndScore implements IsSerializable, Comparable<CorrectAndScore> {
  private String id;
  private boolean correct;
  private int score;
  private long timestamp;

  public CorrectAndScore() {
  }

  public CorrectAndScore(String id, boolean correct, int score, long timestamp) {
    this.id = id;
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
    return getTimestamp() < o.getTimestamp() ? -1: getTimestamp() > o.getTimestamp() ? +1 :0;
  }

  public boolean isCorrect() {
    return correct;
  }

  /**
   *
   * @return 0-100
   */
  public int getScore() {
    return score;
  }

  public String getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String toString() { return "id " + getId() + " " +(isCorrect() ? "C":"I")+ " score " + getScore(); }
}
