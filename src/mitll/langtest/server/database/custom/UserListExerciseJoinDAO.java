package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListExerciseJoinDAO extends DAO {
  private static final Logger logger = Logger.getLogger(UserListExerciseJoinDAO.class);

  private static final String USERLISTID = "userlistid";
  private static final String EXERCISEID = "exerciseid";
  private static final String UNIQUEID = "uniqueid";

  public static final String USER_EXERCISE_LIST_EXERCISE = "userexerciselist_exercise";

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   * @param database
   */
  public UserListExerciseJoinDAO(Database database) {
    super(database);
    try {
      createUserListTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  void createUserListTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USER_EXERCISE_LIST_EXERCISE +
      " (" +
      UNIQUEID +
      " IDENTITY, " +
      USERLISTID +
      " LONG, " +
      EXERCISEID +
      " VARCHAR , " +
      "FOREIGN KEY(" +
      USERLISTID +
      ") REFERENCES " +
      UserListDAO.USER_EXERCISE_LIST +
      "(" +
      UNIQUEID +
      ")" +    // user lists can be combinations of predefined and user defined exercises
 /*       "," +
      "FOREIGN KEY(exerciseid) REFERENCES " +
      UserExerciseDAO.USEREXERCISE +
      "(uniqueid)" +*/
      ")");
    finish(database, connection, statement);
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see UserListManager#addItemToList(long, mitll.langtest.shared.custom.UserExercise)
   */
  public void add(UserList userList, String uniqueID) {
    try {
      // there are much better ways of doing this...
      logger.info("UserListExerciseJoinDAO.add :userList " + userList.getUniqueID() + " exercise " + uniqueID);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + USER_EXERCISE_LIST_EXERCISE +
          "(" +
          USERLISTID +
          "," +
          EXERCISEID +
          ") " +
          "VALUES(?,?);");
      int i = 1;
      statement.setLong(i++, userList.getUniqueID());
      statement.setString(i++, uniqueID);

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");

      finish(connection, statement);

      logger.debug("\tUserListExerciseJoinDAO.add : now " + getCount(USER_EXERCISE_LIST_EXERCISE) + " and user exercise is " + userList);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public void removeListRefs(long listid) {
    remove(USER_EXERCISE_LIST_EXERCISE, USERLISTID, listid);
  }

  /**
   * @see UserListManager#deleteItemFromList(long, String, java.util.Collection)
   * @param listid
   * @param exid
   * @return
   */
  public boolean remove(long listid, String exid) {
    String sql = getRemoveSQL(listid, exid);
    return doSqlOn(sql,USER_EXERCISE_LIST_EXERCISE);
  }

  private String getRemoveSQL(long listid, String exid) {
    return "DELETE FROM " + USER_EXERCISE_LIST_EXERCISE +" WHERE " +
      USERLISTID +
      "=" + listid +
      " AND " +
      EXERCISEID +
      "='" + exid +
      "'; ";
  }
}