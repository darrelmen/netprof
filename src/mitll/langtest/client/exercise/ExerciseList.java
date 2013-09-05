package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
  protected List<ExerciseShell> currentExercises = null;
  protected Map<String,ExerciseShell> idToExercise = null;
  protected int currentExercise = 0;
  private List<HTML> progressMarkers = new ArrayList<HTML>();

  private SimplePanel innerContainer;
  protected LangTestDatabaseAsync service;
  protected UserFeedback feedback;
  private ExercisePanelFactory factory;
  protected UserManager user;
  private String exercise_title;
  protected final boolean showTurkToken;
  private final boolean showInOrder;
  private int countSincePrompt = 0;
  protected int lastReqID = 0;
  private int adHocCount = 0;
  private Set<Integer> visited = new HashSet<Integer>();

  /**
   * @see  mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param showTurkToken
   * @param showInOrder
   */
  public ExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                      ExercisePanelFactory factory,
                      boolean showTurkToken, boolean showInOrder) {
    addWidgets(currentExerciseVPanel);
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
    this.showTurkToken = showTurkToken;
    this.showInOrder = showInOrder;

    // Add history listener
    History.addValueChangeHandler(this);
  }

  protected void addWidgets(Panel currentExerciseVPanel) {
    this.innerContainer = new SimplePanel();
    this.innerContainer.setWidth("100%");
    this.innerContainer.setHeight("100%");
    currentExerciseVPanel.add(innerContainer);
  }

  /**
   * @see mitll.langtest.client.LangTest#setFactory
   * @param factory
   * @param user
   * @paramx doGrading
   * @param expectedGrades
   */
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    this.factory = factory;
    this.user = user;
  }

  public ExercisePanelFactory getFactory() { return factory; }

  /**
   * Get exercises for this user.
   * @see mitll.langtest.client.LangTest#doEverythingAfterFactory(long)
   * @see SectionExerciseList#noSectionsGetExercises(long)
   * @param userID
   */
  public void getExercises(long userID) {
    System.out.println("ExerciseList.getExercises for user " +userID);

    if (showInOrder) {
      getExercisesInOrder();
    } else {
      lastReqID++;
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
   * Calling this will result in an immediate call to onValueChange (reacting to the history change)
   *
   * @see #loadExercise(mitll.langtest.shared.ExerciseShell)
   * @see #pushFirstSelection(String)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param exerciseID
   */
  protected void pushNewItem(String exerciseID) {
    System.out.println("------------ ExerciseList.pushNewItem : push history " + exerciseID + " -------------- ");
    History.newItem("#item=" + exerciseID);
  }

  /**
   * @see mitll.langtest.client.grading.GradedExerciseList#setFactory(ExercisePanelFactory, mitll.langtest.client.user.UserManager, int)
   */
  private void getExercisesInOrder() {
    lastReqID++;
    service.getExerciseIds(lastReqID,new SetExercisesCallback());
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

  protected String unencodeToken(String token) {
    token = token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ").replaceAll("\\+", " ");
    return token;
  }

  /**
   * @see #getExercises(long)
   */
  protected class SetExercisesCallback implements AsyncCallback<ExerciseListWrapper> {
    public void onFailure(Throwable caught) {
      feedback.showErrorMessage("Server error", "Server error - couldn't get exercises.");
    }

    public void onSuccess(ExerciseListWrapper result) {
      //System.out.println("SetExercisesCallback Got " + result.exercises.size() + " results");
      if (isStaleResponse(result)) {
        System.out.println("----> ignoring result " + result.reqID + " b/c before latest " + lastReqID);
      } else {
        rememberAndLoadFirst(result.exercises);
      }
    }
  }

  public void rememberAndLoadFirst(List<ExerciseShell> exercises) {
    rememberExercises(exercises);
    loadFirstExercise();
  }

  protected boolean isStaleResponse(ExerciseListWrapper result) {
    return result.reqID < lastReqID;
  }

  protected void rememberExercises(List<ExerciseShell> result) {
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

/*  public Collection<ExerciseShell> getExerciseShells() {
    return idToExercise.values();
  }
  */
  private Random random = new Random();
  public void askForRandomExercise(AsyncCallback<Exercise> callback) {
    ExerciseShell shell = currentExercises.get(random.nextInt(currentExercises.size()));
    service.getExercise(shell.getID(), callback);
  }

  public void setSelectionState(Map<String, Collection<String>> selectionState) {}

  @Override
  public void hideExerciseList() {
    getParent().setVisible(false);
  }

/*  public void showExerciseList() {
    getParent().setVisible(true);
  }*/

  protected void flush() {}

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

  protected HTML makeAndAddExerciseEntry(ExerciseShell e) {
    final HTML w = new HTML("<b>" + e.getID() + "</b>");
    w.setStylePrimaryName("exercise");
    add(w);
    progressMarkers.add(w);
    return w;
  }


  protected void loadFirstExercise() {
    if (currentExercises.isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      Window.alert("Server error : no exercises. Please contact administrator.");
    } else {
      ExerciseShell toLoad = currentExercises.get(0);

      if (exercise_title != null) {
        ExerciseShell e = byID(exercise_title);
        if (e != null) toLoad = e;
      }

      //System.out.println("loadFirstExercise ex id =" + toLoad.getID());
      pushFirstSelection(toLoad.getID());
    }
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
    //String token = History.getToken();
    //String id = getIDFromToken(unencodeToken(token));
    //System.out.println("ExerciseList.loadExercise token '" + token + "' and id '" +id +"'");

/*    if (id.equals(exerciseShell.getID())) {
      System.out.println("skipping current token " + token);
    } else {*/
      //System.out.println("loadExercise " + exerciseShell.getID() + " vs " +id);
      pushNewItem(exerciseShell.getID());
  //  }
  }

  /**
   * This method is called whenever the application's history changes.
   * @see #pushNewItem(String)
   * @param event
   */
  public void onValueChange(ValueChangeEvent<String> event) {
    // This method is called whenever the application's history changes. Set
    // the label to reflect the current history token.
    String value = event.getValue();
    String token = getTokenFromEvent(event);
    String id = getIDFromToken(token);
    System.out.println("ExerciseList.onValueChange got " + event.getAssociatedType() + " "+ value + " token " + token + " id " + id);

    if (id.length() > 0) {
      loadByIDFromToken(id);
    } else {
      System.out.println("got invalid event " + event + " value " + token);
    }
  }

  protected void loadByIDFromToken(String id) {
    ExerciseShell exerciseShell = byID(id);
    if (exerciseShell != null) {
      askServerForExercise(exerciseShell);
    }
    else {
      Window.alert("unknown item " + id);
      System.out.println("can't load " +id + " keys were " + idToExercise.keySet());
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

  protected String getTokenFromEvent(ValueChangeEvent<String> event) {
    String token = event.getValue();
    token = unencodeToken(token);
    return token;
  }

  protected String getIDFromToken(String token) {
    if (token.startsWith("#item=") || token.startsWith("item=")) {
      String[] split = token.split("=");
      return split[1].trim();
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
        Window.alert("Message from server: " + caught.getMessage());
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
    makeExercisePanel(result);

    int i = getIndex(e);
    System.out.println("ExerciseList.useExercise : " +e.getID() + " index " +i);
    if (i == -1) {
      System.err.println("can't find " + e + " in list of " + currentExercises.size() + " exercises.");
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
  public void makeExercisePanel(Exercise result) {
    Panel exercisePanel = factory.getExercisePanel(result);
    innerContainer.setWidget(exercisePanel);
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
    int i = getIndex(current);
    if (i == -1)
      System.err.println("ExerciseList.getNextExercise : huh? couldn't find " + current + " in " + currentExercises.size() + " exercises : " + idToExercise.keySet());
    else {
      ExerciseShell next = currentExercises.get(i + 1);
      loadExercise(next);
    }
  }

  private int getIndex(ExerciseShell current) {
    String id = current.getID();
    ExerciseShell shell = idToExercise.get(id);
    int i = shell != null ? currentExercises.indexOf(shell) : -1;
/*    System.out.println("index of '" + id + "' is #" + i + " and item is " + current +
        " map size " + idToExercise.size() + " exercise list size " +currentExercises.size());*/
    return i;
  }

  @Override
  public int getPercentComplete() { return (int) (100f*((float)visited.size()/(float)currentExercises.size())); }
  @Override
  public int getComplete() { return visited.size(); }

  @Override
  public void addAdHocExercise(String label) {
    addExerciseToList(new ExerciseShell("ID_"+(adHocCount++),label));
    flush();
  }

  @Override
  public void removeCurrentExercise() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      //System.out.println("Remove current widget");
      if (!innerContainer.remove(current)) {
        System.out.println("\tdidn't remove current widget");
      }
    }
  }

  protected void removeComponents() {
    super.clear();
  }

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

  protected void unselectCurrent() {
    HTML html = progressMarkers.get(currentExercise);
    html.setStyleDependentName("highlighted", false);
  }

  /**
   * @see mitll.langtest.client.LangTest#loadNextExercise
   * @param current
   * @return
   */
  @Override
  public boolean loadNextExercise(ExerciseShell current) {
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

  protected boolean isOnLastItem(int i) {
    return i == currentExercises.size() - 1;
  }

  public boolean isLastExercise(String id) {
    System.out.println("ExerciseList.isLastExercise " + id);
    ExerciseShell exerciseByID = getExerciseByID(id);
    return exerciseByID != null && isOnLastItem(getIndex(exerciseByID));
  }

  public boolean loadNextExercise(String id) {
    System.out.println("ExerciseList.loadNextExercise " + id);
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
    feedback.showErrorMessage("Test Complete", "Test Complete! Thank you!");
  }

  public void startOver() {}


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
   * @see mitll.langtest.client.LangTest#loadPreviousExercise(mitll.langtest.shared.Exercise)
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

    //leftColumnContainer.add(leftColumn);
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
   * @see mitll.langtest.client.LangTest#onFirst(mitll.langtest.shared.Exercise)
   * @param current
   * @return
   */
  @Override
  public boolean onFirst(ExerciseShell current) { return getIndex(current) == 0; }

  @Override
  public void reloadExercises() {}
}
