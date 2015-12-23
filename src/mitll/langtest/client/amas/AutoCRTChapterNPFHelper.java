package mitll.langtest.client.amas;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.exercise.CommonExercise;

/**
* Created by go22670 on 1/22/15.
*/
public class AutoCRTChapterNPFHelper extends SimpleChapterNPFHelper {
  //private Logger logger = Logger.getLogger("AutoCRTChapterNPFHelper");
  private QuizScorePanel child;
  private ResponseExerciseList exerciseList;

  /**
   *
   * @see LangTest#populateBelowHeader
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   */
  public AutoCRTChapterNPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                                 ExerciseController controller) {
    super(service, feedback, userManager, controller,null);
  }

  /**
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper.MyFlexListLayout#getFactory
   * @param exerciseList
   * @return
   */
  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
    return new ExercisePanelFactory(service, controller.getFeedback(), controller, exerciseList) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        if (child != null) {child.setVisible(true);}
        return new FeedbackRecordPanel(e, service, controller, exerciseList, child);
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
   * @see mitll.langtest.client.custom.SimpleChapterNPFHelper#SimpleChapterNPFHelper
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param outer
   * @return
   */
  @Override
  protected NPFHelper.FlexListLayout getMyListLayout(final LangTestDatabaseAsync service, final UserFeedback feedback,
                                                     UserManager userManager, final ExerciseController controller,
                                                     SimpleChapterNPFHelper outer) {
    return new MyFlexListLayout(outer) {
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
       * @see #doInternalLayout(String)
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
