package mitll.langtest.client.custom;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PagingExerciseList;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.flashcard.FlashcardExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 10/8/13
* Time: 5:58 PM
* To change this template use File | Settings | File Templates.
*/
class AVPHelper extends NPFHelper {
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;
  private final ExerciseController controller;
  private ListInterface outerExerciseList;
  private Exercise currentExercise;
  private UserFeedback feedback;
  private BootstrapExercisePanel bootstrapPanel;
  boolean addKeyHandler;

  public AVPHelper(UserFeedback feedback, LangTestDatabaseAsync service, UserManager userManager, ExerciseController controller) {
    super(service, feedback, userManager, controller);
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.feedback = feedback;
  }

  @Override
  protected void setFactory(PagingExerciseList exerciseList) {
    outerExerciseList = exerciseList;
    exerciseList.setFactory(new FlashcardExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(Exercise e) {
        currentExercise = e;

        bootstrapPanel = new BootstrapExercisePanel(e, service, controller, exerciseList, false) {
          NavigationHelper navigationHelper;

          @Override
          protected String getContentFromExercise(Exercise e) {
            return null;
          }

          @Override
          protected FlowPanel getCardPrompt(Exercise e, ExerciseController controller) {
            FlowPanel cardPrompt = super.getCardPrompt(e, controller);
            navigationHelper = new NavigationHelper(currentExercise, controller, new PostAnswerProvider() {
              @Override
              public void postAnswers(ExerciseController controller, Exercise completedExercise) {
                outerExerciseList.loadNextExercise(completedExercise);
              }
            }, outerExerciseList, false);

            cardPrompt.insert(navigationHelper.getPrev(), 0);
            cardPrompt.add(navigationHelper.getNext());
            return cardPrompt;    //To change body of overridden methods use File | Settings | File Templates.
          }
        };


        if (addKeyHandler) {
          System.out.println("adding key handler to " + bootstrapPanel);
          bootstrapPanel.addKeyHandler();
        }
        return bootstrapPanel;
      }
    }, userManager, 1);
  }

  @Override
  protected Panel setupContent(Panel hp) {
    Panel widgets = super.setupContent(hp);
    float v = Window.getClientWidth() * 0.5f;
    System.out.println("Avp width " + v);
    widgets.setWidth(v + "px");
    return widgets;
  }

  @Override
  public void onResize() {
    if (getNpfContentPanel() != null) getNpfContentPanel().setWidth(((Window.getClientWidth() * 0.6f) - 100) + "px");
  }

  @Override
  public void removeKeyHandler() {
    System.out.println("avp removeKeyHandler\n\n\n");
    addKeyHandler = false;
    if (bootstrapPanel != null) bootstrapPanel.removeKeyHandler();
  }

  @Override
  public void addKeyHandler() {
    if (bootstrapPanel != null) {
      System.out.println("avp adding key handler\n\n\n");
      bootstrapPanel.addKeyHandler();
    }
    else {
      System.out.println("avp no panel yet to add key handler to\n\n\n");
      addKeyHandler = true;
    }
  }
}
