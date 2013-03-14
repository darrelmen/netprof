package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidRow;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
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
import mitll.langtest.shared.FlashcardResponse;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapFlashcardExerciseList implements ListInterface {
  private static final int SIZE = 12;
  private final Column column;
  private ExercisePanelFactory factory;
  private LangTestDatabaseAsync service;
  private UserManager user;
  private Heading correct = new Heading(3);

  public BootstrapFlashcardExerciseList(Container currentExerciseVPanel, LangTestDatabaseAsync service, UserManager user) {
    this.service = service;
    FluidRow row = new FluidRow();
    FluidRow row2 = new FluidRow();
    column = new Column(SIZE);
    row.add(column);
    currentExerciseVPanel.add(row);
    correct.addStyleName("darkerBlueColor");
    row2.add(new Column(SIZE, correct));
    currentExerciseVPanel.add(row2);

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
    //System.out.println("Getting next for " +userID);
    service.getNextExercise(userID, new AsyncCallback<FlashcardResponse>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(FlashcardResponse result) {
        Panel exercisePanel = factory.getExercisePanel(result.e);
        column.clear();

        column.add(exercisePanel);
        correct.setText("Correct " +result.correct +"/"+(result.correct+result.incorrect));
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
