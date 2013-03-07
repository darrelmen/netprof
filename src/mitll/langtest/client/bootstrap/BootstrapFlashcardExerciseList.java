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
  private final Column column;
  private ExercisePanelFactory factory;
  private LangTestDatabaseAsync service;
  private UserManager user;
  private FluidRow row, row2, row3, row4;

  public BootstrapFlashcardExerciseList(Container currentExerciseVPanel, LangTestDatabaseAsync service, UserManager user) {
    this.service = service;
    this.row = new FluidRow();
    this.row2 = new FluidRow();
    this.row3 = new FluidRow();
    this.row4 = new FluidRow();
    column = new Column(6);
   // column2 = new Column(6);
    row.add(column);
   // row2.add(column2);
    currentExerciseVPanel.add(row);
    currentExerciseVPanel.add(row2);
    currentExerciseVPanel.add(row3);
    currentExerciseVPanel.add(row4);

    Heading w = new Heading(6);
    row4.add(new Column(6,w));
   // column.add(w);
    w.setText("Click record to check your pronunciation.");
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
    service.getNextExercise(userID, new AsyncCallback<FlashcardResponse>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(FlashcardResponse result) {
        //System.out.println("Got next for " +result.getID());

        Panel exercisePanel = factory.getExercisePanel(result.e);
        column.clear();
        row2.clear();
        row3.clear();


        column.add(exercisePanel);


        ProgressBar correct = new ProgressBar();
        correct.setPercent(result.correct);
        //correct.setText("Correct");
        correct.setColor(ProgressBarBase.Color.SUCCESS);

        Label w1 = new Label("Correct");//w1.setText("Correct");
        row2.add(new Column(1, w1));
        row2.add(new Column(5,correct));

        ProgressBar incorrect = new ProgressBar();
        incorrect.setPercent(result.incorrect);
      //  incorrect.setText("Incorrect");
        incorrect.setColor(ProgressBarBase.Color.WARNING);

        Label w2 = new Label("Incorrect");//w2.setText("Incorrect");
        row3.add(new Column(1,w2));
        row3.add(new Column(5,incorrect));
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
