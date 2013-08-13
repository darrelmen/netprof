package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link AnswerDAO}. Not sure if that's a good idea or not. :)
 *
 */
public class ResultDAO extends DAO {
  private static final Logger logger = Logger.getLogger(ResultDAO.class);

  private static final String ID = "id";
  private static final String USERID = "userid";
  private static final String PLAN = "plan";
  private static final String QID = "qid";
  private static final String ANSWER = "answer";
  private static final String VALID = "valid";

  private static final String RESULTS = "results";

  static final String FLQ = "flq";
  static final String SPOKEN = "spoken";
  static final String AUDIO_TYPE = "audioType";
  static final String DURATION = "duration";
  static final String CORRECT = "correct";
  static final String PRON_SCORE = "pronscore";

  private final GradeDAO gradeDAO;
  private final ScheduleDAO scheduleDAO ;
  private final boolean debug = false;

  public ResultDAO(Database database, UserDAO userDAO) {
    super(database);

    gradeDAO = new GradeDAO(database, userDAO, this);
    scheduleDAO = new ScheduleDAO(database);
  }

  public List<SimpleResult> getSimpleResults() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT " +
        ID + ", " +
        USERID + ", " +
        Database.EXID+ ", "+
        QID +
        " FROM " +
        RESULTS+";");

      return getSimpleResultsForQuery(connection, statement);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<SimpleResult>();
  }

  /**
   * Get a list of Results for this Query.
   * @param connection
   * @param statement
   * @return
   * @throws SQLException
   */
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

  public static class SimpleResult {
    public final int uniqueID;
    public final String id;
    private final int qid;
    public final long userid;


    public SimpleResult(int uniqueID, String id, int qid, long userid) { this.uniqueID = uniqueID; this.id = id; this.qid = qid; this.userid = userid;}

    public String getID() {
      return id + "/" +qid;
    }
  }

  /**
   * Pulls the list of results out of the database.
   * @see mitll.langtest.server.database.DatabaseImpl#getResults()
   * @return
   */
  public List<Result> getResults() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM " +RESULTS+";");

      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
  }

  public int getNumResults() {
    int numResults = 0;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + RESULTS + ";");
      ResultSet rs = statement.executeQuery();
      if (rs.next()) { numResults = rs.getInt(1); }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return numResults;
  }

  /**
   * Get a list of Results for this Query.
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

      String type = rs.getString(AUDIO_TYPE) ;
      int dur = rs.getInt(DURATION);

      boolean correct = rs.getBoolean(CORRECT);
      float pronScore = rs.getFloat(PRON_SCORE);

      Result e = new Result(uniqueID, userID, //id
          plan, // plan
          exid, // id
          qid, // qid
          answer, // answer
          valid, // valid
          timestamp.getTime(),
          flq, spoken, type, dur, correct, pronScore);
      trimPathForWebPage(e);
      results.add(e);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return results;
  }

  private void trimPathForWebPage(Result r) {
    int answer = r.answer.indexOf(PathHelper.ANSWERS);
    if (answer == -1) return;
    r.answer = r.answer.substring(answer);
  }

  /**
   * @see DatabaseImpl#getNextUngradedExerciseSlow
   * @param e
   * @param expected
   * @param englishOnly
   * @return
   */
  public boolean areAnyResultsLeftToGradeFor(Exercise e, int expected, boolean englishOnly) {
    String exerciseID = e.getID();
    GradeDAO.GradesAndIDs resultIDsForExercise = gradeDAO.getResultIDsForExercise(exerciseID);
    return !areAllResultsGraded(exerciseID, resultIDsForExercise.grades, expected, englishOnly);
  }

  /**
   * Return true if all results have been graded at the grade number
   *
   * Does some fancy filtering for english --
   * @see #areAnyResultsLeftToGradeFor
   * @param exerciseID
   * @param gradedResults
   * @param expected if > 1 remove flq results (hack!), if = 2 assumes english-only
   * @param useEnglishGrades true if we should only look at english grades...
   * @return ungraded answers
   */
  private boolean areAllResultsGraded(String exerciseID, Collection<Grade> gradedResults, int expected, boolean useEnglishGrades) {
    List<Result> resultsForExercise = getAllResultsForExercise(exerciseID);
    if (debug && !resultsForExercise.isEmpty()) logger.debug("for " + exerciseID + " expected " + expected +
      " grades/item before " + resultsForExercise.size() + " results, and " + gradedResults.size() + " grades");
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
  }

  /**
   * @see DatabaseImpl#getExercisesFirstNInOrder(long, int)
   * @param userid
   * @return
   */
  public String getExerciseIDLastResult(long userid) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT exid FROM results WHERE TIME IN (SELECT MAX(TIME) FROM results WHERE userid = " +
          userid +
          ");";
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();
      String exid = "INVALID";
      if (rs.next()) {
        exid = rs.getString(1);
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
      return exid;

    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return "INVALID";
  }

  /**
   * @see DatabaseImpl#getResultsForExercise(String, boolean, boolean, boolean)
   * @param exerciseID
   * @return
   */
  public List<Result> getAllResultsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT * FROM results WHERE EXID='" + exerciseID + "'";
      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
  }

  /**
   * @see DatabaseImpl#getNextUngradedExerciseQuick(java.util.Collection, int, boolean, boolean, boolean)
   * @param toExclude
   * @return
   */
  public Collection<Result> getResultExcludingExercises(Collection<String> toExclude) {
    // select results.* from results where results.exid not in ('ac-R0P-006','ac-LOP-001','ac-L0P-013')
    try {
      Connection connection = database.getConnection();

      StringBuilder b = new StringBuilder();
      for (String id : toExclude) b.append("'").append(id).append("'").append(",");
      String list = b.toString();
      list = list.substring(0,Math.max(0,list.length()-1));
      String sql = "SELECT * FROM results WHERE EXID NOT IN (" + list + ")";

      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();

  }

  /**
   * This should only be run once, on an old result table to update it.
   * @see #createResultTable(java.sql.Connection)
   */
  private void enrichResults() {
    List<Result> results = getResults();
    Map<String,List<Result>> exidToResult = new HashMap<String, List<Result>>();

    for (Result r : results) {
      List<Result> resultsForExercise = exidToResult.get(r.id);
      if (resultsForExercise == null) {
        exidToResult.put(r.id, resultsForExercise = new ArrayList<Result>());
      }
      resultsForExercise.add(r);
    }

    Map<Long, List<Schedule>> scheduleForUserAndExercise = scheduleDAO.getSchedule();
    for (String exid : exidToResult.keySet()) {
      for (Result r : exidToResult.get(exid)) {
        List<Schedule> schedules = scheduleForUserAndExercise.get(r.userid);
        if (schedules != null) {
          for (Schedule schedule : schedules) {
            if (schedule.exid.equals(exid)) {
           //   System.out.println("found schedule " + schedule + " for " + exid + " and result " + r);
              r.setFLQ(schedule.flQ);
              r.setSpoken(schedule.spoken);
              enrichResult(r);
            }
          }
        }
      }
    }
  }

  /**
   * Add values for the duration field to old collections that don't have them.
   * @param installPath
   */
/*  public void enrichResultDurations(String installPath) {
    List<Result> results = getResults();
    AudioCheck check = new AudioCheck();
    logger.debug("enrichResultDurations checking " + results.size() + " results");
    int count = 0;
    for (Result r : results) {
      if (r.durationInMillis == 0 && r.valid) {
        File file = new File(installPath +File.separator +r.answer);
        if (file.exists()) {
          double durationInSeconds = check.getDurationInSeconds(file);
          r.durationInMillis = (int)(durationInSeconds*1000d);
      //    logger.debug("dur for " + file + " is " +r.durationInMillis);
          enrichDur(r);
          count++;
        }
        else {
        //  logger.debug("couldn't find " + file.getAbsolutePath());
        }
      }
    }
    logger.debug("enriched " + count + " items of " + results.size() + " results");
  }*/

  /**
   * @see #enrichResults
   * @param toChange
   */
  private void enrichResult(Result toChange) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      String sql = "UPDATE " + RESULTS + " " +
          "SET " +
          "flq='" + toChange.flq + "', " +
          "spoken='" + toChange.spoken + "' " +
          "WHERE id=" + toChange.uniqueID;
      if (debug) System.out.println("enrichResult " + toChange);
      statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (debug) System.out.println("UPDATE " + i);
      if (i == 0) {
        System.err.println("huh? didn't update the grade for " + toChange);
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Set the duration for one entry.
   * @paramx toChange
   */
/*  private void enrichDur(Result toChange) {
    try {
      Connection connection = database.getConnection();

      String sql = "UPDATE " + RESULTS + " " +
          "SET " +
          DURATION + "=" + toChange.durationInMillis + " " +
          "WHERE id=" + toChange.uniqueID;
     // logger.info("enrichResult " + toChange);
      PreparedStatement statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (debug) System.out.println("UPDATE " + i);
      if (i == 0) {
        System.err.println("huh? didn't update the grade for " + toChange);
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/

  void dropResults(Database database) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("DROP TABLE if exists results");
      if (!statement.execute()) {
        System.err.println("couldn't create table?");
      }
      statement.close();
      database.closeConnection(connection);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * No op if table exists and has the current number of columns.
   *
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs()
   * @param connection
   * @throws SQLException
   */
  void createResultTable(Connection connection) throws SQLException {
    createTable(connection);
    removeTimeDefault(connection);

    int numColumns = getNumColumns(connection, RESULTS);
    if (numColumns == 8) {
      logger.info(RESULTS + " table had num columns = " + numColumns);
      addColumnToTable(connection);
      enrichResults();
    }
    if (numColumns <= 11) {//!columnExists(connection,RESULTS, AUDIO_TYPE)) {
      logger.info(RESULTS + " table had num columns = " + numColumns);
      addTypeColumnToTable(connection);
    }
    if (numColumns < 12) {
      logger.info(RESULTS + " table had num columns = " + numColumns);
      addDurationColumnToTable(connection);
    }
    if (numColumns < 14) {
      logger.info(RESULTS + " table had num columns = " + numColumns);
      addFlashcardColumnsToTable(connection);
    }
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
      Database.EXID +" VARCHAR, " +
      "qid INT," +
      Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
      "answer CLOB," +
        "valid BOOLEAN," +
        FLQ + " BOOLEAN," +
        SPOKEN + " BOOLEAN," +
        AUDIO_TYPE + " VARCHAR," +
      DURATION + " INT," +
      CORRECT + " BOOLEAN," +
      PRON_SCORE + " FLOAT" +
      ")");
    statement.execute();
    statement.close();
  }

  private void addColumnToTable(Connection connection) {
    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          FLQ +
          " BOOLEAN");
      statement.execute();
      statement.close();
    } catch (SQLException e) {
      logger.warn("addColumnToTable : flq got " + e);
    }

    try {
      PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD " +
          SPOKEN +
          " BOOLEAN");
      statement.execute();
      statement.close();
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
    logger.info("removing time default value - current_timestamp steps on all values with NOW.");
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ALTER COLUMN " + Database.TIME+
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
      logger.warn("addDurationColumnToTable : got " + e);
    }
  }

  /**
   * So it seems like if I alter an existing table and remove a default boolean value, h2 throws an exception
   * on subsequent inserts.
   * @param connection
   * @throws SQLException
   */
/*  private void removeValidDefault(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ALTER COLUMN " +"valid"+
        " DROP DEFAULT");
    statement.execute();
    statement.close();
  }*/

/*
  private void addValidDefault(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ALTER COLUMN " +"valid"+
        " set DEFAULT true");
    statement.execute();
    statement.close();
  }
*/
}