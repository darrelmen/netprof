package mitll.langtest.client.list;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.Result;

import java.util.*;
import java.util.logging.Logger;

/**
 * Handles left side of NetPron2 -- which exercise is the current one, highlighting, etc.
 * <p>
 * User: GO22670
 * Date: 7/9/12
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExerciseList extends VerticalPanel implements ListInterface, ProvidesResize,
    ValueChangeHandler<String> {
  private final Logger logger = Logger.getLogger("ExerciseList");

  private static final Map<String, Collection<String>> TYPE_TO_SELECTION = new HashMap<String, Collection<String>>();
  private static final int MAX_MSG_LEN = 200;
  protected boolean incorrectFirstOrder = false;

  private SimplePanel innerContainer;
  protected final LangTestDatabaseAsync service;
  private final UserFeedback feedback;
  private ExercisePanelFactory factory;
  private final ExerciseController controller;

  protected Panel createdPanel;
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
   * @seex mitll.langtest.client.LangTest#makeExerciseList
   */
  protected ExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                         ExercisePanelFactory factory,
                         ExerciseController controller,
                         String instance, boolean incorrectFirst) {
    this.instance = instance;
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
    this.allowPlusInURL = controller.getProps().shouldAllowPlusInURL();
    this.controller = controller;
    this.incorrectFirstOrder = incorrectFirst;
    addWidgets(currentExerciseVPanel);
    getElement().setId("ExerciseList_" + instance);
  }

  private HandlerRegistration handlerRegistration;

  @Override
  protected void onLoad() {
    super.onLoad();
    addHistoryListener();
  }

  private void addHistoryListener() {
    if (handlerRegistration == null) {
      handlerRegistration = History.addValueChangeHandler(this);
    }
  }

  @Override
  public void removeHistoryListener() {
    if (handlerRegistration != null) {
      handlerRegistration.removeHandler();
      handlerRegistration = null;
    }
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    removeHistoryListener();
  }

  /**
   * @param currentExerciseVPanel
   * @see #ExerciseList
   */
  private void addWidgets(final Panel currentExerciseVPanel) {
//    logger.info("ExerciseList.addWidgets for currentExerciseVPanel " + currentExerciseVPanel.getElement().getId() + " instance " + getInstance());
    this.innerContainer = new SimplePanel();
    innerContainer.getElement().setId("ExerciseList_innerContainer");
    currentExerciseVPanel.add(innerContainer);
    innerContainer.addStyleName("floatLeft");
    currentExerciseVPanel.addStyleName("floatLeft");
  }

  /**
   * @param factory
   * @see mitll.langtest.client.LangTest#reallySetFactory()
   */
  public void setFactory(ExercisePanelFactory factory) {
    this.factory = factory;
    addHistoryListener();
  }

  /**
   * Get exercises for this user.
   *
   * @param userID
   * @return true if we asked the server for exercises
   * @see mitll.langtest.client.LangTest#configureUIGivenUser(long)
   * @see mitll.langtest.client.list.HistoryExerciseList#noSectionsGetExercises(long)
   */
  public boolean getExercises(long userID) {
    logger.info("ExerciseList.getExercises for user " + userID + " instance " + getInstance());
    lastReqID++;
    service.getExerciseIds(lastReqID, TYPE_TO_SELECTION, "", -1, controller.getUser(), getRole(), false, false,
        incorrectFirstOrder, false, new SetExercisesCallback(""));
    return true;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#doAfterEditComplete(ListInterface, boolean)
   * @see mitll.langtest.client.LangTest#configureUIGivenUser(long)
   */
  public void reload() {
    int user1 = controller.getUser();
//    logger.info("ExerciseList.reload for user " + user1);// + " instance " + instance + " id " + getElement().getId());
    service.getExerciseIds(lastReqID, TYPE_TO_SELECTION, "", -1, user1, getRole(), false, false,
        incorrectFirstOrder, false, new SetExercisesCallback(""));
  }

  /**
   * After re-fetching the ids, select this one.
   *
   * @param id
   * @see mitll.langtest.client.custom.dialog.EditableExercise#doAfterEditComplete(ListInterface, boolean)
   */
  @Override
  public void reloadWith(String id) {
//    logger.info("ExerciseList.reloadWith id = " + id + " for user " + controller.getUser() + " instance " + getInstance());
    service.getExerciseIds(lastReqID, TYPE_TO_SELECTION, "", -1, controller.getUser(), getRole(), false, false,
        incorrectFirstOrder, false, new SetExercisesCallbackWithID(id));
  }

  /**
   * So we have a catch-22 -
   * <p>
   * If we fire the current history, we override the initial selection associated
   * with a user logging in for the first time.
   * <p>
   * If we don't, when we click on a link from an email, the item=NNN value will be ignored.
   * <p>
   * I gotta go with the latter.
   *
   * @param exerciseID
   * @see #loadFirstExercise()
   */
  private void pushFirstSelection(String exerciseID) {
    String token = History.getToken();
    token = getSelectionFromToken(token);
    String idFromToken = getIDFromToken(token);
    logger.info("ExerciseList.pushFirstSelection : current token '" + token + "' id from token '" + idFromToken +
        "' vs new exercise " + exerciseID + " instance " + getInstance());
    if (token != null && idFromToken.equals(exerciseID)) {
      logger.info("\tpushFirstSelection : (" + getInstance() + ") current token " + token + " same as new " + exerciseID);
      checkAndAskServer(exerciseID);
    } else {
      logger.info("\tpushFirstSelection : (" + getInstance() + ") pushNewItem " + exerciseID);

      pushNewItem("", exerciseID);
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
   * @param search
   * @param exerciseID
   * @see ListInterface#loadExercise(String)
   * @see #pushFirstSelection(String)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  void pushNewItem(String search, String exerciseID) {
    logger.info("------------ ExerciseList.pushNewItem : (" + getInstance() + ") push history " + exerciseID + " - ");
    History.newItem("#" + "search=" + search + ";" +
        "item=" + exerciseID + ";instance=" + getInstance());
  }

  public void onResize() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (current instanceof RequiresResize) {
        //logger.info("resizing right side for " + instance + " "+ current.getClass());
        ((RequiresResize) current).onResize();
      }
//      else {
      //logger.warning("huh?  right side is not resizable " + instance + " "+ current.getClass());
      //    }
    }
    //   else {
//      logger.warning("huh? no right side of exercise list");
    //  }
  }

  private String unencodeToken(String token) {
    token = unencodeTokenDontRemovePlus(token).replaceAll("\\+", " ");
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
   *
   * @return
   */
  protected String getRole() {
    String audioTypeRecorder = Result.AUDIO_TYPE_RECORDER;
    return getInstance() == null || getInstance().startsWith("record") ? audioTypeRecorder : getInstance();
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  @Override
  public boolean isPendingReq() {
    return pendingReq;
  }

  /**
   * @see ListInterface#getExercises(long)
   */
  class SetExercisesCallback implements AsyncCallback<ExerciseListWrapper> {
    private final String selectionID;

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
      //logger.info("\tExerciseList.SetExercisesCallback Got " + result.getExercises().size() + " results");
      if (isStaleResponse(result)) {
        // logger.info("----> SetExercisesCallback.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
      } else {
        gotExercises(true);
        if (result.getExercises().isEmpty()) {
          gotEmptyExerciseList();
        }

        rememberAndLoadFirst(result.getExercises(), result.getFirstExercise(), selectionID);
      }
    }
  }

  protected abstract void gotExercises(boolean success);

  /**
   * @see #reloadWith(String)
   */
  private class SetExercisesCallbackWithID implements AsyncCallback<ExerciseListWrapper> {
    private final String id;

    public SetExercisesCallbackWithID(String id) {
      this.id = id;
      // logger.info("ExerciseList.SetExercisesCallbackWithID id = " + id);
    }

    public void onFailure(Throwable caught) {
      gotExercises(false);
      dealWithRPCError(caught);
    }

    public void onSuccess(ExerciseListWrapper result) {
      logger.info("\tExerciseList.SetExercisesCallbackWithID Got " + result.getExercises().size() + " results, id = " + id);
      if (isStaleResponse(result)) {
        logger.info("----> SetExercisesCallbackWithID.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
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
      }
    }
  }

  /**
   * @param caught
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallback#onFailure(Throwable)
   */
  private void dealWithRPCError(Throwable caught) {
    String message = caught.getMessage();
    if (message.length() > MAX_MSG_LEN) message = message.substring(0, MAX_MSG_LEN);
    if (!message.trim().equals("0")) {
      feedback.showErrorMessage("Server error", "Please clear your cache and reload the page. (" +
          message +
          ")");
    }
    logger.info("ExerciseList.SetExercisesCallbackWithID Got exception '" + message + "' " + caught);

    caught.printStackTrace();
    controller.logMessageOnServer("got exception " + caught.getMessage(), " RPCerror?");
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() {}

  public void rememberAndLoadFirst(List<CommonShell> exercises) {
//    logger.info(new Date() + " rememberAndLoadFirst : exercises " + exercises.size());
    rememberAndLoadFirst(exercises, null, "All");
  }

  /**
   * Calls remember exercises -- interacts with flashcard mode and the shuffle option there.
   * <p>
   * Has override for headstart selection of a specific exercise.
   * <p>
   * Previously we would first ask the server for the exercise list and then ask for the first exercise on
   * the list, making the user/client wait for both calls to finish before displaying the first item.
   * <p>
   * Now the first exercise is in the {@link mitll.langtest.shared.ExerciseListWrapper#getFirstExercise()} returned with the exercise list on the first call.
   *
   * @param exercises     - exercise list
   * @param firstExercise - the initial exercise returned from getExercises
   * @param selectionID   - in the context of this selection
   * @see ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @see #rememberAndLoadFirst(java.util.List)
   */
  private void rememberAndLoadFirst(List<CommonShell> exercises, CommonExercise firstExercise, String selectionID) {
    logger.info("ExerciseList : rememberAndLoadFirst instance '" + getInstance() +
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
        logger.info("ExerciseList : rememberAndLoadFirst using first = " + firstExercise);
        useExercise(firstExercise);   // allows us to skip another round trip with the server to ask for the first exercise
      } else {
        logger.info("ExerciseList : rememberAndLoadFirst finding first...");
        loadFirstExercise();
      }
    } else {
      loadFirstExercise();
    }
  }

  boolean isStaleResponse(ExerciseListWrapper result) {
    return result.getReqID() < lastReqID;
  }

  public abstract List<CommonShell> rememberExercises(List<CommonShell> result);

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

  @Override
  public void show() {
    getParent().setVisible(true);
  }

  /**
   * If we're not already showing this item, ask there server for the exercise.
   * Does this by pushing a history item and then noticing the history item change.
   *
   * @see #rememberAndLoadFirst(java.util.List, mitll.langtest.shared.CommonExercise, String)
   */
  private void loadFirstExercise() {
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      logger.info("loadFirstExercise : current exercises is empty?");
      removeCurrentExercise();
    } else {
      CommonShell toLoad = findFirstExercise();
//      logger.info("loadFirstExercise ex id =" + toLoad.getID() + " instance " + instance);
      pushFirstSelection(toLoad.getID());
    }
  }

  protected abstract boolean isEmpty();

  /**
   * Sometimes you want to change which exercise is loaded first.
   *
   * @return
   */
  protected CommonShell findFirstExercise() {
    return getFirst();
  }

  protected abstract CommonShell getFirst();

  boolean hasExercise(String id) {
    return byID(id) != null;
  }

  public abstract CommonShell byID(String name);

  /**
   * @param itemID
   * @see #loadFirstExercise()
   * @see #loadNextExercise
   * @see #loadPreviousExercise
   */
  public void loadExercise(String itemID) {
//    logger.info("ExerciseList.loadExercise itemID " + itemID);
    pushNewItem("", itemID);
  }

  /**
   * This method is called whenever the application's history changes.
   *
   * @param event
   * @see #pushNewItem(String, String)
   */
  public void onValueChange(ValueChangeEvent<String> event) {
    String value = event.getValue();
    String token = getTokenFromEvent(event);
    String id = getIDFromToken(token);
    logger.info("ExerciseList.onValueChange got " + event.getAssociatedType() +
        " " + value + " token " + token + " id '" + id + "'" + " instance " + getInstance());

    if (id.length() > 0) {
      checkAndAskServer(id);
    }
/*    else {
      logger.info("ExerciseList.onValueChange : got invalid event " + event + " value " + token + " id '" + id+
          "'");
    }*/
  }

  @Override
  public void checkAndAskServer(String id) {
//    logger.info("ExerciseList.checkAndAskServer - askServerForExercise = "  + id);
    if (hasExercise(id)) {
      askServerForExercise(id);
    } else if (!id.equals(EditItem.NEW_EXERCISE_ID)) {
      logger.warning("checkAndAskServer : can't load " + id);
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
      if (!selectionState.getInstance().equals(getInstance())) {
        //logger.info("got history item for another instance '" + selectionState.getInstance() + "' vs me '" + instance +"'");
      } else {
        String item = selectionState.getItem();
        // logger.info("got history item for instance '" + selectionState.getInstance() + " : '" + item+"'");
        return item;
      }
    }
    return "";
  }

  protected boolean loadByID(String id) {
    if (hasExercise(id)) {
      // logger.info("loading exercise " + id);
      loadExercise(id);
      return true;
    } else {
      return false;
    }
  }

  private CommonExercise cachedNext = null;
  private boolean pendingReq = false;

  /**
   * goes ahead and asks the server for the next item so we don't have to wait for it.
   *
   * @param itemID
   * @see #checkAndAskServer(String)
   */
  protected void askServerForExercise(String itemID) {
    //logger.info("ExerciseList.askServerForExercise id = " + itemID + " instance " + instance);
    controller.checkUser();
    if (cachedNext != null && cachedNext.getID().equals(itemID)) {
      //logger.info("\tExerciseList.askServerForExercise using cached id = " + itemID + " instance " + instance);

      useExercise(cachedNext);
    } else {
      pendingReq = true;
      service.getExercise(itemID, controller.getUser(), incorrectFirstOrder, new ExerciseAsyncCallback());
    }

    // go get next and cache it
    goGetNextAndCacheIt(itemID);
  }

  private void goGetNextAndCacheIt(String itemID) {
    int i = getIndex(itemID);
    if (!isOnLastItem(i)) {
      CommonShell next = getAt(i + 1);
      service.getExercise(next.getID(), controller.getUser(), incorrectFirstOrder, new AsyncCallback<CommonExercise>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(CommonExercise result) {
          cachedNext = result;
          //logger.info("\tExerciseList.askServerForExercise got cached id = " + cachedNext.getID() + " instance " + instance);
        }
      });
    }
  }

  private class ExerciseAsyncCallback implements AsyncCallback<CommonExercise> {
    @Override
    public void onFailure(Throwable caught) {
      pendingReq = false;
      if (caught instanceof IncompatibleRemoteServiceException) {
        Window.alert("This application has recently been updated.\nPlease refresh this page, or restart your browser." +
            "\nIf you still see this message, clear your cache. (" + caught.getMessage() +
            ")");
      } else {
        if (!caught.getMessage().trim().equals("0")) {
          Window.alert("Message from server: " + caught.getMessage());
        }
        logger.info("ex " + caught.getMessage() + " " + caught);
      }
    }

    @Override
    public void onSuccess(CommonExercise result) {
      pendingReq = false;

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
  protected void useExercise(final CommonExercise commonExercise) {
    //  logger.info("ExerciseList.useExercise : commonExercise " + commonExercise.getID());
    String itemID = commonExercise.getID();
    markCurrentExercise(itemID);

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        createdPanel = makeExercisePanel(commonExercise);
      }
    });
/*    logger.info("ExerciseList.useExercise : item id " + itemID + " currentExercise " +getCurrentExercise() +
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
  private Panel makeExercisePanel(CommonExercise exercise) {
    logger.info("ExerciseList.makeExercisePanel : " + exercise + " instance " + instance);
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
  private void getNextExercise(CommonShell current) {
    // logger.info("ExerciseList.getNextExercise " + current);
    int i = getIndex(current.getID());
    if (i == -1) {
      logger.warning("ExerciseList.getNextExercise : huh? couldn't find " + current +
          " in " + getSize() + " exercises : " + getKeys());
    } else {
      CommonShell next = getAt(i + 1);
      logger.info("ExerciseList.getNextExercise " + next);
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
    // logger.info("getIndex " + currentID + " = " +i);
    return i;
  }

  protected abstract int getRealIndex(CommonShell t);

  protected abstract CommonShell getAt(int i);

  @Override
  public int getComplete() {
    return getIndex(getCurrentExerciseID());
  }

  /**
   * @see #removeExercise
   */
  @Override
  public void removeCurrentExercise() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (!innerContainer.remove(current)) {
        logger.warning("\tdidn't remove current widget");
      } else innerContainer.getParent().removeStyleName("shadowBorder");
    } else {
      logger.warning("\tremoveCurrentExercise : no inner current widget for " + report());
    }
  }

  String report() {
    return "list " + getInstance() + " id " + getElement().getId();
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
    logger.info("ExerciseList.loadNextExercise current is : " + current + " instance " + instance);
    String id = current.getID();
    int i = getIndex(id);
    boolean onLast = isOnLastItem(i);
/*    logger.info("ExerciseList.loadNextExercise current is : " + id + " index " + i +
        " of " + getSize() + " last is " + (getSize() - 1) + " on last " + onLast);*/

    if (onLast) {
      onLastItem();
    } else {
      getNextExercise(current);
    }

    return onLast;
  }

  public boolean loadNextExercise(String id) {
    logger.info("ExerciseList.loadNextExercise id = " + id + " instance " + instance);
    CommonShell exerciseByID = byID(id);
    if (exerciseByID == null) logger.warning("huh? couldn't find exercise with id " + exerciseByID);
    return exerciseByID != null && loadNextExercise(exerciseByID);
  }

  /**
   * @see ListInterface#loadNextExercise
   */
  protected void onLastItem() {
    loadFirstExercise();
  }

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
   * @see mitll.langtest.client.custom.content.NPFHelper#doInternalLayout
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
    //logger.info("onFirst : of " +getSize() +", on checking " + current + " = " + b);
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

  public void redraw() {
  }

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

  /**
   * @param doShuffle
   * @see mitll.langtest.client.flashcard.FlashcardPanel#gotShuffleClick(boolean)
   */
  @Override
  public void setShuffle(boolean doShuffle) {
    simpleSetShuffle(doShuffle);
    rememberAndLoadFirst(getInOrder());
  }

  /**
   * @param doShuffle
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#StatsPracticePanel
   */
  public void simpleSetShuffle(boolean doShuffle) {
    this.doShuffle = doShuffle;
  }
}
