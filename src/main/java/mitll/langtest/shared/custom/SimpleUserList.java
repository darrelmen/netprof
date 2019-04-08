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

package mitll.langtest.shared.custom;

public class SimpleUserList extends Nameable implements IUserList {
  private int projid;
  private int userid;
  private int numItems;
  private int duration, minScore;
  private String userChosenID;
  private String fullName;
  private String desc;
  private boolean showAudio;
  private boolean isPrivate;

  public SimpleUserList() {
  }

  /**
   * @param id
   * @param name
   * @param description
   * @param projid
   * @param userid
   * @param userChosenID
   * @param fullName
   * @param duration
   * @param isPrivate
   * @see mitll.langtest.server.database.custom.UserListManager#getSimpleLists
   */
  public SimpleUserList(int id, String name, String description, int projid, int userid, String userChosenID,
                        String fullName,
                        int numItems, int duration, int minScore, boolean showAudio, boolean isPrivate) {
    super(id, name);
    this.desc = description;
    this.projid = projid;
    this.userid = userid;
    this.userChosenID = userChosenID;
    this.fullName = fullName;
    this.numItems = numItems;
    this.duration = duration;
    this.minScore = minScore;
    this.showAudio = showAudio;
    this.isPrivate = isPrivate;
  }

  @Override
  public int getUserID() {
    return userid;
  }

  @Override
  public int getNumItems() {
    return numItems;
  }

  @Override
  public void setNumItems(int numItems) {
    this.numItems = numItems;
  }

  @Override
  public String getUserChosenID() {
    return userChosenID;
  }

  @Override
  public String getFirstInitialName() {
    return fullName;
  }

  @Override
  public int getProjid() {
    return projid;
  }

  @Override
  public int getMinScore() {
    return minScore;
  }

  @Override
  public boolean shouldShowAudio() {
    return showAudio;
  }

  /**
   * TODOx: allow teacher to choose.
   *
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getQuizInfo
   */
  public int getRoundTimeMinutes() {
    return duration;
  }

  @Override
  public boolean isPrivate() {
    return isPrivate;
  }

  @Override
  public String getDescription() {
    return desc;
  }
}
