package mitll.langtest.shared.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.client.exercise.PagingContainer;

import java.util.Map;

/**
 * Created by go22670 on 1/4/16.
 */
public interface Shell extends HasID {
  /**
   * @see PagingContainer#getEnglishColumn()
   * @return
   */
  STATE getState();
  void setState(STATE state);

  /**
   * @see PagingContainer#getEnglishColumn()
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise(Button)
   * @return
   */
  STATE getSecondState();

  void setSecondState(STATE state);

  Map<String, String> getUnitToValue();

  /**
   * @see mitll.langtest.server.database.exercise.SectionHelper#addExerciseToLesson
   * @param unit
   * @param value
   */
  void addUnitToValue(String unit, String value);
}
