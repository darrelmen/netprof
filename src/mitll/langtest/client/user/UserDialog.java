package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Accordion;
import com.github.gwtbootstrap.client.ui.AccordionGroup;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.BackdropType;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
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
public abstract class UserDialog extends BasicDialog {
  protected static final int USER_ID_MAX_LENGTH = 80;

  private static final String GRADING = "grading";
  private static final String TESTING = "testing";  // TODO make these safer

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
  protected UserManager userManager;
  protected UserNotification userNotification;

  public UserDialog(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager, UserNotification userNotification) {
    this.service = service;
    this.props = props;
    this.userManager = userManager;
    this.userNotification =  userNotification;
  }

  protected int getAge(TextBoxBase ageEntryBox) {
    int i = 0;
    try {
      i = props.isDataCollectAdminView() ? MAX_AGE-1 : Integer.parseInt(ageEntryBox.getText());
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
    dialogBox.add(accordion);
    return accordionGroup;
  }

  protected Button addLoginButton(Modal dialogBox) {
    FlowPanel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.RIGHT);
    final Button login = new Button("Login");
    login.setType(ButtonType.PRIMARY);
    login.setEnabled(true);
    login.setTitle("Hit enter to log in.");
    login.getElement().setId("login");
    hp.add(login);

    dialogBox.add(hp);
    return login;
  }

  protected FormField addControlFormField(Panel dialogBox, String label, int minLength) {
    return addControlFormField(dialogBox, label, false, minLength);
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();

    return getFormField(dialogBox, label, user, minLength);
  }

  private FormField getFormField(Panel dialogBox, String label, TextBox user, int minLength) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user);
    return new FormField(user, userGroup, minLength);
  }

  protected void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, "Try Again", message);
  }

  private HandlerRegistration keyHandler;

  // TODO : replace with enter key handler
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

  protected boolean checkPassword(FormField password) { return checkPassword(password.box);  }

  private boolean checkPassword(TextBoxBase password) {
    String trim = password.getText().trim();
    return trim.equalsIgnoreCase(GRADING) || trim.equalsIgnoreCase(TESTING);
  }
}
