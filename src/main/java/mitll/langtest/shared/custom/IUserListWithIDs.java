package mitll.langtest.shared.custom;

/**
 * @see mitll.langtest.client.scoring.UserListSupport#populateListChoices
 */
public interface IUserListWithIDs extends IUserList {
  boolean containsByID(int id);
}
