package mitll.langtest.server.database;

import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Grade;

import java.sql.*;
import java.util.*;

/**
 * Stored and retrieves grades from the database.
 */
public class GradeDAO {
  private final Database database;
  private boolean debug = false;

  public GradeDAO(Database database) {
    this.database = database;
  }

  /**
   * If a grade already exists, update the value.
   * @see DatabaseImpl#addGrade(int, String, int, long, boolean, String)
   * @param resultID
   * @param exerciseID
   * @param grade
   * @param gradeID
   * @param correct ignored for now
   * @param grader
   * @return
   */
  public CountAndGradeID addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct, String grader) {
    long id = 0;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;
      //System.out.println("addGrade " + grade + " grade for " + resultID + " and " +grader + " ex id " + exerciseID+ " and " +gradeID);

      String sql = "INSERT INTO grades(resultID,exerciseID,grade,correct,grader) VALUES(?,?,?,?,?)";
      boolean exists = gradeID != -1;
      if (exists) {
        sql = "UPDATE grades " +
            "SET grade='" +grade+ "' " +
            "WHERE resultID='" + resultID+ "' " +
            (gradeID != -1 ? " AND id=" +gradeID : "");
        if (debug) System.out.println("UPDATE " + grade + " grade for " + resultID + " and " +grader+ " and " +gradeID);
        statement = connection.prepareStatement(sql);
      }
      else {
        statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      }
      if (!exists) {
        int i = 1;
        statement.setInt(i++, resultID);
        statement.setString(i++, exerciseID);
        statement.setInt(i++, grade);
        statement.setBoolean(i++, correct);
        statement.setString(i++, grader);
      }
      int i = statement.executeUpdate();

      if (exists) {
        if (debug) System.out.println("UPDATE " + i);
        if (i == 0) {
          System.err.println("huh? didn't update the grade for "+ resultID + " and " +grader+ " and " +gradeID);
        }
      }
      else {
        ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
        while (rs.next()) {
          id = rs.getLong(1);
        }
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new CountAndGradeID(getCount(),id);
  }

/*  private boolean gradeExists(int resultID, String grader, long gradeID) {
    boolean val = false;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;
      statement = connection.prepareStatement("SELECT COUNT(*) from grades " +
          "where resultID='" +resultID + "' " +
          "AND grader='" + grader + "'" +
          (gradeID != -1 ? " AND id=" +gradeID : "")
      );
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        int anInt = rs.getInt(1);
        if (anInt > 1) {
         System.err.println("Found " + anInt + " grades for " + resultID + " and " +grader);
        }
        else {
          System.out.println("gradeExists : Found " + anInt + " grades for " + resultID + " and " +grader +" and " +gradeID);
        }
        val = anInt > 0;
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return val;
  }*/

  /**
   * @see ResultDAO#getResultsForExercise
   * @param exerciseID
   * @return
   */
  public GradesAndIDs getResultIDsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT id, resultID, grade, grader from grades where exerciseID='" + exerciseID + "'";
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();
      Set<Grade> grades = new HashSet<Grade>();
      Set<Integer> ids = new HashSet<Integer>();
      while (rs.next()) {
        int i = 1;
        int id = rs.getInt(i++);
        int resultID = rs.getInt(i++);
        int grade = rs.getInt(i++);
        String grader = rs.getString(i++);
        grades.add(new Grade(id, resultID, grade, grader));
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
  }

  /**
   * TODO : remove
   */
  public static class GradesAndIDs {
    Collection<Grade> grades;
    public GradesAndIDs(Collection<Grade> grades, Collection<Integer> ids) {
      this.grades = grades;
    }
  }

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