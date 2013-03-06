package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidRow;

import com.github.gwtbootstrap.client.ui.Heading;
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
public class BootstrapFlashcardExerciseList implements ListInterface {
  private final Column column;
  private ExercisePanelFactory factory;
  private LangTestDatabaseAsync service;
  private UserManager user;
  private FluidRow row;

  public BootstrapFlashcardExerciseList(Container currentExerciseVPanel, LangTestDatabaseAsync service, /*UserFeedback feedback,*/ UserManager user) {
    this.service = service;
    this.row = new FluidRow();
    column = new Column(6);
    row.add(column);
    currentExerciseVPanel.add(row);
    this.user = user;
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
        column.clear();
        column.add(exercisePanel);
      }
    });
  }

  @Override
  public void setExercise_title(String exercise_title) {}

  @Override
  public Widget getWidget() {
    return new SimplePanel();
  }

  /**
   * @see mitll.langtest.client.LangTest#loadNextExercise
   * @param current
   * @return
   */
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
    column.clear();
  }

  @Override
  public void removeCurrentExercise() {
    column.clear();
  }

  @Override
  public void onResize() {
  }
}
