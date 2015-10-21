package mitll.langtest.server.database.analysis;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ResultDAO;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.BestScore;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 10/21/15.
 */
public class Analysis extends DAO {
  private static final Logger logger = Logger.getLogger(Analysis.class);

  public static final int FIVE_MINUTES = 5 * 60 * 1000;

  ParseResultJson parseResultJson;

  public Analysis(Database database) {
    super(database);
    parseResultJson = new ParseResultJson(database.getServerProps());

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
    List<BestScore> bestForQuery = getBestForQuery(connection, statement);
    return bestForQuery;
  }

  private String getPerfSQL(long id) {
    return "SELECT " + Database.EXID + "," + ResultDAO.PRON_SCORE + "," + Database.TIME + "," + ResultDAO.ID + "," + ResultDAO.DEVICE_TYPE + "," + ResultDAO.SCORE_JSON +
        " FROM " + ResultDAO.RESULTS +
        " where " + ResultDAO.USERID + "=" + id +
        " AND " + ResultDAO.PRON_SCORE + ">0" + // discard when they got it wrong in avp
        " order by " + Database.EXID + ", " + Database.TIME;
  }

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
      boolean isiPad = device.startsWith("i");

      long time = timestamp.getTime();
      if ((last != null && !last.equals(exid)) || (lastTimestamp > 0 && time - lastTimestamp > FIVE_MINUTES)) {
        results.add(lastBest);

        lastBest.setCount(count);
        lastTimestamp = time;
        count = 0;
      }
      if (lastTimestamp == 0) lastTimestamp = time;
      last = exid;
      lastBest = new BestScore(exid, pronScore, time, id, json, isiPad);
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

  public List<WordScore> getWordScore(List<BestScore> bestScores) {
    List<WordScore> results = new ArrayList<WordScore>();

    for (BestScore bs : bestScores) {
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJson(bs.getJson());
      results.add(new WordScore(bs, netPronImageTypeListMap));
    }
    return results;
  }
}
