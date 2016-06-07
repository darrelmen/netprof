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

import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IResultDAO {
  List<Result> getResults();

  Collection<UserAndTime> getUserAndTimes();

  List<Result> getResultsForPractice();

  List<Result> getResultsDevices();

  List<Result> getResultsToDecode();

  List<MonitorResult> getMonitorResults();

  Result getResultByID(long id);

  void addUnitAndChapterToResults(List<MonitorResult> monitorResults, Map<String, CommonExercise> join);

  List<MonitorResult> getMonitorResultsByID(String id);

  SessionsAndScores getSessionsForUserIn2(Collection<String> ids, long latestResultID, long userid,
                                          Collection<String> allIds, Map<String, CollationKey> idToKey);

  <T extends CommonShell> List<T> getExercisesSortedIncorrectFirst(Collection<T> exercises, long userid, Collator collator);

  List<ExerciseCorrectAndScore> getExerciseCorrectAndScoresByPhones(long userid, List<String> allIds,
                                                                    Map<String, CommonExercise> idToEx,
                                                                    ExerciseSorter sorter);

  SessionInfo getSessions();

  void attachScoreHistory(int userID, CommonExercise firstExercise, boolean isFlashcardRequest);

  List<CorrectAndScore> getResultsForExIDInForUser(Collection<String> ids, long userid, String session);

  void invalidateCachedResults();

  int getNumResults();
}
