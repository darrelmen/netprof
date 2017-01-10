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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.analysis.AnalysisPlot;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.analysis.UserPerformance;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.List;

@RemoteServiceRelativePath("analysis-manager")
public interface AnalysisService extends RemoteService {
  // Analysis support
  Collection<UserInfo> getUsersWithRecordings();

  /**
   * @param ids
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#setRawBestScores(List)
   */
  List<CommonShell> getShells(List<Integer> ids);

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.client.analysis.AnalysisPlot#getPerformanceForUser(LangTestDatabaseAsync, long, String, int)
   */
  UserPerformance getPerformanceForUser(int id, int minRecordings);

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.client.analysis.AnalysisTab#getWordScores(LangTestDatabaseAsync, ExerciseController, int, ShowTab, AnalysisPlot, Panel, int)
   */
  List<WordScore> getWordScores(int id, int minRecordings);

  /**
   * @param id
   * @param minRecordings
   * @return
   * @see mitll.langtest.client.analysis.AnalysisTab#getPhoneReport(LangTestDatabaseAsync, ExerciseController, int, Panel, AnalysisPlot, ShowTab, int)
   */
  PhoneReport getPhoneScores(int id, int minRecordings);


}
