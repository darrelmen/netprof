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
  private static Logger logger = Logger.getLogger(ReviewedDAO.class);

  public static final String REVIEWED = "reviewed";

  public ReviewedDAO(Database database) {
    super(database);
    try {
      createTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   *   String exerciseID; String field; String status; String comment;
   String userID;
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
      "modified TIMESTAMP,"+
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
  public void add(String exerciseID, long creatorID) {
    long id = 0;

    try {
      // there are much better ways of doing this...
      logger.info("add :reviewed " + exerciseID + " by " + creatorID);

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
        "INSERT INTO " + REVIEWED +
          "(creatorid,exerciseid,modified) " +
          "VALUES(?,?,?);");
      int i = 1;
      //     statement.setLong(i++, annotation.getUserID());
      statement.setLong(i++, creatorID);
      statement.setString(i++, exerciseID);
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
/*
      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      if (rs.next()) {
        id = rs.getLong(1);
      } else {
        logger.error("huh? no key was generated?");
      }
      logger.debug("unique id = " + id);*/

      statement.close();
      database.closeConnection(connection);

    logger.debug("now " + getCount() + " reviewed");
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public int getCount() { return getCount(REVIEWED); }

  public Set<String> getReviewed()  {
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

     // logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("Got " +e + " doing " + sql,e);
    }
    return lists;
  }
}