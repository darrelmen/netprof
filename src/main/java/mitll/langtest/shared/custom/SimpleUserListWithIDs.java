package mitll.langtest.shared.custom;

import java.util.Collection;

public class SimpleUserListWithIDs extends SimpleUserList implements IUserListWithIDs {
  private Collection<Integer> ids;

  public SimpleUserListWithIDs() {
  }

  public SimpleUserListWithIDs(int id, String name, int projid, int userid, String userChosenID,
                               Collection<Integer> ids) {
    super(id, name, projid, userid, userChosenID, ids.size());
    this.ids = ids;
  }

/*  public Collection<Integer> getIds() {
    return ids;
  }*/

  public boolean containsByID(int id) {
    return ids.contains(id);
  }
}
