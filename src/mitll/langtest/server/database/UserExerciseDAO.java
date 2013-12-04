package mitll.langtest.server.database;

import mitll.langtest.shared.custom.UserExercise;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserExerciseDAO extends DAO {
  private static Logger logger = Logger.getLogger(UserExerciseDAO.class);

  private static final String USEREXERCISE = "userexercise";

  public UserExerciseDAO(Database database) {
    super(database);
    try {
      createUserTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see mitll.langtest.server.database.custom.UserListManager#reallyCreateNewItem(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserExercise)
   */
  public void add(UserExercise userExercise) {
    long id = 0;

    try {
      // there are much better ways of doing this...
      logger.info("add :userExercise " + userExercise);

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
        "INSERT INTO " + USEREXERCISE +
          "(english,foreignLanguage,creatorid,refAudio,slowAudioRef) " +
          "VALUES(?,?,?,?,?);");
      int i = 1;
      //     statement.setLong(i++, userExercise.getUserID());
      statement.setString(i++, userExercise.getEnglish());
      statement.setString(i++, userExercise.getForeignLanguage());
      statement.setLong(i++, userExercise.getCreator());
      statement.setString(i++, userExercise.getRefAudio());
      statement.setString(i++, userExercise.getSlowAudioRef());

      int i1 = statement.executeUpdate();
      logger.debug("got " + i1);

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

      userExercise.setUniqueID(id);

      statement.close();
      database.closeConnection(connection);

      logger.debug("now " + getCount(USEREXERCISE) + " and user exercise is " + userExercise);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  void createUserTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USEREXERCISE +
      " (" +
      "uniqueid IDENTITY, " +
      "english VARCHAR, " +
      "foreignLanguage VARCHAR, " +
      "creatorid LONG, " +
      "refAudio VARCHAR, " +
      "slowAudioRef VARCHAR, " +
      "FOREIGN KEY(creatorid) REFERENCES " +
      "USERS" +
      "(ID)" +
      ")");
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  void dropUserTable(Database database) throws Exception {
    System.err.println("----------- dropUserTable -------------------- ");
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement("drop TABLE " + USEREXERCISE);
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  public UserExercise getWhere(String exid) {
    String unique = exid.substring("Custom_".length());
    String sql = "SELECT * from " + USEREXERCISE + " where uniqueid=" + unique + ";";
    try {
      List<UserExercise> userExercises = getUserExercises(sql);
      if (userExercises.isEmpty()) {
        logger.error("huh? no custom exercise with id " + unique);
        return null;
      }
      else return userExercises.iterator().next();
    } catch (SQLException e) {
      logger.error("got " +e,e);
    }                           return null;

  }

  /**
   * Pulls the list of users out of the database.
   *
   * @return
   */
  public List<UserExercise> getAll() {
    try {
      String sql = "SELECT * from " + USEREXERCISE;
      return getUserExercises(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  private List<UserExercise> getUserExercises(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    int i;

    ResultSet rs = statement.executeQuery();
    List<UserExercise> exercises = new ArrayList<UserExercise>();

    while (rs.next()) {
      i = 1;
      exercises.add(new UserExercise(rs.getLong("uniqueid"), //id
        rs.getLong("creatorid"), // age
        rs.getString("english"), // exp
        rs.getString("foreignLanguage"), // exp
        rs.getString("refAudio"), // exp
        rs.getString("slowAudioRef"))
      );
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return exercises;
  }
}