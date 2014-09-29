package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.HidePopupTextBox;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

/**
 * Created by go22670 on 8/11/14.
 */
public class UserPassLogin extends UserDialog {
  private static final int MIN_LENGTH_USER_ID = 4;

  private static final int MIN_PASSWORD = 5;
  private static final int MIN_EMAIL = 13;
  private static final int LEFT_SIDE_WIDTH = 483;
  private static final String SIGN_UP_SUBTEXT = "Or add missing info";//password and email";
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
  private static final String FIRST_BULLET = "Practice vocabulary with audio flashcards";//"Do flashcards to learn or review vocabulary";
  private static final String SECOND_BULLET = "Record your voice and get feedback on your pronunciation";//"Get feedback on your pronunciation";
  private static final String THIRD_BULLET = "Create and share vocab lists for study and review";//"Make your own lists of words to study later or to share.";
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
  private static final String ARE_YOU_A = "Please select one : Are you a";
  private static final String STUDENT = "Student";
  private static final String TEACHER = "Teacher";
  private static final String CONTENT_DEVELOPER = "Content Developer";
  private static final String SIGN_UP_WIDTH = "266px";
  //public static final String PLEASE_ENTER_A_PASSWORD1 = "Please enter a password.";
  private final UserManager userManager;
  private final KeyPressHelper enterKeyButtonHelper;
  private FormField user;
  private FormField signUpUser;
  private FormField signUpEmail;
  private FormField signUpPassword;
  private FormField password;
  private boolean signInHasFocus = true;
  private EventRegistration eventRegistration;
  private Button signIn;

  /**
   * @param service
   * @param props
   * @param userManager
   * @param eventRegistration
   * @see mitll.langtest.client.LangTest#showLogin
   */
  public UserPassLogin(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager, EventRegistration eventRegistration) {
    super(service, props);

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

  public void getRightLogin(Panel leftAndRight) {
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

    Panel hp2 = new HorizontalPanel();

    Anchor forgotUser = getForgotUser();
    hp2.add(forgotUser);
    forgotUser.addStyleName("leftTenMargin");

    Anchor forgotPassword = getForgotPassword();
    hp2.add(forgotPassword);
    fieldset.add(hp2);

    forgotPassword.addStyleName("leftFiveMargin");

    getFocusOnField(user);

    return signInForm;
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
  }


  private DecoratedPopupPanel resetEmailPopup;
  private Button sendEmail;

  public Anchor getForgotPassword() {
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
              markError(emailEntry, "Please check", VALID_EMAIL, Placement.TOP);
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
                setupPopover(sendEmail, heading, message, Placement.LEFT, 5000, new MyPopover() {
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
                });
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

  public Anchor getForgotUser() {
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
              markError(emailEntry,
                  "Please check",
                  VALID_EMAIL, Placement.TOP);
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

                  setupPopover(sendUsernameEmail, CHECK_EMAIL, PLEASE_CHECK_YOUR_EMAIL, Placement.LEFT, 5000, new MyPopover() {
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
                  });
                }
              }
            });
          }
        });
        eventRegistration.register(sendUsernameEmail, "N/A", "send username");

        makePopup(sendUsernamePopup, emailEntry, sendUsernameEmail, "Enter your email to get your username.");
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

  public boolean isValidEmail(String text) {
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

  protected void getFocusOnField(final FormField user) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        user.box.setFocus(true);
      }
    });
  }

  private Button signUp;

  public Form getSignUpForm() {
    Form form = getSignInForm();

    Fieldset fieldset = new Fieldset();
    Heading w = new Heading(3, SIGN_UP, SIGN_UP_SUBTEXT);
    w.addStyleName("signUp");
    form.add(w);
    form.add(fieldset);

    signUpUser = addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, 20, USERNAME);
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

    Heading w1 = new Heading(5, ARE_YOU_A);
    fieldset.add(w1);
    w1.addStyleName("leftTenMargin");
    w1.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    w1.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    fieldset.add(getShowGroup());
    getSignUpButton(userBox, emailBox);

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
    registrationInfo.getGenderGroup().box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
      }
    });

    registrationInfo.setVisible(false);
    fieldset.add(signUp);

    return form;
  }

  private RegistrationInfo registrationInfo;
  private User.Kind selectedRole = User.Kind.STUDENT;

  /**
   *
   * @return
   * @see #getSignUpForm()
   */
  private DivWidget getShowGroup() {
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
      }
    }));
    buttonGroup.add(getChoice(TEACHER, false, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = User.Kind.TEACHER;
        registrationInfo.setVisible(false);
      }
    }));

    buttonGroup.add(getChoice(CONTENT_DEVELOPER, false, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedRole = User.Kind.CONTENT_DEVELOPER;
        registrationInfo.setVisible(true);
      }
    }));

    Style style = w.getElement().getStyle();
    style.setMarginLeft(9, Style.Unit.PX);

    return w;
  }

  private Button getChoice(String title, boolean isActive, ClickHandler handler) {
    Button onButton = new Button(title);
    onButton.getElement().setId("Choice_" + title);
    eventRegistration.register(onButton, "N/A");
    onButton.addClickHandler(handler);
    onButton.setActive(isActive);
    return onButton;
  }

  public void getSignUpButton(final TextBoxBase userBox, final TextBoxBase emailBox) {
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
  }

  private void gotSignUp(String user, String password, String email, User.Kind kind) {
    String passH = Md5Hash.getHash(password);
    String emailH = Md5Hash.getHash(email);

    boolean isCD = kind == User.Kind.CONTENT_DEVELOPER;
    String gender = isCD ? registrationInfo.getGenderGroup().box.getValue() : "male";
    String age = isCD ? registrationInfo.getAgeEntryGroup().getText() : "";
    int age1 = isCD ? Integer.parseInt(age) : 0;
    String dialect = isCD ? registrationInfo.getDialectGroup().getText() : "unk";

    signUp.setEnabled(false);

 //   System.out.println("kind is " +kind);
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
              eventRegistration.logEvent(signUp, "signing up", "N/A", "Tried to sign up, but existing user.");
              signUp.setEnabled(true);
              markError(signUpUser, "User exists already, please sign in or choose a different name.");
            } else {
              if (result.isEnabled()) {
                eventRegistration.logEvent(signUp, "signing up", "N/A", "successful sign up.");
                storeUser(result);
              } else {
                eventRegistration.logEvent(signUp, "signing up", "N/A", "successful sign up but waiting for approval from Tamas.");
                markError(signUp, "Wait for approval", "You will get an approval message by email.", Placement.LEFT);
                Timer t = new Timer() {
                  @Override
                  public void run() {
                    Window.Location.reload();

                  }
                };
                t.schedule(4000);
              }
            }
          }
        });
  }

  public void getLeftIntro(Panel leftAndRight) {
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeft");
    left.setWidth(LEFT_SIDE_WIDTH + "px");
    leftAndRight.add(left);
    int size = 1;
    int subSize = size + 2;
    Heading w2 = new Heading(size, INITIAL_PROMPT);
    left.add(w2);
    w2.getElement().getStyle().setTextAlign(Style.TextAlign.LEFT);
    Heading w1 = new Heading(subSize, FIRST_BULLET);//, "Speak your answers. Compete with your friends.");
    left.add(w1);
    w1.getElement().getStyle().setMarginTop(20, Style.Unit.PX);

    //Node child2 = w1.getElement().getChild(1);

    //com.google.gwt.dom.client.Element as = com.google.gwt.dom.client.Element.as(child2);
    //as.getStyle().setFontSize(16, Style.Unit.PX);
    Heading w = new Heading(subSize, SECOND_BULLET);//, "Compare yourself to a native speaker.");
    left.add(w);
    w.getElement().getStyle().setMarginTop(20, Style.Unit.PX);

    //child2 = w.getElement().getChild(1);
    //as = com.google.gwt.dom.client.Element.as(child2);
    //as.getStyle().setPaddingLeft(2, Style.Unit.PX);
    //as.getStyle().setFontSize(16, Style.Unit.PX);
    Heading w3 = new Heading(subSize, THIRD_BULLET);
    left.add(w3);
    w3.getElement().getStyle().setMarginTop(20, Style.Unit.PX);

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
            signUpUser.box.setText(result.getUserID());
            signUpPassword.box.setText(password.getText());
            getFocusOnField(signUpEmail);
            eventRegistration.logEvent(signIn, "sign in", "N/A", "copied info to sign up form");

            markError(signUpEmail, "Add info", "Current users should add an email and password.", Placement.TOP);
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

  public void storeUser(User result) {
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
