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

package mitll.langtest.server.database.analysis;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.analysis.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * @
 */
class H2Analysis extends Analysis implements IAnalysis {
  private static final Logger logger = LogManager.getLogger(H2Analysis.class);

  private static final boolean DEBUG = false;

  /**
   * @param database
   * @param phoneDAO
   * @param exToRef
   * @seex DatabaseImpl#getAnalysis()
   * @seex DatabaseImpl#makeDAO(String, String, String)
   */
  public H2Analysis(Database database, IPhoneDAO phoneDAO, Map<Integer, String> exToRef) {
    super(database, phoneDAO);
  }

  /**
   * @param id
   * @param minRecordings
   * @return
   * @param listid
   * @seex mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceForUser
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  public UserPerformance getPerformanceForUser(int id, int minRecordings, int listid) {
    try {
      Map<Integer, UserInfo> best = getBest(getPerfSQL(id), minRecordings);

      return getUserPerformance(id, best);
    } catch (Exception ee) {
      logException(ee);
    }
    return new UserPerformance(id);
  }

  @Override
  public AnalysisReport getPerformanceReportForUser(int id, int minRecordings, int listid) {
    return null;
  }

  /**
   * @param userDAO
   * @param minRecordings
   * @return
   * @seex LangTestDatabaseImpl#getUsersWithRecordings
   */
  public List<UserInfo> getUserInfo(IUserDAO userDAO, int minRecordings) {
    String sql = getPerfSQL();
    try {
      Map<Integer, UserInfo> best = getBest(sql, minRecordings);
      return getUserInfos(userDAO, best);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return Collections.emptyList();
  }

  private String getPerfSQL() {
    return getPerfSQL(
        0,  // IGNORED VALUE!
        false);
  }

  private String getPerfSQL(long userid) {
    return getPerfSQL(userid, true);
  }

  private String getPerfSQL(long userid, boolean addUserID) {
    String useridClause = addUserID ? ResultDAO.USERID + "=" + userid + " AND " : "";
    return "SELECT " +
        Database.EXID + "," +
        ResultDAO.PRON_SCORE + "," +
        Database.TIME + "," +
        ResultDAO.ID + "," +
        ResultDAO.DEVICE_TYPE + "," +
        ResultDAO.SCORE_JSON + "," +
        ResultDAO.ANSWER + "," +
        ResultDAO.AUDIO_TYPE + "," +
        ResultDAO.USERID +
        " FROM " + ResultDAO.RESULTS +
        " where " + useridClause +
        ResultDAO.PRON_SCORE + ">0" + // discard when they got it wrong in avp
        " order by " + Database.EXID + ", " + Database.TIME;
  }

//  protected Map<Integer, UserInfo> getBest(int minRecordings) {
//    Map<Integer, UserInfo> best = getBest(getPerfSQL(id), minRecordings);
//  }

  /**
   * @param connection
   * @param statement
   * @param minRecordings
   * @return
   * @throws SQLException
   * @see #getBest(String, int)
   */
  private Map<Integer, UserInfo> getBestForQuery(Connection connection, PreparedStatement statement, int minRecordings)
      throws SQLException {
    Map<Integer, List<BestScore>> userToBest = getUserToResults(connection, statement);

    return getBestForQuery(minRecordings, userToBest);
  }

  /**
   * @param sql
   * @param minRecordings
   * @return
   * @throws SQLException
   * @see Analysis#getPerformanceForUser(int, int, int)
   * @see IAnalysis#getPhonesForUser
   * @seex Analysis#getWordScoresForUser
   */
  private Map<Integer, UserInfo> getBest(String sql, int minRecordings) throws SQLException {
    if (DEBUG) logger.info("getBest sql =\n" + sql);
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    long then = System.currentTimeMillis();
    Map<Integer, UserInfo> bestForQuery = getBestForQuery(connection, statement, minRecordings);
    long now = System.currentTimeMillis();
    logger.debug(getLanguage() + " : getBest took " + (now - then) + " millis to return\t" + bestForQuery.size() + " items");

    return bestForQuery;
  }

  /**
   * @param id
   * @param minRecordings
   * @param listid
   * @paramx projid
   * @return
   * @seez mitll.langtest.server.LangTestDatabaseImpl#getPhoneScores
   */
  public PhoneReport getPhonesForUser(int id, int minRecordings, int listid) {
    try {
      String sql = getPerfSQL(id);
      Map<Integer, UserInfo> best = getBest(sql, minRecordings);
      return getPhoneReport(id, best, database.getLanguage(), null);
    } catch (Exception ee) {
      logException(ee);
    }
    return null;
  }
  /**
   * @param userid
   * @param listid
   * @paramx projid
   * @return
   * @seez mitll.langtest.server.LangTestDatabaseImpl#getWordScores
   */
  public List<WordScore> getWordScoresForUser(int userid, int minRecordings, int listid) {
    try {
      Map<Integer, UserInfo> best = getBest(getPerfSQL(userid), minRecordings);

      return getWordScores(best);
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
   * @see #getBestForQuery(Connection, PreparedStatement, int)
   */
  private Map<Integer, List<BestScore>> getUserToResults(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    Map<Integer, List<BestScore>> userToBest = new HashMap<>();

    int iPad = 0;
    int flashcard = 0;
    int learn = 0;
    int count = 0;
    int missing = 0;
    Set<String> missingAudio = new TreeSet<>();

    int emptyCount =0;
    while (rs.next()) {
      count++;
      String exid = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      float pronScore = rs.getFloat(ResultDAO.PRON_SCORE);
      int id = rs.getInt(ResultDAO.ID);
      int userid = rs.getInt(ResultDAO.USERID);
      String type = rs.getString(ResultDAO.AUDIO_TYPE);

      List<BestScore> results = userToBest.get(userid);
      if (results == null) userToBest.put(userid, results = new ArrayList<BestScore>());

      if (pronScore < 0) logger.warn("huh? got " + pronScore + " for " + exid + " and " + id);

      String json = rs.getString(ResultDAO.SCORE_JSON);
      if (json != null && json.equals(EMPTY_JSON)) {
        //logger.warn("getUserToResults : Got empty json " + json + " for " + exid + " : " + id);
        emptyCount++;
      }
      String device = rs.getString(ResultDAO.DEVICE_TYPE);
      String path = rs.getString(ResultDAO.ANSWER);
      boolean isiPad = device != null && device.startsWith("i");
      if (isiPad) iPad++;
      boolean isFlashcard = !isiPad && (type.startsWith("avp") || type.startsWith("flashcard"));
      if (!isiPad) {
        if (isFlashcard) flashcard++;
        else learn++;
      }
      long time = timestamp.getTime();

      String nativeAudio = null;//exToRef.get(exid);
      if (nativeAudio == null) {
        if (exid.startsWith("Custom")) {
//          logger.debug("missing audio for " + exid);
          missingAudio.add(exid);
        }
        missing++;
      }
      results.add(new BestScore(Integer.parseInt(exid), pronScore, time, id, json, isiPad, isFlashcard, trimPathForWebPage(path),
          nativeAudio));
    }

    if (DEBUG || true) {
      logger.info("getUserToResults total " + count + " missing audio " + missing +
          " iPad = " + iPad + " flashcard " + flashcard + " learn " + learn);// + " exToRef " + exToRef.size());
      if (!missingAudio.isEmpty()) logger.info("missing audio " + missingAudio);
      if (emptyCount > 0) logger.info("missing score json childCount " + emptyCount + "/" + count);
    }

    finish(connection, statement, rs);
    return userToBest;
  }
}
