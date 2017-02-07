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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/21/15.
 */
public abstract class Analysis extends DAO {
  private static final Logger logger = LogManager.getLogger(Analysis.class);

  private static final boolean DEBUG = false;

  private static final int FIVE_MINUTES = 5 * 60 * 1000;
  //private static final float MIN_SCORE_TO_SHOW = 0.20f;
  static final String EMPTY_JSON = "{}";
  private final ParseResultJson parseResultJson;
  private final IPhoneDAO phoneDAO;

  /**
   * ex to ref can get stale, etc.
   * why do we need to do all this work when they may never click on the reference audio?
   * can't we get them for the visible set?
   * or ask exercise service?
   *
   * @deprecated  let's not use this map - super expensive to make, every time
   */
 // final Map<Integer, String> exToRef;

  /**
   * @param database
   * @param phoneDAO
   * @see DatabaseImpl#getAnalysis(int)
   * @see DatabaseImpl#makeDAO
   */
  public Analysis(Database database, IPhoneDAO phoneDAO) {
    super(database);
    parseResultJson = new ParseResultJson(database.getServerProps());
    this.phoneDAO = phoneDAO;
 //   this.exToRef = exToRef;
    //logger.info("Analysis : exToRef has " + exToRef.size());
  }

  /**
   * @param userDAO
   * @param best
   * @return
   * @see IAnalysis#getUserInfo
   */
  List<UserInfo> getUserInfos(IUserDAO userDAO, Map<Integer, UserInfo> best) {
    List<UserInfo> userInfos = getUserInfos(best, userDAO);
    sortUsersByTime(userInfos);

    // TODO : choose the initial granularity and set initial and current to those values
    // TODO : do we want to use a session as the unit for the last group???
/*
    for (UserInfo userInfo : userInfos) {
      Map<Long, List<PhoneSession>> granularityToSessions =
          new PhoneAnalysis().getGranularityToSessions(userInfo.getBestScores());

      List<PhoneSession> phoneSessions = chooseGran(granularityToSessions);
      if (!phoneSessions.isEmpty()) {
        logger.info("getUserInfos For " +userInfo+
            " Got " + phoneSessions.size() + " sessions");
        PhoneSession first = phoneSessions.get(0);
        PhoneSession last = phoneSessions.get(phoneSessions.size() - 1);
        if (phoneSessions.size() > 2 && last.getCount() < 10) {
          last = phoneSessions.get(phoneSessions.size() - 2);
        }

        //userInfo.setStart((int) Math.round(first.getMean() * 100d));
        userInfo.setCurrent((int) Math.round(last.getMean() * 100d));
      }
    }
    */

    return userInfos;
  }

  /**
   * @param best
   * @return
   * @see #getUserInfos(IUserDAO, Map)
   */
  @NotNull
  private List<UserInfo> getUserInfos(Map<Integer, UserInfo> best, IUserDAO userDAO) {
    List<UserInfo> userInfos = new ArrayList<>();

    for (Map.Entry<Integer, UserInfo> pair : best.entrySet()) {
      Integer userid = pair.getKey();

      String userChosenID = userDAO.getUserChosenID(userid);
      if (userChosenID == null) {
        logger.error("getUserInfos huh? no user for " + userid);
      } else {
     //   String userID = user.getUserID();
        boolean isLL = database.getServerProps().getLincolnPeople().contains(userChosenID);
        if (!isLL) {
          UserInfo value = pair.getValue();
          value.setId(userid); // necessary?
          value.setUserID(userChosenID);

          userInfos.add(pair.getValue());
        }
      }
    }
    return userInfos;
  }

  private void sortUsersByTime(List<UserInfo> userInfos) {
    Collections.sort(userInfos, new Comparator<UserInfo>() {
      @Override
      public int compare(UserInfo o1, UserInfo o2) {
        return -1 * Long.valueOf(o1.getTimestampMillis()).compareTo(o2.getTimestampMillis());
      }
    });
  }

/*  private List<PhoneSession> chooseGran(Map<Long, List<PhoneSession>> granularityToSessions) {
    List<Long> grans = new ArrayList<>(granularityToSessions.keySet());

    Collections.sort(grans);
    // boolean oneSet = false;
    List<PhoneSession> phoneSessions1 = Collections.emptyList();
    for (Long gran : grans) {
      //if (!oneSet) {
      List<PhoneSession> phoneSessions = granularityToSessions.get(gran);

      int size = 0;
      int total = 0;
      boolean anyBigger = false;
      for (PhoneSession session : phoneSessions) {
        //  logger.info("\t " + gran + " session " + session);
        size++;
        total += session.getCount();
        if (session.getCount() > 50) anyBigger = true;
      }
      //       String label = granToLabel.get(gran);
//        String seriesInfo = gran + "/" + label;
      // logger.info("setVisibility  " + seriesInfo + " : " + size + " sessions " + phoneSessions.size() + " any bigger " + anyBigger);

      if (PhoneSession.chooseThisSize(size, total, anyBigger)) {
        //  oneSet = true;
        phoneSessions1 = granularityToSessions.get(gran);
        //logger.info("setVisibility 1 chose " + seriesInfo + " : " + size + " visible " + series.isVisible());
        break;
      }
      //else {
      //logger.info("setVisibility 2 too small " + seriesInfo + " : " + size);
      //}
      //}
    }

*//*    if (!oneSet) {
      if (grans.isEmpty()) {
        logger.error("huh? empty map for " + granularityToSessions);
      } else {
        Long first = grans.iterator().next();
        phoneSessions1 = granularityToSessions.get(first);
      }
    }*//*
    return phoneSessions1;
  }*/

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceForUser(int, int)
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  abstract public UserPerformance getPerformanceForUser(long id, int minRecordings);

  /**
   * @see Analysis#getPerformanceForUser(long, int)
   * @param id
   * @param best
   * @return
   */
  UserPerformance getUserPerformance(long id, Map<Integer, UserInfo> best) {
    Collection<UserInfo> values = best.values();
    if (values.isEmpty()) {
      if (DEBUG) logger.debug("getUserPerformance no results for " + id);
      return new UserPerformance();
    } else {
      if (values.size() > 1) logger.error("getUserPerformance only expecting one user for " + id);
      UserInfo next = values.iterator().next();
      if (DEBUG) logger.debug("getUserPerformance results for " + values.size() + "  first  " + next);
      List<BestScore> resultsForQuery = next.getBestScores();
      if (DEBUG) logger.debug("getUserPerformance resultsForQuery for " + resultsForQuery.size());

      UserPerformance userPerformance = new UserPerformance(id, resultsForQuery);
      List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
      logger.debug("getUserPerformance found " + rawBestScores.size() + " scores");
      userPerformance.setGranularityToSessions(
          new PhoneAnalysis().getGranularityToSessions(rawBestScores));
      return userPerformance;
    }
  }

  /**
   * @paramx id
   * @paramx projid
   * @paramx minRecordings
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getWordScores
   */
/*
  public abstract List<WordScore> getWordScoresForUser(long id, int projid, int minRecordings);
*/

  /**
   * @see SlickAnalysis#getWordScoresForUser(long, int)
   * @param best
   * @return
   */
  List<WordScore> getWordScores(Map<Integer, UserInfo> best) {
    Collection<UserInfo> values = best.values();
    if (values.isEmpty()) {
      //logger.warn("no best values for " + id);
      List<BestScore> bestScores = Collections.emptyList();
      return getWordScore(bestScores);
    } else {
      UserInfo next = values.iterator().next();
      List<BestScore> resultsForQuery = next.getBestScores();
      //  if (DEBUG) logger.warn("resultsForQuery " + resultsForQuery.size());

      List<WordScore> wordScore = getWordScore(resultsForQuery);
      // if (DEBUG || true) logger.warn("getWordScoresForUser for # " +id +" min " +minRecordings + " wordScore " + wordScore.size());

      return wordScore;
    }
  }

  /**
   * TODO : still used???
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPhoneScores
   */
/*
  public abstract PhoneReport getPhonesForUser(long id, int minRecordings, int projid);
*/

  /**
   * TODO : don't use exToRef -
   * @param userid
   * @param best
   * @param language
   * @param project
   * @return
   * @see IAnalysis#getPhonesForUser
   */
  PhoneReport getPhoneReport(int userid, Map<Integer, UserInfo> best, String language, Project project) {
    long then = System.currentTimeMillis();
    long start = System.currentTimeMillis();
    long now = System.currentTimeMillis();

    if (DEBUG)
      logger.debug(" getPhonesForUser " + userid + " took " + (now - then) + " millis to get " + best.size());

    if (best.isEmpty()) return new PhoneReport();

    UserInfo next = best.values().iterator().next();
    List<BestScore> resultsForQuery = next.getBestScores();

    List<Integer> ids = new ArrayList<>();
    for (BestScore bs : resultsForQuery) {
      ids.add(bs.getResultID());
    }

    if (DEBUG) logger.info("getPhonesForUser from " + resultsForQuery.size() + " added " + ids.size() + " ids ");
    then = System.currentTimeMillis();
    PhoneReport phoneReport = phoneDAO.getWorstPhonesForResults(userid, ids, language, project);

    now = System.currentTimeMillis();

    long diff = now - then;
    if (DEBUG || diff > 100) {
      logger.debug(" getPhonesForUser " + userid + " took " + diff + " millis to phone report");
    }
    if (DEBUG) logger.info("getPhonesForUser report phoneReport " + phoneReport);

    long diff2 = System.currentTimeMillis() - start;
    if (DEBUG || diff2 > 100) {
      logger.debug(" getPhonesForUser " + userid + " took " + diff2 + " millis to get " +
          /*phonesForUser.size() +*/ " phones");
    }

    setSessions(phoneReport.getPhoneToAvgSorted());

    return phoneReport;
  }

  private void setSessions(Map<String, PhoneStats> phoneToAvgSorted) {
    new PhoneAnalysis().setSessions(phoneToAvgSorted);
  }

  /**
   * @param minRecordings
   * @param userToBest
   * @return
   * @paramx sql
   * @see SlickAnalysis#getBest(Collection, int)
   */
  protected Map<Integer, UserInfo> getBestForQuery(int minRecordings, Map<Integer, List<BestScore>> userToBest) {
//    Map<Long, List<BestScore>> userToBest = getUserToResults(connection, statement, sql);
    if (DEBUG) logger.info("getBestForQuery got " + userToBest.values().iterator().next().size());

    Map<Integer, List<BestScore>> userToBest2 = new HashMap<>();
    Map<Integer, Long> userToEarliest = new HashMap<>();

    for (Integer key : userToBest.keySet()) {
      List<BestScore> value = new ArrayList<>();
      userToBest2.put(key, value);
    }

    for (Map.Entry<Integer, List<BestScore>> pair : userToBest.entrySet()) {
      Integer userID = pair.getKey();
      List<BestScore> bestScores = userToBest2.get(userID);

      int last = -1;

      long lastTimestamp = 0;
      // int count = 0;
      BestScore lastBest = null;
      Set<Integer> seen = new HashSet<>();

      for (BestScore bs : pair.getValue()) {
        int id = bs.getResultID();
        int exid = bs.getExId();
        long time = bs.getTimestamp();

        Long aLong = userToEarliest.get(userID);
        if (aLong == null || time < aLong) userToEarliest.put(userID, time);

        if ((last != -1 && last != exid) || (lastTimestamp > 0 && time - lastTimestamp > FIVE_MINUTES)) {
          if (seen.contains(id)) {
//            logger.warn("skipping " + id);
          } else {
            bestScores.add(lastBest);
            if (DEBUG) logger.info("getBestForQuery Adding " + lastBest);
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
//          logger.warn("getBestForQuery skipping " + lastBest.getResultID());
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

    Map<Integer, UserInfo> userToUserInfo = new HashMap<>();
    int userInitialScores = database.getServerProps().getUserInitialScores();
    int userFinalScores = database.getServerProps().getUserFinalScores();

    for (Map.Entry<Integer, List<BestScore>> pair : userToBest2.entrySet()) {
      List<BestScore> value = pair.getValue();
      Integer userID = pair.getKey();
      if (value.size() >= minRecordings) {
        Long aLong = userToEarliest.get(userID);
        userToUserInfo.put(userID, new UserInfo(value, aLong, userInitialScores, userFinalScores));
      } else {
        if (DEBUG) logger.debug("getBestForQuery skipping user " + userID + " with just " + value.size() + " scores");
      }
    }

    if (DEBUG) logger.info("Return " + userToUserInfo);

    return userToUserInfo;
  }

  String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }

  /**
   * TODO : why do we parse json when we could just get it out of word and phone tables????
   * <p>
   * Only show unique items -- even if BestScore might contain the same item multiple times.
   *
   * @param bestScores
   * @return
   * @see #getWordScores
   */
  private List<WordScore> getWordScore(List<BestScore> bestScores) {
    List<WordScore> results = new ArrayList<>();

    long then = System.currentTimeMillis();
    int skipped = 0;
    for (BestScore bs : bestScores) {
      String json = bs.getJson();
      if (json == null) {
        //c++;
        logger.error("getWordScore huh? no json for " + bs);
      } else if (json.equals(EMPTY_JSON)) {
        logger.warn("getWordScore json is empty for " + bs);
        // skip low scores
      } else if (bs.getScore() > database.getServerProps().getMinAnalysisScore()) {
        if (json.isEmpty()) logger.warn("no json for " + bs);
        Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = parseResultJson.readFromJSON(json);
        WordScore wordScore = new WordScore(bs, netPronImageTypeListMap);
        results.add(wordScore);
      } else {
//        logger.warn("getWordScore score " + bs.getScore()  + " is below threshold.");
        skipped++;
      }
    }

    long now = System.currentTimeMillis();
    if (now - then > 50) {
      logger.debug(getDatabase().getLanguage() + " took " + (now - then) + " millis to parse json for " + bestScores.size() + " best scores");
    }

    then = System.currentTimeMillis();
    Collections.sort(results);
    now = System.currentTimeMillis();
    if (now - then > 50) {
      logger.debug(getDatabase().getLanguage() + " took " + (now - then) + " millis to sort " + bestScores.size() + " best scores");
    }
    logger.info("getWordScore out of " + bestScores.size() + " skipped " + skipped);

    return results;
  }
}