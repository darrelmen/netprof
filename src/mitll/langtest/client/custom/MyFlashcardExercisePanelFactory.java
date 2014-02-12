package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
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
  //Pager pager = new Pager("<",">");
  float totalScore;
  int totalCorrect;
  float totalDone;
  List<T> currentSet = new ArrayList<T>();
  int size = 3;

  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface<T> exerciseList) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();

    exerciseList.addListChangedListener(new ListChangeListener<T>() {
      @Override
      public void listChanged(List<T> items) {
        sets = Lists.partition(items, size);
       // currentSet = new ArrayList<T>(sets.get(setIndex = 0));
        currentSet = new ArrayList<T>(sets.get(setIndex = 0));
        System.out.println("====> new set : currentSet " + currentSet + " set index " + setIndex);
      }
    });
  }

  @Override
  public Panel getExercisePanel(Exercise e) {
    currentExercise = e;
    T current = null;
    for (T t : currentSet) {
      if (t.getID().equals(e.getID())) {
        current = t; break;
      }
    }

    if (current != null) {
      currentSet.remove(current);
    //  System.out.println("========> after removing " + current.getID()+" currentSet " + currentSet.size()+ " : " + currentSet);

    }
    else {
     // System.err.println("========> couldn't find " + e.getID()+" in currentSet " + currentSet.size()+ " : " + currentSet);

    }

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
       // if (result.validity == AudioAnswer.Validity.OK) {
          totalDone++;
          if (result.isCorrect()) totalScore += result.getScore();
       // }
        if (result.isCorrect()) {
          totalCorrect++;
        }

        super.receivedAudioAnswer(result);

        System.out.println("receivedAudioAnswer : currentSet " + currentSet.size() + " : " + currentSet + " total done " + totalDone);

        if (currentSet.isEmpty()) {
          navigationHelper.setVisible(false);
         // String suffix = totalDone != 10 ? " over " + totalDone + " answered." : "";
          add(new Heading(2, totalCorrect + " Correct - Average Score " + ((int) ((totalScore * 100f) / totalDone)) + "%"));
          HorizontalPanel w = new HorizontalPanel();
         // w.setSpacing(5);
          Button w1 = new Button("Repeat this set");
          w1.setType(ButtonType.PRIMARY);
          w.add(w1);
          w1.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              currentSet = new ArrayList<T>(sets.get(setIndex));

              totalDone = 0;
              totalCorrect = 0;
              totalScore = 0;

              System.out.println("====> repeat set : currentSet " + currentSet+ " set index " + setIndex);
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

                System.out.println("====> next set : currentSet " + currentSet+ " set index " + setIndex);
                navigationHelper.setVisible(true);
                totalDone = 0;
                totalCorrect = 0;
                totalScore = 0;
              } catch (Exception e1) {
             //   currentSet = sets.get(setIndex = 0);
                currentSet = new ArrayList<T>(sets.get(setIndex = 0));
              }
              exerciseList.loadExercise(currentSet.iterator().next().getID());
            }
          });
          add(w);
          // TODO : maybe add table showing results per word
          // TODO : do we do aggregate scores
        }
      }

      protected void nextAfterDelay(boolean correct, String feedback) {
        if (!currentSet.isEmpty()) {
          super.nextAfterDelay(correct,feedback);
        }
      }

/*      @Override
      protected void goToNextItem(String infoToShow) {
      }*/

      NavigationHelper<Exercise> navigationHelper;
      @Override
      protected void addWidgetsBelow() {
        navigationHelper = new NavigationHelper<Exercise>(currentExercise, controller, null, exerciseList, true, false) {
          @Override
          protected void clickNext(ExerciseController controller, Exercise exercise) {
            loadNext();
          }
        };
        add(navigationHelper);
      }

      @Override
      protected Widget getHelpRow(ExerciseController controller) { return null; }
    };
  }


  // TODO : pass these in
  // TODO : how do we do a sublist of the larger list
  // TODO : keep track of scores for sublist
  // TODO : display score when complete
  // TODO : when set done, do we do it again or go on to next set?

}
