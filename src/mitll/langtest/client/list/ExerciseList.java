package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles left side of NetPron2 -- which exercise is the current one, highlighting, etc.
 *
 * User: GO22670
 * Date: 7/9/12
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExerciseList<T extends ExerciseShell> extends VerticalPanel implements ListInterface<T>, ProvidesResize,
  ValueChangeHandler<String> {
  private static final int NUM_QUESTIONS_FOR_TOKEN = 5;
  public static final String ITEMS = "Items";

  private SimplePanel innerContainer;
  protected final LangTestDatabaseAsync service;
  protected final UserFeedback feedback;
  private ExercisePanelFactory factory;
  private final ExerciseController controller;

  private Panel createdPanel;
  protected UserManager user;
  private String exercise_title;
  private final boolean showTurkToken;
  private final boolean showInOrder;
  private int countSincePrompt = 0;
  protected int lastReqID = 0;
  private final Set<Integer> visited = new HashSet<Integer>();
  protected final boolean allowPlusInURL;
  protected String instance;

  /**
   * @see  mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param controller
   * @param showTurkToken
   * @param showInOrder
   * @param instance
   */
  public ExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                      ExercisePanelFactory factory,
                      ExerciseController controller,
                      boolean showTurkToken, boolean showInOrder, String instance) {
    addWidgets(currentExerciseVPanel);
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
    this.showTurkToken = showTurkToken;
    this.showInOrder = showInOrder;
    this.allowPlusInURL = controller.getProps().shouldAllowPlusInURL();
    this.controller = controller;
    this.instance = instance;
    getElement().setId("ExerciseList_"+instance);
    //System.out.println("\n\n\tExerciseList : got instance  " + instance);

    // Add history listener

    if (handlerRegistration == null) {
      handlerRegistration = History.addValueChangeHandler(this);
    }
  }

  private HandlerRegistration handlerRegistration;

  @Override
  protected void onLoad() {
    super.onLoad();
//    System.out.println("ExerciseList : History onLoad  " + instance);
    if (handlerRegistration == null) {
      handlerRegistration = History.addValueChangeHandler(this);
    }
  }

  @Override
  protected void onUnload() {
    super.onUnload();

  //  System.out.println("ExerciseList : History onUnload  " + instance);

    handlerRegistration.removeHandler();
    handlerRegistration = null;
  }

  /**
   * @see #ExerciseList(com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExercisePanelFactory, mitll.langtest.client.exercise.ExerciseController, boolean, boolean, String)
   * @param currentExerciseVPanel
   */
  private void addWidgets(final Panel currentExerciseVPanel) {
    this.innerContainer = new SimplePanel();
    this.innerContainer.setWidth("100%");
    this.innerContainer.setHeight("100%");
    currentExerciseVPanel.add(innerContainer);
  //  currentExerciseVPanel.addStyleName("shadowBorder");
  }

  /**
   * @see mitll.langtest.client.LangTest#setFactory
   * @param factory
   * @param user
   * @param expectedGrades
   */
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    this.factory = factory;
    this.user = user;
  }

  /**
   * Get exercises for this user.
   * @see mitll.langtest.client.LangTest#doEverythingAfterFactory(long)
   * @see mitll.langtest.client.list.HistoryExerciseList#noSectionsGetExercises(long)
   * @param userID
   * @param getNext
   */
  public void getExercises(long userID, boolean getNext) {
  //  System.out.println("ExerciseList.getExercises for user " +userID);
    lastReqID++;

    if (showInOrder) {
      service.getExerciseIds(lastReqID, new SetExercisesCallback());
    } else {
      System.out.println("ExerciseList.getExercises for user " +userID);
      service.getExerciseIds(lastReqID, userID, new SetExercisesCallback());
    }
  }

  /**
   * @see mitll.langtest.client.custom.NPFHelper#reload()
   * @see mitll.langtest.client.custom.ReviewEditItem.ReviewEditableExercise#doAfterEditComplete(ListInterface)
   */
  public void reload() {
    System.out.println("ExerciseList.reload for user " + controller.getUser() + " instance " + instance + " id " + getElement().getId());
    service.getExerciseIds(lastReqID, controller.getUser(), new SetExercisesCallback());
  }

  @Override
  public void reloadWith(String id) {
    System.out.println("ExerciseList.reloadWith " + id+ " for user " + controller.getUser());
    service.getExerciseIds(lastReqID, controller.getUser(), new SetExercisesCallback(id));
  }


  /**
   * So we have a catch-22 -
   *
   * If we fire the current history, we override the initial selection associated
   * with a user logging in for the first time.
   *
   * If we don't, when we click on a link from an email, the item=NNN value will be ignored.
   *
   * I gotta go with the latter.
   *
   * @see #loadFirstExercise()
   * @param exerciseID
   */
  private void pushFirstSelection(String exerciseID) {
    String token = History.getToken();
    token = getSelectionFromToken(token);
    String idFromToken = getIDFromToken(token);
    System.out.println("ExerciseList.pushFirstSelection : current token '" + token + "' id from token '" + idFromToken +
      "' vs new exercise " +exerciseID);
    if (token != null && idFromToken.equals(exerciseID)) {
      System.out.println("\tpushFirstSelection : (" + instance+
        ") current token " + token + " same as new " +exerciseID);
      checkAndAskServer(exerciseID);
    }
    else {
      pushNewItem(exerciseID);
    }
  }

  /**
   * Deal with responseType being after ###
   * @param token
   * @return
   */
  protected String getSelectionFromToken(String token) {
    token = token.contains("###") ? token.split("###")[0] : token; // remove any other parameters
    return token;
  }

  /**
   * Calling this will result in an immediate call to onValueChange (reacting to the history change)
   *
   * @see ListInterface#loadExercise(String)
   * @see #pushFirstSelection(String)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param exerciseID
   */
  protected void pushNewItem(String exerciseID) {
    System.out.println("------------ ExerciseList.pushNewItem : (" + instance+ ") push history " + exerciseID + " - ");
    History.newItem("#item=" + exerciseID + ";instance="+instance);
  }

  public void onResize() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (current instanceof RequiresResize) {
        ((RequiresResize) current).onResize();
      }
    }
  }

  /**
   * So you an load a specific exercise
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param exercise_title
   */
  @Override
  public void setExercise_title(String exercise_title) {
    this.exercise_title = exercise_title;
  }

  private String unencodeToken(String token) {
    token = token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ").replaceAll("\\+", " ");
    return token;
  }

  private String unencodeTokenDontRemovePlus(String token) {
    token = token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ");
    return token;
  }

  public Panel getCreatedPanel() {
    return createdPanel;
  }

  /**
   * @see ListInterface#getExercises(long, boolean)
   */
  protected class SetExercisesCallback implements AsyncCallback<ExerciseListWrapper<T>> {
    private String id;

    public SetExercisesCallback(String id) { this.id = id;  }
    public SetExercisesCallback() {}

    public void onFailure(Throwable caught) {
      if (!caught.getMessage().trim().equals("0")) {
        feedback.showErrorMessage("Server error", "SetExercisesCallback : Server error - couldn't get exercises.");
      }
      System.out.println("Got exception '" +caught.getMessage() + "' " +caught);
    }
    public void onSuccess(ExerciseListWrapper<T> result) {
      System.out.println("ExerciseList.SetExercisesCallback Got " + result.getExercises().size() + " results");
      if (isStaleResponse(result)) {
        System.out.println("----> SetExercisesCallback.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
      } else {
        if (result.getExercises().isEmpty()) {
          gotEmptyExerciseList();
        }
        if (id != null) {
          List<T> exercises = result.getExercises();
          rememberExercises(exercises);
          for (ListChangeListener<T> listener : listeners) listener.listChanged(exercises);
          pushFirstSelection(id);
        }
        else {
          rememberAndLoadFirst(result.getExercises(), result.getFirstExercise());
        }
        controller.showProgress();
      }
    }
  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() {
    System.out.println(new Date() +" gotEmptyExerciseList : ------  ------------ ");
  }

  public void rememberAndLoadFirst(List<T> exercises) {
    rememberAndLoadFirst(exercises, null);
  }

  /**
   * @param exercises
   * @see ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   */
  public void rememberAndLoadFirst(List<T> exercises, Exercise firstExercise) {
    System.out.println("ExerciseList : rememberAndLoadFirst remembering " + exercises.size() + " : first = " +firstExercise);

    rememberExercises(exercises);
    for (ListChangeListener<T> listener : listeners) listener.listChanged(exercises);

    if (firstExercise != null) {
      T firstExerciseShell = findFirstExercise();
      if (firstExerciseShell.getID().equals(firstExercise.getID())) {
        useExercise(firstExercise);   // allows us to skip another round trip with the server to ask for the first exercise
      }
      else {
        loadFirstExercise();
      }
    }
    else {
      loadFirstExercise();
    }
  }

  protected boolean isStaleResponse(ExerciseListWrapper<T> result) {
    return result.getReqID() < lastReqID;
  }

  protected abstract void rememberExercises(List<T> result);
  protected abstract int getSize();

  /**
   * Worry about deleting the currently visible item.
   *
   * @see mitll.langtest.client.list.PagingExerciseList#forgetExercise(String)
   * @param es
   */
  protected void removeExercise(T es) {
    String id = es.getID();
    T current = getCurrentExercise();
    if (current.getID().equals(id)) {
      if (!onLast(current)) {
        loadNextExercise(current);
      }
      else if (!onFirst(current)) {
        loadPreviousExercise(current);
      }
    }

    simpleRemove(id);
  }

  @Override
  public void hideExerciseList() {  getParent().setVisible(false);  }

  /**
   * @see #rememberAndLoadFirst
   */
  protected void loadFirstExercise() {
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      noMatches();
      System.out.println("loadFirstExercise : current exercises is empty?");
      removeCurrentExercise();
    } else {
      ExerciseShell toLoad = findFirstExercise();

      if (exercise_title != null) {
        ExerciseShell e = byID(exercise_title);
        if (e != null) toLoad = e;
      }

      System.out.println("loadFirstExercise ex id =" + toLoad.getID());
      pushFirstSelection(toLoad.getID());
    }
  }

  protected abstract boolean isEmpty();

  /**
   */
  private void noMatches() {
    String message = isEmpty() ? "No items yet." : "No matches found. Please try a different search.";
    final PopupPanel pleaseWait = showPleaseWait(message);

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };

    // Schedule the timer to run once in 1 seconds.
    t.schedule(3000);
  }

  private PopupPanel showPleaseWait(String message) {
    final PopupPanel pleaseWait = new PopupPanel();
    pleaseWait.setAutoHideEnabled(false);
    pleaseWait.add(new HTML(message));
    pleaseWait.center();
    return pleaseWait;
  }

  protected T findFirstExercise() {
    return getFirst();
  }

  protected abstract T getFirst();

  protected boolean hasExercise(String id) { return byID(id) != null; }

  public abstract T byID(String name);

  /**
   * @seex #addExerciseToList(mitll.langtest.shared.ExerciseShell)
   * @see #loadFirstExercise()
   * @see #loadNextExercise
   * @see #loadPreviousExercise
   * @param itemID
   */
  public void loadExercise(String itemID) { pushNewItem(itemID); }

  /**
   * This method is called whenever the application's history changes.
   * @see #pushNewItem(String)
   * @param event
   */
  public void onValueChange(ValueChangeEvent<String> event) {
    // Set the label to reflect the current history token. (?)
    String value = event.getValue();
    String token = getTokenFromEvent(event);
    String id = getIDFromToken(token);
    System.out.println("ExerciseList.onValueChange got " + event.getAssociatedType() +
      " "+ value + " token " + token + " id '" + id +"'");

    if (id.length() > 0) {
      checkAndAskServer(id);
    } else {
      System.out.println("ExerciseList.onValueChange : got invalid event " + event + " value " + token);
    }
  }

  @Override
  public void checkAndAskServer(String id) {
    if (hasExercise(id)) {
      askServerForExercise(id);
    }
    else {
      System.err.println("checkAndAskServer : can't load " +id + " keys were " + getKeys());
    }
  }

  protected abstract Set<String> getKeys();

  protected String getTokenFromEvent(ValueChangeEvent<String> event) {
    String token = event.getValue();
    token = allowPlusInURL ? unencodeTokenDontRemovePlus(token) : unencodeToken(token);
    return token;
  }

  /**
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param token
   * @return
   */
  private String getIDFromToken(String token) {
    if (token.startsWith("#item=") || token.startsWith("item=")) {
      SelectionState selectionState = new SelectionState(token, !allowPlusInURL);
      if (!selectionState.getInstance().equals(instance)) {
        //System.out.println("got history item for another instance '" + selectionState.getInstance() + "' vs me '" + instance +"'");
      } else {
        String item = selectionState.getItem();
        System.out.println("got history item for instance '" + selectionState.getInstance() + " : '" + item+"'");
        return item;
      }
    }
    return "";
  }

  protected boolean loadByID(String id) {
    if (hasExercise(id)) {
      System.out.println("loading exercise " + id);
      loadExercise(id);
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * @see #checkAndAskServer(String)
   * @param itemID
   */
  protected void askServerForExercise(String itemID) {
    System.out.println("ExerciseList.askServerForExercise id = " + itemID);
    service.getExercise(itemID, new ExerciseAsyncCallback());
  }

  private class ExerciseAsyncCallback implements AsyncCallback<Exercise> {
    /**
     * @see ExerciseList#askServerForExercise(String)
     * @paramx exerciseShell
     */
    //public ExerciseAsyncCallback(T exerciseShell) { this.exerciseShell = exerciseShell; }

    @Override
    public void onFailure(Throwable caught) {
      if (caught instanceof IncompatibleRemoteServiceException) {
        Window.alert("This application has recently been updated.\nPlease refresh this page, or restart your browser." +
          "\nIf you still see this message, clear your cache. (" +caught.getMessage()+
          ")");
      }
      else {
        if (!caught.getMessage().trim().equals("0")) {
          Window.alert("Message from server: " + caught.getMessage());
        }
        System.out.println("ex " + caught.getMessage() + " " + caught);
      }
    }
    @Override
    public void onSuccess(Exercise result)  {
      if (result == null) {
        Window.alert("Unfortunately there's a configuration error and we can't find this exercise.");
      }
      else {
        useExercise(result);
      }
    }
  }

  /**
   * @see ExerciseAsyncCallback#onSuccess(mitll.langtest.shared.Exercise)
   * @param result
   */
  protected void useExercise(Exercise result) {
    createdPanel = makeExercisePanel(result);
    String itemID = result.getID();
    markCurrentExercise(itemID);
    System.out.println("ExerciseList.useExercise : " + itemID + " currentExercise " +getCurrentExercise() + " or " + getCurrentExerciseID());
  }

  public String getCurrentExerciseID() { return getCurrentExercise() != null ? getCurrentExercise().getID() : "Unknown"; }

  protected abstract  T getCurrentExercise();

  /**
   * @see #useExercise(mitll.langtest.shared.Exercise)
   * @param result
   */
  public Panel makeExercisePanel(Exercise result) {
  //  System.out.println("ExerciseList.makeExercisePanel : " +result);

    Panel exercisePanel = factory.getExercisePanel(result);
    innerContainer.setWidget(exercisePanel);
    //innerContainer.getParent().addStyleName("shadowBorder");
    return exercisePanel;
  }

  protected boolean isExercisePanelBusy() {
    Widget current = innerContainer.getWidget();
    return current != null && current instanceof BusyPanel && ((BusyPanel) current).isBusy();
  }

  /**
   * @see ListInterface#loadNextExercise(mitll.langtest.shared.ExerciseShell)
   * @param current
   */
  protected void getNextExercise(ExerciseShell current) {
   // System.out.println("ExerciseList.getNextExercise " + current);

    int i = getIndex(current.getID());
    if (i == -1) {
      System.err.println("ExerciseList.getNextExercise : huh? couldn't find " + current +
          " in " + getSize() + " exercises : " + getKeys());
    } else {
      T next = getAt(i + 1);
      loadExercise(next.getID());
    }
  }

  /**
   * @see #useExercise(mitll.langtest.shared.Exercise)
   * @param currentID
   * @return
   */
  private int getIndex(String currentID) {
    T shell = byID(currentID);
    int i = shell != null ? getRealIndex(shell) : -1;
    //System.out.println("getIndex " + currentID + " = " +i);

    return i;
  }

  protected abstract int getRealIndex(T t);
  protected abstract T getAt(int i);

  @Override
  public int getPercentComplete() {
    float ratio = (float) visited.size() / (float) getSize();
    System.out.println("Ratio " + ratio);
    return (int) (Math.ceil(100f * ratio));
  }

  @Override
  public int getComplete() {  return visited.size(); }

  /**
   * @see #removeExercise(mitll.langtest.shared.ExerciseShell)
   */
  @Override
  public void removeCurrentExercise() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (!innerContainer.remove(current)) {
        System.err.println("\tdidn't remove current widget");
      }
      else innerContainer.getParent().removeStyleName("shadowBorder");
    }
    else {
      System.err.println("\tno inner current widget for " + report());
    }
  }

  protected String report() {
    return "list " + instance + " id " + getElement().getId();
  }

  protected void removeComponents() { super.clear();  }

  @Override
  public void clear() { removeComponents(); }

  protected abstract void markCurrentExercise(String itemID);

  @Override
  public boolean loadNext() { return loadNextExercise(getCurrentExerciseID()); }

  /**
   * @seex NavigationHelper#loadNextExercise
   * @param current
   * @return
   */
  @Override
  public <S extends ExerciseShell> boolean loadNextExercise(S current) {
    System.out.println("ExerciseList.loadNextExercise current is : " +current);
    String id = current.getID();
    int i = getIndex(id);

    visited.add(i);

    boolean onLast = isOnLastItem(i);
    System.out.println("ExerciseList.loadNextExercise current is : " + id + " index " +i +
      " of " + getSize() +" last is " + (getSize() - 1)+" on last " + onLast);

    if (onLast) {
      onLastItem();
    }
    else {
      getNextExercise(current);
    }
    if (showTurkToken && (onLast || ++countSincePrompt % NUM_QUESTIONS_FOR_TOKEN == 0)) {
      showTurkToken(current);
    }
    return onLast;
  }

  public boolean loadNextExercise(String id) {
    System.out.println("ExerciseList.loadNextExercise id = " + id);
    T exerciseByID = byID(id);
    return exerciseByID != null && loadNextExercise(exerciseByID);
  }

  /**
   * @see ListInterface#loadNextExercise(mitll.langtest.shared.ExerciseShell)
   */
  protected void onLastItem() {
    PropertyHandler props = controller.getProps();
    if (props.isCRTDataCollectMode() || props.isDataCollectMode()) {
      feedback.showErrorMessage("Test Complete", "Test Complete! Thank you!");
    }
    else {
      loadFirstExercise();
    }
  }

  /**
   * So a turker can get credit for their work.
   * @param current
   */
  private void showTurkToken(ExerciseShell current) {
    String code = user.getUser() + "_" + current.getID();

    // Create a basic popup widget
    final DecoratedPopupPanel simplePopup = new DecoratedPopupPanel(true);
    simplePopup.ensureDebugId("cwBasicPopup-simplePopup");
    simplePopup.setWidth("250px");
    simplePopup.setWidget(new HTML("To receive credit, copy and paste this token : " + code.hashCode() + "<br/>Click on the page to dismiss.<br/>"));
    simplePopup.setPopupPosition(Window.getClientWidth()/2,Window.getClientHeight()/2);
    simplePopup.show();
  }

  @Override
  public boolean loadPrev() { return loadPreviousExercise(getCurrentExercise()); }

  /**
   * @seex NavigationHelper#loadPreviousExercise
   * @param current
   * @return true if on first
   */
  @Override
  public <S extends ExerciseShell> boolean loadPreviousExercise(S current) {
    int i = getIndex(current.getID());
    boolean onFirst = i == 0;
    if (!onFirst) {
      loadExercise(getAt(i - 1).getID());
    }
    return onFirst;
  }

  /**
   * @see mitll.langtest.client.ExerciseListLayout#addExerciseListOnLeftSide(com.google.gwt.user.client.ui.Panel)
   * @param props
   * @return
   */
  @Override
  public Widget getExerciseListOnLeftSide(PropertyHandler props) {
    FlowPanel leftColumn = new FlowPanel();
    leftColumn.addStyleName("floatLeft");
    leftColumn.addStyleName("minWidth");
    DOM.setStyleAttribute(leftColumn.getElement(), "paddingRight", "10px");

    if (!props.isFlashcardTeacherView() && !props.isMinimalUI()) {
      Heading items = new Heading(4, ITEMS);
      items.addStyleName("center");
      leftColumn.add(items);
    }
    leftColumn.add(getWidget());
    return leftColumn;
  }

  @Override
  public Widget getWidget() {   return this;  }

  @Override
  public boolean onFirst() {  return onFirst(getCurrentExercise());  }

  /**
   * @see mitll.langtest.client.exercise.NavigationHelper#makePrevButton
   * @param current
   * @return
   */
  @Override
  public <S extends ExerciseShell> boolean onFirst(S current) {
    boolean b = current == null || getSize() == 1 || getIndex(current.getID()) == 0;
    System.out.println("onFirst : of " +getSize() +", on checking " + current + " = " + b);
    return b;
  }


  public boolean onLast() { return onLast(getCurrentExercise());  }

  @Override
  public <S extends ExerciseShell> boolean onLast(S current) {

    boolean b = current == null || getSize() == 1 || isOnLastItem(getIndex(current.getID()));
    System.out.println("onLast : of " +getSize() +", on checking " + current + " = " + b);
    return b;
  }

  private boolean isOnLastItem(int i) { return i == getSize() - 1;  }

  @Override
  public void reloadExercises() { loadFirstExercise();  }
  public void redraw() {}

 private  List<ListChangeListener<T>> listeners = new ArrayList<ListChangeListener<T>>();
  @Override
  public void addListChangedListener(ListChangeListener<T> listener) {
    listeners.add(listener);
  }
}
