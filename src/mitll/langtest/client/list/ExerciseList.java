package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  public static final String ITEMS = "Items";
  protected List<? extends ExerciseShell> currentExercises = null;
  private Map<String,ExerciseShell> idToExercise = null;
  protected int currentExercise = 0;
  private final List<HTML> progressMarkers = new ArrayList<HTML>();

  private SimplePanel innerContainer;
  protected final LangTestDatabaseAsync service;
  protected final UserFeedback feedback;
  private ExercisePanelFactory factory;
  private Panel createdPanel;
  protected UserManager user;
  private String exercise_title;
  private final boolean showTurkToken;
  private final boolean showInOrder;
  private int countSincePrompt = 0;
  protected int lastReqID = 0;
  private final Set<Integer> visited = new HashSet<Integer>();
  protected final boolean allowPlusInURL;
  private final ExerciseController controller;
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

    System.out.println("ExerciseList : got instance  " + instance);

    // Add history listener
    History.addValueChangeHandler(this);
  }

  private void addWidgets(final Panel currentExerciseVPanel) {
    this.innerContainer = new SimplePanel();
    this.innerContainer.setWidth("100%");
    this.innerContainer.setHeight("100%");
    currentExerciseVPanel.add(innerContainer);
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        currentExerciseVPanel.addStyleName("userNPFContent");
      }
    });
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
      System.out.println("\tpushFirstSelection :current token " + token + " same as new " +exerciseID);
      loadByIDFromToken(exerciseID);
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
   * @see #loadExercise(mitll.langtest.shared.ExerciseShell)
   * @see #pushFirstSelection(String)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param exerciseID
   */
  protected void pushNewItem(String exerciseID) {
    System.out.println("------------ ExerciseList.pushNewItem : push history " + exerciseID + " -------------- ");
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
  protected class SetExercisesCallback implements AsyncCallback<ExerciseListWrapper> {
    public void onFailure(Throwable caught) {
      if (!caught.getMessage().trim().equals("0")) {
        feedback.showErrorMessage("Server error", "SetExercisesCallback : Server error - couldn't get exercises.");
      }
      System.out.println("Got exception '" +caught.getMessage() + "' " +caught);
    }
    public void onSuccess(ExerciseListWrapper result) {
      System.out.println("ExerciseList.SetExercisesCallback Got " + result.getExercises().size() + " results");
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

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() {
    System.out.println(new Date() +" gotEmptyExerciseList : ------  ------------ ");
  }

  /**
   * @see ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @param exercises
   */
  public void rememberAndLoadFirst(List<? extends ExerciseShell> exercises, Exercise firstExercise) {
    rememberExercises(exercises);
    if (firstExercise != null) {
      ExerciseShell firstExerciseShell = findFirstExercise();
      if (firstExerciseShell.getID().equals(firstExercise.getID())) {
        useExercise(firstExercise, firstExerciseShell);   // allows us to skip another round trip with the server to ask for the first exercise
      }
      else {
        loadFirstExercise();
      }
    }
    else {
      loadFirstExercise();
    }
  }

  protected boolean isStaleResponse(ExerciseListWrapper result) {
    return result.getReqID() < lastReqID;
  }

  protected void rememberExercises(List<? extends ExerciseShell> result) {
    System.out.println("ExerciseList : remembering " + result.size() + " exercises");
    currentExercises = result; // remember current exercises
    currentExercise = 0;
    idToExercise = new HashMap<String, ExerciseShell>();
    clear();
    for (final ExerciseShell es : result) {
      idToExercise.put(es.getID(),es);
      addExerciseToList(es);
    }
    flush();
  }

  public void setSelectionState(Map<String, Collection<String>> selectionState) {}

  @Override
  public void hideExerciseList() {
    getParent().setVisible(false);
  }

  /**
   * @see #rememberExercises
   */
  protected void flush() {}

  /**
   * @see #rememberExercises
   */
  protected void addExerciseToList(final ExerciseShell e) {
    final HTML w = makeAndAddExerciseEntry(e);

    w.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        loadExercise(e);
      }
    });
    w.addMouseOverHandler(new MouseOverHandler() {
      public void onMouseOver(MouseOverEvent event) {
        w.addStyleName("clickable");
      }
    });
    w.addMouseOutHandler(new MouseOutHandler() {
      public void onMouseOut(MouseOutEvent event) {
        w.removeStyleName("clickable");
      }
    });
  }

  /**
   * @see #rememberExercises
   */
  private HTML makeAndAddExerciseEntry(ExerciseShell e) {
    final HTML w = new HTML("<b>" + e.getID() + "</b>");
    w.setStylePrimaryName("exercise");
    add(w);
    progressMarkers.add(w);
    return w;
  }

  /**
   * @see #rememberAndLoadFirst
   */
  protected void loadFirstExercise() {
    if (currentExercises.isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      noMatches();
      System.err.println("loadFirstExercise : current exercises is empty?");
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

  /**
   */
  private void noMatches() {
    final PopupPanel pleaseWait = showPleaseWait("No matches found. Please try a different search.");

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

  protected ExerciseShell findFirstExercise() {
    return currentExercises.get(0);
  }

  private ExerciseShell byID(String name) {
    return idToExercise == null ? null : idToExercise.get(name);
  }

  protected boolean hasExercise(String id) { return byID(id) != null; }

  /**
   * @see #addExerciseToList(mitll.langtest.shared.ExerciseShell)
   * @see #loadFirstExercise()
   * @see #loadNextExercise
   * @see #loadPreviousExercise
   * @param exerciseShell
   */
  public void loadExercise(ExerciseShell exerciseShell) {
    pushNewItem(exerciseShell.getID());
  }

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
      loadByIDFromToken(id);
    } else {
      System.out.println("ExerciseList.onValueChange : got invalid event " + event + " value " + token);
    }
  }

  protected void loadByIDFromToken(String id) {
    ExerciseShell exerciseShell = byID(id);
    if (exerciseShell != null) {
      askServerForExercise(exerciseShell);
    }
    else {
      System.out.println("can't load " +id + " keys were " + idToExercise.keySet());
    }
  }

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
        System.out.println("got history item for another instance '" + selectionState.getInstance() + "' vs me '" + instance +"'");
      } else {
        String item = selectionState.getItem();
        System.out.println("got history item for instance '" + selectionState.getInstance() + " : '" + item+"'");
        return item;
      }
    }
    return "";
  }

  protected boolean loadByID(String id) {
    ExerciseShell exerciseShell = byID(id);
    if (exerciseShell != null) {
      System.out.println("loading exercise " + id);
      loadExercise(exerciseShell);
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * @see #loadByIDFromToken(String)
   * @param exerciseShell
   */
  protected void askServerForExercise(ExerciseShell exerciseShell) {
    System.out.println("ExerciseList.askServerForExercise id = " + exerciseShell.getID());
    service.getExercise(exerciseShell.getID(), new ExerciseAsyncCallback(exerciseShell));
  }

  private class ExerciseAsyncCallback implements AsyncCallback<Exercise> {
    private final ExerciseShell exerciseShell;

    /**
     * @see ExerciseList#askServerForExercise(mitll.langtest.shared.ExerciseShell)
     * @param exerciseShell
     */
    public ExerciseAsyncCallback(ExerciseShell exerciseShell) { this.exerciseShell = exerciseShell; }

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
        useExercise(result, exerciseShell);
      }
    }
  }

  /**
   * @see ExerciseAsyncCallback#onSuccess(mitll.langtest.shared.Exercise)
   * @param result
   * @param e
   */
  private void useExercise(Exercise result, ExerciseShell e) {
    createdPanel = makeExercisePanel(result);
    int i = getIndex(e);
//  System.out.println("ExerciseList.useExercise : " +e.getID() + " index " +i);
    if (i == -1) {
      System.err.println("useExercise can't find " + e + " in list of " + currentExercises.size() +
        " exercises (" +currentExercises+ ")");
      return;
    }

    markCurrentExercise(i);
    currentExercise = i;
  }

  public String getCurrentExerciseID() { return currentExercises != null ? currentExercises.get(currentExercise).getID() : "Unknown"; }

  /**
   * @see #useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param result
   */
  public Panel makeExercisePanel(Exercise result) {
    System.out.println("ExerciseList.makeExercisePanel : " +result);

    Panel exercisePanel = factory.getExercisePanel(result);
    innerContainer.setWidget(exercisePanel);
    return exercisePanel;
  }

  protected boolean isExercisePanelBusy() {
    Widget current = innerContainer.getWidget();
    return current != null && current instanceof BusyPanel && ((BusyPanel) current).isBusy();
  }

  /**
   * @see #loadNextExercise(mitll.langtest.shared.ExerciseShell)
   * @param current
   */
  protected void getNextExercise(ExerciseShell current) {
    System.out.println("getNextExercise " + current);

    int i = getIndex(current);
    if (i == -1)
      System.err.println("ExerciseList.getNextExercise : huh? couldn't find " + current + " in " + currentExercises.size() + " exercises : " + idToExercise.keySet());
    else {
      ExerciseShell next = currentExercises.get(i + 1);
      loadExercise(next);
    }
  }

  /**
   * @see #useExercise(mitll.langtest.shared.Exercise, mitll.langtest.shared.ExerciseShell)
   * @param current
   * @return
   */
  private int getIndex(ExerciseShell current) {
    ExerciseShell shell = idToExercise.get(current.getID());
    return shell != null ? currentExercises.indexOf(shell) : -1;
  }

  @Override
  public int getPercentComplete() {
    float ratio = (float) visited.size() / (float) currentExercises.size();
    System.out.println("Ratio " + ratio);
    return (int) (Math.ceil(100f * ratio));
  }

  @Override
  public int getComplete() {
    return visited.size();
  }

  @Override
  public void removeCurrentExercise() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      if (!innerContainer.remove(current)) {
        System.err.println("\tdidn't remove current widget");
      }
    }
  }

  protected void removeComponents() { super.clear();  }

  @Override
  public void clear() {
    removeComponents();
    progressMarkers.clear();
  }

  protected void markCurrentExercise(int i) {
    unselectCurrent();
    HTML html = progressMarkers.get(i);
    html.setStyleDependentName("highlighted", true);
    html.getElement().scrollIntoView();
  }

  private void unselectCurrent() {
    HTML html = progressMarkers.get(currentExercise);
    html.setStyleDependentName("highlighted", false);
  }

  /**
   * @seex NavigationHelper#loadNextExercise
   * @param current
   * @return
   */
  @Override
  public boolean loadNextExercise(ExerciseShell current) {
    System.out.println("ExerciseList.loadNextExercise current is : " +current);
    int i = getIndex(current);

    visited.add(i);

    boolean onLast = isOnLastItem(i);
    System.out.println("ExerciseList.loadNextExercise current is : " +current + " index " +i +
      " of " + currentExercises.size() +" last is " + (currentExercises.size() - 1)+" on last " + onLast);

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
    ExerciseShell exerciseByID = getExerciseByID(id);
    return exerciseByID != null && loadNextExercise(exerciseByID);
  }

  private ExerciseShell getExerciseByID(String id) {
    for (ExerciseShell e : currentExercises) {
      if (e.getID().equals(id)) {
        return e;
      }
    }
    return null;
  }

  /**
   * @see #loadNextExercise(mitll.langtest.shared.ExerciseShell)
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

  /**
   * @seex NavigationHelper#loadPreviousExercise
   * @param current
   * @return
   */
  @Override
  public boolean loadPreviousExercise(ExerciseShell current) {
    int i = getIndex(current);
    boolean onFirst = i == 0;
    if (!onFirst) {
      loadExercise(currentExercises.get(i-1));
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

  /**
   * @see mitll.langtest.client.exercise.NavigationHelper#makePrevButton
   * @param current
   * @return
   */
  @Override
  public boolean onFirst(ExerciseShell current) {
    System.out.println("onFirst : of " +currentExercises.size() +", on first is " + current);
    return current == null || currentExercises.size() == 1 || getIndex(current) == 0;
  }

  @Override
  public boolean onLast(ExerciseShell current) {
    return current == null || currentExercises.size() == 1 || isOnLastItem(getIndex(current));
  }

  private boolean isOnLastItem(int i) {
    return i == currentExercises.size() - 1;
  }

  @Override
  public void reloadExercises() {
    loadFirstExercise();
  }
}
