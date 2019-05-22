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

package mitll.langtest.client.services;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.OOVInfo;

import java.util.Collection;

@RemoteServiceRelativePath("exercise-manager")
public interface ExerciseService<T extends CommonShell & ScoredExercise> extends RemoteService {
  /**
   * @param request
   * @return
   * @paramx <T>
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises
   */
  ExerciseListWrapper<T> getExerciseIds(ExerciseListRequest request) throws DominoSessionException;

  /**
   * @param exid
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise(int)
   */
  T getExercise(int exid) throws DominoSessionException;

  void refreshExercise(int projid, int exid) throws DominoSessionException;

  int getExerciseIDOrParent(int exid) throws DominoSessionException;

  ExerciseListWrapper<ClientExercise> getFullExercises(ExerciseListRequest request, Collection<Integer> ids) throws DominoSessionException;

  FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException;

  /**
   * @param userID
   * @param exid
   * @param nearTime
   * @return
   * @throws DominoSessionException
   * @see mitll.langtest.client.analysis.PlayAudio#playLast
   */
  Pair getLatestScoreAudioPath(int userID, int exid, long nearTime) throws DominoSessionException;

  /**
   * @see mitll.langtest.client.banner.OOVViewHelper#showUnsafeAgain(OOVInfo, int)
   * @param projid
   * @throws DominoSessionException
   */
  void reload(int projid) throws DominoSessionException;

  void refreshAudio(int exid) throws DominoSessionException;

  void refreshAllAudio(int projid) throws DominoSessionException;
}
