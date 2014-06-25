package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.constants.Trigger;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class BasicDialog {
  protected static final String TRY_AGAIN = "Try Again";
 // private final List<Popover> visiblePopovers = new ArrayList<Popover>();

  protected FormField addControlFormField(Panel dialogBox, String label) {
    return addControlFormField(dialogBox, label, false, 0, 30, "");
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
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

  ListBoxFormField getListBoxFormField(Panel dialogBox, String label, ListBox user) {
    addControlGroupEntry(dialogBox, label, user, "");
    return new ListBoxFormField(user);
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
    w.setWidth(labelWidth +"px");
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
    return getListBox2(values, StudentDialog.ILR_CHOICE_WIDTH);
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
    markError(dialectGroup.group, dialectGroup.box, dialectGroup.box, header, message);
  }
  protected void markError(ControlGroup dialectGroup, String header, String message) {
    markError(dialectGroup, header, message,Placement.RIGHT);
  }

  protected void markError(ControlGroup dialectGroup, Widget dialect, Focusable focusable, String header, String message) {
    dialectGroup.setType(ControlGroupType.ERROR);
    focusable.setFocus(true);

    setupPopoverThatHidesItself(dialect, header, message, Placement.RIGHT);
  }

  boolean highlightIntegerBox(FormField ageEntryGroup, int min, int max) {
    return highlightIntegerBox(ageEntryGroup, min, max, Integer.MAX_VALUE);
  }

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
   * @see mitll.langtest.client.custom.EditableExercise#checkForForeignChange()
   * @param dialectGroup
   * @param header
   * @param message
   */
  protected void markError(ControlGroup dialectGroup, String header, String message, Placement placement) {
    dialectGroup.setType(ControlGroupType.ERROR);
    setupPopoverThatHidesItself(dialectGroup.getWidget(1), header, message,placement);
  }

  void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message) {
    markError(dialectGroup, dialect, header, message, Placement.RIGHT);
  }

  void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message, Placement placement) {
   // System.out.println("markError on '" + dialect.getElement().getId() + "' with " + header + "/" + message);
    dialectGroup.setType(ControlGroupType.ERROR);
    dialect.setFocus(true);
//    setupPopover(dialect, header, message, placement);
    setupPopoverThatHidesItself(dialectGroup.getWidget(1), header, message,placement);
  }

  private void setupPopoverThatHidesItself(final Widget w, String heading, final String message,Placement placement) {
    System.out.println("\ttriggering popover on '" + w.getTitle() + "' with " + heading + "/" + message);
    setupPopover(w, heading, message, placement);
  }

  protected void setupPopover(Widget w, String heading, String message, Placement placement) {
    int delayMillis = 3000;
    setupPopover(w, heading, message, placement, delayMillis);
  }

  protected void setupPopover(Widget w, String heading, String message, Placement placement, int delayMillis) {
    final MyPopover popover = new MyPopover();

    configurePopup(popover, w, heading, message, placement);

    Timer t = new Timer() {
      @Override
      public void run() {
        popover.dontFireAgain();
      }
    };
    t.schedule(delayMillis);
  }

  /**
   * TODO : bug - once shown these never really go away
   * @param w
   * @param heading
   * @param message
   * @param placement
   */
  void setupPopover(final FocusWidget w, String heading, final String message, Placement placement) {
    System.out.println("setupPopover   : triggering popover on " + w.getElement().getId() + " with " + heading +"/"+message);
    final Popover popover = new Popover();
    configurePopup(popover, w, heading, message, placement);
    //visiblePopovers.add(popover);

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        w.setFocus(true);
      }
    });
  }

  private void configurePopup(Popover popover, Widget w, String heading, String message, Placement placement) {
    System.out.println("configurePopup : triggering popover on " + w.getElement().getId() + " with " + heading +"/"+message + " " + placement);

    popover.setWidget(w);
    popover.setText(message);
    popover.setHeading(heading);
    popover.setPlacement(placement);
    popover.reconfigure();
    popover.show();
  }

/*  void hidePopovers() {
    if (!visiblePopovers.isEmpty()) {
         System.out.println("\n\n\n\thiding " + visiblePopovers.size() + " popovers");
    }
    for (Popover popover : visiblePopovers) popover.hide();
    visiblePopovers.clear();
  }*/

  private static class MyPopover extends Popover {
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

    public void clearError() {
      group.setType(ControlGroupType.NONE);
      //hidePopovers();
    }

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

  protected class ListBoxFormField {
    public final ListBox box;

    public ListBoxFormField(final ListBox box) {
      this.box = box;
/*
      box.addChangeHandler(new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
          hidePopovers();
        }
      });
*/
    }

    public String getValue() {
      return box.getItemText(box.getSelectedIndex());
    }

    void markSimpleError(String message, Placement placement) {
      box.setFocus(true);
  //    System.out.println("ListBoxFormField mark error " + message + " on " +box.getElement().getId());
      setupPopover(box, TRY_AGAIN, message, placement);
    }

    public String toString() {
      return "Box: " + getValue();
    }
  }

  /**
   * @see StudentDialog#displayLoginBox()
   * @param w
   * @param tip
   * @return
   */
  protected Tooltip addTooltip(Widget w, String tip) {
//    System.out.println("adding " +tip + " to " +w.getElement().getId());
    return new TooltipHelper().addTooltip(w, tip);  }
}
