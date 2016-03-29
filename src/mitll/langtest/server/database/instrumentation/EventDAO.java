/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventDAO extends DAO implements IEventDAO {
  private static final Logger logger = Logger.getLogger(EventDAO.class);

  private static final String EVENT = "event";
  private static final String CREATORID = "creatorid";
  private static final String WIDGETTYPE = "widgettype";
  private static final String HITID = "hitid";
  private static final String EXERCISEID = "exerciseid";
  private static final String DEVICE = "device";
  // private final UserDAO userDAO;
  long defectDetector = -1;

  /**
   * @param database
   * @param defectDetector
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public EventDAO(Database database, long defectDetector) {
    super(database);
    this.defectDetector = defectDetector;
    //  this.userDAO = userDAO;
    try {
      createTable(database);
      createIndex(database, WIDGETTYPE, EVENT);
      createIndex(database, CREATORID, EVENT);
      createIndex(database, EXERCISEID, EVENT);

      // check for missing column
      Collection<String> columns = getColumns(EVENT);
      Connection connection = database.getConnection(this.getClass().toString());
      if (!columns.contains(HITID)) {
        addVarchar(connection, EVENT, HITID);
      }
      if (!columns.contains(DEVICE)) {
        addVarchar(connection, EVENT, DEVICE);
      }

      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @param database
   * @throws java.sql.SQLException
   */
  private void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
        EVENT +
        " (" +
        "uniqueid IDENTITY, " +
        CREATORID + " INT, " +
        EXERCISEID + " VARCHAR, " +
        "context VARCHAR, " +
        "widgetid VARCHAR, " +
        WIDGETTYPE + " VARCHAR, " +
        "modified TIMESTAMP, " +
        HITID + " VARCHAR, " +
        DEVICE + " VARCHAR, " +
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
   * Uses return generated keys to get the user id
   *
   * @see mitll.langtest.server.database.DatabaseImpl#logEvent(String, String, String, String, long, String, String)
   */
  @Override
  public boolean add(Event event) {
    Connection connection = getConnection();
    boolean val = true;
    try {
      // there are much better ways of doing this...

      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + EVENT +
              "(" +
              CREATORID + "," +
              EXERCISEID + "," +
              "context" + "," +
              "widgetid" + "," +
              WIDGETTYPE + "," +
              HITID + "," +
              DEVICE + "," +
              "modified) " +
              "VALUES(?,?,?,?,?,?,?,?);");
      int i = 1;

      long creatorID = event.getCreatorID();
      boolean missingCreator = creatorID == -1;
      if (missingCreator) {
        event.setTimestamp(System.currentTimeMillis());
        // logger.warn("creator is " + creatorID + " for " + event);
        creatorID = defectDetector;// userDAO.getDefectDetector();
      }
      statement.setLong(i++, creatorID);
      statement.setString(i++, event.getExerciseID());
      statement.setString(i++, event.getContext());
      statement.setString(i++, event.getWidgetID());
      statement.setString(i++, event.getWidgetType());
      statement.setString(i++, event.getHitID());
      statement.setString(i++, event.getDevice());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      logger.debug(getDatabase().getLanguage() + " : Add " + event);
      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
        val = false;
      }
      statement.close();

    } catch (SQLException ee) {
      logger.error("trying to add event " + event + " got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
      val = false;
    } finally {
      database.closeConnection(connection);
    }
    return val;
  }

  @Override
  public List<Event> getAll() {
    try {
      return getEvents("SELECT * from " + EVENT);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public List<Event> getAllDevices() {
    try {
      return getEvents("SELECT * from " + EVENT + " where length(device)=36");
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public void addPlayedMarkings(long userID, CommonExercise firstExercise) {
    List<Event> allForUserAndExercise = getAllForUserAndExercise(userID, firstExercise.getID());
    Map<String, AudioAttribute> audioToAttr = firstExercise.getAudioRefToAttr();
    for (Event event : allForUserAndExercise) {
      AudioAttribute audioAttribute = audioToAttr.get(event.getContext());
      if (audioAttribute == null) {
        //logger.warn("addPlayedMarkings huh? can't find " + event.getContext() + " in " + audioToAttr.keySet());
      } else {
        audioAttribute.setHasBeenPlayed(true);
      }
    }
  }

  @Override
  public Number getNumRows() {
    return getCount("EVENT");
  }

  private List<Event> getAllForUserAndExercise(long userid, String exid) {
    try {
      String sql = "SELECT * from " + EVENT + " where " +
          WIDGETTYPE +
          "='qcPlayAudio' AND " +
          CREATORID + "=" + userid + " and " +
          EXERCISEID + "='" + exid +
          "'";

      return getEvents(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  private List<Event> getEvents(String sql) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<Event> lists = new ArrayList<Event>();

    while (rs.next()) {
      lists.add(new Event(//rs.getLong(ID),
          rs.getString("widgetid"),
          rs.getString(WIDGETTYPE),
          rs.getString(EXERCISEID),
          rs.getString("context"),
          rs.getLong(CREATORID),
          rs.getTimestamp("modified").getTime(),
          rs.getString(HITID),
          rs.getString(DEVICE))
      );
    }

    finish(connection, statement, rs);
    return lists;
  }
}