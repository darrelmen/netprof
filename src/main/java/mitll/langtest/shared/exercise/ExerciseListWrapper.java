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

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.AlignmentAndScore;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseListWrapper<T extends HasID> implements IsSerializable {
  public ExerciseListWrapper() {
  } // req for serialization

  private int reqID;
  private List<T> exercises;
  private Map<Integer, CorrectAndScore> scoreHistoryPerExercise;
  private Map<Integer, Float> idToScore = new HashMap<>();
  private Map<Integer, AlignmentAndScore> cachedAlignments;

  /**
   * @param reqID
   * @param ids
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExerciseIds
   */
  public ExerciseListWrapper(int reqID, List<T> ids) {
    this(reqID, ids, new HashMap<>(), new HashMap<>());
  }

  /**
   * @param reqID
   * @param ids
   * @see mitll.langtest.server.services.ExerciseServiceImpl#makeExerciseListWrapper
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getFullExercises
   */
  public ExerciseListWrapper(int reqID,
                             List<T> ids,
                             Map<Integer, CorrectAndScore> scoreHistoryPerExercise,
                             Map<Integer, AlignmentAndScore> cachedAlignments
  ) {
    this.reqID = reqID;
    this.exercises = ids;
    this.scoreHistoryPerExercise = scoreHistoryPerExercise;
    this.cachedAlignments = cachedAlignments;
  }

  public int getReqID() {
    return reqID;
  }

  public List<T> getExercises() {
    return exercises;
  }

  public int getSize() {
    return exercises.size();
  }

  /**
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#setScoreHistory
   */
  public Map<Integer, CorrectAndScore> getScoreHistoryPerExercise() {
    return scoreHistoryPerExercise;
  }

  public Map<Integer, Float> getIdToScore() {
    return idToScore;
  }

  /**
   * @param idToScore
   * @see mitll.langtest.server.services.ExerciseServiceImpl#makeExerciseListWrapper(ExerciseListRequest, Collection, int)
   */
  public void setIdToScore(Map<Integer, Float> idToScore) {
    this.idToScore = idToScore;
  }

  public Map<Integer, AlignmentAndScore> getCachedAlignments() {
    return cachedAlignments;
  }

  public String toString() {
    return "req " + reqID + " has " + exercises.size() + " exercises";
  }
}
