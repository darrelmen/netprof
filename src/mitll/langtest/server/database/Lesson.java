package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 1/30/13
 * Time: 7:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class Lesson {
  private List<Exercise> exerciseList = new ArrayList<Exercise>();
  private String unit;

  public Lesson(){}
  public Lesson(String unit) { this.unit = unit; }
  public void addExercise(Exercise e) { exerciseList.add(e); }
  public Collection<Exercise> getExercises() { return Collections.unmodifiableList(exerciseList); }

  public String toString() {
    return "Lesson '" + unit + "' " + exerciseList.size() + " exercises" +
        (exerciseList.isEmpty() ? "" :
        ", first is " + exerciseList.iterator().next());
  }
}
