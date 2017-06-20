package mitll.langtest.server.database.userlist;

import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;

/**
 * Created by go22670 on 3/8/17.
 */
public interface UserListServices {
  IUserListManager getUserListManager();
  UserList<CommonExercise> getUserListByIDExercises(long listid, int projectid);
}
