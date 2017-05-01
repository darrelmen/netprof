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

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.userlist.ListManager;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.SlickUserListDAO;
import mitll.langtest.server.database.userlist.UserListDAO;
import mitll.langtest.shared.exercise.BaseExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.HasID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/27/13
 * Time: 8:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserList<T extends HasID> extends BaseExercise {
  public static final String MY_LIST = "Favorites";

  @Deprecated protected String oldid = "";

  private int userid;
  private String userChosenID;

  private String name;
  private String description;
  private String classMarker;

  private boolean isPrivate;
  private boolean isReview;
  private long modified;
  private String contextURL;

  private List<T> exercises = new ArrayList<>();
  private String richText;

  public UserList() {}

  /**
   * @param uniqueID
   * @param userid
   * @param userChosenID
   * @param name
   * @param description
   * @param classMarker
   * @param richText
   * @paramx user
   * @see mitll.langtest.server.database.custom.UserListManager#createUserList
   * @see UserListDAO#getWhere(long, boolean)
   */
  public UserList(int uniqueID,
                  int userid,
                  String userChosenID,
                  String name,
                  String description,
                  String classMarker,
                  boolean isPrivate,
                  long modified,
                  String contextURL,
                  String richText) {
    super(uniqueID);
     this.userid = userid;
    this.userChosenID = userChosenID;
    this.name = name;
    this.description = description;
    this.classMarker = classMarker;
    this.isPrivate = isPrivate;
    this.modified = modified;
    this.contextURL = contextURL;
    this.richText =richText;
  }

  public int getUserID() { return userid; }

  /**
   * @paramx ul
   * @seex mitll.langtest.shared.custom.UserList#getCopy
   */
/*  public UserList(UserList<T> ul) {
    this(ul.getID(), ul.getUserID() , ul.getUserChosenID(), ul.getName(), ul.getDescription(), ul.getClassMarker(), ul.isPrivate(), ul.getModified(),
        ul.getContextURL());
  }*/

/*
  public UserList<T> getCopy() {
    UserList<T> copy = new UserList<>(this);
    for (T ue : getExercises()) {
      copy.addExercise(ue);
    }
    return copy;
  }
*/

  public void addExerciseAfter(T after, T toAdd) {
    int index = exercises.indexOf(after);
    if (index == -1) {
      addExercise(toAdd);
    } else {
      exercises.add(index + 1, toAdd);
    }
  }

  /**
   * @param toAdd
   * @see mitll.langtest.client.custom.dialog.EditItem#makeExerciseList
   * @see mitll.langtest.client.custom.dialog.EditableExerciseList#showNewItem
   */
  public void addExercise(T toAdd) {
    exercises.add(toAdd);
  }

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
   *
   * @return
   */
  public List<T> getExercises() {
    return exercises;
  }

  public int getNumItems() { return exercises.size(); }

  public T getLast() {
    return exercises.get(exercises.size() - 1);
  }

  /**
   * @param exercises
   * @see UserListDAO#populateList(UserList)
   * @see SlickUserListDAO#populateList(UserList)
   */
  public void setExercises(List<T> exercises) {
    this.exercises = exercises;
  }

  public boolean remove(T newUserExercise) { return exercises.remove(newUserExercise); }
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
   * @see mitll.langtest.server.database.custom.UserListManager#getCommentedList
   */
  public void setUniqueID(int uniqueID) {
    this.id = uniqueID;
  }

/*
  public boolean contains(T userExercise) {
    return getExercises().contains(userExercise);
  }
*/

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
   * @see ListManager#selectTabGivenHistory
   * @see Navigation#showInitialState()
   * @see mitll.langtest.client.custom.UserListCallback#addUserListsToDisplay(Collection, Panel, Map)
   */
  public boolean isEmpty() {
    return getExercises().isEmpty();
  }

  public boolean isFavorite() {
    return getName().equals(MY_LIST);
  }

  public void setReview(boolean isReview) {
    this.isReview = isReview;
  }

  public long getModified() {
    return modified;
  }

  public void setModified(long modified) {
    this.modified = modified;
  }

  @Deprecated
  public String getOldID() {
    return oldid;
  }

  /**
   * @param id
   * @see IUserExerciseDAO#add(CommonExercise, boolean, boolean, List)
   */
  @Deprecated
  public void setOldID(String id) {
    this.oldid = id;
  }

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
  public String toString() {
    return "UserList #" + getID() + " '" + name + "' by " + getUserID() +
        " : " + (isReview ? " REVIEW " : "") +
        " : with " +  getNumItems()  + " exercises.";
  }
}
