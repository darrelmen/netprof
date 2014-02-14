package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
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
 */
class MyFlashcardExercisePanelFactory<T extends ExerciseShell> extends FlashcardExercisePanelFactory {
  private Exercise currentExercise;
  ControlState controlState;
  List<List<T>> sets = new ArrayList<List<T>>();
  int setIndex = 0;
  float totalScore;
  int totalCorrect;
  int totalIncorrect;
  float totalDone;
  List<T> currentSet = new ArrayList<T>();
  int size = 10;
  List<T> allExercises;

  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface<T> exerciseList) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();

    exerciseList.addListChangedListener(new ListChangeListener<T>() {
      @Override
      public void listChanged(List<T> items) {
        allExercises = items;
        partition(items);
      }
    });
  }

  public void partition(List<T> items) {
    sets = Lists.partition(items, size);
    currentSet = new ArrayList<T>(sets.get(setIndex = 0));

  }

  @Override
  public Panel getExercisePanel(Exercise e) {
    currentExercise = e;
    findAndRemoveCurrent(e);

    return new BootstrapExercisePanel(e, service, controller, 40, false, controlState) {
      @Override
      protected void loadNext() {
        if (!currentSet.isEmpty()) {
          exerciseList.loadNextExercise(currentExercise.getID());
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

        System.out.println("receivedAudioAnswer : currentSet " + currentSet.size() + " : " + currentSet + " total done " + totalDone);

        if (currentSet.isEmpty()) {
          onSetComplete();
        }
      }

      public void onSetComplete() {
        navigationHelper.setVisible(false);
        belowContentDiv.add(new Heading(2, totalCorrect + " Correct - Average Score " + ((int) ((totalScore * 100f) / totalDone)) + "%"));
        HorizontalPanel w = new HorizontalPanel();
        Button w1 = new Button("Repeat this set");
        w1.setType(ButtonType.PRIMARY);
        w.add(w1);
        w1.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            currentSet = new ArrayList<T>(sets.get(setIndex));

            reset();

            System.out.println("====> repeat set : currentSet " + currentSet + " set index " + setIndex);
            navigationHelper.setVisible(true);

            exerciseList.loadExercise(currentSet.iterator().next().getID());
          }
        });

        Button w2 = new Button("Next set");
        w.add(w2);
        w2.addStyleName("leftFiveMargin");
        w2.setType(ButtonType.SUCCESS);

        w2.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            try {
              currentSet = new ArrayList<T>(sets.get(++setIndex));

              System.out.println("====> next set : currentSet " + currentSet + " set index " + setIndex);
              navigationHelper.setVisible(true);
              reset();
            } catch (Exception e1) {
              currentSet = new ArrayList<T>(sets.get(setIndex = 0));
            }
            exerciseList.loadExercise(currentSet.iterator().next().getID());
          }
        });
        belowContentDiv.add(w);
        // TODO : maybe add table showing results per word
        // TODO : do we do aggregate scores
      }

      protected void nextAfterDelay(boolean correct, String feedback) {
        if (!currentSet.isEmpty()) {
          super.nextAfterDelay(correct, feedback);
        }
      }

      protected void setSetSize(int i) {
        if (i != size) {
          reset();

          size = i;
          partition(allExercises);
          exerciseList.loadExercise(currentSet.iterator().next().getID());
        }
      }

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
      protected Widget getHelpRow(ExerciseController controller) {
        return null;
      }

      Label remain, incorrectBox, correctBox;

      protected Panel getLeftState() {
        Grid g = new Grid(3, 2);
        ControlGroup remaining = new ControlGroup("Remaining");
        remaining.addStyleName("topFiveMargin");
        remain = new Label();
        remain.setType(LabelType.INFO);
        g.setWidget(0, 0, remaining);
        g.setWidget(0, 1, remain);

        ControlGroup incorrect = new ControlGroup("Incorrect");
        incorrect.addStyleName("topFiveMargin");

        incorrectBox = new Label();
        incorrectBox.setType(LabelType.WARNING);

        g.setWidget(1, 0, incorrect);
        g.setWidget(1, 1, incorrectBox);

        ControlGroup correct = new ControlGroup("Correct");
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
        int remaining = currentSet.size();
        if (initial) ++remaining;
        remain.setText(remaining + "");
        incorrectBox.setText(totalIncorrect + "");
        correctBox.setText(totalCorrect + "");
      }
    };
  }

  public void findAndRemoveCurrent(Exercise e) {
    T current = findCurrent(e);

    if (current != null) {
      currentSet.remove(current);
    } else {
      System.err.println("========> couldn't find " + e.getID() + " in currentSet " + currentSet.size() + " : " + currentSet);
    }
  }

  public T findCurrent(Exercise e) {
    T current = null;
    for (T t : currentSet) {
      if (t.getID().equals(e.getID())) {
        current = t;
        break;
      }
    }
    return current;
  }

  public void reset() {
    totalDone = 0;
    totalCorrect = 0;
    totalScore = 0;
    totalIncorrect = 0;
  }
}
