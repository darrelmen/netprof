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

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserState;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.*;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.content.NPFHelper.COMPLETE;
import static mitll.langtest.client.custom.content.NPFHelper.LIST_COMPLETE;
import static mitll.langtest.client.dialog.ExceptionHandlerDialog.getExceptionAsString;

/**
 * Handles left side of NetPron2 -- which exercise is the current one, highlighting, etc.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 7/9/12
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExerciseList<T extends CommonShell, U extends Shell>
    extends VerticalPanel
    implements ListInterface<T, U>, ProvidesResize {
  public static final String GETTING_EXERCISE = "getting exercise";
  private final Logger logger = Logger.getLogger("ExerciseList");

  private static final String EMPTY_SEARCH = "<b>Your search or selection did not match any items.</b>" +
      "<p>Try clearing one of your selections or changing the search.</p>";

  private static final int LIST_HEIGHT = 450;
  /**
   * @see #showEmptyExercise
   */
  private static final String EMPTY_PANEL = "placeHolderWhenNoExercises";

  private static final int MAX_MSG_LEN = 200;

  protected SimplePanel innerContainer;
  protected final ExerciseServiceAsync service;
  private final UserFeedback feedback;
  protected ExercisePanelFactory<T, U> factory;
  protected final ExerciseController controller;

  protected Panel createdPanel;
  private int lastReqID = 0;
  final boolean allowPlusInURL;
  private final List<ListChangeListener<T>> listeners = new ArrayList<>();
  boolean doShuffle;

  private U cachedNext = null;
  private boolean pendingReq = false;
  /**
   *
   */
  ExerciseListRequest lastSuccessfulRequest = null;

  private static final boolean DEBUG = false;
  private final UserState userState;
  final ListOptions listOptions;

  /**
   * @param currentExerciseVPanel
   * @param factory
   * @param controller
   * @paramx instance
   * @see PagingExerciseList
   */
  ExerciseList(Panel currentExerciseVPanel,
               ExercisePanelFactory<T, U> factory,
               ExerciseController controller,
               ListOptions listOptions) {
    this.listOptions = listOptions;
    this.service = controller.getExerciseService();
    this.feedback = controller.getFeedback();
    this.factory = factory;
    this.allowPlusInURL = controller.getProps().shouldAllowPlusInURL();
    this.userState = controller.getUserState();
    this.controller = controller;
    addWidgets(currentExerciseVPanel);
    getElement().setId("ExerciseList_" + listOptions.getInstance());
  }

  /**
   * @param currentExerciseVPanel
   * @see #ExerciseList
   */
  private void addWidgets(final Panel currentExerciseVPanel) {
//    if (DEBUG) logger.info("ExerciseList.addWidgets for currentExerciseVPanel " + currentExerciseVPanel.getElement().getExID() + " instance " + getInstance());
    this.innerContainer = new SimplePanel();
    innerContainer.getElement().setId("ExerciseList_innerContainer");
    innerContainer.setWidth("100%");
    innerContainer.addStyleName("floatLeftAndClear");

    currentExerciseVPanel.getElement().getStyle().setWidth(100, Style.Unit.PCT);
    currentExerciseVPanel.add(innerContainer);
    currentExerciseVPanel.addStyleName("floatLeftAndClear");
  }

  public void addWidgets() {
  }

  /**
   * @param factory
   * @see mitll.langtest.client.custom.content.NPFHelper#setFactory(PagingExerciseList, String, boolean)
   */
  public void setFactory(ExercisePanelFactory<T, U> factory) {
    this.factory = factory;
  }

  /**
   * Get exercises for this user.
   *
   * @return true if we asked the server for exercises
   * @see HistoryExerciseList#noSectionsGetExercises(long, int)
   */
  public boolean getExercises() {
    if (DEBUG) logger.info("ExerciseList.getExercises instance " + getInstance());
    ExerciseListRequest request = getRequest("");
//    logger.info("request is " +request);
    service.getExerciseIds(request, new SetExercisesCallback("", "", -1, request));
    return true;
  }

  protected abstract ExerciseListRequest getRequest(String prefix);

  int incrRequest() {
    return ++lastReqID;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#doAfterEditComplete
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper#showNPF
   */
  @Override
  public void reloadWithCurrent() {
    reloadWith(getCurrentExerciseID());
  }

  /**
   * After re-fetching the ids, select this one.
   *
   * @param id
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#doAfterEditComplete
   * @see #reloadWithCurrent
   */
  private void reloadWith(int id) {
    if (DEBUG) {
      logger.info("ExerciseList.reloadWith id = " + id + " for user " + getUser() + " instance " + getInstance());
    }
    ExerciseListRequest request = getRequest("");
    service.getExerciseIds(request, new SetExercisesCallbackWithID(id, request));
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
   * @param searchIfAny
   * @see #loadFirstExercise
   */
  abstract void pushFirstSelection(int exerciseID, String searchIfAny);

  /**
   * Calling this will result in an immediate call to onValueChange (reacting to the history change)
   *
   * @param search
   * @param exerciseID
   * @see Reloadable#loadExercise(int)
   * @see #pushFirstSelection(int, String)
   * @see HistoryExerciseList#onValueChange(ValueChangeEvent)
   */
  abstract void pushNewItem(String search, int exerciseID);

  /**
   * @return
   */
  abstract ActivityType getActivityType();

  /**
   * @see ListInterface#getExercises()
   */
  protected abstract void showFinishedGettingExercises();

  protected abstract List<T> rememberExercises(List<T> result);

  public Panel getCreatedPanel() {
    return createdPanel;
  }

  public String getInstance() {
    return listOptions.getInstance();
  }

  @Override
  public boolean isPendingReq() {
    return pendingReq;
  }

  /**
   * @see ListInterface#getExercises
   */
  class SetExercisesCallback implements AsyncCallback<ExerciseListWrapper<T>> {
    private final String selectionID;
    private final String searchIfAny;
    private final int exerciseID;
    long then = System.currentTimeMillis();
    private final ExerciseListRequest request;

    /**
     * @param selectionID
     * @param searchIfAny
     * @param exerciseID
     * @paramx setTypeAheadText
     * @see ListInterface#getExercises()
     */
    SetExercisesCallback(String selectionID, String searchIfAny, int exerciseID, ExerciseListRequest request) {
      this.selectionID = selectionID;
      this.searchIfAny = searchIfAny;
      this.exerciseID = exerciseID;
      this.request = request;
    }

    public void onFailure(Throwable caught) {
//      logger.warning("SetExercisesCallback.onFailure " + lastReqID);
      showFinishedGettingExercises();
      dealWithRPCError(caught);
    }

    public void onSuccess(ExerciseListWrapper<T> result) {
      logger.info("took " + (System.currentTimeMillis() - then) + " to get exercise ids.");

      showFinishedGettingExercises();
      if (DEBUG) {
        List<T> exercises = result.getExercises();
        if (exercises == null)
          logger.info("\tExerciseList.SetExercisesCallback Got " + exercises.size() + " results");
      }
      if (isStaleResponse(result)) {
        if (DEBUG)
          logger.info("SetExercisesCallback.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
        ignoreStaleRequest(result);
      } else {
        lastSuccessfulRequest = request;
        if (DEBUG) logger.info("onSuccess last req now " + lastSuccessfulRequest);
        checkForEmptyExerciseList(result.getExercises().isEmpty());
        int idToUse = exerciseID == -1 ? result.getFirstExercise() == null ? -1 : result.getFirstExercise().getID() : exerciseID;

        Map<Integer, Float> idToScore = result.getIdToScore();
        for (T ex : result.getExercises()) {
          int id = ex.getID();
          if (idToScore.containsKey(id)) {
            ex.getMutableShell().setScore(idToScore.get(id));
          }
        }

        rememberAndLoadFirst(result.getExercises(), selectionID, searchIfAny, idToUse);
      }
    }
  }

  /**
   * TODO : why do we need two of these????
   */
  private class SetExercisesCallbackWithID implements AsyncCallback<ExerciseListWrapper<T>> {
    private final int id;
    private final ExerciseListRequest request;

    /**
     * @param id
     * @see #reloadWith
     */
    SetExercisesCallbackWithID(int id, ExerciseListRequest request) {
      this.id = id;
      this.request = request;
      if (DEBUG) logger.info("ExerciseList.SetExercisesCallbackWithID id = " + id);
    }

    public void onFailure(Throwable caught) {
      showFinishedGettingExercises();
      dealWithRPCError(caught);
    }

    public void onSuccess(ExerciseListWrapper<T> result) {
      showFinishedGettingExercises();

      if (DEBUG)
        logger.info("\tExerciseList.SetExercisesCallbackWithID Got " + result.getExercises().size() + " results, id = " +
            id);
      if (isStaleResponse(result)) {
        if (DEBUG)
          logger.info("----> SetExercisesCallbackWithID.onSuccess ignoring result " + result.getReqID() +
              " b/c before latest " + lastReqID);
      } else {
        useExercises(result);
      }
    }

    private void useExercises(ExerciseListWrapper<T> result) {
      lastSuccessfulRequest = request;
      List<T> exercises = result.getExercises();
      checkForEmptyExerciseList(exercises.isEmpty());
      exercises = rememberExercises(exercises);
      for (ListChangeListener<T> listener : listeners) {
        listener.listChanged(exercises, "");
      }
      if (DEBUG) logger.info("\tExerciseList.SetExercisesCallbackWithID onSuccess id = " + id);

      pushFirstSelection(id, "");
    }
  }

  /**
   * @see #askServerForExercise
   */
  private class ExerciseAsyncCallback implements AsyncCallback<U> {
    @Override
    public void onFailure(Throwable caught) {
      pendingReq = false;
      dealWithFailure(caught);
    }

    @Override
    public void onSuccess(U result) {
      pendingReq = false;
      if (result == null) {
        Window.alert("Unfortunately there's a configuration error and we can't find this exercise.");
      } else {
        showExercise(result);
      }
    }
  }

  private void dealWithFailure(Throwable caught) {
    if (caught instanceof IncompatibleRemoteServiceException) {
      Window.alert("This application has recently been updated.\nPlease refresh this page, or restart your browser." +
          "\nIf you still see this message, clear your cache. (" + caught.getMessage() +
          ")");
    } else {
      if (!caught.getMessage().trim().equals("0")) {
        Window.alert("Message from server: " + caught.getMessage());
      }
      if (DEBUG) logger.info("ex " + caught.getMessage() + " " + caught);
    }
  }

  protected abstract void ignoreStaleRequest(ExerciseListWrapper<T> result);

  /**
   * @see #reloadWith
   */
  private void checkForEmptyExerciseList(boolean isEmpty) {
    // if (DEBUG) logger.info("ExerciseList.gotExercises result = " + result);
    if (isEmpty) {
      gotEmptyExerciseList();
    }
  }

  /**
   * @param caught
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallback#onFailure(Throwable)
   */
  void dealWithRPCError(Throwable caught) {
    String message = caught.getMessage();
    if (message != null && message.length() > MAX_MSG_LEN) message = message.substring(0, MAX_MSG_LEN);
    if (message != null && !message.trim().equals("0")) {
      feedback.showErrorMessage("Server error", "Please clear your cache and reload the page. (" +
          message +
          ")");
    }
    if (DEBUG) logger.info("ExerciseList.SetExercisesCallbackWithID Got exception '" + message + "' " + caught);
    String exceptionAsString = getExceptionAsString(caught);
    //  caught.printStackTrace();
    controller.logMessageOnServer("got exception " + caught.getMessage() + " : " + exceptionAsString, " RPCerror?", true);

    controller.handleNonFatalError(GETTING_EXERCISE, caught);
  }

  /**
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallback#onSuccess(ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() {
  }

  public void rememberAndLoadFirst(List<T> exercises) {
    rememberAndLoadFirst(exercises, "All", "", -1);
  }

  /**
   * Calls remember exercises -- interacts with flashcard mode and the shuffle option there.
   * <p>
   * Has override for headstart selection of a specific exercise???
   * <p>
   * Previously we would first ask the server for the exercise list and then ask for the first exercise on
   * the list, making the user/client wait for both calls to finish before displaying the first item.
   * <p>
   * Now the first exercise is in the {@link ExerciseListWrapper#getFirstExercise()} returned
   * with the exercise list on the first call.
   *
   * @param exercises   - exercise list
   * @param selectionID - in the context of this selection
   * @param searchIfAny
   * @param exerciseID
   * @paramx firstExercise - the initial exercise returned from getExercises
   * @see ExerciseList.SetExercisesCallback#onSuccess(ExerciseListWrapper)
   * @see #rememberAndLoadFirst
   */
  public void rememberAndLoadFirst(List<T> exercises,
                                   String selectionID,
                                   String searchIfAny,
                                   int exerciseID) {
    if (DEBUG) logger.info("ExerciseList : rememberAndLoadFirst instance '" + getInstance() +
        "'" +
        "\n\tremembering " + exercises.size() + " exercises," +
        "\n\tselection   " + selectionID +
        "\n\tsearchIfAny " + searchIfAny +
        "\n\tfirst       " + exerciseID
    );

    exercises = rememberExercises(exercises);
    for (ListChangeListener<T> listener : listeners) {
      listener.listChanged(exercises, selectionID);
    }

    goToFirst(searchIfAny, exerciseID);
  }

  /**
   * TODO : Why would we want to include both a search term and an exercise id???
   *
   * @param searchIfAny
   * @param exerciseID
   * @see #rememberAndLoadFirst(List, String, String, int)
   */
  protected void goToFirst(String searchIfAny, int exerciseID) {
    if (exerciseID < 0) {
      loadFirstExercise(searchIfAny);
    } else {
      markCurrentExercise(exerciseID);
      // logger.info("goToFirst pushFirstSelection " + exerciseID + " searchIfAny '" + searchIfAny + "'");
      pushFirstSelection(exerciseID, searchIfAny);
    }
  }

  private boolean isStaleResponse(ExerciseListWrapper result) {
    return result.getReqID() < lastReqID;
  }


  /**
   * Worry about deleting the currently visible item.
   *
   * @param es
   * @see PagingExerciseList#forgetExercise(int)
   */
  T removeExercise(T es) {
    int id = es.getID();
    T current = getCurrentExercise();
    if (current.getID() == id) {
      if (!onLast(current)) {
        //logger.info(getClass() + " removeExercise - load next after " + id);
        loadNextExercise(current);
      } else if (!onFirst(current)) {
        logger.info(getClass() + " removeExercise - load prev before " + id);
        loadPreviousExercise(current);
      }
    }

    return simpleRemove(id);
  }

  @Override
  public void hide() {
    getParent().setVisible(false);
  }

  /**
   * If we're not already showing this item, ask there server for the exercise.
   * Does this by pushing a history item and then noticing the history item change.
   *
   * @param searchIfAny
   * @see #rememberAndLoadFirst
   */
  protected void loadFirstExercise(String searchIfAny) {
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      if (DEBUG) logger.info("loadFirstExercise (" + getInstance() + ") : current exercises is empty?");
      removeCurrentExercise();
    } else {
      int firstID = findFirstID();
      if (DEBUG) logger.info("loadFirstExercise ex id =" + firstID + " instance " + getInstance());
      pushFirstSelection(firstID, searchIfAny);
    }
  }

  public void loadFirst() {
    pushFirstSelection(getFirstID(), "");
  }

  protected abstract boolean isEmpty();

  /**
   * Sometimes you want to change which exercise is loaded first.
   *
   * @return
   */
  T findFirstExercise() {
    return getFirst();
  }

  private int findFirstID() {
    return findFirstExercise().getID();
  }

  protected abstract T getFirst();

  int getFirstID() {
    return getFirst().getID();
  }

  boolean hasExercise(int id) {
    return byID(id) != null;
  }

  public abstract T byID(int name);

  /**
   * @param itemID
   * @see #loadFirstExercise(String)
   * @see ListInterface ListInterface#loadNextExercise
   * @see ListInterface ListInterface#loadPreviousExercise
   */
  public void loadExercise(int itemID) {
//   if (DEBUG) logger.info("ExerciseList.loadExercise itemID " + itemID);
    pushNewItem("" + itemID, itemID);
  }

  /**
   * @param id
   * @see HistoryExerciseList#checkAndAskOrFirst
   */
  @Override
  public void checkAndAskServer(int id) {
    if (DEBUG) {
      logger.info(getClass() + " : (" + getInstance() + ") ExerciseList.checkAndAskServer - askServerForExercise = " + id);
    }

    if (hasExercise(id)) {
      //  logger.info("checkAndAskServer for " + id);
      askServerForExercise(id);
    } else {
      logger.warning("checkAndAskServer : skipping request for " + id);
    }
  }

  @Override
  public boolean loadByID(int id) {
    if (hasExercise(id)) {
      // if (DEBUG) logger.info("loading exercise " + id);
      loadExercise(id);
      return true;
    } else {
      return false;
    }
  }

  /**
   * goes ahead and asks the server for the next item so we don't have to wait for it.
   *
   * @param itemID
   * @see ListInterface#checkAndAskServer(int)
   */
  protected void askServerForExercise(int itemID) {
    if (cachedNext != null && cachedNext.getID() == itemID) {
      if (DEBUG)
        logger.info("\tExerciseList.askServerForExercise using cached id = " + itemID + " instance " + getInstance());
      showExercise(cachedNext);
    } else {
      pendingReq = true;
      if (DEBUG) logger.info("ExerciseList.askServerForExercise id = " + itemID + " instance " + getInstance());
      service.getExercise(itemID, false, new ExerciseAsyncCallback());
    }

    // go get next and cache it
    goGetNextAndCacheIt(itemID);
  }

  private void goGetNextAndCacheIt(int itemID) {
    int i = getIndex(itemID);
    if (!isOnLastItem(i)) {
      T next = getAt(i + 1);

      if (controller.getUser() > 0) {
        service.getExercise(next.getID(), false, new AsyncCallback<U>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError(GETTING_EXERCISE, caught);
          }

          @Override
          public void onSuccess(U result) {
            cachedNext = result;
            //if (DEBUG) logger.info("\tExerciseList.askServerForExercise got cached id = " + cachedNext.getOldID() + " instance " + getInstance());
          }
        });
      }
      // else no user
    }
  }

  private int getUser() {
    return userState.getUser();
  }

  /**
   * @param commonExercise
   * @see #rememberAndLoadFirst
   * @see ExerciseAsyncCallback#onSuccess
   */
  void showExercise(final U commonExercise) {
    if (DEBUG) {
      logger.info("ExerciseList.showExercise : commonExercise " + commonExercise.getID() + " " + commonExercise.getClass());
    }
    markCurrentExercise(commonExercise.getID());

    Scheduler.get().scheduleDeferred((Command) () -> {
      logger.info("ExerciseList.showExercise : item id " + commonExercise.getID() + " currentExercise " + getCurrentExercise() +
          " or " + getCurrentExerciseID());
      addExerciseWidget(commonExercise);
    });
  }

  /**
   * @param commonExercise
   * @see FacetExerciseList#showExercises
   */
  void addExerciseWidget(U commonExercise) {
    createdPanel = factory.getExercisePanel(commonExercise);
    // logger.info("Add exercise widget "  + commonExercise.getID());
    innerContainer.setWidget(createdPanel);
  }

  public int getCurrentExerciseID() {
    T currentExercise = getCurrentExercise();
    //   logger.info("Current exercise is " + currentExercise);
    return currentExercise != null ? currentExercise.getID() : -1;//"Unknown";
  }

  protected abstract T getCurrentExercise();

  /**
   * @param current
   * @see ListInterface#loadNextExercise
   */
  private void getNextExercise(HasID current) {
    int i = getIndex(current.getID());
    if (DEBUG) logger.info("ExerciseList.getNextExercise current " + current + " at index " + i);
    if (i == -1) {
      logger.warning("ExerciseList.getNextExercise : huh? couldn't find " + current +
          " in " + getSize() //+ " exercises : " + getKeys()
      );
    } else {
      int i1 = i + 1;
      Shell next = getAt(i1);
      if (DEBUG) logger.info("ExerciseList.getNextExercise " + next.getID() + " at next index " + i1);
      loadExercise(next.getID());
    }
  }

  /**
   * @param currentID
   * @return
   * @see #showExercise
   */
  public int getIndex(int currentID) {
    T shell = byID(currentID);
    int i = shell != null ? getRealIndex(shell) : -1;
    // logger.info("getIndex " + currentID + " = " +i);
    return i;
  }

  protected abstract int getRealIndex(T t);

  protected abstract T getAt(int i);

  @Override
  public int getComplete() {
    int currentExerciseID = getCurrentExerciseID();
    int index = getIndex(currentExerciseID);
    return index;
  }

  /**
   * @see #removeExercise
   */
  void removeCurrentExercise() {
//    logger.info("removeCurrentExercise clear container --- >");
    clearExerciseContainer();
    innerContainer.getParent().removeStyleName("shadowBorder");
  }

  void clearExerciseContainer() {
    innerContainer.clear();
  }

  /**
   * Compare with google response for this state.
   */
  void showEmptyExercise() {
    //  logger.info("showEmptyExercise --- ");
    createdPanel = new SimplePanel(new Heading(3, EMPTY_SEARCH));
    createdPanel.addStyleName("leftFiveMargin");
    createdPanel.getElement().setId(EMPTY_PANEL);
    innerContainer.setWidget(createdPanel);
  }

  @Override
  public void clear() {
    removeComponents();
  }

  private void removeComponents() {
    super.clear();
  }

  public abstract void markCurrentExercise(int itemID);

  @Override
  public boolean loadNext() {
    return loadNextExercise(getCurrentExerciseID());
  }

  /**
   * @param current
   * @return
   */
  @Override
  public boolean loadNextExercise(HasID current) {
    if (DEBUG) logger.info("ExerciseList.loadNextExercise current is : " + current + " instance " + getInstance());
    int id = current.getID();
    int index = getIndex(id);
    boolean onLast = isOnLastItem(index);
/*
    logger.info("ExerciseList.loadNextExercise current is : " + id + " index " + index +
        " of " + getSize() + " last is " + (getSize() - 1) +
        "\n\ton last " + onLast);
        */
    if (onLast) {
      onLastItem();
    } else {
      getNextExercise(current);
    }

    return onLast;
  }

  public boolean loadNextExercise(int id) {
    if (DEBUG) logger.info("ExerciseList.loadNextExercise id = " + id + " instance " + getInstance());
    T exerciseByID = byID(id);
    if (exerciseByID == null) logger.warning("huh? couldn't find exercise with id " + id);
    return exerciseByID != null && loadNextExercise(exerciseByID);
  }

  /**
   * @see ListInterface#loadNextExercise
   */
  protected void onLastItem() {
    new ModalInfoDialog(COMPLETE, LIST_COMPLETE, hiddenEvent -> reloadExercises());
  }

  @Override
  public boolean loadPrev() {
    return loadPreviousExercise(getCurrentExercise());
  }

  /**
   * @param current
   * @return true if on first
   * @see mitll.langtest.client.exercise.NavigationHelper#clickPrev
   */
  @Override
  public boolean loadPreviousExercise(HasID current) {
    int i = getIndex(current.getID());
    boolean onFirst = i == 0;
    if (!onFirst) {
      loadExercise(getAt(i - 1).getID());
    }
    return onFirst;
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.content.NPFHelper#doInternalLayout
   */
  @Override
  public Widget getExerciseListOnLeftSide() {
    Panel leftColumn = new FlowPanel();
    leftColumn.getElement().setId("ExerciseList_leftColumn");
    leftColumn.addStyleName("floatLeftAndClear");
    addMinWidthStyle(leftColumn);

    leftColumn.add(getWidget());
    return leftColumn;
  }

  protected void addMinWidthStyle(Panel leftColumn) {
    leftColumn.getElement().getStyle().setProperty("minHeight", LIST_HEIGHT + "px");
    leftColumn.addStyleName("minWidth");
    leftColumn.getElement().getStyle().setPaddingRight(10, Style.Unit.PX);
  }

  private Widget getWidget() {
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
  public boolean onFirst(HasID current) {
    boolean b = current == null || getSize() == 1 || getIndex(current.getID()) == 0;
    //logger.info("onFirst : of " +getSize() +", on checking " + current + " = " + b);
    return b;
  }

  public boolean onLast() {
    return onLast(getCurrentExercise());
  }

  @Override
  public boolean onLast(HasID current) {
    return current == null || getSize() == 1 || isOnLastItem(getIndex(current.getID()));
  }

  private boolean isOnLastItem(int i) {
    return i == getSize() - 1;
  }

  @Override
  public void reloadExercises() {
    loadFirstExercise("");
  }

  public void redraw() {
  }

  /**
   * @param listener
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallbackWithID#onSuccess(ExerciseListWrapper)
   * @see #rememberAndLoadFirst
   */
  @Override
  public void addListChangedListener(ListChangeListener<T> listener) {
    listeners.add(listener);
  }

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

  /**
   * JUST FOR AMAS
   *
   * @return
   */
  public Collection<Integer> getIDs() {
    Set<Integer> ids = new HashSet<>();
    for (T cs : getInOrder()) ids.add(cs.getID());
    return ids;
  }

  protected abstract List<T> getInOrder();

  @Override
  public void setScore(int id, float hydecScore) {
    byID(id).getMutableShell().setScore(hydecScore);
  }
}
