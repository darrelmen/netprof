/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.reviewed;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.exercise.STATE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Records {@link STATE} transitions of each exercise as it is marked by quality control reviewers.
 * <p/>
 * So for instance an exercise can go from UNSET->DEFECT->FIXED and back again, or APPROVED->UNSET->APPROVED.
 * <p/>
 * All state changes are recorded, nothing is overwritten. To get the current state you have to get the latest
 * entry.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReviewedDAO extends DAO implements IReviewedDAO {
  private static final Logger logger = LogManager.getLogger(ReviewedDAO.class);

  public static final String REVIEWED = "reviewed";
  public static final String SECOND_STATE = "secondstate";

  private static final String STATE_COL = "state";
  private static final String CREATORID = "creatorid";
  private static final String MODIFIED = "modified";
  private static final String EXERCISEID = "exerciseid";

  @Override
  public void updateUser(int old, int newUser) {

  }

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
        CREATORID + " LONG, " +
        EXERCISEID + " VARCHAR, " +
        STATE_COL + " VARCHAR, " +
        MODIFIED + " TIMESTAMP," +
        "FOREIGN KEY(" +
        CREATORID +
        ") REFERENCES " +
        "USERS" +
        "(ID)" +
        ")");
    finish(database, connection, statement);
  }

  /**
   * <p/>
   *
   * @see IReviewedDAO#setState
   */
  private void add(int exerciseID, STATE state, long creatorID) {
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
      statement.setString(i++, ""+exerciseID);
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
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(int, int)
   */
  @Override
  public void remove(int exerciseID) {
    try {
      int before = getCount();
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
          "DELETE FROM " + tableName + " WHERE " + EXERCISEID + "='" + exerciseID + "'"
      );

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("remove : huh? didn't remove row for " + exerciseID);
        int count = getCount();
        // logger.debug("now " + childCount + " reviewed");
        if (before - count != 1) logger.error("ReviewedDAO : huh? there were " + before + " before");
      }

      finish(connection, statement);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * @param exerciseID
   * @param state
   * @param creatorID
   * @see mitll.langtest.server.database.custom.UserListManager#setState
   * @see mitll.langtest.server.database.custom.UserListManager#setSecondState
   */
  @Override
  public void setState(int exerciseID, STATE state, long creatorID) {
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
   * @param skipUnset
   * @return
   * @see UserListManager#getDefectList
   * @see UserListManager#getExerciseToState
   * @see IUserListManager#markState
   * @see mitll.langtest.server.database.custom.UserListManager#setStateOnExercises()
   */
  @Override
  public Map<Integer, StateCreator> getExerciseToState(boolean skipUnset) {
    return getExerciseToState(skipUnset, false, "");
  }

  @Override
  public STATE getCurrentState(int exerciseID) {
    Map<Integer, StateCreator> exerciseToState = getExerciseToState(false, true, ""+exerciseID);
    if (exerciseToState.isEmpty()) return STATE.UNSET;
    else return exerciseToState.values().iterator().next().getState();
  }

  private Map<Integer, StateCreator> getExerciseToState(boolean skipUnset, boolean selectSingleExercise, String exerciseIDToFind) {
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
      Map<Integer, StateCreator> exidToState = new HashMap<>();
      while (rs.next()) {
        String exerciseID = rs.getString(EXERCISEID);
        String state = rs.getString(STATE_COL);
        long creator = rs.getLong(CREATORID);
        long when = rs.getTimestamp(4).getTime();
        STATE stateFromTable = (state == null) ? STATE.UNSET : STATE.valueOf(state);

        if (!skipUnset || stateFromTable != STATE.UNSET) {
          try {
            exidToState.put(Integer.parseInt(exerciseID), new StateCreator(stateFromTable, creator, when));
          } catch (NumberFormatException e) {
            logger.error("Got " + e,e);
          }
        }
      }

      finish(connection, statement, rs, sql3);
     // int childCount = getCount();
     // if (childCount % 10 == 0) logger.debug("now " + childCount + " reviewed");
      //logger.debug("query " + sql3 + " returned " + exidToState.size() + " exercise->state items");
      return exidToState;
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql3, e);
    }
    return Collections.emptyMap();
  }

  public Collection<Integer> getInspectedExercises() {
  /*
    Map<String, StateCreator> exerciseToState = getExerciseToState(false);
    Set<String> ids = new HashSet<String>();

   Collection<STATE> toMatch = new HashSet<>();
    toMatch.add(STATE.APPROVED);
    toMatch.add(STATE.DEFECT);
    toMatch.add(STATE.FIXED);
    toMatch.add(STATE.ATTN_LL);
    for (Map.Entry<String,StateCreator> pair : exerciseToState.entrySet()) {
      STATE state = pair.getValue().getState();
      if (toMatch.contains(state)) {
        ids.add(pair.getKey());
      }
    }
    return ids;
    */

    return null;
  }

  public Collection<StateCreator> getAll() {
    Connection connection = database.getConnection(this.getClass().toString());
    List<StateCreator> all = new ArrayList<>();

    String latest = "latest";

    String sql3 = "select * from " +
        "(select " +
        EXERCISEID + "," +
        STATE_COL + "," +
        CREATORID + ", " +
        "max(" + MODIFIED + ") as " + latest +
        " from " + tableName +
        " group by " + EXERCISEID + "," + STATE_COL + "," + CREATORID +
        " order by " + EXERCISEID + ") order by " + EXERCISEID + "," + latest;

    try {
      //logger.debug("Running " + sql3);
      PreparedStatement statement = connection.prepareStatement(sql3);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String exerciseID = rs.getString(EXERCISEID);
        String state = rs.getString(STATE_COL);
        long creator = rs.getLong(CREATORID);
        long when = rs.getTimestamp(4).getTime();
        STATE stateFromTable = (state == null) ? STATE.UNSET : STATE.valueOf(state);

        StateCreator e = new StateCreator(stateFromTable, creator, when);
        try {
          e.setExerciseID(Integer.parseInt(exerciseID));
        } catch (NumberFormatException e1) {
          //e1.printStackTrace();
        }
        e.setOldExID(exerciseID);
        all.add(e);
      }

      finish(connection, statement, rs);
      // int childCount = getCount();
      // if (childCount % 10 == 0) logger.debug("now " + childCount + " reviewed");
      //logger.debug("query " + sql3 + " returned " + exidToState.size() + " exercise->state items");
      return all;
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql3, e);
    }
    return Collections.emptyList();
  }

  @Override
  public Collection<Integer> getDefectExercises() {
    Map<Integer, StateCreator> exerciseToState = getExerciseToState(true);
    Set<Integer> ids = new HashSet<>();
    for (Map.Entry<Integer, StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState() == STATE.DEFECT) {
        ids.add(pair.getKey());
      }
    }
    return ids;
  }

  @Override
  public int getCount() {
    return getCount(tableName);
  }
}