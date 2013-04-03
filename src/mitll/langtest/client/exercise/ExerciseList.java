package mitll.langtest.client.exercise;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private boolean useUserID = false;
  private long userID;
  private final boolean showInOrder;
  private int countSincePrompt = 0;

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
   * @see mitll.langtest.client.LangTest#setFactory()
   * @param factory
   * @param user
   * @paramx doGrading
   * @param expectedGrades
   */
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    this.factory = factory;
    this.user = user;
  }

  /**
   * Get exercises for this user.
   * @see mitll.langtest.client.LangTest#gotUser(long)
   * @see mitll.langtest.client.LangTest#makeFlashContainer
   * @param userID
   */
  public void getExercises(long userID) {
    System.out.println("getExercises for user " +userID);

    useUserID = true;
    this.userID = userID;
    if (showInOrder) {
      getExercisesInOrder();
    }
    else {
      service.getExerciseIds(userID, new SetExercisesCallback());
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
/*    String initToken = History.getToken();
    if (initToken.length() == 0) {
      pushNewItem(first);
    } else {
      System.out.println("fire history for " +initToken);
      History.fireCurrentHistoryState();
    }*/

    String token = History.getToken();
    System.out.println("pushFirstSelection : current token " + token + " vs new " +exerciseID);
    if (token != null && getIDFromToken(token).equals(exerciseID)) {
      System.out.println("current token " + token + " same as new " +exerciseID);
      loadByIDFromToken(exerciseID);
    }
    else {
      pushNewItem(exerciseID);
    }
  }

  /**
   * @see #loadExercise(mitll.langtest.shared.ExerciseShell)
   * @see #pushFirstSelection(String)
   * @param exerciseID
   */
  protected void pushNewItem(String exerciseID) {
    System.out.println("------------ pushNewItem : push history " + exerciseID + " -------------- ");
    History.newItem("#item=" + exerciseID);
  }

  /**
   * @see GradedExerciseList#setFactory(ExercisePanelFactory, mitll.langtest.client.user.UserManager, int)
   */
  private void getExercisesInOrder() {
    service.getExerciseIds(new SetExercisesCallback());
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
   * @see mitll.langtest.client.LangTest#makeExerciseList(com.google.gwt.user.client.ui.Panel, boolean)
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

  protected class SetExercisesCallback implements AsyncCallback<List<ExerciseShell>> {
    public void onFailure(Throwable caught) {
      feedback.showErrorMessage("Server error", "Server error - couldn't get exercises.");
    }

    public void onSuccess(List<ExerciseShell> result) {
      System.out.println("SetExercisesCallback Got " +result.size() + " results");
      rememberExercises(result);
      loadFirstExercise();
    }
  }

  protected void rememberExercises(List<ExerciseShell> result) {
    System.out.println("remembering " + result.size() + " exercises");
    currentExercises = result; // remember current exercises
    idToExercise = new HashMap<String, ExerciseShell>();
    clear();
    for (final ExerciseShell es : result) {
      idToExercise.put(es.getID(),es);
      addExerciseToList(es);
    }
    flush();
  }

  protected void flush() {}

  protected void addExerciseToList(final ExerciseShell e) {
    final HTML w = new HTML("<b>" + e.getID() + "</b>");
    w.setStylePrimaryName("exercise");
    add(w);
    progressMarkers.add(w);

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

  protected void loadFirstExercise() {
    if (currentExercises.isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      Window.alert("Server error : no exercises. Please contact administrator.");
    } else {
      ExerciseShell toLoad = currentExercises.get(0);

      if (exercise_title != null) {
        ExerciseShell e = byID(exercise_title);
        if (e != null) toLoad = e;
      }

      System.out.println("loadFirstExercise ex id =" + toLoad.getID());
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
  protected void loadExercise(ExerciseShell exerciseShell) {
    String token = History.getToken();
    String id = getIDFromToken(unencodeToken(token));
    System.out.println("loadExercise " + token + " -> " +id);

/*    if (id.equals(exerciseShell.getID())) {
      System.out.println("skipping current token " + token);
    } else {*/
      //System.out.println("loadExercise " + exerciseShell.getID() + " vs " +id);
      pushNewItem(exerciseShell.getID());
  //  }
  }

  private void askServerForExercise(ExerciseShell exerciseShell) {
    if (useUserID) {
      service.getExercise(exerciseShell.getID(), userID, new ExerciseAsyncCallback(exerciseShell));
    } else {
      service.getExercise(exerciseShell.getID(), new ExerciseAsyncCallback(exerciseShell));
    }
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
    System.out.println("onValueChange got " + event.getAssociatedType() + " "+ value + " token " + token + " id " + id);

    if (id.length() > 0) {
      loadByIDFromToken(id);
    } else {
      System.out.println("got invalid event " + event + " value " + token);
    }
  }

  protected void loadByIDFromToken(String id) {
    ExerciseShell exerciseShell = byID(id);
    if (exerciseShell != null) {
      checkBeforeLoad(exerciseShell);
      askServerForExercise(exerciseShell);
    }
    else {
      Window.alert("unknown item " + id);
      System.out.println("can't load " +id + " keys were " + idToExercise.keySet());
    }
  }

/*
  protected String getIDFromToken(ValueChangeEvent<String> event) {
    String token = getTokenFromEvent(event);
    return getIDFromToken(token);
  }
*/

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
    public ExerciseAsyncCallback(ExerciseShell exerciseShell) { this.exerciseShell = exerciseShell; }

    @Override
    public void onFailure(Throwable caught) { Window.alert("Can't connect to server."); }
    @Override
    public void onSuccess(Exercise result)  { useExercise(result, exerciseShell); }
  }

  /**
   * @see ExerciseAsyncCallback#onSuccess(mitll.langtest.shared.Exercise)
   * @param result
   * @param e
   */
  private void useExercise(Exercise result, ExerciseShell e) {
    Panel exercisePanel = factory.getExercisePanel(result);
    innerContainer.setWidget(exercisePanel);

    int i = getIndex(e);
    //System.out.println("useExercise : " +e.getID() + " index " +i);
    if (i == -1) {
      System.err.println("can't find " + e + " in list of " + currentExercises.size() + " exercises.");
      return;
    }

    markCurrentExercise(i);
    currentExercise = i;
  }

  protected boolean isExercisePanelBusy() {
    Widget current = innerContainer.getWidget();
    return current != null && ((BusyPanel) current).isBusy();
  }

  /**
   * @see #loadExercise
   * @param e
   */
  protected void checkBeforeLoad(ExerciseShell e) {
    feedback.login();
  }

  /**
   * @see #loadNextExercise(mitll.langtest.shared.ExerciseShell)
   * @param current
   */
  protected void getNextExercise(ExerciseShell current) {
    int i = getIndex(current);
    if (i == -1) System.err.println("huh? couldn't find " +current + " in " + currentExercises.size() + " exercises?");
    ExerciseShell next = currentExercises.get(i + 1);
    loadExercise(next);
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
  public void removeCurrentExercise() {
    Widget current = innerContainer.getWidget();
    if (current != null) {
      System.out.println("Remove current widget");
      if (!innerContainer.remove(current)) {
        System.out.println("\tdidn't remove current widget");
      }
    }
  }

  @Override
  public void clear() {
    super.clear();
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
    //System.out.println("loadNextExercise " +current);
    int i = getIndex(current);

    boolean onLast = i == currentExercises.size() - 1;
    if (onLast) {
      feedback.showErrorMessage("Test Complete", "Test Complete! Thank you!");
    }
    else {
      getNextExercise(current);
    }
    if (showTurkToken && (onLast || ++countSincePrompt % NUM_QUESTIONS_FOR_TOKEN == 0)) {
      showTurkToken(current);
    }
    return onLast;
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

  @Override
  public Widget getWidget() {
    return this;
  }

  /**
   * @see mitll.langtest.client.LangTest#onFirst(mitll.langtest.shared.Exercise)
   * @param current
   * @return
   */
  @Override
  public boolean onFirst(ExerciseShell current) { return getIndex(current) == 0; }

  @Override
  public void reloadExercises() {

  }
}
