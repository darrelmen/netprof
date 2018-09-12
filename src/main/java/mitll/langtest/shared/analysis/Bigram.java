package mitll.langtest.shared.analysis;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class Bigram implements Serializable, Comparable<Bigram> {
  private int count = 0;
  private String bigram;
  private float score = 0F;
  private float rawTotalScore = 0F;

  public Bigram() {
  }

  /**
   * @param bigram
   * @param count
   * @param score
   * @see mitll.langtest.server.database.phone.MakePhoneReport#getPhoneReport
   */
  public Bigram(String bigram, int count, float score) {
    this.count = count;
    this.bigram = bigram;
    this.score = score;
  }

  public Bigram(String bigram) {
    this.bigram = bigram;
  }

  public void increment(float newScore) {
    count++;
    rawTotalScore += newScore;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.BigramContainer#getPhoneStatuses
   */
  public int getCount() {
    return count;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.BigramContainer#getPhoneStatuses
   */
  public String getBigram() {
    return bigram;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.BigramContainer#getPhoneStatuses
   */
  public float getScore() {
    return score;
  }

  public void setScore() {
    score = rawTotalScore / (float) count;
  }

  @Override
  public int compareTo(@NotNull Bigram o) {
    return bigram.compareTo(o.bigram);
  }

  @Override
  public boolean equals(Object obj) {
    return bigram.equals(((Bigram) obj).bigram);
  }

  public String toString() {
    return bigram;// +" (" + count + ") = " + score;
  }
}
