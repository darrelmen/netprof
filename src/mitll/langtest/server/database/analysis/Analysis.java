package mitll.langtest.server.database.analysis;

import mitll.langtest.server.database.*;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created by go22670 on 10/21/15.
 */
public class Analysis extends DAO {
  private static final Logger logger = Logger.getLogger(Analysis.class);

  private static final int MAX_EXAMPLES = 50;
  private static final boolean DEBUG = true;

  private static final int FIVE_MINUTES = 5 * 60 * 1000;

  private ParseResultJson parseResultJson;
  private PhoneDAO phoneDAO;

  /**
   * @param database
   * @param phoneDAO
   * @see DatabaseImpl#getAnalysis()
   */
  public Analysis(Database database, PhoneDAO phoneDAO) {
    super(database);
    parseResultJson = new ParseResultJson(database.getServerProps());
    this.phoneDAO = phoneDAO;
  }

  public List<BestScore> getResultForUser(long id) {
    try {
      String sql = getPerfSQL(id);
      List<BestScore> resultsForQuery = getBest(sql);
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<BestScore>();
  }

  /**
   * @param sql
   * @return
   * @throws SQLException
   */
  private List<BestScore> getBest(String sql) throws SQLException {
    logger.info("got " + sql);
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    long then = System.currentTimeMillis();
    List<BestScore> bestForQuery = getBestForQuery(connection, statement);
    long now = System.currentTimeMillis();
    logger.debug("getBest took " + (now - then) + " millis to return " + bestForQuery.size() + " items");

    return bestForQuery;
  }

  private String getPerfSQL(long id) {
    return "SELECT " +
        Database.EXID + "," +
        ResultDAO.PRON_SCORE + "," +
        Database.TIME + "," +
        ResultDAO.ID + "," +
        ResultDAO.DEVICE_TYPE + "," +
        ResultDAO.SCORE_JSON + ","+
        ResultDAO.ANSWER +
        " FROM " + ResultDAO.RESULTS +
        " where " + ResultDAO.USERID + "=" + id +
        " AND " + ResultDAO.PRON_SCORE + ">0" + // discard when they got it wrong in avp
        " order by " + Database.EXID + ", " + Database.TIME;
  }

  /**
   * TODO : JUST FOR TESTING
   * @param id
   * @param binSize
   * @return
   */
  public UserPerformance getResultForUserByBin(long id, int binSize) {
    try {
      String sql = getPerfSQL(id);

      List<BestScore> resultsForQuery = getBest(sql);
      UserPerformance up = new UserPerformance(id);
      up.setAtBinSize(resultsForQuery, binSize);
      return up;
    } catch (Exception ee) {
      logException(ee);
    }
    return new UserPerformance(id);
  }

  /**
   * @see ResultDAO#getPerformanceForUser(long, PhoneDAO)
   * @param id
   * @return
   */
  public UserPerformance getPerformanceForUser(long id) {
    try {
      String sql = getPerfSQL(id);

      List<BestScore> resultsForQuery = getBest(sql);
      UserPerformance up = new UserPerformance(id, resultsForQuery);
      return up;
    } catch (Exception ee) {
      logException(ee);
    }
    return new UserPerformance(id);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getWordScores(long)
   * @param id
   * @return
   */
  public List<WordScore> getWordScoresForUser(long id) {
    try {
      String sql = getPerfSQL(id);
      List<BestScore> resultsForQuery = getBest(sql);
      return getWordScore(resultsForQuery);
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPhoneScores(long)
   * @param id
   * @return
   */
  public PhoneReport getPhonesForUser(long id) {
    try {
      String sql = getPerfSQL(id);
      List<BestScore> resultsForQuery = getBest(sql);
      List<Integer> ids = new ArrayList<>();
      for (BestScore bs : resultsForQuery) {
        ids.add(bs.getResultID());
      }

      if (DEBUG) logger.info("getPhonesForUser from " + resultsForQuery.size() + " added " + ids.size() + " ids ");
      PhoneReport phoneReport = phoneDAO.getWorstPhonesForResults(id, ids, Collections.emptyMap());

      Map<String, List<WordAndScore>> phonesForUser = phoneReport.getPhoneToWordAndScoreSorted();

      if (DEBUG) logger.info("getPhonesForUser report phonesForUser " + phonesForUser);
      long now = System.currentTimeMillis();

      for (Map.Entry<String, List<WordAndScore>> pair : phonesForUser.entrySet()) {
        List<WordAndScore> value = pair.getValue();
        String phone = pair.getKey();
        if (DEBUG) logger.info(phone + " = " + value.size() + " first " + value.get(0));
        List<WordAndScore> subset = new ArrayList<>();

        Set<String> unique = new HashSet<>();
        for (WordAndScore ws :value) {
          if (!unique.contains(ws.getWord())) {
            unique.add(ws.getWord());
            subset.add(ws);
            if (unique.size() == MAX_EXAMPLES) break;
          }
        }
        phonesForUser.put(phone,subset);
      }
      if (DEBUG) logger.info("getPhonesForUser report phoneReport " + phoneReport);

      return phoneReport;
    } catch (Exception ee) {
      logException(ee);
    }
    return null;
  }

  /**
   * @param connection
   * @param statement
   * @return
   * @throws SQLException
   * @see #getBest(String)
   */
  private List<BestScore> getBestForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<BestScore> results = new ArrayList<BestScore>();
    String last = null;

    long lastTimestamp = 0;
    int count = 0;
    BestScore lastBest = null;
    while (rs.next()) {
      String exid = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      float pronScore = rs.getFloat(ResultDAO.PRON_SCORE);
      int id = rs.getInt(ResultDAO.ID);

      String json = rs.getString(ResultDAO.SCORE_JSON);
      String device = rs.getString(ResultDAO.DEVICE_TYPE);
      String path = rs.getString(ResultDAO.ANSWER);
      boolean isiPad = device != null && device.startsWith("i");

      long time = timestamp.getTime();
      if ((last != null && !last.equals(exid)) || (lastTimestamp > 0 && time - lastTimestamp > FIVE_MINUTES)) {
        results.add(lastBest);

        lastBest.setCount(count);
        lastTimestamp = time;
        count = 0;
      }
      if (lastTimestamp == 0) lastTimestamp = time;
      last = exid;
      lastBest = new BestScore(exid, pronScore, time, id, json, isiPad, path);
      count++;
    }
    finish(connection, statement, rs);

    if (!results.isEmpty()) {
      if (lastBest != results.get(results.size() - 1)) {
        results.add(lastBest);
      }
    }

    return results;
  }

  /**
   * @see #getWordScoresForUser(long)
   * @param bestScores
   * @return
   */
  private List<WordScore> getWordScore(List<BestScore> bestScores) {
    List<WordScore> results = new ArrayList<WordScore>();

    for (BestScore bs : bestScores) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJson(bs.getJson());
      results.add(new WordScore(bs, netPronImageTypeListMap));
    }
    Collections.sort(results);
    return results;
  }
}
