package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.Result;
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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link AnswerDAO}. Not sure if that's a good idea or not. :)
 */
public class RefResultDAO extends DAO {
  private static final Logger logger = Logger.getLogger(RefResultDAO.class);

  private static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();
 // private static final int MINUTE = 60 * 1000;

  private static final String ID = "id";
  public static final String USERID = "userid";
  private static final String ANSWER = "answer";
  public static final String SCORE_JSON = "scoreJson";

  public static final String REFRESULT = "refresult";

  static final String DURATION = "duration";
  static final String CORRECT = "correct";
  static final String PRON_SCORE = "pronscore";

  private static final String YES = "Yes";
  private static final String NO = "No";
  private final LogAndNotify logAndNotify;

  private final boolean debug = false;

  /**
   * @param database
   * @param logAndNotify
   * @see DatabaseImpl#initializeDAOs(PathHelper)
   */
  public RefResultDAO(Database database, LogAndNotify logAndNotify) {
    super(database);
    this.logAndNotify = logAndNotify;
  }

  private List<Result> cachedResultsForQuery = null;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @param database
   * @param userID
   * @param id
   * @param answer
   * @param audioFile
   * @param correct
   * @param pronScore
   * @param scoreJson
   *  @return id of new row in result table
   */
  public long addAnswer(Database database, int userID, String id, String answer,
                        String audioFile, int durationInMillis,
                        boolean correct, float pronScore, String scoreJson) {
    Connection connection = database.getConnection(this.getClass().toString());
    try {
      long then = System.currentTimeMillis();
      long newid = addAnswerToTable(connection, userID, id, answer, audioFile, durationInMillis, correct, pronScore,  scoreJson);
      long now = System.currentTimeMillis();
      if (now - then > 100) System.out.println("took " + (now - then) + " millis to record answer.");
      return newid;

    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    } finally {
      database.closeConnection(connection);
    }
    return -1;
  }

  /**
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
   *
   *
   * @param connection
   * @param userid
   * @param id
   * @param answer
   * @param audioFile
   * @param correct
   * @param pronScore
   * @param scoreJson
   * @throws java.sql.SQLException
   */
  private long addAnswerToTable(Connection connection, int userid, String id,
                                String answer, String audioFile,
                                 int durationInMillis,
                                boolean correct, float pronScore,  String scoreJson
                                ) throws SQLException {
    //  logger.debug("adding answer for exid #" + id + " correct " + correct + " score " + pronScore + " audio type " +audioType + " answer " + answer);

    PreparedStatement statement = connection.prepareStatement("INSERT INTO " +
        REFRESULT +
        "(" +
        "userid," +
        Database.EXID + "," +
        Database.TIME + "," +
        "answer," +
        ResultDAO.DURATION + "," +
        ResultDAO.CORRECT + "," +
        ResultDAO.PRON_SCORE + "," +
        ResultDAO.SCORE_JSON +
        ") VALUES(?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

    int i = 1;

    boolean isAudioAnswer = answer == null || answer.length() == 0;
    String answerInserted = isAudioAnswer ? audioFile : answer;

    statement.setInt(i++, userid);
    statement.setString(i++, copyStringChar(id));
    statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
    statement.setString(i++, copyStringChar(answerInserted));
    statement.setInt(i++, durationInMillis);

    statement.setBoolean(i++, correct);
    statement.setFloat(i++, pronScore);
    statement.setString(i++, scoreJson);

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);

    statement.close();

    return newID;
  }

  private String copyStringChar(String plan) { return new String(plan.toCharArray());  }


  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see DatabaseImpl#populateUserToNumAnswers
   */
  public List<Result> getResults() {
    try {
      synchronized (this) {
        if (cachedResultsForQuery != null) {
          return cachedResultsForQuery;
        }
      }
      String sql = "SELECT * FROM " + REFRESULT;
      List<Result> resultsForQuery = getResultsSQL(sql);

      synchronized (this) {
        cachedResultsForQuery = resultsForQuery;
      }
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<Result>();
  }

  private void logException(Exception ee) {
    logger.error("got " + ee, ee);
    logAndNotify.logAndNotifyServerException(ee);
  }



  /**
   * @param sql
   * @return
   * @throws SQLException
   * @see #getResults()
   */
  private List<Result> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getResultsForQuery(connection, statement);
  }


  public int getNumResults() {
    int numResults = 0;
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + REFRESULT + ";");
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        numResults = rs.getInt(1);
      }
      finish(connection, statement, rs);
    } catch (Exception ee) {
      logException(ee);
    }
    return numResults;
  }

  /**
   * Get a list of Results for this Query.
   *
   * @param connection
   * @param statement
   * @return
   * @throws SQLException
   * @see #getResultsSQL(String)
   */
  private List<Result> getResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<Result> results = new ArrayList<Result>();
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String plan = "";//rs.getString(PLAN);
      String exid = rs.getString(Database.EXID);
      int qid = 0;//rs.getInt(QID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      boolean valid = true;//rs.getBoolean(VALID);
      // boolean flq = rs.getBoolean(FLQ);
      //  boolean spoken = rs.getBoolean(SPOKEN);

      String type = "";//rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      //String stimulus = rs.getString(STIMULUS);

      Result result = new Result(uniqueID, userID, //id
          plan, // plan
          exid, // id
          qid, // qid
          trimPathForWebPage2(answer), // answer
          valid, // valid
          timestamp.getTime(),
          //flq, spoken,
          type, dur, correct, pronScore, "browser");
//      result.setStimulus(stimulus);
      results.add(result);
    }
    finish(connection, statement, rs);

    return results;
  }



  private String trimPathForWebPage2(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }

  /**
   * No op if table exists and has the current number of columns.
   *
   * @param connection
   * @throws SQLException
   * @see DatabaseImpl#initializeDAOs
   */
  void createResultTable(Connection connection) throws SQLException {
    createTable(connection);

    database.closeConnection(connection);

    createIndex(database, Database.EXID, REFRESULT);
  }

  /**
   * So we don't want to use CURRENT_TIMESTAMP as the default for TIMESTAMP
   * b/c if we ever alter the table, say by adding a new column, we will effectively lose
   * the timestamp that was put there when we inserted the row initially.
   * <p></p>
   * Note that the answer column can be either the text of an answer for a written response
   * or a relative path to an audio file on the server.
   *
   * @param connection to make a statement from
   * @throws SQLException
   */
  private void createTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        REFRESULT +
        " (" +
        ID +
        " IDENTITY, " +
        USERID + " INT, " +
        Database.EXID + " VARCHAR, " +
        Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
        "answer CLOB," +
        DURATION + " INT," +
        CORRECT + " BOOLEAN," +
        PRON_SCORE + " FLOAT," +
        SCORE_JSON + " VARCHAR" +
        ")");
    statement.execute();
    statement.close();
  }

  /**
   *
   * @param typeOrder
   * @param out
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeExcelToStream(Collection<MonitorResult> results, List<String> typeOrder, OutputStream out) {
    writeToStream(out, writeExcel(results, typeOrder));
  }

  private SXSSFWorkbook writeExcel(Collection<MonitorResult> results,  List<String> typeOrder
  ) {
    long now;
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(10000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("Results");
    int rownum = 0;
    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss 'yy"));
    //DateTimeFormat format = DateTimeFormat.getFormat("MMM dd h:mm:ss a z ''yy");
    Row headerRow = sheet.createRow(rownum++);

    List<String> columns = new ArrayList<String>(Arrays.asList(
        USERID, "Exercise", "Text"));


    for (final String type : typeOrder) {  columns.add(type);  }

    List<String> columns2 = Arrays.asList(
        "Recording",
        Database.TIME,
        "type",//AUDIO_TYPE,
        DURATION,
        "Valid",
        CORRECT, PRON_SCORE, "Device"
    );

    columns.addAll(columns2);

    for (int i = 0; i < columns.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(columns.get(i));
    }

    for (MonitorResult result : results) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(result.getUserid());
      cell = row.createCell(j++);
      cell.setCellValue(result.getId());
      cell = row.createCell(j++);
      cell.setCellValue(result.getForeignText());

      for (String type : typeOrder) {
        cell = row.createCell(j++);
        cell.setCellValue(result.getUnitToValue().get(type));
      }

      cell = row.createCell(j++);
      cell.setCellValue(result.getAnswer());

      cell = row.createCell(j++);
      cell.setCellValue(new Date(result.getTimestamp()));
      cell.setCellStyle(cellStyle);

      cell = row.createCell(j++);
      String audioType = result.getAudioType();
      cell.setCellValue(audioType.equals("avp")?"flashcard": audioType);

      cell = row.createCell(j++);
      cell.setCellValue(result.getDurationInMillis());

      cell = row.createCell(j++);
      cell.setCellValue(result.isValid() ? YES : NO);

      cell = row.createCell(j++);
      cell.setCellValue(result.isCorrect() ? YES : NO);

      cell = row.createCell(j++);
      cell.setCellValue(result.getPronScore());

      cell = row.createCell(j++);
      cell.setCellValue(result.getDevice());
    }
    now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.warn("toXLSX : took " + (now - then) + " millis to add " + rownum + " rows to sheet, or " + (now - then) / rownum + " millis/row");
    }
    return wb;
  }


  private void writeToStream(OutputStream out, SXSSFWorkbook wb) {
    long then = System.currentTimeMillis();
    try {
      wb.write(out);
      long now2 = System.currentTimeMillis();
      if (now2 - then > 100) {
        logger.warn("toXLSX : took " + (now2 - then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }
}