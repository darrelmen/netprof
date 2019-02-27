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

package mitll.langtest.client.list;

import mitll.langtest.client.custom.INavigation.VIEWS;
import mitll.langtest.shared.answer.ActivityType;

import static mitll.langtest.shared.answer.ActivityType.UNSET;

/**
 * Created by go22670 on 2/21/17.
 */
public class ListOptions {
  private boolean sort = true;
  private boolean showTypeAhead = true;
  private VIEWS instance = VIEWS.NONE;
  private boolean showFirstNotCompleted = false;
  private ActivityType activityType = UNSET;
  private boolean showPager = true;
  private boolean compact = false;

  public ListOptions() {
  }

  public ListOptions(VIEWS instance) {
    this.instance = instance;
  }

  public ListOptions setShowTypeAhead(boolean val) {
    this.showTypeAhead = val;
    return this;
  }

  public ListOptions setShowPager(boolean val) {
    this.showPager = val;
    return this;
  }

  public ListOptions setShowFirstNotCompleted(boolean val) {
    this.showFirstNotCompleted = val;
    return this;
  }

  public ListOptions setCompact(boolean val) {
    this.compact = val;
    return this;
  }

  public ListOptions setInstance(VIEWS instance) {
    this.instance = instance;
    return this;
  }

  public VIEWS getInstance() {
    return instance;
  }

  public ListOptions setActivityType(ActivityType activityType) {
    this.activityType = activityType;
    return this;
  }
  public ListOptions setSort(boolean val) {
    this.sort = val;
    return this;
  }
  /**
   * @see mitll.langtest.client.exercise.SimplePagingContainer#getTableWithPager
   * @return
   */
  public boolean isSort() {
    return sort;
  }

  boolean isShowTypeAhead() {
    return showTypeAhead;
  }

  boolean isShowFirstNotCompleted() {
    return showFirstNotCompleted;
  }

  ActivityType getActivityType() {
    return activityType;
  }

  public boolean isShowPager() {
    return showPager;
  }

  public boolean isCompact() {
    return compact;
  }
}
