package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.database.UserExerciseDAO;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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

  public static final String MY_LIST = "Favorites";

  private final UserDAO userDAO;
  // TODO add a DAO -- do something smarter!
  private int i = 0;
  private List<UserList> userLists = new ArrayList<UserList>();
  private UserExerciseDAO userExerciseDAO;
  int userExerciseCount = 0;

  public UserListManager(UserDAO userDAO) { this.userDAO = userDAO; }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUserList(long, String, String, String)
   * @see mitll.langtest.client.custom.Navigation#doCreate
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @return
   */
  public int addUserList(long userid, String name, String description, String dliClass) {
    UserList userList = createUserList(userid, name, description, dliClass, false);
    if (userList == null) return -1;
    else return userList.getUniqueID();
  }

  private UserList createUserList(long userid, String name, String description, String dliClass, boolean isPrivate) {
    User userWhere = userDAO.getUserWhere(userid);
    if (userWhere == null) {
      logger.error("huh? no user with id " + userid);
      return null;
    } else {
      UserList e = new UserList(i++, userWhere, name, description, dliClass, isPrivate);
      userLists.add(e);
      logger.debug("createUserList : now there are " + userLists.size() + " lists for " + userid);
      return e;
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getListsForUser(int, boolean)
   * @param userid
   * @param onlyCreated
   * @return
   */
  public Collection<UserList> getListsForUser(int userid, boolean onlyCreated) {
    if (userid == -1) return Collections.emptyList();
    logger.debug("getListsForUser for " + userid);

    List<UserList> listsForUser = new ArrayList<UserList>();
    for (UserList userList : userLists) {
      boolean isCreator = userList.getCreator().id == userid;
      if (onlyCreated) {
        if (isCreator) {
          listsForUser.add(userList);
        }
      } else {
        if (userList.getVisitorIDs().contains((long) userid) || isCreator) {
          listsForUser.add(userList);
        }
      }
    }

    if (listsForUser.isEmpty()) {
      UserList userList = createUserList(userid, MY_LIST, "My Favorites", "", true);
      if (userList == null) return Collections.emptyList();
      else return Collections.singletonList(userList);
    }

    sortByTime(listsForUser);

    logger.debug("getListsForUser " + listsForUser.size() + "(" +listsForUser+
      ") for " + userid);

    return listsForUser;
  }

  private void sortByTime(List<UserList> listsForUser) {
    Collections.sort(listsForUser, new Comparator<UserList>() {
      @Override
      public int compare(UserList o1, UserList o2) {
        return o1.getModified() > o2.getModified() ? -1 : o1.getModified() < o2.getModified() ? +1 : 0;  //To change body of implemented methods use File | Settings | File Templates.
      }
    });
  }

  public List<UserExercise> addItemToUserList(int userListID, UserExercise userExercise) {
    for (UserList userList : userLists) {
      if (userList.getUniqueID() == userListID) {
        userList.addExercise(userExercise);
        logger.debug("addItemToUserList " + userList);
        return userList.getExercises();
      }
    }

    // TODO : serialize user exercise in DAO
    return Collections.emptyList();
  }

  public List<UserList> getUserListsForText(String search) {
    List<UserList> listsForUser = new ArrayList<UserList>(userLists);
    sortByTime(listsForUser);
    Iterator<UserList> iterator = listsForUser.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().isPrivate()) iterator.remove();
    }

    System.out.println("before " + userLists.size() + " after " + listsForUser.size());

    return listsForUser;  //To change body of created methods use File | Settings | File Templates.
  }

  public UserExercise createNewItem(long userid, String english, String foreign) {
    int uniqueID = userExerciseCount++;
    return new UserExercise(uniqueID, userid, english, foreign);
  }

  // TODO make a dao for this...
  public void reallyCreateNewItem(UserList userList, UserExercise userExercise) {
    userExerciseDAO.add(userExercise);

    boolean found = false;
    for (UserList ul : userLists) {
      if (ul.getUniqueID() == userList.getUniqueID()) {
        ul.addExercise(userExercise);
        logger.debug("now " + ul + " after adding " + userExercise);
        found = true;
      }
    }
    if (!found) logger.error("couldn't find ul with id " + userList.getUniqueID() + " in " + userLists);
  }

  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
  }

  public UserList getUserListByID(int id) {
    for (UserList ul : userLists) if (ul.getUniqueID() == id) return ul;
    return null;
  }

/*  public UserExerciseDAO getUserExerciseDAO() {
    return userExerciseDAO;
  }*/
}
