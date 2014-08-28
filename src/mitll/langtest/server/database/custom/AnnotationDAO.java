package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
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

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class AnnotationDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AnnotationDAO.class);

  private static final String ANNOTATION = "annotation";
  private static final String CREATORID = "creatorid";
  private static final String STATUS = "status";
  public static final String EXERCISEID = "exerciseid";

  public AnnotationDAO(Database database, UserDAO userDAO) {
    super(database);
    try {
      createTable(database);
      populate(userDAO.getDefectDetector());
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
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        ANNOTATION +
        " (" +
        "uniqueid IDENTITY, " +
        CREATORID + " INT, " +
        EXERCISEID + " VARCHAR, " +
        "field VARCHAR, " +
        STATUS + " VARCHAR, " +
        "modified TIMESTAMP, " +
        "comment VARCHAR, " +
        "FOREIGN KEY(" +
        CREATORID +
        ") REFERENCES " +
        "USERS" +
        "(ID)" +
        ")");
    finish(database, connection, statement);
    index(database);
  }

  void index(Database database) throws SQLException {
    createIndex(database, EXERCISEID, ANNOTATION);
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see UserListManager#reallyCreateNewItem(long, mitll.langtest.shared.custom.UserExercise, String)
   */
  public void add(UserAnnotation annotation) {
    try {
      // there are much better ways of doing this...
     //logger.info("add :annotation " + annotation);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement;

      statement = connection.prepareStatement(
        "INSERT INTO " + ANNOTATION +
          "(" +
          CREATORID +
          ",exerciseid,field," +
          STATUS +
          ",modified,comment) " +
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

      if (j != 1) {
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
      }

      finish(connection, statement);

    //  logger.debug("now " + getCount(ANNOTATION) + " and user exercise is " + annotation);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  private final Map<String,List<UserAnnotation>> exerciseToAnnos = new HashMap<String, List<UserAnnotation>>();

  private void populate(long userid) {
    List<UserAnnotation> all = getAll(userid);
    for (UserAnnotation userAnnotation : all) {
      List<UserAnnotation> userAnnotations = exerciseToAnnos.get(userAnnotation.getExerciseID());
      if (userAnnotations == null) {
        exerciseToAnnos.put(userAnnotation.getExerciseID(),userAnnotations = new ArrayList<UserAnnotation>());
      }
      userAnnotations.add(userAnnotation);
    }
  }

  /**
   * Pulls the list of users out of the database.
   *
   * @return
   */
  private List<UserAnnotation> getAll(long userid) {
    try {
      String sql = "SELECT * from " + ANNOTATION + " where " + CREATORID +"="+userid;

      return getUserAnnotations(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  public boolean hasAnnotation(String exerciseID, String field, String status, String comment) {
    List<UserAnnotation> userAnnotations = exerciseToAnnos.get(exerciseID);
    if (userAnnotations == null) {
      return false;
    }
    Map<String, ExerciseAnnotation> latestByExerciseID = getFieldToAnnotationMap(userAnnotations);
    ExerciseAnnotation annotation = latestByExerciseID.get(field);
    return (annotation != null) && (annotation.status.equals(status) && annotation.comment.equals(comment));
  }

  /**
   * @see UserListManager#addAnnotations
   * @param exerciseID
   * @return
   */
  public Map<String, ExerciseAnnotation> getLatestByExerciseID(String exerciseID) {
    String sql = "SELECT * from " + ANNOTATION + " where " +
        EXERCISEID +
        "='" + exerciseID + "' order by field,modified desc";
    try {
      return getFieldToAnnotationMap(sql);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  private Map<String, ExerciseAnnotation> getFieldToAnnotationMap(String sql) throws SQLException {
    List<UserAnnotation> lists = getUserAnnotations(sql);
    return getFieldToAnnotationMap(lists);
  }

  private Map<String, ExerciseAnnotation> getFieldToAnnotationMap(List<UserAnnotation> lists) {
    Map<String,UserAnnotation> fieldToAnno = new HashMap<String, UserAnnotation>();

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
      //logger.debug("field->anno " + fieldToAnno);
      return fieldToAnnotation;
    }
  }

  /**
   * @see #getAll(long)
   * @see #getFieldToAnnotationMap(String)
   * @param sql
   * @return
   * @throws SQLException
   */
  private List<UserAnnotation> getUserAnnotations(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<UserAnnotation> lists = new ArrayList<UserAnnotation>();

    while (rs.next()) {
      lists.add(new UserAnnotation(
          rs.getString(EXERCISEID),
          rs.getString("field"),
          rs.getString(STATUS),
          rs.getString("comment"),
          rs.getLong(CREATORID),
          rs.getTimestamp("modified").getTime()
      )
      );
    }

    //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
    finish(connection, statement, rs);
    return lists;
  }

    /**
     *
     * @see mitll.langtest.server.database.custom.UserListManager#getCommentedList(java.util.Collection)
     * @see mitll.langtest.server.database.custom.UserListManager#UserListManager(mitll.langtest.server.database.UserDAO, UserListDAO, UserListExerciseJoinDAO, AnnotationDAO, ReviewedDAO, mitll.langtest.server.PathHelper)
     * @return
     */
  public Map<String,Long> getAnnotatedExerciseToCreator()  {
    long then = System.currentTimeMillis();
    Map<String,Long> stateIds = getAnnotationToCreator(true);
    long now = System.currentTimeMillis();
    if(now-then > 200) logger.debug("getAnnotatedExerciseToCreator took " +(now-then) + " millis to find " + stateIds.size());
    return stateIds;
  }

  private Map<String, Long> getAnnotationToCreator(boolean forDefects) {
    Connection connection = database.getConnection(this.getClass().toString());

    String sql2 = "select exerciseid,field," +STATUS +"," +CREATORID +
      " from annotation group by exerciseid,field," + STATUS +",modified order by exerciseid,field,modified;";

    Map<String,Long> exToCreator = Collections.emptyMap();
    try {
      PreparedStatement statement = connection.prepareStatement(sql2);
      ResultSet rs = statement.executeQuery();
      exToCreator = new HashMap<String, Long>();
      String prevExid = "";
      long prevCreatorid = -1;

      Map<String,String> fieldToStatus = new HashMap<String, String>();
      while (rs.next()) {
        String exid = rs.getString(1);
        String field = rs.getString(2);
        String status = rs.getString(3);
        //long modified = rs.getTimestamp(4).getTime();
        long creatorid = rs.getLong(4);

        if (prevExid.isEmpty()) {
          prevExid = exid;
        } else if (!prevExid.equals(exid)) {
          // go through all the fields -- if the latest is "incorrect" on any field, it's a defect
          //examineFields(forDefects, lists, prevExid, fieldToStatus);
          if (examineFields(forDefects, fieldToStatus)) {
            exToCreator.put(prevExid,creatorid);
          }
          fieldToStatus.clear();
          prevExid = exid;
          prevCreatorid = creatorid;
        }

        fieldToStatus.put(field, status);
      }

      if (examineFields(forDefects, fieldToStatus)) {
        exToCreator.put(prevExid,prevCreatorid);
      }

      //logger.debug("getUserAnnotations forDefects " +forDefects+ " sql " + sql2 + " yielded " + exToCreator.size());
   /*   if (lists.size() > 20) {
        Iterator<String> iterator = lists.iterator();
        //for (int i = 0; i < 20;i++) logger.debug("\tgetUserAnnotations e.g. " + iterator.next() );
      }*/

      finish(connection, statement, rs);
    } catch (SQLException e) {
      logger.error("Got " +e + " doing " + sql2,e);
    }
    return exToCreator;
  }

  private boolean examineFields(boolean forDefects, Map<String, String> fieldToStatus) {
    String statusToLookFor = "correct";
    boolean foundIncorrect = false;
    for (String latest : fieldToStatus.values()) {
      if (!latest.equals(statusToLookFor)) {
        //lists.add(prevExid);
        foundIncorrect = true;
        break;
      }
    }
    if (forDefects) {
      if (foundIncorrect) {
       // lists.add(prevExid);
        return true;
      }
    } else {
      if (!foundIncorrect) {
        //lists.add(prevExid);
        return true;
      }
    }
    return false;
  }
}