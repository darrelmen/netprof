package mitll.langtest.server.database;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class ResultDAO extends DAO {
  private static final String RESULTS = "results";
  //private final Database database;
  private GradeDAO gradeDAO;
  private ScheduleDAO scheduleDAO ;
  boolean debug = false;
  public ResultDAO(Database database) {
    super(database);

    gradeDAO = new GradeDAO(database);
    scheduleDAO = new ScheduleDAO(database);

  }

  /**
   * Pulls the list of results out of the database.
   * @see mitll.langtest.server.database.DatabaseImpl#getResults()
   * @return
   */
  public List<Result> getResults() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * from results;");

      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
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
      int i = 1;
      int uniqueID = rs.getInt(i++);
      long userID = rs.getLong(i++);
      String plan = rs.getString(i++);
      String exid = rs.getString(i++);
      int qid = rs.getInt(i++);
      Timestamp timestamp = rs.getTimestamp(i++);
      String answer = rs.getString(i++);
      boolean valid = rs.getBoolean(i++);
      boolean flq = rs.getBoolean(i++);
      boolean spoken = rs.getBoolean(i++);
      Result e = new Result(uniqueID, userID, //id
          plan, // plan
          exid, // id
          qid, // qid
          answer, // answer
          //rs.getString(i++), // audioFile
          valid, // valid
          timestamp.getTime(),
          flq, spoken);
      trimPathForWebPage(e);
      results.add(e);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return results;
  }

  private void trimPathForWebPage(Result r) {
    int answer = r.answer.indexOf(LangTestDatabaseImpl.ANSWERS);
    if (answer == -1) return;
    r.answer = r.answer.substring(answer);
  }

  /**
   * @see DatabaseImpl#getNextUngradedExerciseSlow
   * @param e
   * @param expected
   * @return
   */
  public boolean areAnyResultsLeftToGradeFor(Exercise e, int expected) {
    return !getResultsForExercise(e.getID(),expected).isEmpty();
  }

  /**
   * Joins against grades -- don't return graded exercises
   *
   * @see DatabaseImpl#getResultsForExercise(String)
   * @param exerciseID
   * @return results that haven't been graded yet
   */
  private List<Result> getResultsForExercise(String exerciseID, int expected) {
    GradeDAO.GradesAndIDs resultIDsForExercise = gradeDAO.getResultIDsForExercise(exerciseID);
    return getResultsForExercise(exerciseID, resultIDsForExercise.grades, expected);
  }

  /**
   * Return all answers that don't have the required number of grades (expected).<br></br>
   * I.e. those that require some additional grading
   * Does some fancy filtering for english --
   * TODO : Add proper filtering
   * @see #getResultsForExercise(String, int)
   * @param exerciseID
   * @param gradedResults
   * @param expected if > 1 remove flq results (hack!), if = 2 assumes english-only
   * @return
   */
  private List<Result> getResultsForExercise(String exerciseID, Collection<Grade> gradedResults, int expected) {
    try {
      List<Result> resultsForQuery = getAllResultsForExercise(exerciseID);
      //enrichResults(resultsForQuery,exerciseID);
      if (debug) System.out.println("for " + exerciseID + " expected " + expected +
          " before " + resultsForQuery.size() + " results, and " + gradedResults.size() + " grades");

      boolean useEnglishGrades = expected == 2;

      // conditionally narrow down to only english results
      // hack!
      for (Iterator<Result> iter = resultsForQuery.iterator(); iter.hasNext(); ) {
        Result next = iter.next();
        if (useEnglishGrades && next.flq || next.userid == -1) {
          iter.remove();
        }
      }

      if (debug) System.out.println("\tafter removing flq " + resultsForQuery.size());

      // count the number of grades for each result
      Map<Integer,Integer> idToCount = new HashMap<Integer, Integer>();
      Set<Integer> englishResultsWithGrades = new HashSet<Integer>();

      for (Grade g : gradedResults) {
        Integer countForResult = idToCount.get(g.resultID);
        if (g.grade == Grade.UNASSIGNED) {
          if (debug) System.out.println("\tgetResultsForExercise : skipping grade " + g); // TODO make sure it skips only ungraded items and that we see ungraded items when we look for the next ungraded exercise
        }
        else {
          if (debug) System.out.println("\tgetResultsForExercise : including grade " + g);

          if (countForResult == null) idToCount.put(g.resultID, 1);
          else {
            idToCount.put(g.resultID, countForResult + 1);
          }
          if (useEnglishGrades && g.gradeType.equals("english-only")) {
            englishResultsWithGrades.add(g.resultID);
          }
        }
      }
      if (debug) System.out.println("\t map of result->count for result "+ idToCount.size());

      // now go back through the list of results and remove all those that have the number of grades we require
      // for this grading -- i.e. for english only grading we expect to have two...
      for (Iterator<Result> iter = resultsForQuery.iterator(); iter.hasNext();) {
        Result next = iter.next();
        Integer count = idToCount.get(next.uniqueID);
        if (count != null && count >= expected || (useEnglishGrades && englishResultsWithGrades.contains(next.uniqueID))) {
          if (debug) {
            if (count != null && count >= expected)
              System.out.println("\tremoving graded item for result " + next + " since count = " + count + " vs " + expected);
            else
              System.out.println("\tremoving graded item for result " + next + " since is english grade");
          }
          iter.remove();
        }
        else {
          //System.out.println("NOT removing graded item for result " + next + " count = " + count);
        }
      }
      if (debug) System.out.println("\tafter removing graded items count = " + resultsForQuery.size());

      return resultsForQuery;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
  }

  public String getExerciseIDLastResult(long userid) {
    try {
      Connection connection = database.getConnection();
      String sql = "select exid from results where time in (select max(time) from results where userid = " +
          userid +
          ");";
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();
      //List<Result> results = new ArrayList<Result>();
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

  public List<Result> getAllResultsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT * from results where EXID='" + exerciseID + "'";
      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
  }

  public Collection<Result> getResultExcludingExercises(Collection<String> toExclude) {
    // select results.* from results where results.exid not in ('ac-R0P-006','ac-LOP-001','ac-L0P-013')
    try {
      Connection connection = database.getConnection();

      StringBuilder b = new StringBuilder();
      for (String id : toExclude) b.append("'").append(id).append("'").append(",");
      String list = b.toString();
      list = list.substring(0,Math.max(0,list.length()-1));
      String sql = "SELECT * from results where EXID not in (" + list + ")";

      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();

  }

  public Set<Long> getUsers(List<Result> resultsForExercise) {
    Set<Long> users = new HashSet<Long>();

    for (Result r : resultsForExercise) {
      users.add(r.userid);
    }
    return users;
  }

  /**
   * Currently the results aren't marked with the spoken/written, foreign/english flags -- have to recover
   * them from the schedule.
   *
   * @paramx resultsForExercise
   * @paramx exid
   */
/*
  public void enrichResults(List<Result> resultsForExercise, String exid) {
    Set<Long> users = getUsers(resultsForExercise);

    Map<Long, List<Schedule>> scheduleForUserAndExercise = scheduleDAO.getScheduleForUserAndExercise(users, exid);
    for (Result r : resultsForExercise) {
      List<Schedule> schedules = scheduleForUserAndExercise.get(r.userid);
      if (schedules != null) {
        Schedule schedule = schedules.get(0);
        r.setFLQ(schedule.flQ);
        r.setSpoken(schedule.spoken);
      }
    }
  }
*/

  /**
   * This should only be run once, on an old result table to update it.
   */
  public void enrichResults() {
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
        //  Schedule schedule = schedules.get(0);

        }
      }
    }
  }

  public void enrichResult(Result toChange) {
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
    //return new CountAndGradeID(getCount(), id);
  }

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
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  }
    }
  }

  /**
   * @deprecated
   * @paramx database
   * @throws Exception
   */
/*  private void showResults(Database database) throws Exception {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement("SELECT * FROM results order by " + Database.TIME);
    ResultSet rs = statement.executeQuery();
    int c = 0;
    while (rs.next()) {
      c++;
      int i = 1;
      if (false) {
        System.out.println(rs.getInt(i++) + "," + rs.getString(i++) + "," +
          rs.getString(i++) + "," +
          rs.getInt(i++) + "," +
          rs.getString(i++) + "," +
          rs.getString(i++) + "," +
          rs.getTimestamp(i++));
      }
    }
 //   System.out.println("now " + c + " answers");
    rs.close();
    statement.close();
    database.closeConnection(connection);
  }*/

  /**
   * No op if table exists and has the current number of columns.
   *
   * @see DatabaseImpl#DatabaseImpl(javax.servlet.http.HttpServlet)
   * @param connection
   * @throws SQLException
   */
  void createResultTable(Connection connection) throws SQLException {
    createTable(connection);
    int numColumns = getNumColumns(connection, RESULTS);
    if (numColumns == 8) {
      addColumnToTable(connection);
      enrichResults();
    }
  }

  private void createTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
        RESULTS +
        " (id IDENTITY, userid INT, plan VARCHAR, " +
      Database.EXID +" VARCHAR, " +
      "qid INT," +
      Database.TIME + " TIMESTAMP AS CURRENT_TIMESTAMP," +
      "answer CLOB," +
      //"audioFile VARCHAR, " +
      "valid BOOLEAN" +
      ")");
    statement.execute();
    statement.close();
  }

  private void addColumnToTable(Connection connection) throws SQLException {
 /*   PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD scheduled BOOLEAN NOT NULL default false");
    statement.execute();
    statement.close();*/

    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD flq BOOLEAN");
    statement.execute();
    statement.close();

    statement = connection.prepareStatement("ALTER TABLE " + RESULTS + " ADD spoken BOOLEAN");
    statement.execute();
    statement.close();
  }
}