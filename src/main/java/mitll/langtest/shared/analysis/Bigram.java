package mitll.langtest.shared.analysis;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static mitll.langtest.shared.analysis.SimpleTimeAndScore.SCALE;

public class Bigram implements Serializable, Comparable<Bigram> {
  private int count = 0;
  private String bigram;
  private int score = 0;
  private transient float rawTotalScore = 0F;

  public Bigram() {
  }

  /**
   * @paramx bigram
   * @paramx count
   * @paramx score
   * @see mitll.langtest.server.database.phone.MakePhoneReport#getPhoneReport
   */
/*  public Bigram(String bigram, int count, float score) {
    this.count = count;
    this.bigram = bigram;
    this.score = score;
  }*/
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
    return fromInt(score);
  }

  public void setScore() {
    float avg = rawTotalScore / (float) count;
    this.score = (avg < 0) ? 0 : toInt(avg);
  }

  protected int toInt(float value) {
    return (int) (value * SCALE);
  }

  private float fromInt(int value) {
    return ((float) value) / SCALE;
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
