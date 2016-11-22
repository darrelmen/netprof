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

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.analysis.PhoneAnalysis;
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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
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
  private static final String DURATION = "duration";

  private static final boolean DEBUG = false;
  private static final String RID1 = "RID";
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

  static class Phone {
    final long wid;
    final long rid;
    final String phone;
    final int seq;
    final float score;
    float duration;

    Phone(long rid, long wid, String phone, int seq, float score, float duration) {
      this.rid = rid;
      this.wid = wid;
      this.phone = phone;
      this.seq = seq;
      this.score = score;
      this.duration = duration;
    }

    public String toString() {
      return // "# " + id +
          " rid " + rid + " wid " + wid + " : " + phone + " at " + seq + " score " + score;
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

//    if (database.getServerProps().shouldRecalcStudentAudio()) {
//      drop(PHONE, connection);
//    }

    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        PHONE +
        " (" +
        "ID IDENTITY, " +
        RID + " BIGINT, " +
        WID + " BIGINT, " +
        PHONE + " VARCHAR, " +
        SEQ + " INT, " +
        SCORE + " FLOAT, " +
        DURATION + " FLOAT, " +

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

    Collection<String> columns = getColumns(PHONE);

    if (!columns.contains(DURATION.toLowerCase())) {
      addFloat(connection, PHONE, DURATION);
    }
  }

  public boolean removePhones(long resultid) {
    Connection connection = getConnection();
    boolean val = true;
    try {
      // there are much better ways of doing this...
      PreparedStatement statement = connection.prepareStatement(
          "DELETE FROM " + PHONE +
              " WHERE " +
              RID + "=" +resultid);
      int i = 1;

//      statement.setLong(i++, resultid);
      int j = statement.executeUpdate();

      if (j == 0) {
        logger.error("huh? didn't remove rows for " + resultid + " got ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
        val = false;
      }
      statement.close();

    } catch (SQLException ee) {
      logger.error("trying to drop phones " + resultid + " got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
      val = false;
    } finally {
      database.closeConnection(connection);
    }
    return val;
  }


  /**
   * <p>
   *
   * @param phone
   */
  boolean addPhone(Phone phone) {
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
              SCORE + ","+
              DURATION +
              ") " +
              "VALUES(?,?,?,?,?,?)");
      int i = 1;

      statement.setLong(i++, phone.rid);
      statement.setLong(i++, phone.wid);
      statement.setString(i++, phone.phone);
      statement.setInt(i++, phone.seq);
      statement.setFloat(i++, phone.score);
      statement.setFloat(i++, phone.duration);

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

  /**
   * TODO: sort phones by average score of the latest unique item - if you say pijama 10 times, don't return it as
   * an example 10 times, and only have the last one contribute to the average score for the phone.
   *
   * @param worstPhonesAndScore
   * @return
   * @see #getWorstPhonesJson(long, List, Map)
   */
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
    return getPhoneReport(sql, idToRef, true, false);
  }

  /**
   * @param userid
   * @param exids
   * @paramx sortByLatestExample
   * @return
   * @throws SQLException
   * @see #getPhoneReport(long, List, Map)
   */
  private PhoneReport getWorstPhones(long userid, List<String> exids, Map<String, String> idToRef) throws SQLException {
    String sql = getJoinSQL(userid, exids);
    return getPhoneReport(sql, idToRef, false, true);
  }

  /**
   * @param sql
   * @param idToRef
   * @param addTranscript       true if going to analysis tab
   * @param sortByLatestExample
   * @return
   * @throws SQLException
   * @see #getWorstPhonesForResults(long, List, Map)
   * @see #getWorstPhones
   */
  private PhoneReport getPhoneReport(String sql, Map<String, String> idToRef, boolean addTranscript, boolean sortByLatestExample) throws SQLException {
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
      String exid = rs.getString(i++);
      String audioAnswer = rs.getString(i++);
      String scoreJson = rs.getString(i++);
      float pronScore = rs.getFloat(i++);
      long resultTime = -1;

      Timestamp timestamp = rs.getTimestamp(i++);
      if (timestamp != null) resultTime = timestamp.getTime();

      int wseq = rs.getInt(i++);
      String word = rs.getString(i++);
      float wscore = rs.getFloat(i++);

      long rid = rs.getLong(RID1);
//      logger.info("Got " + exid + " rid " + rid + " word " + word);

      if (!exid.equals(currentExercise)) {
        currentExercise = exid;
        //logger.debug("adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }

      String phone = rs.getString(PHONE);
      int seq = rs.getInt(SEQ);

      List<PhoneAndScore> scores = phoneToScores.get(phone);
      if (scores == null) phoneToScores.put(phone, scores = new ArrayList<PhoneAndScore>());
      float phoneScore = rs.getFloat(SCORE);
      PhoneAndScore phoneAndScore = new PhoneAndScore(phoneScore, resultTime);
      scores.add(phoneAndScore);

      List<WordAndScore> wordAndScores = phoneToWordAndScore.get(phone);
      if (wordAndScores == null) {
        phoneToWordAndScore.put(phone, wordAndScores = new ArrayList<WordAndScore>());
      }

      WordAndScore wordAndScore = new WordAndScore(exid, word, phoneScore, rid, wseq, seq, trimPathForWebPage(audioAnswer),
          idToRef.get(exid), scoreJson, resultTime);

      wordAndScores.add(wordAndScore);
      phoneAndScore.setWordAndScore(wordAndScore);

      if (addTranscript) {
        Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = stringToMap.get(scoreJson);
        if (netPronImageTypeListMap == null) {
          netPronImageTypeListMap = parseResultJson.parseJson(scoreJson);
          stringToMap.put(scoreJson, netPronImageTypeListMap);
        } else {
          // logger.debug("cache hit " + scoreJson.length());
        }

        setTranscript(wordAndScore, netPronImageTypeListMap);
      }
      //    } else {
     /*   logger.debug("------> current " + currentRID +
            " skipping " + exid + " " + rid + " word " + word + "<-------------- ");*/
      //  }
    }
    finish(connection, statement, rs, sql);

    return getPhoneReport(phoneToScores, phoneToWordAndScore, totalScore, totalItems, sortByLatestExample);
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
   * @param sortByLatestExample
   * @return
   * @see #getPhoneReport(String, Map, boolean, boolean)
   */
  private PhoneReport getPhoneReport(Map<String, List<PhoneAndScore>> phoneToScores,
                                     Map<String, List<WordAndScore>> phoneToWordAndScore,
                                     float totalScore, float totalItems, boolean sortByLatestExample) {
    float overallScore = totalItems > 0 ? totalScore / totalItems : 0;
    int percentOverall = (int) (100f * round(overallScore, 2));
    if (DEBUG) {
      logger.debug("score " + overallScore + " items " + totalItems + " percent " + percentOverall +
          " phoneToScores " + phoneToScores.size() + " : " + phoneToScores);
    }

    final Map<String, PhoneStats> phoneToAvg = getPhoneToPhoneStats(phoneToScores);
    //if (DEBUG || true) logger.debug("phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    List<String> sorted = new ArrayList<String>(phoneToAvg.keySet());

    if (DEBUG) logger.debug("before sorted " + sorted);

    setSessions(phoneToAvg);

    if (DEBUG) logger.debug("phoneToAvg " + phoneToAvg.size() + " " + phoneToAvg);

    if (sortByLatestExample) {
      Map<String, List<WordAndScore>> stringCollectionMap = sortPhonesByLatest(phoneToAvg, sorted);
      phoneToWordAndScore = stringCollectionMap;
    } else {
      sortPhonesByCurrentScore(phoneToAvg, sorted);
    }

    if (DEBUG) logger.debug("sorted " + sorted.size() + " " + sorted);

    Map<String, PhoneStats> phoneToAvgSorted = new LinkedHashMap<String, PhoneStats>();
    for (String phone : sorted) {
      phoneToAvgSorted.put(phone, phoneToAvg.get(phone));
    }

    if (DEBUG) logger.debug("phoneToAvgSorted " + phoneToAvgSorted.size() + " " + phoneToAvgSorted);

    Map<String, List<WordAndScore>> phoneToWordAndScoreSorted = new LinkedHashMap<String, List<WordAndScore>>();

    for (String phone : sorted) {
      List<WordAndScore> value = phoneToWordAndScore.get(phone);
      Collections.sort(value);
      phoneToWordAndScoreSorted.put(phone, value);
    }

    if (DEBUG) logger.debug("phone->words " + phoneToWordAndScore);

    return new PhoneReport(percentOverall, phoneToWordAndScoreSorted, phoneToAvgSorted);
  }

  private void sortPhonesByCurrentScore(final Map<String, PhoneStats> phoneToAvg, List<String> sorted) {
    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        PhoneStats first = phoneToAvg.get(o1);
        PhoneStats second = phoneToAvg.get(o2);
        int current = first.getCurrent();
        int current1 = second.getCurrent();
        //if (current == current1) {
        //  logger.info("got same " + current + " for " + o1 + " and " + o2);
        //} else {
        // logger.info("\tgot " + current + " for " + o1 + " and " + current1 + " for "+ o2);
        //}
        int i = Integer.valueOf(current).compareTo(current1);
        return i == 0 ? o1.compareTo(o2) : i;
      }
    });
/*
    for (String phone : sorted) {
      logger.info("phone " + phone + " : " + phoneToAvg.get(phone).getCurrent());
    }
*/
  }

  /**
   * For the iPad, we don't want to return every single example, just the latest one, and the summary score
   * for the phone is just for the latest ones, or else they can seem inconsistent, since the iPad only shows
   * distinct words.
   *
   * @param phoneToAvg
   * @param sorted
   * @return
   */
  private Map<String, List<WordAndScore>> sortPhonesByLatest(final Map<String, PhoneStats> phoneToAvg, List<String> sorted) {
    Map<String, List<WordAndScore>> phoneToMinimal = new HashMap<>();
    for (Map.Entry<String, PhoneStats> pair : phoneToAvg.entrySet()) {
      PhoneStats value = pair.getValue();
      Map<String, WordAndScore> wordToExample = new HashMap<>();
      for (TimeAndScore item : value.getTimeSeries()) {
//        logger.info("got " + item + " at " + new Date(item.getTimestamp()));
        wordToExample.put(item.getWordAndScore().getWord(), item.getWordAndScore());
      }
      phoneToMinimal.put(pair.getKey(), new ArrayList<>(wordToExample.values()));
    }

    final Map<String, Float> phoneToScore = new HashMap<>();
    for (Map.Entry<String, List<WordAndScore>> pair : phoneToMinimal.entrySet()) {
      float total = 0;
      for (WordAndScore example : pair.getValue()) total += example.getScore();
      total /= pair.getValue().size();
      phoneToScore.put(pair.getKey(), total);
    }

    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        Float current = phoneToScore.get(o1);
        Float current1 = phoneToScore.get(o2);
        int i = current.compareTo(current1);
        return i == 0 ? o1.compareTo(o2) : i;
      }
    });

    if (DEBUG) {
      for (String phone : sorted) {
        logger.info("phone " + phone + " : " + phoneToScore.get(phone) + " " + phoneToMinimal.get(phone));
      }
    }
    return phoneToMinimal;
  }

  private Map<String, PhoneStats> getPhoneToPhoneStats(Map<String, List<PhoneAndScore>> phoneToScores) {
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
      phoneToAvg.put(phone, new PhoneStats(phoneTimeSeries.size(), phoneTimeSeries));
    }
    return phoneToAvg;
  }

  /**
   * @param phoneToAvgSorted
   * @see #getPhoneReport(Map, Map, float, float, boolean)
   */
  private void setSessions(Map<String, PhoneStats> phoneToAvgSorted) {
    new PhoneAnalysis().setSessions(phoneToAvgSorted);
  }

  /**
   * @param rawBestScores
   * @return
   * @see #getPhoneReport(Map, Map, float, float, boolean)
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

      WordAndScore wordAndScore = bs.getWordAndScore();
      TimeAndScore timeAndScore = new TimeAndScore("", bs.getTimestamp(), pronScore, moving, wordAndScore);
      phoneTimeSeries.add(timeAndScore);
    }
    return phoneTimeSeries;
  }

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }
}