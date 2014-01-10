package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.shared.ExerciseShell;

/**
* Created by GO22670 on 1/9/14.
*/
class PrevNext<T extends ExerciseShell> extends HorizontalPanel {
  private Button prev, next;
  private PagingContainer<T> container;

  public PrevNext(final T exerciseShell, PagingContainer<T> listContainer) {
    this.container = listContainer;
    makePrevButton(exerciseShell);
    makeNextButton(exerciseShell);
    addStyleName("marginBottomTen");
  }

  private void makePrevButton(final T exercise) {
    this.prev = new Button("Previous");
    prev.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickPrev();
      }
    });
    prev.setEnabled(!container.isFirst(exercise));
    prev.setType(ButtonType.SUCCESS);

    add(prev);
  }

  private void makeNextButton(final T exercise) {
    this.next = new Button("Next");
    next.setType(ButtonType.SUCCESS);
    next.setEnabled(!container.isLast(exercise));

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
      next.setEnabled(!onLast);
      if (container.getSize() != 1) prev.setEnabled(true);
    }
  }
}
