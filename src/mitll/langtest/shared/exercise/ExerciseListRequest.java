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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/30/16.
 */
public class ExerciseListRequest implements Serializable {
  private int reqID = 1;
  private Map<String, Collection<String>> typeToSelection = new HashMap<>();
  private String prefix = "";
  private long userListID = -1;

  private int userID = -5;

  private String role = "";
  private boolean onlyUnrecordedByMe = false;
  private boolean onlyExamples = false;
  private boolean incorrectFirstOrder = false;
  private boolean onlyWithAudioAnno = false;
  private boolean onlyDefaultAudio = false;
  private boolean onlyUninspected = false;

  public ExerciseListRequest() {
  }

  public ExerciseListRequest(int reqID, int userID) {
    this.reqID = reqID;
    this.userID = userID;
  }

  /**
   * TODO something less error prone
   *
   * @param other
   * @return
   */
  public boolean sameAs(ExerciseListRequest other) {
    return prefix.equals(other.getPrefix()) &&
        typeToSelection.equals(other.getTypeToSelection()) &&
        onlyUnrecordedByMe == other.onlyUnrecordedByMe &&
        onlyExamples == other.onlyExamples &&
        incorrectFirstOrder == other.incorrectFirstOrder &&
        onlyWithAudioAnno == other.onlyWithAudioAnno &&
        onlyDefaultAudio == other.onlyDefaultAudio &&
        onlyUninspected == other.onlyUninspected &&
        userListID == other.userListID;
  }

  public int getReqID() {
    return reqID;
  }

  public Map<String, Collection<String>> getTypeToSelection() {
    return typeToSelection;
  }

  public ExerciseListRequest setTypeToSelection(Map<String, Collection<String>> typeToSelection) {
    this.typeToSelection = typeToSelection;
    return this;
  }

  public String getPrefix() {
    return prefix;
  }

  public ExerciseListRequest setPrefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  public long getUserListID() {
    return userListID;
  }

  public ExerciseListRequest setUserListID(long userListID) {
    this.userListID = userListID;
    return this;
  }

  public int getUserID() {
    return userID;
  }

  public ExerciseListRequest setUserID(int userID) {
    this.userID = userID;
    return this;
  }

  public String getRole() {
    return role;
  }

  public ExerciseListRequest setRole(String role) {
    this.role = role;
    return this;
  }

  public boolean isOnlyUnrecordedByMe() {
    return onlyUnrecordedByMe;
  }

  public ExerciseListRequest setOnlyUnrecordedByMe(boolean onlyUnrecordedByMe) {
    this.onlyUnrecordedByMe = onlyUnrecordedByMe;
    return this;
  }

  public boolean isOnlyExamples() {
    return onlyExamples;
  }

  public ExerciseListRequest setOnlyExamples(boolean onlyExamples) {
    this.onlyExamples = onlyExamples;
    return this;
  }

  public boolean isIncorrectFirstOrder() {
    return incorrectFirstOrder;
  }

  public ExerciseListRequest setIncorrectFirstOrder(boolean incorrectFirstOrder) {
    this.incorrectFirstOrder = incorrectFirstOrder;
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#filterExercises(ExerciseListRequest, Collection)
   */
  public boolean isOnlyWithAudioAnno() {
    return onlyWithAudioAnno;
  }

  public ExerciseListRequest setOnlyWithAudioAnno(boolean onlyWithAudioAnno) {
    this.onlyWithAudioAnno = onlyWithAudioAnno;
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#filterExercises(ExerciseListRequest, Collection)
   */
  public boolean isOnlyDefaultAudio() {
    return onlyDefaultAudio;
  }

  public ExerciseListRequest setOnlyDefaultAudio(boolean onlyDefaultAudio) {
    this.onlyDefaultAudio = onlyDefaultAudio;
    return this;
  }

  public boolean isOnlyUninspected() {
    return onlyUninspected;
  }

  public ExerciseListRequest setOnlyUninspected(boolean onlyDefaultAudio) {
    this.onlyUninspected = onlyDefaultAudio;
    return this;
  }

  public String toString() {
    return
        "prefix                  '" + prefix + "'" +
            (getTypeToSelection().isEmpty() ? "" : "\n\tselection           " + getTypeToSelection()) +
            "\n\tuser list id        " + userListID +
            "\n\tuser                " + userID +
            "\n\trole                " + role +
            "\n\tincorrect first     " + incorrectFirstOrder +
            "\n\tonly recorded by me " + onlyUnrecordedByMe +
            "\n\tonly examples       " + onlyExamples +
            "\n\tonly with audio     " + onlyWithAudioAnno +
            "\n\tonly uninspecte     " + onlyUninspected;
  }
}
