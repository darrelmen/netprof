package mitll.langtest.shared.user;

/**
 * @see mitll.langtest.server.database.analysis.Analysis#getUserInfos
 */
public class FirstLastUser extends SimpleUser {
  protected String affiliation = "";

  public FirstLastUser() {
  }

  FirstLastUser(int id) {
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
  public FirstLastUser(int id, String userid, String first, String last, long lastChecked, String affiliation) {
    super(id, userid, first, last, lastChecked);
    this.affiliation = affiliation;
  }

  public String getAffiliation() {
    return this.affiliation;
  }

  public void setAffiliation(String affilation) {
    this.affiliation = affilation;
  }

  public boolean isPoly() {
    return affiliation.toLowerCase().startsWith("poly");
  }

  public boolean isNPQ() {
    return affiliation.toLowerCase().startsWith("npq");
  }
}
