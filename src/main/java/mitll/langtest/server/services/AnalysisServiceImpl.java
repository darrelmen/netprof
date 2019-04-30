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

package mitll.langtest.server.services;

import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.shared.WordsAndTotal;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.user.Permission;
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
   * @param projid - maybe they log out of another window while they do this request
   * @return
   * @see mitll.langtest.client.analysis.StudentAnalysis#StudentAnalysis
   */
  @Override
  public Collection<UserInfo> getUsersWithRecordings(int projid) throws DominoSessionException, RestrictedOperationException {
    long then = System.currentTimeMillis();
    boolean hasTeacherPerm = hasTeacherPerm();
    if (!hasTeacherPerm) {
      User userFromSession = getUserFromSession();
      if (userFromSession != null) {
        logger.info("getUsersWithRecordings for  : " + userFromSession.getID() + " " + userFromSession.getUserID() + " " + userFromSession.getUserKind() + " = " + hasTeacherPerm);
        db.getUserDAO().refreshCacheFor(userFromSession.getID());
      }
      hasTeacherPerm = hasTeacherPerm();
      logger.info("getUsersWithRecordings after refresh : " + hasTeacherPerm);
    }

    if (hasTeacherPerm) {
      //  int projid = getProjectIDFromUser();
      logger.info("getUsersWithRecordings for project # " + projid);
      List<UserInfo> userInfo = db
          .getAnalysis(projid)
          .getUserInfo(db.getUserDAO(), MIN_RECORDINGS, -1);
      long now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.info("getUsersWithRecordings took " + (now - then) + " millis to get " + userInfo.size() + " user infos.");
      }
      return userInfo;
    } else {
      throw getRestricted("getUsersWithRecordings : performance report");
    }
  }

  /**
   * @param dialogID
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.analysis.StudentScores#StudentScores(ExerciseController)
   */
  @Override
  public Collection<UserInfo> getUsersWithRecordingsForDialog(int dialogID)
      throws DominoSessionException, RestrictedOperationException {
    long then = System.currentTimeMillis();
    if (hasTeacherPerm()) {
      if (dialogID == -1) {
        // dialogID = getFirstDialogID(projectIDFromUser);
        new ArrayList<>();
      } else {
        int projectIDFromUser = getProjectIDFromUser();
        logger.info("getUsersWithRecordingsForDialog for project # " + projectIDFromUser + " for dialog " + dialogID);

        List<UserInfo> userInfo = db
            .getAnalysis(projectIDFromUser)
            .getUserInfoForDialog(db.getUserDAO(), dialogID);
        long now = System.currentTimeMillis();
        if (now - then > 100) {
          logger.info("took " + (now - then) + " millis to get " + userInfo.size() + " user infos.");
        }
        return userInfo;
      }
    } else {
      throw getRestricted("getUsersWithRecordings : performance report");
    }
    return new ArrayList<>();
  }

//  private int getFirstDialogID(int projectIDFromUser) {
//    int dialogID = -1;
//    List<IDialog> dialogs = getDialogsForProject(projectIDFromUser);
//    if (dialogs != null && !dialogs.isEmpty()) {
//      dialogID = dialogs.get(0).getID();
//    }
//    logger.info("\tgetUsersWithRecordingsForDialog for project # " + projectIDFromUser + " (" +
//        dialogs.size() +
//        ") for dialog " + dialogID);
//    return dialogID;
//  }

  /**
   * @param analysisRequest
   * @return
   * @paramx userid
   * @see mitll.langtest.client.analysis.AnalysisTab#AnalysisTab
   */
  @Override
  public AnalysisReport getPerformanceReportForUser(AnalysisRequest analysisRequest)
      throws DominoSessionException, RestrictedOperationException {
    // logger.info("getPerformanceForUser " +userid+ " list " + listid + " min " + minRecordings);
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser(userIDFromSessionOrDB);
    if (projectID == -1) {
      return new AnalysisReport();
    } else {
      logger.info("getPerformanceForUser " + analysisRequest.getUserid() + " vs session user " + userIDFromSessionOrDB);

      if (hasTeacherPermOrSelf(analysisRequest.getUserid(), userIDFromSessionOrDB)) {
        long then = System.currentTimeMillis();
        AnalysisReport performanceReportForUser = getSlickAnalysis(projectID)
            .getPerformanceReportForUser(analysisRequest);
        long now = System.currentTimeMillis();

        long serverTime = now - then;
        performanceReportForUser.setServerTime(serverTime);
        logger.info("getPerformanceReportForUser : " + performanceReportForUser + "\n\ttook " + serverTime + " millis");
        return performanceReportForUser;
      } else {
        logger.info("getPerformanceForUser " + analysisRequest.getUserid() +
            "(" + getUserID(analysisRequest.getUserid()) +
            ")" +
            " vs session user " + userIDFromSessionOrDB + "(" +
            getUserID(userIDFromSessionOrDB) +
            ")");

        throw getRestricted("performance report");
      }
    }
  }

  private String getUserID(int userid) {
    return db.getUserDAO().getUserWhere(userid).getUserID();
  }

/*
  public PhoneSummary getPhoneSummary(int userid, int minRecordings, int listid, int req)
      throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser(userIDFromSessionOrDB);
    if (projectID == -1) {
      return new PhoneSummary();
    } else {
      if (hasTeacherPermOrSelf(userid, userIDFromSessionOrDB)) {
        long then = System.currentTimeMillis();
        PhoneSummary phoneSummary = getSlickAnalysis(projectID).getPhoneSummary(userid, minRecordings, listid);
        long now = System.currentTimeMillis();

        long serverTime = now - then;
        phoneSummary.setServerTime(serverTime);
        logger.info("getPerformanceReportForUser : " + phoneSummary + "\n\ttook " + serverTime + " millis");
        return phoneSummary;
      } else {
        throw getRestricted("phone summary report");
      }
    }
  }
*/

  /**
   * So if you're asking about performance data about yourself you are allowed.
   * If you're a teacher, you can ask about everyone.
   *
   * @param userid
   * @param sessionUser
   * @return
   * @throws DominoSessionException
   */
  private boolean hasTeacherPermOrSelf(int userid, int sessionUser) throws DominoSessionException {
    return (userid == sessionUser) || hasTeacherPerm();
  }

  private boolean hasTeacherPerm() throws DominoSessionException {
    User userFromSession = getUserFromSession();
    if (userFromSession == null) {
      logger.error("no user in session?");
      throw new DominoSessionException();
    }
    Collection<Permission> permissions = userFromSession.getPermissions();
    return
        permissions.contains(Permission.TEACHER_PERM) ||
            permissions.contains(Permission.PROJECT_ADMIN);
  }

  /**
   * @param analysisRequest
   * @param rangeStart
   * @param rangeEnd
   * @param sort
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.analysis.WordContainerAsync#createProvider
   */
  @Override
  public WordsAndTotal getWordScoresForUser(AnalysisRequest analysisRequest, int rangeStart, int rangeEnd, String sort)
      throws DominoSessionException {
    long then = System.currentTimeMillis();

    WordsAndTotal wordScoresForUser = getSlickAnalysis(getProjectIDFromUser())
        .getWordScoresForUser(analysisRequest, rangeStart, rangeEnd, sort);

    wordScoresForUser.setReq(analysisRequest.getReqid());

    long now = System.currentTimeMillis();
    wordScoresForUser.setServerTime((now - then));
    return wordScoresForUser;
  }

  /**
   * @param analysisRequest
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.analysis.BigramContainer#clickOnPhone2
   */
  @Override
  public List<WordAndScore> getPerformanceReportForUserForPhone(AnalysisRequest analysisRequest)
      throws DominoSessionException, RestrictedOperationException {
//    logger.info("getPerformanceReportForUserForPhone " + analysisRequest);

    long then = System.currentTimeMillis();
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser(userIDFromSessionOrDB);
    if (projectID == -1) {
      return new ArrayList<>();
    } else {
      if (hasTeacherPermOrSelf(analysisRequest.getUserid(), userIDFromSessionOrDB)) {

        long now = System.currentTimeMillis();

        long serverTime = now - then;

        List<WordAndScore> phoneReportFor =
            getSlickAnalysis(projectID).getWordAndScoreForPhoneAndBigram(analysisRequest);

        return phoneReportFor;
      } else {
        throw getRestricted("performance report for phone");
      }
    }
  }

  /**
   * @param analysisRequest@return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   */
/*  @Override
  public List<Bigram> getPerformanceReportForUserForPhoneBigrams(AnalysisRequest analysisRequest)
      throws DominoSessionException, RestrictedOperationException {
    logger.info("getPerformanceForUser " + analysisRequest.getUserid() + " list " + analysisRequest.getListid() + " phone " + analysisRequest.getPhone());
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser();
    if (projectID == -1) {  // how can this happen??
      return new ArrayList<>();
    } else {
      if (hasTeacherPermOrSelf(analysisRequest.getUserid(), userIDFromSessionOrDB)) {
        return getSlickAnalysis(projectID).getBigramPhoneReportFor(analysisRequest);
      } else {
        throw getRestricted("performance report for phone bigrams");
      }
    }
  }*/

/*  @Override
  public PhoneReport getPhoneSummary(int userid, int listid, long from, long to, int reqid)
      throws DominoSessionException, RestrictedOperationException {
    logger.info("getPerformanceForUser " + userid + " list " + listid);
    long then = System.currentTimeMillis();
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser();
    if (projectID == -1) {
      return new PhoneReport();
    } else {
      if (hasTeacherPermOrSelf(userid, userIDFromSessionOrDB)) {
        PhoneReport phoneReport =
            getSlickAnalysis(projectID).getPhoneReportForPeriod(userid, listid, from, to).setReqid(reqid);

        long now = System.currentTimeMillis();

        long serverTime = now - then;

        phoneReport.setServerTime(serverTime);

        return phoneReport;
      } else {
        throw getRestricted("performance report for phone");
      }
    }
  }*/
  public PhoneSummary getPhoneSummary(AnalysisRequest analysisRequest)
      throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser(userIDFromSessionOrDB);
    if (projectID == -1) {
      return new PhoneSummary();
    } else {
      if (hasTeacherPermOrSelf(analysisRequest.getUserid(), userIDFromSessionOrDB)) {
        long then = System.currentTimeMillis();
        PhoneSummary phoneSummary = getSlickAnalysis(projectID).getPhoneSummaryForPeriod(analysisRequest).setReqid(analysisRequest.getReqid());
        long now = System.currentTimeMillis();

        long serverTime = now - then;
        phoneSummary.setServerTime(serverTime);
        logger.info("getPhoneSummary : " + phoneSummary + "\n\ttook " + serverTime + " millis");
        return phoneSummary;
      } else {
        throw getRestricted("phone summary report");
      }
    }
  }

  public PhoneBigrams getPhoneBigrams(AnalysisRequest analysisRequest)
      throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    int projectID = getProjectIDFromUser(userIDFromSessionOrDB);
    if (projectID == -1) {
      return new PhoneBigrams();
    } else {
      if (hasTeacherPermOrSelf(analysisRequest.getUserid(), userIDFromSessionOrDB)) {
        long then = System.currentTimeMillis();
        PhoneBigrams phoneBigrams = getSlickAnalysis(projectID).getPhoneBigramsForPeriod(analysisRequest).setReqid(analysisRequest.getReqid());
        long now = System.currentTimeMillis();

        long serverTime = now - then;
        phoneBigrams.setServerTime(serverTime);
        logger.info("getPhoneBigrams : " + phoneBigrams + "\n\ttook " + serverTime + " millis");
        return phoneBigrams;
      } else {
        throw getRestricted("phone bigrams report");
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
        project.getLanguageEnum(),
        projectID,
        project.getKind() == ProjectType.POLYGLOT
    );
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