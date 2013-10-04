package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExerciseList;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.exercise.SelectionState;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.flashcard.FlashcardResponse;
import mitll.langtest.shared.flashcard.Leaderboard;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapFlashcardExerciseList implements ListInterface {
  private static final int SIZE = 12;
  public static final float HALF = (1f / 4f);
  private final Column exercisePanelColumn;
  private final LeaderboardPlot leaderboardPlot = new LeaderboardPlot();
  private ExercisePanelFactory factory;
  private LangTestDatabaseAsync service;

  private UserManager user;
  private Heading correct = new Heading(4);
  private ProgressBar bar = new ProgressBar();

  private Timer timer;

  @Override
  public Panel makeExercisePanel(Exercise result) {
    return null;
  }

  @Override
  public void loadExercise(ExerciseShell exerciseShell) {
  }

  @Override
  public void startOver() {
  }

  @Override
  public boolean loadNextExercise(String id) {
    return false;
  }

  private boolean expired = false;
  private boolean timerRunning = false;

  private final int gameTimeSeconds;
  private Panel bottomRow = new FlowPanel();
  private boolean isTimedGame = false;
  private Map<String,Collection<String>> currentSelection;

  /**
   *
   *
   * @param currentExerciseVPanel
   * @param service
   * @param user
   * @param gameTimeSeconds
   * @see mitll.langtest.client.LangTest#doFlashcard()
   */
  public BootstrapFlashcardExerciseList(Container currentExerciseVPanel, LangTestDatabaseAsync service,
                                        UserManager user,
                                        boolean isTimedGame, int gameTimeSeconds) {
    this.service = service;
    this.gameTimeSeconds = gameTimeSeconds;

    FluidRow row = new FluidRow();
    currentExerciseVPanel.add(row);
    exercisePanelColumn = new Column(SIZE);
    row.add(exercisePanelColumn);

    Panel correctAndImageRow = new FlowPanel();
    correctAndImageRow.addStyleName("headerBackground");
    correctAndImageRow.addStyleName("blockStyle");
    Panel pair = new HorizontalPanel();
    correctAndImageRow.add(pair);
    this.isTimedGame = isTimedGame;
    if (isTimedGame) {
      bottomRow.add(bar);
      bar.addStyleName("cardBorder");
      DOM.setStyleAttribute(bar.getElement(), "marginBottom", "0px");
      DOM.setStyleAttribute(bar.getElement(), "borderRadius", "0px");
      DOM.setStyleAttribute(bar.getElement(), "backgroundColor", "#3195b9");
      bottomRow.setVisible(false);
      currentExerciseVPanel.add(bottomRow);
    }
    currentExerciseVPanel.add(correctAndImageRow);

    Image checkmark = new Image(LangTest.LANGTEST_IMAGES + "checkboxCorrectRatio.png");
    checkmark.addStyleName("checkboxPadding");
    pair.add(checkmark);
    correct.addStyleName("correctRatio");
    pair.add(correct);

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
  public void getExercises(final long userID) {
    System.out.println("-- getExercises for " + userID + " expired " + expired + " time running " + timerRunning);

    if (!expired) {
      if (!timerRunning) {
        if (isTimedGame) {
          startTimer(userID);
        }
      }

      SelectionState selectionState = new SelectionState();
      if (selectionState.isEmpty()) {
        service.getNextExercise(userID, new FlashcardResponseAsyncCallback());
      }
      else {
        System.out.println("Getting next for " +userID + " selection state : " +selectionState);
        this.currentSelection = selectionState.getTypeToSection();
        service.getNextExercise(userID, currentSelection, new FlashcardResponseAsyncCallback());
      }
    }
  }

  private void startTimer(final long userID) {
    //System.out.println("startTimer for " + userID);

    bar.setPercent(100);
    bar.setColor(ProgressBarBase.Color.DEFAULT);
    bar.setText(gameTimeSeconds + " seconds");
    int integralStep = gameTimeSeconds < 100 ? (int)Math.ceil(100d/(double)gameTimeSeconds) : 1;
    int numSteps = 100/integralStep;
    int intervalMillis = Math.round(((float)(gameTimeSeconds*1000))/(float)numSteps);

    timer = new MyTimer(numSteps, integralStep,userID);
//    System.out.println("starting " + timer + " for " + userID +" seconds " + gameTimeSeconds +" interval " + intervalMillis + " num steps " +numSteps);
    timer.scheduleRepeating(intervalMillis);
  }

  private void timesUp(final long userID) {
     service.postTimesUp(userID, gameTimeSeconds*1000, currentSelection, new AsyncCallback<Leaderboard>() {
       @Override
       public void onFailure(Throwable caught) {
         Window.alert("Couldn't contact server.");
       }

       @Override
       public void onSuccess(Leaderboard result) {
       //  List<ScoreInfo> scores = result.getScores(currentSelection);
         leaderboardPlot.showLeaderboardPlot(result, userID, gameTimeSeconds, currentSelection,  "Would you like to try again?",
           new ClickHandler() {
             @Override
             public void onClick(ClickEvent event) {
               goAgain(userID);
             }
           },
           new ClickHandler() {
             @Override
             public void onClick(ClickEvent event) {
               stopForNow(userID);
             }
           },0
         );
       }
     });
  }
/*
  private void showLeaderboardPlot(List<ScoreInfo> scores, final long userID) {

    *//*    yesButton.setHeight("30px");
    yesButton.setWidth("50px");*//*

    leaderboardPlot.showLeaderboardPlot(scores, userID);
  }*/

/*
  private void getOutOfTimeDialog(final long userID) {
    List<String> msgs = new ArrayList<String>();
    msgs.add("You got "+lastCorrect +" correct!");

*/
/*    if (prevCorrect != -1 && lastCorrect > prevCorrect) {
      msgs.add("Even better than last time!");
      msgs.add("Before you had " + prevCorrect + " correct.");
    }*//*

    msgs.add("Would you like to try again?");

    String title = lastCorrect == 0 ? "Try again?" : lastCorrect < 5 ? "Good job" : "Congratulations!";

    new DialogHelper(true).showErrorMessage(title, msgs, "Yes", new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
// better than last time? worse?
        // ask user to go again, reset counter on server
        goAgain(userID);
      }

      @Override
      public void gotNo() {
        stopForNow(userID);
      }
    }
    );
  }
*/

  private void stopForNow(long userID) {
    service.clearUserState(userID, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("clearUserState Couldn't contact server.");
      }

      @Override
      public void onSuccess(Void result) {}
    });
  }

  private void goAgain(final long userID) {
    service.resetUserState(userID, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("resetUserState Couldn't contact server.");
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
  public void setExercise_title(String exercise_title) {}

  @Override
  public Widget getWidget() {
    return new SimplePanel();
  }

  public Widget getExerciseListOnLeftSide(PropertyHandler props) {
    FlowPanel leftColumn = new FlowPanel();
    leftColumn.addStyleName("floatLeft");
    DOM.setStyleAttribute(leftColumn.getElement(), "paddingRight", "10px");

    if (!props.isFlashcardTeacherView() && !props.isMinimalUI()) {
      Heading items = new Heading(4, ExerciseList.ITEMS);
      items.addStyleName("center");
      leftColumn.add(items);
    }
    leftColumn.add(getWidget());
    return leftColumn;
  }

  /**
   * @param current
   * @return
   * @see mitll.langtest.client.LangTest#loadNextExercise
   */
  @Override
  public boolean loadNextExercise(ExerciseShell current) {
    System.out.println("-------------- loadNextExercise -------- " + current);
    getExercises(user.getUser());
    return true;
  }

  @Override
  public void reloadExercises() {
    System.out.println("-------------- reloadExercises -------- " + timerRunning);

    if (!timerRunning) {
      expired = false;
      getExercises(user.getUser());
    }
  }

  @Override
  public int getPercentComplete() {
    return 0;
  }

  @Override
  public int getComplete() {
    return 0;
  }

  @Override
  public boolean loadPreviousExercise(ExerciseShell current) {
    return false;
  }

  @Override
  public String getCurrentExerciseID() {
    return "";
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
  public void onResize() {}

  private class FlashcardResponseAsyncCallback implements AsyncCallback<FlashcardResponse> {
    @Override
    public void onFailure(Throwable caught) {
      Window.alert("FlashcardResponseAsyncCallback Couldn't contact server.");
    }

    @Override
    public void onSuccess(FlashcardResponse result) {
      if (result.finished) {
        Window.alert("Flashcards Complete!");
      } else {
        Panel exercisePanel = factory.getExercisePanel(result.getNextExercise());
        exercisePanelColumn.clear();

        exercisePanelColumn.add(exercisePanel);
        bottomRow.setVisible(true);
        correct.setText(result.correct + "/" + (result.correct + result.incorrect));
        grabFocus((BootstrapExercisePanel) exercisePanel);   // TODO : not sure this works really
      }
    }
  }
/*
  @Override
  public void addAdHocExercise(String label) {}*/
  @Override
  public void setSelectionState(Map<String, Collection<String>> selectionState) {
  }

  @Override
  public void hideExerciseList() {
  }

  @Override
  public void askForRandomExercise(AsyncCallback<Exercise> callback) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Panel getCreatedPanel() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void rememberAndLoadFirst(List<? extends ExerciseShell> exercises){}

  protected void grabFocus(final BootstrapExercisePanel panel) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
      public void execute () {
        panel.grabFocus();
      }
    });
  }

  private class MyTimer extends Timer {
    private final int numSteps;
    private int currentSteps;
    private final long userID;
    private int stepSize;

    public MyTimer(int numSteps, int stepSize, long userID) {
      this.numSteps = numSteps;
      this.userID = userID;
      currentSteps = numSteps;
      this.stepSize = stepSize;
    }

    @Override
    public void run() {
      if (expired) {
        System.out.println(this + " - expired????");
        timer.cancel();
      } else {
        timerRunning = true;
        if (currentSteps == 0) {
          expired = true;
          bar.setColor(ProgressBarBase.Color.SUCCESS);
          bar.setPercent(100);
          bar.setText("Time's up!");

          timer.cancel();
          timerRunning = false;
          //getOutOfTimeDialog(userID);
          timesUp(userID);
        } else {
          currentSteps--;
          boolean lastTenSeconds = currentSteps == 0 || numSteps / currentSteps > 5;

          if (lastTenSeconds) {
            bar.setColor(ProgressBarBase.Color.WARNING);
          }
          int percent = currentSteps * stepSize;
          bar.setText("");
          bar.setPercent(percent);
        }
      }
    }
  }
}
