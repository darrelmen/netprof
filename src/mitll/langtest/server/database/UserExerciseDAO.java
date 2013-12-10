package mitll.langtest.server.database;

import mitll.langtest.server.database.custom.UserListExerciseJoinDAO;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.custom.UserExercise;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserExerciseDAO extends DAO {
  private static Logger logger = Logger.getLogger(UserExerciseDAO.class);

  public static final String USEREXERCISE = "userexercise";
  UserListExerciseJoinDAO userListExerciseJoinDAO;
  private ExerciseDAO exerciseDAO;

  public UserExerciseDAO(Database database, UserListExerciseJoinDAO userListExerciseJoinDAO) {
    super(database);
    try {
      createUserTable(database);
      this.userListExerciseJoinDAO = userListExerciseJoinDAO;
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
      logger.info("\n\n\nadd :userExercise " + userExercise);

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

/*  void dropUserTable(Database database) throws Exception {
    System.err.println("----------- dropUserTable -------------------- ");
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement("drop TABLE " + USEREXERCISE);
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }*/

  /**
   * @see mitll.langtest.server.database.custom.UserListDAO#populateList
   * @param listID
   * @param db
   * @return
   */
  public Collection<UserExercise> getOnList(long listID) {
    String sql = "SELECT " +
      "ue.* from " + USEREXERCISE + " ue, " + UserListExerciseJoinDAO.USER_EXERCISE_LIST_EXERCISE +" uele "+
      " where ue.uniqueid=uele.exerciseid AND uele.userlistid=" + listID + ";";
    
    // TODO fix join - don't do uniqueid, do exercise id so we can do mixed lists
    try {
      List<UserExercise> userExercises = getUserExercises(sql);
      Set<String> ids = new HashSet<String>();
      for (UserExercise ue : userExercises) ids.add(ue.getID());

      List<String> allFor = userListExerciseJoinDAO.getAllFor(listID, ids);
      logger.debug("all ids " + allFor);
      logger.debug("userExercises " + userExercises);

      for (String id : allFor) {
        Exercise byID = exerciseDAO.getExercise(id);

        if (byID != null) {
          userExercises.add(new UserExercise(byID.getShell())); // all predefined references
        } else logger.error("huh can't find  " + id);
      }
      logger.debug("userExercises " + userExercises);

      if (userExercises.isEmpty()) {
        logger.info("getOnList : no exercises on list id " + listID);
        return new ArrayList<UserExercise>();
      } else {
        logger.debug("getOnList got " + userExercises);
        return userExercises;
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return new ArrayList<UserExercise>();
  }

  /**
   * @see DatabaseImpl#getUserExerciseWhere(String)
   * @param exid
   * @return
   */
  public UserExercise getWhere(String exid) {
    String unique = exid.substring("Custom_".length());
    String sql = "SELECT * from " + USEREXERCISE + " where uniqueid=" + unique + ";";
    try {
      List<UserExercise> userExercises = getUserExercises(sql);
      if (userExercises.isEmpty()) {
        logger.error("getWhere : huh? no custom exercise with id " + unique);
        return null;
      } else return userExercises.iterator().next();
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
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
    ResultSet rs = statement.executeQuery();
    List<UserExercise> exercises = new ArrayList<UserExercise>();

    while (rs.next()) {
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

  public void update(UserExercise userExercise) {
    try {
      Connection connection = database.getConnection();

      String sql = "UPDATE " + USEREXERCISE +
        " " +
        "SET " +
        "english='" + userExercise.getEnglish() + "', " +
        "foreignLanguage='" + userExercise.getForeignLanguage() + "', " +
        "refAudio='" + userExercise.getRefAudio() + "', " +
        "slowAudioRef='" + userExercise.getSlowAudioRef() + "' " +
        "WHERE uniqueid=" + userExercise.getUniqueID();
 /*       if (false) {
          logger.debug("update " + id + " score " +score);
        }*/
      PreparedStatement statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (false) logger.debug("UPDATE " + i);
      if (i == 0) {
        logger.error("huh? didn't update the userExercise for " + userExercise + " sql " + sql);
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public void setExerciseDAO(ExerciseDAO exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }
}