package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.List;

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
  //private List<List<T>> sets = new ArrayList<List<T>>();
  //private int setIndex = 0;
  private  float totalScore;
  private int totalCorrect;
  private int totalIncorrect;
  private float totalDone;
  //private List<T> currentSet = new ArrayList<T>();
 // private int size = 10;
 private List<T> allExercises;

  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface<T> exerciseList) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();
    System.out.println("========> MyFlashcardExercisePanelFactory made!\n\n\n");

    exerciseList.addListChangedListener(new ListChangeListener<T>() {
      @Override
      public void listChanged(List<T> items) {
     allExercises = items;
      //  partition(items);
       // currentSet.addAll(items);
        System.out.println("got new set of items from list." + items.size());
      }
    });
  }

/*  private void partition(List<T> items) {
    sets = Lists.partition(items, size);
    currentSet = new ArrayList<T>(sets.get(setIndex = 0));
  }*/

  @Override
  public Panel getExercisePanel(Exercise e) {
    currentExercise = e;
   // findAndRemoveCurrent(e);

    System.out.println("========> getExercisePanel = called!\n\n\n");

    return new StatsPracticePanel(e);
  }

/*  private void findAndRemoveCurrent(Exercise e) {
    T current = findCurrent(e);

    if (current != null) {
      currentSet.remove(current);
    } else {
      System.err.println("========> couldn't find " + e.getID() + " in currentSet " + currentSet.size() + " : " + currentSet);
    }
  }*/

/*  private T findCurrent(Exercise e) {
    T current = null;
    for (T t : currentSet) {
      if (t.getID().equals(e.getID())) {
        current = t;
        break;
      }
    }
    return current;
  }*/

  private void reset() {
    totalDone = 0;
    totalCorrect = 0;
    totalScore = 0;
    totalIncorrect = 0;
  }

  private class StatsPracticePanel extends BootstrapExercisePanel {
    public StatsPracticePanel(Exercise e) {
      super(e, MyFlashcardExercisePanelFactory.this.service, MyFlashcardExercisePanelFactory.this.controller, 40, false, MyFlashcardExercisePanelFactory.this.controlState);
    }

    @Override
    protected void loadNext() {
    //  if (!currentSet.isEmpty()) {
      if (!exerciseList.onLast()) {
        exerciseList.loadNextExercise(currentExercise.getID());
      }
      else {
        System.err.println("loadNext : on last so  not doing anything!");
      }
    }

    public void receivedAudioAnswer(final AudioAnswer result) {
      if (result.validity != AudioAnswer.Validity.OK) {
        super.receivedAudioAnswer(result);
        return;
      }
      totalDone++;
      if (result.isCorrect()) totalScore += result.getScore();
      if (result.isCorrect()) {
        totalCorrect++;
      }
      else {
        totalIncorrect++;
      }
      setStateFeedback(false);

      super.receivedAudioAnswer(result);

      //System.out.println("receivedAudioAnswer : currentSet " + currentSet.size() + " : " + currentSet + " total done " + totalDone);

      // if (currentSet.isEmpty()) {
      if (exerciseList.onLast()) {
        onSetComplete();
      }
    }

    public void onSetComplete() {
      navigationHelper.setVisible(false);
      belowContentDiv.add(new Heading(2, totalCorrect + " Correct - Average Score " + ((int) ((totalScore * 100f) / totalDone)) + "%"));
      Panel w = new HorizontalPanel();
      w.add(getRepeatButton());

//        Button w2 = getNextSetButton();
//      w.add(w2);
      belowContentDiv.add(w);
      // TODO : maybe add table showing results per word
      // TODO : do we do aggregate scores
    }

 /*     private Button getNextSetButton() {
        Button w2 = new Button("Next set");
        w2.addStyleName("leftFiveMargin");
        w2.setType(ButtonType.SUCCESS);

        w2.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            try {
              currentSet = new ArrayList<T>(sets.get(++setIndex));

             // System.out.println("====> next set : currentSet " + currentSet + " set index " + setIndex);
              navigationHelper.setVisible(true);
              reset();
            } catch (Exception e1) {
              currentSet = new ArrayList<T>(sets.get(setIndex = 0));
            }
            exerciseList.loadExercise(currentSet.iterator().next().getID());
          }
        });
        return w2;
      }*/

    private Button getRepeatButton() {
      Button w1 = new Button(REPEAT_THIS_SET);
      w1.setType(ButtonType.PRIMARY);
      w1.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          //List<T> ts = sets.get(setIndex);
          //currentSet = new ArrayList<T>(ts);

          reset();

          // System.out.println("====> repeat set : currentSet " + currentSet + " set index " + setIndex);
          navigationHelper.setVisible(true);

         // exerciseList.loadExercise(currentSet.iterator().next().getID());
          exerciseList.loadExercise(allExercises.iterator().next().getID());
        }
      });
      return w1;
    }

    protected void nextAfterDelay(boolean correct, String feedback) {
      if (!exerciseList.onLast()) {
        super.nextAfterDelay(correct, feedback);
      }
    }

/*    protected void setSetSize(int i) {
      if (i != size) {
        reset();

        size = i;
        partition(allExercises);
        exerciseList.loadExercise(currentSet.iterator().next().getID());
      }
    }*/

    private NavigationHelper<Exercise> navigationHelper;

    private Panel belowContentDiv;

    @Override
    protected void addWidgetsBelow(Panel toAddTo) {
      navigationHelper = new NavigationHelper<Exercise>(currentExercise, controller, null, exerciseList, true, false) {
        @Override
        protected void clickNext(ExerciseController controller, Exercise exercise) {
          loadNext();
        }
      };
   //   navigationHelper.addStyleName("bottomColumn");
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

      setStateFeedback(true);
      g.addStyleName("rightTenMargin");
      return g;
    }

    private void setStateFeedback(boolean initial) {
      int remaining = allExercises.size() - totalCorrect - totalIncorrect;
      //if (initial) ++remaining;
      remain.setText(remaining + "");
      incorrectBox.setText(totalIncorrect + "");
      correctBox.setText(totalCorrect + "");
    }
  }
}
