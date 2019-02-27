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

package mitll.langtest.shared.custom;

import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.SlickUserListDAO;
import mitll.langtest.shared.exercise.BaseExercise;
import mitll.langtest.shared.exercise.HasID;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/27/13
 * Time: 8:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserList<T extends HasID> extends BaseExercise implements IUserListWithIDs {
  public static final String MY_LIST = "Favorites";


  public enum LIST_TYPE {NORMAL, QUIZ}

  private int projid;

  private int userid;
  private String userChosenID;
  private String firstInitialName;

  private String name;
  private String description;
  private String classMarker;
  private LIST_TYPE listType;

  private boolean isPrivate;

  private long modified;

  private long start;
  private long end;

  private int duration;
  private int minScore;
  private boolean showAudio;
  private String accessCode;

  private boolean teacher;
  /**
   * Notional
   */
  private String contextURL;

  private List<T> exercises = new ArrayList<>();

  private int numItems;
  private String richText;

  public UserList() {
  }

  /**
   * @param uniqueID
   * @param userid
   * @param userChosenID
   * @param firstInitialName
   * @param name
   * @param description
   * @param classMarker
   * @param richText
   * @param projid
   * @param listType
   * @param start
   * @param end
   * @param duration
   * @param minScore
   * @param showAudio
   * @param accessCode
   * @see mitll.langtest.server.database.custom.UserListManager#createUserList
   * @see IUserListDAO#getWhere(int, boolean)
   */
  public UserList(int uniqueID,
                  int userid,
                  String userChosenID,
                  String firstInitialName,

                  String name,
                  String description,
                  String classMarker,

                  boolean isPrivate,
                  long modified,
                  String contextURL,
                  String richText,
                  int projid,
                  LIST_TYPE listType,
                  long start,
                  long end,
                  int duration, int minScore, boolean showAudio, String accessCode) {
    super(uniqueID);
    this.userid = userid;
    this.userChosenID = userChosenID;
    this.firstInitialName = firstInitialName;
    this.name = name;
    this.description = description;
    this.classMarker = classMarker;
    this.isPrivate = isPrivate;
    this.modified = modified;
    this.contextURL = contextURL;
    this.richText = richText;
    this.projid = projid;
    this.listType = listType;
    this.start = start;
    this.end = end;
    this.duration = duration;
    this.minScore = minScore;
    this.showAudio = showAudio;
    this.accessCode = accessCode;
  }

  @Override
  public int getUserID() {
    return userid;
  }

  /**
   * @param toAdd
   * @see mitll.langtest.client.custom.dialog.EditItem#makeExerciseList
   * @see mitll.langtest.client.custom.dialog.EditableExerciseList#showNewItem
   */
  public void addExercise(T toAdd) {
    exercises.add(toAdd);
  }

  @Override
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getClassMarker() {
    return classMarker;
  }

  /**
   * @return
   */
  public List<T> getExercises() {
    return exercises;
  }

  /**
   * OK to have an empty exercise list here.
   * @return
   */
  @Override
  public int getNumItems() {
    return exercises.size() == 0 ? numItems : exercises.size();
  }

  @Override
  public void setNumItems(int numItems) {
    this.numItems = numItems;
  }

  /**
   * @param exercises
   * @seex UserListDAO#populateList(UserList)
   * @see SlickUserListDAO#populateList
   * @see SlickUserListDAO#populateListEx
   */
  public void setExercises(List<T> exercises) {
    this.exercises = exercises;
  }

  public boolean removeAndCheck(int id) {
    return remove(id) != null;
  }

  public T remove(int id) {
    T toRemove = null;
    for (T ue : exercises) {
      if (id == ue.getID()) {
        toRemove = ue;
        break;
      }
    }
    return toRemove != null && exercises.remove(toRemove) ? toRemove : null;
  }

  /**
   * @param uniqueID
   * @see IUserListDAO#add(UserList, int)
   * @see IUserListManager#getCommentedList
   */
  public void setUniqueID(int uniqueID) {
    this.id = uniqueID;
  }

  @Override
  public boolean containsByID(int id) {
    for (T ex : getExercises()) {
      if (ex.getID() == id) return true;
    }
    return false;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.dialog.EditableExerciseList#EditableExerciseList
   */
  public boolean isEmpty() {
    return getExercises().isEmpty();
  }

  public boolean isFavorite() {
    return getName().equals(MY_LIST);
  }

  public long getModified() {
    return modified;
  }

  @Override
  public String getUserChosenID() {
    return userChosenID;
  }

  public String getContextURL() {
    return contextURL;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRichText() {
    return richText;
  }

  @Override
  public int getProjid() {
    return projid;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setClassMarker(String classMarker) {
    this.classMarker = classMarker;
  }

  public void setPrivate(boolean aPrivate) {
    isPrivate = aPrivate;
  }

  public LIST_TYPE getListType() {
    return listType;
  }

  public void setListType(LIST_TYPE listType) {
    this.listType = listType;
  }

  @Override
  public int getRoundTimeMinutes() {
    return duration;
  }

  /**
   * @return
   * @see mitll.langtest.client.flashcard.QuizIntro#onQuizChoice
   */
  @Override
  public int getMinScore() {
    return minScore;
  }

  @Override
  public boolean shouldShowAudio() {
    return showAudio;
  }

  public void setMinScore(int minScore) {
    this.minScore = minScore;
  }

  public void setShowAudio(boolean showAudio) {
    this.showAudio = showAudio;
  }

  public int getDuration() {
    return getRoundTimeMinutes();
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  /**
   * So we can show which lists are owned by a teacher.
   *
   * @return
   */
  public boolean isTeacher() {
    return teacher;
  }

  public UserList<T> setTeacher(boolean teacher) {
    this.teacher = teacher;
    return this;
  }

  /**
   * Notional idea of when a list or quiz is visible - additional dimension on top of private/public
   *
   * @return
   */
  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  @Override
  public String getFirstInitialName() {
    return firstInitialName;
  }

  public String getAccessCode() {
    return accessCode;
  }

  @Override
  public String toString() {
    return "UserList #" + getID() + " '" + name + "' by " + getUserID() + "/" + getFirstInitialName() +
        "\n\tshow audio " + shouldShowAudio() +
        //  "\n\t : " + (isReview ? " REVIEW " : "") +
        " : with " + getNumItems() + " exercises.";
  }
}
