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

package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.CommonExercise;

import java.util.Collections;
import java.util.List;

/**
 * Three buckets - match by id, matches on vocab item, then match on context sentences
 *
 * @param <T>
 */
public class TripleExercises<T extends CommonExercise> {
  private List<T> byID = Collections.emptyList();
  private List<T> byExercise = Collections.emptyList();
  private List<T> byContext = Collections.emptyList();

  public TripleExercises() {
  }

  TripleExercises(List<T> byID, List<T> byExercise, List<T> byContext) {
    this.byID = byID;
    this.byExercise = byExercise;
    this.byContext = byContext;
  }

  public List<T> getByID() {
    return byID;
  }

  public List<T> getByExercise() {
    return byExercise;
  }

  public TripleExercises<T> setByExercise(List<T> byExercise) {
    this.byExercise = byExercise;
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getSortedContext
   */
  public List<T> getByContext() {
    return byContext;
  }

  public void setByID(List<T> byID) {
    this.byID = byID;
  }


  public boolean isEmpty() {
    return byExercise.isEmpty() && byContext.isEmpty() && byID.isEmpty();
  }

  public String toString() {
    return
        "\n\tby id      " + byID.size() +
            "\n\tby ex      " + byExercise.size() +
            "\n\tby context " + byContext.size();
  }
}
