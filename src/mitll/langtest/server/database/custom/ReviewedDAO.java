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
public class ReviewedDAO extends DAO {
  private static final Logger logger = Logger.getLogger(ReviewedDAO.class);

  private static final String REVIEWED = "reviewed";

  public ReviewedDAO(Database database) {
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
      REVIEWED +
      " (" +
      "uniqueid IDENTITY, " +
      "creatorid LONG, " +
      "exerciseid VARCHAR, " +
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
   * @see UserListManager#markReviewed(String, long)
   */
  public void add(String exerciseID, long creatorID) {
    try {
      // there are much better ways of doing this...
      logger.info("add :reviewed " + exerciseID + " by " + creatorID);

      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + REVIEWED +
          "(creatorid,exerciseid,modified) " +
          "VALUES(?,?,?);");
      int i = 1;
      statement.setLong(i++, creatorID);
      statement.setString(i++, exerciseID);
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for " + exerciseID + " " + creatorID);

      statement.close();
      database.closeConnection(connection);

      logger.debug("now " + getCount() + " reviewed");
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

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#UserListManager(mitll.langtest.server.database.UserDAO, UserListDAO, UserListExerciseJoinDAO, AnnotationDAO, ReviewedDAO, mitll.langtest.server.PathHelper)
   * @return
   */
  public Set<String> getReviewed() {
    Connection connection = database.getConnection();
    String sql = "SELECT DISTINCT exerciseid from " + REVIEWED + " order by exerciseid";

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

  public int getCount() { return getCount(REVIEWED);  }
}