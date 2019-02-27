package mitll.langtest.shared.custom;

import java.util.Collection;

public class SimpleUserListWithIDs extends SimpleUserList implements IUserListWithIDs {
  private Collection<Integer> ids;

  public SimpleUserListWithIDs() {
  }

  /**
   * @param id
   * @param name
   * @param projid
   * @param userid
   * @param userChosenID
   * @param firstInitialName
   * @param ids
   * @param duration
   */
  public SimpleUserListWithIDs(int id, String name, int projid, int userid, String userChosenID,
                               String firstInitialName, Collection<Integer> ids, int duration, boolean isPrivate) {
    super(id, name, projid, userid, userChosenID, firstInitialName, ids.size(), duration, 30, false, isPrivate);
    this.ids = ids;
  }

  public boolean containsByID(int id) {
    return ids.contains(id);
  }
}
