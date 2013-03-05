package mitll.langtest.client.exercise;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
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

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showInOrder
   * @param englishOnly
   */
  public GradedExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            boolean showInOrder, boolean englishOnly) {
    super(currentExerciseVPanel, service, feedback, false, false, showInOrder);
    this.englishOnly = englishOnly;
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

  /**
   * @see #loadNextExercise(mitll.langtest.shared.ExerciseShell)
   * @param current
   */
  @Override
  protected void getNextExercise(ExerciseShell current) {
    getNextUngraded(false);
  }

  private void getNextUngraded(final boolean showFirstIfNoneToGrade) {
    //System.out.println("show first if none to grade " + showFirstIfNoneToGrade + " num " + expectedGrades);
    service.getNextUngradedExercise(""+user.getUser(), expectedGrades, arabicDataCollect, englishOnly, new AsyncCallback<Exercise>() {
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
            Window.alert("All answers graded.");
          }
        }
      }
    });
  }
}
