package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.client.bootstrap.ItemSorter;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 3/9/17.
 */
public class MatchInfo implements IsSerializable, Comparable<MatchInfo> {
  protected String value;
  private int count;

  private ItemSorter itemSorter = new ItemSorter();

  public MatchInfo() {
  }

  public MatchInfo(SectionNode node) {
    this.value = node.getName();
    this.count = node.getCount();
  }

  public MatchInfo(String value, int count) {
    this.value = value;
    this.count = count;
  }

  public void incr(int c) {
    count += c;
  }

  public String getValue() {
    return value;
  }

  public int getCount() {
    return count;
  }

  public String toString() {
    return value + "=" + count;
  }

  @Override
  public int compareTo(@NotNull MatchInfo o) {
    return itemSorter.compare(value, o.getValue());
  }
}
