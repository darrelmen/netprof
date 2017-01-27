/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.PopupHelper;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.exercise.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Show exercises with a cell table that can handle thousands of rows.
 * Does tooltips using tooltip field on...?
 * <p/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/27/12
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PagingExerciseList<T extends CommonShell, U extends Shell> extends ExerciseList<T, U> {
  private final Logger logger = Logger.getLogger("PagingExerciseList");

  static final String SEARCH = "Search";
  private static final int TEN_SECONDS = 10 * 60 * 1000;

  protected final ExerciseController controller;
  protected ClickablePagingContainer<T> pagingContainer;
  private final boolean showTypeAhead;

  private TypeAhead typeAhead;
  long userListID = -1;
  private int unaccountedForVertical = 160;
//  private boolean unrecorded, defaultAudioFilter;
  private boolean onlyExamples;

  private Timer waitTimer = null;
  private final SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");
  private final SafeUri white = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "white_32x32.png");
  private final com.github.gwtbootstrap.client.ui.Image waitCursor = new com.github.gwtbootstrap.client.ui.Image(white);
  boolean showFirstNotCompleted = false;

  /**
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param controller
   * @param showTypeAhead
   * @param instance
   * @param incorrectFirst
   * @param showFirstNotCompleted
   * @see mitll.langtest.client.custom.content.AVPHelper#makeExerciseList
   * @see mitll.langtest.client.custom.content.NPFHelper#makeExerciseList
   */
  PagingExerciseList(Panel currentExerciseVPanel,
                     ExerciseServiceAsync service,
                     UserFeedback feedback,
                     ExercisePanelFactory<T, U> factory,
                     ExerciseController controller,
                     boolean showTypeAhead,
                     String instance,
                     boolean incorrectFirst,
                     boolean showFirstNotCompleted) {
    super(currentExerciseVPanel, service, feedback, factory, controller, instance, incorrectFirst);
    this.controller = controller;
    this.showTypeAhead = showTypeAhead;
    this.showFirstNotCompleted = showFirstNotCompleted;

    addComponents();
    getElement().setId("PagingExerciseList_" + instance);
//    if (showFirstNotCompleted) logger.info("show first completed for " + instance);
  }

  @Override
  protected Set<Integer> getKeys() {
    return pagingContainer.getKeys();
  }

  @Override
  public void setState(int id, STATE state) {
    byID(id).setState(state);
  }

  @Override
  public void setSecondState(int id, STATE state) {
    byID(id).setSecondState(state);
  }

  @Override
  public void reload(Map<String, Collection<String>> typeToSection) {
  }

  /**
   * Add two rows -- the search box and then the item list
   * @see #PagingExerciseList
   */
  protected void addComponents() {
    addTableWithPager(makePagingContainer());
  }

  /**
   * @param selectionState
   * @param prefix
   * @param onlyWithAudioAnno
   * @param onlyUnrecorded
   * @param onlyDefaultUser
   * @param onlyUninspected
   * @paramx setTypeAheadText
   * @see #addTypeAhead(com.google.gwt.user.client.ui.Panel)
   */
  void loadExercises(String selectionState, String prefix, boolean onlyWithAudioAnno, boolean onlyUnrecorded, boolean onlyDefaultUser, boolean onlyUninspected) {
    scheduleWaitTimer();
/*    logger.info("PagingExerciseList.loadExercises : looking for " +
        "'" + prefix + "' (" + prefix.length() + " chars) in list id " + userListID + " instance " + getInstance());
        */
    ExerciseListRequest request = getRequest(prefix);
    service.getExerciseIds(
        request,
        new SetExercisesCallback("", prefix, -1, request));
  }

  ExerciseListRequest getRequest(String prefix) {
    return new ExerciseListRequest(incrRequest(),
        controller.getUserState().getUser())
        .setPrefix(prefix)
        .setUserListID(userListID)
        .setRole(getRole())
        .setOnlyUnrecordedByMe(false)
        .setOnlyExamples(isOnlyExamples())
        .setIncorrectFirstOrder(incorrectFirstOrder)
        .setOnlyDefaultAudio(false)
        .setOnlyUninspected(false);
  }

  /**
   *   ExerciseListRequest getRequest(String prefix) {
   return new ExerciseListRequest(incrRequest(),
   controller.getUserState().getUser())
   .setPrefix(prefix)
   .setUserListID(userListID)
   .setRole(getRole())
   .setOnlyUnrecordedByMe(getUnrecorded())
   .setOnlyExamples(isOnlyExamples())
   .setIncorrectFirstOrder(incorrectFirstOrder)
   .setOnlyDefaultAudio(defaultAudioFilter);
   }
  */

  /**
   * @return
   * @see PagingExerciseList#loadExercises
   */
  protected String getPrefix() {
    return typeAhead.getText();
  }

  /**
   * @return
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addComponents()
   */
  abstract protected ClickablePagingContainer<T> makePagingContainer();
/*  {
    final PagingExerciseList<T, U> outer = this;
    pagingContainer =
        new ClickablePagingContainer<T>(controller) {
          @Override
          protected void gotClickOnItem(T e) {
            outer.gotClickOnItem(e);
          }
        };
    return pagingContainer;
  }*/

  /**
   * Skip to first not completed or just go to the first item.
   *
   * @return
   * @see #loadFirstExercise
   */
  @Override
  protected T findFirstExercise() {
    return showFirstNotCompleted ? getFirstNotCompleted() : super.findFirstExercise();
  }

  /**
   * Sometimes we want to not respect if there's an item selection in the url.
   *
   * @param searchIfAny
   * @param exerciseID
   */
  protected void goToFirst(String searchIfAny, int exerciseID) {
    if (showFirstNotCompleted) {
      // logger.info("goToFirst " + exerciseID + " searchIfAny '" + searchIfAny +"'");
      loadFirstExercise(searchIfAny);
    } else {
      super.goToFirst(searchIfAny, exerciseID);
    }
  }

  private T getFirstNotCompleted() {
    for (T es : pagingContainer.getExercises()) {
      STATE state = es.getState();
      if (state != null && state.equals(STATE.UNSET)) {
        // logger.info("first unset is " + es.getID() + " state " + state);
        return es;
      }
    }
    return super.findFirstExercise();
  }

  public void report() {
    for (T es : pagingContainer.getExercises()) {
      logger.info(userListID + " has " + es);
    }
  }

  /**
   * TODO : Not sure if this is needed anymore
   *
   * @return
   */
  int getVerticalUnaccountedFor() {
    return unaccountedForVertical;
  }

  /**
   * @param v
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#FlexSectionExerciseList
   */
  public void setUnaccountedForVertical(int v) {
    unaccountedForVertical = v;
    pagingContainer.setUnaccountedForVertical(v);
    //logger.info("setUnaccountedForVertical : vert " + v + " for " +getElement().getExID());
  }

  /**
   * add left side components
   *
   * @param pagingContainer
   * @see #addComponents
   */
  protected void addTableWithPager(ClickablePagingContainer<T> pagingContainer) {
    // row 1
    Panel column = new FlowPanel();
    add(column);
    addTypeAhead(column);

    // row 2
    add(pagingContainer.getTableWithPager());
  }

  /**
   * Called on keypress
   * Show wait cursor if the type ahead takes too long.
   *
   * @param column
   * @see mitll.langtest.client.list.PagingExerciseList#addTableWithPager
   */
  protected void addTypeAhead(Panel column) {
    if (showTypeAhead) {
      typeAhead = new TypeAhead(column, waitCursor, SEARCH, true) {
        @Override
        public void gotTypeAheadEntry(String text) {
          gotTypeAheadEvent(text, false);
          controller.logEvent(getTypeAhead(), "TypeAhead", "UserList_" + userListID, "User search ='" + text + "'");
        }
      };
    }
  }

  /**
   * @param text
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#makeClickableText(String, String, String, boolean)
   */
  public void searchBoxEntry(String text) {
    if (showTypeAhead) {
      //   logger.info("searchBoxEntry type ahead '" + text + "'");
      gotTypeAheadEvent(text, true);
    }
  }

  private Stack<Long> pendingRequests = new Stack<>();

  private void gotTypeAheadEvent(String text, boolean setTypeAheadText) {
    //  logger.info("got type ahead '" + text + "' at " + new Date(keypressTimestamp));
    if (!setTypeAheadText) {
      pendingRequests.add(System.currentTimeMillis());
    }
    loadExercises(getHistoryTokenFromUIState(text, -1), text, false,false, false, false);
  }

  void scheduleWaitTimer() {
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

  String getTypeAheadText() {
    return typeAhead != null ? typeAhead.getText() : "";
  }

  /**
   * @param t
   * @see HistoryExerciseList#restoreUIState(SelectionState)
   */
  void setTypeAheadText(String t) {
    if (pendingRequests.isEmpty()) {
      if (typeAhead != null) {
        //   logger.info("Set type ahead to '" + t + "'");
        typeAhead.setText(t);
      }
    } else {
      popRequest();
      // logger.info("setTypeAheadText pendingRequests now" + pendingRequests);
    }
  }

  /**
   * @see HistoryExerciseList#onValueChange(ValueChangeEvent)
   * @see HistoryExerciseList#ignoreStaleRequest(ExerciseListWrapper)
   */
  void popRequest() {
    if (!pendingRequests.isEmpty()) {
      pendingRequests.pop();
      List<Long> toRemove = new ArrayList<>();
      long now = System.currentTimeMillis();
      for (Long pending : pendingRequests) if (pending < now - TEN_SECONDS) toRemove.add(pending);
      pendingRequests.removeAll(toRemove);
    }
  }

  @Override
  protected void gotExercises(boolean success) {
    if (waitTimer != null) {
      waitTimer.cancel();
    }
    waitCursor.setUrl(white);
  }

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#gotEmptyExerciseList
   */
  protected void showEmptySelection() {
/*
    logger.info("for " + getInstance() +
        " showing no items match relative to " + typeAhead.getWidget().getElement().getExID() + " parent " + typeAhead.getWidget().getParent().getElement().getExID());
*/

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        showPopup("No items match the selection and search.", "Try clearing one of your selections or changing the search.", typeAhead.getWidget());
        showEmptyExercise();
      }
    });
  }

  private void showPopup(String toShow, String toShow2, Widget over) {
    new PopupHelper().showPopup(toShow, toShow2, over);
  }

  /**
   * @deprecatedx
   */
/*
  private void tellUserPanelIsBusy() {
    Window.alert("Please stop recording before changing items.");
  }
*/

  String getHistoryTokenFromUIState(String search, int id) {
    return "search=" + search + ";item=" + id;
  }

  public abstract void gotClickOnItem(final T e);
/*  {
    if (isExercisePanelBusy()) {
      tellUserPanelIsBusy();
      markCurrentExercise(pagingContainer.getCurrentSelection().getID());
    } else {
      controller.logEvent(this, "ExerciseList", ""+e.getID(), "Clicked on item '" + e.toString() + "'");
      pushNewItem(getTypeAheadText(), e.getID());
    }
  }*/

  public void clear() {
    pagingContainer.clear();
  }

  public void flush() {
    pagingContainer.flush();
    onResize();
  }

  private List<T> inOrderResult;

  /**
   * A little complicated -- if {@link #doShuffle} is true, shuffles the exercises
   *
   * @param result
   * @see ExerciseList#rememberAndLoadFirst
   * @see #simpleSetShuffle(boolean)
   */
  @Override
  public List<T> rememberExercises(List<T> result) {
    inOrderResult = result;
    if (doShuffle) {
      // logger.info(getInstance() + " : rememberExercises - shuffling " + result.size() + " items");
      ArrayList<T> ts = new ArrayList<>(result);
      result = ts;
      Shuffler.shuffle(ts);
    }
    clear();
    int c = 0;
    for (T es : result) {
      addExercise(es);
    }
    flush();
    return result;
  }

  @Override
  protected List<T> getInOrder() {
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
  protected T getFirst() {
    return pagingContainer.getFirst();
  }

  @Override
  public T byHasID(HasID hasID) {
    return pagingContainer.byID(hasID.getID());
  }

  @Override
  public T byID(int name) {
    return pagingContainer.byID(name);
  }

  @Override
  public T getCurrentExercise() {
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

  /**
   * @param es
   * @see ExerciseList#rememberExercises(List)
   */
  @Override
  public void addExercise(T es) {
    pagingContainer.addExercise(es);
  }

  /**
   * @param after
   * @param es
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise(Button)
   */
  public void addExerciseAfter(T after, T es) {
    pagingContainer.addExerciseAfter(after, es);
  }

  public T forgetExercise(int id) {
    T es = byID(id);
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
  public T simpleRemove(int id) {
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

  /**
   * @param itemID
   * @see #useExercise
   */
  protected void markCurrentExercise(int itemID) {
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
   * @see HistoryExerciseList#loadExercisesUsingPrefix
   */
/*
  boolean getUnrecorded() {
    return unrecorded;
  }
*/

  /**
   * @paramx unrecorded
   * @seex SimpleChapterNPFHelper#getMyListLayout
   */
/*
  public void setUnrecorded(boolean unrecorded) {
    this.unrecorded = unrecorded;
  }

  public void setDefaultAudioFilter(boolean unrecorded) {
    this.defaultAudioFilter = unrecorded;
  }
*/

  boolean isOnlyExamples() {
    return onlyExamples;
  }

  protected void setOnlyExamples(boolean onlyExamples) {
    this.onlyExamples = onlyExamples;
  }
}
