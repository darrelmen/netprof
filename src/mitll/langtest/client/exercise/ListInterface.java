package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ListInterface extends RequiresResize {
  void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades);

  void getExercises(long userID);

  void getExercisesInOrder();

  void setExercise_title(String exercise_title);

  void clear();

  Widget getWidget();


  boolean loadNextExercise(ExerciseShell current);

  boolean loadPreviousExercise(ExerciseShell current);

  boolean onFirst(ExerciseShell current);

  void removeCurrentExercise();
}
