/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.CommonExercise;

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
  private final List<CommonExercise> exerciseList = new ArrayList<CommonExercise>();
  private String unit;

  public Lesson(String unit) { this.unit = unit; }
  public void addExercise(CommonExercise e) { exerciseList.add(e); }
  public boolean remove(CommonExercise exercise) {
    return exerciseList.remove(exercise);
  }
  public Collection<CommonExercise> getExercises() { return Collections.unmodifiableList(exerciseList); }

  public String toString() {
    return "Lesson '" + unit + "' " + exerciseList.size() + " exercises" +
        (exerciseList.isEmpty() ? "" :
        ", first is " + exerciseList.iterator().next());
  }
}
