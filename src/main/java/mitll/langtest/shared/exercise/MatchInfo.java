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
  private String tooltip = "";

  private ItemSorter itemSorter = new ItemSorter();

  public MatchInfo() {
  }

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addOrMerge
   * @param node
   */
  public MatchInfo(SectionNode node) {
    this.value = node.getName();
    this.count = node.getCount();
  }

  /**
   * @see mitll.langtest.client.list.FacetExerciseList#getMatchInfoForEachList
   * @param value
   * @param count
   * @param userListID
   * @param italic
   * @param tooltip
   */
  public MatchInfo(String value, int count, int userListID, boolean italic, String tooltip) {
    this.value = value;
    this.count = count;
    this.userListID = userListID;
    this.italic = italic;
    this.tooltip = tooltip;
  }

  /**
   * @param c
   * @see mitll.langtest.server.database.exercise.SectionHelper#addOrMerge
   */
  public void incr(int c) {
    count += c;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.exercise.SectionHelper#mergeMaps2(Map, Map)
   * @see mitll.langtest.client.list.FacetExerciseList#getAnchor(String, MatchInfo, int, boolean)
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

  @Override
  public boolean equals(Object obj) {
    return value.equals(((MatchInfo)obj).value);
  }

  public int getUserListID() {
    return userListID;
  }

  public boolean isItalic() {   return italic;  }

  public String getTooltip() {
    return tooltip;
  }

  public String toString() {
    return "'"+value + "'=" + count;
  }
}
