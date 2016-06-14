/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.JsonSupport;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.word.WordDAO;
import mitll.langtest.shared.analysis.PhoneAndScore;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhoneDAO extends BasePhoneDAO implements IPhoneDAO<Phone> {
  private static final Logger logger = Logger.getLogger(PhoneDAO.class);
  //  private static final int MAX_EXAMPLES = 30;

  private static final String RID = "rid";
  private static final String WID = "wid";

  /**
   * @param database
   * @see DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public PhoneDAO(Database database) {
    super(database);
    initialSetup(database);
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

  /**
   * <p>
   *
   * @param phone
   * @see DatabaseImpl#recordWordAndPhoneInfo(long, Map)
   */
  @Override
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
   * @param idToRef
   * @return
   * @throws SQLException
   * @see Analysis#getPhonesForUser
   */
  @Override
  public PhoneReport getWorstPhonesForResults(long userid, List<Integer> ids, Map<String, String> idToRef) throws SQLException {
    return getPhoneReport(getResultIDJoinSQL(userid, ids), idToRef, true, false);
  }

  /**
   * @param userid
   * @param exids
   * @param idToRef
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getJsonPhoneReport
   * @see JsonSupport#getJsonPhoneReport(long, Map)
   */
  public JSONObject getWorstPhonesJson(long userid, List<String> exids, Map<String, String> idToRef) {
    return new PhoneJSON().getWorstPhonesJson(getPhoneReport(userid, exids, idToRef));
  }

  /**
   * @param userid
   * @param exids
   * @param idToRef
   * @return
   */
  protected PhoneReport getPhoneReport(long userid, List<String> exids, Map<String, String> idToRef) {
    PhoneReport worstPhonesAndScore = null;
    try {
      worstPhonesAndScore = getPhoneReport(getJoinSQL(userid, exids), idToRef, false, true);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return worstPhonesAndScore;
  }

  /**
   * TODO : huh? doesn't seem to add last item to total score or total items?
   *
   * @param sql
   * @param idToRef
   * @param addTranscript       true if going to analysis tab
   * @param sortByLatestExample
   * @return
   * @throws SQLException
   * @see #getWorstPhonesForResults(long, List, Map)
   * @see #getPhoneReport(String, Map, boolean, boolean)
   */
  protected PhoneReport getPhoneReport(String sql,
                                       Map<String, String> idToRef,
                                       boolean addTranscript,
                                       boolean sortByLatestExample) throws SQLException {
    // logger.debug("getPhoneReport query is " + sql);
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();

    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<>();

    String currentExercise = "";
    Map<String, List<WordAndScore>> phoneToWordAndScore = new HashMap<>();

    float totalScore = 0;
    float totalItems = 0;

    Map<String, Map<NetPronImageType, List<TranscriptSegment>>> stringToMap = new HashMap<>();
    while (rs.next()) {
      int i = 1;

      // info from result table
      String exid = rs.getString(i++);
      String audioAnswer = rs.getString(i++);
      String scoreJson = rs.getString(i++);
      float pronScore = rs.getFloat(i++);

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
        //logger.debug("adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }

      WordAndScore wordAndScore = getAndRememberWordAndScore(idToRef, phoneToScores, phoneToWordAndScore,
          exid, audioAnswer, scoreJson, resultTime,
          wseq, word,
          rid, phone, seq, phoneScore);

      if (addTranscript) {
        addTranscript(stringToMap, scoreJson, wordAndScore);
      }
      //    } else {
     /*   logger.debug("------> current " + currentRID +
            " skipping " + exid + " " + rid + " word " + word + "<-------------- ");*/
      //  }
    }
    finish(connection, statement, rs);

    return new MakePhoneReport().getPhoneReport(phoneToScores, phoneToWordAndScore, totalScore, totalItems, sortByLatestExample);
  }

  /**
   * @param userid
   * @paramx exids
   * @paramx sortByLatestExample
   * @return
   * @throws SQLException
   * @see #getPhoneReport(long, List, Map)
   */
/*  private PhoneReport getWorstPhones(long userid, List<String> exids, Map<String, String> idToRef) throws SQLException {
    return getPhoneReport(getJoinSQL(userid, exids), idToRef, false, true);
  }*/

  private String getResultIDJoinSQL(long userid, List<Integer> ids) {
    String filterClause = ResultDAO.RESULTS + "." + ResultDAO.ID + " in (" + getInList(ids) + ")";
    return getJoinSQL(userid, filterClause);
  }

  private String getJoinSQL(long userid, List<String> exids) {
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

  public Collection<Phone> getAll() {
    Connection connection = getConnection();

    List<Phone> all = new ArrayList<>();
    try {
      PreparedStatement statement = connection.prepareStatement("select * from " + PHONE);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        long rid = rs.getLong(RID1);
        long wid = rs.getLong(WID);
        //      logger.info("Got " + exid + " rid " + rid + " word " + word);
        String phone = rs.getString(PHONE);
        int seq = rs.getInt(SEQ);
        float phoneScore = rs.getFloat(SCORE);
        all.add(new Phone(rid, wid, phone, seq, phoneScore));
      }
      finish(connection, statement, rs);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return all;
  }
}