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

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.shared.Result;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

import static mitll.langtest.server.database.Database.EXID;
import static mitll.langtest.server.database.ResultDAO.MODEL;
import static mitll.langtest.server.database.ResultDAO.MODELUPDATE;

/**
 * Create, drop, alter, read from the results table.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class RefResultDAO extends DAO {
  private static final Logger logger = Logger.getLogger(RefResultDAO.class);

  private static final String ID = "id";
  private static final String USERID = "userid";
  private static final String ANSWER = "answer";
  private static final String SCORE_JSON = "scoreJson";

  private static final String REFRESULT = "refresult";
  private static final String SELECT_ALL = "SELECT * FROM " + REFRESULT;
  public static final String SELECT_PREFIX = SELECT_ALL + " WHERE " + EXID;

  private static final String DURATION = "duration";
  private static final String CORRECT = "correct";
  private static final String PRON_SCORE = "pronscore";

  static final String ALIGNSCORE = "ALIGNSCORE";
  private static final String ALIGNJSON = "ALIGNJSON";
  private static final String NUMDECODE_PHONES = "NUMDECODEPHONES";
  private static final String NUM_ALIGN_PHONES = "NUMALIGNPHONES";
  private static final String MALE = "male";
  static final String SPEED = "speed";
  static final String DECODE_PROCESS_DUR = "decodeProcessDur";
  static final String ALIGN_PROCESS_DUR = "alignProcessDur";
  static final String HYDEC_DECODE_PRON_SCORE = "hydecDecodePronScore";
  static final String HYDEC_DECODE_PROCESS_DUR = "hydecDecodeProcessDur";
  private static final String HYDEC_DECODE_NUM_PHONES = "hydecDecodeNumPhones";
  static final String HYDEC_ALIGN_PRON_SCORE = "hydecAlignPronScore";
  static final String HYDEC_ALIGN_PROCESS_DUR = "hydecAlignProcessDur";
  private static final String HYDEC_ALIGN_NUM_PHONES = "hydecAlignNumPhones";
  private static final String WORDS = "{\"words\":[]}";
  private final boolean dropTable;
  //  private final boolean debug = false;
  private final String currentModel;

  /**
   * @param database
   * @param dropTable
   * @see DatabaseImpl#initializeDAOs(PathHelper)
   */
  RefResultDAO(Database database, boolean dropTable) {
    super(database);
    this.dropTable = dropTable;
    currentModel = database.getServerProps().getCurrentModel();
  }

  public boolean removeForExercise(String exid) {
    return remove(REFRESULT, EXID, exid, true);
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
  long addAnswer(Database database,
                 int userID, String id,
                 String audioFile,
                 long durationInMillis,
                 boolean correct,

                 DecodeAlignOutput alignOutput,
                 DecodeAlignOutput decodeOutput,

                 DecodeAlignOutput alignOutputOld,
                 DecodeAlignOutput decodeOutputOld,

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

                                DecodeAlignOutput alignOutput,
                                DecodeAlignOutput decodeOutput,

                                DecodeAlignOutput alignOutputOld,
                                DecodeAlignOutput decodeOutputOld,

                                boolean isMale, String speed) throws SQLException {
    //  logger.debug("adding answer for exid #" + id + " correct " + correct + " score " + pronScore + " audio type " +
    // audioType + " answer " + answer);
    PreparedStatement statement = connection.prepareStatement("INSERT INTO " +
            REFRESULT +
            "(" +
            "userid," +
            EXID + "," +
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
            SPEED + "," +

            MODEL + "," +
            MODELUPDATE +
            ") " +
            "VALUES(?,?,?,?,?,?," +
            "?,?,?,?," +
            "?,?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?," +
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

    statement.setString(i++, currentModel);
    statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);

    statement.close();

    return newID;
  }

  public boolean removeForAudioFile(String audioFile) {
    return remove(REFRESULT, ANSWER, audioFile, false);
  }

  private String copyStringChar(String plan) {
    return new String(plan.toCharArray());
  }

  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see RefResultDecoder#getDecodedFiles()
   */
  public List<Result> getResults() {
    try {
      synchronized (this) {
        if (cachedResultsForQuery != null) {
          return cachedResultsForQuery;
        }
      }
      List<Result> resultsForQuery = getResultsSQL(SELECT_ALL);

      synchronized (this) {
        cachedResultsForQuery = resultsForQuery;
      }
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  public Result getRefForExAndAudio(String exid, String answer) {
    Result latestResult = null;

    try {
      String exidPrefix = SELECT_PREFIX + "='" + exid + "'";
      List<Result> resultsSQL = getResultsSQL(exidPrefix);
//      if (resultsSQL.size() > 0) {
//        logger.info("getRefForExAndAudio got " + resultsSQL.size() + " for " + exid);
//      }

      long latest = 0;
      for (Result res : resultsSQL) {
        if (res.getAnswer().endsWith(answer)) {
          if (res.getUniqueID() > latest) {
            latest = res.getUniqueID();
            latestResult = res;
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return latestResult;
  }

  /**
   * @param exid
   * @param answer
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPretestScore(int, long, String, String, int, int, boolean, String, boolean)
   */
  public Result getResult(String exid, String answer) {
    String sql = SELECT_ALL + " WHERE " + EXID + "='" + exid + "' AND " + ANSWER + " like '%" + answer + "'";
    try {
      List<Result> resultsSQL = getResultsSQL(sql);
      if (resultsSQL.size() > 1) {
        logger.warn("for " + exid + " and  " + answer + " got " + resultsSQL.size());
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
          EXID + ", " + SCORE_JSON + ", " + ANSWER +
          " FROM " + REFRESULT + " WHERE " +
          EXID + " in (" + list + ")";

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();

      Map<String, List<String>> idToAnswers = new HashMap<>();
      Map<String, List<String>> idToJSONs = new HashMap<>();
      while (rs.next()) {
        String exid = rs.getString(EXID);
        String answer = rs.getString(ANSWER);
        String json = rs.getString(SCORE_JSON);

        List<String> orDefault = idToAnswers.get(exid);
        if (orDefault == null) {
          idToAnswers.put(exid, orDefault = new ArrayList<>());
          int i = answer.lastIndexOf("/");
          String fileName = (i > -1) ? answer.substring(i + 1) : answer;
          orDefault.add(fileName);
        }

        List<String> orDefault2 = idToJSONs.get(exid);
        if (orDefault2 == null) {
          idToJSONs.put(exid, orDefault2 = new ArrayList<>());
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

      finish(connection, statement, rs, sql);

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

    long then = System.currentTimeMillis();
    List<Result> resultsForQuery = getResultsForQuery(connection, statement, sql);
    long now = System.currentTimeMillis();
//    logger.info("getResultsSQL took " + (now - then) + " millis to exec query for " +sql);
    //   logger.debug("getResultsSQL running " + sql + " -> " +resultsForQuery.size() + " results");
    return resultsForQuery;
  }

  public int getNumResults() {
    int numResults = 0;
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "SELECT COUNT(*) FROM " + REFRESULT;
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        numResults = rs.getInt(1);
      }
      finish(connection, statement, rs, sql);
    } catch (Exception ee) {
      logException(ee);
    }
    return numResults;
  }

  /**
   * Get a list of Results for this Query.
   * <p>
   * Use decode score for result score
   * use decode alignment (if valid) for alignment json
   *
   * @param connection
   * @param statement
   * @param sql
   * @return
   * @throws SQLException
   * @see #getResultsSQL(String)
   */
  private List<Result> getResultsForQuery(Connection connection, PreparedStatement statement, String sql) throws SQLException {
    long then = System.currentTimeMillis();
    ResultSet rs = statement.executeQuery();
    long now = System.currentTimeMillis();
//    logger.info("getResultsForQuery took " + (now - then) + " millis to exec query");

    List<Result> results = new ArrayList<>();

    int count = 0;
    int skipped = 0;
    then = System.currentTimeMillis();


    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String exid = rs.getString(EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);

      float pronScore = rs.getFloat(PRON_SCORE);
      String scoreJson = rs.getString(SCORE_JSON);
      boolean validDecodeJSON = pronScore > 0 && !scoreJson.equals(WORDS);

      if (!validDecodeJSON) {
        if (count++ < 10) {
          logger.info("getResultsForQuery got invalid decode result, score " + pronScore +
              " json " + scoreJson + " for " + exid);
        }
      }

      float alignScore = rs.getFloat(ALIGNSCORE);
      String alignJSON = rs.getString(ALIGNJSON);
      String model = rs.getString(MODEL);

      boolean validAlignJSON = alignScore > 0 && !alignJSON.contains(WORDS);

      if (validAlignJSON || validDecodeJSON) {
        float pronScore1 = validDecodeJSON ? pronScore : alignScore;
        String scoreJson1 = validDecodeJSON ? scoreJson : alignJSON;

        Result result = new Result(uniqueID, userID, //id
            "", // plan
            exid, // id
            0, // qid
            trimPathForWebPage2(answer), // answer
            true, // valid
            timestamp.getTime(),
            "", dur, correct, pronScore1,
            "browser",
            model);
        result.setJsonScore(scoreJson1);
        results.add(result);
      } else {
        if (skipped < 20) {
          logger.info("getResultsForQuery skipping invalid ref (decode score " + pronScore + " align score " + alignScore +
              "  result " + exid + " : " + answer);
        }
        skipped++;
      }
    }

    now = System.currentTimeMillis();

 //   logger.info("getResultsForQuery took " + (now - then) + " millis, found " + count + " invalid decode results, skipped " + skipped);
    finish(connection, statement, rs, sql);

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
    if (!columns.contains(DECODE_PROCESS_DUR.toLowerCase())) {
      addInt(connection, REFRESULT, DECODE_PROCESS_DUR);
    }
    if (!columns.contains(ALIGN_PROCESS_DUR.toLowerCase())) {
      addInt(connection, REFRESULT, ALIGN_PROCESS_DUR);
    }

    if (!columns.contains(MODEL.toLowerCase())) {
      addVarchar(connection, REFRESULT, MODEL);
    }
    if (!columns.contains(MODELUPDATE.toLowerCase())) {
      addTimestamp(connection, REFRESULT, MODELUPDATE);
    }

    createTableIndex(database, EXID, REFRESULT);
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
        EXID + " VARCHAR, " +
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