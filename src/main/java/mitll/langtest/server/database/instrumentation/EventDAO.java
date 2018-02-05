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

package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.instrumentation.SlimEvent;
import mitll.npdata.dao.SlickSlimEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
class EventDAO extends DAO implements IEventDAO {
  private static final Logger logger = LogManager.getLogger(EventDAO.class);

  @Override
  public void updateUser(int old, int newUser) {

  }

  private static final String EVENT = "event";
  private static final String CREATORID = "creatorid";
  private static final String WIDGETTYPE = "widgettype";
  private static final String HITID = "hitid";
  private static final String EXERCISEID = "exerciseid";
  private static final String DEVICE = "device";
  private static final String WIDGETID = "widgetid";
  private static final String CONTEXT = "context";
  private long defectDetector = -1;
  private static final String MODIFIED = "modified";
  private static final String WHERE_DEVICE = " where length(device)=36";

  /**
   * @param database
   * @param defectDetector
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  private EventDAO(Database database, long defectDetector) {
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
   * @see Database#logEvent
   */
  @Override
  public boolean addToProject(Event event, int projid) {
    Connection connection = getConnection();
    boolean val = true;
    try {
      // there are much better ways of doing this...

      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + EVENT +
              "(" +
              CREATORID + "," +
              EXERCISEID + "," +
              CONTEXT + "," +
              WIDGETID + "," +
              WIDGETTYPE + "," +
              HITID + "," +
              DEVICE + "," +
              "modified) " +
              "VALUES(?,?,?,?,?,?,?,?);");
      int i = 1;

      long creatorID = event.getUserID();
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
      // statement.setString(i++, event.getHitID());
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
    return getAllMax(-1);
  }

  @Override
  public List<Event> getAll(Integer projid) {
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
  public List<Event> getAllMax(int projid) {
    return getAll(projid);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Report#getReport
   * @param projid
   */
  public List<SlickSlimEvent> getAllSlim(int projid) {
    try {
      List<SlimEvent> slimEvents = getSlimEvents("SELECT " + CREATORID + "," + MODIFIED + " from " + EVENT);
      return getSlickSlimEvents(slimEvents);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public List<SlickSlimEvent> getAllSlim() {
    return getAllSlim(-1);
  }

  private List<SlickSlimEvent> getSlickSlimEvents(List<SlimEvent> slimEvents) {
    List<SlickSlimEvent> copy = new ArrayList<>();
    for (SlimEvent slimEvent : slimEvents)
      copy.add(new SlickSlimEvent(slimEvent.getUserID(),slimEvent.getTimestamp()));
    return copy;
  }

  public SlickSlimEvent getFirstSlim(int projid) {
    try {
      List<SlimEvent> slimEvents = getSlimEvents("SELECT " + CREATORID + "," + MODIFIED +
          " from " + EVENT + " limit 1");
      return slimEvents.isEmpty() ? null : getSlickSlimEvents(slimEvents).iterator().next();
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return null;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.Report#getEventsDevices
   * @param projid
   */
  public List<SlickSlimEvent> getAllDevicesSlim(int projid) {
    try {
      return getSlickSlimEvents(getSlimEvents("SELECT " + CREATORID + "," + MODIFIED + " from " + EVENT + WHERE_DEVICE));
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public void addPlayedMarkings(int userID, CommonExercise firstExercise) {
    List<Event> allForUserAndExercise = getAllForUserAndExercise(userID, firstExercise.getOldID());
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
  public Number getNumRows(int projid) {
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
      lists.add(new Event(
          rs.getString(WIDGETID),
          rs.getString(WIDGETTYPE),
          rs.getString(EXERCISEID),
          rs.getString(CONTEXT),
          rs.getInt(CREATORID),
          rs.getTimestamp(MODIFIED).getTime(),
          //  rs.getString(HITID),
          rs.getString(DEVICE), -1)
      );
    }

    finish(connection, statement, rs, sql);
    return lists;
  }

  private List<SlimEvent> getSlimEvents(String sql) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<SlimEvent> lists = new ArrayList<>();

    while (rs.next()) {
      lists.add(new SlimEvent(
          rs.getInt(CREATORID),
          rs.getTimestamp(MODIFIED).getTime(),
          -1));
    }

    finish(connection, statement, rs, sql);
    return lists;
  }
}