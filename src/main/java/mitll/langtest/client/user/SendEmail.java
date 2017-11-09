package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;

/**
 * Created by go22670 on 2/1/17.
 */
class SendEmail extends UserDialog {
  private DecoratedPopupPanel resetEmailPopup;
  private Button sendEmail;
  private final EventRegistration eventRegistration;
  //private final FormField userField;

  private static final String ENTER_YOUR_EMAIL_TO_RESET_YOUR_PASSWORD = "Enter your email to reset your password.";

//  private static final String FORGOT_PASSWORD = "Forgot password?";
//  private static final String ENTER_A_USER_NAME = "Enter a user name.";
  private static final String CHECK_EMAIL = "Check Email";
  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email";
  private static final String SEND = "Send Reset Email";
  private static final int EMAIL_POPUP_DELAY = 4000;
  private static final String PLEASE_CHECK = "Please check";

  SendEmail(EventRegistration registration) {
    super(null);
    this.eventRegistration = registration;
   // this.userField = userField;
  }

  void showSendEmail(Anchor forgotPassword, final String text, boolean hasEmail) {
    Heading prompt = new Heading(5, "Click the button to reset.");
    resetEmailPopup = new DecoratedPopupPanel(true);

    sendEmail = new Button(SEND);
    sendEmail.setType(ButtonType.PRIMARY);
    sendEmail.addStyleName("leftTenMargin");

    eventRegistration.register(sendEmail, "N/A", "reset password");

    if (hasEmail) {
      sendEmail.addClickHandler(event -> onSendReset(text));
      makePopup(resetEmailPopup, prompt, sendEmail, "");//ENTER_YOUR_EMAIL_TO_RESET_YOUR_PASSWORD);
    } else {
      final TextBox emailEntry = new TextBox();

      sendEmail.addClickHandler(event -> {
        String text1 = emailEntry.getText();
        if (!isValidEmail(text1)) {
          markErrorBlur(emailEntry, PLEASE_CHECK, VALID_EMAIL, Placement.TOP);
          return;
        }

        onSendReset(text1);
      });

      makePopup(resetEmailPopup, emailEntry, sendEmail, ENTER_YOUR_EMAIL_TO_RESET_YOUR_PASSWORD);
    }

    resetEmailPopup.showRelativeTo(forgotPassword);
  }

  /**
   * So - two cases - old legacy users have no email, new ones do.
   * Potentially we could skip asking users for their email...?
   */
  private void onSendReset(final String userID) {
    sendEmail.setEnabled(false);
    //  final String userID = userField.box.getText();
    service.resetPassword(userID,
        new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            sendEmail.setEnabled(true);
          }

          @Override
          public void onSuccess(Boolean result) {
            String heading = result ? CHECK_EMAIL : "Unknown email";
            String message = result ? PLEASE_CHECK_YOUR_EMAIL : userID + " doesn't have that email. Check for a typo?";
            setupPopover(sendEmail, heading, message, Placement.LEFT, EMAIL_POPUP_DELAY, new BasicDialog.MyPopover() {
              boolean isFirst = true;

              @Override
              public void hide() {
                super.hide();
                if (isFirst) {
                  isFirst = false;
                } else {
                  resetEmailPopup.hide(); // TODO : ugly - somehow hide is called twice
                }
              }
            }, false);
          }
        });
  }

  /**
   * @return
   * @see UserPassLogin#UserPassLogin
   */
  boolean clickSendEmail() {
    if (resetEmailPopup != null && resetEmailPopup.isShowing()) {
      sendEmail.fireEvent(new KeyPressHelper.ButtonClickEvent());
      return true;
    } else return false;
  }
}
