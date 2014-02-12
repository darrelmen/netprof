package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.NumberedPager;
import com.github.gwtbootstrap.client.ui.Pager;
import com.github.gwtbootstrap.client.ui.SimplePager;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;
import java_cup.sym;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
* Created by go22670 on 2/10/14.
*/
class MyFlashcardExercisePanelFactory<T extends ExerciseShell> extends FlashcardExercisePanelFactory {
  private Exercise currentExercise;
  ControlState controlState;
  List<List<T>> sets = new ArrayList<List<T>>();
  int setIndex = 0;
  Pager pager = new Pager("<",">");
  float totalScore;
  int totalCorrect;
  float totalDone;
  Set<T> currentSet = new HashSet<T>();
  int size = 10;

  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface<T> exerciseList) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();

    exerciseList.addListChangedListener(new ListChangeListener<T>() {
      @Override
      public void listChanged(List<T> items) {
        sets = Lists.partition(items, size);
        currentSet = new HashSet<T>(sets.get(0));
        System.out.println("====> new set : currentSet " + currentSet);
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
      System.out.println("========> after removing " + current.getID()+" currentSet " + currentSet.size()+ " : " + currentSet);

    }
    else {
      System.err.println("========> couldn't find " + e.getID()+" in currentSet " + currentSet.size()+ " : " + currentSet);

    }

    return new BootstrapExercisePanel(e, service, controller, 40, false, controlState) {
      @Override
      protected void loadNext() {
        exerciseList.loadNextExercise(currentExercise.getID());
      }

      public void receivedAudioAnswer(final AudioAnswer result) {
        if (result.validity == AudioAnswer.Validity.OK) {
          totalDone++;
          totalScore += result.getScore();
        }
        if (result.isCorrect()) {
          totalCorrect++;
        }

        System.out.println("receivedAudioAnswer : currentSet " + currentSet.size() + " : " + currentSet);

        if (currentSet.isEmpty()) {
         // String suffix = totalDone != 10 ? " over " + totalDone + " answered." : "";
          add(new Heading(2, totalCorrect + " Correct - Average Score " + ((int) ((totalScore*100f) / totalDone)) + "%"));
          // TODO : add do this again or do next set?
          // TODO : maybe add table showing results per word
          // TODO : do we do aggregate scores

        } else {
          super.receivedAudioAnswer(result);
        }
      }

      @Override
      protected void addWidgetsBelow() {
        pager = new Pager("<", ">");

        Scheduler.get().scheduleDeferred(new Command() {
          public void execute() {

//            if (exerciseList.onFirst()) {
              pager.getLeft().setDisabled(exerciseList.onFirst());
  //          }

            if (exerciseList.onLast()) {
              // TODO : show score for ones we've done!       ?
            }
            pager.getRight().setDisabled(exerciseList.onLast());
          }
        });

     /*   if (exerciseList.onFirst()) {
          pager.getLeft().setActive(false);
        }

        if (exerciseList.onLast()) {
          pager.getRight().setActive(false);
        }
*/
        pager.getLeft().addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            boolean b = exerciseList.loadPrev();
            /*if (exerciseList.onFirst()) {
              System.out.println("onClick onfirst!");

              pager.getLeft().setActive(false);
            }*/
          }
        });
        pager.getRight().addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            boolean b = exerciseList.loadNext();
           /* if (exerciseList.onLast()) {
              System.out.println("===> onClick on last!");

              pager.getRight().setActive(false);
            }*/
          }
        });

        add(pager);
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
