package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.STATE;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Records {@link STATE} transitions of each exercise as it is marked by quality control reviewers.
 * <p/>
 * So for instance an exercise can go from UNSET->DEFECT->FIXED and back again, or APPROVED->UNSET->APPROVED.
 * <p/>
 * All state changes are recorded, nothing is overwritten. To get the current state you have to get the latest
 * entry.
 *
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReviewedDAO extends DAO {
  private static final Logger logger = Logger.getLogger(ReviewedDAO.class);

  public static final String REVIEWED = "reviewed";
  public static final String SECOND_STATE = "secondstate";

  private static final String STATE_COL = "state";
  private static final String CREATORID = "creatorid";
  private static final String MODIFIED = "modified";
  private static final String EXERCISEID = "exerciseid";

  private final String tableName;

  public ReviewedDAO(Database database, String tableName) {
    super(database);
    this.tableName = tableName;
    try {
      createTable(database);

      // check for missing column
      Collection<String> columns = getColumns(tableName);
      if (!columns.contains(STATE_COL)) {
        Connection connection = database.getConnection(this.getClass().toString());
        addVarchar(connection, tableName, STATE_COL);
        database.closeConnection(connection);
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
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      tableName +
      " (" +
      "uniqueid IDENTITY, " +
      CREATORID +
      " LONG, " +
      EXERCISEID +
      " VARCHAR, " +
      STATE_COL +
      " VARCHAR, " +
      MODIFIED +
      " TIMESTAMP," +
      "FOREIGN KEY(" +
      CREATORID +
      ") REFERENCES " +
      "USERS" +
      "(ID)" +
      ")");
    finish(database, connection, statement);
  }

  /**
   *
   * <p/>
   *
   * @see #setState
   */
  private void add(String exerciseID, STATE state, long creatorID) {
    try {
      // there are much better ways of doing this...
//      logger.info("add : exid " + exerciseID + " = " + state + " by " + creatorID);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + tableName +
          "(" +
          CREATORID + "," +
          EXERCISEID + "," +
          MODIFIED + "," +
          STATE_COL +
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

      finish(connection, statement);

      //logger.debug("now " + getCount() + " reviewed");
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
      Connection connection = database.getConnection(this.getClass().toString());
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
       // logger.debug("now " + count + " reviewed");
        if (before - count != 1) logger.error("ReviewedDAO : huh? there were " + before + " before");
      }

      finish(connection, statement);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#setState(mitll.langtest.shared.CommonShell, mitll.langtest.shared.STATE, long)
   * @see mitll.langtest.server.database.custom.UserListManager#setSecondState(mitll.langtest.shared.CommonShell, mitll.langtest.shared.STATE, long)
   * @param exerciseID
   * @param state
   * @param creatorID
   */
  public void setState(String exerciseID, STATE state, long creatorID) {
    try {
      add(exerciseID, state, creatorID);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * So this returns a map of exercise id to current (latest) state.  The exercise may have gone through
   * many states, but this should return the latest one.
   *
   * @return
   * @see UserListManager#getDefectList(java.util.Collection)
   * @see UserListManager#getExerciseToState
   * @see UserListManager#markState
   * @see mitll.langtest.server.database.custom.UserListManager#setStateOnExercises()
   * @param skipUnset
   */
  public Map<String, StateCreator> getExerciseToState(boolean skipUnset) {
    return getExerciseToState(skipUnset, false, "");
  }

  public STATE getCurrentState(String exerciseID) {
    Map<String, StateCreator> exerciseToState = getExerciseToState(false, true, exerciseID);
    if (exerciseToState.isEmpty()) return STATE.UNSET;
    else return exerciseToState.values().iterator().next().getState();
  }

  private Map<String, StateCreator> getExerciseToState(boolean skipUnset, boolean selectSingleExercise, String exerciseIDToFind) {
    Connection connection = database.getConnection(this.getClass().toString());

    String latest = "latest";
    String whereClause = selectSingleExercise ? " where " + EXERCISEID + "='" + exerciseIDToFind + "'" : "";

    String sql3 = "select * from " +
      "(select " +
      EXERCISEID + "," +
      STATE_COL + "," +
      CREATORID + ", " +
      "max(" + MODIFIED + ") as " + latest +
      " from " + tableName +
      whereClause +
      " group by " + EXERCISEID + "," + STATE_COL + "," + CREATORID +
      " order by " + EXERCISEID + ") order by " + EXERCISEID + "," + latest;

    try {
      //logger.debug("Running " + sql3);
      PreparedStatement statement = connection.prepareStatement(sql3);
      ResultSet rs = statement.executeQuery();
      Map<String, StateCreator> exidToState = new HashMap<String, StateCreator>();
      while (rs.next()) {
        String exerciseID = rs.getString(EXERCISEID);
        String state = rs.getString(STATE_COL);
        long creator = rs.getLong(CREATORID);
        long when = rs.getTimestamp(4).getTime();
        STATE stateFromTable = (state == null) ? STATE.UNSET : STATE.valueOf(state);

        if (!skipUnset || stateFromTable != STATE.UNSET) {
          exidToState.put(exerciseID, new StateCreator(stateFromTable, creator, when));
        }
      }

      finish(connection, statement, rs);
     // int count = getCount();
     // if (count % 10 == 0) logger.debug("now " + count + " reviewed");
      //logger.debug("query " + sql3 + " returned " + exidToState.size() + " exercise->state items");
      return exidToState;
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql3, e);
    }
    return Collections.emptyMap();
  }

  public Collection<String> getDefectExercises() {
    Map<String, StateCreator> exerciseToState = getExerciseToState(true);
    Set<String> ids = new HashSet<String>();
    for (Map.Entry<String,StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState() == STATE.DEFECT) {
          ids.add(pair.getKey());
      }
    }
    return ids;
  }

  public static class StateCreator {
    private STATE state;
    private long creatorID;
    private long when;

    public StateCreator(STATE state, long creatorID, long when) {
      this.state = state;
      this.creatorID = creatorID;
      this.when = when;
    }

    public STATE getState() {
      return state;
    }

    /**
     * @see UserListManager#getAmmendedStateMap()
     * @return
     */
    public long getCreatorID() {
      return creatorID;
    }

    public long getWhen() {
      return when;
    }

    public String toString() {
      return "["+state.toString() + " by " + creatorID +" at " + new Date(when)+ "]";
    }
  }

  public int getCount() { return getCount(tableName); }
}