/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.Shell;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/9/14.
 */
public class PrevNextList<T extends Shell> extends HorizontalPanel {
  private final EventRegistration controller;
  private Button prev, next;
  private final ListInterface<T,?> container;
  private boolean disableNext = true;

  /**
   * @param exerciseShell
   * @param listContainer
   * @param disableNext
   * @param controller
   * @see EditableExerciseDialog#addFields
   */
  PrevNextList(final T exerciseShell, ListInterface<T,?> listContainer, boolean disableNext, EventRegistration controller) {
    this.container = listContainer;
    this.disableNext = disableNext;
    this.controller = controller;
    makePrevButton(exerciseShell);
    makeNextButton(exerciseShell);
    addStyleName("marginBottomTen");
    getElement().setId("PrevNextList");
  }

  public void makePrevButton(final T exercise) {
    this.prev = new Button("Previous");
    prev.getElement().setId("PrevNextList_Previous");
   // controller.register(prev, exercise == null ? "" :exercise.getID());
    prev.setType(ButtonType.SUCCESS);
    prev.setEnabled(!container.onFirst(exercise));
    prev.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickPrev();
      }
    });

    add(prev);
  }

  public void makeNextButton(final T exercise) {
    this.next = new Button("Next");
    next.getElement().setId("nextButton");
    controller.register(next, exercise.getID());

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

  protected void clickPrev() {
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
