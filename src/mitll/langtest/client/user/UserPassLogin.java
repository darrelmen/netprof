package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by go22670 on 8/11/14.
 */
public class UserPassLogin extends UserDialog {
  public static final int MIN_LENGTH_USER_ID = 6;
  private static final String STUDENT = "Student";
  private static final String TEACHER = "Teacher";

  public static final int MIN_PASSWORD = 6;
  private final UserManager userManager;
  private final KeyPressHelper enterKeyButtonHelper;
  private FormField user;
  private FormField password;

  public UserPassLogin(LangTestDatabaseAsync service, PropertyHandler props,UserManager userManager) {
    super(service,props);
    this.userManager = userManager;

     enterKeyButtonHelper = new KeyPressHelper(true) {
      @Override
      public void userHitEnterKey(Button button) {
        if (user.box.getValue().isEmpty() || password.box.getValue().isEmpty()) {
          signUp.fireEvent(new KeyPressHelper.ButtonClickEvent());
        }
        else {
          button.fireEvent(new ButtonClickEvent());
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

    user = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,USER_ID_MAX_LENGTH,"Username or Email");
    user.box.addStyleName("topMargin");
    user.box.addStyleName("rightFiveMargin");

    user.box.setFocus(true);
    user.box.setWidth("266px");

    password = addControlFormField(fieldset, true, MIN_PASSWORD, 15, "Password");
    Panel hp = new HorizontalPanel();
    hp.add(password.box);
    hp.addStyleName("leftFiveMargin");

    Button signIn = new Button("Sign In");
    hp.add(signIn);
    signIn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (user.box.getValue().isEmpty() || password.box.getValue().isEmpty()) {
          signUp.fireEvent(new KeyPressHelper.ButtonClickEvent());
        }
        else {
          if (user.box.getValue().length() < MIN_LENGTH_USER_ID) {
            markError(user, "Please enter either a user id or email.");
          } else if (password.box.getValue().length() < MIN_PASSWORD) {
            markError(user, "Please enter user password.");
          } else {
            gotLogin(user.box.getValue(), password.box.getValue());
          }
        }
      }
    });
    enterKeyButtonHelper.addKeyHandler(signIn);

    signIn.addStyleName("rightFiveMargin");
    signIn.addStyleName("leftFiveMargin");

    signIn.setType(ButtonType.PRIMARY);
    fieldset.add(hp);

    getFocusOnUserID(user);
  }

  protected void getFocusOnUserID(final FormField user) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        user.box.setFocus(true);
      }
    });
  }

  Button signUp;
  public Form getSignUpForm() {
    Form form = new Form();
    form.addStyleName("topMargin");
    form.addStyleName("formRounded");
    form.getElement().getStyle().setBackgroundColor("white");

    Fieldset fieldset = new Fieldset();
    Heading w = new Heading(3, "Sign Up", "New to Classroom?");
    w.addStyleName("signUp");
    form.add(w);
    form.add(fieldset);

    // final ListBoxFormField purpose = getListBoxFormField(fieldset, ARE_YOU_A, getListBox2(ROLES));
    //  purpose.box.setWidth("150px");
    // purpose.box.addStyleName("floatRight");

    final FormField user = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,20,"Username");
    user.box.addStyleName("topMargin");
    user.box.addStyleName("rightFiveMargin");
    user.box.setWidth("266px");
    user.box.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);

    final FormField email = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,USER_ID_MAX_LENGTH,"Email");
    email.box.addStyleName("topMargin");
    email.box.addStyleName("rightFiveMargin");
    email.box.setWidth("266px");

    final FormField password = addControlFormField(fieldset, true, MIN_PASSWORD, 15, "Password");
    //Panel hp = new HorizontalPanel();
   // hp.add(password.box);
   // hp.addStyleName("leftFiveMargin");

    signUp = new Button("Sign Up");
    fieldset.add(signUp);

    signUp.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (user.box.getValue().length() < MIN_LENGTH_USER_ID) {
          markError(user, "Please enter either a user id or email.");
        } else if (password.box.getValue().length() < MIN_PASSWORD) {
          markError(user, "Please enter user password.");
        } else {
          gotSignUp(user.box.getValue(), password.box.getValue(), email.box.getValue());
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

    service.addUser(user,passH,emailH,User.Kind.STUDENT,new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
        storeUser(result);
      }
    });
  }

  public void getLeftIntro(Panel leftAndRight) {
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeft");
    left.setWidth(494 + "px");
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

  private void gotLogin(String user, final String pass) {

    String hashed = toHexString(getMd5Digest(pass.getBytes()));

   // System.out.println("hash is " +hashed);

    service.userExists(user, hashed, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          System.out.println("No user with that name?");
          markError(password, "User-password combination not found.");
        }
        else {
          storeUser(result);
        }
      }
    });
  }

  public void storeUser(User result) {
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
