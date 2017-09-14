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
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.initial.BrowserCheck;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.StartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/11/14.
 */
public class UserPassLogin extends UserDialog implements UserPassDialog {
//  private final Logger logger = Logger.getLogger("UserPassLogin");
  public static final String USER_NAME_BOX = "UserNameBox";

  private static final String IPAD_LINE_1 = "Also consider installing the NetProF app, which is available on the DLI App Store.";// or";
  // private static final String IPAD_LINE_2 = "Or click this link to install <a href='https://np.ll.mit.edu/iOSNetProF/'>iOS NetProF" + "</a>.";
  private static final String IPAD_LINE_3 = "Otherwise, you will not be able to record yourself practicing vocabulary.";

  private static final int LEFT_SIDE_WIDTH = 453;
  private static final String SECOND_BULLET = "Record your voice and get feedback on your pronunciation.";//"Get feedback on your pronunciation";
  private static final String THIRD_BULLET = "Create and share vocab lists for study and review.";//"Make your own lists of words to study later or to share.";
  private static final String CHECK_EMAIL = "Check Email";
  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email";
  private static final String FORGOT_USERNAME = "Forgot username?";
  private static final String SEND = "Send";
  private static final int BULLET_MARGIN = 25;
  private static final String PLEASE_CHECK = "Please check";
  private static final String ENTER_YOUR_EMAIL = "Enter your email to get your username.";
  private static final int EMAIL_POPUP_DELAY = 4000;
  private static final String HELP = "Help";
  private static final int FLAG_DIM = 32;
  private static final int COLUMNS = 4;

  private final KeyPressHelper enterKeyButtonHelper;

  private boolean signInHasFocus = true;
  private final EventRegistration eventRegistration;

  private final SignUp signUpForm;
  private final SignIn signInForm;

  private List<Pair> ccs = new ArrayList<>();

  /**
   * @param props
   * @param userManager
   * @param eventRegistration
   * @see InitialUI#showLogin
   */
  public UserPassLogin(PropertyHandler props,
                       UserManager userManager,
                       EventRegistration eventRegistration,
                       StartupInfo startupInfo) {
    super(props);

    signUpForm = new SignUpForm(props, userManager, eventRegistration, this, startupInfo);
    signInForm = new SignInForm(props, userManager, eventRegistration, this, signUpForm);

    if (BrowserCheck.isIPad()) {
      showSuggestApp();
    }

    getFlags(startupInfo);

    this.eventRegistration = eventRegistration;
    enterKeyButtonHelper = getKeyPressHelper();
    setEnterKeyButtonHelper(enterKeyButtonHelper);
    signInForm.setEnterKeyButtonHelper(enterKeyButtonHelper);
    signUpForm.setEnterKeyButtonHelper(enterKeyButtonHelper);
  }

  private void getFlags(StartupInfo startupInfo) {
    Set<String> seen = new HashSet<>();

    startupInfo.getProjects().forEach(slimProject -> {
      String language = slimProject.getLanguage();
      if (slimProject.getStatus() == ProjectStatus.PRODUCTION) {
        if (!seen.contains(language)) {

          ccs.add(new Pair(slimProject.getCountryCode(), language.substring(0, 1).toUpperCase() + language.substring(1)));
          seen.add(language);
        }
      }
    });
    Collections.sort(ccs);
  }


  /**
   * Sorts by language
   */
  private static class Pair implements Comparable<Pair> {
    String cc, language;

    public Pair(String cc, String language) {
      this.cc = cc;
      this.language = language;
    }

    @Override
    public int compareTo(@NotNull Pair o) {
      return language.toLowerCase().compareTo(o.language.toLowerCase());
    }
  }

  private KeyPressHelper getKeyPressHelper() {
    return new KeyPressHelper(true) {
      @Override
      public void userHitEnterKey(Button button) {
        if (sendUsernamePopup != null && sendUsernamePopup.isShowing()) {
          sendUsernameEmail.fireEvent(new ButtonClickEvent());
        } else if (signInForm.clickSendEmail()) {
          //    sendEmail.fireEvent(new ButtonClickEvent());
        } else if (signInHasFocus) {
          button.fireEvent(new ButtonClickEvent());
        } else {
          // signUp.fireEvent(new KeyPressHelper.ButtonClickEvent());
          signUpForm.clickSignUp();
        }
      }
    };
  }

  @Override
  public void clearSignInHasFocus() {
    signInHasFocus = false;
  }

  @Override
  public void setSignInHasFocus() {
    signInHasFocus = true;
  }

  public void setSignInPasswordFocus() {
    signInForm.setFocusPassword();
  }

  public void tryLogin() {
    signInForm.tryLogin();
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
        null, hiddenEvent -> signInForm.setFocusOnUserID(),
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
    //  leftAndRight.add(getLinksToSites());
    return container;
  }

  private void getRightLogin(Panel leftAndRight) {
    DivWidget right = new DivWidget();

    leftAndRight.add(right);
    right.addStyleName("floatRight");

    DivWidget rightDiv = new DivWidget();
    right.add(rightDiv);

    rightDiv.add(signInForm.populateSignInForm(getUserForm(), getForgotRow(), enterKeyButtonHelper));
    rightDiv.add(signUpForm.getSignUpForm());
  }

  /**
   * @return
   * @see SignInForm#populateSignInForm
   */
  private Panel getForgotRow() {
    Anchor forgotUser = getForgotUser();
    styleLink(forgotUser);

    Anchor forgotPassword = signInForm.getForgotPassword();
    styleLink(forgotPassword);

    Panel hp2 = new HorizontalPanel();
    hp2.getElement().setId("hp_forgotuser_pass_help_row");
    hp2.add(forgotUser);
    hp2.add(forgotPassword);
    hp2.add(getHelpButton());

    return hp2;
  }

  private void styleLink(Anchor forgotUser) {
    forgotUser.addStyleName("topFiveMargin");
    forgotUser.addStyleName("leftTenMargin");
  }

  private Button getHelpButton() {
    Button help = new Button(HELP);
    help.addStyleName("leftTenMargin");
    help.getElement().getStyle().setMarginTop(-5, Style.Unit.PX);
    help.setType(ButtonType.PRIMARY);
    help.setIcon(IconType.QUESTION_SIGN);
    help.addClickHandler(event -> showWelcome2());
    return help;
  }

  private DecoratedPopupPanel sendUsernamePopup;
  private Button sendUsernameEmail;

  /**
   * @return
   * @see SignInForm#populateSignInForm
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
            service.forgotUsername(Md5Hash.getHash(text), text, new AsyncCallback<Boolean>() {
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
        setFocusOn(emailEntry);
      }
    });
    return forgotUsername;
  }

  /**
   * TODO : somehow on chrome the images get smooshed.
   *
   * @param leftAndRight
   */
  private void getLeftIntro(Panel leftAndRight) {
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeftAndClear");
    // left.addStyleName("rightFiveMargin");
    left.setWidth(LEFT_SIDE_WIDTH + "px");
    leftAndRight.add(left);
    int size = 1;

    Heading w2 = new Heading(size, props.getInitialPrompt());
    left.add(w2);
    w2.getElement().getStyle().setPaddingBottom(24, Style.Unit.PX);
    w2.getElement().getStyle().setTextAlign(Style.TextAlign.LEFT);

    String firstBullet = props.getFirstBullet();
    if (props.isAMAS()) {
      addBullett(left, firstBullet, "NewProF2_48x48.png");
    } else {
      addBullett(left, SECOND_BULLET, "NewProF1_48x48.png");
      addBullett(left, firstBullet, "NewProF2_48x48.png");
      addBullett(left, THIRD_BULLET, "listIcon_48x48_transparent.png");
    }

    left.add(getFlagsDisplay());
  }

  @NotNull
  private Grid getFlagsDisplay() {
    int row = (int) Math.ceil((float) ccs.size() / (float) COLUMNS);
    Grid langs = new Grid(row, COLUMNS);
    int r = 0;

    for (Pair pair : ccs) {
      DivWidget both = new DivWidget();
      both.addStyleName("inlineFlex");

      {
        com.google.gwt.user.client.ui.Image flag = getFlag(pair.cc);
        flag.addStyleName("rightFiveMargin");
        flag.setHeight(FLAG_DIM + "px");
        flag.setWidth(FLAG_DIM + "px");
        both.add(flag);
      }

      {
        HTML w = new HTML(pair.language);
        w.addStyleName("rightTenMargin");
        w.addStyleName("topFiveMargin");
        both.add(w);
      }
      langs.setWidget(r / COLUMNS, r % COLUMNS, both);
      r++;
    }

    langs.getElement().getStyle().setMarginTop(30, Style.Unit.PX);
    return langs;
  }

  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    return new com.google.gwt.user.client.ui.Image("langtest/cc/" + cc + ".png");
  }

  private void addBullett(DivWidget left, String bulletText, String image) {
    Widget w1 = new HTML(bulletText);
    Panel h = new HorizontalPanel();
    h.add(new Image(LangTest.LANGTEST_IMAGES + image));
    h.add(w1);
    configure(h);

    left.add(h);
    w1.getElement().getStyle().setMarginTop(COLUMNS, Style.Unit.PX);
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
}
