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
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonShell;

import java.util.List;
import java.util.Set;

/**
 * Show exercises with a cell table that can handle thousands of rows.
 * Does tooltips using tooltip field on {@link CommonShell#tooltip}
 * <p/>
 * User: GO22670
 * Date: 11/27/12
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingExerciseList extends ExerciseList {
  protected final ExerciseController controller;
  protected PagingContainer pagingContainer;
  private final boolean showTypeAhead;

  private TextBox typeAhead = new TextBox();
  private String lastValue = "";
  private long userListID = -1;
  private int unaccountedForVertical = 160;

  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, boolean, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController)
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param controller
   * @param showTurkToken
   * @param showTypeAhead
   * @param instance
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            ExercisePanelFactory factory, ExerciseController controller, boolean showTurkToken, boolean showInOrder,
                            boolean showTypeAhead, String instance) {
    super(currentExerciseVPanel, service, feedback, factory, controller, showTurkToken, showInOrder, instance);
    this.controller = controller;
    this.showTypeAhead = showTypeAhead;
    addComponents();
    getElement().setId("PagingExerciseList_" + instance);
  }

  @Override
  protected Set<String> getKeys() {  return pagingContainer.getKeys();  }

  /**
   * @seex mitll.langtest.client.recorder.FeedbackRecordPanel#enableNext
   * @param completed
   */
  public void setCompleted(Set<String> completed) {
    pagingContainer.setCompleted(completed);
  }

  @Override
  public void removeCompleted(String id) {
    pagingContainer.removeCompleted(id);

  }

  public void addCompleted(String id) {
    pagingContainer.addCompleted(id);
    System.out.println("PagingExerciseList.addCompleted : completed " + id + " now " + getCompleted().size());
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
    PagingContainer exerciseShellPagingContainer = makePagingContainer();

    addTableWithPager(exerciseShellPagingContainer);
  }

  /**
   * @see #addTypeAhead(com.google.gwt.user.client.ui.Panel)
   * @param selectionState
   * @param prefix
   */
  void loadExercises(String selectionState, String prefix) {
    lastReqID++;
    long listID = userListID;
    System.out.println("PagingExerciseList.loadExercises : looking for '" + prefix + "' (" + prefix.length() + " chars) in list id "+listID);

    service.getExerciseIds(lastReqID, controller.getUser(), prefix, listID, new SetExercisesCallback());
  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises(java.util.Map, String)
   * @return
   */
  String getPrefix() { return /*typeAhead == null ? "" :*/ typeAhead.getText(); }

  @Override
  protected void loadFirstExercise() {
    super.loadFirstExercise();
   // typeAhead.setText("");
  }

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

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addComponents()
   * @return
   */
  protected PagingContainer makePagingContainer() {
    final PagingExerciseList outer = this;
    PagingContainer pagingContainer1 =
      new PagingContainer(controller, getVerticalUnaccountedFor()) {
      @Override
      protected void gotClickOnItem(CommonShell e) {  outer.gotClickOnItem(e);  }
    };
    pagingContainer = pagingContainer1;
    return pagingContainer1;
  }

  @Override
  protected CommonShell findFirstExercise() {
    if (controller.showCompleted()) {
      return getFirstNotCompleted();
    }
    else {
      return super.findFirstExercise();
    }
  }

  private CommonShell getFirstNotCompleted() {
    for (CommonShell es : pagingContainer.getExercises()) {
      if (!getCompleted().contains(es.getID())) return es;
    }
    return super.findFirstExercise();
  }

  /**
   * TODO : Not sure if this is needed anymore
   * @return
   */
  protected int getVerticalUnaccountedFor() {
    return unaccountedForVertical;
  }

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#FlexSectionExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, boolean, boolean, mitll.langtest.client.exercise.ExerciseController, boolean, String)
   * @param v
   */
  public void setUnaccountedForVertical(int v) {
    unaccountedForVertical = v;
    pagingContainer.setUnaccountedForVertical(v);
    //System.out.println("setUnaccountedForVertical : vert " + v + " for " +getElement().getId());
  }

  protected void addTableWithPager(PagingContainer pagingContainer) {
    Panel column = new FlowPanel();
    add(column);
    addTypeAhead(column);
    add(pagingContainer.getTableWithPager());
  }

  void addTypeAhead(Panel column) {
    if (showTypeAhead) {
      //typeAhead = new TextBox();
      typeAhead.getElement().setId("ExerciseList_TypeAhead");
      typeAhead.setDirectionEstimator(true);   // automatically detect whether text is RTL
      typeAhead.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          String text = typeAhead.getText();
          //  text = text.trim();
          if (!text.equals(lastValue)) {
            System.out.println("addTypeAhead : looking for '" + text + "' (" + text.length() + " chars)");
            controller.logEvent(typeAhead,"TypeAhead","UserList_"+userListID,"User search ='" +text+ "'");
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

  protected void showEmptySelection() {
    showPopup("No items match the selection and search.", "Try clearing one of your selections or changing the search.", typeAhead);
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

  void tellUserPanelIsBusy() {
    Window.alert("Please stop recording before changing items.");
  }

  String getHistoryToken(String id) { return "item=" +id; }

  void gotClickOnItem(final CommonShell e) {
    if (isExercisePanelBusy()) {
      tellUserPanelIsBusy();
      markCurrentExercise(pagingContainer.getCurrentSelection().getID());
    } else {
      controller.logEvent(this,"ExerciseList",e.getID(),"Clicked on item '" + e.getTooltip() +"'");

      pushNewItem(e.getID());
    }
  }

  public void clear() { pagingContainer.clear(); }

  public void flush() {
    pagingContainer.flush();
    onResize();
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList#rememberAndLoadFirst(java.util.List, mitll.langtest.shared.CommonExercise)
   * @param result
   */
  @Override
  protected void rememberExercises(List<CommonShell> result) {
    //System.out.println("PagingExerciseList : rememberAndLoadFirst remembering " + result.size());

    clear();
    for (CommonShell es : result) {
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
  protected CommonShell getFirst() {
    return pagingContainer.getFirst();
  }

  @Override
  public CommonShell byID(String name) {
    return pagingContainer.byID(name);
  }

  @Override
  public CommonShell getCurrentExercise() {
    return pagingContainer.getCurrentSelection();
  }

  @Override
  protected int getRealIndex(CommonShell t) {
    return pagingContainer.getIndex(t);
  }

  @Override
  protected CommonShell getAt(int i) {
    return pagingContainer.getAt(i);
  }

  @Override
  public void addExercise(CommonShell es) { pagingContainer.addExercise(es);  }
  public void addExerciseAfter(CommonShell after,CommonShell es) { pagingContainer.addExerciseAfter(after, es);  }

  public CommonShell forgetExercise(String id) {
    System.out.println("PagingExerciseList.forgetExercise " + id + " on " + getElement().getId() + " ul " +userListID);
    return removeExercise(byID(id));
  }

  /**
   * @see mitll.langtest.client.custom.NewUserExercise#afterValidForeignPhrase
   * @see mitll.langtest.client.list.ExerciseList#removeExercise
   * @param id
   * @return
   */
  @Override
  public CommonShell simpleRemove(String id) {
    CommonShell es = byID(id);
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
