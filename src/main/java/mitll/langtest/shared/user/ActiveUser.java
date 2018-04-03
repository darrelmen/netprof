package mitll.langtest.shared.user;

/**
 * @see mitll.langtest.server.database.analysis.Analysis#getUserInfos
 */
public class ActiveUser extends SimpleUser {
  private String projectName = "";
  private String language = "";

  public ActiveUser() {
  }

  public ActiveUser(int id) {
    super(id);
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
  public ActiveUser(int id, String userid, String first, String last, long lastChecked, String name, String language) {
    super(id, userid, first, last, lastChecked);
    this.projectName = name;
    this.language = language;
  }

  public ActiveUser(FirstLastUser firstLast) {
    this(firstLast.id, firstLast.getUserID(),
        firstLast.getFirst(), firstLast.getLast(), firstLast.getLastChecked(), "", "");
  }


  public String getProjectName() {
    return projectName;
  }

  public String getLanguage() {
    return language;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public void setLanguage(String language) {
    this.language = language;
  }
}
