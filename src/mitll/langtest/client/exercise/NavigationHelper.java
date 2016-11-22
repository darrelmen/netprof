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

package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.HasIcon;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.Shell;

import java.util.logging.Logger;

/**
 * A row with prev/next buttons.  Key bindings for keys too.
 * Has confirm dialog appear on next button in some modes.
 *
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/6/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class NavigationHelper<T extends Shell> extends HorizontalPanel {
  private Logger logger = Logger.getLogger("NavigationHelper");

  private static final String LEFT_ARROW_TOOLTIP = "Press the left arrow key to go to the previous item.";

  private Button prev;
  protected Button next;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private final PostAnswerProvider provider;
  private final ListInterface<T> listContainer;

  /**
   * @see ExercisePanel#getNavigationHelper(ExerciseController)
   * @param exercise
   * @param controller
   * @param provider
   * @param listContainer
   * @param addKeyHandler
   */
  protected NavigationHelper(HasID exercise, ExerciseController controller, PostAnswerProvider provider,
                          ListInterface<T> listContainer, boolean addKeyHandler) {
    this(exercise, controller, provider, listContainer, true, addKeyHandler, false);
  }

  public NavigationHelper(HasID exercise, ExerciseController controller, PostAnswerProvider provider,
                          ListInterface<T> listContainer, boolean addButtons, boolean addKeyHandler,
                          boolean enableNextOnlyWhenAllCompleted) {
    this.provider = provider;
    this.listContainer = listContainer;
    this.enableNextOnlyWhenAllCompleted = enableNextOnlyWhenAllCompleted;
    setSpacing(5);
    getNextAndPreviousButtons(exercise, controller, addButtons, addKeyHandler);
    getElement().setId("NavigationHelper");
    String id = exercise.getID();
    controller.register(prev, id);
    controller.register(next, id);
  }

  /**
   * @see NavigationHelper#NavigationHelper
   * @param e
   * @param controller
   * @param addButtons
   * @param addKeyHandler
   */
  private void getNextAndPreviousButtons(final HasID e,
                                         final ExerciseController controller, boolean addButtons, boolean addKeyHandler) {
    makePrevButton(e, addButtons, addKeyHandler);
    makeNextButton(e, controller, addButtons);
  }

  private void makePrevButton(final HasID exercise, boolean addButtons, boolean useKeyHandler) {
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

  private void makeNextButton(final HasID exercise, final ExerciseController controller, boolean addButtons) {
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

  /**
   * @see #makeNextButton(HasID, ExerciseController, boolean)
   * @param exercise
   */
  protected void enableNext(HasID exercise) {
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
     // logger.info("enableNextOnlyWhenAllCompleted true");
      next.setEnabled(false);
    }
    else {
      boolean b = listContainer.onLast(exercise);
   //   logger.info("enableNextOnlyWhenAllCompleted false on last = " +  b);
      next.setEnabled(!b);
    }
  }

  private void clickPrev(HasID e) {
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
  private void clickNext(ExerciseController controller, HasID exercise) {
    if (next.isEnabled() && next.isVisible()) {
      if (provider != null) {
        provider.postAnswers(controller, exercise);
      }
    }
  }

  private String getNextButtonText() {
    return "Next";
  }

  /**
   * @see ExercisePanel#enableNext
   * @param val
   */
  public void enableNextButton(boolean val) {
    next.setEnabled(val);
  }
  public Widget getNext() { return next; }
  private Button getPrev() { return prev; }
}
