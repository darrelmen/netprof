package mitll.langtest.client.grading;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
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
public class GradedExerciseList<T extends ExerciseShell> extends PagingExerciseList<T> {
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
                            boolean showInOrder, boolean englishOnly, ExerciseController controller, String instance) {
    super(currentExerciseVPanel, service, feedback, false, showInOrder, controller, true, instance);
    this.englishOnly = englishOnly;
  }

  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    super.setFactory(factory,user,0);
    this.expectedGrades = expectedGrades;
  }

  /**
   * @see SetExercisesCallback#onSuccess
   */
  @Override
  protected void loadFirstExercise() {
    getNextUngraded(true);
    pagingContainer.selectFirst();
  }
  /**
   * @see mitll.langtest.client.list.ListInterface#loadExercise(String)
   * @param itemID
   */
  @Override
  protected void askServerForExercise(final String itemID) {
    service.checkoutExerciseID(""+user.getUser(), itemID, new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) { Window.alert("couldn't checkout " + itemID);}
      public void onSuccess(Void result) {
        feedback.showStatus("");
        GradedExerciseList.super.askServerForExercise(itemID);
      }
    });
  }

  @Override
  protected void onLastItem() {
    getNextUngraded(false);
  }

  /**
   * @see mitll.langtest.client.list.ListInterface#loadNextExercise(mitll.langtest.shared.ExerciseShell)
   * @param current
   */
  @Override
  protected void getNextExercise(ExerciseShell current) {
    getNextUngraded(false);
  }

  //protected void loadExercises(String selectionState, String prefix) {}

  private void getNextUngraded(final boolean showFirstIfNoneToGrade) {
    final PopupPanel popup = getPopup2("Please wait...");
    final Timer t = new Timer() {
      @Override
      public void run() { popup.show(); }
    };
    t.schedule(2000);

    service.getNextUngradedExercise(""+user.getUser(), expectedGrades, englishOnly, new AsyncCallback<Exercise>() {
      public void onFailure(Throwable caught) {
        t.cancel();
        popup.hide();
      }
      public void onSuccess(Exercise result) {
        t.cancel();
        popup.hide();

        if (result != null) {
          if (!loadByID(result.getID())) {
            System.out.println("showing first exercise...");
            T toLoad = getFirst();
            loadExercise(toLoad.getID());
          }
        }
        else {
          if (showFirstIfNoneToGrade) {
            System.out.println("showing first exercise...");
            T toLoad = getFirst();
            loadExercise(toLoad.getID());
          }
          else {
            showPopup("<h5>All answers graded.</h5>");
          }
        }
      }
    });
  }

  private void showPopup(String toShow) {
    final PopupPanel popupImage = getPopup(toShow);
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }

  private PopupPanel getPopup(String toShow) {
    final PopupPanel popupImage = getPopup2(toShow);
    popupImage.show();
    return popupImage;
  }

  private PopupPanel getPopup2(String toShow) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.setPopupPosition(Window.getClientWidth()/2,Window.getClientHeight()/2);
    return popupImage;
  }
}
