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

package mitll.langtest.server.database.refaudio;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.JsonSupport;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.shared.answer.AudioType;
import mitll.npdata.dao.SlickRefResultJson;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

import static mitll.langtest.server.database.Database.EXID;
import static mitll.langtest.server.database.result.ResultDAO.MODEL;
import static mitll.langtest.server.database.result.ResultDAO.MODELUPDATE;

/**
 * Create, drop, alter, read from the results table.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class RefResultDAO extends BaseRefResultDAO implements IRefResultDAO {
  private static final Logger logger = LogManager.getLogger(RefResultDAO.class);

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

  public static final String ALIGNSCORE = "ALIGNSCORE";
  private static final String ALIGNJSON = "ALIGNJSON";
  private static final String NUMDECODE_PHONES = "NUMDECODEPHONES";
  private static final String NUM_ALIGN_PHONES = "NUMALIGNPHONES";
  private static final String MALE = "male";
  public static final String SPEED = "speed";
  public static final String DECODE_PROCESS_DUR = "decodeProcessDur";
  public static final String ALIGN_PROCESS_DUR = "alignProcessDur";
  public static final String HYDEC_DECODE_PRON_SCORE = "hydecDecodePronScore";
  public static final String HYDEC_DECODE_PROCESS_DUR = "hydecDecodeProcessDur";
  private static final String HYDEC_DECODE_NUM_PHONES = "hydecDecodeNumPhones";
  public static final String HYDEC_ALIGN_PRON_SCORE = "hydecAlignPronScore";
  public static final String HYDEC_ALIGN_PROCESS_DUR = "hydecAlignProcessDur";
  private static final String HYDEC_ALIGN_NUM_PHONES = "hydecAlignNumPhones";
  private static final String WORDS = "{\"words\":[]}";
  //  private final boolean debug = false;
//  private final boolean dropTable;
  //  private final boolean debug = false;
  private final String currentModel;

  /**
   * @param database
   * @param dropTable
   * @see DatabaseImpl#initializeDAOs(PathHelper)
   */
  public RefResultDAO(Database database, boolean dropTable) {
    super(database, dropTable);
    currentModel = database.getServerProps().getCurrentModel();
  }

  @Override
  public boolean removeForExercise(int exid) {
    return remove(REFRESULT, EXID, ""+exid, true);
  }

  private List<Result> cachedResultsForQuery = null;

  /**
   * @param userID
   * @param exid
   * @param audioFile
   * @param correct
   * @param isMale
   * @param speed
   * @return id of new row in result table
   * @see DatabaseImpl#addRefAnswer
   */
  @Override
  public long addAnswer(int userID,
                        int exid,
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
      long newid = addAnswerToTable(connection, userID, ""+ exid, audioFile, durationInMillis, correct,
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
   * @seex IRefResultDAO#addAnswer
   */
  private long addAnswerToTable(Connection connection,
                                int userid,
                                String id,
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
            DURATION + "," +
            CORRECT + "," +

            PRON_SCORE + "," +
            SCORE_JSON + "," +
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

  @Override
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
  @Override
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

  @Override
  public List<SlickRefResultJson> getJsonResults() {
    return null;
  }

  public Result getRefForExAndAudio(String exid, String answer) {
    Result latestResult = null;

    try {
      String exidPrefix = SELECT_PREFIX + "='" + exid + "'";
      List<Result> resultsSQL = getResultsSQL(exidPrefix);
      if (resultsSQL.size() > 0) {
        logger.info("getRefForExAndAudio got " + resultsSQL.size() + " for " + exid);
      }

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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPretestScore
   */
  @Override
  public Result getResult(int exid, String answer) {
    String sql = SELECT_ALL +
        " WHERE " + EXID + "='" + exid + "' AND " + ANSWER + " like '%" + answer + "'";
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
  @Override
  public JSONObject getJSONScores(Collection<Integer> ids) {
    try {
      String list = getInList(ids);

      String sql = "SELECT " +
          EXID + ", " + SCORE_JSON + ", " + ANSWER +
          " FROM " + REFRESULT + " WHERE " +
          EXID + " in (" + list + ")";

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();

      Map<Integer, List<String>> idToAnswers = new HashMap<>();
      Map<Integer, List<String>> idToJSONs = new HashMap<>();
      while (rs.next()) {
        String exid = rs.getString(EXID);
        int i = Integer.parseInt(exid);
        String answer = rs.getString(ANSWER);
        String json = rs.getString(SCORE_JSON);

        addToAnswers(idToAnswers, i, answer);
        addToJSONs(idToJSONs, i, json);
      }

      JSONObject jsonObject = getJsonObject(idToAnswers, idToJSONs);

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

    List<Result> resultsForQuery = getResultsForQuery(connection, statement);
    //   logger.debug("getResultsSQL running " + sql + " -> " +resultsForQuery.size() + " results");
    return resultsForQuery;
  }

  @Override
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
   * <p>
   * Use decode score for result score
   * use decode alignment (if valid) for alignment json
   *
   * @param connection
   * @param statement
   * @return
   * @throws SQLException
   * @see #getResultsSQL(String)
   */
  private List<Result> getResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<Result> results = new ArrayList<>();

    int count = 0;
    int skipped = 0;
    long then = System.currentTimeMillis();


    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      int userID = rs.getInt(USERID);
      String exid = rs.getString(EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      int dur = rs.getInt(DURATION);

      boolean isMale = rs.getBoolean(MALE);
      boolean correct = rs.getBoolean(CORRECT);

      float pronScore = rs.getFloat(PRON_SCORE);
      String scoreJson = rs.getString(SCORE_JSON);
      boolean validDecodeJSON = pronScore > 0 && !scoreJson.equals(WORDS);

      String speed = rs.getString(SPEED);
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
        AudioType audioType = speed.equals("reg") ? AudioType.REGULAR : speed.equals("slow") ? AudioType.SLOW : AudioType.UNSET;
        int exid1 = 0;
        try {
          exid1 = Integer.parseInt(exid);
        } catch (NumberFormatException e) {
          //logger.warn("couldn't parse " +exid);
        }
        Result result = new Result(uniqueID, userID, //id
            // "", // plan
            exid1, // id
            0, // qid
            trimPathForWebPage2(answer), // answer
            true, // valid
            timestamp.getTime(),
            audioType, dur,
            correct, pronScore1,
            "browser", "",
            0, 0, false, 30, "");
        result.setMale(isMale);

        long hydraDecodeDur = rs.getLong(RefResultDAO.DECODE_PROCESS_DUR);
        long hydraAlignDur = rs.getLong(RefResultDAO.ALIGN_PROCESS_DUR);

        result.setDecodeOutput(new DecodeAlignOutput(pronScore, scoreJson, hydraDecodeDur, correct, rs.getInt(NUMDECODE_PHONES)));
        result.setAlignOutput(new DecodeAlignOutput(alignScore, alignJSON, hydraAlignDur, true, rs.getInt(NUM_ALIGN_PHONES)));
//            "", dur, correct, pronScore1,
//            "browser",
//            model);
        result.setJsonScore(scoreJson1);
        result.setOldExID(exid);
        results.add(result);
      } else {
        if (skipped < 20) {
          logger.info("getResultsForQuery skipping invalid ref (decode score " + pronScore + " align score " + alignScore +
              "  result " + exid + " : " + answer);
        }
        skipped++;
      }
    }

    long now = System.currentTimeMillis();

    logger.info("getResultsForQuery took " + (now - then) + " millis, found " + count + " invalid decode results, skipped " + skipped);
    finish(connection, statement, rs);

    return results;
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

    createIndex(database, EXID, REFRESULT);
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