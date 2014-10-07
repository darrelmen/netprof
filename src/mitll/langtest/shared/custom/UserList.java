package mitll.langtest.shared.custom;

import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserList extends ExerciseShell {
  public static final String MY_LIST = "Favorites";

  private long uniqueID;

  private User creator;
  private String name;
  private String description;
  private String classMarker;
  private boolean isPrivate;
  private boolean isReview;
  private List<CommonUserExercise> exercises = new ArrayList<CommonUserExercise>();

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
    super(""+uniqueID,name);
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

  public void addExercise(CommonUserExercise toAdd) { exercises.add(toAdd);  }
  public void addExerciseAfter(CommonUserExercise after, CommonUserExercise toAdd) {
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

  public Collection<CommonUserExercise> getExercises() {
    return exercises;
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListDAO#populateList(UserList)
   * @param exercises
   */
  public void setExercises(List<CommonUserExercise> exercises) {
    this.exercises = exercises;
  }

  public boolean remove(CommonUserExercise newUserExercise) {  return exercises.remove(newUserExercise); }

  public CommonUserExercise remove(String id) {
    CommonUserExercise toRemove = null;
    for (CommonUserExercise ue : exercises) {
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

  public boolean contains(CommonUserExercise userExercise) {
    return getExercises().contains(userExercise);
  }
  public boolean contains(String id) {
    for (CommonUserExercise ue : exercises) {
      if (id.equals(ue.getID())) {
        return true;
      }
    }
    return false;
  }

  public boolean isPrivate() {
    return isPrivate;
  }
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
