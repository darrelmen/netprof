package mitll.langtest.shared.custom;

import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.User;

import java.util.*;

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
 // private Set<Long> visitorIDs;

  private String name;
  private String description;
  private String classMarker;
  private boolean isPrivate;
  private boolean isReview;
  private List<UserExercise> exercises = new ArrayList<UserExercise>();

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
    //visitorIDs = new HashSet<Long>();
   // if (user != null) addVisitor(user);
  }

  public UserList(UserList ul) {
    this(ul.uniqueID,ul.getCreator(),ul.getName(),ul.getDescription(),ul.getClassMarker(),ul.isPrivate());
    for (UserExercise ue : ul.getExercises()) { addExercise(ue); }
  }

  public void addExercise(UserExercise toAdd) { exercises.add(toAdd);  }
  public void addExerciseAfter(UserExercise after, UserExercise toAdd) {
    int index = exercises.indexOf(after);
    if (index == -1) {
      exercises.add(toAdd);
    }
    else {
      exercises.add(index+1,toAdd);
    }
  }

  /**
   * @see #UserList(long, mitll.langtest.shared.User, String, String, String, boolean)
   * @paramx user
   */
/*
  public void addVisitor(User user) { visitorIDs.add(user.id); }
*/

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getClassMarker() {
    return classMarker;
  }

  public Collection<UserExercise> getExercises() {
    return exercises;
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListDAO#populateList(UserList)
   * @param exercises
   */
  public void setExercises(List<UserExercise> exercises) {
    this.exercises = exercises;
  }

  public boolean remove(UserExercise newUserExercise) {  return exercises.remove(newUserExercise); }

  public UserExercise remove(String id) {
    UserExercise toRemove = null;
    for (UserExercise ue : exercises) {
      if (id.equals(ue.getID())) {
        toRemove = ue;
      }
    }
    UserExercise userExercise = toRemove != null && exercises.remove(toRemove) ? toRemove : null;
    return userExercise;
  }

  public User getCreator() {
    return creator;
  }

/*
  public Set<Long> getVisitorIDs() {
    return visitorIDs;
  }
*/

/*
  public void setVisitors(Set<Long> where) {
    this.visitorIDs = where;
  }
*/

  public long getUniqueID() { return uniqueID; }
  public void setUniqueID(long uniqueID) {
    this.uniqueID = uniqueID;
  }

  public boolean contains(UserExercise userExercise) {
    return getExercises().contains(userExercise);
  }
  public boolean contains(String id) {
    for (UserExercise ue : exercises) {
      if (id.equals(ue.getID())) {
        return true;
      }
    }
    return false;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

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
    return "UserList #" + getUniqueID() + " '"+name + "' by " + creator.id+
      //" visited by " + visitorIDs+
      " : " + (isReview ? " REVIEW " : "")+
      " :"+
      " with " + getExercises().size() + " exercises.";
  }

}
