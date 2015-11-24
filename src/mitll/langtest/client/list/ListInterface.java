/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.list;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.dialog.PrevNextList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;
import mitll.langtest.shared.custom.UserList;

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
  /**
   * @see mitll.langtest.client.custom.content.AVPHelper#makeExerciseList(Panel, String)
   * @param factory
   */
  void setFactory(ExercisePanelFactory factory);

    /**
     * @see mitll.langtest.client.LangTest#gotUser
     * @see mitll.langtest.client.list.HistoryExerciseList#noSectionsGetExercises(long)
     * @param userID
     */
  boolean getExercises(long userID);

  /**
   * @see mitll.langtest.client.custom.dialog.EditableExercise#doAfterEditComplete(ListInterface, boolean)
   * @param id
   */
  void reloadWith(String id);

  /**
   * @see mitll.langtest.client.custom.content.NPFHelper#doInternalLayout(UserList, String)
   * @param props
   * @return
   */
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

  /**
   * @see Navigation#showLearnAndItem(String)
   * @param id
   * @return
   */
  boolean loadByID(String id);

  String getCurrentExerciseID();

  /**
   * @see PrevNextList#clickNext()
   * @return
   */
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
   * @see mitll.langtest.client.custom.content.NPFHelper#makeExerciseList(com.google.gwt.user.client.ui.Panel, String)
   */
  void reloadExercises();
  void addExercise(CommonShell es);

  CommonShell simpleRemove(String id);

  int getSize();

  /**
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#getAnswerWidget(CommonExercise, LangTestDatabaseAsync, ExerciseController, boolean, String)
   * @return
   */
  boolean isPendingReq();

  void hide();

  /**
   * @see Navigation#getNav()
   * @return
   */
  Panel getCreatedPanel();
  void reload();

  void redraw();
  void setState(String id, STATE state);
  void setSecondState(String id, STATE state);

  void addListChangedListener(ListChangeListener<CommonShell> listener);

  void setShuffle(boolean doShuffle);
  void simpleSetShuffle(boolean doShuffle);

  void reload(Map<String, Collection<String>> typeToSection);

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#makeClickableText(String, String, String, boolean)
   * @param text
   */
  void searchBoxEntry(String text);
}
