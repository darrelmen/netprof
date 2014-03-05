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
  private UserFeedback feedback;

  public AVPHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller) {
    super(service, feedback, userManager, controller);
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.feedback = feedback;
  }

  /**
   * @see mitll.langtest.client.custom.NPFHelper#makeNPFExerciseList
   * @param exerciseList
   * @param instanceName
   * @param userListID
   */
  @Override
  protected void setFactory(final PagingExerciseList<UserExercise> exerciseList, final String instanceName, long userListID) {
    exerciseList.setFactory(new MyFlashcardExercisePanelFactory<UserExercise>(service, feedback,controller,exerciseList,userListID), userManager, 1);
  }

  @Override
  protected Panel setupContent(Panel hp) {
    Panel widgets = super.setupContent(hp);
    float v = Window.getClientWidth() * 0.5f;
    widgets.setWidth(v + "px");
    return widgets;
  }

  @Override
  public void onResize() {
    if (getNpfContentPanel() != null) {
      getNpfContentPanel().setWidth(((Window.getClientWidth() * 0.6f) - 100) + "px");
    }
  }
}
