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
import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("serial")
public class AnalysisServiceImpl extends MyRemoteServiceServlet implements AnalysisService {
  private static final Logger logger = LogManager.getLogger(AnalysisServiceImpl.class);
  private static final int MIN_RECORDINGS = 5;

/*  @Override
  public void init() {
    findSharedDatabase();
    readProperties(getServletContext());
  }*/

  /**
   * @param ids
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores(List)
   */
  @Override
  public List<CommonShell> getShells(List<Integer> ids) {
    List<CommonShell> shells = new ArrayList<>();
    int projectID = getProjectID();

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
  public Collection<UserInfo> getUsersWithRecordings() {
    int projectID = getProjectID();
    return db.getAnalysis(projectID).getUserInfo(db.getUserDAO(), MIN_RECORDINGS, projectID);
  }

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#AnalysisPlot
   */
  @Override
  public UserPerformance getPerformanceForUser(int id, int minRecordings) {
    logger.info("getPerformanceForUser " +id);
    SlickAnalysis slickAnalysis =
        new SlickAnalysis(db,
            db.getPhoneDAO(),
            db.getExerciseIDToRefAudio(getProjectID()),
            (SlickResultDAO) db.getResultDAO());
    return slickAnalysis.getPerformanceForUser(id, getProjectID(), minRecordings);
  }

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.client.analysis.AnalysisTab#getWordScores
   */
  @Override
  public List<WordScore> getWordScores(int id, int minRecordings) {
    int projectID = getProjectID();
    IAnalysis analysis = db.getAnalysis(projectID);
    logger.info("for user " +id + " project is "+ projectID + " and " + analysis);
    List<WordScore> wordScoresForUser = analysis.getWordScoresForUser(id, projectID, minRecordings);
//    for (WordScore ws : wordScoresForUser) if (ws.getNativeAudio() != null) logger.info("got " +ws.getID() + " " + ws.getNativeAudio());
    return wordScoresForUser;
  }

  @Override
  public PhoneReport getPhoneScores(int id, int minRecordings) {
    int projectID = getProjectID();
    return db.getAnalysis(projectID).getPhonesForUser(id, minRecordings, projectID);
  }
}