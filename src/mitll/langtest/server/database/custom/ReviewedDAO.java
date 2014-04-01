package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Let's us keep track of state of read-only exercises too.
 *
 *
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReviewedDAO extends DAO {
  private static final Logger logger = Logger.getLogger(ReviewedDAO.class);

  private static final String REVIEWED = "reviewed";
  private static final String STATE = "state";

  public ReviewedDAO(Database database) {
    super(database);
    try {
      createTable(database);
      Collection<String> columns = getColumns(REVIEWED);
      Connection connection = database.getConnection();
      if (!columns.contains(STATE)) {
        addVarchar(connection, REVIEWED, STATE);
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * String exerciseID; String field; String status; String comment;
   * String userID;
   *
   * @param database
   * @throws java.sql.SQLException
   */
  void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      REVIEWED +
      " (" +
      "uniqueid IDENTITY, " +
      "creatorid LONG, " +
      "exerciseid VARCHAR, " +
      "state VARCHAR, " +
      "modified TIMESTAMP," +
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
   * @see UserListManager#markApproved(String, long)
   */
  public void add(String exerciseID, long creatorID, String state) {
    try {
      // there are much better ways of doing this...
      logger.info("add :reviewed " + exerciseID + " by " + creatorID);

      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + REVIEWED +
          "(creatorid,exerciseid,modified," +STATE+
          ") " +
          "VALUES(?,?,?,?);");
      int i = 1;
      statement.setLong(i++, creatorID);
      statement.setString(i++, exerciseID);
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      statement.setString(i++, state);

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for " + exerciseID + " " + creatorID);

      statement.close();
      database.closeConnection(connection);

      //logger.debug("now " + getCount() + " reviewed");
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#removeReviewed(String)
   * @param exerciseID
   */
  public void remove(String exerciseID) {
    try {
      int before = getCount();
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "DELETE FROM " + REVIEWED +
          " WHERE " +
          "exerciseid" +
          "='" + exerciseID +
          "'");

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("remove : huh? didn't remove row for " + exerciseID);
        int count = getCount();
        logger.debug("now " + count + " reviewed");
        if (before-count != 1) logger.error("ReviewedDAO : huh? there were " +before +" before");
      }

      statement.close();
      database.closeConnection(connection);

      //   int count = getCount();
      //    logger.debug("now " + count + " reviewed");
      //   if (before-count != 1) logger.error("ReviewedDAO : huh? there were " +before +" before");
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public void setState(String exerciseID, String state) {
    try {
    //  int before = getCount();
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "UPDATE " + REVIEWED +
          " SET STATE='" +state+
          "' " +
          " WHERE " +
          "exerciseid" +
          "='" + exerciseID +
          "'");

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("remove : huh? didn't change row for " + exerciseID);
        //int count = getCount();
      //  logger.debug("now " + count + " reviewed");
        //if (before-count != 1) logger.error("ReviewedDAO : huh? there were " +before +" before");
      }

      statement.close();
      database.closeConnection(connection);

      //   int count = getCount();
      //    logger.debug("now " + count + " reviewed");
      //   if (before-count != 1) logger.error("ReviewedDAO : huh? there were " +before +" before");
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#UserListManager(mitll.langtest.server.database.UserDAO, UserListDAO, UserListExerciseJoinDAO, AnnotationDAO, ReviewedDAO, mitll.langtest.server.PathHelper)
   * @return
   */
  public Set<String> getReviewed() {
    Connection connection = database.getConnection();
    String state = "approved";
    String sql = "SELECT DISTINCT exerciseid from " + REVIEWED + " where " +STATE+"='" +
      state +
      "'"+
      " order by exerciseid";

    Set<String> lists = Collections.emptySet();
    try {
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      lists = new TreeSet<String>();

      while (rs.next()) {
        lists.add(rs.getString(1));
      }

      // logger.debug("getReviewed sql " + sql + " yielded " + lists.size());
        logger.debug("getReviewed yielded " + lists.size());
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql, e);
    }
    return lists;
  }

  public Map<String,String> getStateMap() {
    Connection connection = database.getConnection();
    String sql = "SELECT exerciseid," +
      STATE+
      " from " + REVIEWED;

    try {
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      Map<String,String> exidToState = new HashMap<String, String>();
      while (rs.next()) {
        String string = rs.getString(1);
        String state = rs.getString(2);
        if (state == null) state = "approved";
        exidToState.put(string, state);
      }

      // logger.debug("getReviewed sql " + sql + " yielded " + lists.size());
      logger.debug("getStateMap yielded " + exidToState.size());
      rs.close();
      statement.close();
      database.closeConnection(connection);
      return exidToState;
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql, e);
    }
    return Collections.emptyMap();
  }

  public int getCount() { return getCount(REVIEWED);  }
}