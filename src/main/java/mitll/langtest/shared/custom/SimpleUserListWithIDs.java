package mitll.langtest.shared.custom;

import java.util.Collection;

public class SimpleUserListWithIDs extends SimpleUserList implements IUserListWithIDs {
  private Collection<Integer> ids;

  public SimpleUserListWithIDs() {
  }

  /**
   *
   * @param id
   * @param name
   * @param projid
   * @param userid
   * @param userChosenID
   * @param ids
   * @param duration
   */
  public SimpleUserListWithIDs(int id, String name, int projid, int userid, String userChosenID,
                               Collection<Integer> ids, int duration) {
    super(id, name, projid, userid, userChosenID, ids.size(), duration, 30, false);
    this.ids = ids;
  }

  public boolean containsByID(int id) {
    return ids.contains(id);
  }
}
