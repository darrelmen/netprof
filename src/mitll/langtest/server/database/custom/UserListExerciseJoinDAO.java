package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListExerciseJoinDAO extends DAO {
  private static Logger logger = Logger.getLogger(UserListExerciseJoinDAO.class);

  public static final String USER_EXERCISE_LIST_EXERCISE = "userexerciselist_exercise";

  public UserListExerciseJoinDAO(Database database) {
    super(database);
    try {
      createUserListTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  void createUserListTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USER_EXERCISE_LIST_EXERCISE +
      " (" +
      "uniqueid IDENTITY, " +
      "userlistid LONG, " +
      //"exerciseid VARCHAR, " +
      "exerciseid LONG, " +
      "FOREIGN KEY(userlistid) REFERENCES " +
      UserListDAO.USER_EXERCISE_LIST +
      "(uniqueid)" +    // user lists can be combinations of predefined and user defined exercises
        "," +
      "FOREIGN KEY(exerciseid) REFERENCES " +
      UserExerciseDAO.USEREXERCISE +
      "(uniqueid)" +
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
   * @see UserListManager#reallyCreateNewItem(long, mitll.langtest.shared.custom.UserExercise)
   */
  public void add(UserList userList, UserExercise exercise) {
    long id = 0;

    try {
      // there are much better ways of doing this...
      logger.info("UserListExerciseJoinDAO.add :userList " + userList + " exercise " + exercise);
      logger.info("UserListExerciseJoinDAO.add :userList " + userList.getUniqueID() + " exercise " + exercise.getUniqueID());

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
        "INSERT INTO " + USER_EXERCISE_LIST_EXERCISE +
          "(userlistid," +
          "exerciseid" +
          //"uniqueid" +
          ") " +
          "VALUES(?,?);");
      int i = 1;
      //     statement.setLong(i++, userList.getUserID());
      statement.setLong(i++, userList.getUniqueID());
    //  statement.setString(i++, exercise.getID());
      statement.setLong(i++, exercise.getUniqueID());

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

      statement.close();
      database.closeConnection(connection);

      logger.debug("\tUserListExerciseJoinDAO.add : now " + getCount(USER_EXERCISE_LIST_EXERCISE) + " and user exercise is " + userList);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * Pulls the list of exercises out of the database.
   * @see UserExerciseDAO#getOnList(long)
   * @param exclude skip ones in this set of ids
   * @return
   */
  public List<String> getAllFor(long userListID, Set<String> exclude) {
    try {
      String sql = "SELECT * from " + USER_EXERCISE_LIST_EXERCISE +" where userlistid=" + userListID ;
      return getUserExercises(sql,exclude);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  private List<String> getUserExercises(String sql, Set<String> exclude) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<String> exercises = new ArrayList<String>();

    while (rs.next()) {
      String exerciseid = rs.getString("exerciseid");
      if (!exclude.contains(exerciseid)) exercises.add(exerciseid);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return exercises;
  }
}