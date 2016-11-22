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
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.instrumentation.SlimEvent;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventDAO extends DAO {
  private static final Logger logger = Logger.getLogger(EventDAO.class);

  private static final String EVENT = "event";
  private static final String CREATORID = "creatorid";
  private static final String WIDGETTYPE = "widgettype";
  private static final String HITID = "hitid";
  private static final String EXERCISEID = "exerciseid";
  private static final String EVENTS = "Events";
  private static final String DEVICE = "device";
  public static final String MODIFIED = "modified";
  public static final String WHERE_DEVICE = " where length(device)=36";
  public static final String UNIQUEID = "uniqueid";
  public static final int MAX_ROWS = 10000;
  private final UserDAO userDAO;

  /**
   * @param database
   * @param userDAO
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public EventDAO(Database database, UserDAO userDAO) {
    super(database);
    this.userDAO = userDAO;
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
        UNIQUEID +
        " IDENTITY, " +
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
  public synchronized boolean add(Event event) {
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
        creatorID = userDAO.getDefectDetector();
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

  public Collection<Event> getLastRows() {
    String sql = "SELECT * FROM (\n" +
        "    SELECT * FROM " + EVENT +
        " ORDER BY " + UNIQUEID +
        " DESC LIMIT " +
        MAX_ROWS +
        "\n" +
        ") sub\n" +
        "ORDER BY " +
        UNIQUEID +
        " ASC";

    try {
      return getEvents(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return Collections.emptyList();
  }

  /**
   * @see mitll.langtest.server.database.Report#getReport(JSONObject, int)
   * @return
   */
  public List<SlimEvent> getAllSlim() {
    try {
      return getSlimEvents("SELECT " +CREATORID+ "," +MODIFIED+
          " from " + EVENT);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return Collections.emptyList();
  }

  public SlimEvent getFirstSlim() {
    try {
      List<SlimEvent> slimEvents = getSlimEvents("SELECT " + CREATORID + "," + MODIFIED +
          " from " + EVENT + " limit 1");
      return slimEvents.isEmpty() ? null : slimEvents.iterator().next();
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return null;
  }


/*  public List<Event> getAllDevices() {
    try {
      return getEvents("SELECT * from " + EVENT + WHERE_DEVICE);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return Collections.emptyList();
  }*/

  /**
   * @see mitll.langtest.server.database.Report#getEventsDevices(StringBuilder, Set, JSONObject, int)
   * @return
   */
  public List<SlimEvent> getAllDevicesSlim() {
    try {
      return getSlimEvents("SELECT " +CREATORID+ "," +MODIFIED+" from " + EVENT + WHERE_DEVICE);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      if (logAndNotify != null) {
        logAndNotify.logAndNotifyServerException(ee);
      }
    }
    return Collections.emptyList();
  }

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
          rs.getString("widgetid"),
          rs.getString(WIDGETTYPE),
          rs.getString(EXERCISEID),
          rs.getString("context"),
          rs.getLong(CREATORID),
          rs.getTimestamp(MODIFIED).getTime(),
          rs.getString(HITID),
          rs.getString(DEVICE))
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
          rs.getLong(CREATORID),
          rs.getTimestamp(MODIFIED).getTime()
      ));
    }

    finish(connection, statement, rs, sql);
    return lists;
  }


  private static final List<String> COLUMNS2 = Arrays.asList("id", "type", "exercise", "context", "userid", "timestamp", "time_millis", "hitID", "device");

  /**
   * @param out
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void toXLSX(OutputStream out) {
    long then = System.currentTimeMillis();

    List<Event> all = getAll();
    long now = System.currentTimeMillis();
    if (now - then > 100) logger.info("toXLSX : took " + (now - then) + " millis to read " + all.size() +
        " events from database");
    then = now;

    //Workbook wb = new XSSFWorkbook();
    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk

    Sheet sheet = wb.createSheet(EVENTS);
    int rownum = 0;
    Row headerRow = sheet.createRow(rownum++);
    for (int i = 0; i < COLUMNS2.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(COLUMNS2.get(i));
    }

    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss"));

    for (Event event : all) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(event.getWidgetID());

      cell = row.createCell(j++);
      cell.setCellValue(event.getWidgetType());

      cell = row.createCell(j++);
      cell.setCellValue(event.getExerciseID());

      cell = row.createCell(j++);
      cell.setCellValue(event.getContext());

      cell = row.createCell(j++);
      cell.setCellValue(event.getCreatorID());

      cell = row.createCell(j++);
      cell.setCellValue(new Date(event.getTimestamp()));
      cell.setCellStyle(cellStyle);

      cell = row.createCell(j++);
      cell.setCellValue(event.getTimestamp());

      cell = row.createCell(j++);
      cell.setCellValue(event.getHitID());

      cell = row.createCell(j++);
      cell.setCellValue(event.getDevice());

    }
    now = System.currentTimeMillis();
    if (now - then > 100) logger.warn("toXLSX : took " + (now - then) + " millis to write " + rownum +
        " rows to sheet, or " + (now - then) / rownum + " millis/row");
    then = now;
    try {
      wb.write(out);
      now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.warn("toXLSX : took " + (now - then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
      logAndNotify.logAndNotifyServerException(e);
    }
  }

}