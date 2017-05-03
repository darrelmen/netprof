package mitll.langtest.server.database.userlist;

import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.List;

/**
 * Created by go22670 on 3/8/17.
 */
public interface UserListServices {
  IUserListManager getUserListManager();

//  UserList<CommonShell> getUserListByID(long listid, int projectid);
  //List<UserList<CommonShell>> getUserListByName(long listid, int projectid);

  UserList<CommonExercise> getUserListByIDExercises(long listid, int projectid);
}
