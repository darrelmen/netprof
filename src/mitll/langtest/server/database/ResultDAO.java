package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.langtest.shared.monitoring.Session;
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
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link AnswerDAO}. Not sure if that's a good idea or not. :)
 */
public class ResultDAO extends DAO {
  private static final Logger logger = Logger.getLogger(ResultDAO.class);

  private static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();
  private static final int MINUTE = 60 * 1000;
  private static final int SESSION_GAP = 5 * MINUTE;  // 5 minutes

  private static final String ID = "id";
  private static final String USERID = "userid";
  private static final String PLAN = "plan";
  private static final String QID = "qid";
  private static final String ANSWER = "answer";
  private static final String VALID = "valid";

  public static final String RESULTS = "results";

  static final String FLQ = "flq";
  static final String SPOKEN = "spoken";
  static final String AUDIO_TYPE = "audioType";
  static final String DURATION = "duration";
  static final String CORRECT = "correct";
  static final String PRON_SCORE = "pronscore";
  static final String STIMULUS = "stimulus";
  static final String DEVICE_TYPE = "deviceType";  // iPad, iPhone, browser, etc.
  static final String DEVICE = "device"; // device id, or browser type
  private LogAndNotify logAndNotify;

  private final boolean debug = false;

  /**
   * @param database
   * @param logAndNotify
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public ResultDAO(Database database, LogAndNotify logAndNotify) {
    super(database);
    this.logAndNotify = logAndNotify;
  }

  private List<Result> cachedResultsForQuery = null;
  private List<CorrectAndScore> cachedResultsForQuery2 = null;
  private List<MonitorResult> cachedMonitorResultsForQuery = null;

  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see DatabaseImpl#getResultsWithGrades()
   */
  public List<Result> getResults() {
    try {
      synchronized (this) {
        if (cachedResultsForQuery != null) {
          return cachedResultsForQuery;
        }
      }
      String sql = "SELECT * FROM " + RESULTS;
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

  List<CorrectAndScore> getCorrectAndScores() {
    try {
      synchronized (this) {
        if (cachedResultsForQuery2 != null) {
          return cachedResultsForQuery2;
        }
      }
      String sql = getCSSelect() + " FROM " + RESULTS;
      List<CorrectAndScore> resultsForQuery = getScoreResultsSQL(sql);

      synchronized (this) {
        cachedResultsForQuery2 = resultsForQuery;
      }
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<CorrectAndScore>();
  }

  public List<MonitorResult> getMonitorResults() {
    try {
      synchronized (this) {
        if (cachedMonitorResultsForQuery != null) {
          return cachedMonitorResultsForQuery;
        }
      }
      String sql = "SELECT * FROM " + RESULTS;
      List<MonitorResult> resultsForQuery = getMonitorResultsSQL(sql);

      synchronized (this) {
        cachedMonitorResultsForQuery = resultsForQuery;
      }
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }

    return new ArrayList<MonitorResult>();
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getMonitorResultsWithText(java.util.List)
   * @param monitorResults
   * @param join
   */
  public void addUnitAndChapterToResults(List<MonitorResult> monitorResults, Map<String, CommonExercise> join) {
    int n = 0;
    Set<String> unknownIDs = new HashSet<String>();
    for (MonitorResult result : monitorResults) {
      String id = result.getId();
      if (id.contains("\\/")) id = id.substring(0, id.length() - 2);
      CommonExercise exercise = join.get(id);
      if (exercise == null) {
        if (n < 5) {
          logger.error("addUnitAndChapterToResults : for exid " + id + " couldn't find " + result);
        }
        unknownIDs.add(id);
        n++;
        result.setUnitToValue(EMPTY_MAP);
        result.setForeignText("");
      } else {
        result.setUnitToValue(exercise.getUnitToValue());
        result.setForeignText(exercise.getForeignLanguage());
      }
    }
    if (n > 0) {
      logger.warn("addUnitAndChapterToResults : skipped " + n + " out of " + monitorResults.size() + " bad join ids = " +unknownIDs);
    }
  }

  public List<MonitorResult> getMonitorResultsByID(String id) {
    try {
      String sql = "SELECT * FROM " + RESULTS + " WHERE " + Database.EXID + "='" + id + "'";
      return getMonitorResultsSQL(sql);
    } catch (Exception ee) {
      logException(ee);
    }

    return new ArrayList<MonitorResult>();
  }


  /**
   * For a set of exercise ids, find the results for each and make a map of user->results
   * Then for each user's results, make a list of sessions representing a sequence of grouped results
   * A session will have statistics - # correct, avg pronunciation score, maybe duration, etc.
   *
   * @param ids only those items that were actually practiced (or scored)
   * @param allIds all the item ids in the chapter or set of chapters that were covered
   * @param userid who did them
   * @param latestResultID
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  public SessionsAndScores getSessionsForUserIn2(Collection<String> ids, long latestResultID, long userid,
                                                 Collection<String> allIds, Map<String, String> idToFL) {
    List<Session> sessions = new ArrayList<Session>();
    Map<Long, List<CorrectAndScore>> userToAnswers = populateUserToAnswers(getResultsForExIDIn(ids, true));
    if (debug) logger.debug("Got " + userToAnswers.size() + " user->answer map");
    for (Map.Entry<Long, List<CorrectAndScore>> userToResults : userToAnswers.entrySet()) {
      List<Session> c = partitionIntoSessions2(userToResults.getValue(), ids, latestResultID);
      if (debug)
        logger.debug("\tfound " + c.size() + " sessions for " + userToResults.getKey() + " " + ids + " given  " + userToResults.getValue().size());

      sessions.addAll(c);
    }

    List<CorrectAndScore> results = getResultsForExIDInForUser(allIds, true, userid);
    if (debug) logger.debug("found " + results.size() + " results for " + allIds.size() + " items");

    List<ExerciseCorrectAndScore> sortedResults = getSortedAVPHistory(results, allIds, idToFL);
    if (debug) logger.debug("found " + sessions.size() + " sessions for " + ids);

    return new SessionsAndScores(sessions, sortedResults);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds(int, java.util.Map, String, long, int, String, boolean, boolean, boolean)
   * @param exercises
   * @param userid
   * @return
   */
  public List<CommonExercise> getExercisesSortedIncorrectFirst(Collection<CommonExercise> exercises, long userid) {
    List<String> allIds = new ArrayList<String>();
    Map<String, CommonExercise> idToEx = new HashMap<String, CommonExercise>();
    Map<String,String> idToFL = new HashMap<String, String>();
    for (CommonExercise exercise : exercises) {
      String id = exercise.getID();
      allIds.add(id);
      idToEx.put(id, exercise);
      idToFL.put(id,exercise.getForeignLanguage());
    }

    List<ExerciseCorrectAndScore> sortedResults = getExerciseCorrectAndScores(userid, allIds, idToFL);

    List<CommonExercise> commonExercises = new ArrayList<CommonExercise>(exercises.size());
    for (ExerciseCorrectAndScore score : sortedResults) {
      commonExercises.add(idToEx.get(score.getId()));
    }
    return commonExercises;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getJsonScoreHistory(long, java.util.Map)
   * @param userid
   * @param allIds
   * @return
   */
  public List<ExerciseCorrectAndScore> getExerciseCorrectAndScores(long userid, List<String> allIds, Map<String,String> idToFL) {
    List<CorrectAndScore> results = getResultsForExIDInForUser(allIds, true, userid);
    if (debug) logger.debug("found " + results.size() + " results for " + allIds.size() + " items");
    return getSortedAVPHistory(results, allIds, idToFL);
  }

  /**
   * @param results
   * @param allIds
   * @return
   * @see #getSessionsForUserIn2(java.util.Collection, long, long, java.util.Collection)
   */
/*  private List<ExerciseCorrectAndScore> getSortedAVPHistoryOld(List<CorrectAndScore> results, Collection<String> allIds) {
    List<ExerciseCorrectAndScore> sortedResults = getExerciseCorrectAndScores(results, allIds);
    Collections.sort(sortedResults);
    return sortedResults;
  }*/

  private List<ExerciseCorrectAndScore> getSortedAVPHistory(List<CorrectAndScore> results, Collection<String> allIds,
                                                            final Map<String, String> idToFL) {
    List<ExerciseCorrectAndScore> sortedResults = getExerciseCorrectAndScores(results, allIds);
    Collections.sort(sortedResults, new Comparator<ExerciseCorrectAndScore>() {
      @Override
      public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
        String fl = idToFL.get(o1.getId());
        String otherFL = idToFL.get(o2.getId());
      //  if (fl == null) fl = "";
      //  if (otherFL == null) otherFL = "";
        return o1.compareTo(o2, fl, otherFL);
      }
    });
    return sortedResults;
  }


  private List<ExerciseCorrectAndScore> getExerciseCorrectAndScores(List<CorrectAndScore> results, Collection<String> allIds) {
    SortedMap<String, ExerciseCorrectAndScore> idToScores = new TreeMap<String, ExerciseCorrectAndScore>();
    if (results != null) {
      for (CorrectAndScore r : results) {
        String id = r.getId();
        ExerciseCorrectAndScore correctAndScores = idToScores.get(id);
        if (correctAndScores == null) idToScores.put(id, correctAndScores = new ExerciseCorrectAndScore(id));
        //CorrectAndScore correctAndScore = new CorrectAndScore(r);
        //logger.debug("added " + correctAndScore + " for "+ id + " from " + r);
        correctAndScores.add(r);
      }
    }
    for (ExerciseCorrectAndScore exerciseCorrectAndScore : idToScores.values()) {
      exerciseCorrectAndScore.sort();
    }

    for (String id : allIds) {
      if (!idToScores.containsKey(id)) idToScores.put(id, new ExerciseCorrectAndScore(id));
    }
    return new ArrayList<ExerciseCorrectAndScore>(idToScores.values());
  }

  public static class SessionsAndScores {
    List<Session> sessions;
    List<ExerciseCorrectAndScore> sortedResults;

    public SessionsAndScores(List<Session> sessions, List<ExerciseCorrectAndScore> sortedResults) {
      this.sessions = sessions;
      this.sortedResults = sortedResults;
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#attachScoreHistory(long, mitll.langtest.shared.CommonExercise, boolean)
   * @param userID
   * @param firstExercise
   * @param isFlashcardRequest
   */
  public void attachScoreHistory(long userID, CommonExercise firstExercise, boolean isFlashcardRequest) {
    List<CorrectAndScore> resultsForExercise = getCorrectAndScores(userID, firstExercise, isFlashcardRequest);

    //logger.debug("score history " + resultsForExercise);
    int total = 0;
    float scoreTotal = 0f;
    for (CorrectAndScore r : resultsForExercise) {
      float pronScore = r.getScore();
      if (pronScore > 0) { // overkill?
        total++;
        scoreTotal += pronScore;
      }
    }
    firstExercise.setScores(resultsForExercise);
    firstExercise.setAvgScore(total == 0 ? 0f : scoreTotal / total);
  }

  public List<CorrectAndScore> getCorrectAndScores(long userID, CommonExercise firstExercise, boolean isFlashcardRequest) {
    String id = firstExercise.getID();
    return getResultsForExIDInForUser(userID, isFlashcardRequest, id);
  }

  public JSONObject getHistoryAsJson(long userID, String id) {
    JSONObject jsonObject = new JSONObject();

    List<CorrectAndScore> resultsForExercise = getResultsForExIDInForUser(userID, true, id);
  //  List<CorrectAndScore> toUse = getCorrectAndScores();
    if (resultsForExercise.size() > 5) resultsForExercise = resultsForExercise.subList(resultsForExercise.size() - 5, resultsForExercise.size());

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
   //   JSONObject value = new JSONObject();
   //   value.put("c",r.isCorrect() ? "Yes":"No");
      jsonArray.add(r.isCorrect() ? "Yes" : "No");
    }

    jsonObject.put("score",total == 0 ? 0f : scoreTotal / total);
    jsonObject.put("history",jsonArray);
    return jsonObject;
  }

  private List<CorrectAndScore> getResultsForExIDInForUser(long userID, boolean isFlashcardRequest, String id) {
    return getResultsForExIDInForUser(Collections.singleton(id), isFlashcardRequest, userID);
  }

  /**
   * Only take avp audio type and *valid* audio.
   *
   * @param ids
   * @param matchAVP
   * @return
   * @see #getSessionsForUserIn2
   */

  private List<CorrectAndScore> getResultsForExIDIn(Collection<String> ids, boolean matchAVP) {
    try {
      String list = getInList(ids);

      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          VALID + "=true" + " AND " +
          //AUDIO_TYPE + (matchAVP?"=":"<>") + "'avp'" +" AND " +
          getAVPClause(matchAVP) + " AND " +

          Database.EXID + " in (" + list + ")" +
          " order by " + Database.TIME + " asc";

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);

      List<CorrectAndScore> scores = getScoreResultsForQuery(connection, statement);

      if (debug) logger.debug("getResultsForExIDIn for  " + sql + " got\n\t" + scores.size());
      return scores;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<CorrectAndScore>();
  }

  private void logException(Exception ee) {
    logger.error("got " + ee, ee);
    logAndNotify.logAndNotifyServerException(ee);
  }


  /**
   * TODOx : inefficient - better ways to do this in h2???
   * Long story here on h2 support (or lack of) for efficient in query...
   *
   * @param ids
   * @param matchAVP
   * @param userid
   * @return
   * @see #getSessionsForUserIn2
   * @see #attachScoreHistory(long, mitll.langtest.shared.CommonExercise, boolean)
   */
  private List<CorrectAndScore> getResultsForExIDInForUser(Collection<String> ids, boolean matchAVP, long userid) {
    try {
      String list = getInList(ids);

      String sql = getCSSelect() + " FROM " + RESULTS + " WHERE " +
          USERID + "=? AND " +
          VALID + "=true" + " AND " +
          getAVPClause(matchAVP) +
          " AND " +
          Database.EXID + " in (" + list + ")";

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      statement.setLong(1, userid);

      List<CorrectAndScore> scores = getScoreResultsForQuery(connection, statement);

      if (debug) {
        logger.debug("getResultsForExIDInForUser for  " + sql + " got\n\t" + scores.size());
      }
      return scores;
    } catch (Exception ee) {
      logger.error("exception getting results for user " + userid + " and ids " + ids);
      logException(ee);
    }
    return new ArrayList<CorrectAndScore>();
  }

  private String getAVPClause(boolean matchAVP) {
    return AUDIO_TYPE + (matchAVP ? "" : " NOT ") + " LIKE " + "'avp%'";
  }

  private String getCSSelect() {
    return "SELECT " + ID + ", " + USERID + ", " +
        Database.EXID + ", " + Database.TIME + ", " + CORRECT + ", " + PRON_SCORE + ", " + ANSWER + " ";
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

  private List<MonitorResult> getMonitorResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getMonitorResultsForQuery(connection, statement);
  }

  /**
   * @see mitll.langtest.server.database.AnswerDAO#addAnswerToTable
   */
  public synchronized void invalidateCachedResults() {
    cachedResultsForQuery = null;
    cachedResultsForQuery2 = null;
    cachedMonitorResultsForQuery = null;
  }

  public int getNumResults() {
    int numResults = 0;
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + RESULTS + ";");
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
      String plan = rs.getString(PLAN);
      String exid = rs.getString(Database.EXID);
      int qid = rs.getInt(QID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      boolean valid = rs.getBoolean(VALID);
      // boolean flq = rs.getBoolean(FLQ);
      //  boolean spoken = rs.getBoolean(SPOKEN);

      String type = rs.getString(AUDIO_TYPE);
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

  private List<MonitorResult> getMonitorResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<MonitorResult> results = new ArrayList<MonitorResult>();
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String exid = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String answer = rs.getString(ANSWER);
      boolean valid = rs.getBoolean(VALID);
      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String dtype = rs.getString(DEVICE_TYPE);
      String device = dtype == null ? "Unk" : dtype.equals("browser") ? rs.getString(DEVICE) : (dtype + "/" + rs.getString(DEVICE));

      MonitorResult result = new MonitorResult(uniqueID, userID, //id
          // plan
          exid, // id
          // qid
          trimPathForWebPage2(answer), // answer
          valid, // valid
          timestamp.getTime(),
          type, dur, correct, pronScore, device);
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
   * @param connection
   * @param statement
   * @return
   * @throws SQLException
   * @see #getResultsForExIDInForUser(java.util.Collection, boolean, long)
   */
  private List<CorrectAndScore> getScoreResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<CorrectAndScore> results = new ArrayList<CorrectAndScore>();

    //logger.debug("getScoreResultsForQuery");

    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      //logger.debug("Got " + uniqueID);
      long userid = rs.getInt(USERID);
      String id = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String path = rs.getString(ANSWER);

      CorrectAndScore result = new CorrectAndScore(uniqueID, userid, id, correct, pronScore, timestamp.getTime(), trimPathForWebPage2(path));
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
   * @param e
   * @param expected
   * @param englishOnly
   * @return
   * @see DatabaseImpl#getNextUngradedExerciseSlow
   */
/*  public boolean areAnyResultsLeftToGradeFor(CommonExercise e, int expected, boolean englishOnly) {
    String exerciseID = e.getID();
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
   * @param toExclude
   * @return
   * @seex DatabaseImpl#getNextUngradedExerciseQuick(java.util.Collection, int, boolean, boolean, boolean)
   */
/*  public Collection<Result> getResultExcludingExercises(Collection<String> toExclude) {
    // select results.* from results where results.exid not in ('ac-R0P-006','ac-LOP-001','ac-L0P-013')
    try {
      Connection connection = database.getConnection();

      String list = getInList(toExclude);
      String sql = "SELECT * FROM results WHERE EXID NOT IN (" + list + ")";

      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<Result>();

  }*/
  private String getInList(Collection<String> toExclude) {
    StringBuilder b = new StringBuilder();
    for (String id : toExclude) b.append("'").append(id).append("'").append(",");
    String list = b.toString();
    list = list.substring(0, Math.max(0, list.length() - 1));
    return list;
  }

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link #SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   * <p/>
   * Multiple answers to the same exercise count as one answer.
   *
   * @return list of duration and numAnswer pairs
   */
  public SessionInfo getSessions() {
    // List<Result> results = getResults();

    Map<Long, List<CorrectAndScore>> userToAnswers = populateUserToAnswers(getCorrectAndScores());
    List<Session> sessions = new ArrayList<Session>();

    Map<Long, List<Session>> userToSessions = new HashMap<Long, List<Session>>();
    Map<Long, Float> userToRate = new HashMap<Long, Float>();

    for (Map.Entry<Long, List<CorrectAndScore>> userToAnswersEntry : userToAnswers.entrySet()) {
      sessions.addAll(makeSessionsForUser(userToSessions, userToAnswersEntry));
    }
    for (Session session : sessions) session.setNumAnswers();
    removeShortSessions(sessions);

    for (Map.Entry<Long, List<Session>> sessionPair : userToSessions.entrySet()) {
      removeShortSessions(sessionPair.getValue());
      long dur = 0;
      int num = 0;

      for (Session s : sessionPair.getValue()) {
        //logger.debug("user " +sessionPair.getKey() + " " + s);
        dur += s.duration;
        num += s.getNumAnswers();
      }

      if (num > 0) {
        float rate = (float) (dur / 1000) / (float) num;
        //logger.debug("user " +sessionPair.getKey() + " dur " + dur/1000 + " num " + num+ " rate " +rate);
        userToRate.put(sessionPair.getKey(), rate);
      }
    }

    return new SessionInfo(sessions, userToRate);
  }

  private List<Session> makeSessionsForUser(Map<Long, List<Session>> userToSessions,
                                            Map.Entry<Long, List<CorrectAndScore>> userToAnswersEntry) {
    Long userid = userToAnswersEntry.getKey();
    List<CorrectAndScore> answersForUser = userToAnswersEntry.getValue();

    return makeSessionsForUser(userToSessions, userid, answersForUser);
  }

  private List<Session> makeSessionsForUser(Map<Long, List<Session>> userToSessions,
                                            Long userid,
                                            List<CorrectAndScore> answersForUser) {
    sortByTime(answersForUser);

    return partitionIntoSessions(userToSessions, userid, answersForUser);
  }

  private List<Session> partitionIntoSessions(Map<Long, List<Session>> userToSessions,
                                              Long userid, List<CorrectAndScore> answersForUser) {
    Session s = null;
    long last = 0;

    List<Session> sessions = new ArrayList<Session>();

    for (CorrectAndScore r : answersForUser) {
      long timestamp = r.getTimestamp();
      if (s == null || timestamp - last > SESSION_GAP) {
        s = new Session();
        sessions.add(s);

        List<Session> sessions1 = userToSessions.get(userid);
        if (sessions1 == null) userToSessions.put(userid, sessions1 = new ArrayList<Session>());
        sessions1.add(s);
      } else {
        s.duration += timestamp - last;
      }
      s.addExerciseID(r.getId());
      last = timestamp;
    }
    return sessions;
  }

  /**
   * @param answersForUser
   * @return
   * @see #getSessionsForUserIn2
   */
  private List<Session> partitionIntoSessions2(List<CorrectAndScore> answersForUser, Collection<String> ids, long latestResultID) {
    Session s = null;
    long lastTimestamp = 0;

    Set<String> expected = new HashSet<String>(ids);

    List<Session> sessions = new ArrayList<Session>();

    int id = 0;
    for (CorrectAndScore r : answersForUser) {
      //logger.debug("got " + r);
      String id1 = r.getId();
      long timestamp = r.getTimestamp();
      if (s == null || timestamp - lastTimestamp > SESSION_GAP || !expected.contains(id1)) {
        sessions.add(s = new Session(id++, r.getUserid(), timestamp));
        expected = new HashSet<String>(ids); // start a new set of expected items
//        logger.debug("\tpartitionIntoSessions2 expected " +expected.size());
      } else {
        s.duration += timestamp - lastTimestamp;
      }

      s.addExerciseID(id1);
      s.incrementCorrect(id1, r.isCorrect());
      s.setScore(id1, r.getScore());

      if (r.getUniqueID() == latestResultID) {
        logger.debug("\tpartitionIntoSessions2 found current session " + s);

        s.setLatest(true);
      }

      expected.remove(id1);
      // logger.debug("\tpartitionIntoSessions2 expected now " + expected.size() + " session " + s);

      lastTimestamp = timestamp;
    }
    for (Session session : sessions) session.setNumAnswers();
    if (sessions.isEmpty() && !answersForUser.isEmpty()) {
      logger.error("huh? no sessions from " + answersForUser.size() + " given " + ids);
    }
//    logger.debug("\tpartitionIntoSessions2 made " +sessions.size() + " from " + answersForUser.size() + " answers");

    return sessions;
  }

  public static class SessionInfo {
    public final List<Session> sessions;
    public final Map<Long, Float> userToRate;

    public SessionInfo(List<Session> sessions, Map<Long, Float> userToRate) {
      this.sessions = sessions;
      this.userToRate = userToRate;
    }
  }

  private void sortByTime(List<CorrectAndScore> answersForUser) {
    Collections.sort(answersForUser);
  }

  private void removeShortSessions(List<Session> sessions) {
    Iterator<Session> iter = sessions.iterator();
    while (iter.hasNext()) if (iter.next().getNumAnswers() < 2) iter.remove();
  }

  private Map<Long, List<CorrectAndScore>> populateUserToAnswers(List<CorrectAndScore> results) {
    Map<Long, List<CorrectAndScore>> userToAnswers = new HashMap<Long, List<CorrectAndScore>>();
    for (CorrectAndScore r : results) {
      long userid = r.getUserid();
      List<CorrectAndScore> results1 = userToAnswers.get(userid);
      if (results1 == null) userToAnswers.put(userid, results1 = new ArrayList<CorrectAndScore>());
      results1.add(r);
    }
    return userToAnswers;
  }

  //void dropResults() { drop(RESULTS);  }

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
    if (numColumns <= 11) {//!columnExists(connection,RESULTS, AUDIO_TYPE)) {
      addTypeColumnToTable(connection);
    }
    if (numColumns < 12) {
      addDurationColumnToTable(connection);
    }
    if (numColumns < 14) {
      addFlashcardColumnsToTable(connection);
    }
    if (numColumns < 15) {
      addStimulus(connection);
    }
    if (!getColumns(RESULTS).contains(DEVICE_TYPE.toLowerCase())) {
      addVarchar(connection, RESULTS, DEVICE_TYPE);
      addVarchar(connection, RESULTS, DEVICE);
    }

    database.closeConnection(connection);

    createIndex(database, Database.EXID, RESULTS);
    createIndex(database, VALID, RESULTS);
    createIndex(database, AUDIO_TYPE, RESULTS);

    // enrichResults();
    //removeValidDefault(connection);
    // addValidDefault(connection);
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
        ID +
        " IDENTITY, " +
        USERID +
        " INT, " +
        "plan VARCHAR, " +
        Database.EXID + " VARCHAR, " +
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
        DEVICE + " VARCHAR" +
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
          "VARCHAR");
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

  private void addStimulus(Connection connection) {
    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          STIMULUS +
          " " +
          "CLOB");
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addStimulus : got " + e);
    }
  }

  public Map<Long, Map<String, Result>> getUserToResults(boolean isRegular, UserDAO userDAO) {
    String typeToUse = isRegular ? Result.AUDIO_TYPE_REGULAR : Result.AUDIO_TYPE_SLOW;
    return getUserToResults(typeToUse, userDAO);
  }

  Map<Long, Map<String, Result>> getUserToResults(String typeToUse, UserDAO userDAO) {
    List<Result> results = getResults();
    Map<Long, Map<String, Result>> userToResult = new HashMap<Long, Map<String, Result>>();

    Map<Long, User> userMap = userDAO.getUserMap();

    for (Result r : results) {
      if (r.isValid() && r.getAudioType().equals(typeToUse)) {
        User user = userMap.get(r.getUserid());
        if (user != null && user.getExperience() == 240) {    // only natives!
          Map<String, Result> results1 = userToResult.get(r.getUserid());
          if (results1 == null)
            userToResult.put(r.getUserid(), results1 = new HashMap<String, Result>());
          String exerciseID = r.getExerciseID();
          Result result = results1.get(exerciseID);
          if (result == null || (r.getTimestamp() > result.getTimestamp())) {
            results1.put(exerciseID, r);
          }
        }
      }
    }
    return userToResult;
  }

  /**
   *
   * @param typeOrder
   * @param out
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeExcelToStream(List<MonitorResult> results, List<String> typeOrder, OutputStream out) {
    SXSSFWorkbook wb = writeExcel(results, typeOrder);
    writeToStream(out, wb);
  }

  private SXSSFWorkbook writeExcel(List<MonitorResult> results,  List<String> typeOrder
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


    for (final String type : typeOrder) {
      columns.add(type);
    }

    List<String> columns2 = Arrays.asList(
        "Recording",
        Database.TIME,
        AUDIO_TYPE,
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
      cell.setCellValue(result.isValid() ? "Yes" : "No");

      cell = row.createCell(j++);
      cell.setCellValue(result.isCorrect() ? "Yes" : "No");

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

  public void writeExcelToStreamOld(List<Result> results, OutputStream out) {
    SXSSFWorkbook wb = writeExcelOld(results);
    writeToStream(out, wb);
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

  private SXSSFWorkbook writeExcelOld(List<Result> results) {
    long now;
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(10000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("Results");
    int rownum = 0;
    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss"));
    Row headerRow = sheet.createRow(rownum++);

    List<String> columns = Arrays.asList(ID, USERID, Database.EXID,
        //"qid",
        Database.TIME,
        "answer",
        "valid",
        //"grades", FLQ, SPOKEN,
        AUDIO_TYPE, DURATION, CORRECT, PRON_SCORE//, STIMULUS
    );

    for (int i = 0; i < columns.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(columns.get(i));
    }

    for (Result result : results) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(result.getUniqueID());
      cell = row.createCell(j++);
      cell.setCellValue(result.getUserid());
      cell = row.createCell(j++);
      cell.setCellValue(result.getExerciseID());
      cell = row.createCell(j++);
      cell.setCellValue(new Date(result.getTimestamp()));
      cell.setCellStyle(cellStyle);

      cell = row.createCell(j++);
      cell.setCellValue(result.getAnswer());
      cell = row.createCell(j++);
      cell.setCellValue(result.isValid());
      cell = row.createCell(j++);
      cell.setCellValue(result.getAudioType());
      cell = row.createCell(j++);
      cell.setCellValue(result.getDurationInMillis());
      cell = row.createCell(j++);
      cell.setCellValue(result.isCorrect());
      cell = row.createCell(j++);
      cell.setCellValue(result.getPronScore());
    }
    now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.warn("toXLSX : took " + (now - then) + " millis to add " + rownum + " rows to sheet, or " + (now - then) / rownum + " millis/row");
    }
    return wb;
  }
}