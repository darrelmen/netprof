package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.HidePopupTextBox;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.util.Date;

/**
 * Created by go22670 on 8/11/14.
 */
public class UserPassLogin extends UserDialog {
  public static final int MIN_LENGTH_USER_ID = 4;

  public static final int MIN_PASSWORD = 6;
  public static final int MIN_EMAIL = 13;
  public static final int LEFT_SIDE_WIDTH = 483;
  public static final String SIGN_UP_SUBTEXT = "Or add missing info";//password and email";
  public static final String PLEASE_ENTER_YOUR_PASSWORD = "Please enter your password.";
  public static final String BAD_PASSWORD = "Wrong password - have you signed up?";
  public static final String PASSWORD = "Password";
  public static final String USERNAME = "Username";
  public static final String SIGN_IN = "Sign In";
  public static final String PLEASE_ENTER_A_LONGER_USER_ID = "Please enter a longer user id.";
  public static final String VALID_EMAIL = "Please enter a valid email address.";
  private final UserManager userManager;
  private final KeyPressHelper enterKeyButtonHelper;
  private FormField user;
  private FormField signUpUser;
  FormField signUpEmail;
  FormField signUpPassword;
  private FormField password;
  private boolean signInHasFocus = true;
  EventRegistration eventRegistration;
  Button signIn;

  /**
   * @see mitll.langtest.client.LangTest#showLogin
   * @param service
   * @param props
   * @param userManager
   * @param eventRegistration
   */
  public UserPassLogin(LangTestDatabaseAsync service, PropertyHandler props, UserManager userManager, EventRegistration eventRegistration) {
    super(service, props);
    this.userManager = userManager;
    this.eventRegistration = eventRegistration;
    enterKeyButtonHelper = new KeyPressHelper(true) {
      @Override
      public void userHitEnterKey(Button button) {
        if (resetEmailPopup.isVisible()) {
          sendEmail.fireEvent(new ButtonClickEvent());
        }
        else if (signInHasFocus) {//user.box.getValue().isEmpty() || password.box.getValue().isEmpty()) {
          //System.out.println("sending click to " + button.getElement().getId());
          button.fireEvent(new ButtonClickEvent());
        } else {
        //  System.out.println("sending click to " + signUp.getElement().getId());
          signUp.fireEvent(new KeyPressHelper.ButtonClickEvent());
        }
      }
    };
  }

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

  public void getRightLogin(Panel leftAndRight) {
    DivWidget right = new DivWidget();

    leftAndRight.add(right);
    right.addStyleName("floatRight");
    DivWidget rightDiv = new DivWidget();

    Form form = new Form();
    rightDiv.add(form);

    rightDiv.add(getSignUpForm());

    form.addStyleName("topMargin");
    form.addStyleName("formRounded");
    form.getElement().getStyle().setBackgroundColor("white");

    Fieldset fieldset = new Fieldset();
    form.add(fieldset);
    right.add(rightDiv);

    // final ListBoxFormField purpose = getListBoxFormField(fieldset, ARE_YOU_A, getListBox2(ROLES));
    //  purpose.box.setWidth("150px");
    // purpose.box.addStyleName("floatRight");

    user = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,USER_ID_MAX_LENGTH, USERNAME);
    user.box.addStyleName("topMargin");
    user.box.addStyleName("rightFiveMargin");
    user.box.getElement().setId("Username_Box_SignIn");
    user.box.setFocus(true);
    user.box.setWidth("266px");

    user.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = true;
      }
    });

    password = addControlFormField(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    password.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = true;
      }
    });

    Panel hp = new HorizontalPanel();
    hp.add(password.box);
    hp.addStyleName("leftFiveMargin");

    signIn = new Button(SIGN_IN);
    signIn.getElement().setId("SignIn");
    eventRegistration.register(signIn);
    hp.add(signIn);
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
    fieldset.add(hp);

    final Anchor forgotPassword = getForgotPassword();
    fieldset.add(forgotPassword);
    forgotPassword.getElement().getStyle().setProperty("fontSize", "smaller");

    forgotPassword.addStyleName("leftTenMargin");

    getFocusOnField(user);
  }


  DecoratedPopupPanel resetEmailPopup;
  Button sendEmail;

  public Anchor getForgotPassword() {
    final Anchor forgotPassword = new Anchor("Forgot password?");
    forgotPassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (user.getText().isEmpty()) {
          markError(user,"Enter a user name.");
          return;
        }
        final HidePopupTextBox emailEntry = new HidePopupTextBox();
       resetEmailPopup = new DecoratedPopupPanel();
        /*resetEmailPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
          @Override
          public void onClose(CloseEvent<PopupPanel> event) {

            System.out.println("got close event -------");
          }
        });*/
        sendEmail = new Button("Send");
        sendEmail.setType(ButtonType.PRIMARY);
        sendEmail.addStyleName("leftTenMargin");
        sendEmail.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            String text = emailEntry.getText();
            if (!isValidEmail(text)) {
       /*       System.out.println("email is '" + text+
                  "' ");*/
              markError(emailEntry,
                  "Please check",
                  VALID_EMAIL, Placement.TOP);
              return;
            }

            sendEmail.setEnabled(false);
            service.resetPassword(user.box.getText(), text, new AsyncCallback<Void>() {
              @Override
              public void onFailure(Throwable caught) {
                sendEmail.setEnabled(true);
              }

              @Override
              public void onSuccess(Void result) {
                 Popover widgets = setupPopover(sendEmail, "Check Email", "Please check your email", Placement.LEFT, 5000, new MyPopover() {
                  boolean isFirst = true;
                  @Override
                  public void hide() {
                    super.hide();
                    if (isFirst) {
                      isFirst = false;
                    }
                    else {
                      resetEmailPopup.hide(); // TODO : ugly
                    }
                    System.out.println("got hide !" + new Date()
                    );
                  }
                });
              }
            });
          }
        });
        eventRegistration.register(sendEmail, "N/A", "reset password");

        makePopup(resetEmailPopup, emailEntry, sendEmail);
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

  public boolean isValidEmail(String text) {
    return text.toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
  }

  private void makePopup(DecoratedPopupPanel commentPopup,HidePopupTextBox commentEntryText, Button okButton) {
    //final DecoratedPopupPanel commentPopup = new DecoratedPopupPanel();
    commentPopup.setAutoHideEnabled(false);
    //   commentPopup.configure(commentEntryText, commentButton, clearButton);
    //   commentPopup.setField(field);
    VerticalPanel vp = new VerticalPanel();
    Panel w = new Heading(6,"Enter your email to reset your password.");
    vp.add(w);
    w.addStyleName("bottomFiveMargin");
    Panel hp = new HorizontalPanel();
    hp.add(commentEntryText);
    //Button okButton = getOKButton(clickHandler);
    hp.add(okButton);
    vp.add(hp);
    commentPopup.add(vp);
    //return okButton;
    //return commentPopup;
  }

  /**
   * Clicking OK just dismisses the resetEmailPopup.
   * @paramx commentPopup
   * @return
   */
/*
  protected Button getOKButton(//final PopupPanel commentPopup,
                               ClickHandler clickHandler) {
    Button ok = new Button("OK");
    ok.setType(ButtonType.PRIMARY);
    ok.addStyleName("leftTenMargin");
*/
/*    ok.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentPopup.hide();
      }
    });*//*

    if (clickHandler != null) {
      ok.addClickHandler(clickHandler);
    }
    return ok;
  }
*/

  protected void getFocusOnField(final FormField user) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        user.box.setFocus(true);
      }
    });
  }

  private Button signUp;
  public Form getSignUpForm() {
    Form form = new Form();
    form.addStyleName("topMargin");
    form.addStyleName("formRounded");
    form.getElement().getStyle().setBackgroundColor("white");

    Fieldset fieldset = new Fieldset();
    Heading w = new Heading(3, "Sign Up", SIGN_UP_SUBTEXT);
    w.addStyleName("signUp");
    form.add(w);
    form.add(fieldset);

    // final ListBoxFormField purpose = getListBoxFormField(fieldset, ARE_YOU_A, getListBox2(ROLES));
    //  purpose.userBox.setWidth("150px");
    // purpose.userBox.addStyleName("floatRight");

    signUpUser = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,20, USERNAME);
    final TextBoxBase userBox = signUpUser.box;
    userBox.addStyleName("topMargin");
    userBox.addStyleName("rightFiveMargin");
    userBox.setWidth("266px");
    userBox.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    userBox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
      }
    });

    signUpEmail = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,USER_ID_MAX_LENGTH,"Email");
    final TextBoxBase emailBox = signUpEmail.box;
    emailBox.addStyleName("topMargin");
    emailBox.addStyleName("rightFiveMargin");
    emailBox.setWidth("266px");
    emailBox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
      }
    });

    signUpPassword = addControlFormField(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    signUpPassword.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = false;
      }
    });

    signUp = new Button("Sign Up");
    signUp.getElement().setId("SignIn");


    fieldset.add(signUp);

    signUp.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
      //  System.out.println("signUp got click!");
        if (userBox.getValue().length() < MIN_LENGTH_USER_ID) {
          markError(signUpUser, PLEASE_ENTER_A_LONGER_USER_ID);
        } else if (signUpEmail.box.getValue().length() < MIN_EMAIL) {
          markError(signUpEmail, "Please enter your email.");
        } else if (!isValidEmail(signUpEmail.box.getValue())) {
          markError(signUpEmail, VALID_EMAIL);

        } else if (signUpPassword.box.getValue().length() < MIN_PASSWORD) {
          markError(signUpPassword, signUpPassword.box.getValue().isEmpty() ? "Please enter a password." :
              "Please enter a password at least " + MIN_PASSWORD + " characters long.");
        } else {
          gotSignUp(userBox.getValue(), signUpPassword.box.getValue(), emailBox.getValue());
        }
      }
    });
    signUp.addStyleName("floatRight");
    signUp.addStyleName("rightFiveMargin");
    signUp.addStyleName("leftFiveMargin");

    signUp.setType(ButtonType.SUCCESS);
    return form;
  }

  private void gotSignUp(String user, String password, String email) {
    String passH = Md5Hash.getHash(password);
    String emailH = Md5Hash.getHash(email);

    signUp.setEnabled(false);
    service.addUser(user, passH, emailH, User.Kind.STUDENT, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        signUp.setEnabled(true);
        markError(signUp, "Trouble connecting to server.");
      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          signUp.setEnabled(true);
          markError(signUpUser, "User exists already, please sign in or choose a different name.");
        }
        else {
          storeUser(result);
        }
      }
    });
  }

  public void getLeftIntro(Panel leftAndRight) {
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeft");
    left.setWidth(LEFT_SIDE_WIDTH + "px");
    leftAndRight.add(left);
    int size = 2;
    int subSize = size+1;
    left.add(new Heading(size,"Learn how to pronounce words and practice vocabulary."));
    Heading w1 = new Heading(subSize, "Do flashcards to learn or review vocabulary", "Speak your answers. Compete with your friends.");
    left.add(w1);

    Node child2 = w1.getElement().getChild(1);

    com.google.gwt.dom.client.Element as = com.google.gwt.dom.client.Element.as(child2);
    as.getStyle().setFontSize(16, Style.Unit.PX);
    Heading w = new Heading(subSize, "Get feedback on your pronunciation", "Compare yourself to a native speaker.");
    left.add(w);
    child2 = w.getElement().getChild(1);
    as = com.google.gwt.dom.client.Element.as(child2);
    as.getStyle().setPaddingLeft(2, Style.Unit.PX);
    as.getStyle().setFontSize(16, Style.Unit.PX);
    left.add(new Heading(subSize, "Make your own lists of words to study later or to share."));
  }



  /**
   * @see #getRightLogin(com.google.gwt.user.client.ui.Panel)
   * @param user
   * @param pass
   */
  private void gotLogin(String user, final String pass, final boolean emptyPassword) {
    final String hashed = Md5Hash.getHash(pass);

    System.out.println("gotLogin : user is '" +user + "' pass '" + pass +"' or " + hashed);

    signIn.setEnabled(false);
    service.userExists(user, hashed, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        signIn.setEnabled(true);
        markError(signIn,"Trouble connecting to server.");
      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          System.out.println("No user with that name? pass empty " + emptyPassword);
          markError(password, emptyPassword ? PLEASE_ENTER_YOUR_PASSWORD : BAD_PASSWORD);
          signIn.setEnabled(true);
        } else {
          System.out.println("Found user "+result);

          String emailHash = result.getEmailHash();
          String passwordHash = result.getPasswordHash();
          if (emailHash == null || passwordHash == null || emailHash.isEmpty() || passwordHash.isEmpty()) {
            signUpUser.box.setText(result.getUserID());
            signUpPassword.box.setText(password.getText());
            getFocusOnField(signUpEmail);
            markError(signUpEmail,"Add info","Current users should add an email and password.");
            signIn.setEnabled(true);

          }
          else {
            System.out.println("Got valid user " + result);
            if (emptyPassword) {
              markError(password, PLEASE_ENTER_YOUR_PASSWORD);
              signIn.setEnabled(true);
            }
            else if (result.getPasswordHash().equals(hashed)) {
              storeUser(result);
            }
            else {
              markError(password, BAD_PASSWORD);
              signIn.setEnabled(true);
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


  private FormField addControlFormField(Panel dialogBox, boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    user.setMaxLength(maxLength);
    user.setPlaceholder(hint);
    return getFormField(dialogBox, user, minLength);
  }

  private FormField getFormField(Panel dialogBox, TextBox user, int minLength) {
    final ControlGroup userGroup = addControlGroupEntryNoLabel(dialogBox, user);
    return new FormField(user, userGroup, minLength);
  }
}
