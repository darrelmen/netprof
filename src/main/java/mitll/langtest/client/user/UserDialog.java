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
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.services.UserService;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/10/13
 * Time: 4:26 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class UserDialog extends BasicDialog {
  static final String VALID_EMAIL = "Please enter a valid email address.";
  static final int USER_ID_MAX_LENGTH = 35;

  static final int MIN_AGE = 12;
  static final int MAX_AGE = 90;
  protected static final String SIGN_UP_WIDTH = 366 +
      "px";

  final PropertyHandler props;
  private KeyPressHelper enterKeyButtonHelper;

  protected final UserServiceAsync service = GWT.create(UserService.class);

  /**
   * @see SignUpForm#SignUpForm
   * @param props
   */
  protected UserDialog(PropertyHandler props) { this.props = props;  }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#makeEnglishRow
   * @param dialogBox
   * @param label
   * @param isPassword
   * @param minLength
   * @param maxLength
   * @param hint
   * @return
   */
  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    user.setMaxLength(maxLength);
    return getFormField(dialogBox, label, user, minLength, hint);
  }

  private FormField getFormField(Panel dialogBox, String label, TextBox user, int minLength, String hint) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user, hint);
    return new FormField(user, userGroup, minLength);
  }

  protected void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, TRY_AGAIN, message, Placement.TOP);
  }

  protected void markErrorBlur(FormField dialectGroup, String message) {
    markErrorBlur(dialectGroup.group, dialectGroup.box, TRY_AGAIN, message, Placement.TOP, true);
  }

  /**
   * When would we want it to be different for testing?
   * @param url
   * @return
   */
  String trimURL(String url) {
    if (url.contains("127.0.0.1")) {
      return url.split("\\?")[0].split("#")[0];
    }
    else {
      return url.split("\\?")[0].split("#")[0];
    }
  }

  protected boolean isValidEmail(String text) {
    return text.trim().toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
  }

  /**
   * TODO: store selector and validator?
   *
   * @param result
   * @param userManager
   * @paramx passwordHash
   * @seex SignInForm#foundExistingUser
   * @see SignUpForm#gotSignUp
   *
   */
  void storeUser(User result, UserManager userManager) {
    //logger.info("UserPassLogin.storeUser - " + result);
    enterKeyButtonHelper.removeKeyHandler();
    userManager.storeUser(result
        //, passwordHash
    );
  }

  public void setEnterKeyButtonHelper(KeyPressHelper enterKeyButtonHelper) {
    this.enterKeyButtonHelper = enterKeyButtonHelper;
  }

  protected Form getUserForm() {
    Form signInForm = new Form();
    signInForm.addStyleName("topMargin");
    signInForm.addStyleName("formRounded");
    signInForm.getElement().getStyle().setBackgroundColor("white");
    return signInForm;
  }

   void setFocusOn(final FocusWidget widget) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        widget.setFocus(true);
      }
    });
  }

  /**
   * @param commentPopup
   * @param commentEntryText
   * @param okButton
   * @param prompt
   * @see SignInForm#getForgotPassword
   * @see UserPassLogin#getForgotUser
   */
   void makePopup(Panel commentPopup, Widget commentEntryText, Widget okButton, String prompt) {
    Panel vp = new VerticalPanel();
    Panel w = new Heading(6, prompt);
    vp.add(w);
    w.addStyleName("bottomFiveMargin");
    Panel hp = new HorizontalPanel();
    if (commentEntryText != null) {
      hp.add(commentEntryText);
    }
    hp.add(okButton);
    vp.add(hp);
    commentPopup.add(vp);
  }

  protected Panel getTwoPartForm(Heading heading, Fieldset fieldset) {
    Form form = getUserForm();
    form.add(heading);
    form.add(fieldset);
    return form;
  }

  @NotNull
  protected Button getFormButton(String buttonID, String signUpTitle, EventRegistration eventRegistration) {
    Button signUp = new Button(signUpTitle);
    signUp.getElement().setId(buttonID);
    eventRegistration.register(signUp);

    signUp.addStyleName("floatRight");
    signUp.addStyleName("rightFiveMargin");
    signUp.addStyleName("leftFiveMargin");
    signUp.setType(ButtonType.SUCCESS);
    return signUp;
  }

  protected void styleBox(UIObject userBox) {
    userBox.addStyleName("topMargin");
    userBox.addStyleName("rightFiveMargin");
    userBox.setWidth(SIGN_UP_WIDTH);
  }
}
