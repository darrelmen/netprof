package mitll.langtest.server.database;

import mitll.langtest.shared.Grade;

import java.sql.*;
import java.util.*;

public class GradeDAO {
  private final Database database;

  public GradeDAO(Database database) {
    this.database = database;
  }

  /**
   * If a grade already exists, update the value.
   * @see DatabaseImpl#addGrade(int, String, int, boolean, String)
   * @param resultID
   * @param exerciseID
   * @param grade
   * @param correct
   * @param grader
   * @return
   */
  public int addGrade(int resultID, String exerciseID, int grade, boolean correct, String grader) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      String sql = "INSERT INTO grades(resultID,exerciseID,grade,correct,grader) VALUES(?,?,?,?,?)";
      boolean exists = gradeExists(resultID, grader);
      if (exists) {
        sql = "UPDATE grades SET grade='" +grade+
            "' WHERE resultID='" + resultID+
            "' AND grader='" +grader +
            "'";
      }
      statement = connection.prepareStatement(sql);
      if (!exists) {
        int i = 1;
        statement.setInt(i++, resultID);
        statement.setString(i++, exerciseID);
        statement.setInt(i++, grade);
        statement.setBoolean(i++, correct);
        statement.setString(i++, grader);
      }
      statement.executeUpdate();
      statement.close();

      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return getCount();
  }

  private boolean gradeExists(int resultID, String grader) {
    boolean val = false;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;
      statement = connection.prepareStatement("SELECT COUNT(*) from grades " +
          "where resultID='" +
          resultID +
          "' AND grader='" +
          grader +
          "'");
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        val = rs.getInt(1) > 0;
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return val;
  }

  /**
   * @see ResultDAO#getResultsForExercise(String)
   * @param exerciseID
   * @return
   */
  public GradesAndIDs getResultIDsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT resultID, grade, grader from grades where exerciseID='" + exerciseID + "'";
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();
      Set<Grade> grades = new HashSet<Grade>();
      Set<Integer> ids = new HashSet<Integer>();
      while (rs.next()) {
        int resultID = rs.getInt(1);
        int grade = rs.getInt(2);
        String grader = rs.getString(3);
        grades.add(new Grade(resultID, grade, grader));
        ids.add(resultID);
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

    //  System.out.println("found " + results.size() + " graded results for " + exerciseID);
      return new GradesAndIDs(grades,ids);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new GradesAndIDs(new ArrayList<Grade>(),new ArrayList<Integer>());
    //return new HashSet<Grade>();
  }

  public static class GradesAndIDs {
    Collection<Grade> grades;
    Collection<Integer> ids;

    public GradesAndIDs(Collection<Grade> grades, Collection<Integer> ids) {
      this.grades = grades;
      this.ids = ids;
    }
  }

/*  void dropGrades() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("DROP TABLE if exists grades");
      if (!statement.execute()) {
        System.err.println("couldn't create table?");
      }
      statement.close();
      database.closeConnection(connection);

    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  }
    }
  }*/

  public int getCount() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;
      statement = connection.prepareStatement("SELECT COUNT(*) from grades");
      ResultSet rs = statement.executeQuery();
      if (rs.next()) return rs.getInt(1);
      statement.close();

      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }

  /**
   * @see DatabaseImpl#DatabaseImpl(javax.servlet.http.HttpServlet)
   * @param connection
   * @throws SQLException
   */
  void createGradesTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "grades (id IDENTITY, exerciseID VARCHAR, resultID INT, grade INT, correct BOOLEAN, grader VARCHAR)");
    statement.execute();
    statement.close();

    int numColumns = getNumColumns(connection);
    if (numColumns != 6) {
      addColumnToTable(connection);
    }
  }

  private int getNumColumns(Connection connection) throws SQLException {
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM grades");

    // Get result set meta data
    ResultSetMetaData rsmd = rs.getMetaData();
    int numColumns = rsmd.getColumnCount();
    stmt.close();
    return numColumns;
  }

  private void addColumnToTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE grades ADD grader VARCHAR");
    statement.execute();
    statement.close();
  }
}