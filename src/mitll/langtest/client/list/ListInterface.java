package mitll.langtest.client.list;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ListInterface<T extends ExerciseShell> extends RequiresResize {
  /**
   * @see mitll.langtest.client.LangTest#setFactory
   * @param factory
   * @param user
   * @param expectedGrades
   */
  void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades);

  void rememberAndLoadFirst(List<T> exercises, Exercise firstExercise);

    /**
     * @see mitll.langtest.client.LangTest#gotUser(long)
     * @see mitll.langtest.client.LangTest#makeFlashContainer()
     * @param userID
     * @param getNext
     */
  void getExercises(long userID, boolean getNext);

  void reloadWith(String id);

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

  T byID(String name);

  void loadExercise(String itemID);

  boolean loadNext();

  /**
   *
   * @param current
   * @return
   * @see ListInterface#loadNextExercise
   */
  <S extends ExerciseShell> boolean loadNextExercise(S current);

  boolean loadNextExercise(String id);
  Panel makeExercisePanel(Exercise result);

  boolean loadPrev();

  <S extends ExerciseShell> boolean loadPreviousExercise(S current);

  void checkAndAskServer(String id);

  public String getCurrentExerciseID();

  boolean onFirst();
  <S extends ExerciseShell> boolean onFirst(S current);

  boolean onLast();
  <S extends ExerciseShell> boolean onLast(S current);
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
  void setCompleted(Set<String> completed);
  void addCompleted(String id);

  void addExercise(T es);

  T simpleRemove(String id);

  void hideExerciseList();

  Panel getCreatedPanel();
  void reload();

  void redraw();

  void addListChangedListener(ListChangeListener<T> listener);
}
