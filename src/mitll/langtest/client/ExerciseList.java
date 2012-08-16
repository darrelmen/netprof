package mitll.langtest.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
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
public class ExerciseList extends VerticalPanel {
  private List<Exercise> currentExercises = null;
  private int currentExercise = 0;
  private List<HTML> progressMarkers = new ArrayList<HTML>();
  private Panel current = null;

  private Panel currentExerciseVPanel;
  private LangTestDatabaseAsync service;
  private UserFeedback feedback;
  private ExercisePanelFactory factory;
  private boolean doGrading = false;
  //private String currentActiveExercise;
  UserManager user;

  public ExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback, ExercisePanelFactory factory) {
    this.currentExerciseVPanel = currentExerciseVPanel;
    this.service = service;
    this.feedback = feedback;
    this.factory = factory;
  }

  /**
   * @see LangTest#setGrading
   * @param factory
   * @param user
   * @param doGrading
   */
  public void setFactory(ExercisePanelFactory factory, UserManager user, boolean doGrading) {
    this.doGrading = doGrading;
    this.factory = factory;
    this.user = user;
    // System.out.println("Factory is " +factory);
    if (doGrading) {
      loadGradingExercises();
    }
  }

  private void loadGradingExercises() {
    service.getExercises(new AsyncCallback<List<Exercise>>() {
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
    });
  }

  /**
    * Get exercises for this user.
    * @see LangTest#gotUser(long)
    * @see mitll.langtest.client.LangTest#makeFlashContainer()
    * @param userID
    */
  public void getExercises(long userID) {
    doGrading = false;
    //System.out.println("loading exercises for " + userID);
    service.getExercises(userID, new AsyncCallback<List<Exercise>>() {
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
    });
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

  private void loadFirstExercise() {
    Exercise toLoad = currentExercises.get(0);

    if (doGrading) {
      getNextUngraded();
    }
    else {
      loadExercise(toLoad);
    }
  }

  private void getNextUngraded() {
    service.getNextUngradedExercise(user.getGrader(), new AsyncCallback<Exercise>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Exercise result) {
        if (result != null) {
          for (Exercise e : currentExercises) {
            if (e.getID().equals(result.getID())) {
              loadExercise(e);
            }
          }
        }
      }
    });
  }

  private void checkoutExercise(Exercise result) {
     service.checkoutExerciseID(user.getGrader(), result.getID(), new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Void result) {}
    });
  }

  /**
   * @see #addExerciseToList(mitll.langtest.shared.Exercise)
   * @see #loadFirstExercise()
   * @see #loadNextExercise(mitll.langtest.shared.Exercise)
   * @see #loadPreviousExercise(mitll.langtest.shared.Exercise)
   * @param e
   */
  private void loadExercise(Exercise e) {
    if (!doGrading) {
      feedback.login();
    }

   // releaseExercise();
    if (doGrading) {
      checkoutExercise(e);
    }

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
  }

  public boolean loadNextExercise(Exercise current) {
    //showStatus("");
    int i = currentExercises.indexOf(current);
    boolean onLast = i == currentExercises.size() - 1;
    if (onLast) {
      feedback.showErrorMessage("Test Complete! Thank you!");
    }
    else {
      if (doGrading) {
        getNextUngraded();
      }
      else {
        loadExercise(currentExercises.get(i+1));
      }
    }
    return onLast;
  }

  public boolean loadPreviousExercise(Exercise current) {
   // showStatus("");
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
