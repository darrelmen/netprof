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
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      ADDREMOVE +
      " (" +
      "uniqueid IDENTITY, " +
      "exerciseid VARCHAR, " +
      "operation VARCHAR, " +
      "modified TIMESTAMP" +
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
   * @see mitll.langtest.server.database.custom.UserListManager#markReviewed(String, long)
   */
  public void add(String exerciseID, String operation) {
    try {
      // there are much better ways of doing this...
      logger.info("add : " + exerciseID + " operation " + operation);

      Connection connection = database.getConnection();
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

      statement.close();
      database.closeConnection(connection);

      logger.debug("now " + getCount() + " reviewed");
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * @see UserListManager#UserListManager(mitll.langtest.server.database.UserDAO, mitll.langtest.server.database.custom.UserListDAO, mitll.langtest.server.database.custom.UserListExerciseJoinDAO, mitll.langtest.server.database.custom.AnnotationDAO, mitll.langtest.server.database.custom.AddRemoveDAO, mitll.langtest.server.PathHelper)
   * @return
   */
  public Set<String> getAdds() {
    String operation = "ADD";
    Set<String> lists = getIds(operation);
    return lists;
  }

  public Set<String> getRemoves() {
    String operation = "REMOVE";
    Set<String> lists = getIds(operation);
    return lists;
  }

  public Set<String> getIds(String operation) {
    String sql = "SELECT DISTINCT exerciseid from " + ADDREMOVE + " where operation='" +
      operation +
      "'" +
      "";// +
      //" order by exerciseid";

    Set<String> lists = Collections.emptySet();
    try {
      Connection connection = database.getConnection();
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

  public int getCount() { return getCount(ADDREMOVE);  }
}