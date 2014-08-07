package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.User;

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
    this.exerciseList = makeExerciseList(secondRow, feedback, currentExerciseVPanel, service, controller);

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
   * @param feedback
   * @param currentExerciseVPanel
   * @param service
   * @param controller
   * @return
   */
  private ListInterface makeExerciseList(FluidRow secondRow, final UserFeedback feedback,
                                         Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                         ExerciseController controller) {
    boolean showTypeAhead = !props.isCRTDataCollectMode();
    boolean hasQC = controller.getPermissions().contains(User.Permission.QUALITY_CONTROL);
    String instance = hasQC ? User.Permission.QUALITY_CONTROL.toString() : "flex";

    return new FlexSectionExerciseList(secondRow, currentExerciseVPanel, service, feedback,
        props.isShowTurkToken(), props.showExercisesInOrder(), controller, showTypeAhead, instance);
  }

  /**
   * @param exerciseListContainer add exercise list inside this
   * @see #useExerciseList
   */
  private void addExerciseListOnLeftSide(Panel exerciseListContainer) {
    exerciseListContainer.add(exerciseList.getExerciseListOnLeftSide(props));
  }

  private boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }
}
