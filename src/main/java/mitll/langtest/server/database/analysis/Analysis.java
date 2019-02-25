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
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.user.FirstLastUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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
  static final String EMPTY_JSON = "{}";
  private static final String D_ADMIN = "d.admin";
  final ParseResultJson parseResultJson;
  private final IPhoneDAO phoneDAO;

  /**
   * @param database
   * @param phoneDAO
   * @param languageEnum
   * @see DatabaseImpl#getAnalysis(int)
   * @see DatabaseImpl#makeDAO
   */
  public Analysis(Database database, IPhoneDAO phoneDAO, Language languageEnum) {
    super(database);
    parseResultJson = new ParseResultJson(database.getServerProps(), languageEnum);
    this.phoneDAO = phoneDAO;
    //logger.info("Analysis : exToRef has " + exToRef.size());
  }

  /**
   * @param userDAO
   * @param best
   * @return
   * @see SlickAnalysis#getUserInfo
   */
  List<UserInfo> getSortedUserInfos(IUserDAO userDAO, Map<Integer, UserInfo> best, boolean sortByPoly) {
    List<UserInfo> userInfos = getUserInfos(best, userDAO);

    if (sortByPoly) {
      userInfos.sort((o1, o2) -> -1 * Integer.compare(o1.getLastSessionScore(), o2.getLastSessionScore()));
    } else {
      sortUsersByTime(userInfos);
    }

    return userInfos;
  }

  /**
   * For now, don't filter out everyone with a lincoln affiliation
   *
   * @param idToUserInfo
   * @return
   * @see #getSortedUserInfos
   */
  @NotNull
  private List<UserInfo> getUserInfos(Map<Integer, UserInfo> idToUserInfo, IUserDAO userDAO) {
    List<UserInfo> userInfos = new ArrayList<>();
    logger.info("getUserInfos for " + idToUserInfo.size() + " users");
    long then = System.currentTimeMillis();
    Map<Integer, FirstLastUser> firstLastUsers = userDAO.getFirstLastFor(idToUserInfo.keySet());

    idToUserInfo.forEach((userid, value) -> {
      FirstLastUser miniUser = firstLastUsers.get(userid);
      if (miniUser == null) {
        logger.error("getUserInfos huh? no user for " + userid);
      } else {
        value.setFrom(miniUser);
        userInfos.add(value);
      }
    });
    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.info("getUserInfos : took " + (now - then) + " to get " + idToUserInfo.size() + " user infos"
          // +         ", skipped " + skipped
      );
    }
    return userInfos;
  }

  private void sortUsersByTime(List<UserInfo> userInfos) {
    userInfos.sort((o1, o2) -> -1 * Long.compare(o1.getTimestampMillis(), o2.getTimestampMillis()));
  }

  /**
   * @param id
   * @param best
   * @return
   * @see IAnalysis#getPerformanceReportForUser
   */
  UserPerformance getUserPerformance(long id, Map<Integer, UserInfo> best) {
    Collection<UserInfo> values = best.values();
    if (values.isEmpty()) {
      if (DEBUG) logger.debug("getUserPerformance no results for " + id);
      return new UserPerformance();
    } else {
      if (values.size() > 1) logger.error("getUserPerformance only expecting one user for " + id);
      UserInfo firstUser = values.iterator().next();
      if (DEBUG) logger.debug("getUserPerformance results for " + values.size() + "  first  " + firstUser);
      if (DEBUG) logger.debug("getUserPerformance resultsForQuery for " + firstUser.getBestScores().size());
      UserPerformance userPerformance = new UserPerformance(id, firstUser.getBestScores(), firstUser.getFirst(), firstUser.getLast());

      {
        List<TimeAndScore> rawBestScores = userPerformance.getRawBestScores();
        logger.debug("getUserPerformance for " + id + " found " + rawBestScores.size() + " scores");
        userPerformance.setGranularityToSessions(
            new PhoneAnalysis().getGranularityToSessions(rawBestScores));
      }
      return userPerformance;
    }
  }

  int getCount(Collection<UserInfo> values) {
    return (values.isEmpty()) ? 0 : getCount(values.iterator().next().getBestScores());
  }

  /**
   * @return
   * @paramx best
   * @see IAnalysis#getPerformanceReportForUser
   */
/*  List<WordScore> getWordScores(Collection<UserInfo> values, int projID) {
    //Collection<UserInfo> values = best.values();
    logger.info("getWordScores " + values.size() + " users.");
    if (values.isEmpty()) {
      //logger.warn("no best values for " + id);
      return getWordScore(Collections.emptyList(), true, projID);
    } else {
      List<BestScore> resultsForQuery = values.iterator().next().getBestScores();
      if (DEBUG) logger.warn("resultsForQuery " + resultsForQuery.size());

      List<WordScore> wordScore = getWordScore(resultsForQuery, true, projID);
      if (DEBUG) {
        logger.warn("getWordScoresForUser wordScore " + wordScore.size());
      }

      return wordScore;
    }
  }*/

  /**
   * @param userid
   * @param next
   * @param project
   * @param from
   * @param to
   * @return
   * @see SlickAnalysis#getPhoneReportForPeriod
   */
 /* PhoneReport getPhoneReportForPeriod(int userid, UserInfo next, Project project, long from, long to) {
    List<Integer> resultIDs = getResultIDsInTimeWindow(next, from, to);
    PhoneReport phoneReport = phoneDAO.getWorstPhonesForResults(userid, resultIDs, project);
    setSessions(userid, phoneReport);

    return phoneReport;
  }*/

  /**
   * @param analysisRequest
   * @param next
   * @return
   * @see IAnalysis#getPhoneSummaryForPeriod(AnalysisRequest)
   */
  PhoneSummary getPhoneSummaryForPeriod(AnalysisRequest analysisRequest, UserInfo next) {
    //  List<Integer> resultIDs = getResultIDsInTimeWindow(next, from, to, Collections.emptySet());

    List<Integer> resultIDs = getResultIDsForRequest(analysisRequest, next);

    if (DEBUG) logger.info("getPhoneSummaryForPeriod " +
        "\n\treq                 " + analysisRequest +
        "\n\tuser                " + next +
        "\n\tresultIDs " + resultIDs.size()
    );

    PhoneSummary phoneReport = phoneDAO.getPhoneSummary(analysisRequest.getUserid(), resultIDs);
    setSessions(analysisRequest.getUserid(), phoneReport);

    return phoneReport;
  }

  PhoneBigrams getPhoneBigramsForPeriod(AnalysisRequest analysisRequest, UserInfo next) {
    List<Integer> resultIDsForRequest = getResultIDsForRequest(analysisRequest, next);

    if (DEBUG) logger.info("getPhoneBigramsForPeriod " +
        "\n\treq                 " + analysisRequest +
        "\n\tuser                " + next +
        "\n\tresultIDsForRequest " + resultIDsForRequest.size()
    );

    return phoneDAO.getPhoneBigrams(analysisRequest.getUserid(), resultIDsForRequest);
  }

  @NotNull
  private List<Integer> getResultIDsForRequest(AnalysisRequest analysisRequest, UserInfo next) {
    return getResultIDsInTimeWindow(next,
        analysisRequest.getFrom(),
        analysisRequest.getTo(),
        getDialogExerciseIDs(analysisRequest.getDialogID()));
  }

  protected abstract Collection<Integer> getDialogExerciseIDs(int dialogID);

  @NotNull
  private List<Integer> getResultIDsInTimeWindow(UserInfo next, long from, long to, Collection<Integer> exids) {
    List<BestScore> resultsForQuery = next.getBestScores();

    if (!exids.isEmpty()) {
      int before = resultsForQuery.size();

      resultsForQuery = resultsForQuery
          .stream()
          .filter(bestScore -> exids.contains(bestScore.getExId()))
          .collect(Collectors.toList());

      if (DEBUG) {
        logger.info("getResultIDsInTimeWindow " +
            "\n\tbefore " + before +
            "\n\tafter  " + resultsForQuery.size() + " scores ");
      }
    }

    List<Integer> resultIDs = new ArrayList<>();

    resultsForQuery.forEach(bs -> {
      long timestamp = bs.getTimestamp();
      if (timestamp > from && timestamp <= to) {
        resultIDs.add(bs.getResultID());
      } else {
//        logger.info("getResultIDsInTimeWindow : skip " + bs.getResultID() + " at " + new Date(timestamp));
      }
    });

    if (DEBUG)
      logger.info("getResultIDsInTimeWindow " +
          "\n\tfrom  " + resultsForQuery.size() +
          "\n\ttime from  " + from + " " + new Date(from) +
          "\n\ttime to    " + to + " " + new Date(to) +
          "\n\tadded " + resultIDs.size() + " resultIDs ");

    return resultIDs;
  }

  /**
   * @param userid
   * @param next
   * @return
   * @see mitll.langtest.client.analysis.AnalysisTab#AnalysisTab
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceReportForUser
   * @see IAnalysis#getPerformanceReportForUser
   */
/*  PhoneReport getPhoneSummary(int userid, UserInfo next, Project project) {
    if (DEBUG) {
      logger.debug(" getPhonesForUser " + userid + " got " + next);
    }

    if (next == null) {
      return new PhoneReport();
    } else {
      long then = System.currentTimeMillis();
      long start = then;
      long now;
      List<Integer> resultIDs = getResultIDsForUser(next.getBestScores());

      then = System.currentTimeMillis();
      PhoneReport phoneReport = phoneDAO.getWorstPhonesForResults(userid, resultIDs, project);
      now = System.currentTimeMillis();

      long diff = now - then;
      if (DEBUG || diff > 100) {
        logger.debug("getPhonesForUser " + userid + " took " + diff + " millis to phone report");
      }
      if (DEBUG) logger.info("getPhonesForUser report phoneReport " + phoneReport);

      long diff2 = System.currentTimeMillis() - start;
      if (DEBUG || diff2 > 100) {
        logger.debug("getPhonesForUser " + userid + " took " + diff2 + " millis to get " +
            *//*phonesForUser.size() +*//* " phones for " + resultIDs.size() + " result ids ");
      }

      setSessions(userid, phoneReport);

      return phoneReport;
    }
  }*/
  public PhoneSummary getPhoneSummary(int userid, UserInfo next) {
    if (next == null) {
      return new PhoneSummary();
    } else {
      long then = System.currentTimeMillis();
      long start = then;
      long now;
      List<Integer> resultIDs = getResultIDsForUser(next.getBestScores());
      if (DEBUG) logger.info("getPhoneSummary for " + userid + " " + resultIDs.size());

      then = System.currentTimeMillis();
      PhoneSummary phoneReport = phoneDAO.getPhoneSummary(userid, resultIDs);
      now = System.currentTimeMillis();

      long diff = now - then;
      if (DEBUG || diff > 100) {
        logger.debug("getPhonesForUser " + userid + " took " + diff + " millis to phone report");
      }
      if (DEBUG) logger.info("getPhonesForUser report phoneReport " + phoneReport);

      long diff2 = System.currentTimeMillis() - start;
      if (DEBUG || diff2 > 100) {
        logger.debug("getPhonesForUser " + userid + " took " + diff2 + " millis to get " +
            /*phonesForUser.size() +*/ " phones for " + resultIDs.size() + " result ids ");
      }

      setSessions(userid, phoneReport);

      return phoneReport;
    }
  }

  private void setSessions(int userid, PhoneSummary phoneReport) {
    Map<String, PhoneStats> phoneToAvgSorted = phoneReport.getPhoneToAvgSorted();
    if (phoneToAvgSorted.isEmpty()) {
      logger.warn("getPhonesForUser : no phones for " + userid + "?");
    } else {
      if (DEBUG) logger.info("phones for " + userid + " : " + phoneToAvgSorted.keySet());
    }

    new PhoneAnalysis().setSessionsWithPrune(phoneToAvgSorted);
  }


  /**
   * @param userid
   * @param next
   * @param project
   * @param phone
   * @param from
   * @param to
   * @return
   * @seex SlickAnalysis#getBigramPhoneReportFor(AnalysisRequest)
   * @seex IAnalysis#getPhoneReportFor(AnalysisRequest)
   */
/*  PhoneReport getPhoneReportForPhone(int userid, UserInfo next, Project project, String phone, long from, long to) {
    if (DEBUG)
      logger.debug(" getPhoneReportForPhone " + userid + " got " + next + " from " + new Date(from) + " to " + new Date(to));

    if (next == null) {
      return new PhoneReport();
    } else {
      long then = System.currentTimeMillis();
      long start = then;
      long now;
      List<Integer> resultIDs = getResultIDsForUser(next.getBestScores());

      then = System.currentTimeMillis();
      PhoneReport phoneReport = phoneDAO.getWorstPhonesForResultsForPhone(userid, resultIDs, project, phone, from, to);
      now = System.currentTimeMillis();

      long diff = now - then;
      if (DEBUG || diff > 100) {
        logger.debug(" getPhonesForUser " + userid + " took " + diff + " millis to phone report");
      }
      if (DEBUG) logger.info("getPhonesForUser report phoneReport " + phoneReport);

      long diff2 = System.currentTimeMillis() - start;
      if (DEBUG || diff2 > 100) {
        logger.debug(" getPhonesForUser " + userid + " took " + diff2 + " millis to get " +
            *//*phonesForUser.size() +*//* " phones");
      }
      // setSessions(phoneReport.getPhoneToAvgSorted());

      return phoneReport;
    }
  }*/

  /**
   * @param userid
   * @param next
   * @param project
   * @param from
   * @param to
   * @return
   * @see mitll.langtest.client.analysis.BigramContainer#clickOnPhone2
   * @see IAnalysis#getPhoneReportFor(AnalysisRequest)
   */
  public PhoneReport getPhoneReportForPhoneForBigrams(int userid, UserInfo next, Project project, long from, long to) {
    if (DEBUG || true)
      logger.info("getPhoneReportForPhoneForBigrams " + userid + " got " + next + " from " + new Date(from) + " to " + new Date(to));

    if (next == null) {
      return new PhoneReport();
    } else {
      long then = System.currentTimeMillis();
      long start = then;
      long now;
      List<Integer> resultIDs = getResultIDsInTimeWindow(next, from, to, Collections.emptySet());

      then = System.currentTimeMillis();
      PhoneReport phoneReport = phoneDAO.getWorstPhonesForResultsForTimeWindow(userid, resultIDs, project, from, to);
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
      // setSessions(phoneReport.getPhoneToAvgSorted());

      return phoneReport;
    }
  }

  @NotNull
  private List<Integer> getResultIDsForUser(List<BestScore> resultsForQuery) {
    List<Integer> resultIDs = new ArrayList<>(resultsForQuery.size());
    resultsForQuery.forEach(bs -> resultIDs.add(bs.getResultID()));

    if (DEBUG)
      logger.info("getPhonesForUser from " + resultsForQuery.size() + " added " + resultIDs.size() + " resultIDs ");
    return resultIDs;
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
    userToBest.keySet().forEach(id -> userToBest2.put(id, new ArrayList<>()));

    Map<Integer, Long> userToEarliest = new HashMap<>();

    userToBest.forEach((userID, bestScores1) -> {
      List<BestScore> bestScores = userToBest2.get(userID);

      if (bestScores == null) {
        logger.warn("getBestForQuery : huh? no user " + userID + " in " + userToBest2.keySet());
      } else {
        int last = -1;

        long lastTimestamp = 0;
        long currentSession = 0;

        BestScore lastBest = null;
        Set<Integer> seen = new HashSet<>();

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
          long sessionStart = bs.getSessionStart();
          if ((last != -1 && last != exid) ||
              (currentSession > 0 && sessionStart != currentSession) ||
              (lastTimestamp > 0 && time - lastTimestamp > FIVE_MINUTES)

          ) {
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
          currentSession = sessionStart;
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
    });

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
    userToBest2.forEach((userID, bestScores) -> {
      if (bestScores.size() >= minRecordings) {
        UserInfo value = new UserInfo(bestScores, userToEarliest.get(userID));

        if (value.getUserID().equalsIgnoreCase(D_ADMIN)) {
          logger.info("getUserIDToInfo : skipping " + D_ADMIN);
        } else {
          userToUserInfo.put(userID, value);
          if (value.getLastSessionSize() > 0) {
            logger.info("user info for " + userID + " value " +
                "\n\tlast session num   " + value.getLastSessionNum() +
                "\n\tlast session size  " + value.getLastSessionSize() +
                "\n\tlast session score " + value.getLastSessionScore()
            );
          }
        }
      } else {
        if (DEBUG)
          logger.debug("getUserIDToInfo skipping user " + userID + " with just " + bestScores.size() + " scores");
      }
    });

    if (DEBUG) logger.info("getUserIDToInfo Return " + userToUserInfo);
    return userToUserInfo;
  }

  String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }

  public int getCount(List<BestScore> bestScores) {
    int num = 0;
    for (BestScore bs : bestScores) {
      if (bs.getScore() > database.getServerProps().getMinAnalysisScore()) {
        num++;
      }
    }
    return num;
  }

  /**
   * TODO : why do we parse json when we could just get it out of word and phone tables????
   * <p>
   * Only show unique items -- even if BestScore might contain the same item multiple times.
   *
   * @param bestScores
   * @param doDefaultSort
   * @param projID
   * @return
   * @see #getWordScores
   */
  protected List<WordScore> getWordScore(List<BestScore> bestScores, boolean doDefaultSort, int projID) {
    logger.info("getWordScore got " + bestScores.size());
    List<WordScore> results = new ArrayList<>();

    long then = System.currentTimeMillis();
    //   int skipped = 0;
    for (BestScore bs : bestScores) {
      String json = bs.getJson();
      if (json == null) {
        //c++;
        logger.error("getWordScore huh? no json for " + bs);
      } else if (json.equals(EMPTY_JSON)) {
//        logger.warn("getWordScore json is empty for " + bs);
        // skip low scores
      } else if (bs.getScore() > database.getServerProps().getMinAnalysisScore()) {
        if (json.isEmpty()) logger.warn("no json for " + bs);
        Map<NetPronImageType, List<SlimSegment>> netPronImageTypeListMap = parseResultJson.slimReadFromJSON(json);
        netPronImageTypeListMap.remove(NetPronImageType.PHONE_TRANSCRIPT);
        WordScore e = new WordScore(bs, netPronImageTypeListMap);
        int exid = e.getExid();
        CommonExercise exerciseByID = getDatabase().getProject(projID).getExerciseByID(exid);
        if (exerciseByID != null) e.setIsContext(exerciseByID.isContext());
        results.add(e);
      } else {
//        logger.warn("getWordScore score " + bs.getScore()  + " is below threshold.");
        //     skipped++;
      }
    }

    long now = System.currentTimeMillis();
    if (now - then > 20) {
      logger.info("getWordScore took " + (now - then) + " millis to parse json for " + bestScores.size() + " best scores");
    }

    if (doDefaultSort) {
      then = System.currentTimeMillis();
      Collections.sort(results);
      now = System.currentTimeMillis();
      if (now - then > 0) {
        logger.info("getWordScore took " + (now - then) + " millis to sort " + bestScores.size() + " best scores");
      }
    }
    logger.info("getWordScore out of " + bestScores.size() + " return " + results.size());

    return results;
  }
}