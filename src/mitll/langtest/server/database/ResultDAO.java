package mitll.langtest.server.database;

import mitll.langtest.shared.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ResultDAO {
  private final Database database;
  private GradeDAO gradeDAO;
  public ResultDAO(Database database) {
    this.database = database;

    gradeDAO = new GradeDAO(database);

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
      results.add(new Result(uniqueID,userID, //id
        plan, // plan
        exid, // id
        qid, // qid
        answer, // answer
        //rs.getString(i++), // audioFile
        valid, // valid
        timestamp.getTime()
      ));
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return results;
  }

  /**
   * Joins against grades -- don't return graded exercises
   *
   * @see DatabaseImpl#getResultsForExercise(String)
   * @param exerciseID
   * @return results that haven't been graded yet
   */
  public List<Result> getResultsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT * from results where EXID='" + exerciseID + "'";
     // System.err.println("executing " + sql);
      PreparedStatement statement = connection.prepareStatement(sql);

      List<Result> resultsForQuery = getResultsForQuery(connection, statement);
    //  System.err.println("got back " + resultsForQuery.size());

      Set<Integer> resultIDsForExercise = gradeDAO.getResultIDsForExercise(exerciseID);
      for (Iterator<Result> iter = resultsForQuery.iterator(); iter.hasNext();) {
        Result next = iter.next();
        if (resultIDsForExercise.contains(next.uniqueID)) {
        // System.out.println("removing graded item for result " + next.uniqueID);
          iter.remove();
        }
      }
      //System.err.println("after removing graded items count = " + resultsForQuery.size());

      return resultsForQuery;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
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
   * @param database
   * @throws Exception
   */
  private void showResults(Database database) throws Exception {
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
  }

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