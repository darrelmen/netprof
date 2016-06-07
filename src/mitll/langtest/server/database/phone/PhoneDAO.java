/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.phone;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.*;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.word.WordDAO;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.PhoneAndScore;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
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
public class PhoneDAO extends DAO implements IPhoneDAO<Phone> {
  //  private static final int MAX_EXAMPLES = 30;
  private static final Logger logger = Logger.getLogger(PhoneDAO.class);

  private static final String PHONE = "phone";
  private static final String RID = "rid";
  private static final String WID = "wid";
  private static final String SEQ = "seq";
  private static final String SCORE = "score";

  //private static final boolean DEBUG = false;
  private static final String RID1 = "RID";
  private ParseResultJson parseResultJson;

  /**
   * @param database
   * @see DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public PhoneDAO(Database database) {
    super(database);
    parseResultJson = new ParseResultJson(database.getServerProps());
    initialSetup(database);
  }

  protected void initialSetup(Database database) {
    try {
      createTable(database);
      createIndex(database, RID, PHONE);
      createIndex(database, WID, PHONE);
      Connection connection = database.getConnection(this.getClass().toString());
      database.closeConnection(connection);
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
  private PhoneReport getPhoneReport(long userid, List<String> exids, Map<String, String> idToRef) {
    PhoneReport worstPhonesAndScore = null;
    try {
      worstPhonesAndScore = getPhoneReport(getJoinSQL(userid, exids), idToRef, false, true);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return worstPhonesAndScore;
  }

  /**
   * @param userid
   * @param ids
   * @param idToRef
   * @return
   * @throws SQLException
   * @see Analysis#getPhonesForUser
   */
  public PhoneReport getWorstPhonesForResults(long userid, List<Integer> ids, Map<String, String> idToRef) throws SQLException {
    return getPhoneReport(getResultIDJoinSQL(userid, ids), idToRef, true, false);
  }

  /**
   * @param userid
   * @param exids
   * @param sortByLatestExample
   * @return
   * @throws SQLException
   * @see #getPhoneReport(long, List, Map)
   */
/*  private PhoneReport getWorstPhones(long userid, List<String> exids, Map<String, String> idToRef) throws SQLException {
    return getPhoneReport(getJoinSQL(userid, exids), idToRef, false, true);
  }*/

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
  private PhoneReport getPhoneReport(String sql, Map<String, String> idToRef,
                                     boolean addTranscript, boolean sortByLatestExample) throws SQLException {
    // logger.debug("getPhoneReport query is " + sql);
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();

    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<String, List<PhoneAndScore>>();

    String currentExercise = "";
    Map<String, List<WordAndScore>> phoneToWordAndScore = new HashMap<String, List<WordAndScore>>();

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

  private void addTranscript(Map<String, Map<NetPronImageType, List<TranscriptSegment>>> stringToMap, String scoreJson, WordAndScore wordAndScore) {
    Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = stringToMap.get(scoreJson);
    if (netPronImageTypeListMap == null) {
      netPronImageTypeListMap = parseResultJson.parseJson(scoreJson);
      stringToMap.put(scoreJson, netPronImageTypeListMap);
    } else {
      // logger.debug("cache hit " + scoreJson.length());
    }

    setTranscript(wordAndScore, netPronImageTypeListMap);
  }

  private WordAndScore getAndRememberWordAndScore(Map<String, String> idToRef,
                                                  Map<String, List<PhoneAndScore>> phoneToScores,
                                                  Map<String, List<WordAndScore>> phoneToWordAndScore,
                                                  String exid,
                                                  String audioAnswer,
                                                  String scoreJson,
                                                  long resultTime,
                                                  int wseq, String word,
                                                  long rid, String phone, int seq, float phoneScore) {
    PhoneAndScore phoneAndScore = getAndRememberPhoneAndScore(phoneToScores, phone, phoneScore, resultTime);

    List<WordAndScore> wordAndScores = phoneToWordAndScore.get(phone);
    if (wordAndScores == null) {
      phoneToWordAndScore.put(phone, wordAndScores = new ArrayList<WordAndScore>());
    }

    WordAndScore wordAndScore = new WordAndScore(exid, word, phoneScore, rid, wseq, seq, trimPathForWebPage(audioAnswer),
        idToRef.get(exid), scoreJson, resultTime);

    wordAndScores.add(wordAndScore);
    phoneAndScore.setWordAndScore(wordAndScore);
    return wordAndScore;
  }

  private PhoneAndScore getAndRememberPhoneAndScore(Map<String, List<PhoneAndScore>> phoneToScores,
                                                    String phone, float phoneScore, long resultTime) {
    List<PhoneAndScore> scores = phoneToScores.get(phone);
    if (scores == null) phoneToScores.put(phone, scores = new ArrayList<PhoneAndScore>());
    PhoneAndScore phoneAndScore = new PhoneAndScore(phoneScore, resultTime);
    scores.add(phoneAndScore);
    return phoneAndScore;
  }

  private void setTranscript(WordAndScore wordAndScore, Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    wordAndScore.setTranscript(netPronImageTypeListMap);
    wordAndScore.clearJSON();
  }

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

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }
}