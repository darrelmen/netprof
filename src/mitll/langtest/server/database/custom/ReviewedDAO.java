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

/**
 * Let's us keep track of state of read-only exercises too.
 * <p/>
 * <p/>
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReviewedDAO extends DAO {
  private static final Logger logger = Logger.getLogger(ReviewedDAO.class);

  public static final String REVIEWED = "reviewed";
  public static final String SECOND_STATE = "secondstate";

  private static final String STATE = "state";
  private static final String CREATORID = "creatorid";
  private static final String MODIFIED = "modified";
  private static final String EXERCISEID = "exerciseid";

  private final String tableName;

  public ReviewedDAO(Database database,String tableName) {
    super(database);
    this.tableName = tableName;
    try {
      createTable(database);

      // check for missing column
      Collection<String> columns = getColumns(tableName);
      Connection connection = database.getConnection();
      if (!columns.contains(STATE)) {
        addVarchar(connection, tableName, STATE);
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
  private void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      tableName +
      " (" +
      "uniqueid IDENTITY, " +
      CREATORID +
      " LONG, " +
      EXERCISEID +
      " VARCHAR, " +
      STATE +
      " VARCHAR, " +
      MODIFIED +
      " TIMESTAMP," +
      "FOREIGN KEY(" +
      CREATORID +
      ") REFERENCES " +
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
   * @see #setState
   */
  private void add(String exerciseID, mitll.langtest.shared.STATE state, long creatorID) {
    try {
      // there are much better ways of doing this...
      //logger.info("add : exid " + exerciseID + " = " + state+ " by " + creatorID);

      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + tableName +
          "(" +
          CREATORID +
          "," +
          EXERCISEID +
          "," +
          MODIFIED +
          "," + STATE +
          ") " +
          "VALUES(?,?,?,?);"
      );
      int i = 1;
      statement.setLong(i++, creatorID);
      statement.setString(i++, exerciseID);
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      statement.setString(i++, state.toString());

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for " + exerciseID + " " + creatorID);

      statement.close();
      database.closeConnection(connection);

    //  logger.debug("now " + getCount() + " reviewed");
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * @param exerciseID
   * @see mitll.langtest.server.database.custom.UserListManager#removeReviewed(String)
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(String)
   */
  public void remove(String exerciseID) {
    try {
      int before = getCount();
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "DELETE FROM " + tableName +
          " WHERE " +
          EXERCISEID +
          "='" + exerciseID +
          "'"
      );

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("remove : huh? didn't remove row for " + exerciseID);
        int count = getCount();
        logger.debug("now " + count + " reviewed");
        if (before - count != 1) logger.error("ReviewedDAO : huh? there were " + before + " before");
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public void setState(String exerciseID, mitll.langtest.shared.STATE state, long creatorID) {
    try {
      add(exerciseID, state, creatorID);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * @return
   * @see UserListManager#getDefectList()
   * @see UserListManager#getExerciseToState()
   * @see UserListManager#markState(java.util.Collection, String)
   */
  public Map<String, StateCreator> getExerciseToState() {
    Connection connection = database.getConnection();

    String sql3 = "select * from " +
      "(select " +
      EXERCISEID +
      "," +
      STATE +
      "," +
      CREATORID +
      ",max(" +
      MODIFIED +
      ") as latest from " +
      tableName +
      " group by " +
      EXERCISEID +
      "," +
      STATE +
      "," +
      CREATORID +
      " order by " +
      EXERCISEID +
      ") order by " +
      EXERCISEID +
      "," +
      "latest";

    try {
      PreparedStatement statement = connection.prepareStatement(sql3);
      ResultSet rs = statement.executeQuery();
      Map<String, StateCreator> exidToState = new HashMap<String, StateCreator>();
      while (rs.next()) {
        String exerciseID = rs.getString(1);
        String state = rs.getString(2);
        long creator = rs.getLong(3);
        mitll.langtest.shared.STATE stateFromTable = (state == null) ? mitll.langtest.shared.STATE.UNSET : mitll.langtest.shared.STATE.valueOf(state);
        exidToState.put(exerciseID, new StateCreator(stateFromTable, creator));
      }

      rs.close();
      statement.close();
      database.closeConnection(connection);
      int count = getCount();
      if (count % 10 == 0) logger.debug("now " + count + " reviewed");
      return exidToState;
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql3, e);
    }
    return Collections.emptyMap();
  }

  public static class StateCreator {
    mitll.langtest.shared.STATE state;
    long creatorID;

    public StateCreator(mitll.langtest.shared.STATE state, long creatorID) {
      this.state = state;
      this.creatorID = creatorID;
    }

    public String toString() {
      return "["+state.toString() + " by " + creatorID +"]";
    }
  }

  public int getCount() { return getCount(tableName); }
}