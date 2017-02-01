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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserState;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.*;

import java.util.*;
import java.util.logging.Logger;

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
    implements ListInterface<T>, ProvidesResize {
  private final Logger logger = Logger.getLogger("ExerciseList");
  /**
   * @see #showEmptyExercise
   */
  private static final String EMPTY_PANEL = "placeHolderWhenNoExercises";

  private static final int MAX_MSG_LEN = 200;
  boolean incorrectFirstOrder = false;

  protected SimplePanel innerContainer;
  protected final ExerciseServiceAsync service;
  private final UserFeedback feedback;
  private ExercisePanelFactory<T, U> factory;
  private final ExerciseController controller;

  protected Panel createdPanel;
  private int lastReqID = 0;
  final boolean allowPlusInURL;
  private final String instance;
  private final List<ListChangeListener<T>> listeners = new ArrayList<>();
  boolean doShuffle;

  private U cachedNext = null;
  private boolean pendingReq = false;
  ExerciseListRequest lastSuccessfulRequest = null;

  private static final boolean DEBUG = false;
  private UserState userState;

  /**
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param controller
   * @param instance
   * @seex mitll.langtest.client.LangTest#makeExerciseList
   */
  ExerciseList(Panel currentExerciseVPanel,
               ExerciseServiceAsync service,
               UserFeedback feedback,
               ExercisePanelFactory<T, U> factory,
               ExerciseController controller,
               String instance,
               boolean incorrectFirst) {
    this.instance = instance;
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
    this.allowPlusInURL = controller.getProps().shouldAllowPlusInURL();
    this.userState = controller.getUserState();
    this.controller = controller;
    this.incorrectFirstOrder = incorrectFirst;
    addWidgets(currentExerciseVPanel);
    getElement().setId("ExerciseList_" + instance);
  }

  /**
   * @param currentExerciseVPanel
   * @see #ExerciseList
   */
  private void addWidgets(final Panel currentExerciseVPanel) {
//    if (DEBUG) logger.info("ExerciseList.addWidgets for currentExerciseVPanel " + currentExerciseVPanel.getElement().getExID() + " instance " + getInstance());
    this.innerContainer = new SimplePanel();
    innerContainer.getElement().setId("ExerciseList_innerContainer");
    currentExerciseVPanel.add(innerContainer);
    innerContainer.addStyleName("floatLeft");
    currentExerciseVPanel.addStyleName("floatLeft");
  }

  public void addWidgets() {
  }

  /**
   * @param factory
   * @see mitll.langtest.client.custom.content.NPFHelper#setFactory(PagingExerciseList, String, boolean)
   */
  public void setFactory(ExercisePanelFactory factory) {
    this.factory = factory;
  }

  /**
   * Get exercises for this user.
   *
   * @param userID
   * @return true if we asked the server for exercises
   * @see mitll.langtest.client.list.HistoryExerciseList#noSectionsGetExercises(long)
   */
  public boolean getExercises(long userID) {
    if (DEBUG) logger.info("ExerciseList.getExercises for user " + userID + " instance " + getInstance());
    ExerciseListRequest request = getRequest();
    service.getExerciseIds(request, new SetExercisesCallback("", "", -1, request));
    return true;
  }

  private ExerciseListRequest getRequest() {
    return new ExerciseListRequest(incrRequest(), getUser())
        .setActivityType(getActivityType())
        .setIncorrectFirstOrder(incorrectFirstOrder);
  }

  int incrRequest() {
    return ++lastReqID;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#doAfterEditComplete(ListInterface, boolean)
   * @see NPFHelper#reload
   */
  public void reload() {
    //logger.info("reload -");
    getExercises(getUser());
  }

  @Override
  public void reloadWithCurrent() {
    reloadWith(getCurrentExerciseID());
  }

  /**
   * After re-fetching the ids, select this one.
   *
   * @param id
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#doAfterEditComplete
   */
  private void reloadWith(int id) {
    if (DEBUG)
      logger.info("ExerciseList.reloadWith id = " + id + " for user " + getUser() + " instance " + getInstance());
    service.getExerciseIds(getRequest(), new SetExercisesCallbackWithID(id));
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
   */
  abstract void pushNewItem(String search, int exerciseID);

  public void onResize() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (current instanceof RequiresResize) {
        // if (DEBUG || true) logger.info("resizing right side for " + instance + " " + current.getClass());
        ((RequiresResize) current).onResize();
      }
      /*else {
        logger.warning("huh?  right side is not resizable " + instance + " " + current.getClass());
      }*/
    }
    //   else {
//      logger.warning("huh? no right side of exercise list");
    //  }
  }

  public Panel getCreatedPanel() {
    return createdPanel;
  }

  /**
   * TODO : horrible hack here to get role of request.
   *
   * @return
   */
  abstract ActivityType getActivityType();
  //{
  //  return  getInstance() == null || getInstance().startsWith("record") ? AudioType.RECORDER.toString() : getInstance();
 // }

  public String getInstance() {
    return instance;
  }

  @Override
  public boolean isPendingReq() {
    return pendingReq;
  }

  /**
   * @see ListInterface#getExercises(long)
   */
  class SetExercisesCallback implements AsyncCallback<ExerciseListWrapper<T>> {
    private final String selectionID;
    private final String searchIfAny;
    private final int exerciseID;
    private final ExerciseListRequest request;

    /**
     * @param selectionID
     * @param searchIfAny
     * @param exerciseID
     * @paramx setTypeAheadText
     * @see #getExercises(long)
     */
    SetExercisesCallback(String selectionID, String searchIfAny, int exerciseID, ExerciseListRequest request) {
      this.selectionID = selectionID;
      this.searchIfAny = searchIfAny;
      this.exerciseID = exerciseID;
      this.request = request;
    }

    public void onFailure(Throwable caught) {
      logger.warning("SetExercisesCallback.onFailure " + lastReqID);
      gotExercises(false);
      dealWithRPCError(caught);
    }

    public void onSuccess(ExerciseListWrapper<T> result) {
      if (DEBUG) logger.info("\tExerciseList.SetExercisesCallback Got " + result.getExercises().size() + " results");
      if (isStaleResponse(result)) {
        if (DEBUG)
          logger.info("SetExercisesCallback.onSuccess ignoring result " + result.getReqID() + " b/c before latest " + lastReqID);
        ignoreStaleRequest(result);
      } else {
        lastSuccessfulRequest = request;
        if (DEBUG) logger.info("onSuccess last req now " + lastSuccessfulRequest);
        gotExercises(result);
        int idToUse = exerciseID == -1 ? result.getFirstExercise() == null ? -1 : result.getFirstExercise().getID() : exerciseID;
        rememberAndLoadFirst(result.getExercises(), selectionID, searchIfAny, idToUse);
      }
    }
  }

  protected abstract void ignoreStaleRequest(ExerciseListWrapper<T> result);

  private void gotExercises(ExerciseListWrapper<T> result) {
    gotExercises(true);
    if (DEBUG) logger.info("ExerciseList.gotExercises result = " + result);

    boolean isEmpty = result.getExercises().isEmpty();
    if (isEmpty) {
      gotEmptyExerciseList();
    } else {
      //  logger.info("list non empty");
    }
  }

  protected abstract void gotExercises(boolean success);

  /**
   * @see #reloadWith
   */
  private class SetExercisesCallbackWithID implements AsyncCallback<ExerciseListWrapper<T>> {
    private final int id;

    /**
     * @param id
     * @see #reloadWith
     */
    SetExercisesCallbackWithID(int id) {
      this.id = id;
      if (DEBUG) logger.info("ExerciseList.SetExercisesCallbackWithID id = " + id);
    }

    public void onFailure(Throwable caught) {
      gotExercises(false);
      dealWithRPCError(caught);
    }

    public void onSuccess(ExerciseListWrapper<T> result) {
      if (DEBUG)
        logger.info("\tExerciseList.SetExercisesCallbackWithID Got " + result.getExercises().size() + " results, id = " +
            id);
      if (isStaleResponse(result)) {
        if (DEBUG || true)
          logger.info("----> SetExercisesCallbackWithID.onSuccess ignoring result " + result.getReqID() +
              " b/c before latest " + lastReqID);
      } else {
        gotExercises(result);
        List<T> exercises = result.getExercises();
        exercises = rememberExercises(exercises);
        for (ListChangeListener<T> listener : listeners) {
          listener.listChanged(exercises, "");
        }
        if (DEBUG || true) logger.info("\tExerciseList.SetExercisesCallbackWithID onSuccess id = " + id);

        pushFirstSelection(id, "");
      }
    }
  }

  /**
   * @param caught
   * @see mitll.langtest.client.list.ExerciseList.SetExercisesCallback#onFailure(Throwable)
   */
  private void dealWithRPCError(Throwable caught) {
    String message = caught.getMessage();
    if (message != null && message.length() > MAX_MSG_LEN) message = message.substring(0, MAX_MSG_LEN);
    if (!message.trim().equals("0")) {
      feedback.showErrorMessage("Server error", "Please clear your cache and reload the page. (" +
          message +
          ")");
    }
    if (DEBUG) logger.info("ExerciseList.SetExercisesCallbackWithID Got exception '" + message + "' " + caught);

    caught.printStackTrace();
    controller.logMessageOnServer("got exception " + caught.getMessage(), " RPCerror?");
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
   * Has override for headstart selection of a specific exercise.
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
        "\n\tfirst       " + exerciseID);

    exercises = rememberExercises(exercises);
    for (ListChangeListener<T> listener : listeners) {
      listener.listChanged(exercises, selectionID);
    }

    // hack for Headstart support -- if we still do.
/*    String exercise_title1 = controller.getProps().getExercise_title();
    if (exercise_title1 != null) {
      Shell headstartExercise = byID(exercise_title1);
      if (headstartExercise != null) {
        loadExercise(exercise_title1);
        return;
      }
    }*/

//    if (exerciseID < 0) {
//      loadFirstExercise();
//    }

    goToFirst(searchIfAny, exerciseID);

    listLoaded();
  }

  protected void goToFirst(String searchIfAny, int exerciseID) {
    if (exerciseID < 0) {
      loadFirstExercise(searchIfAny);
    } else {
    //  logger.info("goToFirst pushFirstSelection " + exerciseID + " searchIfAny '" + searchIfAny +"'");
      pushFirstSelection(exerciseID, searchIfAny);
    }
  }

  protected void listLoaded() {
  }

  private boolean isStaleResponse(ExerciseListWrapper result) {
    return result.getReqID() < lastReqID;
  }

  protected abstract List<T> rememberExercises(List<T> result);

  /**
   * Worry about deleting the currently visible item.
   *
   * @param es
   * @see PagingExerciseList#forgetExercise(int)
   */
  public T removeExercise(T es) {
    int id = es.getID();
    T current = getCurrentExercise();
    if (current.getID() ==id) {
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
  public void hide() { getParent().setVisible(false);  }

  /**
   * If we're not already showing this item, ask there server for the exercise.
   * Does this by pushing a history item and then noticing the history item change.
   *
   * @see #rememberAndLoadFirst
   * @param searchIfAny
   */
  protected void loadFirstExercise(String searchIfAny) {
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      if (DEBUG) logger.info("loadFirstExercise (" + instance + ") : current exercises is empty?");
      removeCurrentExercise();
    } else {
      T toLoad = findFirstExercise();
      if (DEBUG) logger.info("loadFirstExercise ex id =" + toLoad.getID() + " instance " + instance);
      pushFirstSelection(toLoad.getID(), searchIfAny);
    }
  }

  public void loadFirst() {
    pushFirstSelection(getFirst().getID(), "");
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

  protected abstract T getFirst();

  protected boolean hasExercise(int id) {
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
//    if (DEBUG) logger.info("ExerciseList.loadExercise itemID " + itemID);
    pushNewItem("", itemID);
  }

  @Override
  public void checkAndAskServer(int id) {
    if (DEBUG) {
      logger.info(getClass() + " : (" + instance + ") ExerciseList.checkAndAskServer - askServerForExercise = " + id);
    }

    if (hasExercise(id)) {
//      if (//!getCurrentExerciseID().equals(id) ||
//          createdPanel == null ||
//          (
//              createdPanel.getElement().getExID().equals(EMPTY_PANEL))) {
      askServerForExercise(id);
//      } else {
//        logger.info("got " + hasExercise(id) + " current " + getCurrentExerciseID() + " vs " + id);
//      }
    } else if (id != EditItem.NEW_EXERCISE_ID) {
      logger.warning("checkAndAskServer : can't load " + id);
    }
    else {
      logger.warning("checkAndAskServer : skipping request for " + id);
    }
  }

  protected abstract Set<Integer> getKeys();

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
    userState.checkUser();
    if (cachedNext != null && cachedNext.getID() == itemID) {
      if (DEBUG)
        logger.info("\tExerciseList.askServerForExercise using cached id = " + itemID + " instance " + instance);
      useExercise(cachedNext);
    } else {
      pendingReq = true;
      if (DEBUG || true) logger.info("ExerciseList.askServerForExercise id = " + itemID + " instance " + instance);
      service.getExercise(itemID, incorrectFirstOrder, new ExerciseAsyncCallback());
    }

    // go get next and cache it
    goGetNextAndCacheIt(itemID);
  }

  private void goGetNextAndCacheIt(int itemID) {
    int i = getIndex(itemID);
    if (!isOnLastItem(i)) {
      T next = getAt(i + 1);
      if (next.getID() != EditItem.NEW_EXERCISE_ID) {
      //  logger.info("ask for next " + next);
        service.getExercise(next.getID(), incorrectFirstOrder, new AsyncCallback<U>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(U result) {
            cachedNext = result;
            //if (DEBUG) logger.info("\tExerciseList.askServerForExercise got cached id = " + cachedNext.getOldID() + " instance " + instance);
          }
        });
      }
    }
  }

  private int getUser() {
    return userState.getUser();
  }

  public void clearCachedExercise() {
    cachedNext = null;
  }

  private class ExerciseAsyncCallback implements AsyncCallback<U> {
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
        if (DEBUG) logger.info("ex " + caught.getMessage() + " " + caught);
      }
    }

    @Override
    public void onSuccess(U result) {
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
   * @see #rememberAndLoadFirst
   * @see ExerciseAsyncCallback#onSuccess
   */
  protected void useExercise(final U commonExercise) {
    //  logger.info("ExerciseList.useExercise : commonExercise " + commonExercise.getOldID());
   // String itemID = commonExercise.getOldID();
    markCurrentExercise(commonExercise.getID());

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
/*        logger.info("ExerciseList.useExercise : item id " + itemID + " currentExercise " +getCurrentExercise() +
      " or " + getCurrentExerciseID() + " instance " + instance);*/

        createdPanel = makeExercisePanel(commonExercise);
      }
    });

  }

  public int getCurrentExerciseID() {
    return getCurrentExercise() != null ? getCurrentExercise().getID() : -1;//"Unknown";
  }

  protected abstract T getCurrentExercise();

  /**
   * @param exercise
   * @see #useExercise
   */
  private Panel makeExercisePanel(U exercise) {
    if (DEBUG) logger.info("ExerciseList.makeExercisePanel : " + exercise + " instance " + instance);
    Panel exercisePanel = factory.getExercisePanel(exercise);
    innerContainer.setWidget(exercisePanel);
    return exercisePanel;
  }

  /**
   * @return
   * @see mitll.langtest.client.list.PagingExerciseList#gotClickOnItem
   */
/*  boolean isExercisePanelBusy() {
    Widget current = innerContainer.getWidget();
    return current != null && current instanceof BusyPanel && ((BusyPanel) current).isBusy();
  }*/

  /**
   * @param current
   * @see ListInterface#loadNextExercise
   */
  private void getNextExercise(HasID current) {
    // if (DEBUG) logger.info("ExerciseList.getNextExercise " + current);
    int i = getIndex(current.getID());
    if (i == -1) {
      logger.warning("ExerciseList.getNextExercise : huh? couldn't find " + current +
          " in " + getSize() + " exercises : " + getKeys());
    } else {
      Shell next = getAt(i + 1);
      if (DEBUG) logger.info("ExerciseList.getNextExercise " + next);
      loadExercise(next.getID());
    }
  }

  /**
   * @param currentID
   * @return
   * @see #useExercise
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
    return getIndex(getCurrentExerciseID());
  }

  /**
   * @see #removeExercise
   */
  //@Override
  void removeCurrentExercise() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (!innerContainer.remove(current)) {
        logger.warning("\tdidn't remove current widget");
      } else innerContainer.getParent().removeStyleName("shadowBorder");
    } else {
      logger.warning("\tremoveCurrentExercise : no inner current widget for " + reportLocal());
    }
  }

  public String reportLocal() {
    return "list " + getInstance() + " id " + getElement().getId();
  }

  private void removeComponents() {
    super.clear();
  }

  /**
   * Compare with google response for this state.
   */
  void showEmptyExercise() {
    createdPanel = new SimplePanel(new Heading(3,"<b>Your search or selection did not match any items.</b>"));
    createdPanel.getElement().setId(EMPTY_PANEL);
    innerContainer.setWidget(createdPanel);
  }

  @Override
  public void clear() {
    removeComponents();
  }

  protected abstract void markCurrentExercise(int itemID);

  @Override
  public boolean loadNext() {
    return loadNextExercise(getCurrentExerciseID());
  }

  /**
   * @param current
   * @return
   * @seex AmasNavigationHelper#loadNextExercise
   */
  @Override
  public boolean loadNextExercise(HasID current) {
    if (DEBUG) logger.info("ExerciseList.loadNextExercise current is : " + current + " instance " + instance);
   // String id = current.getID();
    int i = getIndex(current.getID());
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

  public boolean loadNextExercise(int id) {
    if (DEBUG) logger.info("ExerciseList.loadNextExercise id = " + id + " instance " + instance);
    T exerciseByID = byID(id);
    if (exerciseByID == null) logger.warning("huh? couldn't find exercise with id " + id);
    return exerciseByID != null && loadNextExercise(exerciseByID);
  }

  /**
   * @see ListInterface#loadNextExercise
   */
  protected void onLastItem() {
    loadFirstExercise("");
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
    boolean b = current == null || getSize() == 1 || isOnLastItem(getIndex(current.getID()));
    return b;
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

  protected abstract List<T> getInOrder();

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

  public Collection<Integer> getIDs() {
    Set<Integer> ids = new HashSet<>();
    for (T cs : getInOrder()) ids.add(cs.getID());
    return ids;
  }
}
