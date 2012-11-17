package mitll.langtest.server.database;

import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;

import java.sql.*;
import java.util.*;

/**
 * Stored and retrieves grades from the database.
 */
public class GradeDAO extends DAO {
  private static final String GRADES = "grades";
  //private final Database database;
  private boolean debug = false;

  public GradeDAO(Database database) {
    super(database);
  }

  /**
   * @see DatabaseImpl#changeGrade(mitll.langtest.shared.Grade)
   * @param toChange
   */
  public void changeGrade(Grade toChange) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      String sql = "UPDATE grades " +
          "SET grade='" + toChange.grade + "' " +
          "WHERE id=" + toChange.id;
      if (debug) System.out.println("changeGrade " + toChange);
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

  public CountAndGradeID addGradeEasy(String exerciseID, Grade toAdd) {
    return addGrade(toAdd.resultID, exerciseID, toAdd.grade, toAdd.id, true, toAdd.grader, toAdd.gradeType);
  }

  /**
   * If a grade already exists, update the value.
   *
   * @param resultID
   * @param exerciseID
   * @param grade
   * @param gradeID
   * @param correct    ignored for now
   * @param grader
   * @param gradeType
   * @return
   * @see DatabaseImpl#addGrade(String, mitll.langtest.shared.Grade)
   */
  public CountAndGradeID addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct, String grader, String gradeType) {
    long id = 0;
    try {
      Connection connection = database.getConnection();
      //System.out.println("addGrade " + grade + " grade for " + resultID + " and " +grader + " ex id " + exerciseID+ " and " +gradeID);

      String sql = "INSERT INTO grades(resultID,exerciseID,grade,correct,grader,gradeType) VALUES(?,?,?,?,?,?)";

      if (debug)
        System.out.println("INSERT " + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      int i = 1;
      statement.setInt(i++, resultID);
      statement.setString(i++, exerciseID);
      statement.setInt(i++, grade);
      statement.setBoolean(i++, correct);
      statement.setString(i++, grader);
      statement.setString(i++, gradeType);
      int j = statement.executeUpdate();

      if (j != 1)
        System.err.println("huh? didn't insert row for " + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      if (rs.next()) {
        id = rs.getLong(1);
      } else {
        System.err.println("huh? no key was generated?");
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new CountAndGradeID(getCount(), id);
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
    String sql = "SELECT id, resultID, grade, grader, gradeType from grades where exerciseID='" + exerciseID + "'";

    return getGradesForSQL(sql);
  }

  public Collection<Grade> getGrades() {
    Set<String> objects = Collections.emptySet();
    return getAllGradesExcluding(objects).grades;
  }

  public GradesAndIDs getAllGradesExcluding(Collection<String> toExclude) {
    StringBuilder b = new StringBuilder();
    for (String id : toExclude) b.append("'").append(id).append("'").append(",");
    String list = b.toString();
    list = list.substring(0,Math.max(0,list.length()-1));

    String sql = "SELECT id, resultID, grade, grader, gradeType from grades where exerciseID not in (" + list + ")";

    return getGradesForSQL(sql);
  }

  private GradesAndIDs getGradesForSQL(String sql) {
    try {
      Connection connection = database.getConnection();
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
        String type = rs.getString(i++);
        if (type == null) type = "";
        Grade g = new Grade(id, resultID, grade, grader, type);
       // System.out.println("made " +g);
        grades.add(g);
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
   *
   */
  public static class GradesAndIDs {
    Collection<Grade> grades;
    Collection<Integer> resultIDs;
    public GradesAndIDs(Collection<Grade> grades, Collection<Integer> ids) {
      this.grades = grades;
      this.resultIDs = ids;
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
    createTable(connection);

    int numColumns = getNumColumns(connection);
    if (numColumns < 6) {
      addColumnToTable(connection);
    }
    if (numColumns < 7) {
      addColumnToTable2(connection);
    }
  }

  private void createTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "grades (id IDENTITY, exerciseID VARCHAR, resultID INT, grade INT, correct BOOLEAN, grader VARCHAR, gradeType VARCHAR)");
    statement.execute();
    statement.close();
  }

  private int getNumColumns(Connection connection) throws SQLException {
    return getNumColumns(connection, GRADES);
  }

  private void addColumnToTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE grades ADD grader VARCHAR");
    statement.execute();
    statement.close();
  }

  private void addColumnToTable2(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE grades ADD gradeType VARCHAR");
    statement.execute();
    statement.close();
  }
}