package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.base.TextBox;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

import java.util.List;
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
public class PagingExerciseList<T extends ExerciseShell> extends ExerciseList<T> implements RequiresResize {
  protected ExerciseController controller;
  protected PagingContainer<T> pagingContainer;
  private boolean showTypeAhead;

  private TextBox typeAhead = null;
  private String lastValue = "";
  private long userListID = -1;
  private int unaccountedForVertical = 160;

  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, boolean, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController)
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param controller
   * @param showTypeAhead
   * @param instance
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            boolean showTurkToken, boolean showInOrder, ExerciseController controller,
                            boolean showTypeAhead, String instance) {
    super(currentExerciseVPanel, service, feedback, null, controller, showTurkToken, showInOrder, instance);
    this.controller = controller;
    this.showTypeAhead = showTypeAhead;
    addComponents();
    getElement().setId("PagingExerciseList_" + instance);
  }

  @Override
  protected Set<String> getKeys() {  return pagingContainer.getKeys();  }

  /**
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#enableNext()
   * @param completed
   */
  public void setCompleted(Set<String> completed) {
    pagingContainer.setCompleted(completed);
  }

  public void addCompleted(String id) {
    pagingContainer.addCompleted(id);
    //System.out.println("PagingExerciseList.addCompleted : completed " + id + " now " + getCompleted().size());
  }

  private Set<String> getCompleted() { return pagingContainer.getCompleted(); }

  @Override
  public int getPercentComplete() {
    if (controller.showCompleted()) {
      int i = (int) Math.ceil(100f * ((float) getCompleted().size() / (float) getSize()));
      if (i > 100) i = 100;
      System.out.println("completed " + getCompleted().size() + " current " + getSize() + " " + i);
      return i;
    } else {
      return super.getPercentComplete();
    }
  }

  public int getComplete() {
    if (controller.showCompleted()) {
      return getCompleted().size();
    } else {
      return super.getComplete();
    }
  }

  /**
   * Add two rows -- the search box and then the item list
   */
  protected void addComponents() {
    PagingContainer<? extends ExerciseShell> exerciseShellPagingContainer = makePagingContainer();

    addTableWithPager(exerciseShellPagingContainer);
  }

  /**
   * @see #addTypeAhead(com.google.gwt.user.client.ui.Panel)
   * @param selectionState
   * @param prefix
   */
  protected void loadExercises(String selectionState, String prefix) {
    lastReqID++;
    long listID = userListID;
    System.out.println("PagingExerciseList.loadExercises : looking for '" + prefix + "' (" + prefix.length() + " chars) in list id "+listID);

    service.getExerciseIds(lastReqID, controller.getUser(), prefix, listID, new SetExercisesCallback());
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

  protected PagingContainer<T> makePagingContainer() {
    final PagingExerciseList<T> outer = this;
    PagingContainer<T> pagingContainer1 =
      new PagingContainer<T>(controller, getVerticalUnaccountedFor()) {
      @Override
      protected void gotClickOnItem(ExerciseShell e) {  outer.gotClickOnItem(e);  }
    };
    pagingContainer = pagingContainer1;
    return pagingContainer1;
  }

  @Override
  protected T findFirstExercise() {
    if (controller.showCompleted()) {
      return getFirstNotCompleted();
    }
    else {
      return super.findFirstExercise();
    }
  }

  private T getFirstNotCompleted() {
    for (T es : pagingContainer.getExercises()) {
      if (!getCompleted().contains(es.getID())) return es;
    }
    return super.findFirstExercise();
  }

  protected int getVerticalUnaccountedFor() {
    return unaccountedForVertical;
  }

  public void setUnaccountedForVertical(int v) {
    unaccountedForVertical = v;
    pagingContainer.setUnaccountedForVertical(v);
  }

  protected void addTableWithPager(PagingContainer<? extends ExerciseShell> pagingContainer) {
    Panel container = pagingContainer.getTableWithPager();
    FlowPanel column = new FlowPanel();
    add(column);
    addTypeAhead(column);
    add(container);
  }

  protected void addTypeAhead(Panel column) {
    if (showTypeAhead) {
      typeAhead = new TextBox();
      typeAhead.setDirectionEstimator(true);   // automatically detect whether text is RTL
      typeAhead.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          String text = typeAhead.getText();
          //  text = text.trim();
          if (!text.equals(lastValue)) {
            System.out.println("addTypeAhead : looking for '" + text + "' (" + text.length() + " chars)");
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

  public void showEmptySelection() {
    showPopup("No items match the selection and search.","Try clearing one of your selections or changing the search.",typeAhead);
  }

  private void showPopup(String toShow,String toShow2, Widget over) {
    final PopupPanel popupImage = new PopupPanel(true);
    VerticalPanel vp = new VerticalPanel();
    vp.add(new HTML(toShow));
    vp.add(new HTML(toShow2));
    popupImage.add(vp);
    popupImage.showRelativeTo(over);
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }

  protected void tellUserPanelIsBusy() {
    Window.alert("Please stop recording before changing items.");
  }

  protected String getHistoryToken(String id) { return "item=" +id; }

  protected void gotClickOnItem(final ExerciseShell e) {
    if (isExercisePanelBusy()) {
      tellUserPanelIsBusy();
      markCurrentExercise(pagingContainer.getCurrentSelection().getID());
    } else {
      pushNewItem(e.getID());
    }
  }

  public void clear() { pagingContainer.clear(); }

  public void flush() { pagingContainer.flush();  }

  /**
   * @see mitll.langtest.client.list.ExerciseList#rememberAndLoadFirst(java.util.List, mitll.langtest.shared.Exercise)
   * @param result
   */
  @Override
  protected void rememberExercises(List<T> result) {
    //System.out.println("PagingExerciseList : rememberAndLoadFirst remembering " + result.size());

    clear();
    for (final T es : result) {
      addExercise(es);
    }
    flush();
    //System.out.println("PagingExerciseList : size " + getSize());
  }

  @Override
  protected int getSize() {
    return pagingContainer.getSize();
  }

  @Override
  protected boolean isEmpty() {
    return pagingContainer.isEmpty();
  }

  @Override
  protected T getFirst() {
    return pagingContainer.getFirst();
  }

  @Override
  public T byID(String name) {
    return pagingContainer.byID(name);
  }

  @Override
  protected T getCurrentExercise() {
    return pagingContainer.getCurrentSelection();
  }

  @Override
  protected int getRealIndex(T t) {
    return pagingContainer.getIndex(t);
  }

  @Override
  protected T getAt(int i) {
    return pagingContainer.getAt(i);
  }

  @Override
  public void addExercise(T es) { pagingContainer.addExercise(es);  }

  public void forgetExercise(String id) {
    System.out.println("PagingExerciseList.forgetExercise " + id + " on " + getElement().getId() + " ul " +userListID);
    removeExercise(byID(id));
  }

  /**
   * @see mitll.langtest.client.custom.NewUserExercise#afterValidForeignPhrase(mitll.langtest.shared.custom.UserList, ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
   * @see mitll.langtest.client.list.ExerciseList#removeExercise(mitll.langtest.shared.ExerciseShell)
   * @param id
   * @return
   */
  @Override
  public T simpleRemove(String id) {
    T es = byID(id);
    pagingContainer.forgetExercise(es);

    if (isEmpty()) {
      removeCurrentExercise();
    }
    return es;
  }

  @Override
  public void onResize() {
    super.onResize();

    pagingContainer.onResize(getCurrentExercise());
  }

  protected void markCurrentExercise(String itemID) { pagingContainer.markCurrentExercise(itemID); }

  public void setUserListID(long userListID) {
    this.userListID = userListID;
  }

  public void redraw() {
    pagingContainer.flush();
    pagingContainer.redraw();
  }
}
