package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ListInterface extends RequiresResize {
  /**
   * @see mitll.langtest.client.LangTest#setFactory
   * @param factory
   * @param user
   * @param expectedGrades
   */
  void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades);
  ExercisePanelFactory getFactory();

  void rememberAndLoadFirst(List<ExerciseShell> exercises);

    /**
     * @see mitll.langtest.client.LangTest#gotUser(long)
     * @see mitll.langtest.client.LangTest#makeFlashContainer()
     * @param userID
     */
  void getExercises(long userID);

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param exercise_title
   */
  void setExercise_title(String exercise_title);


  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @return
   */
  Widget getWidget();

  Widget getExerciseListOnLeftSide(PropertyHandler props);

  /**
   * @param current
   * @return
   * @see mitll.langtest.client.LangTest#loadNextExercise
   */
  boolean loadNextExercise(ExerciseShell current);

  boolean loadNextExercise(String id);
  boolean isLastExercise(String id);
  void startOver();

  boolean loadPreviousExercise(ExerciseShell current);

  public String getCurrentExerciseID();

  boolean onFirst(ExerciseShell current);

  /**
   * @see mitll.langtest.client.LangTest#resetState()
   */
  void clear();
  /**
   * @see mitll.langtest.client.LangTest#resetState()
   */
  void removeCurrentExercise();

  void reloadExercises();

  int getPercentComplete();

  int getComplete();

  void addAdHocExercise(String label);

  void setSelectionState(Map<String,Collection<String>> selectionState);

  void hideExerciseList();

  void showExerciseList();

}
