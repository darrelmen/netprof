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
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;
import net.liftweb.util.RE;
import sun.rmi.runtime.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SignUpForm extends UserDialog implements SignUp {
  public static final String I_M_SORRY = "I'm sorry";
  private final Logger logger = Logger.getLogger("SignUpForm");

  public static final int BOGUS_AGE = 99;
  private static final String NEW_USER = "New User?";

//  private static final String WAIT_FOR_APPROVAL = "Wait for approval";
//  private static final String YOU_WILL_GET_AN_APPROVAL_MESSAGE_BY_EMAIL = "You will get an approval message by email.";

  //  private static final String MALE = "male";
  private static final int MIN_LENGTH_USER_ID = 4;
  /**
   * TO BE CONSISTENT WITH DOMINO
   */
  private static final int MIN_PASSWORD = 8;

  /**
   * @see #getSignUpButton(TextBoxBase, TextBoxBase)
   */
  private static final String SIGN_UP = "Sign Up";
  private static final String SIGN_UP_SUBTEXT = "Sign up";
  //  private static final String PASSWORD = "Password";
  private static final String USERNAME = "Username";
  private static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer user id.";
  private static final String VALID_EMAIL = "Please enter a valid email address.";
  private static final String PLEASE_ENTER_A_PASSWORD = "Please enter a password";
  //private static final String ARE_YOU_A = "Please choose : Are you a";
  //  private static final String STUDENT = "Student or ";
//  private static final String TEACHER = "Teacher?";
  private static final String SIGN_UP_WIDTH = "266px";
  private static final int USERNAME_WIDTH = 25;
  //  private static final String RECORD_AUDIO_HEADING = "Recording audio/Quality Control";
//  private static final int WAIT_FOR_READING_APPROVAL = 3000;
//  private static final String RECORD_REFERENCE_AUDIO = "Are you an assigned reference audio recorder?";
  private static final String USER_EXISTS = "User exists already, please sign in or choose a different name.";
  private static final String AGE_ERR_MSG = "Enter age between " + MIN_AGE + " and " + MAX_AGE + ".";

  private FormField signUpUser;

  private TextBoxBase userBox;
  private TextBoxBase emailBox;
  Heading demoHeader;

  protected FormField firstName;
  protected FormField lastName;
  protected FormField signUpEmail;

  private RegistrationInfo registrationInfo;
  protected User.Kind selectedRole = User.Kind.STUDENT;

  private final EventRegistration eventRegistration;

  protected Button signUp;
  //  private CheckBox contentDevCheckbox;
  private UserPassDialog userPassLogin;
  private static final String CURRENT_USERS = "Please update your name and email.";
  private String signUpTitle = SIGN_UP;
  //  private String rolesHeader = ARE_YOU_A;
  private boolean markFieldsWithLabels = false;
  protected UserManager userManager;
  //private FormField password;
  private FormField signUpPassword;
  //private Map<User.Kind, RadioButton> roleToChoice = new HashMap<>();

  /**
   * @param props
   * @param userManager
   * @param eventRegistration
   * @param userPassLogin
   * @see UserPassLogin#UserPassLogin
   */
  public SignUpForm(PropertyHandler props,
                    UserManager userManager,
                    EventRegistration eventRegistration,
                    UserPassDialog userPassLogin) {
    super(props);
    this.userManager = userManager;
    this.eventRegistration = eventRegistration;
    this.userPassLogin = userPassLogin;
  }

  public SignUpForm setSignUpButtonTitle(String title) {
    this.signUpTitle = title;
    return this;
  }

  protected void setMarkFieldsWithLabels(boolean val) {
    this.markFieldsWithLabels = val;
  }

  @Override
  public void clickSignUp() {
    signUp.fireEvent(new KeyPressHelper.ButtonClickEvent());
  }

  /**
   * Don't enable the teacher choice for legacy users, b/c it lets them skip over the
   * recorder/not a recorder choice.
   *
   * @param userID
   * @param candidate
   * @see SignInForm#copyInfoToSignUp
   */
  @Override
  public void copyInfoToSignUp(String userID, User candidate) {
    signUpUser.box.setText(userID);

    firstName.setText(candidate.getFirst());
    lastName.setText(candidate.getLast());
    signUpEmail.setText(candidate.getEmail());
    signUpPassword.setVisible(true);

    boolean b = askForDemographic(candidate);
    demoHeader.setVisible(b);
    registrationInfo.setVisible(b);

    FormField firstFocus =
        firstName.isEmpty() ?
            firstName :
            lastName.isEmpty() ? lastName : signUpEmail;

    setFocusOn(firstFocus.getWidget());
    markErrorBlur(firstFocus, "Add info", CURRENT_USERS, Placement.TOP);
  }

  private FormField addPasswordField(Fieldset fieldset, Panel hp) {
    FormField password;
    password = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, "Password");
    password.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.setSignInHasFocus();
        //     eventRegistration.logEvent(userField.box, "PasswordBox", "N/A", "focus in password field");
      }
    });

    hp.add(password.box);
    password.setVisible(false);
    return password;
  }

  /**
   * @return
   * @see UserPassLogin#getRightLogin
   */
  @Override
  public Panel getSignUpForm() {
    Heading heading = new Heading(3, NEW_USER, SIGN_UP_SUBTEXT);
    heading.addStyleName("signUp");
    Fieldset fields = getFields();
    fields.add(getSignUpButton(userBox, emailBox));

    pleaseCheck = new Heading(4, "Please check your email.");
    pleaseCheck.getElement().setId("pleaseCheck");
    fields.add(pleaseCheck);
    pleaseCheck.setVisible(false);
    pleaseCheck.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);

    return getTwoPartForm(heading, fields);
  }

  private Heading pleaseCheck;

  /**
   * @return
   * @paramx user
   * @seex mitll.langtest.client.userops.OpsUserContainer#populateUserEdit(DivWidget, User)
   * @deprecated
   */
/*  public Panel getSignUpForm(User user) {
    Fieldset fields = getFields(user);
    fields.add(getSignUpButton(userBox, emailBox));

    pleaseCheck = new Heading(4, "Please check your email.");
    pleaseCheck.getElement().setId("pleaseCheck");
    fields.add(pleaseCheck);
    pleaseCheck.setVisible(true);

    return getTwoPartForm(
        getHeading(user),
        fields);
  }*/

/*  private Heading getHeading(User user) {
    Heading heading = new Heading(3, "Edit User Fields and Permissions",
        user.getUserID() + " : " + user.getFirst() + " " + user.getLast());
    heading.addStyleName("signUp");
    return heading;
  }*/
  private Panel getTwoPartForm(Heading heading, Fieldset fieldset) {
    Form form = getUserForm();
    form.add(heading);
    form.add(fieldset);

    return form;
  }


  /**
   * @return
   * @paramx user
   */
  protected Fieldset getFields(/*User user*/) {
    Fieldset fieldset = new Fieldset();
    userBox = makeSignUpUsername(fieldset);
    //TextBoxBase firstNameBox =
    makeSignUpFirstName(fieldset);
    //TextBoxBase lastNameBox =
    makeSignUpLastName(fieldset);
    emailBox = makeSignUpEmail(fieldset);

/*    User.Kind userKind = user == null ? User.Kind.UNSET : user.getUserKind();
    if (user != null) {
      userBox.setText(user.getUserID());
      firstNameBox.setText(user.getFirst());
      lastNameBox.setText(user.getLast());
      emailBox.setText(user.getEmail());
//      if (askForDemographic(userKind)) {
//        getContentDevCheckbox();
//      }
    }*/

    makeSignUpPassword(fieldset);
    signUpPassword.setVisible(false);
//    fieldset.add(getRolesHeader());
    //  fieldset.add(getRolesChoices(user == null ? User.Kind.STUDENT : userKind));

//    if (!props.isAMAS() && contentDevCheckbox != null) {
//      fieldset.add(contentDevCheckbox);
//    }

//    if (askForDemographic(userKind)) {
    demoHeader = getHeader("Demographic Info");
    demoHeader.setVisible(false);
    fieldset.add(demoHeader);
    makeRegistrationInfo(fieldset);
    registrationInfo.setVisible(false);
    //  }

    return fieldset;
  }

/*  protected boolean askForDemographic(User.Kind userKind) {
    return userKind == User.Kind.CONTENT_DEVELOPER ||
        userKind == User.Kind.AUDIO_RECORDER;
  } */

  protected boolean askForDemographic(User user) {
    Collection<User.Permission> permissions = user.getPermissions();
    return permissions.contains(User.Permission.DEVELOP_CONTENT) || permissions.contains(User.Permission.RECORD_AUDIO) ||
        permissions.contains(User.Permission.QUALITY_CONTROL);
  }

//  private Heading getRolesHeader() {
//    String rolesHeader = this.rolesHeader;
//    return getHeader(rolesHeader);
//  }

  protected Heading getHeader(String rolesHeader) {
    Heading w1 = new Heading(5, rolesHeader);
    w1.addStyleName("leftTenMargin");
    int value = 5;
    w1.getElement().getStyle().setMarginTop(value, Style.Unit.PX);
    w1.getElement().getStyle().setMarginBottom(value, Style.Unit.PX);
    return w1;
  }

/*  private Widget getContentDevCheckbox() {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    builder.appendHtmlConstant(RECORD_REFERENCE_AUDIO);
    contentDevCheckbox = new CheckBox(builder.toSafeHtml());

    contentDevCheckbox.setVisible(false);
    contentDevCheckbox.addStyleName("leftTenMargin");
    contentDevCheckbox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //  selectedRole = contentDevCheckbox.getValue() ? User.Kind.CONTENT_DEVELOPER : User.Kind.TEACHER;
        registrationInfo.setVisible(contentDevCheckbox.getValue());
      }
    });
*//*
    contentDevCheckbox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        if (studentOrTeacherPopover != null) {
          logger.info("hiding student/teacher popover!");
          studentOrTeacherPopover.hide();
        }
      }
    });
*//*
    contentDevCheckbox.getElement().getStyle().setPaddingBottom(10, Style.Unit.PX);
    if (!props.enableAllUsers()) {
      getRecordAudioPopover();
    }
    return contentDevCheckbox;
  }*/

  /*private void getRecordAudioPopover() {
    String html = props.getRecordAudioPopoverText();
    addPopover(contentDevCheckbox, RECORD_AUDIO_HEADING, html);
  }
*/

 /* private Panel getRolesChoices(User.Kind currentRole) {
    Panel vert = new VerticalPanel();

    Collection<User.Kind> roles1 = getRoles();

    if (roles1.size() == 1) {
      selectedRole = roles1.iterator().next();
    } else {
      Panel roles = new HorizontalPanel();
      roles.addStyleName("leftTenMargin");

      vert.add(roles);

      int c = 0;
      for (User.Kind role : roles1) {
        RadioButton roleChoice = addRoleChoice(roles, role);
        roleToChoice.put(role, roleChoice);
        roleChoice.addStyleName("leftFiveMargin");

        if (role == currentRole) {
          roleChoice.setValue(true);
          selectedRole = role;
        }

        if (c++ < roles1.size() && c % 2 == 0) {
          roles = new HorizontalPanel();
          roles.addStyleName("leftTenMargin");
          vert.add(roles);
        }
      }
    }
    return vert;
  }*/

  protected Collection<User.Kind> getRoles() {
    return User.getSelfChoiceRoles();
  }

/*  private RadioButton addRoleChoice(Panel roles, final User.Kind student) {
    RadioButton studentChoice = new RadioButton("ROLE_CHOICE", student.getName());
    studentChoice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = student;
//        logger.info("selected role is now "+ selectedRole);
        registrationInfo.setVisible(false);
        //      contentDevCheckbox.setVisible(false);
        //     contentDevCheckbox.setValue(false);
      }
    });
    roles.add(studentChoice);
    return studentChoice;
  }*/

  private TextBoxBase makeSignUpUsername(Fieldset fieldset) {
    signUpUser = getFormField(fieldset, false, MIN_LENGTH_USER_ID, USERNAME_WIDTH, USERNAME);
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
    firstName = getFormField(fieldset, false, 3, USERNAME_WIDTH, "First Name");
    final TextBoxBase userBox = firstName.box;
    styleBoxNotLast(userBox);
    addFocusHandler(userBox, "firstName");
    return userBox;
  }

  private FormField getFormField(Fieldset fieldset, boolean isPassword, int minLength, int usernameWidth, String hint) {
    return markFieldsWithLabels ?
        addControlFormField(fieldset, hint, isPassword, minLength, usernameWidth, "") :
        addControlFormFieldWithPlaceholder(fieldset, isPassword, minLength, usernameWidth, hint);
  }

  private TextBoxBase makeSignUpLastName(Fieldset fieldset) {
    lastName = getFormField(fieldset, false, 3, USERNAME_WIDTH, "Last Name");
    final TextBoxBase userBox = lastName.box;
    styleBoxNotLast(userBox);
    addFocusHandler(userBox, "lastName");
    return userBox;
  }

  /**
   * @param fieldset
   * @return
   * @see #getFields
   */
  private TextBoxBase makeSignUpEmail(Fieldset fieldset) {
    signUpEmail = getFormField(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, "Email");
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
        pleaseCheck.setVisible(false);
        signUp.setEnabled(true);
        eventRegistration.logEvent(userBox, "SignUp_" + username + "Box", "N/A", "focus in " + username + " field in sign up form");
      }
    });

    userBox.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        logger.info("check on " + signUpUser.getSafeText());
        service.getUserByID(signUpUser.getSafeText(), new AsyncCallback<User>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(User result) {
            if (result == null || result.isValid()) {
              logger.info("valid " + signUpUser.getSafeText());

              signUpPassword.setVisible(false);
            } else if (!result.isValid()) {
              logger.info("NOT valid " + signUpUser.getSafeText());

              signUpPassword.setVisible(true);
            }
          }
        });
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
    signUpPassword = getFormField(fieldset, true, MIN_PASSWORD, 15, "Password");
    signUpPassword.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.clearSignInHasFocus();
//        eventRegistration.logEvent(signUpPassword.box, "SignUp_PasswordBox", "N/A", "focus in password field in sign up form");
      }
    });
    signUpPassword.box.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    signUpPassword.box.setWidth(SIGN_UP_WIDTH);
  }

  /**
   * @param fieldset collect demographic info (age, gender, dialect) only if it's missing and they have the right permission
   */
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

//    registrationInfo.hideAge();
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
    //  registrationInfo.setVisible(false);
  }

  /**
   * @param userBox
   * @param emailBox
   * @return
   * @see #getSignUpForm
   */
  private Button getSignUpButton(final TextBoxBase userBox, final TextBoxBase emailBox) {
    this.signUp = new Button(signUpTitle);
    this.signUp.getElement().setId("SignUp");
    eventRegistration.register(this.signUp);

    this.signUp.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String userID = userBox.getValue();
        //logger.info("sign up click for " + userID);
        if (isFormValid(userID)) {
          gotSignUp(userID, emailBox.getValue(), selectedRole);
        } else {
          logger.warning("form is not valid!!");
        }
      }
    });
    this.signUp.addStyleName("floatRight");
    this.signUp.addStyleName("rightFiveMargin");
    this.signUp.addStyleName("leftFiveMargin");
    this.signUp.setType(ButtonType.SUCCESS);

    return this.signUp;
  }

/*
  private String getPasswordText() {
    return signUpPassword == null ? "" : signUpPassword.box.getValue();
  }
*/

  protected boolean isFormValid(String userID) {
    if (userID.length() < MIN_LENGTH_USER_ID) {
      eventRegistration.logEvent(SignUpForm.this.signUp, "SignUp_Button", "N/A", "short user id '" + userID + "'");
      markErrorBlur(signUpUser, PLEASE_ENTER_A_LONGER_USER_ID);
      return false;
    } else if (firstName.getSafeText().isEmpty()) {
      eventRegistration.logEvent(firstName.getWidget(), "SignUp_Button", "N/A", "short user first name '" + firstName.getSafeText() + "'");
      markErrorBlur(firstName, "Please enter a first name.");
      return false;
    } else if (lastName.getSafeText().isEmpty()) {
      eventRegistration.logEvent(lastName.getWidget(), "SignUp_Button", "N/A", "short user last name '" + lastName.getSafeText() + "'");
      markErrorBlur(lastName, "Please enter a last name.");
      return false;
    } else {
      String emailText = signUpEmail.box.getValue();
      if (emailText.isEmpty()) {
        eventRegistration.logEvent(SignUpForm.this.signUp, "SignUp_Button", "N/A", "short email");
        markErrorBlur(signUpEmail, "Please enter your email.");
        return false;
        //  } else if (signUpEmail.box.getValue().length() < MIN_EMAIL) {
        //     eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short email");
        //     markErrorBlur(signUpEmail, "Please enter your email.");
      } else if (!isValidEmail(emailText)) {
        markInvalidEmail();
        return false;
/*      }
      else if (signUpPassword != null) {
        String passwordText = getPasswordText();
        if (passwordText.length() < MIN_PASSWORD) {
          eventRegistration.logEvent(SignUpForm.this.signUp, "SignUp_Button", "N/A", "short password");
          markErrorBlur(signUpPassword, passwordText.isEmpty() ? PLEASE_ENTER_A_PASSWORD :
              "Please enter a password at least " + MIN_PASSWORD + " characters long.");
          return false;
        } else {
          return true;
        }*/
      } else {
        return true;
      }
    }
  }

  protected void markInvalidEmail() {
    eventRegistration.logEvent(SignUpForm.this.signUp, "SignUp_Button", "N/A", "invalid email");
    markErrorBlur(signUpEmail, VALID_EMAIL);
  }


/*  private String rot13(String val) {
    StringBuilder builder = new StringBuilder();
    for (char c : val.toCharArray()) {
      if (c >= 'a' && c <= 'm') c += 13;
      else if (c >= 'A' && c <= 'M') c += 13;
      else if (c >= 'n' && c <= 'z') c -= 13;
      else if (c >= 'N' && c <= 'Z') c -= 13;
      builder.append(c);
    }
    return builder.toString();
  }*/

  /**
   * TODO : add first and last name so students can find their teacher
   * <p>
   * When the form is valid, make a new user or update an existing one.
   *
   * @param user
   * @param email
   * @param kind
   * @paramx freeTextPassword
   * @see #getSignUpButton(com.github.gwtbootstrap.client.ui.base.TextBoxBase, com.github.gwtbootstrap.client.ui.base.TextBoxBase)
   */
  protected void gotSignUp(final String user,
                           //String freeTextPassword,
                           String email,
                           User.Kind kind) {
    signUp.setEnabled(false);

    SignUpUser newUser = new SignUpUser(user,
        //rot13(freeTextPassword),
        Md5Hash.getHash(signUpPassword.getSafeText()),
        Md5Hash.getHash(email),
        email,
        kind,

        isMale(),  // don't really know the gender, so guess male...?
        getAge(),
        getDialect(),

        "browser",
        "",
        firstName.getSafeText(),
        lastName.getSafeText(),
        trimURL(Window.Location.getHref()));

    service.addUser(
        newUser,
        Window.Location.getHref(),
        new AsyncCallback<LoginResult>() {
          @Override
          public void onFailure(Throwable caught) {
            eventRegistration.logEvent(signUp, "signing up", "N/A", "Couldn't contact server...?");
            signUp.setEnabled(true);
            markErrorBlur(signUp, "Trouble connecting to server.");
          }

          /**
           * So, what can happen with a user?
           *
           * - user already exists with userid
           * - user slots are incomplete and need to be updated
           *  - user could be locked
           *  - user should provide password, otherwise anyone could take over an account
           * - user is new and needs to be added
           *
           *  @param result
           */
          @Override
          public void onSuccess(LoginResult result) {
            signUp.setEnabled(true);

            LoginResult.ResultType resultType = result.getResultType();
            if (resultType == LoginResult.ResultType.BadPassword) {
              markErrorBlur(signUp, I_M_SORRY,
                  "Your password is incorrect. Please try again.", Placement.TOP);
            }
            else if (resultType == LoginResult.ResultType.Exists) {
              eventRegistration.logEvent(signUp, "signing up", "N/A", "Tried to sign up, but existing user (" + user + ").");
              signUp.setEnabled(true);
              markErrorBlur(signUpUser, USER_EXISTS);
            } else {
              User theUser = result.getLoggedInUser();


              if (resultType == LoginResult.ResultType.Updated) {
                // shift focus to sign in.
                userPassLogin.setSignInPasswordFocus();
              } else {
                userManager.setPendingUserStorage(theUser.getUserID());
                if (theUser.isEnabled()) {
                  eventRegistration.logEvent(signUp, "signing up", "N/A", getSignUpEvent(theUser));
                  pleaseCheck.setVisible(true);
                } else {
                  eventRegistration.logEvent(signUp, "signing up", "N/A", getSignUpEvent(theUser) +
                      //    " but waiting for approval from Tamas."
                      "but gotta check email for next step..."
                  );
                  //  markErrorBlur(signUp, WAIT_FOR_APPROVAL, YOU_WILL_GET_AN_APPROVAL_MESSAGE_BY_EMAIL, Placement.TOP);
                  markErrorBlur(signUp, I_M_SORRY,
                      "Your account has been deactivated. Please contact help email if needed.", Placement.TOP);
//                signUp.setEnabled(false);
                }
              }
            }
          }
        });
  }

  protected String getDialect() {
    return registrationInfo.isVisible() ? registrationInfo.getDialectGroup().getSafeText() : "unk";
  }

  protected boolean isMale() {
    return !registrationInfo.isVisible() || registrationInfo.isMale();
  }

  protected int getAge() {
    String age = registrationInfo.isVisible() ? registrationInfo.getAgeEntryGroup().getSafeText() : "";
    int age1 = registrationInfo.isVisible() ? (age.isEmpty() ? 99 : Integer.parseInt(age)) : 0;
    return age1;
  }

  private String getSignUpEvent(User result) {
    return "successful sign up as " + result.getUserID() + "/" + result.getID() + " as " + result.getUserKind();
  }
}
