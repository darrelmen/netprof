package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 10/8/13
* Time: 5:58 PM
* To change this template use File | Settings | File Templates.
*/
class AVPHelper extends NPFHelper {
  private static final String COMPLETE = "Complete";
  private static final String LIST_COMPLETE = "List complete!";

  /**
   * @see Navigation#Navigation
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   */
  public AVPHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller) {
    super(service, feedback, userManager, controller, false);
  }

  /**
   * @see mitll.langtest.client.custom.NPFHelper#makeNPFExerciseList(com.google.gwt.user.client.ui.Panel, String)
   * @param right
   * @param instanceName
   * @return
   */
  @Override
  protected PagingExerciseList makeExerciseList(final Panel right, final String instanceName) {
    ExercisePanelFactory factory = getFactory(null, instanceName, true);
    PagingExerciseList complete = new PagingExerciseList(right, service, feedback, factory, controller,
      true, instanceName) {
      @Override
      protected void onLastItem() {
        new ModalInfoDialog(COMPLETE, LIST_COMPLETE, new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            reloadExercises();
          }
        });
      }

      @Override
      protected void addTableWithPager(PagingContainer pagingContainer) {
        pagingContainer.getTableWithPager();
      }

      @Override
      protected void addMinWidthStyle(Panel leftColumn) {
      }
    };
    return complete;
  }

  @Override
  protected ExercisePanelFactory getFactory(PagingExerciseList exerciseList, final String instanceName, boolean showQC) {
    return new MyFlashcardExercisePanelFactory(service, feedback, controller, exerciseList, "AVPHelper");
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
