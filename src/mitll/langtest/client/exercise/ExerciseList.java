package mitll.langtest.client.exercise;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles left side of NetPron2 -- which exercise is the current one, highlighting, etc.
 *
 * User: GO22670
 * Date: 7/9/12
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseList extends VerticalPanel implements ProvidesResize, RequiresResize {
  protected List<Exercise> currentExercises = null;
  private int currentExercise = 0;
  private List<HTML> progressMarkers = new ArrayList<HTML>();
  private Panel current = null;

  private Panel currentExerciseVPanel;
  protected LangTestDatabaseAsync service;
  private UserFeedback feedback;
  private ExercisePanelFactory factory;
  protected int expectedGrades = 1;
  protected UserManager user;
  private String exercise_title;
  private boolean goodwaveMode;
  protected final boolean arabicDataCollect;
  protected final boolean showTurkToken;

  /**
   * @see  mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param goodwaveMode
   * @param arabicDataCollect
   * @param showTurkToken
   */
  public ExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                      ExercisePanelFactory factory, boolean goodwaveMode, boolean arabicDataCollect, boolean showTurkToken) {
    this.currentExerciseVPanel = currentExerciseVPanel;
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
    this.goodwaveMode = goodwaveMode;
    this.arabicDataCollect = arabicDataCollect;
    this.showTurkToken = showTurkToken;
  }

  /**
   * @see mitll.langtest.client.LangTest#setGrading
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
 //   doGrading = false;
  //  GWT.log("goodwave mode = " +goodwaveMode);
    service.getExercises(userID, goodwaveMode, arabicDataCollect, new SetExercisesCallback());
  }

  public void getExercisesInOrder() {
    service.getExercises(goodwaveMode, arabicDataCollect, new SetExercisesCallback());
  }

  public void onResize() {
    if (current != null && current instanceof RequiresResize) ((RequiresResize)current).onResize();
  }

  /**
   * So you an load a specific exercise
   * @param exercise_title
   */
  public void setExercise_title(String exercise_title) {
    this.exercise_title = exercise_title;
  }

  protected class SetExercisesCallback implements AsyncCallback<List<Exercise>> {
    public void onFailure(Throwable caught) {
      feedback.showErrorMessage("Server error - couldn't get exercises.");
    }

    public void onSuccess(List<Exercise> result) {
      currentExercises = result; // remember current exercises
      for (final Exercise e : result) {
        addExerciseToList(e);
      }
      loadFirstExercise();
    }
  }

  private void addExerciseToList(final Exercise e) {
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
    Exercise toLoad = currentExercises.get(0);
    if (exercise_title != null) {
      Exercise e = byName(exercise_title);
      if (e != null) toLoad = e;
    }

    loadExercise(toLoad);
  }

  private Exercise byName(String name) {
    Exercise found = null;
    for (Exercise e : currentExercises) {
      String id = e.getID();
      if (id.equals(name)) {
        found = e;
        break;
      }
    }
    return found;
  }

  /**
   * @see #addExerciseToList(mitll.langtest.shared.Exercise)
   * @see #loadFirstExercise()
   * @see #loadNextExercise(mitll.langtest.shared.Exercise)
   * @see #loadPreviousExercise(mitll.langtest.shared.Exercise)
   * @param e
   */
  protected void loadExercise(Exercise e) {
    checkBeforeLoad(e);

    removeCurrentExercise();

    currentExerciseVPanel.add(current = factory.getExercisePanel(e));
    int i = currentExercises.indexOf(e);
    if (i == -1) {
      System.err.println("can't find " + e + " in list of " + currentExercises.size() + " exercises.");
      return;
    }
    markCurrentExercise(i);
    currentExercise = i;
  }

  protected void checkBeforeLoad(Exercise e) {
    feedback.login();
  }

  protected void getNextExercise(Exercise current) {
    int i = currentExercises.indexOf(current);
    loadExercise(currentExercises.get(i+1));
  }

  public void removeCurrentExercise() {
    if (current != null) {
      currentExerciseVPanel.remove(current);
      current = null;
    }
  }

  @Override
  public void clear() {
    super.clear();
    progressMarkers.clear();
  }

  private void markCurrentExercise(int i) {
    HTML html = progressMarkers.get(currentExercise);
    html.setStyleDependentName("highlighted", false);
    html = progressMarkers.get(i);
    html.setStyleDependentName("highlighted", true);
    html.getElement().scrollIntoView();
  }

  private int countSincePrompt = 0;
  public boolean loadNextExercise(Exercise current) {
    int i = currentExercises.indexOf(current);
    boolean onLast = i == currentExercises.size() - 1;
    if (onLast) {
      feedback.showErrorMessage("Test Complete! Thank you!");
    }
    else {
      getNextExercise(current);
    }
    if (showTurkToken && (onLast || ++countSincePrompt % 5 == 0)) {
      Window.alert("Please enter this token : " + user.getUser() + "_" + current.getID());
    }
    return onLast;
  }

  public boolean loadPreviousExercise(Exercise current) {
    int i = currentExercises.indexOf(current);
    boolean onFirst = i == 0;
    if (onFirst) {}
    else {
      loadExercise(currentExercises.get(i-1));
    }
    return onFirst;
  }

  public boolean onFirst(Exercise current) { return currentExercises.indexOf(current) == 0; }
}
