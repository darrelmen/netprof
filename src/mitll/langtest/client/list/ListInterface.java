package mitll.langtest.client.list;

import java.util.List;
import java.util.Set;

import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserManager;
<<<<<<< HEAD
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;
=======
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162

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
     */
  boolean getExercises(long userID);

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

<<<<<<< HEAD
  boolean loadPrev();
=======
  Panel makeExercisePanel(Exercise result);
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162

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

  /**
   * @see mitll.langtest.client.custom.NPFHelper#makeExerciseList(com.google.gwt.user.client.ui.Panel, String)
   */
  void reloadExercises();
  void addExercise(CommonShell es);

  CommonShell simpleRemove(String id);

  void hide();
  void show();
  Panel getCreatedPanel();
  void reload();

<<<<<<< HEAD
  void redraw();
  void setState(String id, STATE state);
  void setSecondState(String id, STATE state);

  void addListChangedListener(ListChangeListener<CommonShell> listener);

  void setInstance(String instance);
  void setShuffle(boolean doShuffle);
  void simpleSetShuffle(boolean doShuffle);
=======
  void hideExerciseList();
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
}
