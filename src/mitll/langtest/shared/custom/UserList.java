/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
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
 * User: GO22670
 * Date: 9/27/13
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

  public Collection<T> getExercises() {
    return exercises;
  }

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
    return exercises.remove(newUserExercise);
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
        " with " + getExercises().size() + " exercises.";
  }
}
