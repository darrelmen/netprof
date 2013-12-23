package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.base.TextBox;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

import java.util.Set;

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
  private Set<String> completed;
  boolean showCompleted = false;
  boolean showTypeAhead = true;
  protected TextBox typeAhead = null;
  private String lastValue = "";

  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, boolean, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController)
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param controller
   * @param instance
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            boolean showTurkToken, boolean showInOrder, ExerciseController controller, String instance) {
    super(currentExerciseVPanel, service, feedback, null, controller, showTurkToken, showInOrder, instance);
    this.controller = controller;
    this.showCompleted = controller.getProps().isCRTDataCollectMode() || controller.getProps().isDataCollectMode();
    if (controller.getProps().isCRTDataCollectMode()) showTypeAhead = false;
    addComponents();
  }

  /**
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#enableNext()
   * @param completed
   */
  public void setCompleted(Set<String> completed) {
    this.completed = completed;
    pagingContainer.setCompleted(completed);
  }

  public void addCompleted(String id) {
    completed.add(id);
    pagingContainer.setCompleted(completed);
    controller.showProgress();
    System.out.println("addCompleted : completed " + completed.size() + " current " + currentExercises.size() + " " + id);
  }

  @Override
  public int getPercentComplete() {
    if (showCompleted) {
      int i = (int) Math.ceil(100f * ((float) completed.size() / (float) currentExercises.size()));
      if (i > 100) i = 100;
      System.out.println("getPercentComplete : completed " + completed.size() + " current " + currentExercises.size() + " " + i);
      return i;
    } else {
      return super.getPercentComplete();
    }
  }

  public int getComplete() {
    if (showCompleted) {
      return completed.size();
    } else {
      return super.getComplete();
    }
  }

  protected void onLastItem() {
    PropertyHandler props = controller.getProps();
    if (props.isCRTDataCollectMode() || props.isDataCollectMode() && isComplete()) {
      String title = props.isDataCollectMode() ? "Collection Complete" : "Test complete";
      feedback.showErrorMessage(title, "All Items Complete! Thank you!");
    }
    else {
      loadFirstExercise();
    }
  }

  @Override
  protected boolean isComplete() {
    return completed.size() == currentExercises.size();
  }

  /**
   * Add two rows -- the search box and then the item list
   */
  protected void addComponents() {
    PagingContainer<? extends ExerciseShell> exerciseShellPagingContainer = makePagingContainer();

    addTableWithPager(exerciseShellPagingContainer);
  }

  protected void addTypeAhead(FlowPanel column) {
    if (showTypeAhead) {
      typeAhead = new TextBox();
      typeAhead.setDirectionEstimator(true);   // automatically detect whether text is RTL
      typeAhead.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          String text = typeAhead.getText();
          //  text = text.trim();
          if (!text.equals(lastValue)) {
            System.out.println("looking for '" + text + "' (" + text.length() + " chars)");
            loadExercises(getHistoryToken(""), text);
            lastValue = text;
          }
        }
      });

      addControlGroupEntry(column, "Search", typeAhead);
      Scheduler.get().scheduleDeferred(new Command() {
        public void execute() {
          typeAhead.setFocus(true);
        }
      });
    }
  }

  protected void loadExercises(String selectionState, String prefix) {
    lastReqID++;
    System.out.println("loadExercises : looking for '" + prefix + "' (" + prefix.length() + " chars)");

    service.getExerciseIds(lastReqID, controller.getUser(), prefix, new SetExercisesCallback());
  }

  protected String getPrefix() { return typeAhead == null ? "" : typeAhead.getText(); }

  private ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget user) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");

    Controls controls = new Controls();
    userGroup.add(new ControlLabel(label));
    controls.add(user);
    userGroup.add(controls);

    dialogBox.add(userGroup);
    return userGroup;
  }


  protected PagingContainer<? extends ExerciseShell> makePagingContainer() {
    final PagingExerciseList outer = this;
    boolean markComplete = controller.getProps().isCRTDataCollectMode() || controller.getProps().isDataCollectMode();
    PagingContainer<ExerciseShell> pagingContainer1 = new PagingContainer<ExerciseShell>(controller, getVerticalUnaccountedFor(), markComplete) {
      @Override
      protected void gotClickOnItem(ExerciseShell e) {
        outer.gotClickOnItem(e);
      }
    };
    pagingContainer = pagingContainer1;
    return pagingContainer1;
  }

  @Override
  protected ExerciseShell findFirstExercise() {
    if (showCompleted) {
      System.out.println("getFirstNotCompleted : ");

      return getFirstNotCompleted();
    }
    else {
      return super.findFirstExercise();
    }
  }

  private ExerciseShell getFirstNotCompleted() {
    for (ExerciseShell es : currentExercises) {

      if (!completed.contains(es.getID())) return es;
    }
    return super.findFirstExercise();
  }

  protected int getVerticalUnaccountedFor() {
    return 100;
  }

  protected void addTableWithPager(PagingContainer<? extends ExerciseShell> pagingContainer) {
    Panel container = pagingContainer.getTableWithPager();
    FlowPanel column = new FlowPanel();
    add(column);
    addTypeAhead(column);
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
