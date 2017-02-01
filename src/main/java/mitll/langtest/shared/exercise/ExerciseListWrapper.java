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

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

public class ExerciseListWrapper<T extends Shell> implements IsSerializable {
  public ExerciseListWrapper() {} // req for serialization

  private int reqID;
  private List<T> exercises;
  private CommonExercise firstExercise;

  /**
   * @see mitll.langtest.server.services.ExerciseServiceImpl#makeExerciseListWrapper
   * @param reqID
   * @param ids
   * @param firstExercise
   */
  public ExerciseListWrapper(int reqID, List<T> ids, CommonExercise firstExercise) {
    this.reqID = reqID;
    this.exercises = ids;
    this.firstExercise = firstExercise;
  }

  public int getReqID() {
    return reqID;
  }
  public List<T> getExercises() { return exercises;  }
  public CommonExercise getFirstExercise() { return firstExercise;  }

  public String toString() {
    return "req " + reqID + " has " + exercises.size() + " exercises" +
        (firstExercise != null ? ", first is " + firstExercise.getID() : "");
  }
}
