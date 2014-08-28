package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ScoreAndPath;
import mitll.langtest.shared.User;
import mitll.langtest.shared.monitoring.Session;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link AnswerDAO}. Not sure if that's a good idea or not. :)
 */
public class ResultDAO extends DAO {
  private static final Logger logger = Logger.getLogger(ResultDAO.class);

  private static final int MINUTE = 60 * 1000;
  private static final int SESSION_GAP = 5 * MINUTE;  // 5 minutes

  public static final String ID = "id";
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

  private final boolean debug = false;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   * @param database
   */
  public ResultDAO(Database database) {
    super(database);
  }

/*  private List<SimpleResult> getSimpleResults(String whereClause) {
    try {
      String sql = "SELECT " +
        ID + ", " +
        USERID + ", " +
        Database.EXID + ", " +
        QID +
        " FROM " +
        RESULTS + whereClause;
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      List<SimpleResult> simpleResultsForQuery = getSimpleResultsForQuery(connection, statement);
//      logger.debug("for sql " + sql  + " found " + simpleResultsForQuery.size() + " results");
      return simpleResultsForQuery;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<SimpleResult>();
  }*/

  /**
   * Get a list of Results for this Query.
   *
   * @param connection
   * @param statement
   * @return
   * @throws SQLException
   */
/*
  private List<SimpleResult> getSimpleResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<SimpleResult> results = new ArrayList<SimpleResult>();
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String exid = rs.getString(Database.EXID);
      int qid = rs.getInt(QID);

      SimpleResult e = new SimpleResult(uniqueID, exid, qid, userID);
      results.add(e);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return results;
  }
*/

/*  public static class SimpleResult {
    public final int uniqueID;
    public final String id;
    private final int qid;
    public final long userid;

    public SimpleResult(int uniqueID, String id, int qid, long userid) {
      this.uniqueID = uniqueID;
      this.id = id;
      this.qid = qid;
      this.userid = userid;
    }

    public String getID() {
      return id + "/" + qid;
    }
  }*/

  private List<Result> cachedResultsForQuery = null;

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
      String sql = "SELECT * FROM " + RESULTS + ";";
      List<Result> resultsForQuery = getResultsSQL(sql);

      synchronized (this) {
        cachedResultsForQuery = resultsForQuery;
      }
      return resultsForQuery;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<Result>();
  }

  /**
   * For a set of exercise ids, find the results for each and make a map of user->results
   * Then for each user's results, make a list of sessions representing a sequence of grouped results
   * A session will have statistics - # correct, avg pronunciation score, maybe duration, etc.
   *
   * @see mitll.langtest.server.database.DatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#onSetComplete()
   * @param ids
   * @param latestResultID
   * @return
   */
  public List<Session> getSessionsForUserIn2(Collection<String> ids, long latestResultID) {
    List<Session> sessions = new ArrayList<Session>();
    Map<Long, List<Result>> userToAnswers = populateUserToAnswers(getResultsForExIDIn(ids, true));
    if (debug) logger.debug("Got " + userToAnswers.size() + " user->answer map");
    for (Map.Entry<Long,List<Result>> userToResults : userToAnswers.entrySet()) {
      List<Session> c = partitionIntoSessions2(userToResults.getValue(), ids, latestResultID);
      if (debug) logger.debug("\tfound " +c.size() + " sessions for " +userToResults.getKey() + " " +ids + " given  " + userToResults.getValue().size());

      sessions.addAll(c);
    }
    if (debug) logger.debug("found " +sessions.size() + " sessions for " +ids );

    return sessions;
  }

  public void attachScoreHistory(long userID, CommonExercise firstExercise) {
    List<Result> resultsForExercise = getResultsForExercise(firstExercise.getID());

    int total = 0;
    float scoreTotal = 0f;
    List<ScoreAndPath> scores = new ArrayList<ScoreAndPath>();
    for (Result r : resultsForExercise) {
      float pronScore = r.getPronScore();
      if (pronScore > 0) { // overkill?
        total++;
        scoreTotal += pronScore;
        if (r.userid == userID) {
          scores.add(new ScoreAndPath(pronScore, r.answer));
        }
      }
    }
    firstExercise.setScores(scores);
    firstExercise.setAvgScore(total == 0 ? 0f : scoreTotal/total);
  }

  public List<Result> getResultsForExercise(String id) {
    return getResultsForExIDIn(Collections.singleton(id), false);
  }

  /**
   * Only take avp audio type and *valid* audio.
   *
   * @see #getSessionsForUserIn2
   * @param ids
   * @param matchAVP
   * @return
   */
  private List<Result> getResultsForExIDIn(Collection<String> ids, boolean matchAVP) {
    try {
      String list = getInList(ids);

      String sql = "SELECT * FROM " + RESULTS + " where " +
          Database.EXID + " in (" + list + ")" +
          " AND " +
        VALID + "=true" +
          " AND " +
        AUDIO_TYPE + (matchAVP?"=":"<>") + "'avp'" +
          " order by " + Database.TIME + " asc";
      List<Result> resultsSQL = getResultsSQL(sql);
      if (debug) logger.debug("getResultsForExIDIn for  " +sql+ " got\n\t" + resultsSQL.size());
      return resultsSQL;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<Result>();
  }

  private List<Result> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    return getResultsForQuery(connection, statement);
  }

  public synchronized void invalidateCachedResults() {
    cachedResultsForQuery = null;
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
      logger.error("got " + ee, ee);
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
      boolean flq = rs.getBoolean(FLQ);
      boolean spoken = rs.getBoolean(SPOKEN);

      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);
      String stimulus = rs.getString(STIMULUS);

      Result result = new Result(uniqueID, userID, //id
        plan, // plan
        exid, // id
        qid, // qid
        answer, // answer
        valid, // valid
        timestamp.getTime(),
        flq, spoken, type, dur, correct, pronScore);
      result.setStimulus(stimulus);
      trimPathForWebPage(result);
      results.add(result);
    }
    finish(connection, statement, rs);

    return results;
  }

  private void trimPathForWebPage(Result r) {
    int answer = r.answer.indexOf(PathHelper.ANSWERS);
    if (answer == -1) return;
    r.answer = r.answer.substring(answer);
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
    List<Result> results = getResults();

    Map<Long, List<Result>> userToAnswers = populateUserToAnswers(results);
    List<Session> sessions = new ArrayList<Session>();

    Map<Long, List<Session>> userToSessions = new HashMap<Long, List<Session>>();
    Map<Long, Float> userToRate = new HashMap<Long, Float>();

    for (Map.Entry<Long, List<Result>> userToAnswersEntry : userToAnswers.entrySet()) {
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
                                            Map.Entry<Long, List<Result>> userToAnswersEntry) {
    Long userid = userToAnswersEntry.getKey();
    List<Result> answersForUser = userToAnswersEntry.getValue();

    return makeSessionsForUser(userToSessions, userid, answersForUser);
  }

  private List<Session> makeSessionsForUser(Map<Long, List<Session>> userToSessions,
                                            Long userid,
                                            List<Result> answersForUser) {
    sortByTime(answersForUser);

    return partitionIntoSessions(userToSessions, userid, answersForUser);
  }

  private List<Session> partitionIntoSessions(Map<Long, List<Session>> userToSessions,
                                              Long userid, List<Result> answersForUser) {
    Session s = null;
    long last = 0;

    List<Session> sessions = new ArrayList<Session>();

    for (Result r : answersForUser) {
      if (s == null || r.timestamp - last > SESSION_GAP) {
        s = new Session();
        sessions.add(s);

        List<Session> sessions1 = userToSessions.get(userid);
        if (sessions1 == null) userToSessions.put(userid, sessions1 = new ArrayList<Session>());
        sessions1.add(s);
      } else {
        s.duration += r.timestamp - last;
      }
      s.addExerciseID(r.id);
      last = r.timestamp;
    }
    return sessions;
  }

  /**
   * @see #getSessionsForUserIn2
   * @param answersForUser
   * @return
   */
  private List<Session> partitionIntoSessions2(List<Result> answersForUser, Collection<String> ids, long latestResultID) {
    Session s = null;
    long lastTimestamp = 0;

    Set<String> expected = new HashSet<String>(ids);

    List<Session> sessions = new ArrayList<Session>();

    int id = 0;
    for (Result r : answersForUser) {
      //logger.debug("got " + r);
      if (s == null || r.timestamp - lastTimestamp > SESSION_GAP || !expected.contains(r.id)) {
        sessions.add(s = new Session(id++, r.userid, r.timestamp));
        expected = new HashSet<String>(ids); // start a new set of expected items
//        logger.debug("\tpartitionIntoSessions2 expected " +expected.size());
      } else {
        s.duration += r.timestamp - lastTimestamp;
      }

      s.addExerciseID(r.id);
      s.incrementCorrect(r.id, r.isCorrect());
      s.setScore(r.id, r.getPronScore());

      if (r.uniqueID == latestResultID) {
        logger.debug("\tpartitionIntoSessions2 found current session " +s);

        s.setLatest(true);
      }

      expected.remove(r.id);
     // logger.debug("\tpartitionIntoSessions2 expected now " + expected.size() + " session " + s);

      lastTimestamp = r.timestamp;
    }
    for (Session session : sessions) session.setNumAnswers();
    if (sessions.isEmpty() && !answersForUser.isEmpty()) {
      logger.error("huh? no sessions from " + answersForUser.size() + " given " + ids);
    }
    logger.debug("\tpartitionIntoSessions2 made " +sessions.size() + " from " + answersForUser.size() + " answers");

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

  private void sortByTime(List<Result> answersForUser) {
    Collections.sort(answersForUser, new Comparator<Result>() {
      @Override
      public int compare(Result o1, Result o2) {
        return o1.timestamp < o2.timestamp ? -1 : o1.timestamp > o2.timestamp ? +1 : 0;
      }
    });
  }

  private void removeShortSessions(List<Session> sessions) {
    Iterator<Session> iter = sessions.iterator();
    while (iter.hasNext()) if (iter.next().getNumAnswers() < 2) iter.remove();
  }

  private Map<Long, List<Result>> populateUserToAnswers(List<Result> results) {
    Map<Long, List<Result>> userToAnswers = new HashMap<Long, List<Result>>();
    for (Result r : results) {
      List<Result> results1 = userToAnswers.get(r.userid);
      if (results1 == null) userToAnswers.put(r.userid, results1 = new ArrayList<Result>());
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
      //logger.info(RESULTS + " table had num columns = " + numColumns);
      addColumnToTable(connection);
    }
    if (numColumns <= 11) {//!columnExists(connection,RESULTS, AUDIO_TYPE)) {
      //logger.info(RESULTS + " table had num columns = " + numColumns);
      addTypeColumnToTable(connection);
    }
    if (numColumns < 12) {
      //logger.info(RESULTS + " table had num columns = " + numColumns);
      addDurationColumnToTable(connection);
    }
    if (numColumns < 14) {
      //logger.info(RESULTS + " table had num columns = " + numColumns);
      addFlashcardColumnsToTable(connection);
    }
    if (numColumns < 15) {
      //logger.info(RESULTS + " table had num columns = " + numColumns);
      addStimulus(connection);
    }

    database.closeConnection(connection);

    createIndex(database,Database.EXID,RESULTS);
    createIndex(database,VALID,RESULTS);
    createIndex(database,AUDIO_TYPE,RESULTS);

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
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      RESULTS +
      " (" +
      "id IDENTITY, " +
      "userid INT, " +
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
      STIMULUS + " CLOB" +
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

  public Map<Long,Map<String,Result>> getUserToResults(boolean isRegular, UserDAO userDAO) {
    String typeToUse = isRegular ? Result.AUDIO_TYPE_REGULAR : Result.AUDIO_TYPE_SLOW;
    return getUserToResults(typeToUse, userDAO);
  }

  public Map<Long, Map<String, Result>> getUserToResults(String typeToUse, UserDAO userDAO) {
    List<Result> results = getResults();
    Map<Long,Map<String,Result>> userToResult = new HashMap<Long, Map<String, Result>>();

    Map<Long, User> userMap = userDAO.getUserMap();

    for (Result r : results) {
      if (r.valid && r.audioType.equals(typeToUse)) {
        User user = userMap.get(r.userid);
        if (user != null && user.getExperience() == 240) {    // only natives!
          Map<String, Result> results1 = userToResult.get(r.userid);
          if (results1 == null)
            userToResult.put(r.userid, results1 = new HashMap<String, Result>());
          Result result = results1.get(r.id);
          if (result == null || (r.timestamp > result.timestamp)) {
            results1.put(r.id, r);
          }
        }
      }
    }
    return userToResult;
  }

  /**
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param out
   */
  public void writeExcelToStream(List<Result> results, OutputStream out) {
    SXSSFWorkbook wb = writeExcel(results);
    long then = System.currentTimeMillis();
    try {
      wb.write(out);
      long now2 = System.currentTimeMillis();
      if (now2-then > 100) {
        logger.warn("toXLSX : took " + (now2-then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }

  private SXSSFWorkbook writeExcel(List<Result> results) {
    long now;
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(10000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("Results");
    int rownum = 0;
    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss"));
    Row headerRow = sheet.createRow(rownum++);

    List<String> columns = Arrays.asList("id", "userid", Database.EXID, "qid", Database.TIME, "answer",
      "valid", "grades", FLQ, SPOKEN, AUDIO_TYPE, DURATION, CORRECT, PRON_SCORE, STIMULUS);

    for (int i = 0; i < columns.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(columns.get(i));
    }

    for (Result result : results) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(result.uniqueID);
      cell = row.createCell(j++);
      cell.setCellValue(result.userid);
      cell = row.createCell(j++);
      cell.setCellValue(result.id);
      cell = row.createCell(j++);
      cell.setCellValue(result.qid);
      cell = row.createCell(j++);
      cell.setCellValue(new Date(result.timestamp));
      cell.setCellStyle(cellStyle);

      cell = row.createCell(j++);
      cell.setCellValue(result.answer);
      cell = row.createCell(j++);
      cell.setCellValue(result.valid);
      cell = row.createCell(j++);
      cell.setCellValue(result.getGradeInfo());
      cell = row.createCell(j++);
      cell.setCellValue(result.flq);
      cell = row.createCell(j++);
      cell.setCellValue(result.spoken);
      cell = row.createCell(j++);
      cell.setCellValue(result.audioType);
      cell = row.createCell(j++);
      cell.setCellValue(result.durationInMillis);
      cell = row.createCell(j++);
      cell.setCellValue(result.isCorrect());
      cell = row.createCell(j++);
      cell.setCellValue(result.getPronScore());
      cell = row.createCell(j++);
      cell.setCellValue(result.getStimulus());
    }
    now = System.currentTimeMillis();
    if (now-then > 100) {
      logger.warn("toXLSX : took " + (now-then) + " millis to add " + rownum + " rows to sheet, or " + (now-then)/rownum + " millis/row");
    }
    return wb;
  }
}