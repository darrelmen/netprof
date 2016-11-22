/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.ExerciseAnnotation;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class AnnotationDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AnnotationDAO.class);

  private static final String ANNOTATION = "annotation";
  private static final String CREATORID = "creatorid";
  private static final String STATUS = "status";
  private static final String EXERCISEID = "exerciseid";
  private static final String FIELD1 = "field";
  private static final String FIELD = FIELD1;
  private static final String MODIFIED = "modified";

  private final Map<String, List<UserAnnotation>> exerciseToAnnos = new HashMap<>();

  /**
   * @param database
   * @param userDAO
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public AnnotationDAO(Database database, UserDAO userDAO) {
    super(database);
    try {
      createTable(database);
      populate(userDAO.getDefectDetector());
      markCorrectForDefectAudio();
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * Fix for an issue where we didn't clear incorrect annotations when the audio is marked with a defect.
   * Later, if we filter for just items with audio defects, we'll find these unless we fix them.
   *
   * @throws SQLException
   */
  private void markCorrectForDefectAudio() throws SQLException {
    String sql = "select annotation.uniqueid from annotation, audio where status='incorrect' and annotation.field = audio.audioref and audio.defect=true";
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    ResultSet rs = statement.executeQuery();

    Set<Long> ids = new HashSet<>();
    while (rs.next()) {
      ids.add(rs.getLong(1));
    }

    //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
    finish(connection, statement, rs, sql);

    if (!ids.isEmpty()) {
      logger.info("fixing " + ids.size() + " annotations where audio was marked defect");
      connection = database.getConnection(this.getClass().toString());
      statement = connection.prepareStatement(
          "update annotation" +
              " set annotation.status='correct'" +
              " where uniqueid" +
              " IN (" + getInClause(ids) + ")"
      );
      statement.executeUpdate();
      finish(connection, statement);
    }
  }

  private String getInClause(Set<Long> longs) {
    StringBuilder buffer = new StringBuilder();
    for (long id : longs) {
      buffer.append(id).append(",");
    }

    String s = buffer.toString();
    if (s.endsWith(",")) s = s.substring(0, s.length() - 1);
    return s;
  }


  /**
   * String exerciseID; String field; String status; String comment;
   * String userID;
   *
   * @param database
   * @throws SQLException
   */
  private void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        ANNOTATION +
        " (" +
        "uniqueid IDENTITY, " +
        CREATORID + " INT, " +
        EXERCISEID + " VARCHAR, " +
        FIELD1 +
        " VARCHAR, " +
        STATUS + " VARCHAR, " +
        MODIFIED +
        " TIMESTAMP, " +
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

  private void index(Database database) throws SQLException {
    createIndex(database, EXERCISEID, ANNOTATION);
    createIndex(database, FIELD1, ANNOTATION);
    createIndex(database, MODIFIED, ANNOTATION);
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
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + ANNOTATION +
              "(" +
              CREATORID +
              ",exerciseid,field," +
              STATUS +
              ",modified,comment) " +
              "VALUES(?,?,?,?,?,?);");
      int i = 1;

      statement.setLong(i++, annotation.getCreatorID());
      statement.setString(i++, annotation.getExerciseID());
      statement.setString(i++, annotation.getField());
      statement.setString(i++, annotation.getStatus());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      statement.setString(i++, annotation.getComment());

      logger.info("adding annotation " + annotation);
      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("add huh? didn't insert row for " + annotation);// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
      }

      finish(connection, statement);
      //  logger.debug("now " + getCount(ANNOTATION) + " and user exercise is " + annotation);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  /**
   * TODO : this seems like a bad idea...
   *
   * @param userid
   * @see #AnnotationDAO
   */
  private void populate(long userid) {
    List<UserAnnotation> all = getAll(userid);
    for (UserAnnotation userAnnotation : all) {
      List<UserAnnotation> userAnnotations = exerciseToAnnos.get(userAnnotation.getExerciseID());
      if (userAnnotations == null) {
        exerciseToAnnos.put(userAnnotation.getExerciseID(), userAnnotations = new ArrayList<>());
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
      String sql = "SELECT * from " + ANNOTATION + " where " + CREATORID + "=" + userid;
      return getUserAnnotations(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  /**
   * TODO : this seems like a bad idea...
   *
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#addDefect
   */
  public boolean hasAnnotation(String exerciseID, String field, String status, String comment) {
    List<UserAnnotation> userAnnotations = exerciseToAnnos.get(exerciseID);
    if (userAnnotations == null) {
      return false;
    }
    Map<String, ExerciseAnnotation> latestByExerciseID = getFieldToAnnotationMap(userAnnotations);
    ExerciseAnnotation annotation = latestByExerciseID.get(field);
    return (annotation != null) && (annotation.getStatus().equals(status) && annotation.getComment().equals(comment));
  }

  /**
   * TODO: Ought to be able to make a sql query that only returns the latest item for a exercise-field pair...
   *
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#getAudioAnnos
   */
  public Set<String> getAudioAnnos() {
/*    String sql = "SELECT " +
        EXERCISEID+ "," +
        FIELD+ "," +
        STATUS+
        " from " + ANNOTATION + " where " +
        FIELD +
        " like'%.wav' order by field,modified desc";*/

/*    String sql2 = "select a.exerciseid, a.field, a.status, r.MaxTime \n" +
        "from (\n" +
        "select exerciseid, field, MAX(modified) as MaxTime from annotation where field like'%.wav' group by exerciseid, field) r \n" +
        "inner join annotation a on a.exerciseid = r.exerciseid AND a.modified = r.MaxTime order by a.exerciseid, a.field";*/

    String sql3 = "select a.exerciseid, a.status, r.MaxTime \n" +
        "from (\n" +
        "select exerciseid, field, MAX(modified) as MaxTime" +
        " from annotation" +
        " where field like'%.wav' group by exerciseid, field) r \n" +
        "inner join annotation a on a.exerciseid = r.exerciseid AND a.modified = r.MaxTime and a.status = 'incorrect' order by a.exerciseid, a.field";
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql3);
      ResultSet rs = statement.executeQuery();
      Set<String> incorrect = new HashSet<>();

      while (rs.next()) {
        incorrect.add(rs.getString(1));
      }

      //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
      finish(connection, statement, rs, sql3);
      return incorrect;
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return new HashSet<>();
  }

  /**
   * @param exerciseID
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addAnnotations
   * @see UserListManager#addAnnotations
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
    return getFieldToAnnotationMap(getUserAnnotations(sql));
  }

  /**
   * Always return the latest annotation.
   *
   * @param lists
   * @return
   */
  private Map<String, ExerciseAnnotation> getFieldToAnnotationMap(List<UserAnnotation> lists) {
    Map<String, UserAnnotation> fieldToAnno = new HashMap<>();

    for (UserAnnotation annotation : lists) {
      UserAnnotation prevAnnotation = fieldToAnno.get(annotation.getField());
      if (prevAnnotation == null) fieldToAnno.put(annotation.getField(), annotation);
      else if (prevAnnotation.getTimestamp() < annotation.getTimestamp()) {
        fieldToAnno.put(annotation.getField(), annotation);
      }
    }
    if (lists.isEmpty()) {
      //logger.error("huh? no annotation with id " + unique);
      return Collections.emptyMap();
    } else {
      Map<String, ExerciseAnnotation> fieldToAnnotation = new HashMap<>();
      for (Map.Entry<String, UserAnnotation> pair : fieldToAnno.entrySet()) {
        fieldToAnnotation.put(pair.getKey(), new ExerciseAnnotation(pair.getValue().getStatus(), pair.getValue().getComment()));
      }
      //logger.debug("field->anno " + fieldToAnno);
      return fieldToAnnotation;
    }
  }

  /**
   * @param sql
   * @return
   * @throws SQLException
   * @see #getAll(long)
   * @see #getFieldToAnnotationMap(String)
   */
  private List<UserAnnotation> getUserAnnotations(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<UserAnnotation> lists = new ArrayList<>();

    while (rs.next()) {
      lists.add(new UserAnnotation(
              rs.getString(EXERCISEID),
              rs.getString(FIELD),
              rs.getString(STATUS),
              rs.getString("comment"),
              rs.getLong(CREATORID),
              rs.getTimestamp(MODIFIED).getTime()
          )
      );
    }

    //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
    finish(connection, statement, rs, sql);
    return lists;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#getCommentedList(java.util.Collection)
   * @see mitll.langtest.server.database.custom.UserListManager#getAmmendedStateMap
   */
  public Map<String, Long> getAnnotatedExerciseToCreator() {
    long then = System.currentTimeMillis();
    Map<String, Long> stateIds = getAnnotationToCreator(true);
    long now = System.currentTimeMillis();
    if (now - then > 200)
      logger.debug("getAnnotatedExerciseToCreator took " + (now - then) + " millis to find " + stateIds.size());
    return stateIds;
  }

  private Map<String, Long> getAnnotationToCreator(boolean forDefects) {
    Connection connection = database.getConnection(this.getClass().toString());

    String sql2 =
        "select exerciseid,field," + STATUS + "," + CREATORID +
            " from annotation" +
            " group by exerciseid,field," + STATUS + ",modified" +
            " order by exerciseid,field,modified;";

    Map<String, Long> exToCreator = Collections.emptyMap();
    try {
      PreparedStatement statement = connection.prepareStatement(sql2);
      ResultSet rs = statement.executeQuery();
      exToCreator = new HashMap<>();
      String prevExid = "";
      long prevCreatorid = -1;

      Map<String, String> fieldToStatus = new HashMap<>();
      while (rs.next()) {
        String exid = rs.getString(1);
        String field = rs.getString(2);
        String status = rs.getString(3);
        //long modified = rs.getTimestamp(4).getStart();
        long creatorid = rs.getLong(4);

        if (prevExid.isEmpty()) {
          prevExid = exid;
        } else if (!prevExid.equals(exid)) {
          // go through all the fields -- if the latest is "incorrect" on any field, it's a defect
          //examineFields(forDefects, lists, prevExid, fieldToStatus);
          if (examineFields(forDefects, fieldToStatus)) {
            exToCreator.put(prevExid, creatorid);
          }
          fieldToStatus.clear();
          prevExid = exid;
          prevCreatorid = creatorid;
        }

        fieldToStatus.put(field, status);
      }

      if (examineFields(forDefects, fieldToStatus)) {
        exToCreator.put(prevExid, prevCreatorid);
      }

      //logger.debug("getUserAnnotations forDefects " +forDefects+ " sql " + sql2 + " yielded " + exToCreator.size());
   /*   if (lists.size() > 20) {
        Iterator<String> iterator = lists.iterator();
        //for (int i = 0; i < 20;i++) logger.debug("\tgetUserAnnotations e.g. " + iterator.next() );
      }*/

      finish(connection, statement, rs, sql2);
    } catch (SQLException e) {
      logger.error("Got " + e + " doing " + sql2, e);
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