package mitll.langtest.server.database;

import mitll.langtest.shared.grade.Grader;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @deprecated there are no graders, just users
 */
public class GraderDAO {
  private static Logger logger = Logger.getLogger(DatabaseImpl.class);

  private final Database database;

  public GraderDAO(Database database) {
    this.database = database;
  }

  /**
   * @return
   */
  public int addGrader(String login) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      String sql = "INSERT INTO grader(login) VALUES(?)";
      boolean exists = false;//gradeExists(resultID);
      /*if (exists) {
        sql = "UPDATE grades SET grade='" +grade+
            "' WHERE resultID='" + resultID+
            "'";
      }*/
      statement = connection.prepareStatement(sql);
      if (!exists) {
        int i = 1;
        statement.setString(i++, login);
      }
      statement.executeUpdate();
      statement.close();

      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return getCount();
  }

  /**
   * @seex DatabaseImpl#graderExists
   * @param id
   * @return
   */
  public boolean graderExists(String id) {
    boolean val = false;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;
      statement = connection.prepareStatement("SELECT COUNT(*) from grader where login='" +
          id +
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
   * @seex mitll.langtest.server.database.ResultDAO#getResultsForExercise(String)
   * @param exerciseID
   * @return
   */
  public Set<Integer> getResultIDsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT resultID from grades where exerciseID='" +exerciseID+
          "'");

      ResultSet rs = statement.executeQuery();
      Set<Integer> results = new HashSet<Integer>();
      while (rs.next()) {
        int resultID = rs.getInt(1);
        results.add(resultID);
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

    //  System.out.println("found " + results.size() + " graded results for " + exerciseID);
      return results;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new HashSet<Integer>();
  }

  void dropGrader() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("DROP TABLE if exists grader");
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

  public Collection<Grader> getGraders() {
    List<Grader> graders = new ArrayList<Grader>();
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM grader ");
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int i = 1;
        graders.add(new Grader((long) rs.getInt(i++), rs.getString(i), "", 0));
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      if (e.getMessage().contains("not found")) {
        //logger.debug("note there is no grader table.");
      } else {
        logger.error("got " + e, e);
      }
    }
    return graders;
  }

  /**
   * @seex mitll.langtest.server.database.DatabaseImpl#DatabaseImpl(javax.servlet.http.HttpServlet)
   * @param connection
   * @throws java.sql.SQLException
   */
  void createGraderTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "grader (id IDENTITY, login VARCHAR)");
    statement.execute();
    statement.close();
  }
}