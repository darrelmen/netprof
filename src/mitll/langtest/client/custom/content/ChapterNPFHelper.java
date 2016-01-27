/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.content;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ChapterNPFHelper<T extends CommonExercise, UL extends UserList<T>> extends NPFHelper<T,UL> {
  private final FlexListLayout flexListLayout;

  /**
   * Makes defect helper.
   *
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param showQC
   * @see mitll.langtest.client.custom.Navigation#Navigation(LangTestDatabaseAsync, UserManager, ExerciseController, UserFeedback)
   */
  public ChapterNPFHelper(final LangTestDatabaseAsync service, final UserFeedback feedback,
                          final UserManager userManager, final ExerciseController controller, final boolean showQC) {
    super(service, feedback, userManager, controller, showQC);
    final NPFHelper<T,UL> outer = this;
    this.flexListLayout = new FlexListLayout(service, feedback, userManager, controller) {
      @Override
      protected ExercisePanelFactory<T> getFactory(PagingExerciseList exerciseList, String instanceName) {
        return outer.getFactory(exerciseList, instanceName, showQC);
      }

      @Override
      protected PagingExerciseList<T> makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, boolean incorrectFirst) {
        return new NPFlexSectionExerciseList<T>(this, topRow, currentExercisePanel, instanceName, incorrectFirst);
      }
    };
  }

  //  {
//    return new NPFlexSectionExerciseList<T>(this, topRow, currentExercisePanel, instanceName, incorrectFirst);
//  }

  /**
   * Left and right components
   *
   * @param ul
   * @param instanceName
   * @return
   */
  protected Panel doInternalLayout(UserList ul, String instanceName) {
    Panel widgets = flexListLayout.doInternalLayout(ul, instanceName);
    npfExerciseList = flexListLayout.npfExerciseList;
    this.instanceName = instanceName;
    return widgets;
  }

  @Override
  public void onResize() {
    if (flexListLayout != null) {
      flexListLayout.onResize();
    } else if (npfExerciseList != null) {
      npfExerciseList.onResize();
    } else {
      System.out.println("ChapterNPFHelper.onResize : not sending resize event - flexListLayout is null?");
    }
  }
}
