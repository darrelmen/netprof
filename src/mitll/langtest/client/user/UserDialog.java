package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.BackdropType;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
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

  protected DisclosurePanel dp;
  protected final LangTestDatabaseAsync service;
  protected UserManager userManager;
  protected UserNotification userNotification;

  public UserDialog(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager, UserNotification userNotification) {
    this.service = service;
    this.props = props;
    this.userManager = userManager;
    this.userNotification =  userNotification;
  }

  public abstract void display(String title);

  /**
   * From Modal code.
   * Centers fixed positioned element vertically.
   * @param e Element to center vertically
   */
  protected native void centerVertically(Element e) /*-{
      $wnd.jQuery(e).css("margin-top", (-1 * $wnd.jQuery(e).outerHeight() / 2) + "px");
  }-*/;

  protected int getAge(TextBox ageEntryBox) {
    int i = 0;
    try {
      i = props.isDataCollectAdminView() ? 89 : Integer.parseInt(ageEntryBox.getText());
    } catch (NumberFormatException e) {
      System.out.println("couldn't parse " + ageEntryBox.getText());
    }
    return i;
  }

  protected Modal getDialog() {
    final Modal dialogBox = new Modal();
    dialogBox.setCloseVisible(false);
    dialogBox.setKeyboard(false);
    dialogBox.setBackdrop(BackdropType.STATIC);
    return dialogBox;
  }

  protected ListBox getGenderBox() {
    List<String> values = Arrays.asList("Male", "Female");
    return getListBox(values);
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

  protected Button makeCloseButton() {
    final Button closeButton = new Button("Login");
    closeButton.setType(ButtonType.PRIMARY);

    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    return closeButton;
  }

  protected Button addLoginButton(Modal dialogBox) {
    final Button login = new Button("Login");
    login.setType(ButtonType.PRIMARY);
    login.setTitle("Hit enter to log in.");
    // We can set the id of a widget by accessing its Element
    login.getElement().setId("login");
    FlowPanel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.RIGHT);
    hp.add(login);

    dialogBox.add(hp);
    return login;
  }

  protected boolean isUserIDValid(FormField user) {
    final String userID = user.box.getText();
    if (user.box.getText().length() == 0) {
      markError(user, "Please enter a userid.");
      return false;
    } else if (userID.length() > USER_ID_MAX_LENGTH) {
      markError(user, "Please enter a user id of reasonable length.");
      return false;
    }
    return true;
  }

  protected boolean isPasswordValid(FormField password) {
    boolean valid = password.box.getText().length() > 0;
    if (!valid) {
      markError(password, "Please enter a password.");
    }
    else if (!checkPassword(password)) {
      markError(password, "Please use password from the email.");
      return false;
    }
    return true;
  }

  protected void storeAudioType(String type) {
    if (props.isCollectAudio()) {
      userNotification.rememberAudioType(type);
    }
  }

  protected void userExists(Integer result, String userID, Modal dialogBox, String audioType, PropertyHandler.LOGIN_TYPE loginType) {
    dialogBox.hide();
    storeAudioType(audioType);
    userManager.storeUser(result, audioType, userID, loginType);
  }
}
