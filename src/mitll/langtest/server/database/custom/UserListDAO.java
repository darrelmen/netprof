package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListDAO extends DAO {
  private static Logger logger = Logger.getLogger(UserListDAO.class);

  public static final String USER_EXERCISE_LIST = "userexerciselist";
  private UserDAO userDAO;
  private UserExerciseDAO userExerciseDAO;
  private UserListVisitorJoinDAO userListVisitorJoinDAO;
  public UserListDAO(Database database, UserDAO userDAO) {
    super(database);
    try {
      createUserListTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    this.userDAO = userDAO;
    userListVisitorJoinDAO = new UserListVisitorJoinDAO(database,userDAO);
    try {
      userListVisitorJoinDAO.createUserListTable(database);
    } catch (SQLException e) {
      logger.error("got " +e,e);
    }
  }

  public void addVisitor(long listid, long userid) { userListVisitorJoinDAO.add(listid,userid);}

  void createUserListTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USER_EXERCISE_LIST +
      " (" +
      "uniqueid IDENTITY, " +
      "creatorid LONG, " +
      // TODO visitor IDS
      "name VARCHAR, description VARCHAR, classmarker VARCHAR, modified TIMESTAMP, isprivate BOOLEAN, " +
      // TODO user exercise ids

      "FOREIGN KEY(creatorid) REFERENCES " +
      "USERS" +
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
  public void add(UserList userList) {
    long id = 0;

    try {
      // there are much better ways of doing this...
      logger.info("add :userList " + userList);

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
        "INSERT INTO " + USER_EXERCISE_LIST +
          "(creatorid,name,description,classmarker,modified,isprivate) " +
          "VALUES(?,?,?,?,?,?);");
      int i = 1;
      //     statement.setLong(i++, userList.getUserID());
      statement.setLong(i++, userList.getCreator().id);
      statement.setString(i++, userList.getName());
      statement.setString(i++, userList.getDescription());
      statement.setString(i++, userList.getClassMarker());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      statement.setBoolean(i++, userList.isPrivate());

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

      userList.setUniqueID(id);

      statement.close();
      database.closeConnection(connection);

    //  logger.debug("now " + getCount(USER_EXERCISE_LIST) + " and user exercise is " + userList);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public void updateModified(long uniqueID) {
    try {
      Connection connection = database.getConnection();

      String sql = "UPDATE " + USER_EXERCISE_LIST +
        " " +
        "SET " +
        "modified=? "+
        "WHERE uniqueid=?";

      PreparedStatement statement = connection.prepareStatement(sql);
      statement.setTimestamp(1,new Timestamp(System.currentTimeMillis()));
      statement.setLong(2, uniqueID);
      int i = statement.executeUpdate();

      //if (false) logger.debug("UPDATE " + i);
      if (i == 0) {
        logger.error("huh? didn't update the userList for " + uniqueID + " sql " + sql);
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public int getCount() { return getCount(USER_EXERCISE_LIST); }


  /**
   * Pulls the list of users out of the database.
   *
   * @return
   * @param userid
   */
  public List<UserList> getAll(long userid) {
    try {
      String sql = "SELECT * from " + USER_EXERCISE_LIST + " order by modified desc";
      return getUserLists(sql,userid);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  public List<UserList> getAllPublic() {
    try {
      String sql = "SELECT * from " + USER_EXERCISE_LIST + " where isprivate=false order by modified";
      return getUserLists(sql,-1);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

/*  public List<UserList> getAllOwnedBy(long id) {
    try {
      String sql = "SELECT * from " + USER_EXERCISE_LIST + " where creatorid=" + id+
        " order by modified";
      return getUserLists(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }*/

  public UserList getWhere(long unique) {
    String sql = "SELECT * from " + USER_EXERCISE_LIST + " where uniqueid=" + unique + " order by modified";
    try {
      List<UserList> lists = getUserLists(sql,-1);
      if (lists.isEmpty()) {
        logger.error("huh? no custom exercise with id " + unique);
        return null;
      } else {
        return lists.iterator().next();
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  /**
   * TODO don't want to always get all the exercises!
   * @see UserListManager#getUserListByID(long)
   * @param unique
   * @return
   */
  public UserList getWithExercises(long unique) {
    UserList where = getWhere(unique);
    populateList(where);
    return where;
  }

  private List<UserList> getUserLists(String sql,long userid) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<UserList> lists = new ArrayList<UserList>();

    while (rs.next()) {
      long uniqueid = rs.getLong("uniqueid");
      lists.add(new UserList(uniqueid, //id
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

    for (UserList ul : lists) {
      if (userid == -1 || ul.getCreator().id == userid || !ul.isFavorite()) {   // skip other's favorites
        populateList(ul);
      }
      else {
        //logger.info("for " + userid +" skipping " + ul);
      }
    }
    return lists;
  }

  /**
   * TODO : This is going to get slow?
   * @param where
   */
  private void populateList(UserList where) {
    Collection<UserExercise> onList = userExerciseDAO.getOnList(where.getUniqueID());
    logger.debug("populateList : got " + onList.size() + " for list "+where.getUniqueID());
    where.setExercises(onList);
    where.setVisitors(userListVisitorJoinDAO.getWhere(where.getUniqueID()));

    logger.debug("\tlist now "+where);
  }

  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
  }
}