package mitll.langtest.client.list;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
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
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles left side of NetPron2 -- which exercise is the current one, highlighting, etc.
 * <p/>
 * User: GO22670
 * Date: 7/9/12
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExerciseList extends VerticalPanel implements ListInterface, ProvidesResize,
  ValueChangeHandler<String> {
  private static final Map<String, Collection<String>> TYPE_TO_SELECTION = new HashMap<String, Collection<String>>();
  private static final int MAX_MSG_LEN = 200;

  private SimplePanel innerContainer;
  protected final LangTestDatabaseAsync service;
  protected final UserFeedback feedback;
  private ExercisePanelFactory factory;
  private final ExerciseController controller;

  protected Panel createdPanel;
  protected UserManager user;
  int lastReqID = 0;
  final boolean allowPlusInURL;
  private String instance;
  private final List<ListChangeListener<CommonShell>> listeners = new ArrayList<ListChangeListener<CommonShell>>();
  protected boolean doShuffle;

  /**
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param controller
   * @param instance
   * @see mitll.langtest.client.LangTest#makeExerciseList
   */
  ExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
               ExercisePanelFactory factory,
               ExerciseController controller,
               String instance) {
    addWidgets(currentExerciseVPanel);
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
    this.allowPlusInURL = controller.getProps().shouldAllowPlusInURL();
    this.controller = controller;
    this.instance = instance;
    getElement().setId("ExerciseList_" + instance);

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
   * @param currentExerciseVPanel
   * @see #ExerciseList(com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExercisePanelFactory, mitll.langtest.client.exercise.ExerciseController, String)
   */
  private void addWidgets(final Panel currentExerciseVPanel) {
    this.innerContainer = new SimplePanel();
    innerContainer.getElement().setId("ExerciseList_innerContainer");
    currentExerciseVPanel.add(innerContainer);
    innerContainer.addStyleName("floatLeft");
    currentExerciseVPanel.addStyleName("floatLeft");
  }

  /**
   * @param factory
   * @param user
   * @param expectedGrades
   * @see mitll.langtest.client.LangTest#reallySetFactory()
   */
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    this.factory = factory;
    this.user = user;
  }

  /**
   * Get exercises for this user.
   *
   * @param userID
   * @return true if we asked the server for exercises
   * @see mitll.langtest.client.LangTest#doEverythingAfterFactory(long)
   * @see mitll.langtest.client.list.HistoryExerciseList#noSectionsGetExercises(long)
   */
  public boolean getExercises(long userID) {
    System.out.println("ExerciseList.getExercises for user " + userID + " instance " + instance);
    lastReqID++;
    service.getExerciseIds(lastReqID, TYPE_TO_SELECTION, "", -1, controller.getUser(), getRole(), false, false, new SetExercisesCallback(""));
    return true;
  }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#doAfterEditComplete(ListInterface, boolean)
   * @see mitll.langtest.client.LangTest#doEverythingAfterFactory(long)
   */
  public void reload() {
    System.out.println("ExerciseList.reload for user " + controller.getUser() + " instance " + instance + " id " + getElement().getId());
    service.getExerciseIds(lastReqID, TYPE_TO_SELECTION, "", -1, controller.getUser(), getRole(), false, false, new SetExercisesCallback(""));
  }

  /**
   * After re-fetching the ids, select this one.
   *
   * @param id
   * @see mitll.langtest.client.custom.EditableExercise#doAfterEditComplete(ListInterface, boolean)
   */
  @Override
  public void reloadWith(String id) {
    System.out.println("ExerciseList.reloadWith id = " + id + " for user " + controller.getUser() + " instance " + instance);
    service.getExerciseIds(lastReqID, TYPE_TO_SELECTION, "", -1, controller.getUser(), getRole(), false, false, new SetExercisesCallbackWithID(id));
  }

  /**
   * So we have a catch-22 -
   * <p/>
   * If we fire the current history, we override the initial selection associated
   * with a user logging in for the first time.
   * <p/>
   * If we don't, when we click on a link from an email, the item=NNN value will be ignored.
   * <p/>
   * I gotta go with the latter.
   *
   * @param exerciseID
   * @see #loadFirstExercise()
   */
  private void pushFirstSelection(String exerciseID) {
    String token = History.getToken();
    token = getSelectionFromToken(token);
    String idFromToken = getIDFromToken(token);
    System.out.println("ExerciseList.pushFirstSelection : current token '" + token + "' id from token '" + idFromToken +
      "' vs new exercise " + exerciseID + " instance " + instance);
    if (token != null && idFromToken.equals(exerciseID)) {
      System.out.println("\tpushFirstSelection : (" + instance +
        ") current token " + token + " same as new " + exerciseID);
      checkAndAskServer(exerciseID);
    } else {
      pushNewItem(exerciseID);
    }
  }

  /**
   * Deal with responseType being after ###
   *
   * @param token
   * @return
   */
  String getSelectionFromToken(String token) {
    return token;
  }

  /**
   * Calling this will result in an immediate call to onValueChange (reacting to the history change)
   *
   * @param exerciseID
   * @see ListInterface#loadExercise(String)
   * @see #pushFirstSelection(String)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  void pushNewItem(String exerciseID) {
    System.out.println("------------ ExerciseList.pushNewItem : (" + instance + ") push history " + exerciseID + " - ");
    History.newItem("#item=" + exerciseID + ";instance=" + instance);
  }

  public void onResize() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (current instanceof RequiresResize) {
        ((RequiresResize) current).onResize();
      }
    }
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
   * TODO : horrible hack here to get role of request.
   * @return
   */
  protected String getRole() {
    String audioTypeRecorder = Result.AUDIO_TYPE_RECORDER;
    return instance.startsWith("record") ? audioTypeRecorder : instance;
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  /**
   * @see ListInterface#getExercises(long)
   */
  class SetExercisesCallback implements AsyncCallback<ExerciseListWrapper> {
    private String selectionID;

    public SetExercisesCallback(String selectionID) {
      this.selectionID = selectionID;

      if (selectionID.equals("-1")) {
        new Exception().printStackTrace();
      }
    }

    public void onFailure(Throwable caught) {
      gotExercises(false);
      dealWithRPCError(caught);
    }

    public void onSuccess(ExerciseListWrapper result) {
      //System.out.println("\tExerciseList.SetExercisesCallback Got " + result.getExercises().size() + " results");
      if (isStaleResponse(result)) {
        // System.out.println("----> SetExercisesCallback.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
      } else {
        gotExercises(true);
        if (result.getExercises().isEmpty()) {
          gotEmptyExerciseList();
        }

        rememberAndLoadFirst(result.getExercises(), result.getFirstExercise(), selectionID);
        controller.showProgress();
      }
    }
  }

  protected abstract void gotExercises(boolean success);

  /**
   * @see #reloadWith(String)
   */
  private class SetExercisesCallbackWithID implements AsyncCallback<ExerciseListWrapper> {
    private String id;

    public SetExercisesCallbackWithID(String id) {
      this.id = id;
      System.out.println("ExerciseList.SetExercisesCallbackWithID id = " + id);
    }

    public void onFailure(Throwable caught) {
      gotExercises(false);
      dealWithRPCError(caught);
    }

    public void onSuccess(ExerciseListWrapper result) {
      System.out.println("\tExerciseList.SetExercisesCallbackWithID Got " + result.getExercises().size() + " results, id = " + id);
      if (isStaleResponse(result)) {
        System.out.println("----> SetExercisesCallbackWithID.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
      } else {
        gotExercises(true);
        if (result.getExercises().isEmpty()) {
          gotEmptyExerciseList();
        }
        List<CommonShell> exercises = result.getExercises();
        exercises = rememberExercises(exercises);
        for (ListChangeListener<CommonShell> listener : listeners) {
          listener.listChanged(exercises, "");
        }
        pushFirstSelection(id);
        controller.showProgress();
      }
    }
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallback#onFailure(Throwable)
   * @param caught
   */
  private void dealWithRPCError(Throwable caught) {
    String message = caught.getMessage();
    if (message.length() > MAX_MSG_LEN) message = message.substring(0, MAX_MSG_LEN);
    if (!message.trim().equals("0")) {
      feedback.showErrorMessage("Server error", "Please clear your cache and reload the page. (" +
        message +
        ")");
    }
    System.out.println("ExerciseList.SetExercisesCallbackWithID Got exception '" + message + "' " + caught);

    caught.printStackTrace();
    controller.logMessageOnServer("got exception " + caught.getMessage(), " RPCerror?");
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() {
    // System.out.println(new Date() + " gotEmptyExerciseList : ------  ------------ ");
  }

  public void rememberAndLoadFirst(List<CommonShell> exercises) {
//    System.out.println(new Date() + " rememberAndLoadFirst : exercises " + exercises.size());
    rememberAndLoadFirst(exercises, null, "All");
  }

  /**
   * Calls remember exercises -- interacts with flashcard mode and the shuffle option there.
   * <p/>
   * Has override for headstart selection of a specific exercise.
   * <p/>
   * Previously we would first ask the server for the exercise list and then ask for the first exercise on
   * the list, making the user/client wait for both calls to finish before displaying the first item.
   * <p/>
   * Now the first exercise is in the {@link mitll.langtest.shared.ExerciseListWrapper#getFirstExercise()} returned with the exercise list on the first call.
   *
   * @param exercises     - exercise list
   * @param firstExercise - the initial exercise returned from getExercises
   * @param selectionID   - in the context of this selection
   * @see ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @see #rememberAndLoadFirst(java.util.List)
   */
  private void rememberAndLoadFirst(List<CommonShell> exercises, CommonExercise firstExercise, String selectionID) {
    System.out.println("ExerciseList : rememberAndLoadFirst instance '" + instance +
        "' remembering " + exercises.size() + " exercises, first = " + firstExercise);

    exercises = rememberExercises(exercises);
    for (ListChangeListener<CommonShell> listener : listeners) {
      listener.listChanged(exercises, selectionID);
    }

    String exercise_title1 = controller.getProps().getExercise_title();
    if (exercise_title1 != null) {
      CommonShell headstartExercise = byID(exercise_title1);
      if (headstartExercise != null) {
        loadExercise(exercise_title1);
        return;
      }
    }
    if (firstExercise != null) {
      CommonShell firstExerciseShell = findFirstExercise();
      if (firstExerciseShell.getID().equals(firstExercise.getID())) {
        //System.out.println("ExerciseList : rememberAndLoadFirst using first = " +firstExercise);
        useExercise(firstExercise);   // allows us to skip another round trip with the server to ask for the first exercise
      } else {
        //System.out.println("ExerciseList : rememberAndLoadFirst finding first...");
        loadFirstExercise();
      }
    } else {
      loadFirstExercise();
    }
  }

  boolean isStaleResponse(ExerciseListWrapper result) {
    return result.getReqID() < lastReqID;
  }

  protected abstract List<CommonShell> rememberExercises(List<CommonShell> result);

  /**
   * Worry about deleting the currently visible item.
   *
   * @param es
   * @see mitll.langtest.client.list.PagingExerciseList#forgetExercise(String)
   */
  public CommonShell removeExercise(CommonShell es) {
    String id = es.getID();
    CommonShell current = getCurrentExercise();
    if (current.getID().equals(id)) {
      if (!onLast(current)) {
        loadNextExercise(current);
      } else if (!onFirst(current)) {
        loadPreviousExercise(current);
      }
    }

    return simpleRemove(id);
  }

  @Override
  public void hide() {
    getParent().setVisible(false);
  }

  public void show() {
    getParent().setVisible(true);
  }

  /**
   * If we're not already showing this item, ask there server for the exercise.
   * Does this by pushing a history item and then noticing the history item change.
   * @see #rememberAndLoadFirst(java.util.List, mitll.langtest.shared.CommonExercise, String)
   */
  protected void loadFirstExercise() {
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      noMatches();
      System.out.println("loadFirstExercise : current exercises is empty?");
      removeCurrentExercise();
    } else {
      CommonShell toLoad = findFirstExercise();
     // System.out.println("loadFirstExercise ex id =" + toLoad.getID() + " instance " + instance);
      pushFirstSelection(toLoad.getID());
    }
  }

  protected abstract boolean isEmpty();

  /**
   */
  private void noMatches() {
    String message = isEmpty() ? "No items yet." : "No matches found. Please try a different search.";
    final PopupPanel pleaseWait = showPleaseWait(message, null);

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };

    // Schedule the timer to run once in 1 seconds.
    t.schedule(3000);
  }

  protected PopupPanel showPleaseWait(String message, UIObject widget) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(false);
    pleaseWait.add(new HTML(message));
    if (widget == null) {
      pleaseWait.center();
    }
    return pleaseWait;
  }

  /**
   * Sometimes you want to change which exercise is loaded first.
   * @return
   */
  protected CommonShell findFirstExercise() { return getFirst();  }

  protected abstract CommonShell getFirst();

  boolean hasExercise(String id) {
    return byID(id) != null;
  }

  public abstract CommonShell byID(String name);

  /**
   * @param itemID
   * @seex #addExerciseToList(mitll.langtest.shared.ExerciseShell)
   * @see #loadFirstExercise()
   * @see #loadNextExercise
   * @see #loadPreviousExercise
   */
  public void loadExercise(String itemID) { pushNewItem(itemID);  }

  /**
   * This method is called whenever the application's history changes.
   *
   * @param event
   * @see #pushNewItem(String)
   */
  public void onValueChange(ValueChangeEvent<String> event) {
    // Set the label to reflect the current history token. (?)
    String value = event.getValue();
    String token = getTokenFromEvent(event);
    String id = getIDFromToken(token);
    System.out.println("ExerciseList.onValueChange got " + event.getAssociatedType() +
      " " + value + " token " + token + " id '" + id + "'" + " instance " + instance);

    if (id.length() > 0) {
      checkAndAskServer(id);
    }
/*    else {
      System.out.println("ExerciseList.onValueChange : got invalid event " + event + " value " + token + " id '" + id+
          "'");
    }*/
  }

  @Override
  public void checkAndAskServer(String id) {
//    System.out.println("ExerciseList.checkAndAskServer - askServerForExercise = "  + id);
    if (hasExercise(id)) {
      askServerForExercise(id);
    } else if (!id.equals(EditItem.NEW_EXERCISE_ID)) {
      System.err.println("checkAndAskServer : can't load " + id);
    }
  }

  protected abstract Set<String> getKeys();

  String getTokenFromEvent(ValueChangeEvent<String> event) {
    String token = event.getValue();
    token = allowPlusInURL ? unencodeTokenDontRemovePlus(token) : unencodeToken(token);
    return token;
  }

  /**
   * @param token
   * @return
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private String getIDFromToken(String token) {
    if (token.startsWith("#item=") || token.startsWith("item=")) {
      SelectionState selectionState = new SelectionState(token, !allowPlusInURL);
      if (!selectionState.getInstance().equals(instance)) {
        //System.out.println("got history item for another instance '" + selectionState.getInstance() + "' vs me '" + instance +"'");
      } else {
        String item = selectionState.getItem();
        // System.out.println("got history item for instance '" + selectionState.getInstance() + " : '" + item+"'");
        return item;
      }
    }
    return "";
  }

  protected boolean loadByID(String id) {
    if (hasExercise(id)) {
      // System.out.println("loading exercise " + id);
      loadExercise(id);
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param itemID
   * @see #checkAndAskServer(String)
   */
  protected void askServerForExercise(String itemID) {
    System.out.println("ExerciseList.askServerForExercise id = " + itemID + " instance " + instance);
    controller.checkUser();
    service.getExercise(itemID, controller.getUser(), new ExerciseAsyncCallback());
  }

  private class ExerciseAsyncCallback implements AsyncCallback<CommonExercise> {
    @Override
    public void onFailure(Throwable caught) {
      if (caught instanceof IncompatibleRemoteServiceException) {
        Window.alert("This application has recently been updated.\nPlease refresh this page, or restart your browser." +
          "\nIf you still see this message, clear your cache. (" + caught.getMessage() +
          ")");
      } else {
        if (!caught.getMessage().trim().equals("0")) {
          Window.alert("Message from server: " + caught.getMessage());
        }
        System.out.println("ex " + caught.getMessage() + " " + caught);
      }
    }

    @Override
    public void onSuccess(CommonExercise result) {
      if (result == null) {
        Window.alert("Unfortunately there's a configuration error and we can't find this exercise.");
      } else {
        useExercise(result);
      }
    }
  }

  /**
   * @param commonExercise
   * @see #rememberAndLoadFirst(java.util.List, mitll.langtest.shared.CommonExercise, String)
   * @see ExerciseAsyncCallback#onSuccess(mitll.langtest.shared.CommonExercise)
   */
  protected void useExercise(CommonExercise commonExercise) {
    //System.out.println("ExerciseList.useExercise : commonExercise " +commonExercise);
    String itemID = commonExercise.getID();
    markCurrentExercise(itemID);
    createdPanel = makeExercisePanel(commonExercise);
/*    System.out.println("ExerciseList.useExercise : item id " + itemID + " currentExercise " +getCurrentExercise() +
      " or " + getCurrentExerciseID() + " instance " + instance);*/
  }

  public String getCurrentExerciseID() {
    return getCurrentExercise() != null ? getCurrentExercise().getID() : "Unknown";
  }

  public abstract CommonShell getCurrentExercise();

  /**
   * @param exercise
   * @see #useExercise(mitll.langtest.shared.CommonExercise)
   */
  public Panel makeExercisePanel(CommonExercise exercise) {
    System.out.println("ExerciseList.makeExercisePanel : " + exercise + " instance " + instance);

    Panel exercisePanel = factory.getExercisePanel(exercise);
    innerContainer.setWidget(exercisePanel);
    return exercisePanel;
  }


  /**
   * @return
   * @see mitll.langtest.client.list.PagingExerciseList#gotClickOnItem
   */
  boolean isExercisePanelBusy() {
    Widget current = innerContainer.getWidget();
    return current != null && current instanceof BusyPanel && ((BusyPanel) current).isBusy();
  }

  /**
   * @param current
   * @see ListInterface#loadNextExercise
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
   * @param currentID
   * @return
   * @see #useExercise(mitll.langtest.shared.CommonExercise)
   */
  private int getIndex(String currentID) {
    CommonShell shell = byID(currentID);
    int i = shell != null ? getRealIndex(shell) : -1;
   // System.out.println("getIndex " + currentID + " = " +i);
    return i;
  }

  protected abstract int getRealIndex(CommonShell t);

  protected abstract CommonShell getAt(int i);

  @Override
  public int getPercentComplete() {
    float ratio = (float) getIndex(getCurrentExerciseID()) / (float) getSize();
   // System.out.println("Ratio " + ratio);
    return (int) (Math.ceil(100f * Math.abs(ratio)));
  }

  @Override
  public int getComplete() {  return  getIndex(getCurrentExerciseID()); }

  /**
   * @see #removeExercise
   */
  @Override
  public void removeCurrentExercise() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (!innerContainer.remove(current)) {
        System.err.println("\tdidn't remove current widget");
      } else innerContainer.getParent().removeStyleName("shadowBorder");
    } else {
      System.err.println("\tremoveCurrentExercise : no inner current widget for " + report());
    }
  }

  String report() {
    return "list " + instance + " id " + getElement().getId();
  }

  void removeComponents() {
    super.clear();
  }

  @Override
  public void clear() {
    removeComponents();
  }

  protected abstract void markCurrentExercise(String itemID);

  @Override
  public boolean loadNext() {
    return loadNextExercise(getCurrentExerciseID());
  }

  /**
   * @param current
   * @return
   * @seex NavigationHelper#loadNextExercise
   */
  @Override
  public boolean loadNextExercise(CommonShell current) {
    //System.out.println("ExerciseList.loadNextExercise current is : " +current + " instance " + instance);
    String id = current.getID();
    int i = getIndex(id);
    boolean onLast = isOnLastItem(i);
/*    System.out.println("ExerciseList.loadNextExercise current is : " + id + " index " + i +
        " of " + getSize() + " last is " + (getSize() - 1) + " on last " + onLast);*/

    if (onLast) {
      onLastItem();
    } else {
      getNextExercise(current);
    }

    return onLast;
  }

  public boolean loadNextExercise(String id) {
    //System.out.println("ExerciseList.loadNextExercise id = " + id + " instance " + instance);
    CommonShell exerciseByID = byID(id);
    return exerciseByID != null && loadNextExercise(exerciseByID);
  }

  /**
   * @see ListInterface#loadNextExercise
   */
  protected void onLastItem() {  loadFirstExercise();  }

  @Override
  public boolean loadPrev() {
    return loadPreviousExercise(getCurrentExercise());
  }

  /**
   * @param current
   * @return true if on first
   * @seex NavigationHelper#loadPreviousExercise
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
   * @param props
   * @return
   * @see mitll.langtest.client.ExerciseListLayout#addExerciseListOnLeftSide(com.google.gwt.user.client.ui.Panel)
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
    leftColumn.getElement().getStyle().setPaddingRight(10, Style.Unit.PX);
  }

  @Override
  public Widget getWidget() {
    return this;
  }

  @Override
  public boolean onFirst() {
    return onFirst(getCurrentExercise());
  }

  /**
   * @param current
   * @return
   * @see mitll.langtest.client.exercise.NavigationHelper#makePrevButton
   */
  public boolean onFirst(CommonShell current) {
    boolean b = current == null || getSize() == 1 || getIndex(current.getID()) == 0;
    //System.out.println("onFirst : of " +getSize() +", on checking " + current + " = " + b);
    return b;
  }

  public boolean onLast() {
    return onLast(getCurrentExercise());
  }

  @Override
  public boolean onLast(CommonShell current) {
    boolean b = current == null || getSize() == 1 || isOnLastItem(getIndex(current.getID()));
    return b;
  }

  private boolean isOnLastItem(int i) {
    return i == getSize() - 1;
  }

  @Override
  public void reloadExercises() {
    loadFirstExercise();
  }

  public void redraw() {}

  /**
   * @param listener
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallbackWithID#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @see #rememberAndLoadFirst
   */
  @Override
  public void addListChangedListener(ListChangeListener<CommonShell> listener) {
    listeners.add(listener);
  }

  protected abstract List<CommonShell> getInOrder();

  @Override
  public void setShuffle(boolean doShuffle) {
    simpleSetShuffle(doShuffle);
    rememberAndLoadFirst(getInOrder());
  }

  /**
   * @see mitll.langtest.client.flashcard.MyFlashcardExercisePanelFactory.StatsPracticePanel#StatsPracticePanel(mitll.langtest.shared.CommonExercise)
   * @param doShuffle
   */
  public void simpleSetShuffle(boolean doShuffle) {
    this.doShuffle = doShuffle;
  }
}
