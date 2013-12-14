package mitll.langtest.shared.custom;

import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
  private Set<Long> visitorIDs;

  private String name;
  private String description;
  private String classMarker;
  private long modified;
  private boolean isPrivate;
  private Collection<UserExercise> exercises = new ArrayList<UserExercise>();

  public UserList(){}

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#createUserList(long, String, String, String, boolean)
   * @param uniqueID
   * @param user
   * @param name
   * @param description
   * @param classMarker
   */
  public UserList(long uniqueID, User user, String name, String description, String classMarker, long modified, boolean isPrivate){
    super(""+uniqueID,name);
    this.uniqueID = uniqueID;
    this.creator = user;
    this.name = name;
    this.description = description;
    this.classMarker = classMarker;
    this.isPrivate = isPrivate;
    visitorIDs = new HashSet<Long>();
    if (user != null) addVisitor(user);
    this.modified = modified;
  }

  public void addExercise(UserExercise toAdd) {
    exercises.add(toAdd);
    modified = System.currentTimeMillis();
  }

  /**
   * @see #UserList(long, mitll.langtest.shared.User, String, String, String, long, boolean)
   * @param user
   */
  public void addVisitor(User user) { visitorIDs.add(user.id); }

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

  public void setExercises(Collection<UserExercise> exercises) {
    this.exercises = exercises;
  }

  public User getCreator() {
    return creator;
  }

  public Set<Long> getVisitorIDs() {
    return visitorIDs;
  }

  public void setVisitors(Set<Long> where) {
    this.visitorIDs = where;
  }

  public long getUniqueID() {
    return uniqueID;
  }

  public void setUniqueID(long uniqueID) {
    this.uniqueID = uniqueID;
  }

/*  public long getModified() {
    return modified;
  }*/

  public boolean contains(UserExercise userExercise) {
    return getExercises().contains(userExercise);
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

  @Override
  public String toString() {
    return "UserList #" + getUniqueID() + " '"+name + "' by " + creator.id+ " visited by " + visitorIDs+
        " :"+
        " with " + getExercises().size() + " exercises.";
  }
}
