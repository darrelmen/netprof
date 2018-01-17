package mitll.langtest.client.list;

import mitll.langtest.shared.answer.ActivityType;

import static mitll.langtest.shared.answer.ActivityType.UNSET;

/**
 * Created by go22670 on 2/21/17.
 */
public class ListOptions {
  private boolean sort = true;
  private boolean showTypeAhead = true;
  private String instance = "unk";
  private boolean showFirstNotCompleted = false;
  private ActivityType activityType = UNSET;
  private boolean showPager = true;
//  private boolean contextOnly = false;

  public ListOptions() {
  }

  public ListOptions(String instance) {
    this.instance = instance;
  }

  public ListOptions setSort(boolean val) {
    this.sort = val;
    return this;
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

 /* public ListOptions setIncorrectFirst(boolean val) {
    this.incorrectFirst = val;
    return this;
  }*/

  public ListOptions setInstance(String instance) {
    this.instance = instance;
    return this;
  }

  public ListOptions setActivityType(ActivityType activityType) {
    this.activityType = activityType;
    return this;
  }

  /**
   * @return
   */
  public boolean isSort() {
    return sort;
  }

  boolean isShowTypeAhead() {
    return showTypeAhead;
  }

  public String getInstance() {
    return instance;
  }

  boolean isIncorrectFirst() {
  //  boolean incorrectFirst = false;
    return false;
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
/*  public boolean isContextOnly() {
    return contextOnly;
  }

  public ListOptions setContextOnly(boolean contextOnly) {
    this.contextOnly = contextOnly;
    return this;
  }*/
}
