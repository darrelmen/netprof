package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.user.UserManager;
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
  private static final String HELP_IMAGE = LangTest.LANGTEST_IMAGES + "/help-4.png";
  boolean isTimedGame;

  /**
   * @see mitll.langtest.client.LangTest#doFlashcard()
   * @param currentExerciseVPanel
   * @param service
   * @param user
   * @param controller
   */
  public BootstrapFlashcardExerciseList(Container currentExerciseVPanel, LangTestDatabaseAsync service,
                                        UserManager user, final ExerciseController controller,boolean isTimedGame) {
    this.service = service;
    FluidRow row = new FluidRow();
    currentExerciseVPanel.add(row);
    FluidRow row2 = new FluidRow();
    currentExerciseVPanel.add(row2);

    column = new Column(SIZE);
    row.add(column);

    correct.addStyleName("darkerBlueColor");
    row2.add(new Column(11, correct));

    Image image = new Image(HELP_IMAGE);
    image.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.showFlashHelp();
      }
    });
    row2.add(new Column(1,image));

    this.user = user;
    this.isTimedGame = isTimedGame;
  }

  @Override
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    this.factory = factory;
  }

  long start = -1;
  Timer timer;
  boolean expired = false;
  /**
   * @param userID
   * @see mitll.langtest.client.LangTest#gotUser(long)
   */
  @Override
  public void getExercises(long userID) {
    if (start == -1) {
      start = System.currentTimeMillis();
      timer = new Timer() {
        @Override
        public void run() {
          Window.alert("Time's up!");
          expired = true;
        }
      };
      timer.schedule(60000);
    }

    service.getNextExercise(userID, new AsyncCallback<FlashcardResponse>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(FlashcardResponse result) {
        if (result.finished) {
          Window.alert("Flashcards Complete!");
        } else {
          Panel exercisePanel = factory.getExercisePanel(result.e);
          column.clear();

          column.add(exercisePanel);
          correct.setText("Correct " + result.correct + "/" + (result.correct + result.incorrect));
        }
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
