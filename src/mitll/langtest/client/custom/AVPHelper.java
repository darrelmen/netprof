package mitll.langtest.client.custom;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserExercise;

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
  //private ListInterface<UserExercise> outerExerciseList;
 // private Exercise currentExercise;
  private UserFeedback feedback;
  //private BootstrapExercisePanel bootstrapPanel;

  public AVPHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller) {
    super(service, feedback, userManager, controller);
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.feedback = feedback;
  }

  @Override
  protected void setFactory(final PagingExerciseList<UserExercise> exerciseList, final String instanceName) {
  //  outerExerciseList = exerciseList;
    exerciseList.setFactory(new MyFlashcardExercisePanelFactory(service, feedback,controller,exerciseList), userManager, 1);
  }

  @Override
  protected Panel setupContent(Panel hp) {
    Panel widgets = super.setupContent(hp);
    float v = Window.getClientWidth() * 0.5f;
  //  System.out.println("Avp width " + v);
    widgets.setWidth(v + "px");
    return widgets;
  }

  @Override
  public void onResize() {
    if (getNpfContentPanel() != null) getNpfContentPanel().setWidth(((Window.getClientWidth() * 0.6f) - 100) + "px");
  }

}
