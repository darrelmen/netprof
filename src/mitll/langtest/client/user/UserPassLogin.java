package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by go22670 on 8/11/14.
 */
public class UserPassLogin extends UserDialog {
  public static final int MIN_LENGTH_USER_ID = 4;
  //private static final String STUDENT = "Student";
  //private static final String TEACHER = "Teacher";

  public static final int MIN_PASSWORD = 6;
  public static final int MIN_EMAIL = 13;
  public static final int LEFT_SIDE_WIDTH = 483;
  public static final String SIGN_UP_SUBTEXT = "Or add missing info";//password and email";
  public static final String PLEASE_ENTER_YOUR_PASSWORD = "Please enter your password.";
  public static final String BAD_PASSWORD = "Wrong password - have you signed up?";
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

        if (signInHasFocus) {//user.box.getValue().isEmpty() || password.box.getValue().isEmpty()) {
          System.out.println("sending click to " + button.getElement().getId());
          button.fireEvent(new ButtonClickEvent());
        } else {
          System.out.println("sending click to " + signUp.getElement().getId());
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

    user = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,USER_ID_MAX_LENGTH,"Username");
    user.box.addStyleName("topMargin");
    user.box.addStyleName("rightFiveMargin");
    user.box.getElement().setId("Username_Box_SignIn");
    user.box.setFocus(true);
    user.box.setWidth("266px");

    user.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        //System.out.println("sign in user box has focus...");
        signInHasFocus = true;
      }
    });

    password = addControlFormField(fieldset, true, MIN_PASSWORD, 15, "Password");
    password.box.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        signInHasFocus = true;
      }
    });

    Panel hp = new HorizontalPanel();
    hp.add(password.box);
    hp.addStyleName("leftFiveMargin");

    signIn = new Button("Sign In");
    signIn.getElement().setId("SignIn");
    eventRegistration.register(signIn);
    hp.add(signIn);
    signIn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //System.out.println("sign in got click!");

        String userID = user.box.getValue();
        if (userID.length() < MIN_LENGTH_USER_ID) {
          markError(user, "Please enter a longer user id.");
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

    getFocusOnField(user);
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

    signUpUser = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,20,"Username");
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

    signUpPassword = addControlFormField(fieldset, true, MIN_PASSWORD, 15, "Password");
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
        System.out.println("signUp got click!");
        if (userBox.getValue().length() < MIN_LENGTH_USER_ID) {
          markError(signUpUser, "Please enter either a valid user id.");
        } else if (signUpEmail.box.getValue().length() < MIN_EMAIL) {
          markError(signUpEmail, "Please enter your email.");
        } else if (signUpPassword.box.getValue().length() < MIN_PASSWORD) {
          markError(signUpPassword, signUpPassword.box.getValue().isEmpty() ? "Please enter a password." : "Please enter a password at least " + MIN_PASSWORD + " characters long.");
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
    String passH = toHexString(getMd5Digest(password.getBytes()));
    String emailH = toHexString(getMd5Digest(email.getBytes()));

    signUp.setEnabled(false);
    service.addUser(user,passH,emailH,User.Kind.STUDENT,new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        signUp.setEnabled(true);

      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          signUp.setEnabled(true);
          markError(signUpUser, "User exists already, please sign in.");
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
   * Generate MD5 digest.
   *
   * @param input input data to be hashed.
   * @return MD5 digest.
   */
  public static byte[] getMd5Digest(byte[] input) {
    MessageDigest md5;

    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      return new byte[1];
    }
    md5.reset();
    md5.update(input);
    return md5.digest();
  }

  public static char[] HEX_CHARS = new char[] {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  public static String toHexString(byte[] bytes) {
    char[] hexString = new char[2 * bytes.length];
    int j = 0;
    for (int i = 0; i < bytes.length; i++) {
      hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
      hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
    }
    return new String(hexString);
  }

  /**
   * @see #getRightLogin(com.google.gwt.user.client.ui.Panel)
   * @param user
   * @param pass
   */
  private void gotLogin(String user, final String pass, final boolean emptyPassword) {
    final String hashed = toHexString(getMd5Digest(pass.getBytes()));

    System.out.println("gotLogin : user is '" +user + "' pass '" + pass +"' or " + hashed);

    signIn.setEnabled(false);
    service.userExists(user, hashed, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {
        signIn.setEnabled(true);

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
