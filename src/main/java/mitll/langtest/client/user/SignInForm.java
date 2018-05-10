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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.user.LoginResult;
import mitll.langtest.shared.user.User;

import javax.servlet.http.HttpSession;
import java.util.logging.Logger;

public class SignInForm extends UserDialog implements SignIn {
  private final Logger logger = Logger.getLogger("SignInForm");

  static final String NO_SPACES = "Please no spaces in user id.";

  /**
   * @see #gotGoodPassword
   * @see #gotLogin
   */
  private static final String DEACTIVATED = "I'm sorry, this account has been deactivated.";
  private static final String APPLICATION_PERMISSION = "I'm sorry, you do not have permission to access this application.";
  private static final String TROUBLE_CONNECTING_TO_SERVER = "Trouble connecting to server.";
  /**
   * @see #checkUserExists
   * @see #handleLoginResponse
   */
  private static final String NO_USER_FOUND = "No user with this id - have you signed up?";

  /**
   * TODO : for legacy back compatibility = new log ins have to be longer
   */
  private static final int MIN_LENGTH_USER_ID = 4;

  private static final int MIN_PASSWORD = 4;
  /**
   * @see #getSignInButton
   */
  private static final String PLEASE_ENTER_YOUR_PASSWORD = "Please enter your password.";
  private static final String BAD_PASSWORD = "Wrong password, please try again.";
  private static final String PASSWORD = "Password";
  private static final String USERNAME = "Username";
  private static final String SIGN_IN = "Log In";
  private static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer user id.";

  /**
   * @see #getForgotPassword
   */
  private static final String FORGOT_PASSWORD = "Forgot password?";
  /**
   * @see #getForgotPassword
   */
  private static final String ENTER_A_USER_NAME = "Enter a user name.";
  private static final String SIGN_UP_WIDTH = "266px";

  private FormField userField;
  /**
   * @see #populateSignInForm
   */
  private FormField password;

  private Button signIn;

  private final EventRegistration eventRegistration;
  private final UserPassDialog userPassLogin;
  private final SignUp signUpForm;
  private final UserManager userManager;
  private SendEmail sendEmail;

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

    password = addPasswordField(fieldset, hp);

    Button signInButton = getSignInButton();
    enterKeyButtonHelper.addKeyHandler(signIn);
    hp.add(signInButton);

    fieldset.add(hp);
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
    userField.box.getElement().setId("Username_Box_SignIn");
    userField.box.setWidth(SIGN_UP_WIDTH);

    userField.box.addFocusHandler(event -> {
      userPassLogin.setSignInHasFocus();
      eventRegistration.logEvent(userField.box, "UserNameBox", "N/A", "focus in username field");
    });

    sendEmail = new SendEmail(eventRegistration);
  }

  /**
   * @param fieldset
   * @param hp
   * @return
   */
  private FormField addPasswordField(Fieldset fieldset, Panel hp) {
    FormField password = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    TextBoxBase box = password.box;
    box.addFocusHandler(event -> {
      userPassLogin.setSignInHasFocus();
      eventRegistration.logEvent(userField.box, "PasswordBox", "N/A", "focus in password field");
    });

    hp.add(password.getGroup());
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

    signIn.addStyleName("rightFiveMargin");
    signIn.addStyleName("leftFiveMargin");

    signIn.setType(ButtonType.PRIMARY);

    signIn.addClickHandler(event -> tryLogin());

    return signIn;
  }

  /**
   *
   */
  public void tryLogin() {
    tryLoginWithUserID(userField.box.getValue());
  }

  private void tryLoginWithUserID(String userID) {
    if (hasSpaces(userID)) {
      eventRegistration.logEvent(signIn, "TextBox", "N/A", "no spaces in userid '" + userID + "'");
      checkLegacyUserWithSpaces(userID);
    } else {
      int length = userID.length();
      if (length < MIN_LENGTH_USER_ID) {
        markErrorBlur(userField, PLEASE_ENTER_A_LONGER_USER_ID);
      } else {
        if (length == 4) {
          userID += "_"; // old netprof 1 accounts could be 4 long.
        }
        String passwordText = password.box.getValue();
        if (!passwordText.isEmpty() && passwordText.length() < MIN_PASSWORD) {
          markErrorBlur(password, "Please enter a password longer than " + MIN_PASSWORD + " characters.");
        } else if (passwordText.isEmpty()) {
          // final String text = userField.getSafeText();

          if (!userID.isEmpty()) {
            checkUserExists(userID);
          }
        } else {
          gotLogin(userID, passwordText);
        }
      }
    }
  }

  private void checkLegacyUserWithSpaces(String userID) {
    String testUserID = normalizeSpaces(userID);

  //  logger.warning("checking using "+ openUserService);
 //   logger.warning("checking using "+ openUserService.getClass());

    openUserService.isKnownUser(testUserID, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        logger.warning("\tgot FAILURE on checkLegacyUserWithSpaces isKnownUser " + testUserID);
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          //    logger.info("try again with " + testUserID);
          tryLoginWithUserID(testUserID);
        } else {
          //     logger.info("nobody with " + testUserID);
          markErrorBlur(userField, NO_SPACES, Placement.BOTTOM);
        }
      }
    });
  }

  private void checkUserExists(String text) {
    logger.warning("checkUserExists checking using "+ openUserService);

    logger.warning("checkUserExists checking using "+ openUserService.getClass());

    openUserService.isKnownUser(text, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        logger.warning("\tgot FAILURE on checkUserExists isKnownUser " + text);
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          checkUserValid(text);
        } else {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "empty password and unknown user");
          markErrorBlur(userField, NO_USER_FOUND, Placement.BOTTOM);
          signIn.setEnabled(true);
        }
      }
    });
  }

  private void checkUserValid(String text) {
    openUserService.isValidUser(text, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        logger.warning("\tgot FAILURE on checkUserValid userExists " + text);
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "empty password");
          markErrorBlur(password, PLEASE_ENTER_YOUR_PASSWORD);
        }
        signIn.setEnabled(true);
      }
    });
  }

  /**
   * @param user
   * @param freeTextPassword
   * @see #getSignInButton
   */
  private void gotLogin(String user, final String freeTextPassword) {
  logger.info("gotLogin : userField is '" + user + "' freeTextPassword " + freeTextPassword.length() + " characters" //+
    );
//    String before = user;

    if (user != null) {
      String trim = user.trim();
      user = normalizeSpaces(trim);
 //     if (!user.equals(before)) {
 //       logger.info("hack for spaces - user now " + user + " was " + before);
   //   }
    } else {
      logger.warning("huh? user id is null?");
    }

    final String fUser = user;

    signIn.setEnabled(false);
    openUserService.loginUser(user, freeTextPassword,
        new AsyncCallback<LoginResult>() {
          @Override
          public void onFailure(Throwable caught) {
            signIn.setEnabled(true);
            markErrorBlur(signIn, TROUBLE_CONNECTING_TO_SERVER);
          }

          @Override
          public void onSuccess(LoginResult result) {
            handleLoginResponse(result, fUser, freeTextPassword);
          }
        });
  }

  private void handleLoginResponse(LoginResult result, String user, String freeTextPassword) {
    if (result.getResultType() == LoginResult.ResultType.Failed) {
      eventRegistration.logEvent(signIn, "sign in", "N/A", "unknown userField " + user);
      logger.info("handleLoginResponse No userField with that name '" + user +
          "' freeTextPassword " + freeTextPassword.length() + " characters - ");

      markErrorBlur(password, NO_USER_FOUND, Placement.BOTTOM);
      signIn.setEnabled(true);
    } else {
      User loggedInUser = result.getLoggedInUser();
      if (!loggedInUser.isEnabled()) {
        markErrorBlur(userField, DEACTIVATED, Placement.BOTTOM);
        signIn.setEnabled(true);
      } else if (!loggedInUser.isHasAppPermission()) {
        markErrorBlur(userField, APPLICATION_PERMISSION, Placement.BOTTOM);
        signIn.setEnabled(true);
      } else {
        //   logger.info("handleLoginResponse user is enabled... result " + result.getResultType());
        /**
         * {@link mitll.langtest.server.database.security.NPUserSecurityManager#getValidLogin}
         */
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
    markErrorBlur(password, PLEASE_ENTER_YOUR_PASSWORD);
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
    forgotPassword.addClickHandler(event -> {
      String safeText = userField.getSafeText();
      if (safeText.isEmpty()) {
        markErrorBlur(userField, ENTER_A_USER_NAME);
      } else {
        if (safeText.length() == 4) safeText += "_";
        sendEmailIfExists(forgotPassword, safeText);
      }
    });
    return forgotPassword;
  }

  private void sendEmailIfExists(Anchor forgotPassword, String safeText) {
    openUserService.isKnownUserWithEmail(safeText, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        logger.warning("\tgot FAILURE on sendEmailIfExists ");
      }

      @Override
      public void onSuccess(Boolean isValid) {
        if (isValid) {
          sendEmail.showSendEmail(forgotPassword, safeText, true);
        } else {
//          String testUserID = normalizeSpaces(safeText);
  //        if (testUserID.equalsIgnoreCase(safeText)) {
            markErrorBlur(userField, NO_USER_FOUND, Placement.BOTTOM);
    //      } else {
      //      sendEmailIfExists(forgotPassword, testUserID);
        //  }
        }
      }
    });
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
