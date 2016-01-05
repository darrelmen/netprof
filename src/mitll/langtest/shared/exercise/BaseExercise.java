package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.custom.UserExercise;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 1/5/16.
 */
public class BaseExercise implements IsSerializable, Shell  {
  protected String id;
  protected Map<String, String> unitToValue = new HashMap<String, String>();
  private STATE state = STATE.UNSET;
  private STATE secondState = STATE.UNSET;

  public BaseExercise() {}
  public BaseExercise(String id ) { this.id = id;}

  public String getID() {
    return id;
  }

  public void setID(String id) {
    this.id = id;
  }

  @Override
  public STATE getState() {
    return state;
  }

  @Override
  public void setState(STATE state) {
    this.state = state;
  }

  @Override
  public STATE getSecondState() {
    return secondState;
  }

  @Override
  public void setSecondState(STATE state) {
    this.secondState = state;
  }

  public Map<String, String> getUnitToValue() { return unitToValue; }

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   * @param unit
   * @param value
   */
  public void addUnitToValue(String unit, String value) {
    if (value == null) return;
//    if (value.isEmpty()) {
//      System.out.println("addUnitToValue " + unit + " value " + value);
//    }
    this.getUnitToValue().put(unit, value);
  }

  /**
   * @see UserExercise#UserExercise(CommonExercise)
   * @param unitToValue
   */
  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }
}
