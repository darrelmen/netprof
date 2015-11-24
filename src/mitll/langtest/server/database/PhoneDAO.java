/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.analysis.PhoneAnalysis;
import mitll.langtest.server.scoring.CollationSort;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
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
  private static final int INITIAL_SAMPLE_PHONES = 30;
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
   * @param userid
   * @param ids
   * @param idToRef
   * @return
   * @throws SQLException
   * @see Analysis#getPhonesForUser
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

  /**
   * @param sql
   * @param idToRef
   * @param addTranscript true if going to analysis tab
   * @return
   * @throws SQLException
   * @see #getWorstPhonesForResults(long, List, Map)
   */
  private PhoneReport getPhoneReport(String sql, Map<String, String> idToRef, boolean addTranscript) throws SQLException {
    // logger.debug("getPhoneReport query is " + sql);
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();

    // long currentRID = -1;
    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<String, List<PhoneAndScore>>();

    String currentExercise = "";
    Map<String, List<WordAndScore>> phoneToWordAndScore = new HashMap<String, List<WordAndScore>>();
    Map<String, Set<Long>> phoneToRID = new HashMap<String, Set<Long>>();
    Map<String, List<PhoneAndScore>> phoneToTimeStamp = new HashMap<String, List<PhoneAndScore>>();

    float totalScore = 0;
    float totalItems = 0;

    Map<String, Map<NetPronImageType, List<TranscriptSegment>>> stringToMap = new HashMap<>();
    while (rs.next()) {
      int i = 1;
      String exid = rs.getString(i++);
      String audioAnswer = rs.getString(i++);
      String scoreJson = rs.getString(i++);
      float pronScore = rs.getFloat(i++);
      long resultTime = -1;

      if (addTranscript) {
        Timestamp timestamp = rs.getTimestamp(i++);
        if (timestamp != null) resultTime = timestamp.getTime();
      } else {
        i++;
      }
      int wseq = rs.getInt(i++);
      String word = rs.getString(i++);
      float wscore = rs.getFloat(i++);

      long rid = rs.getLong("RID");
//      logger.info("Got " + exid + " rid " + rid + " word " + word);

      if (!exid.equals(currentExercise)) {
        //   currentRID = rid;
        currentExercise = exid;
        //logger.debug("adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }

//      if (currentRID == rid) {   // TODO : ? WHY ?
      String phone = rs.getString(PHONE);
      int seq = rs.getInt(SEQ);

      List<PhoneAndScore> scores = phoneToScores.get(phone);
      if (scores == null) phoneToScores.put(phone, scores = new ArrayList<PhoneAndScore>());
      float phoneScore = rs.getFloat(SCORE);
      scores.add(new PhoneAndScore(phoneScore, resultTime));

      List<WordAndScore> wordAndScores = phoneToWordAndScore.get(phone);
      Set<Long> ridsForPhone = phoneToRID.get(phone);
      if (wordAndScores == null) {
        phoneToWordAndScore.put(phone, wordAndScores = new ArrayList<WordAndScore>());
        phoneToRID.put(phone, ridsForPhone = new HashSet<Long>());
      }

      WordAndScore wordAndScore = new WordAndScore(exid, word, phoneScore, rid, wseq, seq, trimPathForWebPage(audioAnswer),
          idToRef.get(exid), scoreJson);
      if (!ridsForPhone.contains(rid)) { // get rid of duplicates
        wordAndScores.add(wordAndScore);
      }
      ridsForPhone.add(rid);

      if (addTranscript) {
        Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = stringToMap.get(scoreJson);
        if (netPronImageTypeListMap == null) {
          netPronImageTypeListMap = parseResultJson.parseJson(scoreJson);
          stringToMap.put(scoreJson, netPronImageTypeListMap);
        } else {
          // logger.debug("cache hit " + scoreJson.length());
        }

        setTranscript(wordAndScore, netPronImageTypeListMap);
        addResultTime(phoneToTimeStamp, resultTime, phone, phoneScore);
      }
      //    } else {
     /*   logger.debug("------> current " + currentRID +
            " skipping " + exid + " " + rid + " word " + word + "<-------------- ");*/
      //  }
    }
    finish(connection, statement, rs);

    // TODO : add this info to phone report
    for (Map.Entry<String, List<PhoneAndScore>> pair : phoneToTimeStamp.entrySet()) {
      Collections.sort(pair.getValue());
    }
    // TODO : use phone time&score info

    return getPhoneReport(phoneToScores, phoneToWordAndScore, totalScore, totalItems);
  }

  private void setTranscript(WordAndScore wordAndScore, Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    wordAndScore.setTranscript(netPronImageTypeListMap);
    wordAndScore.clearJSON();
  }

  /**
   * @param phoneToTimeStamp
   * @param resultTime
   * @param phone
   * @param phoneScore
   * @see #getPhoneReport(String, Map, boolean)
   */
  private void addResultTime(Map<String, List<PhoneAndScore>> phoneToTimeStamp, long resultTime, String phone, float phoneScore) {
    List<PhoneAndScore> times = phoneToTimeStamp.get(phone);
    if (times == null) phoneToTimeStamp.put(phone, times = new ArrayList<PhoneAndScore>());
    times.add(new PhoneAndScore(phoneScore, resultTime));
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
  private PhoneReport getPhoneReport(Map<String, List<PhoneAndScore>> phoneToScores,
                                     Map<String, List<WordAndScore>> phoneToWordAndScore,
                                     float totalScore, float totalItems) {
    float overallScore = totalItems > 0 ? totalScore / totalItems : 0;
    int percentOverall = (int) (100f * round(overallScore, 2));
    if (DEBUG) {
      logger.debug("score " + overallScore + " items " + totalItems + " percent " + percentOverall +
          " phoneToScores " + phoneToScores.size() + " : " + phoneToScores);
    }

    final Map<String, PhoneStats> phoneToAvg = new HashMap<String, PhoneStats>();

    for (Map.Entry<String, List<PhoneAndScore>> pair : phoneToScores.entrySet()) {
      String phone = pair.getKey();
      List<PhoneAndScore> value = pair.getValue();
      Collections.sort(value, new Comparator<PhoneAndScore>() {
        @Override
        public int compare(PhoneAndScore o1, PhoneAndScore o2) {
          return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
        }
      });

      List<TimeAndScore> phoneTimeSeries = getPhoneTimeSeries(value);

      int size = phoneTimeSeries.size();
      List<TimeAndScore> initialSample = phoneTimeSeries.subList(0, Math.min(INITIAL_SAMPLE_PHONES, size));
      float total = 0;

      for (TimeAndScore bs : initialSample) {
        total += bs.getScore();
      }

      int start = toPercent(total, initialSample.size());
      //  logger.info("start " + total + " " + start);
      total = 0;
      for (TimeAndScore bs : phoneTimeSeries) {
        total += bs.getScore();
      }
      int current = toPercent(total, size);
      phoneToAvg.put(phone, new PhoneStats(size, start, current, phoneTimeSeries));
    }

    if (DEBUG) logger.debug("phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    List<String> sorted = new ArrayList<String>(phoneToAvg.keySet());

    if (DEBUG) logger.debug("before sorted " + sorted);

    setSessions(phoneToAvg);

    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        PhoneStats first = phoneToAvg.get(o1);
        PhoneStats second = phoneToAvg.get(o2);
        return Integer.valueOf(first.getCurrent()).compareTo(second.getCurrent());
      }
    });

    if (DEBUG) logger.debug("sorted " + sorted.size() + " " + sorted);

    Map<String, PhoneStats> phoneToAvgSorted = new LinkedHashMap<String, PhoneStats>();
    for (String phone : sorted) {
      phoneToAvgSorted.put(phone, phoneToAvg.get(phone));
    }

    if (DEBUG) logger.debug("phoneToAvgSorted " + phoneToAvgSorted.size() + " " + phoneToAvgSorted);

    for (List<WordAndScore> words : phoneToWordAndScore.values()) {
      Collections.sort(words);
    }

    Map<String, List<WordAndScore>> phoneToWordAndScoreSorted = new LinkedHashMap<String, List<WordAndScore>>();

//    Map<String, Integer> phoneToCount = new HashMap<>();
    for (String phone : sorted) {
      List<WordAndScore> value = phoneToWordAndScore.get(phone);
      phoneToWordAndScoreSorted.put(phone, value);
      //phoneToCount.put(phone, value.size());
    }

    if (DEBUG) logger.debug("phone->words " + phoneToWordAndScore);

    return new PhoneReport(percentOverall, phoneToWordAndScoreSorted, phoneToAvgSorted/*, phoneToCount*/);
  }

  public void setSessions(Map<String, PhoneStats> phoneToAvgSorted) {
    PhoneAnalysis phoneAnalysis = new PhoneAnalysis();
    for (Map.Entry<String, PhoneStats> pair : phoneToAvgSorted.entrySet()) {
      List<PhoneSession> partition = phoneAnalysis.partition(pair.getKey(), pair.getValue().getTimeSeries());
      pair.getValue().setSessions(partition);
    }
  }

  /**
   * @param rawBestScores
   * @return
   * @see #getPhoneReport(Map, Map, float, float)
   */
  private List<TimeAndScore> getPhoneTimeSeries(List<PhoneAndScore> rawBestScores) {
    float total = 0;
    float count = 0;
    List<TimeAndScore> phoneTimeSeries = new ArrayList<>();
    for (PhoneAndScore bs : rawBestScores) {
      float pronScore = bs.getPronScore();
      total += pronScore;
      count++;
      float moving = total / count;

      TimeAndScore timeAndScore = new TimeAndScore("", bs.getTimestamp(), pronScore, moving);
      phoneTimeSeries.add(timeAndScore);
    }
    return phoneTimeSeries;
  }

  private static int toPercent(float total, float size) {
    return (int) Math.ceil(100 * total / size);
  }

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }
}