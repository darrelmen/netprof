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
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;

public class SendResetPassword extends UserDialog {
  //  private final Logger logger = Logger.getLogger("ResetPassword");
  private static final String ENTER_USER_ID = "Enter User ID";
  private static final String SEND_RESET_PASSWORD = "Send Reset Password";
  private static final String PLEASE_ENTER_A_USER_ID = "Please enter a user id.";
  private static final String UNKNOWN_USER = "Unknown user.";
  private static final String PLEASE_CHECK_YOUR_EMAIL = "Please check your email.";

  private static final String SUCCESS = "Success";

  private final EventRegistration eventRegistration;
  private final KeyPressHelper enterKeyButtonHelper;
  private final UserManager userManager;

  /**
   * @param props
   * @param eventRegistration
   * @see
   */
  public SendResetPassword(PropertyHandler props,
                           EventRegistration eventRegistration,
                           UserManager userManager) {
    super(props);
    this.eventRegistration = eventRegistration;
    enterKeyButtonHelper = new KeyPressHelper(false);
    this.userManager = userManager;
  }

  /**
   * @return
   * @see InitialUI#handleResetPass
   */
  public Panel getResetPassword() {
    final Fieldset fieldset = new Fieldset();

    Panel container = getLoginContainer(fieldset);

    Heading w = new Heading(3, ENTER_USER_ID);
    fieldset.add(w);
    w.addStyleName("leftFiveMargin");

    final TextBox user = new TextBox();
    user.setMaxLength(35);
    user.setPlaceholder(USER_ID);
    String pendingUserID = userManager.getPendingUserID();
    user.setText(pendingUserID);
    FormField useridField = getSimpleFormField(fieldset, user, 4);

    Button changePasswordButton = getChangePasswordButton(useridField);

    fieldset.add(changePasswordButton);

    setFocusOn(useridField.getWidget());
    return container;
  }

  /**
   * @see #getResetPassword
   */
  private Button getChangePasswordButton(final FormField userID) {
    final Button changePassword = getChangePasswordButton(SEND_RESET_PASSWORD, enterKeyButtonHelper, eventRegistration);
    changePassword.addClickHandler(event -> onChangePassword(userID, changePassword));
    return changePassword;
  }

  /**
   * @param changePassword
   */
  private void onChangePassword(FormField userIDForm,
                                final Button changePassword) {

    if (userIDForm.isEmpty()) {
      markErrorBlur(userIDForm, PLEASE_ENTER_A_USER_ID);
    } else {
      changePassword.setEnabled(false);
      enterKeyButtonHelper.removeKeyHandler();

      String safeText = userIDForm.getSafeText();

      if (safeText.length() == 4) safeText += "_"; // legacy user ids can be 4 but domino requires length 5
      openUserService.resetPassword(safeText, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {
          changePassword.setEnabled(true);
          markErrorBlur(changePassword, NO_SERVER);
        }

        @Override
        public void onSuccess(Boolean result) {
          if (!result) {
            markErrorBlur(userIDForm, UNKNOWN_USER);
            changePassword.setEnabled(true);
          } else {
            markErrorBlur(changePassword, SUCCESS, PLEASE_CHECK_YOUR_EMAIL, Placement.LEFT);
            reloadPageInThreeSeconds();
          }
        }
      });
    }
  }

  private void reloadPageInThreeSeconds() {
    Timer t = new Timer() {
      @Override
      public void run() {
        reloadPage();
      }
    };
    t.schedule(2000);
  }

  private void reloadPage() {
    String newURL = trimURL(Window.Location.getHref());
    Window.Location.replace(newURL);
  }
}
