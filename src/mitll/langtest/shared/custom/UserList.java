package mitll.langtest.shared.custom;

import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserList extends ExerciseShell {
  private User creator;
  private Set<Long> visitorIDs;
  private int uniqueID;

  private String name;
  private String description;
  private String classMarker;
  private long modified;
  private boolean isPrivate;
  private List<UserExercise> exercises = new ArrayList<UserExercise>();

  public UserList(){}

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#getUserList(long, String, String, String)
   * @param uniqueID
   * @param user
   * @param name
   * @param description
   * @param classMarker
   */
  public UserList(int uniqueID, User user, String name, String description, String classMarker, boolean isPrivate){
    super(""+uniqueID,name);
    this.uniqueID = uniqueID;
    this.creator = user;
    this.name = name;
    this.description = description;
    this.classMarker = classMarker;
    this.isPrivate = isPrivate;
    visitorIDs = new HashSet<Long>();
    addVisitor(user);
    modified = System.currentTimeMillis();
  }

  public void addExercise(UserExercise toAdd) {
    exercises.add(toAdd);
    modified = System.currentTimeMillis();
  }

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

  public List<UserExercise> getExercises() {
    return exercises;
  }

  public User getCreator() {
    return creator;
  }

  public Set<Long> getVisitorIDs() {
    return visitorIDs;
  }

  public int getUniqueID() {
    return uniqueID;
  }

  public long getModified() {
    return modified;
  }

  public boolean contains(UserExercise userExercise) {
    return getExercises().contains(userExercise);
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isEmpty() {
    return getExercises().isEmpty();
  }

  @Override
  public String toString() {
    return "UserList #" + getUniqueID() + " "+name + " by " + creator.id+ " visited by " + visitorIDs+
        "  : "+
        " with " + getExercises().size() + " exercises.";
  }
}
