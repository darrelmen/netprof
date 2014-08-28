package mitll.langtest.server.database.instrumentation;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

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
  private static final String WIDGETTYPE = "widgettype";
  private static final String HITID = "hitid";
  private static final String EXERCISEID = "exerciseid";
  private final UserDAO userDAO;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   * @param database
   * @param userDAO
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

      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @param database
   * @throws java.sql.SQLException
   */
  void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      EVENT +
      " (" +
      "uniqueid IDENTITY, " +
      CREATORID + " INT, " +
      EXERCISEID + " VARCHAR, " +
      "context VARCHAR, " +
      "widgetid VARCHAR, " +
      WIDGETTYPE +
      " VARCHAR, " +
      "modified TIMESTAMP, " +
      HITID +
      " VARCHAR, " +
      "FOREIGN KEY(" +
      CREATORID +
      ") REFERENCES " +
      "USERS" +
      "(ID)" +
      ")");

    finish(database, connection, statement);
  }


  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see mitll.langtest.server.database.DatabaseImpl#logEvent(String, String, String, String, long, String)
   */
  public void add(Event event) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    try {
      // there are much better ways of doing this...

      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + EVENT +
          "(" +
          CREATORID +
          "," +
          EXERCISEID +
          ",context," +
          "widgetid," +
          WIDGETTYPE +
          "," +
          HITID +
          "," +
          "modified) " +
          "VALUES(?,?,?,?,?,?,?);");
      int i = 1;

      long creatorID = event.getCreatorID();
      boolean missingCreator = creatorID == -1;
      if (missingCreator) {
        event.setTimestamp(System.currentTimeMillis());
        logger.warn("creator is " + creatorID + " for " + event);
        creatorID = userDAO.getDefectDetector();
      }
      statement.setLong(i++, creatorID);
      statement.setString(i++, event.getExerciseID());
      statement.setString(i++, event.getContext());
      statement.setString(i++, event.getWidgetID());
      statement.setString(i++, event.getWidgetType());
      statement.setString(i++, event.getHitID());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      statement.close();

      //logger.debug("now " + getCount(EVENT) + " and event is " + event);
    } catch (SQLException ee) {
      logger.error("got " + ee, ee);
      throw ee;
    } finally {
      database.closeConnection(connection);
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

  public void addPlayedMarkings(long userID, CommonExercise firstExercise) {
    List<Event> allForUserAndExercise = getAllForUserAndExercise(userID, firstExercise.getID());
    Map<String, AudioAttribute> audioToAttr = firstExercise.getAudioRefToAttr();
    for (Event event : allForUserAndExercise) {
      AudioAttribute audioAttribute = audioToAttr.get(event.getContext());
      if (audioAttribute == null) {
        //logger.warn("addPlayedMarkings huh? can't find " + event.getContext() + " in " + audioToAttr.keySet());
      }
      else {
        audioAttribute.setHasBeenPlayed(true);
      }
    }
  }

  public List<Event> getAllForUserAndExercise(long userid, String exid) {
    try {
      String sql = "SELECT * from " + EVENT + " where " +
        WIDGETTYPE +
        "='qcPlayAudio' AND " +
        CREATORID +"="+userid + " and " +
        EXERCISEID + "='" +exid+
        "'";

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
/*  public List<Event> getAllBy(long userid) {
    try {
      String sql = "SELECT * from " + EVENT + " where " + CREATORID +"="+userid;

      return getEvents(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }*/

  private List<Event> getEvents(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
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
          rs.getTimestamp("modified").getTime(),
          rs.getString(HITID)
          )
      );
    }

    //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
    finish(connection, statement, rs);
    return lists;
  }

  private static final List<String> COLUMNS2 = Arrays.asList("id", "type", "exercise", "context", "userid", "timestamp","time_millis", "hitID");

  /**
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param out
   */
  public void toXLSX(OutputStream out) {
    long then = System.currentTimeMillis();

    List<Event> all = getAll();
    long now = System.currentTimeMillis();
    if (now-then > 100) logger.info("toXLSX : took " + (now - then) + " millis to read " + all.size() +
      " events from database");
    then = now;

    //Workbook wb = new XSSFWorkbook();
    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk

    Sheet sheet = wb.createSheet("Events");
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

    }
    now = System.currentTimeMillis();
    if (now-then > 100) logger.warn("toXLSX : took " + (now-then) + " millis to write " + rownum+
      " rows to sheet, or " + (now-then)/rownum + " millis/row");
    then = now;
    try {
      wb.write(out);
      now = System.currentTimeMillis();
      if (now-then > 100) {
        logger.warn("toXLSX : took " + (now-then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " +e,e);
    }
  }

}