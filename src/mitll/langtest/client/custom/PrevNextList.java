package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.ExerciseShell;

/**
* Created by GO22670 on 1/9/14.
*/
class PrevNextList<T extends ExerciseShell> extends HorizontalPanel {
  private Button prev, next;
  private ListInterface<T> container;
  boolean disableNext = true;

  /**
   * @see mitll.langtest.client.custom.EditItem.EditableExercise#addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   * @param exerciseShell
   * @param listContainer
   * @param disableNext
   */
  public PrevNextList(final T exerciseShell, ListInterface<T> listContainer, boolean disableNext) {
    this.container = listContainer;
    this.disableNext = disableNext;
    //System.out.println("Disable next " + disableNext);
    makePrevButton(exerciseShell);
    makeNextButton(exerciseShell);
    addStyleName("topFiveMargin");
    addStyleName("marginBottomTen");
  }

  private void makePrevButton(final T exercise) {
    this.prev = new Button("Previous");
    prev.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickPrev();
      }
    });
    prev.setEnabled(!container.onFirst(exercise));
    prev.setType(ButtonType.SUCCESS);

    add(prev);
  }

  private void makeNextButton(final T exercise) {
    this.next = new Button("Next");
    next.setType(ButtonType.SUCCESS);
    next.setEnabled(!disableNext || !container.onLast(exercise));

    add(next);

    DOM.setElementAttribute(next.getElement(), "id", "nextButton");

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
  protected void clickNext() {
    if (next.isEnabled() && next.isVisible()) {
      boolean onLast = container.loadNext();
      next.setEnabled(!disableNext || !onLast);
      if (!container.onFirst()) prev.setEnabled(true);
    }
  }
}
