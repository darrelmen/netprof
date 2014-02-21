package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.bootstrap.ResponseExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.BootstrapFlashcardExerciseList;
import mitll.langtest.client.flashcard.TableSectionExerciseList;
import mitll.langtest.client.grading.GradedExerciseList;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

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
  private ListInterface<? extends ExerciseShell> exerciseList;

  public ExerciseListLayout(PropertyHandler props) {
    this.props = props;
  }

  public ListInterface<? extends ExerciseShell> makeFlashcardExerciseList(FluidContainer container, LangTestDatabaseAsync service,
                                                 UserManager userManager) {
    this.exerciseList = new BootstrapFlashcardExerciseList<Exercise>(container, service, userManager, props.isTimedGame(),
      props.getGameTimeSeconds(), props);
    return exerciseList;
  }

  /**
   * Supports different flavors of exercise list -- Paging, Grading, and vanilla.
   *
   * @see LangTest#onModuleLoad2()
   */
  public ListInterface<? extends ExerciseShell> makeExerciseList(FluidRow secondRow,
                                        Panel leftColumn, UserFeedback feedback,
                                        Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                        ExerciseController controller) {
    boolean isGrading = props.isGrading();
    this.exerciseList = makeExerciseList(secondRow, isGrading, feedback, currentExerciseVPanel, service, controller);

    boolean hideExerciseList = (props.isMinimalUI() && !props.isGrading()) && !props.isAdminView();
    useExerciseList(leftColumn);
    if (hideExerciseList) {
      exerciseList.hideExerciseList();
    }

    return exerciseList;
  }

  private void useExerciseList(Panel leftColumn) {
    if (showOnlyOneExercise()) {
      exerciseList.setExercise_title(props.getExercise_title());
    }
    addExerciseListOnLeftSide(leftColumn);
  }

  /**
   * @see #makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param secondRow
   * @param isGrading
   * @param feedback
   * @param currentExerciseVPanel
   * @param service
   * @param controller
   * @return
   */
  private <T extends ExerciseShell> ListInterface<T> makeExerciseList(FluidRow secondRow, boolean isGrading, final UserFeedback feedback,
                                         Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                         ExerciseController controller) {
    boolean showTypeAhead = !props.isCRTDataCollectMode();
    if (isGrading) {
      return new GradedExerciseList<T>(currentExerciseVPanel, service, feedback,
        true, props.isEnglishOnlyMode(), controller,"grading");
    } else {
      if (props.isShowSections()) {
        if (props.isFlashcardTeacherView()) {
          return new TableSectionExerciseList<T>(secondRow, currentExerciseVPanel, service, feedback,
            props.isShowTurkToken(), props.showExercisesInOrder(), controller,"table");
        } else {
          if (props.isCRTDataCollectMode()) {
            return new ResponseExerciseList(secondRow, currentExerciseVPanel, service, feedback,
              props.isShowTurkToken(), props.showExercisesInOrder(), controller, props.isCRTDataCollectMode(), "response");
          } else {
            //System.out.println("makeExerciseList : show completed " + showCompleted + " flex");
            FlexSectionExerciseList<T> flex = new FlexSectionExerciseList<T>(secondRow, currentExerciseVPanel, service, feedback,
              props.isShowTurkToken(), props.showExercisesInOrder(), controller, showTypeAhead, "flex");
            return flex;
          }
        }
      } else {
        return new PagingExerciseList<T>(currentExerciseVPanel, service, feedback,
          props.isShowTurkToken(), props.showExercisesInOrder(), controller, showTypeAhead, "paging");
      }
    }
  }

  /**
   * @see #useExerciseList
   * @param leftColumnContainer
   */
  private void addExerciseListOnLeftSide(Panel leftColumnContainer) {
   // leftColumnContainer.clear();
    if (props.isTeacherView()) {
      leftColumnContainer.add(exerciseList.getWidget());
    } else {
      leftColumnContainer.addStyleName("inlineBlockStyle");
      leftColumnContainer.add(exerciseList.getExerciseListOnLeftSide(props));
    }
  }

  private boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }
}
