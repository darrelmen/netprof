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
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.user.User;

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
  private final Logger logger = Logger.getLogger("UserPassLogin");

  private static final String IPAD_LINE_1 = "Also consider installing the NetProF app, which is available on the DLI App Store.";// or";
  // private static final String IPAD_LINE_2 = "Or click this link to install <a href='https://np.ll.mit.edu/iOSNetProF/'>iOS NetProF" + "</a>.";
  private static final String IPAD_LINE_3 = "Otherwise, you will not be able to record yourself practicing vocabulary.";

  private static final int LEFT_SIDE_WIDTH = 483;
  //private static final String VALID_EMAIL = "Please enter a valid email address.";
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

  private final KeyPressHelper enterKeyButtonHelper;

  private boolean signInHasFocus = true;
  private final EventRegistration eventRegistration;

  private SignUpForm signUpForm;
  private SignInForm signInForm;

  /**
   * @param props
   * @param userManager
   * @param eventRegistration
   * @see mitll.langtest.client.InitialUI#showLogin
   */
  public UserPassLogin(PropertyHandler props,
                       UserManager userManager,
                       EventRegistration eventRegistration) {
    super(props, userManager);
    boolean willShow = false;// checkWelcome();

    signUpForm = new SignUpForm(props, userManager, eventRegistration, this);
    signInForm = new SignInForm(props, userManager, eventRegistration, this, signUpForm);

    if (!willShow) {
      if (BrowserCheck.isIPad()) {
        showSuggestApp();
      }
    }

    this.eventRegistration = eventRegistration;
    enterKeyButtonHelper = new KeyPressHelper(true) {
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
    setEnterKeyButtonHelper(enterKeyButtonHelper);
    signInForm.setEnterKeyButtonHelper(enterKeyButtonHelper);
    signUpForm.setEnterKeyButtonHelper(enterKeyButtonHelper);
  }

  public void clearSignInHasFocus() {
    signInHasFocus = false;
  }

  public void setSignInHasFocus() {
    signInHasFocus = true;
  }
/*  private boolean checkWelcome() {
    if (!hasShownWelcome() && props.shouldShowWelcome()) {
      keyStorage.storeValue(SHOWN_HELLO, "yes");
      showWelcome();
      return true;
    } else return false;
  }*/

/*
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
*/

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
            signInForm.setFocusOnUserID();
          }
        },
        true);
    modal.setMaxHeigth((600) + "px");
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

    rightDiv.add(signInForm.populateSignInForm(getSignInForm(), getForgotRow(), enterKeyButtonHelper));
    rightDiv.add(signUpForm.getSignUpForm());
  }

  /**
   * @return
   * @see SignInForm#populateSignInForm
   */
  private Panel getForgotRow() {
    Panel hp2 = new HorizontalPanel();

    Anchor forgotUser = getForgotUser();
    forgotUser.addStyleName("topFiveMargin");
    hp2.add(forgotUser);
    forgotUser.addStyleName("leftTenMargin");

    Anchor forgotPassword = signInForm.getForgotPassword();
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

/*
  private Anchor getForgotPassword() {
    final Anchor forgotPassword = new Anchor(FORGOT_PASSWORD);
    forgotPassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (user.getText().isEmpty()) {
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
       *//*       System.out.println("email is '" + text+ "' ");*//*
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
        setFocusOn(emailEntry);
      }
    });
    return forgotPassword;
  }*/


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
    left.addStyleName("floatLeft");
    left.setWidth(LEFT_SIDE_WIDTH + "px");
    leftAndRight.add(left);
    int size = 1;

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
}
