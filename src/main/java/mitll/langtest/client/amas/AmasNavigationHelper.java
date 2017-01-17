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

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.Shell;

/**
 * A row with prev/next buttons.  Key bindings for keys too.
 * Has confirm dialog appear on next button in some modes.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/6/13
 */
class AmasNavigationHelper extends HorizontalPanel {
  private static final String CONTINUE = "Continue";

  private Button next;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private final PostAnswerProvider provider;
  private final ListInterface listContainer;

  AmasNavigationHelper(Shell exercise, ExerciseController controller, PostAnswerProvider provider,
                              ListInterface listContainer, boolean addButtons,
                              boolean enableNextOnlyWhenAllCompleted) {
    this.provider = provider;
    this.listContainer = listContainer;
    this.enableNextOnlyWhenAllCompleted = enableNextOnlyWhenAllCompleted;
    setSpacing(5);
    getNextAndPreviousButtons(exercise, controller, addButtons);
    getElement().setId("AmasNavigationHelper");
    addStyleName("rightTenMargin");
    controller.register(next, exercise.getID());
  }

  /**
   * @paramZ addKeyHandler
   * @param e
   * @param controller
   * @param addButtons
   * @see AmasNavigationHelper#AmasNavigationHelper
   */
  private void getNextAndPreviousButtons(final Shell e,
                                         final ExerciseController controller, boolean addButtons) {
    makeNextButton(e, controller, addButtons);
  }

  private void makeNextButton(final Shell exercise, final ExerciseController controller, boolean addButtons) {
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

  private void enableNext(Shell exercise) {
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
  private void clickNext(final ExerciseController controller, final Shell exercise) {
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
