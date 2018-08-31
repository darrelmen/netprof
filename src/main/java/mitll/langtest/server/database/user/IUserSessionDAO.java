/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.user;

import mitll.langtest.server.database.IDAO;
import mitll.npdata.dao.SlickUserSession;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;

public interface IUserSessionDAO extends IDAO {
  /**
   * @param user
   * @see mitll.langtest.server.database.security.NPUserSecurityManager#setSessionUserAndRemember
   */
  void add(SlickUserSession user);

  /**
   * @param session
   * @return
   * @see mitll.langtest.server.database.security.NPUserSecurityManager#lookupUserIDFromSessionOrDB(HttpServletRequest, boolean)
   */
  int getUserForSession(String session);

  boolean updateVisitedForSession(String session);

  void removeAllSessionsForUser(int userId);

  Map<Integer, ActiveInfo> getActiveSince(long when);

  class ActiveInfo {
    private int userid;
    private long when;
    private long visited;
    private int projid;

    /**
     * @param userid
     * @param loggedInTime
     * @param projid
     * @see SlickUserSessionDAOImpl#getActiveSince
     */
    ActiveInfo(int userid, long loggedInTime, long visited, int projid) {
      this.userid = userid;
      this.when = loggedInTime;
      this.visited = visited;
      this.projid = projid;
    }

    public int getUserid() {
      return userid;
    }

    /**
     * @see mitll.langtest.server.database.security.NPUserSecurityManager#getActiveSince
     * @return
     */
    public long getWhen() {
      return when;
    }

    public int getProjid() {
      return projid;
    }

    public long getVisited() {
      return visited;
    }

    public String toString() {
      return "user " + userid + " proj " + projid + " logged in at " + new Date(when) + " visited " + new Date(visited);
    }
  }
}
