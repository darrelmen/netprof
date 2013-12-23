package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.ExerciseAnnotation;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class AnnotationDAO extends DAO {
  private static Logger logger = Logger.getLogger(AnnotationDAO.class);

  public static final String ANNOTATION = "annotation";

  public AnnotationDAO(Database database) {
    super(database);
    try {
      createTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }

  }

  /**
   *   String exerciseID; String field; String status; String comment;
   String userID;
   * @param database
   * @throws SQLException
   */
  void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
        ANNOTATION +
      " (" +
      "uniqueid IDENTITY, " +
      "creatorid LONG, " +
      "exerciseid VARCHAR, field VARCHAR, status VARCHAR, modified TIMESTAMP, comment VARCHAR, " +
      "FOREIGN KEY(creatorid) REFERENCES " +
      "USERS" +
      "(ID)" +
      ")");
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }


  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see mitll.langtest.server.database.custom.UserListManager#reallyCreateNewItem(long, mitll.langtest.shared.custom.UserExercise)
   */
  public void add(UserAnnotation annotation) {
    long id = 0;

    try {
      // there are much better ways of doing this...
      logger.info("add :annotation " + annotation);

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
        "INSERT INTO " + ANNOTATION +
          "(creatorid,exerciseid,field,status,modified,comment) " +
          "VALUES(?,?,?,?,?,?);");
      int i = 1;
      //     statement.setLong(i++, annotation.getUserID());
      statement.setLong(i++, annotation.getCreatorID());
      statement.setString(i++, annotation.getExerciseID());
      statement.setString(i++, annotation.getField());
      statement.setString(i++, annotation.getStatus());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      statement.setString(i++, annotation.getComment());

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      if (rs.next()) {
        id = rs.getLong(1);
      } else {
        logger.error("huh? no key was generated?");
      }
      logger.debug("unique id = " + id);

      annotation.setUniqueID(id);

      statement.close();
      database.closeConnection(connection);

    //  logger.debug("now " + getCount(ANNOTATION) + " and user exercise is " + annotation);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public int getCount() { return getCount(ANNOTATION); }


  /**
   * Pulls the list of users out of the database.
   *
   * @return
   */
  public List<UserAnnotation> getAll() {
    try {
      String sql = "SELECT * from " + ANNOTATION + " order by modified desc";
      return getUserAnnotations(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  /**
   * @see UserListManager#addAnnotations(mitll.langtest.shared.AudioExercise)
   * @param exerciseID
   * @return
   */
  public Map<String,ExerciseAnnotation> getLatestByExerciseID(String exerciseID) {
    String sql = "SELECT * from " + ANNOTATION + " where exerciseid='" + exerciseID + "' order by field,modified desc";
    try {
      Map<String,UserAnnotation> fieldToAnno = new HashMap<String, UserAnnotation>();
      List<UserAnnotation> lists = getUserAnnotations(sql);
      for (UserAnnotation annotation : lists) {
        UserAnnotation annotation1 = fieldToAnno.get(annotation.getField());
        if (annotation1 == null) fieldToAnno.put(annotation.getField(),annotation);
        else if (annotation1.getTimestamp() < annotation.getTimestamp()) {
          fieldToAnno.put(annotation.getField(),annotation);
        }
      }
      if (lists.isEmpty()) {
        //logger.error("huh? no annotation with id " + unique);
        return Collections.emptyMap();
      } else {
        Map<String,ExerciseAnnotation>  fieldToAnnotation = new HashMap<String, ExerciseAnnotation>();
        for (Map.Entry<String, UserAnnotation> pair : fieldToAnno.entrySet()) {
          fieldToAnnotation.put(pair.getKey(),new ExerciseAnnotation(pair.getValue().getStatus(),pair.getValue().getComment()));
        }
        logger.debug("field->anno " + fieldToAnno);
        return fieldToAnnotation;
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  private List<UserAnnotation> getUserAnnotations(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<UserAnnotation> lists = new ArrayList<UserAnnotation>();

    while (rs.next()) {
      long uniqueid = rs.getLong("uniqueid");
      lists.add(new UserAnnotation(uniqueid, //id
          rs.getString("exerciseid"), // exp
          rs.getString("field"), // exp
          rs.getString("status"), // exp
          rs.getString("comment"),
          rs.getLong("creatorid"), //
          rs.getTimestamp("modified").getTime()
      )
      );
    }

    //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
    rs.close();
    statement.close();
    database.closeConnection(connection);
    return lists;
  }

  public Set<String> getIncorrectIds()  {
    Connection connection = database.getConnection();
    String sql = "SELECT DISTINCT exerciseid from " + ANNOTATION + " order by exerciseid";

    Set<String> lists = Collections.emptySet();
    try {
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      lists = new TreeSet<String>();

      while (rs.next()) {
        lists.add(rs.getString(1));

      }

      //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("Got " +e + " doing " + sql,e);
    }
    return lists;
  }
}