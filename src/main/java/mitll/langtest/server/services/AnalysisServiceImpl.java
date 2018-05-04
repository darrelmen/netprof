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

package mitll.langtest.server.services;

import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.shared.WordsAndTotal;
import mitll.langtest.shared.analysis.AnalysisReport;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("serial")
public class AnalysisServiceImpl extends MyRemoteServiceServlet implements AnalysisService {
  private static final Logger logger = LogManager.getLogger(AnalysisServiceImpl.class);
  public static final int MIN_RECORDINGS = 1;

  /**
   * @param ids
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores
   */
  @Override
  public List<CommonShell> getShells(Collection<Integer> ids) throws DominoSessionException {
    List<CommonShell> shells = new ArrayList<>();
    int projectID = getProjectIDFromUser();
    logger.info("getShells project id from user " + projectID + " ids " + ids.size());
    for (Integer id : ids) {
      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(projectID, id);
      if (customOrPredefExercise == null) {
        logger.warn("Couldn't find exercise for " + id);
      } else {
        shells.add(customOrPredefExercise.getShell());
      }
    }
    return shells;
  }

  /**
   * @return
   * @see mitll.langtest.client.analysis.StudentAnalysis#StudentAnalysis
   */
  @Override
  public Collection<UserInfo> getUsersWithRecordings() throws DominoSessionException, RestrictedOperationException {
    long then = System.currentTimeMillis();
    if (hasTeacherPermOrSelf(-1)) {
      int projectIDFromUser = getProjectIDFromUser();
      logger.info("getUsersWithRecordings for project # " + projectIDFromUser);
      List<UserInfo> userInfo = db
          .getAnalysis(projectIDFromUser)
          .getUserInfo(db.getUserDAO(), MIN_RECORDINGS);
      long now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.info("took " + (now - then) + " millis to get " + userInfo.size() + " user infos.");
      }
      return userInfo;
    } else {
      throw getRestricted("getUsersWithRecordings : performance report");
    }
  }

  /**
   * @param userid
   * @param minRecordings
   * @param listid
   * @param req
   * @return
   * @see mitll.langtest.client.analysis.AnalysisTab#AnalysisTab
   */
  @Override
  public AnalysisReport getPerformanceReportForUser(int userid, int minRecordings, int listid, int req)
      throws DominoSessionException, RestrictedOperationException {
    // logger.info("getPerformanceForUser " +userid+ " list " + listid + " min " + minRecordings);
    int projectID = getProjectIDFromUser();
    if (projectID == -1) {
      return new AnalysisReport();
    } else {
      if (hasTeacherPermOrSelf(userid)) {
        long then = System.currentTimeMillis();
        AnalysisReport performanceReportForUser = getSlickAnalysis(projectID)
            .getPerformanceReportForUser(userid, minRecordings, listid, req);
        long now = System.currentTimeMillis();

        logger.info("getPerformanceReportForUser : " + performanceReportForUser + " took " + (now - then) + " millis");
        return performanceReportForUser;
      } else {
        throw getRestricted("performance report");
      }
    }
  }

  private boolean hasTeacherPermOrSelf(int userid) throws DominoSessionException {
    User userFromSession = getUserFromSession();
    if (userFromSession == null) {
      logger.error("no user in session?");
      throw new DominoSessionException();
    }
    Collection<User.Permission> permissions = userFromSession.getPermissions();
    return
        userFromSession.getID() == userid || // self
            permissions.contains(User.Permission.TEACHER_PERM) ||
            permissions.contains(User.Permission.PROJECT_ADMIN);
  }

  /**
   * @param userid
   * @param minRecordings
   * @param listid
   * @param fromTime
   * @param toTime
   * @param rangeStart
   * @param rangeEnd
   * @param sort
   * @param reqid
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.analysis.WordContainerAsync#createProvider
   */
  @Override
  public WordsAndTotal getWordScoresForUser(int userid, int minRecordings, int listid,
                                            long fromTime, long toTime,
                                            int rangeStart, int rangeEnd, String sort, int reqid) throws DominoSessionException {
    int projectID = getProjectIDFromUser();

//    WordsAndTotal wordsAndTotal = new WordsAndTotal();
    WordsAndTotal wordScoresForUser = getSlickAnalysis(projectID)
        .getWordScoresForUser(userid, minRecordings, listid, fromTime, toTime, rangeStart, rangeEnd, sort);

    wordScoresForUser.setReq(reqid);

    return wordScoresForUser;
    //return new WordsAndTotal(wordScoresForUser, reqid);
  }

  /**
   * @param userid
   * @param listid
   * @param phone
   * @param from
   * @param to
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.analysis.PhoneContainer#clickOnPhone2
   */
  @Override
  public List<WordAndScore> getPerformanceReportForUserForPhone(int userid, int listid, String phone,
                                                                long from, long to)
      throws DominoSessionException, RestrictedOperationException {
    logger.info("getPerformanceForUser " + userid + " list " + listid + " phone " + phone);
    int projectID = getProjectIDFromUser();
    if (projectID == -1) {
      return new ArrayList<>();
    } else {
      if (hasTeacherPermOrSelf(userid)) {
        return getSlickAnalysis(projectID)
            .getPhoneReportFor(userid, listid, phone, from, to);
      } else {
        throw getRestricted("performance report for phone");
      }
    }
  }

  @Override
  public PhoneReport getPhoneReport(int userid, int listid, long from, long to, int reqid)
      throws DominoSessionException, RestrictedOperationException {
    logger.info("getPerformanceForUser " + userid + " list " + listid);
    int projectID = getProjectIDFromUser();
    if (projectID == -1) {
      return new PhoneReport();
    } else {
      if (hasTeacherPermOrSelf(userid)) {
        return getSlickAnalysis(projectID).getPhoneReportForPeriod(userid, listid, from, to).setReqid(reqid);
      } else {
        throw getRestricted("performance report for phone");
      }
    }
  }

  @NotNull
  private SlickAnalysis getSlickAnalysis(int projectID) {
    Project project = db.getProject(projectID);
    return new SlickAnalysis(
        db.getDatabase(),
        db.getPhoneDAO(),
        db.getAudioDAO(),
        (SlickResultDAO) db.getResultDAO(),
        project.getLanguage(),
        projectID,
        project.getKind() == ProjectType.POLYGLOT);
  }

  /**
   * @param id
   * @param minRecordings
   * @param listid
   * @return
   * @see mitll.langtest.client.analysis.AnalysisTab#getWordScores
   */
/*  @Override
  public List<WordScore> getWordScores(int id, int minRecordings, int listid) {
    int projectID = getProjectIDFromUser();
    if (projectID == -1) return new ArrayList<>();

    List<WordScore> wordScoresForUser = db.getAnalysis(projectID).getWordScoresForUser(id, minRecordings, listid);

    *//*
    logger.info("getWordScores for" +
        "\n\tuser       " +id +
        "\n\tproject is "+ projectID + " and " + analysis +
        "\n\tyielded " + wordScoresForUser.size());*//*

//    for (WordScore ws : wordScoresForUser) if (ws.getNativeAudio() != null) logger.info("got " +ws.getID() + " " + ws.getNativeAudio());
    return wordScoresForUser;
  }*/

/*  @Override
  public PhoneReport getPhoneScores(int id, int minRecordings, int listid) {
    int projectID = getProjectIDFromUser();
    if (projectID == -1) return new PhoneReport();
    return db.getAnalysis(projectID).getPhonesForUser(id, minRecordings, listid);
  }*/
}