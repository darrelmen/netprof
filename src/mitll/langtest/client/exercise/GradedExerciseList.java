package mitll.langtest.client.exercise;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

/**
 * Handles left side of NetPron2 -- which exercise is the current one, highlighting, etc.
 *
 * Skips forward in the list to the next ungraded item.
 *
 * User: GO22670
 * Date: 7/9/12
 * Time: 5:59 PM
 */
public class GradedExerciseList extends PagingExerciseList {
  private final boolean englishOnly;
  private int expectedGrades = 1;

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showInOrder
   * @param englishOnly
   */
  public GradedExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            boolean showInOrder, boolean englishOnly, ExerciseController  controller) {
    super(currentExerciseVPanel, service, feedback, false, showInOrder, controller);
    this.englishOnly = englishOnly;
  }

  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    super.setFactory(factory,user,0);
    this.expectedGrades = expectedGrades;
  }

  /**
   * @see SetExercisesCallback#onSuccess(java.util.List)
   */
  @Override
  protected void loadFirstExercise() {
    getNextUngraded(true);
    selectFirst();
  }

  /**
   * @see #loadExercise(mitll.langtest.shared.ExerciseShell)
   * @param e
   */
  @Override
  protected void checkBeforeLoad(final ExerciseShell e) {
    service.checkoutExerciseID(""+user.getUser(), e.getID(), new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) { Window.alert("couldn't checkout " + e.getID());}
      public void onSuccess(Void result) {
        feedback.showStatus("");
      }
    });
  }

  @Override
  protected void onLastItem() {
    getNextUngraded(false);
  }

  /**
   * @see #loadNextExercise(mitll.langtest.shared.ExerciseShell)
   * @param current
   */
  @Override
  protected void getNextExercise(ExerciseShell current) {
    getNextUngraded(false);
  }

  private void getNextUngraded(final boolean showFirstIfNoneToGrade) {
    service.getNextUngradedExercise(""+user.getUser(), expectedGrades, englishOnly, new AsyncCallback<Exercise>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Exercise result) {
        if (result != null) {
          for (ExerciseShell e : currentExercises) {
            if (e.getID().equals(result.getID())) {
              loadExercise(e);
              break;
            }
          }
        }
        else {
          if (showFirstIfNoneToGrade) {
            System.out.println("showing first exercise...");
            ExerciseShell toLoad = currentExercises.get(0);
            loadExercise(toLoad);
          }
          else {
            showPopup("<h5>All answers graded.</h5>");
          }
        }
      }
    });
  }

  private void showPopup(String toShow) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.setPopupPosition(Window.getClientWidth()/2,Window.getClientHeight()/2);
    popupImage.show();
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }
}
