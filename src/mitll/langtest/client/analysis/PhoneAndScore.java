package mitll.langtest.client.analysis;

/**
 * Created by go22670 on 10/22/15.
 */
public class PhoneAndScore {
  private final String phone;
  private final float score;

  public PhoneAndScore(String phone, float score) { this.phone = phone; this.score = score;}

  public String getPhone() {
    return phone;
  }

  public float getScore() {
    return score;
  }
}
