package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.constants.Trigger;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.TooltipHelper;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class BasicDialog {
  public static final int ILR_CHOICE_WIDTH = 80;
  public static final int MIN_LENGTH_USER_ID = 8;
  protected static final String TRY_AGAIN = "Try Again";
  static final String UNSET = "GENDER";
  private static final String MALE = "Male";
  private static final String FEMALE = "Female";

  protected FormField addControlFormField(Panel dialogBox, String label) {
    return addControlFormField(dialogBox, label, false, 0, 30, "");
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    user.setMaxLength(maxLength);
    return getFormField(dialogBox, label, user, minLength);
  }

  protected FormField addControlFormFieldHorizontal(Panel dialogBox, String label, String subtext, boolean isPassword, int minLength,
                                                    Widget rightSide, int labelWidth) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();

    user.getElement().setId("textBox");
    Panel row = new HorizontalPanel();
    row.add(user);
    row.add(rightSide);
    final ControlGroup userGroup = addControlGroupEntryHorizontal(dialogBox, label, row, labelWidth, subtext);

    FormField formField = new FormField(user, userGroup, minLength);
    formField.setRightSide(rightSide);
    return formField;
  }

  protected FormField getFormField(Panel dialogBox, String label, TextBoxBase user, int minLength) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user, "");
    return new FormField(user, userGroup,minLength);
  }

/*  ListBoxFormField getListBoxFormField(Panel dialogBox, String label, ListBox user) {
    ControlGroup group = addControlGroupEntry(dialogBox, label, user, "");
    return new ListBoxFormField(user, group);
  }

  ListBoxFormField getListBoxFormFieldNoLabel(Panel dialogBox, String label, ListBox user) {
    ControlGroup group = addControlGroupEntryNoLabel(dialogBox, user);
    return new ListBoxFormField(user, group);
  }*/

  protected ControlGroup addControlGroupEntryNoLabel(Panel dialogBox, Widget widget) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    widget.addStyleName("leftFiveMargin");

    userGroup.add(widget);
    dialogBox.add(userGroup);
    return userGroup;
  }


  protected ControlGroup addControlGroupEntryNoLabel(Widget widget) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    widget.addStyleName("leftFiveMargin");

    userGroup.add(widget);
    //dialogBox.add(userGroup);
    return userGroup;
  }



  /**
   * Make a control group with a label, a widget, and an optional hint under the widget.
   *
   * @param dialogBox
   * @param label
   * @param widget
   * @param hint if empty, skips adding it.
   * @return
   * @see mitll.langtest.client.user.UserDialog#getFormField(com.google.gwt.user.client.ui.Panel, String, com.github.gwtbootstrap.client.ui.base.TextBoxBase, int)
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

      HTML hint1 = new HTML(hint);
      hint1.getElement().getStyle().setProperty("fontSize", "smaller");
      hint1.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);

      vert.add(widget);
      vert.add(hint1);

      userGroup.add(vert);
    }
    dialogBox.add(userGroup);
    return userGroup;
  }

  protected ControlGroup addControlGroupEntryHorizontal(Panel dialogBox, String label, Widget widget, int labelWidth, String subtext) {
    final ControlGroup userGroup = new ControlGroup();

    final HorizontalPanel hp = new HorizontalPanel();
    hp.addStyleName("leftFiveMargin");
    Heading w = new Heading(6, label,subtext);
    w.getElement().getStyle().setPadding(0, Style.Unit.PX);
    w.getElement().getStyle().setMargin(0, Style.Unit.PX);

    hp.add(w);
    w.setWidth(labelWidth + "px");
    hp.add(widget);
    userGroup.add(hp);

    dialogBox.add(userGroup);
    return userGroup;
  }

  protected ControlGroup addControlGroupEntrySimple(Panel dialogBox, String label, Widget widget) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.add(new ControlLabel(label));
    widget.addStyleName("leftFiveMargin");
    userGroup.add(widget);

    dialogBox.add(userGroup);
    return userGroup;
  }

  /**
   *
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

  protected ControlGroup addControlGroup(String label, Widget leftSide, Widget rightSide) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.add(new ControlLabel(label));
    leftSide.addStyleName("leftFiveMargin");

    Panel row = new DivWidget();
    row.add(leftSide);
    row.add(rightSide);
    userGroup.add(row);
    return userGroup;
  }

  ListBox getListBox2(Collection<String> values) {
    return getListBox2(values, BasicDialog.ILR_CHOICE_WIDTH);
  }

  ListBox getListBox2(Collection<String> values, int ilrChoiceWidth) {
    final ListBox listBox = new ListBox(false);
    for (String s : values) {
      listBox.addItem(s);
    }
    listBox.setWidth(ilrChoiceWidth + "px");
    return listBox;
  }

  protected void markError(FormField dialectGroup, String message) {
    System.out.println("mark error " + message + " on " +dialectGroup.getWidget().getElement().getId());
    markError(dialectGroup, TRY_AGAIN, message);
  }


  protected void markError(FormField dialectGroup, String header, String message) {
    markError(dialectGroup.group, dialectGroup.box, dialectGroup.box, header, message, Placement.RIGHT);
  }

  protected void markError(FormField dialectGroup, String header, String message, Placement right) {
    markError(dialectGroup.group, dialectGroup.box, dialectGroup.box, header, message, right);
  }

  protected void markError(ControlGroup dialectGroup, String header, String message) {
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
/*
  boolean highlightIntegerBox(FormField ageEntryGroup, int min, int max) {
    return highlightIntegerBox(ageEntryGroup, min, max, Integer.MAX_VALUE);
  }*/

/*  boolean highlightIntegerBox(FormField ageEntryGroup) {
    return highlightIntegerBox(ageEntryGroup, 7, 97, -1);
  }*/

  boolean highlightIntegerBox(FormField ageEntryGroup, int min, int max, int exception) {
    String text = ageEntryGroup.box.getText();
    boolean validAge = false;
    if (text.length() == 0) {
      ageEntryGroup.group.setType(ControlGroupType.WARNING);
    } else {
      try {
        int age = Integer.parseInt(text);
        validAge = (age >= min && age <= max) || age == exception;
        ageEntryGroup.group.setType(validAge ? ControlGroupType.NONE : ControlGroupType.ERROR);
      } catch (NumberFormatException e) {
        ageEntryGroup.group.setType(ControlGroupType.ERROR);
      }
    }

    return validAge;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.EditableExercise#checkForForeignChange()
   * @param dialectGroup
   * @param header
   * @param message
   */
  protected void markError(ControlGroup dialectGroup, String header, String message, Placement placement) {
    dialectGroup.setType(ControlGroupType.ERROR);
    setupPopoverThatHidesItself(dialectGroup.getWidget(1), header, message, placement);
  }

  void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message) {
    markError(dialectGroup, dialect, header, message, Placement.RIGHT);
  }

  void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message, Placement placement) {
   // System.out.println("markError on '" + dialect.getElement().getId() + "' with " + header + "/" + message);
    dialectGroup.setType(ControlGroupType.ERROR);
    dialect.setFocus(true);
//    setupPopover(dialect, header, message, placement);
    Widget widget = dialect;

    try {
      widget = dialectGroup.getWidget(1);
    } catch (Exception e) {
      //System.out.println("no nested object...");
    }
    setupPopoverThatHidesItself(widget, header, message, placement);
  }

  void markError(Widget dialect, String message) {
    // System.out.println("markError on '" + dialect.getElement().getId() + "' with " + header + "/" + message);
   // dialect.setFocus(true);
//    setupPopover(dialect, header, message, placement);
    Widget widget = dialect;


    setupPopoverThatHidesItself(widget, "Error", message, Placement.RIGHT);
  }

  void markError(Widget dialect, String header, String message, Placement placement) {
    // System.out.println("markError on '" + dialect.getElement().getId() + "' with " + header + "/" + message);
    Widget widget = dialect;
    setupPopoverThatHidesItself(widget, header, message, placement);
  }

  private void setupPopoverThatHidesItself(final Widget w, String heading, final String message,Placement placement) {
//    System.out.println("\ttriggering popover on '" + w.getTitle() + "' with " + heading + "/" + message);
    setupPopover(w, heading, message, placement);
  }

  protected void setupPopover(Widget w, String heading, String message, Placement placement) {
    int delayMillis = 3000;
    setupPopover(w, heading, message, placement, delayMillis);
  }

  protected Popover setupPopover(Widget w, String heading, String message, Placement placement, int delayMillis) {
    final MyPopover popover = new MyPopover();

    return setupPopover(w, heading, message, placement, delayMillis, popover);
  }

  protected Popover setupPopover(Widget w, String heading, String message, Placement placement, int delayMillis, final MyPopover popover) {
    configurePopup(popover, w, heading, message, placement, false);

    Timer t = new Timer() {
      @Override
      public void run() {
        popover.dontFireAgain();
      }
    };
    t.schedule(delayMillis);
    return popover;
  }

  /**
   * TODO : bug - once shown these never really go away
   * @param w
   * @param heading
   * @param message
   * @param placement
   * @param isHTML
   */
  void setupPopover(final FocusWidget w, String heading, final String message, Placement placement, boolean isHTML) {
    System.out.println(new Date() + " : setupPopover   : triggering popover on " + w.getElement().getId() + " with " + heading +"/"+message);
    final Popover popover = new Popover();
    configurePopup(popover, w, heading, message, placement, isHTML);

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        w.setFocus(true);
      }
    });
  }

  private void configurePopup(Popover popover, Widget w, String heading, String message, Placement placement, boolean isHTML) {
  //  System.out.println(new Date() + "configurePopup : triggering popover on " + w.getElement().getId() + " with " + heading +"/"+message + " " + placement);

    popover.setWidget(w);
    popover.setHtml(isHTML);
    popover.setText(message);
    popover.setHeading(heading);
    popover.setPlacement(placement);
    popover.reconfigure();
    popover.show();
  }

/*  ListBox getGenderBox() {
    List<String> values = Arrays.asList("GENDER", MALE, FEMALE);
    return getListBox(values);
  }*/

  protected ListBox getListBox(List<String> values) {
    final ListBox genderBox = new ListBox(false);
    for (String s : values) {
      genderBox.addItem(s);
    }
    return genderBox;
  }

  protected FormField addControlFormFieldWithPlaceholder(Panel dialogBox, boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    user.setMaxLength(maxLength);
    user.setPlaceholder(hint);
    return getFormField(dialogBox, user, minLength);
  }

/*
  protected FormField addControlFormFieldWithPlaceholder(boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    user.setMaxLength(maxLength);
    user.setPlaceholder(hint);
    final ControlGroup userGroup = addControlGroupEntryNoLabel(user);
    return new FormField(user, userGroup, minLength);
  }
*/

  private FormField getFormField(Panel dialogBox, TextBox user, int minLength) {
    final ControlGroup userGroup = addControlGroupEntryNoLabel(dialogBox, user);
    return new FormField(user, userGroup, minLength);
  }

  protected boolean highlightIntegerBox(FormField ageEntryGroup) {
    return highlightIntegerBox(ageEntryGroup, UserDialog.MIN_AGE, UserDialog.MAX_AGE+1, UserDialog.TEST_AGE);
  }

  protected static class MyPopover extends Popover {
    public void dontFireAgain() {
      hide();
      setTrigger(Trigger.MANUAL);
      reconfigure();
    }
  }

  protected class FormField {
    public final TextBoxBase box;
    public final ControlGroup group;
    public Widget rightSide;

    public FormField(final TextBoxBase box, final ControlGroup group, final int minLength) {
      this.box = box;

      box.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          if (box.getText().length() >= minLength) {
            group.setType(ControlGroupType.NONE);
            //hidePopovers();
          }
        }
      });

      this.group = group;
    }

  /*  public void clearError() {
      group.setType(ControlGroupType.NONE);
      //hidePopovers();
    }*/

    public void setVisible(boolean visible) {
      group.setVisible(visible);
    }
    public String getText() { return box.getText(); }
    public void setRightSide(Widget rightSide) { this.rightSide = rightSide; }
    public ControlGroup getGroup() {
      return group;
    }
    public Widget getWidget() {  return box;    }
    public String toString() { return "FormField value " + getText(); }
  }

/*  protected class ListBoxFormField {
    public final ListBox box;
    private final ControlGroup group;

    public ListBoxFormField(final ListBox box, ControlGroup group) {
      this.group = group;
      this.box = box;
    }

    public String getValue() {
      return box.getItemText(box.getSelectedIndex());
    }

    void markSimpleError(String message, Placement placement) {
      box.setFocus(true);
      setupPopover(box, TRY_AGAIN, message, placement, false);
    }

    public void setVisible(boolean v) { box.setVisible(v); group.setVisible(v); }

    public String toString() {
      return "Box: " + getValue();
    }
  }*/

  /**
   * @see StudentDialog#displayLoginBox()
   * @param w
   * @param tip
   * @return
   */
  protected Tooltip addTooltip(Widget w, String tip) {
//    System.out.println("adding " +tip + " to " +w.getElement().getId());
    return new TooltipHelper().addTooltip(w, tip);
  }
}
