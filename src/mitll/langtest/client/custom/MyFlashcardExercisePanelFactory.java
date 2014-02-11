package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.flashcard.ControlState;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

/**
* Created by go22670 on 2/10/14.
*/
class MyFlashcardExercisePanelFactory extends FlashcardExercisePanelFactory {
  private Exercise currentExercise;
  ControlState controlState;

  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface<? extends ExerciseShell> exerciseList) {
    super(service, feedback, controller, exerciseList);
    controlState = new ControlState();
     System.out.println("made " + controlState);
  }

  @Override
  public Panel getExercisePanel(Exercise e) {
    currentExercise = e;

    return new BootstrapExercisePanel(e, service, controller, 40, false, controlState) {
      @Override
      protected void addRecordingAndFeedbackWidgets(Exercise e, LangTestDatabaseAsync service,
                                                    ExerciseController controller, int feedbackHeight, Panel toAddTo) {
        super.addRecordingAndFeedbackWidgets(e, service, controller, feedbackHeight, toAddTo);
        NavigationHelper<Exercise> child = new NavigationHelper<Exercise>(currentExercise, controller, new PostAnswerProvider() {
          @Override
          public void postAnswers(ExerciseController controller, ExerciseShell completedExercise) {
            exerciseList.loadNextExercise(completedExercise.getID());
          }
        }, exerciseList, true, false);
        child.setWidth("100%");
        toAddTo.add(child);
      }

      @Override
      protected void loadNext() {
        exerciseList.loadNextExercise(currentExercise.getID());
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
