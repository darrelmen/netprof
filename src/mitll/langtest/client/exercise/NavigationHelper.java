package mitll.langtest.client.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.Exercise;

import java.util.Date;

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
  private static final String RIGHT_ARROW_TOOLTIP = "Press enter to go to the next item.";
 // private static final String RIGHT_ARROW_TOOLTIP2 = "Press the right arrow key to go to the next item.";
  private static final String THE_FOREIGN_LANGUAGE = " the foreign language";

  private Button prev;
  private Button next;
  private HandlerRegistration keyHandler;
  private boolean debug = false;
  private boolean enableNextOnlyWhenAllCompleted = true;
  private PostAnswerProvider provider;

  boolean bindEnterKey = true;

  protected ListInterface listContainer;

  /**
   * @see ExercisePanel#getNavigationHelper(ExerciseController)
   * @param exercise
   * @param controller
   * @param provider
   * @param listContainer
   * @param addButtons
   */
  public NavigationHelper(Exercise exercise, ExerciseController controller, PostAnswerProvider provider,
                          ListInterface listContainer, boolean addButtons) {
    enableNextOnlyWhenAllCompleted = !getLanguage(controller).equalsIgnoreCase("Pashto");   // hack?
    this.provider = provider;
    this.listContainer = listContainer;
    setSpacing(5);
    getNextAndPreviousButtons(exercise, controller, addButtons);
    getElement().setId("NavigationHelper");
  }

  private String getLanguage( ExerciseController controller) {
    String language = controller.getLanguage();
    return (language == null || language.length() == 0) ? THE_FOREIGN_LANGUAGE : language;
  }

  /**
   * @see NavigationHelper#NavigationHelper(mitll.langtest.shared.Exercise, ExerciseController, PostAnswerProvider, ListInterface, boolean)
   * @param e
   * @param controller
   * @param addButtons
   */
  private void getNextAndPreviousButtons(final Exercise e,
                                         final ExerciseController controller, boolean addButtons) {
    boolean useKeyHandler = controller.isCollectAudio();

    makePrevButton(e, controller, addButtons);
    makeNextButton(e, controller, addButtons);

    // TODO : revisit in the context of text data collections
    addKeyHandler(e, controller, useKeyHandler);
  }

  private void makePrevButton(final Exercise e, ExerciseController controller, boolean addButtons) {
    this.prev = new Button("Previous");
    getPrev().addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickPrev(e);
      }
    });
    boolean onFirst = !listContainer.onFirst(e);
    getPrev().setEnabled(onFirst);
    if (false) getPrev().setTitle(LEFT_ARROW_TOOLTIP);
    getPrev().setType(ButtonType.SUCCESS);

    if (addButtons) add(getPrev());
    getPrev().setVisible(!controller.isMinimalUI() || !controller.isPromptBeforeNextItem());
  }

  private void makeNextButton(final Exercise e, final ExerciseController controller, boolean addButtons) {
    this.next = new Button(getNextButtonText());
    next.setType(ButtonType.SUCCESS);
    if (enableNextOnlyWhenAllCompleted) { // initially not enabled
      next.setEnabled(false);
    }
    else {
      next.setEnabled(!listContainer.onLast(e));
    }

    if (addButtons)  add(next);
    if (controller.getProps().isBindNextToEnter()) next.setTitle(RIGHT_ARROW_TOOLTIP);

    DOM.setElementAttribute(next.getElement(), "id", "nextButton");

    // send answers to server
    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        clickNext(controller, e);
      }
    });
  }

  public SimplePanel makeSpacer() {
    SimplePanel spacer = new SimplePanel();
    spacer.setSize("100%", "20px");
    return spacer;
  }

  private void clickPrev(Exercise e) {
    if (getPrev().isEnabled() && getPrev().isVisible()) {
      System.out.println("clickPrev " +keyHandler+ " click on prev " + getPrev());
      listContainer.loadPreviousExercise(e);
    }
  }

  /**
   * Ignore clicks or keyboard activity when the widget is not enabled.
   * @see #getNextAndPreviousButtons
   * @see #addKeyHandler
   * @param controller
   * @param e
   */
  protected void clickNext(final ExerciseController controller, final Exercise e) {
    if (next.isEnabled() && next.isVisible()) {
      if (controller.isMinimalUI() && !controller.isGrading() && controller.isPromptBeforeNextItem()) {
        showConfirmNextDialog(controller, e);
      } else {
        provider.postAnswers(controller, e);
      }
    }
    else {
      System.out.println("clickNext " +keyHandler+ " ignoring next");
    }
  }


  /**
   * Paul wanted a dialog that asks the user to confirm they want to move on to the next item.
   * @paramx service
   * @paramx userFeedback
   * @param controller
   * @param e
   */
  private void showConfirmNextDialog(final ExerciseController controller, final Exercise e) {
    final DialogBox dialogBox;
    Button yesButton;

    dialogBox = new DialogBox();
    dialogBox.setText("Please confirm");

    yesButton = new Button("Yes");
    yesButton.getElement().setId("yesButton");
    yesButton.setFocus(true);

    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new Label("Are you ready to move on to the next item?"));

    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    HorizontalPanel hp = new HorizontalPanel();
    hp.setSpacing(5);
    dialogVPanel.add(hp);
    hp.add(yesButton);
    Button no = new Button("No");
    no.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    hp.add(no);
    dialogBox.setWidget(dialogVPanel);

    yesButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        provider.postAnswers(controller, e);
        prev.setEnabled(!controller.getExerciseList().onFirst(e));
        dialogBox.hide();
      }
    });
    dialogBox.center();
  }

  protected String getNextButtonText() {
    return "Next";
  }

  /**
   * Enter key advances to next item, left arrow goes to previous
   * @param e
   * @param controller
   * @param useKeyHandler
   */
  private void addKeyHandler(final Exercise e, final ExerciseController controller, final boolean useKeyHandler) {
    keyHandler = Event.addNativePreviewHandler(new
                                                 Event.NativePreviewHandler() {

                                                   @Override
                                                   public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                     NativeEvent ne = event.getNativeEvent();
                                                     int keyCode = ne.getKeyCode();
                                                     boolean isLeft = keyCode == KeyCodes.KEY_LEFT;
                                                     //   boolean isRight = keyCode == KeyCodes.KEY_RIGHT;
                                                     boolean isEnter =
                                                        (bindEnterKey && keyCode == KeyCodes.KEY_ENTER) ||
                                                       (!bindEnterKey && keyCode == KeyCodes.KEY_RIGHT);
                                                     //   System.out.println("key code is " +keyCode);
                                                     if (((useKeyHandler && isLeft) || isEnter) && event.getTypeInt() == 512 &&
                                                       "[object KeyboardEvent]".equals(ne.getString())) {
                                                       ne.preventDefault();

                                                       if (debug) {
                                                         System.out.println(new Date() +
                                                           " : getNextAndPreviousButtons - key handler : " + keyHandler +
                                                           " Got " + event + " type int " +
                                                           event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                           " native " + event.getNativeEvent() +
                                                           " source " + event.getSource());
                                                       }

                                                       if (isLeft) {
                                                         clickPrev(e);
                                                       } else {
                                                         clickNext(controller, e);
                                                       }
                                                     }
                                                   }
                                                 });
   // System.out.println("addKeyHandler made click handler " + keyHandler);
  }

  public void removeKeyHandler() {
   // System.out.println("removeKeyHandler : " + keyHandler);

    if (keyHandler != null) keyHandler.removeHandler();
  }

  public void enableNextButton(boolean val) {
    next.setEnabled(val);
  }

  public void enablePrevButton(boolean val) {
    prev.setEnabled(val);
  }

  public void setButtonsEnabled(boolean val) {
    getPrev().setEnabled(val);
    next.setEnabled(val);
  }

  //public Widget getPrev() { return prev; }
  public Widget getNext() { return next; }
  public Button getPrev() { return prev; }
}
