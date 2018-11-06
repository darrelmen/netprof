package mitll.langtest.server.database.user;

import java.util.Date;

public class ActiveInfo {
  private final int userid;
  private final long when;
  private final long visited;
  private final int projid;

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
