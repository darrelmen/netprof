package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 1/30/13
 * Time: 7:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class Lesson implements IsSerializable {
  private List<Exercise> exerciseList = new ArrayList<Exercise>();
  public long timestamp;
  public String unit;
  public String chapter;
  public String week;

  public Lesson(){}
  public Lesson(String unit, String chapter, String week) { this.unit = unit; this.chapter = chapter; this.week = week; }
  public void addExercise(Exercise e) { exerciseList.add(e); }

  public String toString() {
    return "Lesson '" + unit + "/" + chapter + "/" + week + "' " + exerciseList.size() + " exercises" +
        (exerciseList.isEmpty() ? "" :
        ", first is " + exerciseList.iterator().next());
  }
}
