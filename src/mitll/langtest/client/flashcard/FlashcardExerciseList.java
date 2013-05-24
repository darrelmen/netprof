package mitll.langtest.client.flashcard;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlashcardExerciseList implements ListInterface {
  private ExercisePanelFactory factory;
  private LangTestDatabaseAsync service;
  private SimplePanel innerContainer;
  private UserManager user;

  public FlashcardExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service, UserFeedback feedback, UserManager user) {
    this.service = service;
    this.user = user;
    this.innerContainer = new SimplePanel();
    this.innerContainer.setWidth("100%");
    this.innerContainer.setHeight("100%");
    currentExerciseVPanel.add(innerContainer);
  }

  @Override
  public int getComplete() {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public int getPercentComplete() {
    return 0;
  }

  @Override
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    this.factory = factory;
  }

  /**
   * @param userID
   * @see mitll.langtest.client.LangTest#gotUser(long)
   */
  @Override
  public void getExercises(long userID) {
    System.out.println("Getting next for " +userID);
    service.getNextExercise(userID, new AsyncCallback<Exercise>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(Exercise result) {
        System.out.println("Got next for " +result.getID());

        Panel exercisePanel = factory.getExercisePanel(result);
        innerContainer.setWidget(exercisePanel);
      }
    });
  }

  @Override
  public void setExercise_title(String exercise_title) {}

  @Override
  public Widget getWidget() {
    return new SimplePanel();
  }

  @Override
  public boolean loadNextExercise(ExerciseShell current) {
    getExercises(user.getUser());
    return true;
  }

  @Override
  public boolean loadPreviousExercise(ExerciseShell current) {
    return false;
  }

  @Override
  public boolean onFirst(ExerciseShell current) {
    return false;
  }

  @Override
  public void clear() {
    innerContainer.clear();
  }

  @Override
  public void removeCurrentExercise() {
    innerContainer.clear();
  }

  @Override
  public void onResize() {
  }
}
