package mitll.langtest.shared.analysis;

import java.io.Serializable;

public class Bigram implements Serializable {
  private int count;
  private String bigram;
  private float score;

  public Bigram() {
  }

  public Bigram(String bigram, int count, float score) {
    this.count = count;
    this.bigram = bigram;
    this.score = score;
  }

  public int getCount() { return count;  }
  public String getBigram() {
    return bigram;
  }
  public float getScore() {
    return score;
  }

  public String toString() {
    return bigram;// + " " + count + " = " + score;
  }
}
