package mitll.langtest.shared.custom;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.Exercise;
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
public class UserList extends ExerciseShell {//implements IsSerializable {
  private User creator;
  private Set<Long> visitorIDs = new HashSet<Long>();
  private int uniqueID;

  private String name;
  private String description;
  private String classMarker;
  private List<Exercise> exercises = new ArrayList<Exercise>();

  public UserList(){}
  public UserList(int uniqueID, User user, String name, String description, String classMarker){
    super(""+uniqueID,name);
    this.uniqueID = uniqueID;
    this.creator = user;
    this.name = name;
    this.description = description;
    this.classMarker = classMarker;
    addVisitor(user);
  }

  public void addExercise(Exercise toAdd) { exercises.add(toAdd); }
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

  public List<Exercise> getExercises() {
    return exercises;
  }

  public User getCreator() {
    return creator;
  }

  public Set<Long> getVisitorIDs() {
    return visitorIDs;
  }

  @Override
  public String toString() {
    return "UserList #" + getUniqueID() + " "+name + " by " + creator+ " visited by " + visitorIDs+
      "  : "+
      " with " + getExercises().size() + " exercises.";
  }

  public int getUniqueID() {
    return uniqueID;
  }
}
