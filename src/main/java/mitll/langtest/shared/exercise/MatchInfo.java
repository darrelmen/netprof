package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.client.bootstrap.ItemSorter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by go22670 on 3/9/17.
 */
public class MatchInfo implements IsSerializable, Comparable<MatchInfo> {
  protected String value;
  private int count;
  private int userListID;
  private boolean italic = false;
  private String tooltip;

  private ItemSorter itemSorter = new ItemSorter();

  public MatchInfo() {
  }

  public MatchInfo(SectionNode node) {
    this.value = node.getName();
    this.count = node.getCount();
  }

  public MatchInfo(String value, int count, int userListID, boolean italic, String tooltip) {
    this.value = value;
    this.count = count;
    this.userListID = userListID;
    this.italic =italic;
    this.tooltip = tooltip;
  }

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addOrMerge
   * @param c
   */
  public void incr(int c) {
    count += c;
  }

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#mergeMaps2(Map, Map)
   * @return
   */
  public int getCount() {
    return count;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int compareTo(@NotNull MatchInfo o) {
    return itemSorter.compare(value, o.getValue());
  }

  public int getUserListID() {
    return userListID;
  }

  public boolean isItalic() {
    return italic;
  }

  public String getTooltip() {
    return tooltip;
  }

  public String toString() {
    return value + "=" + count;
  }
}
