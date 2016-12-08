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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/2/14.
 */
public class ResetPassword extends UserDialog {
  private static final int MIN_PASSWORD = 4;

  private static final String PASSWORD = "Password";

  private static final String PLEASE_ENTER_A_PASSWORD = "Please enter a password";
  private static final String PLEASE_ENTER_A_LONGER_PASSWORD = "Please enter a longer password";
  private static final String PLEASE_ENTER_THE_SAME_PASSWORD = "Please enter the same password";
  private static final String PASSWORD_HAS_BEEN_CHANGED = "Password has been changed";
  private static final String SUCCESS = "Success";
  private static final String CHANGE_PASSWORD = "Change Password";
  private static final String CHOOSE_A_NEW_PASSWORD = "Choose a new password";

  private final EventRegistration eventRegistration;
  private final KeyPressHelper enterKeyButtonHelper;

  /**
   * @see
   * @param props
   * @param eventRegistration
   */
  public ResetPassword(PropertyHandler props, EventRegistration eventRegistration) {
    super(props);
    this.eventRegistration = eventRegistration;
    enterKeyButtonHelper = new KeyPressHelper(false);
  }

  /**
   * @param token
   * @return
   * @see mitll.langtest.client.InitialUI#handleResetPass
   */
  public Panel getResetPassword(final String token) {
    Panel container = new DivWidget();
    container.getElement().setId("ResetPassswordContent");

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

    Heading w = new Heading(3, CHOOSE_A_NEW_PASSWORD);
    fieldset.add(w);
    w.addStyleName("leftFiveMargin");
    final BasicDialog.FormField firstPassword = getPasswordField(fieldset,PASSWORD);
   // final BasicDialog.FormField secondPassword = ;
 //   addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, "Confirm " + PASSWORD);

    //  firstPassword.getWidget().setTabIndex(0);
    // secondPassword.getWidget().setTabIndex(1);

    Button changePasswordButton =
        getChangePasswordButton(
            token,
            firstPassword,
            getPasswordField(fieldset,"Confirm " + PASSWORD));

    fieldset.add(changePasswordButton);

    right.add(rightDiv);
    setFocusOn(firstPassword.getWidget());
    return container;
  }

  private FormField getPasswordField(Fieldset fieldset, String hint) {
    return addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, hint);
  }

  /**
   * @param token
   * @param firstPassword  what we use to reset the password
   * @param secondPassword just so we can make sure the user didn't make a typo
   * @see #getResetPassword
   */
  private Button getChangePasswordButton(final String token,
                                         final BasicDialog.FormField firstPassword,
                                         final BasicDialog.FormField secondPassword) {
    final Button changePassword = new Button(CHANGE_PASSWORD);
    changePassword.setType(ButtonType.PRIMARY);

    // changePassword.setTabIndex(3);
    changePassword.getElement().setId("changePassword");
    changePassword.addStyleName("floatRight");
    changePassword.addStyleName("rightFiveMargin");
    changePassword.addStyleName("leftFiveMargin");

    changePassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onChangePassword(firstPassword, secondPassword, changePassword, token);
      }
    });
    enterKeyButtonHelper.addKeyHandler(changePassword);

    eventRegistration.register(changePassword);

    return changePassword;
  }

/*  private String rot13(String val) {
    StringBuilder builder = new StringBuilder();
    for (char c : val.toCharArray()) {
      if (c >= 'a' && c <= 'm') c += 13;
      else if (c >= 'A' && c <= 'M') c += 13;
      else if (c >= 'n' && c <= 'z') c -= 13;
      else if (c >= 'N' && c <= 'Z') c -= 13;
      builder.append(c);
    }
    return builder.toString();
  }*/

  /**
   *
   * @param firstPassword
   * @param secondPassword for confirmation
   * @param changePassword
   * @param token
   */
  private void onChangePassword(FormField firstPassword,
                                FormField secondPassword,
                                final Button changePassword,
                                String token) {
    String newPassword = firstPassword.box.getText();
    String second = secondPassword.box.getText();
    if (newPassword.isEmpty()) {
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

      String hashNewPassword = Md5Hash.getHash(newPassword);

     // newPassword = rot13(newPassword);

      service.changePFor(token, hashNewPassword, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {
          changePassword.setEnabled(true);
          markErrorBlur(changePassword, "Can't communicate with server - check network connection.");
        }

        @Override
        public void onSuccess(Boolean result) {
          if (!result) {
            markErrorBlur(changePassword, "Password has already been changed?");
          } else {
            markErrorBlur(changePassword, SUCCESS, PASSWORD_HAS_BEEN_CHANGED, Placement.LEFT);
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
    t.schedule(3000);
  }

  private void reloadPage() {
    String newURL = trimURL(Window.Location.getHref());
    Window.Location.replace(newURL);
    Window.Location.reload();
  }
}
