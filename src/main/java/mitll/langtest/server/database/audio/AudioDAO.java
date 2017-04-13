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

package mitll.langtest.server.database.audio;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.Report;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.MiniUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

public class AudioDAO extends BaseAudioDAO implements IAudioDAO {
  private static final Logger logger = LogManager.getLogger(AudioDAO.class);

  private static final String ID = "id";
  private static final String USERID = "userid";
  private static final String AUDIO_REF = "audioRef";

  private static final String AUDIO = "audio";
  private static final String SELECT_ALL = "SELECT * FROM " + AUDIO;

  private static final String AUDIO_TYPE = "audioType";
  private static final String DURATION = "duration";
  private static final String DEFECT = "defect";
  private static final String TRANSCRIPT = "transcript";
  public static final String DNR = "dnr";

  private final boolean DEBUG = false;
  private final Connection connection;
  private final IUserDAO userDAO;

  /**
   * @param database
   * @param userDAO
   * @see DatabaseImpl#initializeDAOs(PathHelper)
   */
  public AudioDAO(Database database, IUserDAO userDAO) {
    super(database, userDAO);

    //   logger.info("making audio dao -------\n\n\n");

    connection = database.getConnection(this.getClass().toString());

    this.userDAO = userDAO;
    try {
      createTable(connection);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    Collection<String> columns = getColumns(AUDIO);
    if (!columns.contains(DEFECT)) {
      try {
        addBoolean(connection, AUDIO, DEFECT);
      } catch (SQLException e) {
        logger.error("got " + e, e);
      }
    }
    if (!columns.contains(TRANSCRIPT)) {
      try {
        addVarchar(connection, AUDIO, TRANSCRIPT);
      } catch (SQLException e) {
        logger.error("got " + e, e);
      }
    }

    if (!columns.contains(DNR)) {
      try {
        PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + AUDIO + " ADD " + DNR + " " + "REAL" + " DEFAULT '-1'");
        statement.execute();
        statement.close();
      } catch (SQLException e) {
        logger.error("got " + e, e);
      }
    }
    database.closeConnection(connection);
  }

  @Override
  protected void addBoolean(Connection connection, String table, String col) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
        table +
        " ADD " + col + " BOOLEAN DEFAULT FALSE");
    statement.execute();
    statement.close();
  }

  /**
   * Go back and mark transcripts on audio cuts that were not marked properly initially.
   *
   * Distinguish between context and regular item audio.
   *
   * @see DatabaseImpl#makeDAO(String, String, String)
   */
  /*void markTranscripts() {
    List<AudioAttribute> toUpdate = new ArrayList<>();

    Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();
    for (AudioAttribute audio : audioAttributes1) {
      CommonExercise exercise = exerciseDAO.getExercise(audio.getExid());
      if (exercise != null) {
        String english = exercise.getEnglish();
        String fl = exercise.getForeignLanguage();
        String transcript = audio.getTranscript();
        if (audio.isContextAudio()) {
          if (exercise.hasContext()) {
            String context = exercise.getContext();
            if ((transcript == null ||
                transcript.isEmpty() ||
                transcript.equals(english) ||
                transcript.equals(fl)) &&
                (!context.isEmpty() && !context.equals(transcript))
                ) {
              audio.setTranscript(context);
              toUpdate.add(audio);
              logger.info("context update " + exercise.getOldID() + "/" + audio.getUniqueID() + " to " + context);
            }
          }
        }
        else {
          if ((transcript == null ||
              transcript.isEmpty() ||
              transcript.equals(english)) &&
              (!fl.isEmpty() && !fl.equals(transcript))
              ) {
            audio.setTranscript(fl);
            logger.info("update " + exercise.getOldID() + "/" +audio.getUniqueID()+" to " + fl + " from " + exercise.getEnglish());

            toUpdate.add(audio);
          }
        }
      }
    }
    updateTranscript(toUpdate);
  }*/

  /**
   * @see #markTranscripts()
   * @param audio
   * @return
   */
/*  private int updateTranscript(Collection<AudioAttribute> audio) {
    int c = 0;

    long then = System.currentTimeMillis();
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO + " " +
          "SET " + TRANSCRIPT + "=? " +
          "WHERE " +
          ID + "=?";

      PreparedStatement statement = connection.prepareStatement(sql);

      for (AudioAttribute audioAttribute : audio) {
        int ii = 1;
        statement.setString(ii++, audioAttribute.getTranscript());
        statement.setInt(ii++, audioAttribute.getUniqueID());
        int i = statement.executeUpdate();
        if (i > 0) c++;
      }

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    long now = System.currentTimeMillis();

    if (c > 0 || (now-then)>100) {
      logger.info("updateTranscript did " + c + "/" + audio.size() + " in " + (now - then) + " millis");
    }
    return c;
  }*/

  /**
   * @see DatabaseImpl#makeDAO(String, String, String)
   * @param exerciseDAO
   */
/*
  void setExerciseDAO(ExerciseDAO<CommonExercise> exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }
*/

  /**
   * Pulls the list of audio recordings out of the database.
   *
   * @param projid here ignored, since in old world there are no project ids
   * @return
   * @see #getExToAudio
   * @see Report#getReport
   */
  @Override
  public Collection<AudioAttribute> getAudioAttributesByProject(int projid) {
    try {
      String sql = SELECT_ALL + " WHERE " + DEFECT + "=false";
      return getResultsSQL(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<>();
  }

  @Override
  Map<Integer, List<AudioAttribute>> getAudioAttributesForExercises(Set<Integer> exids) {
    return null;
  }

  /**
   * Defensively protect against duplicate entries for same audio file.
   *
   * @param exid
   * @return
   * @see #attachAudioToExercise
   */
  protected Collection<AudioAttribute> getAudioAttributesForExercise(int exid) {
    try {
      String sql = SELECT_ALL + " WHERE " + Database.EXID + "='" + exid + "' AND " + DEFECT + "=false";
      Collection<AudioAttribute> resultsSQL = getResultsSQL(sql);
      Set<String> paths = new HashSet<>();

      List<AudioAttribute> ret = new ArrayList<>();

      for (AudioAttribute audioAttribute : resultsSQL) {
        String audioRef = audioAttribute.getAudioRef();
        if (!paths.contains(audioRef)) {
          ret.add(audioAttribute);
          paths.add(audioRef);
        }
        //  else {
        //logger.info("skipping duplicate audio attr " + audioAttribute + " for " + exid);
        //  }
      }
      if (DEBUG) {
        logger.debug("sql for " + exid + " = " + sql + " returned " + ret);
      }
      return ret;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<>();
  }

/*  public Set<String> getRecordedRegularForUser(int userid) {
    return getAudioExercisesForGender(Collections.singleton(userid), REGULAR);
  }*/

  /**
   * Get back the ids of exercises recorded by people who are the same gender as the userid.
   *
   * @param userid only used to determine the gender we should show
   * @return ids with both regular and slow speed recordings
   * @seex mitll.langtest.server.LangTestDatabaseImpl#filterByUnrecorded
   */
  public Set<Integer> getRecordedBy(long userid, Map<Integer, String> exToTranscript) {
/*
    Collection<Long> userMap = getUserIDsMatchingGender(userid);
    //logger.debug("found " + (isMale ? " male " : " female ") + " users : " + userMap.keySet());
    // find set of users of same gender
    Set<String> validAudioAtReg = getAudioForGender(userMap, REGULAR, exToTranscript);
    //logger.debug(" regular speed for " + userMap.keySet() + " " + validAudioAtReg.size());
    Set<String> validAudioAtSlow = getAudioForGender(userMap, SLOW, exToTranscript);
//    logger.debug(" slow speed for " + userMap.keySet() + " " + validAudioAtSlow.size());

    boolean b = validAudioAtReg.retainAll(validAudioAtSlow);
    //  logger.debug("retain all " + b + " " + validAudioAtReg.size());
    return validAudioAtReg;
    */

    return null;

  }

//  public Set<Integer> getWithContext(int userid, Map<Integer, String> exToContext,int projid) {
//    //  return getAudioForGender(getUserIDsMatchingGender(userid), CONTEXT_REGULAR, exToContext);
//    return null;
//  }

  @Override
  Set<Integer> getAudioExercisesForGender(boolean male, String audioSpeed, Map<Integer, String> exToTranscript, int projid) {
    return null;
  }

/*  private Collection<Long> getUserIDsMatchingGender(long userid) {
    User user = userDAO.getUserWhere(userid);
    boolean isMale = (user != null && user.isMale());
    //   logger.info("getUserMapMatchingGender getting users who are " + (isMale ? "male" : "female"));
    return userDAO.getUserIDsMatchingGender(isMale);
  }*/

  protected Set<SlickAudioDAO.Pair> getAudioExercisesForGender(Collection<Integer> userIDs,
                                                               String audioSpeed,
                                                               Map<Integer, String> exToTranscript, int projid) {
    Set<Integer> results = new HashSet<>();
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String s = getInClause(userIDs);
      if (!s.isEmpty()) s = s.substring(0, s.length() - 1);
      String sql = "SELECT distinct " + Database.EXID +
          " FROM " + AUDIO +
          " WHERE " +
          (s.isEmpty() ? "" : USERID + " IN (" + s + ") AND ") +
          DEFECT + "<>true " +
          " AND " + AUDIO_TYPE + "='" +
          audioSpeed +
          "' AND length(" + Database.EXID +
          ") > 0 ";
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String trim = rs.getString(1).trim();
        if (trim.isEmpty()) logger.warn("huh? got empty exid");
        int e = 0;
        try {
          e = Integer.parseInt(trim);
        } catch (NumberFormatException e1) {
          e1.printStackTrace();
        }
        results.add(e);
      }
      //    logger.debug("for " + audioSpeed + " " + sql + " yielded " + results.size());
      finish(connection, statement, rs, sql);

    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  @Override
  Set<Integer> getAudioExercisesForGenderBothSpeeds(boolean isMale, String regSpeed, String slowSpeed, Map<Integer, String> exToTranscript, int projid) {
    return null;
  }


  /**
   * select childCount(*) from (select childCount(*) from (select DISTINCT exid, audiotype from audio where length(exid) > 0 and audiotype='regular' OR audiotype='slow' and defect<>true) where length(exid) > 0 group by exid)
   *
   * @param projid
   * @param audioSpeed
   * @param uniqueIDs
   * @param idsOfRecordedExercisesForFemales
   * @return
   * @see #getRecordedReport
   */
  protected void getCountForGender(int projid, AudioType audioSpeed, Set<Integer> uniqueIDs,
                                   Map<Integer, String> exToTranscript,
                                   Set<Integer> idsOfRecordedExercisesForMales, Set<Integer> idsOfRecordedExercisesForFemales) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String s = getInClause(Collections.EMPTY_SET);
      // logger.info("checking speed " + audioSpeed + " on " + userIds.size() + " users and " + uniqueIDs.size() + " ex ids");
      if (!s.isEmpty()) s = s.substring(0, s.length() - 1);
      String sql = "select " +
          "distinct " + Database.EXID + ", " +
          TRANSCRIPT +
          " from " + AUDIO +
          " WHERE " +
          (s.isEmpty() ? "" : USERID + " IN (" + s + ") AND ") +
          DEFECT + "<>true " + "AND " +
          DNR + ">0" + " AND " +
          AUDIO_TYPE + "='" + audioSpeed + "' ";

      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();

      boolean checkAudioTranscript = database.getServerProps().shouldCheckAudioTranscript();

      while (rs.next()) {
        String exid = rs.getString(1);
        String exerciseFL = exToTranscript.get(exid);

        if (exerciseFL != null || !checkAudioTranscript) {
          String transcript = rs.getString(2);

          boolean isMatch = !checkAudioTranscript || isNoAccentMatch(transcript, exerciseFL);
          if (isMatch) {
            if (uniqueIDs.contains(exid)) {
              try {
                int e = Integer.parseInt(exid);
                idsOfRecordedExercisesForMales.add(e);
              } catch (NumberFormatException e1) {
                logger.warn(e1);
              }
            } else {
//              idsOfStaleExercises.add(exid);
              logger.debug("getCountForGender skipping stale exid " + exid);
            }
          }
          //else {
          //  logger.info("1) no match for " + exid + " '" + trimWhitespace(transcript) + "' vs '" + trimWhitespace(exerciseFL) + "'");
          //}
        }
        //else {
        //        logger.info("2) stale exercise id : no match for " + exid);
        // }
      }
      finish(connection, statement, rs, sql);
/*      logger.debug("getCountForGender : for " + audioSpeed + "\n\t" + sql + "\n\tgot " + idsOfRecordedExercises.size() +
          " and stale " +idsOfStaleExercises);*/
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
   // return idsOfRecordedExercisesForMales.size();
  }

  /**
   * @param userIds
   * @param uniqueIDs
   * @return
   * @see BaseAudioDAO#getRecordedReport
   */
  protected int getCountBothSpeeds(Set<Integer> userIds,
                                   Set<Integer> uniqueIDs) {
    Set<String> results = new HashSet<>();

    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String s = getInClause(userIds);
      if (!s.isEmpty()) s = s.substring(0, s.length() - 1);
//      String sql2 =
//          "select childCount(count1) from " +
//              " (select childCount(*) as count1 from " +
//              " (select DISTINCT exid, audiotype from " +
//              AUDIO +
//              " where length(exid) > 0 and audiotype='regular' OR audiotype='slow' and defect<>true " +
//              (s.isEmpty() ? "" : "AND " + USERID + " IN (" + s + ") ") +
//              ") where length(exid) > 0 group by exid) where count1 = 2";
////        ;

      String sql = "select exid from " +
          "(select exid,count(*) as count1 from " +
          "(select DISTINCT exid, audiotype from audio " +
          "where length(exid) > 0 and audiotype='regular' OR audiotype='slow' and defect<>true " +
          (s.isEmpty() ? "" : "AND " + USERID + " IN (" + s + ") ") +
          ") " +
          "where length(exid) > 0 group by exid) where count1 = 2";

      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String id = rs.getString(1);
        if (uniqueIDs.contains(id)) {
          results.add(id);
        }
      }
      finish(connection, statement, rs, sql);

    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    //logger.debug("both speeds " + results.size());
    return results.size();
  }

  private String getInClause(Collection<Integer> longs) {
    StringBuilder buffer = new StringBuilder();
    for (int id : longs) {
      buffer.append(id).append(",");
    }
    return buffer.toString();
  }


  protected Set<Integer> getValidAudioOfType(int userid, AudioType audioType) {
    String sql = "SELECT " + Database.EXID +
        " FROM " + AUDIO + " WHERE " + USERID + "=" + userid +
        " AND " + DEFECT + "<>true " +
        " AND " + AUDIO_TYPE + "='" + audioType + "'";

    // logger.debug("sql " + sql);
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      return getExidResultsForQuery(connection, statement);
    } catch (SQLException e) {
      logger.error("got " + e, e);
      return Collections.emptySet();
    }
  }

  /**
   * @param sql
   * @return
   * @throws SQLException
   * @see #getAudioAttributesByProject
   * @see BaseAudioDAO#getAudioAttributesForExercise(int)
   */
  private List<AudioAttribute> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    return getResultsForQuery(connection, statement);
  }

  private int c = 0;

  /**
   * Get a list of audio attributes for this Query.
   *
   * @param connection
   * @param statement
   * @return
   * @throws java.sql.SQLException
   * @see #getResultsSQL(String)
   */
  private List<AudioAttribute> getResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<AudioAttribute> results = new ArrayList<>();
    Map<Integer, MiniUser> miniUsers = userDAO.getMiniUsers();

    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      Integer userID = rs.getInt(USERID);
      String exid = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String audioRef = rs.getString(AUDIO_REF);

      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);
      String transcript = rs.getString(TRANSCRIPT);
      Float dnr = rs.getFloat(DNR);

      MiniUser user = miniUsers.get(userID);
      user = checkDefaultUser(userID, user);
      AudioType realType;
      try {
        realType = AudioType.valueOf(type.toUpperCase().replace("=", "_"));
      } catch (IllegalArgumentException e) {
        logger.warn("no audio type for " + type);
        realType = AudioType.UNSET;
      }

      int exid1 = 0;
      try {
        exid1 = Integer.parseInt(exid);
      } catch (NumberFormatException e) {
        // logger.warn("got " +e + " for " +exid);
      }
      AudioAttribute audioAttr = new AudioAttribute(uniqueID, userID, //id
          exid1, // id
          audioRef, // answer
          timestamp.getTime(),
          dur, realType,
          user, transcript,
          audioRef,
          dnr,
          -1);
      audioAttr.setOldexid(exid);

      if (user == null) {
        if (c++ < 20) {
          logger.warn("can't find user " + userID + " for " + audioAttr + " in " + miniUsers.keySet());
        }
      }
      results.add(audioAttr);
    }
    //   logger.debug("found " + results.size() + " audio attributes");

    finish(connection, statement, rs,"");

    return results;
  }

  private Set<Integer> getExidResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    Set<Integer> results = new HashSet<>();
    while (rs.next()) {
      String string = rs.getString(Database.EXID);
      results.add(Integer.parseInt(string));
    }
    finish(connection, statement, rs,"");

    return results;
  }

  /**
   * @seex mitll.langtest.server.database.ImportCourseExamples#copyAudio
   */
  public long add(Result result, int userid, String path) {
    try {
      long then = System.currentTimeMillis();
      //  logger.debug("path was " + result.answer + " now '" + audioRef + "'");
      if (userid < 1) {
        logger.error("huh? userid is " + userid);
        new Exception().printStackTrace();
      }

      long newid = add(connection, result, userid, path, "unknownTranscript");
      database.closeConnection(connection);
      long now = System.currentTimeMillis();
      if (now - then > 100) System.out.println("took " + (now - then) + " millis to record answer.");
      return newid;

    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    }
    return -1;
  }

  /**
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
   * <p>
   * <p>
   * "id IDENTITY, " +
   * "userid INT, " +
   * Database.EXID + " VARCHAR, " +
   * Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
   * "audioRef CLOB," +
   * AUDIO_TYPE + " VARCHAR," +
   * DURATION + " INT" +
   *
   * @param connection
   * @param transcript
   * @throws java.sql.SQLException
   * @see #add(Result, int, String)
   */
  private long add(Connection connection, Result result, int userid, String audioRef, String transcript) throws SQLException {
    int exerciseID = result.getExerciseID();
    long timestamp = result.getTimestamp();
    AudioType audioType = result.getAudioType();
    long durationInMillis = result.getDurationInMillis();
    logger.debug("add result - " + result.getCompoundID() + " for " + userid + " ref " + audioRef);
    return addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis, transcript);
  }


  /**
   * Why does this have to be so schizo? add or update -- should just choose
   *
   * @paramx userid           part of unique id
   * @paramx exerciseID       part of unique id
   * @paramx projid
   * @paramx audioType        part of unique id
   * @paramx audioRef
   * @paramx timestamp
   * @paramx durationInMillis
   * @paramx transcript       @return AudioAttribute that represents the audio that has been added to the exercise
   * @paramx dnr
   * @see BaseAudioDAO#addOrUpdateUser(int, int, AudioAttribute)
   */
  protected void addOrUpdateUser(AudioInfo info) {
    int userid = info.getUserid();
    if (isBadUser(info.getUserid())) {
      logger.error("huh? userid is " + info.getUserid());
      new Exception().printStackTrace();
    }
    try {
//      logger.debug("addOrUpdate userid = " + userid + " audio ref " + audioRef + " ex " + exerciseID + " at " +
//          new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO + " " +
          "SET " + USERID + "=? " +
          "WHERE " +
          Database.EXID + "=?" + " AND " +
          AUDIO_TYPE + "=? AND " +
          DEFECT + "=FALSE AND " +
          AUDIO_REF + "=?";

      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setInt(ii++, info.getUserid());
      int exerciseID = info.getExerciseID();
      statement.setString(ii++, "" + exerciseID);
      AudioType audioType = info.getAudioType();
      statement.setString(ii++, audioType.toString());
      String audioRef = info.getAudioRef();
      statement.setString(ii++, audioRef);

      int i = statement.executeUpdate();

      if (i == 0) { // so we didn't update, so we need to add it
//        logger.debug("\taddOrUpdate adding entry for  " + userid + " " + audioRef + " ex " + exerciseID +
//            " at " + new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);

        long l = addAudio(connection, info.getUserid(), audioRef, exerciseID, info.getTimestamp(), audioType, info.getDurationInMillis(), info.getTranscript());
      }

      finish(connection, statement);

    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * TODO : Why does this have to be so schizo? add or update -- should just choose?
   * <p>
   * This guarantees that there will only be one row in the audio table for the key "user-exid-speed",
   * e.g. user 20 exid 5 and speed "regular" will only appear once if it's not defective.
   *
   * @paramx userid           part of unique id
   * @paramx exerciseID       part of unique id
   * @paramx projid
   * @paramx audioType        part of unique id
   * @paramx audioRef
   * @paramx timestamp
   * @paramx durationInMillis
   * @paramx transcript
   * @paramx dnr
   * @return AudioAttribute that represents the audio that has been added to the exercise
   * @see mitll.langtest.server.services.AudioServiceImpl#addToAudioTable
   */
  @Override
  public AudioAttribute addOrUpdate(AudioInfo info) {
  //  int userid,
 // } int exerciseID, int projid, AudioType audioType, String audioRef, long timestamp,
   //                                 long durationInMillis, String transcript, float dnr) {
//    if (isBadUser(userid)) {
//      logger.error("huh? userid is " + userid, new Exception("huh? userid is " + userid));
//    }
    try {
      //logger.debug("addOrUpdate " + userid + " " + audioRef + " ex " + exerciseID + " at " + new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO +
          " SET " +
          AUDIO_REF + "=?," +
          Database.TIME + "=?," +
          DURATION + "=?, " +
          TRANSCRIPT + "=? " +

          "WHERE " +
          Database.EXID + "=? AND " +
          USERID + "=? AND " +
          AUDIO_TYPE + "=? AND " +
          DEFECT + "=FALSE";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;
/*
      statement.setString(ii++, audioRef);
      statement.setTimestamp(ii++, new Timestamp(timestamp));
      statement.setInt(ii++, (int) durationInMillis);
      statement.setString(ii++, transcript);

      statement.setString(ii++, "" + exerciseID);
      statement.setInt(ii++, userid);
      statement.setString(ii++, audioType.toString());

      int i = statement.executeUpdate();

      AudioAttribute audioAttr;
      if (i == 0) {
//        logger.debug("\taddOrUpdate *adding* entry for  " + userid + " " + audioRef + " ex " + exerciseID +// " at " + new Date(timestamp) +
//            " type " + audioType + " dur " + durationInMillis);

        long l = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis, transcript);
        audioAttr = getAudioAttribute((int) l, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis, transcript, dnr);
      } else {
//        logger.debug("\taddOrUpdate updating entry for  " + userid + " " + audioRef + " ex " + exerciseID +
//            " type " + audioType + " dur " + durationInMillis);
        audioAttr = getAudioAttribute(userid, exerciseID, audioType);
      }*/
      //  logger.debug("returning " + audioAttr);
      finish(connection, statement);

      return null;//audioAttr;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  @Override
  public void updateExerciseID(int uniqueID, int exerciseID, String path) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO +
          " SET " +
          Database.EXID +
          "=? " +
          "WHERE " +
          ID + "=?";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, "" + exerciseID);
      statement.setInt(ii++, uniqueID);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? couldn't update " + uniqueID + " to " + exerciseID);
      }

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public void updateDNR(int uniqueID, float dnr) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO +
          " SET " +
          DNR +
          "=? " +
          "WHERE " +
          ID + "=?";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setFloat(ii++, dnr);
      statement.setInt(ii++, uniqueID);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? couldn't update audio " + uniqueID + " to " + dnr);
      }

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  @Override
  public boolean didFindAnyAudioFiles(int projectid) {
    return true;
  }

  @Override
  public void makeSureAudioIsThere(int projectID, String language, boolean validateAll) {
  }

  @Override
  public String getNativeAudio(Map<Integer, MiniUser.Gender> userToGender, int userid, CommonExercise exercise) {
    return null;
  }

  @Override
  public Map<String, Integer> getPairs(int projid) {
    return null;
  }

  @Override
  public AudioAttribute getByID(int audioID) {
    return null;
  }

  /**
   * An audio cut is uniquely identified by by exercise id, speed (reg/slow), and who recorded it.
   *
   * @param userid     recorded by this user
   * @param exerciseID on this exercise
   * @param audioType  at this speed
   * @return > 0 if audio was marked defective
   * @see mitll.langtest.server.database.DatabaseImpl#editItem
   * @seex mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  protected int markDefect(int userid, int exerciseID, AudioType audioType) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO +
          " " +
          "SET " +
          DEFECT +
          "=TRUE" +
          " WHERE " +
          Database.EXID + "=?" + " AND " +
          USERID + "=?" + " AND " +
          AUDIO_TYPE + "=?";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, "" + exerciseID);
      statement.setInt(ii++, userid);
      statement.setString(ii++, audioType.toString());

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? couldn't find audio by " + userid + " for ex " + exerciseID + " and " + audioType);
      } else {
        logger.debug("Num modified = " + i + ", marked audio defect by " + userid + " ex " + exerciseID + " speed " + audioType);
      }

      finish(connection, statement);
      return i;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return -1;
  }

  /**
   * @param connection
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @param transcript
   * @return key from insert
   * @throws SQLException
   * @see #add(Connection, Result, int, String, String)
   */
  private long addAudio(Connection connection, int userid, String audioRef,
                        int exerciseID, long timestamp,
                        AudioType audioType, long durationInMillis, String transcript) throws SQLException {
    if (isBadUser(userid)) {
      logger.error("huh? userid is " + userid);
      new Exception().printStackTrace();
    }

    // logger.debug("addAudio : by " + userid + " for ex " + exerciseID + " type " + audioType + " ref " + audioRef);
    int before = DEBUG ? getCount(AUDIO) : 0;

    PreparedStatement statement = connection.prepareStatement("INSERT INTO " + AUDIO +
        "(" +
        USERID + "," +
        Database.EXID + "," +
        Database.TIME + "," +
        AUDIO_REF + "," +
        AUDIO_TYPE + "," +
        DURATION + "," +
        TRANSCRIPT + "," +
        DEFECT +
        ") VALUES(?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

    int i = 1;

    statement.setInt(i++, userid);
    statement.setString(i++, "" + exerciseID);
    statement.setTimestamp(i++, new Timestamp(timestamp));
    statement.setString(i++, audioRef);
    statement.setString(i++, audioType.toString());
    statement.setLong(i++, durationInMillis);
    statement.setString(i++, transcript);
    statement.setBoolean(i++, false);

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);
    if (newID == -1) {
      logger.error("addAudio : huh? no key was generated?");
    } else {
      //logger.debug("key was " + newID);
    }

    statement.close();
    connection.commit();

    int after = DEBUG ? getCount(AUDIO) : 1;
    if (DEBUG && before == after) {
      logger.error("huh? after adding " + after + " but before " + before);
    }
    return newID;
  }

  /**
   * So we don't want to use CURRENT_TIMESTAMP as the default for TIMESTAMP
   * b/c if we ever alter the table, say by adding a new column, we will effectively lose
   * the timestamp that was put there when we inserted the row initially.
   * <p></p>
   * Note that the answer column can be either the text of an answer for a written response
   * or a relative path to an audio file on the server.
   *
   * @param connection to make a statement from
   * @throws java.sql.SQLException
   */
  private void createTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        AUDIO +
        " (" +
        ID +
        " IDENTITY, " +
        USERID +
        " INT, " +
        Database.EXID + " VARCHAR, " +
        Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
        "audioRef CLOB," +
        AUDIO_TYPE + " VARCHAR," +
        DURATION + " INT, " +
        DEFECT + " BOOLEAN DEFAULT FALSE, " +
        TRANSCRIPT + " VARCHAR" +
        ")");
    statement.execute();
    statement.close();
    index(database);
  }

  private void index(Database database) throws SQLException {
    createIndex(database, Database.EXID, AUDIO);
  }

  MiniUser checkDefaultUser(long userID, MiniUser user) {
    if (userID == BaseUserDAO.DEFAULT_USER_ID) {
      user = BaseUserDAO.DEFAULT_USER;
    } else if (userID == BaseUserDAO.DEFAULT_MALE_ID) {
      user = BaseUserDAO.DEFAULT_MALE;
    } else if (userID == BaseUserDAO.DEFAULT_FEMALE_ID) {
      user = BaseUserDAO.DEFAULT_FEMALE;
    }
    return user;
  }
}