package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.DialogHelper;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.FlashcardResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapFlashcardExerciseList implements ListInterface {
  private static final int SIZE = 12;
  private final Column exercisePanelColumn;
  private ExercisePanelFactory factory;
  private LangTestDatabaseAsync service;
  private UserManager user;
  private Heading correct = new Heading(3);
  //private Heading timeFeedback = new Heading(3);
  private ProgressBar bar = new ProgressBar();
 // private AudioHelper audioHelper = new AudioHelper();

  private long start = -1;
  private Timer timer;
  private boolean expired = false;
  private boolean timerRunning = false;
  long lastTextMessage = -1;

  private int lastCorrect = 0;
  private int prevCorrect = 0;

  private static final String HELP_IMAGE = LangTest.LANGTEST_IMAGES + "/help-4.png";
  private static final int total = 20;
  private FluidRow bottomRow = new FluidRow();

  /**
   * @param currentExerciseVPanel
   * @param service
   * @param user
   * @param controller
   * @see mitll.langtest.client.LangTest#doFlashcard()
   */
  public BootstrapFlashcardExerciseList(Container currentExerciseVPanel, LangTestDatabaseAsync service,
                                        UserManager user, final ExerciseController controller, boolean isTimedGame) {
    this.service = service;

    FluidRow row = new FluidRow();
    currentExerciseVPanel.add(row);
    exercisePanelColumn = new Column(SIZE);
    row.add(exercisePanelColumn);

    FluidRow correctAndImageRow = new FluidRow();
    currentExerciseVPanel.add(correctAndImageRow);

   // FluidRow correctAndImageRow = new FluidRow();

    correct.addStyleName("darkerBlueColor");

    if (isTimedGame) {
      //FluidRow composite = new FluidRow();
    //  bottomRow.add(new Column(3, correct));
      //Heading widgets = new Heading(3);
     // widgets.setText("Time Left :");
     // bottomRow.add(new Column(2, widgets));
      //Heading heading = new Heading(3);
     // heading.add(bar);
      Column w = new Column(6, 3, bar);
     // bar.addStyleName("magicCenter");
      bottomRow.add(w);
      bottomRow.setVisible(false);
      currentExerciseVPanel.add(bottomRow);
      //   composite.add(new Column(3, timeFeedback));
   //   correctAndImageRow.add(new Column(11, bottomRow));
    }/* else {
      correctAndImageRow.add(new Column(11, correct));
    }*/
    correctAndImageRow.add(new Column(11, correct));

    // add help image on right side
    Image image = new Image(HELP_IMAGE);
    image.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.showFlashHelp();
      }
    });
    correctAndImageRow.add(new Column(1, image));

    this.user = user;
  }

  @Override
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    this.factory = factory;
  }

  long lastMessage = -1;
  /**
   * @param userID
   * @see mitll.langtest.client.LangTest#gotUser(long)
   */
  @Override
  public void getExercises(final long userID) {
    System.out.println("\n\n --------------- getExercises for " + userID + " expired " +expired);

    if (!expired) {
      if (!timerRunning) {
        startTimer(userID);
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
            exercisePanelColumn.clear();

            exercisePanelColumn.add(exercisePanel);
            bottomRow.setVisible(true);
            correct.setText("Correct " + result.correct + "/" + (result.correct + result.incorrect));
            lastCorrect = result.correct;
            List<Integer> correctHistory = result.getCorrectHistory();
            prevCorrect = correctHistory.isEmpty() ? -1 : correctHistory.get(correctHistory.size() - 1);
          }
        }
      });
    }
  }

  private void startTimer(final long userID) {
    start = System.currentTimeMillis();
    lastMessage = start;
    lastTextMessage = start;
    bar.setPercent(100);
    bar.setColor(ProgressBarBase.Color.DEFAULT);
    bar.setText(total + " seconds");

    timer = new Timer() {
      @Override
      public void run() {
        if (expired) {
          System.out.println(this + " - expired????");
          timer.cancel();
        } else {
          timerRunning = true;
          long now = System.currentTimeMillis();
          long diff = now-start;
          int remaining = total - (int)(diff/1000);

          if (remaining < 0) {
            expired = true;
            bar.setColor(ProgressBarBase.Color.SUCCESS);
            bar.setPercent(100);
            bar.setText("Time's up!");

            timer.cancel();
            timerRunning = false;
            //audioHelper.playCorrect(3);

            getOutOfTimeDialog(userID);

          } else {
            boolean lastTenSeconds = remaining < 10;
            if (now - lastMessage > 2000 || lastTenSeconds) {
            /*  if (now - lastTextMessage > 5000) {
                lastTextMessage = now;
                bar.setText(remaining + " seconds");
              }*/
              if (lastTenSeconds) {
                bar.setColor(ProgressBarBase.Color.WARNING);
              }
              float percent = 100f * ((float)remaining/(float)total);
              bar.setText("");
              bar.setPercent((int)percent);
              lastMessage = now;
            }
          }
        }
      }
    };
    System.out.println("starting " + timer + " for " + userID);
    timer.scheduleRepeating(1000);
  }

  private void getOutOfTimeDialog(final long userID) {
    List<String> msgs = new ArrayList<String>();
    msgs.add("You got "+lastCorrect +" correct!");

    if (prevCorrect != -1 && lastCorrect > prevCorrect) {
      msgs.add("Even better than last time!");
      msgs.add("Before you had " + prevCorrect + " correct.");
    }
    msgs.add("Would you like to try again?");

    String title = lastCorrect == 0 ? "Try again?" : lastCorrect < 5 ? "Good job" : "Congratulations!";

    new DialogHelper(true).showErrorMessage(title, msgs, "Yes", new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
// better than last time? worse?
        // ask user to go again, reset counter on server
        service.resetUserState(userID, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Couldn't contact server.");
          }

          @Override
          public void onSuccess(Void result) {
            System.out.println("Going again!");
            expired = false;
            getExercises(userID);
          }
        });
      }

      @Override
      public void gotNo() {
        service.clearUserState(userID, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Couldn't contact server.");
          }

          @Override
          public void onSuccess(Void result) {
          }
        });
      }
    }
    );
  }

  @Override
  public void setExercise_title(String exercise_title) {}

  @Override
  public Widget getWidget() {
    return new SimplePanel();
  }

  /**
   * @param current
   * @return
   * @see mitll.langtest.client.LangTest#loadNextExercise
   */
  @Override
  public boolean loadNextExercise(ExerciseShell current) {
    System.out.println("loadNextExercise -------- " + current);
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
    exercisePanelColumn.clear();
  }

  @Override
  public void removeCurrentExercise() {
    exercisePanelColumn.clear();
  }

  @Override
  public void onResize() {
  }
}
