package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.grading.GradedExerciseList;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;

/**
 * Deals with choosing the right exercise list, depending on the property settings.
 *
 * User: GO22670
 * Date: 8/6/13
 * Time: 1:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseListLayout {
  private final PropertyHandler props;
  private ListInterface exerciseList;

  public ExerciseListLayout(PropertyHandler props) {
    this.props = props;
  }

  /**
   * Supports different flavors of exercise list -- Paging, Grading, and vanilla.
   *
   * @param secondRow add the section panel to this row

   * @see LangTest#makeExerciseList
   */
  public ListInterface makeExerciseList(FluidRow secondRow,
                                        Panel exerciseListContainer, UserFeedback feedback,
                                        Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                        ExerciseController controller) {
    boolean isGrading = props.isGrading();
    System.out.println("ExerciseListLayout.makeExerciseList : ------------->");

    this.exerciseList = makeExerciseList(secondRow, isGrading, feedback, currentExerciseVPanel, service, controller);

    boolean hideExerciseList = (props.isMinimalUI() && !isGrading) && !props.isAdminView();
    useExerciseList(exerciseListContainer);
    if (hideExerciseList) {
      exerciseList.hide();
    }

    return exerciseList;
  }

  private void useExerciseList(Panel exerciseListContainer) {
    if (showOnlyOneExercise()) {
      exerciseList.setExercise_title(props.getExercise_title());
    }
    addExerciseListOnLeftSide(exerciseListContainer);
  }

  /**
   * @see #makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param secondRow add the section panel to this row
   * @param isGrading
   * @param feedback
   * @param currentExerciseVPanel
   * @param service
   * @param controller
   * @return
   */
  private ListInterface makeExerciseList(FluidRow secondRow, boolean isGrading, final UserFeedback feedback,
                                         Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                         ExerciseController controller) {
    boolean showTypeAhead = !props.isCRTDataCollectMode();
    if (isGrading) {
      return new GradedExerciseList(currentExerciseVPanel, service, feedback,
        true, props.isEnglishOnlyMode(), controller, "grading");
    } else {
      if (props.isShowSections()) {
        System.out.println("makeExerciseList : making flex");
        FlexSectionExerciseList flex = new FlexSectionExerciseList(secondRow, currentExerciseVPanel, service, feedback,
          props.isShowTurkToken(), props.showExercisesInOrder(), controller, showTypeAhead, "flex");
        return flex;
      } else {
        return new PagingExerciseList(currentExerciseVPanel, service, feedback,
          null, controller, props.isShowTurkToken(), props.showExercisesInOrder(), showTypeAhead, "paging");
      }
    }
  }

  /**
   * @see #useExerciseList
   * @param exerciseListContainer add exercise list inside this
   */
  private void addExerciseListOnLeftSide(Panel exerciseListContainer) {
    if (props.isTeacherView()) {
      exerciseListContainer.add(exerciseList.getWidget());
    } else {
      exerciseListContainer.add(exerciseList.getExerciseListOnLeftSide(props));
    }
  }

  private boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }
}
