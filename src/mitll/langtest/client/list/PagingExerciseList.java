package mitll.langtest.client.list;

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PopupHelper;
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
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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
  private Logger logger = Logger.getLogger("PagingExerciseList");

  protected final ExerciseController controller;
  protected PagingContainer pagingContainer;
  private final boolean showTypeAhead;

  private TypeAhead typeAhead;
  protected long userListID = -1;
  private int unaccountedForVertical = 160;
  private boolean unrecorded;
  private boolean onlyExamples;

  /**
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param controller
   * @param showTypeAhead
   * @param instance
   * @param incorrectFirst
   */
  public PagingExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            ExercisePanelFactory factory, ExerciseController controller,
                            boolean showTypeAhead, String instance, boolean incorrectFirst) {
    super(currentExerciseVPanel, service, feedback, factory, controller, instance, incorrectFirst);
    this.controller = controller;
    this.showTypeAhead = showTypeAhead;
    addComponents();
    getElement().setId("PagingExerciseList_" + instance);
  }

  @Override
  protected Set<String> getKeys() {
    return pagingContainer.getKeys();
  }

  @Override
  public void setState(String id, STATE state) {
    byID(id).setState(state);
  }

  @Override
  public void setSecondState(String id, STATE state) {
    byID(id).setSecondState(state);
  }

  @Override
  public void reload(Map<String, Collection<String>> typeToSection) {

  }

  /**
   * Add two rows -- the search box and then the item list
   */
  protected void addComponents() {
    PagingContainer exerciseShellPagingContainer = makePagingContainer();

    addTableWithPager(exerciseShellPagingContainer);
  }

  /**
   * @param selectionState
   * @param prefix
   * @param onlyWithAudioAnno
   * @see #addTypeAhead(com.google.gwt.user.client.ui.Panel)
   */
  void loadExercises(String selectionState, String prefix, boolean onlyWithAudioAnno) {
    scheduleWaitTimer();

    lastReqID++;
    logger.info("PagingExerciseList.loadExercises : looking for " +
        "'" + prefix + "' (" + prefix.length() + " chars) in list id " + userListID + " instance " + getInstance());
    service.getExerciseIds(lastReqID, new HashMap<String, Collection<String>>(), prefix, userListID,
        controller.getUser(), getRole(), getUnrecorded(), isOnlyExamples(), incorrectFirstOrder, false, new SetExercisesCallback(""));
  }

  /**
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises(java.util.Map, String)
   */
  protected String getPrefix() {
    return typeAhead.getText();
  }

  /**
   * @return
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addComponents()
   */
  protected PagingContainer makePagingContainer() {
    final PagingExerciseList outer = this;
    PagingContainer pagingContainer1 =
        new PagingContainer(controller, getVerticalUnaccountedFor()) {
          @Override
          protected void gotClickOnItem(CommonShell e) {
            outer.gotClickOnItem(e);
          }
        };
    pagingContainer = pagingContainer1;
    return pagingContainer1;
  }

  @Override
  protected CommonShell findFirstExercise() {
    //logger.info("findFirstExercise : completed " + controller.showCompleted());
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
   *
   * @return
   */
  protected int getVerticalUnaccountedFor() {
    return unaccountedForVertical;
  }

  /**
   * @param v
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#FlexSectionExerciseList
   */
  public void setUnaccountedForVertical(int v) {
    unaccountedForVertical = v;
    pagingContainer.setUnaccountedForVertical(v);
    //logger.info("setUnaccountedForVertical : vert " + v + " for " +getElement().getId());
  }

  /**
   * add left side components
   *
   * @param pagingContainer
   */
  protected void addTableWithPager(PagingContainer pagingContainer) {
    // row 1
    Panel column = new FlowPanel();
    add(column);
    addTypeAhead(column);

    // row 2
    add(pagingContainer.getTableWithPager());
  }

  private Timer waitTimer = null;
  private final SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");
  private final SafeUri white = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "white_32x32.png");
  private final com.github.gwtbootstrap.client.ui.Image waitCursor = new com.github.gwtbootstrap.client.ui.Image(white);

  /**
   * Show wait cursor if the type ahead takes too long.
   *
   * @param column
   * @see mitll.langtest.client.list.PagingExerciseList#addTableWithPager
   */
  protected void addTypeAhead(Panel column) {
    if (showTypeAhead) {
      typeAhead = new TypeAhead(column, waitCursor, "Search", true) {
        @Override
        public void gotTypeAheadEntry(String text) {
          controller.logEvent(getTypeAhead(), "TypeAhead", "UserList_" + userListID, "User search ='" + text + "'");
          loadExercises(getHistoryToken(""), text, false);
        }
      };
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

  protected String getTypeAheadText() {
    return typeAhead.getText();
  }

  @Override
  protected void gotExercises(boolean success) {
//    long now = System.currentTimeMillis();
    // logger.info("took " + (now - then) + " millis");
    if (waitTimer != null) {
      waitTimer.cancel();
    }
    waitCursor.setUrl(white);
  }

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#gotEmptyExerciseList
   */
  protected void showEmptySelection() {
    logger.info("for " +getInstance()+
        " showing no items match relative to " + typeAhead.getWidget().getElement().getId() + " parent " + typeAhead.getWidget().getParent());
    showPopup("No items match the selection and search.", "Try clearing one of your selections or changing the search.", typeAhead.getWidget());
    createdPanel = new SimplePanel();
    createdPanel.getElement().setId("placeHolderWhenNoExercises");
  }

  private void showPopup(String toShow, String toShow2, Widget over) {
    new PopupHelper().showPopup(toShow, toShow2, over);
  }

  /**
   * @deprecated
   */
  void tellUserPanelIsBusy() {
    Window.alert("Please stop recording before changing items.");
  }

  String getHistoryToken(String id) {
    return "item=" + id;
  }

  void gotClickOnItem(final CommonShell e) {
    if (isExercisePanelBusy()) {
      tellUserPanelIsBusy();
      markCurrentExercise(pagingContainer.getCurrentSelection().getID());
    } else {
      controller.logEvent(this, "ExerciseList", e.getID(), "Clicked on item '" + e.getTooltip() + "'");

      pushNewItem(e.getID());
    }
  }

  public void clear() {
    pagingContainer.clear();
  }

  public void flush() {
    pagingContainer.flush();
    onResize();
  }

  private List<CommonShell> inOrderResult;

  /**
   * A little complicated -- if {@link #doShuffle} is true, shuffles the exercises
   *
   * @param result
   * @see ExerciseList#rememberAndLoadFirst(java.util.List, mitll.langtest.shared.CommonExercise, String)
   * @see #simpleSetShuffle(boolean)
   */
  @Override
  public List<CommonShell> rememberExercises(List<CommonShell> result) {
    inOrderResult = result;
    if (doShuffle) {
      logger.info(getInstance() + " : rememberExercises - shuffling " + result.size() + " items");

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
  protected List<CommonShell> getInOrder() {
    return inOrderResult;
  }

  @Override
  public int getSize() {
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
  public void addExercise(CommonShell es) {
/*    if (pagingContainer.getSize() < 5) {
      logger.info(getInstance() +" : addExercise adding " + es.getID() + " state " + es.getState() + "/" + es.getSecondState());
    }*/
    pagingContainer.addExercise(es);
  }

  public void addExerciseAfter(CommonShell after, CommonShell es) {
    pagingContainer.addExerciseAfter(after, es);
  }

  public CommonShell forgetExercise(String id) {
    // logger.info("PagingExerciseList.forgetExercise " + id + " on " + getElement().getId() + " ul " +userListID);
    CommonShell es = byID(id);
    if (es != null) {
      return removeExercise(es);
    } else {
      return null;
    }
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
   * @see mitll.langtest.client.list.ExerciseList#removeExercise
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
   * @param itemID
   * @see #useExercise(mitll.langtest.shared.CommonExercise)
   */
  protected void markCurrentExercise(String itemID) {
    pagingContainer.markCurrentExercise(itemID);
  }

  public void setUserListID(long userListID) {
    this.userListID = userListID;
  }

  public void redraw() {
    pagingContainer.flush();
    pagingContainer.redraw();
  }

  /**
   * @return
   * @see HistoryExerciseList#loadExercisesUsingPrefix(java.util.Map, String, boolean)
   */
  public boolean getUnrecorded() {
    return unrecorded;
  }

  /**
   * @param unrecorded
   * @see mitll.langtest.client.custom.RecorderNPFHelper#getMyListLayout
   */
  public void setUnrecorded(boolean unrecorded) {
    this.unrecorded = unrecorded;
  }

  public boolean isOnlyExamples() {
    return onlyExamples;
  }

  public void setOnlyExamples(boolean onlyExamples) {
    this.onlyExamples = onlyExamples;
  }
}
