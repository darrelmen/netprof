package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by GO22670 on 1/30/14.
 */
public class UserListManagerTest  {
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
      File db = new File(getConfigDir(),test + ".h2.db");
      if (db.exists()) {
        if (!db.delete()) {
          logger.error("huh? couldn't delete " + db.getAbsolutePath());

        }
      }
      else {
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
    assertTrue(users.isEmpty());
    long user = database.addUser(89, "male", 1, "", "english", "boston", "test");
    assertTrue(database.getUsers().size() == 1);

    UserListManager userListManager = database.getUserListManager();

    assertTrue(userListManager.getListsForUser(user,false).isEmpty());

    long listid = userListManager.addUserList(user, "test", "", "");
    assertTrue(userListManager.getListsForUser(user,false).size()==1);

  }
}
