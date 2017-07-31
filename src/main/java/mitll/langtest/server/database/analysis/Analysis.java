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
import mitll.langtest.shared.user.MiniUser;
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

  private static final boolean DEBUG = true;

  private static final int FIVE_MINUTES = 5 * 60 * 1000;
  static final String EMPTY_JSON = "{}";
  private final ParseResultJson parseResultJson;
  private final IPhoneDAO phoneDAO;

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
    logger.info("getUserInfos for " + best.size());
    long then = System.currentTimeMillis();
    for (Map.Entry<Integer, UserInfo> pair : best.entrySet()) {
      Integer userid = pair.getKey();

      MiniUser miniUser = userDAO.getMiniUser(userid);
      if (miniUser == null) {
        logger.error("getUserInfos huh? no user for " + userid);
      } else {
        String userChosenID = miniUser.getUserID();
        boolean isLL = database.getServerProps().getLincolnPeople().contains(userChosenID);
        if (!isLL) {
          UserInfo value = pair.getValue();
          value.setId(userid); // necessary?
          value.setUserID(userChosenID);
          value.setFirst(miniUser.getFirst());
          value.setLast(miniUser.getLast());

          userInfos.add(pair.getValue());
        }
      }
    }
    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.info("getUserInfos : took " + (now - then) + " to get " + best.size() + " user infos");
    }
    return userInfos;
  }

  private void sortUsersByTime(List<UserInfo> userInfos) {
    userInfos.sort((o1, o2) -> -1 * Long.valueOf(o1.getTimestampMillis()).compareTo(o2.getTimestampMillis()));
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
   * @param listid
   * @return
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceForUser(int, int)
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  abstract public UserPerformance getPerformanceForUser(int id, int minRecordings, int listid);

  /**
   * @param id
   * @param best
   * @return
   * @see Analysis#getPerformanceForUser(int, int, int)
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
      if (DEBUG) logger.debug("getUserPerformance resultsForQuery for " + next.getBestScores().size());
      UserPerformance userPerformance = new UserPerformance(id, next.getBestScores(), next.getFirst(), next.getLast());
      List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
      logger.debug("getUserPerformance for " + id + " found " + rawBestScores.size() + " scores");
      userPerformance.setGranularityToSessions(
          new PhoneAnalysis().getGranularityToSessions(rawBestScores));
      return userPerformance;
    }
  }

  /**
   * @param best
   * @return
   * @see IAnalysis#getWordScoresForUser(int, int, int)
   */
  List<WordScore> getWordScores(Map<Integer, UserInfo> best) {
    Collection<UserInfo> values = best.values();
    logger.info("getWordScores " + values.size() + " users.");
    if (values.isEmpty()) {
      //logger.warn("no best values for " + id);
      return getWordScore(Collections.emptyList());
    } else {
      List<BestScore> resultsForQuery = values.iterator().next().getBestScores();
      if (DEBUG) logger.warn("resultsForQuery " + resultsForQuery.size());

      List<WordScore> wordScore = getWordScore(resultsForQuery);
      if (DEBUG) {
        logger.warn("getWordScoresForUser wordScore " + wordScore.size());
      }

      return wordScore;
    }
  }

  /**
   * @param userid
   * @param best
   * @param language
   * @param project
   * @return
   * @see IAnalysis#getPhonesForUser
   */
  PhoneReport getPhoneReport(int userid, Map<Integer, UserInfo> best, String language, Project project) {
    long then = System.currentTimeMillis();
    long start = then;
    long now;

    if (DEBUG)
      logger.debug(" getPhonesForUser " + userid + " got " + best.size());

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
   * remember the last best attempt we have in a sequence for an item, but if they come back to practice it more than 5 minutes later
   * consider it a new score
   *
   * @param minRecordings
   * @param userToBest
   * @return
   * @see SlickAnalysis#getBest
   */
  protected Map<Integer, UserInfo> getBestForQuery(int minRecordings, Map<Integer, List<BestScore>> userToBest) {
    if (DEBUG) {
      Collection<List<BestScore>> values = userToBest.values();
      int size = values.isEmpty() ? 0 : values.iterator().next().size();
      logger.info("getBestForQuery min " + minRecordings + " got " + size);
    }

    Map<Integer, List<BestScore>> userToBest2 = new HashMap<>();
    Map<Integer, Long> userToEarliest = new HashMap<>();

    for (Integer key : userToBest.keySet()) {
      userToBest2.put(key, new ArrayList<>());
    }

    for (Map.Entry<Integer, List<BestScore>> pair : userToBest.entrySet()) {
      Integer userID = pair.getKey();
      List<BestScore> bestScores = userToBest2.get(userID);

      int last = -1;

      long lastTimestamp = 0;
      BestScore lastBest = null;
      Set<Integer> seen = new HashSet<>();

      List<BestScore> bestScores1 = pair.getValue();
      if (DEBUG) logger.info("getBestForQuery examining " + bestScores1.size() + " best scores for " + userID);

      // remember the last best attempt we have in a sequence, but if they come back to practice it more than 5 minutes later
      // consider it a new score
      for (BestScore bs : bestScores1) {
        int id = bs.getResultID();
        int exid = bs.getExId();
        long time = bs.getTimestamp();

        {
          Long aLong = userToEarliest.get(userID);
          if (aLong == null || time < aLong) userToEarliest.put(userID, time);
        }

        // So the purpose here is to skip over multiple tries for an item within a sort session (5 minutes)
        if ((last != -1 && last != exid) || (lastTimestamp > 0 && time - lastTimestamp > FIVE_MINUTES)) {
          if (seen.contains(id)) {
            logger.warn("getBestForQuery skipping " + id); // surprising if this were true
          } else {
            bestScores.add(lastBest);
            seen.add(lastBest.getResultID());
            if (DEBUG)
              logger.info("getBestForQuery Adding " + lastBest + " now " + seen.size() + " vs " + bestScores.size());
          }
          lastTimestamp = time;
        }
        if (lastTimestamp == 0) lastTimestamp = time;
        last = exid;
        lastBest = bs;
      }

      if (lastBest != null) {
        if (seen.contains(lastBest.getResultID())) { // how could this happen?
          logger.warn("getBestForQuery skipping result id " + lastBest.getResultID() + " b/c already added to (" + seen.size() +
              ") " + seen + "\n\tvs " + bestScores.size());
        } else {
          if (DEBUG) logger.debug("getBestForQuery bestScores now " + bestScores.size());
          bestScores.add(lastBest);
        }
      }
    }

    return getUserIDToInfo(minRecordings, userToBest2, userToEarliest);
  }

  /**
   * Filter out users who don't make at least the minimum number of recordings.
   *
   * @param minRecordings  skip users who really didn't make any recordings
   * @param userToBest2
   * @param userToEarliest
   * @return
   */
  @NotNull
  private Map<Integer, UserInfo> getUserIDToInfo(int minRecordings,
                                                 Map<Integer, List<BestScore>> userToBest2,
                                                 Map<Integer, Long> userToEarliest) {
    Map<Integer, UserInfo> userToUserInfo = new HashMap<>();
    int userFinalScores = database.getServerProps().getUserFinalScores();

    for (Map.Entry<Integer, List<BestScore>> pair : userToBest2.entrySet()) {
      List<BestScore> value = pair.getValue();
      Integer userID = pair.getKey();
      if (value.size() >= minRecordings) {
        userToUserInfo.put(userID, new UserInfo(value, userToEarliest.get(userID), userFinalScores));
      } else {
        if (DEBUG) logger.debug("getUserIDToInfo skipping user " + userID + " with just " + value.size() + " scores");
      }
    }

    if (DEBUG) logger.info("getUserIDToInfo Return " + userToUserInfo);
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
    // logger.warn("getWordScore got " + bestScores.size());

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
    //   logger.info("getWordScore out of " + bestScores.size() + " skipped " + skipped);

    return results;
  }
}