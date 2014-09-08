package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.base.TextBox;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Show exercises with a cell table that can handle thousands of rows.
 * Does tooltips using tooltip field on {@link CommonShell#getTooltip}
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
  private String lastTypeAheadValue = "";
  protected long userListID = -1;
  private int unaccountedForVertical = 160;
  private boolean unrecorded;
  private boolean onlyExamples;

  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param controller
   * @param showTypeAhead
   * @param instance
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            ExercisePanelFactory factory, ExerciseController controller,
                            boolean showTypeAhead, String instance) {
    super(currentExerciseVPanel, service, feedback, factory, controller, instance);
    this.controller = controller;
    this.showTypeAhead = showTypeAhead;
    addComponents();
    getElement().setId("PagingExerciseList_" + instance);
  }

  @Override
  protected Set<String> getKeys() {  return pagingContainer.getKeys();  }

  @Override
  public void setState(String id, STATE state) {
    byID(id).setState(state);
  }

  @Override
  public void setSecondState(String id, STATE state) {
    byID(id).setSecondState(state);
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
    scheduleWaitTimer();

    lastReqID++;
    System.out.println("PagingExerciseList.loadExercises : looking for " +
      "'" + prefix + "' (" + prefix.length() + " chars) in list id "+userListID + " instance " + getInstance());
    service.getExerciseIds(lastReqID, new HashMap<String, Collection<String>>(), prefix, userListID,
      controller.getUser(), getRole(), getUnrecorded(), isOnlyExamples(), new SetExercisesCallback(""));
  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises(java.util.Map, String)
   * @return
   */
  protected String getPrefix() { return typeAhead.getText(); }

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
     //System.out.println("findFirstExercise : completed " + controller.showCompleted());

    return controller.showCompleted() ? getFirstNotCompleted() : super.findFirstExercise();
  }

  private CommonShell getFirstNotCompleted() {
    for (CommonShell es : pagingContainer.getExercises()) {
      STATE state = es.getState();
      if (state != null && state.equals(STATE.UNSET)) {
        return es;
      }
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
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#FlexSectionExerciseList(com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, boolean, boolean, mitll.langtest.client.exercise.ExerciseController, boolean, String)
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

  private Timer waitTimer = null;
  private SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");
  private SafeUri white = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "white_32x32.png");
  private final com.github.gwtbootstrap.client.ui.Image waitCursor = new com.github.gwtbootstrap.client.ui.Image(white);

  /**
   * Show wait cursor if the type ahead takes too long.
   *
   * @param column
   */
  protected void addTypeAhead(Panel column) {
    if (showTypeAhead) {
      typeAhead.getElement().setId("ExerciseList_TypeAhead");
      typeAhead.setDirectionEstimator(true);   // automatically detect whether text is RTL
      typeAhead.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          String text = typeAhead.getText();
          if (!text.equals(lastTypeAheadValue)) {
            //System.out.println("addTypeAhead : looking for '" + text + "' (" + text.length() + " chars)");
            controller.logEvent(typeAhead, "TypeAhead", "UserList_" + userListID, "User search ='" + text + "'");
            loadExercises(getHistoryToken(""), text);
            lastTypeAheadValue = text;
          }
        }
      });

      Panel flow = new HorizontalPanel();
      flow.add(typeAhead);
      flow.add(waitCursor);
      waitCursor.getElement().getStyle().setMarginTop(-7, Style.Unit.PX);
      waitCursor.setUrl(white);

      addControlGroupEntry(column, "Search", flow);
      Scheduler.get().scheduleDeferred(new Command() {
        public void execute() {
          typeAhead.setFocus(true);
        }
      });
    }
  }

  protected void scheduleWaitTimer() {
    if (waitTimer != null) {
      waitTimer.cancel();
    }
    waitTimer = new Timer() {
      @Override
      public void run() {
        waitCursor.setUrl(animated);
      }
    };
    waitTimer.schedule(700);
  }

  protected String getTypeAheadText() { return typeAhead.getText(); }

  @Override
  protected void gotExercises(boolean success) {
//    long now = System.currentTimeMillis();
    // System.out.println("took " + (now - then) + " millis");
    if (waitTimer != null) {
      waitTimer.cancel();
    }
    waitCursor.setUrl(white);
  }

  protected void showEmptySelection() {
    showPopup("No items match the selection and search.", "Try clearing one of your selections or changing the search.", typeAhead);
    createdPanel = new SimplePanel();
    createdPanel.getElement().setId("placeHolderWhenNoExercises");
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

  /**
   * @deprecated
   */
  void tellUserPanelIsBusy() {
    Window.alert("Please stop recording before changing items.");
  }

  String getHistoryToken(String id) { return "item=" +id; }

  void gotClickOnItem(final CommonShell e) {
    if (isExercisePanelBusy()) {
      tellUserPanelIsBusy();
      markCurrentExercise(pagingContainer.getCurrentSelection().getID());
    } else {
      controller.logEvent(this, "ExerciseList", e.getID(), "Clicked on item '" + e.getTooltip() + "'");

      pushNewItem(e.getID());
    }
  }

  public void clear() { pagingContainer.clear(); }

  public void flush() {
    pagingContainer.flush();
    onResize();
  }

  private List<CommonShell> inOrderResult;

  /**
   * A little complicated -- if {@link #doShuffle} is true, shuffles the exercises
   *
   * @see ExerciseList#rememberAndLoadFirst(java.util.List, mitll.langtest.shared.CommonExercise, String)
   * @param result
   * @see #simpleSetShuffle(boolean)
   */
  @Override
  protected List<CommonShell> rememberExercises(List<CommonShell> result) {
    inOrderResult = result;
    if (doShuffle) {
      System.out.println(getInstance() +" : rememberExercises - shuffling " + result.size() + " items");

      result = new ArrayList<CommonShell>(result);
      Shuffler.shuffle(result);
    }
    clear();
    int c = 0;
    for (CommonShell es : result) {
      addExercise(es);
    }
    flush();
    return result;
  }

  @Override
  protected List<CommonShell> getInOrder() { return inOrderResult; }

  @Override
  public int getSize() {  return pagingContainer.getSize();  }

  @Override
  protected boolean isEmpty() {
    return pagingContainer.isEmpty();
  }

  @Override
  protected CommonShell getFirst() {
    return pagingContainer.getFirst();
  }

  @Override
  public CommonShell byID(String name) {  return pagingContainer.byID(name);  }

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
  public void addExercise(CommonShell es) {
/*    if (pagingContainer.getSize() < 5) {
      System.out.println(getInstance() +" : addExercise adding " + es.getID() + " state " + es.getState() + "/" + es.getSecondState());
    }*/
    pagingContainer.addExercise(es);
  }
  public void addExerciseAfter(CommonShell after,CommonShell es) { pagingContainer.addExerciseAfter(after, es);  }

  public CommonShell forgetExercise(String id) {
   // System.out.println("PagingExerciseList.forgetExercise " + id + " on " + getElement().getId() + " ul " +userListID);
    CommonShell es = byID(id);
    if (es != null) {
      return removeExercise(es);
    }
    else {
      return null;
    }
  }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
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

  /**
   * @see #useExercise(mitll.langtest.shared.CommonExercise)
   * @param itemID
   */
  protected void markCurrentExercise(String itemID) { pagingContainer.markCurrentExercise(itemID); }

  public void setUserListID(long userListID) {
    this.userListID = userListID;
  }

  public void redraw() {
    pagingContainer.flush();
    pagingContainer.redraw();
  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercisesUsingPrefix(java.util.Map, String)
   * @return
   */
  public boolean getUnrecorded() { return unrecorded;  }

  /**
   * @see mitll.langtest.client.custom.Navigation.RecorderNPFHelper#getMyListLayout(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.custom.SimpleChapterNPFHelper)
   * @param unrecorded
   */
  public void setUnrecorded(boolean unrecorded) { this.unrecorded = unrecorded;  }

  public boolean isOnlyExamples() { return onlyExamples;  }
  public void setOnlyExamples(boolean onlyExamples) {  this.onlyExamples = onlyExamples;  }
}
