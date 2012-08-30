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

public class ResultDAO {
  private final Database database;
  private GradeDAO gradeDAO;
  private ScheduleDAO scheduleDAO ;
  public ResultDAO(Database database) {
    this.database = database;

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
      Result e = new Result(uniqueID, userID, //id
          plan, // plan
          exid, // id
          qid, // qid
          answer, // answer
          //rs.getString(i++), // audioFile
          valid, // valid
          timestamp.getTime()
      );
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
   * @see DatabaseImpl#getNextUngradedExercise
   * @param e
   * @return
   */
/*  public boolean areAnyResultsLeftToGradeFor(Exercise e) {
    return !getResultsForExercise(e.getID(),1).isEmpty();
  }*/

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
   * TODO : Add proper filtering
   * @param exerciseID
   * @param gradedResults
   * @param expected if > 1 remove flq results (hack!)
   * @return
   */
  private List<Result> getResultsForExercise(String exerciseID, Collection<Grade> gradedResults, int expected) {
    try {
      List<Result> resultsForQuery = getAllResultsForExercise(exerciseID);
      enrichResults(resultsForQuery,exerciseID);
      //System.out.println("expected " + expected + " before " + resultsForQuery.size());

      if (expected > 1) { // hack!
        for (Iterator<Result> iter = resultsForQuery.iterator(); iter.hasNext(); ) {
          Result next = iter.next();
          if (next.flq) {
            iter.remove();
          }
        }
      }

      Map<Integer,Integer> idToCount = new HashMap<Integer, Integer>();
      for (Grade g : gradedResults) {
        Integer countForResult = idToCount.get(g.resultID);
        if (countForResult == null) idToCount.put(g.resultID,1);
        else {
          idToCount.put(g.resultID,countForResult+1);
        }
      }
      //System.out.println("expected " + expected + " before " + resultsForQuery.size() + " map "+ idToCount);

      for (Iterator<Result> iter = resultsForQuery.iterator(); iter.hasNext();) {
        Result next = iter.next();
        Integer count = idToCount.get(next.uniqueID);
        if (count != null && count >= expected) {
        //  System.out.println("removing graded item for result " + next);
          iter.remove();
        }
        else {
          //System.out.println("NOT removing graded item for result " + next + " count = " + count);
        }
      }
      //System.out.println("after removing graded items count = " + resultsForQuery.size());

      return resultsForQuery;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
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
   * @param resultsForExercise
   * @param exid
   */
  public void enrichResults(List<Result> resultsForExercise, String exid) {
    Set<Long> users = getUsers(resultsForExercise);

    Map<Long, List<Schedule>> scheduleForUserAndExercise = scheduleDAO.getScheduleForUserAndExercise(users, exid);
    for (Result r : resultsForExercise) {
      List<Schedule> schedules = scheduleForUserAndExercise.get(r.userid);
      Schedule schedule = schedules.get(0);
      r.setFLQ(schedule.flQ);
      r.setSpoken(schedule.spoken);
    }
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

  void createResultTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "results (id IDENTITY, userid INT, plan VARCHAR, " +
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


}