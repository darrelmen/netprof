package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class BasicDialog {
  public static final String TRY_AGAIN = "Try Again";
  //private EnterKeyButtonHelper enterKeyButtonHelper = new EnterKeyButtonHelper();
  private List<Popover> visiblePopovers = new ArrayList<Popover>();

  protected FormField addControlFormField(Panel dialogBox, String label) {
    return addControlFormField(dialogBox, label, false, 0);
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    return getFormField(dialogBox, label, user,minLength);
  }

  private FormField getFormField(Panel dialogBox, String label, TextBox user, int minLength) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user, false);
    return new FormField(user, userGroup, minLength);
  }

  protected ListBoxFormField getListBoxFormField(Panel dialogBox, String label, ListBox user) {
    return getListBoxFormField(dialogBox, label, user,false);
  }

  protected ListBoxFormField getListBoxFormField(Panel dialogBox, String label, ListBox user, boolean leftAlignLabel) {
    ControlGroup group = addControlGroupEntry(dialogBox, label, user, leftAlignLabel);
    return new ListBoxFormField(user, group);
  }

  protected ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget user, boolean leftAlign) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    ControlLabel widget = new ControlLabel(label);

    if (leftAlign) {
      DOM.setStyleAttribute(widget.getElement(), "textAlign", "left");
      DOM.setStyleAttribute(widget.getElement(), "width", "80px");
    }

    userGroup.add(widget);
    if (!leftAlign) {
      user.addStyleName("leftFiveMargin");
    }
    else {
      user.addStyleName("floatRight");
    }
    userGroup.add(user);

    dialogBox.add(userGroup);
    return userGroup;
  }

  protected ListBox getListBox(List<String> values) {
    final ListBox genderBox = new ListBox(false);
    for (String s : values) {
      genderBox.addItem(s);
    }
    return genderBox;
  }

  protected void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, dialectGroup.box, TRY_AGAIN, message);
  }

/*  protected void markError(ControlGroup dialectGroup, Widget dialect, String header, String message) {
    markError(dialectGroup, dialect, (Focusable) dialect, header, message);
  }*/

  protected void markError(ControlGroup dialectGroup, Widget dialect, Focusable focusable, String header, String message) {
    dialectGroup.setType(ControlGroupType.ERROR);
    focusable.setFocus(true);

    setupPopover(dialect, header, message);
  }


  protected void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message) {
    markError(dialectGroup, dialect, header, message, Placement.RIGHT);
  }

  protected void markError(FormField dialectGroup, String message, Placement placement) {
    markError(dialectGroup.group, dialectGroup.box, TRY_AGAIN, message,placement);
  }

  protected void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message, Placement placement) {
    dialectGroup.setType(ControlGroupType.ERROR);
    dialect.setFocus(true);

    setupPopover(dialect, header, message, placement);
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
        validAge = (age >= min && age <= max) || age == exception;
        ageEntryGroup.group.setType(validAge ? ControlGroupType.NONE : ControlGroupType.ERROR);
      } catch (NumberFormatException e) {
        ageEntryGroup.group.setType(ControlGroupType.ERROR);
      }
    }

    return validAge;
  }

  /**
   * @see #markError(com.github.gwtbootstrap.client.ui.ControlGroup, com.google.gwt.user.client.ui.FocusWidget, String, String, com.github.gwtbootstrap.client.ui.constants.Placement)
   * @param w
   * @param heading
   * @param message
   */
  protected void setupPopover(final Widget w, String heading, final String message) {
    System.out.println("--> triggering popover on " + w.getTitle() + " with " + heading + "/"+message);
    final MyPopover popover = new MyPopover();
    showPopover(popover, w, heading, message, Placement.RIGHT);

    hidePopover(popover);

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        if (w instanceof FocusWidget) {
          ((FocusWidget)w).setFocus(true);
        }
      }
    });
  }

  /**
   * TODO : bug - once shown these never really go away
   *
   * @see #markError(com.github.gwtbootstrap.client.ui.ControlGroup, com.google.gwt.user.client.ui.FocusWidget, String, String, com.github.gwtbootstrap.client.ui.constants.Placement)
   * @see mitll.langtest.client.user.BasicDialog.ListBoxFormField#markSimpleError
   * @param w
   * @param heading
   * @param message
   * @param placement
   */
  protected void setupPopover(final FocusWidget w, String heading, final String message, Placement placement) {
    System.out.println("triggering popover on " + w + " with " + message);
    final MyPopover popover = makePopover(w, heading, message, placement);
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

    hidePopover(popover);

    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        w.setFocus(true);
      }
    });
  }

  private void hidePopover(final MyPopover popover) {
    Timer t = new Timer() {
      @Override
      public void run() {
        //System.out.println("hide popover on " + w + " with " + message);
        popover.dontFireAgain();
        popover.setHideDelay(0);
        //popover.clear();
       // popover.reconfigure();
      }
    };
    t.schedule(3000);
  }

  private MyPopover makePopover(FocusWidget w, String heading, String message, Placement placement) {
    final MyPopover popover = new MyPopover();
    showPopover(popover, w, heading, message, placement);
    return popover;
  }

  private void showPopover(Popover popover, Widget w, String heading, String message, Placement placement) {
    popover.setWidget(w);
    popover.setText(message);
    popover.setHeading(heading);
    popover.setPlacement(placement);
    popover.reconfigure();
    popover.show();
  }

  /**
   * @see mitll.langtest.client.user.BasicDialog.FormField#clearError()
   */
  public void hidePopovers() {
/*   for (Popover popover : visiblePopovers) {
      popover.hide();
    }
    visiblePopovers.clear();*/
  }

  private static class MyPopover extends Popover {
    public void dontFireAgain() {
      hide();
      asWidget();
    }
  }

  protected static interface FormItem {
    ControlGroup getGroup();
    Widget getWidget();
  }

  protected class FormField implements FormItem {
    public final TextBox box;
    public final ControlGroup group;

    public FormField(final TextBox box, final ControlGroup group, final int minLength) {
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

    public String getText() {
      return box.getText();
    }

    @Override
    public ControlGroup getGroup() {
      return group;
    }

    @Override
    public Widget getWidget() {
      return box;
    }
  }

  protected class ListBoxFormField {
    public final ListBox box;
    public final ControlGroup group;

    public ListBoxFormField(final ListBox box, ControlGroup group) {
      this.box = box;
      this.group = group;
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

    protected void markSimpleError(String message) {
      markSimpleError(message, Placement.RIGHT);
    }

    protected void markSimpleError(String message, Placement placement) {
      box.setFocus(true);
      setupPopover(box, TRY_AGAIN, message, placement);
    }

    public String toString() {
      return "Box: " + getValue();
    }
  }
}
