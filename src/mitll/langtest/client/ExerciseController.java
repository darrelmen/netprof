package mitll.langtest.client;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/9/12
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseController {
  boolean loadNextExercise(Exercise current);

  boolean loadPreviousExercise(Exercise current);

  boolean onFirst(Exercise current);

  void showRecorder(Exercise exercise, String question, Widget sender);
}
