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
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.BaseExercise;
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

  private long uniqueID;

  private User creator;
  private String name;
  private String description;
  private String classMarker;
  private boolean isPrivate;
  private boolean isReview;
  private List<T> exercises = new ArrayList<>();

  public UserList() {
  }

  /**
   * @param uniqueID
   * @param user
   * @param name
   * @param description
   * @param classMarker
   * @see mitll.langtest.server.database.custom.UserListManager#createUserList(long, String, String, String, boolean)
   * @see mitll.langtest.server.database.custom.UserListDAO#getWhere(long, boolean)
   */
  public UserList(long uniqueID, User user, String name, String description, String classMarker, boolean isPrivate) {
    super("" + uniqueID);
    this.uniqueID = uniqueID;
    this.creator = user;
    this.name = name;
    this.description = description;
    this.classMarker = classMarker;
    this.isPrivate = isPrivate;
  }

  /**
   * @param ul
   * @see mitll.langtest.client.custom.dialog.EditItem#makeListOfOnlyYourItems(UserList)
   */
  public UserList(UserList<T> ul) {
    this(ul.uniqueID, ul.getCreator(), ul.getName(), ul.getDescription(), ul.getClassMarker(), ul.isPrivate());
  }

  public UserList<T> getCopy() {
    UserList<T> copy = new UserList<>(this);
    for (T ue : getExercises()) {
      copy.addExercise(ue);
    }
    return copy;
  }

  /**
   * @param toAdd
   * @see mitll.langtest.client.custom.dialog.EditItem#makeExerciseList(Panel, String, UserList, UserList, boolean)
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterItemCreated(UserExercise, UserList, ListInterface, Panel)
   */
  public void addExercise(T toAdd) {
    exercises.add(toAdd);
  }

  public void addExerciseAfter(T after, T toAdd) {
    int index = exercises.indexOf(after);
    if (index == -1) {
      exercises.add(toAdd);
    } else {
      exercises.add(index + 1, toAdd);
    }
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
  public Collection<T> getExercises() {
    return exercises;
  }

  public int getNumItems() { return exercises.size(); }

  public T getLast() {
    return exercises.get(exercises.size() - 1);
  }

  /**
   * @param exercises
   * @see mitll.langtest.server.database.custom.UserListDAO#populateList(UserList)
   */
  public void setExercises(List<T> exercises) {
    this.exercises = exercises;
  }

  public boolean remove(T newUserExercise) {
    int before = exercises.size();
    boolean remove = exercises.remove(newUserExercise);
    int after = exercises.size();
   // if (after-before != 1) System.err.println("huh? before " + before + " after " + after);
    return remove;
  }

  public boolean removeAndCheck(String id) {
    return remove(id) != null;
  }

  public T remove(String id) {
    T toRemove = null;
    for (T ue : exercises) {
      if (id.equals(ue.getID())) {
        toRemove = ue;
      }
    }
    return toRemove != null && exercises.remove(toRemove) ? toRemove : null;
  }

  public User getCreator() {
    return creator;
  }

  public long getUniqueID() {
    return uniqueID;
  }

  /**
   * @param uniqueID
   * @see mitll.langtest.server.database.custom.UserListDAO#add(UserList)
   * @see mitll.langtest.server.database.custom.UserListManager#getCommentedList(Collection)
   */
  public void setUniqueID(long uniqueID) {
    this.uniqueID = uniqueID;
  }

  public boolean contains(T userExercise) {
    return getExercises().contains(userExercise);
  }

  public boolean containsByID(String id) {
    for (T ex : getExercises()) {
      if (ex.getID().equals(id)) return true;
    }
    return false;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.ListManager#selectTabGivenHistory
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

  @Override
  public String toString() {
    long id = creator == null ? -1 : creator.getId();
    return "UserList #" + getUniqueID() + " '" + name + "' by " + id +
        " : " + (isReview ? " REVIEW " : "") +
        " :" +
        " with " + getNumItems() + " exercises.";
  }
}
