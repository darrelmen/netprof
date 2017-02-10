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
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/11/14.
 */
public class UserPassLogin extends UserDialog {
  private static final String USERNAME_BOX_SIGN_IN = "Username_Box_SignIn";
  public static final String USER_NAME_BOX = "UserNameBox";
  private final Logger logger = Logger.getLogger("UserPassLogin");

  private static final String IPAD_LINE_1 = "Also consider installing the NetProF app, which is available on the DLI App Store.";// or";
 // private static final String IPAD_LINE_2 = "Or click this link to install <a href='https://np.ll.mit.edu/iOSNetProF/'>iOS NetProF" + "</a>.";
  private static final String IPAD_LINE_3 = "Otherwise, you will not be able to record yourself practicing vocabulary.";

  private static final String WAIT_FOR_APPROVAL = "Wait for approval";
  private static final String YOU_WILL_GET_AN_APPROVAL_MESSAGE_BY_EMAIL = "You will get an approval message by email.";

  private static final String MALE = "male";
  private static final String MAGIC_PASS = Md5Hash.getHash("adm!n");
  private static final String CURRENT_USERS = "Current users should add an email and password.";

  private static final int MIN_LENGTH_USER_ID = 4;

  private static final int MIN_PASSWORD = 4;
  private static final int LEFT_SIDE_WIDTH = 483;
  private static final String SIGN_UP_SUBTEXT = "Sign up";
  private static final String PLEASE_ENTER_YOUR_PASSWORD = "Please enter your password.";
  private static final String BAD_PASSWORD = "Wrong password, please try again.";// - have you signed up?";
  private static final String PASSWORD = "Password";
  private static final String USERNAME = "Username";
  private static final String SIGN_IN = "Log In";
  private static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer user id.";
  private static final String VALID_EMAIL = "Please enter a valid email address.";
  private static final String PLEASE_WAIT = "Please wait";
  private static final String SECOND_BULLET = "Record your voice and get feedback on your pronunciation.";//"Get feedback on your pronunciation";
  private static final String THIRD_BULLET = "Create and share vocab lists for study and review.";//"Make your own lists of words to study later or to share.";
  private static final String PLEASE_ENTER_A_PASSWORD = "Please enter a password";
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
  private static final String SIGN_UP_WIDTH = "266px";
  private static final int BULLET_MARGIN = 25;
  private static final String RECORD_AUDIO_HEADING = "Recording audio/Quality Control";
  private static final int WAIT_FOR_READING_APPROVAL = 3000;
  private static final String PLEASE_CHECK = "Please check";
  private static final String RECORD_REFERENCE_AUDIO = "Are you an assigned reference audio recorder?";
  private static final String ENTER_YOUR_EMAIL = "Enter your email to get your username.";
  private static final int EMAIL_POPUP_DELAY = 4000;
  private static final String USER_EXISTS = "User exists already, please sign in or choose a different name.";
  private static final String HELP = "Help";
  private static final String AGE_ERR_MSG = "Enter age between " + MIN_AGE + " and " + MAX_AGE + ".";

  private static final String SHOWN_HELLO = "shownHello";

  private final UserManager userManager;
  private final KeyPressHelper enterKeyButtonHelper;
  private final KeyStorage keyStorage;
  private FormField user;
  private FormField signUpUser;
  private FormField signUpEmail;
  private FormField signUpPassword;
  private FormField password;
  private boolean signInHasFocus = true;
  private final EventRegistration eventRegistration;
  private Button signIn;

  /**
   * @param service
   * @param props
   * @param userManager
   * @param eventRegistration
   * @see mitll.langtest.client.LangTest#showLogin
   */
  public UserPassLogin(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager,
                       EventRegistration eventRegistration) {
    super(service, props);
    keyStorage = new KeyStorage(props.getLanguage(), 1000000);

    boolean willShow = checkWelcome();
    if (!willShow) {
      if (BrowserCheck.isIPad()) {
        showSuggestApp();
      }
    }

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

  private boolean checkWelcome() {
    if (!hasShownWelcome() && props.shouldShowWelcome()) {
      keyStorage.storeValue(SHOWN_HELLO, "yes");
      showWelcome();
      return true;
    } else return false;
  }

  private boolean hasShownWelcome() {
    return keyStorage.hasValue(SHOWN_HELLO);
  }

  private void showWelcome() {
    Modal modal = new ModalInfoDialog().getModal(props.getWelcomeMessage(),
        getLoginInfo(), null, new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            if (BrowserCheck.isIPad()) {
              showSuggestApp();
            } else {
              setFocusOnUserID();
            }
          }
        }, false);
    modal.setMaxHeigth((600) + "px");
    modal.show();
  }

  /**
   * Don't redirect them to download site just yet.
   */
  private void showSuggestApp() {
    List<String> messages = Arrays.asList(IPAD_LINE_1,
       // IPAD_LINE_2,
        IPAD_LINE_3);
    Modal modal = new ModalInfoDialog().getModal(
        "Install App?",
        messages,
        Collections.emptySet(),
        null, new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            setFocusOnUserID();
          }
        },
        true, true);
    modal.setMaxHeigth(600 + "px");
    modal.show();
  }

  private void showWelcome2() {
    new ModalInfoDialog("Login options", getLoginInfo());
  }

  private String getLoginInfo() {
    return props.getHelpMessage();
  }

  /**
   * @return
   * @see mitll.langtest.client.LangTest#showLogin()
   */
  public Panel getContent() {
    Panel container = new DivWidget();
    container.getElement().setId("UserPassLogin");

    DivWidget child = new DivWidget();
    container.add(child);
    child.addStyleName("loginPageBack");

    Panel leftAndRight = new DivWidget();
    leftAndRight.addStyleName("loginPage");

    container.add(leftAndRight);
    getLeftIntro(leftAndRight);
    getRightLogin(leftAndRight);
    leftAndRight.add(getLinksToSites());
    return container;
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

  private Panel getLinksToSites() {
    //Panel hp = new HorizontalPanel();
    DivWidget hp = new DivWidget();
    hp.getElement().setId("UserPassLogin_linksToSites");
    hp.getElement().getStyle().setMarginTop(40, Style.Unit.PX);
    hp.getElement().getStyle().setClear(Style.Clear.RIGHT);

    String sitePrefix = props.getSitePrefix();
    for (String site : props.getSites()) {
      Anchor w = new Anchor(site, sitePrefix + site.replaceAll("Mandarin", "CM"));
      w.getElement().getStyle().setMarginRight(5, Style.Unit.PX);
      w.addStyleName("floatLeftList");

      hp.add(w);
    }
    return hp;
  }


  /**
   * @param signInForm
   * @return
   * @see #getRightLogin(com.google.gwt.user.client.ui.Panel)
   */
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

    setFocusOnUserID();

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

    Button help = new Button(HELP);
    help.addStyleName("leftTenMargin");
    help.getElement().getStyle().setMarginTop(-5, Style.Unit.PX);
    help.setType(ButtonType.PRIMARY);
    help.setIcon(IconType.QUESTION_SIGN);

    help.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showWelcome2();
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
    signIn.setWidth("45px");
    signIn.getElement().setId("SignIn");
    eventRegistration.register(signIn);
    signIn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String userID = user.box.getValue();
        if (userID.length() < MIN_LENGTH_USER_ID) {
          markErrorBlur(user, PLEASE_ENTER_A_LONGER_USER_ID);
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
    enterKeyButtonHelper.addKeyHandler(signIn);

    signIn.addStyleName("rightFiveMargin");
    signIn.addStyleName("leftFiveMargin");

    signIn.setType(ButtonType.PRIMARY);
    return signIn;
  }

  /**
   * If there's an existing user without a password, copy their info to the sign up box.
   *
   * @param fieldset
   * @see #populateSignInForm(Form)
   */
  private void makeSignInUserName(Fieldset fieldset) {
    user = addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, USERNAME);
    user.box.addStyleName("topMargin");
    user.box.addStyleName("rightFiveMargin");
    user.box.getElement().setId(USERNAME_BOX_SIGN_IN);
    user.box.setWidth(SIGN_UP_WIDTH);

    user.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = true;
        eventRegistration.logEvent(user.box, USER_NAME_BOX, "N/A", "focus in username field");
      }
    });

    user.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if (!user.getSafeText().isEmpty()) {
          eventRegistration.logEvent(user.box, USER_NAME_BOX, "N/A", "left username field '" + user.getSafeText() + "'");

          //    logger.info("checking makeSignInUserName " + user.getSafeText());
          service.userExists(user.getSafeText(), "", new AsyncCallback<User>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(User result) {
              //       System.out.println("makeSignInUserName : for " + user.getSafeText() + " got back " + result);
              if (result != null) {
                String emailHash = result.getEmailHash();
                String passwordHash = result.getPasswordHash();
                if (emailHash == null || passwordHash == null || emailHash.isEmpty() || passwordHash.isEmpty()) {
                  eventRegistration.logEvent(user.box, USER_NAME_BOX, "N/A", "existing legacy user " + result.toStringShort());
                  copyInfoToSignUp(result);
                }
              }
            }
          });
        }
      }
    });
  }

  private DecoratedPopupPanel resetEmailPopup;
  private Button sendEmail;

  private Anchor getForgotPassword() {
    final Anchor forgotPassword = new Anchor(FORGOT_PASSWORD);
    forgotPassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (user.getSafeText().isEmpty()) {
          markErrorBlur(user, ENTER_A_USER_NAME);
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
            service.resetPassword(user.box.getText(), text, Window.Location.getHref(), new AsyncCallback<Boolean>() {
              @Override
              public void onFailure(Throwable caught) {
                sendEmail.setEnabled(true);
              }

              @Override
              public void onSuccess(Boolean result) {
                String heading = result ? CHECK_EMAIL : "Unknown email";
                String message = result ? PLEASE_CHECK_YOUR_EMAIL : user.box.getText() + " doesn't have that email. Check for a typo?";
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
   * @return
   * @see #populateSignInForm(com.github.gwtbootstrap.client.ui.Form)
   */
  private Anchor getForgotUser() {
    final Anchor forgotUsername = new Anchor(FORGOT_USERNAME);
    forgotUsername.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        final TextBox emailEntry = new TextBox();
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
              markErrorBlur(emailEntry, PLEASE_CHECK, VALID_EMAIL, Placement.TOP);
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
                  markErrorBlur(sendUsernameEmail, "Check your spelling", "No user has this email.", Placement.LEFT);
                  sendUsernameEmail.setEnabled(true);
                  eventRegistration.logEvent(sendUsernameEmail, "send username link", "N/A", "invalid email request ");
                } else {
                  eventRegistration.logEvent(sendUsernameEmail, "send username link", "N/A", "valid email request ");

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
    return text.trim().toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
  }

  /**
   * @param commentPopup
   * @param commentEntryText
   * @param okButton
   * @param prompt
   * @see #getForgotPassword()
   * @see #getForgotUser()
   */
  private void makePopup(Panel commentPopup, Widget commentEntryText, Widget okButton, String prompt) {
    Panel vp = new VerticalPanel();
    Panel w = new Heading(6, prompt);
    vp.add(w);
    w.addStyleName("bottomFiveMargin");
    Panel hp = new HorizontalPanel();
    hp.add(commentEntryText);
    hp.add(okButton);
    vp.add(hp);
    commentPopup.add(vp);
  }

  private void setFocusOnUserID() {
    setFocusOn(user.box);
  }

  private void setFocusOn(final FocusWidget widget) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        widget.setFocus(true);
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

    fieldset.add(getRolesChoices());

    getContentDevCheckbox();

    if (!props.isAMAS()) fieldset.add(contentDevCheckbox);

    makeRegistrationInfo(fieldset);
    fieldset.add(getSignUpButton(userBox, emailBox));

    return form;
  }

  private void getContentDevCheckbox() {
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
  }

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

    // TODO : this competes with the warning for existing users - don't add a tooltip

    return emailBox;
  }

  private void makeSignUpPassword(Fieldset fieldset, final UIObject emailBox) {
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

    final TextBoxBase ageBox = registrationInfo.getAgeEntryGroup().box;
    ageBox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
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

    //if (!CHECK_AGE)
    registrationInfo.hideAge();
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
        if (userBox.getValue().length() < MIN_LENGTH_USER_ID) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short user id '" + userBox.getValue() + "'");
          markErrorBlur(signUpUser, PLEASE_ENTER_A_LONGER_USER_ID);
        } else if (signUpEmail.box.getValue().isEmpty()) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short email");
          markErrorBlur(signUpEmail, "Please enter your email.");
          //  } else if (signUpEmail.box.getValue().length() < MIN_EMAIL) {
          //     eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short email");
          //     markErrorBlur(signUpEmail, "Please enter your email.");
        } else if (!isValidEmail(signUpEmail.box.getValue())) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "invalid email");
          markErrorBlur(signUpEmail, VALID_EMAIL);
        } else if (signUpPassword.box.getValue().length() < MIN_PASSWORD) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "short password");
          markErrorBlur(signUpPassword, signUpPassword.box.getValue().isEmpty() ? PLEASE_ENTER_A_PASSWORD :
              "Please enter a password at least " + MIN_PASSWORD + " characters long.");
        } else if (selectedRole == User.Kind.UNSET) {
          studentOrTeacherPopover = markErrorBlur(studentChoice, "Please choose", "Please select either student or teacher.", Placement.LEFT);
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't check role");
        } else if (selectedRole == User.Kind.CONTENT_DEVELOPER && !registrationInfo.checkValidGender()) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't check gender");
//        } else if (CHECK_AGE && selectedRole == User.Kind.CONTENT_DEVELOPER && !isValidAge(registrationInfo.getAgeEntryGroup())) {
          //         eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't fill in age ");
          //         markErrorBlur(registrationInfo.getAgeEntryGroup().box, AGE_ERR_MSG,Placement.TOP);
          //   registrationInfo.getAgeEntryGroup().markError(AGE_ERR_MSG);
        } else if (selectedRole == User.Kind.CONTENT_DEVELOPER && registrationInfo.getDialectGroup().getSafeText().isEmpty()) {
          eventRegistration.logEvent(signUp, "SignUp_Button", "N/A", "didn't fill in dialect ");
          markErrorBlur(registrationInfo.getDialectGroup(), "Enter a language dialect.");
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

  /**
   * When the form is valid, make a new user or update an existing one.
   *
   * @param user
   * @param password
   * @param email
   * @param kind
   * @see #getSignUpButton(com.github.gwtbootstrap.client.ui.base.TextBoxBase, com.github.gwtbootstrap.client.ui.base.TextBoxBase)
   */
  private void gotSignUp(final String user, String password, String email, User.Kind kind) {
    String passH = Md5Hash.getHash(password);
    String emailH = Md5Hash.getHash(email);

    boolean isCD = kind == User.Kind.CONTENT_DEVELOPER;
    String gender = isCD ? registrationInfo.isMale() ? MALE : "female" : MALE;
    String age = isCD ? registrationInfo.getAgeEntryGroup().getSafeText() : "";
    int age1 = isCD ? (age.isEmpty() ? 99 : Integer.parseInt(age)) : 0;
    String dialect = isCD ? registrationInfo.getDialectGroup().getSafeText() : "unk";

    signUp.setEnabled(false);

    service.addUser(user, passH, emailH, kind, Window.Location.getHref(), email,
        gender.equalsIgnoreCase(MALE), age1, dialect, isCD, "browser", new AsyncCallback<User>() {
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

  /**
   * TODO : somehow on chrome the images get smooshed.
   *
   * @param leftAndRight
   */
  private void getLeftIntro(Panel leftAndRight) {
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeft");
    left.setWidth(LEFT_SIDE_WIDTH + "px");
    leftAndRight.add(left);
    int size = 1;
    //   int subSize = size + 2;
    Heading w2 = new Heading(size, props.getInitialPrompt());
    left.add(w2);
    w2.getElement().getStyle().setPaddingBottom(24, Style.Unit.PX);
    w2.getElement().getStyle().setTextAlign(Style.TextAlign.LEFT);

    addBullett(left, props.getFirstBullet(), "NewProF2_48x48.png");
    if (!props.isAMAS()) {
      addBullett(left, SECOND_BULLET, "NewProF1_48x48.png");
      addBullett(left, THIRD_BULLET, "listIcon_48x48_transparent.png");
//    w3.getElement().getStyle().setMarginTop(-1, Style.Unit.PX);
//    configure(w3);
    }
  }

  private void addBullett(DivWidget left, String bulletText, String image) {
    Widget w1 = new HTML(bulletText);
    Panel h = new HorizontalPanel();
    h.add(new Image(LangTest.LANGTEST_IMAGES + image));
    h.add(w1);
    configure(h);

    left.add(h);
    w1.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
    configure(w1);
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
    final String hashedPass = Md5Hash.getHash(pass);
    logger.info("gotLogin : user is '" + user + "' pass " + pass.length() + " characters or '" + hashedPass + "'");

    signIn.setEnabled(false);
    service.userExists(user, hashedPass, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        signIn.setEnabled(true);
        markErrorBlur(signIn, "Trouble connecting to server.");
      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "unknown user " + user);

          logger.info("No user with that name '" + user + "' pass " + pass.length() + " characters - " + emptyPassword);
          markErrorBlur(password, emptyPassword ? PLEASE_ENTER_YOUR_PASSWORD : "No user found - have you signed up?");
          signIn.setEnabled(true);
        } else {
          foundExistingUser(result, emptyPassword, hashedPass);
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
     // logger.info("Got valid user " + result);
      if (emptyPassword) {
        eventRegistration.logEvent(signIn, "sign in", "N/A", "empty password");

        markErrorBlur(password, PLEASE_ENTER_YOUR_PASSWORD);
        signIn.setEnabled(true);
      } else if (result.getPasswordHash().equalsIgnoreCase(hashedPass)) {
        if (result.isEnabled() || result.getUserKind() != User.Kind.CONTENT_DEVELOPER || props.enableAllUsers()) {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "successful sign in for " + user);
      //    logger.info("Got valid user " + user + " and matching password, so we're letting them in.");

          storeUser(result);
        } else {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "successful sign in for " + user + " but wait for approval.");

          markErrorBlur(signIn, PLEASE_WAIT, "Please wait until you've been approved. Check your email.", Placement.LEFT);
          signIn.setEnabled(true);
        }
      } else { // special pathway...
        String enteredPass = Md5Hash.getHash(password.getSafeText());
        if (enteredPass.equals(MAGIC_PASS)) {
          eventRegistration.logEvent(signIn, "sign in", "N/A", "sign in as user '" + user + "'");
          storeUser(result);
        } else {
          logger.info("bad pass  " + passwordHash);
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
   * @see #foundExistingUser(mitll.langtest.shared.User, boolean, String)
   * @see #makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   */
  private void copyInfoToSignUp(User result) {
    signUpUser.box.setText(result.getUserID());
    signUpPassword.box.setText(password.getSafeText());
    setFocusOn(signUpEmail.getWidget());
    eventRegistration.logEvent(signIn, "sign in", "N/A", "copied info to sign up form");

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
   * @param result
   * @see #foundExistingUser(mitll.langtest.shared.User, boolean, String)
   * @see #gotSignUp(String, String, String, mitll.langtest.shared.User.Kind)
   */
  private void storeUser(User result) {
    //logger.info("UserPassLogin.storeUser - " + result);
    enterKeyButtonHelper.removeKeyHandler();
    userManager.storeUser(result, getAudioTypeFromPurpose(result.getUserKind()));
  }

  private String getAudioTypeFromPurpose(User.Kind kind) {
    if (kind == User.Kind.STUDENT || kind == User.Kind.TEACHER) return Result.AUDIO_TYPE_PRACTICE;
    else if (kind == User.Kind.CONTENT_DEVELOPER) return Result.AUDIO_TYPE_RECORDER;
    else return Result.AUDIO_TYPE_REVIEW;
  }
}
