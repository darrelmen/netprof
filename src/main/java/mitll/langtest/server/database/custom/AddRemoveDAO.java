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

package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddRemoveDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(AddRemoveDAO.class);

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
        "uniqueid" + "," +
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
        int id = rs.getInt(1);
        String oldid = rs.getString(2);
        long mod = rs.getTimestamp(3).getTime();
        lists.add(new IdAndTime(oldid, mod));
      }

      // logger.debug("getReviewed sql " + sql + " yielded " + lists.size());
      if (!lists.isEmpty()) logger.debug("getReviewed yielded " + lists.size());
      finish(connection, statement, rs, sql);
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql, e);
    }
    return lists;
  }

  public static class IdAndTime implements Comparable<IdAndTime> {
    // private final int id;
    private final String oldid;
    private final long timestamp;

    IdAndTime(String oldid, long timestamp) {
      // this.id = id;
      this.oldid = oldid;
      this.timestamp = timestamp;
    }

/*
    public int getId() {
      return id;
    }
*/

    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public int compareTo(IdAndTime o) {
      int i = oldid.compareTo(o.oldid);
      return i == 0 ? Long.compare(timestamp, o.getTimestamp()) : i;
    }

    @Override
    public boolean equals(Object other) {
      return compareTo((IdAndTime) other) == 0;
    }

    public String getOldid() {
      return oldid;
    }
  }

  private int getCount() {
    return getCount(ADDREMOVE);
  }
}