package mitll.langtest.client.exercise;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

/**
 * Handles left side of NetPron2 -- which exercise is the current one, highlighting, etc.
 *
 * User: GO22670
 * Date: 7/9/12
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class GradedExerciseList extends ExerciseList {
  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param factory
   * @param arabicDataCollect
   */
  public GradedExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback,
                            ExercisePanelFactory factory, boolean arabicDataCollect) {
    super(currentExerciseVPanel, service, feedback, factory, false, arabicDataCollect, false, false);
  }

  /**
   * @see mitll.langtest.client.LangTest#setGrading
   * @param factory
   * @param user
   * @param expectedGrades
   */
  @Override
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    super.setFactory(factory,user,expectedGrades);
    getExercisesInOrder();
  }

  @Override
  protected void loadFirstExercise() {
    getNextUngraded();
  }

  @Override
  protected void checkBeforeLoad(ExerciseShell e) {
    checkoutExercise(e);
  }

  @Override
  protected void getNextExercise(ExerciseShell current) {
    getNextUngraded();
  }

  private void getNextUngraded() {
    service.getNextUngradedExercise(user.getGrader(), expectedGrades, arabicDataCollect, new AsyncCallback<Exercise>() {
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
      }
    });
  }

  private void checkoutExercise(ExerciseShell result) {
     service.checkoutExerciseID(user.getGrader(), result.getID(), new AsyncCallback<Void>() {
       public void onFailure(Throwable caught) {}
       public void onSuccess(Void result) {}
     });
  }
}
