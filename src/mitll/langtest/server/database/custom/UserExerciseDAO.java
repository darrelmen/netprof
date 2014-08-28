package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.AudioDAO;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ExerciseDAO;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.custom.UserExercise;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserExerciseDAO extends DAO {
  private static final Logger logger = Logger.getLogger(UserExerciseDAO.class);

  private static final String EXERCISEID = "exerciseid";
  private static final String TRANSLITERATION = "transliteration";
  private static final String OVERRIDE = "override";
  private static final String UNIT = "unit";
  private static final String LESSON = "lesson";

  private static final String USEREXERCISE = "userexercise";
  private static final String MODIFIED = "modified";
  private static final String STATE = "state";
  private static final String REF_AUDIO = "refAudio";
  private static final String SLOW_AUDIO_REF = "slowAudioRef";

  // ?? when would this be used?
  //private static final boolean ADD_MISSING_AUDIO = false;

  private ExerciseDAO exerciseDAO;
  private static final boolean DEBUG = false;

  public UserExerciseDAO(Database database) {
    super(database);
    try {
      createUserTable(database);
      Collection<String> columns = getColumns(USEREXERCISE);
      Connection connection = database.getConnection(this.getClass().toString());
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
        addVarchar(connection, USEREXERCISE, UNIT);
      }
      if (!columns.contains(LESSON)) {
        addVarchar(connection, USEREXERCISE, LESSON);
      }
      if (!columns.contains(MODIFIED)) {
        addColumnToTable3(connection);
      }
      if (!columns.contains(STATE)) {
        addColumnToTable4(connection);
      }
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see UserListManager#reallyCreateNewItem(long, mitll.langtest.shared.custom.UserExercise, String)
   * @see #update(mitll.langtest.shared.custom.UserExercise, boolean)
   */
  public void add(UserExercise userExercise, boolean isOverride) {
    long id = 0;

    List<String> typeOrder = exerciseDAO.getSectionHelper().getTypeOrder();

    try {
      // there are much better ways of doing this...
     // logger.debug("UserExerciseDAO.add : userExercise " + userExercise);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + USEREXERCISE +
          "(" +
            EXERCISEID +
            ",english,foreignLanguage," + TRANSLITERATION + ",creatorid,refAudio,slowAudioRef,override," + UNIT+
          ","+LESSON+
          ","+MODIFIED+
         // ","+STATE+
          ") " +
          "VALUES(?,?,?,?,?,?,?,?,?,?,?" +
          //",?" +
          ")");
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

      if (typeOrder.size() > 0) {
        String s = typeOrder.get(0);
        String x = unitToValue.containsKey(s) ? unitToValue.get(s) : "";
        statement.setString(i++, x);
      } else {
        statement.setString(i++, "");
      }

      if (typeOrder.size() > 1) {
        String s = typeOrder.get(1);
        statement.setString(i++, unitToValue.containsKey(s) ? unitToValue.get(s) : "");
      } else {
        statement.setString(i++, "");
      }
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
     // statement.setString(i++,userExercise.getState());

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
      boolean predefined = userExercise.isPredefined();
      if (!predefined) {     // cheesy!
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

        //logger.debug("\tuserExercise= " + userExercise);
      }

      finish(connection, statement);

    //  logger.debug("now " + getCount(USEREXERCISE) + " user exercises and user exercise is " + userExercise);
      logger.debug("new " + (predefined ? " PREDEF " : " USER ") + " user exercise is " + userExercise);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  private String fixSingleQuote(String s) { return s.replaceAll("'","''"); }

  void createUserTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USEREXERCISE +
      " (" +
      "uniqueid IDENTITY, " +
        EXERCISEID +" VARCHAR, " +
      "english VARCHAR, " +
      "foreignLanguage VARCHAR, " +
      TRANSLITERATION + " VARCHAR, " +
      "creatorid INT, " +
      REF_AUDIO +
      " VARCHAR, " +
      SLOW_AUDIO_REF +
      " VARCHAR, " +
      "override" +
      " BOOLEAN, " +
      UNIT +
      " VARCHAR, " +
      LESSON +
      " VARCHAR, " +
      "modified TIMESTAMP, " +
      STATE+" VARCHAR, " +
      "FOREIGN KEY(creatorid) REFERENCES " +
      "USERS" +
      "(ID)" +
      ")");
    finish(database, connection, statement);

    index(database);
  }

  void index(Database database) throws SQLException {
    createIndex(database,EXERCISEID,USEREXERCISE);
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListDAO#populateList
   * @param listID
   * @return
   */
  public List<CommonUserExercise> getOnList(long listID) {
    String sql = getJoin(listID);
    
    try {
      if (DEBUG) logger.debug("\tusing for user exercise = " +sql);

      List<CommonUserExercise> userExercises = getUserExercises(sql, false);
      if (DEBUG) logger.debug("\tfound " +userExercises.size()+ " exercises userExercises on list " +listID);

      List<CommonUserExercise> userExercises2 = new ArrayList<CommonUserExercise>();

      for (CommonUserExercise ue : userExercises) {
        if (DEBUG) logger.debug("\ton list " +listID + " " + ue.getID() + " / " +ue.getUniqueID() + " : " + ue);
        if (ue.isPredefined()) {
          CommonExercise byID = getExercise(ue);

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
        CommonExercise exercise = getPredefExercise(exid);
        if (exercise != null) {
          userExercises2.add(new UserExercise(exercise));
        }
        else {
          //logger.info("can't find exercise " + exid);
        }
      }
      if (userExercises2.isEmpty()) {
        if (DEBUG) logger.debug("\tgetOnList : no exercises on list id " + listID);
        return new ArrayList<CommonUserExercise>();
      } else {
        if (DEBUG) logger.debug("\tgetOnList for " + listID+ "  got " + userExercises2.size());
        return userExercises2;
      }

    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return new ArrayList<CommonUserExercise>();
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

  /**
   * @see #getOnList(long)
   * @param ue
   * @return
   */
  private CommonExercise getExercise(CommonShell ue) { return getPredefExercise(ue.getID());  }

  /**
   * @see UserListManager#getReviewedUserExercises(java.util.Map, java.util.Collection)
   * @param id
   * @return
   */
  CommonExercise getPredefExercise(String id) { return exerciseDAO.getExercise(id); }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getUserExerciseWhere(String)
   * @param exid
   * @return
   */
  public CommonUserExercise getWhere(String exid) {
    String sql = "SELECT * from " + USEREXERCISE + " where " +  EXERCISEID + "='" + exid + "'";
    try {
      List<CommonUserExercise> userExercises = getUserExercises(sql, false);
      if (userExercises.isEmpty()) {
        //logger.debug("getWhere : no custom exercise with id " + exid);
        return null;
      } else {
        return userExercises.iterator().next();
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  /**
   * @see mitll.langtest.server.database.ExcelImport#getRawExercises()
   * @see #setAudioDAO(mitll.langtest.server.database.AudioDAO)
   * @param addMissingAudio always false
   * @return
   */
  public Collection<CommonUserExercise> getOverrides(boolean addMissingAudio) {
    String sql = "SELECT * from " + USEREXERCISE + " where override=true";
    try {
      return getUserExercises(sql, addMissingAudio);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  /**
   * @see UserListManager#getDefectList(java.util.Collection)
   * @param exids
   * @return
   */
  List<CommonUserExercise> getWhere(Collection<String> exids) {
    if (exids.isEmpty()) return new ArrayList<CommonUserExercise>();
    String s = getIds(exids);
    String sql = "SELECT * from " + USEREXERCISE + " where " +
        EXERCISEID +
        " in (" + s+ ")";
    try {
      List<CommonUserExercise> userExercises = getUserExercises(sql, false);
      if (userExercises.isEmpty()) {
        logger.warn("getVisitorsOfList : no user exercises in " + exids.size() + " exercise ids");
      }
      return userExercises;
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  private String getIds(Collection<String> exids) {
    StringBuilder builder = new StringBuilder();
    for (String id : exids) builder.append("'"+id+"'").append(",");
    String s = builder.toString();
    s = s.substring(0,s.length()-1);
    return s;
  }

  /**
   * @see #getOnList(long)
   * @see #getOverrides(boolean)
   * @see #getWhere(java.util.Collection)
   * @see #getWhere(java.lang.String)
   * @param sql
   * @param addMissingAudio always false
   * @return user exercises without annotations
   * @throws SQLException
   */
  private List<CommonUserExercise> getUserExercises(String sql, boolean addMissingAudio) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    //logger.debug("getUserExercises sql = " + sql);
    ResultSet rs = statement.executeQuery();
    List<CommonUserExercise> exercises = new ArrayList<CommonUserExercise>();

    List<String> typeOrder = exerciseDAO.getSectionHelper().getTypeOrder();
    while (rs.next()) {
      Map<String, String> unitToValue = getUnitToValue(rs, typeOrder);

      Timestamp timestamp = rs.getTimestamp(MODIFIED);

      Date date = (timestamp != null) ? new Date(timestamp.getTime()) : new Date(0);

      UserExercise e = new UserExercise(
        rs.getLong("uniqueid"),
        rs.getString(EXERCISEID),
        rs.getLong("creatorid"),
        rs.getString("english"),
        rs.getString("foreignLanguage"),
        rs.getString(TRANSLITERATION),
        "",         // TODO complete fill in of context!
        rs.getBoolean(OVERRIDE),
        unitToValue,
        date
      );

      if (addMissingAudio) {
        String ref  = rs.getString(REF_AUDIO);
        String sref = rs.getString(SLOW_AUDIO_REF);
        addMissingAudio(e, ref, sref);
      }
      exercises.add(e);
    }
    finish(connection, statement, rs);

    return exercises;
  }

  /**
   * @see #getUserExercises(String, boolean)
   * @param e
   * @param ref
   * @param sref
   */
  private void addMissingAudio(UserExercise e, String ref, String sref) {
    boolean hasRef = (ref != null && !ref.isEmpty());
    boolean hasSRef = (sref != null && !sref.isEmpty());

    boolean foundReg = false;
    boolean foundSlow = false;

    List<AudioAttribute> audioAttributes = exToAudio.get(e.getID());
    if (audioAttributes != null) {
      for (AudioAttribute attribute : audioAttributes) {
        if (attribute.getUserid() == e.getCreator()) {
          if ((attribute.getAudioType().equalsIgnoreCase(Result.AUDIO_TYPE_REGULAR) && hasRef)) {
            foundReg = true;
          }
        }
        if (attribute.getAudioType().equalsIgnoreCase(Result.AUDIO_TYPE_SLOW) && hasSRef) {
          foundSlow = true;
        }
      }
    }

    long time = e.getModifiedDate().getTime();
    if (time == 0) time = System.currentTimeMillis();
    if (!foundReg && hasRef) {
      audioDAO.add((int) e.getCreator(), ref, e.getID(), time, Result.AUDIO_TYPE_REGULAR, 0);
      logger.debug("adding missing reg  audio ref -- only first time " + ref + " by " + e.getCreator());
    }
    if (!foundSlow && hasSRef) {
      audioDAO.add((int) e.getCreator(), sref, e.getID(), time, Result.AUDIO_TYPE_SLOW, 0);
      logger.debug("adding missing slow audio ref -- only first time " + ref + " by " + e.getCreator());

    }
  }

  private Map<String, List<AudioAttribute>> exToAudio;
  private AudioDAO audioDAO;

  public void setAudioDAO(AudioDAO audioDAO) {
    exToAudio = audioDAO.getExToAudio();
    this.audioDAO = audioDAO;

/*    if (ADD_MISSING_AUDIO) {
      getOverrides(true);
    }*/
  }

  private Map<String, String> getUnitToValue(ResultSet rs, List<String> typeOrder) throws SQLException {
    String first = rs.getString(UNIT);
    String second = rs.getString(LESSON);
    Map<String,String> unitToValue = new HashMap<String, String>();

    if (typeOrder.size() > 0 && first != null) {
      String s = typeOrder.get(0);
      if (!first.isEmpty()) unitToValue.put(s,first);
    }

    if (typeOrder.size() > 1 && second != null) {
      String s = typeOrder.get(1);
      if (!second.isEmpty()) unitToValue.put(s, second);
    }
    return unitToValue;
  }

  private List<String> getExercises(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<String> exercises = new ArrayList<String>();

    while (rs.next()) {
      exercises.add(rs.getString(EXERCISEID));
    }
    finish(connection, statement, rs);

    return exercises;
  }


  /**
   * @see UserListManager#editItem(mitll.langtest.shared.custom.UserExercise, boolean, String)
   * @param id
   * @param state
   */
/*  public void updateState(String id, String state) {
    try {
      Connection connection = database.getConnection();

      String sql = "UPDATE " + USEREXERCISE +
        " " +
        "SET " +
        STATE +
        "=? " +
        "WHERE " +
        EXERCISEID +
        "=?"
        ;
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, state);
      statement.setString(ii++, id);
      int i = statement.executeUpdate();
      if (i == 0) {
        logger.error("huh? no row updated for " + id);
      }
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }*/
/*
  public Set<String> getAtState(String state) {
    try {
      Connection connection = database.getConnection();

      String sql = "SELECT " + EXERCISEID + " from " + USEREXERCISE +
        "WHERE " +
        STATE +
        "=?";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, state);

      ResultSet rs = statement.executeQuery();
      Set<String> exercises = new HashSet<String>();

      while (rs.next()) {
        exercises.add(rs.getString(1));
      }
      statement.close();
      database.closeConnection(connection);

      return exercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptySet();
  }*/

  /**
   * @see UserListManager#editItem(mitll.langtest.shared.custom.UserExercise, boolean, String)
   * @param userExercise
   * @param createIfDoesntExist
   */
  public void update(UserExercise userExercise, boolean createIfDoesntExist) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + USEREXERCISE +
        " " +
        "SET " +
        "english=?,foreignLanguage=?," + TRANSLITERATION + "=?,refAudio=?,slowAudioRef=?," + MODIFIED +
        "=? " +
        "WHERE " +
        EXERCISEID +
        "=?"
       ;
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, userExercise.getEnglish());
      statement.setString(ii++, userExercise.getForeignLanguage());
      statement.setString(ii++, userExercise.getTransliteration());
      statement.setString(ii++, userExercise.getRefAudio());
      statement.setString(ii++, userExercise.getSlowAudioRef());
      statement.setTimestamp(ii++, new Timestamp(System.currentTimeMillis()));
      statement.setString(ii++, userExercise.getID());

      int i = statement.executeUpdate();

      if (i == 0) {
        if (createIfDoesntExist) {
          add(userExercise, true);
        }
        else {
          logger.error("huh? didn't update the userExercise for " + userExercise + "\n\tsql " + sql);
        }
      }

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  private void addColumnToTable(Connection connection) throws SQLException {
    addVarchar(connection, USEREXERCISE, TRANSLITERATION);
  }

  private void addExerciseIDColumnToTable(Connection connection) throws SQLException {
    addVarchar(connection, USEREXERCISE, EXERCISEID);
  }

  private void addColumnToTable2(Connection connection) throws SQLException {
    addBoolean(connection, USEREXERCISE, OVERRIDE);
  }

  private void addColumnToTable3(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
      USEREXERCISE +" ADD " + MODIFIED + " TIMESTAMP ");
    statement.execute();
    statement.close();
  }

  private void addColumnToTable4(Connection connection) throws SQLException {
    addVarchar(connection, USEREXERCISE, STATE);
  }

  public void setExerciseDAO(ExerciseDAO exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }
}