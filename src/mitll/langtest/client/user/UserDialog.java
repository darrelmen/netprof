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
import com.github.gwtbootstrap.client.ui.constants.Placement;
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
  protected static final int USER_ID_MAX_LENGTH = 35;
  private static final String GRADING = "grading";
  private static final String TESTING = "testing";  // TODO make these safer

  static final int MIN_AGE = 12;
  static final int MAX_AGE = 90;
  static final int TEST_AGE = 100;
  static final String UNSET = "Unset";
/*  private static final List<String> EXPERIENCE_CHOICES = Arrays.asList(
      UNSET,
    "0-3 months (Semester 1)",
    "4-6 months (Semester 1)",
    "7-9 months (Semester 2)",
    "10-12 months (Semester 2)",
    "13-16 months (Semester 3)",
    "16+ months",
    "Native speaker");
  protected static final int NATIVE_MONTHS = 20 * 12;*/
  private static final String MALE = "Male";
  private static final String FEMALE = "Female";
  public static final String HIT_ENTER_TO_LOG_IN = "Hit enter to log in.";
  final PropertyHandler props;
  final LangTestDatabaseAsync service;

  UserDialog(LangTestDatabaseAsync service, PropertyHandler props) {
    this.service = service;
    this.props = props;
  }

  int getAge(TextBoxBase ageEntryBox) {
    int i = 0;
    try {
      if (!ageEntryBox.getText().trim().isEmpty()) {
        i = Integer.parseInt(ageEntryBox.getText());
      }
    } catch (NumberFormatException e) {
      System.out.println("couldn't parse '" + ageEntryBox.getText() + "'");
    }
    return i;
  }

  Modal getDialog(String title) {
    final Modal dialogBox = new Modal();
    dialogBox.setTitle(title);
    dialogBox.setCloseVisible(false);
    dialogBox.setKeyboard(false);
    dialogBox.setBackdrop(BackdropType.STATIC);
    return dialogBox;
  }

  AccordionGroup getAccordion(final Panel dialogBox, Panel contents) {
    Accordion accordion = new Accordion();
    AccordionGroup accordionGroup = new AccordionGroup();
    accordionGroup.setHeading("Registration");
    accordionGroup.add(contents);
    accordion.add(accordionGroup);
    dialogBox.add(accordion);

    return accordionGroup;
  }

  /**
   * @see StudentDialog#displayLoginBox()
   * @param dialogBox
   * @return
   */
  Button addLoginButton(Modal dialogBox) {
    Panel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.RIGHT);
    final Button login = new Button("Login");
    login.setType(ButtonType.PRIMARY);
    login.setEnabled(true);
    login.setTitle(HIT_ENTER_TO_LOG_IN);
    login.getElement().setId("login");
    hp.add(login);
    login.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

    dialogBox.add(hp);
    return login;
  }

  FormField addControlFormField(Panel dialogBox, String label, int minLength, String hint) {
    return addControlFormField(dialogBox, label, false, minLength, USER_ID_MAX_LENGTH, hint);
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    user.setMaxLength(maxLength);
    return getFormField(dialogBox, label, user, minLength, hint);
  }

  private FormField getFormField(Panel dialogBox, String label, TextBox user, int minLength, String hint) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user, hint);
    return new FormField(user, userGroup, minLength);
  }

  protected void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, TRY_AGAIN, message, Placement.TOP);
  }

  protected void markError(FormField dialectGroup, String message, Placement placement) {
    markError(dialectGroup.group, dialectGroup.box, TRY_AGAIN, message, placement);
  }

  private HandlerRegistration keyHandler;

  /**
   * @see mitll.langtest.client.user.StudentDialog#configureKeyHandler(com.github.gwtbootstrap.client.ui.Modal, com.github.gwtbootstrap.client.ui.Button)
   * @param send
   */
  // TODO : replace with enter key handler
  void addKeyHandler(final Button send) {
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
                                                       //ne.preventDefault();
                                                       send.fireEvent(new ButtonClickEvent());
                                                     }
                                                   }
                                                 });
    System.out.println("UserManager.addKeyHandler made click handler " + keyHandler);
  }

  boolean checkValidPassword(FormField password, boolean markError) {
    final String userID = password.box.getText();
    if (userID.length() > USER_ID_MAX_LENGTH) {
      if (markError) markError(password, "Please enter a password of reasonable length.");
      return false;
    } else if (userID.length() == 0) {
      if (markError) markError(password, "Please enter the usual password that you've been told.");
      return false;
    } else if (!checkPassword(password)) {
      if (markError) markError(password, "Please enter the usual password for this kind of user.");
      return false;
    }
    return true;
  }

  /**
   * @see mitll.langtest.client.user.StudentDialog#checkUserAndMaybeRegister(com.github.gwtbootstrap.client.ui.Button, String, mitll.langtest.client.user.BasicDialog.FormField, mitll.langtest.client.user.BasicDialog.FormField, com.github.gwtbootstrap.client.ui.Modal, com.github.gwtbootstrap.client.ui.AccordionGroup, mitll.langtest.client.user.StudentDialog.RegistrationInfo, String, java.util.Collection)
   * @param user
   * @return
   */
  boolean checkValidUser(FormField user) {
    final String userID = user.box.getText();

    boolean foundError = false;
    String msg = "";
    if (userID.length() > USER_ID_MAX_LENGTH) {
      msg = "Please enter a user id of reasonable length.";
      foundError = true;
    } else if (userID.length() == 0) {
      msg = "Please enter a user id.";
      foundError = true;
    } else if (userID.length() < StudentDialog.MIN_LENGTH_USER_ID) {
      msg = "Must be at least " + StudentDialog.MIN_LENGTH_USER_ID + " characters.";
      foundError = true;
    } else {
      int c = 0;
      for (int i = 0; i < userID.length(); i++) {
        if (Character.isDigit(userID.charAt(i))) {
          c++;
        }
      }
      //System.out.println("goufn " + c);
      if (c < 2) {
        msg = "Please include at least 2 numbers.";
        foundError = true;
      }
    }
    if (foundError) {
      markError(user, msg, Placement.BOTTOM);
    } else {
      user.clearError();
    }
    return !foundError;
  }

  private class ButtonClickEvent extends ClickEvent {
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }

  void removeKeyHandler() {
   // System.out.println("UserManager.removeKeyHandler : " + keyHandler);
    if (keyHandler != null) keyHandler.removeHandler();
  }

  ListBox getGenderBox() {
    List<String> values = Arrays.asList(UNSET, MALE, FEMALE);
    return getListBox(values);
  }

  protected ListBox getListBox(List<String> values) {
    final ListBox genderBox = new ListBox(false);
    for (String s : values) {
      genderBox.addItem(s);
    }
    return genderBox;
  }

  boolean checkPassword(FormField password) { return checkPassword(password.box);  }

  private boolean checkPassword(TextBoxBase password) {
    String trim = password.getText().trim();
    return trim.equalsIgnoreCase(GRADING) || trim.equalsIgnoreCase(TESTING);
  }
}
