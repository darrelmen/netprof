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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.user.*;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class SignUpForm extends UserDialog implements SignUp {
  public static final String ADD_INFO = "Add info";
  private final Logger logger = Logger.getLogger("SignUpForm");

  private static final String I_M_SORRY = "I'm sorry";
  /**
   *
   */
  private static final String CHOOSE_AFFILIATION = " -- Choose Affiliation -- ";
  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email.";
  public static final String SORRY_NO_EMAIL_MATCH = "Sorry, this email is not in this user account.";

  public static final int BOGUS_AGE = 99;
  /**
   * @see #getSignUpForm
   */
  private static final String NEW_USER = "New User?";
  private static final int MIN_LENGTH_USER_ID = 5;

  /**
   * @see #getSignUpButton
   */
  private static final String SIGN_UP = "Sign Up";
  private static final String SIGN_UP_SUBTEXT = "Sign up";
  private static final String USERNAME = "Username";
  private static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer user id.";
  private static final String VALID_EMAIL = "Please enter a valid email address.";
  //  private static final String PLEASE_ENTER_A_PASSWORD = "Please enter a password";
  private static final String SIGN_UP_WIDTH = "266px";
  private static final int USERNAME_WIDTH = 25;
  private static final String USER_EXISTS = "User exists already, please sign in or choose a different name.";
  private static final String AGE_ERR_MSG = "Enter age between " + MIN_AGE + " and " + MAX_AGE + ".";

  private FormField signUpUser;

  private TextBoxBase userBox;
  private TextBoxBase emailBox;
  //private Heading demoHeader;

  private FormField firstName;
  private FormField lastName;
  private FormField signUpEmail;

  private RegistrationInfo registrationInfo;
  private final User.Kind selectedRole = User.Kind.STUDENT;

  private final EventRegistration eventRegistration;

  private Button signUp;
  private final UserPassDialog userPassLogin;
  private static final String CURRENT_USERS = "Please update your name and email.";
  private boolean markFieldsWithLabels = false;
  private final UserManager userManager;
  private ListBox affBox;
  private final List<Affiliation> affiliations;
  private Heading pleaseCheck;

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
                    UserPassDialog userPassLogin,
                    StartupInfo startupInfo) {
    super(props);
    this.userManager = userManager;
    this.eventRegistration = eventRegistration;
    this.userPassLogin = userPassLogin;
    affiliations = startupInfo.getAffiliations();
    //logger.info("got " + affiliations);
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

    if (firstName.getSafeText().isEmpty()) {
      firstName.setText(candidate.getFirst());
    }

    if (lastName.getSafeText().isEmpty()) {
      lastName.setText(candidate.getLast());
    }

    String email = candidate.getEmail();
    if (signUpEmail.getSafeText().isEmpty()) {
      if (isValidEmail(email)) {
        signUpEmail.setText(email);
      }
    }

    String affiliation = candidate.getAffiliation();
    if (affiliation == null || affiliation.isEmpty()) {
      logger.info("no affiliation?");
    } else {
      setAffBox(affiliation);
    }

    boolean b = askForDemographic(candidate);
    // demoHeader.setVisible(b);
    registrationInfo.setVisible(b);

    FormField firstFocus =
        firstName.isEmpty() ?
            firstName :
            lastName.isEmpty() ? lastName :
                email.isEmpty() ? signUpEmail : null;

    if (b) {
      if (firstFocus == null) {
        boolean isValid = registrationInfo.checkValidity();
        if (!isValid) {
          setFormAndButtonTitles();
        }
      }
    }

    if (firstFocus != null) {
      setFormAndButtonTitles();
      markErrorBlur(firstFocus, ADD_INFO, CURRENT_USERS, Placement.TOP, true);
    }
  }

  private void setFormAndButtonTitles() {
    setNewUserPrompt(false);
    setSignUpButtonTitle(false);
  }

  private void setAffBox(String affiliation) {
    int i = 0;
    boolean found = false;

    for (Affiliation affiliation1 : affiliations) {
      String abb = affiliation1.getAbb();
      if (abb.equals(affiliation)) {
        found = true;
        break;
      } else i++;
    }
    affBox.setSelectedIndex(found ? i + 1 : 0);
  }

  private Heading newUserPrompt;

  /**
   * @return
   * @see UserPassLogin#getRightLogin
   */
  @Override
  public Panel getSignUpForm() {
    newUserPrompt = new Heading(3, NEW_USER, SIGN_UP_SUBTEXT);
    newUserPrompt.addStyleName("signUp");

    Fieldset fields = getFields();
    fields.add(signUp = getSignUpButton(userBox, emailBox));
    fields.add(pleaseCheck = getPleaseCheck());

    return getTwoPartForm(newUserPrompt, fields);
  }

  private void setNewUserPrompt(boolean isNewUser) {
    if (isNewUser) {
      newUserPrompt.setText(NEW_USER);
      newUserPrompt.setSubtext(SIGN_UP_SUBTEXT);
    } else {
      //      setFocusOn(firstFocus.getWidget());
      newUserPrompt.setText("Complete Your Profile");
      newUserPrompt.setSubtext("");
    }
  }

  private Heading getPleaseCheck() {
    Heading pleaseCheck = new Heading(4, PLEASE_CHECK_YOUR_EMAIL);
    pleaseCheck.getElement().setId("pleaseCheck");
    pleaseCheck.setVisible(false);
    pleaseCheck.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    return pleaseCheck;
  }

  /**
   * @return
   * @paramx user
   */
  private Fieldset getFields() {
    Fieldset fieldset = new Fieldset();
    userBox = makeSignUpUsername(fieldset);
    makeSignUpFirstName(fieldset);
    makeSignUpLastName(fieldset);
    emailBox = makeSignUpEmail(fieldset);

    addControlGroupEntrySimple(
        fieldset,
        "",
        affBox = getAffBox())
        .setWidth(SIGN_UP_WIDTH);

    makeRegistrationInfo(fieldset);
    registrationInfo.setVisible(false);

    return fieldset;
  }

  private ListBox getAffBox() {
    ListBox affBox = new ListBox();
    affBox.getElement().setId("Affiliation_Box");
    affBox.setWidth(SIGN_UP_WIDTH + 30);
    affBox.addStyleName("leftTenMargin");

    affBox.addItem(CHOOSE_AFFILIATION);
    for (Affiliation value : affiliations) {
      affBox.addItem(value.getDisp());
    }
    affBox.getElement().getStyle().setWidth(276, Style.Unit.PX);
    return affBox;
  }

  private boolean askForDemographic(User user) {
    Collection<User.Permission> permissions = user.getPermissions();
    return permissions.contains(User.Permission.DEVELOP_CONTENT) || permissions.contains(User.Permission.RECORD_AUDIO) ||
        permissions.contains(User.Permission.QUALITY_CONTROL);
  }

  protected Collection<User.Kind> getRoles() {
    return User.getSelfChoiceRoles();
  }

  private TextBoxBase makeSignUpUsername(Fieldset fieldset) {
    signUpUser = getFormField(fieldset, false, MIN_LENGTH_USER_ID, USERNAME_WIDTH, USERNAME);
    final TextBoxBase userBox = signUpUser.box;
    styleBoxNotLast(userBox);
    addFocusHandler(userBox, "username");

    signUpUser.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        //logger.info("makeSignInUserName : got blur ");
        onUserIDBlur();
      }
    });

    return userBox;
  }

  /**
   * @see #makeSignUpUsername
   */
  private void onUserIDBlur() {
    final String text = signUpUser.getSafeText();

    if (!text.isEmpty()) {
      //  eventRegistration.logEvent(signUpUser.box, "UserNameBox", "N/A", "left username field '" + text + "'");
      //logger.info("\tchecking makeSignInUserName " + text);
      service.getUserByID(text, new AsyncCallback<User>() {
        @Override
        public void onFailure(Throwable caught) {
          logger.warning("\tgot FAILURE on userExists " + text);
        }

        @Override
        public void onSuccess(User result) {
          boolean isNewUser = result == null;
          setSignUpButtonTitle(isNewUser);
          setNewUserPrompt(isNewUser);
        }
      });
    }
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
  }

  /**
   * @param fieldset collect demographic info (age, gender, dialect) only if it's missing and they have the right permission
   */
  private void makeRegistrationInfo(Fieldset fieldset) {
    registrationInfo = new RegistrationInfo(fieldset, false);

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
    FormField dialectGroup = registrationInfo.getDialectGroup();
    if (dialectGroup != null) {
      dialectGroup.box.addFocusHandler(new FocusHandler() {
        @Override
        public void onFocus(FocusEvent event) {
          userPassLogin.clearSignInHasFocus();
        }
      });
    }
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
    Button signUp = getFormButton("SignUp", SIGN_UP, eventRegistration);
    signUp.addClickHandler(getSignUpClickHandler(userBox));

    return signUp;
  }

  private void setSignUpButtonTitle(boolean isSignUp) {
    signUp.setText(isSignUp ? SIGN_UP : "Add Info");
  }

  private ClickHandler getSignUpClickHandler(final TextBoxBase userBox) {
    return new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        signUpNewOrAddInfoToOld(userBox.getValue());
      }
    };
  }

  /**
   * Check email...
   *
   * @param userID
   * @see #getSignUpClickHandler
   */
  private void signUpNewOrAddInfoToOld(String userID) {
    service.getUserByID(signUpUser.getSafeText(), new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          //  logger.info("valid " + signUpUser.getSafeText());
          checkForm(userID);
        } else {
          // existing legacy users can have shorter userids than
          if (isFormValid(userID, true)) {
            gotSignUp(userID, emailBox.getValue());
          } else {
            logger.info("getSignUpClickHandler form is not valid!!");
          }
        }
      }
    });
  }

  /**
   * @see #signUpNewOrAddInfoToOld
   * @param userID
   */
  private void checkForm(String userID) {
    if (isFormValid(userID, false)) {
      gotSignUp(userID, emailBox.getValue());
    } else {
      logger.info("getSignUpClickHandler form is not valid!!");
    }
  }

  /**
   * @param userID
   * @param allowShort
   * @return
   * @see #signUpNewOrAddInfoToOld
   * @see #checkForm
   */
  private boolean isFormValid(String userID, boolean allowShort) {
    String emailText = signUpEmail.box.getValue();

    userID = userID.trim();

    String[] split = userID.split("\\s");
    if (split.length > 1) {//s.length() != userID.length()) {
      eventRegistration.logEvent(SignUpForm.this.signUp, "TextBox", "N/A", "no spaces in userid '" + userID + "'");
      markErrorBlur(signUpUser, "Please no spaces in user id.");
      return false;
    } else {
      int minLengthUserId = allowShort ? 4 : MIN_LENGTH_USER_ID;
      if (userID.length() < minLengthUserId) {
        eventRegistration.logEvent(SignUpForm.this.signUp, "TextBox", "N/A", "short user id '" + userID + "'");
        markErrorBlur(signUpUser, PLEASE_ENTER_A_LONGER_USER_ID);
        return false;
      } else if (firstName.getSafeText().isEmpty()) {
        eventRegistration.logEvent(firstName.getWidget(), "TextBox", "N/A", "short user first name '" + firstName.getSafeText() + "'");
        markErrorBlur(firstName, "Please enter a first name.");
        return false;
      } else if (lastName.getSafeText().isEmpty()) {
        eventRegistration.logEvent(lastName.getWidget(), "TextBox", "N/A", "short user last name '" + lastName.getSafeText() + "'");
        markErrorBlur(lastName, "Please enter a last name.");
        return false;
      } else if (emailText.isEmpty()) {
        eventRegistration.logEvent(signUpEmail.box, "TextBox", "N/A", "empty email");
        markErrorBlur(signUpEmail, "Please enter your email.");
        return false;
      } else if (!isValidEmail(emailText)) {
        markInvalidEmail();
        return false;
      } else if (affBox.getSelectedIndex() == 0) {
        eventRegistration.logEvent(affBox, "Affiliation_ListBox", "N/A", "didn't make choice");
        markErrorBlur(affBox, "Please choose an affiliation.", Placement.RIGHT);
        return false;
      } else if (!registrationInfo.checkValid()) {
        return false;
      } else {
        return true;
      }
    }
  }

  private void markInvalidEmail() {
    eventRegistration.logEvent(signUpEmail.box, "TextBox", "N/A", "invalid email");
    markErrorBlur(signUpEmail, VALID_EMAIL);
  }

  /**
   * add first and last name so students can find their teacher
   * <p>
   * When the form is valid, make a new user or update an existing one.
   *
   * @param user
   * @param email
   * @see #getSignUpButton(com.github.gwtbootstrap.client.ui.base.TextBoxBase, com.github.gwtbootstrap.client.ui.base.TextBoxBase)
   */
  private void gotSignUp(final String user,
                         String email) {
    signUp.setEnabled(false);

    SignUpUser newUser = new SignUpUser(user,
        //rot13(freeTextPassword),
        "",//Md5Hash.getHash(signUpPassword.getSafeText()),
        Md5Hash.getHash(email),
        email,
        selectedRole, //always student

        isMale(),  // don't really know the gender, so guess male...?
        getRealGender(),
        getAge(),
        "", // TODO : not getting dialect for now

        "browser",
        "",
        firstName.getSafeText(),
        lastName.getSafeText(),
        trimURL(Window.Location.getHref()),
        affiliations.get(affBox.getSelectedIndex() - 1).getAbb());

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

            handleAddUserResponse(result, user);
          }
        });
  }

  /**
   * So, what can happen with a user?
   * <p>
   * - user already exists with userid
   * - user slots are incomplete and need to be updated
   * - user could be locked
   * - user should provide password, otherwise anyone could take over an account
   * - user is new and needs to be added
   *
   * @param result
   */
  private void handleAddUserResponse(LoginResult result, String user) {
    LoginResult.ResultType resultType = result.getResultType();
    if (resultType == LoginResult.ResultType.BadPassword) {
      markErrorBlur(signUp, I_M_SORRY,
          "Your password is incorrect. Please try again.", Placement.TOP);
    } else if (resultType == LoginResult.ResultType.Exists) {
      eventRegistration.logEvent(signUp, "signing up", "N/A", "Tried to sign up, but existing user (" + user + ").");
      signUp.setEnabled(true);
      markErrorBlur(signUpUser, USER_EXISTS);
    } else {
      User theUser = result.getLoggedInUser();

      if (resultType == LoginResult.ResultType.Updated) {
        // shift focus to sign in.
        //  userPassLogin.setSignInPasswordFocus();
        userPassLogin.tryLogin();
      } else {
        userManager.setPendingUserStorage(theUser.getUserID());
        if (theUser.isEnabled()) {
          eventRegistration.logEvent(signUp, "signing up", "N/A", getSignUpEvent(theUser));
          pleaseCheck.setVisible(true);
        } else {
          eventRegistration.logEvent(signUp, "signing up", "N/A", getSignUpEvent(theUser) +
              "but gotta check email for next step..."
          );
          markErrorBlur(signUp, I_M_SORRY,
              "Your account has been deactivated. Please contact help email if needed.", Placement.TOP);
        }
      }
    }
  }

/*  private String getDialect() {
    return registrationInfo.isVisible() ? registrationInfo.getDialectGroup().getSafeText() : "unk";
  }*/

  private boolean isMale() {
    return !registrationInfo.isVisible() || registrationInfo.isMale();
  }

  private MiniUser.Gender getRealGender() {
    return !registrationInfo.isVisible() ?
        MiniUser.Gender.Unspecified : registrationInfo.isMale() ?
        MiniUser.Gender.Male :
        MiniUser.Gender.Female;
  }

  private int getAge() {
    String age = registrationInfo.isVisible() ? registrationInfo.getAgeEntryGroup().getSafeText() : "";
    return registrationInfo.isVisible() ? (age.isEmpty() ? 99 : Integer.parseInt(age)) : 0;
  }

  private String getSignUpEvent(User result) {
    return "successful sign up as " + result.getUserID() + "/" + result.getID() + " as " + result.getUserKind();
  }
}
