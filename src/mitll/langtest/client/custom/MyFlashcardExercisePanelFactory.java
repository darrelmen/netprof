package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.flashcard.LeaderboardPlot;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.monitoring.Session;
import org.moxieapps.gwt.highcharts.client.Chart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 2/10/14.
 * TODO : store state -- how many items have been done, where are we in the list
 * TODO : so we can skip around in the list...? if get to end - get score of all the ones we've answered.
 * TODO : concept of rounds explicit?
 * TODO : review table...?
 */
class MyFlashcardExercisePanelFactory<T extends ExerciseShell> extends FlashcardExercisePanelFactory {
  private static final String REMAINING = "Remaining";
  private static final String INCORRECT = "Incorrect";
  private static final String CORRECT = "Correct";
  private static final String REPEAT_THIS_SET = "Start Over";
  private Exercise currentExercise;
  private final ControlState controlState;
  private List<T> allExercises;
  private final long userListID;
  private T lastExercise;
  Map<String,Boolean> exToCorrect = new HashMap<String, Boolean>();
  Map<String,Double>   exToScore = new HashMap<String, Double>();

  /**
   * @see NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList, String, long)
   * @param service
   * @param feedback
   * @param controller
   * @param exerciseList
   */
  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface<T> exerciseList, long userListID) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();
    this.userListID = userListID;
   // System.out.println("========> MyFlashcardExercisePanelFactory made!\n\n\n");

    exerciseList.addListChangedListener(new ListChangeListener<T>() {
      @Override
      public void listChanged(List<T> items) {
        allExercises = items;
        if (!allExercises.isEmpty()) lastExercise = allExercises.get(allExercises.size() - 1);
        System.out.println("got new set of items from list." + items.size());
        reset();
      }
    });
  }

  @Override
  public Panel getExercisePanel(Exercise e) {
    currentExercise = e;
   // System.out.println("========> getExercisePanel = called!\n\n\n");

    return new StatsPracticePanel(e);
  }

  private void reset() {
    exToCorrect.clear();
    exToScore.clear();
  }

  private class StatsPracticePanel extends BootstrapExercisePanel {
    private Panel container;
    public StatsPracticePanel(Exercise e) {
      super(e, MyFlashcardExercisePanelFactory.this.service,
        MyFlashcardExercisePanelFactory.this.controller, 40, false, MyFlashcardExercisePanelFactory.this.controlState);
    }

    @Override
    protected void loadNext() {
      if (exerciseList.onLast()) {
        onSetComplete();
      } else {
        exerciseList.loadNextExercise(currentExercise.getID());
      }
    }

    /**
     * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#receivedAudioAnswer(mitll.langtest.shared.AudioAnswer, mitll.langtest.client.exercise.ExerciseQuestionState, com.google.gwt.user.client.ui.Panel)
     * @param result
     */
    public void receivedAudioAnswer(final AudioAnswer result) {
      System.out.println("receivedAudioAnswer: result " + result);

      exToScore.put(currentExercise.getID(),result.getScore());
      if (result.validity != AudioAnswer.Validity.OK) {
        super.receivedAudioAnswer(result);
        return;
      }
      exToCorrect.put(currentExercise.getID(), result.isCorrect());
      setStateFeedback();

      super.receivedAudioAnswer(result);
    }

    public void onSetComplete() {
      navigationHelper.setVisible(false);
      final int user = controller.getUser();
      service.getUserHistoryForList(user,userListID,lastExercise.getID(),new AsyncCallback<List<Session>>() {
        @Override
        public void onFailure(Throwable caught) {
           System.err.println("getUserHistoryForList failure : caught " + caught);
        }

        @Override
        public void onSuccess(List<Session> result) {
          showFeedbackCharts(result, user);
        }
      });
      // TODO : maybe add table showing results per word
    }

    private void showFeedbackCharts(List<Session> result, int user) {
      float size = (float) allExercises.size();
      //System.out.println("onSetComplete.onSuccess : result " + result.size() + " " +size);

      setMainContentVisible(false);
      int totalCorrect = getCorrect();
      int totalIncorrect = getIncorrect();
      double totalScore = getScore();
      int all = totalCorrect + totalIncorrect;
      String correct = totalCorrect +" Correct (" + toPercent(totalCorrect, all) + ")";
      String pronunciation = "Pronunciation " + toPercent(totalScore, all);

      Chart chart  = new LeaderboardPlot().getChart(result, user, -1, correct, "# correct", true, size);
      Chart chart2 = new LeaderboardPlot().getChart(result, user, -1, pronunciation, "score %", false, -1);
      container = new HorizontalPanel();
      container.add(chart);
      chart.addStyleName("chartDim");
      chart2.addStyleName("chartDim");
      container.add(chart2);
      belowContentDiv.add(container);
      belowContentDiv.add(getRepeatButton());
      exerciseList.hide();
    }

    private int getCorrect() {
      int count = 0;
      for (Boolean val : exToCorrect.values()) {
        if (val) count++;
      }
      return count;
    }
    private int getIncorrect() {
      int count = 0;
      for (Boolean val : exToCorrect.values()) {
        if (!val) count++;
      }
      return count;
    }

    private double getScore() {
      double count = 0;
      for (Double val : exToScore.values()) {
        count += val;
      }
      return count;
    }

    private String toPercent(int numer, int denom) {
      return ((int) ((((float)numer) * 100f) / denom)) + "%";
    }

    private String toPercent(double numer, int denom) {
      return ((int) ((numer * 100f) / denom)) + "%";
    }

    private Button getRepeatButton() {
      Button w1 = new Button(REPEAT_THIS_SET);
      w1.setType(ButtonType.PRIMARY);
      w1.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          setMainContentVisible(true);
          belowContentDiv.remove(container);
          exerciseList.show();

          reset();

          navigationHelper.setVisible(true);
          exerciseList.loadExercise(allExercises.iterator().next().getID());
        }
      });
      return w1;
    }

    protected void nextAfterDelay(boolean correct, String feedback) {
      if (exerciseList.onLast()) {
        onSetComplete();
      }
      else  {
        super.nextAfterDelay(correct, feedback);

      }
    }

    private NavigationHelper<Exercise> navigationHelper;
    private Panel belowContentDiv;

    /**
     * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#BootstrapExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, boolean, mitll.langtest.client.flashcard.ControlState)
     * @param toAddTo
     */
    @Override
    protected void addWidgetsBelow(Panel toAddTo) {
      navigationHelper = new NavigationHelper<Exercise>(currentExercise, controller, null, exerciseList, true, false) {
        @Override
        protected void clickNext(ExerciseController controller, Exercise exercise) {
          cancelTimer();
          loadNext();
        }

        @Override
        protected void clickPrev(Exercise e) {
          cancelTimer();
          super.clickPrev(e);
        }
      };
      toAddTo.add(navigationHelper);
      belowContentDiv = toAddTo;
    }

    @Override
    protected Widget getHelpRow(ExerciseController controller) { return null;  }

    private Label remain, incorrectBox, correctBox;

    protected Panel getLeftState() {
      Grid g = new Grid(3, 2);
      ControlGroup remaining = new ControlGroup(REMAINING);
      remaining.addStyleName("topFiveMargin");
      remain = new Label();
      remain.setType(LabelType.INFO);
      g.setWidget(0, 0, remaining);
      g.setWidget(0, 1, remain);

      ControlGroup incorrect = new ControlGroup(INCORRECT);
      incorrect.addStyleName("topFiveMargin");

      incorrectBox = new Label();
      incorrectBox.setType(LabelType.WARNING);

      g.setWidget(1, 0, incorrect);
      g.setWidget(1, 1, incorrectBox);

      ControlGroup correct = new ControlGroup(CORRECT);
      correct.addStyleName("topFiveMargin");

      correctBox = new Label();
      correctBox.setType(LabelType.SUCCESS);

      g.setWidget(2, 0, correct);
      g.setWidget(2, 1, correctBox);

      setStateFeedback();
      g.addStyleName("rightTenMargin");
      return g;
    }

    private void setStateFeedback() {
      int totalCorrect = getCorrect();
      int totalIncorrect = getIncorrect();
      int remaining = allExercises.size() - totalCorrect - totalIncorrect;
      remain.setText(remaining + "");
      incorrectBox.setText(totalIncorrect + "");
      correctBox.setText(totalCorrect + "");
    }
  }
}
