/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddRemoveDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AddRemoveDAO.class);

  private static final String ADDREMOVE = "addremove";
  public static final String ADD = "ADD";
  public static final String REMOVE = "REMOVE";
  private static final String EXERCISEID = "exerciseid";
  private static final String MODIFIED = "modified";
  private static final String OPERATION = "operation";

  public AddRemoveDAO(Database database) {
    super(database);
    try {
      createTable(database);
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
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        ADDREMOVE +
        " (" +
        "uniqueid IDENTITY, " +
        EXERCISEID +
        " VARCHAR, " +
        "operation VARCHAR, " +
        MODIFIED +
        " TIMESTAMP" +
        ")");
    finish(database, connection, statement);
  }


  /**
   * Add or delete an exercise.
   *
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem
   * @see mitll.langtest.server.database.DatabaseImpl#duplicateExercise(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.ReviewEditableExercise#duplicateExercise
   * @see mitll.langtest.client.custom.ReviewEditableExercise#deleteItem(String, long, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.PagingExerciseList, mitll.langtest.client.list.PagingExerciseList)
   */
  public void add(String exerciseID, String operation) {
    try {
      // there are much better ways of doing this...
      logger.info("add : " + exerciseID + " operation " + operation);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + ADDREMOVE +
              "(" +
              EXERCISEID +
              ",operation," +
              MODIFIED +
              ") " +
              "VALUES(?,?,?);");
      int i = 1;
      statement.setString(i++, exerciseID);
      statement.setString(i++, operation);
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for " + exerciseID + " " + operation);

      finish(connection, statement);

      logger.debug("now " + getCount() + " add/remove");
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * @return
   * @see mitll.langtest.server.database.exercise.ExcelImport#getRawExercises()
   */
  public Collection<IdAndTime> getAdds() {
    return getIds(ADD);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.exercise.ExcelImport#getRawExercises()
   */
  public Collection<IdAndTime> getRemoves() {
    return getIds(REMOVE);
  }

  private Collection<IdAndTime> getIds(String operation) {
    String sql = "SELECT DISTINCT " +
        EXERCISEID + "," +
        MODIFIED +
        " from " + ADDREMOVE +
        " where " +
        OPERATION +
        "='" +
        operation +
        "'";

    Set<IdAndTime> lists = Collections.emptySet();
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      lists = new TreeSet<>();

      while (rs.next()) {
        String id = rs.getString(1);
        long mod = rs.getTimestamp(2).getTime();
        lists.add(new IdAndTime(id, mod));
      }

      // logger.debug("getReviewed sql " + sql + " yielded " + lists.size());
      if (!lists.isEmpty()) logger.debug("getReviewed yielded " + lists.size());
      finish(connection, statement, rs);
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql, e);
    }
    return lists;
  }

  public static class IdAndTime implements Comparable<IdAndTime> {
    private final String id;
    private final long timestamp;

    public IdAndTime(String id, long timestamp) {
      this.id = id;
      this.timestamp = timestamp;
    }

    public String getId() {
      return id;
    }

    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public int compareTo(IdAndTime o) {
      int i = id.compareTo(o.id);
      return i == 0 ? Long.valueOf(timestamp).compareTo(o.getTimestamp()) : i;
    }

    @Override
    public boolean equals(Object other) {
      return compareTo((IdAndTime) other) == 0;
    }
  }

  private int getCount() {
    return getCount(ADDREMOVE);
  }
}