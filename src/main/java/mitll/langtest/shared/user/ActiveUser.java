package mitll.langtest.shared.user;

/**
 * @see mitll.langtest.server.database.analysis.Analysis#getUserInfos
 */
public class ActiveUser extends SimpleUser {
  private String name;
  private String language;
  public ActiveUser() {
  }

  ActiveUser(int id) {
    super(id);
  }

  /**
   * @param id
   * @param userid
   * @param first
   * @param last
   * @param lastChecked
   * @param affiliation
   * @see mitll.langtest.server.database.user.DominoUserDAOImpl#refreshUserCache
   */
  public ActiveUser(int id, String userid, String first, String last, long lastChecked, String name,String language) {
    super(id, userid, first, last, lastChecked);
    this.name = name;
    this.language = language;
  }


  @Override
  public String getName() {
    return name;
  }

  public String getLanguage() {
    return language;
  }
}
