package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ExerciseDAO;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.custom.UserExercise;
import org.apache.log4j.Logger;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserExerciseDAO extends DAO {
  private static Logger logger = Logger.getLogger(UserExerciseDAO.class);

  private static final String EXERCISEID = "exerciseid";
  private static final String TRANSLITERATION = "transliteration";
  private static final String OVERRIDE = "override";
  private static final String UNIT = "unit";
  private static final String LESSON = "lesson";

  public static final String USEREXERCISE = "userexercise";
  private ExerciseDAO exerciseDAO;
  private static final boolean DEBUG = false;

  public UserExerciseDAO(Database database) {
    super(database);
    try {
      createUserTable(database);

      Collection<String> columns = getColumns(USEREXERCISE);
      Connection connection = database.getConnection();
      if (!columns.contains(TRANSLITERATION)) {
        addColumnToTable(connection);
      }
      if (!columns.contains(OVERRIDE)) {
        addColumnToTable2(connection);
      }
      if (!columns.contains(EXERCISEID)) {
        addExerciseIDColumnToTable(connection);
      }
      if (!columns.contains(UNIT)) {
        addColumn(connection, UNIT);
      }
      if (!columns.contains(LESSON)) {
        addColumn(connection, LESSON);
      }
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
   * @see #update(mitll.langtest.shared.custom.UserExercise, boolean)
   */
  public void add(UserExercise userExercise, boolean isOverride) {
    long id = 0;

    List<String> typeOrder = exerciseDAO.getSectionHelper().getTypeOrder();

    try {
      // there are much better ways of doing this...
      logger.info("UserExerciseDAO.add : userExercise " + userExercise);

      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + USEREXERCISE +
          "(" +
            EXERCISEID +
            ",english,foreignLanguage," + TRANSLITERATION + ",creatorid,refAudio,slowAudioRef,override," + UNIT+","+LESSON+
          ") " +
          "VALUES(?,?,?,?,?,?,?,?,?,?);");
      int i = 1;
      statement.setString(i++, userExercise.getID());
      statement.setString(i++, fixSingleQuote(userExercise.getEnglish()));
      statement.setString(i++, fixSingleQuote(userExercise.getForeignLanguage()));
      statement.setString(i++, fixSingleQuote(userExercise.getTransliteration()));
      statement.setLong(i++, userExercise.getCreator());

      String refAudio = userExercise.getRefAudio();
      statement.setString(i++, refAudio == null ? "" : refAudio);
      String slowAudioRef = userExercise.getSlowAudioRef();
      String slowRefNullCheck = slowAudioRef == null || slowAudioRef.equals("null") ? "" : slowAudioRef;
      statement.setString(i++, slowRefNullCheck);
      statement.setBoolean(i++, isOverride);

      Map<String,String> unitToValue = userExercise.getUnitToValue();

      logger.warn("type order " + typeOrder + " and " +unitToValue);

      if (typeOrder.size() > 0) {
        String s = typeOrder.get(0);
        statement.setString(i++, unitToValue.containsKey(s) ? unitToValue.get(s) : "");
      }

      if (typeOrder.size() > 1) {
        String s = typeOrder.get(1);
        statement.setString(i++, unitToValue.containsKey(s) ? unitToValue.get(s) : "");
      }

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      if (rs.next()) {
        id = rs.getLong(1);
      } else {
        logger.error("huh? no key was generated?");
      }
      //logger.debug("unique id = " + id);

      userExercise.setUniqueID(id);

      // TODO : consider making this an actual prepared statement?
      if (!userExercise.isPredefined()) {     // cheesy!
        String customID = UserExercise.CUSTOM_PREFIX + id;
        String sql = "UPDATE " + USEREXERCISE +
          " " +
          "SET " +
            EXERCISEID +
            "='" + customID + "' " +
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

  private String fixSingleQuote(String s) {
    return s.replaceAll("'","''");
  }

  void createUserTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USEREXERCISE +
      " (" +
      "uniqueid IDENTITY, " +
        EXERCISEID +" VARCHAR, " +
      "english VARCHAR, " +
      "foreignLanguage VARCHAR, " +
      TRANSLITERATION + " VARCHAR, " +
      "creatorid LONG, " +
      "refAudio VARCHAR, " +
      "slowAudioRef VARCHAR, " +
      "override" +
      " BOOLEAN, " +
      UNIT +
      " VARCHAR, " +
      LESSON +
      " VARCHAR, " +
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
    String sql = getJoin(listID);
    
    try {
      if (DEBUG) logger.debug("\tusing for user exercise = " +sql);

      List<UserExercise> userExercises = getUserExercises(sql);
      if (DEBUG) logger.debug("\tfound " +userExercises.size()+ " exercises userExercises on list " +listID);

      List<UserExercise> userExercises2 = new ArrayList<UserExercise>();

      for (UserExercise ue : userExercises) {
        if (DEBUG) logger.debug("\ton list " +listID + " " + ue.getID() + " / " +ue.getUniqueID() + " : " + ue);
        if (ue.isPredefined()) {
          Exercise byID = getExercise(ue);

          if (byID != null) {
            userExercises2.add(new UserExercise(byID)); // all predefined references
          } else {
            logger.error("getOnList: huh can't find '" + ue.getID() +"'");
          }
        }
        else {
          userExercises2.add(ue);
        }
      }

      String join2 = getJoin2(listID);
      if (DEBUG) logger.debug("\tusing exercise = " +join2);
      for (String exid : getExercises(join2)) {
        Exercise exercise = exerciseDAO.getExercise(exid);
        if (exercise != null) {
          userExercises2.add(new UserExercise(exercise));
        }
        else logger.info("can't find exercise " + exid);
      }
      if (userExercises2.isEmpty()) {
        if (DEBUG) logger.debug("\tgetOnList : no exercises on list id " + listID);
        return new ArrayList<UserExercise>();
      } else {
        if (DEBUG) logger.debug("\tgetOnList for " + listID+ "  got " + userExercises2.size());
        return userExercises2;
      }

    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return new ArrayList<UserExercise>();
  }

  private String getJoin(long listID) {
    return "SELECT " +
        "ue.* from " + USEREXERCISE + " ue, " + UserListExerciseJoinDAO.USER_EXERCISE_LIST_EXERCISE +" uele "+
        " where ue." +
        EXERCISEID +
        "=" +
        "uele." +
        EXERCISEID +
        " AND uele.userlistid=" + listID + ";";
  }

  private String getJoin2(long listID) {
    return "SELECT uele.exerciseid " +
        "FROM " +
        UserListExerciseJoinDAO.USER_EXERCISE_LIST_EXERCISE  +
        " uele " +
        "LEFT JOIN " +
        USEREXERCISE +
        " ue ON ue.exerciseid = uele.exerciseid" +
        " AND uele.userlistid=" +
        listID +
        "    WHERE ue." +
        EXERCISEID +
        " IS NULL and uele.userlistid=" +
        listID;
  }

  private Exercise getExercise(UserExercise ue) { return getExercise(ue.getID());  }

  /**
   * @see UserListManager#getReviewedExercises()
   * @param id
   * @return
   */
  Exercise getExercise(String id) { return exerciseDAO.getExercise(id); }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getUserExerciseWhere(String)
   * @param exid
   * @return
   */
  public UserExercise getWhere(String exid) {
    String sql = "SELECT * from " + USEREXERCISE + " where " +
        EXERCISEID +
        "='" + exid + "'";
    try {
      List<UserExercise> userExercises = getUserExercises(sql);
      if (userExercises.isEmpty()) {
        //logger.debug("getWhere : no custom exercise with id " + exid);
        return null;
      } else return userExercises.iterator().next();
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  public Collection<UserExercise> getOverrides() {
    String sql = "SELECT * from " + USEREXERCISE + " where override=true";
    try {
      return getUserExercises(sql);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  /**
   * @see UserListManager#getReviewList()
   * @param exids
   * @return
   */
  List<UserExercise> getWhere(Collection<String> exids) {
    if (exids.isEmpty()) return new ArrayList<UserExercise>();
    StringBuilder builder = new StringBuilder();
    for (String id : exids) builder.append("'"+id+"'").append(",");
    String s = builder.toString();
    s = s.substring(0,s.length()-1);
    String sql = "SELECT * from " + USEREXERCISE + " where " +
        EXERCISEID +
        " in (" + s+ ")";
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

  private List<UserExercise> getUserExercises(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    //logger.debug("getUserExercises sql = " + sql);
    ResultSet rs = statement.executeQuery();
    List<UserExercise> exercises = new ArrayList<UserExercise>();

    List<String> typeOrder = exerciseDAO.getSectionHelper().getTypeOrder();
    while (rs.next()) {
      String first = rs.getString(UNIT);
      String second = rs.getString(LESSON);
      Map<String,String> unitToValue = new HashMap<String, String>();
      if (typeOrder.size() > 0) {
        String s = typeOrder.get(0);
        unitToValue.put(s,first);
      }

      if (typeOrder.size() > 1) {
        String s = typeOrder.get(1);
        unitToValue.put(s, second);
      }

      UserExercise e = new UserExercise(
        rs.getLong("uniqueid"), //id
        rs.getString(EXERCISEID),
        rs.getLong("creatorid"),
        rs.getString("english"),
        rs.getString("foreignLanguage"),
        rs.getString(TRANSLITERATION),
        rs.getString("refAudio"),
        rs.getString("slowAudioRef"),
        rs.getBoolean(OVERRIDE),
        unitToValue);

      exercises.add(e);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return exercises;
  }

  private List<String> getExercises(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<String> exercises = new ArrayList<String>();

    while (rs.next()) {
      exercises.add(rs.getString(EXERCISEID));
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return exercises;
  }

  /**
   * @see UserListManager#editItem(mitll.langtest.shared.custom.UserExercise, boolean)
   * @param userExercise
   * @param createIfDoesntExist
   */
  public void update(UserExercise userExercise, boolean createIfDoesntExist) {
    try {
      Connection connection = database.getConnection();

      // TODO : consider making this an actual prepared statement?

      String sql = "UPDATE " + USEREXERCISE +
        " " +
        "SET " +
        "english='" + fixSingleQuote(userExercise.getEnglish()) + "', " +
        "foreignLanguage='" + fixSingleQuote(userExercise.getForeignLanguage()) + "', " +
        TRANSLITERATION + "='" + fixSingleQuote(userExercise.getTransliteration()) + "', " +
        "refAudio='" + userExercise.getRefAudio() + "', " +
        "slowAudioRef='" + userExercise.getSlowAudioRef() + "' " +
        "WHERE " +
          EXERCISEID +
          "='" + userExercise.getID() +"'";

      PreparedStatement statement = connection.prepareStatement(sql);
      int i = statement.executeUpdate();

      if (i == 0) {
        if (createIfDoesntExist) {
          add(userExercise, true);
        }
        else {
          logger.error("huh? didn't update the userExercise for " + userExercise + "\n\tsql " + sql);
        }
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  private void addColumnToTable(Connection connection) throws SQLException {
    addColumn(connection, TRANSLITERATION);
  }

  private void addExerciseIDColumnToTable(Connection connection) throws SQLException {
    String exerciseid = EXERCISEID;
    addColumn(connection, exerciseid);
  }

  private void addColumn(Connection connection, String exerciseid) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
        USEREXERCISE + " ADD " + exerciseid + " VARCHAR");
    statement.execute();
    statement.close();
  }

  private void addColumnToTable2(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
      USEREXERCISE +" ADD " + OVERRIDE + " BOOLEAN");
    statement.execute();
    statement.close();
  }

  public void setExerciseDAO(ExerciseDAO exerciseDAO) {

    this.exerciseDAO = exerciseDAO;
    // exerciseDAO.getSectionHelper().getTypeOrder();
  }
}