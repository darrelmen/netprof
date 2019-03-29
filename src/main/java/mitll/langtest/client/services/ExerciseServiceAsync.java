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

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.list.ExerciseList;
import mitll.langtest.shared.exercise.*;

import java.util.Collection;

public interface ExerciseServiceAsync<T extends CommonShell & HasUnitChapter> {
  /**
   * @param request
   * @param async
   * @see ExerciseList#getExercises
   */
  void getExerciseIds(ExerciseListRequest request, AsyncCallback<ExerciseListWrapper<T>> async);

  /**
   * @param exid
   * @param async
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   */
  void getExercise(int exid, AsyncCallback<T> async);

  void getTypeToValues(FilterRequest request, AsyncCallback<FilterResponse> async);

  void getFullExercises(ExerciseListRequest request, Collection<Integer> ids,
                        AsyncCallback<ExerciseListWrapper<ClientExercise>> async);

  /**
   * @param userID
   * @param exid
   * @param nearTime
   * @param async
   * @see mitll.langtest.client.analysis.PlayAudio#playLast
   */
  void getLatestScoreAudioPath(int userID, int exid, long nearTime, AsyncCallback<Pair> async);

  void getExerciseIDOrParent(int exid, AsyncCallback<Integer> async);

  void refreshExercise(int projid, int exid, AsyncCallback<Void> async);

  void reload(int projid, AsyncCallback<Void> async);
}
