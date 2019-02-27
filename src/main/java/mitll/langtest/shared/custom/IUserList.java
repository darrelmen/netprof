package mitll.langtest.shared.custom;

public interface IUserList extends IUserListLight {
  int getProjid();

  int getUserID();
  String getUserChosenID();
  String getFirstInitialName();

  int getNumItems();

  int getRoundTimeMinutes();
  int getMinScore();
  boolean shouldShowAudio();

  boolean isPrivate();

  void setNumItems(int numItems);
}
