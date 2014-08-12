package mitll.langtest.client.user;

import com.gargoylesoftware.htmlunit.javascript.host.Element;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.AbstractTypography;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;
import com.ibm.icu.impl.CalendarAstronomer;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by go22670 on 8/11/14.
 */
public class UserPassLogin extends UserDialog {
  public static final int MIN_LENGTH_USER_ID = 8;
  private static final String STUDENT = "Student";
  private static final String TEACHER = "Teacher";

  public static final int MIN_PASSWORD = 6;
  private final UserManager userManager;
  final KeyPressHelper enterKeyButtonHelper = new KeyPressHelper(true);
  private FormField password;

  public UserPassLogin(LangTestDatabaseAsync service, PropertyHandler props,UserManager userManager) {
    super(service,props);
    this.userManager = userManager;

    //getContent();

  }

  public Panel getContent() {
   // Panel leftAndRight = new HorizontalPanel();
    //Panel container = new FluidContainer();
    Panel container = new DivWidget();
    DivWidget child = new DivWidget();
    container.add(child);
    child.addStyleName("loginPageBack");
   // container.setWidth("838px");
   // container.addStyleName("loginPage");
    //container.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
   // container.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
  //  Panel leftAndRight = new FluidRow();
    Panel leftAndRight = new DivWidget();

    leftAndRight.addStyleName("loginPage");

    // leftAndRight.getElement().getStyle().setPaddingLeft(5, Style.Unit.PX);
   // leftAndRight.getElement().getStyle().setPaddingRight(5, Style.Unit.PX);
    container.add(leftAndRight);

   // leftAndRight.getElement().getStyle().setBackgroundColor("lightBlue");
     DivWidget left = new DivWidget();
    left.addStyleName("floatLeft");
    left.setWidth(494 + "px");
    //Column left = new Column(6);
    leftAndRight.add(left);
   // left.addStyleName("floatLeft");
    int size = 2;
    int subSize = size+1;
    left.add(new Heading(size,"Learn how to pronounce words and practice vocabulary."));
    Heading w1 = new Heading(subSize, "Do flashcards to learn or review vocabulary", "Speak your answers. Compete with your friends.");
    left.add(w1);

    Node child2 = w1.getElement().getChild(1);

    com.google.gwt.dom.client.Element as = com.google.gwt.dom.client.Element.as(child2);
    //as.getStyle().setPaddingLeft(2, Style.Unit.PX);
    as.getStyle().setFontSize(16, Style.Unit.PX);
    Heading w = new Heading(subSize, "Get feedback on your pronunciation", "Compare yourself to a native speaker.");
    left.add(w);
    child2 = w.getElement().getChild(1);
    as = com.google.gwt.dom.client.Element.as(child2);
    as.getStyle().setPaddingLeft(2, Style.Unit.PX);
    as.getStyle().setFontSize(16, Style.Unit.PX);
  //  AbstractTypography.asWidgetOrNull(child2);
   // Heading.Small.asWidgetOrNull(child2);
    left.add(new Heading(subSize, "Make your own lists of words to study later or to share."));


    DivWidget right = new DivWidget();

    leftAndRight.add(right);
    right.addStyleName("floatRight");
    //FormPanel w = new FormPanel();
    DivWidget rightDiv = new DivWidget();

    Form form = new Form();
    rightDiv.add(form);
    form.addStyleName("topMargin");
    form.addStyleName("formRounded");
    form.getElement().getStyle().setBackgroundColor("white");

    //form.addStyleName("form-horizontal");

    Fieldset fieldset = new Fieldset();
    form.add(fieldset);
    //right.add(new Heading(size,));
    right.add(rightDiv);

   // final ListBoxFormField purpose = getListBoxFormField(fieldset, ARE_YOU_A, getListBox2(ROLES));
  //  purpose.box.setWidth("150px");
   // purpose.box.addStyleName("floatRight");

    final FormField user = addControlFormField(fieldset, false, MIN_LENGTH_USER_ID,USER_ID_MAX_LENGTH,"Username or Email");
    user.box.addStyleName("topMargin");
    user.box.addStyleName("rightFiveMargin");
  //  user.box.addStyleName("floatRight");

    //user.setVisible(isDataCollection(purpose) || isPractice(purpose));
    //addTooltip(user.box);
    user.box.setFocus(true);
    user.box.setWidth("266px");

     password = addControlFormField(fieldset, true, MIN_PASSWORD, 15, "Password");
    Panel hp = new HorizontalPanel();
    hp.add(password.box);
    hp.addStyleName("leftFiveMargin");
    //password.box.setWidth("");


    Button signIn = new Button("Sign In");
    hp.add(signIn);
    signIn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (user.box.getValue().length() < MIN_LENGTH_USER_ID) {
          markError(user, "Please enter either a user id or email.");
        } else if (password.box.getValue().length() < MIN_PASSWORD) {
          markError(user, "Please enter user password.");
        } else {
          gotLogin(user.box.getValue(), password.box.getValue());
        }
      }
    });
    enterKeyButtonHelper.addKeyHandler(signIn);

    signIn.addStyleName("rightFiveMargin");
    signIn.addStyleName("leftFiveMargin");

    signIn.setType(ButtonType.PRIMARY);
    fieldset.add(hp);
    return container;
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

    System.out.println("hash is " +hashed);

    service.userExists(user, hashed, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(User result) {
        if (result == null) {
          System.out.println("No user with that name?");
          markError(password,"User-password combination not found.");
        }
        else {
          enterKeyButtonHelper.removeKeyHandler();
          userManager.storeUser(result, getAudioTypeFromPurpose(result.getUserKind()));
        }
      }
    });
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
