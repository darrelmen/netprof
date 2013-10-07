package mitll.langtest.client.exercise;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

/**
 * Show exercises with a cell table that can handle thousands of rows.
 * Does tooltips using tooltip field on {@link ExerciseShell#tooltip}
 * <p/>
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingExerciseList extends ExerciseList implements RequiresResize {
  protected ExerciseController controller;
  protected PagingContainer<? extends ExerciseShell> pagingContainer;

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param controller
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            boolean showTurkToken, boolean showInOrder, ExerciseController controller) {
    super(currentExerciseVPanel, service, feedback, null, showTurkToken, showInOrder);
    this.controller = controller;
    addComponents();
  }

  protected void addComponents() {
    PagingContainer<? extends ExerciseShell> exerciseShellPagingContainer = makePagingContainer();
    addTableWithPager(exerciseShellPagingContainer);
  }

  protected PagingContainer<? extends ExerciseShell> makePagingContainer() {
    final PagingExerciseList outer = this;
    PagingContainer<ExerciseShell> pagingContainer1 = new PagingContainer<ExerciseShell>(controller) {
      @Override
      protected void gotClickOnItem(ExerciseShell e) {
        outer.gotClickOnItem(e);
      }

      @Override
      protected void loadFirstExercise() {
        outer.loadFirstExercise();

        selectFirst();    //To change body of overridden methods use File | Settings | File Templates.
      }
    };
    pagingContainer = pagingContainer1;
    return pagingContainer1;
  }

  protected void addTableWithPager(PagingContainer<? extends ExerciseShell> pagingContainer) {
    Panel container = pagingContainer.getTableWithPager();
    add(container);
  }

  protected void tellUserPanelIsBusy() {
    Window.alert("Please stop recording before changing items.");
  }

  protected String getHistoryToken(String id) { return "item=" +id; }

  protected void gotClickOnItem(final ExerciseShell e) {
    if (isExercisePanelBusy()) {
      tellUserPanelIsBusy();
      markCurrentExercise(currentExercise);
    } else {
      pushNewItem(e.getID());
    }
  }

  public void clear() {
    pagingContainer.clear();
  }

  @Override
  public void flush() {
    pagingContainer.flush();
  }

  @Override
  protected void addExerciseToList(ExerciseShell exercise) {
    pagingContainer.addExerciseToList2(exercise);
  }

  @Override
  public void onResize() {
    super.onResize();
    pagingContainer.onResize(currentExercise);
  }

  /**
   * not sure how this happens, but need Math.max(0,...)
   *
   * @see ExerciseList#useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param i
   */
 @Override
  protected void markCurrentExercise(int i) {
   pagingContainer.markCurrentExercise(i);
 }
}
