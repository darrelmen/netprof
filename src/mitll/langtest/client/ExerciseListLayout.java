package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.bootstrap.TableSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.GradedExerciseList;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.exercise.PagingExerciseList;
import mitll.langtest.client.taboo.TabooExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
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
  private ListInterface exerciseList;
  private static final String ITEMS = "Items";

  public ExerciseListLayout(PropertyHandler props) {
    this.props = props;
  }

  public ListInterface makeFlashcardExerciseList(FluidContainer container, LangTestDatabaseAsync service, UserManager userManager) {
    this.exerciseList = new BootstrapFlashcardExerciseList(container, service, userManager, props.isTimedGame(), props.getGameTimeSeconds());
    return exerciseList;
  }

  /**
   * Supports different flavors of exercise list -- Paging, Grading, and vanilla.
   *
   * @see LangTest#onModuleLoad2()
   */
  public ListInterface makeExerciseList(FluidRow secondRow,
                                Panel leftColumn, UserFeedback feedback,
                                Panel currentExerciseVPanel, LangTestDatabaseAsync service, ExerciseController controller) {
    boolean isGrading = props.isGrading();
    this.exerciseList = makeExerciseList(secondRow, isGrading, feedback, currentExerciseVPanel, service, controller);

    boolean hideExerciseList = (props.isMinimalUI() && !props.isGrading()) && !props.isAdminView();
    useExerciseList(leftColumn, hideExerciseList);
    return exerciseList;
  }

  private void useExerciseList(Panel leftColumn, boolean hideExerciseList) {
    if (hideExerciseList) {
      exerciseList.getWidget().setVisible(false);
      exerciseList.getWidget().setWidth("1px");
    }

    if (showOnlyOneExercise()) {
      exerciseList.setExercise_title(props.getExercise_title());
    }
    addExerciseListOnLeftSide(leftColumn);
  }

  protected ListInterface makeExerciseList(FluidRow secondRow, boolean isGrading, final UserFeedback feedback,
                                         Panel currentExerciseVPanel, LangTestDatabaseAsync service, ExerciseController controller) {
    if (isGrading) {
      return new GradedExerciseList(currentExerciseVPanel, service, feedback,
        true, props.isEnglishOnlyMode(), controller);
    } else {
      if (props.isShowSections()) {
        boolean showSectionWidgets = props.isShowSectionWidgets();
        if (props.isFlashcardTeacherView()) {
          return new TableSectionExerciseList(secondRow, currentExerciseVPanel, service, feedback,
            props.isShowTurkToken(), isAutoCRTMode(), showSectionWidgets, controller);
        } else {
          if (props.isTrackUsers()) {
            return new TabooExerciseList(secondRow, currentExerciseVPanel, service, feedback,
              props.isShowTurkToken(), isAutoCRTMode(), showSectionWidgets, controller);
          }
          else {
          return new FlexSectionExerciseList(secondRow, currentExerciseVPanel, service, feedback,
            props.isShowTurkToken(), isAutoCRTMode(), showSectionWidgets, controller);
          }
        }
      } else {
        return new PagingExerciseList(currentExerciseVPanel, service, feedback,
          props.isShowTurkToken(), isAutoCRTMode(), controller) {
          @Override
          protected void checkBeforeLoad(ExerciseShell e) {
          } // don't try to login
        };
      }
    }
  }

  private void addExerciseListOnLeftSide(Panel leftColumnContainer) {
    if (props.isTeacherView()) {
      leftColumnContainer.add(exerciseList.getWidget());
    } else {
      Heading items = new Heading(4, ITEMS);
      items.addStyleName("center");

      FlowPanel leftColumn = new FlowPanel();
      leftColumn.addStyleName("floatLeft");
      DOM.setStyleAttribute(leftColumn.getElement(), "paddingRight", "10px");

      leftColumnContainer.add(leftColumn);
      leftColumnContainer.addStyleName("inlineStyle");
      if (!props.isFlashcardTeacherView() && !props.isMinimalUI()) {
        leftColumn.add(items);
      }
      leftColumn.add(exerciseList.getWidget());
    }
  }

/*
  public int getLeftColumnWidth() {
    int offsetWidth = exerciseList.getWidget().getOffsetWidth();
    // System.out.println("left col width " +offsetWidth);
    return offsetWidth;
  }
*/

  private boolean showOnlyOneExercise() {
    return props.getExercise_title() != null;
  }
  private boolean isAutoCRTMode() {  return props.isAutocrt(); }

}
