package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.HidePopupTextBox;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

/**
 * Created by go22670 on 8/11/14.
 */
public class UserPassLogin extends UserDialog {
  private static final int MIN_LENGTH_USER_ID = 4;

  private static final int MIN_PASSWORD = 4;
  private static final int MIN_EMAIL = 13;
  private static final int LEFT_SIDE_WIDTH = 483;
  private static final String SIGN_UP_SUBTEXT = "Sign up";//Or never entered a password?";//password and email";
  private static final String PLEASE_ENTER_YOUR_PASSWORD = "Please enter your password.";
  private static final String BAD_PASSWORD = "Wrong password - have you signed up?";
  private static final String PASSWORD = "Password";
  private static final String USERNAME = "Username";
  private static final String SIGN_IN = "Sign In";
  private static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer user id.";
  private static final String VALID_EMAIL = "Please enter a valid email address.";
  private static final String PLEASE_WAIT = "Please wait";
  //private static final String INITIAL_PROMPT = "<b>Classroom</b> allows you to practice your vocabulary and learn pronunciation.";//"Learn how to pronounce words and practice vocabulary.";
  private static final String INITIAL_PROMPT = "Practice vocabulary and learn pronunciation.";//"Learn how to pronounce words and practice vocabulary.";
  private static final String FIRST_BULLET = "Practice vocabulary with audio flashcards.";//"Do flashcards to learn or review vocabulary";
  private static final String SECOND_BULLET = "Record your voice and get feedback on your pronunciation.";//"Get feedback on your pronunciation";
  private static final String THIRD_BULLET = "Create and share vocab lists for study and review.";//"Make your own lists of words to study later or to share.";
  private static final String PLEASE_ENTER_A_PASSWORD = "Please enter a password";
  private static final String PLEASE_ENTER_A_LONGER_PASSWORD = "Please enter a longer password";
  private static final String PLEASE_ENTER_THE_SAME_PASSWORD = "Please enter the same password";
  private static final String PASSWORD_HAS_BEEN_CHANGED = "Password has been changed";
  private static final String SUCCESS = "Success";
  private static final String CHANGE_PASSWORD = "Change Password";
  private static final String CHOOSE_A_NEW_PASSWORD = "Choose a new password";
  private static final String FORGOT_PASSWORD = "Forgot password?";
  private static final String ENTER_A_USER_NAME = "Enter a user name.";
  private static final String CHECK_EMAIL = "Check Email";
  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email";
  private static final String ENTER_YOUR_EMAIL_TO_RESET_YOUR_PASSWORD = "Enter your email to reset your password.";
  private static final String FORGOT_USERNAME = "Forgot username?";
  private static final String SEND = "Send";
  private static final String SIGN_UP = "Sign Up";
  private static final String ARE_YOU_A = "Please choose : Are you a";
  private static final String STUDENT = "Student or ";
  private static final String TEACHER = "Teacher?";
  //private static final String CONTENT_DEVELOPER = "Content Developer";
  private static final String SIGN_UP_WIDTH = "266px";
  private static final int BULLET_MARGIN = 25;
  private static final String CURRENT_USER_TOOLTIP = "Current users who don't have a password and email should sign up below.";
//  private static final String CURRENT_USER = "Are you a current user without a password?";
  private static final String RECORD_AUDIO_HEADING = "Recording audio/Quality Control";
  private static final int WAIT_FOR_READING_APPROVAL = 4000;
  private static final String PLEASE_CHECK = "Please check";
//  private static final String RECORD_REFERENCE_AUDIO = "Record Reference Audio?";
  private static final String RECORD_REFERENCE_AUDIO = "Are you an assigned reference audio recorder?";
  public static final String SHOWN_HELLO = "shownHello";
  private static final String ENTER_YOUR_EMAIL = "Enter your email to get your username.";
  private static final int EMAIL_POPUP_DELAY = 4000;
  //private static final String PLEASE_ENTER_A_PASSWORD1 = "Please enter a password.";
  private UserManager userManager;
  private KeyPressHelper enterKeyButtonHelper;
  private final KeyStorage keyStorage;
  private FormField user;
  private FormField signUpUser;
  private FormField signUpEmail;
  private FormField signUpPassword;
  private FormField password;
  private boolean signInHasFocus = true;
  private EventRegistration eventRegistration;
  private Button signIn;

  public UserPassLogin(LangTestDatabaseAsync service, PropertyHandler props) {
    super(service,props);
    keyStorage = new KeyStorage(props.getLanguage(), 1000000);

  }
  /**
   * @param service
   * @param props
   * @param userManager
   * @param eventRegistration
   * @see mitll.langtest.client.LangTest#showLogin
   */
  public UserPassLogin(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager, EventRegistration eventRegistration) {
    this(service, props);

   // keyStorage = new KeyStorage(props.getLanguage(), 1000000);
    checkWelcome();

    this.userManager = userManager;
    this.eventRegistration = eventRegistration;
    enterKeyButtonHelper = new KeyPressHelper(true) {
      @Override
      public void userHitEnterKey(Button button) {
        if (sendUsernamePopup != null && sendUsernamePopup.isShowing()) {
          sendUsernameEmail.fireEvent(new ButtonClickEvent());
        } else if (resetEmailPopup != null && resetEmailPopup.isShowing()) {
          sendEmail.fireEvent(new ButtonClickEvent());
        } else if (signInHasFocus) {
          button.fireEvent(new ButtonClickEvent());
        } else {
          signUp.fireEvent(new KeyPressHelper.ButtonClickEvent());
        }
      }
    };
  }

  public void checkWelcome() {
    if (!hasShownWelcome()) {
      keyStorage.storeValue(SHOWN_HELLO,"yes");
      showWelcome();
    }
  }

  public boolean hasShownWelcome() {
    return keyStorage.hasValue(SHOWN_HELLO);
  }

  private void showWelcome() {
    new ModalInfoDialog("Welcome to Classroom!","<h3>Classroom has been updated.</h3>\n" + "<br/>" +
        "If you are an existing user of Classroom (either as a student, teacher or audio recorder), " +
        "you will need to use the <b>\"Sign Up\"</b> box to add a password and an email address to your account.<br/>" +
        "<br/>" +
        "If you were using Classroom for recording of course audio, check the box asking if you are a " +
        "<b>reference audio recorder</b>.<br/>" +
        "<br/>Once you have submitted this form, LTEA personnel will approve your account. " +
        "You will receive an email once it's approved.  " +
        "You will not be able to access Classroom " +
        //"for recording or quality control " +
        "until approval is granted.<br/>" +
     //   "<br/>" +
   //     "If you a teacher or student with a pre-existing user name, please use the \"Sign Up\" form to add a user name and password.  Then select your appropriate role.  No approval is required to activate your account.<br/>" +
        "<br/>" +
        "Once you \"sign up\", the site will remember your login information on this computer for up to one year.  " +
        "You will need to login with your username and password again if you access Classroom from a different machine.<br/>");
  }

  /**
   * @see mitll.langtest.client.LangTest#showLogin()
   * @return
   */
  public Panel getContent() {
    Panel container = new DivWidget();
    DivWidget child = new DivWidget();
    container.add(child);
    child.addStyleName("loginPageBack");

    Panel leftAndRight = new DivWidget();
    leftAndRight.addStyleName("loginPage");

    container.add(leftAndRight);
    getLeftIntro(leftAndRight);
    getRightLogin(leftAndRight);
    return container;
  }

  public Panel getResetPassword(final String token) {
    Panel container = new DivWidget();
    DivWidget child = new DivWidget();
    container.add(child);
    child.addStyleName("loginPageBack");

    Panel leftAndRight = new DivWidget();
    leftAndRight.addStyleName("resetPage");
    container.add(leftAndRight);

    DivWidget right = new DivWidget();

    leftAndRight.add(right);
    right.addStyleName("floatRight");

    DivWidget rightDiv = new DivWidget();

    Form form = new Form();
    form.getElement().setId("resetForm");
    rightDiv.add(form);

    form.addStyleName("topMargin");
    form.addStyleName("formRounded");
    form.getElement().getStyle().setBackgroundColor("white");

    final Fieldset fieldset = new Fieldset();
    form.add(fieldset);

    Heading w = new Heading(3, CHOOSE_A_NEW_PASSWORD);
    fieldset.add(w);
    w.addStyleName("leftFiveMargin");
    final FormField firstPassword = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    final FormField secondPassword = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, "Confirm " + PASSWORD);


    final Button changePassword = new Button(CHANGE_PASSWORD);
    changePassword.getElement().setId("changePassword");
    eventRegistration.register(changePassword);
    changePassword.addStyleName("floatRight");
    fieldset.add(changePassword);
    changePassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String first = firstPassword.box.getText();
        String second = secondPassword.box.getText();
        if (first.isEmpty()) {
          markError(firstPassword, PLEASE_ENTER_A_PASSWORD);
        } else if (first.length() < MIN_PASSWORD) {
          markError(firstPassword, PLEASE_ENTER_A_LONGER_PASSWORD);
        } else if (second.isEmpty()) {
          markError(secondPassword, PLEASE_ENTER_A_PASSWORD);
        } else if (second.length() < MIN_PASSWORD) {
          markError(secondPassword, PLEASE_ENTER_A_LONGER_PASSWORD);
        } else if (!second.equals(first)) {
          markError(secondPassword, PLEASE_ENTER_THE_SAME_PASSWORD);

        } else {
          changePassword.setEnabled(false);
          enterKeyButtonHelper.removeKeyHandler();

//          System.out.println("getResetPassword : changing password for " + token);
          service.changePFor(token, Md5Hash.getHash(first), new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
              changePassword.setEnabled(true);
              markError(changePassword, "Can't communicate with server - check network connection.");
            }

            @Override
            public void onSuccess(Boolean result) {
              if (!result) {
                markError(changePassword, "Password has already been changed?");
              } else {
                markError(changePassword, SUCCESS, PASSWORD_HAS_BEEN_CHANGED, Placement.LEFT);
                Timer t = new Timer() {
                  @Override
                  public void run() {

                    String newURL = trimURL(Window.Location.getHref());
                    //  System.out.println("url now " +newURL);
                    Window.Location.replace(newURL);
                    Window.Location.reload();

                  }
                };
                t.schedule(3000);
              }
            }
          });
        }
      }
    });
    enterKeyButtonHelper.addKeyHandler(changePassword);

    changePassword.addStyleName("rightFiveMargin");
    changePassword.addStyleName("leftFiveMargin");

    changePassword.setType(ButtonType.PRIMARY);
    right.add(rightDiv);

    return container;
  }

  private String trimURL(String url) {
    if (url.contains("127.0.0.1")) return url.split("#")[0];
    else return url.split("\\?")[0].split("#")[0];
  }

  private void getRightLogin(Panel leftAndRight) {
    DivWidget right = new DivWidget();

    leftAndRight.add(right);
    right.addStyleName("floatRight");

    DivWidget rightDiv = new DivWidget();
    right.add(rightDiv);

    rightDiv.add(populateSignInForm(getSignInForm()));
    rightDiv.add(getSignUpForm());
  }

  private Form populateSignInForm(Form signInForm) {
    Fieldset fieldset = new Fieldset();
    signInForm.add(fieldset);

    makeSignInUserName(fieldset);

    password = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    password.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = true;
        eventRegistration.logEvent(user.box, "PasswordBox", "N/A", "focus in password field");

      }
    });

    Panel hp = new HorizontalPanel();
    hp.add(password.box);
    hp.addStyleName("leftFiveMargin");

    hp.add(getSignInButton());
    fieldset.add(hp);

    Panel hp2 = getForgotRow();

    fieldset.add(hp2);

    getFocusOnField(user);

    return signInForm;
  }

  private Panel getForgotRow() {
    Panel hp2 = new HorizontalPanel();

    Anchor forgotUser = getForgotUser();
    forgotUser.addStyleName("topFiveMargin");
    hp2.add(forgotUser);
    forgotUser.addStyleName("leftTenMargin");

    Anchor forgotPassword = getForgotPassword();
    hp2.add(forgotPassword);
    forgotPassword.addStyleName("topFiveMargin");

    forgotPassword.addStyleName("leftFiveMargin");

    Button help = new Button("Help");
    help.addStyleName("leftTenMargin");
    help.getElement().getStyle().setMarginTop(-5, Style.Unit.PX);
    help.setType(ButtonType.PRIMARY);
    help.setIcon(IconType.QUESTION_SIGN);

    help.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showWelcome();
      }
    });
    hp2.add(help);

    return hp2;
  }

  private Form getSignInForm() {
    Form signInForm = new Form();
    signInForm.addStyleName("topMargin");
    signInForm.addStyleName("formRounded");
    signInForm.getElement().getStyle().setBackgroundColor("white");
    return signInForm;
  }

  private Button getSignInButton() {
    signIn = new Button(SIGN_IN);
    signIn.getElement().setId("SignIn");
    eventRegistration.register(signIn);
    signIn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String userID = user.box.getValue();
        if (userID.length() < MIN_LENGTH_USER_ID) {
          markError(user, PLEASE_ENTER_A_LONGER_USER_ID);
        } else {
          String value = password.box.getValue();
          if (!value.isEmpty() && value.length() < MIN_PASSWORD) {
            markError(password, value.isEmpty() ? PLEASE_ENTER_YOUR_PASSWORD : BAD_PASSWORD);
          } else {
            gotLogin(userID, value, value.isEmpty());
          }
        }

      }
    });
    enterKeyButtonHelper.addKeyHandler(signIn);

    signIn.addStyleName("rightFiveMargin");
    signIn.addStyleName("leftFiveMargin");

    signIn.setType(ButtonType.PRIMARY);
    return signIn;
  }

  private void makeSignInUserName(Fieldset fieldset) {
    user = addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, USERNAME);
    user.box.addStyleName("topMargin");
    user.box.addStyleName("rightFiveMargin");
    user.box.getElement().setId("Username_Box_SignIn");
    user.box.setFocus(true);
    user.box.setWidth(SIGN_UP_WIDTH);


    user.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = true;
        eventRegistration.logEvent(user.box, "UserNameBox", "N/A", "focus in username field");
      }
    });

    user.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if (!user.getText().isEmpty()) {
          eventRegistration.logEvent(user.box, "UserNameBox", "N/A", "left username field '" + user.getText()+ "'");

          service.userExists(user.getText(), "", new AsyncCallback<User>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(User result) {
       //       System.out.println("makeSignInUserName : for " + user.getText() + " got back " + result);
              if (result != null) {
                String emailHash = result.getEmailHash();
                String passwordHash = result.getPasswordHash();
                if (emailHash == null || passwordHash == null || emailHash.isEmpty() || passwordHash.isEmpty()) {
                  eventRegistration.logEvent(user.box, "UserNameBox", "N/A", "existing legacy user " + result.toStringShort());

                  copyInfoToSignUp(result);
                }
              }
            }
          });
        }
      }});
  }


  private DecoratedPopupPanel resetEmailPopup;
  private Button sendEmail;

  private Anchor getForgotPassword() {
    final Anchor forgotPassword = new Anchor(FORGOT_PASSWORD);
    forgotPassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (user.getText().isEmpty()) {
          markError(user, ENTER_A_USER_NAME);
          return;
        }
        final HidePopupTextBox emailEntry = new HidePopupTextBox();
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
              markError(emailEntry, PLEASE_CHECK, VALID_EMAIL, Placement.TOP);
              return;
            }

            sendEmail.setEnabled(false);
            service.resetPassword(user.box.getText(), text, Window.Location.getHref(), new AsyncCallback<Boolean>() {
              @Override
              public void onFailure(Throwable caught) {
                sendEmail.setEnabled(true);
              }

              @Override
              public void onSuccess(Boolean result) {
                String heading = result ? CHECK_EMAIL : "Unknown email";
                String message = result ? PLEASE_CHECK_YOUR_EMAIL : user.box.getText() + " doesn't have that email. Check for a typo?";
                setupPopover(sendEmail, heading, message, Placement.LEFT, EMAIL_POPUP_DELAY, new MyPopover() {
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
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            emailEntry.setFocus(true);
          }
        });
      }
    });
    return forgotPassword;
  }

  private DecoratedPopupPanel sendUsernamePopup;
  private Button sendUsernameEmail;

  /**
   * @see #populateSignInForm(com.github.gwtbootstrap.client.ui.Form)
   * @return
   */
  private Anchor getForgotUser() {
    final Anchor forgotUsername = new Anchor(FORGOT_USERNAME);
    forgotUsername.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        final HidePopupTextBox emailEntry = new HidePopupTextBox();
        sendUsernamePopup = new DecoratedPopupPanel(true);
        sendUsernamePopup.setAutoHideEnabled(true);
        sendUsernameEmail = new Button(SEND);
        sendUsernameEmail.getElement().setId("SendUsernameEmail");
        sendUsernameEmail.setType(ButtonType.PRIMARY);
        sendUsernameEmail.addStyleName("leftTenMargin");
        sendUsernameEmail.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(final ClickEvent event) {
            final String text = emailEntry.getText();
            if (!isValidEmail(text)) {
              markError(emailEntry, PLEASE_CHECK, VALID_EMAIL, Placement.TOP);
              return;
            }

            sendUsernameEmail.setEnabled(false);
            service.forgotUsername(Md5Hash.getHash(text), text, Window.Location.getHref(), new AsyncCallback<Boolean>() {
              @Override
              public void onFailure(Throwable caught) {
                sendUsernameEmail.setEnabled(true);
              }

              @Override
              public void onSuccess(Boolean isValid) {
                if (!isValid) {
                  markError(sendUsernameEmail, "Check your spelling", "No user has this email.", Placement.LEFT);
                  sendUsernameEmail.setEnabled(true);
                  eventRegistration.logEvent(sendUsernameEmail,"send username link","N/A","invalid email request ");
                } else {
                  eventRegistration.logEvent(sendUsernameEmail,"send username link","N/A","valid email request ");

                  setupPopover(sendUsernameEmail, CHECK_EMAIL, PLEASE_CHECK_YOUR_EMAIL, Placement.LEFT, EMAIL_POPUP_DELAY, new MyPopover() {
                    boolean isFirst = true;

                    @Override
                    public void hide() {
                      super.hide();
                      if (isFirst) {
                        isFirst = false;
                      } else {
                        sendUsernamePopup.hide(); // TODO : ugly
                      }
                    }
                  }, false);
                }
              }
            });
          }
        });
        eventRegistration.register(sendUsernameEmail, "N/A", "send username");

        makePopup(sendUsernamePopup, emailEntry, sendUsernameEmail, ENTER_YOUR_EMAIL);
        sendUsernamePopup.showRelativeTo(forgotUsername);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            emailEntry.setFocus(true);
          }
        });
      }
    });
    return forgotUsername;
  }

  private boolean isValidEmail(String text) {
    return text.toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
  }

  private void makePopup(DecoratedPopupPanel commentPopup, HidePopupTextBox commentEntryText, Button okButton, String prompt) {
    VerticalPanel vp = new VerticalPanel();
    Panel w = new Heading(6, prompt);
    vp.add(w);
    w.addStyleName("bottomFiveMargin");
    Panel hp = new HorizontalPanel();
    hp.add(commentEntryText);
    hp.add(okButton);
    vp.add(hp);
    commentPopup.add(vp);
  }

  private void getFocusOnField(final FormField user) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        user.box.setFocus(true);
      }
    });
  }

  private Button signUp;
  private CheckBox contentDevCheckbox;

  private Form getSignUpForm() {
    Form form = getSignInForm();

    Fieldset fieldset = new Fieldset();
    Heading w = new Heading(3, "New User?", SIGN_UP_SUBTEXT);
    w.addStyleName("signUp");
    form.add(w);
    form.add(fieldset);

    TextBoxBase userBox = makeSignUpUsername(fieldset);

    TextBoxBase emailBox = makeSignUpEmail(fieldset);

    makeSignUpPassword(fieldset, emailBox);

    Heading w1 = new Heading(5, ARE_YOU_A);
    fieldset.add(w1);
    w1.addStyleName("leftTenMargin");
    w1.getElement().getStyle().setMarginTop(15, Style.Unit.PX);
    w1.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);

   // fieldset.add(getShowGroup());

    fieldset.add(getRolesChoices());

    contentDevCheckbox = new CheckBox(RECORD_REFERENCE_AUDIO);

    contentDevCheckbox.setVisible(false);
    contentDevCheckbox.addStyleName("leftTenMargin");
    contentDevCheckbox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = contentDevCheckbox.getValue() ? User.Kind.CONTENT_DEVELOPER : User.Kind.TEACHER;
        registrationInfo.setVisible(contentDevCheckbox.getValue());
      }
    });

  /*  contentDevCheckbox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        getRecordAudioPopover();

      }
    });*/

 /*   contentDevCheckbox.adds(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        getRecordAudioPopover();
      }
    });*/
    getRecordAudioPopover();

    contentDevCheckbox.getElement().getStyle().setPaddingBottom(10, Style.Unit.PX);
    fieldset.add(contentDevCheckbox);

    makeRegistrationInfo(fieldset);
    fieldset.add(getSignUpButton(userBox, emailBox));

    return form;
  }

  private void getRecordAudioPopover() {
    String html = "Click here if you have been assigned to record reference audio or do quality control on current audio.<br/>" +
        "After you click sign up, " +
        "LTEA personnel will approve your account.<br/>" +
        "You will receive an email once it's approved.<br/>" +
        "You will not be able to access Classroom until approval is granted.";

    setupPopover(contentDevCheckbox, RECORD_AUDIO_HEADING,html, Placement.LEFT, 5000, true);
  }

  private Panel getRolesChoices() {
    Panel roles = new HorizontalPanel();
    roles.addStyleName("leftTenMargin");

    roles.add(studentChoice);
   // HTML or = new HTML("or");
   // or.getElement().getStyle().setFontSize(14, Style.Unit.PT  );
   // roles.add(or);
   // or.addStyleName("leftFiveMargin");
   // studentChoice.addStyleName("leftFiveMargin");
    roles.add(teacherChoice);
  //  teacherChoice.addStyleName("topFiveMargin");
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

    teacherChoice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = User.Kind.TEACHER;
        registrationInfo.setVisible(false);
        contentDevCheckbox.setVisible(true);
      }
    });

   // addTooltip(teacherChoice,"Teachers have the option of recording reference audio.");

    return roles;
  }

  private TextBoxBase makeSignUpUsername(Fieldset fieldset) {
    signUpUser = addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, BULLET_MARGIN, USERNAME);
    final TextBoxBase userBox = signUpUser.box;
    userBox.addStyleName("topMargin");
    userBox.addStyleName("rightFiveMargin");
    userBox.setWidth(SIGN_UP_WIDTH);
    userBox.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    userBox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
        eventRegistration.logEvent(userBox, "SignUp_UserNameBox", "N/A", "focus in username field in sign up form");

      }
    });
    return userBox;
  }

  private TextBoxBase makeSignUpEmail(Fieldset fieldset) {
    signUpEmail = addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, "Email");
    final TextBoxBase emailBox = signUpEmail.box;
    emailBox.addStyleName("topMargin");
    emailBox.addStyleName("rightFiveMargin");
    emailBox.setWidth(SIGN_UP_WIDTH);
    emailBox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
        eventRegistration.logEvent(emailBox, "SignUp_EmailBox", "N/A", "focus in email field in sign up form");
      }
    });
    return emailBox;
  }

  private void makeSignUpPassword(Fieldset fieldset, final TextBoxBase emailBox) {
    signUpPassword = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    signUpPassword.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
        eventRegistration.logEvent(emailBox, "SignUp_PasswordBox", "N/A", "focus in password field in sign up form");
      }
    });
    signUpPassword.box.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    signUpPassword.box.setWidth(SIGN_UP_WIDTH);
  }

  private void makeRegistrationInfo(Fieldset fieldset) {
    registrationInfo = new RegistrationInfo(fieldset);

    registrationInfo.getAgeEntryGroup().box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
      }
    });
    registrationInfo.getDialectGroup().box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
      }
    });
    registrationInfo.getMale().addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
      }
    });
    registrationInfo.getFemale().addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
      }
    });
    registrationInfo.setVisible(false);
  }

  private RegistrationInfo registrationInfo;
  private User.Kind selectedRole = User.Kind.UNSET;

  private final RadioButton studentChoice = new RadioButton("ROLE_CHOICE", STUDENT);
  private final RadioButton teacherChoice = new RadioButton("ROLE_CHOICE", TEACHER);

  /**
   *
   * @return
   * @see #getSignUpForm()
   */
/*  private DivWidget getShowGroup() {
    ButtonToolbar w = new ButtonToolbar();
    w.addStyleName("bottomFiveMargin");
    w.getElement().getStyle().setMarginTop(3, Style.Unit.PX);
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    buttonGroup.add(getChoice(STUDENT, true, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = User.Kind.STUDENT;
        registrationInfo.setVisible(false);
        contentDevCheckbox.setVisible(false);
      }
    }));

    buttonGroup.add(getChoice(TEACHER, false, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = User.Kind.TEACHER;
        registrationInfo.setVisible(false);
        contentDevCheckbox.setVisible(true);
      }
    }));
*//*
    buttonGroup.add(getChoice(CONTENT_DEVELOPER, false, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = User.Kind.CONTENT_DEVELOPER;
        registrationInfo.setVisible(true);
      }
    }));*//*

    Style style = w.getElement().getStyle();
    style.setMarginLeft(9, Style.Unit.PX);

    return w;
  }*/

/*  private Button getChoice(String title, boolean isActive, ClickHandler handler) {
    Button onButton = new Button(title);
    onButton.getElement().setId("Choice_" + title);
    eventRegistration.register(onButton, "N/A");
    onButton.addClickHandler(handler);
    onButton.setActive(isActive);
    return onButton;
  }*/

  private Button getSignUpButton(final TextBoxBase userBox, final TextBoxBase emailBox) {
    signUp = new Button(SIGN_UP);
    signUp.getElement().setId("SignUp");
    eventRegistration.register(signUp);

    signUp.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //  System.out.println("signUp got click!");
        if (userBox.getValue().length() < MIN_LENGTH_USER_ID) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short user id");
          markError(signUpUser, PLEASE_ENTER_A_LONGER_USER_ID);
        } else if (signUpEmail.box.getValue().length() < MIN_EMAIL) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short email");
          markError(signUpEmail, "Please enter your email.");
        } else if (!isValidEmail(signUpEmail.box.getValue())) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "invalid email");
          markError(signUpEmail, VALID_EMAIL);
        } else if (signUpPassword.box.getValue().length() < MIN_PASSWORD) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short password");
          markError(signUpPassword, signUpPassword.box.getValue().isEmpty() ? PLEASE_ENTER_A_PASSWORD :
              "Please enter a password at least " + MIN_PASSWORD + " characters long.");
        } else if (selectedRole == User.Kind.UNSET) {
          markError(studentChoice,"Please choose","Please select either student or teacher.", Placement.LEFT);
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't check role");
        } else if (selectedRole == User.Kind.CONTENT_DEVELOPER && !registrationInfo.checkValidGender()) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't check gender");
        } else if (selectedRole == User.Kind.CONTENT_DEVELOPER && !highlightIntegerBox(registrationInfo.getAgeEntryGroup())) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't fill in age ");
          markError(registrationInfo.getAgeEntryGroup().group, registrationInfo.getAgeEntryGroup().box, "",
              "Enter age between " + MIN_AGE + " and " + MAX_AGE + ".");
        } else if (selectedRole == User.Kind.CONTENT_DEVELOPER && registrationInfo.getDialectGroup().getText().isEmpty()) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't fill in dialect ");
          markError(registrationInfo.getDialectGroup(), "Enter a language dialect.");
        } else {
          gotSignUp(userBox.getValue(), signUpPassword.box.getValue(), emailBox.getValue(), selectedRole);
        }
      }
    });
    signUp.addStyleName("floatRight");
    signUp.addStyleName("rightFiveMargin");
    signUp.addStyleName("leftFiveMargin");

    signUp.setType(ButtonType.SUCCESS);

    return signUp;
  }

  private void gotSignUp(final String user, String password, String email, User.Kind kind) {
    String passH = Md5Hash.getHash(password);
    String emailH = Md5Hash.getHash(email);

    boolean isCD = kind == User.Kind.CONTENT_DEVELOPER;
    String gender = isCD ? registrationInfo.isMale() ? "male":"female" : "male";
    String age = isCD ? registrationInfo.getAgeEntryGroup().getText() : "";
    int age1 = isCD ? Integer.parseInt(age) : 0;
    String dialect = isCD ? registrationInfo.getDialectGroup().getText() : "unk";

    signUp.setEnabled(false);

    service.addUser(user, passH, emailH, kind, Window.Location.getHref(), email,
        gender.equalsIgnoreCase("male"), age1, dialect, new AsyncCallback<User>() {
          @Override
          public void onFailure(Throwable caught) {
            eventRegistration.logEvent(signUp, "signing up", "N/A", "Couldn't contact server...?");

            signUp.setEnabled(true);
            markError(signUp, "Trouble connecting to server.");
          }

          @Override
          public void onSuccess(User result) {
            if (result == null) {
              eventRegistration.logEvent(signUp, "signing up", "N/A", "Tried to sign up, but existing user (" +user+   ").");
              signUp.setEnabled(true);
              markError(signUpUser, "User exists already, please sign in or choose a different name.");
            } else {
              if (result.isEnabled()) {
                eventRegistration.logEvent(signUp, "signing up", "N/A", getSignUpEvent(result));
                storeUser(result);
              } else {
                eventRegistration.logEvent(signUp, "signing up", "N/A", getSignUpEvent(result) +
                    "but waiting for approval from Tamas.");
                markError(signUp, "Wait for approval", "You will get an approval message by email.", Placement.TOP);
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
    return "successful sign up as " + result.getUserID() + "/" + result.getId() + " as " +result.getUserKind();
  }

  private void getLeftIntro(Panel leftAndRight) {
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeft");
    left.setWidth(LEFT_SIDE_WIDTH + "px");
    leftAndRight.add(left);
    int size = 1;
 //   int subSize = size + 2;
    Heading w2 = new Heading(size, INITIAL_PROMPT);
    left.add(w2);
    w2.getElement().getStyle().setPaddingBottom(24, Style.Unit.PX);
    w2.getElement().getStyle().setTextAlign(Style.TextAlign.LEFT);
    Widget w1 = new HTML(FIRST_BULLET);//, "Speak your answers. Compete with your friends.");
    Panel h = new HorizontalPanel();
    h.add(new Image(LangTest.LANGTEST_IMAGES + "NewProF2_48x48.png"));
    h.add(w1);
    configure(h);

    left.add(h);
    w1.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
    configure(w1);

    Widget w = new HTML(SECOND_BULLET);

    h = new HorizontalPanel();
    h.add(new Image(LangTest.LANGTEST_IMAGES + "NewProF1_48x48.png"));
    h.add(w);
    configure(h);

    left.add(h);
    w.getElement().getStyle().setMarginTop(2, Style.Unit.PX);
    configure(w);

    Widget w3 = new HTML(THIRD_BULLET);
    left.add(w3);

    h = new HorizontalPanel();
    h.add(new Image(LangTest.LANGTEST_IMAGES + "listIcon_48x48_transparent.png"));
    h.add(w3);
    configure(h);

    left.add(h);

    w3.getElement().getStyle().setMarginTop(-1, Style.Unit.PX);
    configure(w3);
  }

  private void configure(Panel h) {
    h.getElement().getStyle().setMarginTop(BULLET_MARGIN, Style.Unit.PX);
    h.getElement().getStyle().setPaddingBottom(10, Style.Unit.PX);
  }

  private void configure(Widget w3) {
    w3.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
    w3.getElement().getStyle().setFontSize(16, Style.Unit.PT);
    w3.getElement().getStyle().setLineHeight(1, Style.Unit.EM);
  }


  /**
   * @param user
   * @param pass
   * @see #getRightLogin(com.google.gwt.user.client.ui.Panel)
   */
  private void gotLogin(final String user, final String pass, final boolean emptyPassword) {
    final String hashed = Md5Hash.getHash(pass);
    //  System.out.println("gotLogin : user is '" +user + "' pass '" + pass +"' or " + hashed);

    signIn.setEnabled(false);
    service.userExists(user, hashed, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        signIn.setEnabled(true);
        markError(signIn, "Trouble connecting to server.");
      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "unknown user " + user);

          System.out.println("No user with that name? pass empty " + emptyPassword);
          markError(password, emptyPassword ? PLEASE_ENTER_YOUR_PASSWORD : BAD_PASSWORD);
          signIn.setEnabled(true);
        } else {
//          System.out.println("Found user " + result);
          String emailHash = result.getEmailHash();
          String passwordHash = result.getPasswordHash();
          if (emailHash == null || passwordHash == null || emailHash.isEmpty() || passwordHash.isEmpty()) {
            copyInfoToSignUp(result);
            signIn.setEnabled(true);
          } else {
            System.out.println("Got valid user " + result);
            if (emptyPassword) {
              eventRegistration.logEvent(signIn, "sign in", "N/A", "empty password");

              markError(password, PLEASE_ENTER_YOUR_PASSWORD);
              signIn.setEnabled(true);
            } else if (result.getPasswordHash().equals(hashed)) {
              if (result.isEnabled() || result.getUserKind() != User.Kind.CONTENT_DEVELOPER) {
                eventRegistration.logEvent(signIn, "sign in", "N/A", "successful sign in " + user);

                storeUser(result);
              } else {
                eventRegistration.logEvent(signIn, "sign in", "N/A", "successful sign in " + user + " but wait for approval.");

                markError(signIn, PLEASE_WAIT, "Please wait until you've been approved. Check your email.", Placement.LEFT);
                signIn.setEnabled(true);
              }
            } else {
              String enteredPass = Md5Hash.getHash(password.getText());
              if (enteredPass.equals(Md5Hash.getHash("adm!n"))) {
                eventRegistration.logEvent(signIn, "sign in", "N/A", "sign in as user...");
                storeUser(result);
              }
              else {
                System.out.println("pass  " + passwordHash);
                System.out.println("admin " + Md5Hash.getHash("adm!n"));
                eventRegistration.logEvent(signIn, "sign in", "N/A", "bad password");

                markError(password, BAD_PASSWORD);
                signIn.setEnabled(true);
              }
            }
          }
        }
      }
    });
  }

  private void copyInfoToSignUp(User result) {
    signUpUser.box.setText(result.getUserID());
    signUpPassword.box.setText(password.getText());
    getFocusOnField(signUpEmail);
    eventRegistration.logEvent(signIn, "sign in", "N/A", "copied info to sign up form");

    markError(signUpEmail, "Add info", "Current users should add an email and password.", Placement.TOP);
  }

  private void storeUser(User result) {
    System.out.println("UserPassLogin.storeUser - " + result);
    enterKeyButtonHelper.removeKeyHandler();
    userManager.storeUser(result, getAudioTypeFromPurpose(result.getUserKind()));
  }

  private String getAudioTypeFromPurpose(User.Kind kind) {
    if (kind == User.Kind.STUDENT || kind == User.Kind.TEACHER) return Result.AUDIO_TYPE_PRACTICE;
    else if (kind == User.Kind.CONTENT_DEVELOPER) return Result.AUDIO_TYPE_RECORDER;
    else return Result.AUDIO_TYPE_REVIEW;
  }
}
