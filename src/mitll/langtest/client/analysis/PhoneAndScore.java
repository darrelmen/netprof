package mitll.langtest.client.analysis;

/**
 * Created by go22670 on 10/22/15.
 */
public class PhoneAndScore {
  private final String phone;

  private final float score;
  private final int count;

  /**
   * @see PhoneContainer#getTableWithPager
   * @param phone
   * @param score
   * @param count
   */
  public PhoneAndScore(String phone, float score, int count) { this.phone = phone; this.score = score; this.count = count;}

  public String getPhone() {
    return phone;
  }

  public float getScore() {
    return score;
  }

  public int getCount() {
    return count;
  }
}
