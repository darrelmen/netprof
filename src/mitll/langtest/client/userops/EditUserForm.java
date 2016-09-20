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

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Fieldset;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.user.SignUpForm;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserPassDialog;
import mitll.langtest.shared.user.User;

public class EditUserForm extends SignUpForm {
  /**
   * @param props
   * @param userManager
   * @param eventRegistration
   * @param userPassLogin
   * @see UserPassLogin#UserPassLogin
   */
  public EditUserForm(PropertyHandler props, UserManager userManager, EventRegistration eventRegistration, UserPassDialog userPassLogin) {
    super(props, userManager, eventRegistration, userPassLogin);
    setMarkFieldsWithLabels(true);
  }

  CheckBox enabled;
  /**
   * @see SignUpForm#getSignUpForm(User)
   * @param user
   * @return
   */
  protected Fieldset getFields(User user) {
    Fieldset fields = super.getFields(user);

    enabled = new CheckBox("Enabled");
    enabled.setValue(user.isEnabled());
    enabled.addStyleName("leftTenMargin");

    addControlGroupEntry(fields, "Enabled?", enabled, "Lock or unlock user");

  //  fields.add(enabled);
    return fields;
  }
}
