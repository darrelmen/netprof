package mitll.langtest.client.list;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ListInterface extends RequiresResize {
  void removeHistoryListener();

  /**
   * @see mitll.langtest.client.LangTest#reallySetFactory()
   * @param factory
   */
  void setFactory(ExercisePanelFactory factory);

    /**
     * @see mitll.langtest.client.LangTest#gotUser(long)
     * @see mitll.langtest.client.LangTest#makeFlashContainer()
     * @param userID
     */
  boolean getExercises(long userID);

  void reloadWith(String id);

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

  String getCurrentExerciseID();

  boolean onFirst();
  boolean onFirst(CommonShell current);

  boolean onLast();
  boolean onLast(CommonShell current);
  /**
   * @see mitll.langtest.client.LangTest#resetState()
   */
  void clear();

  int getComplete();

  /**
   * @see mitll.langtest.client.LangTest#resetState()
   */
  void removeCurrentExercise();

  /**
   * @see mitll.langtest.client.custom.content.NPFHelper#makeExerciseList(com.google.gwt.user.client.ui.Panel, String)
   */
  void reloadExercises();
  void addExercise(CommonShell es);

  CommonShell simpleRemove(String id);

  int getSize();

  boolean isPendingReq();

  void hide();
  void show();
  Panel getCreatedPanel();
  void reload();

  void redraw();
  void setState(String id, STATE state);
  void setSecondState(String id, STATE state);

  void addListChangedListener(ListChangeListener<CommonShell> listener);

  void setInstance(String instance);
  void setShuffle(boolean doShuffle);
  void simpleSetShuffle(boolean doShuffle);

  void reload(Map<String, Collection<String>> typeToSection);

  void searchBoxEntry(String text);
}
