/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.analysis;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.audio.NativeAudioResult;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.services.AnalysisServiceImpl;
import mitll.langtest.shared.WordsAndTotal;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.SlickPerfResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.shared.analysis.WordScore.*;

public class SlickAnalysis extends Analysis implements IAnalysis {
  private static final Logger logger = LogManager.getLogger(SlickAnalysis.class);

  private static final int WARN_THRESH = 100;
  //  private static final String ANSWERS = "answers";
//  private static final int MAX_TO_SEND = 25;
  private static final int DEFAULT_PROJECT = 1;
  private static final int UNKNOWN_EXERCISE = 2;
  private final SlickResultDAO resultDAO;
  private final Language language;
  private final int projid;
  private final Project project;

  private final IAudioDAO audioDAO;
  private final boolean sortByPolyScore;
  private Collator collator;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PHONE = false;

  /**
   * @param database
   * @param phoneDAO
   * @param language
   * @param projid
   * @param sortByPolyScore
   * @paramx dialogDAO
   * @see ProjectServices#configureProject
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceReportForUser
   */
  public SlickAnalysis(Database database,
                       IPhoneDAO phoneDAO,
                       IAudioDAO audioDAO,
                       SlickResultDAO resultDAO,

                       Language language,
                       int projid,
                       boolean sortByPolyScore) {
    super(database, phoneDAO, language);
    this.resultDAO = resultDAO;
    this.language = language;
    this.projid = projid;
    this.audioDAO = audioDAO;
    project = database.getProject(projid);
    this.sortByPolyScore = sortByPolyScore;
  }

  private Collator getCollator() {
    if (collator == null) {
      collator = project.getAudioFileHelper().getCollator();
    }
    return collator;
  }

  /**
   * @param analysisRequest
   * @return
   * @paramx userid
   * @see mitll.langtest.client.analysis.AnalysisTab#AnalysisTab
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceReportForUser
   */
  @Override
  public AnalysisReport getPerformanceReportForUser(AnalysisRequest analysisRequest) {
    int userid = analysisRequest.getUserid();

    if (userid == -1) {
      throw new IllegalArgumentException("must specify a user to make a request for");
    } else {
      Map<Integer, UserInfo> bestForUser = getBestForUser(analysisRequest);

      Collection<UserInfo> userInfos = bestForUser.values();
      UserInfo firstUser = bestForUser.isEmpty() ? null : getFirstUser(userInfos);

      long then = System.currentTimeMillis();

      AnalysisReport analysisReport = new AnalysisReport(
          getUserPerformance(userid, bestForUser),
          getPhoneSummary(userid, firstUser),
          getCount(userInfos),
          analysisRequest.getReqid());

      {
        long now = System.currentTimeMillis();
        logger.info("getPerformanceReportForUser (took " + (now - then) + ") analysis report for user " + userid +
            " and list " + analysisRequest.getListid());// + analysisReport);
      }
      return analysisReport;
    }
  }

  /**
   * @param analysisRequest
   * @return
   */
  public PhoneSummary getPhoneSummaryForPeriod(AnalysisRequest analysisRequest) {
    return getPhoneSummaryForPeriod(analysisRequest, getUserInfo(analysisRequest));
  }

  /**
   * @param analysisRequest
   * @return
   * @see AnalysisServiceImpl#getPhoneBigrams
   */
  public PhoneBigrams getPhoneBigramsForPeriod(AnalysisRequest analysisRequest) {
    UserInfo userInfo = getUserInfo(analysisRequest);
    if (userInfo == null) {
      logger.warn("getPhoneBigramsForPeriod no user info for " + analysisRequest);
      return new PhoneBigrams();
    } else {
      return getPhoneBigramsForPeriod(analysisRequest, userInfo);
    }
  }

  /**
   * @param analysisRequest
   * @param rangeStart
   * @param rangeEnd
   * @param sort
   * @return
   * @see AnalysisServiceImpl#getWordScoresForUser
   */
  @Override
  public WordsAndTotal getWordScoresForUser(AnalysisRequest analysisRequest, int rangeStart, int rangeEnd,
                                            String sort) {
    Map<Integer, UserInfo> bestForUser = getBestForUser(analysisRequest);
    Collection<UserInfo> userInfos = bestForUser.values();
    logger.info("getWordScoresForUser for user " + analysisRequest.getUserid() +
        " got " + userInfos.size() + " user from " + rangeStart + " to " + rangeEnd + " sort " + sort);
    return getWordScoresForPeriod(userInfos, analysisRequest.getFrom(), analysisRequest.getTo(), rangeStart, rangeEnd, sort);
  }

  /**
   * @param userInfos
   * @param from
   * @param to
   * @param rangeStart
   * @param rangeEnd
   * @param sortInfo
   * @return
   * @see IAnalysis#getWordScoresForUser(AnalysisRequest, int, int, String)
   */
  private WordsAndTotal getWordScoresForPeriod(Collection<UserInfo> userInfos,
                                               long from, long to,
                                               int rangeStart, int rangeEnd,

                                               String sortInfo) {
    if (userInfos.isEmpty()) {
      //logger.warn("no best values for " + id);
      return new WordsAndTotal(Collections.emptyList(), 0, false);
    } else {
      List<WordScore> wordScores = getWordScores(userInfos, from, to, sortInfo);
      int totalSize = wordScores.size();
      if (DEBUG) logger.info("getWordScoresForUser got " + totalSize + " word and score ");
      // wordScores.forEach(bestScore -> logger.info("after " + bestScore));

      // sublist is not serializable!
      int min = Math.min(wordScores.size(), rangeEnd);

      // prevent sublist range error
      int startToUse = min < rangeStart ? 0 : rangeStart;

      wordScores = new ArrayList<>(wordScores.subList(startToUse, min));

      if (DEBUG) {
        logger.warn("getWordScoresForUser wordScores " + totalSize);
      }
      if (DEBUG) {
        logger.warn("getWordScoresForUser wordScores " + totalSize + " vs " + wordScores.size() + "/" + min);
      }

      return new WordsAndTotal(wordScores, totalSize, areAllSameDay(wordScores));
    }
  }

  private boolean areAllSameDay(List<WordScore> wordScores) {
    boolean allSameDay = true;
    int dayOfYear = -1;
    Calendar instance = Calendar.getInstance();
    for (WordScore ws : wordScores) {
      instance.setTimeInMillis(ws.getTimestamp());
      int i = instance.get(Calendar.DAY_OF_YEAR);
//      logger.info("day of year " + i + " for " + new Date(ws.getTimestamp()) + " ws " + ws);
      if (dayOfYear == -1) {
        dayOfYear = i;
      } else if (i != dayOfYear) {
//        logger.info("day of year " + i + " vs " + dayOfYear + "  for " + new Date(ws.getTimestamp()) + " ws " + ws);
        allSameDay = false;
        break;
      }
    }
    //  logger.info("allSameDay " + allSameDay);
    return allSameDay;
  }

  private List<WordScore> getWordScores(Collection<UserInfo> userInfos, long from, long to, String sortInfo) {
    List<BestScore> inTime = getBestScoresInWindow(userInfos, from, to);
    if (DEBUG || true)
      logger.info("getWordScores " +
          "\n\tgot " + inTime.size() + " scores from " + new Date(from) + " to " + new Date(to));

    //if (DEBUG) logger.warn("getWordScoresForUser " + resultsForQuery.size());

    inTime.sort(getComparator(project, Arrays.asList(sortInfo.split(",")), inTime));

    // inTime.forEach(bestScore -> logger.info("sorted " + bestScore));
    return getWordScore(inTime, false, projid);
  }

  @NotNull
  private List<BestScore> getBestScoresInWindow(Collection<UserInfo> userInfos, long from, long to) {
    List<BestScore> resultsForQuery = getFirstUser(userInfos).getBestScores();

    if (DEBUG || true) logger.info("getBestScoresInWindow got " + resultsForQuery.size() + " scores");

    return resultsForQuery
        .stream()
        .filter(bestScore ->
            from <= bestScore.getTimestamp() &&
                bestScore.getTimestamp() <= to)
        .collect(Collectors.toList());
  }


  /**
   * @param analysisRequest
   * @return
   * @see #getPhoneSummaryForPeriod
   */
  @Nullable
  private UserInfo getUserInfo(AnalysisRequest analysisRequest) {
    Map<Integer, UserInfo> bestForUser = getBestForUser(analysisRequest);
    Collection<UserInfo> userInfos = bestForUser.values();
    return bestForUser.isEmpty() ? null : getFirstUser(userInfos);
  }

  private UserInfo getFirstUser(Collection<UserInfo> userInfos) {
    return userInfos.iterator().next();
  }

  /**
   * Support word async table sort columns
   *
   * @param project
   * @param criteria
   * @param inTime
   * @return
   * @see mitll.langtest.client.analysis.WordContainerAsync#createProvider
   * @see #getWordScoresForPeriod
   */
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
        populateScoreToFL(project, inTime, scoreToFL);
      }

//      logger.info("getComparator " + scoreToFL.size() + " field " + field + " col " + col + " asc " + asc);

      return getBestScoreComparator(field, asc, scoreToFL);
    }
  }

  private void populateScoreToFL(Project project, List<BestScore> inTime, Map<BestScore, String> scoreToFL) {
    inTime.forEach(bestScore -> {
      CommonExercise exerciseByID = project.getExerciseByID(bestScore.getExId());
      if (exerciseByID == null) {
        String transcriptFromJSON = getTranscriptFromJSON(bestScore);
        logger.info("populateScoreToFL no ex for " + bestScore.getExId() + " so " + transcriptFromJSON);
        scoreToFL.put(bestScore, transcriptFromJSON);
      } else {
        String foreignLanguage = exerciseByID.getForeignLanguage();
        scoreToFL.put(bestScore, foreignLanguage);
        if (DEBUG) logger.info("populateScoreToFL " + bestScore + " = " + foreignLanguage);
      }
    });
  }

  @NotNull
  private Comparator<BestScore> getBestScoreComparator(String field, boolean asc, Map<BestScore, String> scoreToFL) {
    return new Comparator<BestScore>() {
      @Override
      public int compare(BestScore o1, BestScore o2) {
        // text
        int comp = 0;
        switch (field) {
          case WORD:
            String s1 = scoreToFL.get(o1);
            String s2 = scoreToFL.get(o2);
            comp = getCollator().compare(s1, s2);  // remember to do locale aware string sorting.
            if (comp == 0) {
              //  logger.info("getComparator fall back to time for " + o1 + " vs " + o2);
              comp = compareTimes(o1, o2);
            }
            break;
          case TIMESTAMP:
            comp = compareTimes(o1, o2);
            break;
          case SCORE:
            comp = Float.compare(o1.getScore(), o2.getScore());
            if (comp == 0) {
              comp = compareTimes(o1, o2);
            }
            break;
          default:
            logger.warn("huh? field '" + field + "' is not defined?");
        }
        if (comp != 0) return getComp(asc, comp);

        return comp;
      }

      int getComp(boolean asc, int comp) {
        return (asc ? comp : -1 * comp);
      }
    };
  }

  private int compareTimes(BestScore o1, BestScore o2) {
    return Long.compare(o1.getTimestamp(), o2.getTimestamp());
  }

  @NotNull
  private String getTranscriptFromJSON(BestScore bestScore) {
    Map<NetPronImageType, List<SlimSegment>> netPronImageTypeListMap =
        parseResultJson.slimReadFromJSON(bestScore.getJson());
    StringBuilder builder = new StringBuilder();
    List<SlimSegment> slimSegments = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT);

    if (slimSegments == null) {
      logger.warn("no word segments for ex " + bestScore.getExId() + " and result " + bestScore.getResultID());
    } else {
      slimSegments.forEach(slimSegment -> builder.append(slimSegment.getEvent()).append(" "));
    }

    return builder.toString().trim();
  }

  /**
   * @param analysisRequest@return
   * @see AnalysisServiceImpl#getPerformanceReportForUserForPhoneBigrams
   */
/*  public List<Bigram> getBigramPhoneReportFor(AnalysisRequest analysisRequest) {
    UserInfo next = getFirstUser(analysisRequest);

    long then = System.currentTimeMillis();
    long from = analysisRequest.getFrom();
    long to = analysisRequest.getTo();
    if (DEBUG || DEBUG_PHONE) {

      logger.info("getPhoneReportFor for" +
          "\n\tuser   " + next +
          "\n\tuserid " + analysisRequest.getUserid() +
          "\n\tlistid " + analysisRequest.getListid() +
          "\n\tphone  " + analysisRequest.getPhone() +
          "\n\tfrom   " + from + " " + new Date(from) +
          "\n\tto     " + to + " " + new Date(to)
      );
    }

    // PhoneReport phoneReportForPhone = getPhoneReportForPhone(userid, next, project, phone, from, to);
    PhoneBigrams phoneReportForPhone = getPhoneBigramsForPeriod(analysisRequest);
    Map<String, List<Bigram>> phoneToBigrams = phoneReportForPhone.getPhoneToBigrams();
    return phoneToBigrams.get(analysisRequest.getPhone());
  }*/

  /**
   * @param analysisRequest@return
   * @see mitll.langtest.client.analysis.BigramContainer#clickOnPhone2
   * @see AnalysisServiceImpl#getPerformanceReportForUserForPhone
   */
  public List<WordAndScore> getWordAndScoreForPhoneAndBigram(AnalysisRequest analysisRequest) {
    UserInfo next = getFirstUser(analysisRequest);

    long then = System.currentTimeMillis();

    String phone = analysisRequest.getPhone();

    if (phone.isEmpty()) logger.warn("no phone in " + analysisRequest);

    String bigram = analysisRequest.getBigram();
    long from = analysisRequest.getFrom();
    long to = analysisRequest.getTo();

    if (DEBUG || DEBUG_PHONE) {
      logger.info("getPhoneReportFor for" +
          "\n\tuser   " + next +
          "\n\tuserid " + analysisRequest.getUserid() +
          "\n\tlistid " + analysisRequest.getListid() +
          "\n\tphone  " + phone +
          "\n\tbigram " + bigram +
          "\n\tfrom   " + from + " " + new Date(from) +
          "\n\tto     " + to + " " + new Date(to)
      );
    }

    PhoneReport phoneReportForPhone = getPhoneReportForPhoneForBigrams(analysisRequest.getUserid(), next, project, from, to);


    Map<String, Map<String, List<WordAndScore>>> phoneToWordAndScoreSorted =
        phoneReportForPhone.getPhoneToWordAndScoreSorted();

//    logger.info("keys " + phoneToWordAndScoreSorted.keySet());
    // logger.info("values " + phoneToWordAndScoreSorted.values());
    Map<String, List<WordAndScore>> bigramToExample = phoneToWordAndScoreSorted.get(phone);

    if (bigramToExample == null) {
      logger.warn("getPhoneReportFor no bigrams for phone '" + phone + "'");
      return Collections.emptyList();
    } else {
      List<WordAndScore> wordAndScores = bigramToExample.get(bigram);
      if (wordAndScores == null || wordAndScores.isEmpty()) {
        logger.warn("getPhoneReportFor no examples for" +
            "\n\tphone  " + phone +
            "\n\tbigram " + bigram +
            "\n\tknown  " + bigramToExample.keySet() +
            "\n\twords  " + wordAndScores
        );
      }
      return wordAndScores;
    }
  }

  @Nullable
  private UserInfo getFirstUser(AnalysisRequest analysisRequest) {
    Map<Integer, UserInfo> bestForUser = getBestForUser(analysisRequest);
    return bestForUser.isEmpty() ? null : getFirstUser(bestForUser.values());
  }

  /**
   * @param analysisRequest
   * @return
   * @seex IAnalysis#getPhoneReportFor
   * @see IAnalysis#getPerformanceReportForUser(AnalysisRequest)
   */
  private Map<Integer, UserInfo> getBestForUser(AnalysisRequest analysisRequest) {
    long then = System.currentTimeMillis();

//    Collection<Integer> dialogExerciseIDs = getDialogExerciseIDs(dialogID);
//    logger.info("getBestForUser Dialog ids " + dialogExerciseIDs.size());

    List<SlickPerfResult> nonEnglishOnly = getNonEnglishOnly(getSlickPerfResultsForDialog(analysisRequest));
    long now = System.currentTimeMillis();

    if (DEBUG) {
      logger.info("getBestForUser best for" +
          analysisRequest +
//        " user " + userid + " in project " + projid + " and list " + listid +
          "\n\twere " + nonEnglishOnly.size());
    }

    {
      long diff = now - then;
      if (diff > WARN_THRESH) {
        logger.warn("getBestForUser best for " + analysisRequest.getUserid() + " in " + projid + " took " + diff);
      }
    }

    return getBest(nonEnglishOnly, analysisRequest.getMinRecordings(), true);
  }

  private Collection<SlickPerfResult> getSlickPerfResultsForDialog(AnalysisRequest analysisRequest) {
    Collection<SlickPerfResult> perfForUser;

//    logger.info("getSlickPerfResultsForDialog request : " + analysisRequest);

    if (analysisRequest.getDialogSessionID() == -1) {
      int dialogID = analysisRequest.getDialogID();
      int userid = analysisRequest.getUserid();
      int listid = analysisRequest.getListid();

      //logger.info("no session " + analysisRequest);
      perfForUser = listid == -1 ?
          (dialogID == -1 ?
              resultDAO.getPerfForUser(userid, projid) :
              resultDAO.getPerfForUserInDialog(userid, dialogID)) :
          resultDAO.getPerfForUserOnList(userid, listid);
    } else {
      if (DEBUG) logger.info("got session " + analysisRequest);
      perfForUser = resultDAO.getPerfForDialogSession(analysisRequest.getDialogSessionID());
    }
    return perfForUser;
  }

  @NotNull
  private List<SlickPerfResult> getNonEnglishOnly(Collection<SlickPerfResult> perfForUser) {
    Project project = database.getProject(projid);

    List<SlickPerfResult> nonEnglishOnly = perfForUser
        .stream()
        .filter(slickPerfResult ->
        {
          CommonExercise exerciseByID = project.getExerciseByID(slickPerfResult.exid());
          return exerciseByID == null || !exerciseByID.hasEnglishAttr();
        }).collect(Collectors.toList());

    if (perfForUser.size() != nonEnglishOnly.size()) {
      logger.info("getBestForUser before filter " + perfForUser.size() + " after removing english " + nonEnglishOnly.size());
    }
    return nonEnglishOnly;
  }

  @NotNull
  @Override
  protected Collection<Integer> getDialogExerciseIDs(int dialogID) {
    return project.getDialogExerciseIDs(dialogID);
  }

  /**
   * For the current project id.
   * <p>
   * TODO : Gets all recordings!!!
   *
   * @param userDAO
   * @param minRecordings
   * @return
   * @see AnalysisServiceImpl#getUsersWithRecordings
   * @see mitll.langtest.client.analysis.StudentAnalysis#StudentAnalysis
   */
  @Override
  public List<UserInfo> getUserInfo(IUserDAO userDAO, int minRecordings) {
    long then = System.currentTimeMillis();
    Collection<SlickPerfResult> perfForUser = resultDAO.getPerf(projid, database.getServerProps().getMinAnalysisScore());
    long now = System.currentTimeMillis();
    if (now - then > 100)
      logger.info("getUserInfo took " + (now - then) + " to get " + perfForUser.size() + " perf infos for project #" + projid);

    return getUserInfos(userDAO, minRecordings, perfForUser, now);
  }

  @Override
  public List<UserInfo> getUserInfoForDialog(IUserDAO userDAO, int dialogID) {
    long then = System.currentTimeMillis();
    Collection<SlickPerfResult> perfForUser = resultDAO.getPerfForDialog(dialogID);
    long now = System.currentTimeMillis();
    if (now - then > 100)
      logger.info("getUserInfoForDialog took " + (now - then) +
          "\n\tto get   " + perfForUser.size() + " perf infos for " +
          "\n\tproject #" + projid + " dialog " + dialogID);

    return getUserInfos(userDAO, 1, perfForUser, now);
  }

  private List<UserInfo> getUserInfos(IUserDAO userDAO, int minRecordings, Collection<SlickPerfResult> perfForUser, long now) {
    long then = now;
    Map<Integer, UserInfo> best = getBest(perfForUser, minRecordings, false);
    now = System.currentTimeMillis();
    if (now - then > 100)
      logger.info("getUserInfo took " + (now - then) + " to get best for " + perfForUser.size() + " for project #" + projid);

    then = now;
    List<UserInfo> userInfos = getSortedUserInfos(userDAO, best, sortByPolyScore);
    now = System.currentTimeMillis();
    if (now - then > 100)
      logger.info("getUserInfo took " + (now - then) + " to get user infos for " + userInfos.size() + " users for project #" + projid);

    return userInfos;
  }

  /**
   * @param perfForUser
   * @param minRecordings
   * @param addNativeAudio
   * @return
   * @see #getBestForUser
   */
  private Map<Integer, UserInfo> getBest(Collection<SlickPerfResult> perfForUser,
                                         int minRecordings, boolean addNativeAudio) {
    Map<Integer, List<BestScore>> userToResults = getUserToResults(perfForUser, addNativeAudio);
    if (DEBUG) logger.info("getBest got " + userToResults.size() + " user to results");
    return getBestForQuery(minRecordings, userToResults);
  }

  /**
   * @param perfs
   * @param addNativeAudio
   * @return
   * @throws SQLException
   * @see #getBest
   */
  private Map<Integer, List<BestScore>> getUserToResults(Collection<SlickPerfResult> perfs, boolean addNativeAudio) {
    long then = System.currentTimeMillis();

    Map<Integer, List<BestScore>> userToBest = new HashMap<>();

    int iPad = 0;
    int flashcard = 0;
    int learn = 0;
    int count = 0;
    int missing = 0;
    Map<Integer, MiniUser.Gender> userToGender = new HashMap<>();
    // logger.info("getUserToResults for " + perfs.size() + " results");

    int emptyCount = 0;

    if (addNativeAudio) {
      getNativeAudio(perfs);
      long l = System.currentTimeMillis() - then;
      if (l > 20) {
        logger.info("getUserToResults took " + l + " millis to get native audio for " + perfs.size());
      }
    }

    // then = System.currentTimeMillis();

    Map<Integer, MiniUser> idToMini = new HashMap<>();

    Map<String, Long> sessionToLong = new HashMap<>();
    Map<String, Integer> sessionNumToInteger = new HashMap<>();

    for (SlickPerfResult perf : perfs) {
      count++;
      int exid = perf.exid();
      long time = perf.modified().getTime();
      float pronScore = perf.pronscore();
      int id = perf.id();
      int userid = perf.userid();
      String type = perf.audiotype();

      List<BestScore> results = userToBest.computeIfAbsent(userid, k -> new ArrayList<>());

      if (pronScore < 0) logger.warn("huh? got " + pronScore + " for " + exid + " and " + id);

      String json = perf.scorejson();
      if (json != null && json.equals(EMPTY_JSON)) {
        //logger.warn("getUserToResults : Got empty json " + json + " for " + exid + " : " + id);
        emptyCount++;
      }
      Long sessionTime = getSessionTime(sessionToLong, perf.device());
      // ?? can't really get the session size here ...?
      Integer sessionSize = getNumInSession(sessionNumToInteger, perf.devicetype());
      String path = perf.answer();

      boolean isiPad = isIOS(perf);
      if (isiPad) iPad++;
      boolean isFlashcard = !isiPad && (type.startsWith("avp") || type.startsWith("flashcard"));
      if (!isiPad) {
        if (isFlashcard) flashcard++;
        else learn++;
      }

      String nativeAudio = null;
      if (addNativeAudio) {
        NativeAudioResult nativeAudio1 = database.getNativeAudio(userToGender, perf.userid(), exid, project, idToMini);
        nativeAudio = nativeAudio1.getNativeAudioRef();
        if (nativeAudio == null) {
//        if (exid.startsWith("Custom")) {
////          logger.debug("missing audio for " + exid);
//          missingAudio.add(exid);
//        }
          missing++;
        }
      }

      BestScore e = new BestScore(exid, pronScore, time, id, json, isiPad, isFlashcard,
          database.getWebPageAudioRef(language.getLanguage(), path),
          nativeAudio,
          sessionTime, sessionSize);
//      if (e.getSessionStart()> 0 && userid == 6) {
//        logger.info("getUserToResults id " + id + " = " + e.getSessionStart());
//      }

      results.add(e);
    }

    if (DEBUG) {
      long now = System.currentTimeMillis();

      logger.info("getUserToResults" +
          "\n\ttotal         " + count +
          "\n\tmissing audio " + missing +
          "\n\tiPad      " + iPad +
          "\n\tflashcard " + flashcard +
          "\n\tlearn     " + learn +
          "\n\ttook      " + (now - then) + " millis");//+ " exToRef " + exToRef.size());
      //   if (!missingAudio.isEmpty()) logger.info("missing audio " + missingAudio);
      if (emptyCount > 0) logger.info("missing score json childCount " + emptyCount + "/" + count);
    }

    return userToBest;
  }

  private boolean isIOS(SlickPerfResult perf) {
    String device = perf.devicetype();
    return device != null && device.startsWith("i");
  }

  private Integer getNumInSession(Map<String, Integer> sessionToLong, String deviceType) {
    Integer parsedTime = sessionToLong.get(deviceType);

    if (parsedTime == null) {
      try {
        parsedTime = Integer.parseInt(deviceType);
//        logger.info("getSessionTime " + parsedTime);
      } catch (NumberFormatException e) {
        //      logger.info("can't parse " + device);
        parsedTime = -1;
      }
      sessionToLong.put(deviceType, parsedTime);
    }
    return parsedTime;
  }

  /**
   * Skip results for default project - orphan exercises.
   *
   * @param perfs
   * @see #getUserToResults
   */
  private void getNativeAudio(Collection<SlickPerfResult> perfs) {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises = new ArrayList<>();

    if (DEBUG) logger.info("getNativeAudio getting exercises for " + perfs.size() + " recordings in project " + projid);

    List<Integer> skipped = new ArrayList<>();

    perfs.forEach(perf -> {
      int exid = perf.exid();

      if (exid == UNKNOWN_EXERCISE) {
        if (skipped.size() < 5 || skipped.size() % 100 == 0) {
          logger.info("getNativeAudio skipping " + perf.id() + " for unknown exercise by " + perf.userid() + " : " + perf.answer() + " " + skipped.size());
        }
        skipped.add(perf.id());
      } else {
        CommonExercise customOrPredefExercise = database.getCustomOrPredefExercise(projid, exid);
        if (customOrPredefExercise != null &&
            customOrPredefExercise.getProjectID() != DEFAULT_PROJECT) {
          exercises.add(customOrPredefExercise);
        }
      }
    });
    long now = System.currentTimeMillis();

    if (now - then > 30) {
      logger.info("getNativeAudio attachAudioToExercises (" + (now - then) +
          " millis) to exercises for " + exercises.size() + " (" + skipped.size() +
          " skipped) and project " + projid);
    }

    audioDAO.attachAudioToExercises(exercises, language, projid);
  }
}