package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
  void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      ADDREMOVE +
      " (" +
      "uniqueid IDENTITY, " +
      "exerciseid VARCHAR, " +
      "operation VARCHAR, " +
      "modified TIMESTAMP" +
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
          "(exerciseid,operation,modified) " +
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
   * @see mitll.langtest.server.database.ExcelImport#getRawExercises()
   * @return
   */
  public Set<String> getAdds() { return getIds(ADD); }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getRawExercises()
   * @return
   */
  public Set<String> getRemoves() {  return getIds(REMOVE); }

  public Set<String> getIds(String operation) {
    String sql = "SELECT DISTINCT exerciseid from " + ADDREMOVE + " where operation='" +
      operation +
      "'" +
      "";// +
      //" order by exerciseid";

    Set<String> lists = Collections.emptySet();
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      lists = new TreeSet<String>();

      while (rs.next()) {
        lists.add(rs.getString(1));
      }

      // logger.debug("getReviewed sql " + sql + " yielded " + lists.size());
      if (!lists.isEmpty())  logger.debug("getReviewed yielded " + lists.size());
      finish(connection, statement, rs);
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql, e);
    }
    return lists;
  }

  public int getCount() { return getCount(ADDREMOVE);  }
}