package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.constants.Trigger;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

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
  private List<Popover> visiblePopovers = new ArrayList<Popover>();

  protected FormField addControlFormField(Panel dialogBox, String label) {
    return addControlFormField(dialogBox, label, false, 0);
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    return getFormField(dialogBox, label, user, minLength);
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength, Widget rightSide) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();

    Panel row = new HorizontalPanel();
    row.add(user);
    row.add(rightSide);
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, row);

    FormField formField = new FormField(user, userGroup, minLength);
    formField.setRightSide(rightSide);
    return formField;
  }

  protected FormField getFormField(Panel dialogBox, String label, TextBoxBase user, int minLength) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user);
    return new FormField(user, userGroup,minLength);
  }


  public ListBoxFormField getListBoxFormField(Panel dialogBox, String label, List<String> values) {
    return getListBoxFormField(dialogBox, label, getListBox2(values));
  }

  public ListBoxFormField getListBoxFormField(Panel dialogBox, String label, ListBox user) {
    addControlGroupEntry(dialogBox, label, user);
    return new ListBoxFormField(user);
  }

  public ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget widget) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    userGroup.add(new ControlLabel(label));
    widget.addStyleName("leftFiveMargin");
    userGroup.add(widget);

    dialogBox.add(userGroup);
    return userGroup;
  }
  public ControlGroup addControlGroupEntrySimple(Panel dialogBox, String label, Widget widget) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.add(new ControlLabel(label));
    widget.addStyleName("leftFiveMargin");
    userGroup.add(widget);

    dialogBox.add(userGroup);
    return userGroup;
  }

  protected ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget widget, Widget rightSide) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    userGroup.add(new ControlLabel(label));
    widget.addStyleName("leftFiveMargin");

    Panel row = new DivWidget();
    row.add(widget);
    row.add(rightSide);
    userGroup.add(row);

    dialogBox.add(userGroup);
    return userGroup;
  }
  protected ControlGroup addControlGroupEntrySimple(Panel dialogBox, String label, Widget widget, Widget rightSide) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.add(new ControlLabel(label));
    widget.addStyleName("leftFiveMargin");

    Panel row = new DivWidget();
    row.add(widget);
    row.add(rightSide);
    userGroup.add(row);

    dialogBox.add(userGroup);
    return userGroup;
  }

  protected ListBox getListBox2(Collection<String> values) {
    return getListBox2(values, StudentDialog.ILR_CHOICE_WIDTH);
  }

  protected ListBox getListBox2(Collection<String> values,int ilrChoiceWidth) {
    final ListBox listBox = new ListBox(false);
    for (String s : values) {
      listBox.addItem(s);
    }
    //int ilrChoiceWidth = ;
    listBox.setWidth(ilrChoiceWidth + "px");
    return listBox;
  }

  protected ListBox getListBox(List<String> values) {
    final ListBox genderBox = new ListBox(false);
    for (String s : values) {
      genderBox.addItem(s);
    }
    return genderBox;
  }

  protected void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, dialectGroup.box, "Try Again", message);
  }

/*  protected void markError(ControlGroup dialectGroup, Widget dialect, String header, String message) {
    markError(dialectGroup, dialect, (Focusable) dialect, header, message);
  }*/

  protected void markError(ControlGroup dialectGroup, Widget dialect, Focusable focusable, String header, String message) {
    dialectGroup.setType(ControlGroupType.ERROR);
    focusable.setFocus(true);

    setupPopoverThatHidesItself(dialect, header, message);
  }

  protected boolean highlightIntegerBox(FormField ageEntryGroup, int min, int max) {
    return highlightIntegerBox(ageEntryGroup, min, max, Integer.MAX_VALUE);
  }

  protected boolean highlightIntegerBox(FormField ageEntryGroup, int min, int max, int exception) {
    String text = ageEntryGroup.box.getText();
    boolean validAge = false;
    if (text.length() == 0) {
      ageEntryGroup.group.setType(ControlGroupType.WARNING);
    } else {
      try {
        int age = Integer.parseInt(text);
        validAge = (age >= min && age < max) || age == exception;
        ageEntryGroup.group.setType(validAge ? ControlGroupType.NONE : ControlGroupType.ERROR);
      } catch (NumberFormatException e) {
        ageEntryGroup.group.setType(ControlGroupType.ERROR);
      }
    }

    return validAge;
  }

  protected void markError(ControlGroup dialectGroup, String header, String message) {
    dialectGroup.setType(ControlGroupType.ERROR);
    setupPopoverThatHidesItself(dialectGroup.getWidget(1), header, message);
  }

  protected void markError(ControlGroup dialectGroup, Widget dialect, String header, String message) {
    dialectGroup.setType(ControlGroupType.ERROR);
    setupPopoverThatHidesItself(dialect, header, message);
  }

  protected void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message) {
    markError(dialectGroup, dialect, header, message, Placement.RIGHT);
  }

  protected void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message, Placement placement) {
    dialectGroup.setType(ControlGroupType.ERROR);
    dialect.setFocus(true);
    setupPopover(dialect, header, message, placement);
  }

  protected void setupPopoverThatHidesItself(final Widget w, String heading, final String message) {
    System.out.println("\ttriggering popover on '" + w.getTitle() + "' with " + heading + "/" + message);
    final MyPopover popover = new MyPopover();

    configurePopup(popover, w, heading, message, Placement.RIGHT);

    Timer t = new Timer() {
      @Override
      public void run() {
        popover.dontFireAgain();
        //popover.setHideDelay(0);
     //   popover.remove(w);
        //  popover.clear();
        //  popover.reconfigure();
        //   popover.clear();
      }
    };
    t.schedule(3000);
  }

  /**
   * TODO : bug - once shown these never really go away
   * @param w
   * @param heading
   * @param message
   * @param placement
   */
  protected void setupPopover(final FocusWidget w, String heading, final String message, Placement placement) {
    System.out.println("setupPopover : triggering popover on " + w + " with " + heading +"/"+message);
    final Popover popover = new Popover();
    configurePopup(popover, w, heading, message, placement);
    visiblePopovers.add(popover);
/*
    Timer t = new Timer() {
      @Override
      public void run() {
        //System.out.println("hide popover on " + w + " with " + message);
        popover.hide();
      }
    };
    t.schedule(3000);*/

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        w.setFocus(true);
      }
    });
  }

  private void configurePopup(Popover popover, Widget w, String heading, String message, Placement placement) {
    popover.setWidget(w);
    popover.setText(message);
    popover.setHeading(heading);
    popover.setPlacement(placement);
    popover.reconfigure();
    popover.show();
  }

  public void hidePopovers() {
    for (Popover popover : visiblePopovers) popover.hide();
    visiblePopovers.clear();
  }

  private static class MyPopover extends Popover {
    public void dontFireAgain() {
      hide();
      //asWidget();
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
            hidePopovers();
          }
        }
      });

      this.group = group;
    }

    public void clearError() {
      group.setType(ControlGroupType.NONE);
      hidePopovers();
    }

    public void setVisible(boolean visible) {
      group.setVisible(visible);
    }

    public String getText() { return box.getText(); }

    public void setRightSide(Widget rightSide) { this.rightSide = rightSide; }

    public String toString() { return "FormField value " + getText(); }
  }

  protected class ListBoxFormField {
    public final ListBox box;

    public ListBoxFormField(final ListBox box) {
      this.box = box;
      box.addChangeHandler(new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
          hidePopovers();
        }
      });
    }

    public String getValue() {
      return box.getItemText(box.getSelectedIndex());
    }

    public String toString() {
      return "Box: " + getValue();
    }

    protected void markSimpleError(String message) {
      box.setFocus(true);
      setupPopover(box, "Try Again", message, Placement.RIGHT);
    }
  }
}
