package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    UserList userList = getUserList(userid, name, description, dliClass);
    if (userList == null) return -1;
    else return userList.getUniqueID();
  }

  public UserList getUserList(long userid, String name, String description, String dliClass) {
    User userWhere = userDAO.getUserWhere(userid);
    if (userWhere == null) {
      logger.error("huh? no user with id " + userid);
      return null;
    } else {
      UserList e = new UserList(i++, userWhere, name, description, dliClass);
      userLists.add(e);
      logger.debug("now there are " + userLists.size() + " lists for " + userid);
      return e;
    }
  }

  public Collection<UserList> getListsForUser(int userid) {
    if (userid == -1) return Collections.emptyList();
    logger.debug("getListsForUser for " + userid);

    List<UserList> listsForUser = new ArrayList<UserList>();
    for (UserList userList : userLists) {
      if (userList.getCreator().id == userid) {
        listsForUser.add(userList);
      }
    }

    if (listsForUser.isEmpty()) {
      UserList userList = getUserList(userid, "My List", "Default list", "Choose a class");
      if (userList == null) return Collections.emptyList();
      else return Collections.singletonList(userList);
    }

    Collections.sort(userLists,new Comparator<UserList>() {
      @Override
      public int compare(UserList o1, UserList o2) {
        return o1.getModified() > o2.getModified() ? -1 : o1.getModified() < o2.getModified()? +1 : 0;  //To change body of implemented methods use File | Settings | File Templates.
      }
    });

    logger.debug("getListsForUser " + listsForUser.size() + " for " + userid);

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
