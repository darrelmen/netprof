package mitll.langtest.shared.custom;

public class SimpleUserList extends UserListLight implements IUserList {
  private int projid;
  private int userid;
  private int numItems;
  private int duration, minScore;
  private String userChosenID;
  private String fullName;
  private boolean showAudio;
  private boolean isPrivate;

  public SimpleUserList() {
  }

  /**
   * @param id
   * @param name
   * @param projid
   * @param userid
   * @param userChosenID
   * @param fullName
   * @param duration
   * @param isPrivate
   * @see mitll.langtest.server.database.custom.UserListManager#getSimpleLists
   */
  public SimpleUserList(int id, String name, int projid, int userid, String userChosenID,
                        String fullName,
                        int numItems, int duration, int minScore, boolean showAudio, boolean isPrivate) {
    super(id, name);
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
}
