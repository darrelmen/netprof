package mitll.langtest.shared.user;

/**
 * @seex mitll.langtest.server.database.analysis.Analysis#getUserInfos
 * @see mitll.langtest.client.result.ActiveUsersManager#show
 */
public class ActiveUser extends SimpleUser {
  private String projectName = "";
  private String language = "";
  private long visited;

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
}
