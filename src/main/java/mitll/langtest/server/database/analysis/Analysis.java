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
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.user.Affiliation;
import mitll.langtest.shared.user.FirstLastUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.shared.analysis.WordScore.*;

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
  protected final ParseResultJson parseResultJson;
  private final IPhoneDAO phoneDAO;

  /**
   * @param database
   * @param phoneDAO
   * @param language
   * @see DatabaseImpl#getAnalysis(int)
   * @see DatabaseImpl#makeDAO
   */
  public Analysis(Database database, IPhoneDAO phoneDAO, String language) {
    super(database);
    parseResultJson = new ParseResultJson(database.getServerProps(), language);
    this.phoneDAO = phoneDAO;
    //logger.info("Analysis : exToRef has " + exToRef.size());
  }

  /**
   * @param userDAO
   * @param best
   * @return
   * @see SlickAnalysis#getUserInfo
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
   * For now, don't filter out everyone with a lincoln affiliation
   *
   * @param idToUserInfo
   * @return
   * @see #getUserInfos(IUserDAO, Map)
   */
  @NotNull
  private List<UserInfo> getUserInfos(Map<Integer, UserInfo> idToUserInfo, IUserDAO userDAO) {
    List<UserInfo> userInfos = new ArrayList<>();
    logger.info("getUserInfos for " + idToUserInfo.size() + " users");
    long then = System.currentTimeMillis();
    Map<Integer, FirstLastUser> firstLastUsers = userDAO.getFirstLastFor(idToUserInfo.keySet());

   // Set<String> lincoln = getLincolnAffiliations();
  //  Set<String> skipped = new TreeSet<>();

    idToUserInfo.forEach((userid, value) -> {
      FirstLastUser miniUser = firstLastUsers.get(userid);
      if (miniUser == null) {
        logger.error("getUserInfos huh? no user for " + userid);
      } else {
//        boolean isLL = lincoln.contains(miniUser.getAffiliation().toLowerCase());
//        if (isLL) {
//          skipped.add(miniUser.getUserID());
//          logger.info("getUserInfos skip " + miniUser);
//        } else {
          value.setFrom(miniUser);
//          logger.info("getUserInfos got " + value.getID() + " " + value.getFirst() + " " + value.getLast());
          userInfos.add(value);
//        }
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

  @NotNull
  private Set<String> getLincolnAffiliations() {
    List<Affiliation> affiliations = database.getServerProps().getAffiliations();

    Set<Affiliation> lincolnPeople = affiliations.stream().filter(affiliation -> affiliation.getAbb().contains("incoln") || affiliation.getDisp().contains("incoln")).collect(Collectors.toSet());
    Set<String> lincoln = new HashSet<>();
    lincolnPeople.forEach(affiliation -> {
      lincoln.add(affiliation.getAbb().toLowerCase());
      lincoln.add(affiliation.getDisp().toLowerCase());
    });
    return lincoln;
  }

  private void sortUsersByTime(List<UserInfo> userInfos) {
    userInfos.sort((o1, o2) -> -1 * Long.compare(o1.getTimestampMillis(), o2.getTimestampMillis()));
  }

  /**
   * @param id
   * @param best
   * @return
   * @see SlickAnalysis#getPerformanceReportForUser
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

  int getCount(Collection<UserInfo> values) {
    return (values.isEmpty()) ? 0 : getCount(values.iterator().next().getBestScores());
  }

  /**
   * @return
   * @paramx best
   * @see SlickAnalysis#getPerformanceReportForUser
   */
  List<WordScore> getWordScores(Collection<UserInfo> values) {
    //Collection<UserInfo> values = best.values();
    logger.info("getWordScores " + values.size() + " users.");
    if (values.isEmpty()) {
      //logger.warn("no best values for " + id);
      return getWordScore(Collections.emptyList(), true);
    } else {
      List<BestScore> resultsForQuery = values.iterator().next().getBestScores();
      if (DEBUG) logger.warn("resultsForQuery " + resultsForQuery.size());

      List<WordScore> wordScore = getWordScore(resultsForQuery, true);
      if (DEBUG) {
        logger.warn("getWordScoresForUser wordScore " + wordScore.size());
      }

      return wordScore;
    }
  }

 /* List<WordScore> getWordScoresForPeriod(int projID, Map<Integer, UserInfo> best, long from, long to, String sortInfo) {
    Collection<UserInfo> values = best.values();
    logger.info("getWordScores " + values.size() + " users.");
    if (values.isEmpty()) {
      //logger.warn("no best values for " + id);
      return getWordScore(Collections.emptyList(), true);
    } else {
      List<BestScore> resultsForQuery = values.iterator().next().getBestScores();

      List<BestScore> inTime =
          resultsForQuery.stream().filter(bestScore -> from < bestScore.getTimestamp() && bestScore.getTimestamp() <= to).collect(Collectors.toList());
      if (DEBUG) logger.warn("resultsForQuery " + resultsForQuery.size());

      inTime.sort(getComparator(database.getProject(projID), Arrays.asList(sortInfo.split(",")), inTime));

      List<WordScore> wordScore = getWordScore(inTime, false);
      if (DEBUG) {
        logger.warn("getWordScoresForUser wordScore " + wordScore.size());
      }

      return wordScore;
    }
  }

  private Comparator<BestScore> getComparator(Project project, List<String> criteria, List<BestScore> inTime) {

    if (criteria.isEmpty() || criteria.iterator().next().equals("")) {
      return Comparator.comparingLong(SimpleTimeAndScore::getTimestamp);
    } else {
      String col = criteria.get(0);
      String[] split = col.split("_");
      String field = split[0];
      boolean asc = split.length <= 1 || split[1].equals(ASC);

      Map<BestScore, String> scoreToFL = new HashMap<>();
      if (field.equalsIgnoreCase(WORD)) {
        inTime.forEach(bestScore -> scoreToFL.put(bestScore, project.getExerciseByID(bestScore.getExId()).getForeignLanguage()));
      }

      return new Comparator<BestScore>() {
        @Override
        public int compare(BestScore o1, BestScore o2) {
          // text
          int comp = 0;
          switch (field) {
            case WORD:
              scoreToFL.get(o1);
              comp = scoreToFL.get(o1).compareTo(scoreToFL.get(o2));
              if (comp == 0) {
                comp = Long.compare(o1.getTimestamp(), o2.getTimestamp());
              }
              break;
            case TIMESTAMP:
              comp = Long.compare(o1.getTimestamp(), o2.getTimestamp());
              break;
            case SCORE:
              comp = Float.compare(o1.getScore(), o2.getScore());
              break;
          }
          if (comp != 0) return getComp(asc, comp);

          return comp;
        }

        int getComp(boolean asc, int comp) {
          return (asc ? comp : -1 * comp);
        }
      };
    }


  }
*/

  /**
   * @param userid
   * @param next
   * @param project
   * @return
   * @see SlickAnalysis#getPerformanceReportForUser
   */
  PhoneReport getPhoneReport(int userid, UserInfo next, Project project) {
    //UserInfo next = best.isEmpty() ? null:best.values().iterator().next();
    if (DEBUG)
      logger.debug(" getPhonesForUser " + userid + " got " + next);

    if (next == null) {
      return new PhoneReport();
    } else {
      long then = System.currentTimeMillis();
      long start = then;
      long now;
      List<Integer> resultIDs = getResultIDsForUser(next);

      then = System.currentTimeMillis();
      PhoneReport phoneReport = phoneDAO.getWorstPhonesForResults(userid, resultIDs, project);
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
  }

  PhoneReport getPhoneReportForPhone(int userid, UserInfo next, Project project, String phone, long from, long to) {
    //UserInfo next = best.isEmpty() ? null:best.values().iterator().next();
    if (DEBUG)
      logger.debug(" getPhonesForUser " + userid + " got " + next);

    if (next == null) {
      return new PhoneReport();
    } else {
      long then = System.currentTimeMillis();
      long start = then;
      long now;
      List<Integer> resultIDs = getResultIDsForUser(next);

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
            /*phonesForUser.size() +*/ " phones");
      }

      // setSessions(phoneReport.getPhoneToAvgSorted());

      return phoneReport;
    }
  }

  @NotNull
  private List<Integer> getResultIDsForUser(UserInfo next) {
    List<BestScore> resultsForQuery = next.getBestScores();

    List<Integer> resultIDs = new ArrayList<>();
    resultsForQuery.forEach(bs -> resultIDs.add(bs.getResultID()));

    if (DEBUG)
      logger.info("getPhonesForUser from " + resultsForQuery.size() + " added " + resultIDs.size() + " resultIDs ");
    return resultIDs;
  }

  private void setSessions(Map<String, PhoneStats> phoneToAvgSorted) {
    new PhoneAnalysis().setSessionsWithPrune(phoneToAvgSorted);
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

      int last = -1;

      long lastTimestamp = 0;
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
        userToUserInfo.put(userID, new UserInfo(bestScores, userToEarliest.get(userID)));
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
   * @return
   * @see #getWordScores
   */
  protected List<WordScore> getWordScore(List<BestScore> bestScores, boolean doDefaultSort) {
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
        Map<NetPronImageType, List<SlimSegment>> netPronImageTypeListMap = parseResultJson.slimReadFromJSON(json);
        netPronImageTypeListMap.remove(NetPronImageType.PHONE_TRANSCRIPT);
        //WordScore wordScore = new WordScore(bs, netPronImageTypeListMap);
        results.add(new WordScore(bs, netPronImageTypeListMap));
      } else {
//        logger.warn("getWordScore score " + bs.getScore()  + " is below threshold.");
        skipped++;
      }
    }

    long now = System.currentTimeMillis();
    if (now - then > 20) {
      logger.debug("getWordScore took " + (now - then) + " millis to parse json for " + bestScores.size() + " best scores");
    }

    if (doDefaultSort) {
      then = System.currentTimeMillis();
      Collections.sort(results);
      now = System.currentTimeMillis();
      if (now - then > 20) {
        logger.debug("getWordScore took " + (now - then) + " millis to sort " + bestScores.size() + " best scores");
      }
    }
    //   logger.info("getWordScore out of " + bestScores.size() + " skipped " + skipped);

    return results;
  }
}