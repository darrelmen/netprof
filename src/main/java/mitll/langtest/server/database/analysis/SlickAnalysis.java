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
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.analysis.*;
import mitll.npdata.dao.SlickPerfResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class SlickAnalysis extends Analysis implements IAnalysis {
  private static final Logger logger = LogManager.getLogger(SlickAnalysis.class);
  public static final int WARN_THRESH = 100;
  private SlickResultDAO resultDAO;
  private static final boolean DEBUG = true;

  /**
   * @param database
   * @param phoneDAO
   * @param exToRef
   * @see DatabaseImpl#configureProject
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPerformanceForUser(int, int)
   */
  public SlickAnalysis(Database database,
                       IPhoneDAO phoneDAO,
                       Map<Integer, String> exToRef,
                       SlickResultDAO resultDAO) {
    super(database, phoneDAO, exToRef);
    this.resultDAO = resultDAO;
  }

  /**
   * @param userid
   * @param projid
   * @param minRecordings
   * @return
   */
  @Override
  public UserPerformance getPerformanceForUser(long userid, int projid, int minRecordings) {
    Map<Integer, UserInfo> best = getBestForUser((int) userid, projid, minRecordings);
    return getUserPerformance(userid, best);
  }

  private Map<Integer, UserInfo> getBestForUser(int id, int projid, int minRecordings) {
    long then = System.currentTimeMillis();
    Collection<SlickPerfResult> perfForUser = resultDAO.getPerfForUser(id, projid);
    long now = System.currentTimeMillis();

    logger.info("getBestForUser best for " + id + " in " + projid + " were " + perfForUser.size());
    long diff = now - then;
    if (diff > WARN_THRESH) {
      logger.warn("getBestForUser best for " + id + " in " + projid + " took " + diff);
    }

    return getBest(perfForUser, minRecordings);
  }

  /**
   * @param id
   * @param projid
   * @param minRecordings
   * @return
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getWordScores
   */
  @Override
  public List<WordScore> getWordScoresForUser(long id, int projid, int minRecordings) {
    long then = System.currentTimeMillis();

    Map<Integer, UserInfo> best = getBestForUser((int) id, projid, minRecordings);
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > WARN_THRESH) {
      logger.warn("getWordScoresForUser for " + id + " in " + projid + " took " + diff);
    }
    return getWordScores(best);
  }

  /**
   * @param id
   * @param minRecordings
   * @param projid
   * @return
   * @see mitll.langtest.server.services.AnalysisServiceImpl#getPhoneScores(int, int)
   */
  @Override
  public PhoneReport getPhonesForUser(long id, int minRecordings, int projid) {
    Map<Integer, UserInfo> best = getBestForUser((int) id, projid, minRecordings);
    return getPhoneReport(id, best);
  }

  /**
   * TODO : how will this work for users on multiple projects?
   *
   * @param userDAO
   * @param minRecordings
   * @param projid
   * @return
   */
  @Override
  public List<UserInfo> getUserInfo(IUserDAO userDAO, int minRecordings, int projid) {
    Collection<SlickPerfResult> perfForUser = resultDAO.getPerf(projid);
    Map<Integer, UserInfo> best = getBest(perfForUser, minRecordings);
    return getUserInfos(userDAO, best);
  }

  private Map<Integer, UserInfo> getBest(Collection<SlickPerfResult> perfForUser, int minRecordings) {
    return getBestForQuery(minRecordings, getUserToResults(perfForUser));
  }

  /**
   * @return
   * @throws SQLException
   * @see #getBest
   */
  private Map<Integer, List<BestScore>> getUserToResults(Collection<SlickPerfResult> perfs) {
    Map<Integer, List<BestScore>> userToBest = new HashMap<>();

    int iPad = 0;
    int flashcard = 0;
    int learn = 0;
    int count = 0;
    int missing = 0;
    Set<String> missingAudio = new TreeSet<>();

    int emptyCount = 0;
    for (SlickPerfResult perf : perfs) {
      count++;
      int exid = perf.exid();
      Timestamp timestamp = perf.modified();
      float pronScore = perf.pronscore();
      int id = perf.id();
      int userid = perf.userid();
      String type = perf.audiotype();

      List<BestScore> results = userToBest.get(userid);
      if (results == null) userToBest.put(userid, results = new ArrayList<BestScore>());

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
      long time = timestamp.getTime();

      String nativeAudio = exToRef.get(exid);
      if (nativeAudio == null) {
//        if (exid.startsWith("Custom")) {
////          logger.debug("missing audio for " + exid);
//          missingAudio.add(exid);
//        }
        missing++;
      }
      BestScore e = new BestScore(exid, pronScore, time, id, json, isiPad, isFlashcard, trimPathForWebPage(path),
          nativeAudio);
      results.add(e);
    }

    if (DEBUG || true) {
      logger.info("getUserToResults total " + count + " missing audio " + missing +
          " iPad = " + iPad + " flashcard " + flashcard + " learn " + learn + " exToRef " + exToRef.size());
      if (!missingAudio.isEmpty()) logger.info("missing audio " + missingAudio);
      if (emptyCount > 0) logger.info("missing score json count " + emptyCount + "/" + count);
    }

    return userToBest;
  }
}
