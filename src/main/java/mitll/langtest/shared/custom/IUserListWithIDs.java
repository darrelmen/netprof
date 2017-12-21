package mitll.langtest.shared.custom;

public interface IUserListWithIDs extends IUserList {
  boolean containsByID(int id);
}
