package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Map;

/**
 * Created by go22670 on 1/4/16.
 */
public interface Shell extends IsSerializable {
  String getID();
  STATE getState();
  void setState(STATE state);

  STATE getSecondState();

  void setSecondState(STATE state);

  Map<String, String> getUnitToValue();

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson(CommonExercise, String, String)
   * @param unit
   * @param value
   */
  void addUnitToValue(String unit, String value);
}
