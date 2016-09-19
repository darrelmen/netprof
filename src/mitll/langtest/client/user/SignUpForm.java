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

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;

import java.util.logging.Logger;

class SignUpForm extends UserDialog implements SignUp {
  public static final int BOGUS_AGE = 99;
  private final Logger logger = Logger.getLogger("SignUpForm");
  private static final String NEW_USER = "New User?";

  private static final String WAIT_FOR_APPROVAL = "Wait for approval";
  private static final String YOU_WILL_GET_AN_APPROVAL_MESSAGE_BY_EMAIL = "You will get an approval message by email.";

  private static final String MALE = "male";
  private static final int MIN_LENGTH_USER_ID = 4;

  private static final int MIN_PASSWORD = 4;
  private static final String SIGN_UP_SUBTEXT = "Sign up";
  private static final String PASSWORD = "Password";
  private static final String USERNAME = "Username";
  private static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer user id.";
  private static final String VALID_EMAIL = "Please enter a valid email address.";
  private static final String PLEASE_ENTER_A_PASSWORD = "Please enter a password";
  private static final String SIGN_UP = "Sign Up";
  private static final String ARE_YOU_A = "Please choose : Are you a";
  private static final String STUDENT = "Student or ";
  private static final String TEACHER = "Teacher?";
  private static final String SIGN_UP_WIDTH = "266px";
  private static final int USERNAME_WIDTH = 25;
  private static final String RECORD_AUDIO_HEADING = "Recording audio/Quality Control";
  private static final int WAIT_FOR_READING_APPROVAL = 3000;
  private static final String RECORD_REFERENCE_AUDIO = "Are you an assigned reference audio recorder?";
  private static final String USER_EXISTS = "User exists already, please sign in or choose a different name.";
  private static final String AGE_ERR_MSG = "Enter age between " + MIN_AGE + " and " + MAX_AGE + ".";

  private BasicDialog.FormField signUpUser;
  private BasicDialog.FormField firstName;
  private BasicDialog.FormField lastName;
  private BasicDialog.FormField signUpEmail;
  private BasicDialog.FormField signUpPassword;

  private final EventRegistration eventRegistration;

  private Button signUp;
  private CheckBox contentDevCheckbox;
  private UserPassDialog userPassLogin;
  private static final String CURRENT_USERS = "Current users should add an email and password.";

  SignUpForm(PropertyHandler props,
             UserManager userManager,
             EventRegistration eventRegistration,
             UserPassDialog userPassLogin) {
    super(props, userManager);
    this.eventRegistration = eventRegistration;
    this.userPassLogin = userPassLogin;
  }

  @Override
  public void clickSignUp() {
    signUp.fireEvent(new KeyPressHelper.ButtonClickEvent());
  }

  /**
   * Don't enable the teacher choice for legacy users, b/c it lets them skip over the
   * recorder/not a recorder choice.
   *
   * @param result
   * @see SignInForm#copyInfoToSignUp(User)
   */
  @Override
  public void copyInfoToSignUp(User result, String passwordText) {
    signUpUser.box.setText(result.getUserID());
    signUpPassword.box.setText(passwordText);
    setFocusOn(signUpEmail.getWidget());
    //   eventRegistration.logEvent(signIn, "sign in", "N/A", "copied info to sign up form");

    markErrorBlur(signUpEmail, "Add info", CURRENT_USERS, Placement.TOP);
    signUpPassword.getGroup().setType(ControlGroupType.ERROR);
    signUpEmail.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        signUpPassword.getGroup().setType(ControlGroupType.NONE);
      }
    });
  }

  /**
   * @return
   * @see UserPassLogin#getRightLogin
   */
  @Override
  public Panel getSignUpForm() {
    Form form = getSignInForm();

    Fieldset fieldset = new Fieldset();
    Heading w = new Heading(3, NEW_USER, SIGN_UP_SUBTEXT);
    w.addStyleName("signUp");
    form.add(w);
    form.add(fieldset);

    TextBoxBase userBox = makeSignUpUsername(fieldset);
    TextBoxBase firstNameBox = makeSignUpFirstName(fieldset);
    TextBoxBase lastNameBox = makeSignUpLastName(fieldset);
    TextBoxBase emailBox = makeSignUpEmail(fieldset);

    makeSignUpPassword(fieldset);

    fieldset.add(getRolesHeader());
    fieldset.add(getRolesChoices());

    //getContentDevCheckbox();

    if (!props.isAMAS()) fieldset.add(contentDevCheckbox);

    makeRegistrationInfo(fieldset);
    fieldset.add(getSignUpButton(userBox, emailBox));

    return form;
  }

  private Heading getRolesHeader() {
    Heading w1 = new Heading(5, ARE_YOU_A);
    w1.addStyleName("leftTenMargin");
    int value = 5;
    w1.getElement().getStyle().setMarginTop(value, Style.Unit.PX);
    w1.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
    return w1;
  }

/*  private void getContentDevCheckbox() {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    builder.appendHtmlConstant(RECORD_REFERENCE_AUDIO);
    contentDevCheckbox = new CheckBox(builder.toSafeHtml());

    contentDevCheckbox.setVisible(false);
    contentDevCheckbox.addStyleName("leftTenMargin");
    contentDevCheckbox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = contentDevCheckbox.getValue() ? User.Kind.CONTENT_DEVELOPER : User.Kind.TEACHER;
        registrationInfo.setVisible(contentDevCheckbox.getValue());
      }
    });
    contentDevCheckbox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        if (studentOrTeacherPopover != null) {
          logger.info("hiding student/teacher popover!");
          studentOrTeacherPopover.hide();
        }
      }
    });

    contentDevCheckbox.getElement().getStyle().setPaddingBottom(10, Style.Unit.PX);
    if (!props.enableAllUsers()) {
      getRecordAudioPopover();
    }
  }*/

  private void getRecordAudioPopover() {
    String html = props.getRecordAudioPopoverText();
    addPopover(contentDevCheckbox, RECORD_AUDIO_HEADING, html);
  }

  private Panel getRolesChoices() {
    Panel roles = new HorizontalPanel();
    roles.addStyleName("leftTenMargin");

    roles.add(studentChoice);
    roles.add(teacherChoice);
    teacherChoice.addStyleName("leftFiveMargin");

    studentChoice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = User.Kind.STUDENT;
        registrationInfo.setVisible(false);
        contentDevCheckbox.setVisible(false);
        contentDevCheckbox.setValue(false);
      }
    });

    // Tamas wanted student by default...?
    studentChoice.setValue(true);

    teacherChoice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = User.Kind.TEACHER;
        registrationInfo.setVisible(false);
        contentDevCheckbox.setVisible(true);
      }
    });

    return roles;
  }

  private TextBoxBase makeSignUpUsername(Fieldset fieldset) {
    signUpUser = addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USERNAME_WIDTH, USERNAME);
    final TextBoxBase userBox = signUpUser.box;
    styleBoxNotLast(userBox);
    addFocusHandler(userBox, "username");
    return userBox;
  }

  private void styleBoxNotLast(TextBoxBase userBox) {
    styleBox(userBox);
    userBox.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
  }

  private TextBoxBase makeSignUpFirstName(Fieldset fieldset) {
    firstName = addControlFormFieldWithPlaceholder(fieldset, false, 3, USERNAME_WIDTH, "First Name");
    final TextBoxBase userBox = firstName.box;
    styleBoxNotLast(userBox);
    addFocusHandler(userBox, "firstName");
    return userBox;
  }

  private TextBoxBase makeSignUpLastName(Fieldset fieldset) {
    lastName = addControlFormFieldWithPlaceholder(fieldset, false, 3, USERNAME_WIDTH, "Last Name");
    final TextBoxBase userBox = lastName.box;
    styleBoxNotLast(userBox);
    addFocusHandler(userBox, "lastName");
    return userBox;
  }

  private TextBoxBase makeSignUpEmail(Fieldset fieldset) {
    signUpEmail = addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, "Email");
    final TextBoxBase emailBox = signUpEmail.box;
    styleBox(emailBox);
    addFocusHandler(emailBox, "email");
    return emailBox;
  }

  private void addFocusHandler(final TextBoxBase userBox, final String username) {
    userBox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.clearSignInHasFocus();
        eventRegistration.logEvent(userBox, "SignUp_" + username + "Box", "N/A", "focus in " + username + " field in sign up form");
      }
    });
  }

  private void styleBox(TextBoxBase userBox) {
    userBox.addStyleName("topMargin");
    userBox.addStyleName("rightFiveMargin");
    userBox.setWidth(SIGN_UP_WIDTH);
  }

  /**
   * @param fieldset
   * @see #getSignUpForm
   */
  private void makeSignUpPassword(Fieldset fieldset) {
    signUpPassword = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    signUpPassword.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.clearSignInHasFocus();
        eventRegistration.logEvent(signUpPassword.box, "SignUp_PasswordBox", "N/A", "focus in password field in sign up form");
      }
    });
    signUpPassword.box.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    signUpPassword.box.setWidth(SIGN_UP_WIDTH);
  }

  private void makeRegistrationInfo(Fieldset fieldset) {
    registrationInfo = new RegistrationInfo(fieldset);

    final TextBoxBase ageBox = registrationInfo.getAgeEntryGroup().box;
    ageBox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.clearSignInHasFocus();
      }
    });
    ageBox.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if (!isValidAge(registrationInfo.getAgeEntryGroup())) {
          //  registrationInfo.getAgeEntryGroup().markError(AGE_ERR_MSG);
          markErrorBlur(registrationInfo.getAgeEntryGroup().box, AGE_ERR_MSG, Placement.TOP);
        }
      }
    });

    registrationInfo.hideAge();
    registrationInfo.getDialectGroup().box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.clearSignInHasFocus();
      }
    });
    registrationInfo.getMale().addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.clearSignInHasFocus();
      }
    });
    registrationInfo.getFemale().addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.clearSignInHasFocus();
      }
    });
    registrationInfo.setVisible(false);
  }

  private RegistrationInfo registrationInfo;
  private User.Kind selectedRole = User.Kind.STUDENT;

  private final RadioButton studentChoice = new RadioButton("ROLE_CHOICE", STUDENT);
  private final RadioButton teacherChoice = new RadioButton("ROLE_CHOICE", TEACHER);
  private Popover studentOrTeacherPopover;

  private Button getSignUpButton(final TextBoxBase userBox, final TextBoxBase emailBox) {
    signUp = new Button(SIGN_UP);
    signUp.getElement().setId("SignUp");
    eventRegistration.register(signUp);

    signUp.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String userID = userBox.getValue();
        if (userID.length() < MIN_LENGTH_USER_ID) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short user id '" + userID + "'");
          markErrorBlur(signUpUser, PLEASE_ENTER_A_LONGER_USER_ID);
        } else if (firstName.getText().isEmpty()) {
          eventRegistration.logEvent(firstName.getWidget(), "SignUp_Button", "N/A", "short user first name '" + firstName.getText() + "'");
          markErrorBlur(firstName, "Please enter a first name.");
        } else if (lastName.getText().isEmpty()) {
          eventRegistration.logEvent(lastName.getWidget(), "SignUp_Button", "N/A", "short user last name '" + lastName.getText() + "'");
          markErrorBlur(lastName, "Please enter a last name.");
        } else {
          String emailText = signUpEmail.box.getValue();
          if (emailText.isEmpty()) {
            eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short email");
            markErrorBlur(signUpEmail, "Please enter your email.");
            //  } else if (signUpEmail.box.getValue().length() < MIN_EMAIL) {
            //     eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short email");
            //     markErrorBlur(signUpEmail, "Please enter your email.");
          } else if (!isValidEmail(emailText)) {
            eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "invalid email");
            markErrorBlur(signUpEmail, VALID_EMAIL);
          } else {
            String passwordText = signUpPassword.box.getValue();
            if (passwordText.length() < MIN_PASSWORD) {
              eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short password");
              markErrorBlur(signUpPassword, passwordText.isEmpty() ? PLEASE_ENTER_A_PASSWORD :
                  "Please enter a password at least " + MIN_PASSWORD + " characters long.");
            } else if (selectedRole == User.Kind.UNSET) {
              studentOrTeacherPopover = markErrorBlur(studentChoice, "Please choose", "Please select either student or teacher.", Placement.LEFT);
              eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't check role");
            }

            // TODO : ask content developers for info when they first sign up

/*            else if (selectedRole == User.Kind.CONTENT_DEVELOPER &&
                !registrationInfo.checkValidGender()) {
              eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't check gender");
              //        } else if (CHECK_AGE && selectedRole == User.Kind.CONTENT_DEVELOPER && !isValidAge(registrationInfo.getAgeEntryGroup())) {
              //         eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't fill in age ");
              //         markErrorBlur(registrationInfo.getAgeEntryGroup().box, AGE_ERR_MSG,Placement.TOP);
              //   registrationInfo.getAgeEntryGroup().markError(AGE_ERR_MSG);
            } else if (selectedRole == User.Kind.CONTENT_DEVELOPER && registrationInfo.getDialectGroup().getText().isEmpty()) {
              eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't fill in dialect ");
              markErrorBlur(registrationInfo.getDialectGroup(), "Enter a language dialect.");
              //  } else if (currentSignUpProject == null) {
              //    markErrorBlur(signUpProjectChoice, "Please choose a language", Placement.TOP);
            } */

            else {
              gotSignUp(userID, passwordText, emailBox.getValue(), selectedRole);
            }
          }
        }
      }
    });
    signUp.addStyleName("floatRight");
    signUp.addStyleName("rightFiveMargin");
    signUp.addStyleName("leftFiveMargin");
    signUp.setType(ButtonType.SUCCESS);

    return signUp;
  }

  /**
   * TODO : add first and last name so students can find their teacher
   * <p>
   * When the form is valid, make a new user or update an existing one.
   *
   * @param user
   * @param password
   * @param email
   * @param kind
   * @see #getSignUpButton(com.github.gwtbootstrap.client.ui.base.TextBoxBase, com.github.gwtbootstrap.client.ui.base.TextBoxBase)
   */
  private void gotSignUp(final String user, String password, String email, User.Kind kind) {
    String passH  = Md5Hash.getHash(password);
    String emailH = Md5Hash.getHash(email);

/*    boolean isCD = kind == User.Kind.CONTENT_DEVELOPER;
    String gender = isCD ? registrationInfo.isMale() ? MALE : "female" : MALE;
    String age = isCD ? registrationInfo.getAgeEntryGroup().getText() : "";
    int age1 = isCD ? (age.isEmpty() ? 99 : Integer.parseInt(age)) : 0;
    String dialect = isCD ? registrationInfo.getDialectGroup().getText() : "unk";*/

    int age1 = BOGUS_AGE;
    String dialect = "unk";
    signUp.setEnabled(false);

    SignUpUser newUser = new SignUpUser(user, passH, emailH, email, kind,
        true,  // don't really know the gender, so guess male...?
        age1, dialect,
        "browser", "", firstName.getText(), lastName.getText());

    service.addUser(
        newUser,
        Window.Location.getHref(),
  //      isCD,
        new AsyncCallback<User>() {
          @Override
          public void onFailure(Throwable caught) {
            eventRegistration.logEvent(signUp, "signing up", "N/A", "Couldn't contact server...?");

            signUp.setEnabled(true);
            markErrorBlur(signUp, "Trouble connecting to server.");
          }

          @Override
          public void onSuccess(User result) {
            if (result == null) {
              eventRegistration.logEvent(signUp, "signing up", "N/A", "Tried to sign up, but existing user (" + user + ").");
              signUp.setEnabled(true);
              markErrorBlur(signUpUser, USER_EXISTS);
            } else {
              if (result.isEnabled()) {
                eventRegistration.logEvent(signUp, "signing up", "N/A", getSignUpEvent(result));
                // logger.info("Got valid, enabled new user " + user + " and so we're letting them in.");

                storeUser(result);
              } else {
                eventRegistration.logEvent(signUp, "signing up", "N/A", getSignUpEvent(result) +
                    " but waiting for approval from Tamas.");
                markErrorBlur(signUp, WAIT_FOR_APPROVAL, YOU_WILL_GET_AN_APPROVAL_MESSAGE_BY_EMAIL, Placement.TOP);
                Timer t = new Timer() {
                  @Override
                  public void run() {
                    Window.Location.reload();

                  }
                };
                t.schedule(WAIT_FOR_READING_APPROVAL);
              }
            }
          }
        });
  }

  private String getSignUpEvent(User result) {
    return "successful sign up as " + result.getUserID() + "/" + result.getId() + " as " + result.getUserKind();
  }
}
