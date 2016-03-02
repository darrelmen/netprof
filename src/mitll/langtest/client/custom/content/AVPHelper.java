/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.content;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.flashcard.StatsFlashcardFactory;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 10/8/13
* Time: 5:58 PM
* To change this template use File | Settings | File Templates.
*/
public class AVPHelper extends NPFHelper {
  private UserList ul;

  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @param service
   * @param feedback
   * @param controller
   */
  public AVPHelper(LangTestDatabaseAsync service, UserFeedback feedback, ExerciseController controller) {
    super(service, feedback, controller, false);
  }

  /**
   * @see #doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   * @param ul
   * @param instanceName
   * @return
   */
  @Override
  protected Panel getRightSideContent(UserList<CommonShell> ul, String instanceName) {
    Panel npfContentPanel = new SimplePanel();
    npfContentPanel.getElement().setId("AVPHelper_internalLayout_RightSideContent");
    this.ul = ul;
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
  PagingExerciseList<CommonShell,CommonExercise> makeExerciseList(final Panel right, final String instanceName) {
    PagingExerciseList<CommonShell,CommonExercise> widgets = new NPExerciseList(right, service, feedback, controller,
        true, instanceName, true) {
      @Override
      protected void onLastItem() {
      } // TODO : necessary?

      @Override
      protected void addTableWithPager(ClickablePagingContainer pagingContainer) {
        pagingContainer.getTableWithPager();
      }

      @Override
      protected void addMinWidthStyle(Panel leftColumn) {
      }
    };
    final ExercisePanelFactory<CommonShell,CommonExercise> factory = getFactory(null, instanceName, true);
    widgets.setFactory(factory);
    return widgets;
  }

  @Override
  ExercisePanelFactory<CommonShell,CommonExercise> getFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList, final String instanceName, boolean showQC) {
    StatsFlashcardFactory<CommonShell,CommonExercise> avpHelper =
        new StatsFlashcardFactory<>(service, feedback, controller, exerciseList, "AVPHelper", ul);
    avpHelper.setContentPanel(contentPanel);
    return avpHelper;
  }

  void addExerciseListOnLeftSide(Panel left, Widget exerciseListOnLeftSide) {}

  @Override
  public void onResize() {}
}
