package mitll.langtest.client.analysis;

import com.google.gwt.user.cellview.client.Column;

import java.util.List;

/**
 * Created by go22670 on 10/22/15.
 */
public class PhoneAndStats {
  private final String phone;

  private final int score, current;
  private final int count;

  /**
   * @param phone
   * @param score
   * @param count
   * @see PhoneContainer#getTableWithPager
   */
  public PhoneAndStats(String phone, int score, int current, int count) {
    this.phone = phone;
    this.score = score;
    this.current = current;
    this.count = count;
  }

  /**
   * @return
   * @see PhoneContainer#getCountSorter(Column, List)
   */
  public String getPhone() {
    return phone;
  }

  /**
   * @return
   * @see PhoneContainer#getItemColumn()
   */
  public int getInitial() {
    return score;
  }

  public int getCurrent() {
    return current;
  }

  public int getDiff() { return current-getInitial();}

  /**
   * @return
   * @see PhoneContainer#getCountColumn()
   */
  public int getCount() {
    return count;
  }
}
