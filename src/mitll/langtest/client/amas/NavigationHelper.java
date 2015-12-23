package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonExercise;

/**
 * A row with prev/next buttons.  Key bindings for keys too.
 * Has confirm dialog appear on next button in some modes.
 * <p>
 * User: GO22670
 * Date: 8/6/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class NavigationHelper extends HorizontalPanel {
  private static final String CONTINUE = "Continue";

  private Button next;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private final PostAnswerProvider provider;
  private final ListInterface listContainer;

  public NavigationHelper(CommonExercise exercise, ExerciseController controller, PostAnswerProvider provider,
                          ListInterface listContainer, boolean addButtons,
                          boolean enableNextOnlyWhenAllCompleted) {
    this.provider = provider;
    this.listContainer = listContainer;
    this.enableNextOnlyWhenAllCompleted = enableNextOnlyWhenAllCompleted;
    setSpacing(5);
    getNextAndPreviousButtons(exercise, controller, addButtons);
    getElement().setId("NavigationHelper");
    addStyleName("rightTenMargin");
    controller.register(next, exercise.getID());
  }

  /**
   * @paramZ addKeyHandler
   * @param e
   * @param controller
   * @param addButtons
   * @see NavigationHelper#NavigationHelper
   */
  private void getNextAndPreviousButtons(final CommonExercise e,
                                         final ExerciseController controller, boolean addButtons) {
    makeNextButton(e, controller, addButtons);
  }

  private void makeNextButton(final CommonExercise exercise, final ExerciseController controller, boolean addButtons) {
    this.next = new Button(getNextButtonText());
    next.getElement().setId("NavigationHelper_" + getNextButtonText());

    next.setType(ButtonType.SUCCESS);
    enableNext(exercise);

    if (addButtons) add(next);

    next.getElement().setId("nextButton");

    // send answers to server
    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickNext(controller, exercise);
      }
    });
  }

  private void enableNext(CommonExercise exercise) {
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
      next.setEnabled(false);
    } else {
      next.setEnabled(!listContainer.onLast(exercise));
    }
  }

  /**
   * Ignore clicks or keyboard activity when the widget is not enabled.
   *
   * @param controller
   * @param exercise
   * @see #getNextAndPreviousButtons
   */
  private void clickNext(final ExerciseController controller, final CommonExercise exercise) {
    if (next.isEnabled() && next.isVisible()) {
      if (provider != null) {
        provider.postAnswers(controller, exercise);
      }
    }
  }

  private String getNextButtonText() {
    return CONTINUE;
  }

  public void enableNextButton(boolean val) {
    next.setEnabled(val);
  }
}
