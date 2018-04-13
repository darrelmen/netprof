package mitll.langtest.shared.custom;

public class SimpleUserList extends UserListLight implements IUserList {
  private int projid;
  private int userid;
  private int numItems;
  private int duration;
  private String userChosenID;

  public SimpleUserList() {
  }

  /**
   * @param id
   * @param name
   * @param projid
   * @param userid
   * @param userChosenID
   * @param duration
   * @see mitll.langtest.server.database.custom.UserListManager#getSimpleListsForUser
   */
  public SimpleUserList(int id, String name, int projid, int userid, String userChosenID, int numItems, int duration) {
    super(id, name);
    this.projid = projid;
    this.userid = userid;
    this.userChosenID = userChosenID;
    this.numItems = numItems;
    this.duration = duration;
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
  public String getUserChosenID() {
    return userChosenID;
  }

  @Override
  public int getProjid() {
    return projid;
  }

  /**
   * TODOx: allow teacher to choose.
   *
   * @return
   */
  public int getRoundTimeMinutes() {
    return duration;
  }
}
