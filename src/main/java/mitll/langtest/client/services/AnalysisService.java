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

package mitll.langtest.client.services;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.client.analysis.ReqCounter;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.WordsAndTotal;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.List;

@RemoteServiceRelativePath("analysis-manager")
public interface AnalysisService extends RemoteService {
  /**
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.analysis.StudentAnalysis#StudentAnalysis
   */
  Collection<UserInfo> getUsersWithRecordings() throws DominoSessionException;

  Collection<UserInfo> getUsersWithRecordingsForDialog(int dialogID) throws DominoSessionException;

  /**
   * TODO : why do we have to do this at all???
   *
   * @param ids
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores
   */
  List<CommonShell> getShells(Collection<Integer> ids) throws DominoSessionException;

  /**
   * @param analysisRequest
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.analysis.AnalysisTab#AnalysisTab(ExerciseController, boolean, int, ReqCounter, mitll.langtest.client.custom.INavigation.VIEWS)
   */
  AnalysisReport getPerformanceReportForUser(AnalysisRequest analysisRequest) throws DominoSessionException, RestrictedOperationException;


  /**
   * @param analysisRequest
   * @param rangeStart
   * @param rangeEnd
   * @param sort
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.analysis.WordContainerAsync#createProvider(int, CellTable)
   */
  WordsAndTotal getWordScoresForUser(AnalysisRequest analysisRequest, int rangeStart, int rangeEnd, String sort) throws DominoSessionException;

  /**
   *
   * @param analysisRequest
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   */
  // List<Bigram> getPerformanceReportForUserForPhoneBigrams(AnalysisRequest analysisRequest) throws DominoSessionException, RestrictedOperationException;

  /**
   * @param analysisRequest
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.analysis.PhoneContainer#timeChanged
   */
  PhoneSummary getPhoneSummary(AnalysisRequest analysisRequest) throws DominoSessionException, RestrictedOperationException;

  /**
   * @param analysisRequest
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.analysis.PhoneContainer#clickOnPhone2
   */
  PhoneBigrams getPhoneBigrams(AnalysisRequest analysisRequest) throws DominoSessionException, RestrictedOperationException;

  /**
   * @param analysisRequest
   * @return
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.analysis.BigramContainer#clickOnPhone2
   */
  List<WordAndScore> getPerformanceReportForUserForPhone(AnalysisRequest analysisRequest) throws DominoSessionException, RestrictedOperationException;
}
