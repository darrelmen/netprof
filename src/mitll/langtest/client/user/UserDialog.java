package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Accordion;
import com.github.gwtbootstrap.client.ui.AccordionGroup;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.BackdropType;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/10/13
 * Time: 4:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserDialog {
  protected static final int USER_ID_MAX_LENGTH = 80;

  private static final String GRADING = "grading";
  private static final String TESTING = "testing";

  protected static final int MIN_AGE = 12;
  protected static final int MAX_AGE = 90;
  protected static final int TEST_AGE = 100;
  protected static final List<String> EXPERIENCE_CHOICES = Arrays.asList(
    "0-3 months (Semester 1)",
    "4-6 months (Semester 1)",
    "7-9 months (Semester 2)",
    "10-12 months (Semester 2)",
    "13-16 months (Semester 3)",
    "16+ months",
    "Native speaker");
  protected static final int NATIVE_MONTHS = 20 * 12;
  protected final PropertyHandler props;

  protected final LangTestDatabaseAsync service;

  public UserDialog(LangTestDatabaseAsync service, PropertyHandler props) {
    this.service = service;
    this.props = props;
  }

  /**
   * From Modal code.
   * Centers fixed positioned element vertically.
   *
   * @paramx e Element to center vertically
   */
/*  protected native void centerVertically(Element e) *//*-{
      $wnd.jQuery(e).css("margin-top", (-1 * $wnd.jQuery(e).outerHeight() / 2) + "px");
  }-*//*;*/

  protected int getAge(TextBox ageEntryBox) {
    int i = 0;
    try {
      i = props.isDataCollectAdminView() ? 89 : Integer.parseInt(ageEntryBox.getText());
    } catch (NumberFormatException e) {
      System.out.println("couldn't parse " + ageEntryBox.getText());
    }
    return i;
  }

  protected Modal getDialog(String title) {
    final Modal dialogBox = new Modal();
    dialogBox.setTitle(title);
    dialogBox.setCloseVisible(false);
    dialogBox.setKeyboard(false);
    dialogBox.setBackdrop(BackdropType.STATIC);
    return dialogBox;
  }


  protected AccordionGroup getAccordion(final Modal dialogBox, Panel register) {
    Accordion accordion = new Accordion();
    AccordionGroup accordionGroup = new AccordionGroup();
    accordionGroup.setHeading("Registration");
    accordionGroup.add(register);
    accordion.add(accordionGroup);
   // ScrollPanel scrollPanel = new ScrollPanel();
   // scrollPanel.add(accordion);
    dialogBox.add(accordion);

/*    accordionGroup.addShownHandler(new ShownHandler() {
      @Override
      public void onShown(ShownEvent shownEvent) {
     //   System.out.println("onShown dialog height is " +dialogBox.getOffsetHeight());
        DOM.setStyleAttribute(dialogBox.getElement(), "top", "3%");
       // dialogBox.addStyleName("dialogExpandedTopMargin");
      }
    });
    accordionGroup.addHiddenHandler(new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
     //   System.out.println("onHide dialog height is " +dialogBox.getOffsetHeight());
        DOM.setStyleAttribute(dialogBox.getElement(), "top", "10%");
        //dialogBox.removeStyleName("dialogExpandedTopMargin");
      }
    });*/
    return accordionGroup;
  }

  /**
   * @deprecated - use accordion instead
   * @param dialogBox
   * @param register
   * @return
   */
  protected DisclosurePanel getDisclosurePanel(final Modal dialogBox, Panel register) {
    DisclosurePanel dp = new DisclosurePanel("Registration");
    dp.setContent(register);
    dp.addOpenHandler(new OpenHandler<DisclosurePanel>() {
      @Override
      public void onOpen(OpenEvent<DisclosurePanel> event) {
        dialogBox.setHeight("750px");
  /*      dialogBox.setMaxHeigth(Window.getClientHeight() * MAX_HEIGHT + "px");
        centerVertically(dialogBox.getElement()); // need to resize the dialog when reveal hidden widgets
*/
        //dialogBox.setMaxHeigth((Window.getClientHeight() * MAX_HEIGHT) + "px");
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            System.out.println("1   centering vertically... " + dialogBox.getOffsetHeight());
            //dialogBox.setHeight("750px");
            //   dialogBox.setMaxHeigth(Window.getClientHeight() * MAX_HEIGHT + "px");
      //      centerVertically(dialogBox.getElement()); // need to resize the dialog when reveal hidden widgets
          }
        });
      }
    });

    dp.addCloseHandler(new CloseHandler<DisclosurePanel>() {
      @Override
      public void onClose(CloseEvent<DisclosurePanel> event) {
        dialogBox.setHeight("481px");

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            System.out.println("2  centering vertically...");
//            dialogBox.setHeight("481px");

            //    centerVertically(dialogBox.getElement()); // need to resize the dialog when reveal hidden widgets
          }
        });
      }
    });
    return dp;
  }

  protected Widget addRegistrationPrompt(Panel dialogBox) {
    HTML child = new HTML("<i>New users : click on Registration below and fill in the fields.</i>");
    dialogBox.add(child);
    return child;
  }

  protected Button addLoginButton(Modal dialogBox) {
    FlowPanel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.RIGHT);
    final Button login = new Button("Login");
    login.setType(ButtonType.PRIMARY);
    login.setEnabled(true);
    login.setTitle("Hit enter to log in.");
    // We can set the id of a widget by accessing its Element
    login.getElement().setId("login");
    hp.add(login);

    dialogBox.add(hp);
    return login;
  }

  protected static class FormField {
    public final TextBox box;
    public final ControlGroup group;

    public FormField(final TextBox box, final ControlGroup group) {
      this.box = box;

      box.addKeyUpHandler(new KeyUpHandler() {
        public void onKeyUp(KeyUpEvent event) {
          if (box.getText().length() > 0) {
            group.setType(ControlGroupType.NONE);
          }
        }
      });

      this.group = group;
    }

    public String getText() {
      return box.getText();
    }
  }

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

  private ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget user) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");

    Controls controls = new Controls();
    userGroup.add(new ControlLabel(label));
    controls.add(user);
    userGroup.add(controls);

    dialogBox.add(userGroup);
    return userGroup;
  }

  protected static class ListBoxFormField {
    public final ListBox box;

    public ListBoxFormField(ListBox box) {
      this.box = box;
    }

    public String getValue() {
      return box.getItemText(box.getSelectedIndex());
    }



    public String toString() {
      return "Box: " + getValue();
    }
  }

  protected void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, "Try Again", message);
  }

  protected void markError(ControlGroup dialectGroup, FocusWidget dialect, String header, String message) {
    dialectGroup.setType(ControlGroupType.ERROR);
    dialect.setFocus(true);

    setupPopover(dialect, header, message);
  }

  protected void setupPopover(final Widget w, String heading, final String message) {
    // System.out.println("triggering popover on " + w + " with " + message);
    final Popover popover = new Popover();
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
        popover.hide();
      }
    };
    t.schedule(3000);
  }

  private HandlerRegistration keyHandler;

  protected void addKeyHandler(final Button send) {
    keyHandler = Event.addNativePreviewHandler(new
                                                 Event.NativePreviewHandler() {

                                                   @Override
                                                   public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                     NativeEvent ne = event.getNativeEvent();
                                                     int keyCode = ne.getKeyCode();

                                                     boolean isEnter = keyCode == KeyCodes.KEY_ENTER;

                                                     //   System.out.println("key code is " +keyCode);
                                                     if (isEnter && event.getTypeInt() == 512 &&
                                                       "[object KeyboardEvent]".equals(ne.getString())) {
                                                       ne.preventDefault();
                                                       send.fireEvent(new ButtonClickEvent());
                                                     }
                                                   }
                                                 });
    System.out.println("UserManager.addKeyHandler made click handler " + keyHandler);
  }

  private class ButtonClickEvent extends ClickEvent {
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }

  protected void removeKeyHandler() {
    System.out.println("UserManager.removeKeyHandler : " + keyHandler);

    if (keyHandler != null) keyHandler.removeHandler();
  }


  protected ListBox getGenderBox() {
    List<String> values = Arrays.asList("Male", "Female");
    return getListBox(values);
  }

  protected ListBox getListBox(List<String> values) {
    final ListBox genderBox = new ListBox(false);
    for (String s : values) {
      genderBox.addItem(s);
    }
    return genderBox;
  }

  protected ListBox getExperienceBox() {
    final ListBox experienceBox = new ListBox(false);
    List<String> choices = EXPERIENCE_CHOICES;
    for (String c : choices) {
      experienceBox.addItem(c);
    }
    experienceBox.ensureDebugId("cwListBox-dropBox");
    return experienceBox;
  }

  protected boolean checkPassword(FormField password) {
    return checkPassword(password.box);
  }

  private boolean checkPassword(TextBox password) {
    String trim = password.getText().trim();
    return trim.equalsIgnoreCase(GRADING) || trim.equalsIgnoreCase(TESTING);
  }
}
