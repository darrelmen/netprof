package mitll.langtest.client.exercise;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
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
public abstract class ExerciseList extends VerticalPanel implements ListInterface, ProvidesResize {
  private static final int NUM_QUESTIONS_FOR_TOKEN = 5;
  protected List<ExerciseShell> currentExercises = null;
  protected Map<String,ExerciseShell> idToExercise = null;
  protected int currentExercise = 0;
  private List<HTML> progressMarkers = new ArrayList<HTML>();
  private Panel current = null;

  private Panel currentExerciseVPanel;
  protected LangTestDatabaseAsync service;
  private UserFeedback feedback;
  private ExercisePanelFactory factory;
  protected int expectedGrades = 1;
  protected UserManager user;
  private String exercise_title;
  private boolean readFromFile;
  protected final boolean arabicDataCollect;
  protected final boolean showTurkToken;
  private boolean useUserID = false;
  private long userID;
  private final boolean showInOrder;

  /**
   * @see  mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param readFromFile
   * @param arabicDataCollect
   * @param showTurkToken
   * @param showInOrder
   */
  public ExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                      ExercisePanelFactory factory, boolean readFromFile, boolean arabicDataCollect,
                      boolean showTurkToken, boolean showInOrder) {
    this.currentExerciseVPanel = currentExerciseVPanel;
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
    this.readFromFile = readFromFile;
    this.arabicDataCollect = arabicDataCollect;
    this.showTurkToken = showTurkToken;
    this.showInOrder = showInOrder;
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
    this.expectedGrades = expectedGrades;
  }

  /**
    * Get exercises for this user.
    * @see mitll.langtest.client.LangTest#gotUser(long)
    * @see mitll.langtest.client.LangTest#makeFlashContainer
    * @param userID
   */
  public void getExercises(long userID) {
    useUserID = true;
    this.userID = userID;
    if (showInOrder) {
      //System.out.println("getExercises : getting exercises in order for " + userID);
      getExercisesInOrder();
    }
    else {
      //System.out.println("getExercises for " + userID + " usefile " + readFromFile + " arabic " + arabicDataCollect);
      service.getExerciseIds(userID, readFromFile, arabicDataCollect, new SetExercisesCallback());
    }
  }

  /**
   * @see GradedExerciseList#setFactory(ExercisePanelFactory, mitll.langtest.client.user.UserManager, int)
   */
  public void getExercisesInOrder() {
    service.getExerciseIds(readFromFile, new SetExercisesCallback());
  }

  public void onResize() {
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

  private class SetExercisesCallback implements AsyncCallback<List<ExerciseShell>> {
    public void onFailure(Throwable caught) {
      feedback.showErrorMessage("Server error", "Server error - couldn't get exercises.");
    }

    public void onSuccess(List<ExerciseShell> result) {
     // System.out.println("SetExercisesCallback Got " +result.size() + " results");
      currentExercises = result; // remember current exercises
      idToExercise = new HashMap<String, ExerciseShell>();
      clear();
      for (final ExerciseShell es : result) {
        idToExercise.put(es.getID(),es);
        addExerciseToList(es);
      }
      flush();
      loadFirstExercise();
    }
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
    ExerciseShell toLoad = currentExercises.get(0);
    if (exercise_title != null) {
      ExerciseShell e = byID(exercise_title);
      if (e != null) toLoad = e;
    }

    loadExercise(toLoad);
  }

  private ExerciseShell byID(String name) {
    return idToExercise.get(name);
  }

  /**
   * @see #addExerciseToList(mitll.langtest.shared.ExerciseShell)
   * @see #loadFirstExercise()
   * @see #loadNextExercise
   * @see #loadPreviousExercise
   * @param exerciseShell
   */
  protected void loadExercise(ExerciseShell exerciseShell) {
    //System.out.println("loadExercise : " + exerciseShell);

    checkBeforeLoad(exerciseShell);

    removeCurrentExercise();

    if (useUserID) {
      service.getExercise(exerciseShell.getID(), userID, readFromFile, arabicDataCollect, new ExerciseAsyncCallback(exerciseShell));
    } else {
      service.getExercise(exerciseShell.getID(), readFromFile, new ExerciseAsyncCallback(exerciseShell));
    }
  }

  private class ExerciseAsyncCallback implements AsyncCallback<Exercise> {
    private final ExerciseShell exerciseShell;

    public ExerciseAsyncCallback(ExerciseShell exerciseShell) {
      this.exerciseShell = exerciseShell;
    }

    @Override
    public void onFailure(Throwable caught) {}

    @Override
    public void onSuccess(Exercise result) {
      useExercise(result, exerciseShell);
    }
  }

  /**
   * @see ExerciseAsyncCallback#onSuccess(mitll.langtest.shared.Exercise)
   * @param result
   * @param e
   */
  private void useExercise(Exercise result, ExerciseShell e) {
    //System.out.println("useExercise using " + e);
    currentExerciseVPanel.add(current = factory.getExercisePanel(result));

    int i = getIndex(e);
    if (i == -1) {
      System.err.println("can't find " + e + " in list of " + currentExercises.size() + " exercises.");
      return;
    }
    //System.out.println("useExercise marking " + i);

    markCurrentExercise(i);
    currentExercise = i;
  }

  protected boolean isExercisePanelBusy() { return current != null && ((BusyPanel) current).isBusy(); }

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
    if (current != null) {
      currentExerciseVPanel.remove(current);
      current = null;
    }
  }

  @Override
  public void clear() {
    //System.out.println("ExerciseList : Clearing list.");
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

  private int countSincePrompt = 0;

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
}
