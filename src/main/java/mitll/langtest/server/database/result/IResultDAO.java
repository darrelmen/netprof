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

package mitll.langtest.server.database.result;

import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.ReportStats;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.ImageOptions;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IResultDAO extends IDAO {
  /**
   * @see mitll.langtest.server.database.Report#getEarliest
   * @param projid
   * @return
   */
  long getFirstTime(int projid);

  /**
   * Just for export
   * @return
   */
  List<Result> getResults();

  /**
   * @see mitll.langtest.server.database.Report#getReportForProject(ReportStats, StringBuilder, boolean)
   * @param projid
   * @return
   */
  Collection<MonitorResult> getResultsDevices(int projid);

  /**
   * @see mitll.langtest.server.services.ScoringServiceImpl#getResultASRInfo
   * @param id
   * @return
   */
  Result getResultByID(int id);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getMonitorResults
   * @param projid
   * @return
   */
  List<MonitorResult> getMonitorResults(int projid);

  List<MonitorResult> getMonitorResultsByID(int id);

  Collection<UserAndTime> getUserAndTimes();

  UserToCount getUserToNumAnswers();

  SessionsAndScores getSessionsForUserIn2(Collection<Integer> ids,
                                          int latestResultID, int userid,
                                          Collection<Integer> allIds,
                                          Map<Integer, CollationKey> idToKey,
                                          String language);

  <T extends CommonShell> List<T> getExercisesSortedIncorrectFirst(Collection<T> exercises,
                                                                   int userid,
                                                                   Collator collator,
                                                                   String language);

  Collection<ExerciseCorrectAndScore> getExerciseCorrectAndScoresByPhones(int userid,
                                                                          List<Integer> allIds,
                                                                          Map<Integer, CommonExercise> idToEx,
                                                                          ExerciseSorter sorter,
                                                                          String language);

  void attachScoreHistory(int userID, CommonExercise firstExercise, boolean isFlashcardRequest, String language);

  Map<Integer, List<CorrectAndScore>> getScoreHistories(int userid, Collection<Integer> exercises, String language);

  List<CorrectAndScore> getResultsForExIDInForUser(int userID,
                                                   int id,
                                                   boolean isFlashcardRequest,
                                                   String language);

  List<CorrectAndScore> getResultsForExIDInForUser(Collection<Integer> ids, int userid, String session, String language);

  int getNumResults(int projid);

  int ensureDefault(int projid, int beforeLoginUser, int unknownExerciseID);

  int getDefaultResult();

  Map<String,Integer> getStudentAnswers(int projid);

  Collection<Integer> getPracticedByUser(int userid, int projid);

  /**
   *
   * @param userid
   * @param exercises
   * @param <T>
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getFullExercises(int, Collection)
   */
  <T extends HasID> Map<Integer, Float> getScores(int userid, Collection<T> exercises);
}
