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
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.User;

import java.util.logging.Logger;

public class SignInForm extends UserDialog implements SignIn {
  private final Logger logger = Logger.getLogger("SignInForm");

  /**
   * @see #gotGoodPassword
   * @see #gotLogin
   */
  private static final String DEACTIVATED = "I'm sorry, this account has been deactivated.";
  private static final String TROUBLE_CONNECTING_TO_SERVER = "Trouble connecting to server.";
  /**
   *
   */
  private static final String NO_USER_FOUND = "No user with this id - have you signed up?";

  private static final int MIN_LENGTH_USER_ID = 4;

  private static final int MIN_PASSWORD = 4;
  /**
   * @see #getSignInButton
   */
  private static final String PLEASE_ENTER_YOUR_PASSWORD = "Please enter your password.";
  private static final String BAD_PASSWORD = "Wrong password, please try again.";// - have you signed up?";
  private static final String PASSWORD = "Password";
  private static final String USERNAME = "Username";
  private static final String SIGN_IN = "Log In";
  private static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer userField id.";

  /**
   * @see #getForgotPassword
   */
  private static final String FORGOT_PASSWORD = "Forgot password?";
  /**
   * @see #getForgotPassword
   */
  private static final String ENTER_A_USER_NAME = "Enter a user name.";
  //  private static final String CHECK_EMAIL = "Check Email";
//  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email";
//  private static final String SEND = "Send Reset Email";
  private static final String SIGN_UP_WIDTH = "266px";
//  private static final int EMAIL_POPUP_DELAY = 4000;

  private FormField userField;
  /**
   * @see #populateSignInForm
   */
  private FormField password;

  private Button signIn;

  //private DecoratedPopupPanel resetEmailPopup;
  // private Button sendEmail;
  private final EventRegistration eventRegistration;
  private final UserPassDialog userPassLogin;
  private final SignUp signUpForm;
  private final UserManager userManager;
  SendEmail sendEmail;

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

    password = addPasswordField(fieldset, hp);

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

    sendEmail = new SendEmail(eventRegistration, userField);
  }

  private FormField addPasswordField(Fieldset fieldset, Panel hp) {
    FormField password;
    password = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    TextBoxBase box = password.box;
    box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        userPassLogin.setSignInHasFocus();
        eventRegistration.logEvent(userField.box, "PasswordBox", "N/A", "focus in password field");
      }
    });

    hp.add(box);
    box.addStyleName("rightFiveMargin");

    return password;
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
        tryLogin();
      }
    });
    signIn.addStyleName("rightFiveMargin");
    signIn.addStyleName("leftFiveMargin");

    signIn.setType(ButtonType.PRIMARY);
    return signIn;
  }

  public void tryLogin() {
    String userID = userField.box.getValue();

    String[] split = userID.split("\\s");
    if (split.length > 1) {//s.length() != userID.length()) {
      eventRegistration.logEvent(signIn, "TextBox", "N/A", "no spaces in userid '" + userID + "'");
      markErrorBlur(userField, "Please no spaces in user id.");
    } else if (userID.length() < MIN_LENGTH_USER_ID) {
      markErrorBlur(userField, PLEASE_ENTER_A_LONGER_USER_ID);
    } else {
      String value = password.box.getValue();
      if (!value.isEmpty() && value.length() < MIN_PASSWORD) {
        markErrorBlur(password, "Please enter a password longer than " + MIN_PASSWORD + " characters.");
      } else if (value.isEmpty()) {
        final String text = userField.getSafeText();

        if (!text.isEmpty()) {
          service.getUserByID(text, new AsyncCallback<User>() {
            @Override
            public void onFailure(Throwable caught) {
              logger.warning("\tgot FAILURE on userExists " + text);
            }

            @Override
            public void onSuccess(User result) {
              if (result == null || result.isValid()) {
                eventRegistration.logEvent(signIn, "sign in", "N/A", "empty password");
                markErrorBlur(password, PLEASE_ENTER_YOUR_PASSWORD);
              }
              signIn.setEnabled(true);
            }
          });
        }
      } else {
        gotLogin(userID, value);
      }
    }
  }

  /**
   * @param user
   * @param freeTextPassword
   * @see #getSignInButton
   */
  private void gotLogin(final String user, final String freeTextPassword) {
    //  final String hashedPass = Md5Hash.getHash(freeTextPassword);
    logger.info("gotLogin : userField is '" + user + "' freeTextPassword " + freeTextPassword.length() + " characters" //+
        //    " or '" + hashedPass + "'"
    );

    signIn.setEnabled(false);

    userManager.getUserService().loginUser(user, freeTextPassword,
        new AsyncCallback<LoginResult>() {
          @Override
          public void onFailure(Throwable caught) {
            signIn.setEnabled(true);
            markErrorBlur(signIn, TROUBLE_CONNECTING_TO_SERVER);
          }

          @Override
          public void onSuccess(LoginResult result) {
            handleLoginResponse(result, user, freeTextPassword);
          }
        });
  }

  private void handleLoginResponse(LoginResult result, String user, String freeTextPassword) {
    if (result.getResultType() == LoginResult.ResultType.Failed) {
      eventRegistration.logEvent(signIn, "sign in", "N/A", "unknown userField " + user);
      logger.info("No userField with that name '" + user +
          "' freeTextPassword " + freeTextPassword.length() + " characters - ");

      markErrorBlur(password, NO_USER_FOUND);
      signIn.setEnabled(true);
    } else {
      User loggedInUser = result.getLoggedInUser();
      if (!loggedInUser.isEnabled()) {
        markErrorBlur(userField, DEACTIVATED);
        signIn.setEnabled(true);
      } else {
        //    logger.info("user is enabled...");
        if (result.getResultType() == LoginResult.ResultType.MissingInfo) {
          copyInfoToSignUp(user, loggedInUser);
          signIn.setEnabled(true);
        } else {
          gotGoodOrBadPassword(result);
        }
      }
    }
  }

  private void gotGoodOrBadPassword(LoginResult result) {
    if (result == null || result.getResultType() != LoginResult.ResultType.Success) {
      gotBadPassword(/*result.getLoggedInUser(), freeTextPassword*/);
    } else {
      gotGoodPassword(result.getLoggedInUser()
      );
    }
  }

  private void gotGoodPassword(User foundUser) {
    String user = foundUser.getUserID();
    if (foundUser.isEnabled()) {
      eventRegistration.logEvent(signIn, "sign in", "N/A", "successful sign in for " + user);
      //    logger.info("Got valid userField " + userField + " and matching password, so we're letting them in.");
      storeUser(foundUser, userManager);
    } else {
      eventRegistration.logEvent(signIn, "sign in", "N/A", "successful sign in for " + user + " but wait for approval.");
      markErrorBlur(signIn, "I'm sorry", DEACTIVATED, Placement.LEFT);
      signIn.setEnabled(true);
    }
  }

  /**
   * TODO : Maybe for now magic pass won't work? think of something better!
   *
   * @paramx foundUser
   * @paramx freeTextPassword
   */
  private void gotBadPassword(/*User foundUser, String freeTextPassword*/) {
    //String enteredPass = Md5Hash.getHash(password.getText());
//    if (freeTextPassword.equals(MAGIC_PASS)) {  // TODO : do masquerade
//      // special pathway...
//      String user = foundUser.getUserID();
//      eventRegistration.logEvent(signIn, "sign in", "N/A", "sign in as userField '" + user + "'");
//      storeUser(foundUser, userManager, freeTextPassword);
//    } else {


    //logger.info("foundExistingUser bad pass  " + freeTextPassword);
    //  logger.info("admin " + Md5Hash.getHash("adm!n"));
    eventRegistration.logEvent(signIn, "sign in", "N/A", "bad password");
    markErrorBlur(password, BAD_PASSWORD);
    signIn.setEnabled(true);

    //}
  }

  /**
   * Don't enable the teacher choice for legacy users, b/c it lets them skip over the
   * recorder/not a recorder choice.
   *
   * @param userID
   * @seex #foundExistingUser
   * @see #makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   */
  private void copyInfoToSignUp(String userID, User candidate) {
    signUpForm.copyInfoToSignUp(
        userID,
        candidate);
    eventRegistration.logEvent(signIn, "sign in", "N/A", "copied info to sign up form");
  }

  /**
   * @see UserPassLogin#showSuggestApp
   */
  @Override
  public void setFocusOnUserID() {
    setFocusOn(userField.box);
  }

  @Override
  public void setFocusPassword() {
    setFocusOn(password.box);
    markErrorBlur(password, "Please enter your password.");
  }

  /**
   * Cases:
   * 1) user is known and has an email hash
   * 2) user is known and has a valid email
   * 3) user is unknown
   *
   * @return
   * @see UserPassLogin#getForgotRow
   */
  @Override
  public Anchor getForgotPassword() {
    final Anchor forgotPassword = new Anchor(FORGOT_PASSWORD);
    forgotPassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String safeText = userField.getSafeText();
        if (safeText.isEmpty()) {
          markErrorBlur(userField, ENTER_A_USER_NAME);
          return;
        } else {

          service.getUserByID(safeText, new AsyncCallback<User>() {
            @Override
            public void onFailure(Throwable caught) {
              logger.warning("\tgot FAILURE on userExists ");
            }

            @Override
            public void onSuccess(User result) {
              if (result == null) {
                markErrorBlur(userField, NO_USER_FOUND);
              } else {
                //  if (result.isValid()) {
                sendEmail.showSendEmail(forgotPassword, safeText, result.isValid());
                //} else {
                //copyInfoToSignUp(safeText, result);
                // }
              }
            }
          });
        }
      }
    });
    return forgotPassword;
  }

  /**
   * @return
   * @see UserPassLogin#UserPassLogin
   */
  @Override
  public boolean clickSendEmail() {
    return sendEmail.clickSendEmail();
  }
}
