/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
   * @param node
   * @see mitll.langtest.server.database.exercise.SectionHelper#addOrMerge
   */
  public MatchInfo(SectionNode node) {
    this.value = node.getName();
    this.count = node.getCount();
  }

  /**
   * @param value
   * @param count
   * @param userListID
   * @param italic
   * @param tooltip
   * @see mitll.langtest.client.list.FacetExerciseList#getMatchInfoForEachList
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

  /**
   * To indicate when a list is mine or someone else's
   *
   * @see mitll.langtest.client.list.FacetExerciseList#addChoicesForType
   * @return
   */
  public boolean isItalic() {
    return italic;
  }

  public String getTooltip() {
    return tooltip;
  }

  public String toString() {
    return "'"+value + "'=" + count;
  }
}
