package mitll.langtest.server.database.analysis;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.*;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.User;
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
  private static final boolean DEBUG = false;

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

  /**
   * JUST FOR TESTING
   *
   * @param id
   * @return
   */
  public List<BestScore> getResultForUser(long id) {
    try {
      String sql = getPerfSQL(id);
      Map<Long, UserInfo> best = getBest(sql);

      List<BestScore> resultsForQuery = best.values().iterator().next().getBestScores();
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<BestScore>();
  }

  public List<UserInfo> getUserInfo(UserDAO userDAO) {
    String sql = getPerfSQL();
    try {
      Map<Long, UserInfo> best = getBest(sql);
      Map<Long, User> userMap = userDAO.getUserMap();
      List<UserInfo> userInfos = new ArrayList<>();
      for (Map.Entry<Long, UserInfo> pair : best.entrySet()) {
        User user = userMap.get(pair.getKey());
        if (user == null) {
          logger.warn("huh? no user for " + pair.getKey());
        }
        else {
          pair.getValue().setUser(user);
          userInfos.add(pair.getValue());
        }
      }
      Collections.sort(userInfos, new Comparator<UserInfo>() {
        @Override
        public int compare(UserInfo o1, UserInfo o2) {
          return Long.valueOf(o1.getUser().getId()).compareTo(o2.getUser().getId());
        }
      });
      return userInfos;
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return Collections.emptyList();
  }

  /**
   * @param sql
   * @return
   * @throws SQLException
   * @see #getPerformanceForUser(long)
   * @see #getPhonesForUser(long)
   * @see #getResultForUser(long)
   * @see #getWordScoresForUser(long)
   */
  private Map<Long, UserInfo> getBest(String sql) throws SQLException {
     logger.info("got " + sql);
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    long then = System.currentTimeMillis();
    Map<Long, UserInfo> bestForQuery = getBestForQuery(connection, statement);
    long now = System.currentTimeMillis();
    logger.debug("getBest took " + (now - then) + " millis to return\t" + bestForQuery.size() + " items");

    return bestForQuery;
  }

  private String getPerfSQL() {
    return getPerfSQL(0, false);
  }

  private String getPerfSQL(long id) {
    return getPerfSQL(id, true);
  }

  private String getPerfSQL(long id, boolean addUserID) {
    //String s = ResultDAO.USERID + "=1";
    String useridClause = addUserID ? //s +" OR " +
        ResultDAO.USERID + "=" + id + " AND " : "";
    return "SELECT " +
        Database.EXID + "," +
        ResultDAO.PRON_SCORE + "," +
        Database.TIME + "," +
        ResultDAO.ID + "," +
        ResultDAO.DEVICE_TYPE + "," +
        ResultDAO.SCORE_JSON + "," +
        ResultDAO.ANSWER + "," +
        ResultDAO.USERID +
        " FROM " + ResultDAO.RESULTS +
        " where " + useridClause +
        ResultDAO.PRON_SCORE + ">0" + // discard when they got it wrong in avp
        " order by " + Database.EXID + ", " + Database.TIME;
  }

  /**
   * TODO : JUST FOR TESTING
   * @param id
   * @param binSize
   * @return
   */
/*  public UserPerformance getResultForUserByBin(long id, int binSize) {
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
  }*/

  /**
   * @param id
   * @return
   * @see ResultDAO#getPerformanceForUser(long, PhoneDAO)
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  public UserPerformance getPerformanceForUser(long id) {
    try {
      Map<Long, UserInfo> best = getBest(getPerfSQL(id));

      UserInfo next = best.values().iterator().next();
      List<BestScore> resultsForQuery = next.getBestScores();

      return new UserPerformance(id, resultsForQuery, next.getStart(), next.getDiff());
    } catch (Exception ee) {
      logException(ee);
    }
    return new UserPerformance(id);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getWordScores(long)
   */
  public List<WordScore> getWordScoresForUser(long id) {
    try {
      String sql = getPerfSQL(id);
      Map<Long, UserInfo> best = getBest(sql);
      UserInfo next = best.values().iterator().next();
      List<BestScore> resultsForQuery = next.getBestScores();
      return getWordScore(resultsForQuery);
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPhoneScores(long)
   */
  public PhoneReport getPhonesForUser(long id) {
    try {
      String sql = getPerfSQL(id);
      Map<Long, UserInfo> best = getBest(sql);
      if (best.isEmpty()) return new PhoneReport();

      UserInfo next = best.values().iterator().next();
      List<BestScore> resultsForQuery = next.getBestScores();

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
        for (WordAndScore ws : value) {
          if (!unique.contains(ws.getWord())) {
            unique.add(ws.getWord());
            subset.add(ws);
            if (unique.size() == MAX_EXAMPLES) break;
          }
        }
        phonesForUser.put(phone, subset);
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
  private Map<Long, UserInfo> getBestForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    Map<Long, List<BestScore>> userToBest = getUserToResults(connection, statement);

    logger.info("got " + userToBest.values().iterator().next().size());

    Map<Long, List<BestScore>> userToBest2 = new HashMap<>();

    for (Long key : userToBest.keySet()) userToBest2.put(key, new ArrayList<>());

    for (Map.Entry<Long, List<BestScore>> pair : userToBest.entrySet()) {
      List<BestScore> bestScores = userToBest2.get(pair.getKey());

      String last = null;

      long lastTimestamp = 0;
     // int count = 0;
      BestScore lastBest = null;
      Set<Integer> seen = new HashSet<>();

      for (BestScore bs : pair.getValue()) {
        int id = bs.getResultID();
        String exid = bs.getId();
        long time = bs.getTimestamp();

        if ((last != null && !last.equals(exid)) || (lastTimestamp > 0 && time - lastTimestamp > FIVE_MINUTES)) {
          if (seen.contains(id)) {
            logger.warn("skipping " + id);
          } else {
            bestScores.add(lastBest);
           // logger.info("Adding " + lastBest);
            seen.add(id);
          }
  //        lastBest.setCount(count);
          lastTimestamp = time;
       //   count = 0;
        }
        if (lastTimestamp == 0) lastTimestamp = time;
        last = exid;
        lastBest = bs;
//        lastBest = new BestScore(exid, pronScore, time, id, json, isiPad, path);
       // count++;
      }

      if (lastBest != null) {
        if (seen.contains(lastBest.getResultID())) {
          logger.warn("skipping " + lastBest.getResultID());
        } else {
          bestScores.add(lastBest);
        }
      }

    }


    /*if (!lastResults.isEmpty()) {
      if (lastBest != null && lastBest != lastResults.get(lastResults.size() - 1)) {
        if (seen.contains(lastBest.getResultID())) {
          logger.warn("skipping " + lastBest.getResultID());
        } else {
          lastResults.add(lastBest);
        }
      }
    }*/

    Map<Long, UserInfo> userToUserInfo = new HashMap<>();
    for (Map.Entry<Long, List<BestScore>> pair : userToBest2.entrySet()) {
      userToUserInfo.put(pair.getKey(), new UserInfo(pair.getValue()));
    }

    return userToUserInfo;
  }

  private Map<Long, List<BestScore>> getUserToResults(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
//    List<BestScore> lastResults = new ArrayList<BestScore>();
//    String last = null;
//
//    long lastTimestamp = 0;
//    int count = 0;
//    BestScore lastBest = null;
//    Set<Integer> seen = new HashSet<>();
    Map<Long, List<BestScore>> userToBest = new HashMap<>();

    while (rs.next()) {
      String exid = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      float pronScore = rs.getFloat(ResultDAO.PRON_SCORE);
      int id = rs.getInt(ResultDAO.ID);
      long userid = rs.getLong(ResultDAO.USERID);

      List<BestScore> results = userToBest.get(userid);
      if (results == null) userToBest.put(userid, results = new ArrayList<BestScore>());
    //  lastResults = results;

      if (pronScore < 0.01) logger.warn("huh? got " + pronScore + " for " + exid + " and " + id);

      String json = rs.getString(ResultDAO.SCORE_JSON);
      String device = rs.getString(ResultDAO.DEVICE_TYPE);
      String path = rs.getString(ResultDAO.ANSWER);
      boolean isiPad = device != null && device.startsWith("i");
      long time = timestamp.getTime();

      results.add(new BestScore(exid, pronScore, time, id, json, isiPad, trimPathForWebPage(path)));

     /*
      if ((last != null && !last.equals(exid)) || (lastTimestamp > 0 && time - lastTimestamp > FIVE_MINUTES)) {
        if (seen.contains(id)) {
          logger.warn("skipping " + id);
        } else {
          results.add(lastBest);
          seen.add(id);
        }
        lastBest.setCount(count);
        lastTimestamp = time;
        count = 0;
      }
      if (lastTimestamp == 0) lastTimestamp = time;
      last = exid;
      lastBest = new BestScore(exid, pronScore, time, id, json, isiPad, path);
      count++;*/
    }
    finish(connection, statement, rs);
    return userToBest;
  }

/*
  public static class BestInfo {
    int start;
    int current;
    int diff;
    int num;
    List<BestScore> bestScores;

    public BestInfo(List<BestScore> bestScores) {
      this.bestScores = bestScores;
      this.num = bestScores.size();
      Collections.sort(bestScores, new Comparator<BestScore>() {
        @Override
        public int compare(BestScore o1, BestScore o2) {
          return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
        }
      });
      List<BestScore> bestScores1 = bestScores.subList(0, Math.min(10, bestScores.size()));
      float total = 0;
      for (BestScore bs : bestScores1) total += bs.getScore();
      int size = bestScores1.size();
      start = toPercent(total, size);


      total = 0;
      float count = 0;
      for (BestScore bs : bestScores) {
        total += bs.getScore();
        count++;
      }
      current = toPercent(total, count);
      diff = current - start;
    }

    public String toString() {
      return "" + start + " " + current + " " + diff + " " + num;
    }
  }

  private static int toPercent(float total, float size) {
    return (int) Math.ceil(100 * total / size);
  }
*/

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }

  /**
   * TODO : why do we parse json when we could just get it out of word and phone tables????
   *
   * @param bestScores
   * @return
   * @see #getWordScoresForUser(long)
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
