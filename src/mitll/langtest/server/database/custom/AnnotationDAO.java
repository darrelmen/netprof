package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
  private UserDAO userDAO;
  private UserExerciseDAO userExerciseDAO;
  private UserListVisitorJoinDAO userListVisitorJoinDAO;
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

/*  public void updateModified(long uniqueID) {
    try {
      Connection connection = database.getConnection();

      String sql = "UPDATE " + ANNOTATION +
        " " +
        "SET " +
        "modified=? "+
        "WHERE uniqueid=?";

      PreparedStatement statement = connection.prepareStatement(sql);
      statement.setTimestamp(1,new Timestamp(System.currentTimeMillis()));
      statement.setLong(2, uniqueID);
      int i = statement.executeUpdate();

      //if (false) logger.debug("UPDATE " + i);
      if (i == 0) {
        logger.error("huh? didn't update the userList for " + uniqueID + " sql " + sql);
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }*/

  public int getCount() { return getCount(ANNOTATION); }


  /**
   * Pulls the list of users out of the database.
   *
   * @return
   * @param userid
   */
  public List<UserAnnotation> getAll(long userid) {
    try {
      String sql = "SELECT * from " + ANNOTATION + " order by modified desc";
      return getUserLists(sql,userid);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  public List<UserAnnotation> getAllPublic() {
    try {
      String sql = "SELECT * from " + ANNOTATION + " order by modified";
      return getUserLists(sql,-1);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

/*  public List<UserList> getAllOwnedBy(long id) {
    try {
      String sql = "SELECT * from " + ANNOTATION + " where creatorid=" + id+
        " order by modified";
      return getUserLists(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }*/

  public UserAnnotation getWhere(long unique) {
    String sql = "SELECT * from " + ANNOTATION + " where uniqueid=" + unique + " order by modified";
    try {
      List<UserAnnotation> lists = getUserAnnotations(sql,-1);
      if (lists.isEmpty()) {
        logger.error("huh? no annotation with id " + unique);
        return null;
      } else {
        return lists.iterator().next();
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  public List<UserAnnotation> getByExerciseID(String exerciseID) {
    String sql = "SELECT * from " + ANNOTATION + " where exerciseid=" + exerciseID + " order by field,modified desc";
    try {
      List<UserAnnotation> lists = getUserAnnotations(sql,-1);
      if (lists.isEmpty()) {
        //logger.error("huh? no annotation with id " + unique);
        return Collections.emptyList();
      } else {
        return lists;
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }


  /**
   * TODO don't want to always get all the exercises!
   * @see mitll.langtest.server.database.custom.UserAnnotationManager#getUserAnnotationByID(long)
   * @param unique
   * @return
   */
  public UserAnnotation getWithExercises(long unique) {
    UserAnnotation where = getWhere(unique);
    populateList(where);
    return where;
  }

  private List<UserAnnotation> getUserAnnotations(String sql,long userid) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<UserAnnotation> lists = new ArrayList<UserAnnotation>();

    while (rs.next()) {
      long uniqueid = rs.getLong("uniqueid");
      lists.add(new UserAnnotation(uniqueid, //id
        userDAO.getUserWhere(rs.getLong("creatorid")), // age
        rs.getString("name"), // exp
        rs.getString("description"), // exp
        rs.getString("classmarker"), // exp
        rs.getTimestamp("modified").getTime(),
        rs.getBoolean("isprivate")
      )
      );
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    for (UserAnnotation ul : lists) {
      if (userid == -1 || ul.getCreator().id == userid || !ul.isFavorite()) {   // skip other's favorites
        populateList(ul);
      }
      else {
        //logger.info("for " + userid +" skipping " + ul);
      }
    }
    return lists;
  }

  /**
   * TODO : This is going to get slow?
   * @param where
   */
  private void populateList(UserAnnotation where) {
    Collection<UserExercise> onList = userExerciseDAO.getOnList(where.getUniqueID());
    logger.debug("populateList : got " + onList.size() + " for list "+where.getUniqueID());
    where.setExercises(onList);
    where.setVisitors(UserAnnotationVisitorJoinDAO.getWhere(where.getUniqueID()));

    logger.debug("\tlist now "+where);
  }

  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
  }
}