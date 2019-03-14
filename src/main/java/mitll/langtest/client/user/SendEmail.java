/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
  public static final String CLICK_THE_BUTTON_TO_RESET = "Click the button to reset.";
  private DecoratedPopupPanel resetEmailPopup;
  private Button sendEmail;
  private final EventRegistration eventRegistration;

  private static final String ENTER_YOUR_EMAIL_TO_RESET_YOUR_PASSWORD = "Enter your email to reset your password.";

  private static final String CHECK_EMAIL = "Check Email";
  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email";
  /**
   * @see #showSendEmail(Anchor, String, boolean)
   */
  private static final String SEND = "Send Reset Email";
  private static final int EMAIL_POPUP_DELAY = 4000;
  private static final String PLEASE_CHECK = "Please check";

  SendEmail(EventRegistration registration) {
    super(null);
    this.eventRegistration = registration;
  }

  void showSendEmail(Anchor forgotPassword, final String text, boolean hasEmail) {
    Heading prompt = new Heading(5, CLICK_THE_BUTTON_TO_RESET);
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

    openUserService.resetPassword(userID,
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
            }, false, true);
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
