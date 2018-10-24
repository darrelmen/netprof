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

package mitll.langtest.server.database.reviewed;

import mitll.langtest.shared.exercise.STATE;

import java.util.Date;

public class StateCreator implements Comparable<StateCreator> {
  private final STATE state;
  private long creatorID;
  private final long when;
  private int exerciseID =-1;
  private String oldExID;

  StateCreator(STATE state, long creatorID, long when) {
    this.state = state;
    this.creatorID = creatorID;
    this.when = when;
  }

  public STATE getState() {
    return state;
  }

  /**
   * @return
   * @seex UserListManager#getAmmendedStateMap
   */
  public long getCreatorID() {
    return creatorID;
  }

  public long getWhen() {
    return when;
  }

  public String toString() {
    return exerciseID +" = [" + state.toString() + " by " + creatorID + " at " + new Date(when) + "]";
  }

  public int getExerciseID() {
    return exerciseID;
  }

  public void setExerciseID(int exerciseID) {
    this.exerciseID = exerciseID;
  }

  public void setCreatorID(long creatorID) {
    this.creatorID = creatorID;
  }

  @Override
  public int compareTo(StateCreator o) {
    int i = Integer.compare(exerciseID, o.exerciseID);
    if (i == 0) i = Long.compare(creatorID, o.creatorID);
    if (i == 0) i = state.compareTo(o.state);
    if (i == 0) i = Long.compare(when, o.when);
    return i;
  }

  public String getOldExID() {
    return oldExID;
  }

  public void setOldExID(String oldExID) {
    this.oldExID = oldExID;
  }
}
