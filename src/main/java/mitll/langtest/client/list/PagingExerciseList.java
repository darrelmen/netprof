/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

public abstract class PagingExerciseList<T extends CommonShell, U extends HasID>
    extends ExerciseList<T, U>
    implements ContainerList<T> {
  private final Logger logger = Logger.getLogger("PagingExerciseList");

  static final String SEARCH = "Search";
  protected final WaitCursorHelper waitCursorHelper;

  protected ClickablePagingContainer<T> pagingContainer;
  /**
   * @see #getInOrder
   */
  private List<T> inOrderResult;

  /**
   * @see #addTypeAhead
   */
  private ITypeAhead typeAhead;
  int userListID = -1;
  private int unaccountedForVertical = 160;
  private static final boolean DEBUG = false;

  /**
   * @param currentExerciseVPanel
   * @param factory
   * @param controller
   * @see mitll.langtest.client.custom.content.NPFHelper#makeExerciseList
   */
  PagingExerciseList(Panel currentExerciseVPanel,
                     ExercisePanelFactory<T, U> factory,
                     ExerciseController controller,
                     ListOptions listOptions) {
    super(currentExerciseVPanel, factory, controller, listOptions);
    this.waitCursorHelper = new WaitCursorHelper();
    addComponents();
    // getElement().setId("PagingExerciseList_" + getInstance());
  }

  public Map<Integer, T> getIdToExercise() {
    //  Map<Integer, T> idToExercise = pagingContainer.getIdToExercise();
    // logger.info("getIdToExercise - idToExercise "+idToExercise.size()   );
    return pagingContainer.getIdToExercise();
  }

  /**
   * @param comp
   * @see ListSorting#sortBy
   */
 /* void sortBy(Comparator<T> comp) {
    if (DEBUG) logger.info("start - sortBy ");
    scheduleWaitTimer();

    pagingContainer.sortBy(comp);
    loadFirst();

    showFinishedGettingExercises();
    if (DEBUG) logger.info("end  - sortBy ");
  }*/
  public void loadFirst() {
    pushFirstSelection(getFirstID(), getTypeAheadText());
  }

  @Override
  public void reload(Map<String, Collection<String>> typeToSection) {
  }

  /**
   * Add two rows -- the search box and then the item list
   *
   * @see #PagingExerciseList
   */
  private void addComponents() {
    ClickablePagingContainer<T> pagingContainer = makePagingContainer();
    pagingContainer.setContainerList(this);
    addTableWithPager(pagingContainer);
  }

  /**
   * @param prefix
   * @return
   * @see #getExercises
   */
  protected ExerciseListRequest getExerciseListRequest(String prefix) {
    // logger.info("getExerciseListRequest prefix " + prefix);

    return new ExerciseListRequest(
        incrRequest(),
        controller.getUserState().getUser(),
        controller.getProjectID())
        .setPrefix(prefix)
        .setUserListID(userListID)
        .setActivityType(getActivityType());
  }

  /**
   * @return
   * @see FacetExerciseList#noSectionsGetExercises
   */
  String getPrefix() {
    return typeAhead != null ? typeAhead.getText() : "";
  }

  /**
   * @return
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#addComponents()
   */
  abstract protected ClickablePagingContainer<T> makePagingContainer();

  public abstract void gotClickOnItem(final T e);

  /**
   * Sometimes we want to not respect if there's an item selection in the url.
   *
   * @param searchIfAny
   * @param exerciseID
   * @see #rememberAndLoadFirst(List, String, String, int)
   */
  protected void goToFirst(String searchIfAny, int exerciseID) {
    if (listOptions.isShowFirstNotCompleted()) {
      //logger.info("goToFirst IGNORING " + exerciseID + " searchIfAny '" + searchIfAny + "'");
      loadFirstExercise(searchIfAny);
    } else {
      super.goToFirst(searchIfAny, exerciseID);
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
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#FlexSectionExerciseList
   */
  public void setUnaccountedForVertical(int v) {
    unaccountedForVertical = v;
    pagingContainer.setUnaccountedForVertical(v);
    //logger.info("setUnaccountedForVertical : vert " + v + " for " +getElement().getExID());
  }

  /**
   * add left side components to vertical panel - rows in a
   *
   * @param pagingContainer
   * @see #addComponents
   */
  void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
    // row 1
    Panel column = new FlowPanel();
    add(column);
    addTypeAhead(column);

    {
      DivWidget optionalWidget = getOptionalWidget();
      if (optionalWidget != null) column.add(optionalWidget);
    }

    // row 2
    add(pagingContainer.getTableWithPager(listOptions));
  }

  /**
   * Called on keypress
   * Show wait cursor if the type ahead takes too long.
   *
   * @param column
   * @see mitll.langtest.client.list.PagingExerciseList#addTableWithPager
   */
  void addTypeAhead(Panel column) {
    if (listOptions.isShowTypeAhead()) {
      typeAhead = new TypeAhead(column, waitCursorHelper, SEARCH, true) {
        @Override
        public void gotTypeAheadEntry(String text) {
//          gotTypeAheadEvent(text, false);
          //      logger.info("gotTypeAheadEntry " + text);
          pushNewItem(text, -1, -1);
          controller.logEvent(getTypeAheadBox(), "TypeAhead", "UserList_" + userListID, "User search ='" + text + "'");
        }
      };
    } else {
      if (DEBUG) logger.info("not adding type ahead to " + getElement().getId());
    }
  }

  protected DivWidget getOptionalWidget() {
    return null;
  }

  /**
   * @param text
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#makeClickableText(String, String, String, boolean)
   */
  public void searchBoxEntry(String text) {
    if (listOptions.isShowTypeAhead()) {
      //logger.info("searchBoxEntry type ahead '" + text + "'");
      // why would this be a bad idea?
      //setTypeAheadText(text);
      alwaysSetTypeAhead(text);
      pushNewItem(text, -1, -1);
      //gotTypeAheadEvent(text, true);
    } else {
      logger.warning("skipping searchBoxEntry ");
    }
  }

  private void alwaysSetTypeAhead(String t) {
    //if (getTypeAheadText().isEmpty()) {
    //  logger.info("alwaysSetTypeAhead Set type ahead to '" + t + "'");
    typeAhead.setText(t);
    //}
  }

  public String getTypeAheadText() {
    //  if (typeAhead == null) logger.warning("type ahead is null?");
    return typeAhead != null ? typeAhead.getText() : "";
  }

  /**
   * @param t
   * @see HistoryExerciseList#restoreUIState
   */
  void setTypeAheadText(String t) {
    String typeAheadText = getTypeAheadText();

    if (typeAheadText.equals(t)) {
      //logger.warning("\n\n\nsetTypeAheadText not setting text from  '" + typeAheadText + "' to '" + t + "'");
    } else {
      if (typeAhead != null) {
        typeAhead.setText(t);
      } else {
        if (DEBUG) logger.info("setTypeAheadText: huh? no type ahead box for '" + t + "'");
      }
    }
  }

  /**
   * @see HistoryExerciseList#onValueChange(ValueChangeEvent)
   * @see HistoryExerciseList#ignoreStaleRequest(ExerciseListWrapper)
   */
  void popRequest() {
    /*if (!pendingRequests.isEmpty()) {
      pendingRequests.pop();
      List<Long> toRemove = new ArrayList<>();
      long now = System.currentTimeMillis();
      for (Long pending : pendingRequests) if (pending < now - TEN_SECONDS) toRemove.add(pending);
      pendingRequests.removeAll(toRemove);
    }*/
  }

  @Override
  protected void showFinishedGettingExercises() {
    waitCursorHelper.showFinished();
  }

  String getHistoryTokenFromUIState(String search, int id, int listID) {
    String listParam = listID > -1 ? (SelectionState.SECTION_SEPARATOR +
        SelectionState.LIST + "=" + listID) : "";

    return
        getSearchTerm(search) +
            SelectionState.SECTION_SEPARATOR +
            SelectionState.ITEM + "=" + id +
            listParam
        ;
  }

  @NotNull
  String getSearchTerm(String search) {
    return SelectionState.SEARCH + "=" + search;
  }

  public void clear() {
    // logger.info(getInstance() + " : clear");
    pagingContainer.clear();
  }

  private void flush() {
    // logger.info(getInstance() + " : flush");
    pagingContainer.flush();
    onResize();
  }

  /**
   * @param comparator if null use original order
   */
  @Override
  public void flushWith(Comparator<T> comparator) {
    //logger.info(getInstance() + " : flushWith " + comparator);
    setComparator(comparator);
    flush();
  }

  public void setComparator(Comparator<T> comparator) {
    pagingContainer.setComparator(comparator);

  }

  /**
   * A little complicated -- if {@link #doShuffle} is true, shuffles the exercises
   *
   * @param toRemember
   * @see ExerciseList#rememberAndLoadFirst
   * @see #simpleSetShuffle
   */
  @Override
  public List<T> rememberExercises(List<T> toRemember) {
    inOrderResult = toRemember;

//    logger.info(getInstance() + " : rememberExercises " + toRemember.size() + " items");

    if (doShuffle) {
      // logger.info(getInstance() + " : rememberExercises - shuffling " + toRemember.size() + " items");
      ArrayList<T> ts = new ArrayList<>(toRemember);
      toRemember = ts;
      Shuffler.shuffle(ts);
    }

    toRemember = resort(toRemember);

    clear();
    toRemember.forEach(this::addExercise);

    flush();
    return toRemember;
  }

  List<T> resort(List<T> toRemember) {
    return toRemember;
  }

  @Override
  public List<T> getInOrder() {
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
    T first = pagingContainer.getFirst();
    return first;
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
   * @see mitll.langtest.client.list.ExerciseList#removeExercise
   */
  public T simpleRemove(int id) {
    T es = byID(id);
    pagingContainer.forgetItem(es);

    if (isEmpty()) {
      removeCurrentExercise();
    }
    return es;
  }

  @Override
  public void onResize() {
    pagingContainer.onResize(getCurrentExercise());
  }

  /**
   * Scrolls container to visible range, if needed.
   *
   * @param itemID
   * @see #showExercise
   */
  public void markCurrentExercise(int itemID) {
    pagingContainer.markCurrentExercise(itemID);
  }

  public void setUserListID(int userListID) {
    this.userListID = userListID;
  }

  public void redraw() {
    pagingContainer.flush();
    pagingContainer.redraw();
  }

  @Override
  ActivityType getActivityType() {
    return listOptions.getActivityType();
  }
}
