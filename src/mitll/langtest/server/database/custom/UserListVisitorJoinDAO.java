package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListVisitorJoinDAO extends DAO {
  private static Logger logger = Logger.getLogger(UserListVisitorJoinDAO.class);

  public static final String USER_EXERCISE_LIST_VISITOR = "userexerciselist_visitor";

  public UserListVisitorJoinDAO(Database database, UserDAO userDAO) {
    super(database);
    try {
      createUserListTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  void createUserListTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USER_EXERCISE_LIST_VISITOR +
      " (" +
      "uniqueid IDENTITY, " +
      "userlistid LONG, " +
      "visitorid LONG, " +
      "FOREIGN KEY(userlistid) REFERENCES " +
      UserListDAO.USER_EXERCISE_LIST +
      "(uniqueid)," +
      "FOREIGN KEY(visitorid) REFERENCES " +
      UserDAO.USERS +
      "(ID)" +
      ")");
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see UserListManager#reallyCreateNewItem(long, mitll.langtest.shared.custom.UserExercise)
   */
  public void add(long uniqueID, long visitor) {
    long id = 0;

    if (existsAlready(uniqueID,visitor)) return;
    try {
      // there are much better ways of doing this...
      logger.info("add :visitor " +visitor+
        " to " + uniqueID);

      Connection connection = database.getConnection();
      PreparedStatement statement;

 /*     String prefix = "IF NOT EXISTS (SELECT visitorid" +
        " FROM " + USER_EXERCISE_LIST_VISITOR +
        " WHERE visitorid = " + visitor +
        " AND userlistid = " + uniqueID +
        ") ";*/
      statement = connection.prepareStatement(
        /*prefix +*/ "INSERT INTO " + USER_EXERCISE_LIST_VISITOR +
          "(userlistid,visitorid) " +
          "VALUES(?,?);");
      int i = 1;
      //     statement.setLong(i++, userList.getUserID());
   //   long uniqueID = userList.getUniqueID();
      statement.setLong(i++, uniqueID);
      statement.setLong(i++, visitor);

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      if (rs.next()) {
        id = rs.getLong(1);
      } else {
        logger.error("huh? no key was generated?");
      }
      logger.debug("unique id = " + id);

      statement.close();
      database.closeConnection(connection);

      logger.debug("now " + getCount(USER_EXERCISE_LIST_VISITOR) + " in " + USER_EXERCISE_LIST_VISITOR);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }


  /**
   * Pulls the list of users out of the database.
   *
   * @return
   */
/*  public List<UserList> getAll() {
    try {
      String sql = "SELECT * from " + USER_EXERCISE_LIST + " order by modified";
      return getUserLists(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }*/


/*  public Collection getWhere(long unique) {
    //String unique = exid.substring("Custom_".length());
    String sql = "SELECT * from " + USER_EXERCISE_LIST + " where uniqueid=" + unique + " order by modified";
    try {
      List<UserList> userExercises = getUserLists(sql);
      if (userExercises.isEmpty()) {
        logger.error("huh? no custom exercise with id " + unique);
        return null;
      } else return userExercises.iterator().next();
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }*/
/*
  private List<UserList> getUserLists(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    int i;

    ResultSet rs = statement.executeQuery();
    List<UserList> exercises = new ArrayList<UserList>();

    while (rs.next()) {
      i = 1;
      exercises.add(new UserList(rs.getLong("uniqueid"), //id
        userDAO.getUserWhere(rs.getLong("creatorid")), // age
        rs.getString("name"), // exp
        rs.getString("description"), // exp
        rs.getString("classmarker"), // exp
        rs.getTimestamp("modified").getTime(),
        rs.getBoolean("isprivate")
        )
      );
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return exercises;
  }*/

  private boolean existsAlready(long uniqueID, long visitor) {
    //String unique = exid.substring("Custom_".length());
    String sql = "SELECT * from " + USER_EXERCISE_LIST_VISITOR + " where userlistid=" + uniqueID + " AND visitorid="+visitor;

    try {
      return !getVisitors(sql).isEmpty();
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return false;
  }


  public Set<Long> getWhere(long listid) {
    //String unique = exid.substring("Custom_".length());
    String sql = "SELECT * from " + USER_EXERCISE_LIST_VISITOR + " where userlistid=" + listid;
    try {
      return getVisitors(sql);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }
  private Set<Long> getVisitors(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    Set<Long> visitors = new HashSet<Long>();

    while (rs.next()) { visitors.add(rs.getLong("visitorid")); }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return visitors;
  }
}