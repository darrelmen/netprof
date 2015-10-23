package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhoneDAO extends DAO {
  private static final int MAX_EXAMPLES = 30;
  private static final Logger logger = Logger.getLogger(PhoneDAO.class);

  private static final String PHONE = "phone";
  private static final String RID = "rid";
  private static final String WID = "wid";
  private static final String SEQ = "seq";
  private static final String SCORE = "score";

  private static final boolean DEBUG = false;
  private ParseResultJson parseResultJson;

  /**
   * @param database
   * @see DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public PhoneDAO(Database database) {
    super(database);
    try {
      createTable(database);
      createIndex(database, RID, PHONE);
      createIndex(database, WID, PHONE);
      Connection connection = database.getConnection(this.getClass().toString());
      database.closeConnection(connection);
      parseResultJson = new ParseResultJson(database.getServerProps());
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  public static class Phone {
    long id;
    final long wid;
    final long rid;
    final String phone;
    final int seq;
    final float score;

/*    public Phone(long id, long rid, long wid, String phone, int seq, float score) {
      this(rid, wid, phone, seq, score);
      this.id = id;
    }*/

    public Phone(long rid, long wid, String phone, int seq, float score) {
      this.rid = rid;
      this.wid = wid;
      this.phone = phone;
      this.seq = seq;
      this.score = score;
    }

    public String toString() {
      return "# " + id + " rid " + rid + " wid " + wid + " : " + phone + " at " + seq + " score " + score;
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
   */
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

      statement.setLong(i++, phone.rid);
      statement.setLong(i++, phone.wid);
      statement.setString(i++, phone.phone);
      statement.setInt(i++, phone.seq);
      statement.setFloat(i++, phone.score);

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
    PhoneReport worstPhonesAndScore = getPhoneReport(userid, exids, idToRef);
    return getWorstPhonesJson(worstPhonesAndScore);
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
      worstPhonesAndScore = getWorstPhones(userid, exids, idToRef);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return worstPhonesAndScore;
  }

  private JSONObject getWorstPhonesJson(PhoneReport worstPhonesAndScore) {
    JSONObject jsonObject = new JSONObject();

    if (worstPhonesAndScore != null) {
      Map<String, List<WordAndScore>> worstPhones = worstPhonesAndScore.getPhoneToWordAndScoreSorted();
      Map<Long, String> resToAnswer = new HashMap<Long, String>();
      Map<Long, String> resToRef = new HashMap<Long, String>();
      Map<Long, String> resToResult = new HashMap<Long, String>();
      if (DEBUG) logger.debug("worstPhones phones are " + worstPhones.keySet());

      JSONObject phones = new JSONObject();

      for (Map.Entry<String, List<WordAndScore>> pair : worstPhones.entrySet()) {
        List<WordAndScore> value = pair.getValue();
        JSONArray words = getWordsJsonArray(resToAnswer, resToRef, resToResult, value);
        phones.put(pair.getKey(), words);
      }

      jsonObject.put("phones", phones);

      JSONArray order = new JSONArray();
      for (String phone : worstPhones.keySet()) order.add(phone);
      jsonObject.put("order", order);
      if (DEBUG) logger.debug("order phones are " + order);

      JSONObject results = new JSONObject();
      for (Map.Entry<Long, String> pair : resToAnswer.entrySet()) {
        JSONObject result = new JSONObject();

        Long key = pair.getKey();
        result.put("answer", pair.getValue());
        result.put("ref", resToRef.get(key));
        result.put("result", resToResult.get(key));

        results.put(Long.toString(key), result);
      }
      jsonObject.put("results", results);
      jsonObject.put("phoneScore", Integer.toString(worstPhonesAndScore.getOverallPercent()));
    }

    return jsonObject;
  }

  /**
   * @param resToAnswer
   * @param resToRef
   * @param resToResult
   * @param value
   * @return
   * @see #getWorstPhonesJson(PhoneReport)
   */
  private JSONArray getWordsJsonArray(Map<Long, String> resToAnswer,
                                      Map<Long, String> resToRef,
                                      Map<Long, String> resToResult,
                                      List<WordAndScore> value) {
    JSONArray words = new JSONArray();

    int count = 0;
    for (WordAndScore wordAndScore : value) {
      JSONObject word = getJsonForWord(wordAndScore);
      words.add(word);
      long resultID = wordAndScore.getResultID();
      resToAnswer.put(resultID, wordAndScore.getAnswerAudio());
      resToRef.put(resultID, wordAndScore.getRefAudio());
      resToResult.put(resultID, wordAndScore.getScoreJson());

      if (count++ > MAX_EXAMPLES) {
        break;
      }
    }
    return words;
  }

  private JSONObject getJsonForWord(WordAndScore wordAndScore) {
    JSONObject word = new JSONObject();
    word.put("wid", Integer.toString(wordAndScore.getWseq()));
    word.put("seq", Integer.toString(wordAndScore.getSeq()));
    word.put("w", wordAndScore.getWord());
    word.put("s", Float.toString(round(wordAndScore.getScore())));
    word.put("result", Long.toString(wordAndScore.getResultID()));
    return word;
  }

  private static float round(float d) {
    return round(d, 3);
  }

  private static float round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }

  /**
   * @see Analysis#getPhonesForUser
   * @param userid
   * @param ids
   * @param idToRef
   * @return
   * @throws SQLException
   */
  public PhoneReport getWorstPhonesForResults(long userid, List<Integer> ids, Map<String, String> idToRef) throws SQLException {
    String sql = getResultIDJoinSQL(userid, ids);
    return getPhoneReport(sql, idToRef, true);
  }

  /**
   * @param userid
   * @param exids
   * @return
   * @throws SQLException
   * @see #getPhoneReport(long, List, Map)
   */
  private PhoneReport getWorstPhones(long userid, List<String> exids, Map<String, String> idToRef) throws SQLException {
    String sql = getJoinSQL(userid, exids);
    return getPhoneReport(sql, idToRef, false);
  }

  private PhoneReport getPhoneReport(String sql, Map<String, String> idToRef, boolean addTranscript) throws SQLException {
   // logger.debug("getPhoneReport query is " + sql);
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();

    long currentRID = -1;
    Map<String, List<Float>> phoneToScores = new HashMap<String, List<Float>>();

    String currentExercise = "";
    Map<String, List<WordAndScore>> phoneToWordAndScore = new HashMap<String, List<WordAndScore>>();
    Map<String, Set<Long>> phoneToRID = new HashMap<String, Set<Long>>();

    float totalScore = 0;
    float totalItems = 0;

    while (rs.next()) {
      int i = 1;
      String exid = rs.getString(i++);
      String audioAnswer = rs.getString(i++);
      String scoreJson = rs.getString(i++);
      float pronScore = rs.getFloat(i++);
      i++;
      int wseq = rs.getInt(i++);
      String word = rs.getString(i++);
      float wscore = rs.getFloat(i++);

      long rid = rs.getLong("RID");

//      logger.info("Got " + exid + " rid " + rid + " word " + word);

      if (!exid.equals(currentExercise)) {
        currentRID = rid;
        currentExercise = exid;
        //logger.debug("adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }

      if (currentRID == rid) {
        String phone = rs.getString(PHONE);
        int seq = rs.getInt(SEQ);
        List<Float> scores = phoneToScores.get(phone);
        if (scores == null) phoneToScores.put(phone, scores = new ArrayList<Float>());
        scores.add(rs.getFloat(SCORE));

        List<WordAndScore> wordAndScores = phoneToWordAndScore.get(phone);
        Set<Long> ridsForPhone = phoneToRID.get(phone);
        if (wordAndScores == null) {
          phoneToWordAndScore.put(phone, wordAndScores = new ArrayList<WordAndScore>());
          phoneToRID.put(phone, ridsForPhone = new HashSet<Long>());
        }

        WordAndScore e = new WordAndScore(word, wscore, rid, wseq, seq, trimPathForWebPage(audioAnswer),
            idToRef.get(exid), scoreJson);
        if (!ridsForPhone.contains(rid)) { // get rid of duplicates
          wordAndScores.add(e);
        }
        ridsForPhone.add(rid);

        if (addTranscript) {
          Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJson(scoreJson);
          e.setTranscript(netPronImageTypeListMap);
          e.clearJSON();
        }
      } else {
        logger.debug("skipping " + exid + " " + rid + " word " + word);
      }
    }
    finish(connection, statement, rs);

    return getPhoneReport(phoneToScores, phoneToWordAndScore, totalScore, totalItems);
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
        "word.score wordscore, " +
        "phone.* " +

        " from " +
        "results, phone, word " +

        "where results.id = phone.rid " +
        "AND " + ResultDAO.RESULTS + "." + ResultDAO.USERID + "=" + userid +
        " AND " + filterClause +
        " AND phone.wid = word.id " +
        " order by results.exid, results.time desc";
  }

  /**
   * @param phoneToScores
   * @param phoneToWordAndScore
   * @param totalScore
   * @param totalItems
   * @return
   * @see #getPhoneReport(String, Map, boolean)
   */
  private PhoneReport getPhoneReport(Map<String, List<Float>> phoneToScores,
                                     Map<String, List<WordAndScore>> phoneToWordAndScore,
                                     float totalScore, float totalItems) {
    if (DEBUG) logger.debug("total items " + totalItems);
    float overallScore = totalItems > 0 ? totalScore / totalItems : 0;
    int percentOverall = (int) (100f * round(overallScore, 2));
    if (DEBUG) logger.debug("score " + overallScore + " items " + totalItems + " percent " + percentOverall);

    if (DEBUG) logger.debug("phoneToScores " + phoneToScores.size() + " : " + phoneToScores);

    final Map<String, Float> phoneToAvg = new HashMap<String, Float>();
    for (Map.Entry<String, List<Float>> pair : phoneToScores.entrySet()) {
      String phone = pair.getKey();
      float total = 0f;
      List<Float> scores = pair.getValue();
      for (Float f : scores) total += f;

      float avg = total / ((float) scores.size());
      if (DEBUG) logger.debug("phone " + phone + " has " + total + " n " + scores.size() + " avg " + avg);
      phoneToAvg.put(phone, avg);
    }

    if (DEBUG) logger.debug("phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    List<String> sorted = new ArrayList<String>(phoneToAvg.keySet());

    if (DEBUG) logger.debug("before sorted " + sorted);

    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        Float first = phoneToAvg.get(o1);
        Float second = phoneToAvg.get(o2);
        return first.compareTo(second);
      }
    });

    if (DEBUG) logger.debug("sorted " + sorted.size() + " " + sorted);

    Map<String, Float> phoneToAvgSorted = new LinkedHashMap<String, Float>();
    for (String phone : sorted) {
      phoneToAvgSorted.put(phone, phoneToAvg.get(phone));
    }

    if (DEBUG) logger.debug("phoneToAvgSorted " + phoneToAvgSorted.size() + " " + phoneToAvgSorted);

    for (List<WordAndScore> words : phoneToWordAndScore.values()) {
      Collections.sort(words);
    }

    Map<String, List<WordAndScore>> phoneToWordAndScoreSorted = new LinkedHashMap<String, List<WordAndScore>>();

    for (String phone : sorted) {
      List<WordAndScore> value = phoneToWordAndScore.get(phone);
      phoneToWordAndScoreSorted.put(phone, value);
    }

    if (DEBUG) logger.debug("phone->words " + phoneToWordAndScore);

    return new PhoneReport(percentOverall, phoneToWordAndScoreSorted, phoneToAvgSorted);
  }

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }
}