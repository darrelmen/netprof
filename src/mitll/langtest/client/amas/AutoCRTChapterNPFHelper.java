package mitll.langtest.client.amas;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.amas.AmasExerciseImpl;

/**
 * Created by go22670 on 1/22/15.
 */
public class AutoCRTChapterNPFHelper extends SimpleChapterNPFHelper {
  //private Logger logger = Logger.getLogger("AutoCRTChapterNPFHelper");
  private QuizScorePanel child;
  private ResponseExerciseList exerciseList;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @see LangTest#populateBelowHeader
   */
  public AutoCRTChapterNPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                                 ExerciseController controller) {
    super(service, feedback, userManager, controller, null);
  }

  /**
   * @param exerciseList
   * @return
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper.MyFlexListLayout#getFactory
   */
  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
    return new ExercisePanelFactory<AmasExerciseImpl>(service, controller.getFeedback(), controller, exerciseList) {
      @Override
      public Panel getExercisePanel(AmasExerciseImpl e) {
        if (child != null) {
          child.setVisible(true);
        }
        return new FeedbackRecordPanel(e, service, controller, (ResponseExerciseList) exerciseList, child);
      }
    };
  }

  @Override
  public void addNPFToContent(Panel listContent, String instanceName) {
    super.addNPFToContent(listContent, instanceName);
    if (!controller.getProps().isAdminView()) {
      hideList();
    }
  }

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param outer
   * @return
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper#SimpleChapterNPFHelper
   */
  @Override
  protected FlexListLayout getMyListLayout(final LangTestDatabaseAsync service, final UserFeedback feedback,
                                           UserManager userManager, final ExerciseController controller,
                                           SimpleChapterNPFHelper outer) {
    return new MyFlexListLayout(service, feedback, userManager, controller, outer) {
      @Override
      protected PagingExerciseList makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName,
                                                    boolean incorrectFirst) {
        exerciseList = new ResponseExerciseList(topRow, currentExercisePanel, service, feedback, controller, instanceName);
        return exerciseList;
      }

      /**
       * @see FlexListLayout#doInternalLayout
       * @return
       */
      @Override
      protected Panel getCurrentExercisePanel() {
        Panel currentExercisePanel = super.getCurrentExercisePanel();
        currentExercisePanel.getElement().getStyle().setPadding(5, Style.Unit.PX);
        return currentExercisePanel;
      }

      /**
       * @see #doInternalLayout
       * @param bottomRow
       */
      @Override
      protected void addThirdColumn(Panel bottomRow) {
        child = new QuizScorePanel();
        child.setVisible(false);
        bottomRow.add(child);
        exerciseList.setQuizPanel(child);
      }
    };
  }
}
