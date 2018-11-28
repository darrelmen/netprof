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
package mitll.langtest.client.domino.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.constants.FormType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Label;
import mitll.langtest.client.banner.UserMenu;
import mitll.langtest.client.domino.common.*;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserDialog;
import mitll.langtest.client.user.UserState;
import mitll.langtest.shared.user.User;

import java.util.logging.Logger;

/**
 * ChangePasswordView
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since May 28, 2013 10:50:11 PM
 */
public class ChangePasswordView extends Composite {
  private static final Logger log = Logger.getLogger(ChangePasswordView.class.getName());

  private DominoSaveableModal modal;

  // private DecoratedFields currentPWDF = null;
  private DecoratedFields pass1DF;
  private DecoratedFields pass2DF;

  private User editUser;
  private final CommonValidation cValidator = new CommonValidation();
  private boolean forcePWChange;
  private final UserState userState;
  private final UserServiceAsync userServiceAsync;

  private final UserDialog basicDialog;

  /**
   * @param editUser
   * @param forcePWChange
   * @param userState
   * @param userServiceAsync
   * @see UserMenu.ChangePasswordClickHandler
   */
  public ChangePasswordView(User editUser,
                            boolean forcePWChange,
                            UserState userState,
                            PropertyHandler props,
                            UserServiceAsync userServiceAsync) {
    this.editUser = editUser;
    this.forcePWChange = forcePWChange;
    this.userState = userState;
    this.userServiceAsync = userServiceAsync;
    basicDialog = new UserDialog(props);
    init();
  }

  private DecoratedFields getFirstDecoratedField() {
    return /*(currentPWDF != null) ? currentPWDF :*/ pass1DF;
  }

  private void init() {
    Form form = new Form();
    form.setType(FormType.HORIZONTAL);
    UIHandler uiHandler = new UIHandler();
    if (editUser == null) {
      initWidget(form);
      return;
    }
    if (forcePWChange) {
      Label pwLabel = new Label("Your password has expired and must be changed.");
      pwLabel.addStyleName("force-pw-label");
      form.add(pwLabel);
    }
    form.add(new DecoratedFormValue("User ID", editUser.getUserID()).getCtrlGroup());

//    User cUser = userState.getAvg();
    /**
     * When would we want them not to re-enter the password - I guess if it's a reset for someone else???
     */
/*    if (cUser.getID() == editUser.getID() ||
        //((!(cUser.hasRole(Role.UM)))&&(!cUser.hasRole(Role.GrAM)))
        !cUser.isAdmin()
        ) { // Role should always be UM or Group AM.
      currPWBox = new PasswordTextBox();
      currPWBox.addKeyPressHandler(uiHandler);
      currentPWDF = new DecoratedFields("Current Password", currPWBox);
      form.add(currentPWDF.getCtrlGroup());
    }*/
    // TODO : should we be sending passwords?
    /*else {
      emailBox = new CheckBox("Email new password?");
      emailBox.setValue(false);
      emailDF = new DecoratedFields(null, emailBox, null, null);
    }*/

    //   PasswordTextBox currPWBox = new PasswordTextBox();
//    currPWBox.addKeyPressHandler(uiHandler);
//    currentPWDF = new DecoratedFields("Current Password", currPWBox);
//    currPWBox.setVisible(false);
    //form.add(currentPWDF.getCtrlGroup());


    PasswordTextBox p1Box = new PasswordTextBox();

    p1Box.addKeyPressHandler(uiHandler);
    pass1DF = new DecoratedFields("Password", p1Box);

    configurePassword(p1Box, pass1DF.getCtrlGroup(), Placement.BOTTOM);

    Scheduler.get().scheduleDeferred(() -> p1Box.setFocus(true));


    form.add(pass1DF.getCtrlGroup());
    PasswordTextBox p2Box = new PasswordTextBox();
    p2Box.addKeyPressHandler(uiHandler);

    pass2DF = new DecoratedFields("Verify password", p2Box);
    configurePassword(p2Box, pass2DF.getCtrlGroup(), Placement.TOP);
    form.add(pass2DF.getCtrlGroup());
//    if (emailDF != null) {
//      form.add(emailDF.getCtrlGroup());
//    }
    initWidget(form);
  }

  private void configurePassword(PasswordTextBox p1Box, ControlGroup group, Placement bottom) {
    basicDialog.setMaxPasswordLength(p1Box);
    basicDialog.addPasswordFeedback(group, p1Box, bottom);
  }

  @Override
  protected void onLoad() {
    Scheduler.get().scheduleDeferred(() -> ((Focusable) getFirstDecoratedField().getPrimaryControl()).setFocus(true));
  }

  public void showModal() {
    modal = new DominoSaveableModal(true, "Change password", "Update",
        DominoSimpleModal.ModalSize.Small, this) {
      @Override
      protected void handleSave() {
        tryToSave();
      }

      @Override
      protected void handleClose() {
        super.handleClose();
        if (forcePWChange) {
          userState.logout();
          //		getSessionHelper().goToLoginPage();
        }
      }
    };
    if (forcePWChange) {
      modal.setKeyboard(false);
      modal.setCloseButtonName("Return to Login");
    }
    modal.addStyleName("ch-password-modal");
    modal.init();
  }

  private void tryToSave() {
    if (validateAndWarn()) {
      changePassword();
      if (modal != null) {
        modal.hide();
      }
    }
  }

  private boolean validateAndWarn() {
    //boolean cPassValid = true;//currentPWDF == null || currentPWDF.performBasicValidate();
    boolean pass1Valid = pass1DF.performBasicValidate();
    boolean pass2Valid = pass2DF.performBasicValidate();

    if (pass1Valid && pass2Valid) {
      String p1 = (String) pass1DF.getValue();
      if (!p1.equals(pass2DF.getValue())) {
        pass1DF.setError("Passwords don't match!");
        pass1Valid = false;
        pass2DF.setError("");
        pass2Valid = false;
//      } else if (currentPWDF != null && p1.equals(currentPWDF.getValue())) {
//        pass1DF.setError("New password can not be same as current!");
//        pass1Valid = false;
      } else {
        CommonValidation.Result r = cValidator.validatePassword(p1);
        if (r.errorFound) {
          pass1DF.setError(r.message);
          pass1Valid = false;
          pass2DF.setError("");
          pass2Valid = false;
        }
      }
    }
    return pass1Valid && pass2Valid;
  }

  private void changePassword() {
    //	final Modal m = getMsgHelper().makeWaitDialog("Updating password");
    //String currPass = (currentPWDF != null) ? (String) currentPWDF.getValue() : null;
    String newPass = (String) pass1DF.getValue();
    // boolean sendEmail = emailBox != null && emailBox.getValue();
    final DecoratedFields df = getFirstDecoratedField();

    String hashNewPass = newPass;// Md5Hash.getHash(newPass);
    //  String hashCurrPass = currPass;//Md5Hash.getHash(currPass);
    userServiceAsync.changePasswordWithCurrent(
        // currPass == null ? "" : hashCurrPass,
        "",
        hashNewPass,
        //sendEmail,
        new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            df.clearError();
            df.setError("Password update failed.");

            modal.show();
          }

          @Override
          public void onSuccess(Boolean result) {
            //		m.hide();
            if ((result == null) || (!result)) {
              df.clearError();
              df.setError("Password update failed.");
              modal.show();
            } else // if (result //&&
            //editUser.getDocumentDBID() == getState().getCurrentUser().getDocumentDBID()
            //    )
            {
//					getSessionHelper().logoutUserInClient(m, false);
              userState.logout();
            }
          }
        });
  }

  private class UIHandler implements KeyPressHandler {
    @Override
    public void onKeyPress(KeyPressEvent event) {
      int keyCode = event.getNativeEvent().getKeyCode();
      if (keyCode == KeyCodes.KEY_ENTER) {
        tryToSave();
      }
    }
  }
}
