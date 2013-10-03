package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListManager {
  private static Logger logger = Logger.getLogger(UserListManager.class);

  private final UserDAO userDAO;
  // TODO add a DAO -- do something smarter!
  int i = 0;
  List<UserList> userLists = new ArrayList<UserList>();

  public UserListManager(UserDAO userDAO) { this.userDAO = userDAO; }
  public int addUserList(long userid, String name, String description, String dliClass) {
    User userWhere = userDAO.getUserWhere(userid);
    UserList e = new UserList(i++, userWhere, name, description, dliClass);
    userLists.add(e);
    logger.debug("now there are " + userLists.size() + " lists for " + userid);
    return e.getUniqueID();
  }

  public Collection<UserList> getListsForUser(int userid) {
    List<UserList> listsForUser = new ArrayList<UserList>();
    for (UserList userList : userLists) {
      if (userList.getCreator().id == userid) {
        listsForUser.add(userList);
      }
    }
    return listsForUser;
  }

  public List<Exercise> addItemToUserList(int userListID, UserExercise userExercise) {
    for (UserList userList : userLists) {
      if (userList.getUniqueID() == userListID) {
        userList.addExercise(userExercise.toExercise());
        return userList.getExercises();
      }
    }

    // TODO : serialize user exercise in DAO
    return new ArrayList<Exercise>();
  }

  public List<UserList> getUserListsForText(String search) {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }


}
