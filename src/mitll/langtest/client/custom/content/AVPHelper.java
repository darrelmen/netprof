package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.flashcard.MyFlashcardExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 10/8/13
* Time: 5:58 PM
* To change this template use File | Settings | File Templates.
*/
public class AVPHelper extends NPFHelper {
  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   */
  public AVPHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller) {
    super(service, feedback, userManager, controller, false);
  }

  @Override
  protected Panel getRightSideContent(UserList ul, String instanceName) {
    Panel npfContentPanel = new SimplePanel();
    npfContentPanel.getElement().setId("AVPHelper_internalLayout_RightSideContent");

    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName + "_"+ul.getUniqueID());
    return npfContentPanel;
  }


  /**
   * @see mitll.langtest.client.custom.content.NPFHelper#makeNPFExerciseList(com.google.gwt.user.client.ui.Panel, String)
   * @param right
   * @param instanceName
   * @return
   */
  @Override
  protected PagingExerciseList makeExerciseList(final Panel right, final String instanceName) {
    final ExercisePanelFactory factory = getFactory(null, instanceName, true);
    PagingExerciseList complete = new PagingExerciseList(right, service, feedback, factory, controller,
      true, instanceName) {
      @Override
      protected void onLastItem() {
        ((MyFlashcardExercisePanelFactory)factory).resetStorage();
        new ModalInfoDialog(COMPLETE, LIST_COMPLETE, new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            ((MyFlashcardExercisePanelFactory)factory).resetStorage();
            reloadExercises();
          }
        });
      }

      @Override
      protected void addTableWithPager(PagingContainer pagingContainer) {
        pagingContainer.getTableWithPager();
      }

      @Override
      protected void addMinWidthStyle(Panel leftColumn) {}
    };
    return complete;
  }

  @Override
  protected ExercisePanelFactory getFactory(PagingExerciseList exerciseList, final String instanceName, boolean showQC) {
    return new MyFlashcardExercisePanelFactory(service, feedback, controller, exerciseList, "AVPHelper");
  }
/*
  @Override
  protected Panel setupContent(Panel hp) {
    Panel widgets = super.setupContent(hp);
    float v = Window.getClientWidth() * 0.5f;
    widgets.setWidth(v + "px");
    return widgets;
  }*/

  @Override
  public void onResize() {}
}
