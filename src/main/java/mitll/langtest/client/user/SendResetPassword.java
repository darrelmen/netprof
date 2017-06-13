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
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.instrumentation.EventRegistration;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/2/14.
 */
public class SendResetPassword extends UserDialog {
  private final Logger logger = Logger.getLogger("ResetPassword");

  private static final String SUCCESS = "Success";

  private final EventRegistration eventRegistration;
  private final KeyPressHelper enterKeyButtonHelper;
  private final UserManager userManager;

  /**
   * @param props
   * @param eventRegistration
   * @see
   */
  public SendResetPassword(PropertyHandler props, EventRegistration eventRegistration, UserManager userManager) {
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
    container.getElement().setId("SendResetPassswordContent");

    DivWidget child = new DivWidget();
    container.add(child);
    child.addStyleName("loginPageBack");

    Panel leftAndRight = new DivWidget();
    leftAndRight.addStyleName("resetPage");
    container.add(leftAndRight);

    DivWidget right = new DivWidget();

    leftAndRight.add(right);
    right.addStyleName("floatRight");

    DivWidget rightDiv = new DivWidget();

    Form form = new Form();
    form.getElement().setId("resetForm");
    rightDiv.add(form);

    form.addStyleName("topMargin");
    form.addStyleName("formRounded");
    form.getElement().getStyle().setBackgroundColor("white");

    final Fieldset fieldset = new Fieldset();
    form.add(fieldset);

    Heading w = new Heading(3, "Enter User ID");
    fieldset.add(w);
    w.addStyleName("leftFiveMargin");

    final TextBox user = new TextBox();
    user.setMaxLength(35);
    user.setPlaceholder("User ID");
    String pendingUserID = userManager.getPendingUserID();
    user.setText(pendingUserID);
    FormField useridField = getSimpleFormField(fieldset, user, 4);

    Button changePasswordButton = getChangePasswordButton(useridField);

    fieldset.add(changePasswordButton);

    right.add(rightDiv);

    setFocusOn(useridField.getWidget());
    return container;
  }

  /**
   * @see #getResetPassword
   */
  private Button getChangePasswordButton(final FormField userID) {
    final Button changePassword = new Button("Send Reset Password");
    changePassword.setType(ButtonType.PRIMARY);

    // changePassword.setTabIndex(3);
    changePassword.getElement().setId("changePassword");
    changePassword.addStyleName("floatRight");
    changePassword.addStyleName("rightFiveMargin");
    changePassword.addStyleName("leftFiveMargin");

    changePassword.addClickHandler(event -> onChangePassword(userID, changePassword));
    enterKeyButtonHelper.addKeyHandler(changePassword);
    eventRegistration.register(changePassword);

    return changePassword;
  }

  /**
   * @param changePassword
   */
  private void onChangePassword(FormField userIDForm,
                                final Button changePassword) {

    if (userIDForm.isEmpty()) {
      markErrorBlur(userIDForm, "Please enter a user id.");
    } else {
      changePassword.setEnabled(false);
      enterKeyButtonHelper.removeKeyHandler();

      service.resetPassword(userIDForm.getSafeText(), new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {
          changePassword.setEnabled(true);
          markErrorBlur(changePassword, "Can't communicate with server - check network connection.");
        }

        @Override
        public void onSuccess(Boolean result) {
          if (!result) {
            markErrorBlur(userIDForm, "Unknown user.");
            changePassword.setEnabled(true);
          } else {
            markErrorBlur(changePassword, SUCCESS, "Please check your email.", Placement.LEFT);
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
