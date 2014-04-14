package mitll.langtest.client.list;

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
import mitll.langtest.client.custom.EditItem;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseListWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
public abstract class ExerciseList extends VerticalPanel implements ListInterface, ProvidesResize,
  ValueChangeHandler<String> {
  private static final int NUM_QUESTIONS_FOR_TOKEN = 5;

  private SimplePanel innerContainer;
  protected final LangTestDatabaseAsync service;
  protected final UserFeedback feedback;
  private ExercisePanelFactory factory;
  private final ExerciseController controller;

  protected Panel createdPanel;
  protected UserManager user;
  private String exercise_title;
  private final boolean showTurkToken;
  private int countSincePrompt = 0;
  int lastReqID = 0;
  private final Set<Integer> visited = new HashSet<Integer>();
  final boolean allowPlusInURL;
  public final String instance;

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
  ExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
               ExercisePanelFactory factory,
               ExerciseController controller,
               boolean showTurkToken, boolean showInOrder, String instance) {
    addWidgets(currentExerciseVPanel);
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
    this.showTurkToken = showTurkToken;
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
    innerContainer.getElement().setId("ExerciseList_innerContainer");
    currentExerciseVPanel.add(innerContainer);
    innerContainer.addStyleName("floatLeft");
    currentExerciseVPanel.addStyleName("floatLeft");
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
    System.out.println("ExerciseList.getExercises for user " +userID + " instance " + instance);
    lastReqID++;
    service.getExerciseIds(lastReqID, new HashMap<String, Collection<String>>(), "", -1, controller.getUser(), new SetExercisesCallback());
  }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#doAfterEditComplete(ListInterface, boolean)
   */
  public void reload() {
    System.out.println("ExerciseList.reload for user " + controller.getUser() + " instance " + instance + " id " + getElement().getId());
    service.getExerciseIds(lastReqID, new HashMap<String, Collection<String>>(), "", -1,  controller.getUser(), new SetExercisesCallback());
  }

  /**
   * After re-fetching the ids, select this one.
   * @see mitll.langtest.client.custom.EditableExercise#doAfterEditComplete(ListInterface, boolean)
   * @param id
   */
  @Override
  public void reloadWith(String id) {
    System.out.println("ExerciseList.reloadWith id = " + id+ " for user " + controller.getUser() + " instance " + instance);
    service.getExerciseIds(lastReqID, new HashMap<String, Collection<String>>(), "", -1, controller.getUser(), new SetExercisesCallbackWithID(id));
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
      "' vs new exercise " +exerciseID +" instance " + instance);
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
  String getSelectionFromToken(String token) {
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
  void pushNewItem(String exerciseID) {
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
  class SetExercisesCallback implements AsyncCallback<ExerciseListWrapper> {
    public SetExercisesCallback() {}

    public void onFailure(Throwable caught) {
      if (!caught.getMessage().trim().equals("0")) {
        feedback.showErrorMessage("Server error", "Please clear your cache and reload the page.");
      }
      System.out.println("Got exception '" +caught.getMessage() + "' " +caught);
    }

    public void onSuccess(ExerciseListWrapper result) {
      System.out.println("\tExerciseList.SetExercisesCallback Got " + result.getExercises().size() + " results");
      if (isStaleResponse(result)) {
        System.out.println("----> SetExercisesCallback.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
      } else {
        if (result.getExercises().isEmpty()) {
          gotEmptyExerciseList();
        }

        rememberAndLoadFirst(result.getExercises(), result.getFirstExercise());

        controller.showProgress();
      }
    }
  }

  class SetExercisesCallbackWithID implements AsyncCallback<ExerciseListWrapper> {
    private String id;

    public SetExercisesCallbackWithID(String id) {
      this.id = id;
      System.out.println("ExerciseList.SetExercisesCallbackWithID id = " + id);
    }

    public void onFailure(Throwable caught) {
      if (!caught.getMessage().trim().equals("0")) {
        feedback.showErrorMessage("Server error", "Please clear your cache and reload the page.");
      }
      System.out.println("Got exception '" + caught.getMessage() + "' " + caught);
    }

    public void onSuccess(ExerciseListWrapper result) {
      System.out.println("\tExerciseList.SetExercisesCallbackWithID Got " + result.getExercises().size() + " results, id = " + id);
      if (isStaleResponse(result)) {
        System.out.println("----> SetExercisesCallbackWithID.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
      } else {
        if (result.getExercises().isEmpty()) {
          gotEmptyExerciseList();
        }
        List<CommonShell> exercises = result.getExercises();
        rememberExercises(exercises);
        for (ListChangeListener<CommonShell> listener : listeners) listener.listChanged(exercises);
        pushFirstSelection(id);
        controller.showProgress();
      }
    }
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() {
   // System.out.println(new Date() + " gotEmptyExerciseList : ------  ------------ ");
  }

  public void rememberAndLoadFirst(List<CommonShell> exercises) {
    //System.out.println(new Date() + " rememberAndLoadFirst : exercises " + exercises.size());

    rememberAndLoadFirst(exercises, null);
  }

  /**
   * @param exercises
   * @see ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @see #rememberAndLoadFirst(java.util.List)
   */
  public void rememberAndLoadFirst(List<CommonShell> exercises, CommonExercise firstExercise) {
    System.out.println("ExerciseList : rememberAndLoadFirst " + instance+
      " remembering " + exercises.size() + " : first = " +firstExercise);

    rememberExercises(exercises);
    for (ListChangeListener<CommonShell> listener : listeners) listener.listChanged(exercises);

    if (firstExercise != null) {
      CommonShell firstExerciseShell = findFirstExercise();
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

  boolean isStaleResponse(ExerciseListWrapper result) {
    return result.getReqID() < lastReqID;
  }

  protected abstract void rememberExercises(List<CommonShell> result);
  protected abstract int getSize();

  /**
   * Worry about deleting the currently visible item.
   *
   * @see mitll.langtest.client.list.PagingExerciseList#forgetExercise(String)
   * @param es
   */
  public CommonShell removeExercise(CommonShell es) {
    String id = es.getID();
    CommonShell current = getCurrentExercise();
    if (current.getID().equals(id)) {
      if (!onLast(current)) {
        loadNextExercise(current);
      }
      else if (!onFirst(current)) {
        loadPreviousExercise(current);
      }
    }

    return simpleRemove(id);
  }

  @Override
  public void hide() {  getParent().setVisible(false);  }
  public void show() {  getParent().setVisible(true);  }

  /**
   * @see #rememberAndLoadFirst
   */
  protected void loadFirstExercise() {
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      noMatches();
      System.out.println("loadFirstExercise : current exercises is empty?");
      removeCurrentExercise();
    } else {
      CommonShell toLoad = findFirstExercise();

      if (exercise_title != null) {
        CommonShell e = byID(exercise_title);
        if (e != null) toLoad = e;
      }

      //System.out.println("loadFirstExercise ex id =" + toLoad.getID() + " instance " + instance);
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

  CommonShell findFirstExercise() {
    return getFirst();
  }

  protected abstract CommonShell getFirst();

  boolean hasExercise(String id) { return byID(id) != null; }

  public abstract CommonShell byID(String name);

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
      " "+ value + " token " + token + " id '" + id +"'" + " instance " + instance);

    if (id.length() > 0) {
      checkAndAskServer(id);
    } else {
      System.out.println("ExerciseList.onValueChange : got invalid event " + event + " value " + token + " id '" + id+
          "'");
    }
  }

  @Override
  public void checkAndAskServer(String id) {
    if (hasExercise(id)) {
      askServerForExercise(id);
    }
    else if (!id.equals(EditItem.NEW_EXERCISE_ID)) {
      System.err.println("checkAndAskServer : can't load " +id);// + " keys were " + getKeys());
    }
  }

  protected abstract Set<String> getKeys();

  String getTokenFromEvent(ValueChangeEvent<String> event) {
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
    System.out.println("ExerciseList.askServerForExercise id = " + itemID + " instance " + instance);
    service.getExercise(itemID, controller.getUser(), new ExerciseAsyncCallback());
  }

  private class ExerciseAsyncCallback implements AsyncCallback<CommonExercise> {
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
    public void onSuccess(CommonExercise result)  {
      if (result == null) {
        Window.alert("Unfortunately there's a configuration error and we can't find this exercise.");
      }
      else {
        useExercise(result);
      }
    }
  }

  /**
   * @see ExerciseAsyncCallback#onSuccess(mitll.langtest.shared.CommonExercise)
   * @param result
   */
  protected void useExercise(CommonExercise result) {
    //System.out.println("ExerciseList.useExercise : result " +result);

    createdPanel = makeExercisePanel(result);
    String itemID = result.getID();
    markCurrentExercise(itemID);
    System.out.println("ExerciseList.useExercise : item id " + itemID + " currentExercise " +getCurrentExercise() +
      " or " + getCurrentExerciseID() + " instance " + instance);
  }

  public String getCurrentExerciseID() { return getCurrentExercise() != null ? getCurrentExercise().getID() : "Unknown"; }

  public abstract CommonShell getCurrentExercise();

  /**
   * @see #useExercise(mitll.langtest.shared.CommonExercise)
   * @param exercise
   */
  public Panel makeExercisePanel(CommonExercise exercise) {
    System.out.println("ExerciseList.makeExercisePanel : " +exercise + " instance " + instance);

    Panel exercisePanel = factory.getExercisePanel(exercise);
    innerContainer.setWidget(exercisePanel);
    return exercisePanel;
  }

  boolean isExercisePanelBusy() {
    Widget current = innerContainer.getWidget();
    return current != null && current instanceof BusyPanel && ((BusyPanel) current).isBusy();
  }

  /**
   * @see ListInterface#loadNextExercise
   * @param current
   */
  protected void getNextExercise(CommonShell current) {
   // System.out.println("ExerciseList.getNextExercise " + current);

    int i = getIndex(current.getID());
    if (i == -1) {
      System.err.println("ExerciseList.getNextExercise : huh? couldn't find " + current +
          " in " + getSize() + " exercises : " + getKeys());
    } else {
      CommonShell next = getAt(i + 1);
      loadExercise(next.getID());
    }
  }

  /**
   * @see #useExercise(mitll.langtest.shared.CommonExercise)
   * @param currentID
   * @return
   */
  private int getIndex(String currentID) {
    CommonShell shell = byID(currentID);
    int i = shell != null ? getRealIndex(shell) : -1;
    //System.out.println("getIndex " + currentID + " = " +i);
    return i;
  }

  protected abstract int getRealIndex(CommonShell t);
  protected abstract CommonShell getAt(int i);

/*  @Override
  public int getPercentComplete() {
    float ratio = (float) visited.size() / (float) getSize();
    System.out.println("Ratio " + ratio);
    return (int) (Math.ceil(100f * ratio));
  }*/

/*  @Override
  public int getComplete() {  return visited.size(); }*/

  /**
   * @see #removeExercise
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
      System.err.println("\tremoveCurrentExercise : no inner current widget for " + report());
    }
  }

  String report() {
    return "list " + instance + " id " + getElement().getId();
  }

  void removeComponents() { super.clear();  }

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
  public  boolean loadNextExercise(CommonShell current) {
    System.out.println("ExerciseList.loadNextExercise current is : " +current + " instance " + instance);
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
    System.out.println("ExerciseList.loadNextExercise id = " + id + " instance " + instance);
    CommonShell exerciseByID = byID(id);
    return exerciseByID != null && loadNextExercise(exerciseByID);
  }

  /**
   * @see ListInterface#loadNextExercise
   */
  protected void onLastItem() {
    PropertyHandler props = controller.getProps();
    if (props.isCRTDataCollectMode() || props.isDataCollectMode()) {
      String title = props.isDataCollectMode() ? "Collection Complete" : "Test complete";
      feedback.showErrorMessage(title, "All Items Complete! Thank you!");
    }
    else {
      loadFirstExercise();
    }
  }

  /**
   * So a turker can get credit for their work.
   * @param current
   */
  private void showTurkToken(CommonShell current) {
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
  public boolean loadPreviousExercise(CommonShell current) {
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
    Panel leftColumn = new FlowPanel();
    leftColumn.getElement().setId("ExerciseList_leftColumn");
    leftColumn.addStyleName("floatLeft");
    addMinWidthStyle(leftColumn);

    leftColumn.add(getWidget());
    return leftColumn;
  }

  protected void addMinWidthStyle(Panel leftColumn) {
    leftColumn.addStyleName("minWidth");
    DOM.setStyleAttribute(leftColumn.getElement(), "paddingRight", "10px");
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
  public boolean onFirst(CommonShell current) {
    boolean b = current == null || getSize() == 1 || getIndex(current.getID()) == 0;
    //System.out.println("onFirst : of " +getSize() +", on checking " + current + " = " + b);
    return b;
  }

  public boolean onLast() { return onLast(getCurrentExercise());  }

  @Override
  public boolean onLast(CommonShell current) {
    boolean b = current == null || getSize() == 1 || isOnLastItem(getIndex(current.getID()));
    //System.out.println("onLast  : of " +getSize() +", on checking " + current + " = " + b);
    return b;
  }

  private boolean isOnLastItem(int i) { return i == getSize() - 1;  }

  @Override
  public void reloadExercises() { loadFirstExercise();  }
  public void redraw() {}

  private final List<ListChangeListener<CommonShell>> listeners = new ArrayList<ListChangeListener<CommonShell>>();
  @Override
  public void addListChangedListener(ListChangeListener<CommonShell> listener) {  listeners.add(listener);  }
}
