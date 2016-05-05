package mitll.langtest.shared.exercise;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 3/30/16.
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

  public ExerciseListRequest() {
  }

  public ExerciseListRequest(int reqID,
                             int userID
  ) {
    this.reqID = reqID;
    this.userID = userID;
  }


  public int getReqID() {
    return reqID;
  }

/*  public void setReqID(int reqID) {
    this.reqID = reqID;
  }*/

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

  public boolean isOnlyWithAudioAnno() {
    return onlyWithAudioAnno;
  }

  public ExerciseListRequest setOnlyWithAudioAnno(boolean onlyWithAudioAnno) {
    this.onlyWithAudioAnno = onlyWithAudioAnno;
    return this;
  }

  public String toString() {
    return " prefix '" + prefix +
        "' and user list id " + userListID + " user " + userID + " role " + role +
        " filter " + onlyUnrecordedByMe + " only examples " + onlyExamples + " only with audio " + onlyWithAudioAnno;
  }

  public boolean isOnlyDefaultAudio() {
    return onlyDefaultAudio;
  }

  public ExerciseListRequest setOnlyDefaultAudio(boolean onlyDefaultAudio) {
    this.onlyDefaultAudio = onlyDefaultAudio;
    return this;
  }
}
