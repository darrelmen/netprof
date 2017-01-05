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

package mitll.langtest.client.userops;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.user.SignUpForm;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserPassDialog;
import mitll.langtest.shared.user.User;

import java.util.logging.Logger;

/**
 * @deprecated we don't do this in netprof
 */
class EditUserForm extends SignUpForm {
  private final Logger logger = Logger.getLogger("SignUpForm");
  private final User toEdit;
  private final UserOps userOps;
  private final User.Kind userKind;

  /**
   * @param props
   * @param userManager
   * @param eventRegistration
   * @param userPassLogin
   * @see OpsUserContainer#populateUserEdit(DivWidget, User)
   */
  EditUserForm(PropertyHandler props,
               UserManager userManager,
               EventRegistration eventRegistration,
               UserPassDialog userPassLogin,
               User toEdit,
               UserOps userOps) {
    super(props, userManager, eventRegistration, userPassLogin);
    setMarkFieldsWithLabels(true);
    //setRolesHeader("Current user role");
    this.toEdit = toEdit;
    this.userOps = userOps;
    userKind = userManager.getCurrent().getUserKind();
  }

  /*private CheckBox enabled;

  *//**
   * @param user
   * @return
   * @see SignUpForm#getSignUpForm(User)
   *//*
  protected Fieldset getFields(User user) {
    Fieldset fields = super.getFields(user);

    fields.add(getHeader("Lock/Unlock Account"));
    enabled = new CheckBox("Enabled");
//    logger.info("user enabled : " + user.isEnabled());
    enabled.setValue(user.isEnabled());
    enabled.addStyleName("leftTenMargin");

    ControlGroup group = addControlGroupEntry(fields,
        //"Enabled?"
        ""
        , enabled, "");//"Lock or unlock user");
    addTooltip(group, "Lock out or unlock user");

    Collection<User.Permission> possiblePermsForRole = User.getPossiblePermsForRole(user.getUserKind());
    if (!possiblePermsForRole.isEmpty()) {
      int size = possiblePermsForRole.size();
      int numRows = (int) Math.ceil(size / 2) + 1;
      logger.info("size " + size + " rows " + numRows);
      Grid grid = new Grid(numRows, 2);
      int n = 0;
      for (User.Permission permission : possiblePermsForRole) {
        grid.setWidget(n / 2, n % 2, getPermChoice(permission, user.getPermissions().contains(permission)));
        n++;
      }

      fields.add(getHeader("Permissions"));
      fields.add(grid);
    }

    return fields;
  }

  Map<User.Permission, CheckBox> permToCheck = new HashMap<>();

  private CheckBox getPermChoice(User.Permission permission, boolean isCurrent) {
    CheckBox checkBox = new CheckBox(permission.getName());
    permToCheck.put(permission, checkBox);
    checkBox.setValue(isCurrent);
    return checkBox;
  }

  protected boolean isFormValid(String userID) {
    String emailText = signUpEmail.box.getValue();

    if (!emailText.isEmpty() && !isValidEmail(emailText)) {
      markInvalidEmail();
      return false;
    } else return true;
  }

  protected void gotSignUp(final String user, String freeTextPassword, String email, User.Kind kind) {
    signUp.setEnabled(false);

    User updated = new User(toEdit);
    //  logger.info("Role for " +user + " = " +selectedRole);

    if (askForDemographic(kind)) {
      updated.setAge(getAge(true));
      updated.setMale(isMale(true));
      updated.setDialect(getDialect(true));
    }
    updated.setUserKind(selectedRole);
    updated.setUserID(user);
    updated.setEmail(email);
    updated.setUserKind(kind);
    updated.setFirst(firstName.getSafeText());
    updated.setLast(lastName.getSafeText());
    updated.setEnabled(enabled.getValue());

    List<User.Permission> perms = new ArrayList<>();
    for (Map.Entry<User.Permission, CheckBox> pair : permToCheck.entrySet()) {
      if (pair.getValue().getValue()) perms.add(pair.getKey());
    }
    updated.setPermissions(perms);
    logger.info("updating with " + updated);

    userManager.getUserService().update(updated, BOGUS_AGE, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable throwable) {
      }

      @Override
      public void onSuccess(Void aVoid) {
        logger.info("Consider updating the user list");
        userOps.reload();
      }
    });
  }

  protected Collection<User.Kind> getRoles() {
    Collection<User.Kind> visibleRoles = User.getVisibleRoles();
    List<User.Kind> choices = new ArrayList<>();
    for (User.Kind role : visibleRoles) {
      if (role.compareTo(userKind) < 0) choices.add(role);
    }
    return choices;
  }*/
}
