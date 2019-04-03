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

package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @seex mitll.langtest.server.database.analysis.Analysis#getUserInfos
 * @see mitll.langtest.client.result.ActiveUsersManager#show
 */
public class ActiveUser extends SimpleUser {
  private String projectName = "";
  private String language = "";
  private long visited;

  public enum PENDING implements IsSerializable {REQUESTED, APPROVED, DENIED}

  private PENDING state;

  public ActiveUser() {
  }

  /**
   * @param firstLast
   * @param visited
   * @see mitll.langtest.server.database.security.NPUserSecurityManager#getActiveSince
   */
  public ActiveUser(FirstLastUser firstLast, long visited) {
    this(firstLast.id, firstLast.getUserID(),
        firstLast.getFirst(), firstLast.getLast(),
        firstLast.getLastChecked(), visited, "", "");
  }

  /**
   * @param id
   * @param userid
   * @param first
   * @param last
   * @param lastChecked
   * @paramz affiliation
   * @see mitll.langtest.server.database.user.DominoUserDAOImpl#refreshUserCache
   */
  private ActiveUser(int id, String userid, String first, String last, long lastChecked, long visited,
                     String name, String language) {
    super(id, userid, first, last, lastChecked);
    this.projectName = name;
    this.language = language;
    this.visited = visited;
  }

  public long getVisited() {
    return visited;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public PENDING getState() {
    return state;
  }

  public ActiveUser setState(PENDING state) {
    this.state = state;
    return this;
  }
}
