package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.CommonExercise;

/**
 * A row with prev/next buttons.  Key bindings for keys too.
 * Has confirm dialog appear on next button in some modes.
 *
 * User: GO22670
 * Date: 8/6/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class NavigationHelper extends HorizontalPanel {
  private static final String LEFT_ARROW_TOOLTIP = "Press the left arrow key to go to the previous item.";

  private Button prev;
  protected Button next;
//  private final boolean debug = false;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private final PostAnswerProvider provider;
  private final ListInterface listContainer;

  /**
   * @see ExercisePanel#getNavigationHelper(ExerciseController)
   * @param exercise
   * @param controller
   * @param provider
   * @param listContainer
   * @param addKeyHandler
   */
  public NavigationHelper(CommonExercise exercise, ExerciseController controller, PostAnswerProvider provider,
                          ListInterface listContainer, boolean addKeyHandler) {
    this(exercise, controller, provider, listContainer, true, addKeyHandler,
      false);
  }

  public NavigationHelper(CommonExercise exercise, ExerciseController controller, PostAnswerProvider provider,
                          ListInterface listContainer, boolean addButtons, boolean addKeyHandler,
                          boolean enableNextOnlyWhenAllCompleted) {
    this.provider = provider;
    this.listContainer = listContainer;
    this.enableNextOnlyWhenAllCompleted = enableNextOnlyWhenAllCompleted;
    setSpacing(5);
    getNextAndPreviousButtons(exercise, controller, addButtons, addKeyHandler);
    getElement().setId("NavigationHelper");
    controller.register(prev, exercise.getID());
    controller.register(next, exercise.getID());
  }

  /**
   * @see NavigationHelper#NavigationHelper
   * @param e
   * @param controller
   * @param addButtons
   * @param addKeyHandler
   */
  private void getNextAndPreviousButtons(final CommonExercise e,
                                         final ExerciseController controller, boolean addButtons, boolean addKeyHandler) {
    makePrevButton(e, controller, addButtons, addKeyHandler);
    makeNextButton(e, controller, addButtons);
  }

  private void makePrevButton(final CommonExercise exercise, ExerciseController controller, boolean addButtons, boolean useKeyHandler) {
    this.prev = new Button("Previous");
    prev.getElement().setId("NavigationHelper_Previous");
    getPrev().addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickPrev(exercise);
      }
    });
    boolean onFirst = !listContainer.onFirst(exercise);
    getPrev().setEnabled(onFirst);
    if (useKeyHandler) getPrev().setTitle(LEFT_ARROW_TOOLTIP);
    getPrev().setType(ButtonType.SUCCESS);

    if (addButtons) add(getPrev());
    //getPrev().setVisible(!controller.isMinimalUI() || !controller.isPromptBeforeNextItem());
  }

  private void makeNextButton(final CommonExercise exercise, final ExerciseController controller, boolean addButtons) {
    this.next = new Button(getNextButtonText());
    next.getElement().setId("NavigationHelper_"+getNextButtonText());

    next.setType(ButtonType.SUCCESS);
    enableNext(exercise);

    if (addButtons)  add(next);

    next.getElement().setId("nextButton");

    // send answers to server
    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickNext(controller, exercise);
      }
    });
  }

  protected void enableNext(CommonExercise exercise) {
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
      next.setEnabled(false);
    }
    else {
      next.setEnabled(!listContainer.onLast(exercise));
    }
  }

  void clickPrev(CommonExercise e) {
    if (getPrev().isEnabled() && getPrev().isVisible()) {
      getPrev().setEnabled(false);
      listContainer.loadPreviousExercise(e);
    }
  }

  /**
   * Ignore clicks or keyboard activity when the widget is not enabled.
   * @see #getNextAndPreviousButtons
   * @seex #addKeyHandler
   * @param controller
   * @param exercise
   */
  void clickNext(final ExerciseController controller, final CommonExercise exercise) {
    if (next.isEnabled() && next.isVisible()) {
      if (provider != null) {
        provider.postAnswers(controller, exercise);
      }
    }
  }

  protected String getNextButtonText() {
    return "Next";
  }

  public void enableNextButton(boolean val) {
    next.setEnabled(val);
  }

  public Widget getNext() { return next; }
  public Button getPrev() { return prev; }
}
