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

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.shared.user.ChoosePasswordResult;
import mitll.langtest.shared.user.User;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/2/14.
 */
public class ResetPassword extends UserDialog {
  //private final Logger logger = Logger.getLogger("ResetPassword");
  private static final int MIN_PASSWORD = 8; // Consistent with Domino minimums

  private static final String PASSWORD = "Password";
  private static final String HINT = "Confirm " + PASSWORD;

  private static final String PLEASE_ENTER_A_PASSWORD = "Please enter a password";

  /**
   *
   */
  private static final String PLEASE_ENTER_A_LONGER_PASSWORD = "Please enter a longer password";
  private static final String PLEASE_ENTER_THE_SAME_PASSWORD = "Please enter the same password";
  private static final String PASSWORD_HAS_BEEN_CHANGED = "Password has been changed";
  private static final String SUCCESS = "Success";
  private static final String CHANGE_PASSWORD = "Change Password";
  private static final String CHOOSE_A_NEW_PASSWORD = "Choose a new password";
  /**
   *
   */
  private static final String PASSWORD_HAS_ALREADY_BEEN_CHANGED = "Couldn't set password - please try again.";
  private static final int DELAY_MILLIS = 2000;
  private static final int OLD_NETPROF_LEN = 4;

  private final EventRegistration eventRegistration;
  private final KeyPressHelper enterKeyButtonHelper;
  private final UserManager userManager;

  /**
   * @param props
   * @param eventRegistration
   * @see InitialUI#handleResetPass
   */
  public ResetPassword(PropertyHandler props, EventRegistration eventRegistration, UserManager userManager) {
    super(props);
    this.eventRegistration = eventRegistration;
    enterKeyButtonHelper = new KeyPressHelper(false);
    this.userManager = userManager;
  }

  /**
   * @param token
   * @return
   * @see InitialUI#handleResetPass
   */
  public Panel getResetPassword(final String token) {
    Panel container = new DivWidget();
    container.getElement().setId("ResetPassswordContent");

    DivWidget child = new DivWidget();
    container.add(child);
    child.addStyleName("loginPageBack");

    Panel leftAndRight = new DivWidget();
    boolean isIOS = Window.getClientWidth() < 800;
    leftAndRight.addStyleName(isIOS ? "resetPageMobile" : "resetPage");
    container.add(leftAndRight);

    DivWidget right = new DivWidget();

    leftAndRight.add(right);
    right.addStyleName("floatRight");

    DivWidget rightDiv = new DivWidget();

    Form form = new Form();
    {
      form.getElement().setId("resetForm");
      rightDiv.add(form);

      form.addStyleName("topMargin");
      form.addStyleName("formRounded");
      form.getElement().getStyle().setBackgroundColor("white");
    }
    final Fieldset fieldset = new Fieldset();
    form.add(fieldset);

    Heading w = new Heading(3, CHOOSE_A_NEW_PASSWORD);
    fieldset.add(w);
    w.addStyleName("leftFiveMargin");

    final TextBox user = new TextBox();
    user.setMaxLength(35);
    user.setPlaceholder(USER_ID);
    String pendingUserID = userManager.getPendingUserID();
    user.setText(userManager.getPendingUserID());
    FormField useridField = getSimpleFormField(fieldset, user, OLD_NETPROF_LEN);

    turnOffAutoCapitalize(useridField);

    final FormField firstPassword = getPasswordField(fieldset, PASSWORD);

    Button changePasswordButton =
        getChangePasswordButton(
            token,
            useridField,
            firstPassword,
            getPasswordField(fieldset, HINT));

    fieldset.add(changePasswordButton);

    right.add(rightDiv);

    setFocusOn((pendingUserID == null || pendingUserID.isEmpty()) ? useridField.getWidget() : firstPassword.getWidget());
    return container;
  }

  private FormField getPasswordField(Fieldset fieldset, String hint) {
    FormField formField = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, hint);
    turnOffAutoCapitalize(formField);
    return formField;
  }

  /**
   * @param token
   * @param firstPassword  what we use to reset the password
   * @param secondPassword just so we can make sure the user didn't make a typo
   * @see #getResetPassword
   */
  private Button getChangePasswordButton(final String token,
                                         final FormField userID,
                                         final FormField firstPassword,
                                         final FormField secondPassword) {
    final Button changePassword = getChangePasswordButton(CHANGE_PASSWORD, enterKeyButtonHelper, eventRegistration);
    changePassword.addClickHandler(event -> onChangePassword(userID, firstPassword, secondPassword, changePassword, token));
    return changePassword;
  }

  /**
   * @param firstPassword
   * @param secondPassword for confirmation
   * @param changePassword
   * @param token
   */
  private void onChangePassword(FormField userIDForm,
                                FormField firstPassword,
                                FormField secondPassword,
                                final Button changePassword,
                                String token) {
    String newPassword = firstPassword.box.getText();
    String second = secondPassword.box.getText();
    if (userIDForm.getSafeText().length()< OLD_NETPROF_LEN) {
      markErrorBlur(userIDForm, "Please enter a longer id.");
    } else if (newPassword.isEmpty()) {
      markErrorBlur(firstPassword, PLEASE_ENTER_A_PASSWORD);
    } else if (newPassword.length() < MIN_PASSWORD) {
      markErrorBlur(firstPassword, PLEASE_ENTER_A_LONGER_PASSWORD);
    } else if (second.isEmpty()) {
      markErrorBlur(secondPassword, PLEASE_ENTER_A_PASSWORD);
    } else if (second.length() < MIN_PASSWORD) {
      markErrorBlur(secondPassword, PLEASE_ENTER_A_LONGER_PASSWORD);
    } else if (!second.equals(newPassword)) {
      markErrorBlur(secondPassword, PLEASE_ENTER_THE_SAME_PASSWORD);
    } else {

      changePassword.setEnabled(false);
      enterKeyButtonHelper.removeKeyHandler();
      String safeText = userIDForm.getSafeText();
      if (safeText.length() == OLD_NETPROF_LEN) safeText += "_"; // legacy user ids can be 4 but domino requires length 5
      openUserService.changePasswordWithToken(safeText, token, newPassword,
          new AsyncCallback<ChoosePasswordResult>() {
            @Override
            public void onFailure(Throwable caught) {
              changePassword.setEnabled(true);
              markErrorBlur(changePassword, NO_SERVER, Placement.LEFT);
            }

            @Override
            public void onSuccess(ChoosePasswordResult result) {
              ChoosePasswordResult.PasswordResultType resultType = result.getResultType();
              if (resultType == ChoosePasswordResult.PasswordResultType.AlreadySet) {
                markErrorBlur(changePassword, PASSWORD_HAS_ALREADY_BEEN_CHANGED, Placement.TOP);
                changePassword.setEnabled(true);
              } else if (resultType == ChoosePasswordResult.PasswordResultType.NotExists) {
                markErrorBlur(changePassword, "No user with this id.", Placement.TOP);
                changePassword.setEnabled(true);
              } else if (resultType == ChoosePasswordResult.PasswordResultType.Success) {
                markErrorBlur(changePassword, SUCCESS, PASSWORD_HAS_BEEN_CHANGED, Placement.TOP);
                reloadPageInThreeSeconds(result.getUser());
              }
            }
          });
    }
  }

  private void reloadPageInThreeSeconds(final User user) {
    Timer t = new Timer() {
      @Override
      public void run() {
        reloadPage(user);
      }
    };
    t.schedule(DELAY_MILLIS);
  }

  private void reloadPage(User user) {
    String newURL = trimURL(Window.Location.getHref());
    userManager.rememberUser(user);
    Window.Location.replace(newURL);
  }
}
