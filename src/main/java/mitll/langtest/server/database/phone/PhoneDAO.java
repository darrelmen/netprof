/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.phone;

import com.google.gson.JsonObject;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.word.WordDAO;
import mitll.langtest.shared.analysis.PhoneBigrams;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.PhoneSummary;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 *
 * @deprecated
 */
public class PhoneDAO extends BasePhoneDAO implements IPhoneDAO<Phone> {
  private static final Logger logger = LogManager.getLogger(PhoneDAO.class);
  private static final String RID = "rid";
  private static final String WID = "wid";

  /**
   * @param database
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyPhone
   */
  public PhoneDAO(Database database) {
    super(database);
    initialSetup(database);
  }

  @Override
  public PhoneReport getWorstPhonesForResultsForTimeWindow(int userid, Collection<Integer> ids, Project project, long from, long to) {
    return null;
  }

  @Override
  public PhoneSummary getPhoneSummary(int userid, Collection<Integer> ids) {
    return null;
  }

  @Override
  public PhoneBigrams getPhoneBigrams(int userid, Collection<Integer> ids) {
    return null;
  }

  @Override
  public int deleteForProject(int projID) {
    return 0;
  }

  @Override
  public boolean updateProjectForRID(int rid, int newprojid) {
    return false;
  }

  private void initialSetup(Database database) {
    try {
      createTable(database);
      createIndex(database, RID, PHONE);
      createIndex(database, WID, PHONE);
      database.closeConnection(database.getConnection(this.getClass().toString())); //why?
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * Word – result id – word seq – score – uid
   * Do we care about start and end offsets into audio???
   *
   * @param database
   * @throws java.sql.SQLException
   */
  private void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        PHONE +
        " (" +
        "ID IDENTITY, " +
        RID + " BIGINT, " +
        WID + " BIGINT, " +
        PHONE + " VARCHAR, " +
        SEQ + " INT, " +
        SCORE + " FLOAT, " +

        "FOREIGN KEY(" +
        RID +
        ") REFERENCES " +
        ResultDAO.RESULTS +
        "(ID)," +

        "FOREIGN KEY(" +
        WID +
        ") REFERENCES " +
        WordDAO.WORD +
        "(ID)" +

        ")");

    finish(database, connection, statement);
  }

  @Override
  public void addBulkPhones(List<Phone> bulk, int projID) {
  }

  /**
   * <p>
   *
   * @param phone
   * @see DatabaseServices#recordWordAndPhoneInfo
   */
  // @Override
  public boolean addPhone(Phone phone) {
    Connection connection = getConnection();
    boolean val = true;
    try {
      // there are much better ways of doing this...
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + PHONE +
              "(" +
              RID + "," +
              WID + "," +
              PHONE + "," +
              SEQ + "," +
              SCORE +
              //"," +
              ") " +
              "VALUES(?,?,?,?,?)");
      int i = 1;

      statement.setLong(i++, phone.getRid());
      statement.setLong(i++, phone.getWid());
      statement.setString(i++, phone.getPhone());
      statement.setInt(i++, phone.getSeq());
      statement.setFloat(i++, phone.getScore());

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
        val = false;
      }
      statement.close();

    } catch (SQLException ee) {
      logger.error("trying to add event " + phone + " got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
      val = false;
    } finally {
      database.closeConnection(connection);
    }
    return val;
  }

  /**
   * @param userid
   * @param ids
   * @param project
   * @return
   * @throws SQLException
   * @seex Analysis#getPhonesForUser
   */
 /* @Override
  public PhoneReport getWorstPhonesForResults(int userid, Collection<Integer> ids, Project project) {
    try {
      return getPhoneReport(getResultIDJoinSQL(userid, ids), true, false);
    } catch (Exception e) {
      logAndNotify.logAndNotifyServerException(e, "sql exception for user " + userid + " and result ids " + ids);
      return new PhoneReport();
    }
  }
*/
  @Override
  public PhoneReport getWorstPhonesForResultsForPhone(int userid, Collection<Integer> ids, Project project, String phone, long from, long to) {
    return null;
  }

  @Override
  public void removeForResult(int resultid) {

  }

  /**
   * @param userid
   * @param exids
   * @param language
   * @param project
   * @return
   * @seex JsonSupport#getJsonPhoneReport(long, int, Map)
   * @see mitll.langtest.server.database.DatabaseImpl#getJsonPhoneReport
   */
  public JsonObject getWorstPhonesJson(int userid, Collection<Integer> exids, String language, Project project) {
    PhoneReport phoneReport = getPhoneReport(userid, exids, null);
    logger.info("getWorstPhonesJson phone report " + phoneReport);
    return new PhoneJSON().getWorstPhonesJson(phoneReport);
  }

  /**
   * @param userid
   * @param exids
   * @param idToRef
   * @return
   */
  protected PhoneReport getPhoneReport(long userid, Collection<Integer> exids, Map<Integer, String> idToRef) {
    PhoneReport worstPhonesAndScore = null;
    try {
      worstPhonesAndScore = getPhoneReport(getJoinSQL(userid, exids), false, true);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return worstPhonesAndScore;
  }

  /**
   * TODO : huh? doesn't seem to add last item to total score or total items?
   *
   * @param sql
   * @param addTranscript       true if going to analysis tab
   * @param sortByLatestExample
   * @return
   * @throws SQLException
   * @see IPhoneDAO#getWorstPhonesForResults(int, Collection, Project)
   * @see #getPhoneReport(String, boolean, boolean)
   */
  protected PhoneReport getPhoneReport(String sql,
                                       boolean addTranscript,
                                       boolean sortByLatestExample) throws SQLException {
    //   logger.debug("getPhoneSummary query is\n" + sql);
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();

    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<>();

    String currentExercise = "";
    Map<String, List<WordAndScore>> phoneToWordAndScore = new HashMap<>();

    float totalScore = 0;
    float totalItems = 0;

    int c = 0;
    Map<String, Map<NetPronImageType, List<TranscriptSegment>>> stringToMap = new HashMap<>();
    while (rs.next()) {
      int i = 1;

      c++;
      // info from result table
      String exid = rs.getString(i++);
      String audioAnswer = rs.getString(i++);
      String scoreJson = rs.getString(i++);
      float pronScore = rs.getFloat(i++);

      //   logger.info("#"+ c + " : " + exid + " audio " + audioAnswer);

      long resultTime = -1;
      Timestamp timestamp = rs.getTimestamp(i++);
      if (timestamp != null) resultTime = timestamp.getTime();

      // info from word table
      int wseq = rs.getInt(i++);
      String word = rs.getString(i++);
      /*float wscore =*/ //rs.getFloat(i++);

      // info from phone table
      long rid = rs.getLong(RID1);
//      logger.info("Got " + exid + " rid " + rid + " word " + word);
      String phone = rs.getString(PHONE);
      int seq = rs.getInt(SEQ);
      float phoneScore = rs.getFloat(SCORE);

      if (!exid.equals(currentExercise)) {
        currentExercise = exid;
        if (false) logger.debug("#" + c + " adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }

      try {
        WordAndScore wordAndScore = null;

//            getAndRememberWordAndScore(null, phoneToScores, phoneToWordAndScore,
//            Integer.parseInt(exid), audioAnswer, scoreJson, resultTime,
//            "", wseq, word,
//            (int)rid, phone, seq, phoneScore, database.getLanguage());

        if (addTranscript) {
          addTranscript(stringToMap, scoreJson, wordAndScore, Language.UNKNOWN);
        }
      } catch (NumberFormatException e) {
        logger.warn("got " + e + " for " + exid);
      }
      //    } else {
     /*   logger.debug("------> current " + currentRID +
            " skipping " + exid + " " + rid + " word " + word + "<-------------- ");*/
      //  }
    }
    finish(connection, statement, rs, sql);

    return null;//new MakePhoneReport().getPhoneSummary(phoneToScores, null, bigramToCount, bigramToScore, totalScore, totalItems);
  }

  /**
   * @param userid
   * @return
   * @throws SQLException
   * @paramx exids
   * @paramx sortByLatestExample
   * @see #getPhoneReport
   */
/*  private PhoneReport getWorstPhones(long userid, List<String> exids, Map<String, String> idToRef) throws SQLException {
    return getPhoneSummary(getJoinSQL(userid, exids), idToRef, false, true);
  }*/
  private String getResultIDJoinSQL(long userid, Collection<Integer> ids) {
    String filterClause = ResultDAO.RESULTS + "." + ResultDAO.ID + " in (" + getInList(ids) + ")";
    return getJoinSQL(userid, filterClause);
  }

  private String getJoinSQL(long userid, Collection<Integer> exids) {
    String filterClause = ResultDAO.RESULTS + "." + Database.EXID + " in (" + getInList(exids) + ")";
    return getJoinSQL(userid, filterClause);
  }

  private String getJoinSQL(long userid, String filterClause) {
    return "select " +
        "results.exid," +
        "results.answer," +
        "results." + ResultDAO.SCORE_JSON + "," +
        "results." + ResultDAO.PRON_SCORE + "," +
        "results.time,  " +

        "word.seq, " +
        "word.word, " +
//        "word.score wordscore, " +
        "phone.* " +

        " from " +
        "results, phone, word " +

        "where " +

        "results.id = phone.rid " + "AND " +
        ResultDAO.RESULTS + "." + ResultDAO.USERID + "=" + userid + " AND " +
        filterClause +
        " AND phone.wid = word.id " +
        " order by results.exid, results.time desc";
  }

  /**
   * For old h2 world we don't have phone duration.
   *
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyPhone
   */
  public Collection<Phone> getAll(int projid) {
    Connection connection = getConnection();

    List<Phone> all = new ArrayList<>(1000000);
    try {
      String sql = "SELECT * FROM " + PHONE;
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        long rid = rs.getLong(RID1);
        long wid = rs.getLong(WID);
        //      logger.info("Got " + exid + " rid " + rid + " word " + word);
        String phone = rs.getString(PHONE);
        int seq = rs.getInt(SEQ);
        float phoneScore = rs.getFloat(SCORE);
        float durationSeconds;
        try {
          durationSeconds = rs.getFloat(DURATION);
        } catch (Exception e) {
          durationSeconds = 0;
        }
        int durationMillis = Float.valueOf(durationSeconds * 1000f).intValue();
        all.add(new Phone(projid, (int) rid, (int) wid, phone, seq, phoneScore, durationMillis));
      }
      finish(connection, statement, rs, sql);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return all;
  }
}