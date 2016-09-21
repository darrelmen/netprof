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
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.user.User;

import java.util.logging.Logger;

public class SignInForm extends UserDialog implements SignIn {
  private final Logger logger = Logger.getLogger("SignUpForm");
  private static final String DEACTIVATED = "I'm sorry, this account has been deactivated.";

  private static final String TROUBLE_CONNECTING_TO_SERVER = "Trouble connecting to server.";
  private static final String NO_USER_FOUND = "No userField found - have you signed up?";

  private static final String MAGIC_PASS = Md5Hash.getHash("adm!n");

  private static final int MIN_LENGTH_USER_ID = 4;

  private static final int MIN_PASSWORD = 4;
  private static final String PLEASE_ENTER_YOUR_PASSWORD = "Please enter your password.";
  private static final String BAD_PASSWORD = "Wrong password, please try again.";// - have you signed up?";
  private static final String PASSWORD = "Password";
  private static final String USERNAME = "Username";
  private static final String SIGN_IN = "Log In";
  private static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer userField id.";
  // private static final String PLEASE_WAIT = "Please wait";
  private static final String FORGOT_PASSWORD = "Forgot password?";
  private static final String ENTER_A_USER_NAME = "Enter a userField name.";
  private static final String CHECK_EMAIL = "Check Email";
  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email";
  private static final String ENTER_YOUR_EMAIL_TO_RESET_YOUR_PASSWORD = "Enter your email to reset your password.";
  // private static final String ENTER_YOUR_EMAIL_TO_RESET_YOUR_PASSWORD_GOT_IT = "Click here and then check your email to reset your password.";
  private static final String SEND = "Send Reset Email";
  private static final String SIGN_UP_WIDTH = "266px";
  private static final String PLEASE_CHECK = "Please check";
  private static final int EMAIL_POPUP_DELAY = 4000;

  private FormField userField;
  private FormField password;

  private Button signIn;

  private DecoratedPopupPanel resetEmailPopup;
  private Button sendEmail;
  private final EventRegistration eventRegistration;
  private UserPassDialog userPassLogin;
  private SignUp signUpForm;
  UserManager userManager;

  /**
   * @param props
   * @param userManager
   * @param eventRegistration
   * @param userPassLogin
   * @param signUpForm
   * @see UserPassLogin#UserPassLogin
   */
  SignInForm(PropertyHandler props,
             UserManager userManager,
             EventRegistration eventRegistration,
             UserPassDialog userPassLogin,
             SignUp signUpForm) {
    super(props);
    this.userManager = userManager;
    this.eventRegistration = eventRegistration;
    this.userPassLogin = userPassLogin;
    this.signUpForm = signUpForm;
  }

  /**
   * @param signInForm
   * @return
   * @see UserPassLogin#getRightLogin
   */
  @Override
  public Panel populateSignInForm(Form signInForm, Panel forgotRow, KeyPressHelper enterKeyButtonHelper) {
    Fieldset fieldset = new Fieldset();
    signInForm.add(fieldset);

    makeSignInUserName(fieldset);

    Panel hp = new HorizontalPanel();
    hp.getElement().setId("password_login_box");
    hp.addStyleName("leftFiveMargin");

    addPasswordField(fieldset, hp);

    Button signInButton = getSignInButton();
    enterKeyButtonHelper.addKeyHandler(signIn);
    hp.add(signInButton);

    fieldset.add(hp);
    //fieldset.add(getForgotRow());
    fieldset.add(forgotRow);
    setFocusOnUserID();

    return signInForm;
  }

  /**
   * If there's an existing userField without a password, copy their info to the sign up box.
   *
   * @param fieldset
   * @see #populateSignInForm
   */
  private void makeSignInUserName(Fieldset fieldset) {
    userField = addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, USERNAME);
    userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("Use`rname_Box_SignIn");
    userField.box.setWidth(SIGN_UP_WIDTH);

    userField.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        // signInHasFocus = true;
        userPassLogin.setSignInHasFocus();
        eventRegistration.logEvent(userField.box, "UserNameBox", "N/A", "focus in username field");
      }
    });

    userField.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if (!userField.getText().isEmpty()) {
          eventRegistration.logEvent(userField.box, "UserNameBox", "N/A", "left username field '" + userField.getText() + "'");

          //    logger.info("checking makeSignInUserName " + userField.getText());
          service.userExists(userField.getText(), "", new AsyncCallback<User>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(User result) {
              // logger.info("makeSignInUserName : for " + userField.getText() + " got back " + result);
              if (result != null) {
                String emailHash = result.getEmailHash();
                String passwordHash = result.getPasswordHash();
                //  this.email = result.getEmail();
                if (emailHash == null || passwordHash == null || emailHash.isEmpty() || passwordHash.isEmpty()) {
                  eventRegistration.logEvent(userField.box, "UserNameBox", "N/A", "existing legacy userField " + result.toStringShort());
                  copyInfoToSignUp(result);
                }
              }
            }
          });
        }
      }
    });
  }

  private void addPasswordField(Fieldset fieldset, Panel hp) {
    password = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    password.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.setSignInHasFocus();
        eventRegistration.logEvent(userField.box, "PasswordBox", "N/A", "focus in password field");
      }
    });

    hp.add(password.box);
  }

  /**
   * @return
   * @see #populateSignInForm
   */
  private Button getSignInButton() {
    signIn = new Button(SIGN_IN);
    signIn.setWidth("45px");
    signIn.getElement().setId("SignIn");
    eventRegistration.register(signIn);
    signIn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String userID = userField.box.getValue();
        if (userID.length() < MIN_LENGTH_USER_ID) {
          markErrorBlur(userField, PLEASE_ENTER_A_LONGER_USER_ID);
        } else {
          String value = password.box.getValue();
          if (!value.isEmpty() && value.length() < MIN_PASSWORD) {
            markErrorBlur(password, "Please enter a password longer than " + MIN_PASSWORD + " characters.");
          } else {
            gotLogin(userID, value, value.isEmpty());
          }
        }
      }
    });
    signIn.addStyleName("rightFiveMargin");
    signIn.addStyleName("leftFiveMargin");

    signIn.setType(ButtonType.PRIMARY);
    return signIn;
  }

  /**
   * TODOx : get list of projects
   *
   * @param user
   * @param pass
   * @see UserPassLogin#getRightLogin
   */
  private void gotLogin(final String user, final String pass, final boolean emptyPassword) {
    final String hashedPass = Md5Hash.getHash(pass);
    logger.info("gotLogin : userField is '" + user + "' pass " + pass.length() + " characters or '" + hashedPass + "'");

    signIn.setEnabled(false);
    service.userExists(user, hashedPass, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        signIn.setEnabled(true);
        markErrorBlur(signIn, TROUBLE_CONNECTING_TO_SERVER);
      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "unknown userField " + user);

          logger.info("No userField with that name '" + user + "' pass " + pass.length() + " characters - " + emptyPassword);
          markErrorBlur(password, emptyPassword ? PLEASE_ENTER_YOUR_PASSWORD : NO_USER_FOUND);
          signIn.setEnabled(true);
        } else {
          if (!result.isEnabled()) {
            markErrorBlur(userField, DEACTIVATED);
            signIn.setEnabled(true);
          } else {
            foundExistingUser(result, emptyPassword, hashedPass);
          }
        }
      }
    });
  }

  /**
   * @param result
   * @param emptyPassword
   * @param hashedPass
   * @see #gotLogin
   */
  private void foundExistingUser(User result, boolean emptyPassword, String hashedPass) {
    String user = result.getUserID();
    String emailHash = result.getEmailHash();
    String passwordHash = result.getPasswordHash();
    if (emailHash == null || passwordHash == null || emailHash.isEmpty() || passwordHash.isEmpty()) {
      copyInfoToSignUp(result);
      signIn.setEnabled(true);
    } else {
      // logger.info("Got valid userField " + result);
      if (emptyPassword) {
        eventRegistration.logEvent(signIn, "sign in", "N/A", "empty password");

        markErrorBlur(password, PLEASE_ENTER_YOUR_PASSWORD);
        signIn.setEnabled(true);
      } else if (result.getPasswordHash().equalsIgnoreCase(hashedPass)) {
        if (result.isEnabled() //||
          //   result.getUserKind() != User.Kind.CONTENT_DEVELOPER ||
          //    props.enableAllUsers()
            ) {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "successful sign in for " + user);
          //    logger.info("Got valid userField " + userField + " and matching password, so we're letting them in.");
          storeUser(result, userManager);
        } else {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "successful sign in for " + user + " but wait for approval.");
          markErrorBlur(signIn, "I'm sorry", DEACTIVATED, Placement.LEFT);
          signIn.setEnabled(true);
        }
      } else { // special pathway...
        String enteredPass = Md5Hash.getHash(password.getText());
        if (enteredPass.equals(MAGIC_PASS)) {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "sign in as userField '" + user + "'");
          storeUser(result, userManager);
        } else {
          logger.info("foundExistingUser bad pass  " + passwordHash);
          //  logger.info("admin " + Md5Hash.getHash("adm!n"));
          eventRegistration.logEvent(signIn, "sign in", "N/A", "bad password");
          markErrorBlur(password, BAD_PASSWORD);
          signIn.setEnabled(true);
        }
      }
    }
  }

  /**
   * Don't enable the teacher choice for legacy users, b/c it lets them skip over the
   * recorder/not a recorder choice.
   *
   * @param result
   * @see #foundExistingUser(User, boolean, String)
   * @see #makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   */
  private void copyInfoToSignUp(User result) {
    signUpForm.copyInfoToSignUp(result, password.getText());
    eventRegistration.logEvent(signIn, "sign in", "N/A", "copied info to sign up form");
  }

  /**
   * @see UserPassLogin#showSuggestApp
   */
  @Override
  public void setFocusOnUserID() {
    setFocusOn(userField.box);
  }

  /**
   * @return
   * @see UserPassLogin#getForgotRow
   */
  @Override
  public Anchor getForgotPassword() {
    final Anchor forgotPassword = new Anchor(FORGOT_PASSWORD);
    forgotPassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (userField.getText().isEmpty()) {
          markErrorBlur(userField, ENTER_A_USER_NAME);
          return;
        }
        final TextBox emailEntry = new TextBox();

        resetEmailPopup = new DecoratedPopupPanel(true);
        sendEmail = new Button(SEND);
        sendEmail.setType(ButtonType.PRIMARY);
        sendEmail.addStyleName("leftTenMargin");
        sendEmail.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            String text = emailEntry.getText();
            if (!isValidEmail(text)) {
       /*       System.out.println("email is '" + text+ "' ");*/
              markErrorBlur(emailEntry, PLEASE_CHECK, VALID_EMAIL, Placement.TOP);
              return;
            }

            sendEmail.setEnabled(false);
            service.resetPassword(userField.box.getText(), text, Window.Location.getHref(), new AsyncCallback<Boolean>() {
              @Override
              public void onFailure(Throwable caught) {
                sendEmail.setEnabled(true);
              }

              @Override
              public void onSuccess(Boolean result) {
                String heading = result ? CHECK_EMAIL : "Unknown email";
                String message = result ? PLEASE_CHECK_YOUR_EMAIL : userField.box.getText() + " doesn't have that email. Check for a typo?";
                setupPopover(sendEmail, heading, message, Placement.LEFT, EMAIL_POPUP_DELAY, new MyPopover(false) {
                  boolean isFirst = true;

                  @Override
                  public void hide() {
                    super.hide();
                    if (isFirst) {
                      isFirst = false;
                    } else {
                      resetEmailPopup.hide(); // TODO : ugly - somehow hide is called twice
                    }
                    //System.out.println("got hide !" + new Date()
                    //);
                  }
                }, false);
              }
            });
          }
        });
        eventRegistration.register(sendEmail, "N/A", "reset password");

        makePopup(resetEmailPopup, emailEntry, sendEmail, ENTER_YOUR_EMAIL_TO_RESET_YOUR_PASSWORD);
        resetEmailPopup.showRelativeTo(forgotPassword);
        setFocusOn(emailEntry);
      }
    });
    return forgotPassword;
  }

  /**
   * @return
   * @see UserPassLogin#UserPassLogin(PropertyHandler, UserManager, EventRegistration)
   */
  @Override
  public boolean clickSendEmail() {
    if (resetEmailPopup != null && resetEmailPopup.isShowing()) {
      sendEmail.fireEvent(new KeyPressHelper.ButtonClickEvent());
      return true;
    } else return false;
  }
}
