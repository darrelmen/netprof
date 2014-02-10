package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.flashcard.FlashcardRecordButtonPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.recorder.FlashcardRecordButton;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserExercise;

/**
* Created by go22670 on 2/10/14.
*/
class MyFlashcardExercisePanelFactory extends FlashcardExercisePanelFactory {
  //private AVPHelper avpHelper;

 // private final LangTestDatabaseAsync service;
 // private final UserManager userManager;
 // private final ExerciseController controller;
 // private ListInterface<UserExercise> outerExerciseList;
  private Exercise currentExercise;
 // private UserFeedback feedback;
 // private BootstrapExercisePanel bootstrapPanel;

  public MyFlashcardExercisePanelFactory(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller,
                                         ListInterface<? extends ExerciseShell> exerciseList) {
    super(service, feedback, controller, exerciseList);

  }

  @Override
  public Panel getExercisePanel(Exercise e) {
    currentExercise = e;

    BootstrapExercisePanel bootstrapPanel = new BootstrapExercisePanel(e, service, controller, 40, false) {
      NavigationHelper<UserExercise> navigationHelper;

      protected RecordButtonPanel getAnswerWidget(final Exercise exercise, LangTestDatabaseAsync service,
                                                  ExerciseController controller, final int index, boolean addKeyBinding) {

          return new FlashcardRecordButtonPanel(this, service, controller, exercise, index) {

            @Override
            protected RecordButton makeRecordButton(ExerciseController controller) {
              return new FlashcardRecordButton(controller.getRecordTimeout(), this, true, false);
            }

            @Override
            protected void loadNext() {
              exerciseList.loadNext();
            }
          };
      }

      @Override
      protected FlowPanel getCardPrompt(Exercise e, ExerciseController controller) {
        FlowPanel cardPrompt = super.getCardPrompt(e, controller);
        UserExercise currentExercise1 = new UserExercise(currentExercise);
        navigationHelper = new NavigationHelper<UserExercise>(currentExercise1, controller, new PostAnswerProvider() {
          @Override
          public void postAnswers(ExerciseController controller, ExerciseShell completedExercise) {
            exerciseList.loadNextExercise(completedExercise.getID());
          }
        }, exerciseList, false, false);

        cardPrompt.insert(navigationHelper.getPrev(), 0);
        cardPrompt.add(navigationHelper.getNext());
        return cardPrompt;
      }

      @Override
      protected Widget getHelpRow(ExerciseController controller) { return null; }
    };

    return bootstrapPanel;
  }

  // TODO : pass these in
  // TODO : how do we do a sublist of the larger list
  // TODO : keep track of scores for sublist
  // TODO : display score when complete
  // TODO : when set done, do we do it again or go on to next set?

  public static class ControlState {
    boolean audioOn;
    boolean audioFeedbackOn;
    boolean visualFeedbackOn;
    String showState; // english/foreign/both
    boolean playStateOn;
  }
}
