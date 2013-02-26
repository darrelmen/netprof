package mitll.langtest.server.database;

import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDAO extends DAO {
  private static Logger logger = Logger.getLogger(UserDAO.class);

 // private final Database database;

  public UserDAO(Database database) {
    super(database);
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see DatabaseImpl#addUser
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr
   * @param firstName
   * @param lastName
   * @param nativeLang
   * @param dialect
   * @param userID
   * @param enabled
   * @return newly inserted user id, or 0 if something goes horribly wrong
   */
  public synchronized long addUser(int age, String gender, int experience, String ipAddr, String firstName,
                                   String lastName, String nativeLang, String dialect, String userID, boolean enabled) {
    try {
      // there are much better ways of doing this...
      long max = 0;
      for (User u : getUsers()) if (u.id > max) max = u.id;
      logger.info("addUser : max is " + max + " new user '" + userID + "' age " +age +
          " gender " + gender + " '" + firstName + " " + lastName +"'");

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
          "INSERT INTO users(id,age,gender,experience,ipaddr,firstName,lastName,nativeLang,dialect, userID,enabled) " +
          "VALUES(?,?,?,?,?,?,?,?,?,?,?);");
      int i = 1;
      long newID = max + 1;
      statement.setLong(i++, newID);
      statement.setInt(i++, age);
      statement.setInt(i++, gender.equalsIgnoreCase("male") ? 0 : 1);
      statement.setInt(i++, experience);
      statement.setString(i++, ipAddr);
      statement.setString(i++, firstName);
      statement.setString(i++, lastName);
      statement.setString(i++, nativeLang);
      statement.setString(i++, dialect);
      statement.setString(i++, userID);
      statement.setBoolean(i++, enabled);
      statement.executeUpdate();

      statement.close();
      database.closeConnection(connection);

      return newID;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return 0;
  }

  public void enableUser(long id, boolean enabled) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      String sql = "UPDATE users " +
          "SET enabled=" +enabled+
          " " +
          "WHERE id=" + id;
      logger.debug("enableUser " + id);
      statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? didn't update " + id);
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized int userExists(String id) {
    int val = -1;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;
      statement = connection.prepareStatement("SELECT id from users where userID='" +
          id +
          "'");
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        val = rs.getInt(1);
      }
  //    logger.debug("user exists " + id + " = " + val);
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return val;
  }

  void createUserTable(Database database) throws Exception {
    Connection connection = database.getConnection();

    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists users (" +
        "id IDENTITY, " +
        "age INT, " +
        "gender INT, " +
        "experience INT, " +
        "ipaddr VARCHAR, " +
        "password VARCHAR, " +
        "firstName VARCHAR, " +
        "lastName VARCHAR, " +
        "nativeLang VARCHAR, " +
        "dialect VARCHAR, " +
        "userID VARCHAR, " +
        "timestamp TIMESTAMP AS CURRENT_TIMESTAMP, " +
        "enabled BOOLEAN, " +
        "CONSTRAINT pkusers PRIMARY KEY (id))");
    statement.execute();
    statement.close();
    database.closeConnection(connection);

    int numColumns = getNumColumns(connection, "users");
    logger.debug("found " + numColumns + " in users table");
    if (numColumns < 13 && numColumns != 7) {
      statement = connection.prepareStatement("ALTER TABLE users ADD enabled BOOLEAN");
      statement.execute();
      statement.close();
    }
  }

  void dropUserTable(Database database) throws Exception {
    System.err.println("----------- dropUserTable -------------------- ");
    Connection connection = database.getConnection();
    PreparedStatement statement;
    statement = connection.prepareStatement("drop TABLE users");
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  /**
   * Pulls the list of users out of the database.
   * @return
   */
  public synchronized List<User> getUsers() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement("SELECT * from users;");
      int i;

      ResultSet rs = statement.executeQuery();
      List<User> users = new ArrayList<User>();
      while (rs.next()) {
    	  i = 1;
        String userid;
        users.add(new User(rs.getLong(i++), //id
          rs.getInt(i++), // age
          rs.getInt(i++), //gender
          rs.getInt(i++), // exp
          rs.getString(i++), // ip
            rs.getString(i++), // password
            rs.getString(i++), // first
            rs.getString(i++), // last
            rs.getString(i++), // native
            rs.getString(i++), // dialect
            userid = rs.getString(i++), // userid
            rs.getTimestamp(i++).getTime(),
            rs.getBoolean(i++),
            userid.equals("gvidaver") | userid.equals("swade")));
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

      return users;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<User>();
  }

  public boolean isUserMale(long userID) {
    List<User> users = getUsers();
    User thisUser = null;
    for (User u : users) {
      if (u.id == userID) {
        thisUser = u;
        break;
      }
    }
    return thisUser != null && thisUser.isMale();
  }

  public Map<Long, User> getUserMap(boolean getMale) {
    List<User> users = getUsers();
    Map<Long,User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      if (u.isMale() && getMale || (!u.isMale() && !getMale)) {
        idToUser.put(u.id, u);
      }
    }
    return idToUser;
  }

  public Map<Long, User> getUserMap() {
    List<User> users = getUsers();
    Map<Long, User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      idToUser.put(u.id, u);
    }
    return idToUser;
  }
}