package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.CommonShell;

/**
* Created by GO22670 on 1/9/14.
*/
class PrevNextList extends HorizontalPanel {
  private final ExerciseController controller;
  private Button prev, next;
  private final ListInterface container;
  private boolean disableNext = true;

  /**
   * @see mitll.langtest.client.custom.dialog.EditableExercise#addNew
   * @param exerciseShell
   * @param listContainer
   * @param disableNext
   * @param controller
   */
  public PrevNextList(final CommonShell exerciseShell, ListInterface listContainer, boolean disableNext, ExerciseController controller) {
    this.container = listContainer;
    this.disableNext = disableNext;
    this.controller = controller;
    makePrevButton(exerciseShell);
    makeNextButton(exerciseShell);
    addStyleName("marginBottomTen");
    getElement().setId("PrevNextList");
  }

  private void makePrevButton(final CommonShell exercise) {
    this.prev = new Button("Previous");
    prev.getElement().setId("PrevNextList_Previous");

    controller.register(prev,exercise.getID());
    prev.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickPrev();
      }
    });
    prev.setEnabled(!container.onFirst(exercise));
    prev.setType(ButtonType.SUCCESS);

    add(prev);
  }

  private void makeNextButton(final CommonShell exercise) {
    this.next = new Button("Next");
    next.getElement().setId("nextButton");
    controller.register(next,exercise.getID());

    next.setType(ButtonType.SUCCESS);
    next.setEnabled(!disableNext || !container.onLast(exercise));

    add(next);

    //DOM.setElementAttribute(next.getElement(), "id", "nextButton");

    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickNext();
      }
    });
  }

  private void clickPrev() {
    if (prev.isEnabled() && prev.isVisible()) {
      boolean onFirst = container.loadPrev();
      prev.setEnabled(!onFirst);
    }
  }

  /**
   */
  void clickNext() {
    if (next.isEnabled() && next.isVisible()) {
      boolean onLast = container.loadNext();
      next.setEnabled(!disableNext || !onLast);
      if (!container.onFirst()) prev.setEnabled(true);
    }
  }
}
