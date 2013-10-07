package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.EnterKeyButtonHelper;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class BasicDialog {
  private EnterKeyButtonHelper enterKeyButtonHelper = new EnterKeyButtonHelper();

  protected FormField addControlFormField(Panel dialogBox, String label) {
    return addControlFormField(dialogBox, label, false);
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    return getFormField(dialogBox, label, user);
  }

  private FormField getFormField(Panel dialogBox, String label, TextBox user) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user);
    return new FormField(user, userGroup);
  }

  protected ListBoxFormField getListBoxFormField(Panel dialogBox, String label, ListBox user) {
    addControlGroupEntry(dialogBox, label, user);
    return new ListBoxFormField(user);
  }

  protected ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget user) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    userGroup.add(new ControlLabel(label));
    userGroup.add(user);

    dialogBox.add(userGroup);
    return userGroup;
  }

  protected void addKeyHandler(final Button send, Modal dialog) {
    enterKeyButtonHelper.addKeyHandler(send, dialog);
  }

 /* protected void removeKeyHandler() {
    enterKeyButtonHelper.removeKeyHandler();
  }
*/
  protected ListBox getListBox(List<String> values) {
    final ListBox genderBox = new ListBox(false);
    for (String s : values) {
      genderBox.addItem(s);
    }
    // genderBox.ensureDebugId("cwListBox-dropBox");
    return genderBox;
  }

  protected void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, dialectGroup.box, "Try Again", message);
  }

  protected void markError(ControlGroup dialectGroup, Widget dialect, String header, String message) {
    markError(dialectGroup, dialect, (Focusable) dialect, header, message);
  }

  protected void markError(ControlGroup dialectGroup, Widget dialect, Focusable focusable, String header, String message) {
    dialectGroup.setType(ControlGroupType.ERROR);
    focusable.setFocus(true);

    setupPopover(dialect, header, message);
  }

  protected void setupPopover(final Widget w, String heading, final String message) {
     System.out.println("\n\n\ntriggering popover on " + w.getTitle() + " with " + heading + "/"+message);
    final MyPopover popover = new MyPopover();
    popover.setWidget(w);
    popover.setText(message);
    popover.setHeading(heading);
    popover.setPlacement(Placement.RIGHT);
    popover.reconfigure();
    popover.show();

    Timer t = new Timer() {
      @Override
      public void run() {
        //System.out.println("hide popover on " + w + " with " + message);
        popover.dontFireAgain();
        popover.setHideDelay(0);
        popover.clear();
        popover.reconfigure();

        //   popover.clear();
      }
    };
    t.schedule(3000);
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

  private static class MyPopover extends Popover {
    public void dontFireAgain() {
      hide();
      asWidget();
    }
  }
}
