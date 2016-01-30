/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.custom;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.BaseExercise;
import mitll.langtest.shared.exercise.CommonUserExercise;
import mitll.langtest.shared.exercise.ExerciseShell;
import mitll.langtest.shared.exercise.Shell;

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
public class UserList<T extends Shell> extends BaseExercise {
  public static final String MY_LIST = "Favorites";

  private long uniqueID;

  private User creator;
  private String name;
  private String description;
  private String classMarker;
  private boolean isPrivate;
  private boolean isReview;
  private List<T> exercises = new ArrayList<>();

  public UserList(){}

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#createUserList(long, String, String, String, boolean)
   * @param uniqueID
   * @param user
   * @param name
   * @param description
   * @param classMarker
   */
  public UserList(long uniqueID, User user, String name, String description, String classMarker, boolean isPrivate){
    super(""+uniqueID);
    this.uniqueID = uniqueID;
    this.creator = user;
    this.name = name;
    this.description = description;
    this.classMarker = classMarker;
    this.isPrivate = isPrivate;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.EditItem#makeListOfOnlyYourItems(UserList)
   * @param ul
   */
  public UserList(UserList ul) {
    this(ul.uniqueID, ul.getCreator(), ul.getName(), ul.getDescription(), ul.getClassMarker(), ul.isPrivate());
  }

  public UserList<T> getCopy() {
    UserList<T> copy  = new UserList<>();
    for (T ue : getExercises()) {
      copy.addExercise(ue);
    }
    return copy;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.EditItem#makeExerciseList(Panel, String, UserList, UserList, boolean)
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterItemCreated(UserExercise, UserList, ListInterface, Panel)
   * @param toAdd
   */
  public void addExercise(T toAdd) { exercises.add(toAdd);  }
  public void addExerciseAfter(T after, T toAdd) {
    int index = exercises.indexOf(after);
    if (index == -1) {
      exercises.add(toAdd);
    }
    else {
      exercises.add(index+1,toAdd);
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

  /**
   * @see mitll.langtest.server.database.custom.UserListDAO#populateList(UserList)
   * @param exercises
   */
  public void setExercises(List<T> exercises) {
    this.exercises = exercises;
  }

  public boolean remove(T newUserExercise) {  return exercises.remove(newUserExercise); }

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
  public long getUniqueID() { return uniqueID; }
  public void setUniqueID(long uniqueID) {
    this.uniqueID = uniqueID;
  }

  public boolean contains(T userExercise) {
    return getExercises().contains(userExercise);
  }

  public boolean containsByID(T userExercise) {
    for (T ex : getExercises()) {
      if (ex.getID().equals(userExercise.getID())) return true;
    }
    return false;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  /**
   * @see mitll.langtest.client.custom.ListManager#selectTabGivenHistory
   * @see Navigation#showInitialState()
   * @see mitll.langtest.client.custom.UserListCallback#addUserListsToDisplay(Collection, Panel, Map)
   * @return
   */
  public boolean isEmpty() { return getExercises().isEmpty();  }
  public boolean isFavorite() { return getName().equals(MY_LIST);  }

  public void setReview(boolean isReview) {
    this.isReview = isReview;
  }

  @Override
  public String toString() {
    return "UserList #" + getUniqueID() + " '"+name + "' by " + creator.getId() +
      " : " + (isReview ? " REVIEW " : "")+
      " :"+
      " with " + getExercises().size() + " exercises.";
  }
}
