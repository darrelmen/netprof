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

package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.constants.Trigger;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.HasBlurHandlers;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.recorder.RecordButton;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/2/13
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class BasicDialog {
  private final Logger logger = Logger.getLogger("BasicDialog");

  private static final boolean DEBUG = false;

 // private static final int ILR_CHOICE_WIDTH = 80;
  static final String TRY_AGAIN = "Try Again";

  public FormField addControlFormField(Panel dialogBox, String label) {
    return addControlFormField(dialogBox, label, false, 0, 30, "", -1);
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword,
                                          int minLength, int maxLength, String hint, int optWidth) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    if (optWidth>0) user.setWidth(optWidth + "px");
    user.setMaxLength(maxLength);
    return getSimpleFormField(dialogBox, label, user, minLength);
  }

  protected FormField addControlFormFieldHorizontal(Panel dialogBox,
                                                    String label,
                                                    String subtext,
                                                    boolean isPassword,
                                                    int minLength,
                                                    Widget rightSide,
                                                    int labelWidth,
                                                    int optWidth) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    if (optWidth>0) user.setWidth(optWidth + "px");

    user.getElement().setId("textBox");
    Panel row = new HorizontalPanel();
    row.add(user);
    row.add(rightSide);
    final ControlGroup userGroup = addControlGroupEntryHorizontal(dialogBox, label, row, labelWidth, subtext);

    FormField formField = new FormField(user, userGroup, minLength);
   // formField.setRightSide(rightSide);
    return formField;
  }

  protected FormField getSimpleFormField(Panel dialogBox,
                                         String label,
                                         TextBoxBase user,
                                         int minLength) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user, "");
    return new FormField(user, userGroup, minLength);
  }

  /**
   * @see #getSimpleFormField(Panel, TextBox, int)
   * @param dialogBox
   * @param widget
   * @return
   */
  private ControlGroup addControlGroupEntryNoLabel(Panel dialogBox, Widget widget) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    widget.addStyleName("leftFiveMargin");

    userGroup.add(widget);
    dialogBox.add(userGroup);
    return userGroup;
  }

  /**
   * Make a control group with a label, a widget, and an optional hint under the widget.
   *
   * @param dialogBox
   * @param label
   * @param widget
   * @param hint      if empty, skips adding it.
   * @return
   * @see mitll.langtest.client.user.UserDialog#getSimpleFormField(com.google.gwt.user.client.ui.Panel, String, com.github.gwtbootstrap.client.ui.base.TextBoxBase, int)
   */
  protected ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget widget, String hint) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    ControlLabel labelWidget = new ControlLabel(label);
    labelWidget.getElement().setId("Label_" + label);
    userGroup.add(labelWidget);
    widget.addStyleName("leftFiveMargin");

    if (hint.isEmpty()) {
      userGroup.add(widget);
    } else {
      Panel vert = new VerticalPanel();

      HTML hint1 = getHint(hint);
      vert.add(widget);
      vert.add(hint1);

      userGroup.add(vert);
    }
    dialogBox.add(userGroup);
    return userGroup;
  }

  private HTML getHint(String hint) {
    HTML hint1 = new HTML(hint);
    hint1.getElement().getStyle().setProperty("fontSize", "smaller");
    hint1.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);
    return hint1;
  }

  private ControlGroup addControlGroupEntryHorizontal(Panel dialogBox, String label, Widget widget, int labelWidth, String subtext) {
    final ControlGroup userGroup = new ControlGroup();

    final HorizontalPanel hp = new HorizontalPanel();
    hp.addStyleName("leftFiveMargin");
    Heading labelHeading = getLabel(label, labelWidth, subtext);

    hp.add(labelHeading);
    hp.add(widget);
    userGroup.add(hp);

    dialogBox.add(userGroup);
    return userGroup;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#makeRegularAudioPanel(Panel)
   * @paramx dialogBox
   * @param label
   * @paramx widget
   * @return
   */
  private Heading getLabel(String label, int labelWidth, String subtext) {
    Heading labelHeading = new Heading(6, label, subtext);
    labelHeading.getElement().getStyle().setPadding(0, Style.Unit.PX);
    labelHeading.getElement().getStyle().setMargin(0, Style.Unit.PX);
    labelHeading.setWidth(labelWidth + "px");
    return labelHeading;
  }

  protected ControlGroup addControlGroupEntrySimple(Panel dialogBox, String label, Widget widget) {
    final ControlGroup userGroup = new ControlGroup();
    if (!label.isEmpty()) {
      userGroup.add(new ControlLabel(label));
    }
    widget.addStyleName("leftFiveMargin");
    userGroup.add(widget);

    dialogBox.add(userGroup);
    return userGroup;
  }

  /**
   * @param dialogBox add to this container
   * @param label
   * @param widget
   * @param rightSide
   * @return
   */
  protected ControlGroup addControlGroupEntrySimple(Panel dialogBox, String label, Widget widget, Widget rightSide) {
    final ControlGroup userGroup = addControlGroup(label, widget, rightSide);

    dialogBox.add(userGroup);
    return userGroup;
  }

  private ControlGroup addControlGroup(String label, Widget leftSide, Widget rightSide) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.add(new ControlLabel(label));
    leftSide.addStyleName("leftFiveMargin");

    Panel row = new DivWidget();
    row.add(leftSide);
    row.add(rightSide);
    userGroup.add(row);
    return userGroup;
  }

  protected void markError(FormField dialectGroup, String message) {
    if (DEBUG) logger.info("mark error " + message + " on " + dialectGroup.getWidget().getElement().getId());
    markError(dialectGroup, TRY_AGAIN, message);
  }

  protected void markError(FormField dialectGroup, String header, String message) {
    markError(dialectGroup.group, dialectGroup.box, dialectGroup.box, header, message, Placement.RIGHT);
  }

  void markErrorBlur(FormField dialectGroup, String header, String message, Placement right, boolean setFocus) {
    markErrorBlur(dialectGroup.group, dialectGroup.box, header, message, right, setFocus);
  }

  /**
   * @seex EditableExerciseDialog#checkForForeignChange
   * @param dialectGroup
   * @param header
   * @param message
   */
  public void markError(ControlGroup dialectGroup, String header, String message) {
    markError(dialectGroup, header, message, Placement.RIGHT);
  }

  protected void markError(ControlGroup dialectGroup, Widget dialect, Focusable focusable, String header, String message) {
    markError(dialectGroup, dialect, focusable, header, message, Placement.RIGHT);
  }

  protected void markError(ControlGroup dialectGroup, Widget dialect, Focusable focusable, String header, String message, Placement right) {
    dialectGroup.setType(ControlGroupType.ERROR);
    focusable.setFocus(true);
    setupPopoverThatHidesItself(dialect, header, message, right);
  }

  /**
   *
   * @param dialectGroup
   * @param dialect
   * @param header
   * @param message
   * @param right
   * @param setFocus
   */
  void markErrorBlur(ControlGroup dialectGroup, FocusWidget dialect, String header, String message, Placement right, boolean setFocus) {
    if (DEBUG) logger.info("markErrorBlur " +header + " message " +message);

    dialectGroup.setType(ControlGroupType.ERROR);
    if (setFocus) dialect.setFocus(true);
    setupPopoverBlur(dialect, header, message, right, new MyPopover(), dialectGroup, setFocus);
  }

/*
  boolean highlightIntegerBox(FormField ageEntryGroup, int min, int max, int exception) {
    String text = ageEntryGroup.box.getSafeText();
    boolean validAge = false;
    if (text.length() == 0) {
      ageEntryGroup.group.setType(ControlGroupType.WARNING);
    } else {
      try {
        int age = Integer.parseInt(text);
        validAge = (age >= min && age <= max) || age == exception;
        ageEntryGroup.group.setType(validAge ? ControlGroupType.NONE : ControlGroupType.ERROR);
      } catch (NumberFormatException e) {

        // if (DEBUG) logger.info("marked error on " + ageEntryGroup);
        ageEntryGroup.group.setType(ControlGroupType.ERROR);
      }
    }

    return validAge;
  }
*/

  boolean isValidAge(FormField ageEntryGroup) {
    return isValidAge(ageEntryGroup, UserDialog.MIN_AGE, UserDialog.MAX_AGE + 1);
  }

  private boolean isValidAge(FormField ageEntryGroup, int min, int max) {
    String text = ageEntryGroup.box.getText();
    if (text.length() == 0) {
      return false;
    } else {
      try {
        int age = Integer.parseInt(text);
        return (age >= min && age <= max);
      } catch (NumberFormatException e) {
        return false;
      }
    }
  }

  /**
   * @param dialectGroup
   * @param header
   * @param message
   * @seex mitll.langtest.client.custom.dialog.EditableExercise#checkForForeignChange()
   */
  private void markError(ControlGroup dialectGroup, String header, String message, Placement placement) {
    dialectGroup.setType(ControlGroupType.ERROR);
    setupPopoverThatHidesItself(dialectGroup.getWidget(1), header, message, placement);
  }

  void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message, Placement placement) {
    // if (DEBUG) logger.info("markError on '" + dialect.getElement().getExID() + "' with " + header + "/" + message);
    dialectGroup.setType(ControlGroupType.ERROR);
    dialect.setFocus(true);
//    setupPopover(dialect, header, message, placement);
    Widget widget = dialect;

    try {
      widget = dialectGroup.getWidget(1);
    } catch (Exception e) {
      //if (DEBUG) logger.info("no nested object...");
    }
    setupPopoverThatHidesItself(widget, header, message, placement);
  }

  void markErrorBlur(FocusWidget focusWidget, String message, Placement placement) {
    markErrorBlurFocus(focusWidget, focusWidget, "Try Again", message, placement, false);
  }

  void markErrorBlur(Button button, String message) {
    markErrorBlurFocus(button, button, "Try Again", message, Placement.RIGHT, false);
  }

  void markErrorBlur(Button button, String heading, String message, Placement placement) {
    markErrorBlurFocus(button, button, heading, message, placement, false);
  }

  Popover markErrorBlur(FocusWidget button, String heading, String message, Placement placement) {
    return markErrorBlurFocus(button, button, heading, message, placement, false);
  }

  private Popover markErrorBlurFocus(Widget widget, HasBlurHandlers dialect, String heading, String message,
                                     Placement placement, boolean showOnlyOnce) {
    // if (DEBUG) logger.info("markError on '" + dialect.getElement().getExID() + "' with " + header + "/" + message);
    // dialect.setFocus(true);
//    setupPopover(dialect, header, message, placement);
    return setupPopoverBlurNoControl(widget, dialect, heading, message, placement, new MyPopover(), true);
  }

  private void setupPopoverThatHidesItself(final Widget w, String heading, final String message, Placement placement) {
    if (DEBUG) logger.info("\tsetupPopoverThatHidesItself triggering popover on '" + w.getTitle() + "' with " + heading + "/" + message);
    setupPopover(w, heading, message, placement);
  }

  private void setupPopover(Widget w, String heading, String message, Placement placement) {
    int delayMillis = 3000;
    setupPopover(w, heading, message, placement, delayMillis, false);
  }

  protected Popover setupPopover(Widget w, String heading, String message, Placement placement, int delayMillis, boolean isHTML) {
    final MyPopover popover = new MyPopover();

    return setupPopover(w, heading, message, placement, delayMillis, popover, isHTML);
  }

  Popover setupPopover(Widget w, String heading, String message, Placement placement, int delayMillis, final MyPopover popover, boolean isHTML) {
    configurePopup(popover, w, heading, message, placement, isHTML, true);

    Timer t = new Timer() {
      @Override
      public void run() {
        popover.dontFireAgain();
      }
    };
    t.schedule(delayMillis);
    return popover;
  }

  private Popover setupPopoverBlur(FocusWidget w, String heading, String message, Placement placement, final MyPopover popover, final ControlGroup dialectGroup, boolean requestFocus) {
    configurePopup(popover, w, heading, message, placement, true, requestFocus);

    w.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if (DEBUG) logger.info("got blur, dismissing popover...");
        popover.dontFireAgain();
        dialectGroup.setType(ControlGroupType.NONE);
      }
    });

    return popover;
  }

  private Popover setupPopoverBlurNoControl(Widget widget, HasBlurHandlers hasBlurHandlers, String heading, String message, Placement placement, final MyPopover popover, boolean requestFocus) {
    configurePopup(popover, widget, heading, message, placement, true, requestFocus);

    hasBlurHandlers.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if (DEBUG) logger.info("got blur, dismissing popover, with no control.");

        popover.dontFireAgain();
      }
    });

    return popover;
  }

  /**
   * TODO : bug - once shown these never really go away
   *
   * @param w
   * @param heading
   * @param message
   * @param placement
   * @param isHTML
   * @param requestFocus
   * @seex UserPassLogin#getSignUpForm
   */
  void setupPopover(final FocusWidget w, String heading, final String message, Placement placement, boolean isHTML, boolean requestFocus) {
    if (DEBUG) logger.info(" : setupPopover (bad)   : triggering popover on " + w.getElement().getId() + " with " + heading + "/" + message);
    final Popover popover = new Popover();
    configurePopup(popover, w, heading, message, placement, isHTML, requestFocus);

    if (requestFocus) requestFocus(w);
  }

  private void configurePopup(Popover popover, Widget w, String heading, String message, Placement placement, boolean isHTML, boolean requestFocus) {
    if (DEBUG) logger.info("configurePopup : triggering popover on " + w.getElement().getId() + " with " + heading + "/" + message + " " + placement);

    if (requestFocus && w instanceof Focusable) {
      requestFocus((Focusable) w);
    }
    showPopover(popover, w, heading, message, placement, isHTML);
  }

  private void requestFocus(final Focusable w) {
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        w.setFocus(true);
      }
    });
  }

  /**
   * @see RecordButton#showTooLoud()
   * @param w
   * @param heading
   * @param message
   * @param placement
   */
  public void showPopover(Widget w, String heading, String message, Placement placement) {
    showPopover(new Popover(), w, heading, message, placement, true);
  }

  /**
   *
   * @param popover
   * @param w
   * @param heading
   * @param message
   * @param placement
   * @param isHTML
   */
  private void showPopover(Popover popover, Widget w, String heading, String message, Placement placement, boolean isHTML) {
    if (DEBUG) logger.info("showPopover : triggering popover on " + w.getElement().getId() + " with " + heading + "/" + message + " " + placement);

    simplePopover(popover, w, heading, message, placement, isHTML);
    popover.show();
  }

  private void simplePopover(Popover popover, Widget w, String heading, String message, Placement placement, boolean isHTML) {
    if (DEBUG) logger.info("simplePopover : triggering popover on " + w.getElement().getId() + " with " + heading + "/" + message + " " + placement);
    popover.setWidget(w);
    popover.setHtml(isHTML);
    popover.setText(message);
    if (heading != null) {
      if (DEBUG) logger.info("simplePopover : set heading " + heading);
      popover.setHeading(heading);
    }
    popover.setPlacement(placement);

    popover.setTrigger(Trigger.HOVER);
    popover.setAnimation(false);
    popover.reconfigure();
    if (heading == null) {
      popover.getWidget().getElement().removeAttribute("data-original-title");
    }
  }

  FormField addControlFormFieldWithPlaceholder(Panel dialogBox, boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    user.setMaxLength(maxLength);
    user.setPlaceholder(hint);
    return getSimpleFormField(dialogBox, user, minLength);
  }

  private final Map<Widget, Popover> widgetToPopover = new HashMap<Widget, Popover>();

  void addPopover(Widget widget, String header, String htmlStr) {
    Popover popover = widgetToPopover.get(widget);
    if (popover == null) {
      widgetToPopover.put(widget, popover = new Popover());
      popover.setHtml(true);
      popover.setPlacement(Placement.LEFT);
      popover.setWidget(widget);
      //   popover.setShowDelay(600);
      popover.setTrigger(Trigger.HOVER);
      popover.setAnimation(false);
    }
    popover.setHeading(header);
    popover.setText(htmlStr);
    popover.reconfigure();
  }

  protected FormField getSimpleFormField(Panel dialogBox, TextBox user, int minLength) {
    final ControlGroup userGroup = addControlGroupEntryNoLabel(dialogBox, user);
    return new FormField(user, userGroup, minLength);
  }

  static class MyPopover extends Popover {
    public MyPopover() {
    }
    public void dontFireAgain() {
      hide();
      setTrigger(Trigger.MANUAL);
      reconfigure();
    }
  }

  /**
   * @param w
   * @param tip
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getDeleteButton(String, com.google.gwt.event.dom.client.ClickHandler)
   */
  protected Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }
}
