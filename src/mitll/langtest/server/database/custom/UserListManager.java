package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.database.UserExerciseDAO;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

 // private List<UserList> userLists = new ArrayList<UserList>();

  private UserExerciseDAO userExerciseDAO;
  private int userExerciseCount = 0;
  private UserListDAO userListDAO;
  private UserListExerciseJoinDAO userListExerciseJoinDAO;

  public UserListManager(Database database,UserDAO userDAO) {
    this.userDAO = userDAO;
    this.userListDAO = new UserListDAO(database, userDAO);
    userListExerciseJoinDAO = new UserListExerciseJoinDAO(database, userDAO);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUserList(long, String, String, String)
   * @see mitll.langtest.client.custom.Navigation#doCreate
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @return
   */
  public long addUserList(long userid, String name, String description, String dliClass) {
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
      UserList e = new UserList(i++, userWhere, name, description, dliClass, System.currentTimeMillis(), isPrivate);
     // userLists.add(e);
      userListDAO.add(e);
      logger.debug("createUserList : now there are " + userListDAO.getCount() + " lists for " + userid);
      return e;
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getListsForUser
   * @param userid
   * @param onlyCreated
   * @return
   */
  public Collection<UserList> getListsForUser(long userid, boolean onlyCreated) {
    if (userid == -1) return Collections.emptyList();
    logger.debug("getListsForUser for " + userid);

    List<UserList> listsForUser = new ArrayList<UserList>();
    for (UserList userList : userListDAO.getAll()) {
      boolean isCreator = userList.getCreator().id == userid;
      if (onlyCreated) {
        if (isCreator) {
          listsForUser.add(userList);
        }
      } else {
        if (userList.getVisitorIDs().contains(userid) || isCreator) {
          listsForUser.add(userList);
        }
      }
    }

    if (listsForUser.isEmpty()) {
      UserList userList = createUserList(userid, MY_LIST, "My Favorites", "", true);
      if (userList == null) return Collections.emptyList();
      else return Collections.singletonList(userList);
    }

  //  sortByTime(listsForUser);

    logger.debug("getListsForUser " + listsForUser.size() + "(" +listsForUser+
      ") for " + userid);

    return listsForUser;
  }

/*  private void sortByTime(List<UserList> listsForUser) {
    Collections.sort(listsForUser, new Comparator<UserList>() {
      @Override
      public int compare(UserList o1, UserList o2) {
        return o1.getModified() > o2.getModified() ? -1 : o1.getModified() < o2.getModified() ? +1 : 0;
      }
    });
  }*/

  public Collection<UserExercise> addItemToUserList(long userListID, UserExercise userExercise) {
    UserList where = userListDAO.getWithExercises(userListID);

    if (where != null) {
      where.addExercise(userExercise);
      userListExerciseJoinDAO.add(where,userExercise);
      return where.getExercises();
    }

/*    for (UserList userList : userLists) {
      if (userList.getUniqueID() == userListID) {
        userList.addExercise(userExercise);
        logger.debug("addItemToUserList " + userList);
        return userList.getExercises();
      }
    }*/

    return Collections.emptyList();
  }

  public List<UserList> getUserListsForText(String search) {
    List<UserList> listsForUser = new ArrayList<UserList>(userListDAO.getAllPublic());
  //  sortByTime(listsForUser);
/*    Iterator<UserList> iterator = listsForUser.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().isPrivate()) iterator.remove();
    }

    System.out.println("before " + userLists.size() + " after " + listsForUser.size());*/

    return listsForUser;  //To change body of created methods use File | Settings | File Templates.
  }

  public UserExercise createNewItem(long userid, String english, String foreign) {
    int uniqueID = userExerciseCount++;
    return new UserExercise(uniqueID, userid, english, foreign);
  }

  public void reallyCreateNewItem(UserList userList, UserExercise userExercise) {
    userExerciseDAO.add(userExercise);

  //  boolean found = false;

    UserList where = userListDAO.getWhere(userList.getUniqueID());
    if (where != null) {
      userListExerciseJoinDAO.add(where,userExercise);
    }

/*    for (UserList ul : userLists) {
      if (ul.getUniqueID() == userList.getUniqueID()) {
        ul.addExercise(userExercise);
        logger.debug("now " + ul + " after adding " + userExercise);
        found = true;
      }
    }*/
    if (where == null) logger.error("reallyCreateNewItem : couldn't find ul with id " + userList.getUniqueID());
  }

  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
    userListDAO.setUserExerciseDAO(userExerciseDAO);
  }

  public UserList getUserListByID(long id) {
    return userListDAO.getWithExercises(id);
  }

  public void addVisitor(UserList userList, long user) {
    UserList where = userListDAO.getWhere(userList.getUniqueID());
    if (where != null) {
      userListDAO.addVisitor(where.getUniqueID(), user);
    }
  }

/*  public UserExerciseDAO getUserExerciseDAO() {
    return userExerciseDAO;
  }*/
}
