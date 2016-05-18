/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database;

import mitll.langtest.shared.User;
import mitll.langtest.shared.grade.Grade;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Stored and retrieves grades from the database.
 * we don't really support grading in classroom right now
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class GradeDAO extends DAO {
  private static final String SELECT_PREFIX = "SELECT id, exerciseID, resultID, grade, grader, gradeType, gradeIndex from grades";
 // private static final String SELECT_PREFIX2 = SELECT_PREFIX +" where exerciseID";
  private static final Logger logger = Logger.getLogger(GradeDAO.class);

  private static final String GRADES = "grades";
 // private final boolean debug = false;
  private final UserDAO userDAO;
//  private final ResultDAO resultDAO;

  public GradeDAO(Database database, UserDAO userDAO, ResultDAO resultDAO) {
    super(database);
    this.userDAO = userDAO;
 //   this.resultDAO = resultDAO;
  }

/*  public class CountAndGradeID implements IsSerializable {
    public int count;
    public int resultCount;
    public long gradeID;

    public CountAndGradeID() {}
    public CountAndGradeID(int c, int resultCount, long g) {
      this.count = c;
      this.resultCount = resultCount;
      this.gradeID = g;
    }
  }*/

  /**
   *
   * @return map of result id to grades for that result
   */
  public Map<Integer, List<Grade>> getIdToGrade() {
    Collection<Grade> grades = getGrades();

    Map<Integer,List<Grade>> idToGrade = new HashMap<Integer, List<Grade>>();
    for (Grade g : grades) {
      List<Grade> gradesForResult = idToGrade.get(g.resultID);
      if (gradesForResult == null) {
        idToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
      }
      gradesForResult.add(g);
    }

    return idToGrade;
  }

  /**
   * @see DatabaseImpl#changeGrade(mitll.langtest.shared.grade.Grade)
   * @param toChange
   */
/*  public void changeGrade(Grade toChange) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement;

      String sql = "UPDATE grades " +
          "SET grade='" + toChange.grade + "' " +
          "WHERE id=" + toChange.id;
      if (debug) {
        logger.debug("changeGrade " + toChange);
      }
      statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (debug) logger.debug("UPDATE " + i);
      if (i == 0) {
        logger.error("huh? didn't update the grade for " + toChange);
      }

      finish(connection, statement);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/

  /**
   * @see DatabaseImpl#addGrade(String, mitll.langtest.shared.grade.Grade)
   * @param exerciseID
   * @param toAdd
   * @return
   */
/*  public CountAndGradeID addGradeEasy(String exerciseID, Grade toAdd) {
    return addGrade(toAdd.resultID, exerciseID, toAdd.grade, toAdd.id, true, toAdd.grader, toAdd.gradeType, toAdd.gradeIndex);
  }*/

  /**
   * If a grade already exists, update the value.
   *
   *
   * @param resultID
   * @param exerciseID
   * @param grade
   * @param gradeID
   * @param correct    ignored for now
   * @param grader
   * @param gradeType
   * @return
   * @see DatabaseImpl#addGrade(String, mitll.langtest.shared.grade.Grade)
   */
/*  private CountAndGradeID addGrade(int resultID, String exerciseID, int grade, long gradeID, boolean correct,
                                   int grader, String gradeType, int gradeIndex) {
    long id = 0;
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      //logger.debug("addGrade " + grade + " grade for " + resultID + " and " +grader + " ex id " + exerciseID+ " and " +gradeID);

      String sql = "INSERT INTO grades(resultID,exerciseID,grade,correct,grader,gradeType,gradeIndex) VALUES(?,?,?,?,?,?,?)";

      if (debug)
        logger.debug("INSERT " + grade + " grade for exercise " + exerciseID + " and result " +resultID +
          " grader = " + grader + ", grade id " + gradeID + ", type = " + gradeType);

      PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      int i = 1;
      statement.setInt(i++, resultID);
      statement.setString(i++, exerciseID);
      statement.setInt(i++, grade);
      statement.setBoolean(i++, correct);
      statement.setString(i++, ""+grader);
      statement.setString(i++, gradeType);
      statement.setInt(i++, gradeIndex);

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for " + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      if (rs.next()) {
        id = rs.getLong(1);
      } else {
        logger.error("huh? no key was generated?");
      }

      finish(connection, statement);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new CountAndGradeID(getCount(), resultDAO.getNumResults(), id);
  }*/

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
         logger.error("Found " + anInt + " grades for " + resultID + " and " +grader);
        }
        else {
          logger.debug("gradeExists : Found " + anInt + " grades for " + resultID + " and " +grader +" and " +gradeID);
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
/*  public GradesAndIDs getResultIDsForExercise(String exerciseID) {
    String sql = SELECT_PREFIX +
      "='" + exerciseID + "'";

    return getGradesForSQL(sql);
  }*/

  /**
   * @see DatabaseImpl#getExercisesGradeBalancing(boolean, boolean)
   * @return
   */
  public Collection<Grade> getGrades() {
    return getGradesForSQL(SELECT_PREFIX).grades;
  }

  /**
   * @seex DatabaseImpl#getNextUngradedExerciseQuick(java.util.Collection, int, boolean, boolean, boolean)
   * @paramx toExclude
   * @return
   */
/*  private GradesAndIDs getAllGradesExcluding(Collection<String> toExclude) {
    String list = getInList(toExclude);

    String sql = SELECT_PREFIX +" not in (" + list + ")";

    return getGradesForSQL(sql);
  }*/

  private GradesAndIDs getGradesForSQL(String sql) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      //logger.debug("getGradesForSQL : sql " + sql);
      ResultSet rs = statement.executeQuery();
      Set<Grade> grades = new HashSet<Grade>();
      Set<Integer> ids = new HashSet<Integer>();

      Map<String, User> idToUser = getIDToUser();

      boolean validIds = (idToUser.size() > 0 && !idToUser.keySet().iterator().next().equals("NOT_SET"));
      int count = 0;
      while (rs.next()) {
        int i = 1;
        int id = rs.getInt(i++);
        String exerciseID = rs.getString(i++);
        int resultID = rs.getInt(i++);
        int grade = rs.getInt(i++);
        String grader = rs.getString(i++);
        int graderID = 0;
        try {
          graderID = Integer.parseInt(grader);
        } catch (NumberFormatException e) {
          User user = idToUser.get(grader);
          if (user != null) {
            graderID = (int) user.getId();
          } else {
            if (count++ < 20 && validIds) {
              logger.warn("couldn't parse grader '" + grader + "' or find user by that id in " + idToUser.keySet());
            }
          }
        }
        String type = rs.getString(i++);
        if (type == null) type = "";
        int gradeIndex = rs.getInt(i++);

        Grade g = new Grade(id, exerciseID, resultID, grade, graderID, type, gradeIndex);
       // logger.debug("made " +g);
        grades.add(g);
        ids.add(resultID);
      }
      finish(connection, statement, rs);

    //  logger.debug("found " + results.size() + " graded results for " + exerciseID);
      return new GradesAndIDs(grades,ids);
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new GradesAndIDs(new ArrayList<Grade>(),new ArrayList<Integer>());
  }

  private Map<String, User> getIDToUser() {
    List<User> users = userDAO.getUsers();
    Map<String,User> idToUser = new HashMap<String, User>();
    for (User u : users) idToUser.put(u.getUserID(),u);
    return idToUser;
  }


  /**
   *
   */
  public static class GradesAndIDs {
    final Collection<Grade> grades;
    final Collection<Integer> resultIDs;
    public GradesAndIDs(Collection<Grade> grades, Collection<Integer> ids) {
      this.grades = grades;
      this.resultIDs = ids;
    }
  }

  /**
   * @see #addGrade
   * @return
   */
/*
  private int getCount() {
    return getCount(GRADES);
  }
*/

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
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
    } else if (numColumns < 8) {
      addColumnToTable3(connection);
    }
  }

  private void createTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "grades (id IDENTITY, " +
      "exerciseID VARCHAR, " +
      "resultID INT, " +
      "grade INT, " +
      "correct BOOLEAN, " +
      "grader VARCHAR, " +
      "gradeType VARCHAR, " +
      "gradeIndex INT " +

      ")");
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

  private void addColumnToTable3(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE grades ADD gradeIndex INT");
    statement.execute();
    statement.close();
  }
}
