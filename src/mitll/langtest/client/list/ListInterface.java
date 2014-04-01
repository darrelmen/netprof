package mitll.langtest.client.list;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;

import java.util.List;
import java.util.Set;

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

  CommonShell byID(String name);

  void loadExercise(String itemID);

  boolean loadNext();

  /**
   *
   * @param current
   * @return
   * @see ListInterface#loadNextExercise
   */
 boolean loadNextExercise(CommonShell current);

  boolean loadNextExercise(String id);

  boolean loadPrev();

  boolean loadPreviousExercise(CommonShell current);

  void checkAndAskServer(String id);

  public String getCurrentExerciseID();

  boolean onFirst();
  boolean onFirst(CommonShell current);

  boolean onLast();
  boolean onLast(CommonShell current);
  /**
   * @see mitll.langtest.client.LangTest#resetState()
   */
  void clear();
  /**
   * @see mitll.langtest.client.LangTest#resetState()
   */
  void removeCurrentExercise();

  void reloadExercises();

/*  int getPercentComplete();

  int getComplete();
  void setCompleted(Set<String> completed);
  void addCompleted(String id);
  void removeCompleted(String id);*/

  void addExercise(CommonShell es);

  CommonShell simpleRemove(String id);

  void hide();
  void show();
  Panel getCreatedPanel();
  void reload();

  void redraw();

  void addListChangedListener(ListChangeListener<CommonShell> listener);
}
