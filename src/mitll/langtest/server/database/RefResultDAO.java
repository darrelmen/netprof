package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.shared.Result;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link AnswerDAO}. Not sure if that's a good idea or not. :)
 */
public class RefResultDAO extends DAO {
  private static final Logger logger = Logger.getLogger(RefResultDAO.class);

  private static final String ID = "id";
  private static final String USERID = "userid";
  public static final String ANSWER = "answer";
  private static final String SCORE_JSON = "scoreJson";

  private static final String REFRESULT = "refresult";

  private static final String DURATION = "duration";
  private static final String CORRECT = "correct";
  private static final String PRON_SCORE = "pronscore";

  private static final String ALIGNSCORE = "ALIGNSCORE";
  private static final String ALIGNJSON = "ALIGNJSON";
  private static final String NUMDECODE_PHONES = "NUMDECODEPHONES";
  private static final String NUM_ALIGN_PHONES = "NUMALIGNPHONES";
  private static final String MALE = "male";
  private static final String SPEED = "speed";
  public static final String DECODE_PROCESS_DUR = "decodeProcessDur";
  public static final String ALIGN_PROCESS_DUR = "alignProcessDur";
  public static final String HYDEC_DECODE_PRON_SCORE = "hydecDecodePronScore";
  public static final String HYDEC_DECODE_PROCESS_DUR = "hydecDecodeProcessDur";
  public static final String HYDEC_DECODE_NUM_PHONES = "hydecDecodeNumPhones";
  public static final String HYDEC_ALIGN_PRON_SCORE = "hydecAlignPronScore";
  public static final String HYDEC_ALIGN_PROCESS_DUR = "hydecAlignProcessDur";
  public static final String HYDEC_ALIGN_NUM_PHONES = "hydecAlignNumPhones";
  private final boolean dropTable;
//  private final boolean debug = false;

  /**
   * @param database
   * @param dropTable
   * @see DatabaseImpl#initializeDAOs(PathHelper)
   */
  public RefResultDAO(Database database, boolean dropTable) {
    super(database);
    this.dropTable = dropTable;
  }

  private List<Result> cachedResultsForQuery = null;

  /**
   * @param database
   * @param userID
   * @param id
   * @param audioFile
   * @param correct
   * @param isMale
   * @param speed
   * @return id of new row in result table
   * @see DatabaseImpl#addRefAnswer
   */
  public long addAnswer(Database database,
                        int userID, String id,
                        String audioFile,
                        long durationInMillis,
                        boolean correct,

                        AudioFileHelper.DecodeAlignOutput alignOutput,
                        AudioFileHelper.DecodeAlignOutput decodeOutput,

                        AudioFileHelper.DecodeAlignOutput alignOutputOld,
                        AudioFileHelper.DecodeAlignOutput decodeOutputOld,

                        boolean isMale, String speed) {
    Connection connection = database.getConnection(this.getClass().toString());
    try {
      long then = System.currentTimeMillis();
      long newid = addAnswerToTable(connection, userID, id, audioFile, durationInMillis, correct,
          alignOutput,
          decodeOutput,

          alignOutputOld,
          decodeOutputOld,

          isMale, speed);
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
   * @param connection
   * @param userid
   * @param id
   * @param audioFile
   * @param durationInMillis
   * @param correct
   * @param isMale
   * @param speed
   * @throws java.sql.SQLException
   * @see #addAnswer
   */
  private long addAnswerToTable(Connection connection,
                                int userid, String id,
                                String audioFile,
                                long durationInMillis,
                                boolean correct,

                                AudioFileHelper.DecodeAlignOutput alignOutput,
                                AudioFileHelper.DecodeAlignOutput decodeOutput,

                                AudioFileHelper.DecodeAlignOutput alignOutputOld,
                                AudioFileHelper.DecodeAlignOutput decodeOutputOld,

                                boolean isMale, String speed) throws SQLException {
    //  logger.debug("adding answer for exid #" + id + " correct " + correct + " score " + pronScore + " audio type " +
    // audioType + " answer " + answer);
    PreparedStatement statement = connection.prepareStatement("INSERT INTO " +
            REFRESULT +
            "(" +
            "userid," +
            Database.EXID + "," +
            Database.TIME + "," +
            ANSWER + "," +
            ResultDAO.DURATION + "," +
            ResultDAO.CORRECT + "," +

            ResultDAO.PRON_SCORE + "," +
            ResultDAO.SCORE_JSON + "," +
            NUMDECODE_PHONES + "," +
            DECODE_PROCESS_DUR + "," +

            ALIGNSCORE + "," +
            ALIGNJSON + "," +
            NUM_ALIGN_PHONES + "," +
            ALIGN_PROCESS_DUR + "," +

            HYDEC_DECODE_PRON_SCORE + ", " +
            HYDEC_DECODE_NUM_PHONES + ", " +
            HYDEC_DECODE_PROCESS_DUR + ", " +

            HYDEC_ALIGN_PRON_SCORE + ", " +
            HYDEC_ALIGN_NUM_PHONES + ", " +
            HYDEC_ALIGN_PROCESS_DUR + ", " +

            MALE + "," +
            SPEED +
            ") VALUES(?,?,?,?,?,?," +
            "?,?,?,?," +
            "?,?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?" +
            ")",
        Statement.RETURN_GENERATED_KEYS);

    int i = 1;

    statement.setInt(i++, userid);
    statement.setString(i++, copyStringChar(id));
    statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
    statement.setString(i++, copyStringChar(audioFile));
    statement.setLong(i++, durationInMillis);

    statement.setBoolean(i++, correct);

    statement.setFloat(i++, decodeOutput.getScore());
    statement.setString(i++, decodeOutput.getJson());
    statement.setInt(i++, decodeOutput.getNumPhones());
    statement.setInt(i++, decodeOutput.getProcessDurInMillis());

    statement.setFloat(i++, alignOutput.getScore());
    statement.setString(i++, alignOutput.getJson());
    statement.setInt(i++, alignOutput.getNumPhones());
    statement.setInt(i++, alignOutput.getProcessDurInMillis());

    statement.setFloat(i++, decodeOutputOld.getScore());
    statement.setInt(i++, decodeOutputOld.getNumPhones());
    statement.setInt(i++, decodeOutputOld.getProcessDurInMillis());

    statement.setFloat(i++, alignOutputOld.getScore());
    statement.setInt(i++, alignOutputOld.getNumPhones());
    statement.setInt(i++, alignOutputOld.getProcessDurInMillis());

    statement.setBoolean(i++, isMale);
    statement.setString(i++, speed);

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);

    statement.close();

    return newID;
  }

  private String copyStringChar(String plan) {
    return new String(plan.toCharArray());
  }

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

/*  private void logException(Exception ee) {
    logger.error("got " + ee, ee);
    logAndNotify.logAndNotifyServerException(ee);
  }*/

  public Result getResult(String exid, String answer) {
    String sql = "SELECT * FROM " + REFRESULT + " WHERE " + Database.EXID + "='" + exid + "' AND " + ANSWER + " like '%" + answer + "'";
    try {
      List<Result> resultsSQL = getResultsSQL(sql);
      if (resultsSQL.size() > 1) {
        logger.warn("for " + exid + " and  " + answer + " got " + resultsSQL);
      }
      return resultsSQL.isEmpty() ? null : resultsSQL.iterator().next();
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return null;
  }

  /**
   * @param ids
   * @return
   * @see JsonSupport#getJsonRefResults(Map)
   */
  public JSONObject getJSONScores(Collection<String> ids) {
    try {
      String list = getInList(ids);

      String sql = "SELECT " +
          Database.EXID + ", " + SCORE_JSON + ", " + ANSWER +
          " FROM " + REFRESULT + " WHERE " +
          Database.EXID + " in (" + list + ")";

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();

      Map<String, List<String>> idToAnswers = new HashMap<>();
      Map<String, List<String>> idToJSONs = new HashMap<>();
      while (rs.next()) {
        String exid = rs.getString(Database.EXID);
        String answer = rs.getString(ANSWER);
        String json = rs.getString(SCORE_JSON);

        List<String> orDefault = idToAnswers.get(exid);
        if (orDefault == null) {
          idToAnswers.put(exid, orDefault = new ArrayList<String>());
          int i = answer.lastIndexOf("/");
          String fileName = (i > -1) ? answer.substring(i + 1) : answer;
          orDefault.add(fileName);
        }

        List<String> orDefault2 = idToJSONs.get(exid);
        if (orDefault2 == null) {
          idToJSONs.put(exid, orDefault2 = new ArrayList<String>());
          orDefault2.add(json);
        }
      }

      JSONObject jsonObject = new JSONObject();
      for (Map.Entry<String, List<String>> pair : idToAnswers.entrySet()) {
        String exid = pair.getKey();
        List<String> answers = pair.getValue();
        List<String> jsons = idToJSONs.get(exid);

        JSONArray array = new JSONArray();

        for (int i = 0; i < answers.size(); i++) {
          JSONObject jsonObject1 = new JSONObject();
          jsonObject1.put("file", answers.get(i));
          jsonObject1.put("scoreJSON", jsons.get(i));
          array.add(jsonObject1);
        }
        jsonObject.put(exid, array);
      }

      finish(connection, statement, rs);

      return jsonObject;
    } catch (Exception ee) {
      logException(ee);
    }
    return new JSONObject();
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

    //  logger.debug("running " + sql + " -> " +resultsForQuery.size() + " results");
    return getResultsForQuery(connection, statement);
  }

  public int getNumResults() {
    int numResults = 0;
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + REFRESULT);
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
      String plan = "";
      String exid = rs.getString(Database.EXID);
      int qid = 0;
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      boolean valid = true;
      String type = "";
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);

      Result result = new Result(uniqueID, userID, //id
          plan, // plan
          exid, // id
          qid, // qid
          trimPathForWebPage2(answer), // answer
          valid, // valid
          timestamp.getTime(),
          type, dur, correct, pronScore, "browser");
      result.setJsonScore(rs.getString(SCORE_JSON));
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
    if (dropTable) {
      drop(REFRESULT, connection);
    }
    createTable(connection);

    Collection<String> columns = getColumns(REFRESULT);
    //  logger.debug("for " + REFRESULT + " found " + columns + " and " + getNumResults());
    if (!columns.contains(ALIGNSCORE.toLowerCase())) {
      addFloat(connection, REFRESULT, ALIGNSCORE);
      addVarchar(connection, REFRESULT, ALIGNJSON);
    }
    if (!columns.contains(NUMDECODE_PHONES.toLowerCase())) {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
          REFRESULT + " ADD " + NUMDECODE_PHONES + " INTEGER");
      statement.execute();
      statement.close();
    }

    if (!columns.contains(NUM_ALIGN_PHONES.toLowerCase())) {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
          REFRESULT + " ADD " + NUM_ALIGN_PHONES + " INTEGER");
      statement.execute();
      statement.close();
    }

    if (!columns.contains(MALE)) {
      addBoolean(connection, REFRESULT, MALE);
    }
    if (!columns.contains(SPEED)) {
      addVarchar(connection, REFRESULT, SPEED);
    }
    createIndex(database, Database.EXID, REFRESULT);
    // seems to complain about index on CLOB???
    // createIndex(database, ANSWER, REFRESULT);

    database.closeConnection(connection);
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
        ID + " IDENTITY, " +
        USERID + " INT, " +
        Database.EXID + " VARCHAR, " +
        Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
        ANSWER + " VARCHAR," +
        DURATION + " INT," +

        CORRECT + " BOOLEAN," +

        PRON_SCORE + " FLOAT," +
        SCORE_JSON + " VARCHAR, " +
        DECODE_PROCESS_DUR + " INT, " +
        NUMDECODE_PHONES + " INT, " +

        ALIGNSCORE + " FLOAT," +
        ALIGNJSON + " VARCHAR," +
        ALIGN_PROCESS_DUR + " INT, " +
        NUM_ALIGN_PHONES + " INT, " +

        HYDEC_DECODE_PRON_SCORE + " FLOAT," +
        //SCORE_JSON + " VARCHAR, " +
        HYDEC_DECODE_PROCESS_DUR + " INT, " +
        HYDEC_DECODE_NUM_PHONES + " INT, " +

        HYDEC_ALIGN_PRON_SCORE + " FLOAT," +
        //SCORE_JSON + " VARCHAR, " +
        HYDEC_ALIGN_PROCESS_DUR + " INT, " +
        HYDEC_ALIGN_NUM_PHONES + " INT, " +

        MALE + " BOOLEAN," +
        SPEED + " VARCHAR" +
        ")");
    statement.execute();
    statement.close();
  }
}