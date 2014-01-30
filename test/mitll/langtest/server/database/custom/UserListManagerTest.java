package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by GO22670 on 1/30/14.
 */
public class UserListManagerTest {
  private static Logger logger = Logger.getLogger(UserListManagerTest.class);
  private static DatabaseImpl database;
  private static String test;

  @BeforeClass
  public static void setup() {
    File file = new File("war" + File.separator + "config" + File.separator + "english" + File.separator + "quizlet.properties");
    logger.debug("config dir " + file.getParent());
    logger.debug("config     " + file.getName());
    test = "test";
    database = new DatabaseImpl(file.getParent(), file.getName(), test, false);
  }

  @AfterClass
  public static void tearDown() {
    try {
      database.closeConnection();
      File db = new File(getConfigDir(), test + ".h2.db");
      if (db.exists()) {
        if (!db.delete()) {
          logger.error("huh? couldn't delete " + db.getAbsolutePath());

        }
      } else {
        logger.error("huh? no " + db.getAbsolutePath());
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private static String getConfigDir() {
    return "war" + File.separator + "config" + File.separator + "english";
  }

  @Test
  public void testAdd() {
    List<User> users = database.getUsers();
    long user;
    if (users.isEmpty()) {
      users = addAndGetUsers("test1");
    }
    user = users.iterator().next().id;
    UserListManager userListManager = database.getUserListManager();

    Collection<UserList> listsForUser = userListManager.getListsForUser(user, false);
    int size = listsForUser.size();
    assertTrue("size is " + size, size == 1);
    assertTrue("first is favorite", listsForUser.iterator().next().isFavorite());

    // add
    long listid = addListCheck(user, userListManager, "test");
    // what happens if add list a second time?
    long listid2 = addListCheck(user, userListManager, "test", false);

    // remove
    removeList(user, userListManager, listid, true);
    assertFalse(userListManager.listExists(listid));

    // remove again
    removeList(user, userListManager, listid, false);

    // make sure we can remove lists that have been visited
    // testAddVisitor(user, userListManager);
  }

  @Test
  public void testAddVisitor() {
    List<User> users = addAndGetUsers("test2");
    logger.debug("1 users size " + users.size());

    logger.debug("2 users size " + users.size());

    User user = users.iterator().next();
    UserListManager userListManager = database.getUserListManager();

    long listid = addListCheck(user.id, userListManager, "test");
    assertTrue(userListManager.getUserListsForText("").contains(userListManager.getUserListByID(listid)));

    Iterator<UserList> iterator = userListManager.getListsForUser(user.id, false).iterator();
    UserList favorite = iterator.next();
    assertTrue(favorite.isFavorite());
    UserList test = iterator.next();
    assertFalse(test.isFavorite());
    assertTrue(test.getName().equals("test"));

    long visitor = getUser("visitor");
    int size = database.getUsers().size();
    assertTrue("size is " +size,size >= 2);

    Collection<UserList> listsForUser = userListManager.getListsForUser(visitor, false);
    assertTrue(!listsForUser.contains(test));  // haven't visited yet, shouldn't see it

    userListManager.addVisitor(test, visitor);
    listsForUser = userListManager.getListsForUser(visitor, false);
    assertTrue(listsForUser.contains(test));   // now that I visited, I should see it on my list
    Collection<UserList> listsForUser2 = userListManager.getListsForUser(visitor, true);
    assertTrue(!listsForUser2.contains(test)); // list isn't mine, I'm just a visitor

    removeList(user.id, userListManager, listid);

    // after removing, I shouldn't see it
    Collection<UserList> listsForUser3 = userListManager.getListsForUser(visitor, false);
    assertTrue(!listsForUser3.contains(test));
  }

  @Test
  public void testAddVisitor2() {
    List<User> users = database.getUsers();
    logger.debug("1 users size " + users.size());
    if (users.isEmpty()) {
      users = addAndGetUsers("test2");
    }

    logger.debug("2 users size " + users.size());

    User owner = users.iterator().next();
    UserListManager userListManager = database.getUserListManager();

    long listid = addListCheck(owner.id, userListManager, "test");
    UserList testList = userListManager.getUserListByID(listid);
    assertTrue(userListManager.getUserListsForText("").contains(testList));

    long visitor = getUser("visitor");
    int size = database.getUsers().size();
    assertTrue("size was " +size,size == 2);

    userListManager.addVisitor(testList, visitor);

    // what happens if the owner adds himself as a visitor
    userListManager.addVisitor(testList, owner.id);

    Collection<UserList> listsForUser = userListManager.getListsForUser(owner.id, false);
    assertTrue(listsForUser.contains(testList));   // should be able to see it, it's mine

    listsForUser = userListManager.getListsForUser(owner.id, true);
    assertTrue(listsForUser.contains(testList));  // should be able to see it, it's mine

    listsForUser = userListManager.getListsForUser(visitor, false);
    assertTrue(listsForUser.contains(testList));   // should be able to see it, it's mine

    listsForUser = userListManager.getListsForUser(visitor, true);
    assertTrue(!listsForUser.contains(testList));   // should be able to see it, it's mine

    removeList(owner.id, userListManager, listid);

    // after removing, I shouldn't see it
    listsForUser = userListManager.getListsForUser(visitor, false);
    assertTrue(!listsForUser.contains(testList));

    listsForUser = userListManager.getListsForUser(owner.id, false);
    assertTrue(!listsForUser.contains(testList));
  }

  private List<User> addAndGetUsers(String test2) {
    List<User> users;
    long l = getUser(test2);
    users = database.getUsers();
//    assertTrue(users.size() == 1);
    assertTrue(database.userExists(test2) == l);
    return users;
  }

  private long getUser(String test2) {
    long l = database.userExists(test2);
    if (l == -1) {
      l = database.addUser(89, "male", 1, "", "english", "boston", test2);
    }
    return l;
  }

  private long addListCheck(long user, UserListManager userListManager, String name) {
    return addListCheck(user, userListManager, name, true);
  }

  private long addListCheck(long user, UserListManager userListManager, String name, boolean expectSuccess) {
    long listid = addList(user, userListManager, name);
    if (expectSuccess) {
      assertTrue(listid != -1);
      assertTrue("got list id " + listid, userListManager.listExists(listid));
      assertTrue(userListManager.hasByName(user, name));
      UserList userListByID = userListManager.getUserListByID(listid);
      assertNotNull(userListByID);
    } else {
      assertTrue(listid == -1);
    }
    return listid;
  }

  private void removeList(long user, UserListManager userListManager, long listid) {
    removeList(user, userListManager, listid, true);
  }

  private void removeList(long user, UserListManager userListManager, long listid, boolean expectSuccess) {
    boolean b = userListManager.deleteList(listid);
    if (expectSuccess) {
      assertTrue(b);
    } else {
      assertFalse(b);
    }
    Collection<UserList> listsForUser2 = userListManager.getListsForUser(user, false);
    assertTrue(listsForUser2.size() == 1);
  }

  private long addList(long user, UserListManager userListManager, String name) {
    long listid = userListManager.addUserList(user, name, "", "");
    logger.debug("adding list " + name + " got " + listid);
    Collection<UserList> listsForUser1 = userListManager.getListsForUser(user, false);
    assertTrue(" size is " + listsForUser1.size(), listsForUser1.size() == 2);
    return listid;
  }
}
