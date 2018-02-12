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
import mitll.langtest.server.database.audio.IAudioDAO;
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
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.SlickPerfResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.shared.analysis.WordScore.*;

public class SlickAnalysis extends Analysis implements IAnalysis {
  private static final Logger logger = LogManager.getLogger(SlickAnalysis.class);
  private static final int WARN_THRESH = 100;
  private static final String ANSWERS = "answers";
  private static final int MAX_TO_SEND = 25;
  public static final int DEFAULT_PROJECT = 1;
  public static final int UNKNOWN_EXERCISE = 2;
  private final SlickResultDAO resultDAO;
  private final String language;
  private final int projid;
  private final Project project;

  private static final boolean DEBUG = false;
  private final IAudioDAO audioDAO;

  /**
   * @param database
   * @param phoneDAO
   * @param projid
   * @see ProjectServices#configureProject
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceReportForUser(int, int, int)
   */
  public SlickAnalysis(Database database,
                       IPhoneDAO phoneDAO,
                       IAudioDAO audioDAO,
                       SlickResultDAO resultDAO,
                       String language,
                       int projid) {
    super(database, phoneDAO, language);
    this.resultDAO = resultDAO;
    this.language = language;
    this.projid = projid;
    this.audioDAO = audioDAO;
    project = database.getProject(projid);
  }

  /**
   * @param userid
   * @param minRecordings
   * @param listid
   * @return
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceReportForUser(int, int, int)
   */
  @Override
  public AnalysisReport getPerformanceReportForUser(int userid, int minRecordings, int listid) {
    Map<Integer, UserInfo> bestForUser = getBestForUser(userid, minRecordings, listid);

    Collection<UserInfo> userInfos = bestForUser.values();
    UserInfo next = bestForUser.isEmpty() ? null : userInfos.iterator().next();

    long then = System.currentTimeMillis();
    AnalysisReport analysisReport = new AnalysisReport(
        getUserPerformance(userid, bestForUser),
        //  getWordScores(bestForUser.values()),
        getPhoneReport(userid, next, project),
        getCount(userInfos));

    long now = System.currentTimeMillis();
    logger.info("Return (took " + (now - then) + ") analysis report for " + userid + " and list " + listid);// + analysisReport);
    return analysisReport;
  }

  /**
   * @param userid
   * @param minRecordings
   * @param listid
   * @param from
   * @param to
   * @param rangeStart
   * @param rangeEnd
   * @param sort
   * @return
   * @see AnalysisServiceImpl#getWordScoresForUser(int, int, int, long, long, int, int, String, int)
   */
  @Override
  public WordsAndTotal getWordScoresForUser(int userid, int minRecordings, int listid,
                                            long from, long to,
                                            int rangeStart, int rangeEnd,
                                            String sort) {
    Map<Integer, UserInfo> bestForUser = getBestForUser(userid, minRecordings, listid);

    Collection<UserInfo> userInfos = bestForUser.values();
    logger.info("getWordScoresForUser for user " + userid + " got " + userInfos.size() + " from " + rangeStart + " to " + rangeEnd + " sort " + sort);
    return getWordScoresForPeriod(userInfos, from, to, rangeStart, rangeEnd, sort);
  }

  /**
   * @param userInfos
   * @param from
   * @param to
   * @param rangeStart
   * @param rangeEnd
   * @param sortInfo
   * @return
   * @see #getWordScoresForUser(int, int, int, long, long, int, int, String)
   */
  private WordsAndTotal getWordScoresForPeriod(Collection<UserInfo> userInfos,
                                               long from, long to,
                                               int rangeStart, int rangeEnd,

                                               String sortInfo) {
    if (userInfos.isEmpty()) {
      //logger.warn("no best values for " + id);
      return new WordsAndTotal(Collections.emptyList(), 0);
    } else {
      List<BestScore> resultsForQuery = userInfos.iterator().next().getBestScores();

      logger.info("getWordScoresForUser got " + resultsForQuery.size() + " scores");

      List<BestScore> inTime =
          resultsForQuery
              .stream()
              .filter(bestScore -> from <= bestScore.getTimestamp() && bestScore.getTimestamp() <= to)
              .collect(Collectors.toList());
      logger.info("getWordScoresForUser got " + inTime.size() + " scores from " + new Date(from) + " to " + new Date(to));

      //if (DEBUG) logger.warn("getWordScoresForUser " + resultsForQuery.size());

      inTime.sort(getComparator(project, Arrays.asList(sortInfo.split(",")), inTime));

      List<WordScore> wordScore = getWordScore(inTime, false);
      int totalSize = wordScore.size();
      //logger.info("getWordScoresForUser got " + totalSize + " word and score ");

      // sublist is not serializable!
      int min = Math.min(wordScore.size(), rangeEnd);

      // prevent sublist range error
      int startToUse = min < rangeStart ? 0 : rangeStart;


      wordScore = new ArrayList<>(wordScore.subList(startToUse, min));
      if (DEBUG) {
        logger.warn("getWordScoresForUser wordScore " + totalSize);
      }
      logger.warn("getWordScoresForUser wordScore " + totalSize + " vs " + wordScore.size() + "/" + min);


      return new WordsAndTotal(wordScore, totalSize);
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
        inTime.forEach(bestScore -> {
          CommonExercise exerciseByID = project.getExerciseByID(bestScore.getExId());
          if (exerciseByID == null) {
            String transcriptFromJSON = getTranscriptFromJSON(bestScore);

            logger.info("getComparator no ex for " + bestScore.getExId() + " so " + transcriptFromJSON);

            scoreToFL.put(bestScore, transcriptFromJSON);
          } else {
            scoreToFL.put(bestScore, exerciseByID.getForeignLanguage());
          }
        });
      }

      return new Comparator<BestScore>() {
        @Override
        public int compare(BestScore o1, BestScore o2) {
          // text
          int comp = 0;
          switch (field) {
            case WORD:

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
              if (comp == 0) {
                comp = Long.compare(o1.getTimestamp(), o2.getTimestamp());
              }
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

  @NotNull
  private String getTranscriptFromJSON(BestScore bestScore) {
    Map<NetPronImageType, List<SlimSegment>> netPronImageTypeListMap =
        parseResultJson.slimReadFromJSON(bestScore.getJson());
    StringBuilder builder = new StringBuilder();
    List<SlimSegment> slimSegments = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT);
    slimSegments.forEach(slimSegment -> builder.append(slimSegment.getEvent()).append(" "));
    return builder.toString().trim();
  }

  public List<WordAndScore> getPhoneReportFor(int userid, int listid, String phone, long from, long to) {
    Map<Integer, UserInfo> bestForUser = getBestForUser(userid, 0, listid);
    UserInfo next = bestForUser.isEmpty() ? null : bestForUser.values().iterator().next();

    long then = System.currentTimeMillis();


    if (DEBUG) {
      logger.info("getPhoneReportFor for" +
          "\n\tuser   " + next +
          "\n\tuserid " + userid +
          "\n\tlistid " + listid +
          "\n\tphone  " + phone +
          "\n\tfrom   " + from + " " + new Date(from) +
          "\n\tto     " + to + " " + new Date(to)
      );
    }

    PhoneReport phoneReportForPhone = getPhoneReportForPhone(userid, next, project, phone, from, to);

    List<WordAndScore> wordAndScores = phoneReportForPhone.getPhoneToWordAndScoreSorted().get(phone);

    if (wordAndScores == null) {
      logger.error("huh? no scores for " + phone);
      return new ArrayList<>();
    } else {

      if (DEBUG) logger.info("getPhoneReportFor for " + phone + " got word num = " + wordAndScores.size());

      SortedSet<WordAndScore> examples = new TreeSet<>(wordAndScores);
      // examples.addAll(wordAndScores);
      List<WordAndScore> filteredWords = new ArrayList<>(examples);

      filteredWords = new ArrayList<>(filteredWords.subList(0, Math.min(filteredWords.size(), MAX_TO_SEND)));

      long now = System.currentTimeMillis();
      logger.info("getPhoneReportFor (took " + (now - then) + ") " +
          "to get " + wordAndScores.size() + " " + filteredWords.size() +
          "  report for " + userid + " and list " + listid);// + analysisReport);
      return filteredWords;
    }
  }

  /**
   * @param id
   * @param minRecordings
   * @param listid
   * @return
   * @see #getPerformanceReportForUser(int, int, int)
   * @see #getPhoneReportFor(int, int, String, long, long)
   */
  private Map<Integer, UserInfo> getBestForUser(int id, int minRecordings, int listid) {
    long then = System.currentTimeMillis();
    Collection<SlickPerfResult> perfForUser = listid == -1 ?
        resultDAO.getPerfForUser(id, projid) :
        resultDAO.getPerfForUserOnList(id, listid);
    long now = System.currentTimeMillis();

    logger.info("getBestForUser best for user " + id + " in project " + projid + " and list " + listid +
        " were " + perfForUser.size());

    long diff = now - then;
    if (diff > WARN_THRESH) {
      logger.warn("getBestForUser best for " + id + " in " + projid + " took " + diff);
    }

    return getBest(perfForUser, minRecordings, true);
  }

  /**
   * For the current project id.
   *
   * @param userDAO
   * @param minRecordings
   * @return
   * @see AnalysisServiceImpl#getUsersWithRecordings
   */
  @Override
  public List<UserInfo> getUserInfo(IUserDAO userDAO, int minRecordings) {
    long then = System.currentTimeMillis();
    Collection<SlickPerfResult> perfForUser = resultDAO.getPerf(projid, database.getServerProps().getMinAnalysisScore());
    long now = System.currentTimeMillis();
    if (now - then > 100)
      logger.info("getUserInfo took " + (now - then) + " to get " + perfForUser.size() + " perf infos for project #" + projid);

    then = now;
    Map<Integer, UserInfo> best = getBest(perfForUser, minRecordings, false);
    now = System.currentTimeMillis();
    if (now - then > 100)
      logger.info("getUserInfo took " + (now - then) + " to get best for " + perfForUser.size() + " for project #" + projid);

    then = now;
    List<UserInfo> userInfos = getUserInfos(userDAO, best);
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
  private Map<Integer, UserInfo> getBest(Collection<SlickPerfResult> perfForUser, int minRecordings, boolean addNativeAudio) {
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
      logger.info("getUserToResults took " + (System.currentTimeMillis() - then) + " millis to get native audio for " + perfs.size());
    }

    then = System.currentTimeMillis();

    Map<Integer, MiniUser> idToMini = new HashMap<>();

    Map<String, Long> sessionToLong = new HashMap<>();

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
      String device = perf.devicetype();
      Long sessionTime = getSessionTime(sessionToLong, perf.device());
      String path = perf.answer();

      boolean isiPad = device != null && device.startsWith("i");
      if (isiPad) iPad++;
      boolean isFlashcard = !isiPad && (type.startsWith("avp") || type.startsWith("flashcard"));
      if (!isiPad) {
        if (isFlashcard) flashcard++;
        else learn++;
      }

      String nativeAudio = null;
      if (addNativeAudio) {
        nativeAudio = database.getNativeAudio(userToGender, perf.userid(), exid, project, idToMini);
        if (nativeAudio == null) {
//        if (exid.startsWith("Custom")) {
////          logger.debug("missing audio for " + exid);
//          missingAudio.add(exid);
//        }
          missing++;
        }
      }

      boolean isLegacy = path.startsWith(ANSWERS);
      String filePath = isLegacy ?
          getRelPrefix(language) + path :
          trimPathForWebPage(path);

      //   logger.info("isLegacy " + isLegacy + " " + path + " : " + filePath);

      BestScore e = new BestScore(exid, pronScore, time, id, json, isiPad, isFlashcard,
          filePath,
          nativeAudio,
          sessionTime);

//      if (e.getSessionStart()> 0) {
//        logger.info("id " + id + " = " + e.getSessionStart());
//      }

      results.add(e);
    }

    if (DEBUG || true) {
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

  private Long getSessionTime(Map<String, Long> sessionToLong, String device) {
    Long parsedTime = sessionToLong.get(device);

    if (parsedTime == null) {
      try {
        parsedTime = Long.parseLong(device);
//        logger.info("getSessionTime " + parsedTime);
      } catch (NumberFormatException e) {
  //      logger.info("can't parse " + device);
        parsedTime = -1L;
      }
      sessionToLong.put(device, parsedTime);
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
    List<CommonExercise> exercises = new ArrayList<>();

    logger.info("getNativeAudio getting exercises for " + perfs.size() + " recordings in project " + projid);

    List<Integer> skipped = new ArrayList<>();

    perfs.forEach(perf -> {
      int exid = perf.exid();

      if (exid == UNKNOWN_EXERCISE) {
        logger.info("getNativeAudio skipping " + perf.id() + " for unknonw exercise by " + perf.userid() + " : " + perf.answer());
        skipped.add(perf.id());
      } else {
        CommonExercise customOrPredefExercise = database.getCustomOrPredefExercise(projid, exid);
        if (customOrPredefExercise != null &&
            customOrPredefExercise.getProjectID() != DEFAULT_PROJECT) {
          exercises.add(customOrPredefExercise);
        }
      }
    });

    logger.info("getNativeAudio attachAudioToExercises to exercises for " + exercises.size() + " (" + skipped.size() +
        " skipped) and project " + projid);

    audioDAO.attachAudioToExercises(exercises, language);
  }

  /**
   * Fix the path -  on hydra it's at:
   * <p>
   * /opt/netprof/answers/english/answers/plan/1039/1/subject-130
   * <p>
   * rel path:
   * <p>
   * answers/english/answers/plan/1039/1/subject-130
   *
   * @param language
   * @return
   */
  private String getRelPrefix(String language) {
    String installPath = database.getServerProps().getAnswerDir();

    String s = language.toLowerCase();
    String prefix = installPath + File.separator + s;
    int netProfDurLength = database.getServerProps().getAudioBaseDir().length();

    return prefix.substring(netProfDurLength) + File.separator;
  }
}
