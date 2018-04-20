package mitll.langtest.shared.custom;

public interface IUserList extends IUserListLight {
  int getProjid();

  int getUserID();
  String getUserChosenID();

  int getNumItems();

  int getRoundTimeMinutes();
  int getMinScore();
  boolean shouldShowAudio();
}
