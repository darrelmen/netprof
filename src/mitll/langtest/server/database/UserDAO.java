package mitll.langtest.server.database;

import mitll.langtest.shared.User;
import mitll.langtest.shared.grade.Grader;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserDAO extends DAO {
  private static Logger logger = Logger.getLogger(UserDAO.class);
  public static final String USERS = "users";

  public UserDAO(Database database) {
    super(database);
    try {
      createUserTable(database);
    } catch (Exception e) {
      logger.error("got "+e,e);
    }
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
   * @param nativeLang
   * @param dialect
   * @param userID
   * @param enabled
   * @return newly inserted user id, or 0 if something goes horribly wrong
   */
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, boolean enabled) {
    try {
      // there are much better ways of doing this...
      long max = 0;
      for (User u : getUsers()) if (u.id > max) max = u.id;
      logger.info("addUser : max is " + max + " new user '" + userID + "' age " +age + " gender " + gender);

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
          "INSERT INTO " +
            USERS +
            "(id,age,gender,experience,ipaddr,nativeLang,dialect,userID,enabled) " +
          "VALUES(?,?,?,?,?,?,?,?,?);");
      int i = 1;
      long newID = max + 1;
      statement.setLong(i++, newID);
      statement.setInt(i++, age);
      statement.setInt(i++, gender.equalsIgnoreCase("male") ? 0 : 1);
      statement.setInt(i++, experience);
      statement.setString(i++, ipAddr);
      statement.setString(i++, nativeLang);
      statement.setString(i++, dialect);
      statement.setString(i++, userID);
      statement.setBoolean(i++, enabled);
      statement.executeUpdate();

      statement.close();
      database.closeConnection(connection);

      return newID;
    } catch (Exception ee) {
      logger.error("Got " + ee);
    }
    return 0;
  }

  public void enableUser(long id, boolean enabled) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      String sql = "UPDATE users " +
          "SET enabled=" +enabled+
          " WHERE id=" + id;
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

  /**
   * @see DatabaseImpl#userExists(String)
   * @param id
   * @return
   */
  public int userExists(String id) {
    int val = -1;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT id from users where userID='" +
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

  public void createUserTable(Database database) throws Exception {
    Connection connection = database.getConnection();

    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists users (" +
        "id IDENTITY, " +
        "age INT, " +
        "gender INT, " +
        "experience INT, " +
        "ipaddr VARCHAR, " +
        "password VARCHAR, " +
        "nativeLang VARCHAR, " +
        "dialect VARCHAR, " +
        "userID VARCHAR, " +
        "timestamp TIMESTAMP AS CURRENT_TIMESTAMP, " +
        "enabled BOOLEAN, " +
        "CONSTRAINT pkusers PRIMARY KEY (id))");
    statement.execute();
    statement.close();
    database.closeConnection(connection);

    //int numColumns = getNumColumns(connection, "users");
    //logger.debug("found " + numColumns + " in users table");

    Set<String> expected = new HashSet<String>();
    expected.addAll(Arrays.asList("id","age","gender","experience",
      //"firstname","lastname",
      "ipaddr","nativelang","dialect","userid","timestamp","enabled"));
    /*boolean users = */expected.removeAll(getColumns(USERS));
    if (!expected.isEmpty()) logger.info("adding columns for " + expected);
    for (String missing : expected) {
      //if (missing.equalsIgnoreCase("firstName")) { addColumn(connection,"firstName","VARCHAR"); }
      //if (missing.equalsIgnoreCase("lastName")) { addColumn(connection,"lastName","VARCHAR"); }

      if (missing.equalsIgnoreCase("nativeLang")) { addColumn(connection,"nativeLang","VARCHAR"); }
      if (missing.equalsIgnoreCase("dialect")) { addColumn(connection,"dialect","VARCHAR"); }
      if (missing.equalsIgnoreCase("userID")) { addColumn(connection,"userID","VARCHAR"); }
      if (missing.equalsIgnoreCase("timestamp")) { addColumn(connection,"timestamp","TIMESTAMP AS CURRENT_TIMESTAMP"); }
      if (missing.equalsIgnoreCase("enabled")) { addColumn(connection,"enabled","BOOLEAN"); }
    }

    convertGraders();
  }

  /**
   * @deprecated lets get rid of this eventually
   */
  private void convertGraders() {
    GraderDAO graderDAO = new GraderDAO(database);
    Collection<Grader> graders = graderDAO.getGraders();
    for (Grader u : graders) {
      if (userExists(u.name) == -1) {
        logger.info("adding grader " + u);
        addUser(0, "male", 240, "", "", "", u.name, true);
      }
    }
  }

  private void addColumn(Connection connection, String column, String type) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE users ADD " +
      column + " " + type);
    statement.execute();
    statement.close();
  }

  public void dropUserTable(Database database) throws Exception {
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
  public List<User> getUsers() {
    String sql = "SELECT * from users;";
    return getUsers(sql);
  }  

  /**
   * @seex OnlineUsers#getUser(long)
   * @param userid
   * @return
   */
  public User getUserWhere(long userid) {
    String sql = "SELECT * from users where id=" +userid+";";
    List<User> users = getUsers(sql);
    if (users.isEmpty()) {
      if (userid > 0) {
        logger.warn("no user with id " + userid);
      }
      return null;
    }
    else if (users.size() > 1) {
      logger.warn("huh? " + users.size() + " with  id " + userid);
    }

    return users.iterator().next();
  }

  private List<User> getUsers(String sql) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      List<User> users = getUsers(rs);
      rs.close();
      statement.close();
      database.closeConnection(connection);

      return users;
    } catch (Exception ee) {
      logger.error("Got " + ee);
    }
    return new ArrayList<User>();
  }

  private List<User> getUsers(ResultSet rs) throws SQLException {
    int i;

    List<User> users = new ArrayList<User>();
    int columnCount = rs.getMetaData().getColumnCount();
    if (columnCount == 7) {
      while (rs.next()) {
        i = 1;
        users.add(new User(rs.getLong(i++), //id
          rs.getInt(i++), // age
          rs.getInt(i++), //gender
          rs.getInt(i++), // exp
          rs.getString(i++), // ip
          rs.getString(i++), // password
          rs.getBoolean(i++)));
      }
    } else {
      while (rs.next()) {
       // i = 1;
        String userid;
        User newUser = new User(rs.getLong("id"), //id
          rs.getInt("age"), // age
          rs.getInt("gender"), //gender
          rs.getInt("experience"), // exp
          rs.getString("ipaddr"), // ip
          rs.getString("password"), // password

          // first
          // last
          rs.getString("nativeLang"), // native
          rs.getString("dialect"), // dialect
          userid = rs.getString("userid"), // userid
          rs.getTimestamp("timestamp").getTime(),

          rs.getBoolean("enabled"),
          userid != null && (userid.equals("gvidaver") | userid.equals("swade")));
        users.add(newUser);
        if (newUser.userID == null) newUser.userID = ""+newUser.id;
      }
    }
    return users;
  }

  public boolean isUserMale(long userID) {
    List<User> users = getUsers();
    return isUserMale(userID, users);
  }

  public boolean isUserMale(long userID, List<User> users) {
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
    return getUserMap(getMale, users);
  }

  public Map<Long, User> getUserMap(boolean getMale, List<User> users) {
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
    return getMap(users);
  }

  public Map<Long, User> getMap(List<User> users) {
    Map<Long, User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      idToUser.put(u.id, u);
    }
    return idToUser;
  }

  public Map<Long, User> getNativeUserMap() {
    List<User> users = getUsers();
    Map<Long, User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      if (u.experience > 239) {
        idToUser.put(u.id, u);
      }
    }
    return idToUser;
  }

  public Set<Long> getNativeUsers() {
    return getNativeUserMap().keySet();
  }
}