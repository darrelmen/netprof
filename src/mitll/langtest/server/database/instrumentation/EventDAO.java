package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.database.custom.UserAnnotation;
import mitll.langtest.shared.ExerciseAnnotation;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventDAO extends DAO {
  private static final Logger logger = Logger.getLogger(EventDAO.class);

  private static final String EVENT = "event";
  private static final String CREATORID = "creatorid";


  public EventDAO(Database database) {
    super(database);
    try {
      createTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @param database
   * @throws java.sql.SQLException
   */
  void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      EVENT +
      " (" +
      "uniqueid IDENTITY, " +
      CREATORID +
      " LONG, " +
      "exerciseid VARCHAR, context VARCHAR, widgetid VARCHAR, modified TIMESTAMP, " +
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
   * @see mitll.langtest.server.database.custom.UserListManager#reallyCreateNewItem(long, mitll.langtest.shared.custom.UserExercise)
   */
  public void add(Event event) {
   // long id = 0;

    try {
      // there are much better ways of doing this...

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
        "INSERT INTO " + EVENT +
          "(" +
          CREATORID +
          ",exerciseid,context,widgetid,modified) " +
          "VALUES(?,?,?,?,?);");
      int i = 1;
      //     statement.setLong(i++, annotation.getUserID());
      statement.setLong(i++, event.getCreatorID());
      statement.setString(i++, event.getExerciseID());
      statement.setString(i++, event.getContext());
      statement.setString(i++, event.getWidgetID());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      statement.close();
      database.closeConnection(connection);

    //  logger.debug("now " + getCount(EVENT) + " and user exercise is " + annotation);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public List<Event> getAll() {
    try {
      String sql = "SELECT * from " + EVENT;

      return getEvents(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  /**
   * Pulls the list of users out of the database.
   *
   * @return
   */
  public List<Event> getAllBy(long userid) {
    try {
      String sql = "SELECT * from " + EVENT + " where " + CREATORID +"="+userid;

      return getEvents(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  private List<Event> getEvents(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<Event> lists = new ArrayList<Event>();

    while (rs.next()) {
      lists.add(new Event(
          rs.getString("widgetid"),
          rs.getString("exerciseid"),
          rs.getString("context"),
          rs.getLong(CREATORID),
          rs.getTimestamp("modified").getTime()
      )
      );
    }

    //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
    rs.close();
    statement.close();
    database.closeConnection(connection);
    return lists;
  }
}