/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.copy.VocabFactory;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.database.userlist.UserListDAO;
import mitll.langtest.server.database.userlist.UserListExerciseJoinDAO;
import mitll.langtest.shared.exercise.*;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickUpdateDominoPair;
import mitll.npdata.dao.userexercise.ExerciseDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 * @deprecated
 */
public class UserExerciseDAO extends BaseUserExerciseDAO implements IUserExerciseDAO {
  private static final Logger logger = LogManager.getLogger(UserExerciseDAO.class);

  private static final String EXERCISEID = "exerciseid";
  private static final String TRANSLITERATION = "transliteration";
  private static final String OVERRIDE = "override";

  @Override
  public IRelatedExercise getRelatedCoreExercise() {
    return null;
  }

  @Override
  public IRelatedExercise getRelatedExercise() {
    return null;
  }

  @Override
  public BothMaps getOldToNew(int projectid) {
    return null;
  }

  @Override
  public Map<Integer, String> getIDToFL(int projid) {
    return null;
  }

/*  @Override
  public IRefResultDAO getRefResultDAO() {
    return null;
  }*/

  @Override
  public boolean updateProjectChinese(int old, int newprojid, List<Integer> justTheseIDs) {
    return false;
  }

  @Override
  public SlickExercise toSlick(Exercise shared, int projectID, Collection<String> typeOrder) {
    return null;
  }

  @Override
  public SlickExercise toSlick(CommonExercise shared, int projectID, int importUserIfNotSpecified, boolean isContext, Collection<String> typeOrder) {
    return null;
  }

  @Override
  public void addBulk(List<SlickExercise> bulk) {
  }

//  @Override
//  public int getAndRememberNumPhones(IPronunciationLookup lookup, int exid, String foreignlanguage, String transliteration) {
//    return 0;
//  }

  @Override
  public int insert(SlickExercise UserExercise) {
    return 0;
  }

  @Override
  public List<CommonExercise> getByProject(List<String> typeOrder, ISection<CommonExercise> sectionHelper, Project theProject, Map<Integer, ExerciseAttribute> allByProject, Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs, boolean isPredef) {
    return null;
  }

  @Override
  public List<CommonExercise> getContextByProject(List<String> typeOrder, ISection<CommonExercise> sectionHelper, Project lookup, Map<Integer, ExerciseAttribute> allByProject, Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs, boolean isPredef) {
    return null;
  }

  @Override
  public List<Integer> getUserDefinedByProjectExactMatch(int projid, int creator, String fl) {
    return null;
  }

  @Override
  public ExerciseDAOWrapper getDao() {
    return null;
  }

  @Override
  public Map<Integer, SlickExercise> getDominoToSlickEx(int projectid) {
    return null;
  }

  @Override
  public IAttributeJoin getExerciseAttributeJoin() {
    return null;
  }

  @Override
  public boolean isProjectEmpty(int projectid) {
    return false;
  }

  private static final String UNIT = "unit";

  @Override
  public IAttribute getExerciseAttribute() {
    return null;
  }

  private static final String LESSON = "lesson";

  @Override
  public SlickExercise getByDominoID(int projID, int docID) {
    return null;
  }

  @Override
  public Map<Integer, Integer> getDominoIDToExID(int docID) {
    return null;
  }

  @Override
  public boolean areThereAnyUnmatched(int projID) {
    return false;
  }

  @Override
  public Map<String, Integer> getNpToExID(int projid) {
    return null;
  }

  @Override
  public int updateDominoBulk(List<SlickUpdateDominoPair> pairs) {
    return 0;
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }

  private static final String USEREXERCISE = "userexercise";
  private static final String GET_ALL_SQL = "SELECT * FROM " + USEREXERCISE;
  private static final String MODIFIED = "modified";
  private static final String STATE = "state";
  private static final String CONTEXT = "context";


  private static final String CONTEXT_TRANSLATION = "contextTranslation";
  private static final String CUSTOM_PREFIX = "Custom_";

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
      if (!columns.contains(CONTEXT)) {
        addColumnToTable5(connection);
      }
      if (!columns.contains(CONTEXT_TRANSLATION.toLowerCase())) {
        addColumnToTable6(connection);
      }
      database.closeConnection(connection);
    } catch (SQLException e) {
      logException(e);
    }
  }

  public SlickExercise getUnknownExercise() {
    return null;
  }

  /**
   * TODO : Consider how to add multiple context sentences?
   * <p>
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p>
   * Uses return generated keys to get the user id
   *
   * @seex UserListManager#newExercise
   * @see IUserExerciseDAO#update
   */
  public int add(CommonExercise userExercise, boolean isContext, Collection<String> typeOrder) {
    // List<String> typeOrder = exerciseDAO.getSectionHelper().getTypeOrder();
    try {
      // there are much better ways of doing this...
      logger.debug("UserExerciseDAO.add : userExercise " + userExercise);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + USEREXERCISE +
              "(" +
              EXERCISEID + "," +
              "english" + "," +
              "foreignLanguage" + "," +
              TRANSLITERATION + "," +
              "creatorid" + "," +
              CONTEXT + "," + CONTEXT_TRANSLATION +
              ",override," + UNIT +
              "," + LESSON +
              "," + MODIFIED +
              ") " +
              "VALUES(?,?,?,?,?,?,?,?,?,?,?" +
              ")");
      int i = 1;
      statement.setString(i++, userExercise.getOldID());
      statement.setString(i++, fixSingleQuote(userExercise.getEnglish()));
      statement.setString(i++, fixSingleQuote(userExercise.getForeignLanguage()));
      statement.setString(i++, fixSingleQuote(userExercise.getTransliteration()));
      //     statement.setLong(i++, userExercise.getCombinedMutableUserExercise().getCreator());
      statement.setLong(i++, userExercise.getCreator());

      if (userExercise.hasContext()) {
        CommonShell next = userExercise.getDirectlyRelated().iterator().next();
        statement.setString(i++, fixSingleQuote(next.getForeignLanguage()));
        statement.setString(i++, fixSingleQuote(next.getEnglish()));
      } else {
        statement.setString(i++, "");
        statement.setString(i++, "");
      }

      statement.setBoolean(i++, false);

      Map<String, String> unitToValue = userExercise.getUnitToValue();

      if (typeOrder.size() > 0) {
        String s = typeOrder.iterator().next();
        String x = unitToValue.containsKey(s) ? unitToValue.get(s) : "";
        statement.setString(i++, x);
      } else {
        statement.setString(i++, "");
      }

      if (typeOrder.size() > 1) {
        Iterator<String> iterator = typeOrder.iterator();
        iterator.next();
        String s = iterator.next();
        statement.setString(i++, unitToValue.containsKey(s) ? unitToValue.get(s) : "");
      } else {
        statement.setString(i++, "");
      }
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      int id = getGeneratedKey(statement);
      if (id == -1) {
        logger.error("huh? no key was generated?");
      }
      //logger.debug("unique id = " + id);

      //   userExercise.getCombinedMutableUserExercise().setID(id);
      ((Exercise) userExercise).setID(id);

      // TODO : consider making this an actual prepared statement?
      boolean predefined = userExercise.isPredefined();
      if (!predefined) {     // cheesy!
        String customID = CUSTOM_PREFIX + id;
        String sql = "UPDATE " + USEREXERCISE +
            " " +
            "SET " +
            EXERCISEID +
            "='" + customID + "' " +
            "WHERE uniqueid=" +
            //  userExercise.getCombinedMutableUserExercise().getID();
            userExercise.getID();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.executeUpdate();
        preparedStatement.close();
        // userExercise.getCombinedMutableUserExercise().setOldID(customID);
        ((Exercise) userExercise).setOldID(customID);

        logger.debug("\tuserExercise= " + userExercise);
      }

      finish(connection, statement);

      logger.debug("now " + getCount(USEREXERCISE) + " user exercises and user exercise is " + userExercise);
      logger.debug("new " + (predefined ? " PREDEF " : " USER ") + " user exercise is " + userExercise);

      return id;
    } catch (Exception ee) {
      logException(ee);
    }
    return -1;
  }

  private String fixSingleQuote(String s) {
    return s == null ? "" : s.replaceAll("'", "''");
  }

  private void createUserTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        USEREXERCISE +
        " (" +
        "uniqueid IDENTITY, " +
        EXERCISEID + " VARCHAR, " +
        "english VARCHAR, " +
        "foreignLanguage VARCHAR, " +
        TRANSLITERATION + " VARCHAR, " +
        "creatorid INT, " +
        CONTEXT + " VARCHAR, " +
        CONTEXT_TRANSLATION + " VARCHAR, " +
        OVERRIDE + " BOOLEAN, " +
        UNIT +
        " VARCHAR, " +
        LESSON +
        " VARCHAR, " +
        "modified TIMESTAMP, " +
        STATE + " VARCHAR, " +
        "FOREIGN KEY(creatorid) REFERENCES " +
        "USERS" +
        "(ID)" +
        ")");
    finish(database, connection, statement);

    index(database);
  }

  void index(Database database) throws SQLException {
    createIndex(database, EXERCISEID, USEREXERCISE);
  }

  final int c = 0;

  /**
   * Handles both userexercise items and predefined exercises.
   *
   * @param listID
   * @param shouldSwap
   * @return
   * @see UserListDAO#populateList
   */
  @Override
  public List<CommonShell> getOnList(int listID, boolean shouldSwap) {
    String sql = getJoin(listID);
    List<CommonShell> userExercises2 = new ArrayList<>();

    try {
      if (DEBUG) logger.debug("\tgetOnList using for user exercise = " + sql);

      long then = System.currentTimeMillis();
      Collection<CommonExercise> userExercises = getUserExercises(sql);
      long now = System.currentTimeMillis();

      if (DEBUG || (now - then) > 30) {
        logger.debug("getOnList : took " + (now - then) +
            " found " + userExercises.size() + " exercises userExercises on list " + listID);
      }


      enrichWithPredefInfo(userExercises2, userExercises);
/*      for (CommonExercise ue : userExercises) {
        // if (DEBUG) logger.debug("\ton list " + listID + " " + ue.getID() + " / " + ue.getUniqueID() + " : " + ue);
        if (ue.isPredefined()) {
          CommonExercise byID = getExercise(ue);

          if (byID != null) {
            userExercises2.add(new UserExercise(byID, byID.getCreator())); // all predefined references
          } else {
            if (c++< 10)
            logger.error("getOnList: huh can't find user exercise '" + ue.getID() + "'");
          }
        } else {
          userExercises2.add(ue);
        }
      }*/

      if (c < 10 && !userExercises.isEmpty())
        logger.warn("huh? can't find " + c + "/" + userExercises.size() + " items???");

      boolean isEnglish = database.getLanguage().equalsIgnoreCase("english");
      String join2 = getJoin2(listID);
      if (DEBUG) logger.debug("\tusing exercise = " + join2);
      for (String exid : getExercises(join2)) {
        int exid1 = 0;
        try {
          exid1 = Integer.parseInt(exid);
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        CommonExercise exercise = getPredefExercise(exid1);
        if (exercise != null) {
          Exercise e = new Exercise(exercise);
          userExercises2.add(e);
          if (isEnglish) {
            e.setEnglish(exercise.getMeaning());
          }
          //logger.info("From " +exercise );
          //logger.info("  To " +e);
        } else {
          //logger.info("can't find exercise " + exid);
        }
      }
/*      if (userExercises2.isEmpty()) {
        if (DEBUG) logger.debug("\tgetOnList : no exercises on list id " + listID);
        return new ArrayList<?>();
      } else {
        if (DEBUG) logger.debug("\tgetOnList for " + listID+ "  got " + userExercises2.size());
        return userExercises2;
      }*/

    } catch (SQLException e) {
      logException(e);
    }
    return userExercises2;
  }

  /**
   * @param exid
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager
   * @see IUserListManager#markState(CommonExercise, STATE, int)
   */
  private CommonExercise getPredefExercise(int exid) {
    return exerciseDAO.getExercise(exid);
  }

  @Override
  public List<CommonExercise> getCommonExercises(int listID, boolean shouldSwap) {
    return null;
  }

  /**
   * @param listID
   * @return
   * @see IUserExerciseDAO#getOnList(int, boolean)
   */
  private String getJoin(long listID) {
    return "SELECT " +
        "ue.* from " + USEREXERCISE + " ue, " + UserListExerciseJoinDAO.USER_EXERCISE_LIST_EXERCISE + " uele " +
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
        UserListExerciseJoinDAO.USER_EXERCISE_LIST_EXERCISE +
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
   * Remove single ticks which break the sql.
   *
   * @param exid
   * @param shouldSwap
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUserExerciseByExID
   */
  @Override
  public CommonExercise getByExID(int exid, boolean shouldSwap) {
    String sql = "SELECT * from " + USEREXERCISE + " where " + EXERCISEID + "='" + exid + "'";
    Collection<CommonExercise> commonExercises = getCommonExercises(sql);
    return commonExercises.isEmpty() ? null : commonExercises.iterator().next();
  }

  @Override
  public SlickExercise getByID(int exid) {
    return null;
  }

  @Override
  public CommonExercise getByExOldID(String oldid, int projID) {
    String sql = "SELECT * from " + USEREXERCISE + " where " + EXERCISEID + "='" + oldid + "'";
    Collection<CommonExercise> commonExercises = getCommonExercises(sql);
    return commonExercises.isEmpty() ? null : commonExercises.iterator().next();
  }

  @Override
  public int getProjectForExercise(int exid) {
    return 0;
  }

  @Override
  public CommonExercise getTemplateExercise(int projID) {
    return null;
  }


  @Override
  public int ensureTemplateExercise(int projID) {
    return -1;
  }

  /**
   * @param shouldSwap
   * @return
   * @seex #setAudioDAO(AudioDAO)
   * @see mitll.langtest.server.database.exercise.ExcelImport#getRawExercises()
   */
  @Override
  public Collection<CommonExercise> getOverrides(boolean shouldSwap) {
    return getCommonExercises("SELECT * from " + USEREXERCISE + " where " + OVERRIDE + "=true");
  }

  private Collection<CommonExercise> getCommonExercises(String sql) {
    try {
      return getUserExercises(sql);
    } catch (SQLException e) {
      logException(e);
    }
    return Collections.emptyList();
  }

  public Collection<Exercise> getAllUserExercises() throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    List<Exercise> exercises = new ArrayList<>();
    try {
      PreparedStatement statement = connection.prepareStatement(GET_ALL_SQL);
      //logger.debug("getUserExercises sql = " + sql);
      ResultSet rs = statement.executeQuery();

      List<String> typeOrder = exerciseDAO.getSectionHelper().getTypeOrder();
      while (rs.next()) {
        exercises.add(getUserExercise(rs, typeOrder));
      }
      finish(connection, statement, rs, "");
    } finally {
      database.closeConnection(connection);
    }

    return exercises;
  }

  /**
   * @param exids
   * @param shouldSwap
   * @return
   * @seex UserListManager#getDefectList
   */
/*
  @Override
  public Collection<CommonExercise> getByExID(Collection<Integer> exids, boolean shouldSwap) {
    if (exids.isEmpty()) return new ArrayList<>();
    String sql = "SELECT * from " + USEREXERCISE + " where " + EXERCISEID + " in (" + getIds(exids) + ")";
    return getCommonExercises(sql);
  }
*/
  @Override
  public List<SlickExercise> getExercisesByIDs(Collection<Integer> exids) {
    return null;
  }

  @Override
  public void deleteByExID(Collection<Integer> exids) {

  }

  private String getIds(Collection<Integer> exids) {
    StringBuilder builder = new StringBuilder();
    for (Integer id : exids) builder.append("'" + id + "'").append(",");
    String s = builder.toString();
    s = s.substring(0, s.length() - 1);
    return s;
  }

  /**
   * @param sql
   * @return user exercises without annotations
   * @throws SQLException
   * @see IUserExerciseDAO#getOnList(int, boolean)
   * @see IUserExerciseDAO#getOverrides(boolean)
   * @see IUserExerciseDAO#getByExID(int, boolean)
   */
  private Collection<CommonExercise> getUserExercises(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    List<CommonExercise> exercises = new ArrayList<>();
    try {
      PreparedStatement statement = connection.prepareStatement(sql);
      //logger.debug("getUserExercises sql = " + sql);
      ResultSet rs = statement.executeQuery();

      ISection<CommonExercise> sectionHelper = exerciseDAO.getSectionHelper();
      List<String> typeOrder = sectionHelper.getTypeOrder();
      while (rs.next()) {
        Exercise e = getUserExercise(rs, typeOrder);
//        logger.info("getUserExercises " + e);
/*        if (addMissingAudio) {
          String ref = rs.getString(REF_AUDIO);
          String sref = rs.getString(SLOW_AUDIO_REF);
          addMissingAudio(e, ref, sref);
        }*/
        exercises.add(e);
      }
      finish(connection, statement, rs, sql);
    } finally {
      database.closeConnection(connection);
    }

    return exercises;
  }

  private VocabFactory factory = new VocabFactory();

  private Exercise getUserExercise(ResultSet rs, List<String> typeOrder) throws SQLException {
    Map<String, String> unitToValue = getUnitToValue(rs, typeOrder);

    Timestamp timestamp = rs.getTimestamp(MODIFIED);

    Date date = (timestamp != null) ? new Date(timestamp.getTime()) : new Date(0);

    String foreignLanguage = rs.getString("foreignLanguage");
    return new Exercise(
        rs.getInt("uniqueid"),
        rs.getString(EXERCISEID),
        rs.getInt("creatorid"),
        rs.getString("english"),
        foreignLanguage,
        foreignLanguage,
        "",
        "",
        rs.getString(TRANSLITERATION),
        rs.getBoolean(OVERRIDE),
        unitToValue,
        date.getTime(),
        -1,
        false,
        System.currentTimeMillis(),
        false,
        -1,
        false);
  }

  //  private Map<String, List<AudioAttribute>> exToAudio;
//  private AudioDAO audioDAO;
  public void setAudioDAO(IAudioDAO audioDAO) {
    //  exToAudio = audioDAO.getExToAudio();
    //  this.audioDAO = audioDAO;
/*    if (ADD_MISSING_AUDIO) {
      getOverrides(true);
    }*/
  }

  private Map<String, String> getUnitToValue(ResultSet rs, List<String> typeOrder) throws SQLException {
    String first = rs.getString(UNIT);
    String second = rs.getString(LESSON);
    Map<String, String> unitToValue = new HashMap<String, String>();

    if (typeOrder.size() > 0 && first != null) {
      String s = typeOrder.get(0);
      if (!first.isEmpty()) unitToValue.put(s, first);
    }

    if (typeOrder.size() > 1 && second != null) {
      String s = typeOrder.get(1);
      if (!second.isEmpty()) unitToValue.put(s, second);
    }
    return unitToValue;
  }

  private Collection<String> getExercises(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<String> exercises = new ArrayList<String>();

    while (rs.next()) {
      exercises.add(rs.getString(EXERCISEID));
    }
    finish(connection, statement, rs, sql);

    return exercises;
  }

  /**
   * @param userExercise
   * @param isContext
   * @param typeOrder
   * @see IUserListManager#editItem
   */
  @Override
  public boolean update(CommonExercise userExercise, boolean isContext, Collection<String> typeOrder) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + USEREXERCISE +
          " " +
          "SET " +
          "english=?," +
          "foreignLanguage=?," +
          TRANSLITERATION + "=?," +
          MODIFIED +
          "=? " +
          "WHERE " +
          EXERCISEID +
          "=?";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, userExercise.getEnglish());
      statement.setString(ii++, userExercise.getForeignLanguage());
      statement.setString(ii++, userExercise.getTransliteration());
      statement.setTimestamp(ii++, new Timestamp(System.currentTimeMillis()));
      statement.setString(ii++, userExercise.getOldID());

      int i = statement.executeUpdate();

      if (i == 0) {
        // if (createIfDoesntExist) {
        //   add(userExercise, true, false);
        //} else {
        logger.error("huh? didn't update the userExercise for " + userExercise + "\n\tsql " + sql);
        //}
      }

      finish(connection, statement);
      return i > 0;
    } catch (Exception e) {
      logException(e);
      return false;
    }
  }

  @Override
  public int getUnknownExerciseID() {
    return 0;
  }

  @Override
  public void deleteForProject(int projID) {
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
        USEREXERCISE + " ADD " + MODIFIED + " TIMESTAMP ");
    statement.execute();
    statement.close();
  }

  private void addColumnToTable4(Connection connection) throws SQLException {
    addVarchar(connection, USEREXERCISE, STATE);
  }

  private void addColumnToTable5(Connection connection) throws SQLException {
    addVarchar(connection, USEREXERCISE, CONTEXT);
  }

  private void addColumnToTable6(Connection connection) throws SQLException {
    addVarchar(connection, USEREXERCISE, CONTEXT_TRANSLATION);
  }

  /**
   * TODO : Do we need to set the english field to meaning for english items???
   *
   * @param userExercises2
   * @param userExercises
   * @deprecated not needed with postgres
   */
  void enrichWithPredefInfo(List<CommonShell> userExercises2, Collection<CommonExercise> userExercises) {
    int c = 0;
    for (CommonExercise ue : userExercises) {
      // if (DEBUG) logger.debug("\ton list " + listID + " " + ue.getOldID() + " / " + ue.getUniqueID() + " : " + ue);
      if (ue.isPredefined()) {
        CommonExercise byID = getExercise(ue);

        if (byID != null) {
          userExercises2.add(new Exercise(byID)); // all predefined references
          /// TODO : put this back???
          // if (isEnglish) {
          //    e.setEnglish(exercise.getMeaning());
          //  }

        } else {
          if (c++ < 10)
            logger.error("getOnList: huh can't find user exercise '" + ue.getOldID() + "'");
        }
      } else {
        userExercises2.add(ue);
      }
    }
    if (c > 0) logger.warn("huh? can't find " + c + "/" + userExercises.size() + " items???");
  }

  /**
   * @param ue
   * @return
   * @see #enrichWithPredefInfo
   */
  private CommonExercise getExercise(HasID ue) {
    return exerciseDAO.getExercise(ue.getID());
  }

  @Override
  public boolean updateModified(int exid) {
    return false;
  }
}