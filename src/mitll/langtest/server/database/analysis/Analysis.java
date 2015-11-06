package mitll.langtest.server.database.analysis;

import mitll.langtest.server.LangTestDatabaseImpl;
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
/*  public List<BestScore> getResultForUser(long id) {
    try {
      String sql = getPerfSQL(id);
      Map<Long, UserInfo> best = getBest(sql, minRecordings);

      List<BestScore> resultsForQuery = best.values().iterator().next().getBestScores();
      return resultsForQuery;
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<BestScore>();
  }*/

  Set<String> lincoln = new HashSet<>(Arrays.asList("gvidaver", "rbudd", "jmelot", "esalesky", "gatewood",
      "testing", "grading", "fullperm",
      //"0001abcd",
      "egodoy",
      "rb2rb2",
      "dajone3",
      //"WagnerSandy",
      "rbtrbt"));

  /**
   * @see LangTestDatabaseImpl#getUsersWithRecordings()
   * @param userDAO
   * @param minRecordings
   * @return
   */
  public List<UserInfo> getUserInfo(UserDAO userDAO, int minRecordings) {
    String sql = getPerfSQL();
    try {
      Map<Long, UserInfo> best = getBest(sql, minRecordings);
      Map<Long, User> userMap = userDAO.getUserMap();
      List<UserInfo> userInfos = new ArrayList<>();
      for (Map.Entry<Long, UserInfo> pair : best.entrySet()) {
        User user = userMap.get(pair.getKey());

        if (user == null) {
          logger.warn("huh? no user for " + pair.getKey());
        } else {
          boolean isLL = lincoln.contains(user.getUserID());
          if (!isLL) {
            pair.getValue().setUser(user);
            userInfos.add(pair.getValue());
          }
        }
      }
      Collections.sort(userInfos, new Comparator<UserInfo>() {
        @Override
        public int compare(UserInfo o1, UserInfo o2) {
          return -1 * Long.valueOf(o1.getTimestampMillis()).compareTo(o2.getTimestampMillis());
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
   * @param minRecordings
   * @return
   * @throws SQLException
   * @see #getPerformanceForUser(long, int)
   * @see #getPhonesForUser(long, int)
   * @see #getWordScoresForUser(long, int)
   */
  private Map<Long, UserInfo> getBest(String sql, int minRecordings) throws SQLException {
    if (DEBUG) logger.info("getBest sql =\n" + sql);
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    long then = System.currentTimeMillis();
    Map<Long, UserInfo> bestForQuery = getBestForQuery(connection, statement, minRecordings);
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
   * @param id
   * @param minRecordings
   * @return
   * @see ResultDAO#getPerformanceForUser(long, PhoneDAO, int)
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  public UserPerformance getPerformanceForUser(long id, int minRecordings) {
    try {
      Map<Long, UserInfo> best = getBest(getPerfSQL(id), minRecordings);

      Collection<UserInfo> values = best.values();
      if (values.isEmpty()) {
        if (DEBUG) logger.debug("no results for " + id);
        return new UserPerformance();
      } else {
        UserInfo next = values.iterator().next();
        if (DEBUG)  logger.debug(" results for " + values.size() + "  first  " + next);
        List<BestScore> resultsForQuery = next.getBestScores();
        if (DEBUG) logger.debug(" resultsForQuery for " + resultsForQuery.size());

        return new UserPerformance(id, resultsForQuery);
      }
    } catch (Exception ee) {
      logException(ee);
    }
    return new UserPerformance(id);
  }

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getWordScores
   */
  public List<WordScore> getWordScoresForUser(long id, int minRecordings) {
    try {
      String sql = getPerfSQL(id);
      Map<Long, UserInfo> best = getBest(sql, minRecordings);

      Collection<UserInfo> values = best.values();
      if (values.isEmpty()) {
        logger.warn("no best values for " + id);
        return getWordScore(Collections.emptyList());
      } else {
        UserInfo next = values.iterator().next();
        List<BestScore> resultsForQuery = next.getBestScores();
        if  (DEBUG) logger.warn("resultsForQuery " + resultsForQuery.size());

        List<WordScore> wordScore = getWordScore(resultsForQuery);
        if  (DEBUG) logger.warn("wordScore " + wordScore.size());

        return wordScore;
      }
    } catch (Exception ee) {
      logException(ee);
    }
    return new ArrayList<>();
  }

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPhoneScores(long)
   */
  public PhoneReport getPhonesForUser(long id, int minRecordings) {
    try {
      String sql = getPerfSQL(id);
      Map<Long, UserInfo> best = getBest(sql, minRecordings);
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
   * @param minRecordings
   * @return
   * @throws SQLException
   * @see #getBest(String, int)
   */
  private Map<Long, UserInfo> getBestForQuery(Connection connection, PreparedStatement statement, int minRecordings) throws SQLException {
    Map<Long, List<BestScore>> userToBest = getUserToResults(connection, statement);

    if (DEBUG) logger.info("getBestForQuery got " + userToBest.values().iterator().next().size());

    Map<Long, List<BestScore>> userToBest2 = new HashMap<>();
    Map<Long, Long> userToEarliest = new HashMap<>();

    for (Long key : userToBest.keySet()) userToBest2.put(key, new ArrayList<>());

    for (Map.Entry<Long, List<BestScore>> pair : userToBest.entrySet()) {
      Long userID = pair.getKey();
      List<BestScore> bestScores = userToBest2.get(userID);

      String last = null;

      long lastTimestamp = 0;
      // int count = 0;
      BestScore lastBest = null;
      Set<Integer> seen = new HashSet<>();

      for (BestScore bs : pair.getValue()) {
        int id = bs.getResultID();
        String exid = bs.getId();
        long time = bs.getTimestamp();

        Long aLong = userToEarliest.get(userID);
        if (aLong == null || time < aLong) userToEarliest.put(userID, time);

        if ((last != null && !last.equals(exid)) || (lastTimestamp > 0 && time - lastTimestamp > FIVE_MINUTES)) {
          if (seen.contains(id)) {
            logger.warn("skipping " + id);
          } else {
            bestScores.add(lastBest);
            if (DEBUG) logger.info("Adding " + lastBest);
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
          logger.warn("getBestForQuery skipping " + lastBest.getResultID());
        } else {
          if (DEBUG) logger.debug("getBestForQuery bestScores now " + bestScores.size());

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
      List<BestScore> value = pair.getValue();
      Long userID = pair.getKey();
      if (value.size() >= minRecordings) {
        Long aLong = userToEarliest.get(userID);
        userToUserInfo.put(userID, new UserInfo(value, aLong));
      }
      else {
        if (DEBUG) logger.debug("skipping  " +userID + ": " + value.size());
      }
    }

    if (DEBUG) logger.info("Return " + userToUserInfo);

    return userToUserInfo;
  }

  private Map<Long, List<BestScore>> getUserToResults(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    Map<Long, List<BestScore>> userToBest = new HashMap<>();

    while (rs.next()) {
      String exid = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      float pronScore = rs.getFloat(ResultDAO.PRON_SCORE);
      int id = rs.getInt(ResultDAO.ID);
      long userid = rs.getLong(ResultDAO.USERID);

      List<BestScore> results = userToBest.get(userid);
      if (results == null) userToBest.put(userid, results = new ArrayList<BestScore>());

      if (pronScore < 0) logger.warn("huh? got " + pronScore + " for " + exid + " and " + id);

      String json = rs.getString(ResultDAO.SCORE_JSON);
      if (json != null && json.equals("{}")) {
        logger.warn("Got empty json " + json + " for " + exid  +" : " +id);
      }
      String device = rs.getString(ResultDAO.DEVICE_TYPE);
      String path = rs.getString(ResultDAO.ANSWER);
      boolean isiPad = device != null && device.startsWith("i");
      long time = timestamp.getTime();

      results.add(new BestScore(exid, pronScore, time, id, json, isiPad, trimPathForWebPage(path)));
    }
    finish(connection, statement, rs);
    return userToBest;
  }

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }

  /**
   * TODO : why do we parse json when we could just get it out of word and phone tables????
   *
   * @param bestScores
   * @return
   * @see #getWordScoresForUser(long, int)
   */
  private List<WordScore> getWordScore(List<BestScore> bestScores) {
    List<WordScore> results = new ArrayList<WordScore>();

    for (BestScore bs : bestScores) {
      String json = bs.getJson();
      if (json == null) {
        logger.error("huh? no json for " + bs);
      }
      else if (json.equals("{}")) {
        logger.warn("json is empty for " + bs);
      }
      else {
        if (json.isEmpty()) logger.warn("no json for " + bs);
        Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.parseJson(json);
        results.add(new WordScore(bs, netPronImageTypeListMap));
      }
    }
    Collections.sort(results);
    return results;
  }
}
