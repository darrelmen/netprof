package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ExerciseDAO;
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
  private ExerciseDAO exerciseDAO;

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
   * @see mitll.langtest.server.database.custom.UserListManager#reallyCreateNewItem(long, mitll.langtest.shared.custom.UserExercise)
   */
  public void add(UserExercise userExercise) {
    long id = 0;

    try {
      // there are much better ways of doing this...
      logger.info("UserExerciseDAO.add :userExercise " + userExercise);

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
        "INSERT INTO " + USEREXERCISE +
          "(exerciseid,english,foreignLanguage,creatorid,refAudio,slowAudioRef) " +
          "VALUES(?,?,?,?,?,?);");
      int i = 1;
      statement.setString(i++, userExercise.getID());
      statement.setString(i++, userExercise.getEnglish());
      statement.setString(i++, userExercise.getForeignLanguage());
      statement.setLong(i++, userExercise.getCreator());

      String refAudio = userExercise.getRefAudio();
      statement.setString(i++, refAudio == null ? "" : refAudio);
      String slowAudioRef = userExercise.getSlowAudioRef();
      String aNull = slowAudioRef == null || slowAudioRef.equals("null") ? "" : slowAudioRef;
      statement.setString(i++, aNull);

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

      // TODO : consider making this an actual prepared statement?
      if (!userExercise.isPredefined()) {     // cheesy!
        String customID = UserExercise.CUSTOM_PREFIX + id;
        String sql = "UPDATE " + USEREXERCISE +
          " " +
          "SET " +
          "exerciseid='" + customID + "' " +
          "WHERE uniqueid=" + userExercise.getUniqueID();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.executeUpdate();
        preparedStatement.close();
        userExercise.setID(customID);

        logger.debug("\tuserExercise= " + userExercise);
      }

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
      "exerciseid VARCHAR, " +
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

  /**
   * @see mitll.langtest.server.database.custom.UserListDAO#populateList
   * @param listID
   * @return
   */
  public Collection<UserExercise> getOnList(long listID) {
    String sql = "SELECT " +
      "ue.* from " + USEREXERCISE + " ue, " + UserListExerciseJoinDAO.USER_EXERCISE_LIST_EXERCISE +" uele "+
      " where ue." +
       "uniqueid" +
      "=uele." +
      "exerciseid" +
      " AND uele.userlistid=" + listID + ";";
    
    try {
      List<UserExercise> userExercises = getUserExercises(sql);
//      logger.debug("\tfound (" +userExercises.size()+ ") userExercises on list " +listID);

      List<UserExercise> userExercises2 = new ArrayList<UserExercise>();

      for (UserExercise ue : userExercises) {
        //logger.debug("\ton list " +listID + " " + ue.getID() + " / " +ue.getUniqueID() + " : " + ue);
        if (ue.isPredefined()) {
          Exercise byID = getExercise(ue);

          if (byID != null) {
            userExercises2.add(new UserExercise(byID)); // all predefined references
          } else logger.error("huh can't find '" + ue.getID() +"'");
        }
        else {
          userExercises2.add(ue);
        }
      }

      if (userExercises2.isEmpty()) {
        logger.info("getOnList : no exercises on list id " + listID);
        return new ArrayList<UserExercise>();
      } else {
        logger.debug("\tgetOnList for " + listID+ "  got " + userExercises2.size());
        return userExercises2;
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return new ArrayList<UserExercise>();
  }

  private Exercise getExercise(UserExercise ue) {
    String id = ue.getID();
    return getExercise(id);
  }

  Exercise getExercise(String id) {
    return exerciseDAO.getExercise(id);
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getUserExerciseWhere(String)
   * @param exid
   * @return
   */
  public UserExercise getWhere(String exid) {
    String sql = "SELECT * from " + USEREXERCISE + " where exerciseid='" + exid + "'";
    try {
      List<UserExercise> userExercises = getUserExercises(sql);
      if (userExercises.isEmpty()) {
        logger.error("getWhere : huh? no custom exercise with id " + exid);
        return null;
      } else return userExercises.iterator().next();
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  public List<UserExercise> getWhere(Collection<String> exids) {
    if (exids.isEmpty()) return new ArrayList<UserExercise>();
    StringBuilder builder = new StringBuilder();
    for (String id : exids) builder.append("'"+id+"'").append(",");
    String s = builder.toString();
    s = s.substring(0,s.length()-1);
    String sql = "SELECT * from " + USEREXERCISE + " where exerciseid in (" + s+ ")";
    try {
      List<UserExercise> userExercises = getUserExercises(sql);
      if (userExercises.isEmpty()) {
        logger.warn("getWhere : no user exercises in " + exids);
      }
      return userExercises;
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
/*  public List<UserExercise> getAll() {
    try {
      String sql = "SELECT * from " + USEREXERCISE;
      return getUserExercises(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }*/

  private List<UserExercise> getUserExercises(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    //logger.debug("getUserExercises sql = " + sql);
    ResultSet rs = statement.executeQuery();
    List<UserExercise> exercises = new ArrayList<UserExercise>();

    while (rs.next()) {
      UserExercise e = new UserExercise(rs.getLong("uniqueid"), //id
        rs.getString("exerciseid"), rs.getLong("creatorid"), // age
        rs.getString("english"), // exp
        rs.getString("foreignLanguage"), // exp
        rs.getString("refAudio"), // exp
        rs.getString("slowAudioRef"));
      exercises.add(e);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return exercises;
  }

  public void update(UserExercise userExercise) {
    try {
      Connection connection = database.getConnection();

      // TODO : consider making this an actual prepared statement?

      String sql = "UPDATE " + USEREXERCISE +
        " " +
        "SET " +
        "english='" + userExercise.getEnglish() + "', " +
        "foreignLanguage='" + userExercise.getForeignLanguage() + "', " +
        "refAudio='" + userExercise.getRefAudio() + "', " +
        "slowAudioRef='" + userExercise.getSlowAudioRef() + "' " +
        "WHERE uniqueid=" + userExercise.getUniqueID();

      PreparedStatement statement = connection.prepareStatement(sql);
      int i = statement.executeUpdate();

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