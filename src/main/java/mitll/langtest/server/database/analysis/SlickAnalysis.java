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
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.SlickPerfResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.tools.cmd.gen.AnyVals;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public class SlickAnalysis extends Analysis implements IAnalysis {
  private static final Logger logger = LogManager.getLogger(SlickAnalysis.class);
  private static final int WARN_THRESH = 100;
  private static final String ANSWERS = "answers";
  private SlickResultDAO resultDAO;
  private String language;
  private int projid;
  private Project project;

  private static final boolean DEBUG = false;
  private IAudioDAO audioDAO;

  /**
   * @param database
   * @param phoneDAO
   * @param projid
   * @see ProjectServices#configureProject
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceForUser
   */
  public SlickAnalysis(Database database,
                       IPhoneDAO phoneDAO,
                       IAudioDAO audioDAO,
                       SlickResultDAO resultDAO,
                       String language,
                       int projid) {
    super(database, phoneDAO);
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
   * @see AnalysisServiceImpl#getPerformanceForUser
   */
/*  @Override
  public UserPerformance getPerformanceForUser(int userid, int minRecordings, int listid) {
    return getUserPerformance(userid, getBestForUser(userid, minRecordings, listid));
  }*/
  @Override
  public AnalysisReport getPerformanceReportForUser(int userid, int minRecordings, int listid) {
    Map<Integer, UserInfo> bestForUser = getBestForUser(userid, minRecordings, listid);

    long then = System.currentTimeMillis();
    AnalysisReport analysisReport = new AnalysisReport(
        getUserPerformance(userid, bestForUser),
        getWordScores(bestForUser),
        getPhoneReport(userid, bestForUser, language, project));

    long now = System.currentTimeMillis();
    logger.info("Return (took " + (now - then) +        ") analysis report");// + analysisReport);
    return analysisReport;
  }

  /**
   * Wasetful? why call getBestForUser again?
   *
   * @param userid
   * @param minRecordings
   * @param listid
   * @return
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getWordScores
   */
/*
  @Override
  public List<WordScore> getWordScoresForUser(int userid, int minRecordings, int listid) {
    long then = System.currentTimeMillis();

    Map<Integer, UserInfo> best = getBestForUser(userid, minRecordings, listid);
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > WARN_THRESH) {
      logger.warn("getWordScoresForUser for " + userid + " in " + projid + " took " + diff);
    }
    return getWordScores(best);
  }
*/

  /**
   * @param id
   * @param minRecordings
   * @param listid
   * @return
   * @seex IAnalysis#getPhonesForUser(int, int, int)
   * @seex IAnalysis#getWordScoresForUser(int, int, int)
   * @see Analysis#getPerformanceForUser(int, int, int)
   */
  private Map<Integer, UserInfo> getBestForUser(int id, int minRecordings, int listid) {
    long then = System.currentTimeMillis();
    Collection<SlickPerfResult> perfForUser = listid == -1 ?
        resultDAO.getPerfForUser(id, projid) :
        resultDAO.getPerfForUserOnList(id, listid);
    long now = System.currentTimeMillis();

    logger.info("getBestForUser best for user " + id + " in project " + projid + " and list " + listid + " were " + perfForUser.size());

    long diff = now - then;
    if (diff > WARN_THRESH) {
      logger.warn("getBestForUser best for " + id + " in " + projid + " took " + diff);
    }

    return getBest(perfForUser, minRecordings, true);
  }

  /**
   * @param id
   * @param minRecordings
   * @param listid
   * @return
   * @paramx projid
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPhoneScores
   * @see mitll.langtest.client.analysis.AnalysisTab#getPhoneReport
   */
/*  @Override
  public PhoneReport getPhonesForUser(int id, int minRecordings, int listid) {
    return getPhoneReport(id, getBestForUser(id, minRecordings, listid), language, project);
  }*/

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
    //Set<String> missingAudio = new TreeSet<>();
    Map<Integer, MiniUser.Gender> userToGender = new HashMap<>();
    // logger.info("getUserToResults for " + perfs.size() + " results");

    int emptyCount = 0;

    if (addNativeAudio) {
      getNativeAudio(perfs);
    }

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
        nativeAudio = database.getNativeAudio(userToGender, perf.userid(), exid, project);
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
          nativeAudio);
      results.add(e);
    }

    if (DEBUG || true) {
      long now = System.currentTimeMillis();

      logger.info("getUserToResults total " + count + " missing audio " + missing +
          " iPad = " + iPad + " flashcard " + flashcard + " learn " + learn + " took " + (now - then) + " millis");//+ " exToRef " + exToRef.size());
      //   if (!missingAudio.isEmpty()) logger.info("missing audio " + missingAudio);
      if (emptyCount > 0) logger.info("missing score json childCount " + emptyCount + "/" + count);
    }

    return userToBest;
  }

  private void getNativeAudio(Collection<SlickPerfResult> perfs) {
    List<CommonExercise> exercises = new ArrayList<>();

    logger.info("getNativeAudio getting exercises for " + perfs.size());

    perfs.forEach(perf -> exercises.add(database.getCustomOrPredefExercise(projid, perf.exid())));

    // Map<Integer, MiniUser.Gender> userToGender = new HashMap<>();
    logger.info("getNativeAudio attachAudioToExercises to exercises for " + exercises.size());

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
