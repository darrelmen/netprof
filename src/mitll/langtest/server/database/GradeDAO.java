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
   * @see DatabaseImpl#addGrade(int, String, int, boolean)
   * @param resultID
   * @param exerciseID
   * @param grade
   * @param correct
   * @return
   */
  public int addGrade(int resultID, String exerciseID, int grade, boolean correct) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      String sql = "INSERT INTO grades(resultID,exerciseID,grade,correct) VALUES(?,?,?,?)";
      boolean exists = gradeExists(resultID);
      if (exists) {
        sql = "UPDATE grades SET grade='" +grade+
            "' WHERE resultID='" + resultID+
            "'";
      }
      statement = connection.prepareStatement(sql);
      if (!exists) {
        int i = 1;
        statement.setInt(i++, resultID);
        statement.setString(i++, exerciseID);
        statement.setInt(i++, grade);
        statement.setBoolean(i++, correct);
      }
      statement.executeUpdate();
      statement.close();

      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return getCount();
  }

  public boolean gradeExists(int resultID) {
    boolean val = false;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;
      statement = connection.prepareStatement("SELECT COUNT(*) from grades where resultID='" +
          resultID +
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
   * Pulls the list of results out of the database.
   *
   * @return
   */
/*  public List<Grade> getGrades() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * from grades;");

      ResultSet rs = statement.executeQuery();
      List<Grade> results = new ArrayList<Grade>();
      while (rs.next()) {
        int i = 1;
        rs.getInt(i++);
        long userID = rs.getLong(i++);
        String plan = rs.getString(i++);
        String exid = rs.getString(i++);
        int qid = rs.getInt(i++);
        Timestamp timestamp = rs.getTimestamp(i++);
        String answer = rs.getString(i++);
        boolean valid = rs.getBoolean(i++);
        results.add(new Grade(userID, //id
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
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
  }*/

  /**
   * @see ResultDAO#getResultsForExercise(String)
   * @param exerciseID
   * @return
   */
  public GradesAndIDs getResultIDsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT resultID, grade from grades where exerciseID='" + exerciseID + "'";
      PreparedStatement statement = connection.prepareStatement(sql);

      ResultSet rs = statement.executeQuery();
      Set<Grade> grades = new HashSet<Grade>();
      Set<Integer> ids = new HashSet<Integer>();
      while (rs.next()) {
        int resultID = rs.getInt(1);
        int grade = rs.getInt(2);
        grades.add(new Grade(resultID, grade));
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

  void dropGrades() {
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
      "grades (id IDENTITY, exerciseID VARCHAR, resultID INT, grade INT, correct BOOLEAN)");
    statement.execute();
    statement.close();
  }
}