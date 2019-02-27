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

package mitll.langtest.server.database.annotation;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

public class AnnotationDAO extends BaseAnnotationDAO implements IAnnotationDAO {
  private static final Logger logger = LogManager.getLogger(AnnotationDAO.class);

  private static final String ANNOTATION = "annotation";
  private static final String CREATORID = "creatorid";
  private static final String STATUS = "status";
  private static final String EXERCISEID = "exerciseid";
  private static final String FIELD1 = "field";
  private static final String FIELD = FIELD1;
  private static final String MODIFIED = "modified";

  //private final Map<String, List<UserAnnotation>> exerciseToAnnos = new HashMap<>();

  /**
   * @param database
   * @param userDAO
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public AnnotationDAO(Database database, IUserDAO userDAO) {
    super(database, userDAO);
    try {
      createTable(database);
//      populate(userDAO.getDefectDetector());
    //  markCorrectForDefectAudio();
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
/*
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
*/
/*

  private String getInClause(Set<Long> longs) {
    StringBuilder buffer = new StringBuilder();
    for (long id : longs) {
      buffer.append(id).append(",");
    }

    String s = buffer.toString();
    if (s.endsWith(",")) s = s.substring(0, s.length() - 1);
    return s;
  }
*/


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
   * @seex UserListManager#addAnnotation
   */
  @Override
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
      statement.setString(i++, annotation.getOldExID());
      statement.setString(i++, annotation.getField());
      statement.setString(i++, annotation.getStatus());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      statement.setString(i++, annotation.getComment());

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

  public Collection<UserAnnotation> getAll() {
    try {
      return getUserAnnotations("select * from " + ANNOTATION);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  /**
   * Pulls the list of users out of the database.
   *
   * @param userid
   * @return
   */
  protected List<UserAnnotation> getAll(int userid) {
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
   * @see IUserListManager#addDefect
   */
/*
  public boolean hasAnnotation(String exerciseID, String field, String status, String comment) {
    List<UserAnnotation> userAnnotations = exerciseToAnnos.get(exerciseID);
    if (userAnnotations == null) {
      return false;
    }
    Map<String, ExerciseAnnotation> latestByExerciseID = getFieldToAnnotationMap(userAnnotations);
    ExerciseAnnotation annotation = latestByExerciseID.get(field);
    return (annotation != null) && (annotation.getStatus().equals(status) && annotation.getComment().equals(comment));
  }
*/

  /**
   * TODO: Ought to be able to make a sql query that only returns the latest item for a exercise-field pair...
   *
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#getAudioAnnos
   */
/*  @Override
  public Collection<Integer> getAudioAnnos() {
*//*    String sql = "SELECT " +
        EXERCISEID+ "," +
        FIELD+ "," +
        STATUS+
        " from " + ANNOTATION + " where " +
        FIELD +
        " like'%.wav' order by field,modified desc";*//*

*//*    String sql2 = "select a.exerciseid, a.field, a.status, r.MaxTime \n" +
        "from (\n" +
        "select exerciseid, field, MAX(modified) as MaxTime from annotation where field like'%.wav' group by exerciseid, field) r \n" +
        "inner join annotation a on a.exerciseid = r.exerciseid AND a.modified = r.MaxTime order by a.exerciseid, a.field";*//*

    String sql3 = "select a.exerciseid, a.status, r.MaxTime \n" +
        "from (\n" +
        "select exerciseid, field, MAX(modified) as MaxTime " +
        " from annotation" +
        " where field like'%.wav' group by exerciseid, field) r \n" +
        "inner join annotation a on a.exerciseid = r.exerciseid " +
        "AND a.modified = r.MaxTime and a.status = 'incorrect' " +
        "order by a.exerciseid, a.field";
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql3);
      ResultSet rs = statement.executeQuery();
      Set<Integer> incorrect = new HashSet<>();

      while (rs.next()) {
        String string = rs.getString(1);
        int i = Integer.parseInt(string);
        incorrect.add(i);
      }

      //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
      finish(connection, statement, rs, sql3);
      return incorrect;
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return new HashSet<>();
  }*/

  /**
   * @param exerciseID
   * @return
   * @seex UserListManager#addAnnotations
   * @seex mitll.langtest.server.LangTestDatabaseImpl#addAnnotations
   */
  @Override
  public Map<String, ExerciseAnnotation> getLatestByExerciseID(int exerciseID) {
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

  @Override
  public Set<Integer> getExercisesWithIncorrectAnnotations(int projID, boolean isContext) {
    return getAnnotationExToCreator(true).keySet();
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
/*
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
*/

  /**
   * @param sql
   * @return
   * @throws SQLException
   * @see BaseAnnotationDAO#getAll(int)
   * @see #getFieldToAnnotationMap(String)
   */
  private List<UserAnnotation> getUserAnnotations(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<UserAnnotation> lists = new ArrayList<>();

    while (rs.next()) {
      lists.add(new UserAnnotation(
              -1,
              rs.getString(FIELD),
              rs.getString(STATUS),
              rs.getString("comment"),
              rs.getLong(CREATORID),
              rs.getTimestamp(MODIFIED).getTime(),
              rs.getString(EXERCISEID)
          )
      );
    }

    //logger.debug("getUserAnnotations sql " + sql + " yielded " + lists);
    finish(connection, statement, rs, sql);
    return lists;
  }

  private Map<Integer, Long> getAnnotationExToCreator(boolean forDefects) {
    Connection connection = database.getConnection(this.getClass().toString());

    String sql2 = "select exerciseid,field," + STATUS + "," + CREATORID +
        " from annotation " +
        "group by exerciseid,field," + STATUS + ",modified " +
        "order by exerciseid,field,modified;";

    Map<Integer, Long> exToCreator = Collections.emptyMap();
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
            int i = Integer.parseInt(prevExid);
            exToCreator.put(i, creatorid);
          }
          fieldToStatus.clear();
          prevExid = exid;
          prevCreatorid = creatorid;
        }

        fieldToStatus.put(field, status);
      }

      if (examineFields(forDefects, fieldToStatus)) {
        int i = Integer.parseInt(prevExid);

        exToCreator.put(i, prevCreatorid);
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

}