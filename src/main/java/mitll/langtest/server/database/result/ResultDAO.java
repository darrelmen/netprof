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

package mitll.langtest.server.database.result;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.Report;
import mitll.langtest.server.database.user.UserManagement;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.result.MonitorResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

import static mitll.langtest.server.database.Database.EXID;

public class ResultDAO extends BaseResultDAO implements IResultDAO {
  private static final Logger logger = LogManager.getLogger(ResultDAO.class);

  public static final String ID = "id";
  public static final String USERID = "userid";
  // private static final String PLAN = "plan";
  private static final String QID = "qid";
  public static final String ANSWER = "answer";
  public static final String SCORE_JSON = "scoreJson";
  public static final String WITH_FLASH = "withFlash";
  private static final String VALID = "valid";

  public static final String RESULTS = "results";

  static final String FLQ = "flq";
  static final String SPOKEN = "spoken";
  public static final String AUDIO_TYPE = "audioType";
  public static final String DURATION = "duration";
  public static final String CORRECT = "correct";
  public static final String PRON_SCORE = "pronscore";
  private static final String STIMULUS = "stimulus";
  public static final String DEVICE_TYPE = "deviceType";  // iPad, iPhone, browser, etc.
  public static final String DEVICE = "device"; // device id, or browser type
  public static final String PROCESS_DUR = "processDur";
  public static final String ROUND_TRIP_DUR = "roundTripDur";
  // public static final int FIVE_MINUTES = 5 * 60 * 1000;
  // public static final int HOUR = 60 * 60 * 1000;
  // public static final int DAY = 24 * HOUR;
  private static final String DEVICETYPE = "devicetype";
  public static final String VALIDITY = "validity";
  public static final String SNR = "SNR";
  private static final String SESSION = "session"; // from ODA

  public static final String USER_SCORE = "userscore";
  //public static final String CLASSIFIER_SCORE = "classifierscore";
  static final String TRANSCRIPT = "transcript";
  public static final String MODEL = "model";
  public static final String MODELUPDATE = "modelupdate";

  private final boolean debug = true;

  /**
   * @param database
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public ResultDAO(Database database) {
    super(database);
  //  logger.warn("\n\n\n\nmade h2 result dao " + this);
  }

  @Override
  public long getFirstTime(int projid) {
    return 0;
  }

  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see UserManagement#populateUserToNumAnswers
   * @see #getUserToResults
   * @see Report#getResults
   */
  @Override
  public List<Result> getResults() {
    try {
      String sql = "SELECT * FROM " + RESULTS;
      List<Result> resultsForQuery = getResultsSQL(sql);
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  @Override
  public Collection<UserAndTime> getUserAndTimes() {
    try {
      String sql = "SELECT " + USERID + "," + EXID + "," + Database.TIME + ", "
          + QID + " FROM " + RESULTS;
      return getUserAndTimeSQL(sql);
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  @Override
  public Collection<MonitorResult> getResultsDevices(int projid) {
    try {
      String sql = "SELECT * FROM " + RESULTS + " where " + DEVICETYPE + " like 'i%'";
      return Collections.emptyList();
    //  return getResultsSQL(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      logException(ee);
    }
    return new ArrayList<>();
  }

  List<CorrectAndScore> getAllCorrectAndScores(String language) {
    try {
      return getScoreResultsSQL(getCSSelect() + " FROM " + RESULTS);
    } catch (SQLException e) {
      logException(e);
      return Collections.emptyList();
    }
  }

  @Override
  Map<Integer, List<CorrectAndScore>> getCorrectAndScoreMap(Collection<Integer> ids, int userid, String language) {
    return null;
  }

  /**
   * @param projid
   * @return
   * @see DatabaseImpl#getMonitorResults(int)
   */
  @Override
  public List<MonitorResult> getMonitorResults(int projid) {
    try {
      synchronized (this) {
        if (cachedMonitorResultsForQuery != null) {
          return cachedMonitorResultsForQuery;
        }
      }
      List<MonitorResult> resultsForQuery = getMonitorResultsSQL("SELECT * FROM " + RESULTS);

      synchronized (this) {
        cachedMonitorResultsForQuery = resultsForQuery;
      }
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }

    return new ArrayList<>();
  }

  @Override
  public Result getResultByID(int id) {
    String sql = "SELECT * FROM " + RESULTS + " WHERE " + ID + "='" + id + "'";
    try {
      List<Result> resultsSQL = getResultsSQL(sql);
      if (resultsSQL.size() > 1) {
        logger.error("for " + id + " got " + resultsSQL);
      } else if (resultsSQL.isEmpty()) {
        logger.error("no result for " + id);
      }
      return resultsSQL.isEmpty() ? null : resultsSQL.iterator().next();
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return null;
  }


  @Override
  public List<MonitorResult> getMonitorResultsByID(int id) {
    try {
      String sql = "SELECT * FROM " + RESULTS + " WHERE " + EXID + "='" + id + "'";
      return getMonitorResultsSQL(sql);
    } catch (Exception ee) {
      logException(ee);
    }

    return new ArrayList<>();
  }

  /**
   * Only take latest five.
   *
   * @param userID
   * @param id
   * @return
   * @see mitll.langtest.server.ScoreServlet#doGet
   */
/*  public JSONObject getHistoryAsJson(long userID, String id) {
    List<CorrectAndScore> resultsForExercise = getResultsForExIDInForUser(userID, true, id);
    int size = resultsForExercise.size();
    if (size > LAST_NUM_RESULTS) {
      resultsForExercise = resultsForExercise.subList(size - LAST_NUM_RESULTS, size);
    }
//    logger.debug("score history " + resultsForExercise);
    int total = 0;
    float scoreTotal = 0f;
    net.sf.json.JSONArray jsonArray = new net.sf.json.JSONArray();

    for (CorrectAndScore r : resultsForExercise) {
      float pronScore = r.getScore();
      if (pronScore > 0) { // overkill?
        total++;
        scoreTotal += pronScore;
      }
      jsonArray.add(r.isCorrect() ? YES : NO);
    }

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("score", total == 0 ? 0f : scoreTotal / new Integer(total).floatValue());
    jsonObject.put("history", jsonArray);
    return jsonObject;
  }*/
/*  private List<CorrectAndScore> getResultsForExIDInForUser(long userID, boolean isFlashcardRequest, String id) {
    return getResultsForExIDInForUser(Collections.singleton(id), isFlashcardRequest, userID);
  }*/

  /**
   * Only take avp audio type and *valid* audio.
   *
   * @param ids
   * @param language
   * @return
   * @see #getSessionsForUserIn2
   */

  public List<CorrectAndScore> getResultsForExIDIn(Collection<Integer> ids, String language) {
    try {
      String list = getInList(ids);

      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          VALID + "=true" + " AND " +
          //AUDIO_TYPE + (matchAVP?"=":"<>") + "'avp'" +" AND " +
          getAVPClause(true) + " AND " +

          EXID + " in (" + list + ")" +
          " order by " + Database.TIME + " asc";

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);

      List<CorrectAndScore> scores = getScoreResultsForQuery(connection, statement);

      if (debug) logger.debug("getResultsForExIDIn for  " + sql + " got\n\t" + scores.size());
      return scores;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }


  /**
   * @param ids
   * @param userid
   * @param language
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoresForUser
   */
  @Override
  public List<CorrectAndScore> getResultsForExIDInForUser(Collection<Integer> ids, int userid, String session, String language) {
    try {
      String list = getInList(ids);

      String sessionClause = session != null && !session.isEmpty() ? SESSION + "='" + session + "' AND " : "";
      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          USERID + "=? AND " +
          sessionClause +

          VALID + "=true" +
          " AND " +
          EXID + " in (" + list + ")";

      return getCorrectAndScoresForUser(userid, sql);
    } catch (Exception ee) {
      logger.error("exception getting results for user " + userid + " and ids " + ids);
      logException(ee);
    }
    return new ArrayList<>();
  }

  private List<CorrectAndScore> getCorrectAndScoresForUser(long userid, String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    statement.setLong(1, userid);

    List<CorrectAndScore> scores = getScoreResultsForQuery(connection, statement);

    //  if (sql.contains("session")) {
    //    logger.debug("getCorrectAndScoresForUser for  " + sql + " got " + scores.size() + " scores");
    //  }
    return scores;
  }

  /**
   * TODOx : inefficient - better ways to do this in h2???
   * Long story here on h2 support (or lack of) for efficient in query...
   *
   * @param ids
   * @param matchAVP
   * @param userid
   * @param language
   * @return
   * @see #getSessionsForUserIn2
   * @see #attachScoreHistory
   * @see mitll.langtest.server.database.DatabaseImpl#getJsonScoreHistory
   */
  public List<CorrectAndScore> getResultsForExIDInForUser(Collection<Integer> ids, boolean matchAVP, int userid, String language) {
    try {
      String list = getInList(ids);

      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          USERID + "=? AND " +
          VALID + "=true" + " AND " +
          getAVPClause(matchAVP) +
          " AND " +
          EXID + " in (" + list + ")";

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      statement.setLong(1, userid);

      long then = System.currentTimeMillis();
      List<CorrectAndScore> scores = getScoreResultsForQuery(connection, statement);
      long now = System.currentTimeMillis();

      if (now - then > 200) {
        logger.warn("getResultsForExIDInForUser " + getLanguage() + " took " + (now - then) + " millis : " +
            " query for " + ids.size() + " and userid " + userid + " returned " + scores.size() + " scores");
      }
      if (debug) {
        logger.debug("getResultsForExIDInForUser for\n" + sql + "\ngot\n\t" + scores.size());
      }
      return scores;
    } catch (Exception ee) {
      logger.error("exception getting results for user " + userid + " and ids " + ids);
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * @param matchAVP
   * @return
   */
  private String getAVPClause(boolean matchAVP) {
    return "(" + AUDIO_TYPE + (matchAVP ? "" : " NOT ") + " LIKE " + "'avp%'" +
        " OR " + AUDIO_TYPE + (matchAVP ? "=" : "<>") + " 'flashcard' " + ")";
  }

  private String getCSSelect() {
    return "SELECT " + ID + ", " + USERID + ", " +
        EXID + ", " + Database.TIME + ", " + CORRECT + ", " + PRON_SCORE + ", " + ANSWER + ", " + SCORE_JSON;
  }

  /**
   * @param sql
   * @return
   * @throws SQLException
   * @see #getResults()
   * @see #getResultsDevices()
   */
  private List<Result> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getResultsForQuery(connection, statement);
  }

  private List<MonitorResult> getMonitorResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getMonitorResultsForQuery(connection, statement);
  }

  private List<UserAndTime> getUserAndTimeSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getUserAndTimeForQuery(connection, statement);
  }

  public int getNumResults(int projid) {
    int numResults = 0;
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "SELECT COUNT(*) FROM " + RESULTS;
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        numResults = rs.getInt(1);
      }
      finish(connection, statement, rs,sql);
    } catch (Exception ee) {
      logException(ee);
    }
    return numResults;
  }

  @Override
  public int ensureDefault(int projid, int beforeLoginUser, int unknownExerciseID) {
    return 0;
  }

  @Override
  public int getDefaultResult() {
    return 0;
  }

  @Override
  public Map<String, Integer> getStudentAnswers(int projid) {
    return null;
  }

  @Override
  public Collection<Integer> getPracticedByUser(int userid, int projid) {
    return null;
  }

  @Override
  public <T extends HasID> Map<Integer, Float> getScores(int userid, Collection<T> exercises) {
    return null;
  }

//  @Override
//  public <T extends CommonShell> Map<Integer, Float> addScores(int userid, Collection<T> exercises) {
//
//  }
//
//  @Override
//  public <T extends CommonShell> void addScoresForAll(int userid, Collection<T> exercises) {
//
//  }

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
    List<Result> results = new ArrayList<>();
    long then = System.currentTimeMillis();
    int missingScoreCol = 0;
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      int userID = rs.getInt(USERID);
      //  String plan = rs.getString(PLAN);
      String exid = rs.getString(EXID);
      int qid = rs.getInt(QID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      boolean valid = rs.getBoolean(VALID);
      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String json = null;
      try {
        json = rs.getString(SCORE_JSON);
      } catch (SQLException e) {
        if (missingScoreCol++ < 2) logger.info("Got " + e);
      }
      String device = rs.getString(DEVICE);
      String model = rs.getString(MODEL);

      AudioType realAudioType = AudioType.UNSET;
      boolean withFlash = rs.getBoolean(WITH_FLASH);

      if (type != null) {
        if (type.contains("WebRTC")) withFlash = false;
        type = normalizeAudioType(type, withFlash);
      }
      try {
        if (type != null) {
          type = type.replaceAll("=", "_");
        }
        realAudioType = type == null ? AudioType.UNSET : AudioType.valueOf(type.toUpperCase());
      } catch (IllegalArgumentException e) {
        logger.warn("getResultsForQuery unknown audio type '" + type + "' at " + uniqueID);
      }

      // NOTE : exercise id is not set - no backwards compatibility

      String validity = rs.getString(VALIDITY);
      if (validity == null) validity = "";
      Result result = new Result(uniqueID, userID, //id
          //    plan, // plan
          -1,//exid, // id
          qid, // qid
          trimPathForWebPage2(answer), // answer
          valid, // valid
          timestamp.getTime(),

          realAudioType, dur, correct, pronScore, device,
          rs.getString(DEVICE_TYPE), rs.getLong(PROCESS_DUR), rs.getLong(ROUND_TRIP_DUR), withFlash,
          rs.getFloat(SNR),
          validity, model);

      result.setJsonScore(json);
      result.setOldExID(exid);
      results.add(result);
    }
    finish(connection, statement, rs);
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 100) logger.warn("getResultsForQuery took " + diff + " to get " + results.size() + " results");
    return results;
  }

  private String normalizeAudioType(String type, boolean withFlash) {
    String practice = AudioType.PRACTICE.toString();
    if (type.isEmpty()) {
      type = AudioType.LEARN.toString();
    } else if (type.equals("avp")) {
      type = practice;
    } else if (type.equals("_by_WebRTC")) {
      type = AudioType.LEARN.toString();
//    } else if (type.equals("fastAndSlow")) {
//      type = AudioType.LEARN.toString();
    } else if (type.equals("flashcard")) {
      type = practice;
    } else if (type.equals("regular_by_WebRTC")) {
      if (withFlash) logger.error("huh? says with flash but also " + type);
      type = AudioType.REGULAR.toString();
    } else if (type.equals("slow_by_WebRTC")) {
      if (withFlash) logger.error("huh? says with flash but also " + type);
      type = AudioType.SLOW.toString();
    } else if (type.equals("practice_by_WebRTC")) {
      if (withFlash) logger.error("huh? says with flash but also " + type);
      type = practice;
    } else if (type.startsWith("avp")) {
      type = practice;
    } else {
      type = type.replaceAll("=", "_");
    }
    return type;
  }

  private List<UserAndTime> getUserAndTimeForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<UserAndTime> results = new ArrayList<>();
    while (rs.next()) {
      int userID = rs.getInt(USERID);
      String exid = rs.getString(EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);

      int exid1 = -1; // NO backwards compatibility
      UserAndTime userAndTime = new MyUserAndTime(userID, exid1, timestamp.getTime()/*, rs.getInt(QID)*/);

      results.add(userAndTime);
    }
    finish(connection, statement, rs,"");

    return results;
  }

  private List<MonitorResult> getMonitorResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<MonitorResult> results = new ArrayList<>();
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      int userID = rs.getInt(USERID);
      String exid = rs.getString(EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      boolean valid = rs.getBoolean(VALID);
      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String dtype = rs.getString(DEVICE_TYPE);
      String simpleDevice = rs.getString(DEVICE);
      String device = dtype == null ? "Unk" : dtype.equals("browser") ? simpleDevice : (dtype + "/" + simpleDevice);
      String validity = rs.getString(VALIDITY);
      float snr = rs.getFloat(SNR);

      int processDur = rs.getInt(PROCESS_DUR);
      int roundTripDur = rs.getInt(ROUND_TRIP_DUR);
      String json = rs.getString(SCORE_JSON);
      AudioType audioType;

      try {
        audioType = AudioType.valueOf(type.toUpperCase());
      } catch (IllegalArgumentException e) {
        logger.info("no audio type for " + type);
        audioType = AudioType.UNSET;
      }

      MonitorResult result = new MonitorResult(uniqueID, userID, //id
          exid,
          trimPathForWebPage2(answer), // answer
          valid, // valid
          timestamp.getTime(),
          audioType, dur, correct, pronScore, device, processDur, roundTripDur, rs.getBoolean(WITH_FLASH),
          snr, validity, dtype, simpleDevice, json, "", -1);

/*      result.setDeviceType(dtype);
      result.setSimpleDevice(simpleDevice);
      result.setScoreJSON(json);*/
      results.add(result);
    }
    finish(connection, statement, rs);

    return results;
  }

  private List<CorrectAndScore> getScoreResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    return getScoreResultsForQuery(connection, statement);
  }

  /**
   * NOTE: exercise id here is *not* set - no backward compatibility...
   *
   * @param connection
   * @param statement
   * @return
   * @throws SQLException
   * @see BaseResultDAO#getResultsForExIDInForUser(Collection, boolean, int, String)
   */
  private List<CorrectAndScore> getScoreResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<CorrectAndScore> results = new ArrayList<>();

    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      int userid = rs.getInt(USERID);
      String id = rs.getString(EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String path = rs.getString(ANSWER);
      String json = rs.getString(SCORE_JSON);

      CorrectAndScore result = new CorrectAndScore(uniqueID, userid, -1, correct, pronScore, timestamp.getTime(),
          trimPathForWebPage2(path), json);
      results.add(result);
    }
    finish(connection, statement, rs,"");

    return results;
  }

  private String trimPathForWebPage2(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }

  /**
   * @param e
   * @param expected
   * @param englishOnly
   * @return
   * @see DatabaseImpl#getNextUngradedExerciseSlow
   */
/*  public boolean areAnyResultsLeftToGradeFor(CommonExercise e, int expected, boolean englishOnly) {
    String exerciseID = e.getOldID();
    GradeDAO.GradesAndIDs resultIDsForExercise = gradeDAO.getResultIDsForExercise(exerciseID);
    return !areAllResultsGraded(exerciseID, resultIDsForExercise.grades, expected, englishOnly);
  }*/

  /**
   * Return true if all results have been graded at the grade number
   * <p/>
   * Does some fancy filtering for english --
   *
   * @param exerciseID
   * @param gradedResults
   * @param expected         if > 1 remove flq results (hack!), if = 2 assumes english-only
   * @param useEnglishGrades true if we should only look at english grades...
   * @return ungraded answers
   * @see #areAnyResultsLeftToGradeFor
   */
/*  private boolean areAllResultsGraded(String exerciseID, Collection<Grade> gradedResults, int expected, boolean useEnglishGrades) {
    List<Result> resultsForExercise = getAllResultsForExercise(exerciseID);
*//*    if (debug && !resultsForExercise.isEmpty()) {
      logger.debug("for " + exerciseID + " expected " + expected +
        " grades/item before " + resultsForExercise.size() + " results, and " + gradedResults.size() + " grades");
    }*//*
    if (resultsForExercise.isEmpty()) {
      return true;
    }

    // conditionally narrow down to only english results
    // hack!
    for (Iterator<Result> iter = resultsForExercise.iterator(); iter.hasNext(); ) {
      Result next = iter.next();
      if (useEnglishGrades && next.flq || next.userid == -1) {
        // logger.debug("removing result " + next + " since userid is " + next.userid);
        iter.remove();
      }
    }

    //if (debug && false) logger.debug("\tafter removing flq " + resultsForExercise.size());

    int countAtIndex = 0;
    for (Grade g : gradedResults) {
      if (g.gradeIndex == expected - 1 && g.grade != Grade.UNASSIGNED) {
        countAtIndex++;
      }
    }

    int numResults = resultsForExercise.size();
    boolean allGraded = numResults <= countAtIndex;

    if (debug) {
      logger.debug("areAllResultsGraded checking exercise " + exerciseID +
        " found " + countAtIndex + " grades at index at grade # " + expected +
        " given " + numResults + " results -- all graded is " + allGraded);
    }

    return allGraded;
  }*/

  /**
   * @param exerciseID
   * @return
   * @seex DatabaseImpl#getResultsForExercise(String, boolean, boolean, boolean)
   */
/*  public List<Result> getAllResultsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT * FROM results WHERE EXID='" + exerciseID + "'";
      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<Result>();
  }*/

  /**
   * No op if table exists and has the current number of columns.
   *
   * @param connection
   * @throws SQLException
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  void createResultTable(Connection connection) throws SQLException {
    createTable(connection);
    removeTimeDefault(connection);

    int numColumns = getNumColumns(connection, RESULTS);
    if (numColumns == 8) {
      addColumnToTable(connection);
    }
    if (numColumns <= 11) {
      addTypeColumnToTable(connection);
    }
    if (numColumns < 12) {
      addDurationColumnToTable(connection);
    }
    if (numColumns < 14) {
      addFlashcardColumnsToTable(connection);
    }
/*    if (numColumns < 15) {
      addStimulus(connection);
    }*/
    Collection<String> columns = getColumns(RESULTS);
    if (!columns.contains(DEVICE_TYPE.toLowerCase())) {
      addVarchar(connection, RESULTS, DEVICE_TYPE);
      addVarchar(connection, RESULTS, DEVICE);
    }

    if (!columns.contains(SCORE_JSON.toLowerCase())) {
      addVarchar(connection, RESULTS, SCORE_JSON);
    }
    if (!columns.contains(WITH_FLASH.toLowerCase())) {
      addBoolean(connection, RESULTS, WITH_FLASH);
    }
    if (!columns.contains(PROCESS_DUR.toLowerCase())) {
      addInt(connection, RESULTS, PROCESS_DUR);
    }
    if (!columns.contains(ROUND_TRIP_DUR.toLowerCase())) {
      addInt(connection, RESULTS, ROUND_TRIP_DUR);
    }

    if (!columns.contains(VALIDITY.toLowerCase())) {
      addVarchar(connection, RESULTS, VALIDITY);
    }
    if (!columns.contains(SNR.toLowerCase())) {
      addFloat(connection, RESULTS, SNR);
    }

    database.closeConnection(connection);

    createIndex(database, EXID, RESULTS);
    createIndex(database, VALID, RESULTS);
    createIndex(database, AUDIO_TYPE, RESULTS);
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
        RESULTS +
        " (" +
        ID + " IDENTITY, " +
        USERID + " INT, " +
        "plan VARCHAR, " +
        EXID + " VARCHAR, " +
        "qid INT," +
        Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
        "answer CLOB," +
        "valid BOOLEAN," +
        FLQ + " BOOLEAN," +
        SPOKEN + " BOOLEAN," +
        AUDIO_TYPE + " VARCHAR," +
        DURATION + " INT," +
        CORRECT + " BOOLEAN," +
        PRON_SCORE + " FLOAT," +
        STIMULUS + " CLOB," +
        DEVICE_TYPE + " VARCHAR," +
        DEVICE + " VARCHAR," +
        SCORE_JSON + " VARCHAR," +
        WITH_FLASH + " BOOLEAN," +
        PROCESS_DUR + " INT," +
        ROUND_TRIP_DUR + " INT, " +
        VALIDITY + " VARCHAR, " +
        SNR + " FLOAT" +
        ")");
    statement.execute();
    statement.close();
  }

  private void addColumnToTable(Connection connection) {
    try {
      addBoolean(connection, RESULTS, FLQ);
    } catch (SQLException e) {
      logger.warn("addColumnToTable : flq got " + e);
    }

    try {
      addBoolean(connection, RESULTS, SPOKEN);
    } catch (SQLException e) {
      logger.warn("addColumnToTable : spoken got " + e);
    }
  }

  private void addTypeColumnToTable(Connection connection) {
    PreparedStatement statement;

    try {
      statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          AUDIO_TYPE +
          " " +
          getVarchar());
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addTypeColumnToTable : got " + e);
    }
  }

  private void removeTimeDefault(Connection connection) throws SQLException {
    //logger.info("removing time default value - current_timestamp steps on all values with NOW.");
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ALTER COLUMN " + Database.TIME +
        " DROP DEFAULT");
    statement.execute();
    statement.close();
  }

  private void addDurationColumnToTable(Connection connection) {
    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          DURATION +
          " " +
          "INT");
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addDurationColumnToTable : got " + e);
    }
  }

  private void addFlashcardColumnsToTable(Connection connection) {
    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          CORRECT +
          " " +
          "BOOLEAN");
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addDurationColumnToTable : got " + e);
    }

    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          PRON_SCORE +
          " " +
          "FLOAT");
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addFlashcardColumnsToTable : got " + e);
    }
  }

  /**
   * Just for import
   *
   * @param isRegular
   * @param userDAO
   * @return
   */
/*  public Map<Integer, Map<String, Result>> getUserToResults(boolean isRegular, IUserDAO userDAO) {
    AudioType typeToUse = isRegular ? AudioType.REGULAR : AudioType.SLOW;
    return getUserToResults(typeToUse, userDAO);
  }

  private Map<Integer, Map<String, Result>> getUserToResults(AudioType typeToUse, IUserDAO userDAO) {
    Map<Integer, Map<String, Result>> userToResult = new HashMap<>();

    Map<Integer, User> userMap = userDAO.getUserMap();

    for (Result r : getResults()) {
      if (r.isValid() && r.getAudioType().equals(typeToUse)) {
        User user = userMap.get(r.getUserid());
        if (user != null && user.getExperience() == 240) {    // only natives!
          Map<String, Result> results1 = userToResult.get(r.getUserid());
          if (results1 == null)
            userToResult.put(r.getUserid(), results1 = new HashMap<>());
          int exerciseID = r.getExerciseID();
          Result result = results1.get(exerciseID);
          if (result == null || (r.getTimestamp() > result.getTimestamp())) {
            results1.put("" + exerciseID, r);
          }
        }
      }
    }
    return userToResult;
  }*/

}